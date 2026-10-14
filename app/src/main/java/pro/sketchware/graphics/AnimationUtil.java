package pro.sketchware.graphics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;

import pro.sketchware.R;

public class AnimationUtil {
    public static void rotate(View view, float rotation, int duration, int delay, Animator.AnimatorListener listener) {
        view.animate().setListener(listener).rotation(rotation)
                .setStartDelay(delay).setDuration(duration).start();
    }

    public static void rotate(View view, float rotation, Animator.AnimatorListener listener) {
        view.animate().setListener(listener).rotation(rotation).start();
    }

    public static void fadeInBackground(View view, int duration) {
        int color = Color.parseColor("#b2000000");
        ValueAnimator valueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), 0, color);
        valueAnimator.setDuration(duration);
        valueAnimator.addUpdateListener(animation -> {
            view.setBackgroundColor((int) animation.getAnimatedValue());
        });
        valueAnimator.start();
    }

    public static void bounceIn(View view, int duration, int startDelay, Animator.AnimatorListener listener) {
        view.setTranslationY((float) (-view.getMeasuredHeight() * 5));
        view.animate().translationY(0.0F)
                .setInterpolator(new BounceInterpolator())
                .setDuration(duration)
                .setStartDelay(startDelay)
                .setListener(null).start();
    }

    public static void collapseView(ViewGroup viewGroup, int duration, Animator.AnimatorListener listener) {
        animateHeight(viewGroup, false, duration, listener);
    }

    public static void fadeSlideIn(View view, int duration, int startDelay, Animator.AnimatorListener listener) {
        view.setAlpha(0.0F);
        view.setTranslationY(140.0F);
        view.animate().setListener(listener)
                .alpha(1.0F)
                .translationY(0.0F)
                .setStartDelay(startDelay)
                .setDuration(duration).start();
    }

    public static void expandView(ViewGroup viewGroup, int duration, Animator.AnimatorListener listener) {
        animateHeight(viewGroup, true, duration, listener);
    }

    public static void cancelHeightAnimation(ViewGroup viewGroup) {
        if (viewGroup.getTag(R.id.layout_height_animator) instanceof ValueAnimator animator) {
            viewGroup.setTag(R.id.layout_height_animator, null);
            animator.cancel();
        }
    }

    private static void animateHeight(ViewGroup viewGroup, boolean expand, int duration,
                                      Animator.AnimatorListener listener) {
        cancelHeightAnimation(viewGroup);
        int startHeight = viewGroup.getVisibility() == View.GONE ? 0 : viewGroup.getHeight();
        ViewGroup.LayoutParams params = viewGroup.getLayoutParams();
        int targetHeight = 0;
        if (expand) {
            int width = viewGroup.getWidth();
            if (width == 0 && viewGroup.getParent() instanceof View parent) {
                width = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
                if (params instanceof ViewGroup.MarginLayoutParams margins) {
                    width -= margins.leftMargin + margins.rightMargin;
                }
            }
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            viewGroup.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, width),
                            width > 0 ? View.MeasureSpec.EXACTLY : View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            targetHeight = viewGroup.getMeasuredHeight();
            viewGroup.setVisibility(View.VISIBLE);
        }
        params.height = startHeight;
        viewGroup.setLayoutParams(params);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(startHeight, targetHeight);
        viewGroup.setTag(R.id.layout_height_animator, valueAnimator);
        View.OnAttachStateChangeListener detachListener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { }
            @Override public void onViewDetachedFromWindow(View view) {
                if (viewGroup.getTag(R.id.layout_height_animator) == valueAnimator) cancelHeightAnimation(viewGroup);
            }
        };
        viewGroup.addOnAttachStateChangeListener(detachListener);
        valueAnimator.addUpdateListener(animation -> {
            if (viewGroup.getTag(R.id.layout_height_animator) != animation) return;
            ViewGroup.LayoutParams currentParams = viewGroup.getLayoutParams();
            currentParams.height = (int) animation.getAnimatedValue();
            viewGroup.setLayoutParams(currentParams);
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationStart(Animator animation) {
                if (listener != null) listener.onAnimationStart(animation);
            }
            @Override public void onAnimationCancel(Animator animation) {
                cancelled = true;
                if (listener != null) listener.onAnimationCancel(animation);
            }
            @Override public void onAnimationEnd(Animator animation) {
                viewGroup.removeOnAttachStateChangeListener(detachListener);
                if (cancelled || viewGroup.getTag(R.id.layout_height_animator) != animation) return;
                viewGroup.setTag(R.id.layout_height_animator, null);
                ViewGroup.LayoutParams currentParams = viewGroup.getLayoutParams();
                currentParams.height = expand ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
                viewGroup.setLayoutParams(currentParams);
                if (listener != null) listener.onAnimationEnd(animation);
            }
        });
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void slideInFromLeft(View view, int duration, int startDelay, Animator.AnimatorListener listener) {
        view.setTranslationX((float) (-view.getMeasuredWidth()));
        view.animate().translationX(0.0F).setStartDelay(startDelay).setDuration(duration).setListener(listener).start();
    }

    public static void slideInFromRight(View view, int duration, int startDelay, Animator.AnimatorListener listener) {
        view.setTranslationX((float) view.getMeasuredWidth());
        view.animate().translationX(0.0F)
                .setStartDelay(startDelay)
                .setDuration(duration)
                .setListener(listener)
                .start();
    }
}
