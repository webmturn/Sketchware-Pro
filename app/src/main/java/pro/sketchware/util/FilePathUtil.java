package pro.sketchware.util;

import java.io.File;

import pro.sketchware.core.project.SketchwarePaths;

public class FilePathUtil {

    /**
     * Returns the primary local libs directory (shared external storage).
     * Libraries stored here survive app uninstall.
     */
    public static File getLocalLibsDir() {
        return SketchwarePaths.getLocalLibsDir();
    }

    /**
     * Returns the fallback local libs directory using app-specific external storage.
     * This bypasses FUSE/MediaProvider restrictions on Android 11+ (e.g. Samsung Android 16)
     * that block file creation even with MANAGE_EXTERNAL_STORAGE granted.
     * Note: files here are deleted when the app is uninstalled.
     */
    public static File getLocalLibsFallbackDir() {
        return SketchwarePaths.getLocalLibsFallbackDir();
    }

    public static String getLastCompileLogPath(String sc_id) {
        return SketchwarePaths.getProjectCompileLogPath(sc_id);
    }

    public static String getPathPermission(String sc_id) {
        return SketchwarePaths.getProjectPermissionPath(sc_id);
    }

    public static String getPathImport(String sc_id) {
        return SketchwarePaths.getProjectImportPath(sc_id);
    }

    public static String getPathBroadcast(String sc_id) {
        return SketchwarePaths.getProjectBroadcastPath(sc_id);
    }

    public static String getPathSvg(String sc_id) {
        return SketchwarePaths.getProjectConvertedVectorsPath(sc_id);
    }

    public static String getSvgFullPath(String sc_id, String resName) {
        return SketchwarePaths.getProjectConvertedVectorFilePath(sc_id, resName);
    }

    public static String getPathService(String sc_id) {
        return SketchwarePaths.getProjectServicePath(sc_id);
    }

    public static String getPathAssets(String sc_id) {
        return SketchwarePaths.getProjectAssetsPath(sc_id);
    }

    public static String getPathJava(String sc_id) {
        return SketchwarePaths.getProjectJavaPath(sc_id);
    }

    public static String getPathKotlinCompilerPlugins(String sc_id) {
        return SketchwarePaths.getProjectKotlinCompilerPluginsPath(sc_id);
    }

    public static String getPathResource(String sc_id) {
        return SketchwarePaths.getProjectResourcePath(sc_id);
    }

    public static String getPathProguard(String sc_id) {
        return SketchwarePaths.getProjectProguardRulesPath(sc_id);
    }

    public static String getPathLocalLibrary(String sc_id) {
        return SketchwarePaths.getProjectLocalLibraryPath(sc_id);
    }

    public static String getJarPathLocalLibrary(String libraryName) {
        return SketchwarePaths.getLocalLibraryJarPath(libraryName);
    }

    public static String getDexPathLocalLibrary(String libraryName) {
        return SketchwarePaths.getLocalLibraryDexPath(libraryName);
    }

    public static String getResPathLocalLibrary(String libraryName) {
        return SketchwarePaths.getLocalLibraryResPath(libraryName);
    }

    public static String getJarPathLocalLibraryUser(String sc_id) {
        return SketchwarePaths.getProjectLibraryJarPath(sc_id);
    }

    public static String getDexPathLocalLibraryUser(String sc_id) {
        return SketchwarePaths.getProjectLibraryDexPath(sc_id);
    }

    public static String getResPathLocalLibraryUser(String sc_id) {
        return SketchwarePaths.getProjectLibraryResPath(sc_id);
    }

    public static String getManifestJava(String sc_id) {
        return SketchwarePaths.getProjectJavaManifestPath(sc_id);
    }

    public static String getManifestBroadcast(String sc_id) {
        return SketchwarePaths.getProjectBroadcastManifestPath(sc_id);
    }

    public static String getPathNativelibs(String sc_id) {
        return SketchwarePaths.getProjectNativeLibsPath(sc_id);
    }

    public static String getManifestService(String sc_id) {
        return SketchwarePaths.getProjectServiceManifestPath(sc_id);
    }
}
