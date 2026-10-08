package pro.sketchware.core.project;

import static org.junit.Assert.*;

import android.os.Environment;
import android.content.Intent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.R;
import pro.sketchware.activities.design.DesignActivity;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.EventBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.settings.ApplicationSettings;
import pro.sketchware.util.io.EncryptedFileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/** Uses isolated, unlisted projects and the editor's real save/backup entry points. */
@RunWith(AndroidJUnit4.class)
public class ProjectPersistenceTest {
    private static final String[] METADATA = {"logic", "view", "library", "resource", "file"};
    private static final String BROKEN = "{\"id\":\"broken\",\"type\":[}";
    private static final EncryptedFileUtil IO = new EncryptedFileUtil();

    @Test(timeout = 30000)
    public void damagedFilesBlockSaveBackupCloseAndCleanup() throws Exception {
        boolean encryption = ApplicationSettings.isSettingEnabled(ApplicationSettings.SETTING_PROJECT_DATA_ENCRYPTION);
        for (boolean encrypted : new boolean[]{false, true}) {
            for (String module : METADATA) {
                try (Fixture fixture = new Fixture()) {
                    fixture.seed(encrypted);
                    fixture.write(fixture.data, module, damaged(module), encrypted);
                    Map<Path, byte[]> before = fixture.snapshot();
                    fixture.load();
                    assertFalse(module, ProjectDataManager.canSaveProject(fixture.id));
                    assertEquals(Boolean.FALSE, save(fixture.id));
                    invokeAutomaticBackup(fixture.id);
                    assertFalse(ProjectDataManager.closeDataManager());
                    assertFalse(ProjectDataManager.closeFileManager());
                    assertFalse(ProjectDataManager.closeLibraryManager());
                    assertFalse(ProjectDataManager.closeResourceManager());
                    for (String affected : METADATA) assertDirectWritesBlocked(fixture, affected);
                    ResourceManager resources = ProjectDataManager.getResourceManager(fixture.id);
                    resources.cleanupAllResources();
                    resources.ensureBackedUp();
                    assertFalse(resources.hasLazyBackup());
                    fixture.assertUnchanged(before);
                }
            }
        }
        assertEquals(encryption, ApplicationSettings.isSettingEnabled(ApplicationSettings.SETTING_PROJECT_DATA_ENCRYPTION));
    }

    @Test(timeout = 30000)
    public void healthyPlainAndEncryptedProjectsRoundTrip() throws Exception {
        for (boolean encrypted : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture()) {
                fixture.seed(encrypted);
                fixture.load();
                assertTrue(ProjectDataManager.canSaveProject(fixture.id));
                assertTrue(ProjectDataManager.closeDataManager());
                assertTrue(ProjectDataManager.closeFileManager());
                assertTrue(ProjectDataManager.closeLibraryManager());
                assertTrue(ProjectDataManager.closeResourceManager());
                fixture.load();
                assertHealthy(fixture.id);
                assertEquals(Boolean.TRUE, save(fixture.id));
                for (String name : METADATA) assertFalse(new File(fixture.backup, name).exists());
                assertTrue(new File(fixture.images, "image_after.png").exists());
                assertFalse(new File(fixture.images, "orphan.png").exists());
                fixture.load();
                assertHealthy(fixture.id);
            }
        }
    }

    @Test(timeout = 30000)
    public void unknownSectionsCannotEnableSaveOrDeleteResources() throws Exception {
        for (boolean encrypted : new boolean[]{false, true}) {
            for (String module : new String[]{"library", "resource", "file"}) {
                try (Fixture fixture = new Fixture()) {
                    fixture.seed(encrypted);
                    String content = healthy(module);
                    if (module.equals("library")) content = content.replace("@compat", "@unknown_library");
                    if (module.equals("resource")) content = content.replace("@images", "@unknown_resource");
                    if (module.equals("file")) content = content.replace("@activity", "@unknown_file");
                    fixture.write(fixture.data, module, content, encrypted);
                    Map<Path, byte[]> before = fixture.snapshot();
                    fixture.load();
                    assertFalse(ProjectDataManager.canSaveProject(fixture.id));
                    assertEquals(Boolean.FALSE, save(fixture.id));
                    invokeAutomaticBackup(fixture.id);
                    assertFalse(ProjectDataManager.closeDataManager());
                    assertFalse(ProjectDataManager.closeLibraryManager());
                    assertFalse(ProjectDataManager.closeResourceManager());
                    assertFalse(ProjectDataManager.closeFileManager());
                    for (String affected : METADATA) assertDirectWritesBlocked(fixture, affected);
                    ProjectDataManager.getResourceManager(fixture.id).cleanupAllResources();
                    fixture.assertUnchanged(before);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void restoredLibrarySettingsRefreshTheExistingCodeGenerator() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.seed(true);
            fixture.load();
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", fixture.id);
            metadata.put("my_sc_pkg_name", "test.f01.persistence");
            metadata.put("my_ws_name", "F01Test");
            metadata.put("my_app_name", "F01Test");
            ProjectFilePaths paths = new ProjectFilePaths(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    SketchwarePaths.getMyscPath(fixture.id), metadata);
            assertFalse(paths.getXMLStyle().contains("Theme.Material3.DayNight.NoActionBar"));
            ProjectLibraryBean compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
            compat.useYn = "Y";
            compat.configurations.put("material3", true);
            compat.configurations.put("theme", "DayNight");
            fixture.write(fixture.backup, "library", "@compat\n" + ProjectDataStore.gson.toJson(compat) + "\n", true);
            Map<Path, byte[]> before = fixture.snapshot();
            ProjectDataManager.getLibraryManager(fixture.id).loadFromBackup();
            paths.refreshLibrarySettings();
            assertTrue(paths.getXMLStyle().contains("Theme.Material3.DayNight.NoActionBar"));
            ProjectDataManager.getLibraryManager(fixture.id).loadFromData();
            paths.refreshLibrarySettings();
            assertFalse(paths.getXMLStyle().contains("Theme.Material3.DayNight.NoActionBar"));
            fixture.assertUnchanged(before);
        }
    }

    @Test(timeout = 30000)
    public void successfulBackupReadAllowsSavingAgain() throws Exception {
        for (boolean encrypted : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture()) {
                fixture.seed(encrypted);
                for (String name : METADATA) fixture.write(fixture.data, name, damaged(name), encrypted);
                Map<Path, byte[]> before = fixture.snapshot();
                fixture.load();
                assertFalse(ProjectDataManager.canSaveProject(fixture.id));
                assertTrue(ProjectDataManager.hasProjectBackup(fixture.id));
                assertTrue(ProjectDataManager.loadProjectBackup(fixture.id));
                assertHealthy(fixture.id);
                fixture.assertUnchanged(before); // Opening a backup never writes to disk.
                assertEquals(Boolean.TRUE, save(fixture.id));
                fixture.load();
                assertHealthy(fixture.id);
            }
        }
    }

    @Test(timeout = 30000)
    public void aDamagedBackupCannotEnableSaving() throws Exception {
        for (boolean encrypted : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture()) {
                fixture.seed(encrypted);
                fixture.write(fixture.data, "logic", damaged("logic"), encrypted);
                fixture.write(fixture.backup, "logic", damaged("logic") + "\n", encrypted);
                Map<Path, byte[]> before = fixture.snapshot();
                fixture.load();
                assertFalse(ProjectDataManager.loadProjectBackup(fixture.id));
                assertEquals(Boolean.FALSE, save(fixture.id));
                invokeAutomaticBackup(fixture.id);
                fixture.assertUnchanged(before);
            }
        }
    }

    @Test(timeout = 30000)
    public void anEagerBackupLoadCannotHideDamagedCanonicalFiles() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.seed(true);
            fixture.write(fixture.data, "library", damaged("library"), true);
            ProjectDataManager.clearAll();
            assertTrue(ProjectDataManager.getLibraryManager(fixture.id).canSave());
            assertTrue(ProjectDataManager.canSaveProject(fixture.id));
            ProjectDataManager.reloadProjectFromData(fixture.id);
            assertFalse(ProjectDataManager.canSaveProject(fixture.id));
            assertEquals(Boolean.FALSE, save(fixture.id));
        }
    }

    @Test(timeout = 30000)
    public void editorShowsLoadFailureAndExitPreservesFiles() throws Exception {
        for (String module : new String[]{"logic", "library"}) {
            try (Fixture fixture = new Fixture()) {
                fixture.seed(true);
                fixture.write(fixture.data, module, damaged(module), true);
                ProjectDataManager.clearAll();
                Map<Path, byte[]> before = fixture.snapshot();
                Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), DesignActivity.class);
                intent.putExtra("sc_id", fixture.id);
                try (ActivityScenario<DesignActivity> scenario = ActivityScenario.launch(intent)) {
                    AtomicReference<String> title = new AtomicReference<>();
                    AtomicReference<String> exit = new AtomicReference<>();
                    scenario.onActivity(activity -> {
                        title.set(activity.getString(R.string.project_load_failed_title));
                        exit.set(activity.getString(R.string.common_word_exit));
                    });
                    AccessibilityNodeInfo dialog = waitForDialog(title.get());
                    assertFalse(ProjectDataManager.canSaveProject(fixture.id));
                    fixture.assertUnchanged(before);
                    List<AccessibilityNodeInfo> buttons = dialog.findAccessibilityNodeInfosByText(exit.get());
                    assertFalse(buttons.isEmpty());
                    boolean clicked = false;
                    for (AccessibilityNodeInfo button : buttons) {
                        if (exit.get().contentEquals(button.getText()) && button.isClickable()) {
                            clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (clicked) break;
                        }
                    }
                    assertTrue("Exit button could not be clicked", clicked);
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                }
                fixture.assertUnchanged(before);
            }
        }
    }

    private static AccessibilityNodeInfo waitForDialog(String title) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if (root != null && !root.findAccessibilityNodeInfosByText(title).isEmpty()) return root;
            Thread.sleep(100);
        }
        throw new AssertionError("Project load failure dialog did not appear");
    }

    @Test
    public void logicSectionsRejectPartialStateAndKeepAllPreviousMaps() throws Exception {
        String[] damagedSections = {
                "@MainActivity.java_onCreate\n" + block("before") + "\n" + BROKEN + "\n" + block("after"),
                "@MainActivity.java_var\n2:before\nwrong:broken\n2:after",
                "@MainActivity.java_list\n2:before\nwrong:broken\n2:after",
                "@MainActivity.java_components\n{}\n" + BROKEN + "\n{}",
                "@MainActivity.java_events\n{}\n" + BROKEN + "\n{}",
                "@MainActivity.java_func\nbefore:spec\nbroken\nafter:spec",
                "unexpected data without header", "@\n{}", "@unexpected_key\n{}"
        };
        for (String damaged : damagedSections) {
            ProjectDataStore store = new ProjectDataStore("f01-reader-only");
            store.readLogicData(reader(healthy("logic")));
            Object[] old = {store.variableMap, store.listMap, store.moreBlockMap,
                    store.componentMap, store.eventMap, store.blockMap};
            expectFailure(() -> store.readLogicData(reader(damaged)));
            assertSame(old[0], store.variableMap);
            assertSame(old[1], store.listMap);
            assertSame(old[2], store.moreBlockMap);
            assertSame(old[3], store.componentMap);
            assertSame(old[4], store.eventMap);
            assertSame(old[5], store.blockMap);
            assertNotNull(store.getLoadFailure());
            store.clearAllData();
            assertFalse(store.canSave());
            store.readLogicData(reader(healthy("logic")));
            assertTrue(store.canSave());
        }
    }

    @Test
    public void viewAndFabFailuresPreservePreviousMaps() throws Exception {
        for (String content : new String[]{damaged("view"), "@partial.xml_fab\n" + BROKEN,
                "@partial.xml_fab\nnull", "@partial.xml\nnull"}) {
            ProjectDataStore store = new ProjectDataStore("f01-reader-only");
            store.readViewData(reader(healthy("view")));
            Object oldViews = store.viewMap;
            Object oldFab = store.fabMap;
            expectFailure(() -> store.readViewData(reader(content)));
            assertSame(oldViews, store.viewMap);
            assertSame(oldFab, store.fabMap);
            assertFalse(store.canSave());
            store.readViewData(reader(healthy("view")));
            assertTrue(store.canSave());
        }
    }

    @Test
    public void libraryResourceAndFileReadersKeepPreviousStateOnFailure() throws Exception {
        LibraryManager libraries = new LibraryManager("f01-reader-only");
        libraries.parseLibraryData(reader(healthy("library")));
        Object oldCompat = libraries.compat;
        Object oldFirebase = libraries.firebaseDB;
        expectFailure(() -> libraries.parseLibraryData(reader(damaged("library"))));
        assertSame(oldCompat, libraries.compat);
        assertSame(oldFirebase, libraries.firebaseDB);
        libraries.initializeDefaults();
        assertFalse(libraries.canSave());
        libraries.parseLibraryData(reader(healthy("library")));
        assertTrue(libraries.canSave());

        ResourceManager resources = new ResourceManager("f01-reader-only");
        resources.parseResourceData(reader(healthy("resource")));
        Object oldImages = resources.images;
        Object oldSounds = resources.sounds;
        Object oldFonts = resources.fonts;
        expectFailure(() -> resources.parseResourceData(reader(damaged("resource"))));
        assertSame(oldImages, resources.images);
        assertSame(oldSounds, resources.sounds);
        assertSame(oldFonts, resources.fonts);
        assertFalse(resources.canSave());
        resources.parseResourceData(reader(healthy("resource")));
        assertTrue(resources.canSave());

        ProjectFileManager files = new ProjectFileManager("f01-reader-only");
        files.parseFileData(reader(healthy("file")));
        Object oldActivities = files.activities;
        Object oldCustomViews = files.customViews;
        Object oldXmlNames = files.xmlNames;
        expectFailure(() -> files.parseFileData(reader(damaged("file"))));
        assertSame(oldActivities, files.activities);
        assertSame(oldCustomViews, files.customViews);
        assertSame(oldXmlNames, files.xmlNames);
        files.initializeDefaults();
        assertFalse(files.canSave());
        files.parseFileData(reader(healthy("file")));
        assertTrue(files.canSave());
    }

    @Test
    public void ioFailuresRemainBlockedUntilASuccessfulReread() throws Exception {
        ProjectDataStore store = new ProjectDataStore("f01-reader-only");
        store.readLogicData(reader(healthy("logic")));
        Object oldBlocks = store.blockMap;
        expectFailure(() -> store.readLogicData(failingReader(healthy("logic"))));
        assertSame(oldBlocks, store.blockMap);
        assertFalse(store.canSave());
        store.readViewData(reader(healthy("view")));
        assertFalse(store.canSave()); // A successful view read cannot clear a logic failure.
        store.readLogicData(reader(healthy("logic")));
        assertTrue(store.canSave());
        LibraryManager libraries = new LibraryManager("f01-reader-only");
        ResourceManager resources = new ResourceManager("f01-reader-only");
        ProjectFileManager files = new ProjectFileManager("f01-reader-only");
        expectFailure(() -> libraries.parseLibraryData(failingReader(healthy("library"))));
        expectFailure(() -> resources.parseResourceData(failingReader(healthy("resource"))));
        expectFailure(() -> files.parseFileData(failingReader(healthy("file"))));
        assertFalse(libraries.canSave());
        assertFalse(resources.canSave());
        assertFalse(files.canSave());
    }

    @Test(timeout = 30000)
    public void decryptFailuresAlsoBlockTheEditorSaveEntry() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.seed(true);
            Files.write(new File(fixture.data, "logic").toPath(), new byte[]{0, 1, 2, 3, 4});
            Map<Path, byte[]> before = fixture.snapshot();
            fixture.load();
            assertFalse(ProjectDataManager.canSaveProject(fixture.id));
            assertNotNull(ProjectDataManager.getProjectDataManager(fixture.id).getLoadFailure());
            assertEquals(Boolean.FALSE, save(fixture.id));
            fixture.assertUnchanged(before);
        }
    }

    @Test(timeout = 30000)
    public void missingNewProjectFilesAndEmptyDataAreAllowed() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.load();
            assertTrue(ProjectDataManager.canSaveProject(fixture.id));
            assertEquals(Boolean.TRUE, save(fixture.id));
            fixture.load();
            assertTrue(ProjectDataManager.canSaveProject(fixture.id));
        }
        ProjectDataStore store = new ProjectDataStore("f01-reader-only");
        store.readLogicData(reader(""));
        store.readViewData(reader(""));
        assertTrue(store.canSave());
    }

    @Test
    public void multilineProjectObjectsAndCollectionImportBehaviorArePreserved() throws Exception {
        String pretty = block("before").replace(",", ",\n");
        ProjectDataStore store = new ProjectDataStore("f01-reader-only");
        store.readLogicData(reader("@MainActivity.java_onCreate\n" + pretty + "\n" + block("after")));
        assertEquals(2, store.getBlocks("MainActivity.java", "onCreate").size());
        assertEquals(1, ProjectDataParser.parseBlockBeans(ProjectDataStore.gson,
                block("before") + "\n" + BROKEN + "\n" + block("after")).size());
    }

    @Test
    public void healthyComponentsEventsMoreBlocksAndFabAreStillAccepted() throws Exception {
        ProjectDataStore store = new ProjectDataStore("f01-reader-only");
        String component = ProjectDataStore.gson.toJson(new ComponentBean(ComponentBean.COMPONENT_TYPE_INTENT, "intent"));
        String event = ProjectDataStore.gson.toJson(new EventBean(EventBean.EVENT_TYPE_ACTIVITY, 0, "", "onStart"));
        store.readLogicData(reader("@MainActivity.java_components\n" + component
                + "\n@MainActivity.java_events\n" + event + "\n@MainActivity.java_func\nfunction:spec:with:colons\n"));
        assertEquals(1, store.componentMap.get("MainActivity.java").size());
        assertEquals(1, store.eventMap.get("MainActivity.java").size());
        assertEquals("spec:with:colons", store.moreBlockMap.get("MainActivity.java").get(0).second);
        store.readViewData(reader("@partial.xml_fab\n" + ProjectDataStore.gson.toJson(new ViewBean("_fab", ViewBean.VIEW_TYPE_WIDGET_FAB))));
        assertNotNull(store.fabMap.get("partial.xml"));
        assertTrue(store.canSave());
    }

    private static void assertDirectWritesBlocked(Fixture fixture, String module) {
        switch (module) {
            case "logic":
            case "view":
                ProjectDataStore data = ProjectDataManager.getProjectDataManager(fixture.id);
                assertFalse(data.saveAllData());
                assertFalse(data.saveAllBackup());
                assertFalse(data.saveLogicFile(new File(fixture.data, "logic").getPath()));
                assertFalse(data.saveViewFile(new File(fixture.data, "view").getPath()));
                break;
            case "library":
                assertFalse(ProjectDataManager.getLibraryManager(fixture.id).saveToData());
                assertFalse(ProjectDataManager.getLibraryManager(fixture.id).saveToBackup());
                break;
            case "resource":
                assertFalse(ProjectDataManager.getResourceManager(fixture.id).saveToData());
                assertFalse(ProjectDataManager.getResourceManager(fixture.id).saveToBackup());
                break;
            case "file":
                assertFalse(ProjectDataManager.getFileManager(fixture.id).saveToData());
                assertFalse(ProjectDataManager.getFileManager(fixture.id).saveToBackup());
                break;
            default: throw new AssertionError(module);
        }
    }

    private static Object save(String id) throws Exception {
        Method save = controllerClass().getDeclaredMethod("saveProjectDataToFiles", String.class);
        save.setAccessible(true);
        return save.invoke(null, id);
    }

    private static void invokeAutomaticBackup(String id) throws Exception {
        Class<?> type = controllerClass();
        Constructor<?> constructor = type.getDeclaredConstructor(
                Class.forName("pro.sketchware.activities.design.DesignActivity"), String.class);
        constructor.setAccessible(true);
        Object controller = constructor.newInstance(null, id);
        Method backup = type.getDeclaredMethod("saveUnsavedChangesInBackground");
        backup.setAccessible(true);
        backup.invoke(controller);
    }

    private static Class<?> controllerClass() throws ClassNotFoundException {
        return Class.forName("pro.sketchware.activities.design.DesignProjectController");
    }

    private static void assertHealthy(String id) {
        ProjectDataStore store = ProjectDataManager.getProjectDataManager(id);
        assertEquals(3, store.getBlocks("MainActivity.java", "onCreate").size());
        assertEquals(3, store.getViews("partial.xml").size());
        assertEquals("Y", ProjectDataManager.getLibraryManager(id).compat.useYn);
        assertEquals(3, ProjectDataManager.getResourceManager(id).images.size());
        assertEquals(2, ProjectDataManager.getFileManager(id).activities.size());
    }

    private static String damaged(String name) {
        switch (name) {
            case "logic": return "@MainActivity.java_onCreate\n" + block("before") + "\n" + BROKEN + "\n" + block("after");
            case "view": return "@partial.xml\n" + view("before") + "\n" + BROKEN + "\n" + view("after");
            case "library": return "@firebaseDB\n" + library("firebase_before") + "\n@compat\n" + BROKEN + "\n@admob\n" + library("admob_after");
            case "resource": return "@images\n" + resource("image_before", ".png") + "\n" + BROKEN + "\n" + resource("image_after", ".png");
            case "file": return "@activity\n" + file("before") + "\nnot_json\n" + file("after");
            default: throw new AssertionError(name);
        }
    }

    private static String healthy(String name) {
        switch (name) {
            case "logic": return "@MainActivity.java_var\n2:variable\n@MainActivity.java_onCreate\n" + block("before") + "\n" + block("middle") + "\n" + block("after") + "\n@MainActivity.java_list\n2:list\n";
            case "view": return "@partial.xml\n" + view("before") + "\n" + view("middle") + "\n" + view("after") + "\n";
            case "library": return "@firebaseDB\n" + library("firebase") + "\n@compat\n" + library("compat") + "\n";
            case "resource": return "@images\n" + resource("image_before", ".png") + "\n" + resource("image_middle", ".png") + "\n" + resource("image_after", ".png") + "\n@sounds\n" + resource("sound", ".wav") + "\n@fonts\n" + resource("font", ".ttf") + "\n";
            case "file": return "@activity\n" + file("main") + "\n" + file("second") + "\n@customview\n";
            default: throw new AssertionError(name);
        }
    }

    private static String block(String id) {
        BlockBean bean = new BlockBean();
        bean.id = id;
        bean.opCode = "doToast";
        bean.spec = id;
        return ProjectDataStore.gson.toJson(bean);
    }

    private static String view(String id) {
        ViewBean bean = new ViewBean(id, ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
        bean.parent = "root";
        return ProjectDataStore.gson.toJson(bean);
    }

    private static String library(String data) {
        ProjectLibraryBean bean = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
        bean.useYn = "Y";
        bean.data = data;
        return ProjectDataStore.gson.toJson(bean);
    }

    private static String resource(String name, String extension) {
        return ProjectDataStore.gson.toJson(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, name + extension));
    }

    private static String file(String name) {
        return ProjectDataStore.gson.toJson(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, name));
    }

    private static BufferedReader reader(String content) {
        return new BufferedReader(new StringReader(content));
    }

    private static BufferedReader failingReader(String content) {
        return new BufferedReader(new StringReader(content)) {
            int reads;
            @Override public String readLine() throws IOException {
                if (++reads == 3) throw new IOException("Injected I/O failure");
                return super.readLine();
            }
        };
    }

    private static void expectFailure(ProjectLoadState.ReadOperation operation) throws Exception {
        try {
            operation.read();
            fail("Damaged data was accepted");
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final String id = "f01_test_" + UUID.randomUUID().toString().replace("-", "");
        final File data = new File(SketchwarePaths.getDataPath(id));
        final File backup = new File(SketchwarePaths.getBackupPath(id));
        final File images = new File(SketchwarePaths.getImagesPath(), id);
        final File sounds = new File(SketchwarePaths.getSoundsPath(), id);
        final File fonts = new File(SketchwarePaths.getFontsResourcePath(), id);
        final File[] roots = {data, backup, images, sounds, fonts};
        final ArrayList<File> createdParents = new ArrayList<>();

        Fixture() throws Exception {
            for (File root : roots) {
                assertEquals(id, root.getName());
                assertTrue(root.getCanonicalPath().startsWith(Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator));
                assertFalse(root.exists());
                createParents(root.getParentFile());
                assertTrue(root.mkdir());
            }
        }

        void createParents(File directory) {
            if (directory.isDirectory()) return;
            createParents(directory.getParentFile());
            assertTrue(directory.mkdir());
            createdParents.add(directory);
        }

        void seed(boolean encrypted) throws Exception {
            for (String name : METADATA) {
                write(data, name, healthy(name), encrypted);
                write(backup, name, healthy(name) + "\n", encrypted);
            }
            for (String name : new String[]{"image_before", "image_middle", "image_after", "orphan"})
                Files.write(new File(images, name + ".png").toPath(), name.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(sounds, "sound.wav").toPath(), new byte[]{1, 2});
            Files.write(new File(fonts, "font.ttf").toPath(), new byte[]{3, 4});
        }

        void write(File directory, String name, String content, boolean encrypted) throws Exception {
            Files.write(new File(directory, name).toPath(), IO.encryptString(content, encrypted));
        }

        void load() {
            ProjectDataManager.clearAll();
            ProjectDataManager.getProjectDataManager(id, false);
            ProjectDataManager.getFileManager(id, false);
            ProjectDataManager.getResourceManager(id, false);
            ProjectDataManager.getLibraryManager(id, false);
        }

        Map<Path, byte[]> snapshot() throws IOException {
            Map<Path, byte[]> result = new HashMap<>();
            for (File root : roots) {
                try (Stream<Path> paths = Files.walk(root.toPath())) {
                    for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator)
                        result.put(path, Files.readAllBytes(path));
                }
            }
            return result;
        }

        void assertUnchanged(Map<Path, byte[]> before) throws IOException {
            Map<Path, byte[]> after = snapshot();
            assertEquals(before.keySet(), after.keySet());
            for (Path path : before.keySet()) assertArrayEquals(path.toString(), before.get(path), after.get(path));
        }

        @Override public void close() throws Exception {
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
