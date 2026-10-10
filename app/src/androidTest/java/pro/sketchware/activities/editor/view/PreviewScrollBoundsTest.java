package pro.sketchware.activities.editor.view;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.editor.view.item.ItemVerticalScrollView;

@RunWith(AndroidJUnit4.class)
public class PreviewScrollBoundsTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[][] PADDING_PIXELS = {
            {3, 5, 2, 21}, {3, 5, 21, 2}, {7, 11, 13, 29}, {0, 0, 0, 0},
            {8, 8, 8, 8}, {5, 11, -7, 29}, {5, 11, 29, -7}, {3, 5, 0, 21}
    };

    @Test public void verticalGestureLimitsMatchNativeScrollView() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Context context = context(dpi);
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (int[] padding : PADDING_PIXELS) {
                        ItemVerticalScrollView preview = new ItemVerticalScrollView(context);
                        ScrollView oracle = new ScrollView(context);
                        oracle.setFillViewport(true);
                        // The viewport is 160dp; dpi is its exact pixel size in this configuration.
                        populate(preview, dpi, padding, false);
                        populate(oracle, dpi, padding, false);
                        layout(preview, dpi, direction);
                        layout(oracle, dpi, direction);
                        assertEquals(oracle.getChildAt(0).getHeight(), preview.getChildAt(0).getHeight());
                        oracle.scrollTo(0, oracle.getChildAt(0).getHeight() + oracle.getHeight());
                        preview.setScrollEnabled(true);
                        swipe(preview, false, true);
                        assertEquals("dpi=" + dpi + " direction=" + direction + " right=" + padding[2]
                                + " bottom=" + padding[3], oracle.getScrollY(), preview.getScrollY());
                        assertEquals(0, preview.getScrollX());
                        swipe(preview, false, false);
                        assertEquals(0, preview.getScrollY());
                        preview.setScrollEnabled(false);
                        swipe(preview, false, true);
                        assertEquals("Disabled vertical container must not scroll", 0, preview.getScrollY());
                        preview.setScrollEnabled(true);
                        swipe(preview, false, true);
                        preview.removeView(preview.getChildAt(0));
                        assertEquals("Removing the direct child resets scrolling", 0, preview.getScrollY());
                        swipe(preview, false, true);
                        assertEquals("Empty vertical container must not scroll", 0, preview.getScrollY());
                        checks++;
                    }
                }
            }
            status("vertical_scroll_bounds_checks", checks);
        });
    }

    @Test public void horizontalLtrGestureLimitsRemainUnchanged() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Context context = context(dpi);
                for (int[] padding : PADDING_PIXELS) {
                    ItemHorizontalScrollView preview = new ItemHorizontalScrollView(context);
                    HorizontalScrollView oracle = new HorizontalScrollView(context);
                    oracle.setFillViewport(true);
                    populate(preview, dpi, padding, true);
                    populate(oracle, dpi, padding, true);
                    layout(preview, dpi, View.LAYOUT_DIRECTION_LTR);
                    layout(oracle, dpi, View.LAYOUT_DIRECTION_LTR);
                    assertEquals(oracle.getChildAt(0).getWidth(), preview.getChildAt(0).getWidth());
                    oracle.scrollTo(oracle.getChildAt(0).getWidth() + oracle.getWidth(), 0);
                    preview.setScrollEnabled(true);
                    swipe(preview, true, true);
                    assertEquals(oracle.getScrollX(), preview.getScrollX());
                    swipe(preview, true, false);
                    assertEquals(0, preview.getScrollX());
                    preview.setScrollEnabled(false);
                    swipe(preview, true, true);
                    assertEquals("Disabled horizontal container must not scroll", 0, preview.getScrollX());
                    preview.setScrollEnabled(true);
                    swipe(preview, true, true);
                    preview.removeView(preview.getChildAt(0));
                    assertEquals(0, preview.getScrollX());
                    swipe(preview, true, true);
                    assertEquals("Empty horizontal container must not scroll", 0, preview.getScrollX());
                    checks++;
                }
            }
            status("horizontal_scroll_control_checks", checks);
        });
    }

    private static void populate(ViewGroup scroll, int dpi, int[] padding, boolean horizontal) {
        if (scroll instanceof ItemView item) item.setPaddingPixels(padding[0], padding[1], padding[2], padding[3]);
        else scroll.setPadding(padding[0], padding[1], padding[2], padding[3]);
        LinearLayout child = new LinearLayout(scroll.getContext());
        child.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        for (int i = 0; i < 3; i++) {
            child.addView(new View(scroll.getContext()), new LinearLayout.LayoutParams(
                    horizontal ? dpi : 48, horizontal ? 48 : dpi));
        }
        scroll.addView(child, new FrameLayout.LayoutParams(horizontal ? ViewGroup.LayoutParams.WRAP_CONTENT
                : ViewGroup.LayoutParams.MATCH_PARENT, horizontal ? ViewGroup.LayoutParams.MATCH_PARENT
                : ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static void swipe(ViewGroup scroll, boolean horizontal, boolean forward) {
        float low = 1;
        float high = (horizontal ? scroll.getWidth() : scroll.getHeight()) - 1;
        for (int i = 0; i < 8; i++) {
            long time = SystemClock.uptimeMillis();
            float start = forward ? high : low;
            float end = forward ? low : high;
            int step = 0;
            for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
                float position = action == MotionEvent.ACTION_DOWN ? start : end;
                MotionEvent event = MotionEvent.obtain(time, time + step++, action,
                        horizontal ? position : 20, horizontal ? 20 : position, 0);
                try {
                    assertFalse("Editor containers continue delivering touches to children", scroll.onInterceptTouchEvent(event));
                } finally {
                    event.recycle();
                }
            }
        }
    }

    private static void layout(View view, int size, int direction) {
        view.setLayoutDirection(direction);
        view.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, size, size);
    }

    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = dpi;
        return new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
    }

    private static void status(String name, int value) {
        Bundle bundle = new Bundle();
        bundle.putInt(name, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static void onMain(Runnable check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ApplicationInfo info = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationInfo();
            int flags = info.flags;
            info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
            try { check.run(); } catch (Throwable failure) { error.set(failure); }
            finally { info.flags = flags; }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
