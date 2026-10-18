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
        File[] candidates = root.listFiles(File::isDirectory);
        if (candidates == null) return;
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        for (File directory : candidates) {
            if (!sessionName(directory.getName()) || activeSessions.contains(directory.getName())
                    || Files.isSymbolicLink(directory.toPath())) continue;
            File manifest = new File(directory, "manifest.json");
            File intent = new File(directory, "xml-commit.json");
            if (manifest.exists() || intent.exists() || Files.isSymbolicLink(manifest.toPath())
                    || Files.isSymbolicLink(intent.toPath())) continue;
            File[] children = directory.listFiles();
            if (children == null || directory.lastModified() >= cutoff) continue;
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
            if (!valid || activeSessions.contains(directory.getName()) || manifest.exists() || intent.exists()) continue;
            boolean deleted = true;
            for (File file : files) if (!file.delete()) deleted = false;
            if (!deleted) continue;
            if (pages != null && !pages.delete()) continue;
            directory.delete();
        }
    }

    private static boolean temporary(File file) { return file.getName().matches("\\.resource-.*\\.tmp"); }
    private static boolean sessionName(String name) {
        try { return name.equals(UUID.fromString(name).toString()); }
        catch (IllegalArgumentException invalid) { return false; }
    }
}
