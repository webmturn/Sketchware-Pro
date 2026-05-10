package pro.sketchware.core.project;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import pro.sketchware.SketchApplication;
import pro.sketchware.core.SharedPrefsHelper;

public class SketchwarePaths {

    public static final String PROVIDED_STRINGS_PATH = "sketchware" + File.separator + "localization" + File.separator + "strings_provided.xml";
    public static final String SIGNED_APK_PATH = "sketchware" + File.separator + "signed_apk";
    public static final String SIGNED_AAB_PATH = "sketchware" + File.separator + "signed_aab";
    public static final String KEYSTORE_DIR_PATH = "sketchware" + File.separator + "keystore";
    public static final String KEYSTORE_FILE_PATH = "sketchware" + File.separator + "keystore" + File.separator + "release_key.jks";
    public static final String SERVICE_ACCOUNT_PATH = "sketchware" + File.separator + "service_account";
    public static final String UPLOAD_PATH = ".sketchware" + File.separator + "upload";
    public static final String LIBS_PATH = ".sketchware" + File.separator + "libs";
    public static final String MYSC_PATH = ".sketchware" + File.separator + "mysc";
    public static final String MYSC_LIST_PATH = ".sketchware" + File.separator + "mysc" + File.separator + "list";
    public static final String DATA_PATH = ".sketchware" + File.separator + "data";
    public static final String BACKUP_PATH = ".sketchware" + File.separator + "bak";
    public static final String TEMP_IMAGES_PATH = ".sketchware" + File.separator + "temp" + File.separator + "images";
    public static final String TEMP_SOUNDS_PATH = ".sketchware" + File.separator + "temp" + File.separator + "sounds";
    public static final String TEMP_FONTS_PATH = ".sketchware" + File.separator + "temp" + File.separator + "fonts";
    public static final String TEMP_PROJ_PATH = ".sketchware" + File.separator + "temp" + File.separator + "proj";
    public static final String TEMP_DATA_PATH = ".sketchware" + File.separator + "temp" + File.separator + "data";
    public static final String RESOURCES_PATH = ".sketchware" + File.separator + "resources";
    public static final String RESOURCES_ICONS_PATH = ".sketchware" + File.separator + "resources" + File.separator + "icons";
    public static final String RESOURCES_IMAGES_PATH = ".sketchware" + File.separator + "resources" + File.separator + "images";
    public static final String RESOURCES_SOUNDS_PATH = ".sketchware" + File.separator + "resources" + File.separator + "sounds";
    public static final String RESOURCES_FONTS_PATH = ".sketchware" + File.separator + "resources" + File.separator + "fonts";
    public static final String DOWNLOAD_APK_PATH = ".sketchware" + File.separator + "download" + File.separator + "apk";
    public static final String DOWNLOAD_DATA_PATH = ".sketchware" + File.separator + "download" + File.separator + "data";
    public static final String TUTORIAL_IMAGES_PATH = ".sketchware" + File.separator + "tutorial" + File.separator + "images";
    public static final String TUTORIAL_SOUNDS_PATH = ".sketchware" + File.separator + "tutorial" + File.separator + "sounds";
    public static final String TUTORIAL_FONTS_PATH = ".sketchware" + File.separator + "tutorial" + File.separator + "fonts";
    public static final String TUTORIAL_PROJ_PATH = ".sketchware" + File.separator + "tutorial" + File.separator + "proj";
    public static final String COLLECTION_PATH = ".sketchware" + File.separator + "collection";
    public static final String LOCALIZATION_PATH = "sketchware" + File.separator + "localization";
    public static final String LOCALIZATION_STRINGS_PATH = "sketchware" + File.separator + "localization" + File.separator + "strings.xml";

    public static final String EXTRA_SYSTEM_DATA = ".sketchware" + File.separator + "data" + File.separator + "system";
    public static final String CUSTOM_EVENTS_FILE = EXTRA_SYSTEM_DATA + File.separator + "events.json";
    public static final String CUSTOM_LISTENERS_FILE = EXTRA_SYSTEM_DATA + File.separator + "listeners.json";
    public static final String CUSTOM_COMPONENT_FILE = EXTRA_SYSTEM_DATA + File.separator + "component.json";
    public static final String REPOSITORIES_JSON_FILE = LIBS_PATH + File.separator + "repositories.json";
    public static final String EXTRA_DATA_EXPORT = EXTRA_SYSTEM_DATA + File.separator + "export";
    public static final String EVENT_EXPORT_PATH = EXTRA_DATA_EXPORT + File.separator + "events";
    public static final String SYSTEM_TEMP_PATH = EXTRA_SYSTEM_DATA + File.separator + "temp";
    public static final String SYSTEM_I18N_PATH = EXTRA_SYSTEM_DATA + File.separator + "i18n";
    public static final String SETTINGS_PATH = DATA_PATH + File.separator + "settings.json";
    public static final String DEBUG_LOG_PATH = ".sketchware" + File.separator + "debug.txt";
    public static final String LOGCAT_PATH = ".sketchware" + File.separator + "logcat";
    public static final String BLOCK_EXPORT_PATH = RESOURCES_PATH + File.separator + "block" + File.separator + "export";
    public static final String BACKUPS_PATH = ".sketchware" + File.separator + "backups";

    public static String getAbsolutePathOf(String relativePath) {
        return resolveExternalStoragePath(relativePath);
    }

    public static String resolveExternalStoragePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.equals(externalStoragePath) || path.startsWith(externalStoragePath + File.separator)) {
            return path;
        }
        if (path.equals(File.separator + ".sketchware")
                || path.startsWith(File.separator + ".sketchware" + File.separator)
                || path.equals(File.separator + "sketchware")
                || path.startsWith(File.separator + "sketchware" + File.separator)) {
            return new File(externalStoragePath, path.substring(1)).getAbsolutePath();
        }
        if (new File(path).isAbsolute()) {
            return path;
        }
        return new File(externalStoragePath, path).getAbsolutePath();
    }

    public static String resolveExternalStorageRelativePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.equals(externalStoragePath) || path.startsWith(externalStoragePath + File.separator)) {
            return path;
        }
        if (isLikelyAbsoluteStoragePath(path)) {
            return path;
        }
        if (path.startsWith(File.separator)) {
            return new File(externalStoragePath, path.substring(1)).getAbsolutePath();
        }
        return new File(externalStoragePath, path).getAbsolutePath();
    }

    private static boolean isLikelyAbsoluteStoragePath(String path) {
        if (matchesKnownAbsoluteStoragePathPrefix(path)) {
            return true;
        }
        Context appContext = SketchApplication.getAppContext();
        if (appContext == null) {
            return false;
        }
        File[] externalFilesDirs = appContext.getExternalFilesDirs(null);
        if (externalFilesDirs == null) {
            return false;
        }
        for (File externalFilesDir : externalFilesDirs) {
            String storageRootPath = getExternalStorageRootPath(externalFilesDir);
            if (storageRootPath != null
                    && (path.equals(storageRootPath)
                    || path.startsWith(storageRootPath + File.separator))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesKnownAbsoluteStoragePathPrefix(String path) {
        return path.equals(File.separator + "storage")
                || path.startsWith(File.separator + "storage" + File.separator)
                || path.equals(File.separator + "mnt")
                || path.startsWith(File.separator + "mnt" + File.separator)
                || path.equals(File.separator + "sdcard")
                || path.startsWith(File.separator + "sdcard" + File.separator)
                || path.equals(File.separator + "sdcard0")
                || path.startsWith(File.separator + "sdcard0" + File.separator)
                || path.equals(File.separator + "sdcard1")
                || path.startsWith(File.separator + "sdcard1" + File.separator)
                || path.equals(File.separator + "external_sd")
                || path.startsWith(File.separator + "external_sd" + File.separator)
                || path.equals(File.separator + "extSdCard")
                || path.startsWith(File.separator + "extSdCard" + File.separator)
                || path.equals(File.separator + "Removable")
                || path.startsWith(File.separator + "Removable" + File.separator)
                || path.equals(File.separator + "removable")
                || path.startsWith(File.separator + "removable" + File.separator);
    }

    private static String getExternalStorageRootPath(File externalFilesDir) {
        if (externalFilesDir == null) {
            return null;
        }
        String path = externalFilesDir.getAbsolutePath();
        String androidDataPath = File.separator + "Android" + File.separator + "data" + File.separator;
        int androidDataIndex = path.indexOf(androidDataPath);
        if (androidDataIndex <= 0) {
            return null;
        }
        return path.substring(0, androidDataIndex);
    }

    public static String toExternalStorageRelativePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (path.equals(externalStoragePath)) {
            return File.separator;
        }
        if (path.startsWith(externalStoragePath + File.separator)) {
            return path.substring(externalStoragePath.length());
        }
        return path;
    }

    public static String getCollectionPath() {
        return getAbsolutePathOf(COLLECTION_PATH);
    }

    public static String getSubscriptionLevel(int type) {
        return switch (type) {
            case 1 -> "SL-01";
            case 2 -> "SL-02";
            case 3 -> "SL-03";
            case 4 -> "SL-04";
            default -> "";
        };
    }

    public static String getBackupPath(String sc_id) {
        return getAbsolutePathOf(BACKUP_PATH + File.separator + sc_id);
    }

    public static String getBackupsPath() {
        return getAbsolutePathOf(BACKUPS_PATH);
    }

    public static String getSettingsPath() {
        return getAbsolutePathOf(SETTINGS_PATH);
    }

    public static String getDebugLogPath() {
        return getAbsolutePathOf(DEBUG_LOG_PATH);
    }

    public static String getRepositoriesJsonPath() {
        return getAbsolutePathOf(REPOSITORIES_JSON_FILE);
    }

    public static String getBlockExportPath() {
        return getAbsolutePathOf(BLOCK_EXPORT_PATH);
    }

    public static String getBlockSelectorMenuPath() {
        return getAbsolutePathOf(RESOURCES_PATH + File.separator + "block" + File.separator + "My Block"
                + File.separator + "menu.json");
    }

    public static String getBlockSelectorExportMenuPath() {
        return getBlockExportPath() + File.separator + "menu" + File.separator + "All_Menus.json";
    }

    public static String getBlockManagerBlockFilePath() {
        return getAbsolutePathOf(RESOURCES_PATH + File.separator + "block" + File.separator + "My Block"
                + File.separator + "block.json");
    }

    public static String getBlockManagerPaletteFilePath() {
        return getAbsolutePathOf(RESOURCES_PATH + File.separator + "block" + File.separator + "My Block"
                + File.separator + "palette.json");
    }

    public static String getSystemI18nPath() {
        return getAbsolutePathOf(SYSTEM_I18N_PATH);
    }

    public static String getLogcatPath(String packageName) {
        return getAbsolutePathOf(LOGCAT_PATH) + File.separator + packageName;
    }

    public static String getLogcatPath(String packageName, String fileName) {
        return getLogcatPath(packageName) + File.separator + fileName;
    }

    public static void clearPreferenceData(Context context, String preferenceName) {
        SharedPrefsHelper dataP17 = new SharedPrefsHelper(context, "P17_" + preferenceName);
        SharedPrefsHelper dataP18 = new SharedPrefsHelper(context, "P18_" + preferenceName);
        SharedPrefsHelper dataP13 = new SharedPrefsHelper(context, "P13_" + preferenceName);
        SharedPrefsHelper dataP14 = new SharedPrefsHelper(context, "P14_" + preferenceName);
        dataP17.clearAll();
        dataP18.clearAll();
        dataP13.clearAll();
        dataP14.clearAll();
        SharedPrefsHelper dataD03 = new SharedPrefsHelper(context, "D03_" + preferenceName);
        SharedPrefsHelper dataD04 = new SharedPrefsHelper(context, "D04_" + preferenceName);
        SharedPrefsHelper dataD01 = new SharedPrefsHelper(context, "D01_" + preferenceName);
        SharedPrefsHelper dataD02 = new SharedPrefsHelper(context, "D02_" + preferenceName);
        dataD03.clearAll();
        dataD04.clearAll();
        dataD01.clearAll();
        dataD02.clearAll();
    }

    public static String getDownloadDataPath() {
        return getAbsolutePathOf(DOWNLOAD_DATA_PATH);
    }

    public static String getWidgetTypeName(int type) {
        return switch (type) {
            case 0 -> "linear";
            case 1 -> "relativelayout";
            case 2 -> "hscroll";
            case 3 -> "button";
            case 4 -> "textview";
            case 5 -> "edittext";
            case 6 -> "imageview";
            case 7 -> "webview";
            case 8 -> "progressbar";
            case 9 -> "listview";
            case 10 -> "spinner";
            case 11 -> "checkbox";
            case 12 -> "vscroll";
            case 13 -> "switch";
            case 14 -> "seekbar";
            case 15 -> "calendarview";
            case 16 -> "fab";
            case 17 -> "adview";
            case 18 -> "mapview";
            case 19 -> "radiobutton";
            case 20 -> "ratingbar";
            case 21 -> "videoview";
            case 22 -> "searchview";
            case 23 -> "autocomplete";
            case 24 -> "multiautocomplete";
            case 25 -> "gridview";
            case 26 -> "analogclock";
            case 27 -> "datepicker";
            case 28 -> "timepicker";
            case 29 -> "digitalclock";
            case 30 -> "tablayout";
            case 31 -> "viewpager";
            case 32 -> "bottomnavigation";
            case 33 -> "badgeview";
            case 34 -> "patternlockview";
            case 35 -> "wavesidebar";
            case 36 -> "cardview";
            case 37 -> "collapsingtoolbar";
            case 38 -> "textinputlayout";
            case 39 -> "swiperefreshlayout";
            case 40 -> "radiogroup";
            case 41 -> "materialbutton";
            case 42 -> "signinbutton";
            case 43 -> "circleimageview";
            case 44 -> "lottie";
            case 45 -> "youtube";
            case 46 -> "otpview";
            case 47 -> "codeview";
            case 48 -> "recyclerview";
            default -> "widget";
        };
    }

    public static String getDataPath(String sc_id) {
        return getAbsolutePathOf(DATA_PATH + File.separator + sc_id);
    }

    public static String getProjectAssetsPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "assets";
    }

    public static String getProjectNativeLibsPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "native_libs";
    }

    public static String getProjectJavaPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "java";
    }

    public static String getProjectLayoutPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "resource"
                + File.separator + "layout";
    }

    public static String getProjectClasspathPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "classpath";
    }

    public static String getProjectCommandPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "command";
    }

    public static String getProjectSettingsPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "project_config";
    }

    public static String getProjectBuildConfigPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "build_config";
    }

    public static String getProjectStringfogConfigPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "stringfog";
    }

    public static String getProjectLocalLibraryPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "local_library";
    }

    public static String getProjectCustomBlocksPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "custom_blocks";
    }

    public static String getProjectExcludedBuiltInLibrariesPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "excluded_library";
    }

    public static String getProjectProguardConfigPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "proguard";
    }

    public static String getProjectProguardFullModeConfigPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "proguard_fm";
    }

    public static String getProjectProguardRulesPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "proguard-rules.pro";
    }

    public static String getProjectResourceValuesFilePath(String sc_id, String fileName) {
        return getDataPath(sc_id) + File.separator + "files" + File.separator + "resource"
                + File.separator + "values" + File.separator + fileName;
    }

    public static String getAndroidManifestInjectionPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "Injection" + File.separator + "androidmanifest";
    }

    public static String getAndroidManifestAttributesPath(String sc_id) {
        return getAndroidManifestInjectionPath(sc_id) + File.separator + "attributes.json";
    }

    public static String getAndroidManifestLauncherActivityPath(String sc_id) {
        return getAndroidManifestInjectionPath(sc_id) + File.separator + "activity_launcher.txt";
    }

    public static String getAndroidManifestActivitiesComponentsPath(String sc_id) {
        return getAndroidManifestInjectionPath(sc_id) + File.separator + "activities_components.json";
    }

    public static String getAndroidManifestAppComponentsPath(String sc_id) {
        return getAndroidManifestInjectionPath(sc_id) + File.separator + "app_components.txt";
    }

    public static String getAppCompatInjectionPath(String sc_id) {
        return getDataPath(sc_id) + File.separator + "injection" + File.separator + "appcompat";
    }

    public static String getAppCompatInjectionPath(String sc_id, String activityFilename) {
        return getAppCompatInjectionPath(sc_id) + File.separator + activityFilename;
    }

    public static String getDownloadPath() {
        return getAbsolutePathOf(".sketchware" + File.separator + "download");
    }

    public static String getProjectListPath(String sc_id) {
        return getAbsolutePathOf(MYSC_LIST_PATH + File.separator + sc_id);
    }

    public static String getFontsResourcePath() {
        return getAbsolutePathOf(RESOURCES_FONTS_PATH);
    }

    public static String getMyscPath(String sc_id) {
        return getAbsolutePathOf(MYSC_PATH + File.separator + sc_id);
    }

    public static String getIconsPath() {
        return getAbsolutePathOf(RESOURCES_ICONS_PATH);
    }

    public static String getResourceZipPath(String sc_id) {
        return "resource" + File.separator + sc_id + File.separator + "res.zip";
    }

    public static String getExtractedIconPackStoreLocation() {
        return new File(SketchApplication.getAppContext().getFilesDir(), "iconpack").getAbsolutePath();
    }

    public static String getImagesPath() {
        return getAbsolutePathOf(RESOURCES_IMAGES_PATH);
    }

    public static String getPublicKey() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxqe7Fu3i3VfnKRSRRljTsMuk7Br0dXaFdGnMNCzMGLQ72PSTEAUo4sXs5+Utmdf9R2s2tZyArdnehk9+Q72F0XzEZGeVfgzfLky7ffuk04yxUye/FlXBun/s0F7g2496+PyfXCP9jIBdncvQ9kaT8Xn6F/j0s2TqS/6xlCD38eYgCVFyp1mld1vYhGCZBlQvXFVJAoKCzqN2QZVO5KarkyTQSGeudvV/UQsJgyHh5zTZKnla1VIVj1Wl3nBb//s2dsmFnAx3500Y/h//XHveLUS7BkP34AGGWPLuoyJruLNvrZ3uUNDnCgnW4+z8Ilaj2SwCTeqQvvw/suZdExs88QIDAQAB";
    }

    @Deprecated
    public static String getKeystoreDirPath() {
        return getAbsolutePathOf(KEYSTORE_DIR_PATH);
    }

    public static String getKeystoreFilePath() {
        return getAbsolutePathOf(KEYSTORE_FILE_PATH);
    }

    public static String getLocalizationPath() {
        return getAbsolutePathOf(LOCALIZATION_PATH);
    }

    public static String getLocalizationStringsPath() {
        return getAbsolutePathOf(LOCALIZATION_STRINGS_PATH);
    }

    public static String getProvidedStringsPath() {
        return getAbsolutePathOf(PROVIDED_STRINGS_PATH);
    }

    public static String getProjectListBasePath() {
        return getAbsolutePathOf(MYSC_LIST_PATH);
    }

    public static String getSignedApkPath() {
        return getAbsolutePathOf(SIGNED_APK_PATH);
    }

    public static String getSignedAabPath() {
        return getAbsolutePathOf(SIGNED_AAB_PATH);
    }

    public static String getServiceAccountPath() {
        return getAbsolutePathOf(SERVICE_ACCOUNT_PATH);
    }

    public static String getSketchwareRootPath() {
        return getAbsolutePathOf(".sketchware" + File.separator);
    }

    public static String getLibsPath() {
        return getAbsolutePathOf(LIBS_PATH);
    }

    public static String getAndroidProguardRulesPath() {
        return getLibsPath() + File.separator + "android-proguard-rules.pro";
    }

    public static String getSketchwarePath() {
        return getAbsolutePathOf("sketchware");
    }

    public static String getSoundsPath() {
        return getAbsolutePathOf(RESOURCES_SOUNDS_PATH);
    }

    public static String getTempFontsPath(String sc_id) {
        return getAbsolutePathOf(TEMP_FONTS_PATH + File.separator + sc_id);
    }

    public static String getTempFontsPath() {
        return getAbsolutePathOf(TEMP_FONTS_PATH);
    }

    public static String getTempImagesPath(String sc_id) {
        return getAbsolutePathOf(TEMP_IMAGES_PATH + File.separator + sc_id);
    }

    public static String getTempImagesPath() {
        return getAbsolutePathOf(TEMP_IMAGES_PATH);
    }

    public static String getTempSoundsPath(String sc_id) {
        return getAbsolutePathOf(TEMP_SOUNDS_PATH + File.separator + sc_id);
    }

    public static String getTempSoundsPath() {
        return getAbsolutePathOf(TEMP_SOUNDS_PATH);
    }

    public static String getTempCommandsPath() {
        return getAbsolutePathOf(".sketchware" + File.separator + "temp" + File.separator + "commands");
    }

    public static String getSystemTempPath() {
        return getAbsolutePathOf(SYSTEM_TEMP_PATH);
    }

    public static String getSystemTempFilePath(String fileName) {
        return getSystemTempPath() + File.separator + fileName;
    }

    public static String getUploadPath() {
        return getAbsolutePathOf(UPLOAD_PATH);
    }

    public static String getCustomComponent() {
        return getAbsolutePathOf(CUSTOM_COMPONENT_FILE);
    }

    public static String getExtraDataExport() {
        return getAbsolutePathOf(EXTRA_DATA_EXPORT);
    }

    public static String getEncryptionKey() {
        return "16011998";
    }

    public static String[] getCipherConfig() {
        return new String[]{"Blowfish", "Blowfish/CBC/PKCS5Padding"};
    }
}
