package pro.sketchware.core;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import mod.jbk.util.LogUtil;

public class ProjectListManager {
    public static SharedPrefsHelper sharedPrefsHelper;

    private static String decryptProjectJson(EncryptedFileUtil fileUtil, String path) throws IOException {
        byte[] fileBytes = fileUtil.readFileBytes(path);
        if (fileBytes == null && new File(path).length() > 0L) {
            throw new IOException("Failed to read project metadata file: " + path);
        }
        return fileUtil.decryptToString(fileBytes);
    }

    private static byte[] encryptProjectJson(EncryptedFileUtil fileUtil, String jsonData, String path) throws IOException {
        try {
            return fileUtil.encryptString(jsonData);
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to encrypt project metadata file: " + path, e);
        }
    }

    public static ArrayList<HashMap<String, Object>> listProjects() {
        String projectFileName = "project";
        ArrayList<HashMap<String, Object>> projects = new ArrayList<>();
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        File[] listFiles = new File(SketchwarePaths.getProjectListBasePath()).listFiles();
        if (listFiles == null) {
            return projects;
        }
        for (File file : listFiles) {
            try {
                if (new File(file, projectFileName).exists()) {
                    String path = file.getAbsolutePath() + File.separator + projectFileName;
                    HashMap<String, Object> parsedProject = GsonMapHelper.fromJson(decryptProjectJson(fileUtil, path));
                    if (MapValueHelper.getString(parsedProject, "sc_id").equals(file.getName())) {
                        projects.add(parsedProject);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LogUtil.e("ProjectListManager", "Failed to load project metadata from " + file.getAbsolutePath(), e);
            }
        }
        return projects;
    }

    public static HashMap<String, Object> getProjectByPackageName(String packageName) {
        for (HashMap<String, Object> project : listProjects()) {
            if (MapValueHelper.getString(project, "my_sc_pkg_name").equals(packageName) && MapValueHelper.getInt(project, "proj_type") == 1) {
                return project;
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
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getImagesPath() + File.separator + projectId);
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getSoundsPath() + File.separator + projectId);
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getFontsResourcePath() + File.separator + projectId);
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getIconsPath() + File.separator + projectId);
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getDataPath(projectId));
            fileUtil.deleteDirectoryByPath(SketchwarePaths.getBackupPath(projectId));
            new SharedPrefsHelper(context, "D01_" + projectId).clearAll();
            new SharedPrefsHelper(context, "D02_" + projectId).clearAll();
            new SharedPrefsHelper(context, "D03_" + projectId).clearAll();
            new SharedPrefsHelper(context, "D04_" + projectId).clearAll();
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
            boolean saved = fileUtil.writeBytes(path, encryptProjectJson(fileUtil, jsonData, path));
            if (!saved) {
                LogUtil.e("ProjectListManager", "Failed to write project metadata for " + projectId + " to " + path);
            }
        } catch (IOException | RuntimeException e) {
            LogUtil.e("ProjectListManager", "Failed to save project metadata for " + projectId + " to " + path, e);
        }
    }

    public static String getNextProjectId() {
        int nextId = 601;
        for (HashMap<String, Object> project : listProjects()) {
            try {
                nextId = Math.max(nextId, Integer.parseInt(MapValueHelper.getString(project, "sc_id")) + 1);
            } catch (NumberFormatException e) {
                LogUtil.w("ProjectListManager", "Caught NumberFormatException", e);
            }
        }
        return String.valueOf(nextId);
    }

    public static HashMap<String, Object> getProjectById(String projectId) {
        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        String projectDir = SketchwarePaths.getProjectListPath(projectId);
        if (!new File(projectDir).exists()) {
            return null;
        }
        String path = projectDir + File.separator + "project";
        try {
            HashMap<String, Object> parsedProject = GsonMapHelper.fromJson(decryptProjectJson(fileUtil, path));
            try {
                return !MapValueHelper.getString(parsedProject, "sc_id").equals(projectId) ? null : parsedProject;
            } catch (RuntimeException e) {
                LogUtil.e("ProjectListManager", "Failed to validate project metadata for " + projectId + " from " + path, e);
                return parsedProject;
            }
        } catch (IOException | RuntimeException e) {
            LogUtil.e("ProjectListManager", "Failed to load project metadata for " + projectId + " from " + path, e);
            return null;
        }
    }

    public static void updateProject(String projectId, HashMap<String, Object> projectData) {
        File file = new File(SketchwarePaths.getProjectListPath(projectId));
        if (file.exists()) {
            String path = file + File.separator + "project";
            EncryptedFileUtil fileUtil = new EncryptedFileUtil();
            try {
                HashMap<String, Object> existingProject = GsonMapHelper.fromJson(decryptProjectJson(fileUtil, path));
                if (MapValueHelper.getString(existingProject, "sc_id").equals(projectId)) {
                    if (projectData.containsKey("isIconAdaptive")) {
                        existingProject.put("isIconAdaptive", projectData.get("isIconAdaptive"));
                    }
                    if (projectData.containsKey("custom_icon")) {
                        existingProject.put("custom_icon", projectData.get("custom_icon"));
                    }
                    existingProject.put("my_sc_pkg_name", projectData.get("my_sc_pkg_name"));
                    existingProject.put("my_ws_name", projectData.get("my_ws_name"));
                    existingProject.put("my_app_name", projectData.get("my_app_name"));
                    existingProject.put("sc_ver_code", projectData.get("sc_ver_code"));
                    existingProject.put("sc_ver_name", projectData.get("sc_ver_name"));
                    existingProject.put("sketchware_ver", projectData.get("sketchware_ver"));
                    existingProject.put("color_accent", projectData.get("color_accent"));
                    existingProject.put("color_primary", projectData.get("color_primary"));
                    existingProject.put("color_primary_dark", projectData.get("color_primary_dark"));
                    existingProject.put("color_control_highlight", projectData.get("color_control_highlight"));
                    existingProject.put("color_control_normal", projectData.get("color_control_normal"));
                    boolean saved = fileUtil.writeBytes(path, encryptProjectJson(fileUtil, GsonMapHelper.toJson(existingProject), path));
                    if (!saved) {
                        LogUtil.e("ProjectListManager", "Failed to write updated project metadata for " + projectId + " to " + path);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LogUtil.e("ProjectListManager", "Failed to update project metadata for " + projectId + " at " + path, e);
            }
        }
    }

    public static String getNextWorkspaceName() {
        ArrayList<HashMap<String, Object>> projects = listProjects();
        ArrayList<Integer> projectIndices = new ArrayList<>();

        for (HashMap<String, Object> project : projects) {
            String workspaceName = MapValueHelper.getString(project, "my_ws_name");
            if (workspaceName.equals("NewProject")) {
                projectIndices.add(1);
            } else if (workspaceName.indexOf("NewProject") == 0) {
                try {
                    projectIndices.add(Integer.parseInt(workspaceName.substring(10)));
                } catch (NumberFormatException e) {
                    LogUtil.w("ProjectListManager", "Failed to parse project index from: " + workspaceName, e);
                }
            }
        }

        projectIndices.sort(new IntegerComparator());
        int lastConsecutiveIndex = 0;

        for (int nextProjectIndex : projectIndices) {
            int expectedNext = lastConsecutiveIndex + 1;
            if (nextProjectIndex == expectedNext) {
                lastConsecutiveIndex = expectedNext;
            } else {
                if (nextProjectIndex == lastConsecutiveIndex) {
                    continue;
                }
                break;
            }
        }

        if (lastConsecutiveIndex == 0) {
            return "NewProject";
        } else {
            return "NewProject" + (lastConsecutiveIndex + 1);
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
