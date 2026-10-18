package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.PalletCustomviewBinding;
import pro.sketchware.databinding.ResourcesEditorFragmentBinding;

/** Drives real dialogs and restores committed drafts, including equal Maps in different chunks. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ResourceEditorStringsInteractionTest {
    @Test public void equalContentEditAndCommentProtectTheActualChunk() throws Exception {
        withEditor(scenario -> {
            String[] replacement = new String[2];
            scenario.onActivity(a -> {
                replacement[0] = (String) a.stringsEditor.listmap.get(10).get("key");
                replacement[1] = (String) a.stringsEditor.listmap.get(10).get("text");
                a.stringsEditor.markDraftChanged();
                protect(a);
                a.stringsEditor.adapter.filter("audit_target");
                assertEquals(1, a.stringsEditor.adapter.getItemCount());
            });
            openRow(scenario, 0);
            submitEdit(scenario, replacement[0], replacement[1], "edited target note");
            scenario.onActivity(a -> {
                assertEquals(replacement[1], a.stringsEditor.listmap.get(700).get("text"));
                assertEquals("earlier note", notes(a).get(10));
                assertEquals("edited target note", notes(a).get(700));
                protect(a);
                JsonObject page = stringPage(a);
                assertEquals("strings-chunks-utf8-v1", page.get("format").getAsString());
                assertTrue("The actual edited chunk must be protected", page.getAsJsonObject("chunks").has("2"));
            });
            scenario.recreate();
            awaitLoaded(scenario, 2049);
            scenario.onActivity(a -> {
                assertEquals(replacement[0], a.stringsEditor.listmap.get(700).get("key"));
                assertEquals(replacement[1], a.stringsEditor.listmap.get(700).get("text"));
                assertEquals("earlier note", notes(a).get(10));
                assertEquals("edited target note", notes(a).get(700));
                a.stringsEditor.adapter.filter("equal_pair");
                assertEquals(2, a.stringsEditor.adapter.getItemCount());
            });
            // Both Maps are already equal: changing only the later comment must still target row 700.
            openRow(scenario, 1);
            submitEdit(scenario, replacement[0], replacement[1], "second target note");
            scenario.onActivity(ResourceEditorStringsInteractionTest::protect);
            scenario.recreate();
            awaitLoaded(scenario, 2049);
            scenario.onActivity(a -> {
                assertEquals("earlier note", notes(a).get(10));
                assertEquals("second target note", notes(a).get(700));
            });
        });
    }

    @Test public void deletingEqualRowFromFreshAdapterRemovesOnlySelectedObject() throws Exception {
        deleteEqualRow(false);
    }

    @Test public void deletingEqualRowFromFilteredAdapterKeepsEarlierRowAndNotes() throws Exception {
        deleteEqualRow(true);
    }

    private static void deleteEqualRow(boolean filtered) throws Exception {
        withEditor(scenario -> {
            @SuppressWarnings("unchecked") HashMap<String, Object>[] selected = new HashMap[2];
            scenario.onActivity(a -> {
                selected[0] = a.stringsEditor.listmap.get(10);
                selected[1] = a.stringsEditor.listmap.get(700);
                selected[1].putAll(selected[0]);
                // addString creates a new adapter; its initial filter must not alias the source list.
                a.stringsEditor.addString("extra_row", "extra value", "extra note");
                a.stringsEditor.markDraftChanged();
                protect(a);
                if (filtered) {
                    a.stringsEditor.adapter.filter("equal_pair");
                    assertEquals(2, a.stringsEditor.adapter.getItemCount());
                }
            });
            openRow(scenario, filtered ? 1 : 700);
            scenario.onActivity(a -> {
                View dialog = editDialog();
                assertEquals("target note", ((EditText) dialog.findViewById(R.id.string_header_input)).getText().toString());
                assertTrue(dialog.findViewById(android.R.id.button3).performClick());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                assertEquals("Exactly one source row must be removed", 2049, a.stringsEditor.listmap.size());
                assertSame(selected[0], a.stringsEditor.listmap.get(10));
                assertFalse(a.stringsEditor.listmap.stream().anyMatch(row -> row == selected[1]));
                assertEquals("earlier note", notes(a).get(10));
                assertFalse(notes(a).containsKey(700));
                assertEquals("extra note", notes(a).get(2048));
                a.stringsEditor.adapter.filter("equal_pair");
                assertEquals(1, a.stringsEditor.adapter.getItemCount());
                protect(a);
            });
            scenario.recreate();
            awaitLoaded(scenario, 2049);
            scenario.onActivity(a -> {
                assertEquals("equal_pair", a.stringsEditor.listmap.get(10).get("key"));
                assertEquals(1, a.stringsEditor.listmap.stream().filter(row -> "equal_pair".equals(row.get("key"))).count());
                assertEquals("earlier note", notes(a).get(10));
                assertEquals("extra note", notes(a).get(2048));
            });
        });
    }

    private static void submitEdit(ActivityScenario<ResourcesEditorActivity> scenario, String key, String value, String note) {
        scenario.onActivity(a -> {
            View dialog = editDialog();
            ((EditText) dialog.findViewById(R.id.string_key_input)).setText(key);
            ((EditText) dialog.findViewById(R.id.string_value_input)).setText(value);
            ((EditText) dialog.findViewById(R.id.string_header_input)).setText(note);
            assertTrue(dialog.findViewById(android.R.id.button1).performClick());
        });
        // AlertDialog posts its button callback to the main queue.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static View editDialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.string_key_input) != null) return root;
        }
        throw new AssertionError("String edit dialog must be visible");
    }

    private static void openRow(ActivityScenario<ResourcesEditorActivity> scenario, int position) {
        scenario.onActivity(a -> ResourcesEditorFragmentBinding.bind(a.stringsEditor.requireView()).recyclerView.scrollToPosition(position));
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] opened = {false};
            scenario.onActivity(a -> {
                RecyclerView recycler = ResourcesEditorFragmentBinding.bind(a.stringsEditor.requireView()).recyclerView;
                RecyclerView.ViewHolder holder = recycler.findViewHolderForAdapterPosition(position);
                if (holder != null) opened[0] = PalletCustomviewBinding.bind(holder.itemView).backgroundCard.performClick();
            });
            if (opened[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("Selected string row did not become visible");
    }

    @SuppressWarnings("unchecked") private static Map<Integer, String> notes(ResourcesEditorActivity a) {
        try {
            Field field = a.stringsEditor.getClass().getDeclaredField("notesMap");
            field.setAccessible(true);
            return (Map<Integer, String>) field.get(a.stringsEditor);
        } catch (Exception failure) { throw new AssertionError(failure); }
    }

    private static void protect(ResourcesEditorActivity a) {
        try {
            Method method = ResourcesEditorActivity.class.getDeclaredMethod("protectDrafts");
            method.setAccessible(true);
            assertEquals(Boolean.TRUE, method.invoke(a));
        } catch (Exception failure) { throw new AssertionError(failure); }
    }

    private static JsonObject stringPage(ResourcesEditorActivity a) {
        try {
            Field controllerField = ResourcesEditorActivity.class.getDeclaredField("draftController");
            controllerField.setAccessible(true);
            Object controller = controllerField.get(a);
            Field sessionField = controller.getClass().getDeclaredField("session");
            sessionField.setAccessible(true);
            File file = new File(a.getNoBackupFilesDir(), "resource-editor-drafts/" + sessionField.get(controller) + "/manifest.json");
            return JsonParser.parseString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pages").get(0).getAsJsonObject();
        } catch (Exception failure) { throw new AssertionError(failure); }
    }

    private static void awaitLoaded(ActivityScenario<ResourcesEditorActivity> scenario, int rows) throws Exception {
        Field field = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
        field.setAccessible(true);
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> {
                try { ready[0] = field.getBoolean(a) && a.stringsEditor.listmap.size() == rows; }
                catch (Exception failure) { throw new AssertionError(failure); }
            });
            if (ready[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("String editor fixture did not load");
    }

    private static void withEditor(Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            StringBuilder xml = new StringBuilder("<resources><string name=\"app_name\">AuditFixture</string>");
            for (int i = 0; i < 2048; i++) {
                if (i == 9) xml.append("<!--earlier note-->");
                if (i == 699) xml.append("<!--target note-->");
                String key = i == 9 ? "equal_pair" : i == 699 ? "audit_target" : "row_" + i;
                xml.append("<string name=\"").append(key).append("\">value_").append(i).append("</string>");
            }
            xml.append("</resources>");
            for (String name : new String[]{"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"}) {
                File file = new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, name));
                assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
                Files.write(file.toPath(), (name.equals("strings.xml") ? xml.toString() : "<resources/>").getBytes(StandardCharsets.UTF_8));
            }
            ProjectDataManager.reloadProjectFromData(fixture.id);
            File formalXml = new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, "strings.xml"));
            byte[] original = Files.readAllBytes(formalXml.toPath());
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(new Intent(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), ResourcesEditorActivity.class).putExtra("sc_id", fixture.id))) {
                awaitLoaded(scenario, 2049);
                check.run(scenario);
            }
            assertArrayEquals("Draft protection must not overwrite formal XML", original, Files.readAllBytes(formalXml.toPath()));
        }
    }

    private interface Check { void run(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception; }
}
