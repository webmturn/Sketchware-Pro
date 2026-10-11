package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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

import com.google.gson.Gson;

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
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;

/** Exercises public preview updates and native measurement with stale parent metadata. */
@RunWith(AndroidJUnit4.class)
public class PreviewParentLayoutParamsTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] PARENTS = {ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
            ViewBean.VIEW_TYPE_LAYOUT_RELATIVE, ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW,
            ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW, ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW,
            ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT,
            ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT,
            ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT, ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP};
    private static final int[] CHILDREN = {ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW,
            ViewBean.VIEW_TYPE_LAYOUT_LINEAR, ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW};
    private static final Gson GSON = new Gson();

    @Test public void cardChildrenWithStaleParentTypesSurvivePropertyUpdates() {
        onMain(project -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Fixture fixture = new Fixture(project, dpi);
                for (int stored : new int[]{ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                        ViewBean.VIEW_TYPE_LAYOUT_RELATIVE, ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT}) {
                    for (int type : CHILDREN) {
                        ViewGroup card = fixture.parent(ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW);
                        ViewBean bean = bean("child", type, "parent", stored);
                        View child = fixture.add(bean);
                        assertTrue("Insertion initially converts parameters", child.getLayoutParams() instanceof FrameLayout.LayoutParams);
                        String original = GSON.toJson(bean);
                        fixture.pane.updateView(bean);
                        // The unfixed code fails here with FrameLayout.onMeasure's ClassCastException.
                        fixture.layout();
                        assertSame(card, child.getParent());
                        fixture.assertParameters(child, ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW);
                        assertEquals("Preview must preserve project metadata", original, GSON.toJson(bean));
                        checks++;
                    }
                }
            }
            status("card_stale_parent_checks", checks);
        });
    }

    @Test public void attachedParentsDetermineParametersAndRelativeRules() {
        onMain(project -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Fixture fixture = new Fixture(project, dpi);
                for (int type : PARENTS) {
                    fixture.parent(type);
                    int stored = type == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE
                            ? ViewBean.VIEW_TYPE_LAYOUT_LINEAR : ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
                    ViewBean bean = bean("child", ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW, "parent", stored);
                    bean.parentAttributes.put("android:layout_centerInParent", "true");
                    View child = fixture.add(bean);
                    if (type == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
                        assertEquals("Insertion applies actual relative-parent rules", RelativeLayout.TRUE,
                                ((RelativeLayout.LayoutParams) child.getLayoutParams()).getRule(RelativeLayout.CENTER_IN_PARENT));
                    }
                    String original = GSON.toJson(bean);
                    fixture.pane.updateView(bean);
                    fixture.layout();
                    fixture.assertParameters(child, type);
                    if (type == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
                        assertEquals(RelativeLayout.TRUE,
                                ((RelativeLayout.LayoutParams) child.getLayoutParams()).getRule(RelativeLayout.CENTER_IN_PARENT));
                    }
                    assertEquals(original, GSON.toJson(bean));
                    checks++;
                }
            }
            status("attached_parent_parameter_checks", checks);
        });
    }

    @Test public void movingBetweenContainersKeepsUpdatesAndMeasurementsValid() {
        onMain(project -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Fixture fixture = new Fixture(project, dpi);
                for (int type : PARENTS) {
                    ViewGroup target = fixture.parent(type);
                    ViewBean bean = bean("child", ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                            "root", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
                    View child = fixture.add(bean);
                    bean.preParent = bean.parent;
                    bean.preParentType = bean.parentType;
                    bean.preIndex = bean.index;
                    bean.parent = "parent";
                    bean.parentType = type;
                    fixture.pane.moveView(bean);
                    fixture.pane.updateView(bean);
                    fixture.layout();
                    assertSame(target, child.getParent());
                    fixture.assertParameters(child, type);
                    bean.preParent = bean.parent;
                    bean.preParentType = bean.parentType;
                    bean.preIndex = bean.index;
                    bean.parent = "root";
                    bean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
                    fixture.pane.moveView(bean);
                    fixture.pane.updateView(bean);
                    fixture.layout();
                    assertSame(fixture.root, child.getParent());
                    fixture.assertParameters(child, ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
                    checks++;
                }
            }
            status("reparent_parameter_checks", checks);
        });
    }

    @Test public void detachedViewsKeepDeclaredParentParameters() {
        onMain(project -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Fixture fixture = new Fixture(project, dpi);
                for (int parent : PARENTS) for (int type : CHILDREN) {
                    ViewBean bean = bean("child", type, "parent", parent);
                    String original = GSON.toJson(bean);
                    View child = fixture.pane.createItemView(bean);
                    assertSame(null, child.getParent());
                    fixture.assertParameters(child, parent);
                    assertEquals(original, GSON.toJson(bean));
                    checks++;
                }
            }
            status("detached_parent_parameter_checks", checks);
        });
    }

    @Test public void previewRootsKeepThePanesRelativeParameters() {
        onMain(project -> {
            int checks = 0;
            for (int dpi : DENSITIES) {
                Fixture fixture = new Fixture(project, dpi);
                ViewBean bean = ((ItemView) fixture.root).getBean();
                String original = GSON.toJson(bean);
                fixture.pane.updateView(bean);
                fixture.layout();
                assertTrue("The preview pane requires relative layout parameters",
                        fixture.root.getLayoutParams() instanceof RelativeLayout.LayoutParams);
                assertEquals(original, GSON.toJson(bean));
                checks++;
            }
            status("preview_root_parameter_checks", checks);
        });
    }

    private static ViewBean bean(String id, int type, String parent, int parentType) {
        ViewBean bean = new ViewBean(id, type);
        bean.parent = parent;
        bean.parentType = parentType;
        bean.index = -1;
        bean.layout.width = 37;
        bean.layout.height = 29;
        bean.layout.marginLeft = 1;
        bean.layout.marginTop = 3;
        bean.layout.marginRight = 5;
        bean.layout.marginBottom = 7;
        bean.layout.layoutGravity = Gravity.BOTTOM | Gravity.END;
        bean.layout.weight = 2;
        bean.image.resName = "NONE";
        bean.getClassInfo();
        return bean;
    }

    private static final class Fixture {
        final ViewPane pane;
        final ViewGroup root;
        final int dpi;

        Fixture(String project, int dpi) {
            this.dpi = dpi;
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.densityDpi = dpi;
            Context themed = new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
            pane = new ViewPane(themed);
            pane.initialize(project, false);
            String cache = themed.getCacheDir().getAbsolutePath();
            pane.setResourceManager(new ResourceManager(project, cache, cache, cache));
            pane.updateRootLayout(project, "main");
            root = pane.findViewWithTag("root");
        }

        ViewGroup parent(int type) {
            root.removeAllViews();
            ViewBean bean = bean("parent", type, "root", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
            bean.layout.width = 160;
            bean.layout.height = 140;
            bean.layout.weight = 0;
            return (ViewGroup) add(bean);
        }

        View add(ViewBean bean) {
            View child = pane.createItemView(bean);
            pane.addViewAndUpdateIndex(child);
            return child;
        }

        void layout() {
            pane.measure(View.MeasureSpec.makeMeasureSpec(pixels(240), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(pixels(320), View.MeasureSpec.EXACTLY));
            pane.layout(0, 0, pixels(240), pixels(320));
        }

        void assertParameters(View child, int parent) {
            ViewGroup.MarginLayoutParams parameters = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
            assertEquals(pixels(37), parameters.width);
            assertEquals(pixels(29), parameters.height);
            assertEquals(pixels(1), parameters.leftMargin);
            assertEquals(pixels(3), parameters.topMargin);
            assertEquals(pixels(5), parameters.rightMargin);
            assertEquals(pixels(7), parameters.bottomMargin);
            if (parent == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
                assertTrue(parameters instanceof RelativeLayout.LayoutParams);
            } else if (parent == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW
                    || parent == ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW
                    || parent == ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW) {
                assertTrue(parameters instanceof FrameLayout.LayoutParams);
                assertEquals(Gravity.BOTTOM | Gravity.END, ((FrameLayout.LayoutParams) parameters).gravity);
            } else {
                assertTrue(parameters instanceof LinearLayout.LayoutParams);
                assertEquals(Gravity.BOTTOM | Gravity.END, ((LinearLayout.LayoutParams) parameters).gravity);
                assertEquals(2f, ((LinearLayout.LayoutParams) parameters).weight, 0f);
            }
        }

        int pixels(int dp) { return Math.round(dp * dpi / 160f); }
    }

    private static void onMain(Check check) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            String project = "CardParentTest." + UUID.randomUUID();
            File[] roots = {new File(SketchwarePaths.getDataPath(project)),
                    new File(SketchwarePaths.getBackupPath(project)),
                    new File(SketchwarePaths.getProjectListPath(project))};
            boolean owned = false;
            try {
                for (File root : roots) assertFalse("Fresh fixture must not exist", root.exists());
                owned = true;
                ProjectDataManager.clearAll();
                status("card_parent_owned_project", project);
                check.run(project);
            } catch (Throwable failure) {
                error.set(failure);
            } finally {
                ProjectDataManager.clearAll();
                if (owned) {
                    try {
                        for (File root : roots) {
                            assertEquals(project, root.getName());
                            assertFalse("Do not follow fixture symlinks", Files.isSymbolicLink(root.toPath()));
                            if (root.exists()) {
                                try (Stream<java.nio.file.Path> paths = Files.walk(root.toPath())) {
                                    for (java.nio.file.Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                                        Files.delete(path);
                                    }
                                }
                            }
                            assertFalse(root.exists());
                        }
                        status("card_parent_cleaned_project", project);
                    } catch (Throwable failure) {
                        error.compareAndSet(null, failure);
                    }
                }
            }
        });
        if (error.get() != null) throw new AssertionError(error.get());
    }

    private static void status(String key, int value) { status(key, Integer.toString(value)); }
    private static void status(String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
    }
    private interface Check { void run(String project); }
}
