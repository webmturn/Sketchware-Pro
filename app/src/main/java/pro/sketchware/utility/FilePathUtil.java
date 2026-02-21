package pro.sketchware.utility;

import android.os.Environment;

import java.io.File;

public class FilePathUtil {

    private static final File SKETCHWARE_DATA = new File(Environment.getExternalStorageDirectory(), ".sketchware/data/");
    private static final File SKETCHWARE_LOCAL_LIBS = new File(Environment.getExternalStorageDirectory(), ".sketchware/libs/local_libs");

    public static String getLastCompileLogPath(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/compile_log").getAbsolutePath();
    }

    public static String getPathPermission(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/permission").getAbsolutePath();
    }

    public static String getPathImport(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/import").getAbsolutePath();
    }

    public static String getPathBroadcast(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/broadcast").getAbsolutePath();
    }

    public static String getPathSvg(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/converted-vectors/").getAbsolutePath();
    }

    public static String getSvgFullPath(String sc_id, String resName) {
        return new File(getPathSvg(sc_id) + File.separator + resName + ".svg").getAbsolutePath();
    }

    public static String getPathService(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/service").getAbsolutePath();
    }

    public static String getPathAssets(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/assets").getAbsolutePath();
    }

    public static String getPathJava(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/java").getAbsolutePath();
    }

    public static String getPathKotlinCompilerPlugins(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/kt_plugins").getAbsolutePath();
    }

    public static String getPathResource(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/resource").getAbsolutePath();
    }

    public static String getPathProguard(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/proguard-rules.pro").getAbsolutePath();
    }

    public static String getPathLocalLibrary(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/local_library").getAbsolutePath();
    }

    public static String getJarPathLocalLibrary(String libraryName) {
        return new File(SKETCHWARE_LOCAL_LIBS, libraryName + "/classes.jar").getAbsolutePath();
    }

    public static String getDexPathLocalLibrary(String libraryName) {
        return new File(SKETCHWARE_LOCAL_LIBS, libraryName + "/classes.dex").getAbsolutePath();
    }

    public static String getResPathLocalLibrary(String libraryName) {
        return new File(SKETCHWARE_LOCAL_LIBS, libraryName + "/res").getAbsolutePath();
    }

    public static String getJarPathLocalLibraryUser(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/library/jar").getAbsolutePath();
    }

    public static String getDexPathLocalLibraryUser(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/library/dex").getAbsolutePath();
    }

    public static String getResPathLocalLibraryUser(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/library/res").getAbsolutePath();
    }

    public static String getManifestJava(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/java").getAbsolutePath();
    }

    public static String getManifestBroadcast(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/broadcast").getAbsolutePath();
    }

    public static String getPathNativelibs(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/files/native_libs").getAbsolutePath();
    }

    public static String getManifestService(String sc_id) {
        return new File(SKETCHWARE_DATA, sc_id + "/service").getAbsolutePath();
    }
}
