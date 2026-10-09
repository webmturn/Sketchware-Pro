package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;

/** Expected pixels come from actual AAPT2 dimensions, through the public ViewPane paths. */
@RunWith(AndroidJUnit4.class)
public class PreviewLayoutParamsTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] VALUES = {0, 1, 3, 4, 5, 6, 7, 9, 11, 13, 15, 16, 17, 999};
    private static final int[] PARENTS = {ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
            ViewBean.VIEW_TYPE_LAYOUT_RELATIVE, ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW};
    private static String projectId;

    @Test
    public void ordinaryLayoutParamsMatchCompiledDimensions() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Environment environment = new Environment(dpi);
                ViewPane pane = environment.pane();
                for (int parent : PARENTS) {
                    for (int type : new int[]{ViewBean.VIEW_TYPE_LAYOUT_LINEAR, ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW}) {
                        for (int i = 0; i < VALUES.length; i++) {
                            int[] dp = new int[6];
                            int[] expected = new int[6];
                            for (int side = 0; side < dp.length; side++) {
                                dp[side] = VALUES[(i + side) % VALUES.length];
                                expected[side] = environment.pixels(dp[side]);
                            }
                            ViewBean bean = bean(type, parent, dp);
                            View view = pane.createItemView(bean);
                            assertArrayEquals("dpi=" + dpi + ", parent=" + parent + ", type=" + type,
                                    expected, params(view));
                            checks++;
                        }
                    }
                }
            }
            status("f17_ordinary_parameter_checks", checks);
        });
    }

    @Test
    public void layoutModesAndParentPropertiesRemainUnchanged() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                ViewPane pane = new Environment(dpi).pane();
                for (int parent : PARENTS) {
                    for (int[] sizes : new int[][]{{-1, -2}, {-2, -1}, {0, 0}}) {
                        ViewBean bean = bean(ViewBean.VIEW_TYPE_LAYOUT_LINEAR, parent,
                                new int[]{sizes[0], sizes[1], 0, 0, 0, 0});
                        bean.layout.weight = 2;
                        bean.layout.layoutGravity = Gravity.CENTER_HORIZONTAL;
                        LayoutBean original = new LayoutBean();
                        original.copy(bean.layout);
                        View view = pane.createItemView(bean);
                        assertArrayEquals(new int[]{sizes[0], sizes[1], 0, 0, 0, 0}, params(view));
                        if (parent == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                            LinearLayout.LayoutParams parameters = (LinearLayout.LayoutParams) view.getLayoutParams();
                            assertEquals(2f, parameters.weight, 0f);
                            assertEquals(Gravity.CENTER_HORIZONTAL, parameters.gravity);
                        } else if (parent == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
                            assertTrue(view.getLayoutParams() instanceof RelativeLayout.LayoutParams);
                        } else {
                            FrameLayout.LayoutParams parameters = (FrameLayout.LayoutParams) view.getLayoutParams();
                            assertEquals(Gravity.CENTER_HORIZONTAL, parameters.gravity);
                        }
                        assertTrue("Preview must not rewrite layout metadata", original.isEqual(bean.layout));
                        checks++;
                    }
                }
            }
            status("f17_layout_mode_checks", checks);
        });
    }

    @Test
    public void fabMarginsMatchCompiledDimensionsAndKeepPlacement() {
        onMain(() -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Environment environment = new Environment(dpi);
                ViewPane pane = environment.pane();
                for (int value : VALUES) {
                    ViewBean bean = bean(ViewBean.VIEW_TYPE_WIDGET_FAB, ViewBean.VIEW_TYPE_LAYOUT_RELATIVE,
                            new int[]{4, 6, value, value, value, value});
                    bean.id = "_fab";
                    bean.layout.layoutGravity = Gravity.RIGHT | Gravity.BOTTOM;
                    View view = pane.addFab(bean);
                    int margin = environment.pixels(value);
                    assertArrayEquals("FAB at " + dpi + " dpi, margin=" + value,
                            new int[]{-2, -2, margin, margin, margin, margin}, params(view));
                    assertPlacement(view);
                    pane.removeView(view);
                    checks++;
                }
                View defaultFab = pane.addFab(null);
                int margin = environment.pixels(16);
                assertArrayEquals(new int[]{-2, -2, margin, margin, margin, margin}, params(defaultFab));
                assertPlacement(defaultFab);
                checks++;
            }
            status("f17_fab_parameter_checks", checks);
        });
    }

    private static void assertPlacement(View view) {
        RelativeLayout.LayoutParams parameters = (RelativeLayout.LayoutParams) view.getLayoutParams();
        assertEquals(RelativeLayout.TRUE, parameters.getRule(RelativeLayout.ALIGN_PARENT_RIGHT));
        assertEquals(RelativeLayout.TRUE, parameters.getRule(RelativeLayout.ALIGN_PARENT_BOTTOM));
    }

    private static ViewBean bean(int type, int parent, int[] dp) {
        ViewBean bean = new ViewBean("subject", type);
        bean.parent = "parent";
        bean.parentType = parent;
        bean.layout.width = dp[0];
        bean.layout.height = dp[1];
        bean.layout.marginLeft = dp[2];
        bean.layout.marginTop = dp[3];
        bean.layout.marginRight = dp[4];
        bean.layout.marginBottom = dp[5];
        bean.image.resName = "NONE";
        bean.text.text = "";
        return bean;
    }

    private static int[] params(View view) {
        ViewGroup.MarginLayoutParams parameters = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return new int[]{parameters.width, parameters.height, parameters.leftMargin,
                parameters.topMargin, parameters.rightMargin, parameters.bottomMargin};
    }

    private static final class Environment {
        final Context preview;
        final Context oracle;

        Environment(int dpi) {
            preview = new ContextThemeWrapper(configured(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), dpi), R.style.Theme_SketchwarePro);
            oracle = configured(InstrumentationRegistry.getInstrumentation().getContext(), dpi);
            assertEquals(dpi, preview.getResources().getConfiguration().densityDpi);
            assertEquals(preview.getResources().getDisplayMetrics().density,
                    oracle.getResources().getDisplayMetrics().density, 0f);
        }

        ViewPane pane() {
            ViewPane pane = new ViewPane(preview);
            pane.initialize(projectId, false);
            String cache = preview.getCacheDir().getAbsolutePath();
            pane.setResourceManager(new ResourceManager(projectId, cache, cache, cache));
            return pane;
        }

        int pixels(int dp) {
            int id = oracle.getResources().getIdentifier("preview_layout_dp_" + dp, "dimen", oracle.getPackageName());
            assertNotEquals("Missing compiled dimension " + dp, 0, id);
            return oracle.getResources().getDimensionPixelSize(id);
        }
    }

    private static Context configured(Context base, int dpi) {
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.densityDpi = dpi;
        return base.createConfigurationContext(configuration);
    }

    private static void status(String key, int value) {
        status(key, Integer.toString(value));
    }

    private static void status(String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }

    private static void onMain(Runnable check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            projectId = "F17LayoutTest." + UUID.randomUUID();
            File[] roots = {new File(SketchwarePaths.getDataPath(projectId)),
                    new File(SketchwarePaths.getBackupPath(projectId)),
                    new File(SketchwarePaths.getProjectListPath(projectId))};
            boolean ownsRoots = false;
            try {
                for (File root : roots) assertFalse("Fresh fixture must not already exist", root.exists());
                ownsRoots = true;
                ProjectDataManager.clearAll();
                status("f17_owned_project", projectId);
                check.run();
            } catch (Throwable failure) {
                error.set(failure);
            } finally {
                ProjectDataManager.clearAll();
                if (ownsRoots) {
                    try {
                        for (File root : roots) {
                            assertEquals(projectId, root.getName());
                            assertFalse("Do not follow fixture symlinks", Files.isSymbolicLink(root.toPath()));
                            if (root.exists()) {
                                try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
                                    for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                                        Files.delete(path);
                                    }
                                }
                            }
                            assertFalse("Fixture storage must be removed", root.exists());
                        }
                        status("f17_cleaned_project", projectId);
                    } catch (Throwable cleanupFailure) {
                        error.compareAndSet(null, cleanupFailure);
                    }
                }
                projectId = null;
            }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
