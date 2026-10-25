package pro.sketchware.activities.main.fragments.projects;

import static org.junit.Assert.*;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ProjectOptionsBottomSheetTest {
    @Test(timeout = 15000)
    public void opensWithActionsVisible() throws Exception {
        checkOptions(false);
    }

    @Test(timeout = 15000)
    public void shortWideWindowKeepsAllActionsReachable() throws Exception {
        checkOptions(true);
    }

    // Attach only a lifecycle host; do not load or modify the user's projects.
    public static class TestProjectsFragment extends ProjectsFragment {
        @Override public void onCreate(Bundle state) {
            super.onCreate(state);
            try {
                var executor = ProjectsFragment.class.getDeclaredField("executorService");
                executor.setAccessible(true);
                executor.set(this, Executors.newSingleThreadExecutor());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            return null;
        }
    }

    private static void checkOptions(boolean constrained) throws Exception {
        Method show = ProjectsAdapter.class.getDeclaredMethod("showProjectOptionsBottomSheet", HashMap.class, int.class);
        show.setAccessible(true);
        AtomicReference<View> dialogRoot = new AtomicReference<>();
        try (var scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            try {
                scenario.onActivity(activity -> {
                    TestProjectsFragment fragment = new TestProjectsFragment();
                    activity.getSupportFragmentManager().beginTransaction().add(fragment, "options_test").commitNow();
                    HashMap<String, Object> project = new HashMap<>();
                    project.put("sc_id", "__options_display_test__");
                    project.put("my_ws_name", "Project options");
                    try {
                        show.invoke(new ProjectsAdapter(fragment, List.of(project)), project, 0);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                    }
                    View root = WindowInspector.getGlobalWindowViews().stream()
                            .filter(view -> view.findViewById(R.id.project_delete) != null)
                            .findFirst().orElseThrow();
                    dialogRoot.set(root);
                    if (constrained) {
                        float density = activity.getResources().getDisplayMetrics().density;
                        WindowManager.LayoutParams params = (WindowManager.LayoutParams) root.getLayoutParams();
                        params.width = Math.round(720 * density);
                        params.height = Math.round(320 * density);
                        activity.getWindowManager().updateViewLayout(root, params);
                    }
                });
                SystemClock.sleep(500);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    View root = dialogRoot.get();
                    View sheet = root.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    assertEquals("Opening the menu must reveal its actions", BottomSheetBehavior.STATE_EXPANDED,
                            BottomSheetBehavior.from(sheet).getState());
                    assertFullyVisible(root.findViewById(R.id.pin_project));
                    NestedScrollView content = (NestedScrollView) root.findViewById(R.id.project_delete).getParent().getParent();
                    if (constrained) assertTrue("Short windows must allow scrolling", content.canScrollVertically(1));
                    content.scrollTo(0, content.getChildAt(0).getHeight());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> assertFullyVisible(dialogRoot.get().findViewById(R.id.project_delete)));
            } finally {
                scenario.onActivity(activity -> {
                    if (dialogRoot.get() != null) {
                        dialogRoot.get().findViewById(com.google.android.material.R.id.touch_outside).performClick();
                    }
                });
            }
        }
    }

    private static void assertFullyVisible(View view) {
        Rect visible = new Rect();
        int[] location = new int[2];
        view.getLocationInWindow(location);
        Rect bounds = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        assertTrue("Action must have a visible hit area", view.getGlobalVisibleRect(visible));
        assertTrue("Action must not be clipped: " + visible + " / " + bounds, visible.contains(bounds));
        assertTrue(view.getWidth() > 0 && view.getHeight() > 0);
    }
}
