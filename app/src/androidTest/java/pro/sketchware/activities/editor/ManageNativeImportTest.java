package pro.sketchware.activities.editor;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import pro.sketchware.R;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ManageFileBinding;

@RunWith(AndroidJUnit4.class)
public class ManageNativeImportTest {
    @Test(timeout = 30000)
    public void copyingRunsOffMainWhileTheUiRemainsResponsive() throws Exception {
        try (Project project = new Project()) {
            byte[] content = new byte[1024 * 1024 + 3];
            for (int i = 0; i < content.length; i++) content[i] = (byte) i;
            GateFile source = project.gated("libimport.so", content);
            File target = new File(project.abiDir, "libimport.so");
            Files.write(target.toPath(), new byte[]{42});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target.toPath()));
            AtomicInteger pulse = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                assertFalse(binding(activity).showOptionsButton.isEnabled());
                binding(activity).filesListRecyclerView.post(pulse::incrementAndGet);
            });
            waitUntil(() -> pulse.get() > 0);
            source.release.countDown();
            project.awaitIdle();
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertEquals(1, project.abiDir.listFiles().length);
            project.scenario.onActivity(activity -> assertEquals(1, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void importingAnExistingLibraryInPlaceDoesNotEraseItsContents() throws Exception {
        try (Project project = new Project()) {
            File file = new File(project.abiDir, "libkeep.so");
            Files.write(file.toPath(), new byte[]{0, 1, -1});
            project.select(file);
            project.awaitIdle();
            assertArrayEquals(new byte[]{0, 1, -1}, Files.readAllBytes(file.toPath()));
            assertEquals(1, project.abiDir.listFiles().length);
        }
    }

    @Test(timeout = 30000)
    public void failedLibrariesAreReportedWhileOtherLibrariesContinue() throws Exception {
        try (Project project = new Project()) {
            File existing = new File(project.abiDir, "libmissing.so");
            Files.write(existing.toPath(), new byte[]{42});
            File missing = project.sources.resolve("libmissing.so").toFile();
            File valid = project.source("libother.so", new byte[]{0, -1});
            project.select(missing, valid);
            project.awaitIdle();
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(existing.toPath()));
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.abiDir, "libother.so").toPath()));
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("libmissing.so")));
            assertEquals(2, project.abiDir.listFiles().length);

            Files.write(missing.toPath(), new byte[]{7});
            project.select(missing);
            project.awaitIdle();
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(existing.toPath()));
        }
    }

    @Test(timeout = 30000)
    public void recreationRetainsProgressAndTheSelectedAbiDirectory() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("libimport.so", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.recreate();
            project.scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertFalse(binding(activity).showOptionsButton.isEnabled());
                assertEquals(project.abiDir.getPath(), currentPath(activity));
                assertEquals(activity.getString(R.string.nativelib_import_library), binding(activity).showOptionsButton.getText().toString());
            });
            source.release.countDown();
            project.awaitIdle();
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.abiDir, "libimport.so").toPath()));
            project.scenario.onActivity(activity -> assertEquals(1, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void destinationIsCapturedBeforeTheAbiPageChanges() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("libimport.so", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            File otherAbi = new File(project.nativeDir, "arm64-v8a");
            project.enter(otherAbi);
            source.release.countDown();
            project.awaitIdle();
            assertFalse(new File(otherAbi, "libimport.so").exists());
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.abiDir, "libimport.so").toPath()));
            project.scenario.onActivity(activity -> assertEquals(0, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void removingTheDestinationDoesNotRecreateItOrLeaveImportBusy() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("libimport.so", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            Files.delete(project.abiDir.toPath());
            source.release.countDown();
            project.awaitIdle();
            assertFalse(project.abiDir.exists());
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("libimport.so")));
        }
    }

    @Test(timeout = 30000)
    public void finishingTheActivityDoesNotAbortOrRefreshItsDestroyedUi() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("libimport.so", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.close();
            source.release.countDown();
            project.drainImports();
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.abiDir, "libimport.so").toPath()));
        }
    }

    private static ManageFileBinding binding(ManageNativelibsActivity activity) {
        try {
            Field field = ManageNativelibsActivity.class.getDeclaredField("binding");
            field.setAccessible(true);
            return (ManageFileBinding) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static String currentPath(ManageNativelibsActivity activity) {
        try {
            Field field = ManageNativelibsActivity.class.getDeclaredField("nativeLibrariesPath");
            field.setAccessible(true);
            return (String) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private interface Condition { boolean ready() throws Exception; }
    private static void waitUntil(Condition condition) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.ready()) return;
            Thread.sleep(30);
        }
        fail("Import did not complete");
    }

    private static final class Project implements AutoCloseable {
        final String id = "__native_import_" + UUID.randomUUID();
        final File root = new File(SketchwarePaths.getDataPath(id));
        final File nativeDir = new File(SketchwarePaths.getProjectNativeLibsPath(id));
        final File abiDir = new File(nativeDir, "armeabi-v7a");
        final Path sources = Files.createTempDirectory(context().getCacheDir().toPath(), "native_import_");
        final List<GateFile> gates = new ArrayList<>();
        final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        final ActivityScenario<ManageNativelibsActivity> scenario;
        ManageNativelibsActivity activity;

        Project() throws Exception {
            assertFalse("Never overwrite a user project", root.exists());
            assertTrue(root.mkdirs());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) toasts.add(event.getText().toString());
            });
            scenario = ActivityScenario.launch(new Intent(context(), ManageNativelibsActivity.class).putExtra("sc_id", id));
            scenario.onActivity(current -> {
                activity = current;
                current.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
            assertTrue(abiDir.isDirectory());
            assertTrue(new File(nativeDir, "arm64-v8a").isDirectory());
            enter(abiDir);
        }

        File source(String name, byte[] content) throws Exception {
            Path file = sources.resolve(name);
            Files.write(file, content);
            return file.toFile();
        }

        GateFile gated(String name, byte[] content) throws Exception {
            GateFile file = new GateFile(source(name, content).getPath());
            gates.add(file);
            return file;
        }

        void select(File... files) {
            scenario.onActivity(current -> {
                binding(current).showOptionsButton.performClick();
                current.getSupportFragmentManager().executePendingTransactions();
                FilePickerDialogFragment picker = (FilePickerDialogFragment) current.getSupportFragmentManager().findFragmentByTag("filePicker");
                assertNotNull(picker);
                try {
                    FilePickerCallback callback = null;
                    for (Field field : picker.getClass().getDeclaredFields()) {
                        if (FilePickerCallback.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            callback = (FilePickerCallback) field.get(picker);
                            break;
                        }
                    }
                    assertNotNull("Use the manager's real picker callback", callback);
                    picker.dismissNow();
                    callback.onFilesSelected(List.of(files));
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void enter(File folder) {
            scenario.onActivity(current -> {
                try {
                    Field path = ManageNativelibsActivity.class.getDeclaredField("nativeLibrariesPath");
                    path.setAccessible(true); path.set(current, folder.getPath());
                    Method refresh = ManageNativelibsActivity.class.getDeclaredMethod("handleAdapter", String.class);
                    refresh.setAccessible(true); refresh.invoke(current, folder.getPath());
                    Method fab = ManageNativelibsActivity.class.getDeclaredMethod("handleFab");
                    fab.setAccessible(true); fab.invoke(current);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void awaitIdle() throws Exception {
            waitUntil(() -> {
                boolean[] done = {false};
                scenario.onActivity(current -> done[0] = binding(current).showOptionsButton.isEnabled());
                return done[0];
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void drainImports() throws Exception {
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(activity), "Native-import-test-cleanup", done::countDown, null, null);
            assertTrue("Import work must finish before fixtures are removed", done.await(10, TimeUnit.SECONDS));
        }

        @Override public void close() throws Exception {
            for (GateFile gate : gates) gate.release.countDown();
            try {
                scenario.close();
                drainImports();
            } finally {
                InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(null);
                assertEquals(id, root.getName());
                assertEquals(context().getCacheDir().getCanonicalFile(), sources.toFile().getCanonicalFile().getParentFile());
                for (Path owned : new Path[]{root.toPath(), sources}) {
                    try (Stream<Path> paths = Files.walk(owned)) {
                        for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                            assertTrue(path.startsWith(owned)); Files.delete(path);
                        }
                    }
                }
            }
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }

    private static final class GateFile extends File {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);

        GateFile(String path) { super(path); }

        @Override public boolean isDirectory() {
            assertNotSame("Source I/O must leave the UI thread", Looper.getMainLooper(), Looper.myLooper());
            return super.isDirectory();
        }

        @Override public Path toPath() {
            assertNotSame("Source I/O must leave the UI thread", Looper.getMainLooper(), Looper.myLooper());
            entered.countDown();
            try { if (!release.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("Test import gate timed out"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return super.toPath();
        }
    }
}
