package pro.sketchware.activities.main.fragments.projects;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inspector.WindowInspector;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.core.settings.ApplicationSettings;
import pro.sketchware.util.Helper;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class BackupInspectionTest {
    @Test(timeout = 30000)
    public void inspectionLeavesMainResponsiveAndSnapshotsTheSelection() throws Exception {
        try (Scope scope = new Scope(true)) {
            GateFile gated = scope.gated("first.swb");
            File second = scope.archive("second.swb", false);
            ArrayList<File> selected = new ArrayList<>(List.of(gated, second));
            scope.select(selected);
            assertTrue(gated.entered.await(5, TimeUnit.SECONDS));
            selected.clear();
            AtomicInteger pulse = new AtomicInteger();
            scope.scenario.onActivity(activity -> activity.getWindow().getDecorView().post(pulse::incrementAndGet));
            waitUntil(() -> pulse.get() > 0);
            gated.release.countDown();
            scope.awaitInspection();
            waitUntil(() -> scope.manager.calls.size() == 2);
            assertEquals(List.of(new Call(gated.getPath(), false), new Call(second.getPath(), false)), new ArrayList<>(scope.manager.calls));
            assertEquals(scope.files.toFile().getAbsolutePath(), ApplicationSettings.getLastRestoreDirectory());
        }
    }

    @Test(timeout = 30000)
    public void zipDirectoryInspectionDoesNotInflateEarlierEntries() throws Exception {
        Path archive = Files.createTempFile(context().getCacheDir().toPath(), "zip_index_", ".swb");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                zip.putNextEntry(new ZipEntry("data/large"));
                zip.write(new byte[65536]);
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("local_libs/example/config"));
                zip.write(new byte[]{1});
                zip.closeEntry();
            }
            byte[] bytes = Files.readAllBytes(archive);
            int headerSize = 30 + ((bytes[26] & 255) | (bytes[27] & 255) << 8)
                    + ((bytes[28] & 255) | (bytes[29] & 255) << 8);
            bytes[headerSize] = 7; // Reserved DEFLATE block: inspecting names must not decode it.
            Files.write(archive, bytes);
            assertTrue(BackupFactory.zipContainsFile(archive.toString(), "local_libs"));
        } finally {
            assertEquals(context().getCacheDir().getCanonicalFile(), archive.toFile().getCanonicalFile().getParentFile());
            Files.delete(archive);
        }
    }

    @Test(timeout = 30000)
    public void libraryCopyDontCopyAndCancelKeepTheirOriginalMeanings() throws Exception {
        for (int button : new int[]{android.R.id.button1, android.R.id.button2, android.R.id.button3}) {
            try (Scope scope = new Scope(true)) {
                File archive = scope.archive("libraries.swb", true);
                scope.select(List.of(archive));
                scope.awaitInspection();
                String message = BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(false, 0, 1, "libraries");
                click(message, button);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertEquals(button == android.R.id.button3 ? List.of()
                        : List.of(new Call(archive.getPath(), button == android.R.id.button1)), new ArrayList<>(scope.manager.calls));
            }
        }
    }

    @Test(timeout = 30000)
    public void multipleLibraryPromptsKeepEachFilenameIndexAndChoice() throws Exception {
        try (Scope scope = new Scope(true)) {
            File first = scope.archive("first.swb", true), second = scope.archive("second.swb", true);
            scope.select(List.of(first, second));
            scope.awaitInspection();
            String secondMessage = BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(true, 1, 2, "second");
            click(secondMessage, android.R.id.button2);
            String firstMessage = BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(true, 0, 2, "first");
            click(firstMessage, android.R.id.button1);
            assertEquals(List.of(new Call(second.getPath(), false), new Call(first.getPath(), true)), new ArrayList<>(scope.manager.calls));
        }
    }

    @Test(timeout = 30000)
    public void unreadableArchivesAreReportedAndDoNotStartRestore() throws Exception {
        try (Scope scope = new Scope(true)) {
            File bad = scope.files.resolve("broken.swb").toFile();
            Files.write(bad.toPath(), new byte[]{1, 2, 3});
            File good = scope.archive("valid.swb", false);
            scope.select(List.of(bad, good));
            scope.awaitInspection();
            waitUntil(() -> scope.manager.calls.size() == 1);
            assertEquals(List.of(new Call(good.getPath(), false)), new ArrayList<>(scope.manager.calls));
            waitUntil(() -> scope.toasts.stream().anyMatch(text -> text.contains("broken.swb")));
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(bad.toPath()));
        }
    }

    @Test(timeout = 30000)
    public void stoppedPagesWaitForTheForegroundBeforeRestoring() throws Exception {
        try (Scope scope = new Scope(true)) {
            GateFile file = scope.gated("waiting.swb");
            scope.select(List.of(file));
            assertTrue(file.entered.await(5, TimeUnit.SECONDS));
            scope.scenario.moveToState(Lifecycle.State.CREATED);
            file.release.countDown();
            scope.awaitInspection();
            assertTrue(scope.manager.calls.isEmpty());
            scope.scenario.moveToState(Lifecycle.State.RESUMED);
            waitUntil(() -> scope.manager.calls.size() == 1);
            assertEquals(new Call(file.getPath(), false), scope.manager.calls.peek());
        }
    }

    @Test(timeout = 30000)
    public void destroyedFragmentViewsDoNotReceiveScanResults() throws Exception {
        try (Scope scope = new Scope(true)) {
            GateFile file = scope.gated("stale.swb");
            scope.select(List.of(file));
            assertTrue(file.entered.await(5, TimeUnit.SECONDS));
            scope.scenario.onActivity(activity -> activity.getSupportFragmentManager().beginTransaction().remove(scope.fragment).commitNow());
            file.release.countDown();
            scope.awaitInspection();
            assertTrue(scope.manager.calls.isEmpty());
            assertFalse(showing(Helper.getResString(R.string.common_message_loading)));
        }
    }

    @Test(timeout = 30000)
    public void finishingTheActivityDoesNotStartRestoreFromAnOldResult() throws Exception {
        try (Scope scope = new Scope(true)) {
            GateFile file = scope.gated("stale.swb");
            scope.select(List.of(file));
            assertTrue(file.entered.await(5, TimeUnit.SECONDS));
            scope.scenario.close();
            file.release.countDown();
            scope.awaitInspection();
            assertTrue(scope.manager.calls.isEmpty());
        }
    }

    @Test(timeout = 30000)
    public void externalOpenKeepsItsDefaultAndDoesNotChangeThePickerDirectory() throws Exception {
        try (Scope scope = new Scope(false)) {
            File file = scope.archive("external.swb", false);
            String prior = ApplicationSettings.getLastRestoreDirectory();
            scope.inspectExternal(file);
            scope.awaitInspection();
            waitUntil(() -> scope.manager.calls.size() == 1);
            assertEquals(List.of(new Call(file.getPath(), true)), new ArrayList<>(scope.manager.calls));
            assertEquals(prior, ApplicationSettings.getLastRestoreDirectory());
        }
    }

    @Test(timeout = 30000)
    public void recreatingTheActivityDropsItsOldExternalOpenResult() throws Exception {
        try (Scope scope = new Scope(false)) {
            GateFile file = scope.gated("external.swb");
            scope.inspectExternal(file);
            assertTrue(file.entered.await(5, TimeUnit.SECONDS));
            scope.scenario.recreate();
            scope.scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            file.release.countDown();
            scope.awaitInspection();
            assertTrue(scope.manager.calls.isEmpty());
        }
    }

    private static void click(String message, int button) throws Exception {
        waitUntil(() -> showing(message));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View root = rootWithMessage(message);
            assertNotNull(root);
            assertTrue(root.findViewById(button).performClick());
        });
    }

    private static boolean showing(String message) {
        boolean[] result = {false};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> result[0] = rootWithMessage(message) != null);
        return result[0];
    }

    private static View rootWithMessage(String message) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            TextView text = root.findViewById(android.R.id.message);
            if (root.isShown() && text != null && text.getText().toString().equals(message)) return root;
        }
        return null;
    }

    private interface Condition { boolean ready() throws Exception; }
    private static void waitUntil(Condition condition) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.ready()) return;
            Thread.sleep(30);
        }
        fail("Backup inspection did not complete");
    }

    private record Call(String file, boolean libraries) {}
    private static class RecordingManager extends BackupRestoreManager {
        final ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<>();
        RecordingManager(Activity activity, ProjectsFragment fragment) { super(activity, fragment); }
        @Override public void doRestore(String file, boolean libraries) {
            assertSame("Restore UI must run on main", Looper.getMainLooper(), Looper.myLooper());
            calls.add(new Call(file, libraries));
        }
    }

    /** Keeps the real picker and view lifecycle, without opening or modifying user projects. */
    public static class TestProjectsFragment extends ProjectsFragment {
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            return new FrameLayout(requireContext());
        }
        @Override public void onViewCreated(View view, Bundle state) {
            try {
                Field executor = ProjectsFragment.class.getDeclaredField("executorService");
                executor.setAccessible(true);
                executor.set(this, Executors.newSingleThreadExecutor());
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
    }

    private static class Scope implements AutoCloseable {
        final Path files = Files.createTempDirectory(context().getCacheDir().toPath(), "backup_scan_");
        final String priorDirectory = ApplicationSettings.getLastRestoreDirectory();
        final ActivityScenario<PermissionResultTestActivity> scenario;
        final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        final List<GateFile> gates = new ArrayList<>();
        RecordingManager manager;
        TestProjectsFragment fragment;
        LiveData<?> results;

        Scope(boolean picker) throws Exception {
            ApplicationSettings.setLastRestoreDirectory(files.toString());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
                if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) toasts.add(event.getText().toString());
            });
            scenario = ActivityScenario.launch(PermissionResultTestActivity.class);
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (picker) {
                    fragment = new TestProjectsFragment();
                    activity.getSupportFragmentManager().beginTransaction().add(fragment, "backup_test").commitNow();
                }
                manager = new RecordingManager(activity, fragment);
            });
        }

        File archive(String name, boolean libraries) throws Exception {
            Path archive = files.resolve(name);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                zip.putNextEntry(new ZipEntry(libraries ? "local_libs/example/config" : "data/project"));
                zip.write("fixture".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            return archive.toFile();
        }

        GateFile gated(String name) throws Exception {
            GateFile gate = new GateFile(archive(name, false).getPath());
            gates.add(gate);
            return gate;
        }

        void select(List<File> selected) {
            scenario.onActivity(activity -> {
                manager.restore();
                fragment.getChildFragmentManager().executePendingTransactions();
                FilePickerDialogFragment picker = (FilePickerDialogFragment) fragment.getChildFragmentManager().findFragmentByTag("file_picker");
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
                    assertNotNull(callback);
                    picker.dismissNow();
                    callback.onFilesSelected(selected);
                    captureResults();
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void inspectExternal(File file) {
            scenario.onActivity(activity -> {
                try {
                    Method inspect = BackupRestoreManager.class.getMethod("inspectAndRestore", List.class, boolean.class);
                    inspect.invoke(manager, List.of(file), true);
                    captureResults();
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
        }

        void captureResults() throws ReflectiveOperationException {
            Field field = BackupRestoreManager.class.getDeclaredField("inspectionResults");
            field.setAccessible(true);
            results = (LiveData<?>) field.get(manager);
            assertNotNull(results);
        }

        void awaitInspection() throws Exception {
            waitUntil(() -> results.getValue() != null);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        @Override public void close() throws Exception {
            for (GateFile gate : gates) gate.release.countDown();
            try {
                scenario.close();
                if (results != null) awaitInspection();
            } finally {
                InstrumentationRegistry.getInstrumentation().getUiAutomation().setOnAccessibilityEventListener(null);
                ApplicationSettings.setLastRestoreDirectory(priorDirectory);
                assertEquals(context().getCacheDir().getCanonicalFile(), files.toFile().getCanonicalFile().getParentFile());
                try (Stream<Path> paths = Files.walk(files)) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        assertTrue(path.startsWith(files)); Files.delete(path);
                    }
                }
            }
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static class GateFile extends File {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        GateFile(String path) { super(path); }
        @Override public String getAbsolutePath() {
            assertNotSame("ZIP inspection must leave main", Looper.getMainLooper(), Looper.myLooper());
            entered.countDown();
            try { if (!release.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("Inspection gate timed out"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return super.getAbsolutePath();
        }
    }
}
