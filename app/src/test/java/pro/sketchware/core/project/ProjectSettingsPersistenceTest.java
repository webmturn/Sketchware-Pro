package pro.sketchware.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
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

    @Test public void emptyFileCanBeReadAndUpdated() throws Exception {
        assertEmptyDocumentCanBeUpdated("");
    }

    @Test public void whitespaceFileCanBeReadAndUpdated() throws Exception {
        assertEmptyDocumentCanBeUpdated(" \r\n\t ");
    }

    @Test public void jsonNullCanBeReadAndUpdated() throws Exception {
        assertEmptyDocumentCanBeUpdated("null");
    }

    private void assertEmptyDocumentCanBeUpdated(String original) throws Exception {
        File file = folder.newFile("settings");
        write(file, original);
        ProjectSettings settings = new ProjectSettings("test", file);
        assertEquals(21, settings.getMinSdkVersion());
        assertEquals(".SketchApplication", settings.getValue("app_class", ".SketchApplication"));
        assertEquals("reading defaults must preserve the original file", original, readText(file));

        settings.setValue("min_sdk", "26");
        settings.setValues(Map.of("app_class", ".CustomApplication", "hidden", "keep"));
        ProjectSettings reloaded = new ProjectSettings("test", file);
        assertEquals(26, reloaded.getMinSdkVersion());
        assertEquals(".CustomApplication", reloaded.getValue("app_class", "default"));
        assertEquals("keep", reloaded.getValue("hidden", "default"));
    }

    @Test public void nullOptionUsesDefaultWithoutRewritingOtherSettings() throws Exception {
        File file = folder.newFile("settings");
        String original = "{\"app_class\":null,\"min_sdk\":\"28\",\"hidden\":\"keep\"}";
        write(file, original);
        ProjectSettings settings = new ProjectSettings("test", file);
        assertEquals(".SketchApplication", settings.getValue("app_class", ".SketchApplication"));
        assertEquals(28, settings.getMinSdkVersion());
        assertEquals("keep", settings.getValue("hidden", "default"));
        assertEquals(original, readText(file));
        settings.setValue("enable_viewbinding", "true");
        ProjectSettings reloaded = new ProjectSettings("test", file);
        assertEquals("keep", reloaded.getValue("hidden", "default"));
        assertEquals("true", reloaded.getValue("enable_viewbinding", "false"));
        assertEquals(".SketchApplication", reloaded.getValue("app_class", ".SketchApplication"));
    }

    @Test public void nullUpdatesUseDefaultsBeforeAndAfterReload() throws Exception {
        File file = new File(folder.getRoot(), "settings");
        ProjectSettings settings = new ProjectSettings("test", file);
        settings.setValue("app_class", null);
        assertEquals("default", settings.getValue("app_class", "default"));
        HashMap<String, String> changes = new HashMap<>();
        changes.put("enable_viewbinding", null);
        changes.put("hidden", "keep");
        settings.setValues(changes);
        assertEquals("false", settings.getValue("enable_viewbinding", "false"));
        ProjectSettings reloaded = new ProjectSettings("test", file);
        assertEquals("default", reloaded.getValue("app_class", "default"));
        assertEquals("false", reloaded.getValue("enable_viewbinding", "false"));
        assertEquals("keep", reloaded.getValue("hidden", "default"));
    }

    @Test public void missingSettingsUseDefaultsWithoutCreatingAFile() {
        File file = new File(folder.getRoot(), "settings");
        ProjectSettings settings = new ProjectSettings("test", file);
        assertEquals(21, settings.getMinSdkVersion());
        assertEquals("default", settings.getValue("missing", "default"));
        assertFalse(file.exists());
    }
}
