package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ProjectDataManager;

/** Checks the real sliding menu and canvas together using a disposable project. */
@RunWith(AndroidJUnit4.class)
public class LogicPaletteAnimationTest {
    @Test public void portraitAnimationKeepsCanvasBehindMenu() throws Exception {
        withEditor(scenario -> {
            orient(scenario, false);
            checkAnimation(scenario, true, false);
            checkAnimation(scenario, false, false);
        });
    }

    @Test public void repeatedLayoutDoesNotShrinkOpenCanvas() throws Exception {
        withEditor(scenario -> {
            orient(scenario, false);
            scenario.onActivity(a -> a.togglePaletteVisibility(true));
            settle(scenario);
            int[] initial = new int[2];
            scenario.onActivity(a -> {
                View editor = a.findViewById(R.id.editor);
                initial[0] = editor.getWidth(); initial[1] = editor.getHeight();
                for (int i = 0; i < 4; i++) a.layoutEditorForOrientation(Configuration.ORIENTATION_PORTRAIT);
            });
            settle(scenario);
            scenario.onActivity(a -> {
                View editor = a.findViewById(R.id.editor);
                assertEquals(initial[0], editor.getWidth());
                assertEquals(initial[1], editor.getHeight());
                assertEdgesMeet(a, false);
            });
            for (int i = 0; i < 3; i++) {
                checkAnimation(scenario, false, false);
                checkAnimation(scenario, true, false);
            }
        });
    }

    @Test public void reversingOpeningAnimationLeavesFullCanvas() throws Exception {
        withEditor(scenario -> {
            orient(scenario, false);
            scenario.onActivity(a -> a.togglePaletteVisibility(true));
            SystemClock.sleep(100);
            scenario.onActivity(a -> a.togglePaletteVisibility(false));
            settle(scenario);
            scenario.onActivity(a -> assertFullCanvas(a, false));
            checkAnimation(scenario, true, false);
        });
    }

    @Test public void rotationDuringOpeningSettlesAndLandscapeHasNoGap() throws Exception {
        withEditor(scenario -> {
            orient(scenario, false);
            scenario.onActivity(a -> {
                a.togglePaletteVisibility(true);
                a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            awaitOrientation(scenario, true);
            settle(scenario);
            scenario.onActivity(a -> assertEdgesMeet(a, true));
            checkAnimation(scenario, false, true);
            checkAnimation(scenario, true, true);
            orient(scenario, false);
            scenario.onActivity(a -> assertEdgesMeet(a, false));
            checkAnimation(scenario, false, false);
        });
    }

    @Test public void rapidReversalsKeepCurrentMenuPosition() throws Exception {
        withEditor(scenario -> {
            for (boolean landscape : new boolean[]{false, true}) {
                orient(scenario, landscape);
                scenario.onActivity(a -> a.togglePaletteVisibility(true));
                settle(scenario);
                scenario.onActivity(a -> a.togglePaletteVisibility(false));
                settle(scenario);
                scenario.onActivity(a -> a.togglePaletteVisibility(true));
                settle(scenario);
                scenario.onActivity(a -> a.togglePaletteVisibility(false));
                SystemClock.sleep(100);
                scenario.onActivity(a -> {
                    View palette = a.findViewById(R.id.area_palette);
                    int[] before = new int[2]; int[] after = new int[2];
                    palette.getLocationOnScreen(before);
                    a.findViewById(R.id.fab_toggle_palette).performClick();
                    palette.getLocationOnScreen(after);
                    assertEquals("Reopening a moving menu must continue from its current position",
                            landscape ? before[0] : before[1], landscape ? after[0] : after[1]);
                });
                SystemClock.sleep(100);
                scenario.onActivity(a -> {
                    View palette = a.findViewById(R.id.area_palette);
                    int[] before = new int[2]; int[] after = new int[2];
                    palette.getLocationOnScreen(before);
                    a.findViewById(R.id.fab_toggle_palette).performClick();
                    palette.getLocationOnScreen(after);
                    assertEquals("Closing a moving menu must continue from its current position",
                            landscape ? before[0] : before[1], landscape ? after[0] : after[1]);
                });
                settle(scenario);
                scenario.onActivity(a -> assertFullCanvas(a, landscape));
            }
        });
    }

    private static void checkAnimation(ActivityScenario<LogicEditorActivity> scenario,
                                       boolean show, boolean landscape) {
        List<String> gaps = new ArrayList<>();
        int[] frames = {0};
        ViewTreeObserver.OnPreDrawListener[] listener = new ViewTreeObserver.OnPreDrawListener[1];
        scenario.onActivity(a -> {
            View editor = a.findViewById(R.id.editor);
            View palette = a.findViewById(R.id.area_palette);
            listener[0] = () -> {
                int[] e = new int[2]; int[] p = new int[2];
                editor.getLocationOnScreen(e); palette.getLocationOnScreen(p);
                int end = landscape ? e[0] + editor.getWidth() : e[1] + editor.getHeight();
                int start = landscape ? p[0] : p[1];
                View parent = (View) editor.getParent();
                int[] pos = new int[2]; parent.getLocationOnScreen(pos);
                int limit = landscape ? pos[0] + parent.getWidth() : pos[1] + parent.getHeight();
                int exposed = Math.min(start, limit) - end;
                if (exposed > 1) gaps.add("frame=" + frames[0] + ", gap=" + exposed);
                frames[0]++;
                return true;
            };
            editor.getViewTreeObserver().addOnPreDrawListener(listener[0]);
            // Use the same button as the user.
            a.findViewById(R.id.fab_toggle_palette).performClick();
        });
        settle(scenario);
        scenario.onActivity(a -> {
            a.findViewById(R.id.editor).getViewTreeObserver().removeOnPreDrawListener(listener[0]);
            assertTrue("The animation should draw multiple frames", frames[0] > 2);
            assertTrue("Canvas must cover the exposed area while the palette slides: " + gaps, gaps.isEmpty());
            if (show) assertEdgesMeet(a, landscape); else assertFullCanvas(a, landscape);
        });
    }

    private static void assertEdgesMeet(LogicEditorActivity a, boolean landscape) {
        View editor = a.findViewById(R.id.editor); View palette = a.findViewById(R.id.area_palette);
        int[] e = new int[2]; int[] p = new int[2];
        editor.getLocationOnScreen(e); palette.getLocationOnScreen(p);
        assertEquals("Open palette should meet the canvas without a gap or overlap",
                landscape ? p[0] : p[1], landscape ? e[0] + editor.getWidth() : e[1] + editor.getHeight());
    }

    private static void assertFullCanvas(LogicEditorActivity a, boolean landscape) {
        View editor = a.findViewById(R.id.editor); View parent = (View) editor.getParent();
        int[] e = new int[2]; int[] p = new int[2];
        editor.getLocationOnScreen(e); parent.getLocationOnScreen(p);
        assertEquals(landscape ? p[0] + parent.getWidth() : p[1] + parent.getHeight(),
                landscape ? e[0] + editor.getWidth() : e[1] + editor.getHeight());
    }

    private static void settle(ActivityScenario<LogicEditorActivity> scenario) {
        SystemClock.sleep(650);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void orient(ActivityScenario<LogicEditorActivity> scenario, boolean landscape) {
        scenario.onActivity(a -> a.setRequestedOrientation(landscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        awaitOrientation(scenario, landscape);
        settle(scenario);
    }

    private static void awaitOrientation(ActivityScenario<LogicEditorActivity> scenario, boolean landscape) {
        int expected = landscape ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = a.getResources().getConfiguration().orientation == expected
                    && a.findViewById(R.id.editor).getWidth() > 0);
            if (ready[0]) return;
            SystemClock.sleep(30);
        }
        fail("Orientation did not settle");
    }

    private static void withEditor(Check check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LogicEditorActivity.class)
                    .putExtra("sc_id", fixture.id).putExtra("id", "onCreate").putExtra("event", "initializeLogic")
                    .putExtra("event_text", "Palette animation test")
                    .putExtra("project_file", new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
            try (ActivityScenario<LogicEditorActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
                settle(scenario);
                check.run(scenario);
            }
        }
    }

    private interface Check { void run(ActivityScenario<LogicEditorActivity> scenario) throws Exception; }
}
