package pro.sketchware.core.settings;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.GsonUtils;
import pro.sketchware.util.LogUtil;

/** Shared application configuration, independent of the settings screen. */
public final class ApplicationSettings {
    public static final String SETTING_ALWAYS_SHOW_BLOCKS = "always-show-blocks";
    public static final String SETTING_BACKUP_DIRECTORY = "backup-dir";
    public static final String SETTING_ROOT_AUTO_INSTALL_PROJECTS = "root-auto-install-projects";
    public static final String SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING = "root-auto-open-after-installing";
    public static final String SETTING_BACKUP_FILENAME = "backup-filename";
    public static final String SETTING_SHOW_BUILT_IN_BLOCKS = "built-in-blocks";
    public static final String SETTING_SHOW_EVERY_SINGLE_BLOCK = "show-every-single-block";
    public static final String SETTING_USE_NEW_VERSION_CONTROL = "use-new-version-control";
    public static final String SETTING_USE_ASD_HIGHLIGHTER = "use-asd-highlighter";
    public static final String SETTING_CRITICAL_UPDATE_REMINDER = "critical-update-reminder";
    public static final String SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH = "palletteDir";
    public static final String SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH = "blockDir";
    public static final String SETTING_PROJECT_DATA_ENCRYPTION = "project-data-encryption";
    public static final String SETTING_LAST_RESTORE_DIRECTORY = "last-restore-dir";

    private static volatile Consumer<Exception> readErrorHandler = error -> { };

    private ApplicationSettings() {
    }

    private static final class Holder {
        private static final SettingsStore STORE = new SettingsStore(
                new File(SketchwarePaths.getSettingsPath()), GsonUtils.getGson(),
                ApplicationSettings::getDefaults, ApplicationSettings::reportReadError);
    }

    /** Registers presentation of read errors without eagerly loading settings. */
    public static void setReadErrorHandler(Consumer<Exception> handler) {
        readErrorHandler = handler;
    }

    private static void reportReadError(Exception error) {
        LogUtil.e("ApplicationSettings", "Failed to parse App Settings.", error);
        readErrorHandler.accept(error);
    }

    private static HashMap<String, Object> getDefaults() {
        HashMap<String, Object> defaults = new HashMap<>();
        for (String key : Arrays.asList(SETTING_ALWAYS_SHOW_BLOCKS, SETTING_BACKUP_DIRECTORY,
                SETTING_ROOT_AUTO_INSTALL_PROJECTS, SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING,
                SETTING_SHOW_BUILT_IN_BLOCKS, SETTING_SHOW_EVERY_SINGLE_BLOCK,
                SETTING_USE_NEW_VERSION_CONTROL, SETTING_USE_ASD_HIGHLIGHTER,
                SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,
                SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH, SETTING_PROJECT_DATA_ENCRYPTION)) {
            defaults.put(key, getDefaultValue(key));
        }
        return defaults;
    }

    public static Object getDefaultValue(String key) {
        return switch (key) {
            case SETTING_ALWAYS_SHOW_BLOCKS, SETTING_ROOT_AUTO_INSTALL_PROJECTS,
                 SETTING_SHOW_BUILT_IN_BLOCKS, SETTING_SHOW_EVERY_SINGLE_BLOCK,
                 SETTING_USE_NEW_VERSION_CONTROL, SETTING_USE_ASD_HIGHLIGHTER,
                 SETTING_PROJECT_DATA_ENCRYPTION -> false;
            case SETTING_BACKUP_DIRECTORY ->
                    SketchwarePaths.toExternalStorageRelativePath(SketchwarePaths.getBackupsPath());
            case SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING -> true;
            case SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH ->
                    SketchwarePaths.toExternalStorageRelativePath(SketchwarePaths.getBlockManagerPaletteFilePath());
            case SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH ->
                    SketchwarePaths.toExternalStorageRelativePath(SketchwarePaths.getBlockManagerBlockFilePath());
            default -> throw new IllegalArgumentException("Unknown key '" + key + "'!");
        };
    }

    public static String getBackupPath() {
        return getString(SETTING_BACKUP_DIRECTORY, (String) getDefaultValue(SETTING_BACKUP_DIRECTORY));
    }

    public static String getBackupFileName() {
        return getString(SETTING_BACKUP_FILENAME,
                "$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd'T'HHmmss)");
    }

    public static String getLastRestoreDirectory() {
        return getString(SETTING_LAST_RESTORE_DIRECTORY, "");
    }

    public static void setLastRestoreDirectory(String path) {
        putString(SETTING_LAST_RESTORE_DIRECTORY, path);
    }

    public static String getStringSettingValueOrSetAndGet(String key, String defaultValue) {
        return Holder.STORE.getOrPutString(key, defaultValue);
    }

    public static boolean isSettingEnabled(String key) {
        return getBoolean(key, false);
    }

    public static void setSetting(String key, Object value) {
        if (value instanceof String s) {
            putString(key, s);
        } else if (value instanceof Boolean b) {
            putBoolean(key, b);
        } else {
            throw new IllegalArgumentException("Unhandled data type " + value.getClass());
        }
    }

    public static String getString(String key, String defaultValue) {
        return Holder.STORE.getString(key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Holder.STORE.getBoolean(key, defaultValue);
    }

    public static void putString(String key, String value) {
        Holder.STORE.putString(key, value);
    }

    public static void putBoolean(String key, boolean value) {
        Holder.STORE.putBoolean(key, value);
    }
}
