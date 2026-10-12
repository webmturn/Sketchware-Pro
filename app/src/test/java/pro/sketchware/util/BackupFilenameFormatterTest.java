package pro.sketchware.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

public class BackupFilenameFormatterTest {
    private TimeZone originalTimeZone;

    @Before public void useUtc() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After public void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test public void expandsDefaultTemplateWithOneBackupTimestamp() {
        assertEquals("Demo v1.0 (com.example.app, 1) 1970-01-01T000001",
                format("$projectName v$versionName ($pkgName, $versionCode) $time(yyyy-MM-dd'T'HHmmss)", 1000));
    }

    @Test public void expandsRepeatedVariablesAndTimeFormats() {
        assertEquals("Demo_Demo_1_1000_1970_000001",
                format("$projectName_$projectName_$versionCode_$timeInMs_$time(yyyy)_$time(HHmmss)", 1000));
    }

    @Test public void keepsLiteralDollarAndBackslashInProjectNames() {
        assertEquals("Cash$Demo\\Name_1970", BackupFilenameFormatter.format("$projectName_$time(yyyy)",
                "Cash$Demo\\Name", "1.0", "1", "com.example.app", 0));
    }

    @Test public void formattedDatesCanContainLiteralDollarSigns() {
        assertEquals("Demo_$1970", format("$projectName_$time('$'yyyy)", 0));
    }

    @Test public void leavesUnknownVariablesAsLiteralText() {
        assertEquals("Demo_$custom", format("$projectName_$custom", 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedDatePatterns() {
        format("$projectName_$time(invalid)", 0);
    }

    private static String format(String template, long time) {
        return BackupFilenameFormatter.format(template, "Demo", "1.0", "1", "com.example.app", time);
    }
}
