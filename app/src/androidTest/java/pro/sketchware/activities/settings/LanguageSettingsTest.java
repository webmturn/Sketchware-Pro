package pro.sketchware.activities.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import dev.pranav.filepicker.FilePickerCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.image.AddImageActivity;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.codegen.StringResource;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.locale.LanguageOverrideManager;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class LanguageSettingsTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private LocaleListCompat originalLocales;
    private int originalNightMode;
    private Map<String, ?> originalOverrides;
    private ActivityScenario<SettingsActivity> scenario;

    @Before
    public void setUp() {
        // AppCompat restores persisted locales when the first activity attaches after a cold start.
        try (ActivityScenario<SettingsActivity> warm =
                ActivityScenario.launch(
                        new Intent(context, SettingsActivity.class)
                                .putExtra(
                                        SettingsActivity.FRAGMENT_TAG_EXTRA,
                                        SettingsActivity.LANGUAGE_SETTINGS_FRAGMENT))) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            originalLocales = AppCompatDelegate.getApplicationLocales();
        }
        originalNightMode = AppCompatDelegate.getDefaultNightMode();
        originalOverrides = new HashMap<>(overridePrefs().getAll());
        overridePrefs().edit().clear().commit();
        LanguageOverrideManager.getInstance().init(context);
        onMain(
                () ->
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("en")));
        String nightMode =
                InstrumentationRegistry.getArguments().getString("languageSettingsNightMode");
        if ("light".equals(nightMode))
            onMain(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO));
        else if ("dark".equals(nightMode))
            onMain(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    @After
    public void tearDown() {
        if (scenario != null) scenario.close();
        SharedPreferences.Editor editor = overridePrefs().edit().clear();
        for (Map.Entry<String, ?> entry : originalOverrides.entrySet()) {
            if (entry.getValue() instanceof String value) editor.putString(entry.getKey(), value);
            else if (entry.getValue() instanceof Integer value)
                editor.putInt(entry.getKey(), value);
        }
        editor.commit();
        LanguageOverrideManager.getInstance().init(context);
        onMain(() -> AppCompatDelegate.setApplicationLocales(originalLocales));
        onMain(() -> AppCompatDelegate.setDefaultNightMode(originalNightMode));
    }

    @Test
    public void openingAndRecreatingPreservesRegionalLocale() throws Exception {
        onMain(
                () ->
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("zh-CN")));
        launch();
        assertSelected("中文", "zh-CN");
        scenario.recreate();
        assertSelected("中文", "zh-CN");
        selectLanguage("zh");
        scenario.onActivity(
                activity -> {
                    assertEquals(
                            View.VISIBLE,
                            activity.findViewById(R.id.override_info).getVisibility());
                    assertEquals(
                            View.GONE,
                            activity.findViewById(R.id.translation_packs).getVisibility());
                    assertTrue(activity.findViewById(R.id.btn_import_strings).isEnabled());
                    assertTrue(activity.findViewById(R.id.btn_export_strings).isEnabled());
                });
        assertSelected("中文", "zh-CN");
        captureScreenshot("language-settings-zh.png");
    }

    @Test
    public void selectingExternalLanguageOverridesUiAndBlockTextUntilBuiltinIsSelected() {
        var translations = LanguageOverrideManager.getInstance();
        var pack =
                translations.addPack(
                        context,
                        "Fixture",
                        Locale.CHINESE,
                        Map.of(
                                "language_settings_import_file", "Fixture import button",
                                "root_spec_initialize_head", "Fixture root",
                                "block_add_list_str_head", "Fixture append"));
        assertNotNull(pack);
        launch();
        selectPack("Fixture");
        scenario.onActivity(
                activity -> {
                    assertEquals("zh", AppCompatDelegate.getApplicationLocales().toLanguageTags());
                    assertEquals(
                            "Fixture",
                            ((TextView) activity.findViewById(R.id.current_locale_display))
                                    .getText()
                                    .toString());
                    assertEquals(
                            "Fixture import button",
                            ((TextView) activity.findViewById(R.id.btn_import_strings))
                                    .getText()
                                    .toString());
                    var labels = StringResource.getInstance();
                    assertEquals(
                            "Fixture root",
                            labels.getRootSpecTranslation(activity, "onCreate", "initializeLogic"));
                    assertTrue(
                            labels.getEventTranslation(activity, "addListStr")
                                    .startsWith("Fixture append "));
                    assertTrue(labels.getEventTranslation(activity, "addListStr").contains(" 到 "));
                });
        selectLanguage("zh");
        assertSelected("中文", "zh");
        scenario.onActivity(
                activity -> {
                    assertNull(translations.getActivePack(activity.getResources()));
                    assertEquals(
                            "活动创建时",
                            StringResource.getInstance()
                                    .getEventTranslation(activity, "initializeLogic"));
                });
        selectLanguage("en");
        assertSelected("English", "en");
        scenario.onActivity(
                activity ->
                        assertEquals(
                                "On activity create",
                                StringResource.getInstance()
                                        .getEventTranslation(activity, "initializeLogic")));
        assertEquals(1, translations.getPacks().size());
        selectPack("Fixture");
        scenario.recreate();
        scenario.onActivity(
                activity ->
                        assertEquals(
                                pack.id(),
                                translations.getActivePack(activity.getResources()).id()));
    }

    @Test
    public void importAndExportTranslationPack() throws Exception {
        File input = File.createTempFile("translation-fixture", ".xml", context.getCacheDir());
        Files.write(
                input.toPath(),
                """
                <resources name="Fixture" fallback="en">
                    <string name="language_settings_fallback_value">Base language: %s</string>
                    <string name="root_spec_initialize_head">Fixture root</string>
                    <string name="block_add_list_str_head">fixture \\'append\\'</string>
                    <string name="common_word_cancel">?example</string>
                    <string name="common_word_add">@string/example</string>
                    <string name="common_word_yes">first\\rsecond</string>
                    <string name="language_settings_system_locale">Locale %s %#%</string>
                </resources>
                """
                        .getBytes(StandardCharsets.UTF_8));
        launch();
        File exported = null;
        try {
            scenario.onActivity(
                    activity -> activity.findViewById(R.id.btn_import_strings).performClick());
            idle();
            scenario.onActivity(
                    activity -> {
                        var settings =
                                activity.getSupportFragmentManager()
                                        .findFragmentById(R.id.settings_fragment_container);
                        var picker =
                                settings.getChildFragmentManager()
                                        .findFragmentByTag("filePickerDialog");
                        assertNotNull(picker);
                        try {
                            for (var field : picker.getClass().getDeclaredFields()) {
                                if (!FilePickerCallback.class.isAssignableFrom(field.getType()))
                                    continue;
                                field.setAccessible(true);
                                var callback = (FilePickerCallback) field.get(picker);
                                ((DialogFragment) picker).dismissNow();
                                callback.onFileSelected(input);
                                return;
                            }
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError(e);
                        }
                        throw new AssertionError("File picker callback missing");
                    });
            captureScreenshot("language-settings-import.png");
            onMain(() -> clickDialogButton(android.R.id.button1));
            idle();
            scenario.onActivity(
                    activity -> {
                        var pack =
                                LanguageOverrideManager.getInstance()
                                        .getActivePack(activity.getResources());
                        assertNotNull(pack);
                        assertEquals(
                                "fixture 'append'", pack.strings().get("block_add_list_str_head"));
                        assertEquals("?example", activity.getString(R.string.common_word_cancel));
                        assertEquals(
                                "@string/example", activity.getString(R.string.common_word_add));
                        assertEquals("first\rsecond", activity.getString(R.string.common_word_yes));
                        assertNull(pack.strings().get("language_settings_system_locale"));
                        assertEquals(
                                "Base language: English",
                                ((TextView)
                                                packRow(activity, "Fixture")
                                                        .findViewById(R.id.language_summary))
                                        .getText()
                                        .toString());
                    });
            captureScreenshot("language-settings-pack.png");
            File directory = new File(SketchwarePaths.getSystemI18nPath());
            File[] existing = directory.listFiles();
            var previous = existing == null ? java.util.Set.<File>of() : java.util.Set.of(existing);
            clickPackAction("Fixture", R.string.language_settings_export_file);
            File[] additions =
                    directory.listFiles(
                            file ->
                                    !previous.contains(file)
                                            && file.getName().startsWith("Fixture-en-"));
            assertNotNull(additions);
            assertEquals(1, additions.length);
            exported = additions[0];
            String xml = new String(Files.readAllBytes(exported.toPath()), StandardCharsets.UTF_8);
            assertTrue(xml.contains("Base language: %s"));
            assertTrue(xml.contains("fixture \\'append\\'"));
            assertTrue(xml.contains("\\?example"));
            assertTrue(xml.contains("\\@string/example"));
            assertTrue(xml.contains("first\\u000dsecond"));
            clickPackAction("Fixture", R.string.language_settings_export_file);
            assertEquals(
                    1,
                    directory.listFiles(
                                    file ->
                                            !previous.contains(file)
                                                    && file.getName().startsWith("Fixture-en-"))
                            .length);
        } finally {
            input.delete();
            if (exported != null) exported.delete();
        }
    }

    @Test
    public void packTranslatesXmlLabelsAndHints() {
        var manager = LanguageOverrideManager.getInstance();
        var pack =
                manager.addPack(
                        context,
                        "Fixture",
                        Locale.ENGLISH,
                        Map.of(
                                "logic_variable_type_number", "Fixture number",
                                "listener_independent_class", "Fixture switch",
                                "logic_editor_filter_reference_name", "Fixture search"));
        assertNotNull(pack);
        manager.select(context, pack);
        launch();
        scenario.onActivity(
                activity -> {
                    var inflater = activity.getLayoutInflater();
                    View variables = inflater.inflate(R.layout.logic_popup_add_variable, null);
                    View listeners = inflater.inflate(R.layout.dialog_add_new_listener, null);
                    View references = inflater.inflate(R.layout.variable_references_sheet, null);
                    assertEquals(
                            "Fixture number",
                            ((TextView) variables.findViewById(R.id.rb_int)).getText().toString());
                    assertEquals(
                            "Fixture switch",
                            ((TextView)
                                            listeners.findViewById(
                                                    R.id.listener_is_independent_class_or_method))
                                    .getText()
                                    .toString());
                    assertEquals(
                            "Fixture search",
                            ((TextView) references.findViewById(R.id.reference_query))
                                    .getHint()
                                    .toString());
                });
    }

    @Test
    public void translucentImagePickerInheritsTranslationInflater() {
        var manager = LanguageOverrideManager.getInstance();
        var pack =
                manager.addPack(
                        context,
                        "Fixture",
                        Locale.ENGLISH,
                        Map.of(
                                "design_manager_image_title_add_image", "Fixture image",
                                "logic_variable_type_number", "Fixture number"));
        assertNotNull(pack);
        manager.select(context, pack);
        Intent intent =
                new Intent(context, AddImageActivity.class)
                        .putExtra("dir_path", context.getCacheDir().getAbsolutePath())
                        .putParcelableArrayListExtra(
                                "images", new ArrayList<ProjectResourceBean>());
        try (ActivityScenario<AddImageActivity> image = ActivityScenario.launch(intent)) {
            image.onActivity(
                    activity -> {
                        assertEquals(
                                "Fixture image",
                                ((TextView) activity.findViewById(R.id.tv_add_photo))
                                        .getText()
                                        .toString());
                        View variables =
                                activity.getLayoutInflater()
                                        .inflate(R.layout.logic_popup_add_variable, null);
                        assertEquals(
                                "Fixture number",
                                ((TextView) variables.findViewById(R.id.rb_int))
                                        .getText()
                                        .toString());
                    });
        }
    }

    @Test
    public void legacyImportUsesSelectedLanguageAndKeepsRejectedEntries() throws Exception {
        onMain(
                () ->
                        AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags("zh")));
        overridePrefs()
                .edit()
                .putString("str_common_word_cancel", "Fixture cancel")
                .putString("str_language_settings_system_locale", "Missing placeholder")
                .putInt("override_count", 2)
                .commit();
        var manager = LanguageOverrideManager.getInstance();
        // Load twice to exercise the migrated JSON as well as the legacy preference keys.
        manager.init(context);
        manager.init(context);
        launch();
        scenario.onActivity(
                activity -> {
                    assertEquals("zh", AppCompatDelegate.getApplicationLocales().toLanguageTags());
                    var pack = manager.getActivePack(activity.getResources());
                    assertNotNull(pack);
                    assertEquals("zh", pack.fallbackTag());
                    assertEquals("Fixture cancel", activity.getString(R.string.common_word_cancel));
                    assertEquals(
                            Map.of("language_settings_system_locale", "Missing placeholder"),
                            pack.rejectedStrings());
                    assertNull(pack.strings().get("language_settings_system_locale"));
                });
        File file = null;
        try {
            var pack = manager.getPacks().get(0);
            File directory = new File(SketchwarePaths.getSystemI18nPath());
            file = new File(directory, pack.name() + "-zh-previous-import.xml");
            byte[] original = file.exists() ? Files.readAllBytes(file.toPath()) : null;
            try {
                clickPackAction(pack.name(), R.string.language_settings_export_file);
                assertTrue(
                        new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
                                .contains("Missing placeholder"));
            } finally {
                if (original != null) {
                    Files.write(file.toPath(), original);
                    file = null;
                }
            }
        } finally {
            if (file != null) file.delete();
        }
    }

    @Test
    public void importButtonOpensPickerWithoutChangingLanguage() {
        launch();
        scenario.onActivity(
                activity -> activity.findViewById(R.id.btn_import_strings).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(
                activity -> {
                    Fragment settings =
                            activity.getSupportFragmentManager()
                                    .findFragmentById(R.id.settings_fragment_container);
                    assertNotNull(settings);
                    DialogFragment picker =
                            (DialogFragment)
                                    settings.getChildFragmentManager()
                                            .findFragmentByTag("filePickerDialog");
                    assertNotNull(picker);
                    assertTrue(picker.requireDialog().isShowing());
                    picker.dismissNow();
                });
        assertSelected("English", "en");
        assertTrue(LanguageOverrideManager.getInstance().getPacks().isEmpty());
    }

    @Test
    public void removingPackRequiresConfirmation() {
        var translations = LanguageOverrideManager.getInstance();
        assertNotNull(
                translations.addPack(
                        context,
                        "Fixture",
                        Locale.ENGLISH,
                        Map.of("common_word_cancel", "Fixture cancel")));
        var retained =
                translations.addPack(
                        context,
                        "Keep",
                        Locale.ENGLISH,
                        Map.of("common_word_cancel", "Keep cancel"));
        assertNotNull(retained);
        launch();
        clickPackAction("Fixture", R.string.language_settings_clear);
        onMain(() -> clickDialogButton(android.R.id.button2));
        assertEquals(2, translations.getPacks().size());
        clickPackAction("Fixture", R.string.language_settings_clear);
        onMain(() -> clickDialogButton(android.R.id.button1));
        idle();
        assertSelected("English", "en");
        assertEquals(java.util.List.of(retained), translations.getPacks());
        scenario.onActivity(
                activity ->
                        assertEquals(
                                1,
                                ((ViewGroup) activity.findViewById(R.id.translation_packs))
                                        .getChildCount()));
    }

    @Test
    public void reopeningLanguagePickerKeepsTheSelectionAfterRecreation() throws Exception {
        launch();
        openLanguagePicker();
        scenario.recreate();
        openLanguagePicker();
        onMain(
                () -> {
                    ListView list = languageList();
                    assertEquals("English", list.getItemAtPosition(list.getCheckedItemPosition()));
                });
        captureScreenshot("language-picker.png");
        onMain(() -> clickLanguage("中文"));
        idle();
        assertSelected("中文", "zh");
    }

    private void selectPack(String name) {
        String[] label = new String[1];
        scenario.onActivity(
                activity ->
                        label[0] =
                                activity.getString(R.string.language_settings_pack_choice, name));
        selectChoice(label[0]);
    }

    private void selectChoice(String label) {
        openLanguagePicker();
        onMain(() -> clickLanguage(label));
        idle();
    }

    private void openLanguagePicker() {
        scenario.onActivity(
                activity -> activity.findViewById(R.id.card_language_picker).performClick());
        idle();
    }

    private static void clickLanguage(String label) {
        if (!clickListItem(languageList(), label)) {
            throw new AssertionError("Language choice not shown: " + label);
        }
    }

    private static ListView languageList() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            ListView list = root.findViewById(androidx.appcompat.R.id.select_dialog_listview);
            if (list != null && list.isShown()) return list;
        }
        throw new AssertionError("Language picker not shown");
    }

    private void clickPackAction(String packName, int titleId) {
        String[] title = new String[1];
        scenario.onActivity(
                activity -> {
                    title[0] = activity.getString(titleId);
                    packRow(activity, packName)
                            .findViewById(R.id.language_pack_actions)
                            .performClick();
                });
        idle();
        onMain(
                () -> {
                    for (View root : WindowInspector.getGlobalWindowViews()) {
                        if (clickMenuItem(root, title[0])) return;
                    }
                    throw new AssertionError("Menu action not shown: " + title[0]);
                });
    }

    private static boolean clickMenuItem(View view, String title) {
        if (view instanceof ListView list && list.isShown() && clickListItem(list, title)) {
            return true;
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                if (clickMenuItem(group.getChildAt(i), title)) return true;
            }
        }
        return false;
    }

    private static boolean clickListItem(ListView list, String label) {
        for (int i = 0; i < list.getCount(); i++) {
            if (!label.equals(list.getItemAtPosition(i).toString())) continue;
            return list.performItemClick(
                    list.getChildAt(i - list.getFirstVisiblePosition()),
                    i,
                    list.getItemIdAtPosition(i));
        }
        return false;
    }

    private static View packRow(SettingsActivity activity, String label) {
        ViewGroup list = activity.findViewById(R.id.translation_packs);
        for (int i = 0; i < list.getChildCount(); i++) {
            View row = list.getChildAt(i);
            TextView name = row.findViewById(R.id.language_name);
            if (label.contentEquals(name.getText())) return row;
        }
        throw new AssertionError("Translation pack not shown: " + label);
    }

    private static void idle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void launch() {
        Intent intent =
                new Intent(context, SettingsActivity.class)
                        .putExtra(
                                SettingsActivity.FRAGMENT_TAG_EXTRA,
                                SettingsActivity.LANGUAGE_SETTINGS_FRAGMENT);
        scenario = ActivityScenario.launch(intent);
    }

    private void assertSelected(String display, String tags) {
        String[] label = {display};
        scenario.onActivity(
                activity -> {
                    assertEquals(tags, AppCompatDelegate.getApplicationLocales().toLanguageTags());
                    if (label[0] == null)
                        label[0] = activity.getString(R.string.language_settings_follow_system);
                    assertEquals(
                            label[0],
                            ((TextView) activity.findViewById(R.id.current_locale_display))
                                    .getText()
                                    .toString());
                });
        openLanguagePicker();
        onMain(
                () -> {
                    ListView list = languageList();
                    int checked = list.getCheckedItemPosition();
                    assertTrue(checked >= 0);
                    assertEquals(label[0], list.getItemAtPosition(checked));
                    clickDialogButton(android.R.id.button2);
                });
        idle();
    }

    private void selectLanguage(String tag) {
        Locale locale = Locale.forLanguageTag(tag);
        selectChoice(locale.getDisplayName(locale));
    }

    private SharedPreferences overridePrefs() {
        return context.getSharedPreferences("language_override", Context.MODE_PRIVATE);
    }

    private static void clickDialogButton(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            View button = root.findViewById(id);
            if (button != null && button.isShown()) {
                button.performClick();
                return;
            }
        }
        throw new AssertionError("Confirmation dialog button not found");
    }

    private void captureScreenshot(String name) throws Exception {
        if (!"true"
                .equals(
                        InstrumentationRegistry.getArguments()
                                .getString("languageSettingsScreenshots"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Export and import toasts from this or the preceding case can cover the settings.
        Thread.sleep(2200);
        Bitmap screenshot =
                InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output =
                new FileOutputStream(new File(context.getExternalFilesDir(null), name))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            screenshot.recycle();
        }
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
