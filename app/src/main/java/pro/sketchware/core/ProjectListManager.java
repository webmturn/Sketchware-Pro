package pro.sketchware.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class ProjectListManager {
    public static SharedPrefsHelper sharedPrefsHelper;

    public static ArrayList<HashMap<String, Object>> listProjects() {
        String projectFileName = "project";
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        File[] listFiles = new File(SketchwarePaths.getProjectListBasePath()).listFiles();
        if (listFiles == null) {
            return arrayList;
        }
        for (File file : listFiles) {
            try {
                if (new File(file, projectFileName).exists()) {
                    String path = file.getAbsolutePath() + File.separator + projectFileName;
                    HashMap<String, Object> a = GsonMapHelper.fromJson(fileUtil.decryptToString(fileUtil.readFileBytes(path)));
                    if (MapValueHelper.getString(a, "sc_id").equals(file.getName())) {
                        arrayList.add(a);
                    }
                }
            } catch (Throwable e) {
                Log.e("ProjectListManager", e.getMessage(), e);
            }
        }
        return arrayList;
    }

    public static HashMap<String, Object> getProjectByPackageName(String packageName) {
        for (HashMap<String, Object> stringObjectHashMap : listProjects()) {
            if (MapValueHelper.getString(stringObjectHashMap, "my_sc_pkg_name").equals(packageName) && MapValueHelper.getInt(stringObjectHashMap, "proj_type") == 1) {
                return stringObjectHashMap;
            }
        }
        return null;
    }

    public static void deleteProject(Context context, String projectId) {
        File file = new File(SketchwarePaths.getProjectListPath(projectId));
        if (file.exists()) {
            EncryptedFileUtil fileUtil = new EncryptedFileUtil();
            fileUtil.deleteDirectory(file);
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getMyscPath(projectId));
            StringBuilder pathBuilder = new StringBuilder();
            pathBuilder.append(SketchwarePaths.getImagesPath());
            pathBuilder.append(File.separator);
            pathBuilder.append(projectId);
            fileUtil.deleteDirectoryByPath(pathBuilder.toString());
            pathBuilder = new StringBuilder();
            pathBuilder.append(SketchwarePaths.getSoundsPath());
            pathBuilder.append(File.separator);
            pathBuilder.append(projectId);
            fileUtil.deleteDirectoryByPath(pathBuilder.toString());
            pathBuilder = new StringBuilder();
            pathBuilder.append(SketchwarePaths.getFontsResourcePath());
            pathBuilder.append(File.separator);
            pathBuilder.append(projectId);
            fileUtil.deleteDirectoryByPath(pathBuilder.toString());
            pathBuilder = new StringBuilder();
            pathBuilder.append(SketchwarePaths.getIconsPath());
            pathBuilder.append(File.separator);
            pathBuilder.append(projectId);
            fileUtil.deleteDirectoryByPath(pathBuilder.toString());
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getDataPath(projectId));
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getBackupPath(projectId));
            pathBuilder = new StringBuilder();
            pathBuilder.append("D01_");
            pathBuilder.append(projectId);
            new SharedPrefsHelper(context, pathBuilder.toString()).clearAll();
            pathBuilder = new StringBuilder();
            pathBuilder.append("D02_");
            pathBuilder.append(projectId);
            new SharedPrefsHelper(context, pathBuilder.toString()).clearAll();
            pathBuilder = new StringBuilder();
            pathBuilder.append("D03_");
            pathBuilder.append(projectId);
            new SharedPrefsHelper(context, pathBuilder.toString()).clearAll();
            pathBuilder = new StringBuilder();
            pathBuilder.append("D04_");
            pathBuilder.append(projectId);
            new SharedPrefsHelper(context, pathBuilder.toString()).clearAll();
        }
    }

    public static void initializeDb(Context context, boolean unused) {
        if (sharedPrefsHelper == null) {
            sharedPrefsHelper = new SharedPrefsHelper(context, "P15");
        }
    }

    public static void saveProject(String projectId, HashMap<String, Object> projectData) {
        File file = new File(SketchwarePaths.getProjectListBasePath());
        if (!file.exists()) {
            file.mkdirs();
        }
        String path = SketchwarePaths.getProjectListPath(projectId);
        path = path + File.separator + "project";
        String jsonData = GsonMapHelper.toJson(projectData);
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        try {
            fileUtil.writeBytes(path, fileUtil.encryptString(jsonData));
        } catch (Throwable e) {
            Log.e("ProjectListManager", e.getMessage(), e);
        }
    }

    public static String getNextProjectId() {
        int parseInt = Integer.parseInt("600") + 1;
        for (HashMap<String, Object> stringObjectHashMap : listProjects()) {
            parseInt = Math.max(parseInt, Integer.parseInt(MapValueHelper.getString(stringObjectHashMap, "sc_id")) + 1);
        }
        return String.valueOf(parseInt);
    }

    public static HashMap<String, Object> getProjectById(String projectId) {
        Throwable e;
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        HashMap<String, Object> projectData = null;
        try {
            String c = SketchwarePaths.getProjectListPath(projectId);
            if (!new File(c).exists()) {
                return null;
            }
            String path = c + File.separator + "project";
            HashMap<String, Object> a = GsonMapHelper.fromJson(fileUtil.decryptToString(fileUtil.readFileBytes(path)));
            try {
                return !MapValueHelper.getString(a, "sc_id").equals(projectId) ? null : a;
            } catch (Exception e2) {
                e = e2;
                projectData = a;
                Log.e("ProjectListManager", e.getMessage(), e);
                return projectData;
            }
        } catch (Exception e3) {
            e = e3;
            Log.e("ProjectListManager", e.getMessage(), e);
            return projectData;
        }
    }

    public static void updateProject(String projectId, HashMap<String, Object> projectData) {
        File file = new File(SketchwarePaths.getProjectListPath(projectId));
        if (file.exists()) {
            String path = file + File.separator + "project";
            EncryptedFileUtil fileUtil = new EncryptedFileUtil();
            try {
                HashMap<String, Object> a = GsonMapHelper.fromJson(fileUtil.decryptToString(fileUtil.readFileBytes(path)));
                if (MapValueHelper.getString(a, "sc_id").equals(projectId)) {
                    if (projectData.containsKey("isIconAdaptive")) {
                        a.put("isIconAdaptive", projectData.get("isIconAdaptive"));
                    }
                    if (projectData.containsKey("custom_icon")) {
                        a.put("custom_icon", projectData.get("custom_icon"));
                    }
                    a.put("my_sc_pkg_name", projectData.get("my_sc_pkg_name"));
                    a.put("my_ws_name", projectData.get("my_ws_name"));
                    a.put("my_app_name", projectData.get("my_app_name"));
                    a.put("sc_ver_code", projectData.get("sc_ver_code"));
                    a.put("sc_ver_name", projectData.get("sc_ver_name"));
                    a.put("sketchware_ver", projectData.get("sketchware_ver"));
                    a.put("color_accent", projectData.get("color_accent"));
                    a.put("color_primary", projectData.get("color_primary"));
                    a.put("color_primary_dark", projectData.get("color_primary_dark"));
                    a.put("color_control_highlight", projectData.get("color_control_highlight"));
                    a.put("color_control_normal", projectData.get("color_control_normal"));
                    fileUtil.writeBytes(path, fileUtil.encryptString(GsonMapHelper.toJson(a)));
                }
            } catch (Throwable e) {
                Log.e("ProjectListManager", e.getMessage(), e);
            }
        }
    }

    public static String getNextWorkspaceName() {
        ArrayList<HashMap<String, Object>> var0 = listProjects();
        ArrayList<Integer> projectIndices = new ArrayList<>();

        for (HashMap<String, Object> stringObjectHashMap : var0) {
            String workspaceName = MapValueHelper.getString(stringObjectHashMap, "my_ws_name");
            if (workspaceName.equals("NewProject")) {
                projectIndices.add(1);
            } else if (workspaceName.indexOf("NewProject") == 0) {
                try {
                    projectIndices.add(Integer.parseInt(workspaceName.substring(10)));
                } catch (NumberFormatException e) {
                    Log.w("ProjectListManager", "Failed to parse project index from: " + workspaceName, e);
                }
            }
        }

        projectIndices.sort(new IntegerComparator());
        int var3 = 0;

        for (int nextProjectIndex : projectIndices) {
            int var5 = var3 + 1;
            if (nextProjectIndex == var5) {
                var3 = var5;
            } else {
                if (nextProjectIndex == var3) {
                    continue;
                }
                break;
            }
        }

        if (var3 == 0) {
            return "NewProject";
        } else {
            return "NewProject" + (var3 + 1);
        }
    }

    public static void syncAllProjects() {
        for (String projectKey : sharedPrefsHelper.getAll().keySet()) {
            saveProject(projectKey, sharedPrefsHelper.getMap(projectKey));
        }
        sharedPrefsHelper.clearAll();
    }

    private static class IntegerComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer first, Integer second) {
            return first.compareTo(second);
        }
    }
}
