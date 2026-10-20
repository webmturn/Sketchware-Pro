package pro.sketchware.core.project;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public class ShrinkingConfigTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void enabledStringAndBooleanValuesAreAccepted() throws Exception {
        for (String content : new String[]{"{\"enabled\":\"true\"}", "{\"enabled\":true}"}) {
            assertRead(content, true);
        }
    }

    @Test public void otherFlagsCannotEnableShrinking() throws Exception {
        for (String content : new String[]{"{\"enabled\":\"false\",\"r8\":\"true\"}",
                "{\"debug\":true,\"r8\":true}", "{}", "{\"enabled\":false}",
                "{\"enabled\":null}", "{\"enabled\":1}", "{\"enabled\":\"TRUE\"}"}) {
            assertRead(content, false);
        }
    }

    @Test public void blankAndNullSettingsAreDisabledWithoutRewriting() throws Exception {
        for (String content : new String[]{"", " \r\n\t ", "null", "\uFEFFnull"}) assertRead(content, false);
    }

    @Test public void malformedSettingsAreDisabledWithoutRewriting() throws Exception {
        for (String content : new String[]{"[{broken", "[]", "true", "\"true\"",
                "{\"enabled\":{}}", "{\"enabled\":[]}"}) assertRead(content, false);
    }

    @Test public void missingFilesAndDirectoriesAreDisabled() {
        File missing = new File(folder.getRoot(), "missing");
        assertFalse(ShrinkingConfig.isEnabled(missing));
        assertFalse(missing.exists());
        assertFalse(ShrinkingConfig.isEnabled(folder.getRoot()));
        assertTrue(folder.getRoot().isDirectory());
    }

    @Test public void eachReadUsesTheLatestSetting() throws Exception {
        File config = folder.newFile();
        Files.write(config.toPath(), "{\"enabled\":true}".getBytes(StandardCharsets.UTF_8));
        assertTrue(ShrinkingConfig.isEnabled(config));
        Files.write(config.toPath(), "{\"enabled\":false}".getBytes(StandardCharsets.UTF_8));
        assertFalse(ShrinkingConfig.isEnabled(config));
        assertTrue(config.delete());
        assertFalse(ShrinkingConfig.isEnabled(config));
    }

    @Test public void malformedUtf8IsDisabledAndPreserved() throws Exception {
        File config = folder.newFile();
        byte[] invalid = {(byte) 0xc3};
        Files.write(config.toPath(), invalid);
        assertFalse(ShrinkingConfig.isEnabled(config));
        assertArrayEquals(invalid, Files.readAllBytes(config.toPath()));
    }

    private void assertRead(String content, boolean expected) throws Exception {
        File config = folder.newFile();
        byte[] original = content.getBytes(StandardCharsets.UTF_8);
        Files.write(config.toPath(), original);
        Files.setLastModifiedTime(config.toPath(), FileTime.fromMillis(1600000000000L));
        long modified = config.lastModified();
        assertEquals(content, expected, ShrinkingConfig.isEnabled(config));
        assertArrayEquals(original, Files.readAllBytes(config.toPath()));
        assertEquals(modified, config.lastModified());
    }
}
