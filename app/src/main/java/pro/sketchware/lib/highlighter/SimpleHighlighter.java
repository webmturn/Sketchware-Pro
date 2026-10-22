package pro.sketchware.lib.highlighter;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Java decorations for the small code input fields (the full editor uses Sora). */
public class SimpleHighlighter {
    private static final long DEBOUNCE_DELAY_MS = 150L;
    private static final int FULL_RENDER_LENGTH = 2048;
    private static final int CONTEXT_CHARACTERS = 512;
    private static final int MAX_OPERATIONS_PER_BATCH = 256;
    private static final long BATCH_BUDGET_NANOS = 2_000_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor WORKER = createWorker();

    private final EditText editor;
    private final List<SyntaxScheme> syntax;
    private final Runnable highlight = this::startAnalysis;
    private final ViewTreeObserver.OnScrollChangedListener scrollListener = this::viewportChanged;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = this::viewportChanged;
    private ViewTreeObserver observer;
    private long revision;
    private boolean scheduled;
    private int requestedStart = -1;
    private int requestedEnd = -1;
    private AnalysisTask task;
    private ApplyTask application;
    private HighlightAnalysis cached;

    public SimpleHighlighter(EditText editor) {
        this.editor = editor;
        syntax = SyntaxScheme.JAVA();
        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                revision++;
                cached = null;
                cancelWork();
            }
            @Override public void afterTextChanged(Editable s) { schedule(DEBOUNCE_DELAY_MS); }
        });
        editor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { attach(); }
            @Override public void onViewDetachedFromWindow(View view) {
                cancelWork();
                cached = null;
                if (observer != null && observer.isAlive()) {
                    observer.removeOnScrollChangedListener(scrollListener);
                    observer.removeOnGlobalLayoutListener(layoutListener);
                }
                observer = null;
            }
        });
        if (editor.isAttachedToWindow()) attach();
    }

    private static ThreadPoolExecutor createWorker() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), runnable -> new Thread(() -> {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    runnable.run();
                }, "simple-highlighter"));
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private void attach() {
        observer = editor.getViewTreeObserver();
        observer.addOnScrollChangedListener(scrollListener);
        observer.addOnGlobalLayoutListener(layoutListener);
        schedule(0);
    }

    private void cancelWork() {
        MAIN.removeCallbacks(highlight);
        scheduled = false;
        if (task != null) {
            task.cancelled = true;
            WORKER.remove(task);
            task = null;
        }
        if (application != null) {
            editor.removeCallbacks(application);
            application = null;
        }
        requestedStart = requestedEnd = -1;
    }

    private void schedule(long delay) {
        MAIN.removeCallbacks(highlight);
        scheduled = editor.isAttachedToWindow();
        if (scheduled) MAIN.postDelayed(highlight, delay);
    }

    private void viewportChanged() {
        // Layout/scroll callbacks must not turn a typing debounce into an immediate full scan.
        if (scheduled || !editor.isAttachedToWindow()) return;
        int[] window = visibleWindow();
        if (window[0] != requestedStart || window[1] != requestedEnd) schedule(0);
    }

    private int[] visibleWindow() {
        int length = editor.length();
        if (length <= FULL_RENDER_LENGTH) return new int[]{0, length};
        Layout layout = editor.getLayout();
        Rect visible = new Rect();
        if (layout == null || !editor.getLocalVisibleRect(visible)) return new int[]{0, 0};
        // getLocalVisibleRect includes this view's scroll offset and clipping by its ancestors.
        int first = layout.getLineForVertical(visible.top - editor.getTotalPaddingTop());
        int last = layout.getLineForVertical(visible.bottom - editor.getTotalPaddingTop());
        int start = layout.getLineStart(first);
        int end = layout.getLineEnd(last);
        // A horizontally scrolling single line can contain a whole document.
        if (end - start > FULL_RENDER_LENGTH) {
            start = layout.getOffsetForHorizontal(first, visible.left - editor.getTotalPaddingLeft());
            end = layout.getOffsetForHorizontal(last, visible.right - editor.getTotalPaddingLeft());
            int lower = Math.min(start, end);
            end = Math.max(start, end);
            start = lower;
        }
        // Quantize the buffer so small scrolls do not replace otherwise identical decorations.
        start = Math.max(0, start / CONTEXT_CHARACTERS * CONTEXT_CHARACTERS - CONTEXT_CHARACTERS);
        end = Math.min(length, (end / CONTEXT_CHARACTERS + 2) * CONTEXT_CHARACTERS);
        return new int[]{start, end};
    }

    private void startAnalysis() {
        scheduled = false;
        if (!editor.isAttachedToWindow()) return;
        int[] window = visibleWindow();
        cancelWork();
        requestedStart = window[0];
        requestedEnd = window[1];
        HighlightAnalysis previous = cached;
        if (window[0] == window[1]) {
            // A wrap-content editor can be entirely below its parent's viewport.
            // Release old decorations now; take the first snapshot when it becomes visible.
            task = new AnalysisTask(this, revision, "", syntax, previous, window[0], window[1]);
            accept(task, previous, new int[0]);
            return;
        }
        String content = previous == null ? editor.getText().toString() : previous.content;
        task = new AnalysisTask(this, revision, content, syntax, previous, window[0], window[1]);
        WORKER.execute(task);
    }

    private void accept(AnalysisTask completed, HighlightAnalysis analysis, int[] ranges) {
        if (task != completed || completed.cancelled || completed.revision != revision
                || !editor.isAttachedToWindow()) return;
        cached = analysis;
        application = new ApplyTask(completed, editor.getText(), ranges);
        if (application.unchanged()) {
            application = null;
            return;
        }
        // Start the bounded first batch now; only remaining work needs another frame.
        application.run();
    }

    /** The worker/queued result never retains a View, Activity, or Editable. */
    private static final class AnalysisTask implements Runnable {
        final WeakReference<SimpleHighlighter> owner;
        final long revision;
        final String content;
        final List<SyntaxScheme> syntax;
        final HighlightAnalysis previous;
        final int start;
        final int end;
        volatile boolean cancelled;

        AnalysisTask(SimpleHighlighter owner, long revision, String content, List<SyntaxScheme> syntax,
                     HighlightAnalysis previous, int start, int end) {
            this.owner = new WeakReference<>(owner);
            this.revision = revision;
            this.content = content;
            this.syntax = syntax;
            this.previous = previous;
            this.start = start;
            this.end = end;
        }

        @Override public void run() {
            if (cancelled) return;
            HighlightAnalysis analysis = previous != null ? previous
                    : HighlightAnalysis.scan(content, syntax, () -> cancelled);
            if (analysis == null || cancelled) return;
            int[] ranges = analysis.window(start, end);
            MAIN.post(() -> {
                if (cancelled) return;
                SimpleHighlighter highlighter = owner.get();
                if (highlighter != null) highlighter.accept(this, analysis, ranges);
            });
        }
    }

    private final class ApplyTask implements Runnable {
        final AnalysisTask source;
        final Editable text;
        final HighlightSpan[] oldSpans;
        final int[] ranges;
        int removed;
        int added;

        ApplyTask(AnalysisTask source, Editable text, int[] ranges) {
            this.source = source;
            this.text = text;
            this.ranges = ranges;
            oldSpans = text.getSpans(0, text.length(), HighlightSpan.class);
        }

        private boolean current() {
            return application == this && task == source && !source.cancelled
                    && revision == source.revision && editor.isAttachedToWindow() && editor.getText() == text;
        }

        private boolean unchanged() {
            if (oldSpans.length != ranges.length / 3) return false;
            // Editable already moves existing spans when text is inserted/deleted. Reuse them if
            // the new analysis confirms every range/color in the same rule precedence order.
            for (int i = 0; i < oldSpans.length; i++) {
                HighlightSpan span = oldSpans[i];
                if (text.getSpanStart(span) != ranges[i * 3] || text.getSpanEnd(span) != ranges[i * 3 + 1]
                        || span.color != ranges[i * 3 + 2]) return false;
            }
            return true;
        }

        @Override public void run() {
            long deadline = SystemClock.elapsedRealtimeNanos() + BATCH_BUDGET_NANOS;
            int operations = 0;
            while (current() && operations++ < MAX_OPERATIONS_PER_BATCH) {
                if (removed < oldSpans.length) {
                    text.removeSpan(oldSpans[removed++]);
                } else if (added < ranges.length) {
                    int start = ranges[added++];
                    int end = ranges[added++];
                    int color = ranges[added++];
                    text.setSpan(new HighlightSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    application = null;
                    return;
                }
                // SpanWatcher callbacks can synchronously replace the text or detach this view.
                if (SystemClock.elapsedRealtimeNanos() >= deadline) break;
            }
            if (current()) editor.postOnAnimation(this);
        }
    }

    /** Non-parcelable decorations must not become saved text or clipboard formatting. */
    private static final class HighlightSpan extends CharacterStyle implements UpdateAppearance, NoCopySpan {
        private final int color;
        HighlightSpan(int color) { this.color = color; }
        @Override public void updateDrawState(TextPaint paint) { paint.setColor(color); }
    }
}
