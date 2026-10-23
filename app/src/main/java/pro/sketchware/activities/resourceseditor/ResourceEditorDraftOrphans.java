package pro.sketchware.activities.resourceseditor;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Collects only old, inactive first-write material that never acquired a manifest or intent. */
final class ResourceEditorDraftOrphans {
    private ResourceEditorDraftOrphans() { }

    static void prune(File root, Set<String> activeSessions) {
        if (Files.isSymbolicLink(root.toPath())) return;
        File[] candidates = root.listFiles();
        if (candidates == null) return;
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        for (File directory : candidates) pruneDirectory(directory, activeSessions, cutoff);
    }

    static void pruneDirectory(File directory, Set<String> activeSessions, long cutoff) {
        if (Files.isSymbolicLink(directory.getParentFile().toPath()) || !directory.isDirectory()) return;
        if (!sessionName(directory.getName()) || activeSessions.contains(directory.getName())
                || Files.isSymbolicLink(directory.toPath())) return;
        File manifest = new File(directory, "manifest.json");
        File intent = new File(directory, "xml-commit.json");
        if (manifest.exists() || intent.exists() || Files.isSymbolicLink(manifest.toPath())
                || Files.isSymbolicLink(intent.toPath())) return;
        File[] children = directory.listFiles();
        if (children == null || directory.lastModified() >= cutoff) return;
        List<File> files = new ArrayList<>();
        File pages = null;
        boolean valid = true;
        for (File child : children) {
            if (Files.isSymbolicLink(child.toPath()) || child.lastModified() >= cutoff) { valid = false; break; }
            if (child.getName().equals("pages") && child.isDirectory()) {
                pages = child;
                File[] objects = pages.listFiles();
                if (objects == null) { valid = false; break; }
                for (File object : objects) {
                    if (!object.isFile() || Files.isSymbolicLink(object.toPath()) || object.lastModified() >= cutoff
                            || !(object.getName().matches("[0-5]-[0-9a-f-]{36}\\.(json|bin|seg)")
                            || temporary(object))) { valid = false; break; }
                    files.add(object);
                }
                if (!valid) break;
            } else if (child.isFile() && temporary(child)) files.add(child);
            else { valid = false; break; }
        }
        // Recheck the activity/commit guards after inspecting every file, before any deletion.
        if (!valid || activeSessions.contains(directory.getName()) || manifest.exists() || intent.exists()
                || Files.isSymbolicLink(manifest.toPath()) || Files.isSymbolicLink(intent.toPath())) return;
        boolean deleted = true;
        for (File file : files) if (!file.delete()) deleted = false;
        if (!deleted) return;
        if (pages != null && !pages.delete()) return;
        directory.delete();
    }

    private static boolean temporary(File file) { return file.getName().matches("\\.resource-.*\\.tmp"); }
    private static boolean sessionName(String name) {
        try { return name.equals(UUID.fromString(name).toString()); }
        catch (IllegalArgumentException invalid) { return false; }
    }
}
