package pro.sketchware.lib.highlighter;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.view.View;
import android.widget.EditText;

import pro.sketchware.activities.editor.LogicEditorActivity;

import java.util.List;
import java.util.regex.Matcher;

/**
 * A Helper class used in {@link LogicEditorActivity}
 * to (currently) highlight add source directly blocks.
 */
public class SimpleHighlighter {
    private static final int MAX_HIGHLIGHT_LENGTH = 15000;
    private static final long DEBOUNCE_DELAY_MS = 150L;

    private final EditText mEditor;
    private final List<SyntaxScheme> syntaxList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable highlight = this::highlightNow;
    private long textRevision;

    public SimpleHighlighter(EditText editor) {
        mEditor = editor;
        syntaxList = SyntaxScheme.JAVA();
        init();
    }

    private void init() {
        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                textRevision++;
                schedule(DEBOUNCE_DELAY_MS);
            }
        });
        mEditor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { schedule(0); }
            @Override public void onViewDetachedFromWindow(View view) { handler.removeCallbacks(highlight); }
        });
        schedule(0);
    }

    private void schedule(long delay) {
        handler.removeCallbacks(highlight);
        if (mEditor.isAttachedToWindow()) handler.postDelayed(highlight, delay);
    }

    private void highlightNow() {
        if (!mEditor.isAttachedToWindow()) return;
        Editable text = mEditor.getText();
        if (text == null) return;
        long revision = textRevision;
        // These decorations are owned by this helper; pasted colors and IME spans belong to others.
        for (HighlightSpan span : text.getSpans(0, text.length(), HighlightSpan.class)) text.removeSpan(span);
        if (revision == textRevision && text.length() <= MAX_HIGHLIGHT_LENGTH) {
            createHighlightSpans(syntaxList, text, revision);
        }
    }

    private void createHighlightSpans(List<SyntaxScheme> syntaxList, Editable editable, long revision) {
        String content = editable.toString();
        for (SyntaxScheme scheme : syntaxList) {
            for (Matcher m = scheme.pattern.matcher(content); m.find(); ) {
                // A SpanWatcher can synchronously edit the text while decorations are being applied.
                if (revision != textRevision) return;
                editable.setSpan(new HighlightSpan(scheme.color), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /** Non-parcelable decorations must not become stale saved text or clipboard formatting. */
    private static final class HighlightSpan extends CharacterStyle implements UpdateAppearance, NoCopySpan {
        private final int color;
        HighlightSpan(int color) { this.color = color; }
        @Override public void updateDrawState(TextPaint paint) { paint.setColor(color); }
    }
}
