package pro.sketchware.core;


import mod.hey.studios.util.Helper;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.editor.manage.image.AddImageActivity;
import com.besome.sketch.editor.manage.image.ManageImageActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import pro.sketchware.R;
import pro.sketchware.activities.importicon.ImportIconActivity;
import pro.sketchware.databinding.FrManageImageListBinding;
import pro.sketchware.databinding.ManageImageListItemBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.SvgUtils;

public class ImageListFragment extends BaseFragment implements MenuProvider {

    private final FilePathUtil fpu = new FilePathUtil();
    public boolean isSelecting = false;
    public SvgUtils svgUtils;
    Map<Integer, Map<String, Object>> colorMap = new HashMap<>();
    private FrManageImageListBinding binding;
    private String sc_id;
    private ArrayList<ProjectResourceBean> images;
    private MaterialCardView actionButtonContainer;
    private FloatingActionButton fab;
    private String projectImagesDirectory = "";
    private Adapter adapter = null;
    private final ActivityResultLauncher<Intent> openImportIconActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            var data = result.getData();
            if (data == null) return;
            ProjectResourceBean icon = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                    data.getStringExtra("iconName"), data.getStringExtra("iconPath")
            );
            icon.savedPos = 2;
            icon.isNew = true;

            int selectedColor = data.getIntExtra("iconColor", -1);
            String selectedColorHex = data.getStringExtra("iconColorHex");
            addNewColorFilterInfo(selectedColorHex, selectedColor, images.size());

            addImage(icon);
            SketchToast.toast(requireActivity(), Helper.getResString(R.string.design_manager_message_add_complete), SketchToast.TOAST_NORMAL).show();
        }
    });
    private final ActivityResultLauncher<Intent> showAddImageDialog = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            var data = result.getData();
            if (data == null) return;
            ArrayList<ProjectResourceBean> addedImages;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addedImages = data.getParcelableArrayListExtra("images", ProjectResourceBean.class);
            } else {
                addedImages = data.getParcelableArrayListExtra("images");
            }
            images.addAll(addedImages);
            adapter.notifyItemRangeInserted(images.size() - addedImages.size(), addedImages.size());
            updateGuideVisibility();
            ((ManageImageActivity) requireActivity()).getCollectionFragment().refreshData();
            SketchToast.toast(requireActivity(), Helper.getResString(R.string.design_manager_message_add_complete), SketchToast.TOAST_NORMAL).show();
        }
    });
    private final ActivityResultLauncher<Intent> showImageDetailsDialog = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            var data = result.getData();
            if (data == null) return;
            ProjectResourceBean editedImage;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                editedImage = data.getParcelableExtra("image", ProjectResourceBean.class);
            } else {
                editedImage = data.getParcelableExtra("image");
            }
            ResourceManager.refreshCacheSignature();
            for (ProjectResourceBean image : images) {
                if (image.resName.equals(editedImage.resName)) {
                    image.copy(editedImage);
                    adapter.notifyItemChanged(images.indexOf(image));
                    break;
                }
            }
            updateGuideVisibility();
            ((ManageImageActivity) requireActivity()).getCollectionFragment().refreshData();
            SketchToast.toast(requireActivity(), Helper.getResString(R.string.design_manager_message_edit_complete), SketchToast.TOAST_NORMAL).show();
        }
    });

    public static void copyFile(String srcPath, String destPath) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);

        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    public ArrayList<ProjectResourceBean> getImages() {
        return images;
    }

    private void initialize() {
        sc_id = requireActivity().getIntent().getStringExtra("sc_id");
        projectImagesDirectory = ProjectDataManager.getResourceManager(sc_id).getImageDirPath();
        ArrayList<ProjectResourceBean> imagesList = ProjectDataManager.getResourceManager(sc_id).images;
        svgUtils = new SvgUtils(requireContext());
        svgUtils.initImageLoader();
        if (imagesList != null) {
            for (ProjectResourceBean next : imagesList) {
                if (next.flipVertical == 0) {
                    next.flipVertical = 1;
                }
                if (next.flipHorizontal == 0) {
                    next.flipHorizontal = 1;
                }
                next.savedPos = 0;
                images.add(next);
            }
        }
    }

    private void unselectAll() {
        for (ProjectResourceBean projectResourceBean : images) {
            projectResourceBean.isSelected = false;
        }
    }

    private void deleteSelected() {
        for (int i = images.size() - 1; i >= 0; i--) {
            if (images.get(i).isSelected) {
                images.remove(i);
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void saveImages() {


        Path svgDir = Paths.get(fpu.getPathSvg(sc_id));
        try {
            if (!Files.exists(svgDir)) {
                Files.createDirectories(svgDir);
            }
        } catch (IOException error) {
            Log.d("pu.java", "Failed to create directory for saving svgs at: " + fpu.getPathResource(sc_id));
        }
        int index = 0;
        for (ProjectResourceBean image : images) {
            if (image.isNew || image.isEdited) {
                try {
                    String path;
                    if (image.savedPos == 0) {
                        path = getImagePath(image);
                    } else {
                        path = image.resFullName;
                    }


                    String imageBasePath = projectImagesDirectory + File.separator + image.resName;
                    Log.d("svg", "full name : " + image.resFullName);
                    if (image.resFullName.endsWith(".svg")) {
                        // convert the svg to vectors
                        String svgPath = fpu.getSvgFullPath(sc_id, image.resName);
                        copyFile(path, svgPath);
                        String colorHex = (String) colorMap.get(index).get("colorHex");
                        colorHex = (colorHex != null) ? colorHex : "#FFFFFF";
                        svgUtils.convert(svgPath, projectImagesDirectory, colorHex);
                    } else {
                        BitmapUtil.processAndSaveBitmap(path, image.isNinePatch() ? imageBasePath + ".9.png" : imageBasePath + ".png", image.rotate, image.flipHorizontal, image.flipVertical);
                    }
                } catch (Exception e) {
                    Log.e("ImageListFragment", e.getMessage(), e);
                }
            }
            index++;
        }
        for (int i = 0; i < images.size(); i++) {
            ProjectResourceBean image = images.get(i);
            if (image.isNew || image.isEdited) {

                // Determine the correct file extension based on image type
                String fileExtension;
                if (image.isNinePatch()) {
                    fileExtension = ".9.png";
                } else if (image.isSvg()) {
                    fileExtension = ".xml";
                } else {
                    fileExtension = ".png";
                }

                // Set the new ProjectResourceBean with the correct file extension
                images.set(i, new ProjectResourceBean(
                        ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                        image.resName,
                        image.resName + fileExtension
                ));
            }
        }
        ProjectDataManager.getResourceManager(sc_id).setImages(images);
        ProjectDataManager.getResourceManager(sc_id).saveToBackup();

        // This method is replaces not exist images to default_image, I removed it because it changes vector images to default_images
        // ProjectDataManager.getProjectDataManager(sc_id).b(ProjectDataManager.getResourceManager(sc_id));

        ProjectDataManager.getProjectDataManager(sc_id).saveAllBackup();
    }

    private void updateGuideVisibility() {
        if (images.isEmpty()) {
            binding.tvGuide.setVisibility(View.VISIBLE);
            binding.imageList.setVisibility(View.GONE);
        } else {
            binding.imageList.setVisibility(View.VISIBLE);
            binding.tvGuide.setVisibility(View.GONE);
        }
    }

    private void showAddImageDialog() {
        Intent intent = new Intent(requireContext(), AddImageActivity.class);
        intent.putParcelableArrayListExtra("images", images);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("dir_path", projectImagesDirectory);
        showAddImageDialog.launch(intent);
    }

    private void openImportIconActivity() {
        Intent intent = new Intent(requireActivity(), ImportIconActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        intent.putStringArrayListExtra("imageNames", getAllImageNames());
        openImportIconActivity.launch(intent);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            initialize();
        } else {
            sc_id = savedInstanceState.getString("sc_id");
            projectImagesDirectory = savedInstanceState.getString("dir_path");
            images = savedInstanceState.getParcelableArrayList("images");
        }
        // mkdirs
        new EncryptedFileUtil().mkdirs(projectImagesDirectory);
        adapter.notifyDataSetChanged();
        updateGuideVisibility();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (binding.imageList.getLayoutManager() instanceof GridLayoutManager manager) {
            manager.setSpanCount(ManageImageActivity.getImageGridColumnCount(requireContext()));
        }
        binding.imageList.requestLayout();
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.manage_image_menu, menu);
        menu.findItem(R.id.menu_image_delete).setVisible(!isSelecting);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FrManageImageListBinding.inflate(inflater, container, false);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        images = new ArrayList<>();
        binding.imageList.setHasFixedSize(true);
        binding.imageList.setLayoutManager(new GridLayoutManager(requireActivity(), ManageImageActivity.getImageGridColumnCount(requireContext())));
        adapter = new Adapter(binding.imageList);
        binding.imageList.setAdapter(adapter);
        binding.tvGuide.setText(R.string.design_manager_image_description_guide_add_image);
        actionButtonContainer = requireActivity().findViewById(R.id.layout_btn_group);
        MaterialButton delete = requireActivity().findViewById(R.id.btn_delete);
        MaterialButton cancel = requireActivity().findViewById(R.id.btn_cancel);
        delete.setOnClickListener(view -> {
            if (isSelecting) {
                deleteSelected();
                setSelectionMode(false);
                updateGuideVisibility();
                SketchToast.toast(requireActivity(), Helper.getResString(R.string.common_message_complete_delete), SketchToast.TOAST_WARNING).show();
                fab.show();
            }
        });
        cancel.setOnClickListener(view -> {
            if (isSelecting) {
                setSelectionMode(false);
            }
        });
        fab = requireActivity().findViewById(R.id.fab);
        fab.setVisibility(View.VISIBLE);
        fab.setOnClickListener(view -> {
            setSelectionMode(false);
            showAddImageDialog();
        });
        ResourceManager.refreshCacheSignature();
        return binding.getRoot();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_image_delete) {
            setSelectionMode(!isSelecting);
            return true;
        } else if (id == R.id.menu_image_import) {
            openImportIconActivity();
            return true;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        outState.putString("dir_path", projectImagesDirectory);
        outState.putParcelableArrayList("images", images);
        super.onSaveInstanceState(outState);
    }

    private void showImageDetailsDialog(ProjectResourceBean projectResourceBean) {
        Intent intent = new Intent(requireContext(), AddImageActivity.class);
        intent.putParcelableArrayListExtra("images", images);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("dir_path", projectImagesDirectory);
        intent.putExtra("edit_target", projectResourceBean);
        showImageDetailsDialog.launch(intent);
    }

    private ArrayList<String> getAllImageNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean projectResourceBean : images) {
            names.add(projectResourceBean.resName);
        }
        return names;
    }

    public void importImages(ArrayList<ProjectResourceBean> arrayList) {
        ArrayList<ProjectResourceBean> imagesToAdd = new ArrayList<>();
        ArrayList<String> duplicateNames = new ArrayList<>();
        for (ProjectResourceBean next : arrayList) {
            String imageName = next.resName;
            if (isImageNameDuplicate(imageName)) {
                duplicateNames.add(imageName);
            } else {
                ProjectResourceBean image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, imageName, next.resFullName);
                image.savedPos = 1;
                image.isNew = true;
                image.rotate = 0;
                image.flipVertical = 1;
                image.flipHorizontal = 1;
                imagesToAdd.add(image);
            }
        }
        addImages(imagesToAdd);
        if (!duplicateNames.isEmpty()) {
            SketchToast.toast(requireActivity(), Helper.getResString(R.string.common_message_name_unavailable) + "\n" +
                    "[" + String.join(", ", duplicateNames) + "]", SketchToast.TOAST_WARNING).show();
        } else {
            SketchToast.toast(requireActivity(), Helper.getResString(R.string.design_manager_message_import_complete), SketchToast.TOAST_WARNING).show();
        }
        adapter.notifyDataSetChanged();
        updateGuideVisibility();
    }

    private boolean isImageNameDuplicate(String imageName) {
        for (ProjectResourceBean image : images) {
            if (image.resName.equals(imageName)) {
                return true;
            }
        }
        return false;
    }

    private String getImagePath(ProjectResourceBean projectResourceBean) {
        return projectImagesDirectory + File.separator + projectResourceBean.resFullName;
    }

    public void setSelectionMode(boolean isSelecting) {
        this.isSelecting = isSelecting;
        requireActivity().invalidateOptionsMenu();
        unselectAll();
        actionButtonContainer.setVisibility(this.isSelecting ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void addImage(ProjectResourceBean projectResourceBean) {
        images.add(projectResourceBean);
        adapter.notifyDataSetChanged();
        adapter.notifyItemInserted(adapter.getItemCount());
        updateGuideVisibility();
    }

    private void addNewColorFilterInfo(String colorHex, int color, int forPosition) {
        Map<String, Object> colorItem1 = new HashMap<>();
        colorItem1.put("colorHex", colorHex);
        colorItem1.put("color", color);

        colorMap.put(forPosition, colorItem1);
        Log.d("color filter", "new color filter item at " + forPosition + ": " + colorHex + " " + color);
    }

    private void addImages(ArrayList<ProjectResourceBean> arrayList) {
        images.addAll(arrayList);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        public Adapter(RecyclerView recyclerView) {
            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (dy > 2) {
                            if (fab.isEnabled()) {
                                fab.hide();
                            }
                        } else if (dy < -2) {
                            if (fab.isEnabled()) {
                                fab.show();
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProjectResourceBean image = images.get(position);

            holder.binding.deleteImgContainer.setVisibility(isSelecting ? View.VISIBLE : View.GONE);
            holder.binding.imgNinePatch.setVisibility(image.isNinePatch() ? View.VISIBLE : View.GONE);
            holder.binding.imgDelete.setImageResource(image.isSelected ? R.drawable.ic_checkmark_green_48dp
                    : R.drawable.ic_trashcan_white_48dp);
            holder.binding.chkSelect.setChecked(image.isSelected);
            holder.binding.tvImageName.setText(image.resName);

            if (colorMap.get(position) != null) {
                int color = colorMap.get(position).get("color") instanceof Number n ? n.intValue() : 0xFFFFFFFF;
                Log.d("Applying filter to " + position, String.valueOf(color));
                holder.binding.img.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else {
                holder.binding.img.clearColorFilter();
            }

            if (svgUtils == null) {
                svgUtils = new SvgUtils(requireContext());
                svgUtils.initImageLoader();
            }

            if (image.resFullName.endsWith(".svg")) {
                svgUtils.loadImage(holder.binding.img, image.isNew ? image.resFullName : String.join(File.separator, projectImagesDirectory, image.resFullName));
            } else if (image.resFullName.endsWith(".xml")) {
                Log.d("loading converted vector: ", fpu.getSvgFullPath(sc_id, image.resName));
                svgUtils.loadImage(holder.binding.img, image.isNew ? image.resFullName : fpu.getSvgFullPath(sc_id, image.resName));
            } else {
                Glide.with(requireActivity())
                        .asBitmap()
                        .load(image.savedPos == 0 ? projectImagesDirectory + File.separator + image.resFullName
                                : images.get(position).resFullName)
                        .transform(new BitmapTransformation() {

                            final String ID = "my-transformation";
                            final byte[] ID_BYTES = ID.getBytes(StandardCharsets.UTF_8);

                            @Override
                            protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
                                return BitmapUtil.scaleAndRotateBitmap(toTransform, image.rotate, image.flipHorizontal, image.flipVertical);
                            }

                            @Override
                            public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
                                messageDigest.update(ID_BYTES);
                            }
                        })
                        .centerCrop()
                        .signature(ResourceManager.getCacheSignature())
                        .error(R.drawable.ic_remove_grey600_24dp)
                        .into(holder.binding.img);
            }
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ManageImageListItemBinding binding = ManageImageListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ManageImageListItemBinding binding;

            public ViewHolder(@NonNull ManageImageListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.img.setOnClickListener(v -> {
                    if (!isSelecting) {
                        if (!(images.get(getLayoutPosition()).resFullName.endsWith(".svg") ||
                                images.get(getLayoutPosition()).resFullName.endsWith(".xml"))) {
                            showImageDetailsDialog(images.get(getLayoutPosition()));
                        }
                    } else {
                        binding.chkSelect.setChecked(!binding.chkSelect.isChecked());
                        images.get(getLayoutPosition()).isSelected = binding.chkSelect.isChecked();
                        notifyItemChanged(getLayoutPosition());
                    }
                });

                binding.img.setOnLongClickListener(v -> {
                    setSelectionMode(true);
                    binding.chkSelect.setChecked(!binding.chkSelect.isChecked());
                    images.get(getLayoutPosition()).isSelected = binding.chkSelect.isChecked();
                    return true;
                });
            }
        }
    }
}