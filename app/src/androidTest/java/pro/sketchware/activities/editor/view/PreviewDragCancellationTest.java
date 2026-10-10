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
import android.os.Handler;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
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
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.ViewHistoryManager;
import pro.sketchware.core.project.SketchwarePaths;

/** Real DOWN events and the scheduled long-press callback, followed by cancel/release controls. */
@RunWith(AndroidJUnit4.class)
public class PreviewDragCancellationTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] DIRECTIONS = {View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL};
    private static final int[] CANCEL_ACTIONS = {MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_SCROLL};

    @Test public void cancellingLongPressedItemsRestoresVisibilityAndKeepsProjectData() {
        List<Case> cases = new ArrayList<>();
        for (int dpi : DENSITIES) for (int direction : DIRECTIONS)
            for (boolean horizontal : new boolean[]{false, true}) for (boolean nested : new boolean[]{false, true})
                for (int action : CANCEL_ACTIONS) cases.add(new Case(dpi, direction, horizontal, nested, action));
        exercise("drag_cancel_item_checks", cases, true, run -> run.press(run.source), run -> {
            run.assertDragging(); run.touch(run.action); run.assertRestored();
            assertSame("Cancel keeps long-press deselection", null, field(run.fixture.editor, "selectedItem"));
            assertArrayEquals(run.offsets, run.fixture.offsets());
        });
    }

    @Test public void cancellingContainerDragsRestoresTheirChildren() {
        List<Case> cases = new ArrayList<>();
        for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true})
            for (int action : CANCEL_ACTIONS) for (int type : new int[]{ViewBean.VIEW_TYPE_LAYOUT_LINEAR, ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW})
                cases.add(new Case(320, direction, horizontal, false, action, type));
        exercise("drag_cancel_container_checks", cases, true, run -> {
            Fixture f = run.fixture;
            ViewBean container = linear("moving_container", "root", ViewBean.VIEW_TYPE_LAYOUT_LINEAR, false);
            container.type = run.variant; container.layout.width = 40; container.layout.height = 40;
            ViewGroup parent = (ViewGroup) f.add(container);
            if (run.variant == ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW) {
                f.add(linear("moving_content", container.id, container.type, false)); parent = f.pane.findViewWithTag("moving_content");
            }
            f.add(image("moving_child", bean(parent).id, 10, 10));
            f.layout(); run.press((View) f.pane.findViewWithTag(container.id));
        }, run -> {
            run.assertDragging(); run.touch(run.action); run.assertRestored();
            assertTrue("Container descendants return to the preview", run.fixture.pane.findViewWithTag("moving_child").isShown());
        });
    }

    @Test public void cancelledItemsCanBeSelectedMovedAndScrolledAgain() {
        List<Case> cases = standardCases(true);
        exercise("drag_cancel_reuse_checks", cases, true, run -> run.press(run.source), run -> {
            run.assertDragging(); run.touch(MotionEvent.ACTION_CANCEL); run.assertRestored();
            run.touch(MotionEvent.ACTION_DOWN); run.touch(MotionEvent.ACTION_UP);
            assertSame(run.source, field(run.fixture.editor, "selectedItem"));
            run.assertScrollGesturesWork();
            // Use the same scheduled long-press callback for a second drag, after cancellation.
            run.press(run.source);
        }, run -> {
            run.assertDragging(); Rect hit = visible(run.fixture.leaves.get(0));
            run.touch(MotionEvent.ACTION_MOVE, hit.centerX(), hit.centerY());
            assertTrue(((ViewDummy) field(run.fixture.editor, "dummyView")).getAllow());
            run.touch(MotionEvent.ACTION_UP, hit.centerX(), hit.centerY()); run.fixture.layout(); run.fixture.preDraw();
            List<String> expected = new ArrayList<>(run.order); expected.remove(bean(run.source).id); expected.add(0, bean(run.source).id);
            assertEquals(expected, run.fixture.order()); run.fixture.assertIndices();
            assertEquals(View.VISIBLE, run.source.getVisibility()); assertSame(run.source, field(run.fixture.editor, "selectedItem"));
            assertTrue(run.source.isShown()); run.assertFinished();
        });
    }

    @Test public void successfulRejectedAndDeletedDropsKeepTheirExistingBehavior() {
        List<Case> cases = new ArrayList<>();
        for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true})
            for (int mode = 0; mode < 3; mode++) cases.add(new Case(320, direction, horizontal, false, MotionEvent.ACTION_UP, mode));
        exercise("drag_cancel_release_controls", cases, true, run -> run.press(run.source), run -> {
            run.assertDragging();
            if (run.variant == 0) {
                Rect hit = visible(run.fixture.leaves.get(0)); run.touch(MotionEvent.ACTION_MOVE, hit.centerX(), hit.centerY());
                run.touch(MotionEvent.ACTION_UP, hit.centerX(), hit.centerY()); run.fixture.layout(); run.fixture.preDraw();
                assertEquals(View.VISIBLE, run.source.getVisibility()); assertSame(run.source, field(run.fixture.editor, "selectedItem"));
                List<String> expected = new ArrayList<>(run.order); expected.remove(bean(run.source).id); expected.add(0, bean(run.source).id);
                assertEquals(expected, run.fixture.order()); run.fixture.assertIndices();
            } else if (run.variant == 1) {
                run.touch(MotionEvent.ACTION_MOVE, -100, -100);
                assertFalse(((ViewDummy) field(run.fixture.editor, "dummyView")).getAllow());
                run.touch(MotionEvent.ACTION_UP, -100, -100); run.assertRestored();
            } else {
                // The host exposes only the pane; set the delete hit state, then use the real release branch.
                set(run.fixture.editor, "isDeleteActive", true);
                ((ViewDummy) field(run.fixture.editor, "dummyView")).setAllow(true);
                run.touch(MotionEvent.ACTION_UP); run.fixture.layout();
                assertSame(null, run.source.getParent());
                List<String> expected = new ArrayList<>(run.order); expected.remove(bean(run.source).id);
                assertEquals(expected, run.fixture.order()); run.fixture.assertIndices();
            }
            run.assertFinished();
        });
    }

    @Test public void cancellationBeforeLongPressRemovesPendingDragWithoutHidingTheSource() {
        List<Case> cases = new ArrayList<>();
        for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true})
            for (int action : CANCEL_ACTIONS) for (int mode = 0; mode < 2; mode++)
                cases.add(new Case(320, direction, horizontal, false, action, mode));
        exercise("drag_cancel_pending_checks", cases, true, run -> {
            if (run.variant == 1) run.source.setVisibility(View.INVISIBLE);
            run.press(run.source); run.touch(run.action);
        }, run -> {
            assertEquals(run.variant == 1 ? View.INVISIBLE : View.VISIBLE, run.source.getVisibility());
            assertEquals(run.data, run.data()); assertEquals(run.order, run.fixture.order());
            assertEquals(run.history, run.history()); run.assertFinished();
        });
    }

    @Test public void paletteCancellationDoesNotAddAnItemOrLeaveDragState() {
        List<Case> cases = new ArrayList<>();
        for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true})
            for (int action : CANCEL_ACTIONS) cases.add(new Case(320, direction, horizontal, false, action));
        exercise("drag_cancel_palette_checks", cases, true, run -> {
            IconBase icon = run.fixture.icon(10, 10);
            icon.measure(View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY));
            icon.layout(0, 0, 20, 20); icon.setOnTouchListener(run.fixture.editor); run.press(icon);
        }, run -> {
            assertTrue((Boolean) field(run.fixture.editor, "isDragged"));
            run.touch(run.action); assertEquals(View.VISIBLE, run.source.getVisibility());
            assertEquals(run.data, run.data()); assertEquals(run.order, run.fixture.order());
            assertEquals(run.history, run.history()); run.assertFinished();
        });
    }

    private record Case(int dpi, int direction, boolean horizontal, boolean nested, int action, int variant) {
        Case(int dpi, int direction, boolean horizontal, boolean nested, int action) { this(dpi, direction, horizontal, nested, action, 0); }
    }
    private static List<Case> standardCases(boolean nested) {
        List<Case> cases = new ArrayList<>();
        for (int direction : DIRECTIONS) for (boolean horizontal : new boolean[]{false, true})
            for (boolean useNested : new boolean[]{false, nested}) cases.add(new Case(320, direction, horizontal, useNested, MotionEvent.ACTION_CANCEL));
        return cases;
    }
    private interface Step { void run(Run run) throws Exception; }
    private static void exercise(String label, List<Case> cases, boolean awaitLongPress, Step prepare, Step verify) {
        exercise(label, cases, awaitLongPress, prepare, verify, null);
    }
    private static void exercise(String label, List<Case> cases, boolean awaitLongPress, Step prepare, Step verify, Step secondVerify) {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            Session[] session = new Session[1]; Run[] active = new Run[1];
            try {
                scenario.onActivity(activity -> session[0] = new Session());
                for (Case value : cases) {
                    scenario.onActivity(activity -> checked(() -> {
                        if (active[0] != null) active[0].dispose();
                        active[0] = new Run(new Fixture(activity.findViewById(android.R.id.content), session[0], value.dpi, value.direction, value.horizontal, value.nested), value);
                        prepare.run(active[0]);
                    }));
                    if (awaitLongPress) waitForLongPress();
                    scenario.onActivity(activity -> checked(() -> verify.run(active[0])));
                    if (secondVerify != null) {
                        waitForLongPress(); scenario.onActivity(activity -> checked(() -> secondVerify.run(active[0])));
                    }
                }
                status(label, cases.size());
            } finally {
                scenario.onActivity(activity -> checked(() -> {
                    if (active[0] != null) active[0].dispose();
                    ((ViewGroup) activity.findViewById(android.R.id.content)).removeAllViews();
                    if (session[0] != null) session[0].close();
                }));
            }
        }
    }
    private interface Checked { void run() throws Exception; }
    private static void checked(Checked operation) { try { operation.run(); } catch (Exception failure) { throw new AssertionError(failure); } }
    private static void waitForLongPress() { SystemClock.sleep(ViewConfiguration.getLongPressTimeout() / 2 + 100); InstrumentationRegistry.getInstrumentation().waitForIdleSync(); }

    private static class Run {
        final Fixture fixture; final int action, variant;
        View source; Object parent; List<String> order; String data, history; int[] offsets; float x, y; long downTime;
        int started, ended;
        Run(Fixture fixture, Case value) throws Exception {
            this.fixture = fixture; action = value.action; variant = value.variant;
            fixture.editor.setOnDraggingListener(new DraggingListener() {
                @Override public boolean isAdmobEnabled() { return false; }
                @Override public boolean isGoogleMapEnabled() { return false; }
                @Override public void onDragStarted() { started++; }
                @Override public void onDragEnded() { ended++; }
            });
            fixture.clipLeadingEdge(); source = fixture.leaves.get(1);
        }
        void press(View target) throws Exception {
            source = target; parent = source.getParent(); order = fixture.order(); data = data(); history = history(); offsets = fixture.offsets();
            Rect hit = source instanceof ItemView && source.isShown() ? visible(source) : visible(fixture.leaves.get(1));
            x = hit.centerX(); y = hit.centerY(); downTime = SystemClock.uptimeMillis();
            touch(MotionEvent.ACTION_DOWN);
        }
        void touch(int action) { touch(action, x, y); }
        void touch(int action, float x, float y) {
            MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
            try { assertTrue(source.dispatchTouchEvent(event)); } finally { event.recycle(); }
        }
        void assertDragging() throws Exception {
            assertTrue("Scheduled long press entered dragging", (Boolean) field(fixture.editor, "isDragged"));
            assertEquals(View.GONE, source.getVisibility());
            assertFalse((Boolean) field(fixture.outer, fixture.horizontal ? "isScrollEnabled" : "scrollEnabled"));
            assertTrue(started > 0);
        }
        void assertRestored() throws Exception {
            fixture.layout(); fixture.preDraw();
            assertEquals("Cancelled item returns to VISIBLE", View.VISIBLE, source.getVisibility());
            assertTrue(source.isShown()); assertSame(parent, source.getParent());
            assertEquals(order, fixture.order()); fixture.assertIndices();
            assertEquals("Cancel does not alter project view data", data, data());
            assertEquals("Cancel adds no history entry", history, history()); assertFinished();
        }
        void assertFinished() throws Exception {
            assertFalse((Boolean) field(fixture.editor, "isDragged"));
            assertSame("Finished gesture clears the touched source", null, field(fixture.editor, "currentTouchedView"));
            assertEquals(View.GONE, ((ViewDummy) field(fixture.editor, "dummyView")).layout_dummy.getVisibility());
            assertSame(null, ((View) field(fixture.pane, "highlightedTextView")).getParent());
            assertTrue((Boolean) field(fixture.outer, fixture.horizontal ? "isScrollEnabled" : "scrollEnabled"));
            if (fixture.inner != null) assertTrue((Boolean) field(fixture.inner, fixture.horizontal ? "scrollEnabled" : "isScrollEnabled"));
            assertTrue((Boolean) field(field(fixture.editor.paletteWidget, "scrollView"), "scrollEnabled"));
            assertTrue((Boolean) field(field(field(fixture.editor, "paletteFavorite"), "scrollView"), "scrollEnabled"));
            assertTrue(ended > 0);
        }
        String data() { StringBuilder result = new StringBuilder(); ProjectDataManager.getProjectDataManager(fixture.session.id).serializeViewData(result); return result.toString(); }
        String history() { ViewHistoryManager manager = ViewHistoryManager.getInstance(fixture.session.id); return manager.positionMap.toString() + ":" + manager.historyMap.size(); }
        void assertScrollGesturesWork() {
            scroll(fixture.outer, fixture.horizontal, fixture.direction); if (fixture.inner != null) scroll(fixture.inner, !fixture.horizontal, fixture.direction);
            fixture.clipLeadingEdge();
        }
        void dispose() throws Exception { ((Handler) field(fixture.editor, "handler")).removeCallbacksAndMessages(null); }
    }
    private static void scroll(ViewGroup view, boolean horizontal, int direction) {
        int before = horizontal ? view.getScrollX() : view.getScrollY();
        float start = 80, end = horizontal && direction == View.LAYOUT_DIRECTION_RTL ? 100 : 60;
        for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP}) {
            float position = action == MotionEvent.ACTION_DOWN ? start : end;
            MotionEvent event = MotionEvent.obtain(0, SystemClock.uptimeMillis(), action, horizontal ? position : 10, horizontal ? 10 : position, 0);
            try { view.onInterceptTouchEvent(event); } finally { event.recycle(); }
        }
        assertTrue("Scroll gesture works after cancellation", before != (horizontal ? view.getScrollX() : view.getScrollY()));
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
            ProjectDataManager.clearAll(); ViewHistoryManager.clearInstance();
            Context context = context(dpi);
            ProjectFileBean file = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "f27_cancel");
            xmlName = file.getXmlName();
            editor = new ViewEditor(context); editor.initialize(session.id, file);
            set(editor, "useVibrate", false);
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
    private static Rect visible(View view) { Rect rect = new Rect(); assertTrue(view.getGlobalVisibleRect(rect) && !rect.isEmpty()); return rect; }
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
    private static class Session implements AutoCloseable {
        final String id = "F27Cancel." + UUID.randomUUID();
        final File[] roots = {new File(SketchwarePaths.getDataPath(id)), new File(SketchwarePaths.getBackupPath(id)), new File(SketchwarePaths.getProjectListPath(id))};
        Session() { for (File root : roots) assertFalse(root.exists()); ProjectDataManager.clearAll(); status("f27_owned_project", id); }
        @Override public void close() throws Exception {
            ProjectDataManager.clearAll();
            for (File root : roots) {
                assertEquals(id, root.getName()); assertFalse(Files.isSymbolicLink(root.toPath()));
                if (root.exists()) try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
                    for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.delete(path);
                }
                assertFalse(root.exists());
            }
            status("f27_cleaned_project", id);
        }
    }
    private static void status(String key, int value) { status(key, Integer.toString(value)); }
    private static void status(String key, String value) {
        Bundle bundle = new Bundle(); bundle.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }
}
