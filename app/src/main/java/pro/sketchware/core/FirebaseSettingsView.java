package pro.sketchware.core;

import android.content.Context;
import android.widget.LinearLayout;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.firebase.FirebaseActivity;

import pro.sketchware.R;
import pro.sketchware.databinding.ManageLibraryFirebaseProjectSettingsBinding;

public class FirebaseSettingsView extends LinearLayout implements LibraryConfigView {

    private final ManageLibraryFirebaseProjectSettingsBinding binding;

    public FirebaseSettingsView(FirebaseActivity activity) {
        super(activity);
        binding = ManageLibraryFirebaseProjectSettingsBinding.inflate(activity.getLayoutInflater(), this, true);
        initialize(activity);
    }

    @Override
    public void onSave() {
    }

    private void initialize(Context context) {
        AnimationUtil.fadeSlideIn(this, 600, 200, null);
        binding.tvTitleProjectId.setText(R.string.design_library_firebase_title_project_id);
        binding.tvTitleAppId.setText(R.string.design_library_firebase_title_app_id);
        binding.tvTitleApiKey.setText(R.string.design_library_firebase_title_api_key);
    }

    @Override
    public void saveToBean(ProjectLibraryBean libraryBean) {
        libraryBean.data = binding.edInputProjectId.getText().toString().trim();
        libraryBean.reserved1 = binding.edInputAppId.getText().toString().trim();
        libraryBean.reserved2 = binding.edInputApiKey.getText().toString().trim();
    }

    public void showInputRequiredToast() {
        SketchToast.toast(getContext(), getContext().getString(R.string.design_library_firebase_message_not_input_text), 1).show();
    }

    @Override
    public String getDocUrl() {
        return "https://docs.sketchware.io/docs/firebase-project-settings.html";
    }

    @Override
    public boolean isValid() {
        if (binding.edInputProjectId.getText().toString().trim().length() == 0) {
            binding.edInputProjectId.requestFocus();
            showInputRequiredToast();
            return false;
        } else if (binding.edInputAppId.getText().toString().trim().length() == 0) {
            binding.edInputAppId.requestFocus();
            showInputRequiredToast();
            return false;
        } else if (binding.edInputApiKey.getText().toString().trim().length() == 0) {
            binding.edInputApiKey.requestFocus();
            showInputRequiredToast();
            return false;
        } else {
            return true;
        }
    }

    public void setData(ProjectLibraryBean libraryBean) {
        String data = libraryBean.data;
        if (data != null && data.length() > 0) {
            binding.edInputProjectId.setText(data);
        }

        String reserved1 = libraryBean.reserved1;
        if (reserved1 != null && reserved1.length() > 0) {
            binding.edInputAppId.setText(reserved1);
        }

        String reserved2 = libraryBean.reserved2;
        if (reserved2 != null && reserved2.length() > 0) {
            binding.edInputApiKey.setText(reserved2);
        }
    }
}
