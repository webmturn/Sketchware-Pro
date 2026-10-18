package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.activities.resourceseditor.components.models.DimenModel;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.core.resources.ColorModel;

public class ResourceEditorSnapshotCacheTest {
    private static StyleModel style(String name) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("first", "1"); map.put("second", "2");
        return new StyleModel(name, "Parent", map);
    }

    @Test public void nestedMutationPathsCannotChangeEarlierSnapshots() {
        List<Consumer<LinkedHashMap<String, String>>> changes = List.of(
                m -> m.put("first", "put"), m -> m.remove("first"),
                m -> m.entrySet().iterator().next().setValue("entry"),
                m -> { var iterator = m.entrySet().iterator(); iterator.next(); iterator.remove(); },
                m -> m.keySet().remove("first"), m -> m.values().remove("1"),
                m -> m.compute("first", (k, v) -> "compute"),
                m -> m.merge("first", "merge", (a, b) -> a + b),
                m -> m.replaceAll((k, v) -> "replaceAll"),
                m -> m.replace("first", "replace"),
                m -> m.putAll(Map.of("third", "3")), m -> m.clear());
        for (var change : changes) {
            var cache = new ResourceEditorSnapshotCache(); var live = style("Style");
            var first = cache.capture(List.of(live), Map.of());
            String before = new Gson().toJson(first.entries);
            change.accept(live.getAttributes()); cache.invalidate(live);
            var second = cache.capture(List.of(live), Map.of());
            assertNotSame(first.entries.get(0), second.entries.get(0));
            assertEquals(before, new Gson().toJson(first.entries));
            assertEquals(live.getAttributes(), ((StyleModel) second.entries.get(0)).getAttributes());
        }
    }

    @Test public void rowIdentitySurvivesDeletionAndDuplicateNames() {
        var cache = new ResourceEditorSnapshotCache();
        var a = style("same"); var b = style("same");
        var live = new ArrayList<>(List.of(a, b));
        var first = cache.capture(live, Map.of(2, "tail"));
        assertNotSame(first.entries.get(0), first.entries.get(1));
        live.remove(0);
        var deleted = cache.capture(live, Map.of(1, "tail"));
        assertSame(first.entries.get(1), deleted.entries.get(0));
        a.setParent("removed"); b.setParent("edited"); cache.invalidate(b);
        var changed = cache.capture(live, Map.of());
        assertEquals("Parent", ((StyleModel) first.entries.get(1)).getParent());
        assertEquals("edited", ((StyleModel) changed.entries.get(0)).getParent());
        live.clear(); assertTrue(cache.capture(live, Map.of(0, "empty tail")).entries.isEmpty());
        live.add(b);
        assertNotSame(changed.entries.get(0), cache.capture(live, Map.of()).entries.get(0));
    }

    @Test public void orderOnlyAttributeChangesAreCaptured() {
        var cache = new ResourceEditorSnapshotCache(); var live = style("Style");
        var first = cache.capture(List.of(live), Map.of());
        live.getAttributes().put("first", live.getAttributes().remove("first")); cache.invalidate(live);
        var second = cache.capture(List.of(live), Map.of());
        assertNotSame(first.entries.get(0), second.entries.get(0));
        assertEquals(List.of("second", "first"), new ArrayList<>(
                ((StyleModel) second.entries.get(0)).getAttributes().keySet()));
    }

    @Test public void restoredModelsNeverBecomeTheCachesFrozenRows() {
        var cache = new ResourceEditorSnapshotCache();
        var old = cache.capture(List.of(style("old UI")), Map.of());
        // Store.open returns fresh, exclusively owned restoration models, as before this change.
        var restored = ResourceEditorDraftState.snapshot(List.of(style("restored")), Map.of(1, "tail"));
        var live = new ArrayList<StyleModel>(); var notes = new HashMap<Integer, String>();
        cache.invalidateAll(); ResourceEditorDraftState.apply(restored, live, notes);
        var firstCapture = cache.capture(live, notes);
        assertNotSame(live.get(0), firstCapture.entries.get(0));
        assertNotSame(old.entries.get(0), firstCapture.entries.get(0));
        live.get(0).getAttributes().entrySet().iterator().next().setValue("new"); cache.invalidate(live.get(0));
        notes.put(1, "new tail");
        var changed = cache.capture(live, notes);
        assertEquals("1", ((StyleModel) firstCapture.entries.get(0)).getAttribute("first"));
        assertEquals("tail", firstCapture.notes.get(1));
        assertEquals("new tail", changed.notes.get(1));
    }

    @Test public void scalarsArraysAndReplacementMapsInvalidateOnlyChangedRows() {
        var cache = new ResourceEditorSnapshotCache();
        var color = new ColorModel("color", "#123456"); var dimen = new DimenModel("size", "1dp");
        var array = new ArrayModel("items", ArraysEditor.ARRAYS_TYPES.STRING,
                new LinkedHashMap<>(Map.of("item1", "one")));
        var styles = style("Style"); var live = List.of(color, dimen, array, styles);
        var first = cache.capture(live, Map.of());
        color.setColorName("renamed"); color.setColorValue("#654321");
        dimen.setDimenName("renamed_size"); dimen.setDimenValue("2dp");
        array.setArrayName("renamed_items"); array.setArrayType(ArraysEditor.ARRAYS_TYPES.INTEGER);
        array.setAttributes(new LinkedHashMap<>(Map.of("item1", "42")));
        styles.setStyleName("Renamed"); styles.setAttributes(null); cache.invalidateAll();
        var changed = cache.capture(live, Map.of());
        for (int i = 0; i < live.size(); i++) assertNotSame(first.entries.get(i), changed.entries.get(i));
        var repeated = cache.capture(live, Map.of());
        for (int i = 0; i < live.size(); i++) assertSame(changed.entries.get(i), repeated.entries.get(i));
        assertEquals("one", ((ArrayModel) first.entries.get(2)).getAttribute("item1"));
        assertNull(((StyleModel) changed.entries.get(3)).getAttributes());
        assertEquals("color", ((ColorModel) first.entries.get(0)).getColorName());
    }

    @Test public void notesAreIndependentIncludingAnEmptyPage() {
        var cache = new ResourceEditorSnapshotCache(); var notes = new HashMap<>(Map.of(0, "tail"));
        var first = cache.capture(List.of(), notes); notes.put(0, "new tail");
        assertEquals("tail", first.notes.get(0));
        assertEquals("new tail", cache.capture(List.of(), notes).notes.get(0));
    }

    @Test public void multipleLocalInvalidationsAccumulateAndBulkInvalidationCoversUnknownEdits() {
        var cache = new ResourceEditorSnapshotCache();
        var a = style("A"); var b = style("B"); var c = style("C");
        var live = List.of(a, b, c);
        var first = cache.capture(live, Map.of());
        a.setParent("changed A"); cache.invalidate(a);
        b.getAttributes().clear(); cache.invalidate(b);
        var local = cache.capture(live, Map.of());
        assertNotSame(first.entries.get(0), local.entries.get(0));
        assertNotSame(first.entries.get(1), local.entries.get(1));
        assertSame(first.entries.get(2), local.entries.get(2));
        c.setParent("unknown bulk edit"); cache.invalidateAll();
        var bulk = cache.capture(live, Map.of());
        assertEquals("unknown bulk edit", ((StyleModel) bulk.entries.get(2)).getParent());
        assertEquals("Parent", ((StyleModel) first.entries.get(2)).getParent());
    }
}
