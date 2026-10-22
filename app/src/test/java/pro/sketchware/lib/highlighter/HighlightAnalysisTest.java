package pro.sketchware.lib.highlighter;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class HighlightAnalysisTest {
    private static SyntaxScheme rule(String regex, int color) {
        return new SyntaxScheme(Pattern.compile(regex), color);
    }

    @Test public void windowIncludesCrossingCommentsAndPreservesRuleOrder() {
        String body = "return 1; /* first\nsecond */ return 2;";
        HighlightAnalysis result = HighlightAnalysis.scan(body,
                List.of(rule("return", 10), rule("/\\*[\\s\\S]*?\\*/", 20)), () -> false);
        int commentEnd = body.indexOf("*/") + 2;
        int lastReturn = body.lastIndexOf("return");
        assertArrayEquals(new int[]{10, commentEnd, 20}, result.window(18, 20));
        assertArrayEquals(new int[]{0, 6, 10, lastReturn, lastReturn + 6, 10, 10, commentEnd, 20},
                result.window(0, body.length()));
    }

    @Test public void windowBoundariesDoNotIncludeAdjacentMatches() {
        HighlightAnalysis result = HighlightAnalysis.scan("abc def ghi",
                List.of(rule("\\w+", 7)), () -> false);
        assertArrayEquals(new int[]{4, 7, 7}, result.window(3, 8));
        assertArrayEquals(new int[0], result.window(7, 8));
        assertArrayEquals(new int[0], result.window(5, 5));
    }

    @Test public void cancellationDiscardsPartialResults() {
        AtomicInteger checks = new AtomicInteger();
        assertNull(HighlightAnalysis.scan("a ".repeat(10000), List.of(rule("a", 1)),
                () -> checks.incrementAndGet() > 20));
        assertTrue(checks.get() < 30);
    }

    @Test public void largeDocumentRetainsEndAndZeroLengthPatternsAreIgnored() {
        String body = "return 1;\n".repeat(10000);
        HighlightAnalysis result = HighlightAnalysis.scan(body,
                List.of(rule("return", 4), rule("(?=return)", 8)), () -> false);
        assertArrayEquals(new int[]{99990, 99996, 4}, result.window(99990, 100000));
    }

    @Test public void linearCommentScanMatchesExistingRegexIncludingUnclosedAndUnicodeLines() {
        SyntaxScheme comments = rule("/\\*[\\s\\S]*?\\*/|//.*", 9);
        ArrayList<String> inputs = new ArrayList<>(List.of("/* unclosed // line\nnext", "/*/ // tail",
                "/**/ // tail", "// /* ignored\n/* ok */", "// one\u0085// two\u2028// three\u2029//four\r\n"));
        Random random = new Random(318);
        String alphabet = "/* ab\r\n\u0085\u2028\u2029";
        for (int i = 0; i < 500; i++) {
            StringBuilder body = new StringBuilder();
            for (int j = 0; j < 200; j++) body.append(alphabet.charAt(random.nextInt(alphabet.length())));
            inputs.add(body.toString());
        }
        for (String input : inputs) {
            ArrayList<Integer> expected = new ArrayList<>();
            var matcher = comments.pattern.matcher(input);
            while (matcher.find()) {
                expected.add(matcher.start()); expected.add(matcher.end()); expected.add(9);
            }
            int[] actual = HighlightAnalysis.scan(input, List.of(comments), () -> false).window(0, input.length());
            assertArrayEquals(input, expected.stream().mapToInt(Integer::intValue).toArray(), actual);
        }
    }

    @Test public void unclosedCommentScanCanBeCancelledInsideTheSearch() {
        AtomicInteger checks = new AtomicInteger();
        assertNull(HighlightAnalysis.scan("/* ".repeat(100000),
                List.of(rule("/\\*[\\s\\S]*?\\*/|//.*", 9)), () -> checks.incrementAndGet() > 6));
        assertEquals(7, checks.get());
    }
}
