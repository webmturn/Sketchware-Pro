package pro.sketchware.core;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import pro.sketchware.R;

public abstract class PermissionFragment extends BaseFragment {
    public PermissionFragment() {
    }

    public boolean checkPermissionOrRequest(int requestCode) {
        boolean hasPermission = hasStoragePermission();
        if (!hasPermission) {
            showPermissionDialog(requestCode);
        }

        return hasPermission;
    }

    public abstract void onPermissionGranted(int requestCode);

    public abstract void openAppSettings(int requestCode);

    public boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(requireContext(), "android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), "android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
    }

    public abstract void onPermissionDenied();

    public void showPermissionDialog(int requestCode) {
        if (!ThrottleTimer.isThrottled) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(super.activity);
            dialog.setTitle(R.string.common_message_permission_title_storage);
            dialog.setIcon(R.drawable.break_warning_96_red);
            dialog.setMessage(R.string.common_message_permission_storage);
            dialog.setPositiveButton(R.string.common_word_ok, (view, which) -> {
                if (!UIHelper.isClickThrottled()) {
                    requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
                            "android.permission.READ_EXTERNAL_STORAGE"}, requestCode);
                    view.dismiss();
                }
            });
            dialog.setNegativeButton(R.string.common_word_cancel, (view, which) -> {
                onPermissionDenied();
                view.dismiss();
            });
            dialog.setOnDismissListener(dialog1 -> ThrottleTimer.isThrottled = false);
            dialog.setCancelable(false);
            dialog.create().setCanceledOnTouchOutside(false);
            dialog.show();
            ThrottleTimer.isThrottled = true;
        }
    }

    public abstract void onSettingsDenied();

    public void showSettingsDialog(int requestCode) {
        if (!ThrottleTimer.isThrottled) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(super.activity);
            builder.setTitle(R.string.common_message_permission_title_storage);
            builder.setIcon(R.drawable.break_warning_96_red);
            builder.setMessage(R.string.common_message_permission_storage1);
            builder.setPositiveButton(R.string.common_word_settings, (view, which) -> {
                if (!UIHelper.isClickThrottled()) {
                    openAppSettings(requestCode);
                    view.dismiss();
                }
            });
            builder.setNegativeButton(R.string.common_word_cancel, (view, which) -> {
                onSettingsDenied();
                view.dismiss();
            });
            builder.setOnDismissListener(dialog1 -> ThrottleTimer.isThrottled = false);
            builder.setCancelable(false);

            var dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            ThrottleTimer.isThrottled = true;
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
