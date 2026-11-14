package pro.sketchware.activities.settings.fragments.language;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import org.xmlpull.v1.XmlPullParserException;
import pro.sketchware.R;
import pro.sketchware.activities.base.BaseFragment;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.DialogImportTranslationBinding;
import pro.sketchware.databinding.FragmentSettingsLanguageBinding;
import pro.sketchware.databinding.TranslationPackRowBinding;
import pro.sketchware.util.DefinitionFiles;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;
import pro.sketchware.util.locale.AppLanguages;
import pro.sketchware.util.locale.LanguageOverrideManager;
import pro.sketchware.util.locale.LanguageOverrideManager.Pack;

public class LanguageSettingsFragment extends BaseFragment {
    private final LanguageOverrideManager translations = LanguageOverrideManager.getInstance();
    private FragmentSettingsLanguageBinding binding;
    private AlertDialog openDialog;
    private PopupMenu packMenu;
    private List<Locale> supported;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        binding = FragmentSettingsLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        super.onViewCreated(view, state);
        configureToolbar(binding.toolbar);
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
        supported = AppLanguages.read(requireContext());
        binding.cardLanguagePicker.setOnClickListener(v -> showLanguagePicker());
        binding.btnImportStrings.setOnClickListener(v -> showImportDialog());
        binding.btnExportStrings.setOnClickListener(v -> exportTemplate());
    }

    private void showLanguagePicker() {
        if (openDialog != null && openDialog.isShowing()) return;
        List<Pack> packs = translations.getPacks();
        int packStart = supported.size() + 1;
        String[] names = new String[packStart + packs.size()];
        names[0] = getString(R.string.language_settings_follow_system);
        Locale chosen = AppCompatDelegate.getApplicationLocales().get(0);
        int checked = chosen == null ? 0 : builtinSelection(chosen);
        for (int i = 0; i < supported.size(); i++) {
            names[i + 1] = localeName(supported.get(i));
        }
        Pack active = translations.getActivePack(getResources());
        for (int i = 0; i < packs.size(); i++) {
            Pack pack = packs.get(i);
            names[packStart + i] = getString(R.string.language_settings_pack_choice, pack.name());
            if (active != null && active.id().equals(pack.id())) checked = packStart + i;
        }
        int current = checked;
        show(
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.language_settings_select_language))
                        .setSingleChoiceItems(
                                names,
                                checked,
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    if (which == current) return;
                                    if (which >= packStart) {
                                        Pack pack = packs.get(which - packStart);
                                        select(pack, pack.fallback());
                                    } else {
                                        select(null, which == 0 ? null : supported.get(which - 1));
                                    }
                                })
                        .setNegativeButton(getString(R.string.common_word_cancel), null));
    }

    private int builtinSelection(Locale locale) {
        int index = AppLanguages.findSelection(supported, locale);
        return index < 0 ? -1 : index + 1;
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.toolbar.setTitle(getString(R.string.language_settings_title));
        renderLanguages();
    }

    private void renderLanguages() {
        Pack active = translations.getActivePack(getResources());
        Locale chosen = AppCompatDelegate.getApplicationLocales().get(0);
        Locale display = getResources().getConfiguration().getLocales().get(0);
        String name;
        if (active != null) {
            name = active.name();
        } else if (chosen == null) {
            name = getString(R.string.language_settings_follow_system);
        } else {
            int index = AppLanguages.findSelection(supported, chosen);
            name = localeName(index >= 0 ? supported.get(index) : chosen);
        }
        binding.currentLocaleDisplay.setText(name);
        Locale systemLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
        binding.systemLocaleDisplay.setText(
                getString(
                        R.string.language_settings_system_locale,
                        systemLocale.getDisplayLanguage(systemLocale)));

        List<Pack> packs = translations.getPacks();
        binding.overrideInfo.setVisibility(
                active != null || packs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.overrideInfo.setText(
                active == null
                        ? getString(R.string.language_settings_no_override)
                        : getString(
                                R.string.language_settings_override_info, active.strings().size()));
        binding.translationPacks.removeAllViews();
        binding.translationPacks.setVisibility(packs.isEmpty() ? View.GONE : View.VISIBLE);
        for (Pack pack : packs) {
            var row =
                    TranslationPackRowBinding.inflate(
                            getLayoutInflater(), binding.translationPacks, false);
            row.languageName.setText(pack.name());
            String fallback =
                    getString(
                            R.string.language_settings_fallback_value,
                            pack.fallback().getDisplayName(display));
            if (!pack.rejectedStrings().isEmpty()) {
                fallback +=
                        "\n"
                                + getResources()
                                        .getQuantityString(
                                                R.plurals.language_settings_inactive_entries,
                                                pack.rejectedStrings().size(),
                                                pack.rejectedStrings().size());
            }
            row.languageSummary.setText(fallback);
            row.languagePackActions.setContentDescription(
                    getString(R.string.language_settings_pack_actions, pack.name()));
            row.languagePackActions.setOnClickListener(v -> showPackActions(v, pack));
            binding.translationPacks.addView(row.getRoot());
        }
    }

    private static String localeName(Locale locale) {
        return locale.getDisplayName(locale);
    }

    private void select(Pack pack, Locale fallback) {
        translations.select(requireContext(), pack);
        LocaleListCompat locales =
                fallback == null
                        ? LocaleListCompat.getEmptyLocaleList()
                        : LocaleListCompat.forLanguageTags(fallback.toLanguageTag());
        if (locales.equals(AppCompatDelegate.getApplicationLocales())) {
            requireActivity().recreate();
        } else {
            AppCompatDelegate.setApplicationLocales(locales);
        }
    }

    private void showImportDialog() {
        var options = new FilePickerOptions();
        options.setTitle(getString(R.string.language_settings_select_xml));
        options.setExtensions(new String[] {"xml"});
        var picker =
                new FilePickerDialogFragment(
                        options,
                        new FilePickerCallback() {
                            @Override
                            public void onFileSelected(@NonNull File file) {
                                importFile(file);
                            }
                        });
        picker.show(getChildFragmentManager(), "filePickerDialog");
    }

    private void importFile(File file) {
        try (var input = Files.newInputStream(file.toPath())) {
            TranslationXml.Document document = TranslationXml.read(input);
            if (document.strings().isEmpty()) {
                SketchwareUtil.toastError(getString(R.string.language_settings_import_empty));
                return;
            }
            chooseFallback(document, file.getName().replaceFirst("(?i)\\.xml$", ""));
        } catch (IOException | XmlPullParserException e) {
            SketchwareUtil.toastError(
                    getString(R.string.language_settings_import_error, e.getMessage()));
        }
    }

    private void chooseFallback(TranslationXml.Document document, String filename) {
        String[] names =
                supported.stream().map(LanguageSettingsFragment::localeName).toArray(String[]::new);
        Locale preferred =
                document.fallbackTag() == null
                        ? getResources().getConfiguration().getLocales().get(0)
                        : Locale.forLanguageTag(document.fallbackTag());
        int[] fallbackIndex = {Math.max(0, AppLanguages.findSelection(supported, preferred))};
        var fields = DialogImportTranslationBinding.inflate(getLayoutInflater());
        fields.packNameField.setHint(getString(R.string.language_settings_pack_name));
        fields.fallbackLanguageField.setHint(
                getString(R.string.language_settings_fallback_language));
        fields.fallbackLanguageField.setHelperText(
                getString(R.string.language_settings_fallback_hint));
        fields.packName.setText(document.name() == null ? filename : document.name());
        fields.fallbackLanguage.setAdapter(
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, names));
        fields.fallbackLanguage.setText(names[fallbackIndex[0]], false);
        fields.fallbackLanguage.setOnItemClickListener(
                (parent, row, position, id) -> {
                    fallbackIndex[0] = position;
                });
        var dialog =
                show(
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.language_settings_import_pack))
                                .setView(fields.getRoot())
                                .setNegativeButton(R.string.common_word_cancel, null)
                                .setPositiveButton(R.string.common_word_import, null));
        dialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            String label = fields.packName.getText().toString().trim();
                            if (label.isEmpty()) {
                                fields.packNameField.setError(
                                        getString(R.string.language_settings_pack_name_required));
                                return;
                            }
                            Pack pack =
                                    translations.addPack(
                                            requireContext(),
                                            label,
                                            supported.get(fallbackIndex[0]),
                                            document.strings());
                            if (pack == null) {
                                SketchwareUtil.toastError(
                                        getString(R.string.language_settings_no_matching_entries));
                                return;
                            }
                            dialog.dismiss();
                            int skipped = document.strings().size() - pack.strings().size();
                            SketchwareUtil.toast(
                                    getString(
                                            R.string.language_settings_pack_imported,
                                            pack.strings().size(),
                                            skipped));
                            select(pack, pack.fallback());
                        });
    }

    private void showPackActions(View anchor, Pack pack) {
        if (packMenu != null) packMenu.dismiss();
        var menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.language_pack_actions);
        menu.getMenu()
                .findItem(R.id.language_pack_export)
                .setTitle(getString(R.string.language_settings_export_file));
        menu.getMenu()
                .findItem(R.id.language_pack_remove)
                .setTitle(getString(R.string.language_settings_clear));
        menu.setOnMenuItemClickListener(
                item -> {
                    Pack current = translations.getPack(pack.id());
                    if (current == null) return false;
                    if (item.getItemId() == R.id.language_pack_export) exportPack(current);
                    else confirmRemove(current);
                    return true;
                });
        menu.setOnDismissListener(
                ignored -> {
                    if (packMenu == menu) packMenu = null;
                });
        packMenu = menu;
        menu.show();
    }

    private void confirmRemove(Pack pack) {
        show(
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.language_settings_clear)
                        .setMessage(getString(R.string.language_settings_remove_pack, pack.name()))
                        .setPositiveButton(
                                R.string.common_word_yes,
                                (dialog, which) -> {
                                    Pack active = translations.getActivePack(getResources());
                                    translations.remove(requireContext(), pack);
                                    if (active != null && pack.id().equals(active.id())) {
                                        requireActivity().recreate();
                                    } else {
                                        renderLanguages();
                                    }
                                })
                        .setNegativeButton(R.string.common_word_cancel, null));
    }

    private void exportTemplate() {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        int index = AppLanguages.findSelection(supported, locale);
        Locale fallback = index < 0 ? supported.get(0) : supported.get(index);
        export(
                new TranslationXml.Document(
                        null,
                        fallback.toLanguageTag(),
                        LanguageOverrideManager.sourceStrings(requireContext(), fallback)),
                "strings_export.xml");
    }

    private void exportPack(Pack pack) {
        var entries = new TreeMap<>(pack.rejectedStrings());
        entries.putAll(pack.strings());
        String name = pack.name().replaceAll("[^\\p{L}\\p{M}\\p{N}_-]", "_");
        int length =
                name.offsetByCodePoints(0, Math.min(32, name.codePointCount(0, name.length())));
        export(
                new TranslationXml.Document(pack.name(), pack.fallbackTag(), entries),
                name.substring(0, length)
                        + "-"
                        + pack.fallback().toLanguageTag()
                        + "-"
                        + pack.id()
                        + ".xml");
    }

    private void export(TranslationXml.Document document, String filename) {
        try {
            File directory = new File(SketchwarePaths.getSystemI18nPath());
            Files.createDirectories(directory.toPath());
            File file = new File(directory, filename);
            DefinitionFiles.writeText(file, TranslationXml.write(document));
            SketchwareUtil.toast(
                    getString(R.string.language_settings_exported, file.getAbsolutePath()));
        } catch (IOException e) {
            SketchwareUtil.toastError(
                    getString(R.string.language_settings_export_error, e.getMessage()));
        }
    }

    private AlertDialog show(MaterialAlertDialogBuilder builder) {
        AlertDialog dialog = builder.show();
        openDialog = dialog;
        dialog.setOnDismissListener(
                ignored -> {
                    if (openDialog == dialog) openDialog = null;
                });
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (packMenu != null) packMenu.dismiss();
        if (openDialog != null) openDialog.dismiss();
        super.onDestroyView();
        binding = null;
    }
}
