package pro.sketchware.activities.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.ViewEditor;
import pro.sketchware.activities.editor.view.item.ItemCardView;
import pro.sketchware.activities.editor.view.item.ItemMaterialButton;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.ViewUtil;
import pro.sketchware.util.io.EncryptedFileUtil;

@RunWith(AndroidJUnit4.class)
public class Material3WidgetPreviewTest {
    private static final String BACKGROUND = "preview_background";
    private static final int PRIMARY = 0xff244e40;

    @Test
    public void buttonStyleUpdatesAfterRemovingBackgroundImage() throws Exception {
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            var buttonBean = new ViewBean("button", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
            buttonBean.text.textColor = 0xffffff;
            seedPreviewProject(fixture, buttonBean);
            writeBackground(fixture);
            writeStyles(
                    fixture,
                    """
                    <resources>
                        <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
                            <item name="colorPrimary">#244E40</item>
                            <item name="colorOnPrimary">#FFFFFF</item>
                        </style>
                        <style name="ProjectOutline" parent="Widget.Material3.Button.OutlinedButton" />
                    </resources>
                    """);

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ViewEditor editor = activity.findViewById(R.id.view_editor);
                            ItemMaterialButton button = editor.findViewWithTag("button");
                            assertEquals(PRIMARY, button.getBackgroundTintList().getDefaultColor());
                            assertEquals(Color.WHITE, button.getCurrentTextColor());

                            ViewBean bean = button.getBean();
                            bean.layout.backgroundResource = BACKGROUND;
                            editor.selectView(bean);
                            button.setSelection(false);
                            int y = button.getHeight() / 2;
                            assertEquals(PRIMARY, pixel(button, button.getWidth() * 3 / 4, y));
                            assertEquals(
                                    Color.TRANSPARENT, pixel(button, button.getWidth() / 4, y));

                            bean.layout.backgroundResource = null;
                            bean.inject =
                                    "style=\"@style/ProjectOutline\" app:cornerRadius=\"12dp\"";
                            editor.selectView(bean);
                            button.setSelection(false);
                            assertEquals(
                                    Color.TRANSPARENT,
                                    button.getBackgroundTintList().getDefaultColor());
                            assertEquals(PRIMARY, button.getCurrentTextColor());
                            assertTrue(button.getStrokeWidth() > 0);
                            var bounds = new RectF(0, 0, button.getWidth(), button.getHeight());
                            float radius =
                                    button.getShapeAppearanceModel()
                                            .getTopLeftCornerSize()
                                            .getCornerSize(bounds);
                            assertEquals(ViewUtil.dpToPx(button.getContext(), 12f), radius, .5f);
                            assertEquals(
                                    Color.TRANSPARENT, pixel(button, button.getWidth() * 3 / 4, y));

                            bean.inject = "style=\"@style/Widget.Material3.Button.TextButton\"";
                            editor.selectView(bean);
                            assertEquals(0, button.getStrokeWidth());
                            assertEquals(
                                    Color.TRANSPARENT,
                                    button.getBackgroundTintList().getDefaultColor());
                            assertEquals(PRIMARY, button.getCurrentTextColor());

                            bean.inject = "";
                            editor.selectView(bean);
                            assertEquals(PRIMARY, button.getBackgroundTintList().getDefaultColor());
                            assertEquals(Color.WHITE, button.getCurrentTextColor());
                        });
            }
        }
    }

    @Test
    public void backgroundTintFollowsTheProjectStyleAndButtonOverrides() throws Exception {
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            var bean = new ViewBean("button", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
            seedPreviewProject(fixture, bean);
            writeStyles(
                    fixture,
                    """
                    <resources>
                        <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
                            <item name="materialButtonStyle">@style/ProjectButton</item>
                        </style>
                        <style name="ProjectButton" parent="Widget.Material3.Button.OutlinedButton">
                            <item name="backgroundTint">#244E40</item>
                        </style>
                    </resources>
                    """);

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ViewEditor editor = activity.findViewById(R.id.view_editor);
                            ItemMaterialButton button = editor.findViewWithTag("button");
                            assertTrue(button.getStrokeWidth() > 0);
                            assertEquals(PRIMARY, button.getBackgroundTintList().getDefaultColor());

                            ViewBean current = button.getBean();
                            current.layout.backgroundColor = Color.RED;
                            editor.selectView(current);
                            assertEquals(
                                    Color.RED, button.getBackgroundTintList().getDefaultColor());

                            current.inject = "app:backgroundTint=\"#0000FF\"";
                            editor.selectView(current);
                            assertEquals(
                                    Color.BLUE, button.getBackgroundTintList().getDefaultColor());

                            current.layout.backgroundColor = 0xffffff;
                            current.inject = "style=\"@style/Widget.Material3.Button.TextButton\"";
                            editor.selectView(current);
                            assertEquals(0, button.getStrokeWidth());
                            assertEquals(
                                    Color.TRANSPARENT,
                                    button.getBackgroundTintList().getDefaultColor());
                        });
            }
        }
    }

    @Test
    public void outlinedStylesResolveFromThemeAndExpressive() throws Exception {
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            var bean = new ViewBean("button", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
            bean.text.textColor = 0xffffff;
            bean.inject = "style=\"?attr/materialButtonOutlinedStyle\"";
            seedPreviewProject(fixture, bean);
            writeStyles(
                    fixture,
                    """
                    <resources>
                        <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
                            <item name="colorPrimary">#244E40</item>
                            <item name="colorOnSurfaceVariant">#0000FF</item>
                            <item name="colorOutlineVariant">#FF0000</item>
                        </style>
                    </resources>
                    """);

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ViewEditor editor = activity.findViewById(R.id.view_editor);
                            ItemMaterialButton button = editor.findViewWithTag("button");
                            assertTrue(button.getStrokeWidth() > 0);
                            assertEquals(
                                    Color.TRANSPARENT,
                                    button.getBackgroundTintList().getDefaultColor());
                            assertEquals(PRIMARY, button.getCurrentTextColor());

                            ViewBean current = button.getBean();
                            current.inject =
                                    "style=\"@style/Widget.Material3Expressive.Button.OutlinedButton\"";
                            editor.selectView(current);
                            assertTrue(button.getStrokeWidth() > 0);
                            assertEquals(
                                    Color.TRANSPARENT,
                                    button.getBackgroundTintList().getDefaultColor());
                            assertEquals(Color.BLUE, button.getCurrentTextColor());
                            assertEquals(Color.RED, button.getStrokeColor().getDefaultColor());
                        });
            }
        }
    }

    @Test
    public void buttonUsesProjectColorSelectorForDefaultAndDisabledStates() throws Exception {
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            var bean = new ViewBean("button", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
            bean.inject =
                    "app:backgroundTint=\"@color/button_colors\" android:textColor=\"@color/button_colors\"";
            seedPreviewProject(fixture, bean);
            var res = fixture.data.toPath().resolve("files/resource");
            Files.createDirectories(res.resolve("values"));
            Files.write(
                    res.resolve("values/colors.xml"),
                    "<resources><color name=\"button_default\">#244E40</color></resources>"
                            .getBytes(StandardCharsets.UTF_8));
            Files.createDirectories(res.resolve("color"));
            Files.write(
                    res.resolve("color/button_colors.xml"),
                    """
                    <selector xmlns:android="http://schemas.android.com/apk/res/android">
                        <item android:state_enabled="false" android:color="#FF0000" android:alpha="0.5"/>
                        <item android:color="@color/button_default"/>
                    </selector>
                    """
                            .getBytes(StandardCharsets.UTF_8));

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ViewEditor editor = activity.findViewById(R.id.view_editor);
                            ItemMaterialButton button = editor.findViewWithTag("button");
                            assertEquals(PRIMARY, button.getBackgroundTintList().getDefaultColor());
                            assertEquals(PRIMARY, button.getCurrentTextColor());

                            button.setEnabled(false);
                            assertEquals(
                                    0x80ff0000,
                                    button.getBackgroundTintList()
                                            .getColorForState(button.getDrawableState(), 0));
                            assertEquals(0x80ff0000, button.getCurrentTextColor());
                        });
            }
        }
    }

    @Test
    public void cardRestoresColorAndUpdatesInsetsAfterRemovingBackgroundImage() throws Exception {
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            var cardBean = new ViewBean("paper", ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW);
            cardBean.layout.backgroundColor = 0xffffff;
            cardBean.inject = "app:cardBackgroundColor=\"#FFF9F2\" app:cardCornerRadius=\"18dp\"";
            seedPreviewProject(fixture, cardBean);
            writeBackground(fixture);

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ViewEditor editor = activity.findViewById(R.id.view_editor);
                            ItemCardView card = editor.findViewWithTag("paper");
                            assertEquals(
                                    0xfffff9f2, card.getCardBackgroundColor().getDefaultColor());
                            assertEquals(
                                    ViewUtil.dpToPx(card.getContext(), 18f), card.getRadius(), .5f);
                            int x = card.getWidth() / 2;
                            assertEquals(255, Color.alpha(pixel(card, x, 0)));

                            ViewBean bean = card.getBean();
                            bean.layout.backgroundResource = BACKGROUND;
                            editor.selectView(bean);
                            card.setSelection(false);
                            assertEquals(
                                    Color.BLUE,
                                    pixel(card, card.getWidth() * 3 / 4, card.getHeight() / 2));

                            bean.layout.backgroundResource = null;
                            bean.inject +=
                                    " app:cardUseCompatPadding=\"true\" app:cardElevation=\"8dp\"";
                            editor.selectView(bean);
                            card.setSelection(false);
                            assertEquals(
                                    0xfffff9f2, card.getCardBackgroundColor().getDefaultColor());
                            assertEquals(0, Color.alpha(pixel(card, x, 0)));
                        });
            }
        }
    }

    private static void writeStyles(ProjectEditorPersistenceTest.Fixture fixture, String xml)
            throws Exception {
        var values =
                Files.createDirectories(fixture.data.toPath().resolve("files/resource/values"));
        Files.write(values.resolve("styles.xml"), xml.getBytes(StandardCharsets.UTF_8));
    }

    private static void seedPreviewProject(
            ProjectEditorPersistenceTest.Fixture fixture, ViewBean bean) throws Exception {
        fixture.seed(false, false);
        var files = fixture.data.toPath();
        var compat = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT);
        compat.useYn = ProjectLibraryBean.LIB_USE_Y;
        compat.configurations.put("material3", true);
        compat.configurations.put("theme", "Light");
        var encryption = new EncryptedFileUtil();
        var gson = ProjectDataStore.gson;
        Files.write(
                files.resolve("library"),
                encryption.encryptString("@compat\n" + gson.toJson(compat) + "\n", true));
        bean.parent = "root";
        bean.layout.width = 160;
        bean.layout.height = 64;
        Files.write(
                files.resolve("view"),
                encryption.encryptString("@main.xml\n" + gson.toJson(bean) + "\n", true));
    }

    private static void writeBackground(ProjectEditorPersistenceTest.Fixture fixture)
            throws Exception {
        String filename = BACKGROUND + ".png";
        var resource =
                new ProjectResourceBean(
                        ProjectResourceBean.PROJECT_RES_TYPE_FILE, BACKGROUND, filename);
        Files.write(
                fixture.data.toPath().resolve("resource"),
                new EncryptedFileUtil()
                        .encryptString(
                                "@images\n"
                                        + ProjectDataStore.gson.toJson(resource)
                                        + "\n@sounds\n@fonts\n",
                                true));
        var bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        try {
            var paint = new Paint();
            paint.setColor(Color.BLUE);
            new Canvas(bitmap).drawRect(8, 0, 16, 16, paint);
            var path = Paths.get(SketchwarePaths.getImagesPath(), fixture.id, filename);
            try (var output = Files.newOutputStream(path)) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            }
        } finally {
            bitmap.recycle();
        }
    }

    private static int pixel(View view, int x, int y) {
        var bitmap =
                Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        try {
            view.draw(new Canvas(bitmap));
            return bitmap.getPixel(x, y);
        } finally {
            bitmap.recycle();
        }
    }
}
