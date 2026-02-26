package pro.sketchware.fragments.settings.language;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import pro.sketchware.R;

/**
 * Manages language string overrides loaded from imported translation files.
 * Caches overrides in memory for fast lookup at runtime.
 */
public class LanguageOverrideManager {

    private static final String PREFS_NAME = "language_override";
    private static final String KEY_OVERRIDE_COUNT = "override_count";
    private static final String KEY_PREFIX = "str_";

    private static volatile LanguageOverrideManager instance;
    private volatile Map<Integer, String> resIdOverrides = Collections.emptyMap();
    private volatile boolean loaded = false;
    private volatile boolean active = false;

    private LanguageOverrideManager() {
    }

    public static LanguageOverrideManager getInstance() {
        if (instance == null) {
            synchronized (LanguageOverrideManager.class) {
                if (instance == null) {
                    instance = new LanguageOverrideManager();
                }
            }
        }
        return instance;
    }

    /**
     * Loads overrides from SharedPreferences into memory.
     * Call this once at app startup (e.g. in Application.onCreate).
     */
    public void init(@NonNull Context context) {
        loadOverrides(context);
    }

    /**
     * Reloads overrides from SharedPreferences (call after import/clear).
     */
    public void reload(@NonNull Context context) {
        loadOverrides(context);
    }

    private void loadOverrides(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_OVERRIDE_COUNT, 0);

        if (count <= 0) {
            resIdOverrides = Collections.emptyMap();
            active = false;
            loaded = true;
            return;
        }

        Map<String, Integer> nameToId = buildNameToIdMap();
        Map<String, ?> all = prefs.getAll();
        Map<Integer, String> newMap = new HashMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(KEY_PREFIX) && entry.getValue() instanceof String) {
                String name = key.substring(KEY_PREFIX.length());
                Integer resId = nameToId.get(name);
                if (resId != null) {
                    newMap.put(resId, (String) entry.getValue());
                }
            }
        }
        resIdOverrides = Collections.unmodifiableMap(newMap);
        active = true;
        loaded = true;
    }

    private Map<String, Integer> buildNameToIdMap() {
        Map<String, Integer> map = new HashMap<>();
        try {
            for (Field field : R.string.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && field.getType() == int.class) {
                    try {
                        map.put(field.getName(), field.getInt(null));
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
            android.util.Log.w("LanguageOverrideManager", "Failed to build name-to-id map", ignored);
        }
        return map;
    }

    /**
     * Returns the overridden string for the given resource ID, or null if not overridden.
     */
    @Nullable
    public String getOverride(@StringRes int resId) {
        if (!active || !loaded) return null;
        return resIdOverrides.get(resId);
    }

    /**
     * Returns true if language override is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the number of overridden strings.
     */
    public int getOverrideCount() {
        return resIdOverrides.size();
    }

    /**
     * Saves imported strings to SharedPreferences and reloads into memory.
     */
    public void applyOverrides(@NonNull Context context, @NonNull Map<String, String> strings) {
        SharedPreferences.Editor editor = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.putInt(KEY_OVERRIDE_COUNT, strings.size());
        for (Map.Entry<String, String> entry : strings.entrySet()) {
            editor.putString(KEY_PREFIX + entry.getKey(), entry.getValue());
        }
        editor.apply();
        reload(context);
    }

    /**
     * Clears all overrides.
     */
    public void clearOverrides(@NonNull Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        resIdOverrides = Collections.emptyMap();
        active = false;
    }

    /**
     * Returns the override count from SharedPreferences (without loading all strings).
     */
    public static int getStoredOverrideCount(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_OVERRIDE_COUNT, 0);
    }
}
