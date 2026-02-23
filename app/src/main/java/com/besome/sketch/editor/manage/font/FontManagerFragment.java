package com.besome.sketch.editor.manage.font;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectResourceBean;

import java.io.File;
import java.util.ArrayList;

import pro.sketchware.core.ImageCollectionManager;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.BaseFragment;
import pro.sketchware.core.SketchwarePaths;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.FrManageFontListBinding;
import pro.sketchware.databinding.ManageFontBinding;
import pro.sketchware.databinding.ManageFontListItemBinding;

public class FontManagerFragment extends BaseFragment {

    private ActivityResultLauncher<Intent> importFontLauncher;
    public String sc_id;
    public fontAdapter adapter;
    public String dirPath = "";
    public ArrayList<ProjectResourceBean> projectResourceBeans;
    private FrManageFontListBinding binding;
    private ManageFontBinding actBinding;

    public void processProjectResources(ArrayList<ProjectResourceBean> resourceBeans) {
        if (resourceBeans == null || resourceBeans.isEmpty()) {
            return;
        }

        ArrayList<ProjectResourceBean> processedResources = new ArrayList<>();

        for (ProjectResourceBean resource : resourceBeans) {
            processedResources.add(new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                    resource.resName,
                    resource.resFullName
            ));
        }

        if (!processedResources.isEmpty()) {
            ManageFontActivity activity = (ManageFontActivity) requireActivity();
            activity.projectFontsFragment.handleResourceImport(processedResources);
            activity.binding.viewPager.setCurrentItem(0);
        }
    }

    public void loadProjectResources() {
        projectResourceBeans = ImageCollectionManager.g().f();
        adapter.notifyDataSetChanged();

        if (projectResourceBeans.isEmpty()) {
            binding.tvGuide.setVisibility(View.VISIBLE);
            binding.fontList.setVisibility(View.GONE);
        } else {
            binding.fontList.setVisibility(View.VISIBLE);
            binding.tvGuide.setVisibility(View.GONE);
        }
    }

    public final void resetSelection() {
        projectResourceBeans.forEach(resource -> resource.isSelected = false);
        adapter.notifyDataSetChanged();
        actBinding.layoutBtnImport.setVisibility(View.GONE);
    }

    public boolean isSelecting() {
        return projectResourceBeans.stream().anyMatch(resource -> resource.isSelected);
    }

    public final void updateImportButtonVisibility() {
        int selectedCount = (int) projectResourceBeans.stream().filter(resource -> resource.isSelected).count();

        if (selectedCount > 0) {
            actBinding.btnImport.setText(Helper.getResString(R.string.common_word_import_count, selectedCount));
            actBinding.layoutBtnImport.animate().translationY(0F).setDuration(200L).start();
            actBinding.layoutBtnImport.setVisibility(View.VISIBLE);
        } else {
            actBinding.layoutBtnImport.animate().translationY(400F).setDuration(200L).start();
            actBinding.layoutBtnImport.setVisibility(View.GONE);
        }
    }

    public void importSelectedFonts() {
        ArrayList<ProjectResourceBean> selectedFonts = new ArrayList<>();

        for (ProjectResourceBean resource : projectResourceBeans) {
            if (resource.isSelected) {
                String path = SketchwarePaths.getCollectionPath() +
                        File.separator +
                        "font" +
                        File.separator +
                        "data" +
                        File.separator +
                        resource.resFullName;
                selectedFonts.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, resource.resName, path));
            }
        }

        if (!selectedFonts.isEmpty()) {
            ArrayList<ProjectResourceBean> fontCollection = ((ManageFontActivity) requireActivity()).projectFontsFragment.getProjectResourceBeans();
            Intent intent = new Intent(getActivity(), ManageFontImportActivity.class);
            intent.putParcelableArrayListExtra("project_fonts", fontCollection);
            intent.putParcelableArrayListExtra("selected_collections", selectedFonts);
            importFontLauncher.launch(intent);
        }

        resetSelection();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        importFontLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        processProjectResources(result.getData().getParcelableArrayListExtra("results"));
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new EncryptedFileUtil().f(dirPath);

        if (savedInstanceState == null) {
            sc_id = requireActivity().getIntent().getStringExtra("sc_id");
            dirPath = requireActivity().getIntent().getStringExtra("dir_path");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            dirPath = savedInstanceState.getString("dir_path");
        }

        loadProjectResources();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        binding = FrManageFontListBinding.inflate(layoutInflater, viewGroup, false);
        actBinding = ((ManageFontActivity) requireActivity()).binding;

        binding.fontList.setLayoutManager(new LinearLayoutManager(requireActivity()));
        adapter = new fontAdapter();
        binding.fontList.setAdapter(adapter);

        actBinding.btnImport.setOnClickListener(v -> importSelectedFonts());

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ImageCollectionManager.g().d();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("sc_id", sc_id);
        bundle.putString("dir_path", dirPath);
    }

    public class fontAdapter extends RecyclerView.Adapter<fontAdapter.fontHolder> {
        private int selectedPosition = -1;

        @Override
        public int getItemCount() {
            return projectResourceBeans.size();
        }

        @Override
        public void onBindViewHolder(fontHolder holder, int position) {
            ProjectResourceBean resource = projectResourceBeans.get(position);

            String fontPath = SketchwarePaths.getCollectionPath() + File.separator + "font" + File.separator + "data" + File.separator + resource.resFullName;

            holder.binding.chkSelect.setVisibility(View.VISIBLE);
            holder.binding.chkSelect.setChecked(resource.isSelected);
            holder.binding.tvFontName.setText(resource.resName + ".ttf");

            try {
                holder.binding.tvFontPreview.setTypeface(Typeface.createFromFile(fontPath));
            } catch (Exception e) {
                android.util.Log.e("FontManagerFragment", "Failed to load font from path: " + fontPath, e);
                // Set default font as fallback
                holder.binding.tvFontPreview.setTypeface(Typeface.DEFAULT);
            }
        }

        @NonNull
        @Override
        public fontHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageFontListItemBinding binding = ManageFontListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new fontHolder(binding);
        }

        public class fontHolder extends RecyclerView.ViewHolder {
            public ManageFontListItemBinding binding;

            public fontHolder(ManageFontListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.getRoot().setOnClickListener(view -> binding.chkSelect.setChecked(!binding.chkSelect.isChecked()));

                binding.chkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    int position = getLayoutPosition();
                    selectedPosition = position;
                    ProjectResourceBean resource = projectResourceBeans.get(position);
                    resource.isSelected = isChecked;
                    updateImportButtonVisibility();

                    new Handler(Looper.getMainLooper()).post(() -> notifyItemChanged(selectedPosition));
                });

            }
        }
    }
}
