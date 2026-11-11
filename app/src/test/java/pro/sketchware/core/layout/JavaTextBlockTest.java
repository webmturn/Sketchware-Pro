package pro.sketchware.core.layout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavaTextBlockTest {
    @Test
    public void escapedQuotesAndTrailingBackslashAreValidInTextBlocks() {
        // checkJavaImports scans this fixture and must ignore the example import.
        String example =
                """
                \"""
                import pro.sketchware.core.layout.MaterialButtonAttributes;
                C:\\""";

        assertEquals(
                "\"\"\"\nimport pro.sketchware.core.layout.MaterialButtonAttributes;\nC:\\",
                example);
    }
}
