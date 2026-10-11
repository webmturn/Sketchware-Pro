package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.Arrays;
import java.util.MissingFormatArgumentException;

public class CodeTemplateArgumentsTest {
    @Test public void handlesAbsentAndLiteralTemplates() {
        count(null, 0); count("", 0); count("plain", 0); count("%%s %%s %%s", 0);
    }

    @Test public void consumesEscapedPercentsBeforeConsideringAnotherSpecifier() {
        for (int n = 0; n <= 16; n++) count("%".repeat(n) + "s", n % 2);
        count("%%1$s %%<s %%3$s", 0);
    }

    @Test public void usesTheHighestExplicitIndexRatherThanOccurrenceCount() {
        count("%1$s %1$s %1$s %1$s", 1); count("%3$s", 3);
        count("%2$s %1$s", 2); count("%2147483647$s", Integer.MAX_VALUE);
    }

    @Test public void advancesOrdinaryIndicesIndependentlyFromExplicitReferences() {
        count("%2$s %s %s", 2); count("%s %2$s %s", 2);
        count("%4$s %s %s %s", 4); count("%2$s %s %s %s", 3);
    }

    @Test public void reusesTheLastArgumentWithoutAdvancingOrdinaryIndices() {
        count("%s %<s %<s %<s", 1); count("%3$s %<s", 3);
        count("%s %% %<s %n %<s", 1); count("%2$s %1$<s", 2);
        count("%2$s %s %<s %s", 2);
    }

    @Test public void detectsReuseWithoutAnyPreviousArgument() {
        for (String template : new String[]{"%<s", "%% %<s", "%n %<s", "%1$<s"}) {
            assertNotNull(template, CodeTemplateArguments.analyze(template).missingPreviousSpecifier);
        }
    }

    @Test public void includesFlagsWidthsPrecisionsAndAllArgumentConversions() {
        count("%-8.2s %3$8.2s %<S", 3); count("%S %S %S", 3);
        count("%b %h %s", 3); count("%d %x %f %tY", 4);
        count("%5% %n %4$% %4$n", 0);
    }

    @Test public void preservesAndroidLegacyIndexParsingVerifiedOnTheDevice() {
        count("%0$s %0$s %0$s", 3); count("%2$s %0$s %s", 2);
        count("%s %2147483648$s", 1); count("%2$s %99999999999999999999$s", 2);
        assertNotNull(CodeTemplateArguments.analyze("%2147483648$s").missingPreviousSpecifier);
        count("%0$% %0$n", 0);
    }

    @Test public void neverFormatsOrAllocatesAccordingToWidthOrIndex() {
        count("%2147483647s %.2147483647s", 2);
        count("%" + "9".repeat(2048) + "s", 1);
        assertNotNull(CodeTemplateArguments.analyze("%" + "9".repeat(2048) + "$s").missingPreviousSpecifier);
    }

    @Test public void agreesWithActualFormatterForEveryThreeTokenCombination() {
        String[] tokens = {"%s", "%S", "%1$s", "%2$s", "%3$s", "%<s", "%1$<s", "%%", "%n", "%-4.2s", "%b", "%h"};
        for (String first : tokens) for (String second : tokens) for (String third : tokens) {
            String template = first + "|" + second + "|" + third;
            CodeTemplateArguments.Analysis analysis = CodeTemplateArguments.analyze(template);
            int minimum = -1;
            for (int size = 0; size <= 3; size++) {
                Object[] args = new Object[size]; Arrays.fill(args, "value");
                try { String.format(template, args); minimum = size; break; }
                catch (MissingFormatArgumentException expected) { }
            }
            if (minimum < 0) assertNotNull(template, analysis.missingPreviousSpecifier);
            else { assertNull(template, analysis.missingPreviousSpecifier); assertEquals(template, minimum, analysis.requiredCount); }
        }
    }

    private static void count(String template, int expected) {
        CodeTemplateArguments.Analysis analysis = CodeTemplateArguments.analyze(template);
        assertEquals(template, expected, analysis.requiredCount);
        assertNull(template, analysis.missingPreviousSpecifier);
    }
}
