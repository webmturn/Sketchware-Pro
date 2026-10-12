package pro.sketchware.activities.settings.fragments.block.selector;

import static pro.sketchware.util.Helper.addBasicTextChangedListener;
import static pro.sketchware.util.GsonUtils.getGson;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pro.sketchware.activities.base.BaseFragment;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import pro.sketchware.util.DefinitionFiles;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogBlockConfigurationBinding;
import pro.sketchware.databinding.DialogSelectorActionsBinding;
import pro.sketchware.databinding.FragmentBlockSelectorManagerBinding;
import pro.sketchware.activities.settings.fragments.block.selector.details.BlockSelectorDetailsFragment;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class BlockSelectorManagerFragment extends BaseFragment {
    private FragmentBlockSelectorManagerBinding binding;
    private ArrayList<Selector> selectors = new ArrayList<>();
    private BlockSelectorAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBlockSelectorManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configureToolbar(binding.toolbar);

        adapter = new BlockSelectorAdapter((selector, index) -> openFragment(R.id.settings_fragment_container, BlockSelectorDetailsFragment.newInstance(index, selectors)), (selector, index) -> showActionsDialog(index));

        try {
            selectors = readSelectors();
            if (!BlockSelectorConsts.BLOCK_SELECTORS_FILE.exists()) {
                selectors.add(new Selector("Select typeview:", "typeview", getTypeViewList()));
                saveAllSelectors();
            }
        } catch (IOException e) {
            selectors = new ArrayList<>();
            SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_selector_file));
        }

        binding.list.setAdapter(adapter);
        adapter.submitList(selectors);

        binding.createNew.setOnClickListener(v -> showCreateEditDialog(0, false));
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
        UI.addSystemWindowInsetToMargin(binding.createNew, false, false, true, true);
    }

    private ArrayList<Selector> readSelectors() throws IOException {
        ArrayList<Selector> items = DefinitionFiles.readList(BlockSelectorConsts.BLOCK_SELECTORS_FILE, Selector.class);
        for (Selector selector : items) {
            if (!selector.isValid()) throw new IOException("Invalid selector definition");
        }
        return items;
    }

    private void showCreateEditDialog(int index, boolean isEdit) {
        DialogBlockConfigurationBinding dialogBinding = DialogBlockConfigurationBinding.inflate(LayoutInflater.from(requireContext()));
        dialogBinding.tilPalettesPath.setHint(Helper.getResString(R.string.selector_hint_name));
        dialogBinding.tilBlocksPath.setHint(Helper.getResString(R.string.selector_hint_title));
        Helper.addClearErrorOnTextChangeTextWatcher(dialogBinding.tilBlocksPath);

        if (isEdit) {
            if (index < 0 || index >= selectors.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            dialogBinding.palettesPath.setText(selectors.get(index).getName());
            dialogBinding.blocksPath.setText(selectors.get(index).getTitle());
        }

        addBasicTextChangedListener(dialogBinding.palettesPath, str -> {
            if (itemAlreadyExists(str, isEdit ? index : -1)) {
                dialogBinding.tilPalettesPath.setError(Helper.getResString(R.string.error_item_already_exists));
            } else {
                dialogBinding.tilPalettesPath.setError(null);
            }
        });

        if ("typeview".equals(Objects.requireNonNull(dialogBinding.palettesPath.getText()).toString())) {
            dialogBinding.palettesPath.setEnabled(false);
            dialogBinding.tilPalettesPath.setOnClickListener(v -> SketchwareUtil.toast(Helper.getResString(R.string.error_cannot_change_selector_name)));
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        dialog.setTitle(!isEdit ? R.string.selector_dialog_title_new : R.string.selector_dialog_title_edit);
        dialog.setView(dialogBinding.getRoot());
        dialog.setPositiveButton(!isEdit ? R.string.selector_dialog_button_create : R.string.common_word_save, null);
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        AlertDialog shown = dialog.show();
        shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String selectorName = Helper.getText(dialogBinding.palettesPath);
            String selectorTitle = Helper.getText(dialogBinding.blocksPath);
            if (selectorName.isEmpty()) {
                dialogBinding.tilPalettesPath.setError(Helper.getResString(R.string.error_selector_name_required));
                return;
            }
            if (selectorTitle.isEmpty()) {
                dialogBinding.tilBlocksPath.setError(Helper.getResString(R.string.error_selector_title_required));
                return;
            }
            if (itemAlreadyExists(selectorName, isEdit ? index : -1)) {
                dialogBinding.tilPalettesPath.setError(Helper.getResString(R.string.error_item_already_exists));
                return;
            }
            ArrayList<Selector> updated = new ArrayList<>(selectors);
            if (isEdit) updated.set(index, new Selector(selectorTitle, selectorName, selectors.get(index).getData()));
            else updated.add(new Selector(selectorTitle, selectorName, new ArrayList<>()));
            if (commitSelectors(updated)) shown.dismiss();
        });
    }

    private void showActionsDialog(int index) {
        if (index < 0 || index >= selectors.size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        DialogSelectorActionsBinding dialogBinding = DialogSelectorActionsBinding.inflate(LayoutInflater.from(requireContext()));
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity()).create();
        dialog.setTitle(R.string.selector_title_actions);
        dialog.setView(dialogBinding.getRoot());

        dialogBinding.edit.setOnClickListener(v -> {
            dialog.dismiss();
            if (index < 0 || index >= selectors.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            showCreateEditDialog(index, true);
        });
        dialogBinding.export.setOnClickListener(v -> {
            dialog.dismiss();
            if (index < 0 || index >= selectors.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            exportSelector(selectors.get(index));
        });
        if ("typeview".equals(selectors.get(index).getName())) {
            dialogBinding.delete.setVisibility(View.GONE);
        }
        dialogBinding.delete.setOnClickListener(v -> {
            dialog.dismiss();
            showConfirmationDialog(Helper.getResString(R.string.selector_confirm_delete), confirmDialog -> {
                if (index < 0 || index >= selectors.size()) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                    confirmDialog.dismiss();
                    return;
                }
                ArrayList<Selector> updated = new ArrayList<>(selectors);
                updated.remove(index);
                commitSelectors(updated);
                confirmDialog.dismiss();
            }, DialogInterface::dismiss);
        });
        dialog.show();
    }

    private void showConfirmationDialog(String message, ConfirmListener onConfirm, CancelListener onCancel) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        dialog.setTitle(R.string.selector_title_attention);
        dialog.setMessage(message);
        dialog.setPositiveButton(R.string.common_word_yes, (v, which) -> onConfirm.onConfirm(v));
        dialog.setNegativeButton(R.string.common_word_cancel, (v, which) -> onCancel.onCancel(v));
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void configureToolbar(MaterialToolbar toolbar) {
        super.configureToolbar(toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.import_block_selector_menus) {
                showImportSelectorDialog();
                return true;
            } else if (item.getItemId() == R.id.export_all_block_selector_menus) {
                saveAllSelectors(BlockSelectorConsts.EXPORT_FILE.getAbsolutePath(), String.format(Helper.getResString(R.string.toast_exported_in), BlockSelectorConsts.EXPORT_FILE.getAbsolutePath()));
                return true;
            }
            return false;
        });
    }

    private void showImportSelectorDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setTitle(Helper.getResString(R.string.selector_select_json));
        options.setExtensions(new String[]{"json"});

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFileSelected(@NonNull File file) {
                handleToImportFile(file);
            }

        };

        FilePickerDialogFragment pickerDialog = new FilePickerDialogFragment(options, callback);

        pickerDialog.show(getChildFragmentManager(), "file_picker_dialog");
    }

    private void saveAllSelectors() {
        commitSelectors(new ArrayList<>(selectors));
    }

    private boolean commitSelectors(ArrayList<Selector> updated) {
        try {
            readSelectors();
            DefinitionFiles.write(BlockSelectorConsts.BLOCK_SELECTORS_FILE, updated);
            selectors = updated;
            adapter.submitList(new ArrayList<>(selectors));
            SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
            return true;
        } catch (IOException e) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_failed_to_save));
            return false;
        }
    }

    private void saveAllSelectors(String path, String message) {
        FileUtil.writeFile(path, getGson().toJson(selectors));
        SketchwareUtil.toast(message);
    }

    private void exportSelector(Selector selector) {
        String path = BlockSelectorConsts.EXPORT_FILE.getAbsolutePath().replace("All_Menus", selector.getName());
        FileUtil.writeFile(path, getGson().toJson(selector));
        SketchwareUtil.toast(String.format(Helper.getResString(R.string.toast_exported_in), path));
    }

    private void handleToImportFile(File file) {
        try {
            JsonElement json = JsonParser.parseString(FileUtil.readFile(file.getAbsolutePath()));
            ArrayList<Selector> incoming = new ArrayList<>();
            if (json.isJsonObject()) incoming.add(getGson().fromJson(json, Selector.class));
            else if (json.isJsonArray()) incoming = getGson().fromJson(json,
                    new TypeToken<ArrayList<Selector>>(){}.getType());
            else throw new JsonSyntaxException("Expected selector or selector list");
            ArrayList<Selector> updated = readSelectors();
            HashSet<String> names = new HashSet<>();
            for (Selector selector : updated) names.add(selector.getName().toLowerCase(Locale.ROOT));
            for (Selector selector : incoming) {
                if (selector == null || !selector.isValid()) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_selector_file));
                    return;
                }
                if (!names.add(selector.getName().toLowerCase(Locale.ROOT))) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.error_item_already_exists));
                    return;
                }
            }
            updated.addAll(incoming);
            commitSelectors(updated);
        } catch (JsonSyntaxException | IOException e) {
            Log.e(BlockSelectorConsts.TAG, "Could not import selectors", e);
            SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_selector_file));
        }
    }

    private boolean itemAlreadyExists(String toCompare, int excludedIndex) {
        for (int i = 0; i < selectors.size(); i++) {
            if (i != excludedIndex && toCompare.equalsIgnoreCase(selectors.get(i).getName())) return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<String> getTypeViewList() {
        return List.of("View", "ViewGroup", "LinearLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView", "TextView", "EditText", "Button", "RadioButton", "CheckBox", "Switch", "ImageView", "SeekBar", "ListView", "Spinner", "WebView", "MapView", "ProgressBar");
    }

    interface ConfirmListener {
        void onConfirm(DialogInterface dialog);
    }

    interface CancelListener {
        void onCancel(DialogInterface dialog);
    }
}
