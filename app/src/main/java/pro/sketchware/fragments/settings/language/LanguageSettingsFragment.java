package pro.sketchware.fragments.settings.language;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

import a.a.a.BaseFragment;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.FragmentSettingsLanguageBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

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
        setupCurrentLanguage();
        setupOverrideStatus();
        setupExport();
        setupImport();
    }

    private void setupInsets() {
        {
            View view = binding.appBarLayout;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top + insets.top, right + insets.right, bottom + insets.bottom);
                return i;
            });
        }
        {
            View view = binding.content;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, i) -> {
                Insets insets = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + insets.left, top, right + insets.right, bottom + insets.bottom);
                return i;
            });
        }
    }

    private void setupCurrentLanguage() {
        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        LanguageOverrideManager mgr = LanguageOverrideManager.getInstance();

        String display;
        if (mgr.isActive()) {
            display = String.format(Helper.getResString(R.string.language_settings_custom_override),
                    mgr.getOverrideCount());
        } else {
            display = "English (default)";
        }
        display += "\n" + String.format(
                Helper.getResString(R.string.language_settings_system_locale),
                systemLocale.getDisplayLanguage() + " (" + systemLocale.getLanguage() + ")");
        binding.currentLanguageValue.setText(display);
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

    private void setupExport() {
        binding.cardExport.setOnClickListener(v -> exportStrings());
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
                    }
                }
            }
        } catch (Exception ignored) {
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

    private void setupImport() {
        binding.cardImport.setOnClickListener(v -> showImportDialog());
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
