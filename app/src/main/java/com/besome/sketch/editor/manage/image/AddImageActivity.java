package com.besome.sketch.editor.manage.image;

import android.util.Log;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.lib.base.BaseDialogActivity;
import com.besome.sketch.lib.ui.EasyDeleteEditText;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import pro.sketchware.core.SketchwareException;
import pro.sketchware.core.UriPathResolver;
import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.SoundCollectionManager;
import pro.sketchware.core.FileNameValidator;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.BitmapUtil;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.BlockConstants;
import pro.sketchware.core.CompileException;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class AddImageActivity extends BaseDialogActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ArrayList<ProjectResourceBean> existingImages;
    private TextView tv_add_photo;
    private ImageView preview;
    private ArrayList<Uri> pickedImageUris;
    private FileNameValidator O;
    private EditText ed_input_edittext;
    private EasyDeleteEditText ed_input;
    private ImageView tv_desc;
    private CheckBox chk_collection;
    private String sc_id;
    private ArrayList<ProjectResourceBean> images;
    private boolean multipleImagesPicked = false;
    private LinearLayout layout_img_inform = null;
    private LinearLayout layout_img_modify = null;
    private TextView tv_imgcnt = null;
    private boolean B = false;
    private String imageFilePath = null;
    private int imageRotationDegrees = 0;
    private int imageExifOrientation = 0;
    private int imageScaleY = 1;
    private int imageScaleX = 1;
    private String dir_path = "";
    private boolean editing = false;
    private ProjectResourceBean image = null;

    private void flipImageVertically() {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            if (imageRotationDegrees != 90 && imageRotationDegrees != 270) {
                imageScaleY *= -1;
            } else {
                imageScaleX *= -1;
            }
            refreshPreview();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.cancel_button) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.common_dialog_cancel_button) {
            finish();
        } else if (id == R.id.common_dialog_ok_button) {
            save();
        } else if (id == R.id.img_horizontal) {
            flipImageHorizontally();
        } else if (id == R.id.img_rotate) {
            setImageRotation(imageRotationDegrees + 90);
        } else if (id == R.id.img_selected) {
            preview.setEnabled(false);
            pickImages(!editing);
        } else if (id == R.id.img_vertical) {
            flipImageVertically();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (preview != null) {
                        preview.setEnabled(true);
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            tv_add_photo.setVisibility(View.GONE);
                            imageRotationDegrees = 0;
                            imageScaleY = 1;
                            imageScaleX = 1;
                            if (data.getClipData() == null) {
                                B = true;
                                multipleImagesPicked = false;
                                setImageFromUri(data.getData());
                                if (O != null) {
                                    O.a(1);
                                }
                            } else {
                                ClipData clipData = data.getClipData();
                                if (clipData.getItemCount() == 1) {
                                    B = true;
                                    multipleImagesPicked = false;
                                    setImageFromUri(clipData.getItemAt(0).getUri());
                                    if (O != null) {
                                        O.a(1);
                                    }
                                } else {
                                    handleImagePickClipData(clipData);
                                    multipleImagesPicked = true;
                                    if (O != null) {
                                        O.a(clipData.getItemCount());
                                    }
                                }
                            }
                        }
                    }
                });
        e(getString(R.string.design_manager_image_title_add_image));
        d(getString(R.string.common_word_save));
        setContentView(R.layout.manage_image_add);
        Intent intent = getIntent();
        existingImages = intent.getParcelableArrayListExtra("images");
        sc_id = intent.getStringExtra("sc_id");
        dir_path = intent.getStringExtra("dir_path");
        image = intent.getParcelableExtra("edit_target");
        if (image != null) {
            editing = true;
        }
        layout_img_inform = findViewById(R.id.layout_img_inform);
        layout_img_modify = findViewById(R.id.layout_img_modify);
        chk_collection = findViewById(R.id.chk_collection);
        tv_desc = findViewById(R.id.tv_desc);
        tv_imgcnt = findViewById(R.id.tv_imgcnt);
        tv_add_photo = findViewById(R.id.tv_add_photo);
        preview = findViewById(R.id.img_selected);
        ImageView img_rotate = findViewById(R.id.img_rotate);
        ImageView img_vertical = findViewById(R.id.img_vertical);
        ImageView img_horizontal = findViewById(R.id.img_horizontal);
        ed_input = findViewById(R.id.ed_input);
        ed_input_edittext = ed_input.getEditText();
        ed_input_edittext.setPrivateImeOptions("defaultInputmode=english;");
        ed_input.setHint(getString(R.string.design_manager_image_hint_enter_image_name));
        O = new FileNameValidator(this, ed_input.getTextInputLayout(), BlockConstants.RESERVED_KEYWORDS, getReservedImageNames());
        O.a(1);
        chk_collection.setText(R.string.design_manager_title_add_to_collection);
        tv_add_photo.setText(R.string.design_manager_image_title_add_image);
        preview.setOnClickListener(this);
        img_rotate.setOnClickListener(this);
        img_vertical.setOnClickListener(this);
        img_horizontal.setOnClickListener(this);
        dialogOkButton.setOnClickListener(this);
        dialogCancelButton.setOnClickListener(this);
        B = false;
        imageRotationDegrees = 0;
        imageScaleY = 1;
        imageScaleX = 1;
        new EncryptedFileUtil().f(dir_path); // java.io.File.mkdirs
        images = new ArrayList<>();
    }

    @Override
    public void onPostCreate(Bundle bundle) {
        super.onPostCreate(bundle);
        if (editing) {
            image.isEdited = true;
            e(getString(R.string.design_manager_image_title_edit_image));
            imageRotationDegrees = image.rotate;
            imageScaleX = image.flipHorizontal;
            imageScaleY = image.flipVertical;
            O = new FileNameValidator(this, ed_input.getTextInputLayout(), BlockConstants.RESERVED_KEYWORDS, getReservedImageNames(), image.resName);
            O.a(1);
            ed_input_edittext.setText(image.resName);
            ed_input_edittext.setEnabled(false);
            chk_collection.setEnabled(false);
            tv_add_photo.setVisibility(View.GONE);
            if (image.savedPos == 0) {
                setImageFromFile(a(image));
            } else {
                setImageFromFile(image.resFullName);
            }
        }
    }

    private ArrayList<String> getReservedImageNames() {
        var names = new ArrayList<String>();
        for (var existingImage : existingImages) {
            names.add(existingImage.resName);
        }
        return names;
    }

    private void refreshPreview() {
        preview.setImageBitmap(BitmapUtil.scaleAndRotateBitmap(BitmapUtil.rotateBitmap(BitmapUtil.decodeSampledBitmap(imageFilePath, 1024, 1024), imageExifOrientation), imageRotationDegrees, imageScaleX, imageScaleY));
    }

    private void save() {
        if (a(O)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                k();
                new SaveAsyncTask(this).execute();
            }, 500L);
        }
    }

    private void s() {
        if (tv_desc != null) {
            tv_desc.setVisibility(View.INVISIBLE);
        }
        if (layout_img_inform != null && layout_img_modify != null && tv_imgcnt != null) {
            layout_img_inform.setVisibility(View.GONE);
            layout_img_modify.setVisibility(View.VISIBLE);
            tv_imgcnt.setVisibility(View.GONE);
        }
    }

    private void pickImages(boolean allowMultiple) {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            if (allowMultiple) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.common_word_choose)));
        } catch (ActivityNotFoundException unused) {
            SketchToast.warning(this, getString(R.string.common_error_activity_not_found), SketchToast.TOAST_NORMAL).show();
        }
    }

    private void setImageFromFile(String path) {
        imageFilePath = path;
        preview.setImageBitmap(BitmapUtil.decodeSampledBitmap(path, 1024, 1024));
        int indexOfFilenameExtension = path.lastIndexOf(".");
        if (path.endsWith(".9.png")) {
            indexOfFilenameExtension = path.lastIndexOf(".9.png");
        }
        if (ed_input_edittext != null && (ed_input_edittext.getText() == null || ed_input_edittext.getText().length() <= 0)) {
            ed_input_edittext.setText(path.substring(path.lastIndexOf("/") + 1, indexOfFilenameExtension));
        }
        try {
            imageExifOrientation = BitmapUtil.getExifRotation(path);
            refreshPreview();
        } catch (Exception e) {
            Log.e("AddImageActivity", e.getMessage(), e);
        }
        s();
    }

    private void setImageRotation(int i) {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            imageRotationDegrees = i;
            if (imageRotationDegrees == 360) {
                imageRotationDegrees = 0;
            }
            refreshPreview();
        }
    }

    private void onMultipleImagesPicked(int count) {
        if (layout_img_inform == null || layout_img_modify == null || tv_imgcnt == null) {
            return;
        }
        layout_img_inform.setVisibility(View.VISIBLE);
        layout_img_modify.setVisibility(View.GONE);
        tv_imgcnt.setVisibility(View.VISIBLE);
        tv_imgcnt.setText("+ " + (count - 1) + " more");
    }

    private void flipImageHorizontally() {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            if (imageRotationDegrees != 90 && imageRotationDegrees != 270) {
                imageScaleX *= -1;
            } else {
                imageScaleY *= -1;
            }
            refreshPreview();
        }
    }

    private boolean a(FileNameValidator pb) {
        if (!pb.isValid()) {
            return false;
        }
        if (B || imageFilePath != null) {
            return true;
        }
        tv_desc.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ani_1));
        return false;
    }

    private void setImageFromUri(Uri uri) {
        String filePath;
        if (uri != null && (filePath = UriPathResolver.resolve(this, uri)) != null) {
            setImageFromFile(filePath);
        }
    }

    private void handleImagePickClipData(ClipData clipData) {
        if (clipData != null) {
            pickedImageUris = new ArrayList<>();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                if (i == 0) {
                    setImageFromUri(clipData.getItemAt(i).getUri());
                }
                pickedImageUris.add(clipData.getItemAt(i).getUri());
            }
            onMultipleImagesPicked(clipData.getItemCount());
        }
    }

    private String a(ProjectResourceBean projectResourceBean) {
        return dir_path + File.separator + projectResourceBean.resFullName;
    }

    private static class SaveAsyncTask extends BaseAsyncTask {
        private final WeakReference<AddImageActivity> activity;

        public SaveAsyncTask(AddImageActivity activity) {
            super(activity.getApplicationContext());
            this.activity = new WeakReference<>(activity);
            activity.a(this);
        }

        @Override
        public void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.h();
            Intent intent = new Intent();
            intent.putExtra("sc_id", activity.sc_id);
            if (activity.editing) {
                intent.putExtra("image", activity.image);
            } else {
                intent.putExtra("images", activity.images);
            }
            activity.setResult(RESULT_OK, intent);
            activity.finish();
        }

        @Override
        public void doWork() throws SketchwareException {
            var activity = this.activity.get();
            if (activity == null) return;
            try {
                publishProgress("Now processing..");
                if (!activity.multipleImagesPicked) {
                    if (!activity.editing) {
                        var image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                                Helper.getText(activity.ed_input_edittext).trim(), activity.imageFilePath);
                        image.savedPos = 1;
                        image.isNew = true;
                        image.rotate = activity.imageRotationDegrees;
                        image.flipVertical = activity.imageScaleY;
                        image.flipHorizontal = activity.imageScaleX;
                        if (activity.chk_collection.isChecked()) {
                            SoundCollectionManager.g().a(activity.sc_id, image);
                        }
                        activity.images.add(image);
                    } else if (!activity.B) {
                        var image = activity.image;
                        image.rotate = activity.imageRotationDegrees;
                        image.flipHorizontal = activity.imageScaleX;
                        image.flipVertical = activity.imageScaleY;
                        image.isEdited = true;
                    } else {
                        var image = activity.image;
                        image.resFullName = activity.imageFilePath;
                        image.savedPos = 1;
                        image.rotate = activity.imageRotationDegrees;
                        image.flipVertical = activity.imageScaleY;
                        image.flipHorizontal = activity.imageScaleX;
                        image.isEdited = true;
                    }
                } else {
                    var toAdd = new ArrayList<ProjectResourceBean>();
                    int i = 0;
                    while (i < activity.pickedImageUris.size()) {
                        var uri = activity.pickedImageUris.get(i);
                        var imageName = Helper.getText(activity.ed_input_edittext).trim() + "_" + ++i;
                        var imageFilePath = UriPathResolver.resolve(activity.getApplicationContext(), uri);
                        if (imageFilePath == null) {
                            return;
                        }
                        var image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                                imageName, imageFilePath);
                        image.savedPos = 1;
                        image.isNew = true;
                        image.rotate = BitmapUtil.getExifRotation(imageFilePath);
                        image.flipVertical = 1;
                        image.flipHorizontal = 1;
                        toAdd.add(image);
                    }
                    if (activity.chk_collection.isChecked()) {
                        SoundCollectionManager.g().a(activity.sc_id, toAdd, true);
                    }
                    activity.multipleImagesPicked = false;
                    activity.images.addAll(toAdd);
                }
            } catch (Exception e) {
                // the bytecode's lying
                // noinspection ConstantValue
                if (e instanceof CompileException compileException) {
                    var errorMessage = compileException.getMessage();
                    var code = switch (errorMessage) {
                        case "fail_to_copy" -> R.string.collection_failed_to_copy;
                        case "file_no_exist" -> R.string.collection_no_exist_file;
                        case "duplicate_name" -> R.string.collection_duplicated_name;
                        default -> 0;
                    };
                    var message = code != 0 ? activity.getString(code) : null;

                    var a = compileException.a();
                    if (a != null && !a.isEmpty()) {
                        var names = "";
                        for (String name : a) {
                            if (!names.isEmpty()) {
                                names += ", ";
                            }
                            names += name;
                        }
                        message += "[" + names + "]";
                    }
                    throw new SketchwareException(message);
                }
                Log.e("AddImageActivity", e.getMessage(), e);
                throw new SketchwareException(e.getMessage());
            }
        }

        @Override
        public void onError(String str) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.h();
        }
    }
}
