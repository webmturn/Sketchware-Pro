package pro.sketchware.activities.resourceseditor;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.activities.resourceseditor.components.models.DimenModel;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.core.resources.ColorModel;

/** Keeps editor models and comments intact without parsing or writing resource XML. */
public final class ResourceEditorDraftState {
    private static final Gson GSON = new Gson();

    private ResourceEditorDraftState() { }

    public interface Owner {
        @Nullable Bundle saveDraftState();
        void restoreDraftState(@NonNull Bundle state, String filePath);
        @Nullable Snapshot snapshotDraft();
        default Snapshot snapshotDraft(long protectedRevision) { return snapshotDraft(); }
        void restoreSnapshot(@NonNull Snapshot snapshot, String filePath);
        long draftRevision();
        boolean hasDraftChanges();
    }

    /** Owned exclusively by the storage task after capture; never shares mutable UI models. */
    public static final class Snapshot {
        final List<Object> entries;
        final Map<Integer, String> notes;
        final StringChunks chunks;

        Snapshot(List<Object> entries, Map<Integer, String> notes) {
            this.entries = entries;
            this.notes = notes;
            this.chunks = null;
        }

        Snapshot(StringChunks chunks) {
            this.entries = List.of();
            this.notes = Map.of();
            this.chunks = chunks;
        }
    }

    static final class StringChunks {
        final long baseRevision;
        final int rows;
        final Map<Integer, Snapshot> changed;

        StringChunks(long baseRevision, int rows, Map<Integer, Snapshot> changed) {
            this.baseRevision = baseRevision;
            this.rows = rows;
            this.changed = changed;
        }
    }

    public static Snapshot snapshot(List<?> entries, Map<Integer, String> notes) {
        List<Object> copy = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (entry instanceof ColorModel color) {
                copy.add(new ColorModel(color.getColorName(), color.getColorValue()));
            } else if (entry instanceof DimenModel dimen) {
                copy.add(new DimenModel(dimen.getDimenName(), dimen.getDimenValue()));
            } else if (entry instanceof StyleModel style) {
                copy.add(new StyleModel(style.getStyleName(), style.getParent(),
                        style.getAttributes() == null ? null : new LinkedHashMap<>(style.getAttributes())));
            } else if (entry instanceof ArrayModel array) {
                copy.add(new ArrayModel(array.getArrayName(), array.getArrayType(),
                        array.getAttributes() == null ? null : new LinkedHashMap<>(array.getAttributes())));
            } else if (entry instanceof Map<?, ?> map) {
                // String resources contain scalar string values, including optional XML attributes.
                HashMap<String, Object> row = new HashMap<>();
                for (Map.Entry<?, ?> value : map.entrySet()) {
                    if (value.getValue() != null && !(value.getValue() instanceof String)) {
                        throw new IllegalArgumentException("Non-string resource attribute");
                    }
                    row.put((String) value.getKey(), value.getValue());
                }
                copy.add(row);
            } else {
                throw new IllegalArgumentException("Unknown resource model");
            }
        }
        return new Snapshot(copy, new HashMap<>(notes));
    }

    @SuppressWarnings("unchecked")
    public static <T> void apply(Snapshot snapshot, List<T> entries, Map<Integer, String> notes) {
        entries.clear();
        entries.addAll((List<T>) (List<?>) snapshot.entries);
        notes.clear();
        notes.putAll(snapshot.notes);
    }

    public static Bundle capture(List<?> entries, Map<Integer, String> notes) {
        Bundle state = new Bundle();
        state.putString("entries", GSON.toJson(entries));
        state.putString("notes", GSON.toJson(notes));
        return state;
    }

    public static <T> void restore(Bundle state, List<T> entries, Map<Integer, String> notes,
                                   Class<?> entryClass) {
        List<T> savedEntries = GSON.fromJson(state.getString("entries"),
                TypeToken.getParameterized(List.class, entryClass).getType());
        Map<Integer, String> savedNotes = GSON.fromJson(state.getString("notes"),
                new TypeToken<Map<Integer, String>>() { }.getType());
        entries.clear();
        entries.addAll(savedEntries);
        notes.clear();
        notes.putAll(savedNotes);
    }

    public static Snapshot fromLegacy(Bundle state, Class<?> entryClass) {
        List<Object> entries = GSON.fromJson(state.getString("entries"),
                TypeToken.getParameterized(List.class, entryClass).getType());
        Map<Integer, String> notes = GSON.fromJson(state.getString("notes"),
                new TypeToken<Map<Integer, String>>() { }.getType());
        if (entries == null || notes == null) throw new IllegalArgumentException("Incomplete legacy draft");
        return new Snapshot(entries, notes);
    }
}
