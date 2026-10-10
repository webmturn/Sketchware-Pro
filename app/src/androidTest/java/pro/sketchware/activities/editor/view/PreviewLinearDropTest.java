package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.view.item.ItemHorizontalScrollView;
import pro.sketchware.activities.editor.view.item.ItemImageView;
import pro.sketchware.activities.editor.view.item.ItemLinearLayout;
import pro.sketchware.beans.ViewBean;

/** Uses attached editor items and public drag/drop paths without opening or writing a project. */
@RunWith(AndroidJUnit4.class)
public class PreviewLinearDropTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] DIRECTIONS = {View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL};

    @Test public void additionsFollowActualChildPositionsForEveryGravity() {
        onHost(host -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    int[] gravities = horizontal
                            ? new int[]{Gravity.LEFT, Gravity.RIGHT, Gravity.CENTER_HORIZONTAL, Gravity.START, Gravity.END}
                            : new int[]{Gravity.TOP, Gravity.BOTTOM, Gravity.CENTER_VERTICAL};
                    for (int gravity : gravities) {
                        Fixture fixture = new Fixture(host, dpi, direction, horizontal, gravity, false);
                        for (View target : fixture.leaves) { fixture.addBefore(target); checks++; }
                    }
                }
            }
            status("linear_drop_position_checks", checks);
        });
    }

    @Test public void leadingAndTrailingSpaceUseTheLogicalStartAndEnd() {
        onHost(host -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    for (int gravity : new int[]{Gravity.CENTER, Gravity.RIGHT | Gravity.BOTTOM}) {
                        Fixture fixture = new Fixture(host, dpi, direction, horizontal, gravity, false);
                        Rect parent = visible(fixture.subject), first = visible(fixture.leaves.get(0));
                        Rect last = visible(fixture.leaves.get(3));
                        boolean rtl = horizontal && direction == View.LAYOUT_DIRECTION_RTL;
                        int leading = horizontal ? (rtl ? (first.right + parent.right) / 2
                                : (parent.left + first.left) / 2) : (parent.top + first.top) / 2;
                        int trailing = horizontal ? (rtl ? (parent.left + last.left) / 2
                                : (last.right + parent.right) / 2) : (last.bottom + parent.bottom) / 2;
                        fixture.select(horizontal ? leading : first.centerX(), horizontal ? first.centerY() : leading, null);
                        assertEquals("Leading space inserts before the first logical child", 0, fixture.dropBean().index);
                        fixture.pane.clearViews(); fixture.layout();
                        fixture.select(horizontal ? trailing : last.centerX(), horizontal ? last.centerY() : trailing, null);
                        assertEquals("Trailing space appends", -1, fixture.dropBean().index);
                        fixture.pane.clearViews(); fixture.layout(); checks += 2;
                    }
                }
            }
            status("linear_drop_blank_space_checks", checks);
        });
    }

    @Test public void hiddenChildrenRetainTheirPositionsDuringInsertion() {
        onHost(host -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    Fixture fixture = new Fixture(host, dpi, direction, horizontal, Gravity.CENTER, false);
                    fixture.leaves.get(0).setVisibility(View.GONE);
                    fixture.leaves.get(1).setVisibility(View.GONE);
                    fixture.leaves.get(2).setVisibility(View.INVISIBLE);
                    fixture.layout();
                    fixture.addBefore(fixture.leaves.get(3));
                    assertEquals(0, bean(fixture.leaves.get(0)).index);
                    assertEquals(1, bean(fixture.leaves.get(1)).index);
                    assertEquals(2, bean(fixture.leaves.get(2)).index); checks++;
                }
            }
            status("linear_drop_hidden_checks", checks);
        });
    }

    @Test public void movingChildrenUsesTheIndexAfterRemoval() {
        onHost(host -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) {
                    for (int source = 0; source < 4; source++) for (int targetIndex = 0; targetIndex < 4; targetIndex++) {
                        if (source == targetIndex) continue;
                        Fixture fixture = new Fixture(host, dpi, direction, horizontal, Gravity.CENTER, false);
                        View moving = fixture.leaves.get(source), target = fixture.leaves.get(targetIndex);
                        Rect hit = visible(target);
                        // ViewEditor hides the moving item and builds the zones before the next layout.
                        moving.setVisibility(View.GONE);
                        fixture.select(hit.centerX(), hit.centerY(), bean(moving));
                        ViewBean probe = fixture.dropBean();
                        int expectedIndex = targetIndex - (source < targetIndex ? 1 : 0);
                        assertEquals("Drop indices exclude only the moving item", expectedIndex, probe.index);
                        assertEquals(fixture.subject.indexOfChild(target) - 1, fixture.subject.indexOfChild(fixture.marker));
                        fixture.layout();
                        Rect markerBounds = visible(fixture.marker);
                        fixture.pane.resetView(false);
                        fixture.pane.updateViewBeanProperties(bean(moving), hit.centerX(), hit.centerY());
                        List<String> expected = fixture.order(); expected.remove(bean(moving).id);
                        expected.add(expectedIndex, bean(moving).id);
                        fixture.pane.moveView(bean(moving)); fixture.pane.clearViews(); fixture.layout();
                        assertEquals(expected, fixture.order());
                        assertEquals(expectedIndex, bean(moving).index);
                        assertEquals(View.VISIBLE, moving.getVisibility());
                        assertTrue("Move highlight is visible", !markerBounds.isEmpty());
                        fixture.assertIndices(); checks++;
                    }
                }
            }
            status("linear_drop_move_checks", checks);
        });
    }

    @Test public void nestedScrolledAndScaledItemsKeepTheSameHighlightedAndFinalPosition() {
        onHost(host -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (float[] scale : new float[][]{{1f, 1f}, {.5f, .75f}, {1.25f, .5f}}) {
                    Fixture fixture = new Fixture(host, dpi, direction, true, Gravity.START, true);
                    fixture.pane.setPivotX(0); fixture.pane.setPivotY(0);
                    fixture.pane.setScaleX(scale[0]); fixture.pane.setScaleY(scale[1]);
                    fixture.scroll.setPivotX(0); fixture.scroll.setPivotY(0);
                    fixture.scroll.setScaleX(.8f); fixture.scroll.setScaleY(.9f);
                    for (int state = 0; state < 3; state++) {
                        fixture.scroll.scrollTo(state == 1 ? -100000 : state == 2 ? 100000 : 0, 0);
                        fixture.layout();
                        int scrollX = fixture.scroll.getScrollX(), visibleTargets = 0;
                        for (View target : fixture.leaves) {
                            Rect hit = new Rect();
                            if (!target.getGlobalVisibleRect(hit) || hit.width() < fixture.pixels(12) * scale[0] * .8f) continue;
                            fixture.addBefore(target); checks++; visibleTargets++;
                            fixture.scroll.scrollTo(scrollX, 0); fixture.layout();
                        }
                        assertTrue("Every scroll state exercises a visible target", visibleTargets > 0);
                    }
                }
            }
            status("linear_drop_nested_scale_checks", checks);
        });
    }

    @Test public void cancelledAndEmptyDropsPreserveChildIndices() {
        onHost(host -> {
            int checks = 0;
            for (int direction : DIRECTIONS) {
                Fixture fixture = new Fixture(host, 320, direction, true, Gravity.CENTER, false);
                List<String> original = fixture.order();
                Rect first = visible(fixture.leaves.get(0)), last = visible(fixture.leaves.get(3));
                fixture.select(first.centerX(), first.centerY(), null); fixture.layout();
                fixture.pane.updateView(last.centerX(), last.centerY(), fixture.pixels(10), fixture.pixels(10));
                assertEquals(3, fixture.dropBean().index);
                fixture.pane.updateView(-100000, -100000, 10, 10);
                assertSame(null, fixture.marker.getParent());
                fixture.pane.clearViews(); fixture.layout();
                assertEquals(original, fixture.order()); fixture.assertIndices();
                fixture.subject.removeAllViews(); fixture.layout();
                Rect empty = visible(fixture.subject);
                fixture.select(empty.centerX(), empty.centerY(), null);
                assertEquals(-1, fixture.dropBean().index);
                fixture.pane.clearViews(); checks += 2;
            }
            status("linear_drop_cancel_empty_checks", checks);
        });
    }

    private static class Fixture {
        final ViewPane pane;
        final ItemLinearLayout root, subject;
        final ItemHorizontalScrollView scroll;
        final View marker;
        final List<View> leaves = new ArrayList<>();
        final int dpi, direction;

        Fixture(ViewGroup host, int dpi, int direction, boolean horizontal, int gravity, boolean nested) throws Exception {
            this.dpi = dpi; this.direction = direction;
            Context context = context(dpi);
            pane = new ViewPane(context);
            // Initialize only the two visual fields; no project manager or project storage is involved.
            Method initMarker = ViewPane.class.getDeclaredMethod("initTextView");
            initMarker.setAccessible(true); initMarker.invoke(pane);
            Field rootField = ViewPane.class.getDeclaredField("rootLayout"); rootField.setAccessible(true);
            Field markerField = ViewPane.class.getDeclaredField("highlightedTextView"); markerField.setAccessible(true);
            marker = (View) markerField.get(pane);
            root = linear(context, "root", LinearLayout.VERTICAL);
            rootField.set(pane, root);
            pane.addView(root, new RelativeLayout.LayoutParams(pixels(200), pixels(160)));
            if (nested) {
                scroll = new ItemHorizontalScrollView(context);
                scroll.setTag("scroll"); scroll.setBean(new ViewBean("scroll", ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW));
                root.addView(scroll, new LinearLayout.LayoutParams(pixels(160), pixels(120)));
                scroll.setPaddingPixels(pixels(8), pixels(5), pixels(8), pixels(7));
                subject = linear(context, "subject", LinearLayout.HORIZONTAL);
                scroll.addView(subject, new FrameLayout.LayoutParams(-2, -1));
                scroll.setScrollEnabled(true);
            } else { scroll = null; subject = root; }
            subject.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            subject.setLayoutGravity(gravity);
            subject.setPaddingPixels(pixels(7), pixels(5), pixels(9), pixels(11));
            for (int i = 0; i < 4; i++) {
                View leaf = leaf("leaf_" + i, nested ? 64 : 24, 24);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) leaf.getLayoutParams();
                params.leftMargin = pixels(2 + i); params.rightMargin = pixels(3);
                params.topMargin = pixels(1); params.bottomMargin = pixels(2 + i);
                // Exercise asymmetric relative margins as well as actual resolved positions.
                if (horizontal) { params.setMarginStart(pixels(2 + i)); params.setMarginEnd(pixels(3)); }
                subject.addView(leaf, -1, params); leaves.add(leaf);
            }
            subject.reindexChildren();
            host.removeAllViews(); host.addView(pane, new ViewGroup.LayoutParams(pixels(200), pixels(160)));
            layout(); assertTrue(pane.isAttachedToWindow());
        }

        View leaf(String id, int width, int height) {
            ItemImageView view = new ItemImageView(pane.getContext());
            ViewBean bean = new ViewBean(id, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
            bean.parent = (String) subject.getTag(); bean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            bean.index = -1; view.setBean(bean); view.setTag(id);
            view.setLayoutParams(new LinearLayout.LayoutParams(pixels(width), pixels(height)));
            return view;
        }

        void addBefore(View target) {
            int expectedIndex = bean(target).index;
            List<String> original = order(); Rect hit = visible(target);
            select(hit.centerX(), hit.centerY(), null);
            ViewBean drop = dropBean();
            assertEquals("Hit " + target.getTag() + " dpi=" + dpi + " direction=" + direction,
                    expectedIndex, drop.index);
            assertEquals("Marker is immediately before the hit child",
                    subject.indexOfChild(target) - 1, subject.indexOfChild(marker));
            layout(); Rect markerRect = new Rect(); boolean markerVisible = marker.getGlobalVisibleRect(markerRect);
            pane.resetView(false);
            View added = leaf("added", 10, 10);
            pane.updateViewBeanProperties(bean(added), hit.centerX(), hit.centerY());
            pane.addViewAndUpdateIndex(added); pane.clearViews(); layout();
            List<String> expected = new ArrayList<>(original); expected.add(expectedIndex, "added");
            assertEquals(expected, order()); assertEquals(expectedIndex, bean(added).index); assertIndices();
            Rect addedRect = new Rect(); boolean addedVisible = added.getGlobalVisibleRect(addedRect);
            assertEquals("Highlight and final view have the same visibility", markerVisible, addedVisible);
            assertEquals("Highlight and final view occupy the same position", markerRect, addedRect);
            pane.removeView(bean(added)); layout();
            assertEquals(original, order()); assertIndices();
        }

        void select(int x, int y, ViewBean moving) {
            pane.clearViews(); pane.addRootLayout(moving);
            pane.updateView(x, y, pixels(10), pixels(10));
            assertSame(subject, marker.getParent());
        }

        ViewBean dropBean() {
            ViewBean bean = new ViewBean("probe", ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
            pane.updateViewBeanProperties(bean, 0, 0);
            assertEquals(subject.getTag(), bean.parent); return bean;
        }

        List<String> order() {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < subject.getChildCount(); i++) {
                View child = subject.getChildAt(i);
                if (child instanceof ItemView) ids.add(bean(child).id);
            }
            return ids;
        }

        void assertIndices() {
            int index = 0;
            for (int i = 0; i < subject.getChildCount(); i++) {
                View child = subject.getChildAt(i);
                if (child instanceof ItemView) assertEquals(index++, bean(child).index);
            }
        }

        int pixels(int dp) { return Math.round(dp * dpi / 160f); }
        void layout() {
            if (scroll != null) scroll.requestLayout();
            pane.setLayoutDirection(direction);
            pane.measure(View.MeasureSpec.makeMeasureSpec(pixels(200), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(pixels(160), View.MeasureSpec.EXACTLY));
            pane.layout(0, 0, pixels(200), pixels(160));
        }
    }

    private static ItemLinearLayout linear(Context context, String id, int orientation) {
        ItemLinearLayout view = new ItemLinearLayout(context); view.setTag(id);
        view.setBean(new ViewBean(id, ViewBean.VIEW_TYPE_LAYOUT_LINEAR)); view.setOrientation(orientation); return view;
    }
    private static ViewBean bean(View view) { return ((ItemView) view).getBean(); }
    private static Rect visible(View view) {
        Rect rect = new Rect(); assertTrue("Expected a visible target: " + view.getTag(), view.getGlobalVisibleRect(rect)); return rect;
    }
    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration()); config.densityDpi = dpi;
        ApplicationInfo info = new ApplicationInfo(base.getApplicationInfo()); info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        Context configured = new ContextWrapper(base.createConfigurationContext(config)) {
            @Override public ApplicationInfo getApplicationInfo() { return info; }
        };
        return new ContextThemeWrapper(configured, R.style.Theme_SketchwarePro);
    }
    private interface HostCheck { void run(ViewGroup host) throws Exception; }
    private static void onHost(HostCheck check) {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup host = activity.findViewById(android.R.id.content);
                try { check.run(host); } catch (Exception failure) { throw new AssertionError(failure); }
                finally { host.removeAllViews(); }
            });
        }
    }
    private static void status(String key, int value) {
        Bundle bundle = new Bundle(); bundle.putInt(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }
}
