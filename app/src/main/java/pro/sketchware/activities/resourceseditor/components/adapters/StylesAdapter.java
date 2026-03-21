package pro.sketchware.activities.resourceseditor.components.adapters;


import mod.hey.studios.util.Helper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.fragments.StylesEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.ThemesEditor;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.databinding.PalletCustomviewBinding;

public class StylesAdapter extends RecyclerView.Adapter<StylesAdapter.StyleViewHolder> {

    private final List<StyleModel> originalList;
    private List<StyleModel> filteredList;
    private final HashMap<Integer, String> notesMap;
    private final Fragment fragment;

    public StylesAdapter(ArrayList<StyleModel> stylesList, Fragment fragment, HashMap<Integer, String> notesMap) {
        originalList = new ArrayList<>(stylesList);
        filteredList = new ArrayList<>(stylesList);
        this.notesMap = notesMap;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public StyleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PalletCustomviewBinding binding = PalletCustomviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StyleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StyleViewHolder holder, int position) {
        StyleModel style = filteredList.get(position);
        holder.bind(style);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String newText) {
        if (newText == null || newText.isEmpty()) {
            filteredList = new ArrayList<>(originalList);
        } else {
            ArrayList<StyleModel> filtered = new ArrayList<>();
            for (StyleModel style : originalList) {
                if (style.getStyleName().toLowerCase().contains(newText)) {
                    filtered.add(style);
                }
            }
            filteredList = filtered;
        }

        notifyDataSetChanged();
    }

    public class StyleViewHolder extends RecyclerView.ViewHolder {

        private final PalletCustomviewBinding binding;

        public StyleViewHolder(PalletCustomviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StyleModel style) {
            binding.title.setText(style.getStyleName());
            if (style.getParent().isEmpty()) {
                binding.sub.setText(Helper.getResString(R.string.style_no_parent));
            } else {
                binding.sub.setText(style.getParent());
            }
            int originalIndex = originalList.indexOf(style);
            if (originalIndex >= 0 && notesMap.containsKey(originalIndex)) {
                binding.tvTitle.setText(notesMap.get(originalIndex));
                binding.tvTitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvTitle.setVisibility(View.GONE);
            }

            binding.backgroundCard.setOnClickListener(view -> {
                if (fragment instanceof StylesEditor stylesEditor) {
                    stylesEditor.showStyleAttributesDialog(style);
                } else if (fragment instanceof ThemesEditor themesEditor) {
                    themesEditor.showThemeAttributesDialog(style);
                }
            });

            binding.backgroundCard.setOnLongClickListener(view -> {
                if (fragment instanceof StylesEditor stylesEditor) {
                    stylesEditor.showEditStyleDialog(style, originalIndex);
                } else if (fragment instanceof ThemesEditor themesEditor) {
                    themesEditor.showEditThemeDialog(style, originalIndex);
                }
                return true;
            });
        }
    }
}
