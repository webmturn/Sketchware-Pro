package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Bundle;
import android.system.Os;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.project.ProjectContentSnapshot;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.UIHelper;
import pro.sketchware.util.io.EncryptedFileUtil;
import pro.sketchware.util.io.SharedPrefsHelper;
import pro.sketchware.widgets.LoadingDialog;

/** Real editor return paths, with isolated projects and preferences; never changes P12. */
@RunWith(AndroidJUnit4.class)
public class ProjectExitChangesTest {
    private final ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
    private static final EncryptedFileUtil IO = new EncryptedFileUtil();

    @Before public void listenForToasts() throws Exception {
        // Toasts outlive their Activity; drain the preceding test's queued notifications.
        instrumentation().waitForIdleSync();
        Thread.sleep(4000);
        instrumentation().getUiAutomation().setOnAccessibilityEventListener(event -> {
            if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                toasts.add(event.getText().toString());
            }
        });
    }

    @After public void stopListening() {
        instrumentation().getUiAutomation().setOnAccessibilityEventListener(null);
    }

    @Test(timeout = 30000) public void unchangedWithAutomaticSaveDoesNotWriteOrToast() throws Exception {
        assertUnchangedExit(true, false, false);
    }

    @Test(timeout = 30000) public void unchangedWithoutAutomaticSaveDoesNotPrompt() throws Exception {
        assertUnchangedExit(false, false, false);
    }

    @Test(timeout = 30000) public void revertedEditDoesNotSave() throws Exception {
        assertUnchangedExit(true, true, false);
    }

    @Test(timeout = 30000) public void unchangedAfterRecreationDoesNotSave() throws Exception {
        assertUnchangedExit(true, false, true);
    }

    private void assertUnchangedExit(boolean automatic, boolean revert, boolean recreate) throws Exception {
        try (TestProject project = new TestProject()) {
            Map<Path, byte[]> before = project.fixture.snapshot();
            Map<Path, Long> times = modifiedTimes(before);
            project.launch(automatic);
            if (revert) {
                project.scenario.onActivity(activity -> {
                    var data = ProjectDataManager.getProjectDataManager(project.fixture.id);
                    data.addVariable(ProjectFileBean.DEFAULT_JAVA_NAME, 2, "temporary_edit");
                    data.removeVariable(ProjectFileBean.DEFAULT_JAVA_NAME, "temporary_edit");
                });
            }
            if (recreate) project.recreate(automatic);
            Thread.sleep(1100);
            toasts.clear();
            project.back();
            project.waitFinished();
            project.fixture.assertUnchanged(before);
            assertEquals(times, modifiedTimes(before));
            assertNoSaveToast();
        }
    }

    @Test(timeout = 30000) public void changedWithAutomaticSavePersistsAndCloses() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            toasts.clear();
            project.back();
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
            waitForToast(R.string.common_message_complete_save);
        }
    }

    @Test(timeout = 30000) public void changedWithoutAutomaticSaveCanCancelThenSave() throws Exception {
        try (TestProject project = new TestProject()) {
            Map<Path, byte[]> before = project.fixture.snapshot();
            project.launch(false);
            project.editVariable();
            project.back();
            waitForText(R.string.design_quit_message_confirm_save);
            clickText(R.string.common_word_cancel);
            project.assertOpen();
            project.fixture.assertUnchanged(before);
            project.back();
            waitForText(R.string.design_quit_message_confirm_save);
            clickText(R.string.design_quit_button_save_and_exit);
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
        }
    }

    @Test(timeout = 30000) public void discardChangedProjectLeavesCanonicalFilesUntouched() throws Exception {
        try (TestProject project = new TestProject()) {
            Map<Path, byte[]> before = project.fixture.snapshot();
            project.launch(false);
            project.editVariable();
            toasts.clear();
            project.back();
            waitForText(R.string.design_quit_message_confirm_save);
            clickText(R.string.common_word_exit);
            project.waitFinished();
            project.fixture.assertUnchanged(before);
            assertNoSaveToast();
        }
    }

    @Test(timeout = 30000) public void manualSaveResetsBaselineForExit() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            ProjectEditorPersistenceTest.clickSave();
            waitForToast(R.string.common_message_complete_save);
            project.waitReady();
            Map<Path, byte[]> before = project.fixture.snapshot();
            Map<Path, Long> times = modifiedTimes(before);
            Thread.sleep(1100);
            toasts.clear();
            project.back();
            project.waitFinished();
            project.fixture.assertUnchanged(before);
            assertEquals(times, modifiedTimes(before));
            assertNoSaveToast();
        }
    }

    @Test(timeout = 30000) public void editedProjectRemainsDirtyAfterRecreation() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            project.recreate(true);
            project.back();
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
        }
    }

    @Test(timeout = 30000) public void restoredBackupIsSavedOnExit() throws Exception {
        try (TestProject project = new TestProject()) {
            Files.write(new File(project.fixture.backup, "logic").toPath(),
                    IO.encryptString("@MainActivity.java_var\n2:restored_exit_edit\n", true));
            project.launch(true);
            waitForText(R.string.design_restore_data_message_confirm);
            clickText(R.string.common_word_restore);
            project.waitReady();
            project.back();
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("restored_exit_edit"));
        }
    }

    @Test(timeout = 30000) public void failedReadRecoveryIsNotMistakenForAnUnchangedProject() throws Exception {
        try (TestProject project = new TestProject()) {
            project.fixture.seed(true, true);
            project.launchWithoutWaiting(true);
            waitForText(R.string.project_load_failed_title);
            clickText(R.string.common_word_restore);
            project.waitReady();
            project.back();
            project.waitFinished();
            assertFalse(project.fixture.read(project.fixture.data, "logic").contains("invalid:broken"));
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("2:initial"));
        }
    }

    @Test(timeout = 30000) public void layoutFilesResourcesAndLibrariesAllTriggerChangeDetection() throws Exception {
        for (String module : new String[]{"view", "file", "resource", "library"}) {
            try (TestProject project = new TestProject()) {
                project.launch(false);
                project.scenario.onActivity(activity -> {
                    switch (module) {
                        case "view" -> ProjectDataManager.getProjectDataManager(project.fixture.id)
                                .getViewBean(ProjectFileBean.DEFAULT_XML_NAME, "status").text.text = "changed label";
                        case "file" -> ProjectDataManager.getFileManager(project.fixture.id).getActivities().get(0)
                                .keyboardSetting = ProjectFileBean.KEYBOARD_STATE_VISIBLE;
                        case "resource" -> ProjectDataManager.getResourceManager(project.fixture.id).fonts.add(
                                new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, "font_exit", "font_exit.ttf"));
                        case "library" -> ProjectDataManager.getLibraryManager(project.fixture.id).getCompat()
                                .configurations.put("material3", true);
                    }
                });
                project.back();
                waitForText(R.string.design_quit_message_confirm_save);
                clickText(R.string.common_word_exit);
                project.waitFinished();
            }
        }
    }

    @Test(timeout = 30000) public void sameNameResourceReplacementIsDetectedAndCanBeDiscarded() throws Exception {
        try (TestProject project = new TestProject()) {
            File font = project.seedFont();
            byte[] original = Files.readAllBytes(font.toPath());
            project.launch(false);
            String baseline = ProjectContentSnapshot.capture(project.fixture.id);
            project.scenario.onActivity(activity -> ProjectDataManager.getResourceManager(project.fixture.id).backupFonts());
            Files.write(font.toPath(), new byte[]{4, 3, 2, 1});
            assertNotEquals(baseline, ProjectContentSnapshot.capture(project.fixture.id));
            project.back();
            waitForText(R.string.design_quit_message_confirm_save);
            clickText(R.string.common_word_exit);
            project.waitFinished();
            assertArrayEquals(original, Files.readAllBytes(font.toPath()));
        }
    }

    @Test(timeout = 30000) public void identicalResourceCopyDoesNotCauseSaving() throws Exception {
        try (TestProject project = new TestProject()) {
            File font = project.seedFont();
            project.launch(true);
            byte[] original = Files.readAllBytes(font.toPath());
            project.scenario.onActivity(activity -> ProjectDataManager.getResourceManager(project.fixture.id).backupFonts());
            Files.write(font.toPath(), original);
            Map<Path, byte[]> before = project.fixture.snapshot();
            Map<Path, Long> times = modifiedTimes(before);
            Thread.sleep(1100);
            toasts.clear();
            project.back();
            project.waitFinished();
            project.fixture.assertUnchanged(before);
            assertEquals(times, modifiedTimes(before));
            assertNoSaveToast();
        }
    }

    @Test(timeout = 30000) public void saveFailureKeepsEditsAndAllowsRetry() throws Exception {
        assertSaveFailureAllowsRetry(false);
    }

    @Test(timeout = 30000) public void saveAndCloseFailureKeepsEditsAndAllowsRetry() throws Exception {
        assertSaveFailureAllowsRetry(true);
    }

    private void assertSaveFailureAllowsRetry(boolean closeAfterSave) throws Exception {
        try (TestProject project = new TestProject()) {
            Path files = new File(project.fixture.data, "file").toPath();
            byte[] original = Files.readAllBytes(files);
            project.launch(true);
            project.editVariable();
            Files.delete(files);
            Files.createDirectory(files);
            Files.write(files.resolve("block-replacement"), new byte[]{1});
            try {
                if (closeAfterSave) project.scenario.onActivity(activity -> controller(activity).saveAndClose());
                else ProjectEditorPersistenceTest.clickSave();
                waitForToast(R.string.common_message_save_failed);
                project.waitReady();
                project.assertOpen();
            } finally {
                Files.delete(files.resolve("block-replacement"));
                Files.delete(files);
                Files.write(files, original);
            }
            project.back();
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
        }
    }

    @Test(timeout = 30000) public void unreadableResourceDoesNotPreventUnsavedMetadataBackup() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            byte[] before = Files.readAllBytes(new File(project.fixture.data, "logic").toPath());
            Field controller = DesignActivity.class.getDeclaredField("projectController");
            controller.setAccessible(true);
            AtomicReference<DesignProjectController> active = new AtomicReference<>();
            project.scenario.onActivity(activity -> {
                try { active.set((DesignProjectController) controller.get(activity)); }
                catch (IllegalAccessException error) { throw new AssertionError(error); }
            });
            try (UnreadableResource ignored = new UnreadableResource(project)) {
                assertSnapshotFails(project);
                var saveBackup = DesignProjectController.class.getDeclaredMethod("saveUnsavedChangesInBackground");
                saveBackup.setAccessible(true);
                saveBackup.invoke(active.get());
                assertTrue(project.fixture.read(project.fixture.backup, "logic").contains("exit_edit"));
                assertArrayEquals(before, Files.readAllBytes(new File(project.fixture.data, "logic").toPath()));
            }
        }
    }

    @Test(timeout = 30000) public void saveAndCloseDoesNotSerializeANewBaseline() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            CountingImages images = countResourceSerializations(project);
            project.scenario.onActivity(activity -> controller(activity).saveAndClose());
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
            // The resource metadata is serialized once for the write, with no post-save snapshot.
            assertEquals(1, images.iterations.get());
        }
    }

    @Test(timeout = 30000) public void saveAndCloseDuringRecreationKeepsSavedEditsInNewEditor() throws Exception {
        try (TestProject project = new TestProject()) {
            project.launch(true);
            project.editVariable();
            BlockingImages images = new BlockingImages();
            project.scenario.onActivity(activity -> {
                ResourceManager resources = ProjectDataManager.getResourceManager(project.fixture.id);
                images.addAll(resources.images);
                resources.images = images;
                controller(activity).saveAndClose();
            });
            try {
                assertTrue("Save never reached resource serialization", images.entered.await(10, TimeUnit.SECONDS));
                project.recreate(true);
            } finally {
                images.release.countDown();
            }
            ProjectEditorPersistenceTest.waitUntil(() -> !ProjectDataManager.getResourceManager(project.fixture.id).hasLazyBackup());
            instrumentation().waitForIdleSync();
            project.assertOpen();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
            project.back();
            project.waitFinished();
            assertTrue(project.fixture.read(project.fixture.data, "logic").contains("exit_edit"));
        }
    }

    @Test(timeout = 30000) public void unknownBaselineRequiresConfirmationWithoutASnapshot() throws Exception {
        assertConservativeCloseSkipsSnapshot(false);
    }

    @Test(timeout = 30000) public void restoredBackupRequiresConfirmationWithoutASnapshot() throws Exception {
        assertConservativeCloseSkipsSnapshot(true);
    }

    private void assertConservativeCloseSkipsSnapshot(boolean restored) throws Exception {
        try (TestProject project = new TestProject()) {
            Map<Path, byte[]> before = project.fixture.snapshot();
            project.launch(false);
            CountingImages images = countResourceSerializations(project);
            project.scenario.onActivity(activity -> {
                DesignProjectController controller = controller(activity);
                if (restored) controller.onBackupRestored();
                else {
                    try {
                        Field baseline = DesignProjectController.class.getDeclaredField("savedContent");
                        baseline.setAccessible(true);
                        baseline.set(controller, null);
                    } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                }
            });
            project.back();
            waitForText(R.string.design_quit_message_confirm_save);
            assertEquals(0, images.iterations.get());
            project.assertOpen();
            project.fixture.assertUnchanged(before);
            clickText(R.string.common_word_exit);
            project.waitFinished();
        }
    }

    private static DesignProjectController controller(DesignActivity activity) {
        try {
            Field controller = DesignActivity.class.getDeclaredField("projectController");
            controller.setAccessible(true);
            return (DesignProjectController) controller.get(activity);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static CountingImages countResourceSerializations(TestProject project) {
        CountingImages result = new CountingImages();
        project.scenario.onActivity(activity -> {
            ResourceManager resources = ProjectDataManager.getResourceManager(project.fixture.id);
            result.addAll(resources.images);
            resources.images = result;
        });
        return result;
    }

    private static final class CountingImages extends ArrayList<ProjectResourceBean> {
        final AtomicInteger iterations = new AtomicInteger();
        @Override public Iterator<ProjectResourceBean> iterator() {
            iterations.incrementAndGet();
            return super.iterator();
        }
    }

    private static final class BlockingImages extends ArrayList<ProjectResourceBean> {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean first = new AtomicBoolean(true);
        @Override public Iterator<ProjectResourceBean> iterator() {
            if (first.compareAndSet(true, false)) {
                entered.countDown();
                try {
                    if (!release.await(15, TimeUnit.SECONDS)) throw new AssertionError("Blocked save was not released");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                }
            }
            return super.iterator();
        }
    }

    @Test(timeout = 30000) public void unreadableResourceDoesNotAllowSilentDiscard() throws Exception {
        try (TestProject project = new TestProject()) {
            Map<Path, byte[]> before = project.fixture.snapshot();
            project.launch(false);
            project.editVariable();
            try (UnreadableResource ignored = new UnreadableResource(project)) {
                assertSnapshotFails(project);
                project.back();
                waitForText(R.string.design_quit_message_confirm_save);
                project.assertOpen();
                project.fixture.assertUnchanged(before);
            }
            clickText(R.string.common_word_exit);
            project.waitFinished();
            project.fixture.assertUnchanged(before);
        }
    }

    private static void assertSnapshotFails(TestProject project) throws Exception {
        try {
            ProjectContentSnapshot.capture(project.fixture.id);
            fail("Unreadable resource was accepted");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static final class UnreadableResource implements AutoCloseable {
        final ResourceManager resources;
        final String originalPath;
        final File directory;
        final File font;

        UnreadableResource(TestProject project) throws Exception {
            resources = ProjectDataManager.getResourceManager(project.fixture.id);
            originalPath = resources.fontDirPath;
            directory = new File(project.context.getFilesDir(), "F11UnreadableResource." + project.fixture.id);
            font = new File(directory, "blocked.ttf");
            assertFalse(directory.toString(), directory.exists());
            assertTrue(directory.mkdir());
            try {
                Files.write(font.toPath(), new byte[]{1, 2, 3, 4});
                Os.chmod(font.getAbsolutePath(), 0);
                resources.fontDirPath = directory.getAbsolutePath();
            } catch (Exception error) {
                close();
                throw error;
            }
        }

        @Override public void close() throws Exception {
            resources.fontDirPath = originalPath;
            if (font.exists()) {
                Os.chmod(font.getAbsolutePath(), 0600);
                Files.delete(font.toPath());
            }
            Files.delete(directory.toPath());
        }
    }

    private static android.app.Instrumentation instrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    private static String text(int id) { return instrumentation().getTargetContext().getString(id); }

    private void waitForToast(int id) throws Exception {
        ProjectEditorPersistenceTest.waitUntil(() -> toasts.stream().anyMatch(value -> value.contains(text(id))));
    }

    private void assertNoSaveToast() throws Exception {
        Thread.sleep(1200);
        assertFalse(toasts.toString(), toasts.stream().anyMatch(value -> value.contains(text(R.string.common_message_complete_save))));
    }

    private static AccessibilityNodeInfo root() {
        return instrumentation().getUiAutomation().getRootInActiveWindow();
    }

    private static void waitForText(int id) throws Exception {
        ProjectEditorPersistenceTest.waitUntil(() -> {
            AccessibilityNodeInfo root = root();
            return root != null && !root.findAccessibilityNodeInfosByText(text(id)).isEmpty();
        });
    }

    private static void clickText(int id) throws Exception {
        Thread.sleep(600);
        ProjectEditorPersistenceTest.waitUntil(() -> {
            AccessibilityNodeInfo root = root();
            if (root == null) return false;
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text(id))) {
                if (text(id).contentEquals(node.getText()) && node.isClickable()
                        && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
            return false;
        });
    }

    private static Map<Path, Long> modifiedTimes(Map<Path, byte[]> files) throws Exception {
        Map<Path, Long> times = new HashMap<>();
        for (Path path : files.keySet()) times.put(path, Files.getLastModifiedTime(path).toMillis());
        return times;
    }

    private static final class TestProject implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        final Context context = instrumentation().getTargetContext();
        final String preferenceName = "F11ExitTest." + fixture.id;
        final File[] tempRoots = {new File(SketchwarePaths.getTempImagesPath(fixture.id)),
                new File(SketchwarePaths.getTempSoundsPath(fixture.id)), new File(SketchwarePaths.getTempFontsPath(fixture.id))};
        ActivityScenario<DesignActivity> scenario;

        TestProject() throws Exception {
            for (File path : tempRoots) assertFalse(path.toString(), path.exists());
            assertFalse(context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).contains("P12I2"));
            fixture.seed(false, false);
            Bundle status = new Bundle();
            status.putString("f11_owned_project", fixture.id);
            instrumentation().sendStatus(0, status);
        }

        File seedFont() throws Exception {
            File font = new File(fixture.roots[4], "same.ttf");
            Files.write(font.toPath(), new byte[]{1, 2, 3, 4});
            String resource = "@images\n@sounds\n@fonts\n" + pro.sketchware.core.project.ProjectDataStore.gson.toJson(
                    new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, "same", "same.ttf")) + "\n";
            Files.write(new File(fixture.data, "resource").toPath(), IO.encryptString(resource, true));
            return font;
        }

        void launch(boolean automatic) throws Exception { launchWithoutWaiting(automatic); waitReady(); }

        void launchWithoutWaiting(boolean automatic) throws Exception {
            scenario = fixture.launch();
            setAutomatic(automatic);
        }

        void setAutomatic(boolean automatic) throws Exception {
            context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().putBoolean("P12I2", automatic).commit();
            Field preference = DesignActivity.class.getDeclaredField("prefP12");
            preference.setAccessible(true);
            scenario.onActivity(activity -> {
                try { preference.set(activity, new SharedPrefsHelper(context, preferenceName)); }
                catch (IllegalAccessException error) { throw new AssertionError(error); }
            });
        }

        void recreate(boolean automatic) throws Exception { scenario.recreate(); setAutomatic(automatic); waitReady(); }

        void waitReady() throws Exception {
            ProjectEditorPersistenceTest.waitForEditor(scenario);
            Field loading = BaseAppCompatActivity.class.getDeclaredField("lottieDialog");
            loading.setAccessible(true);
            ProjectEditorPersistenceTest.waitUntil(() -> {
                AtomicBoolean ready = new AtomicBoolean();
                scenario.onActivity(activity -> {
                    try { ready.set(!((LoadingDialog) loading.get(activity)).isShowing()); }
                    catch (IllegalAccessException error) { throw new AssertionError(error); }
                });
                return ready.get();
            });
        }

        void editVariable() {
            scenario.onActivity(activity -> ProjectDataManager.getProjectDataManager(fixture.id)
                    .addVariable(ProjectFileBean.DEFAULT_JAVA_NAME, 2, "exit_edit"));
        }

        void back() {
            scenario.onActivity(activity -> { UIHelper.lastClickTime = 0; activity.getOnBackPressedDispatcher().onBackPressed(); });
        }

        void waitFinished() throws Exception {
            ProjectEditorPersistenceTest.waitUntil(() -> scenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED);
            instrumentation().waitForIdleSync();
        }

        void assertOpen() { assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, scenario.getState()); }

        @Override public void close() throws Exception {
            if (scenario != null) scenario.close();
            fixture.close();
            assertTrue(context.deleteSharedPreferences(preferenceName));
            for (File directory : tempRoots) {
                if (!directory.exists()) continue;
                assertEquals(fixture.id, directory.getName());
                try (Stream<Path> paths = Files.walk(directory.toPath())) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        assertTrue(path.startsWith(directory.toPath()));
                        Files.delete(path);
                    }
                }
            }
            Bundle status = new Bundle();
            status.putString("f11_cleaned_project", fixture.id);
            instrumentation().sendStatus(0, status);
        }
    }
}
