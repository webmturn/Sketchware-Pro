package com.besome.sketch.design;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.besome.sketch.adapters.JavaFileAdapter;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.common.SrcViewerActivity;
import com.besome.sketch.editor.manage.ManageCollectionActivity;
import com.besome.sketch.editor.manage.ViewSelectorActivity;
import com.besome.sketch.editor.manage.font.ManageFontActivity;
import com.besome.sketch.editor.manage.image.ManageImageActivity;
import com.besome.sketch.editor.manage.library.ManageLibraryActivity;
import com.besome.sketch.editor.manage.sound.ManageSoundActivity;
import com.besome.sketch.editor.manage.view.ManageViewActivity;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.CustomViewPager;
import com.besome.sketch.tools.CompileLogActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import pro.sketchware.core.SharedPrefsHelper;
import pro.sketchware.core.DeviceUtil;
import pro.sketchware.core.LayoutGenerator;
import pro.sketchware.core.ProjectBuilder;
import pro.sketchware.core.ViewEditorFragment;
import pro.sketchware.core.SketchToast;
import pro.sketchware.core.BlockHistoryManager;
import pro.sketchware.core.ComponentListFragment;
import pro.sketchware.core.ViewHistoryManager;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.ResourceManager;
import pro.sketchware.core.ProjectListManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.EventListFragment;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.MapValueHelper;
import pro.sketchware.core.ProjectFilePaths;
import pro.sketchware.core.SimpleException;
import dev.chrisbanes.insetter.Insetter;
import mod.agus.jcoderz.editor.manage.permission.ManagePermissionActivity;
import mod.agus.jcoderz.editor.manage.resource.ManageResourceActivity;
import mod.hey.studios.activity.managers.assets.ManageAssetsActivity;
import mod.hey.studios.activity.managers.java.ManageJavaActivity;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.hey.studios.project.custom_blocks.CustomBlocksDialog;
import mod.hey.studios.project.proguard.ManageProguardActivity;
import mod.hey.studios.project.proguard.ProguardHandler;
import mod.hey.studios.project.stringfog.ManageStringFogFragment;
import mod.hey.studios.project.stringfog.StringfogHandler;
import mod.hey.studios.util.Helper;
import mod.hey.studios.util.SystemLogPrinter;
import mod.hilal.saif.activities.android_manifest.AndroidManifestInjection;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.diagnostic.CompileErrorSaver;
import mod.jbk.diagnostic.MissingFileException;
import mod.jbk.util.LogUtil;
import mod.khaled.logcat.LogReaderActivity;
import pro.sketchware.R;
import pro.sketchware.activities.appcompat.ManageAppCompatActivity;
import pro.sketchware.activities.editor.command.ManageXMLCommandActivity;
import pro.sketchware.activities.editor.view.CodeViewerActivity;
import pro.sketchware.activities.editor.view.ViewCodeEditorActivity;
import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.dialogs.BuildSettingsBottomSheet;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;
import pro.sketchware.utility.apk.ApkSignatures;

public class DesignActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    public static String sc_id;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
    private ImageView xmlLayoutOrientation;
    private boolean isRestoringData;
    private int currentTabNumber;
    private CustomViewPager viewPager;
    private CoordinatorLayout coordinatorLayout;
    private DrawerLayout drawer;
    private ProjectFilePaths projectFilePaths;
    private SharedPrefsHelper prefP1;
    private SharedPrefsHelper prefP12;
    private Menu bottomMenu;
    private PopupMenu bottomPopupMenu;
    private MaterialButton btnRun;
    private MaterialButton btnOptions;
    private ProjectFileBean projectFile;
    private TextView fileName;
    private String currentJavaFileName;
    private ViewEditorFragment viewTabAdapter;
    private final ActivityResultLauncher<Intent> openCollectionManager = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (viewTabAdapter != null) {
                viewTabAdapter.refreshFavorites();
            }
        }
    });
    private final ActivityResultLauncher<Intent> openResourcesManager = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (viewTabAdapter != null && viewPager.getCurrentItem() == 0) {
                viewTabAdapter.refreshAllViews();
                refreshViewTabAdapter();
            }
        }
    });
    private final ActivityResultLauncher<Intent> openViewCodeEditor = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            if (viewTabAdapter != null) {
                viewTabAdapter.refreshAllViews();
            }
        }
    });
    private EventListFragment eventTabAdapter;
    private ComponentListFragment componentTabAdapter;
    private final ActivityResultLauncher<Intent> openImageManager = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            refresh();
        }
    });
    public final ActivityResultLauncher<Intent> changeOpenFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            var data = result.getData();
            if (data == null) return;
            projectFile = data.getParcelableExtra("project_file");
            refresh();
        }
    });
    private final ActivityResultLauncher<Intent> openLibraryManager = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            refresh();
            if (viewTabAdapter != null) {
                viewTabAdapter.updatePropertyViews();
            }
        }
    });
    private final ActivityResultLauncher<Intent> openViewManager = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            refresh();
        }
    });
    private BuildTask currentBuildTask;
    private final BroadcastReceiver buildCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BuildTask.ACTION_CANCEL_BUILD.equals(intent.getAction())) {
                if (currentBuildTask != null) {
                    currentBuildTask.cancelBuild();
                }
            }
        }
    };

    /**
     * Saves the app's version information to the currently opened Sketchware project file.
     */
    private void saveVersionCodeInformationToProject() {
        HashMap<String, Object> projectMetadata = ProjectListManager.getProjectById(sc_id);
        if (projectMetadata != null) {
            projectMetadata.put("sketchware_ver", DeviceUtil.getVersionCode(getApplicationContext()));
            ProjectListManager.updateProject(sc_id, projectMetadata);
        }
    }

    private void loadProject(boolean haveSavedState) {
        projectFile = getDefaultProjectFile();
        ProjectDataManager.getProjectDataManager(sc_id, haveSavedState);
        ProjectDataManager.getFileManager(sc_id, haveSavedState);
        ResourceManager var2 = ProjectDataManager.getResourceManager(sc_id, haveSavedState);
        ProjectDataManager.getLibraryManager(sc_id, haveSavedState);
        ViewHistoryManager.getInstance(sc_id);
        BlockHistoryManager.getInstance(sc_id);
        if (!haveSavedState) {
            var2.backupImages();
            var2.backupSounds();
            var2.backupFonts();
        }
    }

    private ProjectFileBean getDefaultProjectFile() {
        return ProjectDataManager.getFileManager(sc_id).getFileByXmlName(ProjectFileBean.DEFAULT_XML_NAME);
    }

    private void refreshFileSelector() {
        if (projectFile == null) {
            projectFile = getDefaultProjectFile();
        }

        String javaFileName = projectFile.getJavaName();
        String xmlFileName = projectFile.getXmlName();

        if (!javaFileName.isEmpty()) {
            currentJavaFileName = javaFileName;
        }

        if (viewPager.getCurrentItem() == 0) {
            if (!ProjectFileBean.DEFAULT_XML_NAME.equals(xmlFileName) && ProjectDataManager.getFileManager(sc_id).getFileByXmlName(xmlFileName) == null) {
                projectFile = getDefaultProjectFile();
                xmlFileName = ProjectFileBean.DEFAULT_XML_NAME;
            }
            fileName.setText(xmlFileName);
        } else {
            if (!ProjectFileBean.DEFAULT_JAVA_NAME.equals(currentJavaFileName) && ProjectDataManager.getFileManager(sc_id).getActivityByJavaName(currentJavaFileName) == null) {
                projectFile = getDefaultProjectFile();
                currentJavaFileName = ProjectFileBean.DEFAULT_JAVA_NAME;
            }
            fileName.setText(currentJavaFileName);
        }
    }

    private void refreshViewTabAdapter() {
        if (viewTabAdapter != null && projectFile != null) {
            int orientation = projectFile.orientation;
            if (orientation == ProjectFileBean.ORIENTATION_PORTRAIT) {
                xmlLayoutOrientation.setImageResource(R.drawable.ic_screen_portrait_grey600_24dp);
            } else if (orientation == ProjectFileBean.ORIENTATION_LANDSCAPE) {
                xmlLayoutOrientation.setImageResource(R.drawable.ic_screen_landscape_grey600_24dp);
            } else {
                xmlLayoutOrientation.setImageResource(R.drawable.ic_screen_rotation_grey600_24dp);
            }
            viewTabAdapter.initialize(projectFile);
        }
    }

    private void refreshEventTabAdapter() {
        if (eventTabAdapter != null && projectFile != null) {
            eventTabAdapter.setCurrentActivity(projectFile);
            eventTabAdapter.refreshEvents();
        }
    }

    private void refreshComponentTabAdapter() {
        if (componentTabAdapter != null && projectFile != null) {
            componentTabAdapter.setProjectFile(projectFile);
            componentTabAdapter.refreshData();
        }
    }

    private void refresh() {
        refreshFileSelector();
        if (viewPager.getCurrentItem() == 0) {
            refreshViewTabAdapter();
        } else {
            refreshEventTabAdapter();
            refreshComponentTabAdapter();
        }
    }

    public void setTouchEventEnabled(boolean touchEventEnabled) {
        if (touchEventEnabled) {
            viewPager.enableTouchEvent();
        } else {
            viewPager.disableTouchEvent();
        }
    }

    /**
     * Shows a Snackbar indicating that a problem occurred while compiling. The user can click on "SHOW" to get to {@link CompileLogActivity}.
     *
     * @param error The error, to be later displayed as text in {@link CompileLogActivity}
     */
    private void indicateCompileErrorOccurred(String error) {
        new CompileErrorSaver(sc_id).writeLogsToFile(error);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, Helper.getResString(R.string.snackbar_show_compile_log), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(Helper.getResString(R.string.common_word_show), v -> {
            if (!UIHelper.isClickThrottled()) {
                snackbar.dismiss();
                Intent intent = new Intent(getApplicationContext(), CompileLogActivity.class);
                intent.putExtra("error", error);
                intent.putExtra("sc_id", sc_id);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        snackbar.show();
    }

    @Override
    public void finish() {
        ProjectDataManager.clearAll();
        ViewHistoryManager.clearInstance();
        BlockHistoryManager.clearInstance();
        setResult(RESULT_CANCELED, getIntent());
        super.finish();
    }

    private void checkForUnsavedProjectData() {
        if (ProjectDataManager.getLibraryManager(sc_id).hasBackup() || ProjectDataManager.getFileManager(sc_id).hasBackup() || ProjectDataManager.getResourceManager(sc_id).hasBackup() || ProjectDataManager.getProjectDataManager(sc_id).hasViewBackup() || ProjectDataManager.getProjectDataManager(sc_id).hasLogicBackup()) {
            askIfToRestoreOldUnsavedProjectData();
        }
    }

    /**
     * Opens the debug APK to install.
     */
    private void installBuiltApk() {
        if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_ROOT_AUTO_INSTALL_PROJECTS)) {
            requestPackageInstallerInstall();
        } else {
            File apkUri = new File(projectFilePaths.finalToInstallApkPath);
            long length = apkUri.length();
            Shell.getShell(shell -> {
                if (shell.isRoot()) {
                    List<String> stdout = new LinkedList<>();
                    List<String> stderr = new LinkedList<>();

                    Shell.cmd("cat " + apkUri + " | pm install -S " + length).to(stdout, stderr).submit(result -> {
                        if (result.isSuccess()) {
                            SketchwareUtil.toast(Helper.getResString(R.string.design_toast_package_installed));
                            if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_ROOT_AUTO_OPEN_AFTER_INSTALLING)) {
                                Intent launcher = getPackageManager().getLaunchIntentForPackage(projectFilePaths.packageName);
                                if (launcher != null) {
                                    startActivity(launcher);
                                } else {
                                    SketchwareUtil.toastError(Helper.getResString(R.string.design_error_cannot_launch));
                                }
                            }
                        } else {
                            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.design_error_install_failed), result.getCode()), Toast.LENGTH_LONG);
                            LogUtil.e("DesignActivity", "Failed to install package, result code: " + result.getCode() + ". stdout: " + stdout + ", stderr: " + stderr);
                        }
                    });
                } else {
                    SketchwareUtil.toastError(Helper.getResString(R.string.design_error_no_root_access));
                    requestPackageInstallerInstall();
                }
            });
        }
    }

    private void requestPackageInstallerInstall() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", new File(projectFilePaths.finalToInstallApkPath));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            SketchwareUtil.toastError(Helper.getResString(R.string.error_no_package_installer));
        }
    }

    public void hideViewPropertyView() {
        viewTabAdapter.togglePropertyView(false);
    }

    private void saveChangesAndCloseProject() {
        showLoadingDialog();
        SaveChangesProjectCloser saveChangesProjectCloser = new SaveChangesProjectCloser(this);
        saveChangesProjectCloser.execute();
    }

    private void saveProject() {
        showLoadingDialog();
        ProjectSaver projectSaver = new ProjectSaver(this);
        projectSaver.execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.END)) {
                    drawer.closeDrawer(GravityCompat.END);
                } else if (viewTabAdapter.isPropertyViewVisible()) {
                    hideViewPropertyView();
                } else {
                    if (currentTabNumber > 0) {
                        currentTabNumber--;
                        viewPager.setCurrentItem(currentTabNumber);
                    } else if (prefP12.getBooleanDefault("P12I2")) {
                        showLoadingDialog();
                        saveChangesAndCloseProject();
                    } else {
                        showSaveBeforeQuittingDialog();
                    }
                }
            }
        });
        setContentView(R.layout.design);
        if (!isStoragePermissionGranted()) {
            finish();
        }

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        prefP1 = new SharedPrefsHelper(getApplicationContext(), "P1");
        prefP12 = new SharedPrefsHelper(getApplicationContext(), "P12");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(sc_id);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        Insetter.builder().margin(WindowInsetsCompat.Type.navigationBars()).applyToView(findViewById(R.id.container));

        coordinatorLayout = findViewById(R.id.layout_coordinator);
        fileName = findViewById(R.id.file_name);

        findViewById(R.id.file_name_container).setOnClickListener(this);

        btnRun = findViewById(R.id.btn_run);
        btnRun.setOnClickListener(v -> {
            if (currentBuildTask != null && !currentBuildTask.canceled && !currentBuildTask.isBuildFinished) {
                currentBuildTask.cancelBuild();
                return;
            }

            BuildTask buildTask = new BuildTask(this);
            currentBuildTask = buildTask;
            buildTask.execute();
        });

        btnOptions = findViewById(R.id.btn_options);
        btnOptions.setOnClickListener(v -> bottomPopupMenu.show());

        bottomPopupMenu = new PopupMenu(this, btnOptions);
        bottomMenu = bottomPopupMenu.getMenu();
        bottomMenu.add(Menu.NONE, 1, Menu.NONE, Helper.getResString(R.string.design_menu_build_settings)).setOnMenuItemClickListener(item -> {
            BuildSettingsBottomSheet sheet = BuildSettingsBottomSheet.newInstance(sc_id);
            sheet.show(getSupportFragmentManager(), BuildSettingsBottomSheet.TAG);
            return true;
        });
        bottomMenu.add(Menu.NONE, 2, Menu.NONE, Helper.getResString(R.string.design_menu_clean_temp)).setVisible(false).setOnMenuItemClickListener(item -> {
            backgroundExecutor.execute(() -> {
                try {
                    FileUtil.deleteFile(projectFilePaths.projectMyscPath);
                    updateBottomMenu();
                    runOnUiThread(() -> SketchwareUtil.toast(Helper.getResString(R.string.design_toast_clean_temp_done)));
                } catch (Exception e) {
                    Log.e("DesignActivity", "Failed to clean temporary files", e);
                }
            });
            return true;
        });
        bottomMenu.add(Menu.NONE, 3, Menu.NONE, Helper.getResString(R.string.design_menu_show_last_error)).setOnMenuItemClickListener(item -> {
            new CompileErrorSaver(sc_id).showLastErrors(this);
            return true;
        });
        bottomMenu.add(Menu.NONE, 5, Menu.NONE, Helper.getResString(R.string.design_menu_show_source)).setOnMenuItemClickListener(item -> {
            showCurrentActivitySrcCode();
            return true;
        });
        bottomMenu.add(Menu.NONE, 4, Menu.NONE, Helper.getResString(R.string.design_menu_install_apk)).setVisible(false).setOnMenuItemClickListener(item -> {
            if (FileUtil.isExistFile(projectFilePaths.finalToInstallApkPath)) {
                installBuiltApk();
            } else SketchwareUtil.toast(Helper.getResString(R.string.design_error_apk_not_exist));
            return true;
        });
        bottomMenu.add(Menu.NONE, 6, Menu.NONE, Helper.getResString(R.string.design_menu_show_signatures)).setVisible(false).setOnMenuItemClickListener(item -> {
            ApkSignatures apkSignatures = new ApkSignatures(this, projectFilePaths.finalToInstallApkPath);
            apkSignatures.showSignaturesDialog();
            return true;
        });
        bottomMenu.add(Menu.NONE, 7, Menu.NONE, Helper.getResString(R.string.design_menu_xml_editor)).setOnMenuItemClickListener(item -> {
            toViewCodeEditor();
            return true;
        });
        bottomPopupMenu.setOnDismissListener(menu -> btnOptions.setChecked(false));

        xmlLayoutOrientation = findViewById(R.id.img_orientation);
        viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (currentTabNumber == 1) {
                    if (eventTabAdapter != null) {
                        eventTabAdapter.resetEventValues();
                    }
                } else if (currentTabNumber == 2 && componentTabAdapter != null) {
                    componentTabAdapter.unselectAll();
                }
                if (position == 0) {
                    bottomMenu.findItem(7).setVisible(true);
                    if (viewTabAdapter != null) {
                        viewTabAdapter.showHidePropertyView(true);
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_screen);
                    }
                } else if (position == 1) {
                    bottomMenu.findItem(7).setVisible(false);
                    if (viewTabAdapter != null) {
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_code);
                        viewTabAdapter.showHidePropertyView(false);
                        if (eventTabAdapter != null) {
                            eventTabAdapter.refreshEvents();
                        }
                    }
                } else {
                    bottomMenu.findItem(7).setVisible(false);
                    if (viewTabAdapter != null) {
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_code);
                        viewTabAdapter.showHidePropertyView(false);
                        if (componentTabAdapter != null) {
                            componentTabAdapter.refreshData();
                        }
                    }
                }
                refresh();
                currentTabNumber = position;
                invalidateOptionsMenu();
            }
        });
        viewPager.getAdapter().notifyDataSetChanged();
        ((TabLayout) findViewById(R.id.tab_layout)).setupWithViewPager(viewPager);

        IntentFilter filter = new IntentFilter(BuildTask.ACTION_CANCEL_BUILD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(buildCancelReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(buildCancelReceiver, filter);
        }

    }

    private boolean isDebugApkExists() {
        if (projectFilePaths != null) {
            return FileUtil.isExistFile(projectFilePaths.finalToInstallApkPath);
        }
        return false;
    }

    private void updateBottomMenu() {
        if (bottomMenu != null) {
            handler.post(() -> {
                bottomMenu.findItem(2).setVisible(projectFilePaths != null && FileUtil.isExistFile(projectFilePaths.projectMyscPath));
                var isDebugApkExists = isDebugApkExists();
                bottomMenu.findItem(4).setVisible(isDebugApkExists);
                bottomMenu.findItem(6).setVisible(isDebugApkExists);
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(buildCancelReceiver);
        backgroundExecutor.shutdownNow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.design_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.design_option_menu_search);
        if (searchItem != null) {
            searchItem.setVisible(currentTabNumber == 1);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.design_actionbar_titleopen_drawer) {
            if (!drawer.isDrawerOpen(GravityCompat.END)) {
                drawer.openDrawer(GravityCompat.END);
            }
        } else if (itemId == R.id.design_option_menu_title_save_project) {
            saveProject();
        } else if (itemId == R.id.design_option_menu_search) {
            if (eventTabAdapter != null) {
                eventTabAdapter.toggleSearchBar();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        showLoadingDialog();

        HashMap<String, Object> projectInfo = ProjectListManager.getProjectById(sc_id);
        getSupportActionBar().setTitle(MapValueHelper.getString(projectInfo, "my_ws_name"));
        projectFilePaths = new ProjectFilePaths(getApplicationContext(), SketchwarePaths.getMyscPath(sc_id), projectInfo);

        try {
            ProjectLoader projectLoader = new ProjectLoader(this, savedInstanceState);
            projectLoader.execute();
        } catch (Exception e) {
            crashlytics.log("ProjectLoader failed");
            crashlytics.recordException(e);
        } finally {
            SystemLogPrinter.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isStoragePermissionGranted()) {
            finish();
        }

        long freeMegabytes = DeviceUtil.getFreeStorageMB();
        if (freeMegabytes < 100L && freeMegabytes > 0L) {
            warnAboutInsufficientStorageSpace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", sc_id);
        super.onSaveInstanceState(outState);
        if (!isStoragePermissionGranted()) {
            finish();
        }

        if (!isRestoringData) {
            UnsavedChangesSaver unsavedChangesSaver = new UnsavedChangesSaver(this);
            unsavedChangesSaver.execute();
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.file_name_container) {
            if (viewPager.getCurrentItem() == 0) {
                showAvailableViews();
            } else {
                showAvailableJavaFiles();
            }
        }
    }

    /**
     * Show a dialog asking about saving the project before quitting.
     */
    private void showSaveBeforeQuittingDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.design_quit_title_exit_projet));
        dialog.setIcon(R.drawable.ic_mtrl_exit);
        dialog.setMessage(Helper.getResString(R.string.design_quit_message_confirm_save));
        dialog.setPositiveButton(Helper.getResString(R.string.design_quit_button_save_and_exit), (v, which) -> {
            if (!UIHelper.isClickThrottled()) {
                v.dismiss();
                try {
                    saveChangesAndCloseProject();
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    dismissLoadingDialog();
                }
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_exit), (v, which) -> {
            if (!UIHelper.isClickThrottled()) {
                v.dismiss();
                try {
                    showLoadingDialog();
                    DiscardChangesProjectCloser discardChangesProjectCloser = new DiscardChangesProjectCloser(this);
                    discardChangesProjectCloser.execute();
                } catch (Exception e) {
                    crashlytics.recordException(e);
                    dismissLoadingDialog();
                }
            }
        });
        dialog.setNeutralButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    /**
     * Show a dialog warning the user about low free space.
     */
    private void warnAboutInsufficientStorageSpace() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
        dialog.setIcon(R.drawable.break_warning_96_red);
        dialog.setMessage(Helper.getResString(R.string.common_message_insufficient_storage_space));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    private void askIfToRestoreOldUnsavedProjectData() {
        isRestoringData = true;
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.ic_mtrl_history);
        dialog.setTitle(Helper.getResString(R.string.design_restore_data_title));
        dialog.setMessage(Helper.getResString(R.string.design_restore_data_message_confirm));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_restore), (v, which) -> {
            if (!UIHelper.isClickThrottled()) {
                boolean g = ProjectDataManager.getLibraryManager(sc_id).hasBackup();
                boolean g2 = ProjectDataManager.getFileManager(sc_id).hasBackup();
                boolean q = ProjectDataManager.getResourceManager(sc_id).hasBackup();
                boolean d = ProjectDataManager.getProjectDataManager(sc_id).hasViewBackup();
                boolean c = ProjectDataManager.getProjectDataManager(sc_id).hasLogicBackup();
                if (g) {
                    ProjectDataManager.getLibraryManager(sc_id).loadFromBackup();
                }
                if (g2) {
                    ProjectDataManager.getFileManager(sc_id).loadFromBackup();
                }
                if (q) {
                    ProjectDataManager.getResourceManager(sc_id).loadFromBackup();
                }
                if (d) {
                    ProjectDataManager.getProjectDataManager(sc_id).loadViewFromBackup();
                }
                if (c) {
                    ProjectDataManager.getProjectDataManager(sc_id).loadLogicFromBackup();
                }
                if (g) {
                    ProjectDataManager.getFileManager(sc_id).syncWithLibrary(ProjectDataManager.getLibraryManager(sc_id));
                    ProjectDataManager.getProjectDataManager(sc_id).removeAdmobComponents(ProjectDataManager.getLibraryManager(sc_id).getFirebaseDB());
                }
                if (g2 || g) {
                    ProjectDataManager.getProjectDataManager(sc_id).syncWithFileManager(ProjectDataManager.getFileManager(sc_id));
                }
                if (q) {
                    ProjectDataManager.getProjectDataManager(sc_id).syncSounds(ProjectDataManager.getResourceManager(sc_id));
                    ProjectDataManager.getProjectDataManager(sc_id).syncFonts(ProjectDataManager.getResourceManager(sc_id));
                }
                refresh();
                isRestoringData = false;
                v.dismiss();
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_no), (v, which) -> {
            isRestoringData = false;
            v.dismiss();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showCurrentActivitySrcCode() {
        if (projectFile == null) return;
        showLoadingDialog();
        backgroundExecutor.execute(() -> {
            try {
                var filename = Helper.getText(fileName);
                var code = new ProjectFilePaths(getApplicationContext(), sc_id).getFileSrc(filename, ProjectDataManager.getFileManager(sc_id), ProjectDataManager.getProjectDataManager(sc_id), ProjectDataManager.getLibraryManager(sc_id));
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    dismissLoadingDialog();
                    if (code.isEmpty()) {
                        SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_source));
                        return;
                    }
                    var scheme = filename.endsWith(".xml") ? CodeViewerActivity.SCHEME_XML : CodeViewerActivity.SCHEME_JAVA;
                    launchActivity(CodeViewerActivity.class, null, new Pair<>("code", code), new Pair<>("sc_id", sc_id), new Pair<>("scheme", scheme));
                });
            } catch (Exception e) {
                Log.e("DesignActivity", "Failed to generate source code", e);
                runOnUiThread(() -> { dismissLoadingDialog(); SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_source)); });
            }
        });
    }

    private void showAvailableJavaFiles() {
        var dialog = new MaterialAlertDialogBuilder(this).create();
        dialog.setTitle(R.string.design_file_selector_title_java);
        dialog.setIcon(R.drawable.ic_mtrl_java);
        View customView = pro.sketchware.core.ViewUtil.inflateLayout(this, R.layout.file_selector_popup_select_java);
        RecyclerView recyclerView = customView.findViewById(R.id.file_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        var adapter = new JavaFileAdapter(sc_id);
        adapter.setOnItemClickListener(projectFileBean -> {
            projectFile = projectFileBean;
            refreshFileSelector();
            refreshEventTabAdapter();
            refreshComponentTabAdapter();
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);
        dialog.setView(customView);
        dialog.show();
    }

    private void showAvailableViews() {
        Intent intent = new Intent(getApplicationContext(), ViewSelectorActivity.class);
        intent.putExtra("sc_id", sc_id);
        intent.putExtra("current_xml", projectFile.getXmlName());
        intent.putExtra("is_custom_view", projectFile.fileType == 1 || projectFile.fileType == 2);
        changeOpenFile.launch(intent);
    }

    /**
     * Opens {@link ViewCodeEditorActivity}.
     */
    void toViewCodeEditor() {
        if (projectFile == null) return;
        showLoadingDialog();
        backgroundExecutor.execute(() -> {
            try {
                String filename = Helper.getText(fileName);
                // var ProjectFilePaths = new ProjectFilePaths(getApplicationContext(), sc_id);
                var xmlGenerator = new LayoutGenerator(projectFilePaths.buildConfig, projectFile);
                var projectDataManager = ProjectDataManager.getProjectDataManager(sc_id);
                var viewBeans = projectDataManager.getViews(filename);
                var viewFab = projectDataManager.getFabView(filename);
                xmlGenerator.setExcludeAppCompat(true);
                xmlGenerator.setViews(ProjectDataStore.getSortedRootViews(viewBeans), viewFab);
                String content = xmlGenerator.toXmlString();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    dismissLoadingDialog();
                    launchActivity(ViewCodeEditorActivity.class, openViewCodeEditor, new Pair<>("title", filename), new Pair<>("content", content));
                });
            } catch (Exception e) {
                Log.e("DesignActivity", "Failed to generate view code", e);
                runOnUiThread(() -> { dismissLoadingDialog(); SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_code)); });
            }
        });
    }

    /**
     * Opens {@link LogReaderActivity}.
     */
    void toLogReader() {
        Intent intent = new Intent(getApplicationContext(), LogReaderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        startActivity(intent);
    }

    /**
     * Opens {@link ManageCollectionActivity}.
     */
    void toCollectionManager() {
        launchActivity(ManageCollectionActivity.class, openCollectionManager);
    }

    /**
     * Opens {@link AndroidManifestInjection}.
     */
    void toAndroidManifestManager() {
        if (projectFile == null) return;
        launchActivity(AndroidManifestInjection.class, null, new Pair<>("file_name", currentJavaFileName));
    }

    /**
     * Opens {@link ManageAppCompatActivity}.
     */
    void toAppCompatInjectionManager() {
        if (projectFile == null) return;
        launchActivity(ManageAppCompatActivity.class, null, new Pair<>("file_name", projectFile.getXmlName()));
    }

    /**
     * Opens {@link ManageAssetsActivity}.
     */
    void toAssetManager() {
        launchActivity(ManageAssetsActivity.class, null);
    }

    /**
     * Shows a {@link CustomBlocksDialog}.
     */
    void toCustomBlocksViewer() {
        new CustomBlocksDialog().show(this, sc_id);
    }

    /**
     * Opens {@link ManageJavaActivity}.
     */
    void toJavaManager() {
        launchActivity(ManageJavaActivity.class, null, new Pair<>("pkgName", projectFilePaths.packageName));
    }

    /**
     * Opens {@link ManagePermissionActivity}.
     */
    void toPermissionManager() {
        launchActivity(ManagePermissionActivity.class, null);
    }

    /**
     * Opens {@link ManageProguardActivity}.
     */
    void toProguardManager() {
        launchActivity(ManageProguardActivity.class, null);
    }

    /**
     * Opens {@link ManageResourceActivity}.
     */
    void toResourceManager() {
        launchActivity(ManageResourceActivity.class, openResourcesManager);
    }

    /**
     * Opens {@link ResourcesEditorActivity}.
     */
    void toResourceEditor() {
        launchActivity(ResourcesEditorActivity.class, openResourcesManager);
    }

    /**
     * Opens {@link ManageStringFogFragment}.
     */
    void toStringFogManager() {
        var fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag("stringFogFragment") == null) {
            var bottomSheet = new ManageStringFogFragment();
            bottomSheet.show(fragmentManager, "stringFogFragment");
        }
    }

    /**
     * Opens {@link ManageFontActivity}.
     */
    void toFontManager() {
        launchActivity(ManageFontActivity.class, null);
    }

    /**
     * Opens {@link ManageImageActivity}.
     */
    void toImageManager() {
        launchActivity(ManageImageActivity.class, openImageManager);
    }

    /**
     * Opens {@link ManageLibraryActivity}.
     */
    void toLibraryManager() {
        launchActivity(ManageLibraryActivity.class, openLibraryManager);
    }

    /**
     * Opens {@link ManageViewActivity}.
     */
    void toViewManager() {
        launchActivity(ManageViewActivity.class, openViewManager);
    }

    /**
     * Opens {@link ManageSoundActivity}.
     */
    void toSoundManager() {
        launchActivity(ManageSoundActivity.class, null);
    }

    /**
     * Opens {@link SrcViewerActivity}.
     */
    void toSourceCodeViewer() {
        launchActivity(SrcViewerActivity.class, null, new Pair<>("current", Helper.getText(fileName)));
    }

    /**
     * Opens {@link ManageXMLCommandActivity}.
     */
    void toXMLCommandManager() {
        launchActivity(ManageXMLCommandActivity.class, null);
    }

    @SafeVarargs
    private void launchActivity(Class<? extends Activity> toLaunch, ActivityResultLauncher<Intent> optionalLauncher, Pair<String, String>... extras) {
        Intent intent = new Intent(getApplicationContext(), toLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("sc_id", sc_id);
        for (Pair<String, String> extra : extras) {
            intent.putExtra(extra.first, extra.second);
        }

        if (optionalLauncher == null) {
            startActivity(intent);
        } else {
            optionalLauncher.launch(intent);
        }
    }

    private abstract static class BaseTask {
        protected final WeakReference<DesignActivity> activityRef;

        protected BaseTask(DesignActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        protected DesignActivity getActivity() {
            return activityRef.get();
        }
    }

    private static class BuildTask extends BaseTask implements BuildProgressReceiver {
        public static final String ACTION_CANCEL_BUILD = "com.besome.sketch.design.ACTION_CANCEL_BUILD";
        private static final String CHANNEL_ID = "build_notification_channel";
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private final NotificationManager notificationManager;
        private final int notificationId = 1;
        private final MaterialButton btnRun;
        private final MaterialButton btnOptions;
        private final LinearLayout progressContainer;
        private final TextView progressText;
        private final LinearProgressIndicator progressBar;
        public volatile boolean canceled;
        private volatile boolean isBuildFinished;
        private boolean isShowingNotification = false;

        public BuildTask(DesignActivity activity) {
            super(activity);
            notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            btnRun = activity.btnRun;
            btnOptions = activity.btnOptions;
            progressContainer = activity.findViewById(R.id.progress_container);
            progressText = activity.findViewById(R.id.progress_text);
            progressBar = activity.findViewById(R.id.progress);
        }

        public void execute() {
            onPreExecute();
            executorService.execute(this::doInBackground);
        }

        private void onPreExecute() {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(() -> {
                updateRunButton(true);
                activity.prefP1.put("P1I10", true);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                maybeShowNotification();
            });
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            try {
                var q = activity.projectFilePaths;
                var sc_id = DesignActivity.sc_id;
                onProgress("Deleting temporary files...", 1);
                FileUtil.deleteFile(q.projectMyscPath);

                q.createBuildDirectories(activity.getApplicationContext());
                q.deleteValuesV21Directory();
                q.extractAssetsToRes(activity.getApplicationContext(), SketchwarePaths.getResourceZipPath("600"));
                if (MapValueHelper.get(ProjectListManager.getProjectById(sc_id), "custom_icon")) {
                    q.copyMipmapFolder(SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "mipmaps");
                    if (MapValueHelper.get(ProjectListManager.getProjectById(sc_id), "isIconAdaptive", false)) {
                        q.createLauncherIconXml("""
                                <?xml version="1.0" encoding="utf-8"?>
                                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                                <background android:drawable="@mipmap/ic_launcher_background"/>
                                <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                                <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                                </adaptive-icon>""");
                    } else {
                        q.copyAppIcon(SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "icon.png");
                    }
                }

                onProgress("Generating source code...", 2);
                ResourceManager ResourceManager = ProjectDataManager.getResourceManager(sc_id);
                ResourceManager.copyImagesToDir(q.resDirectoryPath + File.separator + "drawable-xhdpi");
                ResourceManager = ProjectDataManager.getResourceManager(sc_id);
                ResourceManager.copySoundsToDir(q.resDirectoryPath + File.separator + "raw");
                ResourceManager = ProjectDataManager.getResourceManager(sc_id);
                ResourceManager.copyFontsToDir(q.assetsPath + File.separator + "fonts");

                ProjectBuilder builder = new ProjectBuilder(this, activity.getApplicationContext(), q);

                var fileManager = ProjectDataManager.getFileManager(sc_id);
                var dataManager = ProjectDataManager.getProjectDataManager(sc_id);
                var libraryManager = ProjectDataManager.getLibraryManager(sc_id);
                q.initializeMetadata(libraryManager, fileManager, dataManager);
                builder.buildBuiltInLibraryInformation();
                q.generateProjectFiles(fileManager, dataManager, libraryManager, builder.getBuiltInLibraryManager());
                q.cleanBuildCache();
                q.prepareBuildDirectories();

                builder.maybeExtractAapt2();
                if (canceled) {
                    return;
                }

                onProgress("Extracting built-in libraries...", 3);
                BuiltInLibraries.extractCompileAssets(this);
                if (canceled) {
                    return;
                }

                onProgress("AAPT2 is running...", 8);
                builder.compileResources();
                if (canceled) {
                    return;
                }

                onProgress("Generating view binding...", 11);
                builder.generateViewBinding();
                if (canceled) {
                    return;
                }

                KotlinCompilerBridge.compileKotlinCodeIfPossible(this, builder);
                if (canceled) {
                    return;
                }

                onProgress("Java is compiling...", 13);
                builder.compileJavaCode();
                if (canceled) {
                    return;
                }

                StringfogHandler stringfogHandler = new StringfogHandler(sc_id);
                stringfogHandler.start(this, builder);
                if (canceled) {
                    return;
                }

                ProguardHandler proguardHandler = new ProguardHandler(sc_id);
                proguardHandler.start(this, builder);
                if (canceled) {
                    return;
                }

                onProgress(builder.getDxRunningText(), 17);
                builder.createDexFilesFromClasses();
                if (canceled) {
                    return;
                }

                onProgress("Merging DEX files...", 18);
                builder.getDexFilesReady();
                if (canceled) {
                    return;
                }

                onProgress("Building APK...", 19);
                builder.buildApk();
                if (canceled) {
                    return;
                }

                onProgress("Signing APK...", 20);
                builder.signDebugApk();
                if (canceled) {
                    return;
                }

                activity.installBuiltApk();
                isBuildFinished = true;
            } catch (MissingFileException e) {
                isBuildFinished = true;
                activity.runOnUiThread(() -> {
                    boolean isMissingDirectory = e.isMissingDirectory();

                    MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
                    if (isMissingDirectory) {
                        dialog.setTitle(R.string.build_error_missing_directory_title);
                        dialog.setMessage(String.format(Helper.getResString(R.string.build_error_missing_directory_msg), e.getMissingFile().getAbsolutePath()));
                        dialog.setNeutralButton(R.string.common_word_create, (v, which) -> {
                            v.dismiss();
                            if (!e.getMissingFile().mkdirs()) {
                                SketchwareUtil.toastError(Helper.getResString(R.string.build_error_failed_create_directory));
                            }
                        });
                    } else {
                        dialog.setTitle(R.string.build_error_missing_file_title);
                        dialog.setMessage(String.format(Helper.getResString(R.string.build_error_missing_file_msg), e.getMissingFile().getAbsolutePath()));
                    }
                    dialog.setPositiveButton(R.string.common_word_dismiss, null);
                    dialog.show();
                });
            } catch (SimpleException simpleException) {
                isBuildFinished = true;
                activity.indicateCompileErrorOccurred(simpleException.getMessage());
            } catch (Throwable tr) {
                isBuildFinished = true;
                LogUtil.e("DesignActivity$BuildTask", "Failed to build project", tr);
                activity.indicateCompileErrorOccurred(Log.getStackTraceString(tr));
            } finally {
                activity.runOnUiThread(this::onPostExecute);
            }
        }

        @Override
        public void onProgress(String progress, int step) {
            int totalSteps = 20;

            DesignActivity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(() -> {
                progressBar.setIndeterminate(step == -1);
                if (!canceled) {
                    updateNotification(progress + " (" + step + " / " + totalSteps + ")");
                }
                progressText.setText(progress);
                var progressInt = (step * 100) / totalSteps;
                progressBar.setProgress(progressInt, true);
                Log.d("DesignActivity$BuildTask", step + " / " + totalSteps);
            });
        }

        private void onPostExecute() {
            executorService.shutdown();
            DesignActivity activity = getActivity();
            if (activity == null) return;

            activity.runOnUiThread(() -> {
                if (!activity.isDestroyed()) {
                    if (isShowingNotification) {
                        notificationManager.cancel(notificationId);
                        isShowingNotification = false;
                    }
                    updateRunButton(false);
                    activity.updateBottomMenu();
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }

        public void cancelBuild() {
            canceled = true;
            onProgress("Canceling build...", -1);
            if (isShowingNotification) {
                notificationManager.cancel(notificationId);
                isShowingNotification = false;
            }
            DesignActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                });
            }
        }

        private void maybeShowNotification() {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            if (!isShowingNotification) {
                createNotificationChannelIfNeeded();

                NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_mtrl_code)
                        .setContentTitle(Helper.getResString(R.string.notification_building_project))
                        .setContentText(Helper.getResString(R.string.notification_starting_build))
                        .setOngoing(true)
                        .setProgress(0, 0, true)
                        .addAction(R.drawable.ic_cancel_white_96dp, Helper.getResString(R.string.notification_cancel_build), getCancelPendingIntent());

                notificationManager.notify(notificationId, builder.build());
                isShowingNotification = true;
            }
        }

        private void updateNotification(String progress) {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_mtrl_code)
                    .setContentTitle(Helper.getResString(R.string.notification_building_project))
                    .setContentText(progress)
                    .setOngoing(true)
                    .setProgress(0, 0, true)
                    .addAction(R.drawable.ic_cancel_white_96dp, Helper.getResString(R.string.notification_cancel_build), getCancelPendingIntent());

            notificationManager.notify(notificationId, builder.build());
        }

        private PendingIntent getCancelPendingIntent() {
            DesignActivity activity = getActivity();
            if (activity == null) return null;

            Intent cancelIntent = new Intent(BuildTask.ACTION_CANCEL_BUILD);
            return PendingIntent.getBroadcast(activity, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        private void createNotificationChannelIfNeeded() {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            CharSequence name = Helper.getResString(R.string.notification_channel_build);
            String description = Helper.getResString(R.string.notification_channel_build_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        private void updateRunButton(boolean isRunning) {
            var context = getActivity();
            btnRun.setBackgroundTintList(ColorStateList.valueOf(ThemeUtils.getColor(context, isRunning ? R.attr.colorErrorContainer : R.attr.colorPrimary)));
            btnRun.setIcon(ContextCompat.getDrawable(context, isRunning ? R.drawable.ic_mtrl_stop : R.drawable.ic_mtrl_run));
            btnRun.setIconTint(ColorStateList.valueOf(ThemeUtils.getColor(context, isRunning ? R.attr.colorOnErrorContainer : R.attr.colorSurfaceContainerLowest)));
            btnRun.setTextColor(ColorStateList.valueOf(ThemeUtils.getColor(context, isRunning ? R.attr.colorOnErrorContainer : R.attr.colorSurfaceContainerLowest)));
            btnRun.setText(isRunning ? "Stop" : "Run");
            btnOptions.setEnabled(!isRunning);
            progressContainer.setVisibility(isRunning ? View.VISIBLE : View.GONE);
        }
    }

    private static class ProjectLoader extends BaseTask {
        private final Bundle savedInstanceState;

        public ProjectLoader(DesignActivity activity, Bundle savedInstanceState) {
            super(activity);
            this.savedInstanceState = savedInstanceState;
        }

        public void execute() {
            getActivity().showLoadingDialog();
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                activity.loadProject(savedInstanceState != null);
                activity.runOnUiThread(() -> {
                    activity.updateBottomMenu();
                    activity.refresh();
                    activity.dismissLoadingDialog();
                    if (savedInstanceState == null) {
                        activity.checkForUnsavedProjectData();
                    }
                });
            }
        }
    }

    private static class DiscardChangesProjectCloser extends BaseTask {

        public DiscardChangesProjectCloser(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            getActivity().showLoadingDialog();
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                var sc_id = DesignActivity.sc_id;
                ProjectDataManager.getResourceManager(sc_id).restoreImagesFromTemp();
                ProjectDataManager.getResourceManager(sc_id).restoreSoundsFromTemp();
                ProjectDataManager.getResourceManager(sc_id).restoreFontsFromTemp();
                activity.runOnUiThread(() -> {
                    activity.dismissLoadingDialog();
                    activity.finish();
                });
            }
        }
    }

    private static class ProjectSaver extends BaseTask {

        public ProjectSaver(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            getActivity().showLoadingDialog();
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                var sc_id = DesignActivity.sc_id;
                ProjectDataManager.getResourceManager(sc_id).cleanupAllResources();
                ProjectDataManager.getFileManager(sc_id).saveToData();
                ProjectDataManager.getProjectDataManager(sc_id).saveAllData();
                ProjectDataManager.getResourceManager(sc_id).saveToData();
                ProjectDataManager.getLibraryManager(sc_id).saveToData();
                activity.runOnUiThread(() -> {
                    SketchToast.toast(activity.getApplicationContext(), Helper.getResString(R.string.common_message_complete_save), SketchToast.TOAST_NORMAL).show();
                    activity.saveVersionCodeInformationToProject();
                    activity.dismissLoadingDialog();
                    ProjectDataManager.getResourceManager(sc_id).backupImages();
                    ProjectDataManager.getResourceManager(sc_id).backupSounds();
                    ProjectDataManager.getResourceManager(sc_id).backupFonts();
                });
            }
        }
    }

    private static class SaveChangesProjectCloser extends BaseTask {

        public SaveChangesProjectCloser(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            getActivity().showLoadingDialog();
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                var sc_id = DesignActivity.sc_id;
                ProjectDataManager.getResourceManager(sc_id).cleanupAllResources();
                ProjectDataManager.getFileManager(sc_id).saveToData();
                ProjectDataManager.getProjectDataManager(sc_id).saveAllData();
                ProjectDataManager.getResourceManager(sc_id).saveToData();
                ProjectDataManager.getLibraryManager(sc_id).saveToData();
                ProjectDataManager.getResourceManager(sc_id).deleteTempDirs();
                activity.runOnUiThread(() -> {
                    SketchToast.toast(activity.getApplicationContext(), Helper.getResString(R.string.common_message_complete_save), SketchToast.TOAST_NORMAL).show();
                    activity.saveVersionCodeInformationToProject();
                    activity.dismissLoadingDialog();
                    activity.finish();
                });
            }
        }
    }

    private static class UnsavedChangesSaver extends BaseTask {

        public UnsavedChangesSaver(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            new Thread(this::doInBackground).start();
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                ProjectDataStore ecInstance = ProjectDataManager.getProjectDataManager(sc_id);
                synchronized (ecInstance) {
                    ecInstance.saveAllBackup();
                }
            }
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final String[] labels;

        public ViewPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            labels = new String[]{
                    Helper.getResString(R.string.design_tab_title_view),
                    Helper.getResString(R.string.design_tab_title_event),
                    Helper.getResString(R.string.design_tab_title_component)};
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return labels[position];
        }

        @Override
        @NonNull
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            if (position == 0) {
                viewTabAdapter = (ViewEditorFragment) fragment;
            } else if (position == 1) {
                eventTabAdapter = (EventListFragment) fragment;
            } else {
                componentTabAdapter = (ComponentListFragment) fragment;
            }

            return fragment;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            if (position == 0) {
                return new ViewEditorFragment();
            } else {
                return position == 1 ? new EventListFragment() : new ComponentListFragment();
            }
        }
    }
}
