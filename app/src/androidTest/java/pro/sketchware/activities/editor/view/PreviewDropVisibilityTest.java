package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.view.palette.IconBase;
import pro.sketchware.activities.editor.view.palette.WidgetPaletteIcon;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;

/** Exercises ViewEditor release handling and its first pre-draw, using owned project fixtures. */
@RunWith(AndroidJUnit4.class)
public class PreviewDropVisibilityTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] DIRECTIONS = {View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL};

    @Test public void paletteDropsRevealPartialEdgeInsertionsAfterLayout() {
        onHost((host, session) -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) for (boolean nested : new boolean[]{false, true}) {
                    for (float[] scale : new float[][]{{1f, 1f}, {.5f, .75f}, {1.25f, .5f}}) {
                        Fixture fixture = new Fixture(host, session, dpi, direction, horizontal, nested);
                        fixture.pane.setPivotX(0); fixture.pane.setPivotY(0);
                        fixture.pane.setScaleX(scale[0]); fixture.pane.setScaleY(scale[1]);
                        fixture.outer.setPivotX(0); fixture.outer.setPivotY(0);
                        fixture.outer.setScaleX(.8f); fixture.outer.setScaleY(.9f);
                        fixture.clipLeadingEdge();
                        View target = fixture.leaves.get(0); Rect hit = visible(target);
                        assertTrue("Fixture hits a partly clipped target", hit.width() < bounds(target).width()
                                || hit.height() < bounds(target).height());
                        List<String> original = fixture.order();
                        View added = fixture.dropPalette(hit, 10, 10);
                        assertFalse("Reveal waits for final layout", added.isLaidOut());
                        fixture.layout();
                        assertFalse("Before reveal, the insertion is outside the viewport", isVisible(added));
                        fixture.preDraw(); assertFullyVisible(added);
                        assertSame(added, field(fixture.editor, "selectedItem"));
                        List<String> expected = new ArrayList<>(original); expected.add(0, bean(added).id);
                        assertEquals(expected, fixture.order()); fixture.assertIndices();
                        int[] offsets = fixture.offsets(); fixture.layout(); fixture.preDraw();
                        assertArrayEquals("Later layout does not undo or repeat the reveal", offsets, fixture.offsets());
                        assertFullyVisible(added); checks++;
                    }
                }
            }
            status("drop_reveal_palette_checks", checks);
        });
    }

    @Test public void movingWithinAndBetweenParentsRevealsTheFinalItem() {
        onHost((host, session) -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) {
                for (boolean horizontal : new boolean[]{false, true}) for (boolean crossParent : new boolean[]{false, true}) {
                    Fixture fixture = new Fixture(host, session, dpi, direction, horizontal, false);
                    View moving;
                    if (crossParent) moving = fixture.add(image("moving", "root", 10, 10));
                    else {
                        moving = fixture.leaves.get(3);
                        bean(moving).layout.width = 10; bean(moving).layout.height = 10;
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) moving.getLayoutParams();
                        params.width = fixture.pixels(10); params.height = fixture.pixels(10); moving.setLayoutParams(params);
                    }
                    fixture.clipLeadingEdge(); Rect hit = visible(fixture.leaves.get(0));
                    List<String> expected = fixture.order(); expected.remove(bean(moving).id); expected.add(0, bean(moving).id);
                    moving.setVisibility(View.GONE);
                    fixture.pane.addRootLayout(bean(moving));
                    fixture.pane.updateView(hit.centerX(), hit.centerY(), moving.getWidth(), moving.getHeight());
                    fixture.release(moving, hit, true);
                    fixture.layout(); assertFalse(isVisible(moving)); fixture.preDraw();
                    assertFullyVisible(moving); assertSame(fixture.content, moving.getParent());
                    assertSame(moving, field(fixture.editor, "selectedItem"));
                    assertEquals(expected, fixture.order()); fixture.assertIndices();
                    assertEquals(View.VISIBLE, moving.getVisibility()); checks++;
                }
            }
            status("drop_reveal_move_checks", checks);
        });
    }

    @Test public void collectionDropsRevealTheSelectedRootAndKeepChildOrder() {
        onHost((host, session) -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true}) {
                Fixture fixture = new Fixture(host, session, dpi, direction, horizontal, true);
                fixture.clipLeadingEdge(); Rect hit = visible(fixture.leaves.get(0));
                ViewBean root = linear("collection", "", ViewBean.VIEW_TYPE_LAYOUT_LINEAR, horizontal);
                root.layout.width = 10; root.layout.height = 10;
                ArrayList<ViewBean> data = new ArrayList<>(); data.add(root);
                data.add(image("collection_child_0", root.id, 3, 3));
                data.add(image("collection_child_1", root.id, 3, 3));
                List<String> original = fixture.order();
                fixture.pane.addRootLayout(null); fixture.pane.updateView(hit.centerX(), hit.centerY(), fixture.pixels(10), fixture.pixels(10));
                fixture.release(new WidgetPaletteIcon(fixture.pane.getContext(), "F26 collection", data), hit, true);
                ViewGroup added = (ViewGroup) field(fixture.editor, "selectedItem");
                fixture.layout(); assertFalse(isVisible(added)); fixture.preDraw(); assertFullyVisible(added);
                List<String> expected = new ArrayList<>(original); expected.add(0, bean(added).id);
                assertEquals(expected, fixture.order()); fixture.assertIndices();
                assertEquals(2, added.getChildCount());
                for (int i = 0; i < 2; i++) {
                    assertEquals(i, bean(added.getChildAt(i)).index);
                    assertEquals(bean(added).id, bean(added.getChildAt(i)).parent);
                }
                checks++;
            }
            status("drop_reveal_collection_checks", checks);
        });
    }

    @Test public void visibleDropsAndOrdinarySelectionDoNotMoveTheViewport() {
        onHost((host, session) -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true}) {
                Fixture fixture = new Fixture(host, session, dpi, direction, horizontal, false);
                fixture.clipLeadingEdge(); View target = fixture.leaves.get(1); assertFullyVisible(target);
                int[] offsets = fixture.offsets();
                fixture.editor.setSelectedItem((ItemView) target, true); fixture.layout(); fixture.preDraw();
                assertArrayEquals("Ordinary selection does not request scrolling", offsets, fixture.offsets());
                View added = fixture.dropPalette(visible(target), 10, 10); fixture.layout(); fixture.preDraw();
                assertFullyVisible(added);
                assertArrayEquals("An already visible drop keeps the scroll position", offsets, fixture.offsets()); checks++;
            }
            status("drop_reveal_visible_controls", checks);
        });
    }

    @Test public void draggingRejectedDropsAndDeletionKeepTheScrollPosition() {
        onHost((host, session) -> {
            int checks = 0;
            for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true}) {
                for (int action : new int[]{MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP}) {
                    Fixture fixture = new Fixture(host, session, 320, direction, horizontal, false);
                    fixture.clipLeadingEdge(); Rect hit = visible(fixture.leaves.get(0)); int[] offsets = fixture.offsets();
                    IconBase icon = fixture.icon(10, 10);
                    fixture.pane.addRootLayout(null); fixture.pane.updateView(hit.centerX(), hit.centerY(), 20, 20);
                    set(fixture.editor, "isDragged", true); set(fixture.editor, "currentTouchedView", icon);
                    for (int i = 0; i < 3; i++) {
                        fixture.layout(); fixture.preDraw();
                        assertArrayEquals("Dragging does not scroll cached hit regions", offsets, fixture.offsets());
                    }
                    ((ViewDummy) field(fixture.editor, "dummyView")).setAllow(false);
                    fixture.touch(icon, action, hit); fixture.layout(); fixture.preDraw();
                    assertEquals(4, fixture.order().size()); assertArrayEquals(offsets, fixture.offsets()); checks++;
                }
                Fixture fixture = new Fixture(host, session, 320, direction, horizontal, false);
                fixture.clipLeadingEdge(); int[] offsets = fixture.offsets(); View deleted = fixture.leaves.get(3);
                set(fixture.editor, "isDeleteActive", true);
                fixture.release(deleted, visible(fixture.leaves.get(0)), true);
                fixture.layout(); fixture.preDraw(); assertSame(null, deleted.getParent());
                assertArrayEquals("Deletion never requests reveal", offsets, fixture.offsets()); checks++;
            }
            status("drop_reveal_cancel_delete_controls", checks);
        });
    }

    @Test public void pendingRevealIgnoresRemovedItemsNewSelectionAndNewDrag() {
        onHost((host, session) -> {
            int checks = 0;
            for (int direction : DIRECTIONS) for (int operation = 0; operation < 4; operation++) {
                Fixture fixture = new Fixture(host, session, 320, direction, true, false);
                fixture.clipLeadingEdge(); View added = fixture.dropPalette(visible(fixture.leaves.get(0)), 10, 10);
                int[] offsets = fixture.offsets();
                if (operation == 0) fixture.editor.setSelectedItem((ItemView) fixture.leaves.get(1), true);
                if (operation == 1) fixture.pane.removeView(bean(added));
                if (operation == 2) set(fixture.editor, "isDragged", true);
                if (operation == 3) host.removeView(fixture.pane);
                fixture.layout(); fixture.preDraw();
                assertArrayEquals("A stale drop cannot move the current viewport", offsets, fixture.offsets()); checks++;
            }
            status("drop_reveal_stale_controls", checks);
        });
    }

    @Test public void oversizedItemsRemainReachableWithoutRepeatedScrolling() {
        onHost((host, session) -> {
            int checks = 0;
            for (int dpi : DENSITIES) for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true}) {
                Fixture fixture = new Fixture(host, session, dpi, direction, horizontal, false);
                fixture.clipLeadingEdge(); View added = fixture.dropPalette(visible(fixture.leaves.get(0)),
                        horizontal ? 256 : 24, horizontal ? 24 : 256);
                fixture.layout(); fixture.outer.setClipToPadding(false);
                Rect itemRect = new Rect(); added.getDrawingRect(itemRect);
                fixture.outer.offsetDescendantRectToMyCoords(added, itemRect);
                // Touching the viewport edge gives zero visible pixels and still needs revealing.
                int edge = horizontal ? direction == View.LAYOUT_DIRECTION_RTL
                        ? itemRect.left - fixture.outer.getWidth() : itemRect.right : itemRect.bottom;
                fixture.outer.scrollTo(horizontal ? edge : 0, horizontal ? 0 : edge); fixture.layout();
                assertFalse("Oversized fixture starts exactly outside the viewport", isVisible(added));
                fixture.preDraw(); assertTrue("Oversized insertion has a visible portion", isVisible(added));
                int[] offsets = fixture.offsets(); fixture.layout(); fixture.preDraw();
                assertArrayEquals("Oversized selection does not oscillate between edges", offsets, fixture.offsets());
                fixture.assertIndices(); checks++;
            }
            status("drop_reveal_oversized_checks", checks);
        });
    }

    private static class Fixture {
        final ViewEditor editor;
        final ViewPane pane;
        final ViewGroup outer, inner, content;
        final List<View> leaves = new ArrayList<>();
        final Session session;
        final int dpi, direction;
        final boolean horizontal;
        final String xmlName;

        Fixture(ViewGroup host, Session session, int dpi, int direction, boolean horizontal, boolean nested) throws Exception {
            this.session = session; this.dpi = dpi; this.direction = direction; this.horizontal = horizontal;
            ProjectDataManager.clearAll();
            Context context = context(dpi);
            ProjectFileBean file = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "f26_drop");
            xmlName = file.getXmlName();
            editor = new ViewEditor(context); editor.initialize(session.id, file);
            pane = (ViewPane) field(editor, "viewPane"); ((ViewGroup) pane.getParent()).removeView(pane);
            pane.setResourceManager(new ResourceManager(session.id, context.getCacheDir().getAbsolutePath(),
                    context.getCacheDir().getAbsolutePath(), context.getCacheDir().getAbsolutePath()));
            pane.updateRootLayout(session.id, xmlName);
            ViewBean scroll = new ViewBean("scroll", horizontal ? ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW : ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW);
            scroll.parent = "root"; scroll.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            scroll.index = -1; scroll.layout.width = 160; scroll.layout.height = 160;
            scroll.layout.paddingLeft = 8; scroll.layout.paddingTop = 8;
            scroll.layout.paddingRight = 8; scroll.layout.paddingBottom = 8;
            outer = (ViewGroup) add(scroll);
            add(linear("outer_content", "scroll", scroll.type, horizontal));
            String parent = "outer_content";
            if (nested) {
                ViewBean cross = new ViewBean("inner", horizontal ? ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW : ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW);
                cross.parent = parent; cross.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR; cross.index = -1;
                cross.layout.width = horizontal ? -2 : -1; cross.layout.height = horizontal ? -1 : -2;
                inner = (ViewGroup) add(cross);
                add(linear("content", "inner", cross.type, horizontal)); parent = "content";
            } else inner = null;
            content = pane.findViewWithTag(parent);
            for (int i = 0; i < 4; i++) leaves.add(add(image("leaf_" + i, parent,
                    horizontal ? 64 : nested ? 256 : 24, horizontal ? nested ? 256 : 24 : 64)));
            host.removeAllViews(); host.addView(pane, new ViewGroup.LayoutParams(pixels(200), pixels(200)));
            layout(); assertTrue(pane.isAttachedToWindow());
        }

        View add(ViewBean bean) {
            ProjectDataManager.getProjectDataManager(session.id).addView(xmlName, bean);
            return (View) editor.createAndAddView(bean);
        }

        void clipLeadingEdge() {
            int offset = pixels(32) * (horizontal && direction == View.LAYOUT_DIRECTION_RTL ? -1 : 1);
            outer.scrollTo(horizontal ? offset : 0, horizontal ? 0 : offset);
            if (inner != null) inner.scrollTo(horizontal ? 0 : direction == View.LAYOUT_DIRECTION_RTL ? -pixels(64) : pixels(64), horizontal ? pixels(64) : 0);
            layout();
        }

        IconBase icon(int width, int height) {
            return new IconBase(pane.getContext()) {
                @Override public ViewBean getBean() { return image("", "", width, height); }
            };
        }

        View dropPalette(Rect hit, int width, int height) throws Exception {
            pane.addRootLayout(null); pane.updateView(hit.centerX(), hit.centerY(), pixels(width), pixels(height));
            release(icon(width, height), hit, true);
            return (View) field(editor, "selectedItem");
        }

        void release(View source, Rect hit, boolean allow) throws Exception {
            set(editor, "isDragged", true); set(editor, "currentTouchedView", source);
            ((ViewDummy) field(editor, "dummyView")).setAllow(allow);
            touch(source, MotionEvent.ACTION_UP, hit);
        }

        void touch(View source, int action, Rect hit) {
            long time = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(time, time, action, hit.centerX(), hit.centerY(), 0);
            try { assertTrue(editor.onTouch(source, event)); } finally { event.recycle(); }
        }

        List<String> order() {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < content.getChildCount(); i++) {
                if (content.getChildAt(i) instanceof ItemView item) ids.add(item.getBean().id);
            }
            return ids;
        }
        void assertIndices() {
            int index = 0;
            for (int i = 0; i < content.getChildCount(); i++) {
                if (content.getChildAt(i) instanceof ItemView item) assertEquals(index++, item.getBean().index);
            }
        }
        int[] offsets() { return new int[]{outer.getScrollX(), outer.getScrollY(), inner == null ? 0 : inner.getScrollX(), inner == null ? 0 : inner.getScrollY()}; }
        int pixels(int dp) { return Math.round(dp * dpi / 160f); }
        void layout() {
            outer.requestLayout(); pane.setLayoutDirection(direction);
            pane.measure(View.MeasureSpec.makeMeasureSpec(pixels(200), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(pixels(200), View.MeasureSpec.EXACTLY));
            pane.layout(0, 0, pixels(200), pixels(200));
        }
        void preDraw() { pane.getViewTreeObserver().dispatchOnPreDraw(); }
    }

    private static ViewBean image(String id, String parent, int width, int height) {
        ViewBean bean = new ViewBean(id, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
        bean.parent = parent; bean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR; bean.index = -1;
        bean.layout.width = width; bean.layout.height = height; bean.image.resName = "NONE"; return bean;
    }
    private static ViewBean linear(String id, String parent, int parentType, boolean horizontal) {
        ViewBean bean = new ViewBean(id, ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        bean.parent = parent; bean.parentType = parentType; bean.index = -1;
        bean.layout.orientation = horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
        bean.layout.width = horizontal ? -2 : -1; bean.layout.height = horizontal ? -1 : -2; return bean;
    }
    private static ViewBean bean(View view) { return ((ItemView) view).getBean(); }
    private static Rect bounds(View view) {
        float[] points = {0, 0, view.getWidth(), view.getHeight()};
        View current = view;
        while (current.getParent() instanceof View parent) {
            current.getMatrix().mapPoints(points);
            for (int i = 0; i < 4; i += 2) { points[i] += current.getLeft() - parent.getScrollX(); points[i + 1] += current.getTop() - parent.getScrollY(); }
            current = parent;
        }
        int[] location = new int[2]; current.getLocationOnScreen(location);
        return new Rect(Math.round(points[0]) + location[0], Math.round(points[1]) + location[1],
                Math.round(points[2]) + location[0], Math.round(points[3]) + location[1]);
    }
    private static boolean isVisible(View view) { Rect rect = new Rect(); return view.getGlobalVisibleRect(rect) && !rect.isEmpty(); }
    private static Rect visible(View view) { Rect rect = new Rect(); assertTrue(view.getGlobalVisibleRect(rect) && !rect.isEmpty()); return rect; }
    private static void assertFullyVisible(View view) {
        Rect actual = visible(view), full = bounds(view);
        assertTrue("Drop is fully visible: visible=" + actual + " full=" + full,
                actual.left <= full.left + 1 && actual.top <= full.top + 1 && actual.right >= full.right - 1 && actual.bottom >= full.bottom - 1);
    }
    private static Object field(Object object, String name) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object); }
    private static void set(Object object, String name, Object value) throws Exception { Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); field.set(object, value); }
    private static Context context(int dpi) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(base.getResources().getConfiguration()); config.densityDpi = dpi;
        ApplicationInfo info = new ApplicationInfo(base.getApplicationInfo()); info.flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        Context configured = new ContextWrapper(base.createConfigurationContext(config)) {
            @Override public ApplicationInfo getApplicationInfo() { return info; }
        };
        return new ContextThemeWrapper(configured, R.style.Theme_SketchwarePro);
    }
    private interface HostCheck { void run(ViewGroup host, Session session) throws Exception; }
    private static void onHost(HostCheck check) {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                ViewGroup host = activity.findViewById(android.R.id.content);
                try (Session session = new Session()) { check.run(host, session); }
                catch (Exception failure) { throw new AssertionError(failure); }
                finally { host.removeAllViews(); }
            });
        }
    }
    private static class Session implements AutoCloseable {
        final String id = "F26Drop." + UUID.randomUUID();
        final File[] roots = {new File(SketchwarePaths.getDataPath(id)), new File(SketchwarePaths.getBackupPath(id)), new File(SketchwarePaths.getProjectListPath(id))};
        Session() { for (File root : roots) assertFalse(root.exists()); ProjectDataManager.clearAll(); status("f26_owned_project", id); }
        @Override public void close() throws Exception {
            ProjectDataManager.clearAll();
            for (File root : roots) {
                assertEquals(id, root.getName()); assertFalse(Files.isSymbolicLink(root.toPath()));
                if (root.exists()) try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
                    for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.delete(path);
                }
                assertFalse(root.exists());
            }
            status("f26_cleaned_project", id);
        }
    }
    private static void status(String key, int value) { status(key, Integer.toString(value)); }
    private static void status(String key, String value) {
        Bundle bundle = new Bundle(); bundle.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }
}
