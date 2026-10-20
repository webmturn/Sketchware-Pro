package pro.sketchware.core.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProjectVersionCodeTest {
    @Test public void acceptsBothInclusiveBoundsAndNormalizesWhitespace() {
        assertEquals(1, ProjectVersionCode.parse("1"));
        assertEquals(2_100_000_000, ProjectVersionCode.parse("2100000000"));
        assertEquals(42, ProjectVersionCode.parse(" 00042 "));
    }

    @Test public void rejectsInvalidAndOverflowingInputWithoutWrapping() {
        for (String value : new String[]{null, "", " ", "0", "-1", "2100000001", "2147483647",
                "9223372036854775807", "9223372036854775808", "1.5", "not_a_number"}) {
            assertEquals(String.valueOf(value), 0, ProjectVersionCode.parse(value));
        }
    }

    @Test public void pickerClampsNumericExtremes() {
        assertEquals(1, ProjectVersionCode.clampForPicker("-9223372036854775808"));
        assertEquals(1, ProjectVersionCode.clampForPicker("0"));
        assertEquals(42, ProjectVersionCode.clampForPicker("42"));
        assertEquals(2_100_000_000, ProjectVersionCode.clampForPicker("2147483647"));
        assertEquals(2_100_000_000, ProjectVersionCode.clampForPicker("9223372036854775807"));
    }

    @Test public void pickerUsesASafeDefaultForUnparseableImports() {
        for (String value : new String[]{null, "", " ", "1.5", "9223372036854775808", "bad"}) {
            assertEquals(String.valueOf(value), 1, ProjectVersionCode.clampForPicker(value));
        }
    }
}
