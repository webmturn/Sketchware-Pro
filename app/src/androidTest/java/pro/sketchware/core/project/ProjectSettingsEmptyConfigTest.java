package pro.sketchware.core.project;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** Real Android file I/O; every settings file belongs to this test's cache fixture. */
@RunWith(AndroidJUnit4.class)
public class ProjectSettingsEmptyConfigTest {
    @Test public void emptyFileCanReadDefaultsAndPersistUpdates() throws Exception {
        exerciseEmptyDocument("");
    }

    @Test public void whitespaceFileCanReadDefaultsAndPersistUpdates() throws Exception {
        exerciseEmptyDocument(" \r\n\t ");
    }

    @Test public void jsonNullCanReadDefaultsAndPersistUpdates() throws Exception {
        exerciseEmptyDocument("null");
    }

    private static void exerciseEmptyDocument(String original) throws Exception {
        File file = fixture();
        try {
            Files.write(file.toPath(), original.getBytes(StandardCharsets.UTF_8));
            ProjectSettings settings = new ProjectSettings("empty-config-test", file);
            assertEquals(21, settings.getMinSdkVersion());
            assertEquals("false", settings.getValue("enable_viewbinding", "false"));
            assertEquals(original, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            settings.setValue("min_sdk", "26");
            settings.setValues(Map.of("app_class", ".TestApplication", "custom_option", "保留"));
            ProjectSettings reloaded = new ProjectSettings("empty-config-test", file);
            assertEquals(26, reloaded.getMinSdkVersion());
            assertEquals(".TestApplication", reloaded.getValue("app_class", "default"));
            assertEquals("保留", reloaded.getValue("custom_option", "default"));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test public void nullOptionPreservesOtherSettingsThroughSaveAndReload() throws Exception {
        File file = fixture();
        String original = "{\"app_class\":null,\"min_sdk\":\"28\",\"custom_option\":\"保留\"}";
        try {
            Files.write(file.toPath(), original.getBytes(StandardCharsets.UTF_8));
            ProjectSettings settings = new ProjectSettings("empty-config-test", file);
            assertEquals(".SketchApplication", settings.getValue("app_class", ".SketchApplication"));
            assertEquals(28, settings.getMinSdkVersion());
            assertEquals(original, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            settings.setValues(Map.of("enable_viewbinding", "true"));
            settings.setValue("app_class", null);
            assertEquals(".SketchApplication", settings.getValue("app_class", ".SketchApplication"));
            ProjectSettings reloaded = new ProjectSettings("empty-config-test", file);
            assertEquals(28, reloaded.getMinSdkVersion());
            assertEquals("true", reloaded.getValue("enable_viewbinding", "false"));
            assertEquals("保留", reloaded.getValue("custom_option", "default"));
            assertEquals(".SketchApplication", reloaded.getValue("app_class", ".SketchApplication"));
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test public void missingFileIsNotCreatedByReadingDefaults() throws Exception {
        File file = fixture();
        try {
            Files.delete(file.toPath());
            ProjectSettings settings = new ProjectSettings("empty-config-test", file);
            assertEquals(21, settings.getMinSdkVersion());
            assertEquals("fallback", settings.getValue("app_class", "fallback"));
            assertFalse(file.exists());
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static File fixture() throws Exception {
        File cache = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        return File.createTempFile("project-settings-regression-", ".json", cache);
    }
}
