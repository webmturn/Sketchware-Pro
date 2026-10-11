package pro.sketchware.activities.editor.block;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import pro.sketchware.R;

/** Uses private cache palettes to exercise real creator watchers and saving. */
@RunWith(AndroidJUnit4.class)
public class CodeTemplateWarningTest {
    private static final Gson GSON = new Gson();

    @Test public void updatesFormatWarningsAndClearsThemAfterCorrection() throws Exception {
        withCreator("add", "", (activity, palette) -> {
            set(activity, R.id.spec, "call %s.value");
            for (String code : List.of("%", "%z", "%--s", "%#s", "%d", "%tY")) {
                set(activity, R.id.code, code); warning(activity, true);
                assertTrue(activity.findViewById(R.id.save).isEnabled());
            }
            for (String code : List.of("%s", "%%d", "%1$s|%<s", "%-8.2s", "plain", "")) {
                set(activity, R.id.code, code); warning(activity, false);
            }
        });
    }

    @Test public void recalculatesAvailableArgumentsWhenSpecChanges() throws Exception {
        withCreator("add", "", (activity, palette) -> {
            set(activity, R.id.spec, "call"); set(activity, R.id.code, "%3$s"); warning(activity, true);
            set(activity, R.id.spec, "call %s.value"); warning(activity, false);
            set(activity, R.id.spec, "call \\%s.literal"); warning(activity, true);
            set(activity, R.id.code, "%<s"); warning(activity, true);
            set(activity, R.id.code, "%s %<s"); warning(activity, false);
        });
    }

    @Test public void validatesAnExistingTemplateWhenOpeningEditMode() throws Exception {
        withCreator("edit", "%d", (activity, palette) -> {
            warning(activity, true);
            assertEquals("%d", ((EditText) activity.findViewById(R.id.code)).getText().toString());
            set(activity, R.id.code, "%s"); warning(activity, false);
        });
    }

    @Test public void keepsWarningsNonblockingAndPreservesSavedTemplatesInAllModes() throws Exception {
        for (String mode : List.of("add", "insert", "edit")) {
            withCreator(mode, "%d", (activity, palette) -> {
                set(activity, R.id.name, "F32Saved"); set(activity, R.id.spec, "call %s.value");
                set(activity, R.id.code, "%d"); warning(activity, true);
                assertTrue(activity.findViewById(R.id.save).isEnabled());
                assertTrue(activity.findViewById(R.id.save).performClick());
                assertTrue(activity.isFinishing());
                List<Map<String, Object>> saved = GSON.fromJson(read(palette), new TypeToken<List<Map<String, Object>>>() {}.getType());
                assertEquals(mode.equals("insert") ? 2 : 1, saved.size());
                assertEquals("F32Saved", saved.get(0).get("name"));
                assertEquals("call %s.value", saved.get(0).get("spec"));
                assertEquals("%d", saved.get(0).get("code"));
                if (mode.equals("insert")) assertEquals(seed("%d"), saved.get(1));
                status("f32_warned_save_mode", mode);
            });
        }
    }

    private static void warning(BlocksManagerCreatorActivity activity, boolean expected) {
        CharSequence helper = ((TextInputLayout) activity.findViewById(R.id.code_layout)).getHelperText();
        assertEquals("helper=" + helper, expected, helper != null && helper.length() > 0);
    }

    private static void set(BlocksManagerCreatorActivity activity, int id, String text) {
        ((EditText) activity.findViewById(id)).setText(text);
    }

    private static Map<String, Object> seed(String code) {
        Map<String, Object> seed = new HashMap<>(); seed.put("name", "F32Existing"); seed.put("type", " ");
        seed.put("typeName", ""); seed.put("spec", "call %s.value"); seed.put("color", "#8a55d7");
        seed.put("code", code); seed.put("palette", "0"); return seed;
    }

    private static void withCreator(String mode, String initialCode, Check check) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        File fixture = new File(context.getCacheDir(), "F32Template." + UUID.randomUUID());
        assertTrue(fixture.mkdir()); status("f32_owned_fixture", fixture.getName());
        File palette = new File(fixture, "palette.json");
        String initial = mode.equals("add") ? "[]" : GSON.toJson(List.of(seed(initialCode)));
        BlocksManagerCreatorActivity activity = null;
        try {
            Files.write(palette.toPath(), initial.getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(context, BlocksManagerCreatorActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("mode", mode).putExtra("path", palette.getAbsolutePath()).putExtra("color", "#8a55d7")
                    .putExtra("pallet", "0").putExtra("pos", "0");
            activity = (BlocksManagerCreatorActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            BlocksManagerCreatorActivity target = activity;
            AtomicReference<Throwable> failure = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                try { check.run(target, palette); } catch (Throwable error) { failure.set(error); }
            });
            if (failure.get() instanceof Error) throw (Error) failure.get();
            if (failure.get() != null) throw new AssertionError(failure.get());
            if (!activity.isFinishing()) assertEquals(initial, read(palette));
        } finally {
            if (activity != null) {
                BlocksManagerCreatorActivity target = activity;
                instrumentation.runOnMainSync(target::finish); instrumentation.waitForIdleSync();
            }
            assertTrue(palette.delete()); assertTrue(fixture.delete());
            status("f32_cleaned_fixture", fixture.getName());
        }
    }

    private static String read(File file) throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    private static void status(String key, String value) {
        Bundle data = new Bundle(); data.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, data);
    }
    private interface Check { void run(BlocksManagerCreatorActivity activity, File palette) throws Exception; }
}
