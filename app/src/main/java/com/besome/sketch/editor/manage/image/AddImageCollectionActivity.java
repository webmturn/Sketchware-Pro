package com.besome.sketch.editor.manage.image;

import android.util.Log;
import android.content.ActivityNotFoundException;
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
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.BlockConstants;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.CompileException;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class AddImageCollectionActivity extends BaseDialogActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private TextView tv_add_photo;
    private ImageView preview;
    private FileNameValidator imageNameValidator;
    private EditText ed_input_edittext;
    private EasyDeleteEditText ed_input;
    private ImageView tv_desc;
    private CheckBox chk_collection;
    private String sc_id;
    private ArrayList<ProjectResourceBean> images;
    private LinearLayout layout_img_inform = null;
    private LinearLayout layout_img_modify = null;
    private TextView tv_imgcnt = null;
    private boolean z = false;
    private String imageFilePath = null;
    private int imageRotationDegrees = 0;
    private int imageExifOrientation = 0;
    private int imageScaleY = 1;
    private int imageScaleX = 1;
    private boolean editing = false;
    private ProjectResourceBean editTarget = null;

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
    public void onClick(View v) {
        if (!UIHelper.isClickThrottled()) {
            int id = v.getId();
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
                if (!editing) {
                    pickImage();
                }
            } else if (id == R.id.img_vertical) {
                flipImageVertically();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (preview != null) {
                        preview.setEnabled(true);
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            imageRotationDegrees = 0;
                            imageScaleY = 1;
                            imageScaleX = 1;
                            z = true;
                            setImageFromUri(result.getData().getData());
                            if (imageNameValidator != null) {
                                imageNameValidator.setBatchCount(1);
                            }
                        }
                    }
                });
        e(getString(R.string.design_manager_image_title_add_image));
        d(getString(R.string.common_word_save));
        setContentView(R.layout.manage_image_add);
        Intent intent = getIntent();
        images = intent.getParcelableArrayListExtra("images");
        sc_id = intent.getStringExtra("sc_id");
        editTarget = intent.getParcelableExtra("edit_target");
        if (editTarget != null) {
            editing = true;
        }
        layout_img_inform = findViewById(R.id.layout_img_inform);
        layout_img_modify = findViewById(R.id.layout_img_modify);
        chk_collection = findViewById(R.id.chk_collection);
        chk_collection.setVisibility(View.GONE);
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
        imageNameValidator = new FileNameValidator(this, ed_input.getTextInputLayout(), BlockConstants.RESERVED_KEYWORDS, getReservedImageNames());
        imageNameValidator.setBatchCount(1);
        tv_add_photo.setText(R.string.design_manager_image_title_add_image);
        preview.setOnClickListener(this);
        img_rotate.setOnClickListener(this);
        img_vertical.setOnClickListener(this);
        img_horizontal.setOnClickListener(this);
        dialogOkButton.setOnClickListener(this);
        dialogCancelButton.setOnClickListener(this);
        z = false;
        imageRotationDegrees = 0;
        imageScaleY = 1;
        imageScaleX = 1;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (editing) {
            editTarget.isEdited = true;
            e(getString(R.string.design_manager_image_title_edit_image_name));
            imageNameValidator = new FileNameValidator(this, ed_input.getTextInputLayout(), BlockConstants.RESERVED_KEYWORDS, getReservedImageNames(), editTarget.resName);
            imageNameValidator.setBatchCount(1);
            ed_input_edittext.setText(editTarget.resName);
            chk_collection.setVisibility(View.GONE);
            tv_add_photo.setVisibility(View.GONE);
            setImageFromFile(a(editTarget));
            layout_img_modify.setVisibility(View.GONE);
        }
    }

    private ArrayList<String> getReservedImageNames() {
        ArrayList<String> names = new ArrayList<>();
        names.add("app_icon");
        for (ProjectResourceBean image : images) {
            names.add(image.resName);
        }
        return names;
    }

    private void pickImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.common_word_choose)));
        } catch (ActivityNotFoundException unused) {
            SketchToast.warning(this, getString(R.string.common_error_activity_not_found), SketchToast.TOAST_NORMAL).show();
        }
    }

    private void refreshPreview() {
        preview.setImageBitmap(BitmapUtil.scaleAndRotateBitmap(BitmapUtil.rotateBitmap(BitmapUtil.decodeSampledBitmap(imageFilePath, 1024, 1024), imageExifOrientation), imageRotationDegrees, imageScaleX, imageScaleY));
    }

    private void save() {
        if (a(imageNameValidator)) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showLoadingDialog();
                new SaveAsyncTask(this).execute();
            }, 500L);
        }
    }

    private void t() {
        if (tv_desc != null) {
            tv_desc.setVisibility(View.INVISIBLE);
        }
        if (layout_img_inform != null && layout_img_modify != null && tv_imgcnt != null) {
            layout_img_inform.setVisibility(View.GONE);
            layout_img_modify.setVisibility(View.VISIBLE);
            tv_imgcnt.setVisibility(View.GONE);
        }
    }

    private boolean a(FileNameValidator validator) {
        if (!validator.isValid()) {
            return false;
        }
        if (z || imageFilePath != null) {
            return true;
        }
        tv_desc.startAnimation(AnimationUtils.loadAnimation(this, R.anim.ani_1));
        return false;
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
            Log.e("AddImageCollectionActivity", e.getMessage(), e);
        }
        t();
    }

    private void setImageRotation(int degrees) {
        if (imageFilePath != null && !imageFilePath.isEmpty()) {
            imageRotationDegrees = degrees;
            if (imageRotationDegrees == 360) {
                imageRotationDegrees = 0;
            }
            refreshPreview();
        }
    }

    private void setImageFromUri(Uri uri) {
        String filePath;
        if (uri != null && (filePath = UriPathResolver.resolve(this, uri)) != null) {
            setImageFromFile(filePath);
        }
    }

    private String a(ProjectResourceBean projectResourceBean) {
        return SketchwarePaths.getCollectionPath() + File.separator + "image" + File.separator + "data" + File.separator + projectResourceBean.resFullName;
    }

    private static class SaveAsyncTask extends BaseAsyncTask {
        private final WeakReference<AddImageCollectionActivity> activity;

        public SaveAsyncTask(AddImageCollectionActivity activity) {
            super(activity.getApplicationContext());
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            SketchToast.toast(activity.getApplicationContext(), activity.getString(
                    activity.editing ? R.string.design_manager_message_edit_complete :
                            R.string.design_manager_message_add_complete), SketchToast.TOAST_NORMAL).show();
            activity.dismissLoadingDialog();
            activity.finish();
        }

        @Override
        public void doWork() throws SketchwareException {
            var activity = this.activity.get();
            if (activity == null) return;
            try {
                publishProgress("Now processing..");
                if (!activity.editing) {
                    var image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                            Helper.getText(activity.ed_input_edittext).trim(), activity.imageFilePath);
                    image.savedPos = 1;
                    image.isNew = true;
                    image.rotate = activity.imageRotationDegrees;
                    image.flipVertical = activity.imageScaleY;
                    image.flipHorizontal = activity.imageScaleX;
                    SoundCollectionManager.getInstance().addResource(activity.sc_id, image);
                } else {
                    SoundCollectionManager.getInstance().renameResource(activity.editTarget, Helper.getText(activity.ed_input_edittext), false);
                }
            } catch (Exception e) {
                // the bytecode's lying
                // noinspection ConstantValue
                if (e instanceof CompileException compileException) {
                    var messageId = switch (compileException.getMessage()) {
                        case "fail_to_copy" -> R.string.collection_failed_to_copy;
                        case "file_no_exist" -> R.string.collection_no_exist_file;
                        case "duplicate_name" -> R.string.collection_duplicated_name;
                        default -> 0;
                    };
                    var message = messageId != 0 ? activity.getString(messageId) : "";

                    var a = compileException.getErrorDetails();
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
                Log.e("AddImageCollectionActivity", e.getMessage(), e);
                throw new SketchwareException(e.getMessage());
            }
        }

        @Override
        public void onError(String str) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
        }
    }
}
