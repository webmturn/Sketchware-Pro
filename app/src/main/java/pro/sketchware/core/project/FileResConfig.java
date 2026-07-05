package pro.sketchware.core.project;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;

import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.util.LogUtil;

public class FileResConfig {

    public ArrayList<String> listBroadcastManifest = new ArrayList<>();
    public ArrayList<String> listFileAssets = new ArrayList<>();
    public ArrayList<String> listFileBroadcast = new ArrayList<>();
    public ArrayList<String> listFileImport = new ArrayList<>();
    public ArrayList<String> listFileJava = new ArrayList<>();
    public ArrayList<String> listFileNativeLibs = new ArrayList<>();
    public ArrayList<String> listFilePermission = new ArrayList<>();
    public ArrayList<String> listFileResource = new ArrayList<>();
    public ArrayList<String> listFileService = new ArrayList<>();
    public ArrayList<String> listJavaManifest = new ArrayList<>();
    public ArrayList<String> listServiceManifest = new ArrayList<>();
    public String numProj;
    private boolean assetsLoaded, broadcastLoaded, serviceLoaded;

    public FileResConfig(String sc_id) {
        numProj = sc_id;
        if ((sc_id == null || sc_id.isEmpty())) return;

        String permissions = FileUtil.readFile(SketchwarePaths.getProjectPermissionPath(numProj));
        if (permissions.isEmpty()) return;

        try {
            listFilePermission = new Gson().fromJson(permissions, Helper.TYPE_STRING);
        } catch (JsonSyntaxException e) {
            LogUtil.w("FileResConfig", "Failed to parse JSON config for project", e);
        }
    }

    public ArrayList<String> getNativelibsFile(String dirPath) {
        listFileNativeLibs.clear();
        FileUtil.listDir(dirPath, listFileNativeLibs);
        return listFileNativeLibs;
    }

    public String getPackageNameProject() {
        return "mod.agus.jcoderz";
    }

    /**
     * This helper method will clear and list a specified path into the existing list
     *
     * @return The listed files
     */
    private ArrayList<String> listDir(String dirPath, ArrayList<String> existing_list) {
        existing_list.clear();
        FileUtil.listDir(dirPath, existing_list);
        return existing_list;
    }

    public ArrayList<String> getJavaFile() {
        return listDir(SketchwarePaths.getProjectJavaPath(numProj), listFileJava);
    }

    public ArrayList<String> getAssetsFile() {
        if (!assetsLoaded) {
            listDir(SketchwarePaths.getProjectAssetsPath(numProj), listFileAssets);
            assetsLoaded = true;
        }
        return listFileAssets;
    }

    public ArrayList<String> getResourceFile(String dirPath) {
        return listDir(dirPath, listFileResource);
    }

    public ArrayList<String> getPermissionList() {
        return listFilePermission;
    }

    public ArrayList<String> getImportList() {
        String readFile = FileUtil.readFile(SketchwarePaths.getProjectImportPath(numProj));
        if (readFile.isEmpty()) return listFileImport;

        try {
            listFileImport = new Gson().fromJson(readFile, Helper.TYPE_STRING);
        } catch (JsonSyntaxException e) {
            LogUtil.w("FileResConfig", "Failed to parse JSON config for project", e);
        }

        return listFileImport;
    }

    public ArrayList<String> getBroadcastFile() {
        if (!broadcastLoaded) {
            listDir(SketchwarePaths.getProjectBroadcastPath(numProj), listFileBroadcast);
            broadcastLoaded = true;
        }
        return listFileBroadcast;
    }

    public ArrayList<String> getServiceFile() {
        if (!serviceLoaded) {
            listDir(SketchwarePaths.getProjectServicePath(numProj), listFileService);
            serviceLoaded = true;
        }
        return listFileService;
    }

    public ArrayList<String> getJavaManifestList() {
        String readFile = FileUtil.readFile(SketchwarePaths.getProjectJavaManifestPath(numProj));
        if (readFile.isEmpty()) return listJavaManifest;

        try {
            listJavaManifest = new Gson().fromJson(readFile, Helper.TYPE_STRING);
        } catch (JsonSyntaxException e) {
            LogUtil.w("FileResConfig", "Failed to parse JSON config for project", e);
        }

        return listJavaManifest;
    }

    public ArrayList<String> getBroadcastManifestList() {
        String readFile = FileUtil.readFile(SketchwarePaths.getProjectBroadcastManifestPath(numProj));
        if (readFile.isEmpty()) return listBroadcastManifest;

        try {
            listBroadcastManifest = new Gson().fromJson(readFile, Helper.TYPE_STRING);
        } catch (JsonSyntaxException e) {
            LogUtil.w("FileResConfig", "Failed to parse JSON config for project", e);
        }

        return listBroadcastManifest;
    }

    public ArrayList<String> getServiceManifestList() {
        String readFile = FileUtil.readFile(SketchwarePaths.getProjectServiceManifestPath(numProj));
        if (readFile.isEmpty()) return listServiceManifest;

        try {
            listServiceManifest = new Gson().fromJson(readFile, Helper.TYPE_STRING);
        } catch (JsonSyntaxException e) {
            LogUtil.w("FileResConfig", "Failed to parse JSON config for project", e);
        }

        return listServiceManifest;
    }
}
