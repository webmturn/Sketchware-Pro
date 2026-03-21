package pro.sketchware.activities.resourceseditor.components.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.databinding.PalletCustomviewBinding;

public class ArraysAdapter extends RecyclerView.Adapter<ArraysAdapter.ArrayViewHolder> {

    private final List<ArrayModel> originalList;
    private List<ArrayModel> filteredList;
    private final HashMap<Integer, String> notesMap;
    private final Fragment fragment;

    public ArraysAdapter(ArrayList<ArrayModel> arraysList, Fragment fragment, HashMap<Integer, String> notesMap) {
        originalList = new ArrayList<>(arraysList);
        filteredList = new ArrayList<>(arraysList);
        this.notesMap = notesMap;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ArrayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PalletCustomviewBinding binding = PalletCustomviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ArrayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ArrayViewHolder holder, int position) {
        ArrayModel array = filteredList.get(position);
        holder.bind(array);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String newText) {
        if (newText == null || newText.isEmpty()) {
            filteredList = new ArrayList<>(originalList);
        } else {
            ArrayList<ArrayModel> filtered = new ArrayList<>();
            for (ArrayModel array : originalList) {
                if (array.getArrayName().toLowerCase().contains(newText)) {
                    filtered.add(array);
                }
            }
            filteredList = filtered;
        }

        notifyDataSetChanged();
    }

    public class ArrayViewHolder extends RecyclerView.ViewHolder {

        private final PalletCustomviewBinding binding;

        public ArrayViewHolder(PalletCustomviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ArrayModel array) {
            binding.title.setText(array.getArrayName());
            binding.sub.setText(array.getArrayType().name());
            int originalIndex = originalList.indexOf(array);
            if (originalIndex >= 0 && notesMap.containsKey(originalIndex)) {
                binding.tvTitle.setText(notesMap.get(originalIndex));
                binding.tvTitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvTitle.setVisibility(View.GONE);
            }

            binding.backgroundCard.setOnClickListener(view -> {
                if (fragment instanceof ArraysEditor arraysEditor) {
                    arraysEditor.showArrayAttributesDialog(array);
                }
            });

            binding.backgroundCard.setOnLongClickListener(view -> {
                if (fragment instanceof ArraysEditor arraysEditor) {
                    arraysEditor.showEditArrayDialog(array, originalIndex);
                }
                return true;
            });
        }
    }
}
