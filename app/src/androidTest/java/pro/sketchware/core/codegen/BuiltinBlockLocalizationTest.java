package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.core.block.BlockSpecParser;
import pro.sketchware.core.block.view.BlockColorMapper;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.block.view.DefinitionBlockView;
import pro.sketchware.core.block.view.FieldBlockView;
import pro.sketchware.util.locale.LocalizedDisplayNames;

@RunWith(AndroidJUnit4.class)
public class BuiltinBlockLocalizationTest {
    private Context context(Locale locale) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(locale);
        return base.createConfigurationContext(configuration);
    }

    @Test public void chineseSpecsKeepEveryParameterAndOriginalDefinition() {
        Context english = context(Locale.ENGLISH);
        Context chinese = context(Locale.SIMPLIFIED_CHINESE);
        ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
        BuiltInBlockDefinitions.builtInBlocks(definitions);
        int translated = 0;
        int checked = 0;
        for (HashMap<String, Object> definition : definitions) {
            String opcode = (String) definition.get("name");
            for (String field : new String[]{"spec", "spec2"}) {
                String original = (String) definition.get(field);
                if (original == null || original.isEmpty()) continue;
                boolean secondary = field.equals("spec2");
                String display = BuiltinBlockLocalization.getSpec(chinese, opcode, original, secondary);
                assertEquals(opcode + ": " + field, BlockSpecParser.parseParameters(original),
                        BlockSpecParser.parseParameters(display));
                assertEquals(original, definition.get(field));
                assertEquals(original, BuiltinBlockLocalization.getSpec(english, opcode, original, secondary));
                if (!display.equals(original)) translated++;
                checked++;
            }
        }
        assertEquals(308, checked);
        assertTrue("Built-in labels should be translated", translated > 280);
    }

    @Test public void customSpecsArePreservedEvenWhenOpcodeMatchesABuiltin() {
        Context chinese = context(Locale.SIMPLIFIED_CHINESE);
        ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
        BuiltInBlockDefinitions.builtInBlocks(definitions);
        String builtin = (String) definitions.get(0).get("name");
        String custom = "my custom label %s.value %m.view";
        assertEquals(custom, BuiltinBlockLocalization.getSpec(chinese, builtin, custom, false));
        assertEquals(custom, BuiltinBlockLocalization.getSpec(chinese, "userBlock", custom, false));
        assertNull(BuiltinBlockLocalization.getSpec(chinese, builtin, null, false));
    }

    @Test public void blockViewRendersAndSearchesChineseWithoutChangingItsStoredSpec() {
        Context chinese = new ContextThemeWrapper(context(Locale.SIMPLIFIED_CHINESE), R.style.Theme_SketchwarePro);
        String original = "Activity hasExtra key %s";
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            BlockView view = new BlockView(chinese, 1, original, "b", "intentHasExtra");
            assertEquals(original, view.spec);
            assertTrue(view.matchesPaletteSearchQuery("附加数据"));
            assertTrue(view.matchesPaletteSearchQuery("hasextra"));
            int fields = 0;
            boolean translatedLabel = false;
            for (View child : view.specViews) {
                if (child instanceof FieldBlockView) fields++;
                else if (child instanceof TextView text) {
                    translatedLabel |= text.getText().toString().contains("附加数据");
                }
            }
            assertTrue(translatedLabel);
            assertEquals(1, fields);
        });
    }

    @Test public void displayNamesKeepCanonicalProjectTypesAndUserNames() {
        Context chinese = context(Locale.SIMPLIFIED_CHINESE);
        String canonical = BlockColorMapper.getListTypeName(2);
        assertEquals("List String", canonical);
        assertEquals("字符串列表", LocalizedDisplayNames.get(chinese, canonical));
        assertEquals("基础 Activity", LocalizedDisplayNames.get(chinese, "Basic Activity"));
        assertEquals("My custom preset", LocalizedDisplayNames.get(chinese, "My custom preset"));
        assertEquals(canonical, BlockColorMapper.getListTypeName(2));
    }

    @Test public void customClassNamesStayVerbatimInVariablesArgumentsAndCollections() {
        Context chinese = new ContextThemeWrapper(context(Locale.SIMPLIFIED_CHINESE), R.style.Theme_SketchwarePro);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (String name : new String[]{"Image", "Color", "Default", "CustomView", "View", "Map", "List"}) {
                for (String opcode : new String[]{"getVar", "getArg"}) {
                    BlockView variable = new BlockView(chinese, 1, "value", "v", name, opcode);
                    assertEquals(name + " : value", ((TextView) variable.specViews.get(0)).getText().toString());
                    assertEquals(name, variable.getBean().typeName);
                    assertEquals("value", variable.getBean().spec);
                }
                DefinitionBlockView collection = new DefinitionBlockView(chinese, "v", name,
                        "getVar", "SavedCollection", new ArrayList<>());
                assertEquals(name + " : SavedCollection", ((TextView) collection.specViews.get(0)).getText().toString());
                assertEquals("SavedCollection", collection.getCollectionName());
            }
            BlockView list = new BlockView(chinese, 2, "items", "l", "List String", "getVar");
            assertEquals("字符串列表 : items", ((TextView) list.specViews.get(0)).getText().toString());
            assertEquals("List String", list.getBean().typeName);
            BlockView map = new BlockView(chinese, 3, "data", "a", "Map", "getArg");
            assertEquals("映射 : data", ((TextView) map.specViews.get(0)).getText().toString());
        });
    }

    @Test public void selectorLabelsUseBuiltinIdsAndPreserveCollidingCustomNames() {
        Context chinese = new ContextThemeWrapper(context(Locale.SIMPLIFIED_CHINESE), R.style.Theme_SketchwarePro);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (String name : new String[]{"Image", "Color", "Default", "CustomView", "View", "Map", "String", "Number"}) {
                FieldBlockView field = new FieldBlockView(chinese, "m", name);
                assertEquals(name + " : ", field.dropdownLabel.getText().toString());
                assertEquals(name, field.getMenuName());
            }
            FieldBlockView image = new FieldBlockView(chinese, "m", "resource");
            assertEquals("图片 : ", image.dropdownLabel.getText().toString());
            assertEquals("resource", image.getMenuName());
            FieldBlockView color = new FieldBlockView(chinese, "m", "color");
            assertEquals("颜色 : ", color.dropdownLabel.getText().toString());
            assertEquals("color", color.getMenuName());
            FieldBlockView list = new FieldBlockView(chinese, "m", "listStr");
            assertEquals("字符串列表 : ", list.dropdownLabel.getText().toString());
            assertEquals("listStr", list.getMenuName());
        });
    }

    @Test public void localeSwitchRefreshesCachedBlockAndEventTranslations() throws Exception {
        Context english = context(Locale.ENGLISH);
        Context chinese = context(Locale.SIMPLIFIED_CHINESE);
        StringResource translations = new StringResource();
        // Isolate built-in locale switching from an existing user block-translation file.
        Field overrides = StringResource.class.getDeclaredField("blockTranslations");
        overrides.setAccessible(true);
        HashMap<String, String> fixture = new HashMap<>();
        fixture.put("__locale_test__", "");
        overrides.set(translations, fixture);
        String before = translations.getEventTranslation(english, "onClick");
        String after = translations.getEventTranslation(chinese, "onClick");
        assertNotEquals(before, after);
        assertEquals(before, translations.getEventTranslation(english, "onClick"));
        assertEquals(chinese.getString(R.string.root_spec_on_clicked_head), after);
    }

    @Test public void translatedFormatMessagesKeepArgumentTypesAndLiteralPercent() {
        Context chinese = context(Locale.SIMPLIFIED_CHINESE);
        String syntaxError = chinese.getString(R.string.block_parameter_syntax_error, 5);
        assertTrue(syntaxError.contains("%m"));
        assertTrue(syntaxError.contains("5"));
        String progress = chinese.getString(R.string.build_progress_java_incremental, 2, 10);
        assertTrue(progress.contains("10"));
        assertTrue(progress.contains("2"));
        assertEquals("Tab 1", chinese.getString(R.string.editor_preview_tab1));
        assertEquals("Error: ", context(Locale.ENGLISH).getString(R.string.library_download_error_prefix));
    }
}
