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
import java.nio.charset.StandardCharsets;
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
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ManageFileBinding;

@RunWith(AndroidJUnit4.class)
public class ManageAssetsImportTest {
    @Test(timeout = 30000)
    public void copyingRunsOffMainWhileTheUiRemainsResponsive() throws Exception {
        try (Project project = new Project()) {
            byte[] content = new byte[1024 * 1024 + 3];
            for (int i = 0; i < content.length; i++) content[i] = (byte) i;
            GateFile source = project.gated("data.bin", content);
            File target = new File(project.assetsDir, "data.bin");
            Files.write(target.toPath(), new byte[]{42});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target.toPath()));
            AtomicInteger pulse = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                assertFalse(binding(activity).importNewButton.isEnabled());
                binding(activity).filesListRecyclerView.post(pulse::incrementAndGet);
            });
            waitUntil(() -> pulse.get() > 0);
            source.release.countDown();
            project.awaitDone(target);
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertEquals(1, project.assetsDir.listFiles().length);
        }
    }

    @Test(timeout = 30000)
    public void importingFilesAndFoldersInPlaceDoesNotEraseTheirContents() throws Exception {
        try (Project project = new Project()) {
            File file = new File(project.assetsDir, "keep.bin");
            Files.write(file.toPath(), new byte[]{0, 1, -1});
            File folder = new File(project.assetsDir, "nested");
            assertTrue(folder.mkdir());
            File child = new File(folder, "keep.txt");
            Files.write(child.toPath(), "keep this file".getBytes(StandardCharsets.UTF_8));
            project.select(file, folder);
            project.awaitDone(child);
            assertArrayEquals(new byte[]{0, 1, -1}, Files.readAllBytes(file.toPath()));
            assertEquals("keep this file", read(child));
        }
    }

    @Test(timeout = 30000)
    public void directoriesMergeWithoutChangingBinaryFilesOrRemovingOtherChildren() throws Exception {
        try (Project project = new Project()) {
            Path source = Files.createDirectory(project.sources.resolve("bundle"));
            Files.createDirectories(source.resolve("nested/empty"));
            Files.write(source.resolve("nested/data.bin"), new byte[]{0, -128, -1, 10});
            Path target = project.assetsDir.toPath().resolve("bundle");
            Files.createDirectories(target.resolve("nested"));
            Files.write(target.resolve("nested/data.bin"), new byte[]{42});
            Files.write(target.resolve("keep.txt"), new byte[]{7});
            project.select(source.toFile());
            project.awaitDone(target.resolve("nested/data.bin").toFile());
            assertArrayEquals(new byte[]{0, -128, -1, 10}, Files.readAllBytes(target.resolve("nested/data.bin")));
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(target.resolve("keep.txt")));
            assertTrue(Files.isDirectory(target.resolve("nested/empty")));
        }
    }

    @Test(timeout = 30000)
    public void failedEntriesAreReportedWithoutStoppingTheRestOfTheBatch() throws Exception {
        try (Project project = new Project()) {
            File existing = new File(project.assetsDir, "Missing.bin");
            Files.write(existing.toPath(), new byte[]{42});
            File missing = project.sources.resolve("Missing.bin").toFile();
            File valid = project.source("other.txt", "other".getBytes(StandardCharsets.UTF_8));
            project.select(missing, valid);
            project.awaitDone(new File(project.assetsDir, "other.txt"));
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(existing.toPath()));
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("Missing.bin")));
            assertEquals(2, project.assetsDir.listFiles().length);
        }
    }

    @Test(timeout = 30000)
    public void overlappingFoldersAndSymlinkLoopsFailWithoutRecursiveCopying() throws Exception {
        try (Project project = new Project()) {
            File nested = new File(project.assetsDir, "nested");
            assertTrue(nested.mkdir());
            project.enter(nested);
            Path loop = Files.createDirectory(project.sources.resolve("loop"));
            Files.createSymbolicLink(loop.resolve("again"), loop);
            File valid = project.source("other.txt", new byte[]{9});
            project.select(project.assetsDir, loop.toFile(), valid);
            project.awaitDone(new File(nested, "other.txt"));
            assertFalse(new File(nested, project.assetsDir.getName()).exists());
            assertFalse(new File(nested, "loop/again").exists());
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("assets") && text.contains("loop")));
        }
    }

    @Test(timeout = 30000)
    public void recreationRetainsImportProgressAndTheDestinationFolder() throws Exception {
        try (Project project = new Project()) {
            File nested = new File(project.assetsDir, "nested");
            assertTrue(nested.mkdir());
            project.enter(nested);
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.recreate();
            project.scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertFalse(binding(activity).importNewButton.isEnabled());
                assertEquals(nested.getPath(), currentPath(activity));
            });
            source.release.countDown();
            project.awaitDone(new File(nested, "data.bin"));
            project.scenario.onActivity(activity -> assertEquals(1, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(nested, "data.bin").toPath()));
        }
    }

    @Test(timeout = 30000)
    public void destinationIsCapturedBeforeThePageChanges() throws Exception {
        try (Project project = new Project()) {
            File first = new File(project.assetsDir, "first"), second = new File(project.assetsDir, "second");
            assertTrue(first.mkdir()); assertTrue(second.mkdir());
            project.enter(first);
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.enter(second);
            source.release.countDown();
            project.awaitDone(new File(first, "data.bin"));
            assertFalse(new File(second, "data.bin").exists());
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(first, "data.bin").toPath()));
        }
    }

    @Test(timeout = 30000)
    public void finishingTheActivityDoesNotAbortOrRefreshItsDestroyedUi() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.close();
            source.release.countDown();
            project.drainImports();
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.assetsDir, "data.bin").toPath()));
        }
    }

    private static ManageFileBinding binding(ManageAssetsActivity activity) {
        try {
            Field field = ManageAssetsActivity.class.getDeclaredField("binding");
            field.setAccessible(true);
            return (ManageFileBinding) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static String currentPath(ManageAssetsActivity activity) {
        try {
            Field field = ManageAssetsActivity.class.getDeclaredField("current_path");
            field.setAccessible(true);
            return (String) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
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
        final String id = "__assets_import_" + UUID.randomUUID();
        final File root = new File(SketchwarePaths.getDataPath(id));
        final File assetsDir = new File(SketchwarePaths.getProjectAssetsPath(id));
        final Path sources = Files.createTempDirectory(context().getCacheDir().toPath(), "assets_import_");
        final List<GateFile> gates = new ArrayList<>();
        final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        final ActivityScenario<ManageAssetsActivity> scenario;
        ManageAssetsActivity activity;

        Project() throws Exception {
            assertFalse("Never overwrite a user project", root.exists());
            assertTrue(root.mkdirs());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) toasts.add(event.getText().toString());
            });
            scenario = ActivityScenario.launch(new Intent(context(), ManageAssetsActivity.class).putExtra("sc_id", id));
            scenario.onActivity(current -> {
                activity = current;
                current.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
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
                try {
                    Method show = ManageAssetsActivity.class.getDeclaredMethod("showImportDialog");
                    show.setAccessible(true); show.invoke(current);
                    current.getSupportFragmentManager().executePendingTransactions();
                    FilePickerDialogFragment picker = (FilePickerDialogFragment) current.getSupportFragmentManager().findFragmentByTag("filePicker");
                    assertNotNull(picker);
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
                    Field path = ManageAssetsActivity.class.getDeclaredField("current_path");
                    path.setAccessible(true); path.set(current, folder.getPath());
                    Method refresh = ManageAssetsActivity.class.getDeclaredMethod("refresh");
                    refresh.setAccessible(true); refresh.invoke(current);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void awaitDone(File output) throws Exception {
            waitUntil(() -> {
                boolean[] done = {false};
                scenario.onActivity(current -> done[0] = binding(current).importNewButton.isEnabled());
                return done[0] && output.exists();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void drainImports() throws Exception {
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(activity), "Assets-import-test-cleanup", done::countDown, null, null);
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
