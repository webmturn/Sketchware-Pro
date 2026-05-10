package pro.sketchware.activities.resourceseditor.components.adapters;

import static com.besome.sketch.design.DesignActivity.sc_id;
import static com.besome.sketch.editor.LogicEditorActivity.getAllJavaFileNames;
import static com.besome.sketch.editor.LogicEditorActivity.getAllXmlFileNames;
import static pro.sketchware.utility.UI.animateLayoutChanges;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ViewBean;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectDataManager;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.databinding.PalletCustomviewBinding;
import pro.sketchware.databinding.ViewStringEditorAddBinding;
import pro.sketchware.utility.SketchwareUtil;

public class StringsAdapter extends RecyclerView.Adapter<StringsAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> originalData;
    private final ResourcesEditorActivity activity;
    private final HashMap<Integer, String> notesMap;
    private ArrayList<HashMap<String, Object>> filteredData;

    public StringsAdapter(ResourcesEditorActivity activity, ArrayList<HashMap<String, Object>> data, HashMap<Integer, String> notesMap) {
        originalData = new ArrayList<>(data);
        filteredData = data;
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

        int originalIndex = originalData.indexOf(item);
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
            int origIdx = originalData.indexOf(currentItem);
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
                currentItem.put("key", keyInput);
                currentItem.put("text", valueInput);
                String note = Objects.requireNonNull(dialogBinding.stringHeaderInput.getText()).toString().trim();
                int filteredIndex = filteredData.indexOf(currentItem);
                int oi = originalData.indexOf(currentItem);
                if (oi >= 0) {
                    if (note.isEmpty()) {
                        notesMap.remove(oi);
                    } else {
                        notesMap.put(oi, note);
                    }
                }
                if (filteredIndex >= 0) {
                    notifyItemChanged(filteredIndex);
                } else {
                    notifyDataSetChanged();
                }
                activity.stringsEditor.hasUnsavedChanges = true;
            });

            String keyInput = Objects.requireNonNull(dialogBinding.stringKeyInput.getText()).toString();
            if (!keyInput.equals("app_name")) {
                dialog.setNeutralButton(Helper.getResString(R.string.common_word_delete), (d, which) -> {
                    if (isXmlStringUsed(key)) {
                        SketchwareUtil.toastError(Helper.getResString(R.string.logic_editor_title_remove_xml_string_error));
                    } else {
                        int filteredIndex = filteredData.indexOf(currentItem);
                        int removedIndex = originalData.indexOf(currentItem);
                        if (filteredIndex < 0 || removedIndex < 0) {
                            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                            return;
                        }
                        filteredData.remove(filteredIndex);
                        originalData.remove(currentItem);
                        activity.stringsEditor.listmap.remove(currentItem);
                        if (removedIndex >= 0) {
                            notesMap.remove(removedIndex);
                            HashMap<Integer, String> reindexedNotes = new HashMap<>();
                            for (Map.Entry<Integer, String> entry : notesMap.entrySet()) {
                                int noteIndex = entry.getKey();
                                reindexedNotes.put(noteIndex > removedIndex ? noteIndex - 1 : noteIndex, entry.getValue());
                            }
                            notesMap.clear();
                            notesMap.putAll(reindexedNotes);
                        }
                        notifyItemRemoved(filteredIndex);
                        notifyItemRangeChanged(filteredIndex, filteredData.size() - filteredIndex);
                        activity.stringsEditor.updateNoContentLayout();
                        activity.stringsEditor.hasUnsavedChanges = true;
                    }
                });
            }
            dialog.setNegativeButton(Helper.getResString(R.string.cancel), null);
            dialog.setView(dialogBinding.getRoot());
            dialog.show();
        });
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
        if ("app_name".equals(key) || sc_id == null) {
            return false;
        }

        String projectScId = sc_id;
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
