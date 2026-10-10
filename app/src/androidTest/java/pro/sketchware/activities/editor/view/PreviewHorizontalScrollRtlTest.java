package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.base.PermissionResultTestActivity;

@RunWith(AndroidJUnit4.class)
public class PreviewHorizontalScrollRtlTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] GRAVITIES = {-1, Gravity.LEFT, Gravity.RIGHT,
            Gravity.CENTER_HORIZONTAL, Gravity.START, Gravity.END};
    private static final int[][] PADDING = {{0, 0, 0, 0}, {8, 8, 8, 8},
            {3, 5, 2, 21}, {3, 5, 21, -7}};
    private static final int[][] MARGINS = {{0, 0}, {5, 7}, {13, 2}, {-5, -7}, {-5, 7}, {7, -5}};

    @Test public void rtlAndLtrEndpointsMatchNativeVisibleContentWithoutMargins() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (int[] padding : PADDING) {
                        for (boolean nested : new boolean[]{false, true}) {
                            Fixture fixture = new Fixture(dpi, direction, -1, MARGINS[0], false, padding, nested, 4);
                            HorizontalScrollView nativeView = new HorizontalScrollView(fixture.scroll.getContext());
                            nativeView.setFillViewport(true);
                            nativeView.setPadding(padding[0], padding[1], padding[2], padding[3]);
                            nativeView.addView(content(nativeView.getContext(), dpi, nested, 4),
                                    new FrameLayout.LayoutParams(-2, -1));
                            layout(nativeView, dpi, direction);
                            // Compare rendered content positions, since the coordinate origins differ in RTL.
                            swipe(fixture.scroll, false);
                            nativeView.scrollTo(0, 0);
                            assertEquals("Left endpoint dpi=" + dpi + " direction=" + direction,
                                    nativeView.getChildAt(0).getLeft() - nativeView.getScrollX(),
                                    fixture.subject.getLeft() - fixture.scroll.getScrollX());
                            fixture.assertLeftVisible();
                            int leftOffset = fixture.scroll.getScrollX();
                            swipe(fixture.scroll, false);
                            assertEquals("Further gestures stop at the left endpoint", leftOffset, fixture.scroll.getScrollX());
                            swipe(fixture.scroll, true);
                            nativeView.scrollTo(1000000, 0);
                            assertEquals("Right endpoint dpi=" + dpi + " direction=" + direction,
                                    nativeView.getChildAt(0).getRight() - nativeView.getScrollX(),
                                    fixture.subject.getRight() - fixture.scroll.getScrollX());
                            fixture.assertRightVisible();
                            int rightOffset = fixture.scroll.getScrollX();
                            swipe(fixture.scroll, true);
                            assertEquals("Further gestures stop at the right endpoint", rightOffset, fixture.scroll.getScrollX());
                            fixture.scroll.setScrollEnabled(false);
                            swipe(fixture.scroll, false);
                            assertEquals("Disabled scrolling preserves position", rightOffset, fixture.scroll.getScrollX());
                            assertEquals(0, fixture.scroll.getScrollY());
                            checks++;
                        }
                    }
                }
            }
            status("horizontal_native_endpoints_checks", checks);
        });
    }

    @Test public void marginsGravityAndNestedContentRemainReachableInBothDirections() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                    for (int gravity : GRAVITIES) {
                        for (int m = 0; m < MARGINS.length; m++) {
                            for (boolean nested : new boolean[]{false, true}) {
                                for (int count : new int[]{1, 4}) {
                                    Fixture fixture = new Fixture(dpi, direction, gravity, MARGINS[m], m == 4,
                                            PADDING[1], nested, count);
                                    swipe(fixture.scroll, false);
                                    // A short filled container reserves blank space after its content.
                                    if (count == 4 || direction == View.LAYOUT_DIRECTION_LTR) fixture.assertLeftVisible();
                                    swipe(fixture.scroll, true);
                                    if (count == 4 || direction == View.LAYOUT_DIRECTION_RTL) fixture.assertRightVisible();
                                    int width = fixture.subject.getMeasuredWidth();
                                    int height = fixture.subject.getMeasuredHeight();
                                    fixture.layout(dpi, direction);
                                    assertEquals("Scrolling does not change measured width", width, fixture.subject.getMeasuredWidth());
                                    assertEquals(height, fixture.subject.getMeasuredHeight());
                                    if (count == 4 || direction == View.LAYOUT_DIRECTION_RTL) fixture.assertRightVisible();
                                    else { swipe(fixture.scroll, false); fixture.assertLeftVisible(); }
                                    checks++;
                                }
                            }
                        }
                    }
                }
            }
            status("horizontal_gravity_margin_checks", checks);
        });
    }

    @Test public void negativeRtlPositionsSurviveGrowthAndClampAfterShrink() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (int[] padding : PADDING) {
                    for (boolean enabled : new boolean[]{false, true}) {
                        for (int operation = 0; operation < 4; operation++) {
                            Fixture fixture = new Fixture(dpi, View.LAYOUT_DIRECTION_RTL, -1, MARGINS[0], false,
                                    padding, false, 4);
                            swipe(fixture.scroll, false);
                            fixture.assertLeftVisible();
                            assertTrue("RTL wide content must have a negative scroll position", fixture.scroll.getScrollX() < 0);
                            int position = fixture.scroll.getScrollX() / 2;
                            fixture.scroll.scrollTo(position, 7);
                            addLeaf(fixture.subject, dpi / 2);
                            fixture.layout(dpi, View.LAYOUT_DIRECTION_RTL);
                            assertEquals("Growth preserves a valid negative position", position, fixture.scroll.getScrollX());
                            fixture.scroll.requestLayout();
                            fixture.layout(dpi, View.LAYOUT_DIRECTION_RTL);
                            assertEquals("Unchanged relayout preserves position", position, fixture.scroll.getScrollX());
                            swipe(fixture.scroll, false);
                            fixture.scroll.setScrollEnabled(enabled);
                            if (operation == 3) {
                                fixture.layout(dpi * 4, View.LAYOUT_DIRECTION_RTL);
                            } else {
                                for (int i = fixture.subject.getChildCount() - 1; i > 0; i--) {
                                    View leaf = fixture.subject.getChildAt(i);
                                    if (operation == 0) fixture.subject.removeViewAt(i);
                                    else if (operation == 1) {
                                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) leaf.getLayoutParams();
                                        params.width = dpi / 16;
                                        leaf.setLayoutParams(params);
                                    } else leaf.setVisibility(View.GONE);
                                }
                                fixture.layout(dpi, View.LAYOUT_DIRECTION_RTL);
                            }
                            assertEquals("Content now fits; invalid negative position is reset", 0, fixture.scroll.getScrollX());
                            assertEquals("Relayout preserves the other axis", 7, fixture.scroll.getScrollY());
                            fixture.assertLeftVisible();
                            fixture.assertRightVisible();
                            swipe(fixture.scroll, false);
                            assertEquals(0, fixture.scroll.getScrollX());
                            checks++;
                        }
                    }
                }
            }
            status("horizontal_rtl_relayout_checks", checks);
        });
    }

    @Test public void directionChangesAndEmptyChildrenKeepValidPositions() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                for (int gravity : GRAVITIES) {
                    for (int operation = 0; operation < 4; operation++) {
                        Fixture fixture = new Fixture(dpi, View.LAYOUT_DIRECTION_RTL, gravity, MARGINS[0], false,
                                PADDING[1], false, 4);
                        swipe(fixture.scroll, false);
                        fixture.assertLeftVisible();
                        fixture.layout(dpi, View.LAYOUT_DIRECTION_LTR);
                        swipe(fixture.scroll, true);
                        fixture.assertRightVisible();
                        fixture.layout(dpi, View.LAYOUT_DIRECTION_RTL);
                        swipe(fixture.scroll, false);
                        fixture.assertLeftVisible();
                        fixture.scroll.setScrollY(7);
                        fixture.scroll.setScrollEnabled(false);
                        switch (operation) {
                            case 0 -> fixture.scroll.removeView(fixture.subject);
                            case 1 -> fixture.scroll.removeViewAt(0);
                            case 2 -> fixture.scroll.removeAllViews();
                            case 3 -> fixture.subject.setVisibility(View.GONE);
                        }
                        fixture.layout(dpi, View.LAYOUT_DIRECTION_RTL);
                        assertEquals("Empty or hidden content resets the scroll axis", 0, fixture.scroll.getScrollX());
                        assertEquals(7, fixture.scroll.getScrollY());
                        swipe(fixture.scroll, false);
                        assertEquals(0, fixture.scroll.getScrollX());
                        checks++;
                    }
                }
            }
            status("horizontal_direction_empty_checks", checks);
        });
    }

    @Test public void focusScrollingCanReachEitherSideOfTheCoordinateOrigin() {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup host = activity.findViewById(android.R.id.content);
                try {
                    int checks = 0;
                    for (int dpi : DENSITIES) {
                        for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
                            for (int gravity : GRAVITIES) {
                                for (boolean left : new boolean[]{false, true}) {
                                    Fixture fixture = new Fixture(dpi, direction, gravity, MARGINS[0], false,
                                            PADDING[0], false, 3);
                                    host.removeAllViews();
                                    host.addView(fixture.scroll, new ViewGroup.LayoutParams(dpi, dpi));
                                    fixture.layout(dpi, direction);
                                    assertTrue(fixture.scroll.isAttachedToWindow());
                                    swipe(fixture.scroll, left);
                                    View target = extremeLeaf(fixture.subject, left);
                                    target.setFocusableInTouchMode(true);
                                    assertTrue(target.requestFocus());
                                    fixture.scroll.onSizeChanged(dpi, dpi, dpi, dpi);
                                    Rect rect = new Rect(); target.getDrawingRect(rect);
                                    fixture.scroll.offsetDescendantRectToMyCoords(target, rect);
                                    rect.offset(-fixture.scroll.getScrollX(), -fixture.scroll.getScrollY());
                                    assertTrue("Focused descendant reaches the viewport: direction=" + direction
                                                    + " gravity=" + gravity + " left=" + left,
                                            rect.left >= 0 && rect.right <= fixture.scroll.getWidth());
                                    checks++;
                                }
                            }
                        }
                    }
                    status("horizontal_focus_checks", checks);
                } finally { host.removeAllViews(); }
            });
        }
    }

    private static class Fixture {
        final ItemHorizontalScrollView scroll;
        final LinearLayout subject;
        final LinearLayout leaves;

        Fixture(int dpi, int direction, int gravity, int[] margins, boolean relative, int[] padding,
                boolean nested, int count) {
            Context context = context(dpi);
            scroll = new ItemHorizontalScrollView(context);
            scroll.setPaddingPixels(padding[0], padding[1], padding[2], padding[3]);
            subject = content(context, dpi, nested, count);
            leaves = nested ? (LinearLayout) subject.getChildAt(0) : subject;
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -1, gravity);
            params.leftMargin = margins[0]; params.rightMargin = margins[1];
            if (relative) { params.setMarginStart(margins[0]); params.setMarginEnd(margins[1]); }
            scroll.addView(subject, params);
            scroll.setScrollEnabled(true);
            layout(dpi, direction);
        }

        void layout(int size, int direction) { PreviewHorizontalScrollRtlTest.layout(scroll, size, direction); }

        void assertLeftVisible() { assertLeafVisible(extremeLeaf(leaves, true)); }
        void assertRightVisible() { assertLeafVisible(extremeLeaf(leaves, false)); }

        private void assertLeafVisible(View leaf) {
            Rect rect = new Rect(); leaf.getDrawingRect(rect); scroll.offsetDescendantRectToMyCoords(leaf, rect);
            rect.offset(-scroll.getScrollX(), -scroll.getScrollY());
            assertTrue("Endpoint leaf is fully visible: " + rect + " scroll=" + scroll.getScrollX(),
                    rect.left >= scroll.getPaddingLeft() && rect.right <= scroll.getWidth() - scroll.getPaddingRight());
        }
    }

    private static View extremeLeaf(LinearLayout leaves, boolean left) {
        View extreme = null;
        for (int i = 0; i < leaves.getChildCount(); i++) {
            View leaf = leaves.getChildAt(i);
            if (leaf.getVisibility() == View.GONE) continue;
            if (extreme == null || (left ? leaf.getLeft() < extreme.getLeft() : leaf.getRight() > extreme.getRight())) extreme = leaf;
        }
        assertTrue("Fixture has a visible leaf", extreme != null);
        return extreme;
    }

    private static LinearLayout content(Context context, int dpi, boolean nested, int count) {
        LinearLayout subject = new LinearLayout(context); subject.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout leaves = subject;
        if (nested) {
            leaves = new LinearLayout(context); leaves.setOrientation(LinearLayout.HORIZONTAL);
            subject.addView(leaves, new LinearLayout.LayoutParams(-2, -1));
        }
        for (int i = 0; i < count; i++) addLeaf(leaves, dpi / 2);
        return subject;
    }

    private static void addLeaf(LinearLayout parent, int width) {
        parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(width, 48));
    }

    private static void layout(View view, int size, int direction) {
        view.setLayoutDirection(direction);
        view.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, size, size);
    }

    // right=true moves toward the physical right end, independent of layout direction.
    private static void swipe(ItemHorizontalScrollView view, boolean right) {
        for (int i = 0; i < 8; i++) {
            long time = SystemClock.uptimeMillis(); int step = 0;
            for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
                float x = (action == MotionEvent.ACTION_DOWN) == right ? view.getWidth() - 1 : 1;
                MotionEvent event = MotionEvent.obtain(time, time + step++, action, x, 20, 0);
                try { assertFalse("Editor scrolling leaves child input available", view.onInterceptTouchEvent(event)); }
                finally { event.recycle(); }
            }
        }
    }

    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration()); config.densityDpi = dpi;
        Context configured = base.createConfigurationContext(config);
        ApplicationInfo info = new ApplicationInfo(base.getApplicationInfo()); info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        ContextWrapper wrapper = new ContextWrapper(configured) {
            @Override public ApplicationInfo getApplicationInfo() { return info; }
        };
        return new ContextThemeWrapper(wrapper, R.style.Theme_SketchwarePro);
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
