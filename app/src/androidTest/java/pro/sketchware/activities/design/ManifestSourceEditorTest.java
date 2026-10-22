package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.rosemoe.sora.widget.CodeEditor;
import pro.sketchware.R;
import pro.sketchware.activities.editor.code.SrcCodeEditor;
import pro.sketchware.core.project.SketchwarePaths;

/** Manifest source loading/saving in real Activities, using only disposable projects and cache files. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ManifestSourceEditorTest {
    private static final String INITIAL = "initial source";
    private static final String EDITED = "<!-- edited 活动内容 -->";
    private static final String MIXED = "[null,{\"name\":\"OtherActivity\",\"value\":\"keep other\","
            + "\"extra\":{\"keep\":true}},{\"name\":\"MainActivity\",\"value\":\"old main\","
            + "\"extra\":\"keep main\"},null]";
    private SharedPreferences preferences;
    private boolean hadSize;
    private int oldSize, oldLanguage;

    @Before public void preservePreferences() {
        preferences = context().getSharedPreferences("code_editor_settings", Context.MODE_PRIVATE);
        hadSize = preferences.contains("src_text_size");
        oldSize = preferences.getInt("src_text_size", 14);
        oldLanguage = SrcCodeEditor.languageId;
    }

    @After public void restorePreferences() {
        SharedPreferences.Editor changes = preferences.edit();
        if (hadSize) changes.putInt("src_text_size", oldSize); else changes.remove("src_text_size");
        assertTrue(changes.commit());
        SrcCodeEditor.languageId = oldLanguage;
    }

    // These four methods are also run on the old APK. A normal launch followed by setting the
    // existing private mode fields reaches the actual save method without an unhandled onCreate NPE.
    @Test public void savingAnEmptyDocumentCreatesCurrentActivity() throws Exception {
        assertInitiallyEmptySave("");
    }

    @Test public void savingJsonNullCreatesCurrentActivity() throws Exception {
        assertInitiallyEmptySave("null");
    }

    private static void assertInitiallyEmptySave(String original) throws Exception {
        try (var fixture = new Fixture()) {
            fixture.write(original);
            try (var scenario = fixture.launch(false)) {
                switchToManifestSave(scenario, fixture);
                editAndSave(scenario, EDITED);
                assertCurrent(fixture, EDITED);
                check(scenario, activity -> assertEquals(EDITED, field(activity, "beforeContent")));
            }
        }
    }

    @Test public void savingNullEntriesPreservesOtherActivitiesAndUnknownFields() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.write(MIXED);
            try (var scenario = fixture.launch(false)) {
                switchToManifestSave(scenario, fixture);
                editAndSave(scenario, EDITED);
                assertMixedPreserved(fixture, EDITED);
            }
        }
    }

    @Test public void malformedSaveKeepsOriginalAndUnsavedExitWarning() throws Exception {
        try (var fixture = new Fixture()) {
            String original = "[{broken";
            fixture.write(original);
            try (var scenario = fixture.launch(false)) {
                switchToManifestSave(scenario, fixture);
                editAndSave(scenario, EDITED);
                assertEquals(original, fixture.read());
                check(scenario, activity -> {
                    assertEquals(INITIAL, field(activity, "beforeContent"));
                    assertEquals(EDITED, editor(activity).getText().toString());
                    activity.getOnBackPressedDispatcher().onBackPressed();
                });
                String warning = context().getString(R.string.src_code_editor_unsaved_changes_dialog_warning_message);
                ProjectEditorPersistenceTest.waitUntil(() -> hasWindowText(warning));
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    for (View root : WindowInspector.getGlobalWindowViews()) {
                        View cancel = root.findViewById(android.R.id.button2);
                        if (cancel != null && cancel.isShown()) cancel.performClick();
                    }
                });
            }
        }
    }

    @Test public void emptyVariantsOpenWithoutRewritingAndReloadSavedContent() throws Exception {
        for (String original : new String[]{"", " \r\n\t ", "null", "[null]", "[]"}) {
            try (var fixture = new Fixture()) {
                fixture.write(original);
                try (var scenario = fixture.launch(true)) {
                    check(scenario, activity -> assertEquals("", editor(activity).getText().toString()));
                    assertEquals(original, fixture.read());
                    editAndSave(scenario, EDITED);
                    assertCurrent(fixture, EDITED);
                    scenario.recreate();
                    check(scenario, activity -> {
                        keepAwake(activity);
                        assertEquals(EDITED, editor(activity).getText().toString());
                    });
                }
            }
        }
    }

    @Test public void mixedEntriesOpenAndRecreateWithoutDroppingOtherContent() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.write(MIXED);
            try (var scenario = fixture.launch(true)) {
                check(scenario, activity -> assertEquals("old main", editor(activity).getText().toString()));
                scenario.recreate();
                check(scenario, activity -> {
                    keepAwake(activity);
                    assertEquals("old main", editor(activity).getText().toString());
                });
                assertEquals(MIXED, fixture.read());
                editAndSave(scenario, EDITED);
                assertMixedPreserved(fixture, EDITED);
            }
        }
    }

    @Test public void missingFileIsCreatedOnlyByExplicitSave() throws Exception {
        try (var fixture = new Fixture(); var scenario = fixture.launch(true)) {
            assertFalse(fixture.config.exists());
            check(scenario, activity -> assertEquals("", editor(activity).getText().toString()));
            editAndSave(scenario, EDITED);
            assertCurrent(fixture, EDITED);
        }
    }

    @Test public void saveRereadsOtherActivitiesAddedWhileEditorWasOpen() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.write("[{\"name\":\"MainActivity\",\"value\":\"old main\"}]");
            try (var scenario = fixture.launch(true)) {
                fixture.write(MIXED);
                editAndSave(scenario, EDITED);
                assertMixedPreserved(fixture, EDITED);
            }
        }
    }

    @Test public void unreadableConfigurationIsNotTreatedAsEmptyAndCanRetry() throws Exception {
        try (var fixture = new Fixture()) {
            assertTrue(fixture.config.mkdir());
            File marker = new File(fixture.config, "preserve-this-marker");
            Files.write(marker.toPath(), "keep".getBytes(StandardCharsets.UTF_8));
            try (var scenario = fixture.launch(true)) {
                editAndSave(scenario, EDITED);
                assertTrue(fixture.config.isDirectory());
                assertEquals("keep", new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8));
                check(scenario, activity -> {
                    assertEquals("", field(activity, "beforeContent"));
                    assertEquals(EDITED, editor(activity).getText().toString());
                });
                Files.delete(marker.toPath());
                Files.delete(fixture.config.toPath());
                fixture.write("[]");
                check(scenario, SrcCodeEditor::save);
                assertCurrent(fixture, EDITED);
            }
        }
    }

    @Test public void wrongRootAndInvalidEntriesAreNotSilentlyReplaced() throws Exception {
        for (String original : new String[]{"{}", "[17]", "[\"invalid entry\"]"}) {
            try (var fixture = new Fixture()) {
                fixture.write(original);
                try (var scenario = fixture.launch(false)) {
                    switchToManifestSave(scenario, fixture);
                    editAndSave(scenario, EDITED);
                    assertEquals(original, fixture.read());
                    check(scenario, activity -> assertEquals(INITIAL, field(activity, "beforeContent")));
                }
            }
        }
    }

    @Test public void parseFailureOnOpenKeepsNewTextUntilConfigurationIsRepaired() throws Exception {
        try (var fixture = new Fixture()) {
            String original = "[{broken";
            fixture.write(original);
            try (var scenario = fixture.launch(true)) {
                editAndSave(scenario, EDITED);
                assertEquals(original, fixture.read());
                check(scenario, activity -> {
                    assertEquals("", field(activity, "beforeContent"));
                    assertEquals(EDITED, editor(activity).getText().toString());
                });
                fixture.write(MIXED);
                check(scenario, SrcCodeEditor::save);
                assertMixedPreserved(fixture, EDITED);
                check(scenario, activity -> assertEquals(EDITED, field(activity, "beforeContent")));
            }
        }
    }

    @Test public void ordinarySourceFileStillSavesNormally() throws Exception {
        try (var fixture = new Fixture(); var scenario = fixture.launch(false)) {
            editAndSave(scenario, EDITED);
            assertEquals(EDITED, new String(Files.readAllBytes(fixture.source.toPath()), StandardCharsets.UTF_8));
            assertFalse(fixture.config.exists());
            check(scenario, activity -> assertEquals(EDITED, field(activity, "beforeContent")));
        }
    }

    private static void assertCurrent(Fixture fixture, String content) throws Exception {
        JsonArray entries = JsonParser.parseString(fixture.read()).getAsJsonArray();
        int matches = 0;
        for (var entry : entries) {
            if (entry.isJsonNull()) continue;
            if ("MainActivity".equals(entry.getAsJsonObject().get("name").getAsString())) {
                assertEquals(content, entry.getAsJsonObject().get("value").getAsString());
                matches++;
            }
        }
        assertEquals(1, matches);
    }

    private static void assertMixedPreserved(Fixture fixture, String content) throws Exception {
        JsonArray before = JsonParser.parseString(MIXED).getAsJsonArray();
        JsonArray after = JsonParser.parseString(fixture.read()).getAsJsonArray();
        assertEquals(4, after.size());
        assertEquals(before.get(0), after.get(0));
        assertEquals(before.get(1), after.get(1));
        assertEquals(before.get(3), after.get(3));
        assertEquals("keep main", after.get(2).getAsJsonObject().get("extra").getAsString());
        assertCurrent(fixture, content);
    }

    private static void editAndSave(ActivityScenario<SrcCodeEditor> scenario, String content) throws Exception {
        check(scenario, activity -> { editor(activity).setText(content); activity.save(); });
    }

    private static void switchToManifestSave(ActivityScenario<SrcCodeEditor> scenario, Fixture fixture) throws Exception {
        check(scenario, activity -> {
            field(activity, "fromAndroidManifest", true);
            field(activity, "scId", fixture.project.id);
            field(activity, "activityName", "MainActivity");
        });
    }

    private static CodeEditor editor(SrcCodeEditor activity) { return activity.findViewById(R.id.editor); }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static void keepAwake(SrcCodeEditor activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private static boolean hasWindowText(String text) {
        AtomicBoolean found = new AtomicBoolean();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (View root : WindowInspector.getGlobalWindowViews()) if (containsText(root, text)) found.set(true);
        });
        return found.get();
    }

    private static boolean containsText(View view, String text) {
        if (view instanceof TextView label && text.contentEquals(label.getText()) && view.isShown()) return true;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = SrcCodeEditor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void field(Object target, String name, Object value) throws Exception {
        Field field = SrcCodeEditor.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private interface Check { void run(SrcCodeEditor activity) throws Exception; }
    private static void check(ActivityScenario<SrcCodeEditor> scenario, Check check) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        scenario.onActivity(activity -> {
            try { check.run(activity); } catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Exception exception) throw exception;
        if (error != null) throw new AssertionError(error);
    }

    private static final class Fixture implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture project;
        final File config;
        final File source;

        Fixture() throws Exception {
            project = new ProjectEditorPersistenceTest.Fixture();
            try {
                project.seed(false, false);
                config = new File(SketchwarePaths.getAndroidManifestActivitiesComponentsPath(project.id));
                assertTrue(config.getCanonicalPath().startsWith(project.data.getCanonicalPath() + File.separator));
                Files.createDirectories(config.toPath().getParent());
                source = File.createTempFile("manifest-source-regression-", ".txt", context().getCacheDir());
                Files.write(source.toPath(), INITIAL.getBytes(StandardCharsets.UTF_8));
            } catch (Exception | AssertionError error) { project.close(); throw error; }
        }

        ActivityScenario<SrcCodeEditor> launch(boolean manifest) throws Exception {
            Intent intent = new Intent(context(), SrcCodeEditor.class).putExtra("content", source.getAbsolutePath())
                    .putExtra("title", "ManifestSourceTest").putExtra("sc_id", project.id)
                    .putExtra("activity_name", "MainActivity")
                    .putExtra(SrcCodeEditor.FLAG_FROM_ANDROID_MANIFEST, manifest);
            ActivityScenario<SrcCodeEditor> scenario = ActivityScenario.launch(intent);
            check(scenario, ManifestSourceEditorTest::keepAwake);
            return scenario;
        }

        void write(String content) throws Exception {
            Files.write(config.toPath(), content.getBytes(StandardCharsets.UTF_8));
        }

        String read() throws Exception { return new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8); }

        @Override public void close() throws Exception {
            try { Files.deleteIfExists(source.toPath()); } finally { project.close(); }
        }
    }
}
