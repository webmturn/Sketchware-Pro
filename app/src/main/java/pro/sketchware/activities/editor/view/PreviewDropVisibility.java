package pro.sketchware.activities.editor.view;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.editor.view.item.ItemVerticalScrollView;

/** Reveals a completed drop within the preview's own scroll containers. */
final class PreviewDropVisibility {
    private PreviewDropVisibility() {}

    static void reveal(ViewPane pane, View target) {
        if (!pane.isAttachedToWindow() || !target.isShown() || !isInsidePane(pane, target)) return;
        Rect drawingRect = new Rect();
        target.getDrawingRect(drawingRect);
        if (drawingRect.isEmpty()) return;
        RectF position = new RectF(drawingRect);
        View child = target;
        while (child != pane && child.getParent() instanceof ViewGroup parent) {
            // Keep fractional coordinates through ancestor scale/translation and nested scrolling.
            position.offset(-child.getScrollX(), -child.getScrollY());
            child.getMatrix().mapRect(position);
            position.offset(child.getLeft(), child.getTop());
            if (parent.getChildCount() > 0 && parent.getChildAt(0).getVisibility() != View.GONE) {
                View content = parent.getChildAt(0);
                if (parent instanceof ItemHorizontalScrollView) {
                    int min = Math.min(0, content.getLeft() - parent.getPaddingLeft());
                    int max = Math.max(0, content.getRight() + parent.getPaddingRight() - parent.getWidth());
                    int next = scrollPosition(position.left, position.right, parent.getScrollX(), parent.getWidth(),
                            parent.getClipToPadding() ? parent.getPaddingLeft() : 0,
                            parent.getClipToPadding() ? parent.getPaddingRight() : 0, min, max);
                    if (next != parent.getScrollX()) parent.scrollTo(next, parent.getScrollY());
                } else if (parent instanceof ItemVerticalScrollView) {
                    int max = Math.max(0, content.getBottom() + parent.getPaddingBottom() - parent.getHeight());
                    int next = scrollPosition(position.top, position.bottom, parent.getScrollY(), parent.getHeight(),
                            parent.getClipToPadding() ? parent.getPaddingTop() : 0,
                            parent.getClipToPadding() ? parent.getPaddingBottom() : 0, 0, max);
                    if (next != parent.getScrollY()) parent.scrollTo(parent.getScrollX(), next);
                }
            }
            // The next conversion uses the inner container's updated scroll offset.
            child = parent;
        }
    }

    private static boolean isInsidePane(ViewPane pane, View target) {
        View current = target;
        while (current.getParent() instanceof View parent) {
            if (parent == pane) return true;
            current = parent;
        }
        return false;
    }

    private static int scrollPosition(float start, float end, int current, int size,
                                      int startPadding, int endPadding, int min, int max) {
        int insetStart = Math.max(0, startPadding);
        int insetEnd = Math.max(0, endPadding);
        int viewportSize = size - insetStart - insetEnd;
        if (viewportSize <= 0) return current;
        int visibleStart = current + insetStart;
        int visibleEnd = current + size - insetEnd;
        int desired = current;
        if (end - start > viewportSize) {
            // A larger item needs a visible portion; keep an existing overlap to avoid jumping edges.
            if (end <= visibleStart) desired = (int) Math.ceil(end - size + insetEnd);
            else if (start >= visibleEnd) desired = (int) Math.floor(start - insetStart);
        } else if (start < visibleStart) {
            desired = (int) Math.floor(start - insetStart);
        } else if (end > visibleEnd) {
            desired = (int) Math.ceil(end - size + insetEnd);
        }
        return Math.max(min, Math.min(desired, max));
    }
}
