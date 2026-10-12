package pro.sketchware.activities.settings;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.Preference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


import pro.sketchware.R;
import pro.sketchware.core.settings.ApplicationSettings;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class BackupFilenameSettingsTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private ActivityScenario<ConfigActivity> scenario;
    private String originalFilename;
    private String sampleProject;
    private LocaleListCompat originalLocales;
    private int originalNightMode;

    @Before public void setUp() {
        originalFilename = ApplicationSettings.getString(ApplicationSettings.SETTING_BACKUP_FILENAME, null);
        originalLocales = AppCompatDelegate.getApplicationLocales();
        originalNightMode = AppCompatDelegate.getDefaultNightMode();
        String language = InstrumentationRegistry.getArguments().getString("settingsPresentationLanguage", "zh");
        String theme = InstrumentationRegistry.getArguments().getString("settingsPresentationTheme", "dark");
        onMain(() -> {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
            AppCompatDelegate.setDefaultNightMode("light".equals(theme)
                    ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        });
        ApplicationSettings.putString(ApplicationSettings.SETTING_BACKUP_FILENAME, null);
    }

    @After public void restore() {
        if (scenario != null) scenario.close();
        ApplicationSettings.putString(ApplicationSettings.SETTING_BACKUP_FILENAME, originalFilename);
        onMain(() -> {
            AppCompatDelegate.setApplicationLocales(originalLocales);
            AppCompatDelegate.setDefaultNightMode(originalNightMode);
        });
    }

    @Test public void savedTemplateUpdatesSummaryAndSurvivesReopening() throws Exception {
        launch();
        assertSummaryExpanded();
        openDialog();
        onMain(() -> {
            TextView variables = (TextView) dialogView(R.id.backup_filename_variables);
            assertTrue(variables.getText().toString().contains("$projectName"));
            assertTrue(variables.getText().toString().contains("$timeInMs"));
            assertTrue(variables.getText().toString().contains("$time(yyyy-MM-dd'T'HHmmss)"));
            setTemplate("$projectName_$versionCode");
            assertEquals(sampleProject + "_1.swb", preview());
            dialogView(android.R.id.button1).performClick();
        });
        settle();
        assertEquals("$projectName_$versionCode", ApplicationSettings.getBackupFileName());
        scenario.onActivity(activity -> assertEquals(activity.getString(R.string.settings_backup_filename_example,
                activity.getString(R.string.settings_backup_filename_sample_project) + "_1.swb"), filenamePreference(activity).getSummary()));
        scenario.close();
        scenario = null;
        launch();
        assertSummaryExpanded();
        openDialog();
        onMain(() -> {
            assertEquals("$projectName_$versionCode", ((TextView) dialogView(R.id.backup_filename_input)).getText().toString());
            dialogView(android.R.id.button2).performClick();
        });
    }

    @Test public void cancelKeepsSavedTemplateAndExample() {
        ApplicationSettings.putString(ApplicationSettings.SETTING_BACKUP_FILENAME, "Saved_$projectName");
        launch();
        openDialog();
        onMain(() -> {
            setTemplate("Draft_$projectName");
            assertTrue(preview().startsWith("Draft_"));
            dialogView(android.R.id.button2).performClick();
        });
        settle();
        assertEquals("Saved_$projectName", ApplicationSettings.getBackupFileName());
        scenario.onActivity(activity -> assertTrue(filenamePreference(activity).getSummary().toString().contains("Saved_")));
    }

    @Test public void invalidDateOrEmptyTemplateKeepsDialogOpenAndDoesNotSave() {
        launch();
        String original = ApplicationSettings.getBackupFileName();
        openDialog();
        onMain(() -> {
            setTemplate("$projectName_$time(invalid)");
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.backup_filename_input_layout)).getError());
            setTemplate("");
            dialogView(android.R.id.button1).performClick();
            assertNotNull(((TextInputLayout) dialogView(R.id.backup_filename_input_layout)).getError());
            setTemplate("$projectName_$time(yyyy)");
            assertNull(((TextInputLayout) dialogView(R.id.backup_filename_input_layout)).getError());
            assertTrue(preview().endsWith(".swb"));
            dialogView(android.R.id.button2).performClick();
        });
        assertEquals(original, ApplicationSettings.getBackupFileName());
    }

    @Test public void resetUpdatesSummaryAndRestoresDefaultTemplate() {
        ApplicationSettings.putString(ApplicationSettings.SETTING_BACKUP_FILENAME, "Custom_$projectName");
        launch();
        openDialog();
        onMain(() -> dialogView(android.R.id.button3).performClick());
        settle();
        assertNull(ApplicationSettings.getString(ApplicationSettings.SETTING_BACKUP_FILENAME, null));
        assertSummaryExpanded();
        openDialog();
        onMain(() -> {
            assertTrue(((TextView) dialogView(R.id.backup_filename_input)).getText().toString().contains("$time("));
            dialogView(android.R.id.button2).performClick();
        });
    }

    @Test public void customPreferenceRowKeepsSwitchBindingAndClickBehavior() {
        boolean original = ApplicationSettings.getBoolean(ApplicationSettings.SETTING_SHOW_BUILT_IN_BLOCKS, false);
        launch();
        try {
            scenario.onActivity(activity -> {
                ConfigActivity.PreferenceFragment fragment = preferenceFragment(activity);
                fragment.getListView().findViewHolderForAdapterPosition(0).itemView.performClick();
                androidx.preference.SwitchPreferenceCompat preference = fragment.findPreference(ApplicationSettings.SETTING_SHOW_BUILT_IN_BLOCKS);
                assertEquals(!original, preference.isChecked());
                assertEquals(!original, ApplicationSettings.getBoolean(ApplicationSettings.SETTING_SHOW_BUILT_IN_BLOCKS, false));
            });
            settle();
            scenario.onActivity(activity -> {
                View row = preferenceFragment(activity).getListView().findViewHolderForAdapterPosition(0).itemView;
                android.widget.CompoundButton toggle = row.findViewById(R.id.switchWidget);
                assertNotNull(toggle);
                assertEquals(!original, toggle.isChecked());
                row.performClick();
            });
            settle();
            assertEquals(original, ApplicationSettings.getBoolean(ApplicationSettings.SETTING_SHOW_BUILT_IN_BLOCKS, false));
        } finally {
            ApplicationSettings.putBoolean(ApplicationSettings.SETTING_SHOW_BUILT_IN_BLOCKS, original);
        }
    }

    private static ConfigActivity.PreferenceFragment preferenceFragment(ConfigActivity activity) {
        return (ConfigActivity.PreferenceFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    private void launch() {
        scenario = ActivityScenario.launch(new Intent(context, ConfigActivity.class));
        scenario.onActivity(activity -> {
            activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            sampleProject = activity.getString(R.string.settings_backup_filename_sample_project);
        });
        settle();
    }

    private void openDialog() {
        scenario.onActivity(activity -> {
            Preference preference = filenamePreference(activity);
            preference.getOnPreferenceClickListener().onPreferenceClick(preference);
        });
        settle();
    }

    private void assertSummaryExpanded() {
        scenario.onActivity(activity -> {
            String summary = filenamePreference(activity).getSummary().toString();
            assertTrue(summary.contains(activity.getString(R.string.settings_backup_filename_sample_project)));
            assertTrue(summary.endsWith(".swb"));
            assertFalse(summary.contains("$projectName"));
            assertFalse(summary.contains("$time("));
        });
    }

    private static Preference filenamePreference(ConfigActivity activity) {
        ConfigActivity.PreferenceFragment fragment = (ConfigActivity.PreferenceFragment) activity.getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        return fragment.findPreference(ApplicationSettings.SETTING_BACKUP_FILENAME);
    }

    private static void setTemplate(String value) {
        ((TextView) dialogView(R.id.backup_filename_input)).setText(value);
    }

    private static String preview() {
        return ((TextView) dialogView(R.id.backup_filename_preview)).getText().toString();
    }

    private static View dialogView(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            View view = root.findViewById(id);
            if (view != null && view.isShown()) return view;
        }
        throw new AssertionError("Dialog control not visible: " + id);
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
