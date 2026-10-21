package pro.sketchware.lib.highlighter;

import static org.junit.Assert.*;

import android.content.Intent;
import android.graphics.Color;
import android.os.Parcel;
import android.os.SystemClock;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpanWatcher;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.block.BlocksManagerCreatorActivity;
import pro.sketchware.databinding.ActivityBlocksManagerCreatorBinding;

@RunWith(AndroidJUnit4.class)
public class SimpleHighlighterTest {
    @Test public void rulesAreReadyOnTheFirstRequest() {
        // Run this method alone on a fresh process to exercise class initialization.
        List<SyntaxScheme> java = SyntaxScheme.JAVA();
        assertTrue("Primary rules must be initialized before compilation", java.get(0).pattern.matcher("out").find());
        assertTrue(java.get(1).pattern.matcher("return").find());
        assertTrue(java.get(8).pattern.matcher("/* first\nsecond */").find());
    }

    @Test(timeout = 30000) public void rapidTypingDefersWorkAndHighlightsOnlyTheLatestTextOnce() throws Exception {
        try (Editor editor = new Editor()) {
            AtomicInteger additions = new AtomicInteger();
            CountDownLatch pulse = new CountDownLatch(1);
            editor.main(() -> {
                Editable text = editor.input.getText();
                text.setSpan(new WatchingSpans() {
                    @Override public void onSpanAdded(Spannable text, Object span, int start, int end) {
                        if (isHighlight(span)) additions.incrementAndGet();
                    }
                }, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                for (int i = 0; i < 20; i++) text.replace(0, text.length(), "return " + i + ";");
                assertEquals("Typing callbacks must not run the regex pass", 0, editor.highlights());
                editor.input.post(() -> {
                    assertEquals("The next UI callback must run before deferred coloring", 0, editor.highlights());
                    pulse.countDown();
                });
            });
            assertTrue(pulse.await(5, TimeUnit.SECONDS));
            editor.awaitHighlights();
            editor.main(() -> {
                assertEquals("return 19;", editor.input.getText().toString());
                assertEquals("A burst should cause one coloring pass", editor.highlights(), additions.get());
            });
        }
    }

    @Test(timeout = 30000) public void ordinaryJavaKeepsItsColorsRangesAndSelection() throws Exception {
        try (Editor editor = new Editor()) {
            String body = "return 42; // comment\nString text = \"hello\";\ncall();";
            editor.main(() -> { editor.input.setText(body); editor.input.setSelection(4); });
            editor.awaitHighlights();
            editor.main(() -> {
                List<SyntaxScheme> rules = SyntaxScheme.JAVA();
                assertColor(editor.input.getText(), 0, 6, rules.get(1).color);
                assertColor(editor.input.getText(), body.indexOf("42"), body.indexOf("42") + 2, rules.get(2).color);
                assertColor(editor.input.getText(), body.indexOf("//"), body.indexOf('\n'), rules.get(8).color);
                assertColor(editor.input.getText(), body.indexOf('"'), body.lastIndexOf('"') + 1, rules.get(7).color);
                assertColor(editor.input.getText(), body.indexOf("call("), body.indexOf("call(") + 5, rules.get(4).color);
                assertEquals(body, editor.input.getText().toString());
                assertEquals(4, editor.input.getSelectionStart());
                assertEquals(4, editor.input.getSelectionEnd());
            });
        }
    }

    @Test(timeout = 30000) public void lengthLimitClearsOnlyOwnedSpansAndRecoversAfterShortening() throws Exception {
        try (Editor editor = new Editor()) {
            editor.main(() -> editor.input.setText("return 1;"));
            editor.awaitHighlights();
            ForegroundColorSpan pasted = new ForegroundColorSpan(Color.MAGENTA);
            UnderlineSpan underline = new UnderlineSpan();
            Object composing = new Object();
            editor.main(() -> {
                Editable text = editor.input.getText();
                text.setSpan(pasted, 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(underline, 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text.setSpan(composing, 7, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                editor.input.setSelection(3);
                text.append(" x".repeat(7500));
            });
            editor.afterDelay(() -> {
                Editable text = editor.input.getText();
                assertEquals(15009, text.length());
                assertEquals("Only caller-owned color should remain", 1, editor.highlights());
                assertEquals(0, text.getSpanStart(pasted));
                assertEquals(6, text.getSpanEnd(pasted));
                assertEquals(0, text.getSpanStart(underline));
                assertEquals(7, text.getSpanStart(composing));
                assertTrue((text.getSpanFlags(composing) & Spanned.SPAN_COMPOSING) != 0);
                assertEquals(3, editor.input.getSelectionStart());
                assertEquals(3, editor.input.getSelectionEnd());
                text.replace(0, text.length(), "return 2;");
            });
            editor.awaitColor(0, 6, SyntaxScheme.JAVA().get(1).color);
            editor.main(() -> assertEquals("return 2;", editor.input.getText().toString()));
        }
    }

    @Test(timeout = 30000) public void detachedEditorStopsColoringAndReattachmentUsesCurrentText() throws Exception {
        try (Editor editor = new Editor()) {
            editor.main(() -> {
                editor.input.setText("return 1;");
                editor.root.removeView(editor.input);
                editor.input.setText("return 2;");
            });
            editor.afterDelay(() -> {
                assertFalse(editor.input.isAttachedToWindow());
                assertEquals(0, editor.highlights());
                editor.root.addView(editor.input);
            });
            editor.awaitHighlights();
            editor.main(() -> assertEquals("return 2;", editor.input.getText().toString()));
        }
    }

    @Test(timeout = 30000) public void transientColorsAreNotSavedAsEditableContent() throws Exception {
        try (Editor editor = new Editor()) {
            editor.main(() -> editor.input.setText("return 1;"));
            editor.awaitHighlights();
            editor.main(() -> {
                ForegroundColorSpan pasted = new ForegroundColorSpan(Color.MAGENTA);
                editor.input.getText().setSpan(pasted, 0, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                Parcel parcel = Parcel.obtain();
                try {
                    TextUtils.writeToParcel(editor.input.getText(), parcel, 0);
                    parcel.setDataPosition(0);
                    CharSequence restored = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                    assertEquals("return 1;", restored.toString());
                    ForegroundColorSpan[] colors = ((Spanned) restored).getSpans(0, restored.length(), ForegroundColorSpan.class);
                    assertEquals("Only the pasted color is document content", 1, colors.length);
                    assertEquals(Color.MAGENTA, colors[0].getForegroundColor());
                } finally { parcel.recycle(); }
            });
        }
    }

    @Test(timeout = 30000) public void boundaryLengthHandlesLongIdentifiersAndMultilineComments() throws Exception {
        try (Editor editor = new Editor()) {
            String identifier = "return 1; " + "x".repeat(14990);
            assertEquals(15000, identifier.length());
            editor.main(() -> editor.input.setText(identifier));
            editor.awaitHighlights();
            String prefix = "/* first\r\n";
            String suffix = "\nlast */";
            String comment = prefix + "x".repeat(15000 - prefix.length() - suffix.length()) + suffix;
            editor.main(() -> editor.input.setText(comment));
            waitUntil(() -> {
                AtomicBoolean found = new AtomicBoolean();
                editor.main(() -> found.set(hasColor(editor.input.getText(), 0, 15000, SyntaxScheme.JAVA().get(8).color)));
                return found.get();
            });
            editor.main(() -> assertEquals(comment, editor.input.getText().toString()));
        }
    }

    @Test(timeout = 30000) public void synchronousSpanEditsCannotApplyRangesFromTheOldText() throws Exception {
        try (Editor editor = new Editor()) {
            AtomicBoolean changed = new AtomicBoolean();
            editor.main(() -> {
                Editable text = editor.input.getText();
                text.setSpan(new WatchingSpans() {
                    @Override public void onSpanAdded(Spannable current, Object span, int start, int end) {
                        if (isHighlight(span) && changed.compareAndSet(false, true)) {
                            editor.input.getText().replace(0, editor.input.length(), "return 2;");
                        }
                    }
                }, 0, 0, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                text.replace(0, 0, "return 123456789;\nString longer = \"text\";");
            });
            waitUntil(changed::get);
            editor.awaitHighlights();
            editor.afterDelay(() -> {
                assertEquals("return 2;", editor.input.getText().toString());
                for (CharacterStyle style : spans(editor.input.getText())) {
                    assertTrue(editor.input.getText().getSpanEnd(style) <= editor.input.length());
                }
                assertColor(editor.input.getText(), 0, 6, SyntaxScheme.JAVA().get(1).color);
            });
        }
    }

    @Test(timeout = 30000) public void blockCreatorSavesTheEntireCodeWhenHighlightingIsSuspended() throws Exception {
        Path root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir().toPath(), "highlighter-creator-");
        Path file = root.resolve("blocks.json");
        Files.write(file, "[]".getBytes(StandardCharsets.UTF_8));
        String code = "int value = 1;\n".repeat(1200) + "return value;";
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), BlocksManagerCreatorActivity.class)
                .putExtra("mode", "add").putExtra("path", file.toString()).putExtra("color", "#1976D2").putExtra("pallet", "1");
        try (ActivityScenario<BlocksManagerCreatorActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                var views = binding(activity);
                views.name.setText("highlight_fixture");
                views.spec.setText("saved code");
                views.code.setText(code);
            });
            SystemClock.sleep(350);
            scenario.onActivity(activity -> {
                assertEquals(code, binding(activity).code.getText().toString());
                assertEquals(0, spans(binding(activity).code.getText()).size());
                binding(activity).save.performClick();
            });
            waitUntil(() -> scenario.getState() == Lifecycle.State.DESTROYED);
            List<?> blocks = new Gson().fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), List.class);
            assertEquals(1, blocks.size());
            assertEquals(code, ((Map<?, ?>) blocks.get(0)).get("code"));
        } finally {
            assertEquals(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir().getCanonicalFile().toPath(), root.toRealPath().getParent());
            Files.delete(file);
            Files.delete(root);
        }
    }

    private static class WatchingSpans implements SpanWatcher, NoCopySpan {
        @Override public void onSpanAdded(Spannable text, Object span, int start, int end) {}
        @Override public void onSpanRemoved(Spannable text, Object span, int start, int end) {}
        @Override public void onSpanChanged(Spannable text, Object span, int oldStart, int oldEnd, int newStart, int newEnd) {}
    }

    private static final class Editor implements AutoCloseable {
        final ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class);
        EditText input;
        FrameLayout root;
        Editor() {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                root = new FrameLayout(activity);
                input = new AppCompatEditText(activity);
                root.addView(input, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300));
                activity.setContentView(root);
                new SimpleHighlighter(input);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
        void main(Runnable action) { scenario.onActivity(activity -> action.run()); }
        int highlights() { return spans(input.getText()).size(); }
        void awaitHighlights() throws Exception {
            waitUntil(() -> {
                AtomicBoolean ready = new AtomicBoolean();
                main(() -> ready.set(highlights() > 0));
                return ready.get();
            });
        }
        void awaitColor(int start, int end, int color) throws Exception {
            waitUntil(() -> {
                AtomicBoolean ready = new AtomicBoolean();
                main(() -> ready.set(hasColor(input.getText(), start, end, color)));
                return ready.get();
            });
        }
        void afterDelay(Runnable action) { SystemClock.sleep(350); main(action); }
        @Override public void close() { scenario.close(); }
    }

    private static boolean isHighlight(Object span) {
        return span instanceof ForegroundColorSpan || span.getClass().getName().equals(SimpleHighlighter.class.getName() + "$HighlightSpan");
    }
    private static List<CharacterStyle> spans(Spanned text) {
        ArrayList<CharacterStyle> result = new ArrayList<>();
        for (CharacterStyle style : text.getSpans(0, text.length(), CharacterStyle.class)) {
            if (isHighlight(style)) result.add(style);
        }
        return result;
    }
    private static boolean hasColor(Spanned text, int start, int end, int color) {
        for (CharacterStyle style : spans(text)) {
            TextPaint paint = new TextPaint();
            style.updateDrawState(paint);
            if (text.getSpanStart(style) == start && text.getSpanEnd(style) == end && paint.getColor() == color) return true;
        }
        return false;
    }
    private static void assertColor(Spanned text, int start, int end, int color) {
        assertTrue("Expected color span at " + start + ".." + end, hasColor(text, start, end, color));
    }
    private static ActivityBlocksManagerCreatorBinding binding(BlocksManagerCreatorActivity activity) {
        try {
            Field field = BlocksManagerCreatorActivity.class.getDeclaredField("binding");
            field.setAccessible(true);
            return (ActivityBlocksManagerCreatorBinding) field.get(activity);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private interface Check { boolean get() throws Exception; }
    private static void waitUntil(Check check) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        while (!check.get()) {
            if (SystemClock.uptimeMillis() > end) fail("Timed out waiting for highlighting");
            SystemClock.sleep(30);
        }
    }
}
