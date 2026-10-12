package pro.sketchware.activities.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import pro.sketchware.R;
import pro.sketchware.util.locale.LanguageOverrideManager;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class LanguageSettingsTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private LocaleListCompat originalLocales;
    private int originalNightMode;
    private Map<String, ?> originalOverrides;
    private ActivityScenario<SettingsActivity> scenario;

    @Before public void setUp() {
        // AppCompat restores persisted locales when the first activity attaches after a cold start.
        try (ActivityScenario<SettingsActivity> warm = ActivityScenario.launch(new Intent(context, SettingsActivity.class)
                .putExtra(SettingsActivity.FRAGMENT_TAG_EXTRA, SettingsActivity.LANGUAGE_SETTINGS_FRAGMENT))) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            originalLocales = AppCompatDelegate.getApplicationLocales();
        }
        originalNightMode = AppCompatDelegate.getDefaultNightMode();
        originalOverrides = new HashMap<>(overridePrefs().getAll());
        LanguageOverrideManager.getInstance().clearOverrides(context);
        onMain(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")));
        String nightMode = InstrumentationRegistry.getArguments().getString("languageSettingsNightMode");
        if ("light".equals(nightMode)) onMain(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO));
        else if ("dark".equals(nightMode)) onMain(() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES));
    }

    @After public void tearDown() {
        if (scenario != null) scenario.close();
        SharedPreferences.Editor editor = overridePrefs().edit().clear();
        for (Map.Entry<String, ?> entry : originalOverrides.entrySet()) {
            if (entry.getValue() instanceof String value) editor.putString(entry.getKey(), value);
            else if (entry.getValue() instanceof Integer value) editor.putInt(entry.getKey(), value);
        }
        editor.commit();
        LanguageOverrideManager.getInstance().reload(context);
        onMain(() -> AppCompatDelegate.setApplicationLocales(originalLocales));
        onMain(() -> AppCompatDelegate.setDefaultNightMode(originalNightMode));
    }

    @Test public void openingAndRecreatingPreservesRegionalLocale() throws Exception {
        onMain(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN")));
        launch();
        assertSelected("中文", "zh-CN");
        scenario.recreate();
        assertSelected("中文", "zh-CN");
        selectLanguage("zh");
        scenario.onActivity(activity -> {
            assertEquals(View.VISIBLE, activity.findViewById(R.id.override_card).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.btn_clear_override).getVisibility());
            assertTrue(activity.findViewById(R.id.btn_import_strings).isEnabled());
            assertTrue(activity.findViewById(R.id.btn_export_strings).isEnabled());
        });
        assertSelected("中文", "zh-CN");
        captureScreenshot("language-settings-zh.png");
    }

    @Test public void selectingLanguageAppliesImmediatelyAndKeepsCustomTranslations() throws Exception {
        LanguageOverrideManager.getInstance().applyOverrides(context,
                Map.of("common_word_cancel", "Fixture translation"));
        launch();
        selectLanguage("zh");
        assertSelected("中文", "zh");
        scenario.onActivity(activity -> {
            assertEquals("语言设置", activity.getString(R.string.language_settings_title));
            assertEquals(View.VISIBLE, activity.findViewById(R.id.btn_clear_override).getVisibility());
            assertEquals("已启用 · 1 条译文", ((TextView) activity.findViewById(R.id.override_info)).getText().toString());
        });
        assertEquals("Fixture translation", LanguageOverrideManager.getInstance().getOverride(R.string.common_word_cancel));
        captureScreenshot("language-settings-custom-zh.png");

        selectLanguage("");
        assertSelected(null, "");
        scenario.recreate();
        assertSelected(null, "");
        assertEquals(1, LanguageOverrideManager.getInstance().getOverrideCount());

        selectLanguage("en");
        assertSelected("English", "en");
        scenario.onActivity(activity -> assertEquals("Language Settings", activity.getString(R.string.language_settings_title)));
        captureScreenshot("language-settings-en.png");
    }

    @Test public void importButtonOpensPickerWithoutChangingLanguage() {
        launch();
        scenario.onActivity(activity -> activity.findViewById(R.id.btn_import_strings).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            Fragment settings = activity.getSupportFragmentManager().findFragmentById(R.id.settings_fragment_container);
            assertNotNull(settings);
            DialogFragment picker = (DialogFragment) settings.getChildFragmentManager().findFragmentByTag("filePickerDialog");
            assertNotNull(picker);
            assertTrue(picker.requireDialog().isShowing());
            picker.dismissNow();
        });
        assertSelected("English", "en");
        assertEquals(0, LanguageOverrideManager.getInstance().getOverrideCount());
    }

    @Test
    public void removingCustomTranslationsRequiresConfirmationAndKeepsSelectedLanguage() {
        LanguageOverrideManager.getInstance().applyOverrides(context,
                Map.of("common_word_cancel", "Fixture translation"));
        onMain(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh")));
        launch();
        scenario.onActivity(activity -> activity.findViewById(R.id.btn_clear_override).performClick());
        onMain(() -> clickDialogButton(android.R.id.button2));
        assertEquals(1, LanguageOverrideManager.getInstance().getOverrideCount());
        scenario.onActivity(activity -> activity.findViewById(R.id.btn_clear_override).performClick());
        onMain(() -> clickDialogButton(android.R.id.button1));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertSelected("中文", "zh");
        assertEquals(0, LanguageOverrideManager.getInstance().getOverrideCount());
        scenario.onActivity(activity -> {
            assertEquals(View.VISIBLE, activity.findViewById(R.id.override_card).getVisibility());
            assertEquals(View.GONE, activity.findViewById(R.id.btn_clear_override).getVisibility());
            assertEquals("未导入翻译文件", ((TextView) activity.findViewById(R.id.override_info)).getText().toString());
        });
    }

    private void launch() {
        Intent intent = new Intent(context, SettingsActivity.class)
                .putExtra(SettingsActivity.FRAGMENT_TAG_EXTRA, SettingsActivity.LANGUAGE_SETTINGS_FRAGMENT);
        scenario = ActivityScenario.launch(intent);
    }

    private void assertSelected(String display, String tags) {
        scenario.onActivity(activity -> {
            assertEquals(tags, AppCompatDelegate.getApplicationLocales().toLanguageTags());
            String expected = display == null ? activity.getString(R.string.language_settings_follow_system) : display;
            assertEquals(expected, ((TextView) activity.findViewById(R.id.current_locale_display)).getText().toString());
            activity.findViewById(R.id.card_language_picker).performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onMain(() -> {
            ListView list = languageList();
            int checked = list.getCheckedItemPosition();
            assertTrue(checked >= 0);
            if (display == null) assertEquals(0, checked);
            else assertEquals(display, list.getAdapter().getItem(checked).toString());
            clickDialogButton(android.R.id.button2);
        });
        scenario.onActivity(activity -> assertEquals(tags, AppCompatDelegate.getApplicationLocales().toLanguageTags()));
    }

    private void selectLanguage(String tag) {
        scenario.onActivity(activity -> activity.findViewById(R.id.card_language_picker).performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        onMain(() -> {
            ListView list = languageList();
            Locale locale = Locale.forLanguageTag(tag);
            for (int index = 0; index < list.getCount(); index++) {
                if ((tag.isEmpty() && index == 0) || (!tag.isEmpty()
                        && locale.getDisplayName(locale).equals(list.getAdapter().getItem(index).toString()))) {
                    list.performItemClick(list.getChildAt(index - list.getFirstVisiblePosition()),
                            index, list.getAdapter().getItemId(index));
                    return;
                }
            }
            throw new AssertionError("Language not offered: " + tag);
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static ListView languageList() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            ListView list = root.findViewById(androidx.appcompat.R.id.select_dialog_listview);
            if (list != null && list.isShown()) return list;
        }
        throw new AssertionError("Language picker not shown");
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
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("languageSettingsScreenshots"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(context.getExternalFilesDir(null), name))) {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            screenshot.recycle();
        }
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
