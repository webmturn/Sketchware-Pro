package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemVerticalScrollView;

/** Native fillViewport is the measurement oracle; preview editing minimums are kept. */
@RunWith(AndroidJUnit4.class)
public class PreviewScrollMarginsTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[][] MARGINS_PIXELS = {
            {0, 0, 0, 0}, {5, 13, 7, 17}, {13, 3, 2, 5},
            {-5, -13, -7, -17}, {-5, 13, 7, -17}, {320, 260, 340, 290}
    };
    private static final int[] WIDTHS = {ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 80};

    @Test public void shortAndNestedChildrenMatchNativeFillViewport() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Context context = context(dpi, 28);
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (int widthMode : new int[]{View.MeasureSpec.EXACTLY, View.MeasureSpec.AT_MOST}) {
                        for (int heightMode : new int[]{View.MeasureSpec.EXACTLY, View.MeasureSpec.AT_MOST, View.MeasureSpec.UNSPECIFIED}) {
                            for (int m = 0; m < MARGINS_PIXELS.length; m++) {
                                for (int width : WIDTHS) {
                                    for (boolean nested : new boolean[]{false, true}) {
                                        Pair pair = new Pair(context, width, nested, 37, MARGINS_PIXELS[m], m == 4);
                                        pair.layout(dpi, direction, widthMode, heightMode);
                                        pair.assertLayout("Short dpi=" + dpi + " direction=" + direction + " margin=" + m
                                                + " widthMode=" + widthMode + " heightMode=" + heightMode + " width=" + width);
                                        checks++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            status("vertical_margin_short_checks", checks);
        });
    }

    @Test public void longChildrenRemainUnchanged() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Context context = context(dpi, 28);
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (int heightMode : new int[]{View.MeasureSpec.EXACTLY, View.MeasureSpec.AT_MOST, View.MeasureSpec.UNSPECIFIED}) {
                        for (int m = 0; m < MARGINS_PIXELS.length; m++) {
                            for (int width : WIDTHS) {
                                Pair pair = new Pair(context, width, false, dpi * 3, MARGINS_PIXELS[m], m == 4);
                                pair.layout(dpi, direction, View.MeasureSpec.EXACTLY, heightMode);
                                pair.assertLayout("Long dpi=" + dpi + " direction=" + direction + " margin=" + m);
                                checks++;
                            }
                        }
                    }
                }
            }
            status("vertical_margin_long_checks", checks);
        });
    }

    @Test public void marginPolicyMatchesNativeAcrossTargetSdkThreshold() {
        onMain(() -> {
            int checks = 0;
            for (int targetSdk : new int[]{22, 23, 28}) {
                int before = checks;
                for (int dpi : DENSITIES) {
                    Context context = context(dpi, targetSdk);
                    for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                        for (int m = 0; m < MARGINS_PIXELS.length; m++) {
                            for (int width : WIDTHS) {
                                Pair pair = new Pair(context, width, false, 37, MARGINS_PIXELS[m], m == 4);
                                pair.layout(dpi, direction, View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
                                pair.assertLayout("targetSdk=" + targetSdk + " dpi=" + dpi + " direction=" + direction + " margin=" + m);
                                checks++;
                            }
                        }
                    }
                }
                status("vertical_margin_target_" + targetSdk + "_checks", checks - before);
            }
            status("vertical_margin_target_sdk_checks", checks);
        });
    }

    @Test public void changedMarginsAndShortenedContentClearExtraScrollSpace() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (boolean nested : new boolean[]{false, true}) {
                        Pair pair = new Pair(context(dpi, 28), ViewGroup.LayoutParams.MATCH_PARENT, nested,
                                dpi * 3, MARGINS_PIXELS[0], false);
                        pair.layout(dpi, direction, View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
                        pair.preview.setScrollEnabled(true);
                        swipe(pair.preview);
                        pair.oracle.scrollTo(0, pair.oracle.getChildAt(0).getHeight() + dpi);
                        assertEquals(pair.oracle.getScrollY(), pair.preview.getScrollY());
                        pair.setMargins(MARGINS_PIXELS[1], false);
                        pair.setLeafHeight(37);
                        pair.layout(dpi, direction, View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
                        pair.assertLayout("Changed margins and shortened content");
                        assertEquals(0, pair.preview.getScrollY());
                        swipe(pair.preview);
                        assertEquals("Short content with positive margins has no extra scroll space", 0, pair.preview.getScrollY());
                        for (int m : new int[]{4, 5, 0}) {
                            pair.setMargins(MARGINS_PIXELS[m], m == 4);
                            pair.layout(dpi, direction, View.MeasureSpec.EXACTLY, View.MeasureSpec.EXACTLY);
                            pair.assertLayout("Reapplied margin=" + m);
                        }
                        checks++;
                    }
                }
            }
            status("vertical_margin_change_and_shrink_checks", checks);
        });
    }

    private static class Pair {
        final ItemVerticalScrollView preview;
        final ScrollView oracle;
        final View[] leaves = new View[2];
        final boolean nested;

        Pair(Context context, int width, boolean nested, int height, int[] margins, boolean relative) {
            this.nested = nested;
            preview = new ItemVerticalScrollView(context);
            oracle = new ScrollView(context);
            oracle.setFillViewport(true);
            // Minimum editing size is separate from margin consumption.
            oracle.setMinimumWidth(preview.getMinimumWidth());
            oracle.setMinimumHeight(preview.getMinimumHeight());
            int index = 0;
            for (ViewGroup scroll : new ViewGroup[]{preview, oracle}) {
                if (scroll == preview) preview.setPaddingPixels(8, 8, 8, 8);
                else scroll.setPadding(8, 8, 8, 8);
                LinearLayout child = new LinearLayout(context);
                child.setOrientation(LinearLayout.VERTICAL);
                LinearLayout content = child;
                if (nested) {
                    content = new LinearLayout(context);
                    content.setOrientation(LinearLayout.VERTICAL);
                    child.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                View leaf = new View(context);
                leaves[index++] = leaf;
                content.addView(leaf, new LinearLayout.LayoutParams(48, height));
                FrameLayout.LayoutParams params = margins(width, margins, relative);
                scroll.addView(child, params);
            }
        }

        void setMargins(int[] values, boolean relative) {
            for (ViewGroup scroll : new ViewGroup[]{preview, oracle}) {
                View child = scroll.getChildAt(0);
                child.setLayoutParams(margins(child.getLayoutParams().width, values, relative));
            }
        }

        void setLeafHeight(int height) {
            for (View leaf : leaves) {
                ViewGroup.LayoutParams params = leaf.getLayoutParams();
                params.height = height;
                leaf.setLayoutParams(params);
            }
        }

        void layout(int size, int direction, int widthMode, int heightMode) {
            for (View view : new View[]{preview, oracle}) {
                view.setLayoutDirection(direction);
                view.measure(View.MeasureSpec.makeMeasureSpec(size, widthMode), View.MeasureSpec.makeMeasureSpec(size, heightMode));
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            }
        }

        void assertLayout(String message) {
            assertArrayEquals(message + " container", size(oracle), size(preview));
            View expected = oracle.getChildAt(0), actual = preview.getChildAt(0);
            assertArrayEquals(message + " child", size(expected), size(actual));
            assertArrayEquals(message + " bounds", bounds(expected), bounds(actual));
            if (nested) assertArrayEquals(message + " nested child", size(((ViewGroup) expected).getChildAt(0)),
                    size(((ViewGroup) actual).getChildAt(0)));
        }
    }

    private static FrameLayout.LayoutParams margins(int width, int[] values, boolean relative) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(values[0], values[1], values[2], values[3]);
        if (relative) {
            params.setMarginStart(values[0]);
            params.setMarginEnd(values[2]);
        }
        return params;
    }

    private static int[] size(View view) { return new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()}; }
    private static int[] bounds(View view) { return new int[]{view.getLeft(), view.getTop(), view.getRight(), view.getBottom()}; }

    private static void swipe(ItemVerticalScrollView scroll) {
        for (int i = 0; i < 8; i++) {
            long time = SystemClock.uptimeMillis();
            int step = 0;
            for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
                MotionEvent event = MotionEvent.obtain(time, time + step++, action, 20,
                        action == MotionEvent.ACTION_DOWN ? scroll.getHeight() - 1 : 1, 0);
                try { assertFalse(scroll.onInterceptTouchEvent(event)); }
                finally { event.recycle(); }
            }
        }
    }

    private static Context context(int dpi, int targetSdk) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = dpi;
        Context themed = new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
        return new ContextWrapper(themed) {
            private final ApplicationInfo info = createInfo();
            private ApplicationInfo createInfo() {
                ApplicationInfo copy = new ApplicationInfo(themed.getApplicationInfo());
                copy.targetSdkVersion = targetSdk;
                copy.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
                return copy;
            }
            @Override public ApplicationInfo getApplicationInfo() { return info; }
        };
    }

    private static void status(String key, int value) {
        Bundle bundle = new Bundle(); bundle.putInt(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static void onMain(Runnable check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { check.run(); } catch (Throwable failure) { error.set(failure); }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
