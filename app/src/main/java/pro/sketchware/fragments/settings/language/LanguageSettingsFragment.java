package pro.sketchware.fragments.settings.language;

import android.content.res.Resources;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import pro.sketchware.core.BaseFragment;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentSettingsLanguageBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class LanguageSettingsFragment extends BaseFragment {

    private static final String EXPORT_DIR = FileUtil.getExternalStorageDir() + "/.sketchware/data/system/i18n/";

    private FragmentSettingsLanguageBinding binding;

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
        setupCurrentLanguage();
        setupOverrideStatus();
    }

    private void setupInsets() {
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
    }

    private static final String[] LOCALE_TAGS = {"", "en", "zh"};
    private static final String[] DISPLAY_NAMES = {"", "English", "中文"};

    private int getCurrentLocaleIndex() {
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.isEmpty()) return 0;
        String currentTag = currentLocales.get(0).getLanguage();
        for (int i = 1; i < LOCALE_TAGS.length; i++) {
            if (LOCALE_TAGS[i].equals(currentTag)) return i;
        }
        return 0;
    }

    private String getLocaleDisplayName(int index) {
        if (index == 0) {
            Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
            return Helper.getResString(R.string.language_settings_follow_system)
                    + " (" + systemLocale.getDisplayLanguage(systemLocale) + ")";
        }
        return DISPLAY_NAMES[index];
    }

    private void setupLanguagePicker() {
        int selectedIndex = getCurrentLocaleIndex();
        binding.currentLocaleDisplay.setText(getLocaleDisplayName(selectedIndex));

        binding.cardLanguagePicker.setOnClickListener(v -> {
            int currentIndex = getCurrentLocaleIndex();
            String[] items = new String[LOCALE_TAGS.length];
            for (int i = 0; i < items.length; i++) {
                items[i] = getLocaleDisplayName(i);
            }

            final int[] pendingChoice = {currentIndex};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.language_settings_select_language)
                    .setSingleChoiceItems(items, currentIndex, (dialog, which) -> {
                        pendingChoice[0] = which;
                    })
                    .setPositiveButton(R.string.common_word_ok, (dialog, which) -> {
                        if (pendingChoice[0] == currentIndex) return;
                        applyLocaleAndRestart(LOCALE_TAGS[pendingChoice[0]]);
                    })
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .show();
        });
    }

    private void applyLocaleAndRestart(String tag) {
        LocaleListCompat locales = tag.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(tag);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_select_language)
                .setMessage(R.string.language_settings_restart_hint)
                .setPositiveButton(R.string.common_word_ok, (dialog, which) -> {
                    AppCompatDelegate.setApplicationLocales(locales);
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .setCancelable(false)
                .show();
    }

    private void setupCurrentLanguage() {
        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        Locale appLocale = requireContext().getResources().getConfiguration().getLocales().get(0);
        LanguageOverrideManager mgr = LanguageOverrideManager.getInstance();

        StringBuilder display = new StringBuilder();
        display.append(appLocale.getDisplayLanguage(appLocale));
        display.append(" (").append(appLocale.getLanguage()).append(")");

        if (mgr.isActive()) {
            display.append("\n").append(String.format(
                    Helper.getResString(R.string.language_settings_custom_override),
                    mgr.getOverrideCount()));
        }

        display.append("\n").append(String.format(
                Helper.getResString(R.string.language_settings_system_locale),
                systemLocale.getDisplayLanguage(systemLocale) + " (" + systemLocale.getLanguage() + ")"));
        binding.currentLanguageValue.setText(display.toString());
    }

    private void setupOverrideStatus() {
        LanguageOverrideManager mgr = LanguageOverrideManager.getInstance();

        if (mgr.isActive()) {
            binding.overrideCard.setVisibility(View.VISIBLE);
            binding.overrideInfo.setText(
                    String.format(Helper.getResString(R.string.language_settings_override_info),
                            mgr.getOverrideCount()));
            binding.btnClearOverride.setOnClickListener(v -> clearOverride());
        } else {
            binding.overrideCard.setVisibility(View.GONE);
        }
    }

    private void clearOverride() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_clear_confirm_title)
                .setMessage(R.string.language_settings_clear_confirm_msg)
                .setPositiveButton(R.string.common_word_yes, (dialog, which) -> {
                    LanguageOverrideManager.getInstance().clearOverrides(requireContext());
                    refreshUI();
                    SketchwareUtil.toast(Helper.getResString(R.string.language_settings_cleared));
                })
                .setNegativeButton(R.string.common_word_no, null)
                .show();
    }

    private void refreshUI() {
        setupCurrentLanguage();
        setupOverrideStatus();
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

    @Override
    public void configureToolbar(@NonNull com.google.android.material.appbar.MaterialToolbar toolbar) {
        super.configureToolbar(toolbar);
        toolbar.inflateMenu(R.menu.language_settings_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.import_language_strings) {
                showImportDialog();
                return true;
            } else if (id == R.id.export_language_strings) {
                exportStrings();
                return true;
            }
            return false;
        });
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
        LanguageOverrideManager.getInstance().applyOverrides(requireContext(), strings);
        refreshUI();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_settings_import_success_title)
                .setMessage(String.format(
                        Helper.getResString(R.string.language_settings_import_success_msg),
                        strings.size()))
                .setPositiveButton(R.string.common_word_ok, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
