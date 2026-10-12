package pro.sketchware.activities.settings;

import pro.sketchware.core.settings.ApplicationSettings;


import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.topjohnwu.superuser.Shell;


import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.util.UI;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.databinding.DialogBackupFilenameBinding;
import pro.sketchware.util.BackupFilenameFormatter;
import pro.sketchware.activities.main.fragments.projects.BackupFactory;
import pro.sketchware.databinding.PreferenceActivityBinding;

public class ConfigActivity extends BaseAppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        var binding = PreferenceActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.topAppBar.setTitle(R.string.settings_title);
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        var fragment = new PreferenceFragment();
        fragment.setSnackbarView(binding.getRoot());
        getSupportFragmentManager().beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.fragmentContainer, true, false, true, true);
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {
        private View snackbarView;
        private DataStore dataStore;
        private Preference backupFilenamePreference;
        private final long exampleTimeMillis = System.currentTimeMillis();

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            dataStore = DataStore.getInstance();
            getPreferenceManager().setPreferenceDataStore(dataStore);
            setPreferencesFromResource(R.xml.preferences_config_activity, rootKey);
            Preference backupDir = findPreference("backup-dir");
            assert backupDir != null;
            backupDir.setOnPreferenceClickListener(preference -> {
                DialogCreateNewFileLayoutBinding binding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
                binding.inputText.setText(ApplicationSettings.getBackupPath());
                binding.chipGroupTypes.setVisibility(View.GONE);
                AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                        .setView(binding.getRoot())
                        .setTitle(R.string.settings_backup_directory_title)
                        .setMessage(R.string.settings_backup_directory_msg)
                        .setNegativeButton(R.string.common_word_cancel, null)
                        .setPositiveButton(R.string.common_word_save, null)
                        .create();

                dialog.setOnShowListener(dialogInterface -> {
                    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(
                            Helper.getDialogDismissListener(dialogInterface));
                    Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(view -> {
                        getDataStore().putString(ApplicationSettings.SETTING_BACKUP_DIRECTORY, Helper.getText(binding.inputText));
                        dialog.dismiss();
                    });

                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    binding.inputText.requestFocus();
                });
                dialog.show();
                return true;
            });

            SwitchPreferenceCompat installWithRoot = findPreference("root-auto-install-projects");
            assert installWithRoot != null;
            installWithRoot.setOnPreferenceClickListener(preference -> {
                if (installWithRoot.isChecked()) {
                    Shell.getShell(shell -> {
                        if (!shell.isRoot()) {
                            Snackbar.make(snackbarView, Helper.getResString(R.string.config_error_no_root), BaseTransientBottomBar.LENGTH_SHORT).show();
                            installWithRoot.setChecked(false);
                        }
                    });
                }
                return true;
            });

            backupFilenamePreference = findPreference(ApplicationSettings.SETTING_BACKUP_FILENAME);
            assert backupFilenamePreference != null;
            updateBackupFilenameSummary();
            backupFilenamePreference.setOnPreferenceClickListener(preference -> {
                showBackupFilenameDialog();
                return true;
            });
        }

        private String getExampleFilename(String template) {
            if (template.isEmpty()) throw new IllegalArgumentException("Empty backup filename template");
            return BackupFilenameFormatter.format(template,
                    Helper.getResString(R.string.settings_backup_filename_sample_project),
                    "1.0", "1", "com.example.app", exampleTimeMillis) + "." + BackupFactory.EXTENSION;
        }

        private void updateBackupFilenameSummary() {
            try {
                backupFilenamePreference.setSummary(String.format(
                        Helper.getResString(R.string.settings_backup_filename_example),
                        getExampleFilename(ApplicationSettings.getBackupFileName())));
            } catch (IllegalArgumentException e) {
                backupFilenamePreference.setSummary(Helper.getResString(R.string.settings_backup_filename_invalid_summary));
            }
        }

        private boolean updateFilenamePreview(DialogBackupFilenameBinding binding) {
            String template = Helper.getText(binding.backupFilenameInput);
            if (template.isEmpty()) {
                binding.backupFilenameInputLayout.setError(Helper.getResString(R.string.settings_backup_filename_required));
                binding.backupFilenamePreview.setText(Helper.getResString(R.string.settings_backup_filename_invalid_preview));
                return false;
            }
            try {
                binding.backupFilenamePreview.setText(getExampleFilename(template));
                binding.backupFilenameInputLayout.setError(null);
                return true;
            } catch (IllegalArgumentException e) {
                binding.backupFilenameInputLayout.setError(Helper.getResString(R.string.settings_backup_filename_date_error));
                binding.backupFilenamePreview.setText(Helper.getResString(R.string.settings_backup_filename_invalid_preview));
                return false;
            }
        }

        private void showBackupFilenameDialog() {
            DialogBackupFilenameBinding binding = DialogBackupFilenameBinding.inflate(getLayoutInflater());
            binding.backupFilenameInput.setText(ApplicationSettings.getBackupFileName());
            updateFilenamePreview(binding);
            Helper.addBasicTextChangedListener(binding.backupFilenameInput, text -> updateFilenamePreview(binding));
            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setView(binding.getRoot())
                    .setTitle(R.string.settings_backup_filename_title)
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .setPositiveButton(R.string.common_word_save, null)
                    .setNeutralButton(R.string.common_word_reset, (dialogInterface, which) -> {
                        getDataStore().putString(ApplicationSettings.SETTING_BACKUP_FILENAME, null);
                        updateBackupFilenameSummary();
                        Snackbar.make(snackbarView, Helper.getResString(R.string.config_toast_reset_default),
                                BaseTransientBottomBar.LENGTH_SHORT).show();
                    })
                    .create();
            dialog.show();
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (!updateFilenamePreview(binding)) return;
                getDataStore().putString(ApplicationSettings.SETTING_BACKUP_FILENAME, Helper.getText(binding.backupFilenameInput));
                updateBackupFilenameSummary();
                dialog.dismiss();
            });
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }

        public DataStore getDataStore() {
            return dataStore;
        }

        public void setSnackbarView(View snackbarView) {
            this.snackbarView = snackbarView;
        }
    }

    /** Adapts shared application settings to Android's preference UI. */
    public static class DataStore extends PreferenceDataStore {
        private static final DataStore INSTANCE = new DataStore();

        private DataStore() {
        }

        public static DataStore getInstance() {
            return INSTANCE;
        }

        @Override
        public void putString(String key, @Nullable String value) {
            ApplicationSettings.putString(key, value);
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defaultValue) {
            return ApplicationSettings.getString(key, defaultValue);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            ApplicationSettings.putBoolean(key, value);
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return ApplicationSettings.getBoolean(key, defaultValue);
        }
    }
}
