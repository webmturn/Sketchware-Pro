package pro.sketchware.control.logic;

import static android.text.TextUtils.isEmpty;
import static pro.sketchware.SketchApplication.getContext;
import static pro.sketchware.utility.SketchwareUtil.dpToPx;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import pro.sketchware.core.validation.IdentifierValidator;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.codegen.BlockConstants;
import pro.sketchware.core.ui.BlockView;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.AddCustomListBinding;
import pro.sketchware.databinding.AddCustomVariableBinding;
import pro.sketchware.lib.validator.VariableModifierValidator;
import pro.sketchware.lib.validator.VariableTypeValidator;
import pro.sketchware.menu.ExtraMenuBean;
import pro.sketchware.utility.SketchwareUtil;

public class LogicClickListener implements View.OnClickListener {

    private final ProjectDataStore projectDataManager;
    private final LogicEditorActivity logicEditor;
    private final ProjectFileBean projectFile;
    private final String currentBlockEntryKey;
    private final String javaName;

    public LogicClickListener(LogicEditorActivity logicEditor) {
        this.logicEditor = logicEditor;
        projectDataManager = ProjectDataManager.getProjectDataManager(logicEditor.scId);
        projectFile = logicEditor.projectFile;
        currentBlockEntryKey = logicEditor.id + "_" + logicEditor.eventName;
        javaName = logicEditor.projectFile.getJavaName();
    }

    private ArrayList<String> getUsedVariable(int type) {
        return projectDataManager.getVariableNamesByType(projectFile.getJavaName(), type);
    }

    private ArrayList<String> getUsedList(int type) {
        return projectDataManager.getListNamesByType(projectFile.getJavaName(), type);
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (!isEmpty(tag)) {
            switch (tag) {
                case "listAddCustom":
                    addCustomList();
                    break;

                case "variableAddNew":
                    addCustomVariable();
                    break;

                case "variableManage":
                    manageVariables();
                    break;

                case "listManage":
                    manageList();
                    break;

                case "variableFindRefs":
                    findVariableReferences(false);
                    break;

                case "listFindRefs":
                    findVariableReferences(true);
                    break;
            }
        }
    }

    private void addCustomVariable() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle(R.string.logic_add_custom_variable);

        AddCustomVariableBinding binding = AddCustomVariableBinding.inflate(logicEditor.getLayoutInflater());

        binding.modifierLayout.setHelperText(Helper.getResString(R.string.logic_editor_helper_modifier));

        VariableModifierValidator modifiersValidator = new VariableModifierValidator(getContext(), binding.modifierLayout);
        binding.modifier.addTextChangedListener(modifiersValidator);

        VariableTypeValidator varTypeValidator = new VariableTypeValidator(getContext(), binding.typeLayout);
        binding.type.addTextChangedListener(varTypeValidator);

        IdentifierValidator validator = new IdentifierValidator(getContext(), binding.nameLayout, BlockConstants.RESERVED_KEYWORDS, BlockConstants.COMPONENT_TYPES, projectDataManager.getAllIdentifiers(projectFile));

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_add), (v, which) -> {
            String variableModifier = Helper.getText(binding.modifier).trim();
            String variableType = Helper.getText(binding.type).trim();
            String variableName = Helper.getText(binding.name).trim();
            String variableInitializer = Helper.getText(binding.initializer).trim();

            boolean isValidModifier = modifiersValidator.isValid() || variableModifier.isEmpty();
            boolean isValidType = varTypeValidator.isValid();
            boolean isValidName = validator.isValid();

            if (!isValidModifier) {
                binding.modifierLayout.requestFocus();
                return;
            }

            if (isValidType) {
                binding.typeLayout.setError(null);
                binding.typeLayout.setErrorEnabled(false);
            } else {
                binding.typeLayout.requestFocus();
                if (variableType.isEmpty()) {
                    binding.typeLayout.setError(Helper.getResString(R.string.error_type_empty));
                    binding.typeLayout.setErrorEnabled(true);
                }
                return;
            }

            if (isValidName) {
                binding.nameLayout.setError(null);
                binding.nameLayout.setErrorEnabled(false);
            } else {
                binding.nameLayout.requestFocus();
                if (variableName.isEmpty()) {
                    binding.nameLayout.setError(Helper.getResString(R.string.error_name_empty));
                    binding.nameLayout.setErrorEnabled(true);
                }
                return;
            }

            String variable = !variableModifier.isEmpty() ? variableModifier + " " : "";
            variable += variableType + " " + variableName;
            if (!variableInitializer.isEmpty()) {
                variable += " = " + variableInitializer;
            }
            logicEditor.addVariable(6, variable.trim());
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();

        // dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        binding.modifierLayout.requestFocus();
    }

    private void manageVariables() {
        int horizontalPadding = dpToPx(20);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, dpToPx(8), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(logicEditor));

        List<Item> data = new LinkedList<>();
        List<Pair<List<Integer>, String>> variableTypes = List.of(
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_BOOLEAN), "Boolean (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_NUMBER), "Number (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_STRING), "String (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_MAP), "Map (%d)"),
                new Pair<>(List.of(5, 6), "Custom Variable (%d)")
        );
        for (Pair<List<Integer>, String> variableType : variableTypes) {
            List<String> instances = new LinkedList<>();
            for (Integer type : variableType.first) {
                instances.addAll(getUsedVariable(type));
            }
            for (int i = 0, size = instances.size(); i < size; i++) {
                if (i == 0) data.add(new Item(String.format(variableType.second, size)));
                data.add(new Item(instances.get(i), true));
            }
        }
        if (data.isEmpty()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.logic_editor_message_no_variables));
            return;
        }

        RenameAdapter adapter = new RenameAdapter(logicEditor, data);
        recyclerView.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(logicEditor);
        builder.setTitle(R.string.logic_editor_title_manage_variable);
        builder.setView(recyclerView);
        builder.setNegativeButton(R.string.common_word_cancel, null);
        androidx.appcompat.app.AlertDialog dialog = builder.show();
        adapter.setOnItemClickListener(name -> {
            dialog.dismiss();
            showManageActionDialog(name, false);
        });
    }

    private void addCustomList() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle(R.string.logic_add_custom_list);

        AddCustomListBinding listBinding = AddCustomListBinding.inflate(logicEditor.getLayoutInflater());

        IdentifierValidator validator = new IdentifierValidator(getContext(), listBinding.nameLayout, BlockConstants.RESERVED_KEYWORDS, BlockConstants.COMPONENT_TYPES, projectDataManager.getAllIdentifiers(projectFile));

        dialog.setView(listBinding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_add), (v, which) -> {
            String variableType = Helper.getText(listBinding.type);
            String variableName = Helper.getText(listBinding.name);

            boolean validType = !isEmpty(variableType);
            boolean validName = !isEmpty(variableName);

            if (validType) {
                listBinding.typeLayout.setError(null);
                listBinding.typeLayout.setErrorEnabled(false);
            } else {
                if (validName) listBinding.typeLayout.requestFocus();
                listBinding.typeLayout.setError(Helper.getResString(R.string.error_type_empty));
                listBinding.typeLayout.setErrorEnabled(true);
            }

            CharSequence nameError = listBinding.nameLayout.getError();
            if (nameError == null || "Name can't be empty".contentEquals(nameError)) {
                if (validName) {
                    listBinding.nameLayout.setError(null);
                    listBinding.nameLayout.setErrorEnabled(false);
                } else {
                    listBinding.nameLayout.requestFocus();
                    listBinding.nameLayout.setError(Helper.getResString(R.string.error_name_empty));
                    listBinding.nameLayout.setErrorEnabled(true);
                }
            }

            if (validType && validName && validator.isValid()) {
                logicEditor.addListVariable(4, variableType + " " + variableName + " = new ArrayList<>()");
                v.dismiss();
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();

        // dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        listBinding.typeLayout.requestFocus();
    }

    private void manageList() {
        int horizontalPadding = dpToPx(20);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, dpToPx(8), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(logicEditor));

        List<Item> data = new LinkedList<>();
        List<Pair<Integer, String>> listTypes = List.of(
                new Pair<>(ExtraMenuBean.LIST_TYPE_NUMBER, "List Integer (%d)"),
                new Pair<>(ExtraMenuBean.LIST_TYPE_STRING, "List String (%d)"),
                new Pair<>(ExtraMenuBean.LIST_TYPE_MAP, "List Map (%d)"),
                new Pair<>(4, "List Custom (%d)")
        );
        for (Pair<Integer, String> listType : listTypes) {
            ArrayList<String> lists = getUsedList(listType.first);
            for (int i = 0, size = lists.size(); i < size; i++) {
                if (i == 0) data.add(new Item(String.format(listType.second, size)));
                data.add(new Item(lists.get(i), true));
            }
        }
        if (data.isEmpty()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.logic_editor_message_no_lists));
            return;
        }

        RenameAdapter adapter = new RenameAdapter(logicEditor, data);
        recyclerView.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(logicEditor);
        builder.setTitle(R.string.logic_editor_title_manage_list);
        builder.setView(recyclerView);
        builder.setNegativeButton(R.string.common_word_cancel, null);
        androidx.appcompat.app.AlertDialog dialog = builder.show();
        adapter.setOnItemClickListener(name -> {
            dialog.dismiss();
            showManageActionDialog(name, true);
        });
    }

    private void showManageActionDialog(String name, boolean isList) {
        new MaterialAlertDialogBuilder(logicEditor)
                .setTitle(name)
                .setItems(new CharSequence[]{
                        Helper.getResString(R.string.common_word_rename),
                        Helper.getResString(R.string.common_word_remove)
                }, (d, which) -> {
                    if (which == 0) {
                        showRenameInputDialog(name, isList, isList ? this::manageList : this::manageVariables);
                    } else {
                        boolean inUse;
                        if (isList) {
                            inUse = logicEditor.blockPane.hasListReference(name)
                                    || projectDataManager.isListUsedInBlocks(javaName, name, currentBlockEntryKey);
                        } else {
                            inUse = logicEditor.blockPane.hasMapReference(name)
                                    || projectDataManager.isVariableUsedInBlocks(javaName, name, currentBlockEntryKey);
                        }
                        if (inUse) {
                            SketchwareUtil.toastError(Helper.getResString(isList
                                    ? R.string.logic_editor_message_currently_used_list
                                    : R.string.logic_editor_message_currently_used_variable));
                        } else {
                            confirmRemoveItem(name, isList);
                        }
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void confirmRemoveItem(String name, boolean isList) {
        new MaterialAlertDialogBuilder(logicEditor)
                .setTitle(name)
                .setMessage(R.string.common_word_delete_confirm)
                .setPositiveButton(R.string.common_word_remove, (dialog, which) -> {
                    if (isList) {
                        logicEditor.removeListVariable(name);
                    } else {
                        logicEditor.removeVariable(name);
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void showRenameInputDialog(String oldName, boolean isList, Runnable onCancel) {
        TextInputLayout inputLayout = new TextInputLayout(logicEditor,
                null, com.google.android.material.R.attr.textInputOutlinedStyle);
        inputLayout.setPadding(dpToPx(20), dpToPx(8), dpToPx(20), 0);
        inputLayout.setHint(Helper.getResString(R.string.logic_editor_hint_new_name));
        TextInputEditText editText = new TextInputEditText(inputLayout.getContext());
        editText.setText(oldName);
        editText.selectAll();
        inputLayout.addView(editText);

        ArrayList<String> existingIds = projectDataManager.getAllIdentifiers(projectFile);
        existingIds.remove(oldName);
        IdentifierValidator validator = new IdentifierValidator(
                getContext(), inputLayout, BlockConstants.RESERVED_KEYWORDS,
                BlockConstants.COMPONENT_TYPES, existingIds);
        editText.addTextChangedListener(validator);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(logicEditor);
        builder.setTitle(String.format(Helper.getResString(R.string.logic_editor_title_enter_new_name), oldName));
        builder.setView(inputLayout);
        builder.setPositiveButton(R.string.common_word_save, null);
        builder.setNegativeButton(R.string.common_word_cancel, (d, w) -> onCancel.run());
        androidx.appcompat.app.AlertDialog dialog = builder.show();
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (newName.isEmpty() || newName.equals(oldName)) {
                dialog.dismiss();
                return;
            }
            if (!validator.isValid()) return;
            if (isList) {
                logicEditor.renameListVariable(oldName, newName);
                SketchwareUtil.toast(String.format(
                        Helper.getResString(R.string.logic_editor_message_list_renamed), oldName, newName));
            } else {
                logicEditor.renameVariable(oldName, newName);
                SketchwareUtil.toast(String.format(
                        Helper.getResString(R.string.logic_editor_message_variable_renamed), oldName, newName));
            }
            dialog.dismiss();
        });
    }

    private void findVariableReferences(boolean isList) {
        int horizontalPadding = dpToPx(20);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, dpToPx(8), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(logicEditor));

        List<Item> data = new LinkedList<>();
        if (isList) {
            List<Pair<Integer, String>> listTypes = List.of(
                    new Pair<>(ExtraMenuBean.LIST_TYPE_NUMBER, "List Integer (%d)"),
                    new Pair<>(ExtraMenuBean.LIST_TYPE_STRING, "List String (%d)"),
                    new Pair<>(ExtraMenuBean.LIST_TYPE_MAP, "List Map (%d)"),
                    new Pair<>(4, "List Custom (%d)")
            );
            for (Pair<Integer, String> listType : listTypes) {
                ArrayList<String> lists = getUsedList(listType.first);
                for (int i = 0, size = lists.size(); i < size; i++) {
                    if (i == 0) data.add(new Item(String.format(listType.second, size)));
                    data.add(new Item(lists.get(i), true));
                }
            }
        } else {
            List<Pair<List<Integer>, String>> variableTypes = List.of(
                    new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_BOOLEAN), "Boolean (%d)"),
                    new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_NUMBER), "Number (%d)"),
                    new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_STRING), "String (%d)"),
                    new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_MAP), "Map (%d)"),
                    new Pair<>(List.of(5, 6), "Custom Variable (%d)")
            );
            for (Pair<List<Integer>, String> variableType : variableTypes) {
                List<String> instances = new LinkedList<>();
                for (Integer type : variableType.first) {
                    instances.addAll(getUsedVariable(type));
                }
                for (int i = 0, size = instances.size(); i < size; i++) {
                    if (i == 0) data.add(new Item(String.format(variableType.second, size)));
                    data.add(new Item(instances.get(i), true));
                }
            }
        }

        if (data.isEmpty()) {
            SketchwareUtil.toastError(Helper.getResString(isList
                    ? R.string.logic_editor_message_no_lists
                    : R.string.logic_editor_message_no_variables));
            return;
        }

        RenameAdapter adapter = new RenameAdapter(logicEditor, data);
        recyclerView.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(logicEditor);
        builder.setTitle(isList
                ? R.string.logic_editor_find_refs_select_list
                : R.string.logic_editor_find_refs_select_variable);
        builder.setView(recyclerView);
        builder.setNegativeButton(R.string.common_word_cancel, null);
        androidx.appcompat.app.AlertDialog dialog = builder.show();
        adapter.setOnItemClickListener(name -> {
            dialog.dismiss();
            showVariableReferencesResult(name, isList);
        });
    }

    private void showVariableReferencesResult(String variableName, boolean isList) {
        logicEditor.saveBlocks();
        ArrayList<ProjectDataStore.VariableReference> references =
                projectDataManager.findVariableReferences(javaName, variableName, isList);

        if (references.isEmpty()) {
            SketchwareUtil.toast(Helper.getResString(R.string.logic_editor_message_no_refs_found));
            return;
        }

        LinkedHashMap<String, List<ProjectDataStore.VariableReference>> grouped = new LinkedHashMap<>();
        for (ProjectDataStore.VariableReference ref : references) {
            grouped.computeIfAbsent(ref.blockEntryKey, k -> new LinkedList<>()).add(ref);
        }

        int horizontalPadding = dpToPx(20);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, dpToPx(8), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(logicEditor));

        List<Item> data = new ArrayList<>();
        ArrayList<ProjectDataStore.VariableReference> refByPosition = new ArrayList<>();
        for (Map.Entry<String, List<ProjectDataStore.VariableReference>> entry : grouped.entrySet()) {
            String blockEntryKey = entry.getKey();
            List<ProjectDataStore.VariableReference> refs = entry.getValue();
            boolean isCurrentEntry = blockEntryKey.equals(currentBlockEntryKey);
            String suffix = isCurrentEntry ? " ★" : "";
            data.add(new Item(blockEntryKey + " (" + refs.size() + ")" + suffix));
            refByPosition.add(null);
            for (ProjectDataStore.VariableReference ref : refs) {
                data.add(new Item(ref.opCode, true));
                refByPosition.add(ref);
            }
        }

        RenameAdapter adapter = new RenameAdapter(logicEditor, data);
        recyclerView.setAdapter(adapter);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(logicEditor);
        builder.setTitle(String.format(Helper.getResString(R.string.logic_editor_title_find_refs), variableName));
        builder.setMessage(String.format(Helper.getResString(R.string.logic_editor_find_refs_count), references.size()));
        builder.setView(recyclerView);
        builder.setNegativeButton(R.string.common_word_cancel, null);
        androidx.appcompat.app.AlertDialog dialog = builder.show();

        adapter.setOnItemClickByPositionListener(position -> {
            if (position < 0 || position >= refByPosition.size()) return;
            ProjectDataStore.VariableReference ref = refByPosition.get(position);
            if (ref == null) return;
            dialog.dismiss();
            if (ref.blockEntryKey.equals(currentBlockEntryKey)) {
                BlockView blockView = logicEditor.blockPane.findBlockByString(ref.blockId);
                if (blockView != null) {
                    logicEditor.blockPane.clearSearchHighlight();
                    ArrayList<BlockView> single = new ArrayList<>();
                    single.add(blockView);
                    logicEditor.blockPane.highlightSearchResults(single, blockView);
                    logicEditor.scrollToBlock(blockView);
                }
            } else {
                navigateToEventByKey(ref.blockEntryKey, ref.blockId);
            }
        });
    }

    private void navigateToEventByKey(String blockEntryKey, String blockId) {
        ArrayList<EventBean> events = projectDataManager.getEvents(javaName);
        for (EventBean event : events) {
            String eventBlockEntryKey = event.targetId + "_" + event.eventName;
            if (eventBlockEntryKey.equals(blockEntryKey)) {
                logicEditor.navigateToEvent(event.targetId, event.eventName, blockId);
                return;
            }
        }
        SketchwareUtil.toast(blockEntryKey);
    }

    private static class RenameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;
        private final List<Item> data;
        private java.util.function.Consumer<String> onItemClickListener;
        private java.util.function.IntConsumer onItemClickByPositionListener;

        private RenameAdapter(Context context, List<Item> data) {
            this.context = context;
            this.data = data;
        }

        public void setOnItemClickListener(java.util.function.Consumer<String> listener) {
            this.onItemClickListener = listener;
        }

        public void setOnItemClickByPositionListener(java.util.function.IntConsumer listener) {
            this.onItemClickByPositionListener = listener;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Item.TYPE_TITLE) {
                TextView textView = new TextView(context);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT));
                textView.setPadding(dpToPx(4), dpToPx(12), dpToPx(4), dpToPx(4));
                textView.setTextSize(12);
                textView.setAlpha(0.7f);
                return new TitleHolder(textView);
            } else {
                TextView textView = new TextView(context);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                textView.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
                textView.setTextSize(16);
                android.util.TypedValue outValue = new android.util.TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                textView.setBackgroundResource(outValue.resourceId);
                textView.setClickable(true);
                textView.setFocusable(true);
                return new ItemHolder(textView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = data.get(position);
            if (holder instanceof TitleHolder titleHolder) {
                titleHolder.title.setText(item.text);
            } else if (holder instanceof ItemHolder itemHolder) {
                itemHolder.textView.setText(item.text);
                itemHolder.textView.setOnClickListener(v -> {
                    int adapterPos = holder.getAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION && onItemClickByPositionListener != null) {
                        onItemClickByPositionListener.accept(adapterPos);
                    } else if (onItemClickListener != null) {
                        onItemClickListener.accept(item.text);
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).type;
        }

        private static class TitleHolder extends RecyclerView.ViewHolder {
            public final TextView title;

            public TitleHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView;
            }
        }

        private static class ItemHolder extends RecyclerView.ViewHolder {
            public final TextView textView;

            public ItemHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    private static class Item {
        public static final int TYPE_TITLE = 0;
        public static final int TYPE_ITEM = 1;

        private final int type;
        private final String text;

        public Item(String title) {
            type = TYPE_TITLE;
            text = title;
        }

        public Item(String itemName, boolean ignored) {
            type = TYPE_ITEM;
            text = itemName;
        }

        public int getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
