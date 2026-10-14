package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inspector.WindowInspector;
import android.widget.EditText;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.ColorModel;
import pro.sketchware.widgets.ExpandableImageLayout;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ColorHelpAnimationTest {
    @Test public void revealKeepsSurfaceStableAndBackgroundFittedToContent() throws Exception {
        withEditor(scenario -> {
            openDialog(scenario);
            ArrayList<int[]> frames = new ArrayList<>();
            View[] root = {null};
            int[] initialSurfaceHeight = {0};
            ViewTreeObserver.OnPreDrawListener[] listener = {null};
            scenario.onActivity(a -> {
                root[0] = dialogRoot();
                initialSurfaceHeight[0] = root[0].getHeight();
                listener[0] = () -> {
                    View guide = root[0].findViewById(R.id.color_guide);
                    int fullHeight = root[0].findViewById(R.id.img_color_guide).getHeight();
                    if (guide.getHeight() > 0 && guide.getHeight() < fullHeight) {
                        View panel = root[0].findViewById(androidx.appcompat.R.id.parentPanel);
                        int[] location = new int[2];
                        panel.getLocationInWindow(location);
                        Outline outline = new Outline();
                        Rect background = new Rect();
                        root[0].getBackground().getOutline(outline);
                        boolean hasOutline = outline.getRect(background);
                        frames.add(new int[]{root[0].getHeight(), background.top, background.bottom,
                                location[1], location[1] + panel.getHeight(), hasOutline ? 1 : 0});
                    }
                    return true;
                };
                root[0].getViewTreeObserver().addOnPreDrawListener(listener[0]);
                root[0].findViewById(R.id.importantNote).performClick();
            });
            SystemClock.sleep(350);
            scenario.onActivity(a -> {
                root[0].getViewTreeObserver().removeOnPreDrawListener(listener[0]);
                assertTrue("Reveal must contain several intermediate frames", frames.size() >= 3);
                for (int[] frame : frames) {
                    assertEquals("Dialog background must retain a rounded outline", 1, frame[5]);
                    assertEquals("Window surface must stay at its initial height throughout reveal", initialSurfaceHeight[0], frame[0]);
                    assertTrue("Background top must follow the dialog content", Math.abs(frame[1] - frame[3]) <= 1);
                    assertTrue("Background bottom must follow the dialog content", Math.abs(frame[2] - frame[4]) <= 1);
                }
            });
        });
    }

    @Test public void outsideTapDuringRevealCancelsDialogAndKeepsColor() throws Exception {
        withEditor(scenario -> {
            openDialog(scenario);
            View[] previous = {null};
            boolean[] hasOutline = {false};
            int[] emptyHeight = {0};
            CountDownLatch tapped = new CountDownLatch(1);
            scenario.onActivity(a -> {
                View root = dialogRoot();
                previous[0] = root;
                root.findViewById(R.id.importantNote).performClick();
                root.postDelayed(() -> {
                    Outline outline = new Outline();
                    Rect bounds = new Rect();
                    root.getBackground().getOutline(outline);
                    hasOutline[0] = outline.getRect(bounds);
                    emptyHeight[0] = bounds.top;
                    if (hasOutline[0] && bounds.top > 2) {
                        long now = SystemClock.uptimeMillis();
                        for (int action : new int[]{MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                            MotionEvent event = MotionEvent.obtain(now, now, action, root.getWidth() / 2f, bounds.top / 2f, 0);
                            root.dispatchTouchEvent(event);
                            event.recycle();
                        }
                    }
                    tapped.countDown();
                }, 60);
            });
            assertTrue(tapped.await(5, TimeUnit.SECONDS));
            assertTrue("Background must expose its visible bounds", hasOutline[0]);
            assertTrue("Reveal should have a transparent area above its visible background", emptyHeight[0] > 2);
            SystemClock.sleep(300);
            scenario.onActivity(a -> {
                assertFalse("Tap outside the visible dialog must dismiss it during reveal", previous[0].isAttachedToWindow());
                assertEquals("#007FAC", color(a).getColorValue());
                assertFalse(a.colorsEditor.hasUnsavedChanges);
            });
        });
    }

    @Test public void revealingGuideKeepsItsNaturalSizeAndHelpPosition() throws Exception {
        withEditor(scenario -> {
            openDialog(scenario);
            ArrayList<int[]> frames = new ArrayList<>();
            View[] window = {null};
            ViewTreeObserver.OnPreDrawListener[] observer = {null};
            int[] originalHelpY = {0};
            scenario.onActivity(a -> {
                window[0] = dialogRoot();
                originalHelpY[0] = centerY(window[0].findViewById(R.id.help));
                observer[0] = () -> {
                    View guide = window[0].findViewById(R.id.color_guide);
                    View image = window[0].findViewById(R.id.img_color_guide);
                    if (guide.getHeight() > 0) frames.add(new int[]{guide.getHeight(), image.getHeight(),
                            centerY(window[0].findViewById(R.id.help))});
                    return true;
                };
                window[0].getViewTreeObserver().addOnPreDrawListener(observer[0]);
                window[0].findViewById(R.id.importantNote).performClick();
            });
            SystemClock.sleep(400);
            scenario.onActivity(a -> {
                window[0].getViewTreeObserver().removeOnPreDrawListener(observer[0]);
                assertTrue("Guide must be observed during its reveal", frames.size() > 1);
                int naturalHeight = window[0].findViewById(R.id.img_color_guide).getHeight();
                boolean partialFrame = false;
                for (int[] frame : frames) {
                    assertEquals("Image must not shrink as its container grows", naturalHeight, frame[1]);
                    partialFrame |= frame[0] < naturalHeight;
                    assertTrue("Questionmark must not jump ahead of its card: original=" + originalHelpY[0]
                                    + ", actual=" + frame[2] + ", guideHeight=" + frame[0],
                            Math.abs(frame[2] - originalHelpY[0]) < Math.max(12, naturalHeight / 6));
                }
                assertTrue("The animation must include a partially revealed image", partialFrame);
                assertEquals(naturalHeight, window[0].findViewById(R.id.color_guide).getHeight());
            });
        });
    }

    @Test public void rapidHelpTogglesContinueFromCurrentHeightAndKeepInputs() throws Exception {
        withEditor(scenario -> {
            openDialog(scenario);
            CountDownLatch reversed = new CountDownLatch(1);
            int[] before = new int[2], after = new int[2];
            scenario.onActivity(a -> {
                View root = dialogRoot();
                ((EditText) root.findViewById(R.id.color_value_input)).setText("#445566");
                ((EditText) root.findViewById(R.id.string_header_input)).setText("draft note");
                ExpandableImageLayout guide = root.findViewById(R.id.color_guide);
                root.findViewById(R.id.importantNote).performClick();
                root.postDelayed(() -> {
                    before[0] = guide.getHeight();
                    root.findViewById(R.id.importantNote).performClick();
                    after[0] = guide.getLayoutParams().height;
                    root.postDelayed(() -> {
                        before[1] = guide.getHeight();
                        root.findViewById(R.id.importantNote).performClick();
                        after[1] = guide.getLayoutParams().height;
                        reversed.countDown();
                    }, 50);
                }, 70);
            });
            assertTrue(reversed.await(5, TimeUnit.SECONDS));
            assertTrue("First toggle must reverse a partially open guide", before[0] > 0);
            assertArrayEquals("Reversal must start at the visible height", before, after);
            SystemClock.sleep(350);
            scenario.onActivity(a -> {
                View root = dialogRoot();
                ExpandableImageLayout guide = root.findViewById(R.id.color_guide);
                assertTrue(guide.isExpanded());
                assertEquals(root.findViewById(R.id.img_color_guide).getHeight(), guide.getHeight());
                assertEquals("#445566", ((EditText) root.findViewById(R.id.color_value_input)).getText().toString());
                assertEquals("draft note", ((EditText) root.findViewById(R.id.string_header_input)).getText().toString());
                root.findViewById(R.id.importantNote).performClick();
            });
            SystemClock.sleep(300);
            scenario.onActivity(a -> assertEquals(View.GONE, dialogRoot().findViewById(R.id.color_guide).getVisibility()));
        });
    }

    @Test public void cancellingDialogDuringRevealStopsAnimationAndKeepsColor() throws Exception {
        withEditor(scenario -> {
            openDialog(scenario);
            ExpandableImageLayout[] previous = {null};
            scenario.onActivity(a -> {
                View root = dialogRoot();
                previous[0] = root.findViewById(R.id.color_guide);
                root.findViewById(R.id.importantNote).performClick();
                root.findViewById(android.R.id.button2).performClick();
            });
            SystemClock.sleep(350);
            scenario.onActivity(a -> {
                assertFalse(previous[0].isAttachedToWindow());
                assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, previous[0].getLayoutParams().height);
                assertEquals("#007FAC", color(a).getColorValue());
                assertFalse(a.colorsEditor.hasUnsavedChanges);
            });
            openDialog(scenario);
            scenario.onActivity(a -> assertEquals(View.GONE, dialogRoot().findViewById(R.id.color_guide).getVisibility()));
        });
    }

    @Test public void landscapeGuideScrollsWhileDialogActionsStayVisible() throws Exception {
        withEditor(scenario -> {
            scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            awaitEditor(scenario, Configuration.ORIENTATION_LANDSCAPE);
            openDialog(scenario);
            scenario.onActivity(a -> dialogRoot().findViewById(R.id.importantNote).performClick());
            SystemClock.sleep(350);
            scenario.onActivity(a -> {
                View root = dialogRoot();
                View guide = root.findViewById(R.id.color_guide);
                assertEquals(View.VISIBLE, guide.getVisibility());
                NestedScrollView scroll = (NestedScrollView) guide.getParent().getParent().getParent().getParent().getParent();
                scroll.fullScroll(View.FOCUS_DOWN);
                for (int id : new int[]{android.R.id.button1, android.R.id.button2}) {
                    android.graphics.Rect visible = new android.graphics.Rect();
                    View button = root.findViewById(id);
                    assertTrue("Dialog action must remain reachable", button.getGlobalVisibleRect(visible));
                    assertTrue(visible.height() >= button.getHeight());
                }
                assertTrue("Overflowing form must be scrollable", scroll.canScrollVertically(-1) || scroll.canScrollVertically(1));
            });
        });
    }

    private static int centerY(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1] + view.getHeight() / 2;
    }

    private static ColorModel color(ResourcesEditorActivity a) {
        return a.colorsEditor.colorList.stream().filter(c -> "colorPrimaryDark".equals(c.getColorName())).findFirst().orElseThrow();
    }

    private static void openDialog(ActivityScenario<ResourcesEditorActivity> scenario) {
        scenario.onActivity(a -> a.colorsEditor.showColorEditDialog(color(a), a.colorsEditor.colorList.indexOf(color(a))));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(150);
    }

    private static View dialogRoot() {
        for (View root : WindowInspector.getGlobalWindowViews()) if (root.findViewById(R.id.color_guide) != null) return root;
        throw new AssertionError("Color editor dialog missing");
    }

    private static void awaitEditor(ActivityScenario<ResourcesEditorActivity> scenario, int orientation) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = a.colorsEditor.adapter != null && a.dimensEditor.adapter != null
                    && a.getResources().getConfiguration().orientation == orientation);
            if (ready[0]) return;
            SystemClock.sleep(30);
        }
        fail("Resource editor did not become ready");
    }

    private static void withEditor(EditorCheck check) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            Method seed = ResourceEditorLifecycleTest.class.getDeclaredMethod("seedResources", String.class, String.class, String.class);
            seed.setAccessible(true);
            seed.invoke(null, fixture.id, "", "base");
            File file = new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, "", "colors.xml"));
            StringBuilder xml = new StringBuilder("<resources>");
            for (String name : new String[]{"colorPrimaryDark", "colorPrimary", "colorAccent", "colorControlHighlight", "colorControlNormal"})
                xml.append("<color name=\"").append(name).append("\">#007FAC</color>");
            Files.write(file.toPath(), xml.append("</resources>").toString().getBytes(StandardCharsets.UTF_8));
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ResourcesEditorActivity.class)
                    .putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                awaitEditor(scenario, Configuration.ORIENTATION_PORTRAIT);
                check.run(scenario);
            }
        }
    }

    private interface EditorCheck { void run(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception; }
}
