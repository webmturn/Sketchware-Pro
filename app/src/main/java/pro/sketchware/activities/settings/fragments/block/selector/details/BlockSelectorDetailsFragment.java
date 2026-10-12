package pro.sketchware.activities.settings.fragments.block.selector.details;

import static pro.sketchware.util.GsonUtils.getGson;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.activities.base.BaseFragment;
import pro.sketchware.util.DefinitionFiles;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAddCustomActivityBinding;
import pro.sketchware.databinding.DialogSelectorActionsBinding;
import pro.sketchware.databinding.FragmentBlockSelectorManagerBinding;
import pro.sketchware.activities.settings.fragments.block.selector.BlockSelectorConsts;
import pro.sketchware.util.UI;
import pro.sketchware.activities.settings.fragments.block.selector.Selector;
import pro.sketchware.util.SketchwareUtil;

public class BlockSelectorDetailsFragment extends BaseFragment {
    private int index;
    private List<Selector> selectors;
    private FragmentBlockSelectorManagerBinding binding;
    private BlockSelectorDetailsAdapter adapter;

    public static BlockSelectorDetailsFragment newInstance(int index, ArrayList<Selector> selectors) {
        var fragment = new BlockSelectorDetailsFragment();
        var args = new Bundle();
        args.putInt("index", index);
        args.putParcelableArrayList("selectors", selectors);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            selectors = new ArrayList<>();
            index = -1;
            return;
        }
        index = args.getInt("index", -1);
        selectors = args.getParcelableArrayList("selectors");
        if (selectors == null) {
            selectors = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlockSelectorManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (index < 0 || index >= selectors.size() || selectors.get(index) == null || !selectors.get(index).isValid()) {
            getParentFragmentManager().popBackStack();
            return;
        }

        configureToolbar(binding.toolbar);

        adapter = new BlockSelectorDetailsAdapter(
                (selector, indexA) -> showActionsDialog(indexA)
        );

        adapter.submitList(selectors.get(index).getData());
        binding.list.setAdapter(adapter);

        binding.createNew.setOnClickListener(v -> showCreateEditDialog(false, 0));

        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
        UI.addSystemWindowInsetToPadding(binding.content, true, false, true, true);
        UI.addSystemWindowInsetToMargin(binding.createNew, false, false, true, true);
    }

    private void showCreateEditDialog(boolean isEdit, int indexA) {
        if (index < 0 || index >= selectors.size() || selectors.get(index) == null || !selectors.get(index).isValid()) {
            getParentFragmentManager().popBackStack();
            return;
        }
        DialogAddCustomActivityBinding dialogBinding = DialogAddCustomActivityBinding.inflate(LayoutInflater.from(requireContext()));
        dialogBinding.activityNameInputLayout.setHint(Helper.getResString(R.string.selector_hint_item));
        if (isEdit) {
            if (indexA < 0 || indexA >= selectors.get(index).getData().size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            dialogBinding.activityNameInput.setText(selectors.get(index).getData().get(indexA));
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        dialog.setTitle(isEdit ? R.string.common_word_edit : R.string.selector_title_new_item);
        dialog.setPositiveButton(isEdit ? R.string.common_word_save : R.string.common_word_create, null);
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.setView(dialogBinding.getRoot());
        Helper.addClearErrorOnTextChangeTextWatcher(dialogBinding.activityNameInputLayout);
        AlertDialog shown = dialog.show();
        shown.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newItem = Helper.getText(dialogBinding.activityNameInput);
            if (newItem.isEmpty()) {
                dialogBinding.activityNameInputLayout.setError(Helper.getResString(R.string.error_selector_item_required));
                return;
            }
            List<String> data = selectors.get(index).getData();
            List<String> original = new ArrayList<>(data);
            if (isEdit) data.set(indexA, newItem);
            else data.add(newItem);
            if (saveAll()) {
                adapter.notifyDataSetChanged();
                shown.dismiss();
            } else {
                data.clear();
                data.addAll(original);
            }
        });
    }

    private void showActionsDialog(int indexA) {
        if (index < 0 || index >= selectors.size() || indexA < 0 || indexA >= selectors.get(index).getData().size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        DialogSelectorActionsBinding dialogBinding = DialogSelectorActionsBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity()).create();
        dialog.setTitle(R.string.selector_title_actions);
        dialog.setView(dialogBinding.getRoot());

        dialogBinding.edit.setOnClickListener(v -> {
            dialog.dismiss();
            if (index < 0 || index >= selectors.size() || indexA < 0 || indexA >= selectors.get(index).getData().size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            showCreateEditDialog(true, indexA);
        });

        dialogBinding.export.setVisibility(View.GONE);

        dialogBinding.delete.setOnClickListener(v -> {
            dialog.dismiss();
            showConfirmationDialog(
                    Helper.getResString(R.string.selector_message_delete_item),
                    confirmDialog -> {
                        if (index < 0 || index >= selectors.size() || indexA < 0 || indexA >= selectors.get(index).getData().size()) {
                            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                            confirmDialog.dismiss();
                            return;
                        }
                        String removed = selectors.get(index).getData().remove(indexA);
                        if (saveAll()) adapter.notifyDataSetChanged();
                        else selectors.get(index).getData().add(indexA, removed);
                        confirmDialog.dismiss();
                    },
                    DialogInterface::dismiss
            );
        });

        dialog.show();
    }

    private void showConfirmationDialog(String message, OnDialogClickListener onConfirm, OnDialogClickListener onCancel) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        dialog.setTitle(R.string.selector_title_attention);
        dialog.setMessage(message);
        dialog.setPositiveButton(R.string.common_word_yes, (v, which) -> onConfirm.onClick(v));
        dialog.setNegativeButton(R.string.common_word_cancel, (v, which) -> onCancel.onClick(v));

        dialog.show();
    }

    private boolean saveAll() {
        try {
            for (Selector selector : DefinitionFiles.readList(BlockSelectorConsts.BLOCK_SELECTORS_FILE, Selector.class)) {
                if (!selector.isValid()) throw new IOException("Invalid selector definition");
            }
            DefinitionFiles.write(BlockSelectorConsts.BLOCK_SELECTORS_FILE, selectors);
            SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
            return true;
        } catch (IOException e) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_failed_to_save));
            return false;
        }
    }

    @Override
    public void configureToolbar(@NonNull MaterialToolbar toolbar) {
        super.configureToolbar(toolbar);
        if (!selectors.get(index).getName().isEmpty()) {
            toolbar.setTitle(selectors.get(index).getName());
        }
        toolbar.getMenu().clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @FunctionalInterface
    public interface OnDialogClickListener {
        void onClick(DialogInterface dialog);
    }
}
