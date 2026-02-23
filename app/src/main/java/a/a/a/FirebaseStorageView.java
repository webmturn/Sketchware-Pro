package a.a.a;

import android.widget.LinearLayout;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.editor.manage.library.firebase.FirebaseActivity;

import pro.sketchware.R;
import pro.sketchware.databinding.ManageLibraryFirebaseStorageUrlSettingsBinding;

public class FirebaseStorageView extends LinearLayout implements LibraryConfigView {

    private final ManageLibraryFirebaseStorageUrlSettingsBinding binding;

    public FirebaseStorageView(FirebaseActivity firebaseActivity) {
        super(firebaseActivity);
        binding = ManageLibraryFirebaseStorageUrlSettingsBinding.inflate(firebaseActivity.getLayoutInflater(), this, true);

        AnimationUtil.fadeSlideIn(this, 600, 200, null);
        binding.tvTitleStorageUrl.setText(R.string.design_library_firebase_title_storage_bucket_url);
    }

    @Override
    public void a() {
        UIHelper.a(getContext(), binding.edInputStorageUrl);
    }

    @Override
    public void a(ProjectLibraryBean libraryBean) {
        String var2 = binding.edInputStorageUrl.getText().toString().trim();
        if (!var2.isEmpty()) {
            String cleaned = var2;
            if (cleaned.startsWith("gs://")) {
                cleaned = cleaned.substring(5);
            }
            if (cleaned.endsWith("/")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            libraryBean.reserved3 = cleaned;
        }

    }

    //todo: Update docs url
    @Override
    public String getDocUrl() {
        return "https://docs.sketchware.io/docs/firebase-storage.html";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public void setData(ProjectLibraryBean libraryBean) {
        String reserved3 = libraryBean.reserved3;
        if (reserved3 != null && reserved3.length() > 0) {
            binding.edInputStorageUrl.setText(libraryBean.reserved3);
        }
    }
}
