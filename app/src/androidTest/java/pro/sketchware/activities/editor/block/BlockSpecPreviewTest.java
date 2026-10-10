package pro.sketchware.activities.editor.block;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.block.view.FieldBlockView;

/** Exercises the creator's real TextWatcher without saving any block or project. */
@RunWith(AndroidJUnit4.class)
public class BlockSpecPreviewTest {
    @Test public void creatorShowsEscapedLabelsAndKeepsRealFields() throws Exception {
        withCreator(activity -> check(activity,
                "中文\\ 空格 \\%s.literal path\\\\end %s.value %b.flag %d.count %m.textview",
                new String[] {"中文 空格", "%s.literal", "path\\end"},
                new String[] {"%s.value", "%b.flag", "%d.count", "%m.textview"}));
    }

    @Test public void creatorAcceptsDanglingEscapeAndRecoversAfterEditing() throws Exception {
        withCreator(activity -> {
            check(activity, "end\\", new String[] {"end"}, new String[0]);
            check(activity, "\\", new String[] {""}, new String[0]);
            check(activity, "three\\\\\\", new String[] {"three\\"}, new String[0]);
            check(activity, "recovered %s.value", new String[] {"recovered"}, new String[] {"%s.value"});
        });
    }

    @Test public void creatorPreservesOrdinaryLabelsAndParameterOrder() throws Exception {
        withCreator(activity -> check(activity, "set %s.value to %d.count when %b.flag using %m.textview",
                new String[] {"set", "to", "when", "using"},
                new String[] {"%s.value", "%d.count", "%b.flag", "%m.textview"}));
    }

    @Test public void creatorKeepsMalformedMenuErrorAndCanRecover() throws Exception {
        withCreator(activity -> {
            EditText spec = activity.findViewById(R.id.spec);
            spec.setText("%m");
            ViewGroup area = activity.findViewById(R.id.block_area);
            assertEquals(1, area.getChildCount());
            assertTrue(area.getChildAt(0) instanceof TextView);
            assertTrue(((TextView) area.getChildAt(0)).getText().toString().contains("Error: '%m'"));
            check(activity, "ready %m.textview", new String[] {"ready"}, new String[] {"%m.textview"});
        });
    }

    private static void check(BlocksManagerCreatorActivity activity, String spec,
                              String[] expectedLabels, String[] expectedFields) {
        ((EditText) activity.findViewById(R.id.spec)).setText(spec);
        ViewGroup area = activity.findViewById(R.id.block_area);
        assertEquals(1, area.getChildCount());
        View preview = area.getChildAt(0);
        assertTrue("Expected a real BlockView after spec=" + spec + "; actual="
                + (preview instanceof TextView ? ((TextView) preview).getText() : preview.getClass().getName()),
                preview instanceof BlockView);
        BlockView block = (BlockView) preview;
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> fields = new ArrayList<>();
        for (int i = 0; i < block.specViews.size(); i++) {
            View part = block.specViews.get(i);
            if (part instanceof TextView) {
                labels.add(((TextView) part).getText().toString());
            } else {
                assertTrue(part instanceof FieldBlockView);
                fields.add(block.specTypes.get(i));
            }
        }
        assertEquals(Arrays.asList(expectedLabels), labels);
        assertEquals(Arrays.asList(expectedFields), fields);
        assertEquals(spec, block.spec);
    }

    private static void withCreator(PreviewCheck check) throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        File fixture = new File(context.getCacheDir(), "F28Spec." + UUID.randomUUID());
        assertTrue(fixture.mkdir());
        status("f28_owned_fixture", fixture.getName());
        File palette = new File(fixture, "palette.json");
        BlocksManagerCreatorActivity activity = null;
        try {
            Files.write(palette.toPath(), "[]".getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(context, BlocksManagerCreatorActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("mode", "add").putExtra("path", palette.getAbsolutePath())
                    .putExtra("color", "#8a55d7").putExtra("pallet", "0");
            activity = (BlocksManagerCreatorActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            BlocksManagerCreatorActivity target = activity;
            AtomicReference<Throwable> failure = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                try {
                    ((EditText) target.findViewById(R.id.name)).setText("F28Preview");
                    check.run(target);
                } catch (Throwable error) {
                    failure.set(error);
                }
            });
            if (failure.get() instanceof Error) throw (Error) failure.get();
            if (failure.get() instanceof RuntimeException) throw (RuntimeException) failure.get();
            if (failure.get() != null) throw new AssertionError(failure.get());
            assertEquals("[]", new String(Files.readAllBytes(palette.toPath()), StandardCharsets.UTF_8));
        } finally {
            if (activity != null) {
                BlocksManagerCreatorActivity target = activity;
                instrumentation.runOnMainSync(target::finish);
                instrumentation.waitForIdleSync();
            }
            assertEquals("[]", new String(Files.readAllBytes(palette.toPath()), StandardCharsets.UTF_8));
            assertTrue(palette.delete());
            assertTrue(fixture.delete());
            status("f28_cleaned_fixture", fixture.getName());
        }
    }

    private static void status(String key, String value) {
        Bundle data = new Bundle();
        data.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, data);
    }

    private interface PreviewCheck {
        void run(BlocksManagerCreatorActivity activity);
    }
}
