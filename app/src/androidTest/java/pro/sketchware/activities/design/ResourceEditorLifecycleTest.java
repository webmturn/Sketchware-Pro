package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.os.Bundle;
import android.os.Parcel;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourceEditorTabs;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.ResourceEditorDraftState;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ResourcesEditorFragmentBinding;

/** Exercises live pager fragments and loading callbacks using a disposable project. */
@RunWith(AndroidJUnit4.class)
public class ResourceEditorLifecycleTest {
    @Test public void incrementalStringEditsSurviveRecreationAndKeepFormalXmlUnchanged() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            byte[] original = Files.readAllBytes(resourceFile(fixture.id,"","strings.xml").toPath());
            String[] session = {null};
            scenario.onActivity(a -> {
                for (int i=0;i<2048;i++) a.stringsEditor.listmap.add(new HashMap<>(java.util.Map.of("key","chunk_"+i,"text","original "+i)));
                a.stringsEditor.markDraftChanged();
                saveForIncrementalTest(a);
                a.stringsEditor.addString("incremental_append","appended","tail note");
                a.stringsEditor.listmap.get(640).put("text","edited <&> 😀");
                a.stringsEditor.markStringChanged(640,false);
                Bundle saved=saveForIncrementalTest(a); session[0]=saved.getString("resource_draft_session");
                File manifest=new File(a.getNoBackupFilesDir(),"resource-editor-drafts/"+session[0]+"/manifest.json");
                try {
                    String text=new String(Files.readAllBytes(manifest.toPath()),StandardCharsets.UTF_8);
                    assertTrue("Small changes must reach the chunked storage path",text.contains("strings-chunks-utf8-v1"));
                } catch(Exception failure) { throw new AssertionError(failure); }
            });
            scenario.recreate(); awaitLoaded(scenario,"base");
            scenario.onActivity(a -> {
                assertEquals("edited <&> 😀",a.stringsEditor.listmap.get(640).get("text"));
                assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "incremental_append".equals(row.get("key"))));
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                a.stringsEditor.listmap.get(900).put("text","after recreate");
                a.stringsEditor.markStringChanged(900,false); saveForIncrementalTest(a);
            });
            scenario.recreate(); awaitLoaded(scenario,"base");
            scenario.onActivity(a -> assertEquals("after recreate",a.stringsEditor.listmap.get(900).get("text")));
            assertArrayEquals(original,Files.readAllBytes(resourceFile(fixture.id,"","strings.xml").toPath()));
        });
    }

    private static Bundle saveForIncrementalTest(ResourcesEditorActivity activity) {
        try {
            Bundle saved=new Bundle();
            Method method=ResourcesEditorActivity.class.getDeclaredMethod("onSaveInstanceState",Bundle.class);
            method.setAccessible(true); method.invoke(activity,saved);
            return saved;
        } catch(Exception failure) { throw new AssertionError(failure); }
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void largeDraftBackgroundAndRepeatedRecreationUseOnlySmallState() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            File xml = resourceFile(fixture.id, "", "strings.xml");
            byte[] original = Files.readAllBytes(xml.toPath());
            scenario.onActivity(a -> {
                String value = new String(new char[100]).replace('\0', 'x');
                for (int i = 0; i < 10000; i++) {
                    HashMap<String, Object> entry = new HashMap<>();
                    entry.put("key", "large_" + i); entry.put("text", value);
                    a.stringsEditor.listmap.add(entry);
                }
                a.stringsEditor.addString("large_unsaved", "kept", "large note");
            });
            Application app = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
            int[] saves = {0};
            Application.ActivityLifecycleCallbacks observer = new EmptyCallbacks() {
                @Override public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    if (!(activity instanceof ResourcesEditorActivity a) || !fixture.id.equals(a.sc_id)) return;
                    assertFalse(state.containsKey("resource_editor_drafts"));
                    assertNotNull(state.getString("resource_draft_session"));
                    Parcel parcel = Parcel.obtain();
                    try { parcel.writeBundle(state); assertTrue("Resource text must not enter saved state", parcel.dataSize() < 128 * 1024); }
                    finally { parcel.recycle(); }
                    saves[0]++;
                }
            };
            app.registerActivityLifecycleCallbacks(observer);
            try {
                scenario.moveToState(Lifecycle.State.CREATED);
                scenario.moveToState(Lifecycle.State.RESUMED);
                scenario.recreate();
                scenario.recreate();
                awaitLoaded(scenario, "base");
                scenario.onActivity(a -> {
                    assertTrue(a.stringsEditor.hasUnsavedChanges);
                    assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "large_unsaved".equals(row.get("key"))));
                    assertTrue(a.stringsEditor.listmap.size() >= 10002);
                });
                assertTrue(saves[0] >= 3);
                assertArrayEquals(original, Files.readAllBytes(xml.toPath()));
            } finally { app.unregisterActivityLifecycleCallbacks(observer); }
        });
    }

    @Test public void failedXmlSaveKeepsDraftAndDoesNotReportClean() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            File original = resourceFile(fixture.id, "", "strings.xml");
            byte[] bytes = Files.readAllBytes(original.toPath());
            File blocked = new File(original.getParentFile(), "blocked-destination");
            assertTrue(blocked.mkdir());
            scenario.onActivity(a -> {
                a.stringsEditor.addString("failed_save_probe", "recoverable", "");
                a.colorsEditor.colorList.get(0).setColorValue("#778899");
                a.colorsEditor.markDraftChanged();
                a.stringsEditor.filePath = blocked.getPath();
                saveFromToolbar(a);
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                assertFalse("Other successful pages become clean", a.colorsEditor.hasUnsavedChanges);
            });
            assertArrayEquals(bytes, Files.readAllBytes(original.toPath()));
            assertTrue(new String(Files.readAllBytes(resourceFile(fixture.id, "", "colors.xml").toPath()), StandardCharsets.UTF_8).contains("#778899"));
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "failed_save_probe".equals(row.get("key"))));
                saveFromToolbar(a);
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
            assertTrue(new String(Files.readAllBytes(original.toPath()), StandardCharsets.UTF_8).contains("failed_save_probe"));
        });
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void legacyInlineDraftMigratesAndSurvivesAnotherRecreation() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> a.stringsEditor.addString("legacy_probe", "legacy", "note"));
            Application app = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
            Application.ActivityLifecycleCallbacks legacy = new EmptyCallbacks() {
                @Override public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    if (!(activity instanceof ResourcesEditorActivity a) || !fixture.id.equals(a.sc_id)) return;
                    Bundle drafts = new Bundle(); drafts.putBundle("0", a.stringsEditor.saveDraftState());
                    state.remove("resource_draft_session"); state.putBundle("resource_editor_drafts", drafts);
                }
            };
            app.registerActivityLifecycleCallbacks(legacy);
            try { scenario.recreate(); } finally { app.unregisterActivityLifecycleCallbacks(legacy); }
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "legacy_probe".equals(row.get("key")))));
        });
    }

    private static class EmptyCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity activity, Bundle state) { }
        @Override public void onActivityStarted(Activity activity) { }
        @Override public void onActivityResumed(Activity activity) { }
        @Override public void onActivityPaused(Activity activity) { }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
        @Override public void onActivityDestroyed(Activity activity) { }
    }

    @Test public void recreationUsesRestoredEditorsAndLoadsEveryTab() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.recreate();
            scenario.onActivity(ResourceEditorLifecycleTest::assertHosts);
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                for (Fragment editor : editors(a)) {
                    assertTrue(ResourcesEditorFragmentBinding.bind(editor.requireView()).recyclerView.getAdapter().getItemCount() > 0);
                }
            });
        });
    }

    @Test public void immediateRecreationStillLoadsEveryEditor() throws Exception {
        withEditor((fixture, scenario) -> {
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(ResourceEditorLifecycleTest::assertHosts);
        });
    }

    @Test @SdkSuppress(minSdkVersion=29)
    public void olderSavedPagerStateStillRestoresLiveEditors() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            // Strip only the new activity reference keys; retain Android's real pager/fragment state.
            int[] stripped = {0};
            Application.ActivityLifecycleCallbacks oldState = new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    if (activity instanceof ResourcesEditorActivity editor && fixture.id.equals(editor.sc_id)) {
                        for (int i = 0; i < ResourceEditorTabs.COUNT; i++) state.remove("resource_editor_" + i);
                        stripped[0]++;
                    }
                }
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
                @Override public void onActivityCreated(Activity activity, Bundle state) { }
                @Override public void onActivityStarted(Activity activity) { }
                @Override public void onActivityResumed(Activity activity) { }
                @Override public void onActivityPaused(Activity activity) { }
                @Override public void onActivityStopped(Activity activity) { }
                @Override public void onActivityDestroyed(Activity activity) { }
            };
            Application[] application = {null};
            scenario.onActivity(a -> {
                application[0] = a.getApplication();
                application[0].registerActivityLifecycleCallbacks(oldState);
            });
            try {
                scenario.recreate();
                assertTrue("Recreation must use a saved state without the new reference keys", stripped[0] > 0);
                scenario.onActivity(ResourceEditorLifecycleTest::assertHosts);
                awaitLoaded(scenario, "base");
            } finally {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> application[0].unregisterActivityLifecycleCallbacks(oldState));
            }
        });
    }

    @Test public void rotatingTheWindowRestoresWorkingEditors() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            awaitOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE);
            awaitLoaded(scenario, "base");
            scenario.onActivity(ResourceEditorLifecycleTest::assertHosts);
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            awaitOrientation(scenario, Configuration.ORIENTATION_PORTRAIT);
            awaitLoaded(scenario, "base");
        });
    }

    @Test public void finishingImmediatelyDoesNotLoadIntoDestroyedEditors() throws Exception {
        withEditor((fixture, scenario) -> {
            ResourcesEditorActivity[] closed = {null};
            int[] count = {0};
            scenario.onActivity(a -> {
                closed[0] = a;
                count[0] = a.stringsEditor.listmap.size();
                a.finish();
            });
            SystemClock.sleep(1300);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertTrue(closed[0].isDestroyed());
                assertEquals("Stopped callbacks must not mutate the former editor", count[0], closed[0].stringsEditor.listmap.size());
            });
        });
    }

    @Test public void rapidVariantChangesKeepEditorsAndLoadOnlyTheLastDirectory() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                Fragment[] before = editors(a);
                Object adapter = ((ViewPager2)a.findViewById(R.id.view_pager)).getAdapter();
                switchVariant(a, "-fr");
                switchVariant(a, "-ja");
                assertSame(adapter, ((ViewPager2)a.findViewById(R.id.view_pager)).getAdapter());
                Fragment[] after = editors(a);
                for (int i = 0; i < before.length; i++) assertSame(before[i], after[i]);
            });
            awaitLoaded(scenario, "ja");
            scenario.onActivity(a -> {
                assertEquals("-ja", a.variant);
                assertTrue(a.stringsEditor.filePath.contains("values-ja"));
                assertEquals(6, a.getSupportFragmentManager().getFragments().size());
            });
        });
    }

    @Test public void selectedVariantAndTabSurviveRecreation() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                switchVariant(a, "-fr");
                ((ViewPager2)a.findViewById(R.id.view_pager)).setCurrentItem(ResourceEditorTabs.DIMENS, false);
            });
            awaitLoaded(scenario, "fr");
            scenario.recreate();
            awaitLoaded(scenario, "fr");
            scenario.onActivity(a -> {
                assertEquals("-fr", a.variant);
                assertEquals(ResourceEditorTabs.DIMENS, ((ViewPager2)a.findViewById(R.id.view_pager)).getCurrentItem());
            });
        });
    }

    @Test public void editingAndSavingAfterRecreationUsesTheVisibleEditor() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                a.stringsEditor.addString("created", "kept", "draft note");
                MaterialToolbar toolbar = a.findViewById(R.id.topAppBar);
                assertNotNull(toolbar.getMenu().findItem(R.id.action_save));
                a.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_save));
            });
            String saved = new String(Files.readAllBytes(resourceFile(fixture.id, "", "strings.xml").toPath()), StandardCharsets.UTF_8);
            assertTrue(saved.contains("created"));
            assertTrue(saved.contains("kept"));
            assertTrue(saved.contains("draft note"));
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "created".equals(row.get("key")))));
        });
    }

    @Test public void returningFromCodeEditorKeepsOtherUnsavedTabs() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                a.colorsEditor.colorList.get(0).setColorValue("#445566");
                a.colorsEditor.markDraftChanged();
                a.isComingFromSrcCodeEditor = true;
            });
            scenario.moveToState(Lifecycle.State.STARTED);
            Files.write(resourceFile(fixture.id, "", "strings.xml").toPath(), strings("edited_in_code").getBytes(StandardCharsets.UTF_8));
            scenario.moveToState(Lifecycle.State.RESUMED);
            awaitLoaded(scenario, "edited_in_code");
            scenario.onActivity(a -> {
                assertTrue(a.colorsEditor.hasUnsavedChanges);
                assertEquals("#445566", a.colorsEditor.colorList.get(0).getColorValue());
            });
        });
    }

    @Test public void allUnsavedDraftsAndCommentsSurviveRecreationAndRotation() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
            byte[][] originalFiles = new byte[names.length][];
            for (int i = 0; i < names.length; i++) originalFiles[i] = Files.readAllBytes(resourceFile(fixture.id, "", names[i]).toPath());
            scenario.onActivity(a -> {
                a.stringsEditor.addString("unsaved_probe", "draft <&> value", "draft note");
                a.colorsEditor.colorList.get(0).setColorValue("#445566");
                a.colorsEditor.markDraftChanged();
                list(a.stylesEditor, "stylesList", StyleModel.class).get(0).addAttribute("android:text", "draft <&> style");
                a.stylesEditor.markDraftChanged();
                list(a.themesEditor, "themesList", StyleModel.class).get(0).setParent("Theme.AppCompat.DayNight.NoActionBar");
                a.themesEditor.markDraftChanged();
                list(a.arraysEditor, "arraysList", ArrayModel.class).get(0).addAttribute("draft_item", "draft <&> array");
                a.arraysEditor.markDraftChanged();
                a.dimensEditor.dimenList.clear();
                a.dimensEditor.markDraftChanged();
                for (Fragment editor : editors(a)) notes(editor).put(0, "draft note <&>\nsecond line");
            });
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(ResourceEditorLifecycleTest::assertDrafts);
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            awaitOrientation(scenario, Configuration.ORIENTATION_LANDSCAPE);
            awaitLoaded(scenario, "base");
            scenario.onActivity(ResourceEditorLifecycleTest::assertDrafts);
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            awaitOrientation(scenario, Configuration.ORIENTATION_PORTRAIT);
            awaitLoaded(scenario, "base");
            scenario.onActivity(ResourceEditorLifecycleTest::assertDrafts);
            for (int i = 0; i < names.length; i++) assertArrayEquals("Recreation must not save " + names[i],
                    originalFiles[i], Files.readAllBytes(resourceFile(fixture.id, "", names[i]).toPath()));
            scenario.onActivity(ResourceEditorLifecycleTest::saveFromToolbar);
            for (String name : names) {
                String xml = new String(Files.readAllBytes(resourceFile(fixture.id, "", name).toPath()), StandardCharsets.UTF_8);
                assertTrue(name + " must save its restored comment", xml.contains("draft note <&>"));
            }
            assertFalse(new String(Files.readAllBytes(resourceFile(fixture.id, "", "dimens.xml").toPath()),
                    StandardCharsets.UTF_8).contains("probe_size"));
            scenario.recreate();
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "unsaved_probe".equals(row.get("key"))));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
                assertFalse(a.colorsEditor.hasUnsavedChanges);
                assertFalse(a.stylesEditor.hasUnsavedChanges);
                assertFalse(a.themesEditor.hasUnsavedChanges);
                assertFalse(a.arraysEditor.hasUnsavedChanges);
                assertFalse(a.dimensEditor.hasUnsavedChanges);
                assertTrue(a.dimensEditor.dimenList.isEmpty());
            });
        });
    }

    @Test public void unsavedVariantDraftRestoresAndSavesOnlyItsDirectory() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            byte[] original = Files.readAllBytes(resourceFile(fixture.id, "", "strings.xml").toPath());
            scenario.onActivity(a -> switchVariant(a, "-fr"));
            awaitLoaded(scenario, "fr");
            scenario.onActivity(a -> a.stringsEditor.addString("unsaved_fr", "fr draft", "fr note"));
            scenario.recreate();
            awaitLoaded(scenario, "fr");
            scenario.onActivity(a -> {
                assertEquals("-fr", a.variant);
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "unsaved_fr".equals(row.get("key"))));
                assertEquals(resourceFile(fixture.id, "-fr", "strings.xml").getPath(), a.stringsEditor.filePath);
                saveFromToolbar(a);
            });
            assertArrayEquals(original, Files.readAllBytes(resourceFile(fixture.id, "", "strings.xml").toPath()));
            assertTrue(new String(Files.readAllBytes(resourceFile(fixture.id, "-fr", "strings.xml").toPath()),
                    StandardCharsets.UTF_8).contains("fr draft"));
        });
    }

    @Test public void codeEditorReturnAcrossRecreationKeepsOtherDrafts() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                a.colorsEditor.colorList.get(0).setColorValue("#445566");
                a.colorsEditor.markDraftChanged();
                a.isComingFromSrcCodeEditor = true;
            });
            Files.write(resourceFile(fixture.id, "", "strings.xml").toPath(), strings("edited_in_code").getBytes(StandardCharsets.UTF_8));
            scenario.recreate();
            awaitLoaded(scenario, "edited_in_code");
            scenario.onActivity(a -> {
                assertTrue(a.colorsEditor.hasUnsavedChanges);
                assertEquals("#445566", a.colorsEditor.colorList.get(0).getColorValue());
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    private static void assertDrafts(ResourcesEditorActivity a) {
        assertTrue(a.stringsEditor.hasUnsavedChanges);
        assertTrue(a.colorsEditor.hasUnsavedChanges);
        assertTrue(a.stylesEditor.hasUnsavedChanges);
        assertTrue(a.themesEditor.hasUnsavedChanges);
        assertTrue(a.arraysEditor.hasUnsavedChanges);
        assertTrue(a.dimensEditor.hasUnsavedChanges);
        assertTrue(a.stringsEditor.listmap.stream().anyMatch(row -> "unsaved_probe".equals(row.get("key"))
                && "draft <&> value".equals(row.get("text"))));
        assertEquals("#445566", a.colorsEditor.colorList.get(0).getColorValue());
        assertEquals("draft <&> style", list(a.stylesEditor, "stylesList", StyleModel.class).get(0).getAttribute("android:text"));
        assertEquals("Theme.AppCompat.DayNight.NoActionBar", list(a.themesEditor, "themesList", StyleModel.class).get(0).getParent());
        assertEquals("draft <&> array", list(a.arraysEditor, "arraysList", ArrayModel.class).get(0).getAttribute("draft_item"));
        assertTrue("Deleting the last resource must remain an empty draft", a.dimensEditor.dimenList.isEmpty());
        for (Fragment editor : editors(a)) assertEquals("draft note <&>\nsecond line", notes(editor).get(0));
    }

    private static void saveFromToolbar(ResourcesEditorActivity a) {
        MaterialToolbar toolbar = a.findViewById(R.id.topAppBar);
        a.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_save));
    }

    @SuppressWarnings("unchecked")
    private static <T> ArrayList<T> list(Object editor, String fieldName, Class<T> type) {
        try {
            Field field = editor.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (ArrayList<T>) field.get(editor);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    @SuppressWarnings("unchecked")
    private static HashMap<Integer, String> notes(Object editor) {
        try {
            Field field = editor.getClass().getDeclaredField("notesMap");
            field.setAccessible(true);
            return (HashMap<Integer, String>) field.get(editor);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    @Test public void searchRequestedWhileReloadingAppliesAfterDataIsReady() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario, "base");
            scenario.onActivity(a -> {
                switchVariant(a, "-fr");
                MaterialToolbar toolbar = a.findViewById(R.id.topAppBar);
                SearchView search = (SearchView)toolbar.getMenu().findItem(R.id.action_search).getActionView();
                search.setQuery("no_such_resource", false);
            });
            awaitLoaded(scenario, "fr");
            scenario.onActivity(a -> assertEquals(0, a.stringsEditor.adapter.getItemCount()));
        });
    }

    private static void awaitLoaded(ActivityScenario<ResourcesEditorActivity> scenario, String value) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] loaded = {false};
            scenario.onActivity(a -> loaded[0] = a.stringsEditor != null && a.stringsEditor.adapter != null
                    && a.colorsEditor.adapter != null && a.stylesEditor.adapter != null && a.themesEditor.adapter != null
                    && a.arraysEditor.adapter != null && a.dimensEditor.adapter != null
                    && a.stringsEditor.listmap.stream().anyMatch(row -> "probe".equals(row.get("key")) && value.equals(row.get("text"))));
            if (loaded[0]) { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); return; }
            SystemClock.sleep(30);
        }
        StringBuilder detail = new StringBuilder();
        scenario.onActivity(a -> {
            detail.append(" lifecycle=").append(a.getLifecycle().getCurrentState());
            for (Fragment fragment : editors(a)) detail.append(" ").append(fragment.getClass().getSimpleName())
                    .append(":added=").append(fragment.isAdded()).append(",view=").append(fragment.getView() != null);
            for (String fieldName : new String[]{"editorsLoaded", "initialLoadPending", "draftController"}) {
                try {
                    Field field = ResourcesEditorActivity.class.getDeclaredField(fieldName); field.setAccessible(true);
                    Object fieldValue = field.get(a); detail.append(" ").append(fieldName).append("=").append(fieldValue);
                    if (fieldName.equals("draftController") && fieldValue != null) {
                        Field opening = fieldValue.getClass().getDeclaredField("opening"); opening.setAccessible(true);
                        detail.append(" opening=").append(opening.get(fieldValue));
                    }
                } catch (Exception failure) { detail.append(failure); }
            }
        });
        fail("All resource editors should load: " + value + detail);
    }

    private static void awaitOrientation(ActivityScenario<ResourcesEditorActivity> scenario, int expected) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = a.getResources().getConfiguration().orientation == expected);
            if (ready[0]) return;
            SystemClock.sleep(30);
        }
        fail("Window did not rotate");
    }

    private static Fragment[] editors(ResourcesEditorActivity a) {
        return new Fragment[]{a.stringsEditor, a.colorsEditor, a.stylesEditor, a.themesEditor, a.arraysEditor, a.dimensEditor};
    }

    private static void assertHosts(ResourcesEditorActivity a) {
        for (Fragment editor : editors(a)) {
            assertSame("Activity must refer to the live restored page", a, editor.getActivity());
            assertTrue(a.getSupportFragmentManager().getFragments().contains(editor));
        }
    }

    private static void switchVariant(ResourcesEditorActivity a, String variant) {
        try {
            Method change = ResourcesEditorActivity.class.getDeclaredMethod("initializeBackgroundTask", String.class);
            change.setAccessible(true);
            change.invoke(a, variant);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    private static File resourceFile(String id, String variant, String file) {
        return new File(SketchwarePaths.getProjectResourceValuesFilePath(id, variant, file));
    }

    private static String strings(String value) {
        return "<resources><!-- probe note --><string name=\"probe\">" + value + "</string></resources>";
    }

    private static void seedResources(String id, String variant, String value) throws Exception {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] content = {strings(value), "<resources><color name=\"probe_color\">#336699</color></resources>",
                "<resources><style name=\"ProbeStyle\"><item name=\"android:textColor\">#336699</item></style></resources>",
                "<resources><style name=\"ProbeTheme\" parent=\"Theme.AppCompat.Light.NoActionBar\"><item name=\"colorPrimary\">#336699</item></style></resources>",
                "<resources><string-array name=\"probe_array\"><item>one</item></string-array></resources>",
                "<resources><dimen name=\"probe_size\">12dp</dimen></resources>"};
        for (int i = 0; i < names.length; i++) {
            File file = resourceFile(id, variant, names[i]);
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
            Files.write(file.toPath(), content[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void withEditor(Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            seedResources(fixture.id, "", "base");
            seedResources(fixture.id, "-fr", "fr");
            seedResources(fixture.id, "-ja", "ja");
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ResourcesEditorActivity.class)
                    .putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                check.run(fixture, scenario);
            }
        }
    }

    private interface Check { void run(ProjectEditorPersistenceTest.Fixture fixture, ActivityScenario<ResourcesEditorActivity> scenario) throws Exception; }
}
