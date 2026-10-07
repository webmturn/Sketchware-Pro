package pro.sketchware.core.project;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class ProjectSettingsPersistenceTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private static void write(File file, String text) throws IOException {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String readText(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void batchUpdatesPreserveSettingsOutsideTheForm() throws Exception {
        File file = folder.newFile("settings");
        write(file, "{\"min_sdk\":\"26\",\"hidden\":\"keep\"}");
        ProjectSettings settings = new ProjectSettings("test", file);
        settings.setValues(Map.of("min_sdk", "28", "enable_viewbinding", "true"));

        ProjectSettings reloaded = new ProjectSettings("test", file);
        assertEquals(28, reloaded.getMinSdkVersion());
        assertEquals("true", reloaded.getValue("enable_viewbinding", "false"));
        assertEquals("keep", reloaded.getValue("hidden", ""));
    }

    @Test public void emptyValuesRemainSerializedWhileReadersUseTheirDefault() throws Exception {
        File file = new File(folder.getRoot(), "settings");
        ProjectSettings settings = new ProjectSettings("test", file);
        settings.setValues(Map.of("app_class", "", "min_sdk", "invalid"));

        JsonObject stored = JsonParser.parseString(readText(file)).getAsJsonObject();
        assertEquals("", stored.get("app_class").getAsString());
        assertEquals(".SketchApplication", settings.getValue("app_class", ".SketchApplication"));
    }
}
