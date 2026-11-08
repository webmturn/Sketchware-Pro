package pro.sketchware.activities.editor.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.item.ItemCodeView;
import pro.sketchware.activities.editor.view.item.ItemOTPView;
import pro.sketchware.activities.editor.view.item.ItemPatternLockView;
import pro.sketchware.activities.editor.view.item.ItemProgressBar;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.core.codegen.LayoutGenerator;
import pro.sketchware.core.layout.InjectAttributeHandler;
import pro.sketchware.core.layout.ViewBeanFactory;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.util.ViewUtil;

/** Verifies preview dimensions, attribute updates, and visible content on Android. */
@RunWith(AndroidJUnit4.class)
public class LibraryWidgetPreviewTest {
    @Test
    public void narrowCodePreviewKeepsLineNumbersAndTextSize() throws Exception {
        onMain(
                () -> {
                    ItemCodeView light =
                            new ItemCodeView(themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    ItemCodeView dark =
                            new ItemCodeView(
                                    themed(
                                            R.style
                                                    .ThemeOverlay_SketchwarePro_ViewEditor_Material3_NON_DYNAMIC_Dark));
                    TextView code = (TextView) light.getChildAt(0);
                    assertTrue(code.getText().toString().startsWith("String"));
                    assertEquals(
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    14,
                                    light.getResources().getDisplayMetrics()),
                            code.getTextSize(),
                            0f);
                    light.applyPreviewAttributes(Map.of("app:cv_show_line_number", "true"));
                    measure(light, 320);
                    int wideWidth = code.getWidth();
                    float fontSize = code.getTextSize();

                    measure(light, 112);
                    assertTrue(code.getWidth() < wideWidth);
                    assertEquals(fontSize, code.getTextSize(), 0f);
                    assertEquals(3, code.getLineCount());
                    assertTrue(code.getText().toString().startsWith("1  "));
                    assertNotEquals(
                            code.getCurrentTextColor(),
                            ((TextView) dark.getChildAt(0)).getCurrentTextColor());
                });
    }

    @Test
    public void selectingCodePreviewChangesOnlyTheOutline() throws Exception {
        onMain(
                () -> {
                    ItemCodeView preview =
                            new ItemCodeView(themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    measure(preview, 180);
                    int width = preview.getWidth();
                    int height = preview.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        preview.draw(canvas);
                        int center = bitmap.getPixel(width / 2, height / 2);
                        int edge = bitmap.getPixel(width / 2, 1);

                        preview.setSelection(true);
                        bitmap.eraseColor(Color.TRANSPARENT);
                        preview.draw(canvas);
                        assertEquals(center, bitmap.getPixel(width / 2, height / 2));
                        assertNotEquals(edge, bitmap.getPixel(width / 2, 1));
                    } finally {
                        bitmap.recycle();
                    }
                });
    }

    @Test
    public void codePreviewAppliesAndResetsInjectedAttributes() throws Exception {
        onMain(
                () -> {
                    ItemCodeView preview =
                            new ItemCodeView(themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    TextView code = (TextView) preview.getChildAt(0);
                    float defaultSize = code.getTextSize();
                    ViewBean bean = new ViewBean("code", ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW);
                    bean.inject =
                            " app:cv_font_size=\"18\" app:cv_wrap_line=\"true\""
                                    + " app:cv_show_line_number=\"true\""
                                    + " app:cv_start_line_number=\"10\" app:cv_highlight_line_number=\"11\"";
                    InjectAttributeHandler attributes = new InjectAttributeHandler(bean);
                    preview.applyPreviewAttributes(attributes.getNamespacedAttributes());
                    measure(preview, 112);

                    assertTrue(code.getTextSize() > defaultSize);
                    assertTrue(code.getLineCount() > 3);
                    Spanned text = (Spanned) code.getText();
                    assertTrue(text.toString().startsWith("10  "));
                    BackgroundColorSpan[] highlights =
                            text.getSpans(0, text.length(), BackgroundColorSpan.class);
                    assertEquals(1, highlights.length);
                    assertTrue(
                            text.subSequence(
                                            text.getSpanStart(highlights[0]),
                                            text.getSpanEnd(highlights[0]))
                                    .toString()
                                    .startsWith("11  "));

                    bean.inject = " app:cv_font_size=\"invalid\"";
                    preview.applyPreviewAttributes(attributes.getNamespacedAttributes());
                    measure(preview, 112);
                    assertEquals(defaultSize, code.getTextSize(), 0f);
                    assertEquals(3, code.getLineCount());
                    assertTrue(code.getText().toString().startsWith("String"));

                    preview.applyPreviewAttributes(Map.of("app:cv_font_size", "6"));
                    assertEquals(
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    8,
                                    code.getResources().getDisplayMetrics()),
                            code.getTextSize(),
                            0f);
                });
    }

    @Test
    public void otpPreviewKeepsFixedFieldWidthsAndClipsInNarrowContainers() throws Exception {
        onMain(
                () -> {
                    ItemOTPView otp =
                            new ItemOTPView(themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    int slotWidth =
                            (int) ViewUtil.dpToPx(otp.getContext(), 36f)
                                    + 2 * (int) ViewUtil.dpToPx(otp.getContext(), 5f);
                    otp.measure(0, 0);
                    assertEquals(6 * slotWidth, otp.getMeasuredWidth());
                    measure(otp, 300);
                    assertEquals(6, visibleFields(otp));
                    View field = otp.getChildAt(0);
                    assertEquals(slotWidth - 2 * field.getLeft(), field.getWidth());
                    assertEquals(field.getHeight() + 2 * field.getTop(), otp.getHeight());
                    Bitmap bitmap =
                            Bitmap.createBitmap(
                                    otp.getWidth(), otp.getHeight(), Bitmap.Config.ARGB_8888);
                    try {
                        otp.draw(new Canvas(bitmap));
                        int center = field.getLeft() + field.getWidth() / 2;
                        assertEquals(0xfffafafa, bitmap.getPixel(center, field.getTop() + 1));
                        assertEquals(0xff7c8ba2, bitmap.getPixel(center, field.getBottom() - 1));
                    } finally {
                        bitmap.recycle();
                    }
                    measure(otp, 104);
                    assertEquals(3, visibleFields(otp));
                });
    }

    private static int visibleFields(View view) {
        Bitmap bitmap =
                Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        try {
            view.draw(new Canvas(bitmap));
            int mostSegments = 0;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int segments = 0;
                boolean inSegment = false;
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    boolean painted = Color.alpha(bitmap.getPixel(x, y)) > 64;
                    if (painted && !inSegment) {
                        segments++;
                    }
                    inSegment = painted;
                }
                mostSegments = Math.max(mostSegments, segments);
            }
            return mostSegments;
        } finally {
            bitmap.recycle();
        }
    }

    @Test
    public void horizontalProgressFitsNarrowWidthAndAppliesValues() throws Exception {
        onMain(
                () -> {
                    ItemProgressBar preview =
                            new ItemProgressBar(
                                    themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    ViewBean bean = new ViewBean("progress", ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR);
                    bean.progressStyle = ViewBean.PROGRESSBAR_STYLE_HORIZONTAL;
                    bean.max = 80;
                    bean.progress = 25;
                    preview.setPaddingPixels(7, 3, 11, 5);
                    PreviewValues values =
                            new PreviewValues(
                                    preview.getContext(),
                                    null,
                                    null,
                                    new ColorResourceResolver(null));
                    preview.applyPreviewAttributes(bean, Map.of(), values);
                    measure(preview, 104);
                    ProgressBar bar = (ProgressBar) preview.getChildAt(0);
                    var defaultTint = bar.getProgressTintList();
                    assertEquals(preview.getWidth() - 18, bar.getWidth());
                    assertEquals(80, bar.getMax());
                    assertEquals(25, bar.getProgress());
                    assertFalse(bar.isIndeterminate());

                    preview.applyPreviewAttributes(
                            bean,
                            Map.of(
                                    "android:max",
                                    "120",
                                    "android:progress",
                                    "90",
                                    "android:indeterminate",
                                    "true"),
                            values);
                    assertTrue(bar.isIndeterminate());
                    preview.applyPreviewAttributes(
                            bean, Map.of("android:max", "120", "android:progress", "90"), values);
                    assertFalse(bar.isIndeterminate());
                    assertEquals(120, bar.getMax());
                    assertEquals(90, bar.getProgress());

                    preview.applyPreviewAttributes(
                            bean,
                            Map.of(
                                    "android:min",
                                    "150",
                                    "android:max",
                                    "250",
                                    "android:progress",
                                    "200",
                                    "android:progressTint",
                                    "#c22"),
                            values);
                    assertEquals(150, bar.getMin());
                    assertEquals(250, bar.getMax());
                    assertEquals(200, bar.getProgress());
                    assertEquals(0xffcc2222, bar.getProgressTintList().getDefaultColor());

                    preview.applyPreviewAttributes(bean, Map.of(), values);
                    assertSame(bar, preview.getChildAt(0));
                    assertEquals(defaultTint, bar.getProgressTintList());
                    assertEquals(0, bar.getMin());
                    assertEquals(80, bar.getMax());
                    assertEquals(25, bar.getProgress());

                    preview.applyPreviewAttributes(
                            bean, Map.of("android:max", "@integer/unavailable"), values);
                    assertEquals(80, bar.getMax());

                    preview.applyPreviewAttributes(
                            bean, Map.of("tools:replace", "android:max,android:progress"), values);
                    assertEquals(ViewBean.DEFAULT_MAX, bar.getMax());
                    assertEquals(ViewBean.DEFAULT_PROGRESS, bar.getProgress());
                });
    }

    @Test
    public void codePreviewResolvesProjectFontSize() throws Exception {
        Context context = themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor);
        withPreviewValues(
                context,
                "<resources><integer name=\"code_size\">18</integer></resources>",
                values -> {
                    var code = new ItemCodeView(context);
                    code.applyPreviewAttributes(
                            values.resolveAttributes(
                                    Map.of("app:cv_font_size", "@integer/code_size")));
                    assertEquals(
                            TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    18,
                                    context.getResources().getDisplayMetrics()),
                            ((TextView) code.getChildAt(0)).getTextSize(),
                            0f);
                });
    }

    @Test
    public void progressPreviewUsesTheImportedStyleAndItsResources() throws Exception {
        Context context = themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor);
        withPreviewValues(
                context,
                """
                <resources>
                  <integer name="buffer_size">18</integer>
                  <integer name="buffered">@integer/buffer_size</integer>
                  <bool name="loading">true</bool>
                  <color name="progress_color">#c22</color>
                  <style name="Player" parent="@android:style/Widget.ProgressBar.Horizontal">
                    <item name="android:max">80</item>
                    <item name="android:progressTint">@color/progress_color</item>
                  </style>
                  <style name="Player.Custom">
                    <item name="android:secondaryProgress">@integer/buffered</item>
                  </style>
                </resources>
                """,
                values -> {
                    var bean = new ViewBean("progress", ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR);
                    new ViewBeanFactory(bean)
                            .applyAttributes(Map.of("style", "@style/Player.Custom"));
                    Map<String, String> injected =
                            new InjectAttributeHandler(bean).getNamespacedAttributes();
                    assertEquals("@style/Player.Custom", injected.get("style"));

                    var preview = new ItemProgressBar(context);
                    preview.applyPreviewAttributes(bean, injected, values);
                    measure(preview, 104);
                    ProgressBar bar = (ProgressBar) preview.getChildAt(0);
                    assertEquals(preview.getWidth(), bar.getWidth());
                    assertEquals(80, bar.getMax());
                    assertEquals(18, bar.getSecondaryProgress());
                    assertEquals(0xffcc2222, bar.getProgressTintList().getDefaultColor());

                    var loading = new HashMap<>(injected);
                    loading.put("android:indeterminate", "@bool/loading");
                    preview.applyPreviewAttributes(bean, loading, values);
                    assertTrue(bar.isIndeterminate());
                });
    }

    @Test
    public void previewResolvesMixedColorResourcesAndRefreshesAfterDeletion() throws Exception {
        Context context = themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor);
        var directory = Files.createTempDirectory(context.getCacheDir().toPath(), "preview-colors");
        var night = directory.resolve("values-night");
        var defaultsFile = directory.resolve("colors.xml");
        var nightFile = night.resolve("colors.xml");
        var aliases = directory.resolve("aliases.xml");
        try {
            Files.createDirectory(night);
            Files.write(
                    defaultsFile,
                    """
                    <resources>
                      <color name="track">#c22</color>
                      <integer name="track_max">80</integer>
                    </resources>
                    """
                            .getBytes(StandardCharsets.UTF_8));
            Files.write(
                    nightFile,
                    "<resources><color name=\"track\">#f80</color></resources>"
                            .getBytes(StandardCharsets.UTF_8));
            Files.write(
                    aliases,
                    "<resources><color name=\"progress\">@color/track</color></resources>"
                            .getBytes(StandardCharsets.UTF_8));

            ColorResourceResolver colors = new ColorResourceResolver(null);
            colors.loadColorsFromPath(defaultsFile.toString(), false);
            Map<String, String> attributes =
                    Map.of(
                            "android:progressTint",
                            "@color/progress",
                            "android:max",
                            "@integer/track_max");
            PreviewValues day = new PreviewValues(context, directory.toFile(), null, colors);
            Map<String, String> dayValues = day.resolveAttributes(attributes);
            assertEquals("80", dayValues.get("android:max"));
            assertEquals(
                    Integer.valueOf(0xffcc2222), day.color(dayValues.get("android:progressTint")));

            PreviewValues dark =
                    new PreviewValues(context, directory.toFile(), night.toFile(), colors);
            String darkTint = dark.resolveAttributes(attributes).get("android:progressTint");
            assertEquals(Integer.valueOf(0xffff8800), dark.color(darkTint));

            Files.delete(defaultsFile);
            PreviewValues refreshed = new PreviewValues(context, directory.toFile(), null, colors);
            String removed = refreshed.resolveAttributes(attributes).get("android:progressTint");
            assertNull(refreshed.color(removed));
        } finally {
            Files.deleteIfExists(aliases);
            Files.deleteIfExists(nightFile);
            Files.deleteIfExists(night);
            Files.deleteIfExists(defaultsFile);
            Files.delete(directory);
        }
    }

    @Test
    public void generatedProgressBarKeepsInjectedValuesAndOmitsReplacedAttributes() {
        ViewBean bean = new ViewBean("progress", ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR);
        bean.parent = "root";
        bean.max = 80;
        bean.progress = 25;
        bean.inject =
                """
                 style="@style/Player.Custom" android:max="120" tools:replace="android:progress"
                """;
        BuildConfig config = new BuildConfig();
        config.sc_id = "PreviewProgress";
        ProjectFileBean file =
                new ProjectFileBean(
                        ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "progress_preview");
        LayoutGenerator generator = new LayoutGenerator(config, file);
        generator.setViews(new ArrayList<>(List.of(bean)));
        String xml = generator.toXmlString();
        assertTrue(xml.contains("style=\"@style/Player.Custom\""));
        assertTrue(xml.contains("android:max=\"120\""));
        assertFalse(xml.contains("android:max=\"80\""));
        assertFalse(xml.contains("android:progress=\"25\""));
        assertFalse(xml.contains("style=\"" + ViewBean.PROGRESSBAR_STYLE_CIRCLE + "\""));
    }

    @Test
    public void patternPreviewDrawsConfiguredGridAndColors() throws Exception {
        Context context = themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor);
        withPreviewValues(
                context,
                "<resources><dimen name=\"dot_size\">20dp</dimen></resources>",
                values -> {
                    ItemPatternLockView preview = new ItemPatternLockView(context);
                    ViewBean bean =
                            new ViewBean("pattern", ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW);
                    bean.inject =
                            " app:dotCount=\"4\" app:dotNormalSize=\"@dimen/dot_size\""
                                    + " app:normalStateColor=\"#a0f0\" app:correctStateColor=\"#c22\"";
                    InjectAttributeHandler injected = new InjectAttributeHandler(bean);
                    preview.applyPreviewAttributes(
                            values.resolveAttributes(injected.getNamespacedAttributes()),
                            values::color);
                    measure(preview, 144);
                    int width = preview.getWidth();
                    int height = preview.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        preview.draw(canvas);
                        int y = height / 8;
                        for (int column = 0; column < 4; column++) {
                            int x = (2 * column + 1) * width / 8;
                            assertEquals(
                                    column == 0 ? 0xffcc2222 : 0xaa00ff00, bitmap.getPixel(x, y));
                        }
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(width / 2, y));

                        int beyondDefaultRadius =
                                3 * width / 8 + Math.round(ViewUtil.dpToPx(context, 7f));
                        assertEquals(0xaa00ff00, bitmap.getPixel(beyondDefaultRadius, y));
                        bean.inject =
                                bean.inject.replace(" app:dotNormalSize=\"@dimen/dot_size\"", "");
                        preview.applyPreviewAttributes(
                                values.resolveAttributes(injected.getNamespacedAttributes()),
                                values::color);
                        bitmap.eraseColor(Color.TRANSPARENT);
                        preview.draw(canvas);
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(beyondDefaultRadius, y));
                    } finally {
                        bitmap.recycle();
                    }
                });
    }

    @Test
    public void patternPreviewUsesBoundedHeightAndHonorsAspectModes() throws Exception {
        onMain(
                () -> {
                    ItemPatternLockView preview =
                            new ItemPatternLockView(
                                    themed(R.style.ThemeOverlay_SketchwarePro_ViewEditor));
                    int width = Math.round(ViewUtil.dpToPx(preview.getContext(), 180f));
                    int heightLimit = Math.round(ViewUtil.dpToPx(preview.getContext(), 500f));
                    int widthSpec =
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                    int heightSpec =
                            View.MeasureSpec.makeMeasureSpec(heightLimit, View.MeasureSpec.AT_MOST);
                    preview.measure(widthSpec, heightSpec);
                    assertEquals(heightLimit, preview.getMeasuredHeight());
                    preview.applyPreviewAttributes(
                            Map.of("app:aspectRatioEnabled", "true"), Color::parseColor);
                    preview.measure(widthSpec, heightSpec);
                    assertEquals(width, preview.getMeasuredWidth());
                    assertEquals(width, preview.getMeasuredHeight());

                    for (String aspect : List.of("height_bias", "2")) {
                        preview.applyPreviewAttributes(
                                Map.of("app:aspectRatioEnabled", "true", "app:aspectRatio", aspect),
                                Color::parseColor);
                        preview.measure(widthSpec, heightSpec);
                        assertEquals(width, preview.getMeasuredWidth());
                        assertEquals(heightLimit, preview.getMeasuredHeight());
                    }
                    int wideSpec =
                            View.MeasureSpec.makeMeasureSpec(heightLimit, View.MeasureSpec.EXACTLY);
                    int shortSpec =
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST);
                    for (String aspect : List.of("width_bias", "1")) {
                        preview.applyPreviewAttributes(
                                Map.of("app:aspectRatioEnabled", "true", "app:aspectRatio", aspect),
                                Color::parseColor);
                        preview.measure(wideSpec, shortSpec);
                        assertEquals(heightLimit, preview.getMeasuredWidth());
                        assertEquals(width, preview.getMeasuredHeight());
                    }
                });
    }

    private static void withPreviewValues(
            Context context, String xml, Consumer<PreviewValues> check) throws Exception {
        var directory = Files.createTempDirectory(context.getCacheDir().toPath(), "preview-values");
        var file = directory.resolve("widgets.xml");
        try {
            Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
            onMain(
                    () ->
                            check.accept(
                                    new PreviewValues(
                                            context,
                                            directory.toFile(),
                                            null,
                                            new ColorResourceResolver(null))));
        } finally {
            Files.deleteIfExists(file);
            Files.delete(directory);
        }
    }

    private static void onMain(Runnable check) throws Exception {
        FutureTask<Void> task = new FutureTask<>(check, null);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task);
        task.get();
    }

    private static Context themed(int style) {
        return new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), style);
    }

    private static void measure(View view, int widthDp) {
        int width = Math.round(ViewUtil.dpToPx(view.getContext(), (float) widthDp));
        view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }
}
