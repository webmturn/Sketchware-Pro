package pro.sketchware.util.locale;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.image.AddImageActivity;
import pro.sketchware.activities.editor.property.PropertyGravityItem;
import pro.sketchware.activities.editor.property.PropertyMeasureItem;
import pro.sketchware.activities.editor.property.PropertySelectorItem;
import pro.sketchware.activities.editor.property.PropertyStringPairSelectorItem;
import pro.sketchware.activities.editor.property.PropertyStringSelectorItem;
import pro.sketchware.beans.ColorBean;
import pro.sketchware.beans.ImageBean;
import pro.sketchware.beans.TextBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.block.view.BlockViewContainer;
import pro.sketchware.core.project.SketchwareConstants;
import pro.sketchware.widgets.ColorPickerDialog;

/** Checks rendered editor labels alongside the canonical tags, values and color callbacks. */
@RunWith(AndroidJUnit4.class)
public class EditorDisplayLocalizationTest {
    private Context context(Locale locale) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return new ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_SketchwarePro);
    }

    @Test public void integerPropertyRowsAndSummariesKeepTheirStoredValues() {
        onMain(() -> {
            Context chinese = context(Locale.SIMPLIFIED_CHINESE);
            for (String property : new String[]{"property_orientation", "property_text_style", "property_ime_option",
                    "property_input_type", "property_spinner_mode", "property_choice_mode", "property_first_day_of_week"}) {
                PropertySelectorItem item = new PropertySelectorItem(chinese, false);
                item.setKey(property);
                for (Pair<Integer, String> option : SketchwareConstants.getPropertyPairs(property)) {
                    String original = option.second;
                    RadioButton row = (RadioButton) invoke(item, "getOption", Pair.class, option);
                    assertEquals(option.first, row.getTag());
                    assertNotEquals(property + ": " + original, original, row.getText().toString());
                    item.setValue(option.first);
                    assertEquals(option.first.intValue(), item.getValue());
                    assertEquals(row.getText().toString(), text(item, R.id.tv_value));
                    assertEquals(original, SketchwareConstants.getPropertyValueString(property, option.first));
                    assertEquals(original, PropertyDisplayNames.get(context(Locale.ENGLISH), property, original));
                }
            }
            assertEquals("星期日", PropertyDisplayNames.get(chinese, "property_first_day_of_week", "Sunday"));
            assertEquals("numberSigned|numberDecimal", SketchwareConstants.getPropertyValueString("property_input_type", TextBean.INPUT_TYPE_NUMBER_SIGNED_DECIMAL));
        });
    }

    @Test public void stringPropertyRowsKeepStyleAndScaleIdentifiers() {
        onMain(() -> {
            Context chinese = context(Locale.SIMPLIFIED_CHINESE);
            PropertyStringPairSelectorItem style = new PropertyStringPairSelectorItem(chinese, false);
            style.setKey("property_progressbar_style");
            for (Pair<String, String> option : SketchwareConstants.getStringPropertyPairs("property_progressbar_style")) {
                RadioButton row = (RadioButton) invoke(style, "getOption", Pair.class, option);
                assertEquals(option.first, row.getTag());
                style.setValue(option.first);
                assertEquals(option.first, style.getValue());
                assertEquals(row.getText().toString(), text(style, R.id.tv_value));
                assertNotEquals(option.second, row.getText().toString());
            }
            style.setValue(ViewBean.PROGRESSBAR_STYLE_HORIZONTAL);
            assertEquals("水平", text(style, R.id.tv_value));
            style.setValue("?attr/my_custom_style");
            assertEquals("?attr/my_custom_style", text(style, R.id.tv_value));

            for (String property : new String[]{"property_scale_type", "property_indeterminate", "property_ad_size"}) {
                PropertyStringSelectorItem item = new PropertyStringSelectorItem(chinese, false);
                item.setKey(property);
                String[] options = switch (property) {
                    case "property_scale_type" -> SketchwareConstants.IMAGE_SCALE_TYPES;
                    case "property_indeterminate" -> SketchwareConstants.BOOLEAN_OPTIONS;
                    default -> SketchwareConstants.AD_SIZES;
                };
                for (String option : options) {
                    RadioButton row = (RadioButton) invoke(item, "getOption", String.class, option);
                    assertEquals(option, row.getTag());
                    item.setValue(option);
                    assertEquals(option, item.getValue());
                    assertEquals(row.getText().toString(), text(item, R.id.tv_value));
                    assertNotEquals(option, row.getText().toString());
                }
                item.setValue("CUSTOM_OPTION");
                assertEquals("CUSTOM_OPTION", text(item, R.id.tv_value));
            }
        });
    }

    @Test public void sizeAndGravitySummariesPreserveNumericDataAndCanonicalConstants() {
        onMain(() -> {
            Context chinese = context(Locale.SIMPLIFIED_CHINESE);
            PropertyMeasureItem size = new PropertyMeasureItem(chinese, false);
            size.setKey("property_layout_width");
            size.setValue(ViewGroup.LayoutParams.MATCH_PARENT);
            assertEquals("填满父容器", text(size, R.id.tv_value));
            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, size.getValue());
            size.setValue(ViewGroup.LayoutParams.WRAP_CONTENT);
            assertEquals("包裹内容", text(size, R.id.tv_value));
            size.setValue(48);
            assertEquals("48dp", text(size, R.id.tv_value));
            assertEquals(48, size.getValue());
            PropertyGravityItem gravity = new PropertyGravityItem(chinese, false);
            gravity.setKey("property_layout_gravity");
            int value = Gravity.LEFT | Gravity.TOP;
            gravity.setValue(value);
            assertEquals(value, gravity.getValue());
            assertEquals("left, top", SketchwareConstants.gravityToString(value));
            assertTrue(text(gravity, R.id.tv_value).contains("左"));
            assertTrue(text(gravity, R.id.tv_value).contains("顶"));
            assertEquals("48dp", PropertyDisplayNames.get(chinese, "property_layout_width", "48dp"));
            assertEquals("Sunday", PropertyDisplayNames.get(chinese, "my_custom_property", "Sunday"));
        });
    }

    @Test public void collapsedStacksRenderBothBranchesAndEnglishPluralForms() {
        onMain(() -> {
            for (Locale locale : new Locale[]{Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH}) {
                Context ctx = context(locale);
                Map<Integer, BlockView> blocks = new HashMap<>();
                BlockViewContainer pane = new BlockViewContainer() {
                    @Override public BlockView getBlockByTag(int id) { return blocks.get(id); }
                    @Override public void updatePaneSize() {}
                };
                BlockView root = new BlockView(ctx, 1, "if %b", "e", "ifElse");
                String initializedSpec = root.spec;
                root.blockPane = pane;
                blocks.put(1, root);
                root.setCollapsed(true);
                assertEquals(collapseText(locale, 0), root.collapsedLabel.getText().toString());
                root.setCollapsed(false);
                for (int id = 2; id <= 5; id++) {
                    BlockView child = new BlockView(ctx, id, "test", " ", "testStatement");
                    child.blockPane = pane;
                    blocks.put(id, child);
                }
                root.setSubstack1Block(blocks.get(2));
                root.setCollapsed(true);
                assertEquals(collapseText(locale, 1), root.collapsedLabel.getText().toString());
                root.setCollapsed(false);
                blocks.get(2).setNextBlock(blocks.get(3));
                blocks.get(3).setNextBlock(blocks.get(4));
                root.setSubstack2Block(blocks.get(5));
                root.setCollapsed(true);
                root.setCollapsed2(true);
                assertEquals(collapseText(locale, 3), root.collapsedLabel.getText().toString());
                assertEquals(collapseText(locale, 1), root.collapsedLabel2.getText().toString());
                assertEquals(initializedSpec, root.spec);
                assertEquals(2, root.subStack1);
                assertEquals(5, root.subStack2);
                root.setCollapsed(false);
                root.setCollapsed2(false);
                assertEquals(View.VISIBLE, blocks.get(2).getVisibility());
                assertEquals(View.VISIBLE, blocks.get(5).getVisibility());
            }
        });
    }

    @Test public void imageBadgeAndColorPickerUseChineseAndReturnOriginalColors() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context base = instrumentation.getTargetContext();
        File fixture = new File(base.getCacheDir(), "EditorI18n." + UUID.randomUUID());
        assertTrue(fixture.mkdir());
        LocaleListCompat originalLocales = AppCompatDelegate.getApplicationLocales();
        AddImageActivity activity = null;
        try {
            onMain(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN")));
            Intent intent = new Intent(base, AddImageActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("dir_path", fixture.getAbsolutePath())
                    .putParcelableArrayListExtra("images", new ArrayList<ImageBean>());
            activity = (AddImageActivity) instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            AddImageActivity target = activity;
            onMain(() -> {
                invoke(target, "onMultipleImagesPicked", int.class, 3);
                assertEquals("+ 2 张", text(target.findViewById(android.R.id.content), R.id.tv_imgcnt));
                assertEquals(View.VISIBLE, target.findViewById(R.id.tv_imgcnt).getVisibility());
                invoke(target, "onMultipleImagesPicked", int.class, 2);
                assertEquals("+ 1 张", text(target.findViewById(android.R.id.content), R.id.tv_imgcnt));

                ColorPickerDialog picker = new ColorPickerDialog(target, Color.parseColor("#F44336"), true, true);
                ViewGroup groups = picker.getContentView().findViewById(R.id.layout_color_title);
                boolean foundRed = false, foundCustom = false;
                for (int i = 0; i < groups.getChildCount(); i++) {
                    String label = text(groups.getChildAt(i), R.id.tv_color_name);
                    foundRed |= label.equals("红色");
                    foundCustom |= label.equals("自定义");
                }
                assertTrue(foundRed);
                assertTrue(foundCustom);
                AtomicReference<Integer> selectedColor = new AtomicReference<>();
                AtomicReference<String> selectedName = new AtomicReference<>();
                picker.setColorPickerCallback(new ColorPickerDialog.OnColorPickedListener() {
                    @Override public void onColorPicked(int color) { selectedColor.set(color); }
                    @Override public void onResourceColorPicked(String name, int color) {
                        selectedName.set(name); selectedColor.set(color);
                    }
                });
                RecyclerView list = picker.getContentView().findViewById(R.id.color_list);
                View red = bindFirst(list.getAdapter(), list);
                assertEquals("红色", text(red, R.id.tv_color_name));
                red.performClick();
                assertEquals(Integer.valueOf(Color.parseColor("#F44336")), selectedColor.get());
                assertEquals("RED", SketchwareConstants.COLOR_RED[0].colorName);

                @SuppressWarnings("unchecked") ArrayList<ColorBean[]> palette =
                        (ArrayList<ColorBean[]>) field(picker, "colorGroups");
                for (int expected : new int[]{0, 0xffffff}) {
                    for (int i = 0; i < palette.size(); i++) {
                        if (palette.get(i).length > 0 && palette.get(i)[0].colorCode == expected) {
                            setField(picker, "currentGroupIndex", i);
                            View special = bindFirst(list.getAdapter(), list);
                            assertEquals(expected == 0 ? "TRANSPARENT" : "NONE", text(special, R.id.tv_color_code));
                            assertEquals(expected == 0 ? "透明" : "无", text(special, R.id.tv_color_name));
                            special.performClick();
                            assertEquals(Integer.valueOf(expected), selectedColor.get());
                        }
                    }
                }
                ArrayList<ColorPickerDialog.ResColor> resourceColors = new ArrayList<>();
                resourceColors.add(new ColorPickerDialog.ResColor("RED", Color.BLUE, -1));
                View resource = bindFirst(picker.new resColorsAdapter(resourceColors, -1), list);
                assertEquals("RED", text(resource, R.id.tvAttrName));
                resource.findViewById(R.id.container).performClick();
                assertEquals("RED", selectedName.get());
                assertEquals(Integer.valueOf(Color.BLUE), selectedColor.get());
                ColorPickerDialog.Attribute attr = new ColorPickerDialog.Attribute("colorPrimary", "Primary");
                assertEquals("主色：", attr.attrTitle(target));
                assertEquals("colorPrimary", attr.name());
                assertEquals("Primary Colors :", attr.attrTitle(context(Locale.ENGLISH)));
                picker.dismiss();
            });
        } finally {
            if (activity != null) {
                AddImageActivity target = activity;
                onMain(target::finish);
                instrumentation.waitForIdleSync();
            }
            onMain(() -> AppCompatDelegate.setApplicationLocales(originalLocales));
            assertTrue(fixture.delete());
        }
    }

    private static String collapseText(Locale locale, int count) {
        return locale.equals(Locale.ENGLISH) ? "... (" + count + (count == 1 ? " block)" : " blocks)")
                : "…（" + count + " 个积木）";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static View bindFirst(RecyclerView.Adapter adapter, RecyclerView parent) {
        RecyclerView.ViewHolder holder = adapter.createViewHolder(parent, adapter.getItemViewType(0));
        adapter.bindViewHolder(holder, 0);
        return holder.itemView;
    }

    private static String text(View view, int id) { return ((TextView) view.findViewById(id)).getText().toString(); }

    private static Object invoke(Object target, String name, Class<?> argType, Object arg) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, argType);
        method.setAccessible(true);
        return method.invoke(target, arg);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private interface Check { void run() throws Exception; }

    private static void onMain(Check check) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { check.run(); } catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Error) throw (Error) error;
        if (error != null) throw new AssertionError(error);
    }
}
