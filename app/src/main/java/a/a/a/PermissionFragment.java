package a.a.a;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.R;

public abstract class PermissionFragment extends BaseFragment {
    public PermissionFragment() {
    }

    public boolean checkPermissionOrRequest(int var1) {
        boolean var2 = hasStoragePermission();
        if (!var2) {
            showPermissionDialog(var1);
        }

        return var2;
    }

    public abstract void onPermissionGranted(int var1);

    public abstract void openAppSettings(int var1);

    public boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(requireContext(), "android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
    }

    public abstract void onPermissionDenied();

    public void showPermissionDialog(int var1) {
        if (!ThrottleTimer.a) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(super.a);
            dialog.setTitle(R.string.common_message_permission_title_storage);
            dialog.setIcon(R.drawable.break_warning_96_red);
            dialog.setMessage(R.string.common_message_permission_storage);
            dialog.setPositiveButton(R.string.common_word_ok, (view, which) -> {
                if (!UIHelper.a()) {
                    requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
                            "android.permission.READ_EXTERNAL_STORAGE"}, var1);
                    view.dismiss();
                }
            });
            dialog.setNegativeButton(R.string.common_word_cancel, (view, which) -> {
                onPermissionDenied();
                view.dismiss();
            });
            dialog.setOnDismissListener(dialog1 -> ThrottleTimer.a = false);
            dialog.setCancelable(false);
            dialog.create().setCanceledOnTouchOutside(false);
            dialog.show();
            ThrottleTimer.a = true;
        }
    }

    public abstract void onSettingsDenied();

    public void showSettingsDialog(int var1) {
        if (!ThrottleTimer.a) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(super.a);
            builder.setTitle(R.string.common_message_permission_title_storage);
            builder.setIcon(R.drawable.break_warning_96_red);
            builder.setMessage(R.string.common_message_permission_storage1);
            builder.setPositiveButton(R.string.common_word_settings, (view, which) -> {
                if (!UIHelper.a()) {
                    openAppSettings(var1);
                    view.dismiss();
                }
            });
            builder.setNegativeButton(R.string.common_word_cancel, (view, which) -> {
                onSettingsDenied();
                view.dismiss();
            });
            builder.setOnDismissListener(dialog1 -> ThrottleTimer.a = false);
            builder.setCancelable(false);

            var dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            ThrottleTimer.a = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        for (String permission : permissions) {
            if ("android.permission.WRITE_EXTERNAL_STORAGE".equals(permission)) {
                if (grantResults.length == 0 || grantResults[0] != 0 || grantResults[1] != 0) {
                    showSettingsDialog(requestCode);
                    break;
                }
                onPermissionGranted(requestCode);
            }
        }

    }
}
