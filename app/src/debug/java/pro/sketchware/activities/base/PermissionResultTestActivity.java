package pro.sketchware.activities.base;

import android.os.Bundle;
import android.widget.FrameLayout;

import java.util.ArrayList;

/** Debug-only host for permission callbacks, without opening user projects or changing permission grants. */
public class PermissionResultTestActivity extends BasePermissionAppCompatActivity {
    public final ArrayList<Integer> grantedRequests = new ArrayList<>();
    public final ArrayList<Integer> deniedRequests = new ArrayList<>();
    public String[] callbackPermissions;
    public int[] callbackResults;
    public RuntimeException callbackFailure;
    public Boolean storagePermissionOverride;
    public boolean showRealDialogs;
    public int deniedCancelCount;
    public int settingsCount;
    public RecordingPermissionFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FrameLayout(this));
        fragment = new RecordingPermissionFragment();
        getSupportFragmentManager().beginTransaction().add(fragment, "permission_test").commitNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        callbackPermissions = permissions.clone();
        callbackResults = grantResults.clone();
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } catch (RuntimeException failure) {
            // Record the original callback exception so a pre-fix probe does not crash the user's app process.
            callbackFailure = failure;
        }
    }

    @Override
    public boolean isStoragePermissionGranted() {
        return storagePermissionOverride != null ? storagePermissionOverride : super.isStoragePermissionGranted();
    }

    @Override public void onStoragePermissionGranted(int requestCode) { grantedRequests.add(requestCode); }

    @Override public void showPermissionDeniedDialog(int requestCode) {
        deniedRequests.add(requestCode);
        if (showRealDialogs) super.showPermissionDeniedDialog(requestCode);
    }

    @Override public void onOpenSettings(int requestCode) { settingsCount++; }
    @Override public void onPermissionCancelled() { }
    @Override public void onPermissionDeniedCancelled() { deniedCancelCount++; }

    public void resetCallbacks() {
        grantedRequests.clear();
        deniedRequests.clear();
        callbackPermissions = null;
        callbackResults = null;
        callbackFailure = null;
    }

    public static class RecordingPermissionFragment extends PermissionFragment {
        public final ArrayList<Integer> grantedRequests = new ArrayList<>();
        public final ArrayList<Integer> deniedRequests = new ArrayList<>();
        public String[] callbackPermissions;
        public int[] callbackResults;
        public RuntimeException callbackFailure;
        public Boolean storagePermissionOverride;
        public boolean showRealDialogs;
        public int deniedCancelCount;
        public int settingsCount;

        @Override
        public boolean hasStoragePermission() {
            return storagePermissionOverride != null ? storagePermissionOverride : super.hasStoragePermission();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            callbackPermissions = permissions.clone();
            callbackResults = grantResults.clone();
            try {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } catch (RuntimeException failure) {
                callbackFailure = failure;
            }
        }

        @Override public void onPermissionGranted(int requestCode) { grantedRequests.add(requestCode); }
        @Override public void showSettingsDialog(int requestCode) {
            deniedRequests.add(requestCode);
            if (showRealDialogs) super.showSettingsDialog(requestCode);
        }
        @Override public void openAppSettings(int requestCode) { settingsCount++; }
        @Override public void onPermissionDenied() { }
        @Override public void onSettingsDenied() { deniedCancelCount++; }
        public void resetCallbacks() {
            grantedRequests.clear();
            deniedRequests.clear();
            callbackPermissions = null;
            callbackResults = null;
            callbackFailure = null;
        }
    }
}
