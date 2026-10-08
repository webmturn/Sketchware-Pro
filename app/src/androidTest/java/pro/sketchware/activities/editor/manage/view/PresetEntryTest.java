package pro.sketchware.activities.editor.manage.view;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.viewpager.widget.ViewPager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.view.fragments.CustomViewFilesFragment;
import pro.sketchware.activities.editor.manage.view.fragments.ViewFilesFragment;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.UIHelper;
import pro.sketchware.util.io.EncryptedFileUtil;

/** Verifies typed Intent transport and real preset selection/result paths on isolated projects. */
@RunWith(AndroidJUnit4.class)
public class PresetEntryTest {
    private static final String ITEM = "f09_item";
    private static final String DRAWER = ProjectFileBean.getDrawerName("main");
    private static final String[] LAYOUTS = {"main.xml", ITEM + ".xml", DRAWER + ".xml"};

    @Test public void parcelledExtrasPreserveCharAndIntTypes() {
        for (int code : new int[]{ViewPresetRequestCodes.ACTIVITY, ViewPresetRequestCodes.LIST_ITEM, ViewPresetRequestCodes.DRAWER}) {
            Intent integer = roundTrip(new Intent().putExtra("request_code", code));
            assertTrue(integer.getExtras().get("request_code") instanceof Integer);
            assertEquals(code, integer.getIntExtra("request_code", -1));
            if (code == ViewPresetRequestCodes.ACTIVITY) continue;
            Intent character = roundTrip(new Intent().putExtra("request_code", (char) code));
            assertTrue(character.getExtras().get("request_code") instanceof Character);
            assertEquals((char) code, character.getCharExtra("request_code", (char) 0));
            assertEquals(-1, character.getIntExtra("request_code", -1));
            status("f09_transport", "code=" + code + ", charAsInt=-1, intAsInt=" + integer.getIntExtra("request_code", -1));
        }
    }

    @Test(timeout = 30000) public void integerSelectionReturnsEachPresetCategory() throws Exception {
        for (int code : new int[]{ViewPresetRequestCodes.ACTIVITY, ViewPresetRequestCodes.LIST_ITEM, ViewPresetRequestCodes.DRAWER}) {
            Intent intent = new Intent(context(), PresetSettingActivity.class).putExtra("request_code", code);
            try (ActivityScenario<PresetSettingActivity> scenario = ActivityScenario.launchActivityForResult(intent)) {
                scenario.onActivity(activity -> {
                    assertEquals(firstPreset(code), label(activity));
                    activity.findViewById(R.id.common_dialog_ok_button).performClick();
                });
                waitUntil(() -> scenario.getState() == Lifecycle.State.DESTROYED);
                Instrumentation.ActivityResult result = scenario.getResult();
                assertEquals(Activity.RESULT_OK, result.getResultCode());
                result.getResultData().setExtrasClassLoader(ProjectFileBean.class.getClassLoader());
                ProjectFileBean preset = result.getResultData().getParcelableExtra("preset_data");
                assertNotNull(preset);
                assertEquals(firstPreset(code), preset.presetName);
                assertEquals(fileType(code), preset.fileType);
            }
        }
    }

    @Test(timeout = 30000) public void customViewEditOpensListItemCategory() throws Exception {
        assertEditCategory(ViewPresetRequestCodes.LIST_ITEM);
    }

    @Test(timeout = 30000) public void drawerEditOpensDrawerCategory() throws Exception {
        assertEditCategory(ViewPresetRequestCodes.DRAWER);
    }

    private void assertEditCategory(int code) throws Exception {
        try (Fixture fixture = new Fixture(); Entry entry = new Entry(fixture, code)) {
            assertEntry(entry, code);
        }
    }

    @Test(timeout = 30000) public void customViewImportReplacesOnlySelectedLayout() throws Exception {
        assertImport(ViewPresetRequestCodes.LIST_ITEM);
    }

    @Test(timeout = 30000) public void drawerImportReplacesOnlySelectedLayout() throws Exception {
        assertImport(ViewPresetRequestCodes.DRAWER);
    }

    private void assertImport(int code) throws Exception {
        try (Fixture fixture = new Fixture(); Entry entry = new Entry(fixture, code)) {
            String selected = code == ViewPresetRequestCodes.LIST_ITEM ? LAYOUTS[1] : LAYOUTS[2];
            Map<String, List<String>> before = fixture.views();
            Map<Path, byte[]> files = fixture.snapshot();
            // Let the actual result handler run even on the legacy version, to expose lost views.
            entry.importPreset();
            ArrayList<ViewBean> imported = ProjectDataManager.getProjectDataManager(fixture.id).getViews(selected);
            status("f09_import_result", "code=" + code + ", views=" + imported.size()
                    + ", originalPresent=" + imported.stream().anyMatch(view -> view.id.startsWith("original_")));
            assertEquals("Wrong preset category cleared the selected layout", 7, imported.size());
            assertEquals(7, new HashSet<>(imported.stream().map(view -> view.id).toList()).size());
            assertTrue(imported.stream().allMatch(view -> "TextView".equals(view.text.text)));
            assertOtherLayoutsUnchanged(before, fixture.views(), selected);
            fixture.assertUnchanged(files);
            assertTrue(ProjectDataManager.getProjectDataManager(fixture.id).saveAllData());
            ProjectDataManager.reloadProjectFromData(fixture.id);
            assertEquals(7, ProjectDataManager.getProjectDataManager(fixture.id).getViews(selected).size());
            assertOtherLayoutsUnchanged(before, fixture.views(), selected);
        }
    }

    @Test(timeout = 30000) public void cancelledCustomAndDrawerEditsPreserveProjectData() throws Exception {
        for (int code : new int[]{ViewPresetRequestCodes.LIST_ITEM, ViewPresetRequestCodes.DRAWER}) {
            try (Fixture fixture = new Fixture(); Entry entry = new Entry(fixture, code)) {
                Map<String, List<String>> before = fixture.views();
                Map<Path, byte[]> files = fixture.snapshot();
                onMain(() -> entry.activity.findViewById(R.id.common_dialog_cancel_button).performClick());
                entry.waitForManager();
                assertEquals(before, fixture.views());
                fixture.assertUnchanged(files);
            }
        }
    }

    @Test(timeout = 30000) public void recreatedCustomViewEditorKeepsListItemCategory() throws Exception {
        try (Fixture fixture = new Fixture(); Entry entry = new Entry(fixture, ViewPresetRequestCodes.LIST_ITEM)) {
            PresetSettingActivity original = entry.activity;
            onMain(original::recreate);
            waitUntil(() -> {
                AtomicReference<PresetSettingActivity> recreated = new AtomicReference<>();
                onMain(() -> {
                    for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                        if (activity instanceof PresetSettingActivity preset && activity != original) recreated.set(preset);
                    }
                });
                if (recreated.get() == null) return false;
                entry.activity = recreated.get();
                return true;
            });
            assertEntry(entry, ViewPresetRequestCodes.LIST_ITEM);
        }
    }

    @Test(timeout = 30000) public void activityPresetEntryStillImportsSelectedPreset() throws Exception {
        try (Fixture fixture = new Fixture(); Entry entry = new Entry(fixture, ViewPresetRequestCodes.ACTIVITY)) {
            assertEntry(entry, ViewPresetRequestCodes.ACTIVITY);
            Map<String, List<String>> before = fixture.views();
            onMain(() -> {
                entry.activity.findViewById(R.id.btn_left).performClick();
                assertEquals("Text Activity", label(entry.activity));
                entry.activity.findViewById(R.id.btn_right).performClick();
                assertEquals("Empty Activity", label(entry.activity));
                entry.activity.findViewById(R.id.btn_left).performClick();
            });
            entry.importPreset();
            assertEquals(7, ProjectDataManager.getProjectDataManager(fixture.id).getViews(LAYOUTS[0]).size());
            assertOtherLayoutsUnchanged(before, fixture.views(), LAYOUTS[0]);
        }
    }

    private static void assertEntry(Entry entry, int code) {
        onMain(() -> {
            assertTrue("Preset entry must send an integer", entry.activity.getIntent().getExtras().get("request_code") instanceof Integer);
            assertEquals(code, entry.activity.getIntent().getIntExtra("request_code", -1));
            assertTrue(entry.activity.getIntent().getBooleanExtra("edit_mode", false));
            assertEquals(firstPreset(code), label(entry.activity));
        });
    }

    private static void assertOtherLayoutsUnchanged(Map<String, List<String>> before, Map<String, List<String>> after, String selected) {
        for (String layout : LAYOUTS) if (!layout.equals(selected)) assertEquals(layout, before.get(layout), after.get(layout));
    }

    private static Intent roundTrip(Intent intent) {
        Parcel parcel = Parcel.obtain();
        try {
            intent.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Intent.CREATOR.createFromParcel(parcel);
        } finally { parcel.recycle(); }
    }

    private static int fileType(int code) {
        return code == ViewPresetRequestCodes.ACTIVITY ? ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY
                : code == ViewPresetRequestCodes.LIST_ITEM ? ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW : ProjectFileBean.PROJECT_FILE_TYPE_DRAWER;
    }

    private static String firstPreset(int code) {
        return code == ViewPresetRequestCodes.ACTIVITY ? "Empty Activity"
                : code == ViewPresetRequestCodes.LIST_ITEM ? "Basic List Item" : "Basic Drawer";
    }

    private static String label(Activity activity) { return ((TextView) activity.findViewById(R.id.tv_activity_name)).getText().toString(); }
    private static Instrumentation instrumentation() { return InstrumentationRegistry.getInstrumentation(); }
    private static android.content.Context context() { return instrumentation().getTargetContext(); }
    private static void onMain(Runnable action) { instrumentation().runOnMainSync(action); }
    private static void status(String key, String value) {
        Bundle status = new Bundle(); status.putString(key, value); instrumentation().sendStatus(0, status);
    }

    private interface Condition { boolean satisfied() throws Exception; }
    private static void waitUntil(Condition condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.satisfied()) return;
            Thread.sleep(100);
        }
        fail("Preset operation did not complete");
    }

    private static void clickConfirmation() throws Exception {
        String ok = context().getString(R.string.common_word_ok);
        waitUntil(() -> {
            AccessibilityNodeInfo root = instrumentation().getUiAutomation().getRootInActiveWindow();
            if (root == null) return false;
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(ok)) {
                if (ok.contentEquals(node.getText()) && node.isClickable()
                        && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
            return false;
        });
    }

    private static final class Entry implements AutoCloseable {
        final Instrumentation.ActivityMonitor monitor = instrumentation().addMonitor(PresetSettingActivity.class.getName(), null, false);
        ActivityScenario<ManageViewActivity> scenario;
        PresetSettingActivity activity;

        Entry(Fixture fixture, int code) throws Exception {
            try {
                Intent intent = new Intent(context(), ManageViewActivity.class).putExtra("sc_id", fixture.id).putExtra("compatUseYn", "Y");
                scenario = ActivityScenario.launch(intent);
                scenario.onActivity(manager -> ((ViewPager) manager.findViewById(R.id.view_pager))
                        .setCurrentItem(code == ViewPresetRequestCodes.ACTIVITY ? 0 : 1, false));
                instrumentation().waitForIdleSync();
                waitUntil(() -> {
                    AtomicBoolean clicked = new AtomicBoolean();
                    scenario.onActivity(manager -> {
                        for (Fragment fragment : manager.getSupportFragmentManager().getFragments()) {
                            boolean correct = code == ViewPresetRequestCodes.ACTIVITY ? fragment instanceof ViewFilesFragment
                                    : fragment instanceof CustomViewFilesFragment;
                            if (!correct || fragment.getView() == null) continue;
                            RecyclerView list = fragment.getView().findViewById(R.id.list_activities);
                            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(code == ViewPresetRequestCodes.DRAWER ? 1 : 0);
                            if (holder != null) {
                                UIHelper.lastClickTime = 0;
                                clicked.set(holder.itemView.findViewById(R.id.img_preset_setting).performClick());
                            }
                        }
                    });
                    return clicked.get();
                });
                Activity launched = instrumentation().waitForMonitorWithTimeout(monitor, 10000);
                assertTrue("Preset icon did not open the editor", launched instanceof PresetSettingActivity);
                activity = (PresetSettingActivity) launched;
                instrumentation().waitForIdleSync();
                onMain(() -> status("f09_entry", "code=" + code + ", extraType="
                        + activity.getIntent().getExtras().get("request_code").getClass().getName()
                        + ", received=" + activity.getIntent().getIntExtra("request_code", -1) + ", category=" + label(activity)));
            } catch (Exception | Error error) {
                close();
                throw error;
            }
        }

        void importPreset() throws Exception {
            onMain(() -> activity.findViewById(R.id.common_dialog_ok_button).performClick());
            clickConfirmation();
            waitForManager();
        }

        void waitForManager() throws Exception {
            waitUntil(() -> scenario.getState() == Lifecycle.State.RESUMED);
            instrumentation().waitForIdleSync();
        }

        @Override public void close() {
            if (activity != null) onMain(() -> { if (!activity.isDestroyed()) activity.finish(); });
            instrumentation().waitForIdleSync();
            if (scenario != null) scenario.close();
            instrumentation().removeMonitor(monitor);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final String id = "f09_test_" + UUID.randomUUID().toString().replace("-", "");
        final File[] roots = {new File(SketchwarePaths.getDataPath(id)), new File(SketchwarePaths.getBackupPath(id)),
                new File(SketchwarePaths.getImagesPath(), id), new File(SketchwarePaths.getSoundsPath(), id),
                new File(SketchwarePaths.getFontsResourcePath(), id)};
        final long previousClickTime = UIHelper.lastClickTime;

        Fixture() throws Exception {
            for (File root : roots) { assertFalse(root.toString(), root.exists()); assertEquals(id, root.getName()); }
            status("f09_owned_project", id);
            try {
                for (File root : roots) { assertTrue(root.getParentFile().isDirectory()); assertTrue(root.mkdir()); }
                ProjectFileBean main = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
                main.setActivityOptions(main.options | ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                write("file", "@activity\n" + ProjectDataStore.gson.toJson(main) + "\n@customview\n"
                        + ProjectDataStore.gson.toJson(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, ITEM)) + "\n"
                        + ProjectDataStore.gson.toJson(new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_DRAWER, DRAWER)) + "\n");
                StringBuilder views = new StringBuilder();
                for (String layout : LAYOUTS) {
                    ViewBean bean = new ViewBean("original_" + layout.replace('.', '_'), ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
                    bean.parent = "root"; bean.text.text = layout;
                    views.append('@').append(layout).append('\n').append(ProjectDataStore.gson.toJson(bean)).append('\n');
                }
                write("view", views.toString());
                write("logic", "@MainActivity.java_var\n2:initial\n");
                write("resource", "@images\n@sounds\n@fonts\n");
                ProjectLibraryBean compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
                compat.useYn = "Y";
                write("library", "@compat\n" + ProjectDataStore.gson.toJson(compat) + "\n");
                ProjectDataManager.clearAll();
                assertTrue(ProjectDataManager.canSaveProject(id));
            } catch (Exception | Error error) {
                close();
                throw error;
            }
        }

        void write(String name, String content) throws Exception {
            Files.write(new File(roots[0], name).toPath(), new EncryptedFileUtil().encryptString(content, true));
        }

        Map<String, List<String>> views() {
            Map<String, List<String>> result = new HashMap<>();
            for (String layout : LAYOUTS) result.put(layout, ProjectDataManager.getProjectDataManager(id).getViews(layout)
                    .stream().map(ProjectDataStore.gson::toJson).toList());
            return result;
        }

        Map<Path, byte[]> snapshot() throws Exception {
            Map<Path, byte[]> result = new HashMap<>();
            for (File root : roots) try (Stream<Path> paths = Files.walk(root.toPath())) {
                for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) result.put(path, Files.readAllBytes(path));
            }
            return result;
        }

        void assertUnchanged(Map<Path, byte[]> before) throws Exception {
            Map<Path, byte[]> after = snapshot();
            assertEquals(before.keySet(), after.keySet());
            for (Path path : before.keySet()) assertArrayEquals(path.toString(), before.get(path), after.get(path));
        }

        @Override public void close() throws Exception {
            onMain(() -> { ProjectDataManager.clearAll(); UIHelper.lastClickTime = previousClickTime; });
            for (File root : roots) {
                if (!root.exists()) continue;
                assertEquals(id, root.getName());
                try (Stream<Path> paths = Files.walk(root.toPath())) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        assertTrue(path.startsWith(root.toPath())); Files.delete(path);
                    }
                }
                assertFalse(root.exists());
            }
            status("f09_cleaned_project", id);
        }
    }
}
