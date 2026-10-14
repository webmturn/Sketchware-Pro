package pro.sketchware.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Reveals an image by clipping its natural size instead of scaling it on each frame. */
public class ExpandableImageLayout extends FrameLayout {
    private boolean expanded;
    private ValueAnimator heightAnimator;
    private Window dialogWindow;
    private DialogAnimation dialogAnimation;
    private ViewTreeObserver.OnPreDrawListener prepareDialog;

    public ExpandableImageLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDialog(@NonNull Dialog dialog) {
        dialogWindow = dialog.getWindow();
        if (dialogWindow == null) return;
        View decor = dialogWindow.getDecorView();
        prepareDialog = () -> {
            // Prepare before the first visible frame. Resizing an already visible window can
            // move its surface a frame later than its content on some devices.
            for (View parent = (View) getParent(); parent != decor && parent != null;
                    parent = parent.getParent() instanceof View view ? view : null) {
                if (parent.getVisibility() == GONE) return true;
            }
            dialogAnimation = DialogAnimation.start(dialogWindow, measureImageHeight());
            if (dialogAnimation == null) return true;
            decor.getViewTreeObserver().removeOnPreDrawListener(prepareDialog);
            prepareDialog = null;
            return false;
        };
        decor.getViewTreeObserver().addOnPreDrawListener(prepareDialog);
        // The surface can be larger than the visible background. Hit-test the
        // visible dialog so taps in that transparent area still count as outside touches.
        decor.setOnTouchListener((view, event) -> {
            Outline outline = new Outline();
            Rect bounds = new Rect();
            Drawable background = decor.getBackground();
            if (background == null) return false;
            background.getOutline(outline);
            if (!outline.getRect(bounds)) {
                background.getPadding(bounds);
                bounds.set(bounds.left, bounds.top, decor.getWidth() - bounds.right,
                        decor.getHeight() - bounds.bottom);
            }
            if (bounds.contains((int) event.getX(), (int) event.getY())) return false;
            MotionEvent outside = MotionEvent.obtain(event);
            if (event.getAction() == MotionEvent.ACTION_UP) outside.setAction(MotionEvent.ACTION_OUTSIDE);
            view.performClick();
            boolean handled = dialog.onTouchEvent(outside);
            outside.recycle();
            return handled;
        });
    }

    public void toggle() {
        int startHeight = getVisibility() == GONE ? 0 : getHeight();
        cancelAnimation();
        expanded = !expanded;
        int targetHeight = 0;
        if (expanded) targetHeight = measureImageHeight();
        setHeight(startHeight);
        setVisibility(VISIBLE);
        ValueAnimator animator = ValueAnimator.ofInt(startHeight, targetHeight);
        heightAnimator = animator;
        animator.setDuration(200);
        animator.addUpdateListener(value -> {
            if (heightAnimator == value) setHeight((int) value.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (heightAnimator != animation) return;
                heightAnimator = null;
                settle();
            }
        });
        animator.start();
    }

    public void collapseImmediately() {
        cancelAnimation();
        expanded = false;
        settle();
    }

    public boolean isExpanded() { return expanded; }

    private void cancelAnimation() {
        ValueAnimator previous = heightAnimator;
        heightAnimator = null;
        if (previous != null) previous.cancel();
    }

    private void setHeight(int height) {
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = height;
        setLayoutParams(params);
    }

    private int measureImageHeight() {
        int width = getWidth();
        if (width == 0 && getParent() instanceof View parent) {
            width = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        }
        measure(MeasureSpec.makeMeasureSpec(Math.max(0, width), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return getMeasuredHeight();
    }

    private void settle() {
        setVisibility(expanded ? VISIBLE : GONE);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Children keep their full height; only this clipping container changes height.
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        setMeasuredDimension(getMeasuredWidthAndState(), resolveSizeAndState(getMeasuredHeight(),
                heightMeasureSpec, getMeasuredState() << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (oldWidth > 0 && width != oldWidth && heightAnimator != null) {
            cancelAnimation();
            settle();
        }
    }

    @Override protected void onDetachedFromWindow() {
        cancelAnimation();
        settle();
        if (prepareDialog != null && dialogWindow != null) {
            dialogWindow.getDecorView().getViewTreeObserver().removeOnPreDrawListener(prepareDialog);
            prepareDialog = null;
        }
        if (dialogAnimation != null) {
            dialogAnimation.finish();
            dialogAnimation = null;
        }
        super.onDetachedFromWindow();
    }

    /** Resizes the visible dialog inside a stable surface, avoiding a WindowManager call per frame. */
    private static class DialogAnimation {
        private final Window window;
        private final View decor;
        private final View panel;
        private final Drawable background;
        private final int originalGravity;
        private final int originalHeight;
        private final View.OnLayoutChangeListener layoutListener;

        static DialogAnimation start(Window window, int extraHeight) {
            View decor = window.getDecorView();
            View panel = decor.findViewById(androidx.appcompat.R.id.parentPanel);
            if (decor.getHeight() == 0 || panel == null || decor.getBackground() == null
                    || !(panel.getLayoutParams() instanceof FrameLayout.LayoutParams)) return null;
            return new DialogAnimation(window, decor, panel, extraHeight);
        }

        private DialogAnimation(Window window, View decor, View panel, int extraHeight) {
            this.window = window;
            this.decor = decor;
            this.panel = panel;
            background = decor.getBackground();
            originalHeight = window.getAttributes().height;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) panel.getLayoutParams();
            originalGravity = params.gravity;
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            panel.setLayoutParams(params);
            Rect available = new Rect();
            decor.getWindowVisibleDisplayFrame(available);
            int maxHeight = decor.getHeight() + extraHeight;
            if (available.height() > 0) maxHeight = Math.min(maxHeight, available.height());
            // A fixed width also avoids measuring the form at several candidate dialog widths.
            int width = window.getAttributes().width == ViewGroup.LayoutParams.WRAP_CONTENT
                    ? decor.getWidth() : window.getAttributes().width;
            window.setLayout(width, maxHeight);
            window.setBackgroundDrawable(new DrawableWrapper(background) {
                private void fitBackground() {
                    Rect bounds = getBounds();
                    int height = panel.getHeight() + decor.getPaddingTop() + decor.getPaddingBottom();
                    int inset = Math.max(0, (bounds.height() - height) / 2);
                    background.setBounds(bounds.left, bounds.top + inset, bounds.right, bounds.bottom - inset);
                }

                @Override public void draw(@NonNull Canvas canvas) {
                    fitBackground();
                    background.draw(canvas);
                }

                @Override public void getOutline(@NonNull Outline outline) {
                    fitBackground();
                    background.getOutline(outline);
                }
            });
            layoutListener = (v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
                decor.invalidateOutline();
                decor.invalidate();
            };
            panel.addOnLayoutChangeListener(layoutListener);
        }

        void finish() {
            panel.removeOnLayoutChangeListener(layoutListener);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) panel.getLayoutParams();
            params.gravity = originalGravity;
            panel.setLayoutParams(params);
            window.setBackgroundDrawable(background);
            if (decor.isAttachedToWindow()) window.setLayout(window.getAttributes().width, originalHeight);
        }
    }
}
