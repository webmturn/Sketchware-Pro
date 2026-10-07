package pro.sketchware.core.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsStoreTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private static void write(File file, String text) throws IOException {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String readText(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<Exception> errors = new ArrayList<>();

    private SettingsStore open(File file) {
        return new SettingsStore(file, gson,
                () -> Map.of("project-data-encryption", false, "backup-dir", "backups"), errors::add);
    }

    private Map<String, Object> read(File file) throws Exception {
        return gson.fromJson(readText(file),
                new TypeToken<Map<String, Object>>() { }.getType());
    }

    @Test public void readsExistingConfigurationWithoutReplacingCustomValues() throws Exception {
        File file = folder.newFile("settings");
        String legacy = "{\"project-data-encryption\":true,\"backup-dir\":\"custom\",\"unknown-key\":42}";
        write(file, legacy);

        SettingsStore store = open(file);

        assertTrue(store.getBoolean("project-data-encryption", false));
        assertEquals("custom", store.getString("backup-dir", "default"));
        assertEquals(legacy, readText(file));
        store.putBoolean("project-data-encryption", false);
        assertEquals(42.0, read(file).get("unknown-key"));
        assertTrue(errors.isEmpty());
    }

    @Test public void createsDefaultsOnlyWhenConfigurationIsMissing() throws Exception {
        File file = new File(folder.getRoot(), "settings");
        SettingsStore store = open(file);

        assertFalse(store.getBoolean("project-data-encryption", true));
        assertEquals("backups", read(file).get("backup-dir"));
        assertTrue(errors.isEmpty());
    }

    @Test public void updatesAreVisibleImmediatelyAndAfterReopening() throws Exception {
        File file = new File(folder.getRoot(), "settings");
        SettingsStore store = open(file);
        store.putBoolean("project-data-encryption", true);
        assertTrue(store.getBoolean("project-data-encryption", false));
        assertTrue(open(file).getBoolean("project-data-encryption", false));

        store.putBoolean("project-data-encryption", false);
        assertFalse(store.getBoolean("project-data-encryption", true));
        assertFalse(open(file).getBoolean("project-data-encryption", true));
    }

    @Test public void clearingAStringRestoresTheCallersDefault() throws Exception {
        File file = new File(folder.getRoot(), "settings");
        SettingsStore store = open(file);
        store.putString("backup-filename", "custom");
        store.putString("backup-filename", null);

        assertEquals("default", store.getString("backup-filename", "default"));
        assertFalse(read(file).containsKey("backup-filename"));
    }

    @Test public void preservesTypedFallbacksAndPersistsInsertedDefaults() throws Exception {
        File file = folder.newFile("settings");
        write(file, "{\"backup-dir\":true,\"project-data-encryption\":\"true\"}");
        SettingsStore store = open(file);

        assertFalse(store.getBoolean("project-data-encryption", false));
        assertEquals("fallback", store.getOrPutString("backup-dir", "fallback"));
        assertEquals("fallback", read(file).get("backup-dir"));
    }

    @Test public void reportsCorruptOrNullJsonAndPersistsFallbacks() throws Exception {
        for (String invalid : List.of("{invalid", "null")) {
            File file = folder.newFile();
            write(file, invalid);
            int previousErrors = errors.size();
            SettingsStore store = open(file);

            assertEquals(previousErrors + 1, errors.size());
            assertFalse(store.getBoolean("project-data-encryption", true));
            assertEquals("backups", read(file).get("backup-dir"));
        }
    }
}
