package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManagerCreatorActivity;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.block.view.FieldBlockView;
import pro.sketchware.util.LogUtil;
import pro.sketchware.util.format.FormatUtil;

/** Exercises spec counting, validation logs, and actual add/insert/edit saves in owned cache fixtures. */
@RunWith(AndroidJUnit4.class)
public class BlockSpecValidationTest {
    private static final Gson GSON = new Gson();
    private static final String COLOR = "#8a55d7";

    private static final class Sample {
        final String name, spec;
        final int fields, loaderCount;
        Sample(String name, String spec, int fields, int loaderCount) {
            this.name = name; this.spec = spec; this.fields = fields; this.loaderCount = loaderCount;
        }
    }

    private static List<Sample> samples() {
        List<Sample> result = new ArrayList<>(Arrays.asList(
                new Sample("ordinary", "set %s.value %d.count %b.flag %m.textview", 4, 4),
                new Sample("literal-all", "show \\%s \\%d \\%b \\%m", 0, 0),
                new Sample("literal-prefix", "show x\\%s.y x\\%d.y x\\%b.y x\\%m", 0, 0),
                new Sample("literal-and-real", "show \\%s.fake %s.real", 1, 1),
                new Sample("mixed-literal-type", "show \\%d.fake %s.real", 1, 1),
                new Sample("escape-inside-field", "show %s.default\\%d.fake", 1, 1),
                new Sample("ordinary-label-percent", "show 100% percent %", 0, 0),
                new Sample("empty", "", 0, 0)));
        for (int n = 0; n <= 6; n++) {
            for (String type : Arrays.asList("s.value", "m.textview")) {
                result.add(new Sample("slashes-" + n + "-" + type,
                        "show " + "\\".repeat(n) + "%" + type, n % 2 == 0 ? 1 : 0, n % 2 == 0 ? 1 : 0));
            }
        }
        return result;
    }

    @Test public void countsOnlyRealFieldsIncludingBackslashParity() throws Exception {
        withCreator("add", (activity, palette, initial) -> {
            for (Sample sample : samples()) {
                input(activity, sample.spec);
                BlockView preview = preview(activity);
                int fields = fieldCount(preview);
                assertEquals(sample.name, sample.fields, fields);
                assertEquals(sample.name, sample.fields, ComponentTypeMapper.getParamClassInfoList(sample.spec).size());
                assertEquals(sample.name, sample.loaderCount, BlockLoader.countSpecParams(sample.spec));
                Map<String, Object> row = row(sample.name, sample.spec);
                row.put("tokens", FormatUtil.parseBlockSpec(sample.spec));
                row.put("actualFields", fields);
                row.put("loaderCount", BlockLoader.countSpecParams(sample.spec));
                status("block_spec_count_row", GSON.toJson(row));
            }
            assertEquals(initial, read(palette));
        });
        assertEquals(0, BlockLoader.countSpecParams(null));
    }

    @Test public void savesAddSpecsWithLiteralPercents() throws Exception { auditSave("add"); }
    @Test public void savesInsertSpecsWithLiteralPercents() throws Exception { auditSave("insert"); }
    @Test public void savesEditSpecsWithLiteralPercents() throws Exception { auditSave("edit"); }

    @Test public void reportsTheRealMalformedMenuPositionAndRecovers() throws Exception {
        withCreator("add", (activity, palette, initial) -> {
            String prefix = "literal \\%m then ";
            input(activity, prefix + "%m");
            View error = ((ViewGroup) activity.findViewById(R.id.block_area)).getChildAt(0);
            assertTrue(error instanceof android.widget.TextView);
            assertEquals("Error: '%m' must be followed by '.param' at position " + (prefix.length() + 2),
                    ((android.widget.TextView) error).getText().toString());
            assertEquals(initial, read(palette));
            input(activity, prefix + "%m.textview");
            assertEquals(1, fieldCount(preview(activity)));
            assertTrue(activity.findViewById(R.id.save).performClick());
            assertSaved("add", palette, prefix + "%m.textview");
        });
    }

    private void auditSave(String mode) throws Exception {
        List<Sample> cases = Arrays.asList(
                new Sample("ordinary-menu", "show %m.textview", 1, 1),
                new Sample("escaped-bare-menu", "show \\%m", 0, 0),
                new Sample("escaped-named-menu", "show \\%m.textview", 0, 0),
                new Sample("double-slash-bare-menu", "show \\\\%m", -1, 1),
                new Sample("triple-slash-bare-menu", "show \\\\\\%m", 0, 0),
                new Sample("escaped-string-and-real", "show \\%s.fake %s.real", 1, 1),
                new Sample("double-slash-named-menu", "show \\\\%m.textview", 1, 1),
                new Sample("ordinary-bare-menu", "show %m", -1, 1));
        for (Sample sample : cases) {
            withCreator(mode, (activity, palette, initial) -> {
                input(activity, sample.spec);
                if (sample.fields < 0) {
                    assertFalse(((ViewGroup) activity.findViewById(R.id.block_area)).getChildAt(0) instanceof BlockView);
                } else {
                    assertEquals(sample.name, sample.fields, fieldCount(preview(activity)));
                }
                boolean rejected = sample.fields < 0;
                assertEquals(sample.name, sample.name.equals("double-slash-bare-menu") || sample.name.equals("ordinary-bare-menu"), rejected);
                assertTrue(activity.findViewById(R.id.save).isEnabled());
                assertTrue(activity.findViewById(R.id.save).performClick());
                if (rejected) {
                    assertEquals(sample.name, initial, read(palette));
                    assertFalse(activity.isFinishing());
                } else {
                    assertSaved(mode, palette, sample.spec);
                    assertTrue(activity.isFinishing());
                }
                Map<String, Object> row = row(sample.name, sample.spec);
                row.put("mode", mode);
                row.put("actualFields", sample.fields);
                row.put("loaderCount", BlockLoader.countSpecParams(sample.spec));
                row.put("saveRejected", rejected);
                row.put("literalWronglyRejected", rejected && sample.fields == 0);
                row.put("fileUnchangedWhenRejected", rejected && initial.equals(read(palette)));
                status("block_spec_save_row", GSON.toJson(row));
                if (rejected) {
                    input(activity, "recovered %s.real");
                    assertTrue(activity.findViewById(R.id.save).performClick());
                    assertSaved(mode, palette, "recovered %s.real");
                    assertTrue(activity.isFinishing());
                }
            });
        }
    }

    @Test public void warnsForActualTemplateArgumentShortages() throws Exception {
        BlockLoader.getBlockInfo("F29.initialize." + UUID.randomUUID());
        Field mapField = BlockLoader.class.getDeclaredField("blocksByName");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked") HashMap<String, ExtraBlockInfo> original =
                (HashMap<String, ExtraBlockInfo>) mapField.get(null);
        HashMap<String, ExtraBlockInfo> isolated = new HashMap<>(original);
        mapField.set(null, isolated);
        try {
            template(isolated, "ordinary-too-many", "call %s.real", 1, 1, 4, true, false);
            template(isolated, "literal-too-many", "call \\%s.fake %s.real", 1, 1, 4, true, false);
            template(isolated, "literal-valid-template", "call \\%s.fake %s.real", 1, 1, 3, false, true);
            template(isolated, "literal-only-too-many", "call \\%s \\%d \\%b", 0, 0, 3, true, false);
            for (int n = 0; n <= 4; n++) {
                template(isolated, "template-slashes-" + n, "call " + "\\".repeat(n) + "%s.real",
                        n % 2 == 0 ? 1 : 0, n % 2 == 0 ? 1 : 0, 3, n % 2 != 0, n % 2 == 0);
            }
        } finally {
            mapField.set(null, original);
            assertSame(original, mapField.get(null));
            status("block_spec_loader_cache_restored", "true");
        }
    }

    private void template(HashMap<String, ExtraBlockInfo> isolated, String label, String spec,
                          int fields, int loaderCount, int placeholders, boolean expectedWarning,
                          boolean expectedSuccess) throws Exception {
        ExtraBlockInfo info = new ExtraBlockInfo();
        info.setName("F29Audit" + UUID.randomUUID().toString().replace("-", ""));
        info.setSpec(spec);
        info.setCode("System.out.println(%s);" + "\n%s".repeat(placeholders - 1));
        assertEquals(loaderCount, BlockLoader.countSpecParams(spec));
        assertEquals(placeholders, BlockLoader.countCodeArguments(info.getCode()));
        assertEquals(fields, ComponentTypeMapper.getParamClassInfoList(spec).size());
        Method validate = BlockLoader.class.getDeclaredMethod("validateBlock", ExtraBlockInfo.class, String.class);
        validate.setAccessible(true);
        Field enabled = LogUtil.class.getDeclaredField("loggingEnabled");
        enabled.setAccessible(true);
        boolean originalEnabled = enabled.getBoolean(null);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(captured, true, "UTF-8")) {
            enabled.setBoolean(null, true);
            System.setOut(capture);
            validate.invoke(null, info, "F29 isolated audit");
        } finally {
            System.setOut(originalOut);
            enabled.setBoolean(null, originalEnabled);
        }
        String warning = captured.toString("UTF-8");
        boolean warned = warning.contains("Block '" + info.getName() + "'");
        assertEquals(label, expectedWarning, warned);
        isolated.put(info.getName(), info);
        BlockBean bean = new BlockBean();
        bean.id = "1"; bean.opCode = info.getName(); bean.spec = spec; bean.type = " "; bean.typeName = ""; bean.nextBlock = -1;
        for (int i = 0; i < fields; i++) bean.parameters.add("value");
        String code = new BlockInterpreter("F29AuditActivity", null,
                new ArrayList<>(List.of(bean)), false).interpretBlocks();
        if (expectedSuccess) {
            assertEquals(label, "System.out.println(\"value\");\n\n", code);
        } else {
            assertTrue(label + ": " + code, code.startsWith("/* Failed to resolve Custom Block's code:"));
            assertTrue(label + ": " + code, code.contains("MissingFormatArgumentException"));
        }
        Map<String, Object> row = row(label, spec);
        row.put("actualFields", fields); row.put("loaderCount", loaderCount);
        row.put("actualAvailable", fields + 2); row.put("loaderAvailable", loaderCount + 2);
        row.put("placeholders", placeholders); row.put("actualWarning", warned);
        row.put("warning", warning); row.put("templateSucceeded", expectedSuccess); row.put("generatedCode", code);
        status("block_spec_template_row", GSON.toJson(row));
        isolated.remove(info.getName());
    }

    private static void input(BlocksManagerCreatorActivity activity, String spec) {
        ((EditText) activity.findViewById(R.id.spec)).setText(spec);
    }

    private static BlockView preview(BlocksManagerCreatorActivity activity) {
        ViewGroup area = activity.findViewById(R.id.block_area);
        assertEquals(1, area.getChildCount());
        assertTrue(area.getChildAt(0).getClass().getName(), area.getChildAt(0) instanceof BlockView);
        return (BlockView) area.getChildAt(0);
    }

    private static int fieldCount(BlockView preview) {
        int count = 0;
        for (View part : preview.specViews) if (part instanceof FieldBlockView) count++;
        return count;
    }

    private static Map<String, Object> row(String name, String spec) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name); row.put("spec", spec); return row;
    }

    private static Map<String, Object> seed() {
        Map<String, Object> seed = row("F29Existing", "existing %s.value");
        seed.put("type", " "); seed.put("typeName", ""); seed.put("color", COLOR);
        seed.put("code", "seed();"); seed.put("palette", "0"); seed.put("imports", "");
        return seed;
    }

    private static void assertSaved(String mode, File palette, String spec) throws Exception {
        List<Map<String, Object>> entries = GSON.fromJson(read(palette),
                new TypeToken<List<Map<String, Object>>>() {}.getType());
        assertEquals(mode, mode.equals("insert") ? 2 : 1, entries.size());
        Map<String, Object> saved = entries.get(0);
        assertEquals("F29Audit", saved.get("name"));
        assertEquals(spec, saved.get("spec"));
        assertEquals("audit();", saved.get("code"));
        assertEquals("0", saved.get("palette"));
        assertEquals(COLOR, saved.get("color"));
        if (mode.equals("insert")) assertEquals(seed(), entries.get(1));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void withCreator(String mode, CreatorCheck check) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        File fixture = new File(context.getCacheDir(), "BlockSpecSave." + UUID.randomUUID());
        assertTrue(fixture.mkdir());
        status("block_spec_owned_fixture", fixture.getName());
        File palette = new File(fixture, "palette.json");
        BlocksManagerCreatorActivity activity = null;
        try {
            String initial = mode.equals("add") ? "[]" : GSON.toJson(List.of(seed()));
            Files.write(palette.toPath(), initial.getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(context, BlocksManagerCreatorActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("mode", mode).putExtra("path", palette.getAbsolutePath())
                    .putExtra("color", COLOR).putExtra("pallet", "0").putExtra("pos", "0");
            activity = (BlocksManagerCreatorActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            BlocksManagerCreatorActivity target = activity;
            AtomicReference<Throwable> failure = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                try {
                    ((EditText) target.findViewById(R.id.name)).setText("F29Audit");
                    ((EditText) target.findViewById(R.id.code)).setText("audit();");
                    check.run(target, palette, initial);
                } catch (Throwable error) { failure.set(error); }
            });
            if (failure.get() instanceof Error) throw (Error) failure.get();
            if (failure.get() instanceof RuntimeException) throw (RuntimeException) failure.get();
            if (failure.get() != null) throw new AssertionError(failure.get());
        } finally {
            if (activity != null) {
                BlocksManagerCreatorActivity target = activity;
                instrumentation.runOnMainSync(target::finish);
                instrumentation.waitForIdleSync();
            }
            if (palette.exists()) assertTrue(palette.delete());
            assertTrue(fixture.delete());
            status("block_spec_cleaned_fixture", fixture.getName());
        }
    }

    private static void status(String key, String value) {
        Bundle data = new Bundle(); data.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, data);
    }

    private interface CreatorCheck {
        void run(BlocksManagerCreatorActivity activity, File palette, String initial) throws Exception;
    }
}
