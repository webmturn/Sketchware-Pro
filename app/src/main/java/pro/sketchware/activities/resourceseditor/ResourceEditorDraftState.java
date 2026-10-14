package pro.sketchware.activities.resourceseditor;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/** Keeps editor models and comments intact without parsing or writing resource XML. */
public final class ResourceEditorDraftState {
    private static final Gson GSON = new Gson();

    private ResourceEditorDraftState() { }

    public interface Owner {
        @Nullable Bundle saveDraftState();
        void restoreDraftState(@NonNull Bundle state, String filePath);
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
}
