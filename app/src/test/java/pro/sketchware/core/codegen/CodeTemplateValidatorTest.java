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

    @Test public void formatFailuresStillTakePrecedenceOverEarlierArgumentFailures() {
        String prefix = "%<s %999$s " + "%1$-8.2s ".repeat(500);
        CodeTemplateValidator.Problem problem = CodeTemplateValidator.findProblem(prefix + "%d", 2);
        assertEquals(CodeTemplateValidator.Kind.STRING_CONVERSION, problem.kind);
        assertEquals("%d", problem.specifier);
        assertEquals(prefix.length() + 1, problem.position);
        problem = CodeTemplateValidator.findProblem(prefix + "%--s", 2);
        assertEquals(CodeTemplateValidator.Kind.INVALID_FORMAT, problem.kind);
        assertEquals("%--s", problem.specifier);
        assertEquals(prefix.length() + 1, problem.position);
    }

    @Test public void repeatedFormatsStillCountEveryOrdinaryAndExplicitArgument() {
        assertNull(CodeTemplateValidator.findProblem("%1$s %% %n ".repeat(1000), 1));
        var ordinary = CodeTemplateValidator.findProblem("%s ".repeat(1000), 2);
        assertEquals(CodeTemplateValidator.Kind.NOT_ENOUGH_ARGUMENTS, ordinary.kind);
        assertEquals(1000, ordinary.requiredArguments);
        String widths = "%1$-8.2s %2$-20.5s ".repeat(1000);
        assertNull(CodeTemplateValidator.findProblem(widths, 2));
        var explicit = CodeTemplateValidator.findProblem(widths, 1);
        assertEquals(2, explicit.requiredArguments);
        assertEquals(1, explicit.availableArguments);
    }

    @Test public void cachedFormatsDoNotHideFlagDateOrPrecisionFailures() {
        String valid = "%1$-8.2s ".repeat(500);
        for (String invalid : List.of("%#s", "%<d", "%.1n", "%tY", "%--s")) {
            var expected = CodeTemplateValidator.findProblem(invalid, 3);
            var actual = CodeTemplateValidator.findProblem(valid + invalid, 3);
            assertEquals(invalid, expected.kind, actual.kind);
            assertEquals(invalid, expected.errorType, actual.errorType);
            assertEquals(invalid, actual.specifier);
            assertEquals(valid.length() + 1, actual.position);
        }
        assertEquals(CodeTemplateValidator.Kind.MISSING_PREVIOUS_ARGUMENT,
                CodeTemplateValidator.findProblem("%<s " + valid, 3).kind);
    }

    @Test public void cannotSkipMalformedPercentBeforeALaterValidSpecifier() {
        for (String broken : List.of("%?", "%.?", "%1$?", "%_", "% ?")) {
            String template = "prefix " + broken + " literal %s";
            var problem = CodeTemplateValidator.findProblem(template, 1);
            assertEquals(broken, CodeTemplateValidator.Kind.INVALID_FORMAT, problem.kind);
            assertEquals("%", problem.specifier);
            assertEquals(8, problem.position);
            assertEquals("MalformedFormatSpecifier", problem.errorType);
        }
    }
}
