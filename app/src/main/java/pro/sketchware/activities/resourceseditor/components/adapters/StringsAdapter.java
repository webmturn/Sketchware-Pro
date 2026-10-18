package pro.sketchware.activities.resourceseditor.components.adapters;

import static pro.sketchware.activities.editor.LogicEditorActivity.getAllJavaFileNames;
import static pro.sketchware.activities.editor.LogicEditorActivity.getAllXmlFileNames;
import static pro.sketchware.util.UI.animateLayoutChanges;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ViewBean;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.databinding.PalletCustomviewBinding;
import pro.sketchware.databinding.ViewStringEditorAddBinding;
import pro.sketchware.util.SketchwareUtil;

public class StringsAdapter extends RecyclerView.Adapter<StringsAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> originalData;
    private final ArrayList<HashMap<String, Object>> sourceData;
    private final ResourcesEditorActivity activity;
    private final HashMap<Integer, String> notesMap;
    private ArrayList<HashMap<String, Object>> filteredData;

    public StringsAdapter(ResourcesEditorActivity activity, ArrayList<HashMap<String, Object>> data, HashMap<Integer, String> notesMap) {
        sourceData = data;
        originalData = new ArrayList<>(data);
        filteredData = new ArrayList<>(data);
        this.activity = activity;
        this.notesMap = notesMap;
    }

    @NonNull
    @Override
    public StringsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PalletCustomviewBinding itemBinding = PalletCustomviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StringsAdapter.ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull StringsAdapter.ViewHolder holder, int position) {
        HashMap<String, Object> item = filteredData.get(position);
        Object keyObj = item.get("key");
        Object textObj = item.get("text");
        String key = keyObj != null ? keyObj.toString() : "";
        String text = textObj != null ? textObj.toString() : "";
        holder.binding.title.setText(key);
        holder.binding.sub.setText(text);

        int originalIndex = identityIndexOf(sourceData, item);
        if (originalIndex >= 0 && notesMap.containsKey(originalIndex)) {
            holder.binding.tvTitle.setText(notesMap.get(originalIndex));
            holder.binding.tvTitle.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvTitle.setVisibility(View.GONE);
        }

        holder.binding.backgroundCard.setOnClickListener(v -> {
            int adapterPosition = holder.getAbsoluteAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= filteredData.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            HashMap<String, Object> currentItem = filteredData.get(adapterPosition);

            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
            ViewStringEditorAddBinding dialogBinding = ViewStringEditorAddBinding.inflate(activity.getLayoutInflater());

            dialogBinding.stringKeyInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    animateLayoutChanges(dialogBinding.getRoot());
                    dialogBinding.importantNote.setVisibility(s.toString().equals("app_name") ? View.VISIBLE : View.GONE);
                }
            });
            dialogBinding.stringKeyInput.setText((String) currentItem.get("key"));
            dialogBinding.stringValueInput.setText((String) currentItem.get("text"));
            int origIdx = identityIndexOf(sourceData, currentItem);
            dialogBinding.stringHeaderInput.setText(origIdx >= 0 ? notesMap.getOrDefault(origIdx, "") : "");

            if ("app_name".equals(currentItem.get("key"))) {
                dialogBinding.stringKeyInput.setEnabled(false);
            }

            dialog.setTitle(R.string.strings_edit_title);
            dialog.setPositiveButton(R.string.common_word_save, (d, which) -> {
                String keyInput = Objects.requireNonNull(dialogBinding.stringKeyInput.getText()).toString();
                String valueInput = Objects.requireNonNull(dialogBinding.stringValueInput.getText()).toString();
                if (keyInput.isEmpty() || valueInput.isEmpty()) {
                    SketchwareUtil.toast(Helper.getResString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT);
                    return;
                }
                String note = Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString().trim();
                int index = editString(currentItem, keyInput, valueInput, note);
                if (index < 0) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                    return;
                }
                activity.stringsEditor.markStringChanged(index, false);
            });

            String keyInput = Objects.requireNonNull(dialogBinding.stringKeyInput.getText()).toString();
            if (!keyInput.equals("app_name")) {
                dialog.setNeutralButton(Helper.getResString(R.string.common_word_delete), (d, which) -> {
                    if (isXmlStringUsed(key)) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.logic_editor_title_remove_xml_string_error));
                    } else {
                        int removedIndex = removeString(currentItem);
                        if (removedIndex < 0) {
                            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                            return;
                        }
                        activity.stringsEditor.updateNoContentLayout();
                        activity.stringsEditor.markStringChanged(removedIndex, true);
                    }
                });
            }
            dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
            dialog.setView(dialogBinding.getRoot());
            dialog.show();
        });
    }

    // Resolve positions before mutating a Map: equal resource contents do not identify the same row.
    private int editString(HashMap<String, Object> item, String key, String value, String note) {
        int sourceIndex = identityIndexOf(sourceData, item);
        int filteredIndex = identityIndexOf(filteredData, item);
        if (sourceIndex < 0 || identityIndexOf(originalData, item) < 0) return -1;
        item.put("key", key);
        item.put("text", value);
        if (note.isEmpty()) notesMap.remove(sourceIndex);
        else notesMap.put(sourceIndex, note);
        if (filteredIndex >= 0) notifyItemChanged(filteredIndex);
        else notifyDataSetChanged();
        return sourceIndex;
    }

    private int removeString(HashMap<String, Object> item) {
        int sourceIndex = identityIndexOf(sourceData, item);
        int originalIndex = identityIndexOf(originalData, item);
        int filteredIndex = identityIndexOf(filteredData, item);
        if (sourceIndex < 0 || originalIndex < 0 || filteredIndex < 0) return -1;
        sourceData.remove(sourceIndex);
        originalData.remove(originalIndex);
        filteredData.remove(filteredIndex);
        Map<Integer, String> reindexedNotes = new HashMap<>();
        for (Map.Entry<Integer, String> entry : notesMap.entrySet()) {
            int index = entry.getKey();
            if (index != sourceIndex) reindexedNotes.put(index > sourceIndex ? index - 1 : index, entry.getValue());
        }
        notesMap.clear();
        notesMap.putAll(reindexedNotes);
        notifyItemRemoved(filteredIndex);
        notifyItemRangeChanged(filteredIndex, filteredData.size() - filteredIndex);
        return sourceIndex;
    }

    private static int identityIndexOf(List<?> items, Object item) {
        for (int i = 0; i < items.size(); i++) if (items.get(i) == item) return i;
        return -1;
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public void filter(String query) {
        if (query == null || query.isEmpty()) {
            filteredData = new ArrayList<>(originalData);
        } else {
            filteredData = new ArrayList<>();
            for (HashMap<String, Object> item : originalData) {
                Object keyObj = item.get("key");
                Object textObj = item.get("text");
                String key = keyObj != null ? keyObj.toString() : null;
                String text = textObj != null ? textObj.toString() : null;
                if (key != null && key.toLowerCase().contains(query) || text != null && text.toLowerCase().contains(query)) {
                    filteredData.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public boolean isXmlStringUsed(String key) {
        if ("app_name".equals(key) || activity.sc_id == null) {
            return false;
        }

        String projectScId = activity.sc_id;
        ProjectDataStore projectDataManager = ProjectDataManager.getProjectDataManager(projectScId);

        return isStringUsedInJavaFiles(projectScId, projectDataManager, key) || isStringUsedInXmlFiles(projectScId, projectDataManager, key);
    }

    private boolean isStringUsedInJavaFiles(String projectScId, ProjectDataStore projectDataManager, String key) {
        for (String javaFileName : getAllJavaFileNames(projectScId)) {
            for (Map.Entry<String, ArrayList<BlockBean>> entry : projectDataManager.getBlockMap(javaFileName).entrySet()) {
                for (BlockBean block : entry.getValue()) {
                    if ("getResStr".equals(block.opCode) && key.equals(block.spec) ||
                            "getResString".equals(block.opCode) && block.parameters != null && !block.parameters.isEmpty() && ("R.string." + key).equals(block.parameters.get(0))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isStringUsedInXmlFiles(String projectScId, ProjectDataStore projectDataManager, String key) {
        for (String xmlFileName : getAllXmlFileNames(projectScId)) {
            for (ViewBean view : projectDataManager.getViews(xmlFileName)) {
                if (view != null && view.text != null && ("@string/" + key).equals(view.text.text) ||
                        view != null && view.text != null && ("@string/" + key).equals(view.text.hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        PalletCustomviewBinding binding;

        public ViewHolder(PalletCustomviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
