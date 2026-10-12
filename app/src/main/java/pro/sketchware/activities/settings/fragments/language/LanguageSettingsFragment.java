package pro.sketchware.activities.settings.fragments.language;

import pro.sketchware.util.locale.LanguageOverrideManager;

import android.content.res.Resources;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import pro.sketchware.activities.base.BaseFragment;
import pro.sketchware.core.project.SketchwarePaths;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentSettingsLanguageBinding;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;
import pro.sketchware.util.locale.AppLanguages;

public class LanguageSettingsFragment extends BaseFragment {

    private static final String EXPORT_DIR = SketchwarePaths.getSystemI18nPath();

    private FragmentSettingsLanguageBinding binding;
    private AlertDialog languagePickerDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureToolbar(binding.toolbar);

        setupInsets();
        setupLanguagePicker();
        binding.btnImportStrings.setOnClickListener(v -> showImportDialog());
        binding.btnExportStrings.setOnClickListener(v -> exportStrings());
        binding.btnClearOverride.setOnClickListener(v -> clearOverride());
        setupOverrideStatus();
    }

    private void setupInsets() {
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
    }

    private void setupLanguagePicker() {
        List<Locale> supported = AppLanguages.read(requireContext());
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        String display;
        if (currentLocales.isEmpty()) {
            display = Helper.getResString(R.string.language_settings_follow_system);
        } else {
            Locale current = currentLocales.get(0);
            int index = AppLanguages.findSelection(supported, current);
            Locale displayed = index < 0 ? current : supported.get(index);
            display = displayed.getDisplayName(displayed);
        }
        binding.currentLocaleDisplay.setText(display);

        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        binding.systemLocaleDisplay.setText(String.format(
                Helper.getResString(R.string.language_settings_system_locale),
                systemLocale.getDisplayLanguage(systemLocale)));

        binding.cardLanguagePicker.setOnClickListener(v -> showLanguagePicker(supported));
    }

    private void showLanguagePicker(List<Locale> supported) {
        if (languagePickerDialog != null && languagePickerDialog.isShowing()) return;
        String[] names = new String[supported.size() + 1];
        names[0] = Helper.getResString(R.string.language_settings_follow_system);
        for (int i = 0; i < supported.size(); i++) {
            Locale locale = supported.get(i);
            names[i + 1] = locale.getDisplayName(locale);
        }
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        int selected = 0;
        if (!currentLocales.isEmpty()) {
            int index = AppLanguages.findSelection(supported, currentLocales.get(0));
            selected = index < 0 ? -1 : index + 1;
        }
        final int currentIndex = selected;
        languagePickerDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_select_language)
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == currentIndex) return;
                    LocaleListCompat locales = which == 0
                            ? LocaleListCompat.getEmptyLocaleList()
                            : LocaleListCompat.forLanguageTags(supported.get(which - 1).toLanguageTag());
                    AppCompatDelegate.setApplicationLocales(locales);
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
        languagePickerDialog.setOnDismissListener(dialog -> languagePickerDialog = null);
    }

    private void setupOverrideStatus() {
        LanguageOverrideManager mgr = LanguageOverrideManager.getInstance();

        if (mgr.isActive()) {
            binding.overrideInfo.setText(
                    String.format(Helper.getResString(R.string.language_settings_override_info),
                            mgr.getOverrideCount()));
            binding.btnClearOverride.setVisibility(View.VISIBLE);
        } else {
            binding.overrideInfo.setText(Helper.getResString(R.string.language_settings_no_override));
            binding.btnClearOverride.setVisibility(View.GONE);
        }
    }

    private void clearOverride() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_clear_confirm_title)
                .setMessage(R.string.language_settings_clear_confirm_msg)
                .setPositiveButton(R.string.common_word_yes, (dialog, which) -> {
                    LanguageOverrideManager.getInstance().clearOverrides(requireContext());
                    SketchwareUtil.toast(Helper.getResString(R.string.language_settings_cleared));
                    recreateHostActivity();
                })
                .setNegativeButton(R.string.common_word_no, null)
                .show();
    }

    private void refreshUI() {
        setupOverrideStatus();
    }

    private void recreateHostActivity() {
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private static final Pattern NON_TRANSLATABLE_PATTERN = Pattern.compile(
            "^(appbar_scrolling_view_behavior|bottom_sheet_behavior|" +
            "besome_blog_url|docs_url|firebase_database_url|facebook_url|" +
            "ideas_url|medium_url|slack_url_.*|ga_trackingId|" +
            "google_.*|default_web_client_id|project_id|gcm_.*|storage_.*|" +
            "root_spec_.*|block_(?!move_to_|type_).*|" +
            "s[1-7])$"
    );

    private boolean isTranslatable(String name, String value) {
        if (NON_TRANSLATABLE_PATTERN.matcher(name).matches()) return false;
        if (value.startsWith("http://") || value.startsWith("https://")) return false;
        if (value.startsWith("com.google.") || value.startsWith("com.besome.")) return false;
        if (value.isEmpty()) return false;
        return true;
    }

    private void exportStrings() {
        try {
            TreeMap<String, String> allStrings = getAllStringResources();
            TreeMap<String, String> translatable = new TreeMap<>();
            for (Map.Entry<String, String> entry : allStrings.entrySet()) {
                if (isTranslatable(entry.getKey(), entry.getValue())) {
                    translatable.put(entry.getKey(), entry.getValue());
                }
            }

            File dir = new File(EXPORT_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "strings_export.xml";
            File outFile = new File(dir, fileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
                writer.write("<!-- Sketchware Pro translatable strings (" + translatable.size() + " entries) -->\n");
                writer.write("<resources>\n");
                for (Map.Entry<String, String> entry : translatable.entrySet()) {
                    String name = entry.getKey();
                    String value = escapeXml(entry.getValue());
                    writer.write("    <string name=\"" + name + "\">" + value + "</string>\n");
                }
                writer.write("</resources>\n");
            }

            SketchwareUtil.toast(
                    String.format(Helper.getResString(R.string.language_settings_exported), outFile.getAbsolutePath()),
                    Toast.LENGTH_LONG);
        } catch (Exception e) {
            SketchwareUtil.toastError(
                    String.format(Helper.getResString(R.string.language_settings_export_error), e.getMessage()));
        }
    }

    private TreeMap<String, String> getAllStringResources() {
        TreeMap<String, String> result = new TreeMap<>();
        try {
            Field[] fields = R.string.class.getDeclaredFields();
            Resources res = requireContext().getResources();
            for (Field field : fields) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && field.getType() == int.class) {
                    try {
                        int id = field.getInt(null);
                        String value = res.getString(id);
                        result.put(field.getName(), value);
                    } catch (Exception ignored) {
                        Log.w("LanguageSettingsFragment", "Failed to read string resource field", ignored);
                    }
                }
            }
        } catch (Exception ignored) {
            Log.w("LanguageSettingsFragment", "Failed to collect all string resources", ignored);
        }
        return result;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "\\'")
                .replace("\"", "&quot;")
                .replace("\n", "\\n");
    }

    private void showImportDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setTitle(Helper.getResString(R.string.language_settings_select_xml));
        options.setExtensions(new String[]{"xml"});

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFileSelected(@NonNull File file) {
                importStringsFile(file);
            }
        };

        FilePickerDialogFragment pickerDialog = new FilePickerDialogFragment(options, callback);
        pickerDialog.show(getChildFragmentManager(), "filePickerDialog");
    }

    private void importStringsFile(File file) {
        try {
            String content = FileUtil.readFile(file.getAbsolutePath());
            if (content == null || content.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_file_empty));
                return;
            }

            TreeMap<String, String> parsed = parseStringsXml(content);
            if (parsed.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.language_settings_import_empty));
                return;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.language_settings_import_confirm_title)
                    .setMessage(String.format(
                            Helper.getResString(R.string.language_settings_import_confirm_msg),
                            parsed.size()))
                    .setPositiveButton(R.string.common_word_import, (dialog, which) -> {
                        applyImportedStrings(parsed);
                    })
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .show();
        } catch (Exception e) {
            SketchwareUtil.toastError(
                    String.format(Helper.getResString(R.string.language_settings_import_error), e.getMessage()));
        }
    }

    private TreeMap<String, String> parseStringsXml(String xml) throws Exception {
        TreeMap<String, String> result = new TreeMap<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));

        String currentName = null;
        StringBuilder currentValue = null;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("string".equals(parser.getName())) {
                        currentName = parser.getAttributeValue(null, "name");
                        currentValue = new StringBuilder();
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (currentValue != null) {
                        currentValue.append(parser.getText());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("string".equals(parser.getName()) && currentName != null && currentValue != null) {
                        result.put(currentName, currentValue.toString());
                        currentName = null;
                        currentValue = null;
                    }
                    break;
            }
            eventType = parser.next();
        }

        return result;
    }

    private void applyImportedStrings(TreeMap<String, String> strings) {
        int totalCount = strings.size();
        int matchedCount = LanguageOverrideManager.getInstance().applyOverrides(requireContext(), strings);
        int failedCount = Math.max(0, totalCount - matchedCount);
        refreshUI();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_import_success_title)
                .setMessage(String.format(
                        Helper.getResString(R.string.language_settings_import_success_msg),
                        totalCount, matchedCount, failedCount))
                .setPositiveButton(R.string.common_word_ok, (dialog, which) -> recreateHostActivity())
                .show();
    }

    @Override
    public void onDestroyView() {
        if (languagePickerDialog != null) languagePickerDialog.dismiss();
        super.onDestroyView();
        binding = null;
    }
}
