package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.UUID;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.util.LogUtil;

/** Compares production warnings and generation with the device's String arguments. */
@RunWith(AndroidJUnit4.class)
public class CodeTemplateFormatValidationTest {
    private Field cacheField;
    private HashMap<String, ExtraBlockInfo> original, isolated;
    private Method validate;

    @Before public void isolateLookup() throws Exception {
        BlockLoader.getBlockInfo("F32.initialize." + UUID.randomUUID());
        cacheField = BlockLoader.class.getDeclaredField("blocksByName"); cacheField.setAccessible(true);
        @SuppressWarnings("unchecked") HashMap<String, ExtraBlockInfo> map = (HashMap<String, ExtraBlockInfo>) cacheField.get(null);
        original = map; isolated = new HashMap<>(original); cacheField.set(null, isolated);
        validate = BlockLoader.class.getDeclaredMethod("validateBlock", ExtraBlockInfo.class, String.class); validate.setAccessible(true);
    }

    @After public void restoreLookup() throws Exception {
        if (cacheField != null && original != null) {
            cacheField.set(null, original); assertSame(original, cacheField.get(null));
            status("f32_loader_cache_restored", "true");
        }
    }

    @Test public void warnsForMalformedSyntaxAndFlagsWithoutChangingGeneratedFailure() throws Exception {
        for (String template : List.of("%", "%z", "%1$", "%.s", "%t?", "%--s", "%#s", "%+s", "%-s", "%.1n", "%<%")) {
            generated(template, true);
        }
        for (String template : List.of("plain", "%%z", "%%%%_", "%s|%<s", "%-8.2s", "%5%", "%n")) generated(template, false);
    }

    @Test public void warnsWhenGeneratedStringsAreUsedAsNumericCharacterOrDateArguments() throws Exception {
        for (String template : List.of("%d", "%x", "%f", "%e", "%g", "%a", "%c", "%tY", "%TY", "%s %<d")) generated(template, true);
        for (String template : List.of("%s", "%S", "%b", "%h", "%s %<S")) generated(template, false);
    }

    @Test public void matchesDeviceFormatterForBoundedConversionsFlagsAndRuntimeEdges() throws Exception {
        int checks = 0;
        for (String template : List.of("%", "%1$", "%.s", "%t", "%t?", "%1$$s", "%_", "%é", "%4.s",
                "%%z", "%%%%_", "%%%d", "%z %s", "abc % \n xyz %s", "%--s", "%#s", "%d", "%tY",
                "%0$s", "%0$%", "%0$n", "%2147483648$s", "%s %2147483648$s", "%1$<s",
                "%2147483648s", "%-2147483648s", "%.2147483648s", "%n %.2147483648n",
                "%2147483648%", "%.2147483648%", "%2147483648n", "%s|%<s", "%1$% %<s")) {
            compare(template); checks++;
        }
        String[] flags = {"", "-", "+", " ", "0", ",", "(", "#", "<", "--", "++", "  ", "00", ",,", "((", "##", "<<", "-0", "+ ", "+("};
        for (char conversion : "bBhHsScCdoxXeEfgGaA%nz".toCharArray()) {
            for (String flag : flags) for (String width : new String[]{"", "1", "8"}) for (String precision : new String[]{"", ".0", ".2"}) {
                compare("%s|%" + flag + width + precision + conversion); checks++;
            }
        }
        for (char prefix : new char[]{'t', 'T'}) for (char conversion : "HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc%n?".toCharArray()) {
            compare("%s|%" + prefix + conversion); checks++;
        }
        assertEquals(4061, checks); status("f32_formatter_oracle_checks", String.valueOf(checks));
    }

    private void compare(String template) throws Exception {
        boolean fails = false;
        try { String.format(template, "a", "b", "c", "d", "e"); }
        catch (IllegalFormatException error) { fails = true; }
        ExtraBlockInfo info = info(template, "call %s.a %s.b %s.c");
        assertEquals(template, fails, warned(info));
    }

    @Test(timeout = 5000) public void checksHugeWidthsWithoutAllocatingTheirOutput() {
        long started = System.nanoTime();
        for (String code : List.of("%2147483647s", "%.2147483647s", "%-2147483647s", "%1$2147483647s",
                "%" + "9".repeat(2048) + "s", "%." + "9".repeat(2048) + "s")) {
            assertNull(code, CodeTemplateValidator.findProblem(code, 3));
        }
        assertEquals(CodeTemplateValidator.Kind.NOT_ENOUGH_ARGUMENTS, CodeTemplateValidator.findProblem("%2147483647$s", 3).kind);
        status("f32_production_large_width_ms", String.valueOf((System.nanoTime() - started) / 1000000));
    }

    private void generated(String template, boolean expectedWarning) throws Exception {
        ExtraBlockInfo info = info(template, "call %s.value");
        assertEquals(template, expectedWarning, warned(info));
        BlockBean bean = new BlockBean(); bean.id = "1"; bean.opCode = info.getName(); bean.spec = info.getSpec();
        bean.type = " "; bean.typeName = ""; bean.nextBlock = -1; bean.parameters.add("value");
        BlockInterpreter interpreter = new BlockInterpreter("F32Activity", null, new ArrayList<>(List.of(bean)), false);
        ArrayList<String> args = interpreter.getBlockParams(bean); args.add(""); args.add("");
        String formatted = null, error = null;
        try { formatted = String.format(template, args.toArray()); }
        catch (IllegalFormatException failure) { error = failure.getClass().getSimpleName(); }
        isolated.put(info.getName(), info);
        String generated = interpreter.interpretBlocks(); isolated.remove(info.getName());
        if (error == null) assertEquals(template, formatted, generated);
        else assertTrue(template + ": " + generated, generated.startsWith("/* Failed to resolve Custom Block's code:") && generated.contains(error));
    }

    private static ExtraBlockInfo info(String code, String spec) {
        ExtraBlockInfo info = new ExtraBlockInfo(); info.setName("F32" + UUID.randomUUID().toString().replace("-", ""));
        info.setCode(code); info.setSpec(spec); return info;
    }

    private boolean warned(ExtraBlockInfo info) throws Exception {
        Field enabled = LogUtil.class.getDeclaredField("loggingEnabled"); enabled.setAccessible(true);
        boolean oldEnabled = enabled.getBoolean(null); PrintStream oldOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(captured, true, "UTF-8")) {
            enabled.setBoolean(null, true); System.setOut(stream); validate.invoke(null, info, "F32 isolated validation");
        } finally { System.setOut(oldOut); enabled.setBoolean(null, oldEnabled); }
        return captured.toString("UTF-8").contains("Block '" + info.getName() + "'");
    }

    private static void status(String key, String value) {
        Bundle data = new Bundle(); data.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, data);
    }
}
