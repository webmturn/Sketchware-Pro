package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.ManageCollectionActivity;

/** Real editor drawer, with constrained viewports rather than changing device display settings. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class DesignDrawerViewportTest {
    private static final int[] ITEMS = {
            R.id.item_library_manager, R.id.item_view_manager, R.id.item_image_manager,
            R.id.item_sound_manager, R.id.item_font_manager, R.id.item_java_manager,
            R.id.item_resource_manager, R.id.item_resource_editor, R.id.item_assets_manager,
            R.id.item_permission_manager, R.id.item_appcompat_manager, R.id.item_manifest_manager,
            R.id.item_used_custom_blocks, R.id.item_code_shrinking_manager, R.id.item_stringfog_manager,
            R.id.item_show_src, R.id.item_xml_command_manager, R.id.item_logcat_reader,
            R.id.item_collection_manager
    };

    @Test public void normalViewportCanRevealEveryMenu() throws Exception {
        withDrawer(scenario -> assertEveryMenuReachable(scenario));
    }

    @Test public void shortViewportCanFullyRevealTheFirstManager() throws Exception {
        withDrawer(scenario -> {
            constrain(scenario, false);
            reveal(scenario, R.id.item_library_manager);
            screenshot(scenario, "drawer-short.png");
            check(scenario, activity -> {
                android.os.Bundle geometry = new android.os.Bundle();
                DesignDrawer drawer = drawer(activity);
                ScrollView scroll = scroll(drawer);
                geometry.putString("drawer_short_geometry", "drawer=" + bounds(drawer) + ", scroll="
                        + bounds(scroll) + ", padding=" + scroll.getPaddingTop() + "/" + scroll.getPaddingBottom()
                        + ", scrollY=" + scroll.getScrollY() + ", item="
                        + bounds(activity.findViewById(R.id.item_library_manager)));
                instrumentation().sendStatus(0, geometry);
                assertFullyVisible(activity, R.id.item_library_manager);
            });
        });
    }

    @Test public void largeTextInShortViewportKeepsEveryMenuReachable() throws Exception {
        withDrawer(scenario -> {
            constrain(scenario, true);
            assertEveryMenuReachable(scenario);
        });
    }

    @Test public void repeatedSystemInsetsKeepTotalPaddingStable() throws Exception {
        withDrawer(scenario -> check(scenario, activity -> {
            DesignDrawer drawer = drawer(activity);
            ScrollView scroll = scroll(drawer);
            WindowInsetsCompat original = ViewCompat.getRootWindowInsets(drawer);
            assertNotNull(original);
            Insets initial = original.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int baseBottom = drawer.getPaddingBottom() + scroll.getPaddingBottom() - initial.bottom;
            int baseTop = drawer.getPaddingTop() + scroll.getPaddingTop() - initial.top;
            int baseRight = drawer.getPaddingRight() + scroll.getPaddingRight() - initial.right;
            for (int bottom : new int[]{48, 48, 72, 0, 48}) {
                Insets bars = Insets.of(0, dp(activity, 32), dp(activity, 24), dp(activity, bottom));
                WindowInsetsCompat insets = new WindowInsetsCompat.Builder(original)
                        .setInsets(WindowInsetsCompat.Type.systemBars(), bars)
                        .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.NONE).build();
                ViewCompat.dispatchApplyWindowInsets(drawer, insets);
                assertEquals(baseBottom + bars.bottom, drawer.getPaddingBottom() + scroll.getPaddingBottom());
                assertEquals(baseTop + bars.top, drawer.getPaddingTop() + scroll.getPaddingTop());
                assertEquals(baseRight + bars.right, drawer.getPaddingRight() + scroll.getPaddingRight());
            }
            ViewCompat.dispatchApplyWindowInsets(drawer, original);
        }));
    }

    @Test public void lastMenuCanBeTappedAfterRecreation() throws Exception {
        withDrawer(scenario -> {
            scenario.recreate();
            open(scenario);
            reveal(scenario, R.id.item_collection_manager);
            AtomicReference<Rect> target = new AtomicReference<>();
            check(scenario, activity -> {
                assertFullyVisible(activity, R.id.item_collection_manager);
                target.set(bounds(activity.findViewById(R.id.item_collection_manager)));
            });
            screenshot(scenario, "drawer-bottom.png");
            AtomicReference<Intent> destination = new AtomicReference<>();
            Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
                @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                    if (intent.getComponent() != null && ManageCollectionActivity.class.getName()
                            .equals(intent.getComponent().getClassName())) {
                        destination.set(new Intent(intent));
                        return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
                    }
                    return null;
                }
            };
            instrumentation().addMonitor(monitor);
            try {
                Rect rect = target.get();
                long downTime = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
                        rect.centerX(), rect.centerY(), 0);
                MotionEvent up = MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP,
                        rect.centerX(), rect.centerY(), 0);
                try {
                    instrumentation().sendPointerSync(down);
                    instrumentation().sendPointerSync(up);
                } finally { down.recycle(); up.recycle(); }
                instrumentation().waitForIdleSync();
                assertNotNull("A real tap must still open the collection manager", destination.get());
            } finally { instrumentation().removeMonitor(monitor); }
        });
    }

    private static void constrain(ActivityScenario<DesignActivity> scenario, boolean largeText) throws Exception {
        if (largeText) {
            check(scenario, activity -> scaleText(drawer(activity)));
            instrumentation().waitForIdleSync();
        }
        check(scenario, activity -> {
            DesignDrawer drawer = drawer(activity);
            Insets insets = ViewCompat.getRootWindowInsets(drawer).getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int tallest = 0;
            for (int id : ITEMS) {
                View item = activity.findViewById(id);
                tallest = Math.max(tallest, ((View) item.getParent()).getHeight());
            }
            // Enough room for one complete row plus system bars. A fixed footer must not consume it.
            drawer.getLayoutParams().height = tallest + insets.top + insets.bottom + dp(activity, 14);
            drawer.requestLayout();
        });
        instrumentation().waitForIdleSync();
    }

    private static void assertEveryMenuReachable(ActivityScenario<DesignActivity> scenario) throws Exception {
        for (int id : ITEMS) {
            reveal(scenario, id);
            check(scenario, activity -> assertFullyVisible(activity, id));
        }
    }

    private static void reveal(ActivityScenario<DesignActivity> scenario, int id) throws Exception {
        check(scenario, activity -> {
            View item = activity.findViewById(id);
            ScrollView scroll = scroll(drawer(activity));
            View parent = item;
            while (parent.getParent() instanceof View view && parent != scroll) parent = view;
            if (parent == scroll) {
                ViewGroup content = (ViewGroup) scroll.getChildAt(0);
                Rect rect = new Rect(0, 0, item.getWidth(), item.getHeight());
                content.offsetDescendantRectToMyCoords(item, rect);
                int safeCenter = (scroll.getPaddingTop() + scroll.getHeight() - scroll.getPaddingBottom()) / 2;
                scroll.scrollTo(0, content.getTop() + rect.centerY() - safeCenter);
            }
        });
        instrumentation().waitForIdleSync();
    }

    private static void assertFullyVisible(DesignActivity activity, int id) {
        View item = activity.findViewById(id);
        Rect actual = bounds(item);
        Rect visible = new Rect();
        assertTrue("The menu must have a visible hit area", item.getGlobalVisibleRect(visible));
        assertTrue("The whole menu must be visible: item=" + actual + ", visible=" + visible,
                visible.contains(actual));
        DesignDrawer drawer = drawer(activity);
        ScrollView scroll = scroll(drawer);
        View parent = item;
        while (parent.getParent() instanceof View view && parent != scroll) parent = view;
        Rect safe;
        if (parent == scroll) {
            safe = bounds(scroll);
            safe.top += scroll.getPaddingTop();
            safe.bottom -= scroll.getPaddingBottom();
        } else {
            safe = bounds(drawer);
            safe.top += drawer.getPaddingTop();
            safe.bottom -= drawer.getPaddingBottom();
        }
        assertTrue("The menu must fit above system bars: item=" + actual + ", safe=" + safe,
                safe.contains(actual));
    }

    private static Rect bounds(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    private static void scaleText(View view) {
        if (view instanceof TextView text) text.setTextSize(TypedValue.COMPLEX_UNIT_PX, text.getTextSize() * 2);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) scaleText(group.getChildAt(i));
        }
    }

    private static void withDrawer(Body body) throws Exception {
        try (var project = new ProjectEditorPersistenceTest.Fixture()) {
            project.seed(false, false);
            try (var scenario = project.launch()) {
                open(scenario);
                body.run(scenario);
            }
        }
    }

    private static void open(ActivityScenario<DesignActivity> scenario) throws Exception {
        check(scenario, activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        ProjectEditorPersistenceTest.waitForEditor(scenario);
        check(scenario, activity -> ((DrawerLayout) activity.findViewById(R.id.drawer_layout))
                .openDrawer(GravityCompat.END, false));
        instrumentation().waitForIdleSync();
    }

    private static DesignDrawer drawer(DesignActivity activity) {
        DrawerLayout layout = activity.findViewById(R.id.drawer_layout);
        for (int i = 0; i < layout.getChildCount(); i++) {
            if (layout.getChildAt(i) instanceof DesignDrawer drawer) return drawer;
        }
        throw new AssertionError("Editor drawer missing");
    }

    private static ScrollView scroll(DesignDrawer drawer) { return (ScrollView) drawer.getChildAt(0); }
    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static void screenshot(ActivityScenario<DesignActivity> scenario, String name) throws Exception {
        CountDownLatch committed = new CountDownLatch(1);
        check(scenario, activity -> {
            View decor = activity.getWindow().getDecorView();
            decor.getViewTreeObserver().registerFrameCommitCallback(committed::countDown);
            decor.invalidate();
        });
        assertTrue("The screenshot must follow the resized frame", committed.await(3, TimeUnit.SECONDS));
        CountDownLatch presented = new CountDownLatch(1);
        check(scenario, activity -> {
            View decor = activity.getWindow().getDecorView();
            // Frame commit precedes SurfaceFlinger presentation; allow the buffer to be latched.
            decor.postOnAnimation(() -> decor.postOnAnimation(presented::countDown));
        });
        assertTrue("The committed frame must reach the display", presented.await(3, TimeUnit.SECONDS));
        SystemClock.sleep(300);
        Bitmap bitmap = instrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File directory = instrumentation().getTargetContext().getExternalFilesDir("drawer-regression");
        assertNotNull(directory);
        try (FileOutputStream out = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }

    private static Instrumentation instrumentation() { return InstrumentationRegistry.getInstrumentation(); }
    private interface Body { void run(ActivityScenario<DesignActivity> scenario) throws Exception; }
    private interface Check { void run(DesignActivity activity) throws Exception; }
    private static void check(ActivityScenario<DesignActivity> scenario, Check check) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        scenario.onActivity(activity -> {
            try { check.run(activity); } catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Exception exception) throw exception;
        if (error != null) throw new AssertionError(error);
    }
}
