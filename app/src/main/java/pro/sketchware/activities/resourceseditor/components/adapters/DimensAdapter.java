package pro.sketchware.activities.resourceseditor.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.activities.resourceseditor.components.models.DimenModel;
import pro.sketchware.databinding.PalletCustomviewBinding;

public class DimensAdapter extends RecyclerView.Adapter<DimensAdapter.ViewHolder> {

    private final ArrayList<DimenModel> originalData;
    private final HashMap<Integer, String> notesMap;
    private final ResourcesEditorActivity activity;
    private ArrayList<DimenModel> filteredData;

    public DimensAdapter(ArrayList<DimenModel> filteredData, ResourcesEditorActivity activity, HashMap<Integer, String> notesMap) {
        originalData = new ArrayList<>(filteredData);
        this.filteredData = filteredData;
        this.activity = activity;
        this.notesMap = notesMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PalletCustomviewBinding itemBinding = PalletCustomviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DimenModel dimenModel = filteredData.get(position);
        int originalIndex = originalData.indexOf(dimenModel);
        if (originalIndex >= 0 && notesMap.containsKey(originalIndex)) {
            holder.itemBinding.tvTitle.setText(notesMap.get(originalIndex));
            holder.itemBinding.tvTitle.setVisibility(View.VISIBLE);
        } else {
            holder.itemBinding.tvTitle.setVisibility(View.GONE);
        }

        holder.itemBinding.title.setText(dimenModel.getDimenName());
        holder.itemBinding.sub.setText(dimenModel.getDimenValue());
        holder.itemBinding.color.setVisibility(View.GONE);

        holder.itemBinding.backgroundCard.setOnClickListener(v -> activity.dimensEditor.showDimenEditDialog(dimenModel, position));
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public void filter(String newText) {
        if (newText == null || newText.isEmpty()) {
            filteredData = new ArrayList<>(originalData);
        } else {
            String filterText = newText.toLowerCase().trim();
            filteredData = originalData.stream()
                    .filter(item -> item.getDimenName().toLowerCase().contains(filterText) ||
                            item.getDimenValue().toLowerCase().contains(filterText))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public PalletCustomviewBinding itemBinding;

        public ViewHolder(PalletCustomviewBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }
    }
}
