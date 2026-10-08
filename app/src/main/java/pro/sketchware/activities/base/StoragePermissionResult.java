package pro.sketchware.activities.base;

import android.Manifest;
import android.content.pm.PackageManager;

/** Matches each reported storage permission to its own result; current full access is checked by the caller. */
final class StoragePermissionResult {
    private StoragePermissionResult() { }

    static boolean containsStoragePermission(String[] permissions) {
        for (String permission : permissions) {
            if (isStoragePermission(permission)) return true;
        }
        return false;
    }

    static boolean allReportedStoragePermissionsGranted(String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (isStoragePermission(permissions[i])
                    && (i >= grantResults.length || grantResults[i] != PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStoragePermission(String permission) {
        return Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission);
    }
}
