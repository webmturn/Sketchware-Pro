package pro.sketchware.util.locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.LocaleManagerCompat;
import androidx.core.os.LocaleListCompat;
import com.google.gson.JsonParseException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import pro.sketchware.R;
import pro.sketchware.core.block.BlockSpecParser;
import pro.sketchware.util.GsonUtils;
import pro.sketchware.util.LogUtil;

public final class LanguageOverrideManager {
    private static final String PREFS_NAME = "language_override";
    private static final String PACK_PREFIX = "pack_";
    private static final String SELECTED_PACK = "selected_pack";
    private static final Pattern INTERNAL_NAMES =
            Pattern.compile(
                    "appbar_scrolling_view_behavior|bottom_sheet_behavior|"
                            + "besome_blog_url|docs_url|firebase_database_url|facebook_url|ideas_url|"
                            + "medium_url|slack_url_.*|ga_trackingId|google_.*|default_web_client_id|"
                            + "project_id|gcm_.*|storage_.*|s[1-7]");
    private static final Pattern SPEC_PART =
            Pattern.compile("(?:block|root_spec)_.+_(?:head|tail|body_\\d+)");

    private static final LanguageOverrideManager INSTANCE = new LanguageOverrideManager();

    private final Map<String, Pack> packs = new LinkedHashMap<>();
    private volatile Pack selected;
    private volatile long revision;

    public record Pack(
            String id,
            String name,
            String fallbackTag,
            Map<String, String> strings,
            Map<String, String> rejectedStrings) {
        public Pack {
            strings = Map.copyOf(strings == null ? Map.of() : strings);
            rejectedStrings = Map.copyOf(rejectedStrings == null ? Map.of() : rejectedStrings);
        }

        public Locale fallback() {
            return Locale.forLanguageTag(fallbackTag);
        }
    }

    private static final class ResourceNames {
        private static final Map<Integer, String> BY_ID = read();
        private static final Map<String, Integer> BY_NAME = byName();

        private static Map<String, Integer> byName() {
            var ids = new HashMap<String, Integer>();
            BY_ID.forEach((id, name) -> ids.put(name, id));
            return Map.copyOf(ids);
        }

        private static Map<Integer, String> read() {
            var names = new LinkedHashMap<Integer, String>();
            for (Field field : R.string.class.getFields()) {
                if (field.getType() != int.class || !Modifier.isStatic(field.getModifiers()))
                    continue;
                try {
                    names.put(field.getInt(null), field.getName());
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read string resource names", e);
                }
            }
            return Map.copyOf(names);
        }
    }

    private LanguageOverrideManager() {}

    public static LanguageOverrideManager getInstance() {
        return INSTANCE;
    }

    public synchronized void init(Context context) {
        var prefs = preferences(context);
        packs.clear();
        var resourcesByLocale = new HashMap<String, Resources>();
        var oldStrings = new TreeMap<String, String>();
        for (var entry : prefs.getAll().entrySet()) {
            if (!(entry.getValue() instanceof String value)) continue;
            String key = entry.getKey();
            if (key.startsWith(PACK_PREFIX)) {
                try {
                    Pack pack = GsonUtils.getGson().fromJson(value, Pack.class);
                    if (pack != null
                            && key.equals(PACK_PREFIX + pack.id())
                            && pack.name() != null
                            && pack.fallbackTag() != null) {
                        Resources source =
                                resourcesByLocale.computeIfAbsent(
                                        pack.fallbackTag(),
                                        tag -> sourceResources(context, pack.fallback()));
                        packs.put(pack.id(), validateStoredPack(pack, source));
                    }
                } catch (JsonParseException | IllegalArgumentException e) {
                    LogUtil.w("LanguageOverride", "Unable to read " + key, e);
                }
            } else if (key.startsWith("str_")) {
                oldStrings.put(key.substring(4), value);
            }
        }
        if (!oldStrings.isEmpty()) {
            Locale locale = AppCompatDelegate.getApplicationLocales().get(0);
            if (locale == null) locale = LocaleManagerCompat.getApplicationLocales(context).get(0);
            if (locale == null) locale = LocaleManagerCompat.getSystemLocales(context).get(0);
            List<Locale> supported = AppLanguages.read(context);
            int index = locale == null ? -1 : AppLanguages.findSelection(supported, locale);
            Locale fallback = index < 0 ? supported.get(0) : supported.get(index);
            Configuration config = new Configuration();
            config.setLocale(fallback);
            String name =
                    context.createConfigurationContext(config)
                            .getString(R.string.language_settings_previous_import);
            var pack =
                    validateStoredPack(
                            new Pack(
                                    "previous-import",
                                    name,
                                    fallback.toLanguageTag(),
                                    oldStrings,
                                    Map.of()),
                            sourceResources(context, fallback));
            var changes = prefs.edit().remove("override_count");
            oldStrings.keySet().forEach(key -> changes.remove("str_" + key));
            changes.putString(PACK_PREFIX + pack.id(), GsonUtils.getGson().toJson(pack));
            if (!pack.strings().isEmpty()
                    && !packs.containsKey(prefs.getString(SELECTED_PACK, ""))) {
                changes.putString(SELECTED_PACK, pack.id());
            }
            changes.apply();
            packs.put(pack.id(), pack);
        }
        selected = packs.get(prefs.getString(SELECTED_PACK, ""));
        revision++;
    }

    public synchronized List<Pack> getPacks() {
        return packs.values().stream()
                .sorted(
                        Comparator.comparing(Pack::name, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(Pack::id))
                .toList();
    }

    public synchronized Pack getPack(String id) {
        return packs.get(id);
    }

    public Pack getActivePack(Resources resources) {
        Pack pack = selected;
        if (pack == null) return null;
        Locale current = resources.getConfiguration().getLocales().get(0);
        return current != null
                        && LocaleListCompat.matchesLanguageAndScript(pack.fallback(), current)
                ? pack
                : null;
    }

    public long getRevision() {
        return revision;
    }

    public String getString(Context context, @StringRes int id) {
        Resources resources = context.getResources();
        if (resources instanceof LanguageOverrideResources) return resources.getString(id);
        String override = getOverride(resources, id);
        return override != null ? override : context.getString(id);
    }

    public String getOverride(Resources resources, @StringRes int id) {
        Pack pack = getActivePack(resources);
        if (pack == null) return null;
        String name = ResourceNames.BY_ID.get(id);
        return name != null ? pack.strings().get(name) : null;
    }

    public synchronized Pack addPack(
            Context context, String name, Locale fallback, Map<String, String> strings) {
        Map<String, String> compatible =
                compatibleStrings(sourceResources(context, fallback), strings);
        if (compatible.isEmpty()) return null;
        var pack =
                new Pack(
                        UUID.randomUUID().toString(),
                        name,
                        fallback.toLanguageTag(),
                        compatible,
                        Map.of());
        preferences(context)
                .edit()
                .putString(PACK_PREFIX + pack.id(), GsonUtils.getGson().toJson(pack))
                .apply();
        packs.put(pack.id(), pack);
        return pack;
    }

    public synchronized void select(Context context, Pack pack) {
        preferences(context)
                .edit()
                .putString(SELECTED_PACK, pack == null ? null : pack.id())
                .apply();
        selected = pack;
        revision++;
    }

    public synchronized void remove(Context context, Pack pack) {
        var editor = preferences(context).edit().remove(PACK_PREFIX + pack.id());
        packs.remove(pack.id());
        if (selected != null && selected.id().equals(pack.id())) {
            editor.remove(SELECTED_PACK);
            selected = null;
            revision++;
        }
        editor.apply();
    }

    public static Map<String, String> sourceStrings(Context context, Locale fallback) {
        Resources resources = sourceResources(context, fallback);
        var source = new TreeMap<String, String>();
        ResourceNames.BY_ID.forEach(
                (id, name) -> {
                    String value = sourceString(resources, id, name);
                    if (value != null) source.put(name, value);
                });
        return source;
    }

    private static Resources sourceResources(Context context, Locale fallback) {
        Configuration config = new Configuration();
        config.setLocale(fallback);
        Resources resources = context.createConfigurationContext(config).getResources();
        if (resources instanceof LanguageOverrideResources overrides)
            return overrides.baseResources();
        return resources;
    }

    private static String sourceString(Resources resources, int id, String name) {
        if (INTERNAL_NAMES.matcher(name).matches()) return null;
        try {
            String value = resources.getString(id);
            if (value.startsWith("http://")
                    || value.startsWith("https://")
                    || value.startsWith("com.google.")
                    || value.startsWith("com.besome.")) return null;
            return !value.isEmpty() || isSpecPart(name) ? value : null;
        } catch (Resources.NotFoundException e) {
            // Some library resources are available only in particular configurations.
            return null;
        }
    }

    private static boolean isSpecPart(String name) {
        return SPEC_PART.matcher(name).matches();
    }

    private static boolean isValidTranslation(String name, String source, String translation) {
        if (translation.isBlank() && !isSpecPart(name)) return false;
        if (name.startsWith("builtin_block_") || isSpecPart(name)) {
            return BlockSpecParser.parseParameters(source)
                            .equals(BlockSpecParser.parseParameters(translation))
                    && BlockSpecParser.findInvalidMenuParameterEnd(translation) < 0;
        }
        return TranslationPlaceholders.matches(source, translation);
    }

    private static Pack validateStoredPack(Pack pack, Resources source) {
        Map<String, String> compatible = compatibleStrings(source, pack.strings());
        if (compatible.size() == pack.strings().size()) return pack;
        // Keep rejected legacy entries available for export and correction.
        var rejected = new TreeMap<>(pack.rejectedStrings());
        rejected.putAll(pack.strings());
        compatible.keySet().forEach(rejected::remove);
        return new Pack(pack.id(), pack.name(), pack.fallbackTag(), compatible, rejected);
    }

    private static Map<String, String> compatibleStrings(
            Resources resources, Map<String, String> translations) {
        var compatible = new TreeMap<String, String>();
        for (var entry : translations.entrySet()) {
            String key = entry.getKey();
            Integer id = ResourceNames.BY_NAME.get(key);
            if (id == null) continue;
            String source = sourceString(resources, id, key);
            String translation = entry.getValue();
            if (source != null && isValidTranslation(key, source, translation)) {
                compatible.put(key, translation);
            }
        }
        return compatible;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
