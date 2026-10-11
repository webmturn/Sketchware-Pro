package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pro.sketchware.beans.BlockBean;

public class BlockInterpreterParametersTest {
    @Test public void extractsOnlyRealTypesInOrder() {
        assertEquals(List.of("%s.value", "%d.count", "%b.flag", "%m.textview"),
                interpreter(true).extractParamsTypes("show \\%m.textview \\%s.fake %s.value %d.count %b.flag %m.textview"));
        assertEquals(List.of(), interpreter(true).extractParamsTypes("\\%s \\%d \\%b \\%m.textview tail\\"));
    }

    @Test public void preservesLegacyTypePrefixesAndExtensions() {
        assertEquals(List.of("%s.inputonly", "%m.textview", "%l.custom", "%n.count", "%v.widget", "%p.intent"),
                interpreter(true).extractParamsTypes("demo %s.inputOnly %m.textview.label %l.Custom.values %n.count %v.Widget %p.intent"));
    }

    @Test public void handlesBackslashParityBeforeTypesAndConversions() {
        for (int n = 0; n <= 8; n++) {
            String spec = "show " + "\\".repeat(n) + "%m.textview %s.real";
            if (n % 2 == 0) {
                assertEquals(List.of("%m.textview", "%s.real"), interpreter(true).extractParamsTypes(spec));
                params(true, spec, List.of("binding.title", "\"value\""), "title", "value");
            } else {
                assertEquals(List.of("%s.real"), interpreter(true).extractParamsTypes(spec));
                params(true, spec, List.of("\"value\""), "value");
            }
        }
    }

    @Test public void ignoresEscapedPercentsWithinRealParameterTokens() {
        String spec = "show %s.default\\%m.textview %m.textview";
        assertEquals(List.of("%s.default", "%m.textview"), interpreter(true).extractParamsTypes(spec));
        params(true, spec, List.of("\"value\"", "binding.title"), "value", "title");
    }

    @Test public void convertsOnlyRealWidgetParametersWithBindingEnabled() {
        for (boolean binding : List.of(false, true)) {
            params(binding, "show \\%m.textview %s.real", List.of("\"value\""), "value");
            params(binding, "show \\%s.fake %m.textview", List.of(binding ? "binding.title" : "title"), "title");
            params(binding, "show %m.textview %s.real", List.of(binding ? "binding.titleText" : "title_text", "\"value\""), "title_text", "value");
            params(binding, "show %m.textview", List.of("binding.title"), "binding.title");
            params(binding, "show %m.intent", List.of("intent"), "intent");
        }
    }

    @Test public void convertsOnlyRealColorsAndPreservesContexts() {
        params(true, "show \\%m.color %s.real", List.of("\"R.color.primary\""), "R.color.primary");
        color("AuditActivity", "show \\%s.fake %m.color", "R.color.primary", "getResources().getColor(R.color.primary)");
        color("AuditActivity", "show \\%m.textview %m.color", "R.attr.colorPrimary", "SketchwareUtil.getMaterialColor(AuditActivity.this, R.attr.colorPrimary)");
        color("AuditFragmentActivity", "show \\%s.fake %m.color", "R.attr.colorPrimary", "SketchwareUtil.getMaterialColor(getContext(), R.attr.colorPrimary)");
        color("AuditActivity", "show %m.color", "getMaterialColor(R.attr.colorPrimary)", "SketchwareUtil.getMaterialColor(AuditActivity.this, R.attr.colorPrimary)");
        color("AuditActivity", "show %m.color", "0xFF123456", "0xFF123456");
    }

    @Test public void preservesEmptyDefaultsAndRawInputTypes() {
        params(true, "show \\%s.fake %b.flag %d.count %s.value", List.of("true", "0", "\"\""), "", "", "");
        params(true, "show %d.count", List.of("2.5d"), "2.5");
        assertEquals("calculate()", interpreter(true).getParamValue("calculate()", "%s.inputonly"));
    }

    @Test public void skipsOnlyRealEmptySelectors() {
        for (String spec : List.of("set %m.textview %s.value", "set \\%s.fake %m.textview %s.value",
                "set \\%m then %m.textview %s.value", "set \\%m.textview %m.textview %s.value")) {
            assertEquals(spec, "", code(true, "setText", spec, "", "value"));
        }
        assertEquals("", code(true, "setText", "set %s.label\\%s.fake %m.textview", "value", ""));
        assertEquals("_demo(\"\");", code(true, "definedFunc", "demo \\%m.textview %s.value", ""));
    }

    @Test public void generatesStatementsWithCorrectWidgetAndStringArguments() {
        for (boolean binding : List.of(false, true)) {
            String expected = (binding ? "binding.title" : "title") + ".setText(\"value\");";
            for (String prefix : List.of("", "\\%s.fake ", "\\%m.textview ", "\\%m ")) {
                assertEquals(expected, code(binding, "setText", "set " + prefix + "%m.textview %s.value", "title", "value"));
            }
        }
    }

    @Test public void generatesMoreBlockCallsWithoutRewritingStringLiterals() {
        assertEquals("_demo(binding.title, \"value\");",
                code(true, "definedFunc", "demo \\%s.fake %m.textview %s.value", "title", "value"));
        assertEquals("_demo(\"value\");", code(true, "definedFunc", "demo \\%m.textview %s.value", "value"));
        assertEquals("_demo(title, \"value\");",
                code(false, "definedFunc", "demo %m.textview %s.value", "title", "value"));
    }

    @Test public void preservesNestedBlockReferencesAndTheNextChain() {
        BlockBean first = bean("setText", "set \\%s.fake %m.textview %s.value", "@2", "value");
        first.nextBlock = 3;
        BlockBean nested = bean("getVar", "selectedView"); nested.id = "2";
        BlockBean next = bean("definedFunc", "after \\%m.textview %s.value", "done"); next.id = "3";
        assertEquals("selectedView.setText(\"value\");\r\n_after(\"done\");",
                new BlockInterpreter("AuditActivity", null, new ArrayList<>(List.of(first, nested, next)), true).interpretBlocks());
    }

    private static BlockInterpreter interpreter(boolean binding) {
        return new BlockInterpreter("AuditActivity", null, new ArrayList<>(), binding);
    }

    private static void color(String activity, String spec, String raw, String expected) {
        // Color/custom type metadata reads Android storage; the device regression covers that layer.
        BlockInterpreter interpreter = new BlockInterpreter(activity, null, new ArrayList<>(), true);
        String type = interpreter.getParamType(interpreter.extractParamsTypes(spec), 0);
        assertEquals(expected, interpreter.getParamValue(raw, type));
    }

    private static void params(boolean binding, String spec, List<String> expected, String... raw) {
        assertEquals(spec, expected, interpreter(binding).getBlockParams(bean("definedFunc", spec, raw)));
    }

    private static String code(boolean binding, String opcode, String spec, String... raw) {
        return new BlockInterpreter("AuditActivity", null, new ArrayList<>(List.of(bean(opcode, spec, raw))), binding).interpretBlocks();
    }

    private static BlockBean bean(String opcode, String spec, String... raw) {
        BlockBean bean = new BlockBean();
        bean.id = "1"; bean.opCode = opcode; bean.spec = spec; bean.type = " "; bean.typeName = ""; bean.nextBlock = -1;
        bean.parameters.addAll(Arrays.asList(raw));
        return bean;
    }
}
