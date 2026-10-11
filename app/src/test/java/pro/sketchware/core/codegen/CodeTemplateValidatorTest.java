package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.IllegalFormatException;
import java.util.List;

public class CodeTemplateValidatorTest {
    @Test public void acceptsLiteralTextEscapesAndGeneralStringConversions() {
        for (String code : new String[]{null, "", "plain", "%%d", "%%1$s", "%s %<s", "%S %b %h", "%-8.2s", "%5% %n"}) {
            assertNull(code, CodeTemplateValidator.findProblem(code, 3));
        }
    }

    @Test public void identifiesInvalidSyntaxFlagsAndPrecisionAtTheOriginalPosition() {
        for (String code : List.of("%", "%z", "%1$", "%.s", "%t?", "%--s", "%#s", "%+s", "%-s", "%.1n", "%<%")) {
            CodeTemplateValidator.Problem problem = CodeTemplateValidator.findProblem("abc " + code, 3);
            assertNotNull(code, problem); assertEquals(code, CodeTemplateValidator.Kind.INVALID_FORMAT, problem.kind);
            assertEquals(code, 5, problem.position); assertNotNull(problem.errorType);
            assertTrue(problem.describe(), problem.describe().contains("position 5"));
        }
    }

    @Test public void identifiesConversionsThatCannotUseGeneratedCodeStrings() {
        for (String code : List.of("%d", "%x", "%f", "%e", "%g", "%a", "%c", "%tY", "%TY", "%s %<d")) {
            CodeTemplateValidator.Problem problem = CodeTemplateValidator.findProblem(code, 3);
            assertNotNull(code, problem); assertEquals(code, CodeTemplateValidator.Kind.STRING_CONVERSION, problem.kind);
            assertTrue(problem.describe(), problem.describe().contains("with %s") && problem.describe().contains("write %%"));
        }
    }

    @Test public void sharesArgumentSelectionRulesWithLoader() {
        assertEquals(CodeTemplateValidator.Kind.MISSING_PREVIOUS_ARGUMENT, CodeTemplateValidator.findProblem("%<s", 3).kind);
        assertEquals(CodeTemplateValidator.Kind.MISSING_PREVIOUS_ARGUMENT, CodeTemplateValidator.findProblem("%2147483648$s", 3).kind);
        CodeTemplateValidator.Problem missing = CodeTemplateValidator.findProblem("%4$s", 3);
        assertEquals(CodeTemplateValidator.Kind.NOT_ENOUGH_ARGUMENTS, missing.kind);
        assertEquals(4, missing.requiredArguments); assertEquals(3, missing.availableArguments);
        assertNull(CodeTemplateValidator.findProblem("%s %2147483648$s", 3));
        assertNull(CodeTemplateValidator.findProblem("%0$s", 3));
    }

    @Test(timeout = 5000) public void boundsOutputForHugeWidthPrecisionAndArgumentIndex() {
        for (String code : List.of("%2147483647s", "%.2147483647s", "%-2147483647s", "%1$2147483647s")) {
            assertNull(code, CodeTemplateValidator.findProblem(code, 3));
        }
        assertEquals(CodeTemplateValidator.Kind.NOT_ENOUGH_ARGUMENTS, CodeTemplateValidator.findProblem("%2147483647$s", 3).kind);
        // Overflow validity differs by runtime, but checking it must never allocate huge output.
        for (String code : List.of("%" + "9".repeat(2048) + "s", "%." + "9".repeat(2048) + "s")) {
            CodeTemplateValidator.findProblem(code, 3);
        }
    }

    @Test public void agreesWithFormatterForBoundedConversionsFlagsAndPrecisions() {
        String[] flags = {"", "-", "+", " ", "0", ",", "(", "#", "<", "--", "++", "  ", "00", ",,", "((", "##", "<<", "-0", "+ ", "+("};
        int checks = 0;
        for (char conversion : "bBhHsScCdoxXeEfgGaA%nz".toCharArray()) {
            for (String flag : flags) for (String width : new String[]{"", "1", "8"}) for (String precision : new String[]{"", ".0", ".2"}) {
                String template = "%s|%" + flag + width + precision + conversion;
                boolean fails = false;
                try { String.format(template, "a", "b", "c", "d", "e"); }
                catch (IllegalFormatException expected) { fails = true; }
                assertEquals(template, fails, CodeTemplateValidator.findProblem(template, 5) != null); checks++;
            }
        }
        assertEquals(3960, checks);
    }
}
