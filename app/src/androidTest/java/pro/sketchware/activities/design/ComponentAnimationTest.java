package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager.widget.ViewPager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import pro.sketchware.R;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ProjectDataManager;

/** Checks the camera event buttons during transitions, not only after animations finish. */
@RunWith(AndroidJUnit4.class)
public class ComponentAnimationTest {
    @Test public void cameraEventsAreVisibleAsTheirAreaExpands() throws Exception {
        withCamera((fixture, scenario) -> {
            int[] exposedFrames = {0};
            ArrayList<String> blankFrames = new ArrayList<>();
            View[] root = {null};
            ViewTreeObserver.OnPreDrawListener[] observer = {null};
            scenario.onActivity(a -> {
                root[0] = row(a);
                View options = root[0].findViewById(R.id.component_option_layout);
                RecyclerView events = root[0].findViewById(R.id.component_events);
                assertEquals("Camera must expose both available events", 2, events.getAdapter().getItemCount());
                observer[0] = () -> {
                    if (options.getVisibility() == View.VISIBLE && events.getHeight() > 0
                            && options.getHeight() >= events.getHeight()) {
                        exposedFrames[0]++;
                        View first = events.getChildCount() == 0 ? null : events.getChildAt(0);
                        Rect visible = new Rect();
                        if (events.getAlpha() < 0.99f || Math.abs(events.getTranslationX()) > 1
                                || first == null || !first.getGlobalVisibleRect(visible)
                                || visible.width() < first.getWidth() - 1) {
                            blankFrames.add("height=" + options.getHeight() + ", alpha=" + events.getAlpha()
                                    + ", x=" + events.getTranslationX() + ", children=" + events.getChildCount());
                        }
                    }
                    return true;
                };
                root[0].getViewTreeObserver().addOnPreDrawListener(observer[0]);
                root[0].performClick();
            });
            SystemClock.sleep(400);
            scenario.onActivity(a -> {
                root[0].getViewTreeObserver().removeOnPreDrawListener(observer[0]);
                assertTrue("The expanded event area must be observed on screen", exposedFrames[0] > 0);
                assertTrue("Revealed space must already contain event buttons: " + blankFrames, blankFrames.isEmpty());
                assertExpanded(a);
            });
        });
    }

    @Test public void reversingCameraCollapseKeepsContentAndPreviewContinuous() throws Exception {
        withCamera((fixture, scenario) -> {
            scenario.onActivity(a -> row(a).performClick());
            SystemClock.sleep(300);
            CountDownLatch reversed = new CountDownLatch(1);
            float[] before = new float[4], after = new float[4];
            scenario.onActivity(a -> {
                View item = row(a);
                item.performClick();
                item.postDelayed(() -> {
                    readState(item, before);
                    item.performClick();
                    readState(item, after);
                    reversed.countDown();
                }, 60);
            });
            assertTrue("Reversal did not run", reversed.await(5, TimeUnit.SECONDS));
            assertTrue("The preview must be sampled while fading", before[0] > 0 && before[0] < 1);
            assertArrayEquals("Reversal must continue from the current geometry and opacity", before, after, 1f / 100f);
            SystemClock.sleep(300);
            scenario.onActivity(ComponentAnimationTest::assertExpanded);
        });
    }

    @Test public void rebindingCollapsedCameraCancelsPendingPreviewFade() throws Exception {
        withCamera((fixture, scenario) -> {
            scenario.onActivity(a -> {
                View item = row(a);
                item.performClick();
                item.postDelayed(() -> {
                    camera(fixture.id).isCollapsed = true;
                    list(a).getAdapter().notifyItemChanged(0);
                }, 60);
            });
            SystemClock.sleep(500);
            scenario.onActivity(a -> {
                View item = row(a);
                assertEquals(View.GONE, item.findViewById(R.id.component_option_layout).getVisibility());
                assertEquals(1f, item.findViewById(R.id.events_preview).getAlpha(), 0.01f);
                assertEquals(0f, item.findViewById(R.id.events_preview).getTranslationX(), 0.01f);
                assertEquals(0f, item.findViewById(R.id.img_menu).getRotation(), 0.01f);
            });
        });
    }

    @Test public void rebindingExpandedCameraCancelsPendingPreviewFade() throws Exception {
        withCamera((fixture, scenario) -> {
            scenario.onActivity(a -> row(a).performClick());
            SystemClock.sleep(300);
            scenario.onActivity(a -> {
                View item = row(a);
                item.performClick();
                item.postDelayed(() -> {
                    camera(fixture.id).isCollapsed = false;
                    list(a).getAdapter().notifyItemChanged(0);
                }, 60);
            });
            SystemClock.sleep(500);
            scenario.onActivity(ComponentAnimationTest::assertExpanded);
        });
    }

    private static void readState(View item, float[] state) {
        View preview = item.findViewById(R.id.events_preview);
        View events = item.findViewById(R.id.component_events);
        state[0] = preview.getAlpha();
        state[1] = events.getAlpha();
        state[2] = events.getTranslationX();
        state[3] = item.findViewById(R.id.component_option_layout).getHeight();
    }

    private static void assertExpanded(DesignActivity activity) {
        View item = row(activity);
        View options = item.findViewById(R.id.component_option_layout);
        RecyclerView events = item.findViewById(R.id.component_events);
        assertEquals(View.VISIBLE, options.getVisibility());
        assertTrue(options.getHeight() > 0);
        assertEquals(1f, events.getAlpha(), 0.01f);
        assertEquals(0f, events.getTranslationX(), 0.01f);
        assertEquals(0f, item.findViewById(R.id.events_preview).getAlpha(), 0.01f);
        assertEquals(0f, item.findViewById(R.id.events_preview).getTranslationX(), 0.01f);
        assertEquals(-180f, item.findViewById(R.id.img_menu).getRotation(), 0.01f);
        assertEquals(2, events.getAdapter().getItemCount());
    }

    private static RecyclerView list(DesignActivity activity) { return activity.findViewById(R.id.component_list); }

    private static View row(DesignActivity activity) {
        RecyclerView.ViewHolder holder = list(activity).findViewHolderForAdapterPosition(0);
        assertNotNull("Camera row must be attached", holder);
        return holder.itemView;
    }

    private static ComponentBean camera(String projectId) {
        return ProjectDataManager.getProjectDataManager(projectId).getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0);
    }

    private static void withCamera(CameraCheck check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            ProjectDataManager.getProjectDataManager(fixture.id).addComponent(ProjectFileBean.DEFAULT_JAVA_NAME,
                    ComponentBean.COMPONENT_TYPE_CAMERA, "animationCamera");
            assertTrue(ProjectDataManager.getProjectDataManager(fixture.id).saveAllData());
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(a -> {
                    a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    ((ViewPager) a.findViewById(R.id.viewpager)).setCurrentItem(2, false);
                });
                long deadline = SystemClock.uptimeMillis() + 10000;
                boolean[] attached = {false};
                while (SystemClock.uptimeMillis() < deadline && !attached[0]) {
                    scenario.onActivity(a -> attached[0] = list(a).findViewHolderForAdapterPosition(0) != null);
                    if (!attached[0]) SystemClock.sleep(30);
                }
                assertTrue("Camera row did not load", attached[0]);
                check.run(fixture, scenario);
            }
        }
    }

    private interface CameraCheck {
        void run(ProjectEditorPersistenceTest.Fixture fixture, ActivityScenario<DesignActivity> scenario) throws Exception;
    }
}
