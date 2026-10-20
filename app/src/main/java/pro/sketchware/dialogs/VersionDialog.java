package pro.sketchware.dialogs;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.util.UIHelper;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAdvancedVersionControlBinding;
import pro.sketchware.core.validation.VersionNamePostfixValidator;
import pro.sketchware.core.validation.ProjectVersionCode;

public class VersionDialog {

    /** Receives the confirmed version values when the user saves. */
    public interface OnVersionSavedListener {
        void onVersionSaved(String versionCode, String versionName);
    }

    private final Context context;
    private final DialogAdvancedVersionControlBinding binding;
    private final String initialVersionCode;
    private final String initialVersionName;
    private final OnVersionSavedListener listener;

    public VersionDialog(Context context, String versionCode, String versionName, OnVersionSavedListener listener) {
        this.context = context;
        initialVersionCode = versionCode;
        initialVersionName = versionName;
        this.listener = listener;
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DialogAdvancedVersionControlBinding.inflate(inflater);
    }

    public void show() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setIcon(R.drawable.numbers_48);
        dialog.setTitle(R.string.version_control_title);

        // Preserve invalid imported values so the user can correct or cancel them explicitly.
        binding.versionCode.setText(initialVersionCode);
        VersionNamePostfixValidator postfixValidator =
                new VersionNamePostfixValidator(context, binding.tilVersionNameExtra);
        String[] versionName = (initialVersionName == null ? "" : initialVersionName).split(" ", 2);
        binding.versionName1.setText(versionName[0]);
        binding.versionName2.setText(versionName.length > 1 ? versionName[1] : "");

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), null);
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        AlertDialog shownDialog = dialog.create();
        shownDialog.show();
        shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int verCode = ProjectVersionCode.parse(Helper.getText(binding.versionCode));
            String verName = Helper.getText(binding.versionName1).trim();
            String verNamePostfix = Helper.getText(binding.versionName2);

            boolean validVerCode = verCode != 0;
            boolean validVerName = !TextUtils.isEmpty(verName);

            if (validVerCode) {
                binding.tilVersionCode.setError(null);
            } else {
                binding.tilVersionCode.setError(Helper.getResString(R.string.version_error_invalid_code));
            }

            if (validVerName) {
                binding.tilVersionName.setError(null);
            } else {
                binding.tilVersionName.setError(Helper.getResString(R.string.version_error_invalid_name));
            }

            if (validVerCode && validVerName && postfixValidator.isValid() && !UIHelper.isClickThrottled()) {
                listener.onVersionSaved(Integer.toString(verCode),
                        !verNamePostfix.isEmpty() ? verName + " " + verNamePostfix : verName);
                shownDialog.dismiss();
            }
        });
    }
}
