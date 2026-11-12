package pro.sketchware.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BackupFilenameFormatterTest {
    private TimeZone originalTimeZone;

    @Before
    public void useUtc() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void expandsDefaultTemplateWithOneBackupTimestamp() {
        assertEquals(
                "Demo v1.0 (com.example.app, 1) 1970-01-01T000001",
                format(
                        "$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd'T'HHmmss)",
                        1000));
    }

    @Test
    public void expandsRepeatedVariablesAndTimeFormats() {
        assertEquals(
                "Demo_Demo_1_1000_1970_000001",
                format(
                        "$projectName_$projectName_$versionCode_$timeInMs_$time(yyyy)_$time(HHmmss)",
                        1000));
    }

    @Test
    public void keepsLiteralDollarAndBackslashInProjectNames() {
        assertEquals(
                "Cash$Demo\\Name_1970",
                BackupFilenameFormatter.format(
                        "$projectName_$time(yyyy)",
                        "Cash$Demo\\Name",
                        "1.0",
                        "1",
                        "com.example.app",
                        0));
    }

    @Test
    public void formattedDatesCanContainLiteralDollarSigns() {
        assertEquals("Demo_$1970", format("$projectName_$time('$'yyyy)", 0));
    }

    @Test
    public void leavesUnknownVariablesAsLiteralText() {
        assertEquals("Demo_$custom", format("$projectName_$custom", 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedDatePatterns() {
        format("$projectName_$time(invalid)", 0);
    }

    @Test
    public void validatesTheExpandedFilenameAsASinglePathComponent() {
        assertTrue(BackupFilenameFormatter.isValidFilenameStem("my_demo v1.0"));
        assertFalse(
                BackupFilenameFormatter.isValidFilenameStem(
                        format("../../outside/$projectName", 0)));
        assertFalse(BackupFilenameFormatter.isValidFilenameStem(format("sub\\$projectName", 0)));
        assertFalse(BackupFilenameFormatter.isValidFilenameStem(format("$time(yyyy/MM/dd)", 0)));
        assertFalse(BackupFilenameFormatter.isValidFilenameStem(".."));
        assertFalse(BackupFilenameFormatter.isValidFilenameStem(" "));
    }

    @Test
    public void checksTheByteLimitIncludingTheArchiveExtension() {
        assertFalse(BackupFilenameFormatter.isArchiveFilenameTooLong("a".repeat(251)));
        assertTrue(BackupFilenameFormatter.isArchiveFilenameTooLong("a".repeat(252)));
        assertFalse(BackupFilenameFormatter.isArchiveFilenameTooLong("界".repeat(83)));
        assertTrue(BackupFilenameFormatter.isArchiveFilenameTooLong("界".repeat(84)));
    }

    @Test
    public void makesRoomForADuplicateSuffixWithoutSplittingUnicode() {
        String prefix = "a".repeat(247);
        String stem = prefix + "🌙";
        assertEquals(stem + ".swb", BackupFilenameFormatter.archiveFilename(stem, 1));
        assertEquals(prefix + "_2.swb", BackupFilenameFormatter.archiveFilename(stem, 2));
        assertEquals(prefix + "_100.swb", BackupFilenameFormatter.archiveFilename(stem, 100));
    }

    private static String format(String template, long time) {
        return BackupFilenameFormatter.format(
                template, "Demo", "1.0", "1", "com.example.app", time);
    }
}
