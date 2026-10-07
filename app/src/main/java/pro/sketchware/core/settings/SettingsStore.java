package pro.sketchware.core.settings;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import pro.sketchware.util.FileUtil;

/** Cached JSON storage shared by configuration callers and the preference adapter. */
final class SettingsStore {
    private final File file;
    private final Gson gson;
    private final Map<String, Object> settings;

    SettingsStore(File file, Gson gson, Supplier<Map<String, Object>> defaults,
                  Consumer<Exception> readErrorHandler) {
        this.file = file;
        this.gson = gson;
        Map<String, Object> loaded = null;
        if (file.exists()) {
            try {
                loaded = gson.fromJson(FileUtil.readFile(file.getAbsolutePath()),
                        new TypeToken<HashMap<String, Object>>() { }.getType());
                if (loaded == null) {
                    readErrorHandler.accept(new NullPointerException("settings == null"));
                }
            } catch (JsonParseException e) {
                readErrorHandler.accept(e);
            }
        }
        settings = loaded == null ? new HashMap<>(defaults.get()) : loaded;
        if (loaded == null) {
            persist();
        }
    }

    synchronized String getString(String key, String defaultValue) {
        return settings.get(key) instanceof String value ? value : defaultValue;
    }

    synchronized boolean getBoolean(String key, boolean defaultValue) {
        return settings.get(key) instanceof Boolean value ? value : defaultValue;
    }

    synchronized String getOrPutString(String key, String defaultValue) {
        if (settings.get(key) instanceof String value) {
            return value;
        }
        putString(key, defaultValue);
        return defaultValue;
    }

    synchronized void putString(String key, String value) {
        if (value == null) {
            settings.remove(key);
        } else {
            settings.put(key, value);
        }
        persist();
    }

    synchronized void putBoolean(String key, boolean value) {
        settings.put(key, value);
        persist();
    }

    private void persist() {
        FileUtil.writeFile(file.getAbsolutePath(), gson.toJson(settings));
    }
}
