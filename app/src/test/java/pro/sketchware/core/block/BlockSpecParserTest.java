package pro.sketchware.core.block;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

import pro.sketchware.core.codegen.ComponentTypeMapper;

public class BlockSpecParserTest {
    @Test public void agreesWithParameterMetadataForOrdinaryAndEscapedSpecs() {
        String spec = "show \\%m.fake \\%s.fake %s.value %d.count %b.flag %m.textview";
        assertEquals(List.of("%s.value", "%d.count", "%b.flag", "%m.textview"),
                BlockSpecParser.parseParameters(spec));
        assertEquals(ComponentTypeMapper.getParamClassInfoList(spec).size(),
                BlockSpecParser.parseParameters(spec).size());
    }

    @Test public void ignoresEscapedPercentsWithinLabelsAndParameters() {
        assertEquals(List.of(), BlockSpecParser.parseParameters("\\%s \\%d \\%b \\%m label\\%s"));
        assertEquals(List.of("%s.default\\%d.fake", "%m.view\\%m"),
                BlockSpecParser.parseParameters("%s.default\\%d.fake %m.view\\%m"));
        assertEquals(-1, BlockSpecParser.findInvalidMenuParameterEnd("%m.view\\%m"));
    }

    @Test public void distinguishesOddAndEvenBackslashes() {
        for (int n = 0; n <= 8; n++) {
            String prefix = "label " + "\\".repeat(n);
            assertEquals("backslashes=" + n, n % 2 == 0 ? List.of("%s.value") : List.of(),
                    BlockSpecParser.parseParameters(prefix + "%s.value"));
            assertEquals("backslashes=" + n, n % 2 == 0 ? prefix.length() + 2 : -1,
                    BlockSpecParser.findInvalidMenuParameterEnd(prefix + "%m"));
        }
    }

    @Test public void keepsSeparatorsAndTrailingEscapes() {
        assertEquals(List.of("%s.value", "%d.count", "%b.flag"),
                BlockSpecParser.parseParameters("label%s.value?tail-%d.count %b.flag end\\"));
        assertEquals(-1, BlockSpecParser.findInvalidMenuParameterEnd("end\\"));
    }

    @Test public void preservesExistingMenuNameValidation() {
        for (String valid : List.of("%m.view", "%m._", "%m.a1", "%m.view.more", "%m.view?label")) {
            assertEquals(valid, -1, BlockSpecParser.findInvalidMenuParameterEnd(valid));
        }
        for (String invalid : List.of("%m", "%m.", "%m.-", "%m.中文", "%menu")) {
            assertEquals(invalid, 2, BlockSpecParser.findInvalidMenuParameterEnd(invalid));
        }
    }

    @Test public void locatesOnlyTheRealMalformedMenuAfterEscapedLabels() {
        String prefix = "中文\\ 标签 \\%m %s.value ";
        assertEquals(prefix.length() + 2, BlockSpecParser.findInvalidMenuParameterEnd(prefix + "%m"));
        assertEquals(5, BlockSpecParser.findInvalidMenuParameterEnd("a  %m"));
        assertEquals(3, BlockSpecParser.findInvalidMenuParameterEnd("a%m"));
    }

    @Test public void handlesAbsentSpecsWithoutParameters() {
        assertEquals(List.of(), BlockSpecParser.parseParameters(null));
        assertEquals(List.of(), BlockSpecParser.parseParameters("   "));
        assertEquals(-1, BlockSpecParser.findInvalidMenuParameterEnd(null));
        assertEquals(-1, BlockSpecParser.findInvalidMenuParameterEnd("   "));
    }

    @Test public void countsOnlyFieldTypesSupportedByThePreview() {
        assertEquals(List.of("%s", "%d", "%b", "%m.view"),
                BlockSpecParser.parseParameters("100% %x.fake %n.number % %s %d %b %m.view"));
    }
}
