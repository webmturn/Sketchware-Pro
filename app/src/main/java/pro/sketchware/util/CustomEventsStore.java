package pro.sketchware.util;

import android.util.AtomicFile;

import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.beans.CustomEvent;
import pro.sketchware.beans.CustomListener;
import pro.sketchware.core.project.SketchwarePaths;

/** Keeps listener names and their event references consistent, including interrupted saves. */
public final class CustomEventsStore {
    public static final CustomEventsStore INSTANCE = new CustomEventsStore(
            new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_EVENTS_FILE)),
            new File(SketchwarePaths.getAbsolutePathOf(SketchwarePaths.CUSTOM_LISTENERS_FILE)));

    private final File eventsFile;
    private final File listenersFile;
    private final File rollbackFile;

    public CustomEventsStore(File eventsFile, File listenersFile) {
        this.eventsFile = eventsFile;
        this.listenersFile = listenersFile;
        rollbackFile = new File(eventsFile.getParentFile(), "custom-events-rollback.json");
    }

    public synchronized ArrayList<CustomEvent> readEvents() throws IOException {
        recover();
        return DefinitionFiles.readList(eventsFile, CustomEvent.class);
    }

    public synchronized ArrayList<CustomListener> readListeners() throws IOException {
        recover();
        return DefinitionFiles.readList(listenersFile, CustomListener.class);
    }

    public synchronized void saveEvents(List<CustomEvent> events) throws IOException {
        readEvents(); // Refuse to overwrite an unreadable existing file.
        DefinitionFiles.write(eventsFile, events);
    }

    public synchronized void saveBoth(List<CustomEvent> events, List<CustomListener> listeners) throws IOException {
        readEvents();
        readListeners();
        Rollback rollback = new Rollback();
        rollback.version = 1;
        rollback.events = DefinitionFiles.read(eventsFile);
        rollback.listeners = DefinitionFiles.read(listenersFile);
        DefinitionFiles.write(rollbackFile, rollback);
        try {
            DefinitionFiles.write(eventsFile, events);
            DefinitionFiles.write(listenersFile, listeners);
            if (!rollbackFile.delete()) throw new IOException("Could not complete definition save");
        } catch (IOException e) {
            try {
                recover();
            } catch (IOException recoveryError) {
                e.addSuppressed(recoveryError);
            }
            throw e;
        }
    }

    private void recover() throws IOException {
        String json = DefinitionFiles.read(rollbackFile);
        if (json == null) return;
        Rollback rollback;
        try {
            rollback = GsonUtils.getGson().fromJson(json, Rollback.class);
            if (rollback == null || rollback.version == null || rollback.version != 1) {
                throw new JsonParseException("Invalid rollback data");
            }
        } catch (JsonParseException e) {
            throw new IOException("Could not recover definitions", e);
        }
        restore(eventsFile, rollback.events);
        restore(listenersFile, rollback.listeners);
        if (!rollbackFile.delete()) throw new IOException("Could not complete definition recovery");
    }

    private static void restore(File file, String text) throws IOException {
        if (text != null) DefinitionFiles.writeText(file, text);
        else new AtomicFile(file).delete();
    }

    private static final class Rollback {
        Integer version;
        String events;
        String listeners;
    }
}
