package pro.sketchware.activities.main.activities;

/** Exercises the first-run UI without revoking any of the device's permission grants. */
public class StartupPromptTestActivity extends MainActivity {
    public boolean storageGranted;
    @Override public boolean isStoragePermissionGranted() { return storageGranted; }
}
