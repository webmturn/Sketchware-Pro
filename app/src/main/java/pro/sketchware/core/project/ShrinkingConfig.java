package pro.sketchware.core.project;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

/** Read-only shrinking flag shared by local builds and Gradle export. */
public final class ShrinkingConfig {
    private static final Gson GSON = new Gson();
    private static final Type CONFIG_TYPE = new TypeToken<HashMap<String, String>>() {}.getType();

    private ShrinkingConfig() {}

    public static boolean isEnabled(File config) {
        if (!config.isFile()) return false;
        try (Reader reader = Files.newBufferedReader(config.toPath(), StandardCharsets.UTF_8)) {
            HashMap<String, String> values = GSON.fromJson(reader, CONFIG_TYPE);
            return values != null && "true".equals(values.get("enabled"));
        } catch (IOException | JsonParseException e) {
            return false;
        }
    }
}
