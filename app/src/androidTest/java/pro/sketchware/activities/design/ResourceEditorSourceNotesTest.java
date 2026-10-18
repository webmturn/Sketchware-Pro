package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.editor.code.SrcCodeEditor;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.StringResourceResolver;
import pro.sketchware.databinding.CodeEditorHsBinding;

/** Real source Activity round trips plus comment/model/draft/XML consistency. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ResourceEditorSourceNotesTest {
    private static final String BASE = "<resources>\n<!-- app note --><string name=\"app_name\">NotesTest</string>"
            + "<!-- probe note --><string name=\"probe\">base</string><!-- page tail -->\n</resources>";

    @After public void resetFault() throws Exception { fault(point -> { }); }

    @Test public void cleanSourceEntryDoesNotRewriteOrInvalidateThePage() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml");
            byte[] before = Files.readAllBytes(xml.toPath());
            long[] revisions = new long[2];
            AtomicInteger encoded = new AtomicInteger();
            on(scenario, a -> {
                a.colorsEditor.colorList.get(0).setColorValue("#445566");
                a.colorsEditor.markDraftChanged(); protect(a);
                revisions[0] = a.stringsEditor.draftRevision();
            });
            fault(point -> { if ("before_encode".equals(point)) encoded.incrementAndGet(); });
            sourceRoundTrip(scenario, () -> on(scenario, a -> {
                openSource(a); revisions[1] = a.stringsEditor.draftRevision();
            }), source -> { });
            assertArrayEquals("an existing clean XML must keep its exact bytes", before, Files.readAllBytes(xml.toPath()));
            assertEquals("opening source must not change the strings revision", revisions[0], revisions[1]);
            assertEquals("no dirty page requires a new payload here", 0, encoded.get());
            on(scenario, a -> {
                assertEquals("page tail", notes(a).get(a.stringsEditor.listmap.size()));
                assertTrue(a.colorsEditor.hasUnsavedChanges);
                assertEquals("#445566", a.colorsEditor.colorList.get(0).getColorValue());
                assertFalse(a.stringsEditor.hasUnsavedChanges);
            });
        });
    }

    @Test public void dirtySourceEntryKeepsIncrementalCaptureBeforeXmlSave() throws Exception {
        withEditor(largeXml(1024), (fixture, scenario) -> {
            String[] activeSession = new String[1];
            on(scenario, a -> { a.stringsEditor.markDraftChanged(); protect(a);
                activeSession[0] = (String) field(field(a, "draftController"), "session");
                a.stringsEditor.listmap.get(700).put("text", "edited at 700");
                a.stringsEditor.markStringChanged(700, false); });
            AtomicBoolean chunked = new AtomicBoolean();
            fault(point -> {
                if (!"after_manifest".equals(point)) return;
                try {
                    File root = new File(instrumentation().getTargetContext().getNoBackupFilesDir(), "resource-editor-drafts");
                    File file = new File(new File(root, activeSession[0]), "manifest.json");
                    JsonObject manifest = JsonParser.parseString(read(file)).getAsJsonObject();
                    assertEquals(fixture.id, manifest.get("project").getAsString());
                    JsonObject page = manifest.getAsJsonArray("pages").get(0).getAsJsonObject();
                    if (page.has("format") && "strings-chunks-utf8-v1".equals(page.get("format").getAsString())) chunked.set(true);
                } catch (Exception error) { throw new AssertionError(error); }
            });
            sourceRoundTrip(scenario, () -> on(scenario, ResourceEditorSourceNotesTest::openSource), source -> { });
            assertTrue("source entry must not reset a valid incremental draft", chunked.get());
            assertTrue(read(resource(fixture.id, "", "strings.xml")).contains("edited at 700"));
        });
    }

    @Test public void missingCleanTargetIsProtectedAndCreatedBeforeSourceLaunch() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml");
            assertTrue(xml.delete());
            sourceRoundTrip(scenario, () -> on(scenario, ResourceEditorSourceNotesTest::openSource), source -> {
                assertTrue(xml.isFile());
                assertTrue(read(xml).contains("page tail"));
                assertTrue(read(xml).contains("name=\"probe\""));
            });
            on(scenario, a -> assertFalse(a.stringsEditor.hasUnsavedChanges));
        });
    }

    @Test public void failedTargetXmlDoesNotLaunchSourceAndCanBeRetried() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml"); byte[] before = Files.readAllBytes(xml.toPath());
            File blocked = new File(xml.getParentFile(), "blocked-source-target"); assertTrue(blocked.mkdir());
            Instrumentation.ActivityMonitor monitor = instrumentation().addMonitor(SrcCodeEditor.class.getName(), null, false);
            try {
                on(scenario, a -> { a.stringsEditor.addString("new_row", "new", "row note");
                    a.stringsEditor.filePath = blocked.getAbsolutePath(); openSource(a);
                    assertFalse(a.isComingFromSrcCodeEditor); assertTrue(a.stringsEditor.hasUnsavedChanges); });
                instrumentation().waitForIdleSync(); assertEquals(0, monitor.getHits());
            } finally { instrumentation().removeMonitor(monitor); }
            assertArrayEquals(before, Files.readAllBytes(xml.toPath()));
            sourceRoundTrip(scenario, () -> on(scenario, a -> { a.stringsEditor.filePath = xml.getAbsolutePath(); openSource(a); }), source -> { });
            assertTrue(read(xml).contains("new_row"));
        });
    }

    @Test public void cleanCheckpointFailureBlocksSourceUntilRetry() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            on(scenario, a -> { a.stringsEditor.addString("protected_b", "B", ""); protect(a); });
            fault(point -> { if ("before_manifest".equals(point)) throw new RuntimeException("source CLEAN failure"); });
            Instrumentation.ActivityMonitor monitor = instrumentation().addMonitor(SrcCodeEditor.class.getName(), null, false);
            try {
                on(scenario, a -> { openSource(a); assertFalse(a.isComingFromSrcCodeEditor);
                    assertFalse("XML was saved but source navigation still needs CLEAN", a.stringsEditor.hasUnsavedChanges); });
                instrumentation().waitForIdleSync(); assertEquals(0, monitor.getHits());
                assertTrue(read(resource(fixture.id, "", "strings.xml")).contains("protected_b"));
            } finally { instrumentation().removeMonitor(monitor); }
            fault(point -> { });
            sourceRoundTrip(scenario, () -> on(scenario, ResourceEditorSourceNotesTest::openSource), source -> { });
        });
    }

    @Test public void malformedXmlRepairEntryAlsoProtectsOtherDrafts() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml");
            String malformed = "<resources><string name=\"broken\">unfinished";
            write(xml, malformed);
            on(scenario, a -> { a.colorsEditor.colorList.get(0).setColorValue("#778899"); a.colorsEditor.markDraftChanged();
                a.stringsEditor.updateStringsList(xml.getAbsolutePath(), 0, false); });
            fault(point -> { if ("before_manifest".equals(point)) throw new RuntimeException("repair draft failure"); });
            clickDialog(scenario, R.string.resources_manager_xml_load_failed_title, android.R.id.button1);
            clickDialog(scenario, R.string.resources_editor_draft_xml_only_title, android.R.id.button2);
            assertEquals(malformed, read(xml));
            on(scenario, a -> { assertFalse(a.isComingFromSrcCodeEditor); assertTrue(a.colorsEditor.hasUnsavedChanges); });
            fault(point -> { });
            sourceRoundTrip(scenario, () -> on(scenario, ResourceEditorSourceNotesTest::openSource), source -> {
                instrumentation().runOnMainSync(() -> { try {
                    CodeEditorHsBinding binding = (CodeEditorHsBinding) field(source, "binding");
                    binding.editor.setText(BASE); assertTrue(binding.toolbar.getMenu().performIdentifierAction(3, 0));
                } catch (Exception error) { throw new AssertionError(error); } });
            });
            on(scenario, a -> { assertTrue(a.colorsEditor.hasUnsavedChanges); assertEquals("page tail", notes(a).get(2)); });
        });
    }

    @Test public void draftFailureOffersCancelAndExplicitXmlOnlyContinuesSource() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml"); byte[] before = Files.readAllBytes(xml.toPath());
            on(scenario, a -> { a.stringsEditor.addString("protected_a", "A", ""); protect(a);
                a.stringsEditor.addString("saved_b", "B", ""); });
            fault(point -> { if ("before_manifest".equals(point)) throw new RuntimeException("source draft failure"); });
            on(scenario, ResourceEditorSourceNotesTest::openSource);
            clickDialog(scenario, R.string.resources_editor_draft_xml_only_title, android.R.id.button2);
            assertArrayEquals(before, Files.readAllBytes(xml.toPath()));
            on(scenario, a -> { assertTrue(a.stringsEditor.hasUnsavedChanges); assertFalse(a.isComingFromSrcCodeEditor); });
            on(scenario, ResourceEditorSourceNotesTest::openSource);
            sourceRoundTrip(scenario,
                    () -> clickDialog(scenario, R.string.resources_editor_draft_xml_only_title, android.R.id.button1),
                    source -> assertTrue(read(xml).contains("saved_b")));
            fault(point -> { });
            on(scenario, ResourceEditorSourceNotesTest::protect);
        });
    }

    @Test public void unsafeRetryContinuesTheOriginallyRequestedSourcePage() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            on(scenario, a -> { a.stringsEditor.addString("draft_a", "A", ""); protect(a); });
            fault(point -> { if ("before_manifest".equals(point) || "before_close_manifest".equals(point))
                throw new RuntimeException("source unsafe setup"); });
            on(scenario, a -> { a.stringsEditor.addString("xml_b", "B", "");
                call(a, "performXmlOnlySave", Runnable.class, null);
                assertEquals(Boolean.TRUE, field(a, "restoreReferenceUnsafe")); });
            fault(point -> { });
            on(scenario, ResourceEditorSourceNotesTest::openSource);
            sourceRoundTrip(scenario,
                    () -> clickDialog(scenario, R.string.resources_editor_unsafe_navigation_title, android.R.id.button1),
                    source -> assertEquals(resource(fixture.id, "", "strings.xml").getAbsolutePath(), source.getIntent().getStringExtra("content")));
        });
    }

    @Test public void sourceReturnKeepsShiftedCommentsTailAndOtherDirtyPages() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            on(scenario, a -> { a.colorsEditor.colorList.get(0).setColorValue("#778899"); a.colorsEditor.markDraftChanged(); });
            sourceRoundTrip(scenario, () -> on(scenario, ResourceEditorSourceNotesTest::openSource), source -> {
                instrumentation().runOnMainSync(() -> {
                    try {
                        CodeEditorHsBinding binding = (CodeEditorHsBinding) field(source, "binding");
                        binding.editor.setText("<resources><!-- first note --><string name=\"probe\">from source</string><!-- source tail --></resources>");
                        assertTrue(binding.toolbar.getMenu().performIdentifierAction(3, 0));
                    } catch (Exception error) { throw new AssertionError(error); }
                });
            });
            on(scenario, a -> { assertEquals("app_name", a.stringsEditor.listmap.get(0).get("key"));
                assertEquals("first note", notes(a).get(index(a, "probe")));
                assertEquals("source tail", notes(a).get(a.stringsEditor.listmap.size()));
                assertTrue(a.colorsEditor.hasUnsavedChanges); protect(a); });
            scenario.recreate(); awaitReady(scenario);
            on(scenario, a -> { assertEquals("source tail", notes(a).get(a.stringsEditor.listmap.size()));
                assertEquals("#778899", a.colorsEditor.colorList.get(0).getColorValue());
                assertTrue(a.colorsEditor.hasUnsavedChanges); });
        });
    }

    @Test public void appendingAcrossChunkBoundaryKeepsTailSeparateFromNewRowNote() throws Exception {
        withEditor(largeXml(1024), (fixture, scenario) -> {
            on(scenario, a -> { a.stringsEditor.markDraftChanged(); protect(a);
                a.stringsEditor.addString("appended", "new value", "new row note");
                assertEquals("page tail", notes(a).get(1025)); assertEquals("new row note", notes(a).get(1024));
                protect(a); });
            scenario.recreate(); awaitReady(scenario);
            on(scenario, a -> { assertEquals(1025, a.stringsEditor.listmap.size());
                assertEquals("page tail", notes(a).get(1025)); assertEquals("new row note", notes(a).get(1024));
                save(a); });
            String xml = read(resource(fixture.id, "", "strings.xml"));
            assertTrue(xml.indexOf("new row note") < xml.indexOf("name=\"appended\""));
            assertTrue(xml.indexOf("page tail") > xml.indexOf("name=\"appended\""));
        });
    }

    @Test public void emptyLocalizedPageKeepsTailThroughDraftRestoreAndXmlSave() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            write(resource(fixture.id, "-fr", "strings.xml"), "<resources><!-- empty tail --></resources>");
            on(scenario, a -> call(a, "initializeBackgroundTask", String.class, "-fr")); awaitReady(scenario);
            on(scenario, a -> { assertTrue(a.stringsEditor.listmap.isEmpty()); assertEquals("empty tail", notes(a).get(0));
                a.stringsEditor.markDraftChanged(); protect(a); });
            scenario.recreate(); awaitReady(scenario);
            on(scenario, a -> { assertTrue(a.stringsEditor.hasUnsavedChanges); assertTrue(a.stringsEditor.listmap.isEmpty());
                assertEquals("empty tail", notes(a).get(0)); save(a); });
            assertTrue(read(resource(fixture.id, "-fr", "strings.xml")).contains("empty tail"));
        });
    }

    @Test public void replacingImportKeepsOnlyImportedTail() throws Exception { importNotes(0); }
    @Test public void skippingImportKeepsRowsAndBothPageTails() throws Exception { importNotes(1); }
    @Test public void overridingImportKeepsRowOwnershipAndBothPageTails() throws Exception { importNotes(2); }

    private void importNotes(int mode) throws Exception {
        String imported = "<resources><!-- app --><string name=\"app_name\">App</string><!-- incoming common -->"
                + "<string name=\"common\">new</string><!-- incoming added --><string name=\"added\">new</string><!-- incoming tail --></resources>";
        withEditor(imported, (fixture, scenario) -> {
            File target = resource(fixture.id, "-fr", "strings.xml");
            String old = "<resources><!-- old common --><string name=\"common\">old</string><!-- kept row -->"
                    + "<string name=\"kept\">old</string><!-- old tail --></resources>";
            write(target, old);
            on(scenario, a -> call(a, "initializeBackgroundTask", String.class, "-fr")); awaitReady(scenario);
            on(scenario, a -> { a.stringsEditor.updateStringsList(resource(fixture.id, "", "strings.xml").getAbsolutePath(), mode, true);
                assertEquals(mode == 1 ? "old common" : "incoming common", notes(a).get(index(a, "common")));
                assertEquals(mode == 0 ? "incoming tail" : "old tail\nincoming tail", notes(a).get(a.stringsEditor.listmap.size()));
                assertEquals("incoming added", notes(a).get(index(a, "added"))); protect(a); });
            assertEquals("import must not implicitly write the localized XML", old, read(target));
            scenario.recreate(); awaitReady(scenario);
            on(scenario, a -> { assertEquals(mode == 0 ? "incoming tail" : "old tail\nincoming tail", notes(a).get(a.stringsEditor.listmap.size()));
                assertEquals(mode == 1 ? "old common" : "incoming common", notes(a).get(index(a, "common"))); save(a); });
            StringResourceResolver resolver = new StringResourceResolver();
            ArrayList<HashMap<String, Object>> rows = new ArrayList<>(); resolver.convertXmlStringsToListMap(read(target), rows);
            assertEquals(mode == 0 ? "incoming tail" : "old tail\nincoming tail", resolver.notesMap.get(rows.size()));
            for (int i = 0; i < rows.size(); i++) if ("common".equals(rows.get(i).get("key")))
                assertEquals(mode == 1 ? "old common" : "incoming common", resolver.notesMap.get(i));
        });
    }

    @Test public void synthesizedAppNameShiftsRowCommentsAndTailOnInitialLoad() throws Exception {
        withEditor("<resources><!-- first note --><string name=\"probe\">base</string><!-- page tail --></resources>",
                (fixture, scenario) -> on(scenario, a -> { assertEquals("app_name", a.stringsEditor.listmap.get(0).get("key"));
                    assertFalse(notes(a).containsKey(0)); assertEquals("first note", notes(a).get(1));
                    assertEquals("page tail", notes(a).get(2)); }));
    }

    @Test public void replacingPageDoesNotCarryTheOldTailIntoNewXml() throws Exception {
        withEditor(BASE, (fixture, scenario) -> {
            File xml = resource(fixture.id, "", "strings.xml");
            write(xml, "<resources><string name=\"app_name\">App</string><string name=\"probe\">new</string></resources>");
            on(scenario, a -> { a.stringsEditor.updateStringsList(xml.getAbsolutePath(), 0, false);
                assertFalse(notes(a).containsKey(a.stringsEditor.listmap.size())); });
        });
    }

    private static Instrumentation instrumentation() { return InstrumentationRegistry.getInstrumentation(); }
    private interface SourceCheck { void run(SrcCodeEditor source) throws Exception; }
    private static void sourceRoundTrip(ActivityScenario<ResourcesEditorActivity> scenario, Runnable trigger, SourceCheck check) throws Exception {
        Instrumentation.ActivityMonitor monitor = instrumentation().addMonitor(SrcCodeEditor.class.getName(), null, false);
        Activity source = null;
        try {
            trigger.run(); source = instrumentation().waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull("the source action must continue into the real source Activity", source);
            Activity shown = source;
            instrumentation().runOnMainSync(() -> shown.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            check.run((SrcCodeEditor) source);
        } finally {
            Activity opened = source != null ? source : monitor.getLastActivity();
            if (opened != null) instrumentation().runOnMainSync(opened::finish);
            instrumentation().removeMonitor(monitor);
        }
        awaitReady(scenario);
    }

    private static void openSource(ResourcesEditorActivity a) {
        MaterialToolbar toolbar = a.findViewById(R.id.topAppBar);
        a.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_open_editor));
    }
    private static void save(ResourcesEditorActivity a) throws Exception {
        assertTrue(((ResourceEditorSaveResult) call(a, "saveEditorsChanges")).navigationAllowed);
    }
    private static void protect(ResourcesEditorActivity a) throws Exception { assertEquals(Boolean.TRUE, call(a, "protectDrafts")); }
    private static int index(ResourcesEditorActivity a, String key) {
        for (int i = 0; i < a.stringsEditor.listmap.size(); i++) if (key.equals(a.stringsEditor.listmap.get(i).get("key"))) return i;
        throw new AssertionError("Missing row: " + key);
    }
    @SuppressWarnings("unchecked") private static Map<Integer, String> notes(ResourcesEditorActivity a) throws Exception {
        return (Map<Integer, String>) field(a.stringsEditor, "notesMap");
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static Object call(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(object);
    }
    private static Object call(Object object, String name, Class<?> type, Object value) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, type); method.setAccessible(true); return method.invoke(object, value);
    }
    private static void fault(Consumer<String> fault) throws Exception {
        Field field = Class.forName("pro.sketchware.activities.resourceseditor.ResourceEditorDraftStore").getDeclaredField("testFault");
        field.setAccessible(true); field.set(null, fault);
    }
    private interface Action { void run(ResourcesEditorActivity activity) throws Exception; }
    private static void on(ActivityScenario<ResourcesEditorActivity> scenario, Action action) {
        scenario.onActivity(a -> { try { action.run(a); } catch (Exception error) { throw new AssertionError(error); } });
    }
    private static void awaitReady(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            on(scenario, a -> {
                a.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                ready[0] = (Boolean) field(a, "editorsLoaded") && !(Boolean) field(a, "initialLoadPending")
                    && !(Boolean) field(a, "sourceReloadPending") && !a.isComingFromSrcCodeEditor && (Integer) field(a, "sourceEditorPage") == -1;
            });
            if (ready[0]) { instrumentation().waitForIdleSync(); return; } SystemClock.sleep(20);
        }
        StringBuilder details = new StringBuilder();
        on(scenario, a -> { for (String name : new String[]{"editorsLoaded", "initialLoadPending", "sourceReloadPending", "sourceEditorPage"})
                details.append(name).append('=').append(field(a, name)).append(' ');
            details.append("coming=").append(a.isComingFromSrcCodeEditor).append(" rows=").append(a.stringsEditor.listmap.size()); });
        fail("Editor never completed loading/returning from source: " + details);
    }
    private static boolean hasText(View root, String text) {
        if (root instanceof TextView view && text.contentEquals(view.getText())) return true;
        if (root instanceof android.view.ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) if (hasText(group.getChildAt(i), text)) return true;
        return false;
    }
    private static void clickDialog(ActivityScenario<ResourcesEditorActivity> scenario, int title, int button) {
        on(scenario, a -> {
            for (View root : WindowInspector.getGlobalWindowViews()) {
                if (hasText(root, a.getString(title))) { assertTrue(root.findViewById(button).performClick()); return; }
            }
            fail("Dialog missing: " + a.getString(title));
        });
    }
    private static String read(File file) throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    private static void write(File file, String text) throws Exception {
        assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory()); Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
    private static File resource(String id, String variant, String name) { return new File(SketchwarePaths.getProjectResourceValuesFilePath(id, variant, name)); }
    private static String largeXml(int rows) {
        StringBuilder xml = new StringBuilder("<resources><string name=\"app_name\">App</string>");
        for (int i = 1; i < rows; i++) xml.append("<string name=\"row_").append(i).append("\">value ").append(i).append("</string>");
        return xml.append("<!-- page tail --></resources>").toString();
    }
    private interface Check { void run(ProjectEditorPersistenceTest.Fixture fixture, ActivityScenario<ResourcesEditorActivity> scenario) throws Exception; }
    private static void withEditor(String strings, Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            write(resource(fixture.id, "", "strings.xml"), strings);
            for (String name : new String[]{"colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"}) {
                String content = name.equals("colors.xml") ? "<resources><color name=\"probe_color\">#336699</color></resources>" : "<resources/>";
                write(resource(fixture.id, "", name), content); write(resource(fixture.id, "-fr", name), content);
            }
            write(resource(fixture.id, "-fr", "strings.xml"), strings);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(new Intent(instrumentation().getTargetContext(), ResourcesEditorActivity.class).putExtra("sc_id", fixture.id))) {
                awaitReady(scenario); check.run(fixture, scenario);
            }
        }
    }
}
