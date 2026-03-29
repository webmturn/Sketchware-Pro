package mod.agus.jcoderz.editor.manage.library.locallibrary;

import android.os.Environment;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;

import dev.aldi.sayuti.editor.manage.LocalLibrariesUtil;
import dev.aldi.sayuti.editor.manage.LocalLibraryImportPackageIndex;
import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ManageLocalLibrary {

    private final String projectId;
    public ArrayList<HashMap<String, Object>> list;

    public ManageLocalLibrary(String sc_id) {
        projectId = sc_id;
        String localLibraryConfigPath = FilePathUtil.getPathLocalLibrary(projectId);
        if (FileUtil.isExistFile(localLibraryConfigPath)) {
            try {
                Gson gson = new Gson();
                list = gson.fromJson(FileUtil.readFile(localLibraryConfigPath), Helper.TYPE_MAP_LIST);

                if (list == null) {
                    LogUtil.w(getClass().getSimpleName(), "Read null from file " + localLibraryConfigPath + ", deleting invalid configuration.");
                    if (!new File(localLibraryConfigPath).delete()) {
                        LogUtil.e(getClass().getSimpleName(), "Couldn't delete file " + localLibraryConfigPath);
                    }

                    // fall-through to shared error handler
                } else {
                    ArrayList<HashMap<String, Object>> normalizedList = new ArrayList<>();
                    boolean changed = false;
                    for (HashMap<String, Object> entry : list) {
                        Object nameObject = entry.get("name");
                        if (!(nameObject instanceof String) || ((String) nameObject).isEmpty()) {
                            changed = true;
                            continue;
                        }

                        String name = (String) nameObject;
                        File primaryLibDir = new File(FilePathUtil.getLocalLibsDir(), name);
                        File fallbackLibDir = new File(FilePathUtil.getLocalLibsFallbackDir(), name);
                        if (!primaryLibDir.exists() && !fallbackLibDir.exists()) {
                            changed = true;
                            continue;
                        }

                        Object dependencyObject = entry.get("dependency");
                        String dependency = dependencyObject instanceof String && !((String) dependencyObject).isEmpty()
                                ? (String) dependencyObject : null;
                        HashMap<String, Object> normalizedEntry = LocalLibrariesUtil.createLibraryMap(name, dependency);
                        normalizedList.add(normalizedEntry);
                        if (!entry.equals(normalizedEntry)) {
                            changed = true;
                        }
                    }
                    list = normalizedList;
                    if (changed) {
                        FileUtil.writeFile(localLibraryConfigPath, gson.toJson(list));
                    }
                    return;
                }
            } catch (JsonParseException e) {
                // fall-through to shared error handler
            }

            SketchwareUtil.toastError(Helper.getResString(R.string.local_lib_error_invalid_config));
        }
        list = new ArrayList<>();
    }

    public ArrayList<String> getAssets() {
        ArrayList<String> assets = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);

            if (localLibrary.containsKey("assetsPath")) {
                Object assetsPath = localLibrary.get("assetsPath");

                if (assetsPath instanceof String) {
                    assets.add((String) assetsPath);
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_assets), i), Toast.LENGTH_LONG);
                }
            }
        }

        return assets;
    }

    public ArrayList<String> getDexLocalLibrary() {
        ArrayList<String> dexes = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            Object dexPath = localLibrary.get("dexPath");

            if (dexPath instanceof String) {
                dexes.add((String) dexPath);
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_dex), i), Toast.LENGTH_LONG);
            }
        }

        return dexes;
    }

    public ArrayList<String> getExtraDexes() {
        ArrayList<String> extraDexes = new ArrayList<>();

        for (String localLibraryDexPath : getDexLocalLibrary()) {
            File dexPath = new File(localLibraryDexPath);
            if (dexPath.getParentFile() != null) {
                File[] dexPathFiles = dexPath.getParentFile().listFiles();

                if (dexPathFiles != null) {
                    for (File dexPathFile : dexPathFiles) {
                        String dexPathFilename = dexPathFile.getName();
                        if (!dexPathFilename.equals("classes.dex")
                                && dexPathFilename.startsWith("classes")
                                && dexPathFilename.endsWith(".dex")) {
                            extraDexes.add(dexPathFile.getAbsolutePath());
                        }
                    }
                }
            }
        }

        return extraDexes;
    }

    public ArrayList<String> getGenLocalLibrary() {
        ArrayList<String> genPaths = new ArrayList<>();

        for (String packageName : getPackageNames()) {
            if (!packageName.isEmpty()) {
                File projectGenFolder = new File(Environment.getExternalStorageDirectory(),
                        ".sketchware/mysc/".concat(projectId).concat("/gen"));
                String rJavaPath = packageName.replace(".", File.separator)
                        .concat(File.separator).concat("R.java");
                genPaths.add(new File(projectGenFolder, rJavaPath).getAbsolutePath());
            }
        }

        return genPaths;
    }

    public ArrayList<String> getImportLocalLibrary() {
        LinkedHashSet<String> imports = new LinkedHashSet<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            ArrayList<String> importPackages = getImportPackages(localLibrary, i);

            if (importPackages.isEmpty()) {
                Object packageName = localLibrary.get("packageName");
                if (packageName instanceof String && !((String) packageName).isEmpty()) {
                    imports.add(((String) packageName).concat(".*"));
                }
                continue;
            }

            for (String packageName : importPackages) {
                if (!packageName.isEmpty()) {
                    imports.add(packageName.concat(".*"));
                }
            }
        }

        return new ArrayList<>(imports);
    }

    public ArrayList<File> getLocalLibraryJars() {
        ArrayList<File> jars = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            Object jarPath = localLibrary.get("jarPath");

            if (jarPath instanceof String) {
                jars.add(new File((String) jarPath));
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_jar), i, localLibrary.get("name")), Toast.LENGTH_LONG);
            }
        }

        return jars;
    }

    public String getJarLocalLibrary() {
        StringBuilder classpath = new StringBuilder();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            Object jarPath = localLibrary.get("jarPath");

            if (jarPath instanceof String) {
                classpath.append(":");
                classpath.append((String) jarPath);
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_jar), i, localLibrary.get("name")), Toast.LENGTH_LONG);
            }
        }

        return classpath.toString();
    }

    public ArrayList<String> getNativeLibs() {
        ArrayList<String> nativeLibraryDirectories = new ArrayList<>();

        for (String localLibraryDexPath : getDexLocalLibrary()) {
            File localLibraryDexFile = new File(localLibraryDexPath);
            File parentDir = localLibraryDexFile.getParentFile();
            if (parentDir == null) continue;
            File jniFolder = new File(parentDir, "jni");
            if (jniFolder.isDirectory()) {
                nativeLibraryDirectories.add(jniFolder.getAbsolutePath());
            }
        }

        return nativeLibraryDirectories;
    }

    public ArrayList<String> getPackageNames() {
        ArrayList<String> packageNames = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            if (localLibrary.containsKey("packageName")) {
                Object packageName = localLibrary.get("packageName");

                if (packageName instanceof String) {
                    packageNames.add((String) packageName);
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_package), i), Toast.LENGTH_LONG);
                }
            }
        }

        return packageNames;
    }

    private ArrayList<String> getImportPackages(HashMap<String, Object> localLibrary, int index) {
        ArrayList<String> importPackages = getConfiguredImportPackages(localLibrary, index);
        if (!importPackages.isEmpty()) {
            return importPackages;
        }

        String libraryName = localLibrary.get("name") instanceof String
                ? (String) localLibrary.get("name")
                : null;
        if (libraryName != null && !libraryName.isEmpty()) {
            return LocalLibraryImportPackageIndex.readPackages(
                    LocalLibrariesUtil.getLocalLibraryDirectory(libraryName));
        }

        return new ArrayList<>();
    }

    private ArrayList<String> getConfiguredImportPackages(HashMap<String, Object> localLibrary, int index) {
        ArrayList<String> importPackages = new ArrayList<>();
        Object configuredPackages = localLibrary.get("importPackages");
        if (configuredPackages == null) {
            return importPackages;
        }

        if (!(configuredPackages instanceof Collection)) {
            LogUtil.w(getClass().getSimpleName(),
                    "Invalid importPackages entry at index " + index + " for local library "
                            + localLibrary.get("name"));
            return importPackages;
        }

        for (Object configuredPackage : (Collection<?>) configuredPackages) {
            if (configuredPackage instanceof String) {
                String packageName = ((String) configuredPackage).trim();
                if (!packageName.isEmpty()) {
                    importPackages.add(packageName);
                }
            }
        }

        return importPackages;
    }

    public String getPackageNameLocalLibrary() {
        StringBuilder packageNames = new StringBuilder();

        for (String packageName : getPackageNames()) {
            if (!packageName.isEmpty()) {
                packageNames.append(packageName);
                packageNames.append(":");
            }
        }

        return packageNames.toString();
    }

    public ArrayList<String> getPgRules() {
        ArrayList<String> proguardRules = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            if (localLibrary.containsKey("pgRulesPath")) {
                Object proguardRulesPath = localLibrary.get("pgRulesPath");

                if (proguardRulesPath instanceof String) {
                    proguardRules.add((String) proguardRulesPath);
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_proguard), i), Toast.LENGTH_LONG);
                }
            }
        }

        return proguardRules;
    }

    public ArrayList<String> getResLocalLibrary() {
        ArrayList<String> localLibraryRes = new ArrayList<>();

        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            HashMap<String, Object> localLibrary = list.get(i);
            if (localLibrary.containsKey("resPath")) {
                Object resPath = localLibrary.get("resPath");

                if (resPath instanceof String) {
                    localLibraryRes.add((String) resPath);
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.local_lib_error_invalid_res), i), Toast.LENGTH_LONG);
                }
            }
        }

        return localLibraryRes;
    }
}
