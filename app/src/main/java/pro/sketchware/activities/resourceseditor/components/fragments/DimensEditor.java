package pro.sketchware.activities.resourceseditor.components.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.components.adapters.DimensAdapter;
import pro.sketchware.activities.resourceseditor.components.models.DimenModel;
import pro.sketchware.activities.resourceseditor.components.utils.DimensEditorManager;
import pro.sketchware.databinding.DimenEditorAddBinding;
import pro.sketchware.databinding.ResourcesEditorFragmentBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.XmlUtil;

public class DimensEditor extends Fragment {

    public static String contentPath;
    public final ArrayList<DimenModel> dimenList = new ArrayList<>();
    public DimensAdapter adapter;
    public boolean hasUnsavedChanges;
    public DimensEditorManager dimensEditorManager;
    private ResourcesEditorActivity activity;
    private ResourcesEditorFragmentBinding binding;
    private HashMap<Integer, String> notesMap = new HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (ResourcesEditorActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ResourcesEditorFragmentBinding.inflate(inflater, container, false);
        dimensEditorManager = new DimensEditorManager();
        return binding.getRoot();
    }

    public void updateDimensList(String filePath, int updateMode, boolean hasUnsavedChangesStatus) {
        hasUnsavedChanges = hasUnsavedChangesStatus;
        contentPath = filePath;
        dimensEditorManager.contentPath = contentPath;

        ArrayList<DimenModel> parsedDimens = new ArrayList<>();

        if (FileUtil.isExistFile(contentPath)) {
            dimensEditorManager.parseDimensXML(parsedDimens, FileUtil.readFileIfExist(contentPath));
        } else {
            dimensEditorManager.notesMap.clear();
        }
        notesMap = new HashMap<>(dimensEditorManager.notesMap);

        boolean isSkippingMode = updateMode == 1;
        boolean isMergeAndReplace = updateMode == 2;

        if (isSkippingMode) {
            java.util.HashSet<String> existingNames = new java.util.HashSet<>();
            for (DimenModel existing : dimenList) {
                existingNames.add(existing.getDimenName());
            }
            for (DimenModel model : parsedDimens) {
                if (!existingNames.contains(model.getDimenName())) {
                    dimenList.add(model);
                }
            }
        } else {
            if (isMergeAndReplace) {
                java.util.HashSet<String> newNames = new java.util.HashSet<>();
                for (DimenModel d : parsedDimens) {
                    newNames.add(d.getDimenName());
                }
                dimenList.removeIf(existing -> newNames.contains(existing.getDimenName()));
            } else {
                dimenList.clear();
            }
            dimenList.addAll(parsedDimens);
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || binding == null || activity.isFinishing() || activity.isDestroyed()) return;
            adapter = new DimensAdapter(dimenList, activity, notesMap);
            binding.recyclerView.setAdapter(adapter);
            activity.checkForInvalidResources();
            updateNoContentLayout();
            if (hasUnsavedChanges) {
                contentPath = activity.dimensFilePath;
            }
        });
    }

    private void updateNoContentLayout() {
        if (dimenList.isEmpty()) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
            binding.noContentTitle.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_title), Helper.getResString(R.string.resource_type_dimen)));
            binding.noContentBody.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_body), Helper.getResString(R.string.resource_type_dimen_lower)));
        } else {
            binding.noContentLayout.setVisibility(View.GONE);
        }
    }

    public void showDimenEditDialog(DimenModel dimenModel, int position) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
        DimenEditorAddBinding dialogBinding = DimenEditorAddBinding.inflate(getLayoutInflater());

        if (dimenModel != null) {
            dialogBinding.dimenKeyInput.setText(dimenModel.getDimenName());
            dialogBinding.dimenValueInput.setText(dimenModel.getDimenValue());
            dialogBinding.stringHeaderInput.setText(notesMap.getOrDefault(position, ""));
            dialog.setTitle(R.string.dimen_title_edit);
        } else {
            dialog.setTitle(R.string.dimen_title_create);
        }

        dialog.setPositiveButton(R.string.common_word_save, (v1, which) -> {
            String key = Objects.requireNonNull(dialogBinding.dimenKeyInput.getText()).toString().trim();
            String value = Objects.requireNonNull(dialogBinding.dimenValueInput.getText()).toString().trim();

            if (key.isEmpty() || value.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT);
                return;
            }

            if (dimenModel != null) {
                dimenModel.setDimenName(key);
                dimenModel.setDimenValue(value);

                String headerInput = Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString();
                if (headerInput.isEmpty()) {
                    notesMap.remove(position);
                } else {
                    notesMap.put(position, headerInput);
                }
                adapter.notifyItemChanged(position);
            } else {
                addDimen(key, value, Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString());
            }
            hasUnsavedChanges = true;
            updateNoContentLayout();
        });

        if (dimenModel != null) {
            dialog.setNeutralButton(R.string.common_word_delete, (v1, which) -> {
                dimenList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, dimenList.size());
                updateNoContentLayout();
                hasUnsavedChanges = true;
                v1.dismiss();
            });
        }

        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(dialogBinding.getRoot());
        dialog.show();
    }

    private void addDimen(String name, String value, String note) {
        for (int i = 0; i < dimenList.size(); i++) {
            if (dimenList.get(i).getDimenName().equals(name)) {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_key_already_exists), name));
                return;
            }
        }
        dimenList.add(new DimenModel(name, value));
        int notifyPosition = dimenList.size() - 1;
        if (!note.isEmpty()) {
            notesMap.put(notifyPosition, note);
        }
        adapter.notifyItemInserted(notifyPosition);
        SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
    }

    public void saveDimensFile() {
        if (hasUnsavedChanges) {
            XmlUtil.saveXml(contentPath, dimensEditorManager.convertListToXml(dimenList, notesMap));
            hasUnsavedChanges = false;
        }
    }
}
