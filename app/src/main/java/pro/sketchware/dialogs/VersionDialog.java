package pro.sketchware.dialogs;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.util.UIHelper;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogAdvancedVersionControlBinding;
import pro.sketchware.core.validation.VersionNamePostfixValidator;

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

        try {
            binding.versionCode.setText(String.valueOf(Integer.parseInt(initialVersionCode)));
        } catch (NumberFormatException e) {
            binding.versionCode.setText(initialVersionCode);
        }
        binding.versionName1.setText(initialVersionName.split(" ")[0]);
        if (initialVersionName.split(" ").length > 1)
            binding.versionName2.setText(initialVersionName.split(" ")[1]);

        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (v, which) -> {
            String verCode = Helper.getText(binding.versionCode);
            String verName = Helper.getText(binding.versionName1);
            String verNamePostfix = Helper.getText(binding.versionName2);

            boolean validVerCode = !TextUtils.isEmpty(verCode);
            boolean validVerName = !TextUtils.isEmpty(verName);

            if (validVerCode) {
                binding.versionCode.setError(null);
            } else {
                binding.versionCode.setError(Helper.getResString(R.string.version_error_invalid_code));
            }

            if (validVerName) {
                binding.versionName1.setError(null);
            } else {
                binding.versionName1.setError(Helper.getResString(R.string.version_error_invalid_name));
            }

            if (!UIHelper.isClickThrottled() && validVerCode && validVerName) {
                listener.onVersionSaved(verCode, !verNamePostfix.isEmpty() ? verName + " " + verNamePostfix : verName);
                v.dismiss();
            }
        });

        binding.versionName2.addTextChangedListener(new VersionNamePostfixValidator(context, binding.tilVersionNameExtra));
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }
}
