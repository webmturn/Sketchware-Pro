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
import java.util.IdentityHashMap;
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

    public String contentPath;
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

        ArrayList<DimenModel> existingDimens = new ArrayList<>(dimenList);
        HashMap<Integer, String> existingNotes = new HashMap<>(notesMap);

        ArrayList<DimenModel> parsedDimens = new ArrayList<>();

        if (FileUtil.isExistFile(contentPath)) {
            dimensEditorManager.parseDimensXML(parsedDimens, FileUtil.readFileIfExist(contentPath));
        } else {
            dimensEditorManager.notesMap.clear();
        }
        HashMap<Integer, String> parsedNotes = new HashMap<>(dimensEditorManager.notesMap);

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
            notesMap = rebuildNotesMap(existingDimens, existingNotes, parsedDimens, parsedNotes, dimenList);
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
            int originalIndex = dimenList.indexOf(dimenModel);
            dialogBinding.dimenKeyInput.setText(dimenModel.getDimenName());
            dialogBinding.dimenValueInput.setText(dimenModel.getDimenValue());
            dialogBinding.stringHeaderInput.setText(originalIndex >= 0 ? notesMap.getOrDefault(originalIndex, "") : "");
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
                int idx = dimenList.indexOf(dimenModel);
                dimenModel.setDimenName(key);
                dimenModel.setDimenValue(value);

                if (idx >= 0) {
                    String headerInput = Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString();
                    if (headerInput.isEmpty()) {
                        notesMap.remove(idx);
                    } else {
                        notesMap.put(idx, headerInput);
                    }
                }
                adapter = new DimensAdapter(dimenList, activity, notesMap);
                binding.recyclerView.setAdapter(adapter);
            } else {
                addDimen(key, value, Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString());
            }
            hasUnsavedChanges = true;
            updateNoContentLayout();
        });

        if (dimenModel != null) {
            dialog.setNeutralButton(R.string.common_word_delete, (v1, which) -> {
                int idx = dimenList.indexOf(dimenModel);
                if (idx >= 0) {
                    dimenList.remove(idx);
                    notesMap.remove(idx);
                    // Reindex: shift all comment keys above the deleted position down by 1
                    HashMap<Integer, String> reindexed = new HashMap<>();
                    for (java.util.Map.Entry<Integer, String> entry : notesMap.entrySet()) {
                        int k = entry.getKey();
                        reindexed.put(k > idx ? k - 1 : k, entry.getValue());
                    }
                    notesMap.clear();
                    notesMap.putAll(reindexed);
                }
                adapter = new DimensAdapter(dimenList, activity, notesMap);
                binding.recyclerView.setAdapter(adapter);
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
        if (!note.isEmpty()) {
            notesMap.put(dimenList.size() - 1, note);
        }
        adapter = new DimensAdapter(dimenList, activity, notesMap);
        binding.recyclerView.setAdapter(adapter);
        SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
    }

    private <T> HashMap<Integer, String> rebuildNotesMap(ArrayList<T> existingItems,
                                                         HashMap<Integer, String> existingNotes,
                                                         ArrayList<T> importedItems,
                                                         HashMap<Integer, String> importedNotes,
                                                         ArrayList<T> finalItems) {
        IdentityHashMap<T, Integer> existingIndexes = new IdentityHashMap<>();
        for (int i = 0; i < existingItems.size(); i++) {
            existingIndexes.put(existingItems.get(i), i);
        }

        IdentityHashMap<T, Integer> importedIndexes = new IdentityHashMap<>();
        for (int i = 0; i < importedItems.size(); i++) {
            importedIndexes.put(importedItems.get(i), i);
        }

        HashMap<Integer, String> rebuiltNotes = new HashMap<>();
        for (int i = 0; i < finalItems.size(); i++) {
            T item = finalItems.get(i);

            Integer importedIndex = importedIndexes.get(item);
            if (importedIndex != null && importedNotes.containsKey(importedIndex)) {
                rebuiltNotes.put(i, importedNotes.get(importedIndex));
                continue;
            }

            Integer existingIndex = existingIndexes.get(item);
            if (existingIndex != null && existingNotes.containsKey(existingIndex)) {
                rebuiltNotes.put(i, existingNotes.get(existingIndex));
            }
        }

        return rebuiltNotes;
    }

    public void saveDimensFile() {
        if (hasUnsavedChanges) {
            XmlUtil.saveXml(contentPath, dimensEditorManager.convertListToXml(dimenList, notesMap));
            hasUnsavedChanges = false;
        }
    }
}
