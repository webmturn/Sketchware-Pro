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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mod.hey.studios.util.Helper;
import pro.sketchware.utility.FilePathUtil;

public class LocalLibrariesUtil {
    private static String getLocalLibsPath() {
        return FilePathUtil.getLocalLibsDir().getAbsolutePath() + "/";
    }

    private static String getLegacyLocalLibsPath() {
        return FilePathUtil.getLocalLibsLegacyDir().getAbsolutePath() + "/";
    }

    public static List<LocalLibrary> getAllLocalLibraries() {
        ArrayList<File> localLibraryFiles = new ArrayList<>();
        // Check new app-specific path
        listDirAsFile(getLocalLibsPath(), localLibraryFiles);
        // Also check legacy path for backward compatibility
        ArrayList<File> legacyFiles = new ArrayList<>();
        listDirAsFile(getLegacyLocalLibsPath(), legacyFiles);
        for (File legacyFile : legacyFiles) {
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

    public static void deleteSelectedLocalLibraries(String scId, List<LocalLibrary> localLibraries, ArrayList<HashMap<String, Object>> projectUsedLibs) {
        localLibraries.removeIf(library -> {
            if (library.isSelected()) {
                deleteFile(getLocalLibsPath().concat(library.getName()));
                deleteFile(getLegacyLocalLibsPath().concat(library.getName()));
                if (projectUsedLibs != null) {
                    int indexToRemove = -1;
                    for (int i = 0; i < projectUsedLibs.size(); i++) {
                        Map<String, Object> libraryMap = projectUsedLibs.get(i);
                        if (library.getName().equals(String.valueOf(libraryMap.get("name")))) {
                            indexToRemove = i;
                            break;
                        }
                    }
                    if (indexToRemove != -1) {
                        projectUsedLibs.remove(indexToRemove);
                    }
                }
                return true;
            }
            return false;
        });
        if (projectUsedLibs != null)
            rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
    }

    public static File getLocalLibFile(String scId) {
        return new File(getExternalStorageDir().concat("/.sketchware/data/").concat(scId.concat("/local_library")));
    }

    public static void rewriteLocalLibFile(String scId, String newContent) {
        writeFile(getLocalLibFile(scId).getAbsolutePath(), newContent);
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
        String legacyPath = getLegacyLocalLibsPath() + name;
        if (new File(legacyPath).exists()) {
            return legacyPath;
        }
        return newPath;
    }
}
