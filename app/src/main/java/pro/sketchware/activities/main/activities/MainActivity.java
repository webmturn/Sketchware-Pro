package pro.sketchware.activities.main.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.besome.sketch.lib.base.BasePermissionAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import pro.sketchware.core.SharedPrefsHelper;
import pro.sketchware.core.DeviceUtil;
import mod.hey.studios.project.backup.BackupFactory;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.tyron.backup.SingleCopyTask;
import pro.sketchware.R;
import pro.sketchware.activities.about.AboutActivity;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.activities.main.fragments.projects_store.ProjectsStoreFragment;
import pro.sketchware.databinding.MainBinding;
import pro.sketchware.lib.base.BottomSheetDialogView;
import pro.sketchware.utility.DataResetter;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class MainActivity extends BasePermissionAppCompatActivity {
    private ActivityResultLauncher<Intent> programInfoLauncher;
    private static final String PROJECTS_FRAGMENT_TAG = "projects_fragment";
    private static final String PROJECTS_STORE_FRAGMENT_TAG = "projects_store_fragment";
    private ActionBarDrawerToggle drawerToggle;
    private SharedPrefsHelper sharedPrefs;
    private Snackbar storageAccessDenied;
    private MainBinding binding;
    private final OnBackPressedCallback closeDrawer = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            binding.drawerLayout.closeDrawers();
        }
    };
    private ProjectsFragment projectsFragment;
    private ProjectsStoreFragment projectsStoreFragment;
    private Fragment activeFragment;
    @IdRes
    private int currentNavItemId = R.id.item_projects;

    private static boolean isFirebaseInitialized(Context context) {
        try {
            return FirebaseApp.getApps(context) != null && !FirebaseApp.getApps(context).isEmpty();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    // onRequestPermissionsResult but for Storage access only, and only when granted
    public void onStoragePermissionGranted(int i) {
        if (i == 9501) {
            allFilesAccessCheck();

            if (activeFragment instanceof ProjectsFragment) {
                projectsFragment.refreshProjectsList();
            }
        }
    }

    @Override
    public void onOpenSettings(int i) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        startActivity(intent);
    }

    @Override
    public void onPermissionCancelled() {
    }

    @Override
    public void onPermissionDeniedCancelled() {
    }

    public void refreshProjects() {
        if (activeFragment instanceof ProjectsFragment) {
            projectsFragment.refreshProjectsList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 111:
                    invalidateOptionsMenu();
                    break;

                case 113:
                    if (data != null && data.getBooleanExtra("not_show_popup_anymore", false)) {
                        sharedPrefs.put("U1I2", (Object) false);
                    }
                    break;

                case 212:
                    if (!(data.getStringExtra("save_as_new_id") == null ? "" : data.getStringExtra("save_as_new_id")).isEmpty() && isStoragePermissionGranted()) {
                        if (activeFragment instanceof ProjectsFragment) {
                            projectsFragment.refreshProjectsList();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        programInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        DataResetter.resetData(this, result.getData().getBooleanExtra("onlyConfig", true));
                    }
                });
        enableEdgeToEdgeNoContrast();

        binding = MainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        binding.statusBarOverlapper.setMinimumHeight(UI.getStatusBarHeight(this));
        UI.addSystemWindowInsetToPadding(binding.appbar, true, false, true, false);

        sharedPrefs = new SharedPrefsHelper(getApplicationContext(), "U1");
        int u1I0 = sharedPrefs.getInt("U1I0", -1);
        long u1I1 = sharedPrefs.getLong("U1I1");
        if (u1I1 <= 0) {
            sharedPrefs.put("U1I1", System.currentTimeMillis());
        }
        if (System.currentTimeMillis() - u1I1 > /* (a day) */ 1000 * 60 * 60 * 24) {
            sharedPrefs.put("U1I0", Integer.valueOf(u1I0 + 1));
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        drawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, R.string.app_name, R.string.app_name);
        binding.drawerLayout.addDrawerListener(drawerToggle);
        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawer.setEnabled(true);
                getOnBackPressedDispatcher().addCallback(closeDrawer);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        boolean hasStorageAccess = isStoragePermissionGranted();
        if (!hasStorageAccess) {
            showNoticeNeedStorageAccess();
        }
        if (hasStorageAccess) {
            allFilesAccessCheck();
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data != null) {
                new SingleCopyTask(this, new SingleCopyTask.CallBackTask() {
                    @Override
                    public void onCopyPreExecute() {
                    }

                    @Override
                    public void onCopyProgressUpdate(int progress) {
                    }

                    @Override
                    public void onCopyPostExecute(@NonNull String path, boolean wasSuccessful, @NonNull String reason) {
                        if (wasSuccessful) {
                            BackupRestoreManager manager = new BackupRestoreManager(MainActivity.this, projectsFragment);

                            if (BackupFactory.zipContainsFile(path, "local_libs")) {
                                new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle(R.string.common_word_warning)
                                        .setMessage(BackupRestoreManager.getRestoreIntegratedLocalLibrariesMessage(false, -1, -1, null))
                                        .setPositiveButton(R.string.common_word_copy, (dialog, which) -> manager.doRestore(path, true))
                                        .setNegativeButton(R.string.common_word_dont_copy, (dialog, which) -> manager.doRestore(path, false))
                                        .setNeutralButton(R.string.common_word_cancel, null)
                                        .show();
                            } else {
                                manager.doRestore(path, true);
                            }

                            // Clear intent so it doesn't duplicate
                            getIntent().setData(null);
                        } else {
                            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_copy_backup), reason), Toast.LENGTH_LONG);
                        }
                    }
                }).copyFile(data);
            }
        } else if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_CRITICAL_UPDATE_REMINDER)) {
            BottomSheetDialogView bottomSheetDialog = getBottomSheetDialogView();
            bottomSheetDialog.getPositiveButton().setEnabled(false);

            CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    bottomSheetDialog.setPositiveButtonText(millisUntilFinished / 1000 + "");
                }

                @Override
                public void onFinish() {
                    bottomSheetDialog.setPositiveButtonText("View changes");
                    bottomSheetDialog.getPositiveButton().setEnabled(true);
                }
            };
            countDownTimer.start();

            if (!isFinishing()) bottomSheetDialog.show();
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_projects) {
                navigateToProjectsFragment();
                return true;
            } else if (id == R.id.item_sketchub) {
                navigateToSketchubFragment();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            projectsFragment = (ProjectsFragment) getSupportFragmentManager().findFragmentByTag(PROJECTS_FRAGMENT_TAG);
            projectsStoreFragment = (ProjectsStoreFragment) getSupportFragmentManager().findFragmentByTag(PROJECTS_STORE_FRAGMENT_TAG);
            currentNavItemId = savedInstanceState.getInt("selected_tab_id");
            Fragment current = getFragmentForNavId(currentNavItemId);
            if (current instanceof ProjectsFragment) {
                navigateToProjectsFragment();
            } else if (current instanceof ProjectsStoreFragment) {
                navigateToSketchubFragment();
            }

            return;
        }

        navigateToProjectsFragment();
    }

    private Fragment getFragmentForNavId(int navItemId) {
        if (navItemId == R.id.item_projects) {
            return projectsFragment;
        } else if (navItemId == R.id.item_sketchub) {
            return projectsStoreFragment;
        }
        throw new IllegalArgumentException();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_tab_id", currentNavItemId);
    }

    private void navigateToProjectsFragment() {
        if (projectsFragment == null) {
            projectsFragment = new ProjectsFragment();
        }

        boolean shouldShow = true;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        binding.createNewProject.show();
        if (activeFragment != null) transaction.hide(activeFragment);
        if (fm.findFragmentByTag(PROJECTS_FRAGMENT_TAG) == null) {
            shouldShow = false;
            transaction.add(binding.container.getId(), projectsFragment, PROJECTS_FRAGMENT_TAG);
        }
        if (shouldShow) transaction.show(projectsFragment);
        transaction.commit();

        activeFragment = projectsFragment;
        currentNavItemId = R.id.item_projects;
    }

    private void navigateToSketchubFragment() {
        if (projectsStoreFragment == null) {
            projectsStoreFragment = new ProjectsStoreFragment();
        }

        boolean shouldShow = true;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        binding.createNewProject.hide();
        if (activeFragment != null) transaction.hide(activeFragment);
        if (fm.findFragmentByTag(PROJECTS_STORE_FRAGMENT_TAG) == null) {
            shouldShow = false;
            transaction.add(binding.container.getId(), projectsStoreFragment, PROJECTS_STORE_FRAGMENT_TAG);
        }
        if (shouldShow) transaction.show(projectsStoreFragment);
        transaction.commit();

        activeFragment = projectsStoreFragment;
        currentNavItemId = R.id.item_sketchub;
    }

    @NonNull
    private BottomSheetDialogView getBottomSheetDialogView() {
        BottomSheetDialogView bottomSheetDialog = new BottomSheetDialogView(this);
        bottomSheetDialog.setTitle(Helper.getResString(R.string.main_major_changes_title));
        bottomSheetDialog.setDescription(Helper.getResString(R.string.main_major_changes_desc));

        bottomSheetDialog.setPositiveButton(Helper.getResString(R.string.common_word_view_changes), (dialog, which) -> {
            ConfigActivity.setSetting(ConfigActivity.SETTING_CRITICAL_UPDATE_REMINDER, true);
            Intent launcher = new Intent(this, AboutActivity.class);
            launcher.putExtra("select", "changelog");
            startActivity(launcher);
        });
        bottomSheetDialog.setCancelable(false);
        return bottomSheetDialog;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        if (isFirebaseInitialized(this)) {
            FirebaseMessaging.getInstance().subscribeToTopic("all");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        /* Check if the device is running low on storage space */
        long freeMegabytes = DeviceUtil.getFreeStorageMB();
        if (freeMegabytes < 100 && freeMegabytes > 0) {
            showNoticeNotEnoughFreeStorageSpace();
        }
        if (isStoragePermissionGranted() && storageAccessDenied != null && storageAccessDenied.isShown()) {
            storageAccessDenied.dismiss();
        }
        if (isFirebaseInitialized(this)) {
            if (mAnalytics == null) {
                mAnalytics = FirebaseAnalytics.getInstance(this);
            }
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity");
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");
            mAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
        }
    }

    private void allFilesAccessCheck() {
        if (Build.VERSION.SDK_INT > 29) {
            File optOutFile = new File(getFilesDir(), ".skip_all_files_access_notice");
            boolean granted = Environment.isExternalStorageManager();

            if (!optOutFile.exists() && !granted) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
                dialog.setIcon(R.drawable.ic_expire_48dp);
                dialog.setTitle(R.string.main_storage_access_title);
                dialog.setMessage(Helper.getResString(R.string.main_storage_access_message));
                dialog.setPositiveButton(Helper.getResString(R.string.common_word_settings), (v, which) -> {
                    FileUtil.requestAllFilesAccessPermission(this);
                    v.dismiss();
                });
                dialog.setNegativeButton(R.string.common_word_skip, null);
                dialog.setNeutralButton(R.string.common_word_dont_show_anymore, (v, which) -> {
                    try {
                        if (!optOutFile.createNewFile())
                            throw new IOException("Failed to create file " + optOutFile);
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error while trying to create " + "\"Don't show Android 11 hint\" dialog file: " + e.getMessage(), e);
                    }
                    v.dismiss();
                });
                dialog.show();
            }
        }
    }

    private void showNoticeNeedStorageAccess() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_message_permission_title_storage));
        dialog.setIcon(R.drawable.color_about_96);
        dialog.setMessage(Helper.getResString(R.string.common_message_permission_need_load_project));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), (v, which) -> {
            v.dismiss();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 9501);
        });
        dialog.show();
    }

    private void showNoticeNotEnoughFreeStorageSpace() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_message_insufficient_storage_space_title));
        dialog.setIcon(R.drawable.high_priority_96_red);
        dialog.setMessage(Helper.getResString(R.string.common_message_insufficient_storage_space));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    public void showStorageAccessDeniedSnackbar() {
        if (storageAccessDenied == null || !storageAccessDenied.isShown()) {
            storageAccessDenied = Snackbar.make(binding.layoutCoordinator, Helper.getResString(R.string.common_message_permission_denied), Snackbar.LENGTH_INDEFINITE);
            storageAccessDenied.setAction(Helper.getResString(R.string.common_word_settings), v -> {
                storageAccessDenied.dismiss();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 9501);
            });
            storageAccessDenied.setActionTextColor(Color.YELLOW);
            storageAccessDenied.show();
        }
    }

    public void launchProgramInfo(Intent intent) {
        programInfoLauncher.launch(intent);
    }
}
