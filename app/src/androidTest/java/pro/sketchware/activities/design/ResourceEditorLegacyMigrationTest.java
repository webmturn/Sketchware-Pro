package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import pro.sketchware.activities.resourceseditor.ResourceEditorDraftState;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.ColorModel;

/**
 * Task 2 regression: the pre-session Bundle is migrated as early as the draft controller opens,
 * is retained when the commit fails, and is released only after a durable commit covers it.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ResourceEditorLegacyMigrationTest {
    private static final String STORE_CLASS =
            "pro.sketchware.activities.resourceseditor.ResourceEditorDraftStore";

    @After
    public void resetFault() {
        try {
            setFault(point -> { });
        } catch (Exception ignored) {
        }
    }

    @Test
    public void migratedLegacyWritesOnlyUuidAndPreservesEverything() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario);
            Map<String, String> before = xmlHashes(fixture.id);
            Bundle legacy = legacyBundle(true, true);
            int legacyBytes = parcelSize(legacy);

            Application app = application();
            Application.ActivityLifecycleCallbacks injector = injectLegacy(fixture.id, legacy, false);
            app.registerActivityLifecycleCallbacks(injector);
            try {
                scenario.recreate();
            } finally {
                unregister(app, injector);
            }
            awaitLoaded(scenario);

            on(scenario, a -> {
                assertTrue("legacy strings must load", hasString(a, "legacy_only"));
                assertEquals("legacy note", notes(a).get(1));
                assertEquals("#112233", a.colorsEditor.colorList.get(0).getColorValue());
                assertTrue("an empty legacy page stays a dirty empty page",
                        a.dimensEditor.dimenList.isEmpty() && a.dimensEditor.hasUnsavedChanges);
                assertTrue(a.stringsEditor.hasUnsavedChanges);
                assertTrue(a.colorsEditor.hasUnsavedChanges);

                Bundle state = savedState(a);
                assertFalse("a migrated Bundle must not be written back",
                        state.containsKey("resource_editor_drafts"));
                assertNotNull("the migrated session must publish a UUID",
                        state.getString("resource_draft_session"));
                int bytes = parcelSize(state);
                System.out.println("legacy-parcel-bytes=" + legacyBytes + " state-parcel-bytes=" + bytes);
                assertTrue("saved state must stay small, was " + bytes + " (legacy " + legacyBytes + ")",
                        bytes < 64 * 1024);
            });
            assertEquals("migration must not change project XML", before, xmlHashes(fixture.id));

            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue("the migrated session must survive a second recreation", hasString(a, "legacy_only"));
                assertEquals("legacy note", notes(a).get(1));
                assertEquals("#112233", a.colorsEditor.colorList.get(0).getColorValue());
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_editor_drafts"));
                assertNotNull(state.getString("resource_draft_session"));
            });
            assertEquals(before, xmlHashes(fixture.id));
        });
    }

    @Test
    public void failedMigrationRetainsBundleAndRetriesToUuid() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario);
            Bundle legacy = legacyBundle(true, false);
            setFault(point -> {
                if ("before_manifest".equals(point)) {
                    throw new RuntimeException("injected legacy migration failure");
                }
            });
            Application app = application();
            Application.ActivityLifecycleCallbacks injector = injectLegacy(fixture.id, legacy, false);
            app.registerActivityLifecycleCallbacks(injector);
            try {
                scenario.recreate();
            } finally {
                unregister(app, injector);
            }
            awaitLoaded(scenario);

            on(scenario, a -> {
                assertTrue("legacy content must still load", hasString(a, "legacy_only"));
                Bundle state = savedState(a);
                assertTrue("the original Bundle must be retained", state.containsKey("resource_editor_drafts"));
                assertNull("an uncovered legacy Bundle must not publish a new UUID",
                        state.getString("resource_draft_session"));
                Bundle again = savedState(a);
                assertTrue("persistent failure must keep the retry source",
                        again.containsKey("resource_editor_drafts"));
                assertNull(again.getString("resource_draft_session"));
            });

            setFault(point -> { });
            on(scenario, a -> assertTrue("a successful protect covers the loaded legacy pages",
                    (Boolean) call(a, "protectDrafts")));
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertFalse("successful protection releases the Bundle",
                        state.containsKey("resource_editor_drafts"));
                assertNotNull("successful protection publishes the session UUID",
                        state.getString("resource_draft_session"));
            });

            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue(hasString(a, "legacy_only"));
                assertTrue("loaded legacy content stays dirty until saved", a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    @Test
    public void unreadableLegacyPageKeepsMaterialAndMigratesReadablePages() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario);
            Bundle legacy = legacyBundle(true, false);
            Bundle corrupt = new Bundle();
            corrupt.putString("entries", "not json");
            corrupt.putString("notes", "{}");
            legacy.putBundle("1", corrupt);

            Application app = application();
            Application.ActivityLifecycleCallbacks injector = injectLegacy(fixture.id, legacy, false);
            app.registerActivityLifecycleCallbacks(injector);
            try {
                scenario.recreate();
            } finally {
                unregister(app, injector);
            }
            awaitLoaded(scenario);

            on(scenario, a -> {
                assertTrue("readable pages must migrate", hasString(a, "legacy_only"));
                assertTrue("the unreadable colors page must not be applied",
                        a.colorsEditor.colorList.stream().noneMatch(
                                color -> "legacy_color".equals(color.getColorName())));
                assertTrue("the saved colors XML must be loaded instead",
                        a.colorsEditor.colorList.stream().anyMatch(color ->
                                "probe_color".equals(color.getColorName())
                                        && "#336699".equals(color.getColorValue())));
                Bundle state = savedState(a);
                assertFalse("readable pages are covered, so the valid Bundle is released",
                        state.containsKey("resource_editor_drafts"));
                assertFalse("raw diagnostics must leave system state", state.containsKey("resource_legacy_corrupt"));
                assertEquals("not json", archivedPages(state, "1", "not json").getBundle("1").getString("entries"));
                assertNotNull(state.getString("resource_draft_session"));
            });

            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue(hasString(a, "legacy_only"));
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_editor_drafts"));
                assertNotNull("the diagnostic reference survives another recreation",
                        state.getString("resource_legacy_material"));
                assertNotNull(archivedPages(state, "1", "not json"));
            });
        });
    }

    @Test
    public void existingNewerDraftWinsOverLegacyBundle() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario);
            String[] session = new String[1];
            on(scenario, a -> {
                a.stringsEditor.addString("newer_b", "newer", "newer note");
                assertTrue((Boolean) call(a, "protectDrafts"));
                session[0] = savedState(a).getString("resource_draft_session");
                assertNotNull(session[0]);
            });

            Bundle legacy = legacyBundle(true, false);
            Application app = application();
            Application.ActivityLifecycleCallbacks injector = injectLegacy(fixture.id, legacy, true);
            app.registerActivityLifecycleCallbacks(injector);
            try {
                scenario.recreate();
            } finally {
                unregister(app, injector);
            }
            awaitLoaded(scenario);

            on(scenario, a -> {
                assertTrue("the newer session draft must win", hasString(a, "newer_b"));
                assertFalse("the old Bundle must not be applied", hasString(a, "legacy_only"));
                Bundle state = savedState(a);
                assertFalse("the superseded Bundle must be released",
                        state.containsKey("resource_editor_drafts"));
                assertNotNull(state.getString("resource_draft_session"));
            });
        });
    }

    @Test
    public void provenXmlWinsOverLegacyOnRestore() throws Exception {
        withEditor((fixture, scenario) -> {
            awaitLoaded(scenario);
            File xml = new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, "strings.xml"));
            on(scenario, a -> {
                a.stringsEditor.addString("draft_a", "A", "");
                assertTrue((Boolean) call(a, "protectDrafts"));
                assertNotNull(savedState(a).getString("resource_draft_session"));
            });
            setFault(point -> {
                if ("before_manifest".equals(point) || "before_close_manifest".equals(point)) {
                    throw new RuntimeException("injected draft/close failure");
                }
            });
            on(scenario, a -> {
                a.stringsEditor.addString("saved_b", "B", "");
                ResourceEditorSaveResult failed =
                        (ResourceEditorSaveResult) call(a, "saveEditorsChanges");
                assertTrue(failed.canSaveXmlOnly);
                callWithNull(a, "performXmlOnlySave", Runnable.class);
            });
            assertTrue(read(xml).contains("saved_b"));

            Bundle legacy = new Bundle();
            legacy.putBundle("0", ResourceEditorDraftState.capture(
                    List.of(new HashMap<>(Map.of("key", "legacy_a", "text", "A"))), Map.of()));
            Application app = application();
            // Keep the session UUID: the intent inside it proves XML B for the page; the legacy
            // Bundle must not override that proven XML on restore.
            Application.ActivityLifecycleCallbacks injector = new EmptyCallbacks() {
                @Override
                public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    if (!(activity instanceof ResourcesEditorActivity editor)
                            || !fixture.id.equals(editor.sc_id)) return;
                    state.putBundle("resource_editor_drafts", legacy);
                }
            };
            app.registerActivityLifecycleCallbacks(injector);
            try {
                scenario.recreate();
            } finally {
                unregister(app, injector);
            }
            setFault(point -> { });
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue("proven XML B must win", hasString(a, "saved_b"));
                assertFalse("legacy A must not resurrect", hasString(a, "legacy_a"));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
                Bundle state = savedState(a);
                assertFalse("the superseded Bundle must be released",
                        state.containsKey("resource_editor_drafts"));
            });
        });
    }

    @Test public void largeFailedLegacyStaysInMemoryUntilSuccessfulProtection() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = new Bundle();
            legacy.putBundle("0", ResourceEditorDraftState.capture(
                    List.of(new HashMap<>(Map.of("key", "large_legacy", "text", "small"))),
                    Map.of(1, "x".repeat(300 * 1024))));
            setFault(point -> { if ("before_manifest".equals(point)) throw new RuntimeException("large legacy failure"); });
            recreateWithLegacy(fixture.id, scenario, legacy);
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertTrue(parcelSize(state) < 64 * 1024);
                assertFalse(state.containsKey("resource_editor_drafts"));
                assertNull(state.getString("resource_draft_session"));
                assertTrue(state.getBoolean("resource_legacy_unprotected"));
                Field pending = ResourcesEditorActivity.class.getDeclaredField("pendingDrafts");
                pending.setAccessible(true);
                assertNotNull("over-budget content must remain available to retry", pending.get(a));
                assertEquals(300 * 1024, notes(a).get(1).length());
            });
            setFault(point -> { });
            on(scenario, a -> {
                assertTrue((Boolean) call(a, "protectDrafts"));
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_editor_drafts"));
                assertFalse(state.getBoolean("resource_legacy_unprotected"));
                assertNotNull(state.getString("resource_draft_session"));
                assertTrue(parcelSize(state) < 64 * 1024);
            });
            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                assertTrue(hasString(a, "large_legacy"));
                assertEquals(300 * 1024, notes(a).get(1).length());
            });
        });
    }

    @Test public void multipleLargeCorruptPagesAreArchivedOutsideSavedState() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = legacyBundle(true, false);
            String broken = " ".repeat(150 * 1024) + "{";
            for (String page : List.of("1", "2")) {
                Bundle bad = new Bundle();
                bad.putString("entries", broken);
                bad.putString("notes", "{}");
                legacy.putBundle(page, bad);
            }
            recreateWithLegacy(fixture.id, scenario, legacy);
            on(scenario, a -> {
                assertTrue((Boolean) call(a, "protectDrafts"));
                Bundle state = savedState(a);
                assertTrue(parcelSize(state) < 64 * 1024);
                assertFalse(state.containsKey("resource_legacy_corrupt"));
                Bundle pages = archivedPages(state, "1", broken);
                assertEquals(broken, pages.getBundle("2").getString("entries"));
                assertNotNull(state.getString("resource_draft_session"));
            });
            scenario.recreate();
            awaitLoaded(scenario);
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertTrue(hasString(a, "legacy_only"));
                assertNotNull(archivedPages(state, "1", broken));
                assertFalse(state.containsKey("resource_legacy_corrupt"));
                assertTrue(parcelSize(state) < 64 * 1024);
            });
        });
    }

    @Test public void failedArchivalUsesOnlyBudgetedSmallCompatibilityInput() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = legacyBundle(true, false);
            Bundle bad = new Bundle(); bad.putString("entries", "not json"); bad.putString("notes", "{}");
            legacy.putBundle("1", bad);
            setFault(point -> { if ("before_legacy_material".equals(point)) throw new RuntimeException("archive failure"); });
            recreateWithLegacy(fixture.id, scenario, legacy);
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertEquals("not json", state.getBundle("resource_legacy_corrupt").getBundle("1").getString("entries"));
                assertFalse(state.getBoolean("resource_legacy_material_unprotected"));
                assertTrue(parcelSize(state) < 48 * 1024);
                assertNotNull(state.getString("resource_draft_session"));
            });
            setFault(point -> { });
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_legacy_corrupt"));
                assertNotNull(archivedPages(state, "1", "not json"));
            });
        });
    }

    @Test public void largeArchivalFailureRetainsMemoryAndMarksOmissionUntilRetry() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = legacyBundle(true, false);
            String broken = " ".repeat(300 * 1024) + "{";
            Bundle bad = new Bundle(); bad.putString("entries", broken); bad.putString("notes", "{}");
            legacy.putBundle("1", bad);
            setFault(point -> { if ("before_legacy_material".equals(point)) throw new RuntimeException("archive failure"); });
            recreateWithLegacy(fixture.id, scenario, legacy);
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_legacy_corrupt"));
                assertTrue(state.getBoolean("resource_legacy_material_unprotected"));
                assertNotNull("the readable page has its own real checkpoint", state.getString("resource_draft_session"));
                assertTrue(parcelSize(state) < 64 * 1024);
                Field pending = ResourcesEditorActivity.class.getDeclaredField("pendingCorruptDrafts");
                pending.setAccessible(true);
                assertEquals(broken, ((Bundle) pending.get(a)).getBundle("1").getString("entries"));
            });
            setFault(point -> { });
            on(scenario, a -> {
                Bundle state = savedState(a);
                assertFalse(state.getBoolean("resource_legacy_material_unprotected"));
                assertFalse(state.containsKey("resource_legacy_corrupt"));
                assertNotNull(archivedPages(state, "1", broken));
            });
        });
    }

    @Test public void typedCorruptStringsDoNotPreventReadableColorsFromMigrating() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = legacyBundle(false, true);
            String broken = "[{\"key\":\"bad\",\"text\":{\"nested\":\"value\"}}]";
            Bundle bad = new Bundle(); bad.putString("entries", broken); bad.putString("notes", "{}");
            legacy.putBundle("0", bad);
            recreateWithLegacy(fixture.id, scenario, legacy);
            on(scenario, a -> {
                assertFalse(hasString(a, "bad"));
                assertTrue(hasString(a, "app_name"));
                assertFalse(a.stringsEditor.hasUnsavedChanges);
                assertEquals("#112233", a.colorsEditor.colorList.get(0).getColorValue());
                assertTrue(a.colorsEditor.hasUnsavedChanges);
                Bundle state = savedState(a);
                assertFalse(state.containsKey("resource_editor_drafts"));
                assertNotNull(archivedPages(state, "0", broken));
                assertNotNull(state.getString("resource_draft_session"));
            });
        });
    }

    @Test public void repeatedSavesBeforeViewsLoadRetainSourceAndRetry() throws Exception {
        withEditor((fixture, scenario) -> {
            Bundle legacy = legacyBundle(true, true);
            setFault(point -> { if ("before_manifest".equals(point)) throw new RuntimeException("early save failure"); });
            java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
            boolean[] observed = {false};
            Application app = application();
            Application.ActivityLifecycleCallbacks callbacks = new EmptyCallbacks() {
                @Override public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                    if (activity instanceof ResourcesEditorActivity a && fixture.id.equals(a.sc_id)) {
                        state.remove("resource_draft_session"); state.putBundle("resource_editor_drafts", legacy);
                    }
                }
                @Override public void onActivityPostCreated(Activity activity, Bundle input) {
                    if (!(activity instanceof ResourcesEditorActivity a) || !fixture.id.equals(a.sc_id)
                            || input == null || !input.containsKey("resource_editor_drafts")) return;
                    observed[0] = true;
                    try {
                        Field loaded = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
                        loaded.setAccessible(true);
                        assertFalse(loaded.getBoolean(a));
                        for (int i = 0; i < 2; i++) {
                            Bundle state = savedState(a);
                            assertNotNull(state.getBundle("resource_editor_drafts"));
                            assertNull(state.getString("resource_draft_session"));
                        }
                        setFault(point -> { });
                        Bundle state = savedState(a);
                        assertFalse(state.containsKey("resource_editor_drafts"));
                        assertNotNull(state.getString("resource_draft_session"));
                        assertFalse(loaded.getBoolean(a));
                    } catch (Throwable error) { failure.set(error); }
                }
            };
            app.registerActivityLifecycleCallbacks(callbacks);
            try { scenario.recreate(); } finally { unregister(app, callbacks); }
            assertTrue(observed[0]);
            if (failure.get() != null) throw new AssertionError(failure.get());
            awaitLoaded(scenario);
            on(scenario, a -> { assertTrue(hasString(a, "legacy_only")); assertEquals("legacy note", notes(a).get(1)); });
        });
    }

    private static void recreateWithLegacy(String id, ActivityScenario<ResourcesEditorActivity> scenario,
                                           Bundle legacy) throws Exception {
        Application app = application();
        Application.ActivityLifecycleCallbacks callbacks = injectLegacy(id, legacy, false);
        app.registerActivityLifecycleCallbacks(callbacks);
        try { scenario.recreate(); } finally { unregister(app, callbacks); }
        awaitLoaded(scenario);
    }

    private static Bundle archivedPages(Bundle state, String page, String expectedEntries) throws Exception {
        String identity = state.getString("resource_legacy_material");
        assertNotNull("durable diagnostic reference", identity);
        assertTrue(identity.matches("[0-9a-f]{64}"));
        File root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "resource-editor-drafts/legacy-material/" + identity);
        File[] files = root.listFiles((dir, name) -> name.matches("[0-9a-f]{64}\\.parcel"));
        assertNotNull(files);
        for (File file : files) {
            Parcel parcel = Parcel.obtain();
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                parcel.unmarshall(bytes, 0, bytes.length); parcel.setDataPosition(0);
                Bundle material = parcel.readBundle(ResourceEditorLegacyMigrationTest.class.getClassLoader());
                Bundle pages = material.getBundle("pages");
                Bundle candidate = pages.getBundle(page);
                if (candidate != null && expectedEntries.equals(candidate.getString("entries"))) return pages;
            } finally { parcel.recycle(); }
        }
        throw new AssertionError("Original diagnostic material not found");
    }

    private static Bundle legacyBundle(boolean includeStrings, boolean includeEmptyPage) {
        Bundle drafts = new Bundle();
        if (includeStrings) {
            drafts.putBundle("0", ResourceEditorDraftState.capture(
                    List.of(new HashMap<>(Map.of("key", "legacy_only", "text", "legacy value"))),
                    Map.of(1, "legacy note")));
        }
        if (includeEmptyPage) {
            drafts.putBundle("1", ResourceEditorDraftState.capture(
                    List.of(new ColorModel("legacy_color", "#112233")), Map.of()));
            drafts.putBundle("5", ResourceEditorDraftState.capture(List.of(), Map.of()));
        }
        return drafts;
    }

    private static Application application() {
        return (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getApplicationContext();
    }

    private static Application.ActivityLifecycleCallbacks injectLegacy(
            String projectId, Bundle legacy, boolean keepSession) {
        return new EmptyCallbacks() {
            @Override
            public void onActivityPostSaveInstanceState(Activity activity, Bundle state) {
                if (!(activity instanceof ResourcesEditorActivity editor)
                        || !projectId.equals(editor.sc_id)) return;
                if (!keepSession) state.remove("resource_draft_session");
                state.putBundle("resource_editor_drafts", legacy);
            }
        };
    }

    private static void unregister(Application app, Application.ActivityLifecycleCallbacks callbacks) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> app.unregisterActivityLifecycleCallbacks(callbacks));
    }

    private static Bundle savedState(ResourcesEditorActivity activity) throws Exception {
        Bundle state = new Bundle();
        Method method = ResourcesEditorActivity.class.getDeclaredMethod("onSaveInstanceState", Bundle.class);
        method.setAccessible(true);
        method.invoke(activity, state);
        return state;
    }

    private static int parcelSize(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            return parcel.dataSize();
        } finally {
            parcel.recycle();
        }
    }

    private static Map<String, String> xmlHashes(String id) throws Exception {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        Map<String, String> hashes = new HashMap<>();
        for (String name : names) {
            byte[] bytes = Files.readAllBytes(new File(
                    SketchwarePaths.getProjectResourceValuesFilePath(id, name)).toPath());
            hashes.put(name, digest(bytes));
        }
        return hashes;
    }

    private static String digest(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, String> notes(ResourcesEditorActivity activity) throws Exception {
        Field field = Class.forName(
                "pro.sketchware.activities.resourceseditor.components.fragments.StringsEditor")
                .getDeclaredField("notesMap");
        field.setAccessible(true);
        return (Map<Integer, String>) field.get(activity.stringsEditor);
    }

    private static boolean hasString(ResourcesEditorActivity activity, String key) {
        return activity.stringsEditor.listmap.stream().anyMatch(row -> key.equals(row.get("key")));
    }

    private static void on(ActivityScenario<ResourcesEditorActivity> scenario, Action action) {
        scenario.onActivity(activity -> {
            try {
                action.run(activity);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    private interface Action {
        void run(ResourcesEditorActivity activity) throws Exception;
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object callWithNull(Object target, String name, Class<?> parameter) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        return method.invoke(target, new Object[]{null});
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void awaitLoaded(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception {
        Field loaded = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
        loaded.setAccessible(true);
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            on(scenario, a -> {
                try {
                    ready[0] = loaded.getBoolean(a) && !a.stringsEditor.listmap.isEmpty();
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            if (ready[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("Editors did not load");
    }

    private interface Check {
        void run(ProjectEditorPersistenceTest.Fixture fixture,
                 ActivityScenario<ResourcesEditorActivity> scenario) throws Exception;
    }

    private void withEditor(Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            seedResources(fixture.id);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    ResourcesEditorActivity.class).putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitLoaded(scenario);
                check.run(fixture, scenario);
            }
        }
    }

    private static void seedResources(String id) throws Exception {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] content = {
                "<resources><string name=\"app_name\">LegacyTest</string><string name=\"probe\">base</string></resources>",
                "<resources><color name=\"probe_color\">#336699</color></resources>",
                "<resources><style name=\"ProbeStyle\"><item name=\"android:textColor\">#336699</item></style></resources>",
                "<resources><style name=\"ProbeTheme\" parent=\"Theme.AppCompat.Light.NoActionBar\"><item name=\"colorPrimary\">#336699</item></style></resources>",
                "<resources><string-array name=\"probe_array\"><item>one</item></string-array></resources>",
                "<resources><dimen name=\"probe_size\">12dp</dimen></resources>"
        };
        for (int i = 0; i < names.length; i++) {
            File file = new File(SketchwarePaths.getProjectResourceValuesFilePath(id, names[i]));
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
            Files.write(file.toPath(), content[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void setFault(Consumer<String> fault) throws Exception {
        Field field = Class.forName(STORE_CLASS).getDeclaredField("testFault");
        field.setAccessible(true);
        field.set(null, fault);
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
}
