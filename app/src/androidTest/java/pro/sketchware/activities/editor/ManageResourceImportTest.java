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
import pro.sketchware.R;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ManageFileBinding;

@RunWith(AndroidJUnit4.class)
public class ManageResourceImportTest {
    @Test(timeout = 30000)
    public void copyingRunsOffMainWhileTheUiRemainsResponsive() throws Exception {
        try (Project project = new Project()) {
            byte[] content = new byte[1024 * 1024 + 3];
            for (int i = 0; i < content.length; i++) content[i] = (byte) i;
            GateFile source = project.gated("data.bin", content);
            File target = new File(project.typeDir, "data.bin");
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
            project.awaitIdle();
            assertArrayEquals(content, Files.readAllBytes(target.toPath()));
            assertEquals(1, project.typeDir.listFiles().length);
            project.scenario.onActivity(activity -> assertEquals(1, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void importingExistingResourcesInPlaceDoesNotEraseTheirContents() throws Exception {
        try (Project project = new Project()) {
            File file = new File(project.typeDir, "keep.bin");
            Files.write(file.toPath(), new byte[]{0, 1, -1});
            File nested = new File(project.typeDir, "nested");
            assertTrue(nested.mkdir());
            Path xml = nested.toPath().resolve("keep.xml");
            byte[] content = "<?xml version=\"1.0\" encoding=\"utf-8\"?><data>保留</data>".getBytes(StandardCharsets.UTF_8);
            Files.write(xml, content);
            project.select(file, nested);
            project.awaitIdle();
            assertArrayEquals(new byte[]{0, 1, -1}, Files.readAllBytes(file.toPath()));
            assertArrayEquals(content, Files.readAllBytes(xml));
        }
    }

    @Test(timeout = 30000)
    public void directoryMergesPreserveXmlBytesEmptyFoldersAndOtherChildren() throws Exception {
        try (Project project = new Project()) {
            Path source = Files.createDirectory(project.sources.resolve("bundle"));
            Files.createDirectories(source.resolve("nested/empty"));
            byte[] content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<data>中文 &amp; text</data>\r\n".getBytes(StandardCharsets.UTF_8);
            Files.write(source.resolve("nested/data.xml"), content);
            Path target = project.typeDir.toPath().resolve("bundle");
            Files.createDirectories(target.resolve("nested"));
            Files.write(target.resolve("nested/data.xml"), new byte[]{42});
            Files.write(target.resolve("keep.bin"), new byte[]{7});
            project.select(source.toFile());
            project.awaitIdle();
            assertArrayEquals(content, Files.readAllBytes(target.resolve("nested/data.xml")));
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(target.resolve("keep.bin")));
            assertTrue(Files.isDirectory(target.resolve("nested/empty")));
        }
    }

    @Test(timeout = 30000)
    public void failedResourcesAreReportedWhileOtherEntriesContinueAndRetryWorks() throws Exception {
        try (Project project = new Project()) {
            File existing = new File(project.typeDir, "missing.bin");
            Files.write(existing.toPath(), new byte[]{42});
            File missing = project.sources.resolve("missing.bin").toFile();
            File valid = project.source("other.bin", new byte[]{0, -1});
            project.select(missing, valid);
            project.awaitIdle();
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(existing.toPath()));
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.typeDir, "other.bin").toPath()));
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("missing.bin")));
            assertEquals(2, project.typeDir.listFiles().length);

            Files.write(missing.toPath(), new byte[]{7});
            project.select(missing);
            project.awaitIdle();
            assertArrayEquals(new byte[]{7}, Files.readAllBytes(existing.toPath()));
        }
    }

    @Test(timeout = 30000)
    public void emptySelectionsKeepTheirWarningAndDoNotStartImport() throws Exception {
        try (Project project = new Project()) {
            project.select();
            project.awaitIdle();
            String warning = context().getString(R.string.error_no_files_selected);
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains(warning)));
            assertEquals(0, project.typeDir.listFiles().length);
        }
    }

    @Test(timeout = 30000)
    public void recreationRetainsProgressAndTheSelectedResourceDirectory() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.recreate();
            project.scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertFalse(binding(activity).importNewButton.isEnabled());
                assertEquals(project.typeDir.getPath(), currentPath(activity));
                assertEquals(activity.getString(R.string.resource_create_or_import), binding(activity).showOptionsButton.getText().toString());
            });
            source.release.countDown();
            project.awaitIdle();
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.typeDir, "data.bin").toPath()));
            project.scenario.onActivity(activity -> assertEquals(1, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void destinationIsCapturedBeforeTheResourcePageChanges() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            File otherType = new File(project.resourceDir, "values");
            project.enter(otherType);
            source.release.countDown();
            project.awaitIdle();
            assertFalse(new File(otherType, "data.bin").exists());
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.typeDir, "data.bin").toPath()));
            project.scenario.onActivity(activity -> assertEquals(0, binding(activity).filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void removingTheDestinationDoesNotRecreateItOrLeaveImportBusy() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("data.bin", new byte[]{0, -1});
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            Files.delete(project.typeDir.toPath());
            source.release.countDown();
            project.awaitIdle();
            assertFalse(project.typeDir.exists());
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("data.bin")));
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
            assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(new File(project.typeDir, "data.bin").toPath()));
        }
    }

    private static ManageFileBinding binding(ManageResourceActivity activity) {
        try {
            Field field = ManageResourceActivity.class.getDeclaredField("binding");
            field.setAccessible(true);
            return (ManageFileBinding) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static String currentPath(ManageResourceActivity activity) {
        try {
            Field field = ManageResourceActivity.class.getDeclaredField("temp");
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
        final String id = "__res_import_" + UUID.randomUUID();
        final File root = new File(SketchwarePaths.getDataPath(id));
        final File resourceDir = new File(SketchwarePaths.getProjectResourcePath(id));
        final File typeDir = new File(resourceDir, "raw");
        final Path sources = Files.createTempDirectory(context().getCacheDir().toPath(), "res_import_");
        final List<GateFile> gates = new ArrayList<>();
        final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        final ActivityScenario<ManageResourceActivity> scenario;
        ManageResourceActivity activity;

        Project() throws Exception {
            assertFalse("Never overwrite a user project", root.exists());
            assertTrue(root.mkdirs());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) toasts.add(event.getText().toString());
            });
            scenario = ActivityScenario.launch(new Intent(context(), ManageResourceActivity.class).putExtra("sc_id", id));
            scenario.onActivity(current -> {
                activity = current;
                current.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
            assertTrue(new File(resourceDir, "values").isDirectory());
            assertTrue(typeDir.mkdir());
            enter(typeDir);
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
                binding(current).importNewButton.performClick();
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
                    Field path = ManageResourceActivity.class.getDeclaredField("temp");
                    path.setAccessible(true); path.set(current, folder.getPath());
                    Method refresh = ManageResourceActivity.class.getDeclaredMethod("handleAdapter", String.class);
                    refresh.setAccessible(true); refresh.invoke(current, folder.getPath());
                    Method fab = ManageResourceActivity.class.getDeclaredMethod("handleFab");
                    fab.setAccessible(true); fab.invoke(current);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void awaitIdle() throws Exception {
            waitUntil(() -> {
                boolean[] done = {false};
                scenario.onActivity(current -> done[0] = binding(current).importNewButton.isEnabled());
                return done[0];
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void drainImports() throws Exception {
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(activity), "Resource-import-test-cleanup", done::countDown, null, null);
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
