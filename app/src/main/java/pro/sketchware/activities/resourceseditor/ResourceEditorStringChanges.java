package pro.sketchware.activities.resourceseditor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** UI-thread revision stamps; failed/in-flight writes never consume pending changes. */
public final class ResourceEditorStringChanges {
    static final int CHUNK_SIZE = 256;
    private final Map<Integer, Long> revisions = new HashMap<>();
    private long resetRevision;
    private boolean compatibleNotes = true;

    public void reset(long revision) {
        resetRevision = revision;
        revisions.clear();
    }

    public void restored(long revision, int rows, Map<Integer, String> notes) {
        reset(revision);
        compatibleNotes = validNotes(rows, notes);
    }

    public void changed(int index, boolean shiftsFollowingRows, int rows, long revision) {
        int first = Math.max(0, Math.min(index, Math.max(0, rows - 1))) / CHUNK_SIZE;
        int last = shiftsFollowingRows ? Math.max(0, rows - 1) / CHUNK_SIZE : first;
        for (int chunk = first; chunk <= last; chunk++) revisions.put(chunk, revision);
        int count = chunkCount(rows);
        revisions.keySet().removeIf(chunk -> chunk >= count);
    }

    public ResourceEditorDraftState.Snapshot snapshot(List<?> entries, Map<Integer, String> notes, long baseRevision) {
        int count = chunkCount(entries.size());
        Map<Integer, ResourceEditorDraftState.Snapshot> changed = new HashMap<>();
        if (baseRevision >= resetRevision && baseRevision >= 0 && compatibleNotes && count > 1) {
            int changedCount = 0;
            for (Map.Entry<Integer, Long> entry : revisions.entrySet())
                if (entry.getValue() > baseRevision && entry.getKey() < count) changedCount++;
            if (changedCount == 0 || changedCount * 2 >= count) {
                compatibleNotes = validNotes(entries.size(), notes);
                return ResourceEditorDraftState.snapshot(entries, notes);
            }
            for (Map.Entry<Integer, Long> entry : revisions.entrySet()) {
                if (entry.getValue() <= baseRevision || entry.getKey() >= count) continue;
                int start = entry.getKey() * CHUNK_SIZE;
                int end = Math.min(entries.size(), start + CHUNK_SIZE);
                Map<Integer, String> localNotes = new HashMap<>();
                int noteEnd = end == entries.size() ? end + 1 : end;
                for (int i = start; i < noteEnd; i++) {
                    if (notes.containsKey(i)) localNotes.put(i - start, notes.get(i));
                }
                changed.put(entry.getKey(), ResourceEditorDraftState.snapshot(entries.subList(start, end), localNotes));
            }
            // A full snapshot uses one write when a bulk operation touches much of the page.
            if (!changed.isEmpty() && changed.size() * 2 < count) {
                return new ResourceEditorDraftState.Snapshot(
                        new ResourceEditorDraftState.StringChunks(baseRevision, entries.size(), changed));
            }
        }
        compatibleNotes = validNotes(entries.size(), notes);
        return ResourceEditorDraftState.snapshot(entries, notes);
    }

    static int chunkCount(int rows) { return rows == 0 ? 0 : (rows - 1) / CHUNK_SIZE + 1; }

    private static boolean validNotes(int rows, Map<Integer, String> notes) {
        for (Integer index : notes.keySet()) if (index == null || index < 0 || index > rows) return false;
        return true;
    }
}
