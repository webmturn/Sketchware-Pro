package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.deleteFile;
import static pro.sketchware.utility.FileUtil.getExternalStorageDir;
import static pro.sketchware.utility.FileUtil.isExistFile;
import static pro.sketchware.utility.FileUtil.listDirAsFile;
import static pro.sketchware.utility.FileUtil.readFile;
import static pro.sketchware.utility.FileUtil.writeFile;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mod.hey.studios.util.Helper;
import pro.sketchware.utility.FilePathUtil;

public class LocalLibrariesUtil {
    private static String getLocalLibsPath() {
        return FilePathUtil.getLocalLibsDir().getAbsolutePath() + "/";
    }

    private static String getFallbackLocalLibsPath() {
        return FilePathUtil.getLocalLibsFallbackDir().getAbsolutePath() + "/";
    }

    public static List<LocalLibrary> getAllLocalLibraries() {
        ArrayList<File> localLibraryFiles = new ArrayList<>();
        // Check primary shared storage path
        listDirAsFile(getLocalLibsPath(), localLibraryFiles);
        // Also check app-specific fallback path
        ArrayList<File> fallbackFiles = new ArrayList<>();
        listDirAsFile(getFallbackLocalLibsPath(), fallbackFiles);
        for (File legacyFile : fallbackFiles) {
            boolean alreadyExists = false;
            for (File newFile : localLibraryFiles) {
                if (newFile.getName().equals(legacyFile.getName())) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                localLibraryFiles.add(legacyFile);
            }
        }
        localLibraryFiles.sort(new LocalLibrariesComparator());

        List<LocalLibrary> localLibraries = new LinkedList<>();
        for (File libraryFile : localLibraryFiles) {
            if (libraryFile.isDirectory()) {
                localLibraries.add(LocalLibrary.fromFile(libraryFile));
            }
        }

        return localLibraries;
    }

    public static ArrayList<HashMap<String, Object>> getLocalLibraries(String scId) {
        File localLibFile = getLocalLibFile(scId);
        String fileContent;
        if (!localLibFile.exists() || (fileContent = readFile(localLibFile.getAbsolutePath())).isEmpty()) {
            writeFile(localLibFile.getAbsolutePath(), "[]");
            return new ArrayList<>();
        }
        try {
            return new Gson().fromJson(fileContent, Helper.TYPE_MAP_LIST);
        } catch (com.google.gson.JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    public static File getLocalLibFile(String scId) {
        return new File(getExternalStorageDir().concat("/.sketchware/data/").concat(scId.concat("/local_library")));
    }

    public static void rewriteLocalLibFile(String scId, String newContent) {
        writeFile(getLocalLibFile(scId).getAbsolutePath(), newContent);
    }

    /**
     * Deletes selected libraries with smart sub-dependency handling:
     * sub-deps shared by other root libraries are kept and reported.
     *
     * @return names of sub-deps that were kept because they are shared
     */
    public static List<String> deleteSelectedLocalLibraries(
            String scId,
            List<LocalLibrary> localLibraries,
            ArrayList<HashMap<String, Object>> projectUsedLibs) {
        // Build a reference map: subDepName -> list of root lib names that reference it
        Map<String, List<String>> refMap = buildSubDepReferenceMap(localLibraries);

        List<String> retained = new ArrayList<>();
        localLibraries.removeIf(library -> {
            if (!library.isSelected()) return false;

            // Attempt to delete sub-deps of this root library
            if (library.isRootLibrary()) {
                for (String subDep : library.getSubDependencyNames()) {
                    List<String> refs = refMap.getOrDefault(subDep, Collections.emptyList());
                    // Only delete if exclusively used by this library
                    if (refs.size() <= 1) {
                        deleteFile(getLocalLibsPath().concat(subDep));
                        deleteFile(getFallbackLocalLibsPath().concat(subDep));
                        removeFromProjectLibs(projectUsedLibs, subDep);
                    } else {
                        retained.add(subDep);
                    }
                }
            }

            // Delete the library itself
            deleteFile(getLocalLibsPath().concat(library.getName()));
            deleteFile(getFallbackLocalLibsPath().concat(library.getName()));
            removeFromProjectLibs(projectUsedLibs, library.getName());
            return true;
        });

        if (projectUsedLibs != null) {
            rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
        }
        return retained;
    }

    private static void removeFromProjectLibs(
            ArrayList<HashMap<String, Object>> projectUsedLibs, String name) {
        if (projectUsedLibs == null) return;
        projectUsedLibs.removeIf(map -> name.equals(String.valueOf(map.get("name"))));
    }

    /**
     * Builds a map from sub-dependency folder name to the list of root library names
     * that include it in their dependency-tree.json.
     */
    public static Map<String, List<String>> buildSubDepReferenceMap(List<LocalLibrary> libraries) {
        Map<String, List<String>> refMap = new HashMap<>();
        for (LocalLibrary lib : libraries) {
            if (!lib.isRootLibrary()) continue;
            for (String subDep : lib.getSubDependencyNames()) {
                refMap.computeIfAbsent(subDep, k -> new ArrayList<>()).add(lib.getName());
            }
        }
        return refMap;
    }

    /**
     * Returns libraries that are not root libraries AND are not referenced by any
     * root library's dependency-tree.json — i.e., they are orphaned transitive deps.
     */
    public static List<LocalLibrary> getOrphanLibraries() {
        List<LocalLibrary> all = getAllLocalLibraries();
        Set<String> allReferenced = new HashSet<>();
        for (LocalLibrary lib : all) {
            if (lib.isRootLibrary()) {
                allReferenced.addAll(lib.getSubDependencyNames());
            }
        }
        List<LocalLibrary> orphans = new ArrayList<>();
        for (LocalLibrary lib : all) {
            if (!lib.isRootLibrary() && !allReferenced.contains(lib.getName())) {
                orphans.add(lib);
            }
        }
        return orphans;
    }

    /**
     * Returns sub-dependency folder names for a root library that are not already
     * enabled in the project's local lib file.
     */
    public static List<String> getMissingSubDeps(
            LocalLibrary rootLib,
            ArrayList<HashMap<String, Object>> projectUsedLibs) {
        if (!rootLib.isRootLibrary()) return Collections.emptyList();
        Set<String> enabled = new HashSet<>();
        for (HashMap<String, Object> map : projectUsedLibs) {
            enabled.add(String.valueOf(map.get("name")));
        }
        List<String> missing = new ArrayList<>();
        for (String subDep : rootLib.getSubDependencyNames()) {
            if (!enabled.contains(subDep)) missing.add(subDep);
        }
        return missing;
    }

    public static HashMap<String, Object> createLibraryMap(String name, String dependency) {
        String basePath = resolveLibBasePath(name);
        String configPath = basePath + "/config";
        String resPath = basePath + "/res";
        String jarPath = basePath + "/classes.jar";
        String dexPath = basePath + "/classes.dex";
        String manifestPath = basePath + "/AndroidManifest.xml";
        String pgRulesPath = basePath + "/proguard.txt";
        String assetsPath = basePath + "/assets";

        HashMap<String, Object> localLibrary = new HashMap<>();
        localLibrary.put("name", name);
        if (dependency != null) {
            localLibrary.put("dependency", dependency);
        }
        if (isExistFile(configPath)) {
            localLibrary.put("packageName", readFile(configPath));
        }
        if (isExistFile(resPath)) {
            localLibrary.put("resPath", resPath);
        }
        if (isExistFile(jarPath)) {
            localLibrary.put("jarPath", jarPath);
        }
        if (isExistFile(dexPath)) {
            localLibrary.put("dexPath", dexPath);
        }
        if (isExistFile(manifestPath)) {
            localLibrary.put("manifestPath", manifestPath);
        }
        if (isExistFile(pgRulesPath)) {
            localLibrary.put("pgRulesPath", pgRulesPath);
        }
        if (isExistFile(assetsPath)) {
            localLibrary.put("assetsPath", assetsPath);
        }
        return localLibrary;
    }

    private static String resolveLibBasePath(String name) {
        String newPath = getLocalLibsPath() + name;
        if (new File(newPath).exists()) {
            return newPath;
        }
        String fallbackPath = getFallbackLocalLibsPath() + name;
        if (new File(fallbackPath).exists()) {
            return fallbackPath;
        }
        return newPath;
    }
}
