package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.Environment;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.io.EncryptedFileUtil;

/** Exercises editor controls using temporary projects with their own metadata and build directories. */
@RunWith(AndroidJUnit4.class)
public class ProjectEditorPersistenceTest {
    @Test(timeout = 30000)
    public void toolbarSavePersistsHealthyData() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.seed(false, false);
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                waitForEditor(scenario);
                scenario.onActivity(activity -> ProjectDataManager.getProjectDataManager(fixture.id)
                        .addVariable(ProjectFileBean.DEFAULT_JAVA_NAME, 2, "saved_from_editor"));
                clickSave();
                waitUntil(() -> fixture.read(fixture.data, "logic").contains("saved_from_editor"));
                waitUntil(() -> ProjectListManager.getProjectById(fixture.id).containsKey("sketchware_ver"));
                ProjectDataManager.reloadProjectFromData(fixture.id);
                assertTrue(ProjectDataManager.canSaveProject(fixture.id));
                assertEquals("saved_from_editor", ProjectDataManager.getProjectDataManager(fixture.id)
                        .getVariables(ProjectFileBean.DEFAULT_JAVA_NAME).get(1).second);
            }
        }
    }

    @Test(timeout = 30000)
    public void normalRestoreRefreshesExistingGeneratorAndSavePersistsLibrary() throws Exception {
        verifyRestore(false);
    }

    @Test(timeout = 30000)
    public void failedLoadCanRestoreFromDialogAndSaveHealthyData() throws Exception {
        verifyRestore(true);
    }

    private static void verifyRestore(boolean damaged) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.seed(true, damaged);
            Map<Path, byte[]> before = fixture.snapshot();
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                restore(scenario, damaged);
                fixture.assertUnchanged(before);
                assertTrue(ProjectDataManager.canSaveProject(fixture.id));
                assertTrue(ProjectDataManager.getLibraryManager(fixture.id).getCompat().configurations
                        .get("material3").equals(true));
                scenario.onActivity(activity -> assertTrue(activity.getProjectFilePaths().getXMLStyle()
                        .contains("Theme.Material3.DayNight.NoActionBar")));
                clickSave();
                waitUntil(() -> !new File(fixture.backup, "library").exists()
                        && ProjectListManager.getProjectById(fixture.id).containsKey("sketchware_ver"));
                ProjectDataManager.reloadProjectFromData(fixture.id);
                assertTrue(ProjectDataManager.canSaveProject(fixture.id));
                assertTrue(ProjectDataManager.getLibraryManager(fixture.id).getCompat().configurations
                        .get("material3").equals(true));
                assertTrue(fixture.read(fixture.data, "logic").contains("initial"));
            }
        }
    }

    static void restore(ActivityScenario<DesignActivity> scenario, boolean damaged) throws Exception {
        String title = string(damaged ? R.string.project_load_failed_title : R.string.design_restore_data_title);
        waitUntil(() -> hasText(title));
        if (!damaged) {
            scenario.onActivity(activity -> assertFalse(activity.getProjectFilePaths().getXMLStyle()
                    .contains("Theme.Material3.DayNight.NoActionBar")));
        }
        // Let the app's shared click throttle expire between independently launched scenarios.
        Thread.sleep(600);
        String restore = string(R.string.common_word_restore);
        waitUntil(() -> clickText(restore));
        waitUntil(() -> !hasText(title));
        waitForEditor(scenario);
    }

    static void waitForEditor(ActivityScenario<DesignActivity> scenario) throws Exception {
        waitUntil(() -> {
            AtomicBoolean ready = new AtomicBoolean();
            scenario.onActivity(activity -> ready.set(activity.getProjectFilePaths() != null
                    && ProjectFileBean.DEFAULT_XML_NAME.contentEquals(
                    ((android.widget.TextView) activity.findViewById(R.id.file_name)).getText())));
            return ready.get();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    static void clickSave() throws Exception {
        waitUntil(() -> {
            AccessibilityNodeInfo root = root();
            if (root == null) return false;
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId(
                    "pro.sketchware:id/design_option_menu_title_save_project")) {
                if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
            return false;
        });
    }

    private static String string(int id) {
        return InstrumentationRegistry.getInstrumentation().getTargetContext().getString(id);
    }

    private static AccessibilityNodeInfo root() {
        return InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
    }

    private static boolean hasText(String text) {
        AccessibilityNodeInfo root = root();
        return root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty();
    }

    private static boolean clickText(String text) {
        AccessibilityNodeInfo root = root();
        if (root == null) return false;
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
            if (text.contentEquals(node.getText()) && node.isClickable()
                    && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }
        return false;
    }

    interface Condition { boolean satisfied() throws Exception; }

    static void waitUntil(Condition condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.satisfied()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Editor operation did not complete");
    }

    static final class Fixture implements AutoCloseable {
        private static final EncryptedFileUtil IO = new EncryptedFileUtil();
        final String id;
        final File data;
        final File backup;
        final File[] roots;
        private final ArrayList<File> createdParents = new ArrayList<>();

        Fixture() throws Exception {
            int candidate = Integer.parseInt(ProjectListManager.getNextProjectId());
            File[] candidates;
            while (true) {
                candidates = rootsFor(Integer.toString(candidate));
                boolean exists = false;
                for (File root : candidates) exists |= root.exists();
                if (!exists) break;
                candidate++;
            }
            id = Integer.toString(candidate);
            roots = candidates;
            data = roots[0];
            backup = roots[1];
            for (File root : roots) {
                assertEquals(id, root.getName());
                assertTrue(root.getCanonicalPath().startsWith(
                        Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator));
                assertFalse(root.exists());
                createParents(root.getParentFile());
                assertTrue(root.mkdir());
            }
        }

        private static File[] rootsFor(String id) {
            return new File[]{new File(SketchwarePaths.getDataPath(id)),
                    new File(SketchwarePaths.getBackupPath(id)),
                    new File(SketchwarePaths.getImagesPath(), id),
                    new File(SketchwarePaths.getSoundsPath(), id),
                    new File(SketchwarePaths.getFontsResourcePath(), id),
                    new File(SketchwarePaths.getIconsPath(), id),
                    new File(SketchwarePaths.getProjectListPath(id)),
                    new File(SketchwarePaths.getMyscPath(id))};
        }

        private void createParents(File directory) {
            if (directory.isDirectory()) return;
            createParents(directory.getParentFile());
            assertTrue(directory.mkdir());
            createdParents.add(directory);
        }

        void seed(boolean withBackup, boolean damaged) throws Exception {
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", id);
            metadata.put("my_sc_pkg_name", "test.f01.editor");
            metadata.put("my_ws_name", "F01EditorTest");
            metadata.put("my_app_name", "F01EditorTest");
            metadata.put("sc_ver_code", "1");
            metadata.put("sc_ver_name", "1.0");
            metadata.put("proj_type", 1);
            ProjectListManager.saveProject(id, metadata);
            assertNotNull(ProjectListManager.getProjectById(id));
            ViewBean label = new ViewBean("status", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
            label.parent = "root";
            String[] names = {"logic", "view", "file", "resource", "library"};
            String[] content = {"@MainActivity.java_var\n2:initial\n",
                    "@main.xml\n" + ProjectDataStore.gson.toJson(label) + "\n",
                    "@activity\n" + ProjectDataStore.gson.toJson(new ProjectFileBean(
                            ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main")) + "\n@customview\n",
                    "@images\n@sounds\n@fonts\n", library(false)};
            for (int i = 0; i < names.length; i++) {
                write(data, names[i], content[i]);
                if (withBackup && (damaged || names[i].equals("library")))
                    write(backup, names[i], names[i].equals("library") ? library(true) : content[i]);
            }
            if (damaged) write(data, "logic", "@MainActivity.java_var\n2:initial\ninvalid:broken\n");
        }

        private static String library(boolean material3) {
            ProjectLibraryBean compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
            compat.useYn = "Y";
            compat.configurations.put("material3", material3);
            compat.configurations.put("theme", "DayNight");
            return "@compat\n" + ProjectDataStore.gson.toJson(compat) + "\n";
        }

        private void write(File dir, String name, String content) throws Exception {
            Files.write(new File(dir, name).toPath(), IO.encryptString(content, true));
        }

        String read(File dir, String name) throws Exception {
            return IO.decryptToString(Files.readAllBytes(new File(dir, name).toPath()));
        }

        ActivityScenario<DesignActivity> launch() {
            ProjectDataManager.clearAll();
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), DesignActivity.class);
            intent.putExtra("sc_id", id);
            return ActivityScenario.launch(intent);
        }

        Map<Path, byte[]> snapshot() throws Exception {
            Map<Path, byte[]> result = new HashMap<>();
            for (File root : roots) {
                try (Stream<Path> paths = Files.walk(root.toPath())) {
                    for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator)
                        result.put(path, Files.readAllBytes(path));
                }
            }
            return result;
        }

        void assertUnchanged(Map<Path, byte[]> before) throws Exception {
            Map<Path, byte[]> after = snapshot();
            assertEquals(before.keySet(), after.keySet());
            for (Path path : before.keySet()) assertArrayEquals(path.toString(), before.get(path), after.get(path));
        }

        @Override public void close() throws Exception {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            ProjectDataManager.clearAll();
            for (File root : roots) {
                assertEquals(id, root.getName());
                Path ownedRoot = root.toPath();
                try (Stream<Path> paths = Files.walk(ownedRoot)) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        assertTrue(path.startsWith(ownedRoot));
                        Files.delete(path);
                    }
                }
                assertFalse(root.exists());
            }
            for (int i = createdParents.size() - 1; i >= 0; i--) createdParents.get(i).delete();
        }
    }
}
