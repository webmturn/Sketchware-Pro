package pro.sketchware.activities.editor.manage.library;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.core.library.LocalLibrary;

/** Uses intent fixtures and private cache files; never edits the user's project/library data. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class DependencyBrowserTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private ActivityScenario<SubDependenciesActivity> scenario;
    private SharedPreferences preferences;
    private boolean hadPreference, originalPreference;
    private File fixtureDir, fixtureFile, fallbackRoot;
    private boolean createdFallbackRoot, createdFixture;

    @Before public void prepare() throws Exception {
        preferences = context.getSharedPreferences(SubDependenciesActivity.VIEW_PREFERENCES, Context.MODE_PRIVATE);
        hadPreference = preferences.contains(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE);
        originalPreference = preferences.getBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, false);
        preferences.edit().putBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, false).commit();
        fallbackRoot = new File(context.getExternalFilesDir(null), "local_libs");
        createdFallbackRoot = !fallbackRoot.exists();
        fixtureDir = new File(fallbackRoot, "__dependency_browser_review_leaf");
        assertFalse("Do not overwrite an existing library", fixtureDir.exists());
        assertTrue(fixtureDir.mkdirs());
        createdFixture = true;
        fixtureFile = new File(fixtureDir, "fixture.txt");
        Files.write(fixtureFile.toPath(), "fixture".getBytes(StandardCharsets.UTF_8));
    }
    @After public void restore() throws Exception {
        if (scenario != null) scenario.close();
        SharedPreferences.Editor edit = preferences.edit();
        if (hadPreference) edit.putBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, originalPreference);
        else edit.remove(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE);
        edit.commit();
        if (fixtureFile != null) Files.deleteIfExists(fixtureFile.toPath());
        if (createdFixture) Files.deleteIfExists(fixtureDir.toPath());
        if (createdFallbackRoot) Files.deleteIfExists(fallbackRoot.toPath());
    }

    private Intent fixture(boolean hierarchy) {
        List<String> names = List.of("branch", "child", "leaf", "internal-root", "internal-child", "grandchild");
        List<String> parents = List.of("review:root:1", "review:branch:1", "review:root:1",
                "review:root:1", "review:branch:1", "review:child:1");
        ArrayList<String> coords = new ArrayList<>(), folders = new ArrayList<>();
        for (String name : names) { coords.add("review:" + name + ":1"); folders.add("__dependency_browser_review_" + name); }
        return new Intent(context, SubDependenciesActivity.class)
                .putExtra(SubDependenciesActivity.EXTRA_ROOT_LIB_NAME, "root-v1")
                .putExtra(SubDependenciesActivity.EXTRA_ROOT_COORDINATE, "review:root:1")
                .putExtra(SubDependenciesActivity.EXTRA_SC_ID, "system")
                .putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_FOLDERS, folders)
                .putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_COORDS, coords)
                .putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_PARENTS, hierarchy ? new ArrayList<>(parents) : null)
                .putExtra(SubDependenciesActivity.EXTRA_DEP_DEPTHS, hierarchy ? new int[]{1,2,1,1,2,3} : null)
                .putExtra(SubDependenciesActivity.EXTRA_DEP_BUILTIN, new boolean[]{false,false,false,true,true,false});
    }
    private void launch(boolean hierarchy) {
        scenario = ActivityScenario.launch(fixture(hierarchy));
        scenario.onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = list(a).getAdapter().getItemCount() > 0);
            if (ready[0]) { settle(); return; }
            try { Thread.sleep(30); } catch (InterruptedException e) { throw new AssertionError(e); }
        }
        fail("Dependencies did not load");
    }
    @Test public void switchPreservesBranchAndPreferenceAcrossReopening() {
        launch(true);
        scenario.onActivity(a -> {
            assertEquals(3, list(a).getAdapter().getItemCount());
            list(a).findViewHolderForAdapterPosition(0).itemView.performClick();
        });
        settle();
        scenario.onActivity(a -> {
            assertEquals(5, list(a).getAdapter().getItemCount());
            toggle(a);
            assertEquals(2, list(a).getAdapter().getItemCount());
            assertTrue(((TextView) a.findViewById(R.id.dependency_path)).getText().toString().endsWith("branch"));
            assertEquals(a.getString(R.string.dependency_browser_switch_tree), toolbar(a).getMenu()
                    .findItem(R.id.dependency_switch_view).getContentDescription());
        });
        assertTrue(preferences.getBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, false));
        scenario.close(); scenario = null;
        launch(true);
        scenario.onActivity(a -> assertTrue(((TextView) a.findViewById(R.id.dependency_section_title))
                .getText().toString().contains(a.getString(R.string.dependency_browser_levels))));
    }
    @Test public void levelNavigationAndRecreationKeepTheCurrentPath() {
        preferences.edit().putBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, true).commit();
        launch(true);
        scenario.onActivity(a -> list(a).findViewHolderForAdapterPosition(0).itemView.performClick());
        settle();
        scenario.recreate();
        awaitRows(2);
        scenario.onActivity(a -> {
            assertEquals(2, list(a).getAdapter().getItemCount());
            assertTrue(((TextView) a.findViewById(R.id.dependency_path)).getText().toString().endsWith("branch"));
            a.findViewById(R.id.dependency_up).performClick();
            assertEquals(3, list(a).getAdapter().getItemCount());
            assertEquals(View.GONE, a.findViewById(R.id.dependency_path_bar).getVisibility());
        });
    }
    @Test public void builtInGroupsExpandWithoutProjectSwitches() {
        launch(true);
        scenario.onActivity(a -> {
            View group = list(a).findViewHolderForAdapterPosition(2).itemView;
            assertFalse(group.isLongClickable());
            group.performClick();
        });
        settle();
        scenario.onActivity(a -> {
            assertEquals(4, list(a).getAdapter().getItemCount());
            list(a).findViewHolderForAdapterPosition(3).itemView.performClick();
        });
        settle();
        onMain(() -> {
            assertEquals(View.GONE, dialogView(R.id.dependency_project_enabled).getVisibility());
            assertTrue(((TextView) dialogView(R.id.dependency_detail_coordinate)).getText().toString().contains("internal-root"));
        });
    }
    @Test public void legacyCacheIsFlatAndMissingFoldersAreNotCountedAsLocal() {
        launch(false);
        scenario.onActivity(a -> {
            assertEquals(View.VISIBLE, a.findViewById(R.id.dependency_relationship_note).getVisibility());
            assertEquals(a.getString(R.string.dependency_browser_summary, 1, 2, 3),
                    ((TextView) a.findViewById(R.id.dependency_summary)).getText().toString());
            assertEquals(5, list(a).getAdapter().getItemCount());
            toggle(a);
            assertEquals(5, list(a).getAdapter().getItemCount());
        });
    }
    @Test public void missingProjectDependencyCannotBeEnabled() {
        launch(true);
        scenario.onActivity(a -> {
            setField(a, "notAssociatedWithProject", false);
            setField(a, "projectUsedLibs", new ArrayList<HashMap<String,Object>>());
            assertTrue(list(a).findViewHolderForAdapterPosition(0).itemView.performLongClick());
        });
        settle();
        onMain(() -> {
            assertFalse(((MaterialSwitch) dialogView(R.id.dependency_project_enabled)).isEnabled());
            assertFalse(((MaterialSwitch) dialogView(R.id.dependency_project_enabled)).isChecked());
            assertFalse(((TextView) dialogView(R.id.dependency_project_reason)).getText().toString().isEmpty());
        });
    }
    @Test public void parentLongPressShowsDetailsWithoutNavigatingInBothViews() {
        for (boolean level : new boolean[]{false, true}) {
            preferences.edit().putBoolean(SubDependenciesActivity.LEVEL_VIEW_PREFERENCE, level).commit();
            launch(true);
            scenario.onActivity(a -> {
                View parent = list(a).findViewHolderForAdapterPosition(0).itemView;
                assertTrue(parent.performLongClick());
                assertEquals(3, list(a).getAdapter().getItemCount());
                assertEquals(View.GONE, a.findViewById(R.id.dependency_path_bar).getVisibility());
                assertTrue(parent.createAccessibilityNodeInfo().getActionList().stream().anyMatch(action ->
                        action.getId() == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK
                                && a.getString(R.string.dependency_browser_details, "branch").contentEquals(action.getLabel())));
            });
            settle();
            onMain(() -> assertEquals("review:branch:1",
                    ((TextView) dialogView(R.id.dependency_detail_coordinate)).getText().toString()));
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            settle();
            scenario.onActivity(a -> list(a).findViewHolderForAdapterPosition(0).itemView.performClick());
            awaitRows(level ? 2 : 5);
            scenario.close();
            scenario = null;
        }
    }
    @Test public void requiredProjectDependencyShowsWhyItCannotBeDisabled() {
        launch(true);
        scenario.onActivity(a -> {
            setField(a, "notAssociatedWithProject", false);
            ArrayList<HashMap<String,Object>> enabled = new ArrayList<>();
            HashMap<String,Object> entry = new HashMap<>();
            entry.put("name", "__dependency_browser_review_leaf");
            enabled.add(entry);
            setField(a, "projectUsedLibs", enabled);
            setField(a, "enabledRootLibrariesBySubDependency", Map.of(
                    "__dependency_browser_review_leaf", List.of("required-root-v1")));
            list(a).findViewHolderForAdapterPosition(1).itemView.performClick();
        });
        settle();
        onMain(() -> {
            MaterialSwitch toggle = (MaterialSwitch) dialogView(R.id.dependency_project_enabled);
            assertTrue(toggle.isChecked());
            assertFalse(toggle.isEnabled());
            assertTrue(((TextView) dialogView(R.id.dependency_project_reason)).getText().toString().contains("required-root-v1"));
        });
    }
    @Test public void cacheReaderPreservesSharedParentsAndExplicitFlatFallback() throws Exception {
        File dir = new File(context.getCacheDir(), "dependency-browser-cache-review");
        assertTrue(dir.mkdir() || dir.isDirectory());
        File tree = new File(dir, "dependency-tree.json");
        try {
            String json = "[{\"groupId\":\"review\",\"artifactId\":\"shared\",\"version\":\"1\","
                    + "\"parents\":[\"review:a:1\",\"review:b:1\"],\"parent\":\"review:a:1\",\"hierarchyKnown\":true,\"builtIn\":true},"
                    + "{\"groupId\":\"review\",\"artifactId\":\"flat\",\"version\":\"1\",\"parent\":\"review:root:1\",\"hierarchyKnown\":false}]";
            Files.write(tree.toPath(), json.getBytes(StandardCharsets.UTF_8));
            LocalLibrary lib = LocalLibrary.fromFile(dir);
            assertEquals(List.of("review:a:1", "review:b:1"), lib.getAllDependencies().get(0).parentCoords);
            assertTrue(lib.getAllDependencies().get(0).hierarchyKnown);
            assertTrue(lib.getAllDependencies().get(0).builtIn);
            assertFalse(lib.getAllDependencies().get(1).hierarchyKnown);
        } finally { Files.deleteIfExists(tree.toPath()); Files.deleteIfExists(dir.toPath()); }
    }
    private void awaitRows(int expected) {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = list(a).getAdapter().getItemCount() == expected);
            if (ready[0]) { settle(); return; }
            try { Thread.sleep(30); } catch (InterruptedException e) { throw new AssertionError(e); }
        }
        fail("Rows did not restore");
    }
    private static RecyclerView list(SubDependenciesActivity a) { return a.findViewById(R.id.dependencies_list); }
    private static MaterialToolbar toolbar(SubDependenciesActivity a) { return a.findViewById(R.id.toolbar); }
    private static void toggle(SubDependenciesActivity a) { toolbar(a).getMenu().performIdentifierAction(R.id.dependency_switch_view, 0); }
    private static void setField(Object target, String name, Object value) {
        try { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target,value); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static View dialogView(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) { View v=root.findViewById(id); if(v!=null)return v; }
        throw new AssertionError("Missing dialog control " + id);
    }
    private static void settle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); }
    private static void onMain(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
}
