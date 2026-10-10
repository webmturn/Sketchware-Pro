package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
public class PreviewScrollRelayoutTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[][] PADDING_PIXELS = {
            {0, 0, 0, 0}, {8, 8, 8, 8}, {3, 5, 2, 21}, {3, 5, 21, -7}
    };

    @Test public void verticalShrinkingContentMatchesNativeScrollView() {
        onMain(() -> checkShrinking(false, new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}));
    }

    @Test public void horizontalLtrShrinkingContentMatchesNativeScrollView() {
        // RTL positioning of wide preview children is tracked as a separate issue.
        onMain(() -> checkShrinking(true, new int[]{View.LAYOUT_DIRECTION_LTR}));
    }

    private static void checkShrinking(boolean horizontal, int[] directions) {
        int checks = 0;
        for (int dpi : DENSITIES) {
            for (int direction : directions) {
                for (int[] padding : PADDING_PIXELS) {
                    for (int operation = 0; operation < 3; operation++) {
                        for (boolean enabled : new boolean[]{true, false}) {
                            Pair pair = new Pair(dpi, horizontal, direction, padding, dpi / 2);
                            pair.scrollToEnd();
                            assertTrue("Fixture must initially scroll", offset(pair.preview, horizontal) > 0);
                            enable(pair.preview, enabled);
                            shrink(pair.preview.getChildAt(0), operation, dpi);
                            shrink(pair.oracle.getChildAt(0), operation, dpi);
                            pair.layout(dpi);
                            pair.assertOffset("Content shrink: dpi=" + dpi + " direction=" + direction
                                    + " operation=" + operation + " enabled=" + enabled);
                            assertEquals("Short content fits the viewport", 0, offset(pair.preview, horizontal));
                            swipe(pair.preview, horizontal, true);
                            assertEquals(0, offset(pair.preview, horizontal));
                            checks++;
                        }
                    }
                }
            }
        }
        status(horizontal ? "horizontal_relayout_checks" : "vertical_relayout_checks", checks);
    }

    @Test public void validPositionsSurviveGrowthAndViewportChanges() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    for (int[] padding : PADDING_PIXELS) {
                        Pair pair = new Pair(dpi, horizontal, View.LAYOUT_DIRECTION_LTR, padding, dpi);
                        int position = dpi / 3;
                        scrollTo(pair.preview, horizontal, position);
                        scrollTo(pair.oracle, horizontal, position);
                        addLeaf((LinearLayout) pair.preview.getChildAt(0), horizontal, dpi);
                        addLeaf((LinearLayout) pair.oracle.getChildAt(0), horizontal, dpi);
                        pair.layout(dpi);
                        pair.assertOffset("Growth keeps a valid offset");
                        assertEquals(position, offset(pair.preview, horizontal));
                        pair.preview.requestLayout();
                        pair.oracle.requestLayout();
                        pair.layout(dpi);
                        pair.assertOffset("Unchanged relayout keeps a valid offset");
                        assertEquals(position, offset(pair.preview, horizontal));

                        pair.scrollToEnd();
                        for (ViewGroup scroll : new ViewGroup[]{pair.preview, pair.oracle}) {
                            ViewGroup child = (ViewGroup) scroll.getChildAt(0);
                            while (child.getChildCount() > 2) child.removeViewAt(child.getChildCount() - 1);
                        }
                        pair.layout(dpi);
                        pair.assertOffset("Partial shrink clamps to the remaining nonzero range");
                        assertTrue(offset(pair.preview, horizontal) > 0);
                        swipe(pair.preview, horizontal, false);
                        scrollTo(pair.oracle, horizontal, 0);
                        pair.assertOffset("Reverse gestures reach the start after shrink");

                        scrollTo(pair.preview, horizontal, position);
                        scrollTo(pair.oracle, horizontal, position);
                        pair.layout(dpi / 2);
                        pair.assertOffset("A smaller viewport keeps a valid offset");
                        assertEquals(position, offset(pair.preview, horizontal));
                        pair.layout(dpi * 5);
                        pair.assertOffset("A larger viewport clamps an invalid offset");
                        assertEquals(0, offset(pair.preview, horizontal));
                        checks++;
                    }
                }
            }
            status("scroll_growth_and_viewport_checks", checks);
        });
    }

    @Test public void emptyAndGoneDirectChildrenResetOnlyTheScrollAxis() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    for (int operation = 0; operation < 3; operation++) {
                        Pair pair = new Pair(dpi, horizontal, View.LAYOUT_DIRECTION_LTR, PADDING_PIXELS[0], dpi);
                        pair.scrollToEnd();
                        if (horizontal) pair.preview.setScrollY(7);
                        else pair.preview.setScrollX(7);
                        enable(pair.preview, false);
                        switch (operation) {
                            case 0 -> pair.preview.removeViewAt(0);
                            case 1 -> pair.preview.removeAllViews();
                            case 2 -> pair.preview.getChildAt(0).setVisibility(View.GONE);
                        }
                        pair.layout(dpi);
                        assertEquals("Empty or hidden direct child: axis=" + horizontal + " operation=" + operation,
                                0, offset(pair.preview, horizontal));
                        assertEquals("Relayout does not change the other axis", 7,
                                horizontal ? pair.preview.getScrollY() : pair.preview.getScrollX());
                        checks++;
                    }
                }
            }
            status("empty_or_gone_scroll_checks", checks);
        });
    }

    private static class Pair {
        final ViewGroup preview, oracle;
        final boolean horizontal;
        final int direction;

        Pair(int dpi, boolean horizontal, int direction, int[] padding, int leafSize) {
            this.horizontal = horizontal;
            this.direction = direction;
            Context context = context(dpi);
            preview = horizontal ? new ItemHorizontalScrollView(context) : new ItemVerticalScrollView(context);
            oracle = horizontal ? new HorizontalScrollView(context) : new ScrollView(context);
            if (oracle instanceof ScrollView vertical) vertical.setFillViewport(true);
            else ((HorizontalScrollView) oracle).setFillViewport(true);
            for (ViewGroup scroll : new ViewGroup[]{preview, oracle}) {
                if (scroll instanceof ItemView item) item.setPaddingPixels(padding[0], padding[1], padding[2], padding[3]);
                else scroll.setPadding(padding[0], padding[1], padding[2], padding[3]);
                LinearLayout child = new LinearLayout(context);
                child.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                for (int i = 0; i < 4; i++) addLeaf(child, horizontal, leafSize);
                scroll.addView(child, new FrameLayout.LayoutParams(horizontal ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : ViewGroup.LayoutParams.MATCH_PARENT, horizontal ? ViewGroup.LayoutParams.MATCH_PARENT
                        : ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            enable(preview, true);
            layout(dpi);
        }

        void layout(int size) {
            for (View view : new View[]{preview, oracle}) {
                view.setLayoutDirection(direction);
                view.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY));
                view.layout(0, 0, size, size);
            }
        }

        void scrollToEnd() {
            View child = oracle.getChildAt(0);
            scrollTo(oracle, horizontal, (horizontal ? child.getWidth() : child.getHeight())
                    + (horizontal ? oracle.getWidth() : oracle.getHeight()));
            swipe(preview, horizontal, true);
            assertOffset("Initial gesture range matches native");
        }

        void assertOffset(String message) {
            assertEquals(message, offset(oracle, horizontal), offset(preview, horizontal));
        }
    }

    private static void shrink(View view, int operation, int dpi) {
        LinearLayout child = (LinearLayout) view;
        for (int i = child.getChildCount() - 1; i >= (operation == 1 ? 0 : 1); i--) {
            View leaf = child.getChildAt(i);
            switch (operation) {
                case 0 -> child.removeViewAt(i);
                case 1 -> {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) leaf.getLayoutParams();
                    if (child.getOrientation() == LinearLayout.HORIZONTAL) params.width = dpi / 8;
                    else params.height = dpi / 8;
                    leaf.setLayoutParams(params);
                }
                case 2 -> leaf.setVisibility(View.GONE);
            }
        }
    }

    private static void addLeaf(LinearLayout child, boolean horizontal, int size) {
        child.addView(new View(child.getContext()), new LinearLayout.LayoutParams(horizontal ? size : 48,
                horizontal ? 48 : size));
    }

    private static void enable(ViewGroup scroll, boolean enabled) {
        if (scroll instanceof ItemVerticalScrollView vertical) vertical.setScrollEnabled(enabled);
        else ((ItemHorizontalScrollView) scroll).setScrollEnabled(enabled);
    }

    private static int offset(View view, boolean horizontal) { return horizontal ? view.getScrollX() : view.getScrollY(); }

    private static void scrollTo(View view, boolean horizontal, int position) {
        view.scrollTo(horizontal ? position : 0, horizontal ? 0 : position);
    }

    private static void swipe(ViewGroup scroll, boolean horizontal, boolean forward) {
        float low = 1, high = (horizontal ? scroll.getWidth() : scroll.getHeight()) - 1;
        for (int i = 0; i < 8; i++) {
            long time = SystemClock.uptimeMillis();
            int step = 0;
            for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
                float position = action == MotionEvent.ACTION_DOWN ? (forward ? high : low) : (forward ? low : high);
                MotionEvent event = MotionEvent.obtain(time, time + step++, action,
                        horizontal ? position : 20, horizontal ? 20 : position, 0);
                try { assertFalse(scroll.onInterceptTouchEvent(event)); }
                finally { event.recycle(); }
            }
        }
    }

    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = dpi;
        return new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
    }

    private static void status(String key, int value) {
        Bundle bundle = new Bundle(); bundle.putInt(key, value);
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
