package pro.sketchware.activities.resourceseditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Main-thread only. Cached copies belong to snapshots, never to editable UI models. */
public final class ResourceEditorSnapshotCache {
    private IdentityHashMap<Object, Object> copies = new IdentityHashMap<>();

    /** Call for every model mutated by a local edit, including its nested attributes. */
    public void invalidate(Object entry) { copies.remove(entry); }

    /** Conservative fallback for imports and callers without a complete changed-row set. */
    public void invalidateAll() { copies.clear(); }

    public ResourceEditorDraftState.Snapshot capture(List<?> entries, Map<Integer, String> notes) {
        IdentityHashMap<Object, Object> next = new IdentityHashMap<>(entries.size());
        List<Object> captured = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            Object copy = next.get(entry);
            if (copy == null) {
                copy = copies.get(entry);
                if (copy == null) copy = ResourceEditorDraftState.copyEntry(entry);
                next.put(entry, copy);
            }
            captured.add(copy);
        }
        copies = next; // Deleted/replaced models must not stay alive through this cache.
        return new ResourceEditorDraftState.Snapshot(Collections.unmodifiableList(captured),
                Collections.unmodifiableMap(new HashMap<>(notes)));
    }

}
