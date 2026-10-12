package pro.sketchware.core.library;

import static pro.sketchware.util.FileUtil.formatFileSize;
import static pro.sketchware.util.FileUtil.getFileSize;
import static pro.sketchware.util.FileUtil.readFile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import pro.sketchware.util.LogUtil;

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
    private final String coordinate;

    /** Lightweight descriptor for a transitive dependency entry. */
    public static class DepInfo {
        public final String folderName;   // e.g. "okhttp-android-v5.1.0"
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final boolean builtIn;
        public final int depth;           // tree depth (1 = direct dep of root)
        public final String parentCoord;  // Maven coordinate of parent, e.g. "com.squareup.okhttp3:okhttp:5.1.0"
        public final List<String> parentCoords;
        public final boolean hierarchyKnown;

        DepInfo(String folderName, String groupId, String artifactId, String version,
                boolean builtIn, int depth, List<String> parentCoords, boolean hierarchyKnown) {
            this.folderName = folderName;
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.builtIn = builtIn;
            this.depth = depth;
            this.parentCoords = List.copyOf(parentCoords);
            this.parentCoord = parentCoords.isEmpty() ? null : parentCoords.get(0);
            this.hierarchyKnown = hierarchyKnown;
        }
    }

    private LocalLibrary(String name, String size, boolean isRootLibrary,
                         List<String> subDependencyNames, List<DepInfo> allDependencies, String coordinate) {
        this.name = name;
        this.size = size;
        this.isRootLibrary = isRootLibrary;
        this.subDependencyNames = subDependencyNames;
        this.allDependencies = allDependencies;
        this.coordinate = coordinate;
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
                        Object artifactIdValue = entry.get("artifactId");
                        Object versionValue = entry.get("version");
                        Object groupIdValue = entry.get("groupId");
                        String artifactId = artifactIdValue != null ? artifactIdValue.toString() : null;
                        String version = versionValue != null ? versionValue.toString() : null;
                        String groupId = groupIdValue != null ? groupIdValue.toString() : null;
                        boolean builtIn = Boolean.TRUE.equals(entry.get("builtIn"));
                        if (artifactId != null && version != null) {
                            String folderName = artifactId + "-v" + version;
                            int depth = 1;
                            Object depthObj = entry.get("depth");
                            if (depthObj instanceof Number) depth = ((Number) depthObj).intValue();
                            List<String> parents = new ArrayList<>();
                            if (entry.get("parents") instanceof List<?> parentValues) {
                                for (Object value : parentValues) {
                                    if (value instanceof String parent && !parent.isEmpty() && !parents.contains(parent)) {
                                        parents.add(parent);
                                    }
                                }
                            }
                            if (parents.isEmpty() && entry.get("parent") instanceof String parent && !parent.isEmpty()) {
                                parents.add(parent);
                            }
                            allDeps.add(new DepInfo(folderName, groupId != null ? groupId : "",
                                    artifactId, version, builtIn, depth, parents,
                                    !parents.isEmpty() && !Boolean.FALSE.equals(entry.get("hierarchyKnown"))));
                            if (!builtIn) {
                                subDeps.add(folderName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.w("LocalLibrary", "Failed to parse dependency tree for " + file.getName() + ": " + e.getMessage());
            }
        }
        String coordinate = "";
        File coordinateFile = new File(file, "maven-coordinate");
        if (coordinateFile.isFile()) {
            String stored = readFile(coordinateFile.getAbsolutePath()).trim();
            if (stored.split(":", -1).length == 3) coordinate = stored;
        }
        if (coordinate.isEmpty()) {
            for (DepInfo dep : allDeps) {
                for (String parent : dep.parentCoords) {
                    String[] parts = parent.split(":", -1);
                    if (parts.length == 3 && file.getName().equals(parts[1] + "-v" + parts[2])) {
                        coordinate = parent;
                        break;
                    }
                }
                if (!coordinate.isEmpty()) break;
            }
        }
        return new LocalLibrary(file.getName(), formatFileSize(getFileSize(file)), isRoot, subDeps, allDeps, coordinate);
    }

    public String getName() {
        return name;
    }

    public String getCoordinate() { return coordinate; }

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
