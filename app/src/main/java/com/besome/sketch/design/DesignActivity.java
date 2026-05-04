package com.besome.sketch.design;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CompletableFuture;
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

import pro.sketchware.core.BackgroundTasks;
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
import pro.sketchware.core.TaskHost;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.EventListFragment;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.MapValueHelper;
import pro.sketchware.core.ProjectFilePaths;
import pro.sketchware.core.SimpleException;
import pro.sketchware.utility.UI;
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
    public static String sc_id;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FirebaseCrashlytics crashlytics = getFirebaseCrashlytics();
    private ImageView xmlLayoutOrientation;
    private boolean isRestoringData;
    private int currentTabNumber;
    private ProjectFileBean lastViewTabProjectFile;
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
        ProjectDataManager.getProjectDataManager(sc_id, haveSavedState);
        ProjectDataManager.getFileManager(sc_id, haveSavedState);
        ResourceManager resourceManager = ProjectDataManager.getResourceManager(sc_id, haveSavedState);
        ProjectDataManager.getLibraryManager(sc_id, haveSavedState);
        projectFile = getDefaultProjectFile();
        ViewHistoryManager.getInstance(sc_id);
        BlockHistoryManager.getInstance(sc_id);
        // Resource backup is now lazy — ensureBackedUp() is called
        // before any resource modification, not eagerly on project open.
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
        lastViewTabProjectFile = projectFile;
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
        TaskHost taskHost = TaskHost.of(this);
        taskHost.postToUi(() -> {
            if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_ROOT_AUTO_INSTALL_PROJECTS)) {
                requestPackageInstallerInstall();
            } else {
                File apkUri = new File(projectFilePaths.finalToInstallApkPath);
                long length = apkUri.length();
                Shell.getShell(shell -> {
                    if (shell.isRoot()) {
                        List<String> stdout = new LinkedList<>();
                        List<String> stderr = new LinkedList<>();

                        Shell.cmd("cat " + apkUri + " | pm install -S " + length).to(stdout, stderr).submit(result ->
                                taskHost.postToUi(() -> {
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
                                }));
                    } else {
                        taskHost.postToUi(() -> {
                            SketchwareUtil.toastError(Helper.getResString(R.string.design_error_no_root_access));
                            requestPackageInstallerInstall();
                        });
                    }
                });
            }
        });
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

        if (sc_id == null || sc_id.isEmpty()) {
            finish();
            return;
        }

        prefP1 = new SharedPrefsHelper(getApplicationContext(), "P1");
        prefP12 = new SharedPrefsHelper(getApplicationContext(), "P12");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(sc_id);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        UI.addWindowInsetToMargin(findViewById(R.id.container), WindowInsetsCompat.Type.navigationBars(), false, false, false, true);

        coordinatorLayout = findViewById(R.id.layout_coordinator);
        fileName = findViewById(R.id.file_name);

        findViewById(R.id.file_name_container).setOnClickListener(this);

        btnRun = findViewById(R.id.btn_run);
        btnRun.setOnClickListener(v -> {
            if (currentBuildTask != null && !currentBuildTask.isBuildFinished) {
                if (!currentBuildTask.canceled) {
                    currentBuildTask.cancelBuild();
                }
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
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
            BackgroundTasks.runIo(TaskHost.of(this), "DesignActivity", () -> FileUtil.deleteFile(projectFilePaths.projectMyscPath), () -> {
                updateBottomMenu();
                SketchwareUtil.toast(Helper.getResString(R.string.design_toast_clean_temp_done));
            }, error -> Log.e("DesignActivity", "Failed to clean temporary files", error));
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
        bottomMenu.add(Menu.NONE, 8, Menu.NONE, Helper.getResString(R.string.menu_import_xml)).setOnMenuItemClickListener(item -> {
            if (viewTabAdapter != null) {
                viewTabAdapter.showImportXmlDialog();
            }
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
                if (viewTabAdapter != null && viewTabAdapter.viewEditor != null) {
                    viewTabAdapter.viewEditor.setLayerType(
                            state != ViewPager.SCROLL_STATE_IDLE ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE,
                            null);
                }
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
                    bottomMenu.findItem(8).setVisible(true);
                    if (viewTabAdapter != null) {
                        viewTabAdapter.showHidePropertyView(true);
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_screen);
                    }
                } else if (position == 1) {
                    bottomMenu.findItem(7).setVisible(false);
                    bottomMenu.findItem(8).setVisible(false);
                    if (viewTabAdapter != null) {
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_code);
                        viewTabAdapter.showHidePropertyView(false);
                        if (eventTabAdapter != null) {
                            eventTabAdapter.refreshEvents();
                        }
                    }
                } else {
                    bottomMenu.findItem(7).setVisible(false);
                    bottomMenu.findItem(8).setVisible(false);
                    if (viewTabAdapter != null) {
                        xmlLayoutOrientation.setImageResource(R.drawable.ic_mtrl_code);
                        viewTabAdapter.showHidePropertyView(false);
                        if (componentTabAdapter != null) {
                            componentTabAdapter.refreshData();
                        }
                    }
                }
                currentTabNumber = position;
                refreshFileSelector();
                if (position == 0) {
                    if (projectFile != lastViewTabProjectFile) {
                        refreshViewTabAdapter();
                    }
                } else {
                    refreshEventTabAdapter();
                    refreshComponentTabAdapter();
                }
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
        } catch (RuntimeException e) {
            if (crashlytics != null) {
                crashlytics.log("ProjectLoader failed");
                crashlytics.recordException(e);
            }
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
                } catch (RuntimeException e) {
                    if (crashlytics != null) crashlytics.recordException(e);
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
                } catch (RuntimeException e) {
                    if (crashlytics != null) crashlytics.recordException(e);
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
                boolean hasLibraryBackup = ProjectDataManager.getLibraryManager(sc_id).hasBackup();
                boolean hasFileBackup = ProjectDataManager.getFileManager(sc_id).hasBackup();
                boolean hasResourceBackup = ProjectDataManager.getResourceManager(sc_id).hasBackup();
                boolean hasViewBackup = ProjectDataManager.getProjectDataManager(sc_id).hasViewBackup();
                boolean hasLogicBackup = ProjectDataManager.getProjectDataManager(sc_id).hasLogicBackup();
                if (hasLibraryBackup) {
                    ProjectDataManager.getLibraryManager(sc_id).loadFromBackup();
                }
                if (hasFileBackup) {
                    ProjectDataManager.getFileManager(sc_id).loadFromBackup();
                }
                if (hasResourceBackup) {
                    ProjectDataManager.getResourceManager(sc_id).loadFromBackup();
                }
                if (hasViewBackup) {
                    ProjectDataManager.getProjectDataManager(sc_id).loadViewFromBackup();
                }
                if (hasLogicBackup) {
                    ProjectDataManager.getProjectDataManager(sc_id).loadLogicFromBackup();
                }
                if (hasLibraryBackup) {
                    ProjectDataManager.getFileManager(sc_id).syncWithLibrary(ProjectDataManager.getLibraryManager(sc_id));
                    ProjectDataManager.getProjectDataManager(sc_id).removeAdmobComponents(ProjectDataManager.getLibraryManager(sc_id).getFirebaseDB());
                    ProjectDataManager.getProjectDataManager(sc_id).removeFirebaseViews(ProjectDataManager.getLibraryManager(sc_id).getAdmob(), ProjectDataManager.getFileManager(sc_id));
                    ProjectDataManager.getProjectDataManager(sc_id).removeMapViews(ProjectDataManager.getLibraryManager(sc_id).getGoogleMap(), ProjectDataManager.getFileManager(sc_id));
                }
                if (hasFileBackup || hasLibraryBackup) {
                    ProjectDataManager.getProjectDataManager(sc_id).syncWithFileManager(ProjectDataManager.getFileManager(sc_id));
                }
                if (hasResourceBackup) {
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
        String filename = Helper.getText(fileName);
        BackgroundTasks.callIoIfAlive(TaskHost.of(this), "DesignActivity", () ->
                new ProjectFilePaths(getApplicationContext(), sc_id).getFileSrc(
                        filename,
                        ProjectDataManager.getFileManager(sc_id),
                        ProjectDataManager.getProjectDataManager(sc_id),
                        ProjectDataManager.getLibraryManager(sc_id)), code -> {
            dismissLoadingDialog();
            if (code.isEmpty()) {
                SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_source));
                return;
            }
            var scheme = filename.endsWith(".xml") ? CodeViewerActivity.SCHEME_XML : CodeViewerActivity.SCHEME_JAVA;
            launchActivity(CodeViewerActivity.class, null, new Pair<>("code", code), new Pair<>("sc_id", sc_id), new Pair<>("scheme", scheme));
        }, error -> {
            Log.e("DesignActivity", "Failed to generate source code", error);
            dismissLoadingDialog();
            SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_source));
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
        String filename = Helper.getText(fileName);
        ProjectFileBean currentProjectFile = projectFile;
        BackgroundTasks.callIoIfAlive(TaskHost.of(this), "DesignActivity", () -> {
            var xmlGenerator = new LayoutGenerator(projectFilePaths.buildConfig, currentProjectFile);
            var projectDataManager = ProjectDataManager.getProjectDataManager(sc_id);
            var viewBeans = projectDataManager.getViews(filename);
            var viewFab = projectDataManager.getFabView(filename);
            xmlGenerator.setExcludeAppCompat(true);
            xmlGenerator.setViews(ProjectDataStore.getSortedRootViews(viewBeans), viewFab);
            return xmlGenerator.toXmlString();
        }, content -> {
            dismissLoadingDialog();
            launchActivity(ViewCodeEditorActivity.class, openViewCodeEditor, new Pair<>("title", filename), new Pair<>("content", content));
        }, error -> {
            Log.e("DesignActivity", "Failed to generate view code", error);
            dismissLoadingDialog();
            SketchwareUtil.toast(Helper.getResString(R.string.design_error_generate_code));
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

        /**
         * Persists all project data (views, logic, files, resources, libraries) in parallel.
         *
         * @return {@code true} if data was saved successfully
         */
        protected static boolean saveProjectDataToFiles(String sc_id) {
            ProjectDataManager.getResourceManager(sc_id).cleanupAllResources();
            ExecutorService pool = Executors.newFixedThreadPool(4);
            CompletableFuture<Boolean> fileFuture = CompletableFuture.supplyAsync(
                () -> ProjectDataManager.getFileManager(sc_id).saveToData(), pool);
            CompletableFuture<Boolean> dataFuture = CompletableFuture.supplyAsync(
                () -> ProjectDataManager.getProjectDataManager(sc_id).saveAllData(), pool);
            CompletableFuture<Boolean> resourceFuture = CompletableFuture.supplyAsync(
                () -> ProjectDataManager.getResourceManager(sc_id).saveToData(), pool);
            CompletableFuture<Boolean> libraryFuture = CompletableFuture.supplyAsync(
                () -> ProjectDataManager.getLibraryManager(sc_id).saveToData(), pool);
            CompletableFuture.allOf(fileFuture, dataFuture, resourceFuture, libraryFuture).join();
            pool.shutdown();
            return fileFuture.join() && dataFuture.join() && resourceFuture.join() && libraryFuture.join();
        }
    }

    private static class BuildTask extends BaseTask implements BuildProgressReceiver {
        public static final String ACTION_CANCEL_BUILD = "com.besome.sketch.design.ACTION_CANCEL_BUILD";
        private static final String CHANNEL_ID = "build_notification_channel";
        private final ExecutorService executorService = BackgroundTasks.createSingleThreadExecutor("DesignBuild");
        private final NotificationManager notificationManager;
        private final int notificationId = 1;
        private final MaterialButton btnRun;
        private final MaterialButton btnOptions;
        private final LinearLayout progressContainer;
        private final TextView progressText;
        private final TextView stepInfoText;
        private final LinearProgressIndicator progressBar;
        public volatile boolean canceled;
        private volatile boolean isBuildFinished;
        private boolean isShowingNotification = false;
        private long buildStartTime;

        public BuildTask(DesignActivity activity) {
            super(activity);
            notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            btnRun = activity.btnRun;
            btnOptions = activity.btnOptions;
            progressContainer = activity.findViewById(R.id.progress_container);
            progressText = activity.findViewById(R.id.progress_text);
            stepInfoText = activity.findViewById(R.id.progress_step_info);
            progressBar = activity.findViewById(R.id.progress);
        }

        public void execute() {
            onPreExecute();
            executorService.execute(this::doInBackground);
        }

        private void onPreExecute() {
            DesignActivity activity = getActivity();
            if (activity == null) return;

            postToUi(activity, () -> {
                buildStartTime = System.currentTimeMillis();
                updateRunButton(activity, true);
                activity.prefP1.put("P1I10", true);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                maybeShowNotification();
            });
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();

            try {
                if (activity == null) return;
                var q = activity.projectFilePaths;
                var sc_id = DesignActivity.sc_id;
                onProgress("Deleting temporary files...", 1);
                FileUtil.deleteFile(q.generatedFilesPath);

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
                long generateSourceStepStarted = System.currentTimeMillis();
                long copyImagesStarted = System.currentTimeMillis();
                ResourceManager resourceManager = ProjectDataManager.getResourceManager(sc_id);
                resourceManager.copyImagesToDir(q.resDirectoryPath + File.separator + "drawable-xhdpi");
                long copyImagesDuration = System.currentTimeMillis() - copyImagesStarted;
                long copySoundsStarted = System.currentTimeMillis();
                resourceManager = ProjectDataManager.getResourceManager(sc_id);
                resourceManager.copySoundsToDir(q.resDirectoryPath + File.separator + "raw");
                long copySoundsDuration = System.currentTimeMillis() - copySoundsStarted;
                long copyFontsStarted = System.currentTimeMillis();
                resourceManager = ProjectDataManager.getResourceManager(sc_id);
                resourceManager.copyFontsToDir(q.assetsPath + File.separator + "fonts");
                long copyFontsDuration = System.currentTimeMillis() - copyFontsStarted;
                Log.d("DesignActivity$BuildTask", "Step 2 timing: copied resources (images=" + copyImagesDuration
                        + " ms, sounds=" + copySoundsDuration
                        + " ms, fonts=" + copyFontsDuration + " ms)");
                long builderInitializationStarted = System.currentTimeMillis();
                ProjectBuilder builder = new ProjectBuilder(this, activity.getApplicationContext(), q);
                long builderInitializationDuration = System.currentTimeMillis() - builderInitializationStarted;
                Log.d("DesignActivity$BuildTask", "Step 2 timing: ProjectBuilder initialization took "
                        + builderInitializationDuration + " ms");

                var fileManager = ProjectDataManager.getFileManager(sc_id);
                var dataManager = ProjectDataManager.getProjectDataManager(sc_id);
                var libraryManager = ProjectDataManager.getLibraryManager(sc_id);
                long metadataInitializationStarted = System.currentTimeMillis();
                q.initializeMetadata(libraryManager, fileManager, dataManager);
                long metadataInitializationDuration = System.currentTimeMillis() - metadataInitializationStarted;
                Log.d("DesignActivity$BuildTask", "Step 2 timing: initializeMetadata took "
                        + metadataInitializationDuration + " ms");
                long builtInLibraryInformationStarted = System.currentTimeMillis();
                builder.buildBuiltInLibraryInformation();
                long builtInLibraryInformationDuration = System.currentTimeMillis() - builtInLibraryInformationStarted;
                Log.d("DesignActivity$BuildTask", "Step 2 timing: buildBuiltInLibraryInformation took "
                        + builtInLibraryInformationDuration + " ms, builtInLibraryCount="
                        + builder.getBuiltInLibraryManager().getLibraries().size());
                long generateProjectFilesStarted = System.currentTimeMillis();
                q.generateProjectFiles(fileManager, dataManager, libraryManager, builder.getBuiltInLibraryManager());
                long generateProjectFilesDuration = System.currentTimeMillis() - generateProjectFilesStarted;
                Log.d("DesignActivity$BuildTask", "Step 2 timing: generateProjectFiles took "
                        + generateProjectFilesDuration + " ms");
                long incrementalPrecheckStarted = System.currentTimeMillis();
                pro.sketchware.core.IncrementalBuildCache buildCache =
                        new pro.sketchware.core.IncrementalBuildCache(q.binDirectoryPath);
                buildCache.load();
                String buildClasspath = builder.getClasspath();
                boolean compiledClassesAvailable = new File(q.compiledClassesPath).exists()
                        && !FileUtil.listFilesRecursively(new File(q.compiledClassesPath), ".class").isEmpty();
                boolean cacheFileExists = buildCache.hasCacheFile();
                boolean proguardShrinkingEnabled = builder.proguard.isShrinkingEnabled();
                boolean classpathChanged = buildCache.isClasspathChanged(buildClasspath);
                boolean cacheMigrationRequired = buildCache.requiresFullRebuildMigration();
                boolean incrementalMode = compiledClassesAvailable
                        && cacheFileExists
                        && !proguardShrinkingEnabled
                        && !classpathChanged
                        && !cacheMigrationRequired;
                Log.d("DesignActivity$BuildTask", "Incremental build precheck: mode=" + incrementalMode
                        + ", compiledClassesAvailable=" + compiledClassesAvailable
                        + ", cacheFileExists=" + cacheFileExists
                        + ", proguardShrinkingEnabled=" + proguardShrinkingEnabled
                        + ", classpathChanged=" + classpathChanged
                        + ", cacheMigrationRequired=" + cacheMigrationRequired
                        + ", classpathHash=" + Integer.toHexString(buildClasspath.hashCode())
                        + ", classpathLength=" + buildClasspath.length());
                Log.d("DesignActivity$BuildTask", "Step 2 timing: build cache load + classpath + incremental precheck took "
                        + (System.currentTimeMillis() - incrementalPrecheckStarted) + " ms");
                builder.preloadedBuildCache = buildCache;
                long prepareBuildDirectoriesStarted = System.currentTimeMillis();
                if (incrementalMode) {
                    Log.d("DesignActivity$BuildTask", "Build cache strategy: incremental mode, cleaning only R.java directory");
                    q.cleanRJavaOnly();
                } else {
                    Log.d("DesignActivity$BuildTask", "Build cache strategy: full rebuild, cleaning bin and R.java directories");
                    q.cleanBuildCache();
                }
                q.prepareBuildDirectories();
                Log.d("DesignActivity$BuildTask", "Step 2 timing: cache cleanup + prepareBuildDirectories took "
                        + (System.currentTimeMillis() - prepareBuildDirectoriesStarted) + " ms");
                Log.d("DesignActivity$BuildTask", "Step 2 total timing: "
                        + (System.currentTimeMillis() - generateSourceStepStarted) + " ms");
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

                postToUi(activity, activity::installBuiltApk);
            } catch (MissingFileException e) {
                postToUi(activity, () -> {
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
                postToUi(activity, () -> activity.indicateCompileErrorOccurred(simpleException.getMessage()));
            } catch (Throwable tr) {
                LogUtil.e("DesignActivity$BuildTask", "Failed to build project", tr);
                postToUi(activity, () -> activity.indicateCompileErrorOccurred(Log.getStackTraceString(tr)));
            } finally {
                onPostExecute(activity);
            }
        }

        @Override
        public void onProgress(String progress, int step) {
            int totalSteps = 20;

            DesignActivity activity = getActivity();
            if (activity == null) return;

            postToUi(activity, () -> {
                progressBar.setIndeterminate(step == -1);
                if (!canceled) {
                    updateNotification(progress + " (" + step + " / " + totalSteps + ")");
                }
                progressText.setText(progress);
                var progressInt = (step * 100) / totalSteps;
                progressBar.setProgress(progressInt, true);

                long elapsed = (System.currentTimeMillis() - buildStartTime) / 1000;
                String elapsedStr = String.format("%d:%02d", elapsed / 60, elapsed % 60);
                if (step >= 1) {
                    stepInfoText.setText(step + "/" + totalSteps + " · " + elapsedStr);
                } else {
                    stepInfoText.setText(elapsedStr);
                }

                Log.d("DesignActivity$BuildTask", step + " / " + totalSteps);
            });
        }

        private void onPostExecute(DesignActivity activity) {
            isBuildFinished = true;
            executorService.shutdown();
            if (isShowingNotification) {
                notificationManager.cancel(notificationId);
                isShowingNotification = false;
            }
            if (activity == null) return;

            postToUi(activity, () -> {
                if (activity.currentBuildTask == this) {
                    activity.currentBuildTask = null;
                }
                updateRunButton(activity, false);
                activity.updateBottomMenu();
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
                postToUi(activity, () -> activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            }
        }

        private boolean hasNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                DesignActivity activity = getActivity();
                return activity != null
                        && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        private void maybeShowNotification() {
            DesignActivity activity = getActivity();
            if (activity == null) return;
            if (!hasNotificationPermission()) return;

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
            if (!hasNotificationPermission()) return;

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

        private void postToUi(DesignActivity activity, Runnable action) {
            if (activity == null || action == null) {
                return;
            }
            TaskHost.of(activity).postToUi(action);
        }

        private void updateRunButton(Context context, boolean isRunning) {
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
            DesignActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.showLoadingDialog();
            BackgroundTasks.runIoIfAlive(TaskHost.of(activity), "DesignActivity$ProjectLoader", this::doInBackground, () -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity == null) {
                    return;
                }
                currentActivity.updateBottomMenu();
                currentActivity.refresh();
                currentActivity.dismissLoadingDialog();
                if (savedInstanceState == null) {
                    currentActivity.checkForUnsavedProjectData();
                }
            }, error -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity != null) {
                    currentActivity.dismissLoadingDialog();
                }
            });
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                activity.loadProject(savedInstanceState != null);
            }
        }
    }

    private static class DiscardChangesProjectCloser extends BaseTask {

        public DiscardChangesProjectCloser(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            DesignActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.showLoadingDialog();
            BackgroundTasks.runIo(TaskHost.of(activity), "DesignActivity$DiscardChangesProjectCloser", this::doInBackground, () -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity != null) {
                    currentActivity.dismissLoadingDialog();
                    currentActivity.finish();
                }
            }, error -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity != null) {
                    currentActivity.dismissLoadingDialog();
                    currentActivity.finish();
                }
            });
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                try {
                    var sc_id = DesignActivity.sc_id;
                    ResourceManager rm = ProjectDataManager.getResourceManager(sc_id);
                    if (rm.hasLazyBackup()) {
                        rm.restoreImagesFromTemp();
                        rm.restoreSoundsFromTemp();
                        rm.restoreFontsFromTemp();
                    }
                    ProjectDataManager.discardAll();
                } catch (RuntimeException e) {
                    if (activity.crashlytics != null) {
                        activity.crashlytics.log("DiscardChangesProjectCloser cleanup failed");
                        activity.crashlytics.recordException(e);
                    }
                }
            }
        }
    }

    private static class ProjectSaver extends BaseTask {

        public ProjectSaver(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            DesignActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.showLoadingDialog();
            BackgroundTasks.callIo(TaskHost.of(activity), "DesignActivity$ProjectSaver", this::doInBackground, dataSaved -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity == null) {
                    return;
                }
                if (dataSaved) {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_complete_save), SketchToast.TOAST_NORMAL).show();
                    currentActivity.saveVersionCodeInformationToProject();
                } else {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_save_failed), SketchToast.TOAST_WARNING).show();
                }
                currentActivity.dismissLoadingDialog();
            }, error -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity != null) {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_save_failed), SketchToast.TOAST_WARNING).show();
                    currentActivity.dismissLoadingDialog();
                }
            });
        }

        private boolean doInBackground() {
            var currentScId = DesignActivity.sc_id;
            boolean dataSaved = saveProjectDataToFiles(currentScId);
            if (dataSaved) {
                ProjectDataManager.getResourceManager(currentScId).deleteTempDirs();
            }
            return dataSaved;
        }
    }

    private static class SaveChangesProjectCloser extends BaseTask {

        public SaveChangesProjectCloser(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            DesignActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.showLoadingDialog();
            BackgroundTasks.callIo(TaskHost.of(activity), "DesignActivity$SaveChangesProjectCloser", this::doInBackground, dataSaved -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity == null) {
                    return;
                }
                if (dataSaved) {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_complete_save), SketchToast.TOAST_NORMAL).show();
                    currentActivity.saveVersionCodeInformationToProject();
                    currentActivity.dismissLoadingDialog();
                    currentActivity.finish();
                } else {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_save_failed), SketchToast.TOAST_WARNING).show();
                    currentActivity.dismissLoadingDialog();
                }
            }, error -> {
                DesignActivity currentActivity = getActivity();
                if (currentActivity != null) {
                    SketchToast.toast(currentActivity.getApplicationContext(), Helper.getResString(R.string.common_message_save_failed), SketchToast.TOAST_WARNING).show();
                    currentActivity.dismissLoadingDialog();
                }
            });
        }

        private boolean doInBackground() {
            var currentScId = DesignActivity.sc_id;
            boolean dataSaved = saveProjectDataToFiles(currentScId);
            if (dataSaved) {
                ProjectDataManager.getResourceManager(currentScId).deleteTempDirs();
            }
            return dataSaved;
        }
    }

    private static class UnsavedChangesSaver extends BaseTask {

        public UnsavedChangesSaver(DesignActivity activity) {
            super(activity);
        }

        public void execute() {
            DesignActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            BackgroundTasks.runIo(TaskHost.of(activity), "DesignActivity$UnsavedChangesSaver", this::doInBackground, null, null);
        }

        private void doInBackground() {
            DesignActivity activity = getActivity();
            if (activity != null) {
                var currentScId = DesignActivity.sc_id;
                ProjectDataStore ecInstance = ProjectDataManager.getProjectDataManager(currentScId);
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

    private static FirebaseCrashlytics getFirebaseCrashlytics() {
        try {
            return FirebaseCrashlytics.getInstance();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
