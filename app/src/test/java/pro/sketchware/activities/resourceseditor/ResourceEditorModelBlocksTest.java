package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;

public class ResourceEditorModelBlocksTest {
    private static List<StyleModel> rows(int count) {
        var rows = new ArrayList<StyleModel>();
        for (int i = 0; i < count; i++) rows.add(new StyleModel("same-name", "Parent"));
        return rows;
    }

    @Test public void countsIncludeAnEmptyPageAndAvoidIntegerOverflow() {
        assertEquals(1, ResourceEditorModelBlocks.count(0));
        assertEquals(1, ResourceEditorModelBlocks.count(256));
        assertEquals(2, ResourceEditorModelBlocks.count(257));
        assertEquals(8388608, ResourceEditorModelBlocks.count(Integer.MAX_VALUE));
    }

    @Test public void pointChangesOnlyInvalidateTheirOwnBlockDespiteDuplicateNames() {
        var live = rows(1024); var cache = new ResourceEditorSnapshotCache();
        var first = cache.capture(live, Map.of());
        live.get(700).addAttribute("attr", "B"); cache.invalidate(live.get(700));
        var second = cache.capture(live, Map.of());
        for (int block = 0; block < 4; block++) {
            assertEquals(block == 2, ResourceEditorModelBlocks.changed(first, second, block));
        }
    }

    @Test public void notesAndTailUseTheLatestBlockOwnershipAfterBoundaryAppend() {
        var live = rows(256); var cache = new ResourceEditorSnapshotCache();
        var first = cache.capture(live, Map.of(256, "tail"));
        live.add(new StyleModel("new", ""));
        var second = cache.capture(live, Map.of(256, "new row note", 257, "tail"));
        assertFalse(ResourceEditorModelBlocks.changed(first, second, 0));
        assertTrue(ResourceEditorModelBlocks.changed(first, second, 1));
        var slice = ResourceEditorModelBlocks.slice(second, 1);
        assertEquals(Map.of(0, "new row note", 1, "tail"), slice.notes);
        assertFalse(ResourceEditorModelBlocks.slice(second, 0).notes.containsKey(256));
    }

    @Test public void boundaryShrinkRewritesNewLastBlockEvenWhenRowIdentitiesMatch() {
        var live = rows(512); var cache = new ResourceEditorSnapshotCache();
        var first = cache.capture(live, Map.of(256, "row note", 512, "old tail"));
        live.subList(256, 512).clear();
        var second = cache.capture(live, Map.of(256, "row note"));
        assertTrue(ResourceEditorModelBlocks.changed(first, second, 0));
        assertEquals("row note", ResourceEditorModelBlocks.slice(second, 0).notes.get(256));
    }

    @Test public void notesOnlyRemovalAndEmptyPageAreRecognized() {
        var live = rows(512); var cache = new ResourceEditorSnapshotCache();
        var first = cache.capture(live, Map.of(300, "note", 512, "tail"));
        var second = cache.capture(live, Map.of(512, "tail"));
        assertFalse(ResourceEditorModelBlocks.changed(first, second, 0));
        assertTrue(ResourceEditorModelBlocks.changed(first, second, 1));
        var emptyA = cache.capture(List.of(), Map.of(0, "A"));
        var emptyB = cache.capture(List.of(), Map.of(0, "B"));
        assertTrue(ResourceEditorModelBlocks.changed(emptyA, emptyB, 0));
        assertEquals(Map.of(0, "B"), ResourceEditorModelBlocks.slice(emptyB, 0).notes);
    }

    @Test public void oldTailIsNotPulledIntoANewNonFinalBlock() {
        var live = rows(512); var cache = new ResourceEditorSnapshotCache();
        var first = cache.capture(live, Map.of(512, "old tail"));
        live.add(new StyleModel("append", ""));
        var second = cache.capture(live, Map.of(513, "old tail"));
        assertFalse(ResourceEditorModelBlocks.changed(first, second, 1));
        assertEquals(Map.of(1, "old tail"), ResourceEditorModelBlocks.slice(second, 2).notes);
    }
}
