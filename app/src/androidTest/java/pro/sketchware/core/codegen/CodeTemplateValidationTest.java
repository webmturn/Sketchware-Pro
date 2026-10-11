package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.UUID;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.util.LogUtil;

/** Checks production loading warnings against the device's Formatter and actual generated code. */
@RunWith(AndroidJUnit4.class)
public class CodeTemplateValidationTest {
    private Field cacheField;
    private HashMap<String, ExtraBlockInfo> original, isolated;
    private Method count, validate;

    @Before public void isolateBlockLookup() throws Exception {
        BlockLoader.getBlockInfo("F31.initialize." + UUID.randomUUID());
        cacheField = BlockLoader.class.getDeclaredField("blocksByName"); cacheField.setAccessible(true);
        @SuppressWarnings("unchecked") HashMap<String, ExtraBlockInfo> map = (HashMap<String, ExtraBlockInfo>) cacheField.get(null);
        original = map; isolated = new HashMap<>(original); cacheField.set(null, isolated);
        try { count = BlockLoader.class.getDeclaredMethod("countCodeArguments", String.class); }
        catch (NoSuchMethodException beforeFix) { count = BlockLoader.class.getDeclaredMethod("countCodePlaceholders", String.class); }
        count.setAccessible(true);
        validate = BlockLoader.class.getDeclaredMethod("validateBlock", ExtraBlockInfo.class, String.class); validate.setAccessible(true);
    }

    @After public void restoreBlockLookup() throws Exception {
        if (cacheField != null && original != null) {
            cacheField.set(null, original); assertSame(original, cacheField.get(null));
            status("f31_loader_cache_restored", "true");
        }
    }

    @Test public void ignoresLiteralPercentSignsIncludingPercentParity() throws Exception {
        sample("plain", 0, false, 0);
        sample("%%s %%s %%s", 0, false, 0);
        for (int n = 0; n <= 16; n++) sample("%".repeat(n) + "s", n % 2, false, 0);
        sample("%%1$s %%<s %%3$s", 0, false, 0);
        sample("System.out.println(\"%%s %%s %%s\");", 0, false, 0);
    }

    @Test public void tracksExplicitIndicesAndIndependentOrdinaryIndices() throws Exception {
        sample("%1$s %1$s %1$s %1$s", 1, false, 0);
        sample("%3$s", 3, false, 0);
        sample("%2$s %1$s", 2, false, 0);
        sample("%3$s %s", 3, false, 0);
        sample("%2$s %s %s", 2, false, 0);
        sample("%s %2$s %s", 2, false, 0);
        sample("%4$s %s %s", 4, false, 0);
        sample("%s|%s|%s", 3, false, 0);
        sample("%s|%s|%s", 3, false, 1);
        sample("System.out.println(%1$s);\nSystem.out.println(%1$s);\nSystem.out.println(%1$s);\nSystem.out.println(%1$s);", 1, false, 1);
        sample("%2147483647$s", Integer.MAX_VALUE, false, 0);
    }

    @Test public void reusesOnlyAnArgumentThatWasPreviouslySelected() throws Exception {
        sample("%s %<s %<s %<s", 1, false, 0);
        sample("%3$s %<s", 3, false, 0);
        sample("%2$s %1$<s", 2, false, 0);
        sample("%s %% %<s %n %<s", 1, false, 0);
        sample("%s %s %s %<s", 3, false, 0);
        sample("%2$s %s %<s %s", 2, false, 0);
        for (String template : List.of("%<s", "%% %<s", "%n %<s", "%1$<s")) sample(template, 0, true, 0);
    }

    @Test public void countsGeneralConversionsWithFlagsWidthAndPrecision() throws Exception {
        sample("%-8.2s %-8.2s %-8.2s", 3, false, 0);
        sample("%S %S %S", 3, false, 0);
        sample("%b %h %s", 3, false, 0);
        sample("%3$8.2s", 3, false, 0);
        sample("%-8.2s %3$8.2s %<S", 3, false, 1);
        sample("%5% %n", 0, false, 0);
        sample("%4$% %4$n", 0, false, 0);
    }

    @Test public void preservesRuntimeIndexEdgesAndExistingFormattingErrorHandling() throws Exception {
        sample("%0$s", 1, false, 0);
        sample("%0$s %0$s %0$s", 3, false, 0);
        sample("%2$s %0$s %s", 2, false, 0);
        sample("%0$% %0$n", 0, false, 0);
        sample("%2147483648$s", 0, true, 0);
        sample("%999999999999999999999999$s", 0, true, 0);
        sample("%s %2147483648$s", 1, false, 0);
        sample("%2$s %999999999999999999999999$s", 2, false, 0);
        sample("%2147483648s", 1, false, 0);
        sample("%.2147483648s", 1, false, 0);
        for (String template : List.of("%z", "%", "%--s", "%#s", "%d", "%tY")) sample(template, -1, false, 0, true);
    }

    @Test public void matchesFormatterArgumentSelectionForAllThreeTokenCombinations() throws Exception {
        String[] tokens = {"%s", "%S", "%1$s", "%2$s", "%3$s", "%<s", "%1$<s", "%%", "%n", "%-4.2s", "%b", "%h"};
        int checks = 0;
        ExtraBlockInfo info = new ExtraBlockInfo(); info.setName("F31Oracle"); info.setSpec("call %s.a %s.b %s.c");
        for (String first : tokens) for (String second : tokens) for (String third : tokens) {
            String template = first + "|" + second + "|" + third;
            int minimum = -1;
            for (int size = 0; size <= 3; size++) {
                Object[] args = new Object[size]; Arrays.fill(args, "value");
                try { String.format(template, args); minimum = size; break; }
                catch (MissingFormatArgumentException expected) { }
            }
            info.setCode(template);
            if (minimum >= 0) assertEquals(template, minimum, ((Integer) count.invoke(null, template)).intValue());
            assertEquals(template, minimum < 0, warned(info)); checks++;
        }
        assertEquals(1728, checks); status("f31_formatter_oracle_checks", String.valueOf(checks));
    }

    private void sample(String template, int required, boolean missingPrevious, int fields) throws Exception {
        sample(template, required, missingPrevious, fields, false);
    }

    private void sample(String template, int required, boolean missingPrevious, int fields, boolean invalidFormat) throws Exception {
        ExtraBlockInfo info = new ExtraBlockInfo(); info.setName("F31" + UUID.randomUUID().toString().replace("-", "")); info.setCode(template);
        StringBuilder spec = new StringBuilder("call");
        BlockBean bean = new BlockBean(); bean.id = "1"; bean.opCode = info.getName(); bean.type = " "; bean.typeName = ""; bean.nextBlock = -1;
        for (int i = 0; i < fields; i++) { spec.append(" %s.value").append(i); bean.parameters.add("value"); }
        bean.spec = spec.toString(); info.setSpec(bean.spec);
        int actualCount = (Integer) count.invoke(null, template);
        boolean warning = warned(info);
        BlockInterpreter interpreter = new BlockInterpreter("F31AuditActivity", null, new ArrayList<>(List.of(bean)), false);
        ArrayList<String> args = interpreter.getBlockParams(bean); args.add(""); args.add("");
        String formatted = null, error = null;
        try { formatted = String.format(template, args.toArray()); }
        catch (Exception exception) { error = exception.getClass().getSimpleName() + ": " + exception.getMessage(); }
        isolated.put(info.getName(), info);
        String generated = interpreter.interpretBlocks(); isolated.remove(info.getName());
        Map<String, Object> row = new HashMap<>(); row.put("template", template); row.put("spec", bean.spec);
        row.put("required", required); row.put("actualCount", actualCount); row.put("available", args.size());
        row.put("missingPrevious", missingPrevious); row.put("actualWarning", warning); row.put("formatError", error);
        row.put("formatted", formatted); row.put("generated", generated);
        status("f31_fixed_row", new Gson().toJson(row));
        if (required >= 0) assertEquals(template, required, actualCount);
        assertEquals(template, invalidFormat || missingPrevious || required > args.size(), warning);
        if (error == null) assertEquals(template, formatted, generated);
        else assertTrue(generated, generated.startsWith("/* Failed to resolve Custom Block's code:") && generated.contains(error.split(":", 2)[0]));
    }

    private boolean warned(ExtraBlockInfo info) throws Exception {
        Field enabled = LogUtil.class.getDeclaredField("loggingEnabled"); enabled.setAccessible(true);
        boolean oldEnabled = enabled.getBoolean(null); PrintStream oldOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(captured, true, "UTF-8")) {
            enabled.setBoolean(null, true); System.setOut(stream); validate.invoke(null, info, "F31 isolated validation");
        } finally { System.setOut(oldOut); enabled.setBoolean(null, oldEnabled); }
        return captured.toString("UTF-8").contains("Block '" + info.getName() + "'");
    }

    private static void status(String key, String value) {
        Bundle data = new Bundle(); data.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, data);
    }
}
