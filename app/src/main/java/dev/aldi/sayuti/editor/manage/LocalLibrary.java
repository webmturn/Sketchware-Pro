package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.formatFileSize;
import static pro.sketchware.utility.FileUtil.getFileSize;
import static pro.sketchware.utility.FileUtil.readFile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocalLibrary {
    private final String name;
    private final String size;
    private boolean isSelected;
    /** True if this library was explicitly downloaded (has a dependency-tree.json). */
    private final boolean isRootLibrary;
    /** Folder names of locally-downloaded (non-built-in) transitive deps. */
    private final List<String> subDependencyNames;
    /** All transitive dep descriptions (including built-in), for UI display. */
    private final List<DepInfo> allDependencies;

    /** Lightweight descriptor for a transitive dependency entry. */
    public static class DepInfo {
        public final String folderName;   // e.g. "okhttp-android-v5.1.0"
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final boolean builtIn;

        DepInfo(String folderName, String groupId, String artifactId, String version, boolean builtIn) {
            this.folderName = folderName;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.builtIn = builtIn;
        }
    }

    private LocalLibrary(String name, String size, boolean isRootLibrary,
                         List<String> subDependencyNames, List<DepInfo> allDependencies) {
        this.name = name;
        this.size = size;
        this.isRootLibrary = isRootLibrary;
        this.subDependencyNames = subDependencyNames;
        this.allDependencies = allDependencies;
    }

    public static LocalLibrary fromFile(File file) {
        File depTreeFile = new File(file, "dependency-tree.json");
        List<String> subDeps = new ArrayList<>();
        List<DepInfo> allDeps = new ArrayList<>();
        boolean isRoot = depTreeFile.exists();
        if (isRoot) {
            try {
                String json = readFile(depTreeFile.getAbsolutePath());
                List<Map<String, Object>> entries = new Gson().fromJson(
                        json, new TypeToken<List<Map<String, Object>>>() {}.getType());
                if (entries != null) {
                    for (Map<String, Object> entry : entries) {
                        String artifactId = (String) entry.get("artifactId");
                        String version = (String) entry.get("version");
                        String groupId = (String) entry.get("groupId");
                        boolean builtIn = Boolean.TRUE.equals(entry.get("builtIn"));
                        if (artifactId != null && version != null) {
                            String folderName = artifactId + "-v" + version;
                            allDeps.add(new DepInfo(folderName, groupId != null ? groupId : "",
                                    artifactId, version, builtIn));
                            if (!builtIn) {
                                subDeps.add(folderName);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return new LocalLibrary(file.getName(), formatFileSize(getFileSize(file)), isRoot, subDeps, allDeps);
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    /** Returns true if this library was explicitly downloaded (has dependency-tree.json). */
    public boolean isRootLibrary() {
        return isRootLibrary;
    }

    /** Returns the folder names of non-built-in transitive deps (used for smart deletion). */
    public List<String> getSubDependencyNames() {
        return Collections.unmodifiableList(subDependencyNames);
    }

    /** Returns all transitive deps including built-in ones (used for UI display). */
    public List<DepInfo> getAllDependencies() {
        return Collections.unmodifiableList(allDependencies);
    }
}
