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

@RunWith(AndroidJUnit4.class)
public class ManageJavaImportTest {
    @Test(timeout = 30000)
    public void readsRunOffMainWhileTheUiRemainsResponsive() throws Exception {
        try (Project project = new Project()) {
            GateFile java = project.gated("Imported.java", "package old.name;\nclass Imported {}\n");
            File existing = new File(project.javaDir, "Imported.java");
            Files.write(existing.toPath(), "previous source".getBytes(StandardCharsets.UTF_8));
            File kotlin = project.source("Other.kt", "package old.name\nclass Other\n");
            project.select(java, kotlin);
            assertTrue(java.entered.await(5, TimeUnit.SECONDS));
            assertEquals("previous source", read(existing));
            AtomicInteger pulse = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                assertFalse(activity.binding.importNewButton.isEnabled());
                activity.binding.filesListRecyclerView.post(pulse::incrementAndGet);
            });
            waitUntil(() -> pulse.get() > 0);
            java.release.countDown();
            project.awaitDone(new File(project.javaDir, "Other.kt"));
            assertEquals("package test.java.importer;\nclass Imported {}\n", read(new File(project.javaDir, "Imported.java")));
            assertEquals("package test.java.importer\nclass Other\n", read(new File(project.javaDir, "Other.kt")));
            project.scenario.onActivity(activity -> assertEquals(2, activity.binding.filesListRecyclerView.getAdapter().getItemCount()));
        }
    }

    @Test(timeout = 30000)
    public void failedReadsKeepExistingFilesAndDoNotStopTheRestOfTheBatch() throws Exception {
        try (Project project = new Project()) {
            File existing = new File(project.javaDir, "Missing.java");
            Files.write(existing.toPath(), "keep this file".getBytes(StandardCharsets.UTF_8));
            File missing = new File(project.sources.toFile(), "Missing.java");
            File valid = project.source("Other.kt", "package old\nclass Other\n");
            project.select(missing, valid);
            project.awaitDone(new File(project.javaDir, "Other.kt"));
            assertEquals("keep this file", read(existing));
            waitUntil(() -> project.toasts.stream().anyMatch(text -> text.contains("Missing.java")));
            assertEquals(2, project.javaDir.listFiles().length);
        }
    }

    @Test(timeout = 30000)
    public void recreationRetainsImportProgressAndTheDestinationFolder() throws Exception {
        try (Project project = new Project()) {
            File nested = new File(project.javaDir, "nested");
            assertTrue(nested.mkdir());
            project.enter(nested);
            GateFile source = project.gated("Imported.java", "package old;\nclass Imported {}\n");
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.recreate();
            project.scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertFalse(activity.binding.importNewButton.isEnabled());
                assertEquals(nested.getPath(), currentPath(activity));
            });
            source.release.countDown();
            project.awaitDone(new File(nested, "Imported.java"));
            project.scenario.onActivity(activity -> assertEquals(1, activity.binding.filesListRecyclerView.getAdapter().getItemCount()));
            assertEquals("package test.java.importer.nested;\nclass Imported {}\n", read(new File(nested, "Imported.java")));
        }
    }

    @Test(timeout = 30000)
    public void destinationAndPackageAreCapturedBeforeThePageChanges() throws Exception {
        try (Project project = new Project()) {
            File first = new File(project.javaDir, "first"), second = new File(project.javaDir, "second");
            assertTrue(first.mkdir()); assertTrue(second.mkdir());
            project.enter(first);
            GateFile source = project.gated("Imported.java", "package old;\nclass Imported {}\n");
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.enter(second);
            source.release.countDown();
            project.awaitDone(new File(first, "Imported.java"));
            assertFalse(new File(second, "Imported.java").exists());
            assertEquals("package test.java.importer.first;\nclass Imported {}\n", read(new File(first, "Imported.java")));
        }
    }

    @Test(timeout = 30000)
    public void finishingTheActivityDoesNotAbortOrRefreshItsDestroyedUi() throws Exception {
        try (Project project = new Project()) {
            GateFile source = project.gated("Imported.java", "package old;\nclass Imported {}\n");
            project.select(source);
            assertTrue(source.entered.await(5, TimeUnit.SECONDS));
            project.scenario.close();
            source.release.countDown();
            project.drainImports();
            assertEquals("package test.java.importer;\nclass Imported {}\n", read(new File(project.javaDir, "Imported.java")));
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String currentPath(ManageJavaActivity activity) {
        try {
            Field path = ManageJavaActivity.class.getDeclaredField("current_path");
            path.setAccessible(true);
            return (String) path.get(activity);
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
        final String id = "__java_import_" + UUID.randomUUID();
        final File root = new File(SketchwarePaths.getDataPath(id));
        final File javaDir = new File(SketchwarePaths.getProjectJavaPath(id));
        final Path sources = Files.createTempDirectory(context().getCacheDir().toPath(), "java_import_");
        final List<GateFile> gates = new ArrayList<>();
        final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        final ActivityScenario<ManageJavaActivity> scenario;
        ManageJavaActivity activity;

        Project() throws Exception {
            assertFalse("Never overwrite a user project", root.exists());
            assertTrue(root.mkdirs());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) toasts.add(event.getText().toString());
            });
            scenario = ActivityScenario.launch(new Intent(context(), ManageJavaActivity.class)
                    .putExtra("sc_id", id).putExtra("pkgName", "test.java.importer"));
            scenario.onActivity(current -> {
                activity = current;
                current.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        File source(String name, String content) throws Exception {
            File file = sources.resolve(name).toFile();
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return file;
        }

        GateFile gated(String name, String content) throws Exception {
            GateFile file = new GateFile(source(name, content).getPath());
            gates.add(file);
            return file;
        }

        void select(File... files) {
            scenario.onActivity(current -> {
                try {
                    Method show = ManageJavaActivity.class.getDeclaredMethod("showImportDialog");
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
                    Field path = ManageJavaActivity.class.getDeclaredField("current_path");
                    path.setAccessible(true); path.set(current, folder.getPath());
                    Method refresh = ManageJavaActivity.class.getDeclaredMethod("refresh");
                    refresh.setAccessible(true); refresh.invoke(current);
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void awaitDone(File output) throws Exception {
            waitUntil(() -> {
                boolean[] done = {false};
                scenario.onActivity(current -> done[0] = current.binding.importNewButton.isEnabled());
                return done[0] && output.isFile();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void drainImports() throws Exception {
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(activity), "Java-import-test-cleanup", done::countDown, null, null);
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

        @Override public String getAbsolutePath() {
            assertNotSame("Source I/O must leave the UI thread", Looper.getMainLooper(), Looper.myLooper());
            return super.getAbsolutePath();
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
