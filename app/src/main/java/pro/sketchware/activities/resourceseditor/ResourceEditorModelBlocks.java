package pro.sketchware.activities.resourceseditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/** Operates only on the independently captured, frozen non-string models. */
final class ResourceEditorModelBlocks {
    static final String FORMAT = "models-segments-json-v1";
    static final int ROWS = 256;

    private ResourceEditorModelBlocks() { }

    static int count(int rows) {
        if (rows < 0) throw new IllegalArgumentException("Negative model row count");
        return rows == 0 ? 1 : (rows - 1) / ROWS + 1;
    }

    static boolean changed(ResourceEditorDraftState.Snapshot previous,
                           ResourceEditorDraftState.Snapshot current, int block) {
        return changed(previous, current, block, previous != null && previous.notes.equals(current.notes));
    }

    static boolean changed(ResourceEditorDraftState.Snapshot previous,
                           ResourceEditorDraftState.Snapshot current, int block, boolean identicalNotes) {
        if (previous == null || block >= count(previous.entries.size())) return true;
        int start = block * ROWS;
        int end = start + Math.min(ROWS, current.entries.size() - start);
        int oldEnd = start + Math.min(ROWS, previous.entries.size() - start);
        if (end != oldEnd) return true;
        boolean last = block == count(current.entries.size()) - 1;
        // An old non-final block did not contain the new trailing comment.
        if (last && block != count(previous.entries.size()) - 1) return true;
        for (int i = start; i < end; i++) {
            if (previous.entries.get(i) != current.entries.get(i)) return true;
        }
        if (identicalNotes) return false;
        for (int i = start; i < end + (last ? 1 : 0); i++) {
            if (!Objects.equals(previous.notes.get(i), current.notes.get(i))) return true;
        }
        return false;
    }

    static ResourceEditorDraftState.Snapshot slice(ResourceEditorDraftState.Snapshot snapshot, int block) {
        int start = block * ROWS;
        int end = start + Math.min(ROWS, snapshot.entries.size() - start);
        boolean last = block == count(snapshot.entries.size()) - 1;
        var notes = new HashMap<Integer, String>();
        for (int i = start; i < end + (last ? 1 : 0); i++) {
            if (snapshot.notes.containsKey(i)) notes.put(i - start, snapshot.notes.get(i));
        }
        return new ResourceEditorDraftState.Snapshot(new ArrayList<>(snapshot.entries.subList(start, end)), notes);
    }
}
