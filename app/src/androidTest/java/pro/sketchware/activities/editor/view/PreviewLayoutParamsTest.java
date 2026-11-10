package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.resources.MaterialAttributes;
import com.google.gson.Gson;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.codegen.AppCompatInjection;
import pro.sketchware.core.codegen.LayoutGenerator;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.ProjectThemeColors;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.util.ThemeUtils;

/** Expected pixels come from actual AAPT2 dimensions, through the public ViewPane paths. */
@RunWith(AndroidJUnit4.class)
public class PreviewLayoutParamsTest {
    private static final int[] DENSITIES = {160, 240, 320, 420, 480};
    private static final int[] VALUES = {0, 1, 3, 4, 5, 6, 7, 9, 11, 13, 15, 16, 17, 999};
    private static final int[] PARENTS = {
        ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
        ViewBean.VIEW_TYPE_LAYOUT_RELATIVE,
        ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW
    };
    private static String projectId;

    @Test
    public void toolbarNavigationFollowsTheActiveLayoutOptions() {
        onMain(
                () -> {
                    ProjectLibraryBean compat =
                            ProjectDataManager.getLibraryManager(projectId).getCompat();
                    compat.useYn = ProjectLibraryBean.LIB_USE_Y;
                    compat.configurations.put("material3", true);
                    compat.configurations.put("theme", "Light");
                    var editor = new ViewEditor(new Environment(320).preview);
                    var file =
                            new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
                    file.setActivityOptions(
                            ProjectFileBean.OPTION_ACTIVITY_TOOLBAR
                                    | ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                    editor.initialize(projectId, file);
                    var navigation = toolbarNavigation(editor);
                    assertEquals(View.VISIBLE, navigation.getVisibility());
                    assertTrue(navigation.getDrawable() instanceof DrawerArrowDrawable);
                    var drawer = (DrawerArrowDrawable) navigation.getDrawable();
                    assertEquals(0f, drawer.getProgress(), 0f);

                    file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
                    editor.initialize(projectId, file);
                    assertEquals(View.VISIBLE, navigation.getVisibility());
                    assertNotNull(navigation.getDrawable());
                    assertFalse(navigation.getDrawable() instanceof DrawerArrowDrawable);

                    file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                    editor.initialize(projectId, file);
                    assertEquals(View.GONE, navigation.getVisibility());

                    var custom =
                            new ProjectFileBean(
                                    ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "row");
                    editor.initialize(projectId, custom);
                    assertEquals(View.GONE, navigation.getVisibility());

                    compat.useYn = ProjectLibraryBean.LIB_USE_N;
                    file.setActivityOptions(
                            ProjectFileBean.OPTION_ACTIVITY_TOOLBAR
                                    | ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                    editor.initialize(projectId, file);
                    assertEquals(View.GONE, navigation.getVisibility());

                    var cameraPage =
                            new ProjectFileBean(
                                    ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "camera_page");
                    ProjectDataManager.getFileManager(projectId)
                            .addFile(cameraPage.fileType, cameraPage.fileName);
                    ProjectDataManager.getProjectDataManager(projectId)
                            .addComponent(
                                    cameraPage.getJavaName(),
                                    ComponentBean.COMPONENT_TYPE_CAMERA,
                                    "camera");
                    editor.initialize(projectId, file);
                    assertEquals(View.VISIBLE, navigation.getVisibility());
                    assertTrue(navigation.getDrawable() instanceof DrawerArrowDrawable);
                });
    }

    @Test
    public void material3ToolbarPreviewMatchesTheGeneratedBackground() {
        onMain(
                () -> {
                    Context host = new Environment(320).preview;
                    ProjectLibraryBean compat =
                            ProjectDataManager.getLibraryManager(projectId).getCompat();
                    compat.useYn = ProjectLibraryBean.LIB_USE_Y;
                    compat.configurations.put("material3", true);
                    compat.configurations.put("theme", "Light");
                    ProjectFileBean file =
                            new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
                    file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
                    BuildConfig config = new BuildConfig();
                    config.sc_id = projectId;
                    config.isAppCompatEnabled = true;
                    LayoutGenerator generator = new LayoutGenerator(config, file);
                    generator.setViews(
                            new ArrayList<>(), new ViewBean("_fab", ViewBean.VIEW_TYPE_WIDGET_FAB));
                    String xml = generator.toXmlString();
                    int start = xml.indexOf("<com.google.android.material.appbar.MaterialToolbar");
                    assertTrue(start >= 0);
                    assertTrue(
                            xml.substring(start, xml.indexOf('>', start))
                                    .contains("android:background=\"?attr/colorPrimary\""));

                    ViewEditor editor = new ViewEditor(host);
                    editor.initialize(projectId, file);
                    Context theme =
                            new ContextThemeWrapper(
                                    host,
                                    com.google.android.material.R.style
                                            .Theme_Material3_Light_NoActionBar);
                    assertEquals(
                            ThemeUtils.getColor(theme, R.attr.colorPrimary), toolbarColor(editor));
                });
    }

    @Test
    public void material3ToolbarPreviewReadsSavedAppThemeColors() {
        onMain(
                () -> {
                    ProjectLibraryBean compat =
                            ProjectDataManager.getLibraryManager(projectId).getCompat();
                    compat.useYn = ProjectLibraryBean.LIB_USE_Y;
                    compat.configurations.put("material3", true);
                    compat.configurations.put("theme", "Light");
                    var metadata = new HashMap<String, Object>();
                    metadata.put("sc_id", projectId);
                    metadata.put(ProjectThemeColors.COLOR_PRIMARY, 0xff007fac);
                    ProjectListManager.saveProject(projectId, metadata);

                    var file =
                            new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
                    file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
                    var editor = new ViewEditor(new Environment(320).preview);

                    writeAppThemePrimary("#006A60");
                    editor.initialize(projectId, file);
                    assertEquals(0xff006a60, toolbarColor(editor));

                    writeAppThemePrimary("@color/colorPrimary");
                    editor.initialize(projectId, file);
                    assertEquals(0xff007fac, toolbarColor(editor));

                    FileUtil.writeFile(
                            SketchwarePaths.getProjectResourceValuesFilePath(
                                    projectId, "colors.xml"),
                            "<resources><color name=\"colorPrimary\">@color/brand</color>"
                                    + "<color name=\"brand\">#725C00</color></resources>");
                    FileUtil.writeFile(
                            SketchwarePaths.getProjectResourceValuesFilePath(
                                    projectId, "-night", "colors.xml"),
                            "<resources><color name=\"brand\">#F2DA78</color></resources>");
                    editor.initialize(projectId, file);
                    int expected = systemNight() ? 0xfff2da78 : 0xff725c00;
                    assertEquals(expected, toolbarColor(editor));

                    compat.configurations.put("theme", "Dark");
                    editor.initialize(projectId, file);
                    assertEquals(expected, toolbarColor(editor));
                });
    }

    private static void writeAppThemePrimary(String color) {
        FileUtil.writeFile(
                SketchwarePaths.getProjectResourceValuesFilePath(projectId, "styles.xml"),
                "<resources><style name=\"AppTheme\" parent=\"Theme.Material3.DayNight.NoActionBar\">"
                        + "<item name=\"colorPrimary\">"
                        + color
                        + "</item></style></resources>");
    }

    @Test
    public void material3ToolbarUsesPageOverridesAndCompatibleVersionedColors() {
        onMain(
                () -> {
                    ProjectLibraryBean compat =
                            ProjectDataManager.getLibraryManager(projectId).getCompat();
                    compat.useYn = ProjectLibraryBean.LIB_USE_Y;
                    compat.configurations.put("material3", true);
                    compat.configurations.put("theme", "Light");

                    FileUtil.writeFile(
                            SketchwarePaths.getProjectResourceValuesFilePath(
                                    projectId, "styles.xml"),
                            """
                            <resources>
                                <style name="AppTheme"><item name="colorPrimary">#6750A4</item></style>
                                <style name="AppTheme.AppBarOverlay" parent="ThemeOverlay.Material3">
                                    <item name="colorPrimary">?attr/colorSecondary</item>
                                    <item name="colorSecondary">@color/bar</item>
                                    <item name="colorOnSurface">#332244</item>
                                    <item name="toolbarStyle">@style/ProjectToolbar</item>
                                </style>
                                <style name="ProjectToolbar" parent="Widget.Material3.Toolbar">
                                    <item name="navigationIconTint">#164B53</item>
                                </style>
                            </resources>
                            """);
                    writeBarColor("", "#882211");
                    writeBarColor("-v" + Build.VERSION.SDK_INT, "#007FAC");
                    writeBarColor("-v" + (Build.VERSION.SDK_INT + 1), "#FF0000");
                    writeBarColor("-night", "#33BB44");
                    writeBarColor("-night-v" + Build.VERSION.SDK_INT, "#F2DA78");

                    var file =
                            new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
                    file.setActivityOptions(
                            ProjectFileBean.OPTION_ACTIVITY_TOOLBAR
                                    | ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                    var editor = new ViewEditor(new Environment(320).preview);
                    try {
                        editor.initialize(projectId, file);
                        int expected = systemNight() ? 0xfff2da78 : 0xff007fac;
                        assertEquals(expected, toolbarColor(editor));
                        assertEquals(0xff332244, toolbarTitle(editor).getCurrentTextColor());
                        var navigation = toolbarNavigation(editor);
                        assertEquals(
                                0xff164b53,
                                ((DrawerArrowDrawable) navigation.getDrawable()).getColor());

                        compat.configurations.put("theme", "Dark");
                        editor.initialize(projectId, file);
                        assertEquals(expected, toolbarColor(editor));

                        FileUtil.writeFile(
                                SketchwarePaths.getAppCompatInjectionPath(projectId, file.fileName),
                                new Gson()
                                        .toJson(
                                                List.of(
                                                        Map.of(
                                                                "type",
                                                                "Toolbar",
                                                                "value",
                                                                "android:background=\"#006A60\""),
                                                        Map.of(
                                                                "type",
                                                                "Toolbar",
                                                                "value",
                                                                "app:titleTextColor=\"@color/bar\""),
                                                        Map.of(
                                                                "type",
                                                                "Toolbar",
                                                                "value",
                                                                "app:navigationIconTint=\"#82191D\""))));
                        AppCompatInjection.invalidate(projectId, file.fileName);
                        editor.initialize(projectId, file);
                        assertEquals(0xff006a60, toolbarColor(editor));
                        assertEquals(expected, toolbarTitle(editor).getCurrentTextColor());
                        assertEquals(
                                0xff82191d,
                                ((DrawerArrowDrawable) navigation.getDrawable()).getColor());

                        file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
                        editor.initialize(projectId, file);
                        assertEquals(0xff82191d, navigation.getImageTintList().getDefaultColor());
                    } finally {
                        AppCompatInjection.invalidate(projectId, file.fileName);
                    }
                });
    }

    private static void writeBarColor(String qualifier, String color) {
        FileUtil.writeFile(
                SketchwarePaths.getProjectResourceValuesFilePath(
                        projectId, qualifier, "colors.xml"),
                "<resources><color name=\"bar\">" + color + "</color></resources>");
    }

    private static int toolbarColor(ViewEditor editor) {
        View toolbar = (View) toolbarTitle(editor).getParent();
        return ((ColorDrawable) toolbar.getBackground()).getColor();
    }

    private static ImageView toolbarNavigation(ViewEditor editor) {
        return editor.findViewById(R.id.preview_toolbar_navigation);
    }

    private static TextView toolbarTitle(ViewEditor editor) {
        editor.findViewById(R.id.shape).setVisibility(View.VISIBLE);
        var titles = new ArrayList<View>();
        editor.findViewsWithText(
                titles,
                Helper.getResString(R.string.editor_preview_toolbar),
                View.FIND_VIEWS_WITH_TEXT);
        assertEquals(1, titles.size());
        return (TextView) titles.get(0);
    }

    @Test
    public void dynamicOverlayReplacesPrimaryAndSecondaryButKeepsToolbarStyle() {
        assumeTrue(Build.VERSION.SDK_INT >= 31);
        onMain(
                () -> {
                    var theme =
                            new HashMap<>(
                                    Map.of(
                                            "colorPrimary", "#007FAC",
                                            "colorSecondary", "#006A60",
                                            "toolbarStyle", "@style/ProjectToolbar"));
                    Context host = new Environment(320).preview;
                    var values =
                            new PreviewValues(
                                    host, null, null, new ColorResourceResolver(projectId));
                    values.removeAttributesOverriddenBy(
                            theme,
                            com.google.android.material.R.style
                                    .ThemeOverlay_Material3_DynamicColors_Light);
                    assertEquals(Map.of("toolbarStyle", "@style/ProjectToolbar"), theme);
                });
    }

    @Test
    public void material3PreviewMatchesTheProjectThemeWhenSettingsChange() {
        onMain(
                () -> {
                    Context host = new Environment(320).preview;
                    ProjectLibraryBean compat =
                            ProjectDataManager.getLibraryManager(projectId).getCompat();
                    compat.useYn = ProjectLibraryBean.LIB_USE_Y;
                    compat.configurations.put("material3", true);
                    ViewPane pane = new ViewPane(host);
                    View previousRoot = null;
                    boolean systemIsDark = systemNight();

                    for (boolean dark : new boolean[] {false, true}) {
                        compat.configurations.put("theme", dark ? "Dark" : "Light");
                        for (boolean dynamic : new boolean[] {false, true}) {
                            compat.configurations.put("dynamic_colors", dynamic);
                            pane.initialize(projectId, true);
                            pane.updateRootLayout(projectId, "main.xml");
                            assertNotEquals(previousRoot, pane.getChildAt(0));
                            previousRoot = pane.getChildAt(0);

                            Configuration configuration =
                                    new Configuration(host.getResources().getConfiguration());
                            configuration.uiMode =
                                    (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                                            | (systemIsDark
                                                    ? Configuration.UI_MODE_NIGHT_YES
                                                    : Configuration.UI_MODE_NIGHT_NO);
                            Context expected =
                                    new ContextThemeWrapper(
                                            host.createConfigurationContext(configuration),
                                            dark
                                                    ? com.google.android.material.R.style
                                                            .Theme_Material3_Dark_NoActionBar
                                                    : com.google.android.material.R.style
                                                            .Theme_Material3_Light_NoActionBar);
                            if (dynamic) {
                                expected = DynamicColors.wrapContextIfAvailable(expected);
                            }
                            Context actual = pane.getPreviewContext();
                            assertEquals(systemIsDark, ThemeUtils.isDarkThemeEnabled(actual));
                            assertEquals(
                                    !dark,
                                    MaterialAttributes.resolveBoolean(
                                            actual, androidx.appcompat.R.attr.isLightTheme, true));
                            for (int attr :
                                    new int[] {
                                        R.attr.colorPrimary,
                                        R.attr.colorSurface,
                                        R.attr.colorOnSurface,
                                        android.R.attr.colorBackground
                                    }) {
                                assertEquals(
                                        ThemeUtils.getColor(expected, attr),
                                        ThemeUtils.getColor(actual, attr));
                            }
                            assertEquals(
                                    ThemeUtils.getColor(expected, android.R.attr.colorBackground),
                                    ((ColorDrawable) pane.getBackground()).getColor());
                            assertEquals(
                                    Integer.valueOf(
                                            ThemeUtils.getColor(expected, R.attr.colorPrimary)),
                                    new ColorResourceResolver(projectId)
                                            .resolveColorInt(
                                                    host, "?attr/colorPrimary", 3, systemIsDark));

                            ViewBean button =
                                    bean(
                                            ViewBean.VIEW_TYPE_WIDGET_BUTTON,
                                            ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                                            new int[6]);
                            ViewBean text =
                                    bean(
                                            ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW,
                                            ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                                            new int[6]);
                            View buttonView = pane.createItemView(button);
                            assertTrue(buttonView instanceof MaterialButton);
                            assertEquals(
                                    new MaterialButton(expected).getCurrentTextColor(),
                                    ((TextView) buttonView).getCurrentTextColor());
                            assertEquals(
                                    new TextView(expected).getCurrentTextColor(),
                                    ((TextView) pane.createItemView(text)).getCurrentTextColor());
                            FloatingActionButton fab = (FloatingActionButton) pane.addFab(null);
                            assertEquals(
                                    new FloatingActionButton(expected)
                                            .getBackgroundTintList()
                                            .getDefaultColor(),
                                    fab.getBackgroundTintList().getDefaultColor());
                            pane.removeView(fab);
                        }
                    }
                    compat.configurations.put("theme", "DayNight");
                    Configuration opposite =
                            new Configuration(host.getResources().getConfiguration());
                    opposite.uiMode =
                            (opposite.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                                    | (systemIsDark
                                            ? Configuration.UI_MODE_NIGHT_NO
                                            : Configuration.UI_MODE_NIGHT_YES);
                    ViewPane systemPreview =
                            new ViewPane(host.createConfigurationContext(opposite));
                    systemPreview.initialize(projectId, true);
                    assertEquals(
                            systemIsDark,
                            ThemeUtils.isDarkThemeEnabled(systemPreview.getPreviewContext()));
                    status(
                            "material3_dynamic_color_available",
                            Boolean.toString(DynamicColors.isDynamicColorAvailable()));
                });
    }

    private static boolean systemNight() {
        return (Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @Test
    public void ordinaryLayoutParamsMatchCompiledDimensions() {
        onMain(
                () -> {
                    int checks = 0;
                    for (int dpi : DENSITIES) {
                        Environment environment = new Environment(dpi);
                        ViewPane pane = environment.pane();
                        for (int parent : PARENTS) {
                            for (int type :
                                    new int[] {
                                        ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                                        ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW
                                    }) {
                                for (int i = 0; i < VALUES.length; i++) {
                                    int[] dp = new int[6];
                                    int[] expected = new int[6];
                                    for (int side = 0; side < dp.length; side++) {
                                        dp[side] = VALUES[(i + side) % VALUES.length];
                                        expected[side] = environment.pixels(dp[side]);
                                    }
                                    ViewBean bean = bean(type, parent, dp);
                                    View view = pane.createItemView(bean);
                                    assertArrayEquals(
                                            "dpi=" + dpi + ", parent=" + parent + ", type=" + type,
                                            expected,
                                            params(view));
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
        onMain(
                () -> {
                    int checks = 0;
                    for (int dpi : DENSITIES) {
                        ViewPane pane = new Environment(dpi).pane();
                        for (int parent : PARENTS) {
                            for (int[] sizes : new int[][] {{-1, -2}, {-2, -1}, {0, 0}}) {
                                ViewBean bean =
                                        bean(
                                                ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                                                parent,
                                                new int[] {sizes[0], sizes[1], 0, 0, 0, 0});
                                bean.layout.weight = 2;
                                bean.layout.layoutGravity = Gravity.CENTER_HORIZONTAL;
                                LayoutBean original = new LayoutBean();
                                original.copy(bean.layout);
                                View view = pane.createItemView(bean);
                                assertArrayEquals(
                                        new int[] {sizes[0], sizes[1], 0, 0, 0, 0}, params(view));
                                if (parent == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                                    LinearLayout.LayoutParams parameters =
                                            (LinearLayout.LayoutParams) view.getLayoutParams();
                                    assertEquals(2f, parameters.weight, 0f);
                                    assertEquals(Gravity.CENTER_HORIZONTAL, parameters.gravity);
                                } else if (parent == ViewBean.VIEW_TYPE_LAYOUT_RELATIVE) {
                                    assertTrue(
                                            view.getLayoutParams()
                                                    instanceof RelativeLayout.LayoutParams);
                                } else {
                                    FrameLayout.LayoutParams parameters =
                                            (FrameLayout.LayoutParams) view.getLayoutParams();
                                    assertEquals(Gravity.CENTER_HORIZONTAL, parameters.gravity);
                                }
                                assertTrue(
                                        "Preview must not rewrite layout metadata",
                                        original.isEqual(bean.layout));
                                checks++;
                            }
                        }
                    }
                    status("f17_layout_mode_checks", checks);
                });
    }

    @Test
    public void fabMarginsMatchCompiledDimensionsAndKeepPlacement() {
        onMain(
                () -> {
                    int checks = 0;
                    for (int dpi : DENSITIES) {
                        Environment environment = new Environment(dpi);
                        ViewPane pane = environment.pane();
                        for (int value : VALUES) {
                            ViewBean bean =
                                    bean(
                                            ViewBean.VIEW_TYPE_WIDGET_FAB,
                                            ViewBean.VIEW_TYPE_LAYOUT_RELATIVE,
                                            new int[] {4, 6, value, value, value, value});
                            bean.id = "_fab";
                            bean.layout.layoutGravity = Gravity.RIGHT | Gravity.BOTTOM;
                            View view = pane.addFab(bean);
                            int margin = environment.pixels(value);
                            assertArrayEquals(
                                    "FAB at " + dpi + " dpi, margin=" + value,
                                    new int[] {-2, -2, margin, margin, margin, margin},
                                    params(view));
                            assertPlacement(view);
                            pane.removeView(view);
                            checks++;
                        }
                        View defaultFab = pane.addFab(null);
                        int margin = environment.pixels(16);
                        assertArrayEquals(
                                new int[] {-2, -2, margin, margin, margin, margin},
                                params(defaultFab));
                        assertPlacement(defaultFab);
                        checks++;
                    }
                    status("f17_fab_parameter_checks", checks);
                });
    }

    private static void assertPlacement(View view) {
        RelativeLayout.LayoutParams parameters =
                (RelativeLayout.LayoutParams) view.getLayoutParams();
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
        ViewGroup.MarginLayoutParams parameters =
                (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return new int[] {
            parameters.width,
            parameters.height,
            parameters.leftMargin,
            parameters.topMargin,
            parameters.rightMargin,
            parameters.bottomMargin
        };
    }

    private static final class Environment {
        final Context preview;
        final Context oracle;

        Environment(int dpi) {
            preview =
                    new ContextThemeWrapper(
                            configured(
                                    InstrumentationRegistry.getInstrumentation().getTargetContext(),
                                    dpi),
                            R.style.Theme_SketchwarePro);
            oracle = configured(InstrumentationRegistry.getInstrumentation().getContext(), dpi);
            assertEquals(dpi, preview.getResources().getConfiguration().densityDpi);
            assertEquals(
                    preview.getResources().getDisplayMetrics().density,
                    oracle.getResources().getDisplayMetrics().density,
                    0f);
        }

        ViewPane pane() {
            ViewPane pane = new ViewPane(preview);
            pane.initialize(projectId, false);
            String cache = preview.getCacheDir().getAbsolutePath();
            pane.setResourceManager(new ResourceManager(projectId, cache, cache, cache));
            return pane;
        }

        int pixels(int dp) {
            int id =
                    oracle.getResources()
                            .getIdentifier(
                                    "preview_layout_dp_" + dp, "dimen", oracle.getPackageName());
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
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            projectId = "F17LayoutTest." + UUID.randomUUID();
                            File[] roots = {
                                new File(SketchwarePaths.getDataPath(projectId)),
                                new File(SketchwarePaths.getBackupPath(projectId)),
                                new File(SketchwarePaths.getProjectListPath(projectId))
                            };
                            boolean ownsRoots = false;
                            try {
                                for (File root : roots)
                                    assertFalse(
                                            "Fresh fixture must not already exist", root.exists());
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
                                            assertFalse(
                                                    "Do not follow fixture symlinks",
                                                    Files.isSymbolicLink(root.toPath()));
                                            if (root.exists()) {
                                                try (Stream<java.nio.file.Path> paths =
                                                        Files.walk(root.toPath())) {
                                                    for (java.nio.file.Path path :
                                                            paths.sorted(Comparator.reverseOrder())
                                                                    .collect(Collectors.toList())) {
                                                        Files.delete(path);
                                                    }
                                                }
                                            }
                                            assertFalse(
                                                    "Fixture storage must be removed",
                                                    root.exists());
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
