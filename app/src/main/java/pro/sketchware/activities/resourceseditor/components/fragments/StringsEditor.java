package pro.sketchware.activities.resourceseditor.components.fragments;

import static pro.sketchware.util.UI.animateLayoutChanges;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;

import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.ResourceEditorDraftState;
import pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult;
import pro.sketchware.activities.resourceseditor.ResourceEditorStringChanges;
import pro.sketchware.activities.resourceseditor.components.adapters.StringsAdapter;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.databinding.ResourcesEditorFragmentBinding;
import pro.sketchware.databinding.ViewStringEditorAddBinding;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.XmlUtil;

public class StringsEditor extends ResourceEditorFragment {
    private ResourcesEditorFragmentBinding binding;

    private ResourcesEditorActivity activity;

    public StringsAdapter adapter;

    public final ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
    private HashMap<Integer, String> notesMap = new HashMap<>();
    private final ResourceEditorStringChanges draftChanges = new ResourceEditorStringChanges();
    private boolean specificDraftChange;

    @Override protected void onDraftChanged(long revision) {
        if (!specificDraftChange) draftChanges.reset(revision);
    }

    public void markStringChanged(int index, boolean shiftsFollowingRows) {
        if (index < 0) {
            markDraftChanged();
            return;
        }
        specificDraftChange = true;
        try { markDraftChanged(); }
        finally { specificDraftChange = false; }
        draftChanges.changed(index, shiftsFollowingRows, listmap.size(), draftRevision());
    }

    public String filePath;

    public final StringsEditorManager stringsEditorManager = new StringsEditorManager();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (ResourcesEditorActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ResourcesEditorFragmentBinding.inflate(inflater, container, false);
        stringsEditorManager.sc_id = activity.sc_id;
        return binding.getRoot();
    }

    @Nullable
    @Override
    public Bundle saveDraftState() {
        return hasUnsavedChanges ? ResourceEditorDraftState.capture(listmap, notesMap) : null;
    }

    @Override
    public void restoreDraftState(@NonNull Bundle state, String filePath) {
        restoreSnapshot(ResourceEditorDraftState.fromLegacy(state, HashMap.class), filePath);
    }

    @Nullable
    @Override
    public ResourceEditorDraftState.Snapshot snapshotDraft() {
        return hasUnsavedChanges ? ResourceEditorDraftState.snapshot(listmap, notesMap) : null;
    }

    @Override public ResourceEditorDraftState.Snapshot snapshotDraft(long protectedRevision) {
        return hasUnsavedChanges ? draftChanges.snapshot(listmap, notesMap, protectedRevision) : null;
    }

    @Override
    public void restoreSnapshot(@NonNull ResourceEditorDraftState.Snapshot snapshot, String filePath) {
        ResourceEditorDraftState.apply(snapshot, listmap, notesMap);
        draftChanges.restored(draftRevision(), listmap.size(), notesMap);
        this.filePath = filePath;
        hasUnsavedChanges = true;
        stringsEditorManager.isDefaultVariant = activity.variant.isEmpty();
        adapter = new StringsAdapter(activity, listmap, notesMap);
        binding.recyclerView.setAdapter(adapter);
        updateNoContentLayout();
    }

    public void updateStringsList(String filePath, int updateMode, boolean hasUnsavedChangesStatus) {
        this.filePath = filePath;
        if (hasUnsavedChangesStatus) markDraftChanged();
        else markDraftClean();

        boolean isSkippingMode = updateMode == 1;
        boolean isMergeAndReplace = updateMode == 2;
        stringsEditorManager.isDefaultVariant = activity.variant.isEmpty();

        ArrayList<HashMap<String, Object>> existingStrings = new ArrayList<>(listmap);
        HashMap<Integer, String> existingNotes = new HashMap<>(notesMap);

        ArrayList<HashMap<String, Object>> defaultStrings = new ArrayList<>();
        if ((activity.variant.isEmpty() || hasUnsavedChanges) && !FileUtil.isExistFile(filePath)) {
            String generatedContent = activity.projectFilePaths.getXMLString();
            stringsEditorManager.convertXmlStringsToListMap(generatedContent, defaultStrings);
        } else {
            stringsEditorManager.convertXmlStringsToListMap(FileUtil.readFileIfExist(filePath), defaultStrings);
        }
        HashMap<Integer, String> defaultNotes = new HashMap<>(stringsEditorManager.notesMap);

        if (isSkippingMode) {
            HashSet<String> existingKeys = new HashSet<>();
            for (HashMap<String, Object> existingMap : listmap) {
                Object existingKey = existingMap.get("key");
                if (existingKey != null) {
                    existingKeys.add(existingKey.toString());
                }
            }

            for (HashMap<String, Object> stringMap : defaultStrings) {
                Object keyValue = stringMap.get("key");
                String key = keyValue != null ? keyValue.toString() : null;
                if (existingKeys.add(key)) {
                    listmap.add(stringMap);
                }
            }
        } else {
            if (isMergeAndReplace) {
                HashSet<String> newKeys = new HashSet<>();
                for (HashMap<String, Object> stringMap : defaultStrings) {
                    Object keyValue = stringMap.get("key");
                    if (keyValue != null) {
                        newKeys.add(keyValue.toString());
                    }
                }

                listmap.removeIf(existingMap -> {
                    Object k = existingMap.get("key");
                    return k != null && newKeys.contains(k.toString());
                });
            } else {
                listmap.clear();
            }
            listmap.addAll(defaultStrings);
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || binding == null || activity.isFinishing() || activity.isDestroyed()) return;
            notesMap = rebuildNotesMap(existingStrings, existingNotes, defaultStrings, defaultNotes, listmap);
            adapter = new StringsAdapter(activity, listmap, notesMap);
            binding.recyclerView.setAdapter(adapter);
            activity.checkForInvalidResources();
            updateNoContentLayout();
            if (hasUnsavedChanges) {
                this.filePath = activity.stringsFilePath;
            }
        });
    }

    public void updateNoContentLayout() {
        if (listmap.isEmpty()) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
            binding.noContentTitle.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_title), Helper.getResString(R.string.resource_type_strings)));
            binding.noContentBody.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_body), Helper.getResString(R.string.resource_type_string_lower)));
        } else {
            binding.noContentLayout.setVisibility(View.GONE);
        }
    }

    public void showAddStringDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        ViewStringEditorAddBinding binding = ViewStringEditorAddBinding.inflate(getLayoutInflater());
        dialog.setTitle(R.string.string_title_create);
        binding.stringKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                animateLayoutChanges(binding.getRoot());
                binding.importantNote.setVisibility(s.toString().equals("app_name") ? View.VISIBLE : View.GONE);
            }
        });
        dialog.setPositiveButton(R.string.common_word_create, (d, which) -> {
            String key = Objects.requireNonNull(binding.stringKeyInput.getText()).toString();
            String value = Objects.requireNonNull(binding.stringValueInput.getText()).toString();

            if (key.isEmpty() || value.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_fill_all_fields));
                return;
            }

            if (stringsEditorManager.isXmlStringsExist(listmap, key)) {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_key_already_exists), key));
                return;
            }
            addString(key, value, Objects.requireNonNull(binding.stringHeaderInput.getText()).toString().trim());
            updateNoContentLayout();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
    }

    public void addString(final String key, final String text, String note) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("key", key);
        map.put("text", text);
        listmap.add(map);
        if (!note.isEmpty()) {
            notesMap.put(listmap.size() - 1, note);
        }
        markStringChanged(listmap.size() - 1, false);
        adapter = new StringsAdapter(activity, listmap, notesMap);
        binding.recyclerView.setAdapter(adapter);
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

    @Override public String renderXml() { return stringsEditorManager.convertListMapToXmlStrings(listmap, notesMap); }

    @Override public String resourcePath() { return filePath; }

    public ResourceEditorSaveResult.XmlWrite saveStringsFile() {
        if (!hasUnsavedChanges) return ResourceEditorSaveResult.XmlWrite.UNCHANGED;
        return writeResourceXml(filePath, renderXml());
    }
}
