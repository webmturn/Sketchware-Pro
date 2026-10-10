package pro.sketchware.util.format;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pro.sketchware.core.codegen.ComponentCodeGenerator;
import pro.sketchware.core.codegen.ComponentTypeMapper;
import pro.sketchware.core.project.ClassInfo;

public class BlockSpecParsingTest {
    @Test public void preservesOrdinarySpecsAndExistingSeparators() {
        tokens("");
        tokens("   ");
        tokens("  set  %s.value %b.flag %d.count %m.textview  ",
                "set", "%s.value", "%b.flag", "%d.count", "%m.textview");
        tokens("before%s.value?tail-%d.count", "before", "%s.value", "?tail-", "%d.count");
        tokens("one\ttwo\nthree", "one\ttwo\nthree");
    }

    @Test public void preservesEscapedSpaceAndUnicodeAsOneToken() {
        tokens("hello\\ world 中文\\ 标签 \\😀", "hello\\ world", "中文\\ 标签", "\\😀");
    }

    @Test public void preservesEscapedPercentAsLiteral() {
        tokens("\\%s.literal label\\%d.text%s.real", "\\%s.literal", "label\\%d.text", "%s.real");
    }

    @Test public void preservesEscapedBackslashAndFollowingTokenBoundaries() {
        tokens("path\\\\name \\\\%s.real", "path\\\\name", "\\\\", "%s.real");
    }

    @Test public void preservesEscapedQuestionAndHyphenInsideParameter() {
        tokens("%s.value\\?suffix\\-end next", "%s.value\\?suffix\\-end", "next");
    }

    @Test public void consumesUnpairedFinalBackslashWithoutOverrunning() {
        tokens("\\", "\\");
        tokens("end\\", "end\\");
        tokens("%s.value end\\", "%s.value", "end\\");
        tokens("three\\\\\\", "three\\\\\\");
    }

    @Test public void advancesScannerByConsumedCharactersAndStaysAtEnd() {
        FormatUtil.StringScanner scanner = new FormatUtil.StringScanner("a\\ b %d.count tail\\");
        assertEquals("a\\ b", scanner.nextToken());
        assertEquals(4, scanner.position);
        assertEquals("%d.count", scanner.nextToken());
        assertEquals(13, scanner.position);
        assertEquals("tail\\", scanner.nextToken());
        assertEquals(scanner.input.length(), scanner.position);
        assertEquals("", scanner.nextToken());
        assertEquals(scanner.input.length(), scanner.position);
    }

    @Test public void keepsExistingLabelUnescapeRulesIncludingDanglingMarker() {
        assertEquals("hello world", FormatUtil.unescapeString("hello\\ world"));
        assertEquals("%s.literal", FormatUtil.unescapeString("\\%s.literal"));
        assertEquals("path\\name", FormatUtil.unescapeString("path\\\\name"));
        assertEquals("nrt", FormatUtil.unescapeString("\\n\\r\\t"));
        assertEquals("end", FormatUtil.unescapeString("end\\"));
        assertEquals("", FormatUtil.unescapeString("\\"));
    }

    @Test public void generatedMethodUsesOnlyRealParametersInOrder() {
        assertEquals("public void _demo(final String _value, final boolean _flag, final double _count, final TextView _label) {\r\n\r\n}\r\n",
                ComponentCodeGenerator.getMoreBlockCode("demo",
                        "demo hello\\ world \\%s.fake %s.value %b.flag %d.count %m.textview.label", ""));
    }

    @Test public void generatedMethodAcceptsTrailingLabelEscape() {
        assertEquals("public String _demo(final String _value) {\r\nreturn _value;\r\n}\r\n",
                ComponentCodeGenerator.getMoreBlockCode("demo[String]", "demo %s.value end\\", "return _value;"));
    }

    @Test public void parameterMetadataUsesOnlyRealParametersInOrder() {
        assertEquals(List.of("String", "boolean", "double", "TextView"), classes(
                "demo hello\\ world \\%d.fake %s.value %b.flag %d.count %m.textview"));
    }

    @Test public void parameterMetadataAcceptsTrailingLabelEscape() {
        assertEquals(List.of("String"), classes("demo %s.value end\\"));
    }

    private static void tokens(String spec, String... expected) {
        assertEquals(spec, Arrays.asList(expected), FormatUtil.parseBlockSpec(spec));
    }

    private static List<String> classes(String spec) {
        return ComponentTypeMapper.getParamClassInfoList(spec).stream()
                .map(ClassInfo::getClassName).collect(Collectors.toList());
    }
}
