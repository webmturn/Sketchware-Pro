package pro.sketchware.activities.resourceseditor.components.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.besome.sketch.editor.property.PropertyInputItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.components.adapters.StylesAdapter;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.activities.resourceseditor.components.utils.StylesEditorManager;
import pro.sketchware.databinding.PropertyPopupParentAttrBinding;
import pro.sketchware.databinding.ResourcesEditorFragmentBinding;
import pro.sketchware.databinding.StyleEditorAddAttrBinding;
import pro.sketchware.databinding.StyleEditorAddBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class StylesEditor extends Fragment {

    private ResourcesEditorFragmentBinding binding;

    public StylesAdapter adapter;
    private PropertyInputItem.AttributesAdapter attributesAdapter;

    private final ArrayList<StyleModel> stylesList = new ArrayList<>();
    private HashMap<Integer, String> notesMap = new HashMap<>();

    public final StylesEditorManager stylesEditorManager = new StylesEditorManager();

    public boolean hasUnsavedChanges;
    private String filePath;
    private ResourcesEditorActivity activity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (ResourcesEditorActivity) getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ResourcesEditorFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void updateStylesList(String filePath, int updateMode, boolean hasUnsavedChangesStatus) {
        hasUnsavedChanges = hasUnsavedChangesStatus;
        this.filePath = filePath;
        boolean isSkippingMode = updateMode == 1;
        boolean isMergeAndReplace = updateMode == 2;

        ArrayList<StyleModel> existingStyles = new ArrayList<>(stylesList);
        HashMap<Integer, String> existingNotes = new HashMap<>(notesMap);

        ArrayList<StyleModel> defaultStyles;

        if ((activity.variant.isEmpty() || hasUnsavedChanges) && !FileUtil.isExistFile(filePath)) {
            String generatedContent = activity.projectFilePaths.getXMLStyle();
            defaultStyles = stylesEditorManager.parseStylesFile(generatedContent);
        } else {
            defaultStyles = stylesEditorManager.parseStylesFile(FileUtil.readFileIfExist(filePath));
        }

        HashMap<Integer, String> defaultNotes = new HashMap<>(stylesEditorManager.notesMap);

        if (isSkippingMode) {
            HashSet<String> existingStyleNames = new HashSet<>();
            for (StyleModel existingStyle : stylesList) {
                existingStyleNames.add(existingStyle.getStyleName());
            }

            for (StyleModel style : defaultStyles) {
                if (!existingStyleNames.contains(style.getStyleName())) {
                    stylesList.add(style);
                }
            }
        } else {
            if (isMergeAndReplace) {
                HashSet<String> newStyleNames = new HashSet<>();
                for (StyleModel style : defaultStyles) {
                    newStyleNames.add(style.getStyleName());
                }

                stylesList.removeIf(existingStyle -> newStyleNames.contains(existingStyle.getStyleName()));
            } else {
                stylesList.clear();
            }
            stylesList.addAll(defaultStyles);
        }

        activity.runOnUiThread(() -> {
            if (!isAdded() || getActivity() == null || binding == null || activity.isFinishing() || activity.isDestroyed()) return;
            notesMap = rebuildNotesMap(existingStyles, existingNotes, defaultStyles, defaultNotes, stylesList);
            adapter = new StylesAdapter(stylesList, this, notesMap);
            binding.recyclerView.setAdapter(adapter);
            activity.checkForInvalidResources();
            updateNoContentLayout();
            if (hasUnsavedChanges) {
                this.filePath = activity.stylesFilePath;
            }
        });
    }

    private void updateNoContentLayout() {
        if (stylesList.isEmpty()) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
            binding.noContentTitle.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_title), Helper.getResString(R.string.resource_type_styles)));
            binding.noContentBody.setText(String.format(Helper.getResString(R.string.resource_manager_no_list_body), Helper.getResString(R.string.resource_type_styles_lower)));
        } else {
            binding.noContentLayout.setVisibility(View.GONE);
        }
    }

    public void showAddStyleDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        StyleEditorAddBinding binding = StyleEditorAddBinding.inflate(getLayoutInflater());
        dialog.setTitle(R.string.style_title_create);
        dialog.setPositiveButton(R.string.common_word_create, (d, which) -> {
            String styleName = Objects.requireNonNull(binding.styleName.getText()).toString();
            String parent = Objects.requireNonNull(binding.styleParent.getText()).toString();
            String header = Objects.requireNonNull(binding.styleHeaderInput.getText()).toString();

            if (styleName.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_style_name_empty));
                return;
            }

            if (stylesEditorManager.isStyleExist(stylesList, styleName)) {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.styles_error_already_exists), styleName));
                return;
            }

            StyleModel style = new StyleModel(styleName, parent);
            stylesList.add(style);
            int notifyPosition = stylesList.size() - 1;
            if (!header.isEmpty()) {
                notesMap.put(notifyPosition, header);
            }
            hasUnsavedChanges = true;
            adapter = new StylesAdapter(stylesList, this, notesMap);
            StylesEditor.this.binding.recyclerView.setAdapter(adapter);
            updateNoContentLayout();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
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

    public void showEditStyleDialog(StyleModel style, int originalIndex) {
        if (style == null || originalIndex < 0) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        StyleEditorAddBinding binding = StyleEditorAddBinding.inflate(getLayoutInflater());

        binding.styleName.setText(style.getStyleName());
        binding.styleParent.setText(style.getParent());
        if (notesMap.containsKey(originalIndex)) {
            binding.styleHeaderInput.setText(notesMap.get(originalIndex));
        }

        dialog.setTitle(R.string.style_title_edit);
        dialog.setPositiveButton(R.string.common_word_edit, (d, which) -> {
            String styleName = Objects.requireNonNull(binding.styleName.getText()).toString();
            String parent = Objects.requireNonNull(binding.styleParent.getText()).toString();
            String header = Objects.requireNonNull(binding.styleHeaderInput.getText()).toString();
            int currentIndex = stylesList.indexOf(style);

            if (currentIndex < 0) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }

            if (styleName.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_style_name_empty));
                return;
            }

            style.setStyleName(styleName);
            style.setParent(parent);
            if (header.isEmpty()) {
                notesMap.remove(currentIndex);
            } else {
                notesMap.put(currentIndex, header);
            }
            hasUnsavedChanges = true;
            adapter = new StylesAdapter(stylesList, this, notesMap);
            StylesEditor.this.binding.recyclerView.setAdapter(adapter);
        });
        dialog.setNeutralButton(Helper.getResString(R.string.common_word_delete), (d, which) -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.common_word_warning)
                .setMessage(String.format(Helper.getResString(R.string.dialog_msg_delete_confirm), style.getStyleName()))
                .setPositiveButton(R.string.common_word_yes, (d2, w) -> {
                    int currentIndex = stylesList.indexOf(style);
                    if (currentIndex < 0) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                        d.dismiss();
                        return;
                    }
                    stylesList.remove(style);
                    notesMap.remove(currentIndex);
                    HashMap<Integer, String> reindexedNotes = new HashMap<>();
                    for (java.util.Map.Entry<Integer, String> entry : notesMap.entrySet()) {
                        int noteIndex = entry.getKey();
                        reindexedNotes.put(noteIndex > currentIndex ? noteIndex - 1 : noteIndex, entry.getValue());
                    }
                    notesMap.clear();
                    notesMap.putAll(reindexedNotes);
                    adapter = new StylesAdapter(stylesList, this, notesMap);
                    StylesEditor.this.binding.recyclerView.setAdapter(adapter);
                    d.dismiss();
                    updateNoContentLayout();
                    hasUnsavedChanges = true;
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .create()
                .show());
        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
    }

    public void showStyleAttributesDialog(StyleModel style) {
        if (style == null) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        var binding = PropertyPopupParentAttrBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        dialog.show();

        binding.title.setText(String.format(Helper.getResString(R.string.resource_editor_attributes_title), style.getStyleName()));

        attributesAdapter = new PropertyInputItem.AttributesAdapter();
        attributesAdapter.setOnItemClickListener(
                new PropertyInputItem.AttributesAdapter.ItemClickListener() {
                    @Override
                    public void onItemClick(LinkedHashMap<String, String> attributes, String attr) {
                        showAttributeDialog(style, attr);
                    }

                    @Override
                    public void onItemLongClick(LinkedHashMap<String, String> attributes, String attr) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.common_word_warning)
                                .setMessage(String.format(Helper.getResString(R.string.dialog_msg_delete_confirm), attr))
                                .setPositiveButton(R.string.common_word_yes, (d, w) -> {
                                    attributes.remove(attr);
                                    style.setAttributes(attributes);
                                    attributesAdapter.submitList(new ArrayList<>(attributes.keySet()));
                                    hasUnsavedChanges = true;
                                })
                                .setNegativeButton(R.string.common_word_cancel, null)
                                .create()
                                .show();
                    }
                });
        binding.recyclerView.setAdapter(attributesAdapter);
        var dividerItemDecoration =
                new DividerItemDecoration(
                        binding.recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        binding.recyclerView.addItemDecoration(dividerItemDecoration);
        var attributes = style.getAttributes();
        attributesAdapter.setAttributes(attributes);
        List<String> keys = new ArrayList<>(attributes.keySet());
        attributesAdapter.submitList(keys);

        binding.add.setOnClickListener(
                v -> showAttributeDialog(style, ""));
    }

    private void showAttributeDialog(StyleModel style, String attr) {
        boolean isEditing = !attr.isEmpty();

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireActivity());
        StyleEditorAddAttrBinding binding = StyleEditorAddAttrBinding.inflate(getLayoutInflater());

        if (isEditing) {
            binding.attrName.setText(attr);
            binding.attrValue.setText(style.getAttribute(attr));
        }

        dialog.setTitle(isEditing ? R.string.attr_title_edit : R.string.attr_title_create);

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (d, which) -> {
            String attribute = Objects.requireNonNull(binding.attrName.getText()).toString();
            String value = Objects.requireNonNull(binding.attrValue.getText()).toString();

            if (attribute.isEmpty() || value.isEmpty()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_fill_all_fields));
                return;
            }

            if (!attribute.equals(attr)) style.getAttributes().remove(attr);

            style.addAttribute(attribute, value);
            attributesAdapter.submitList(new ArrayList<>(style.getAttributes().keySet()));
            attributesAdapter.notifyDataSetChanged();
            hasUnsavedChanges = true;
        });

        dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
    }

    public void saveStylesFile() {
        if (hasUnsavedChanges) {
            FileUtil.writeFile(filePath, stylesEditorManager.convertStylesToXML(stylesList, notesMap));
            hasUnsavedChanges = false;
        }
    }

}
