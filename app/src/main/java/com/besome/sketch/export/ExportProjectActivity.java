package com.besome.sketch.export;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.ZipUtil;
import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.ProjectBuilder;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectFileManager;
import pro.sketchware.core.LibraryManager;
import pro.sketchware.core.ResourceManager;
import pro.sketchware.core.ProjectListManager;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.VersionCodeValidator;
import pro.sketchware.core.MapValueHelper;
import pro.sketchware.core.ProjectFilePaths;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.CustomKeySigner;
import kellinwood.security.zipsigner.optional.LoadKeystoreException;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.hey.studios.project.proguard.ProguardHandler;
import mod.hey.studios.project.stringfog.StringfogHandler;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.build.compiler.bundle.AppBundleCompiler;
import mod.jbk.export.GetKeyStoreCredentialsDialog;
import mod.jbk.util.TestkeySignBridge;
import pro.sketchware.R;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExportProjectActivity extends BaseAppCompatActivity {

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final EncryptedFileUtil file_utility = new EncryptedFileUtil();
    /**
     * /sketchware/signed_apk
     */
    private String signed_apk_postfix;
    /**
     * /sketchware/export_src
     */
    private String export_src_postfix;
    /**
     * /sdcard/sketchware/export_src
     */
    private String export_src_full_path;
    private String export_src_filename;
    private String sc_id;
    private HashMap<String, Object> sc_metadata = null;
    private ProjectFilePaths project_metadata = null;

    private Button sign_apk_button;
    private Button export_aab_button;
    private Button export_source_button;
    private TextView sign_apk_output_path;
    private Button export_source_send_button;
    private LinearLayout sign_apk_output_stage;
    private TextView export_source_output_path;
    private LinearLayout export_source_output_stage;
    private com.airbnb.lottie.LottieAnimationView sign_apk_loading_anim;
    private com.airbnb.lottie.LottieAnimationView export_source_loading_anim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export_project);

        ImageView sign_apk_ic = findViewById(R.id.sign_apk_ic);
        ImageView export_aab_ic = findViewById(R.id.export_aab_ic);
        TextView sign_apk_title = findViewById(R.id.sign_apk_title);
        sign_apk_button = findViewById(R.id.sign_apk_button);
        ImageView export_source_ic = findViewById(R.id.export_source_ic);
        TextView export_aab_title = findViewById(R.id.export_aab_title);
        export_aab_button = findViewById(R.id.export_aab_button);
        TextView export_source_title = findViewById(R.id.export_source_title);
        sign_apk_output_path = findViewById(R.id.sign_apk_output_path);
        export_source_button = findViewById(R.id.export_source_button);
        sign_apk_output_stage = findViewById(R.id.sign_apk_output_stage);
        sign_apk_loading_anim = findViewById(R.id.sign_apk_loading_anim);
        export_source_output_path = findViewById(R.id.export_source_output_path);
        export_source_send_button = findViewById(R.id.export_source_send_button);
        export_source_output_stage = findViewById(R.id.export_source_output_stage);
        export_source_loading_anim = findViewById(R.id.export_source_loading_anim);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.layout_main_logo).setVisibility(View.GONE);
        getSupportActionBar().setTitle(Helper.getResString(R.string.myprojects_export_project_actionbar_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        if (savedInstanceState == null) {
            sc_id = getIntent().getStringExtra("sc_id");
        } else {
            sc_id = savedInstanceState.getString("sc_id");
        }

        sc_metadata = ProjectListManager.getProjectById(sc_id);
        project_metadata = new ProjectFilePaths(getApplicationContext(), SketchwarePaths.getMyscPath(sc_id), sc_metadata);

        initializeOutputDirectories();
        initializeSignApkViews();
        initializeExportSrcViews();
        initializeAppBundleExportViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdownNow();
        if (export_source_loading_anim.isAnimating()) {
            export_source_loading_anim.cancelAnimation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("sc_id", sc_id);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Sets exported signed APK file path texts' content.
     */
    private void f(String filePath) {
        sign_apk_output_stage.setVisibility(View.VISIBLE);
        sign_apk_button.setVisibility(View.GONE);
        if (sign_apk_loading_anim.isAnimating()) {
            sign_apk_loading_anim.cancelAnimation();
        }
        sign_apk_loading_anim.setVisibility(View.GONE);
        sign_apk_output_path.setText(signed_apk_postfix + File.separator + filePath);
        SketchwareUtil.toast(Helper.getResString(R.string.sign_apk_title_export_apk_file));
    }

    private void exportSrc() {
        try {
            FileUtil.deleteFile(project_metadata.projectMyscPath);

            ProjectFileManager hCVar = new ProjectFileManager(sc_id);
            ResourceManager kCVar = new ResourceManager(sc_id);
            ProjectDataStore eCVar = new ProjectDataStore(sc_id);
            LibraryManager iCVar = new LibraryManager(sc_id);
            hCVar.i();
            kCVar.s();
            eCVar.g();
            eCVar.e();
            iCVar.i();

            /* Extract project type template */
            project_metadata.extractAssetsToRes(getApplicationContext(), SketchwarePaths.getResourceZipPath(VersionCodeValidator.isValid(sc_id) ? "600" : sc_id));

            /* Start generating project files */
            ProjectBuilder builder = new ProjectBuilder(this, project_metadata);
            project_metadata.initializeMetadata(iCVar, hCVar, eCVar, ProjectFilePaths.ExportType.ANDROID_STUDIO);
            builder.buildBuiltInLibraryInformation();
            project_metadata.generateProjectFiles(hCVar, eCVar, iCVar, builder.getBuiltInLibraryManager());
            if (MapValueHelper.a(ProjectListManager.getProjectById(sc_id), "custom_icon")) {
                project_metadata.copyMipmapFolder(SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "mipmaps");
                if (MapValueHelper.a(ProjectListManager.getProjectById(sc_id), "isIconAdaptive", false)) {
                    project_metadata.createLauncherIconXml("""
                            <?xml version="1.0" encoding="utf-8"?>
                            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                            <background android:drawable="@mipmap/ic_launcher_background"/>
                            <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                            <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                            </adaptive-icon>""");
                }
            }
            project_metadata.deleteValuesV21Directory();
            kCVar.b(project_metadata.resDirectoryPath + File.separator + "drawable-xhdpi");
            kCVar.c(project_metadata.resDirectoryPath + File.separator + "raw");
            kCVar.a(project_metadata.assetsPath + File.separator + "fonts");
            project_metadata.cleanBuildCache();

            /* It makes no sense that those methods aren't static */
            FilePathUtil util = new FilePathUtil();
            File pathJava = new File(util.getPathJava(sc_id));
            File pathResources = new File(util.getPathResource(sc_id));
            File pathAssets = new File(util.getPathAssets(sc_id));
            File pathNativeLibraries = new File(util.getPathNativelibs(sc_id));

            if (pathJava.exists()) {
                FileUtil.copyDirectory(pathJava, new File(project_metadata.javaFilesPath + File.separator + project_metadata.packageNameAsFolders));
            }
            if (pathResources.exists()) {
                FileUtil.copyDirectory(pathResources, new File(project_metadata.resDirectoryPath));
            }
            String pathProguard = util.getPathProguard(sc_id);
            if (FileUtil.isExistFile(pathProguard)) {
                FileUtil.copyFile(pathProguard, project_metadata.proguardFilePath);
            }
            if (pathAssets.exists()) {
                FileUtil.copyDirectory(pathAssets, new File(project_metadata.assetsPath));
            }
            if (pathNativeLibraries.exists()) {
                FileUtil.copyDirectory(pathNativeLibraries, new File(project_metadata.generatedFilesPath, "jniLibs"));
            }

            ArrayList<String> toCompress = new ArrayList<>();
            toCompress.add(project_metadata.projectMyscPath);
            String exportedFilename = MapValueHelper.c(sc_metadata, "my_ws_name") + ".zip";

            String exportedSourcesZipPath = SketchwarePaths.getSketchwarePath() + File.separator + "export_src" + File.separator + exportedFilename;
            if (file_utility.e(exportedSourcesZipPath)) {
                file_utility.c(exportedSourcesZipPath);
            }

            ArrayList<String> toExclude = new ArrayList<>();
            if (!new File(new FilePathUtil().getPathJava(sc_id) + File.separator + "SketchApplication.java").exists()) {
                toExclude.add("SketchApplication.java");
            }
            toExclude.add("DebugActivity.java");

            new ZipUtil().createZipFile(exportedSourcesZipPath, toCompress, toExclude);
            project_metadata.prepareBuildDirectories();
            runOnUiThread(() -> initializeAfterExportedSourceViews(exportedFilename));
        } catch (Exception e) {
            runOnUiThread(() -> {
                Log.e("ProjectExporter", "While trying to export project's sources: "
                        + e.getMessage(), e);
                SketchwareUtil.showAnErrorOccurredDialog(this, Log.getStackTraceString(e));
                export_source_output_stage.setVisibility(View.GONE);
                export_source_loading_anim.setVisibility(View.GONE);
                export_source_button.setVisibility(View.VISIBLE);
            });
        }
    }

    private void initializeAppBundleExportViews() {
        export_aab_button.setOnClickListener(view -> {
            MaterialAlertDialogBuilder confirmationDialog = new MaterialAlertDialogBuilder(this);
            confirmationDialog.setTitle(Helper.getResString(R.string.export_title_important_note));
            confirmationDialog.setMessage(Helper.getResString(R.string.export_aab_sign_note));
            confirmationDialog.setIcon(R.drawable.ic_mtrl_info);

            confirmationDialog.setPositiveButton(Helper.getResString(R.string.export_btn_understood), (v, which) -> {
                showAabSigningDialog();
                v.dismiss();
            });
            confirmationDialog.show();
        });
    }

    private void showAabSigningDialog() {
        GetKeyStoreCredentialsDialog credentialsDialog = new GetKeyStoreCredentialsDialog(this,
                R.drawable.ic_mtrl_key, Helper.getResString(R.string.export_aab_sign_dialog_title), Helper.getResString(R.string.export_aab_sign_dialog_desc));
        credentialsDialog.setListener(credentials -> {
            BuildingAsyncTask task = new BuildingAsyncTask(this, ProjectFilePaths.ExportType.AAB);
            task.enableAppBundleBuild();
            if (credentials != null) {
                if (credentials.isForSigningWithTestkey()) {
                    task.setSignWithTestkey(true);
                } else {
                    task.configureResultJarSigning(
                            SketchwarePaths.getKeystoreFilePath(),
                            credentials.getKeyStorePassword().toCharArray(),
                            credentials.getKeyAlias(),
                            credentials.getKeyPassword().toCharArray(),
                            credentials.getSigningAlgorithm()
                    );
                }
            }
            task.execute();
        });
        credentialsDialog.show();
    }

    /**
     * Initialize Export to Android Studio views
     */
    private void initializeExportSrcViews() {
        export_source_loading_anim.setVisibility(View.GONE);
        export_source_output_stage.setVisibility(View.GONE);
        export_source_button.setOnClickListener(v -> {
            export_source_button.setVisibility(View.GONE);
            export_source_output_stage.setVisibility(View.GONE);
            export_source_loading_anim.setVisibility(View.VISIBLE);
            export_source_loading_anim.playAnimation();
            backgroundExecutor.execute(() -> {
                try {
                    exportSrc();
                } catch (Exception e) {
                    Log.e("ExportProjectActivity", "Failed to export source", e);
                }
            });
        });
        export_source_send_button.setOnClickListener(v -> shareExportedSourceCode());
    }

    /**
     * Initialize APK Export views
     */
    private void initializeSignApkViews() {
        sign_apk_loading_anim.setVisibility(View.GONE);
        sign_apk_output_stage.setVisibility(View.GONE);

        sign_apk_button.setOnClickListener(view -> {
            MaterialAlertDialogBuilder confirmationDialog = new MaterialAlertDialogBuilder(this);
            confirmationDialog.setTitle(Helper.getResString(R.string.export_title_important_note));
            confirmationDialog.setMessage(Helper.getResString(R.string.export_apk_sign_note));
            confirmationDialog.setIcon(R.drawable.ic_mtrl_info);

            confirmationDialog.setPositiveButton(Helper.getResString(R.string.export_btn_understood), (v, which) -> {
                showApkSigningDialog();
                v.dismiss();
            });
            confirmationDialog.show();
        });
    }

    private void showApkSigningDialog() {
        GetKeyStoreCredentialsDialog credentialsDialog = new GetKeyStoreCredentialsDialog(this,
                R.drawable.ic_mtrl_key,
                Helper.getResString(R.string.export_apk_sign_dialog_title),
                Helper.getResString(R.string.export_apk_sign_dialog_desc));
        credentialsDialog.setListener(credentials -> {
            sign_apk_button.setVisibility(View.GONE);
            sign_apk_output_stage.setVisibility(View.GONE);
            sign_apk_loading_anim.setVisibility(View.VISIBLE);
            sign_apk_loading_anim.playAnimation();

            BuildingAsyncTask task = new BuildingAsyncTask(this, ProjectFilePaths.ExportType.SIGN_APP);
            if (credentials != null) {
                if (credentials.isForSigningWithTestkey()) {
                    task.setSignWithTestkey(true);
                } else {
                    task.configureResultJarSigning(
                            SketchwarePaths.getKeystoreFilePath(),
                            credentials.getKeyStorePassword().toCharArray(),
                            credentials.getKeyAlias(),
                            credentials.getKeyPassword().toCharArray(),
                            credentials.getSigningAlgorithm()
                    );
                }
            } else {
                task.disableResultJarSigning();
            }
            task.execute();
        });
        credentialsDialog.show();
    }

    private void initializeOutputDirectories() {
        signed_apk_postfix = File.separator + "sketchware" + File.separator + "signed_apk";
        export_src_postfix = File.separator + "sketchware" + File.separator + "export_src";
        /* /sdcard/sketchware/signed_apk */
        String signed_apk_full_path = SketchwarePaths.getSketchwarePath() + File.separator + "signed_apk";
        export_src_full_path = SketchwarePaths.getSketchwarePath() + File.separator + "export_src";

        /* Check if they exist, if not, create them */
        file_utility.f(signed_apk_full_path);
        file_utility.f(export_src_full_path);
    }

    private void shareExportedSourceCode() {
        if (!export_src_filename.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("plain/text");
            intent.putExtra(Intent.EXTRA_SUBJECT, Helper.getResString(R.string.myprojects_export_src_title_email_subject, export_src_filename));
            intent.putExtra(Intent.EXTRA_TEXT, Helper.getResString(R.string.myprojects_export_src_title_email_body, export_src_filename));
            String filePath = export_src_full_path + File.separator + export_src_filename;
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", new File(filePath)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(Intent.createChooser(intent, Helper.getResString(R.string.myprojects_export_src_chooser_title_email)));
        }
    }

    /**
     * Set content of exported source views
     */
    private void initializeAfterExportedSourceViews(String exportedSrcFilename) {
        export_src_filename = exportedSrcFilename;
        export_source_loading_anim.cancelAnimation();
        export_source_loading_anim.setVisibility(View.GONE);
        export_source_output_stage.setVisibility(View.VISIBLE);
        export_source_output_path.setText(export_src_postfix + File.separator + export_src_filename);
    }

    private static class BuildingAsyncTask extends BaseAsyncTask implements DialogInterface.OnCancelListener, BuildProgressReceiver {
        private final WeakReference<ExportProjectActivity> activity;
        private final ProjectFilePaths project_metadata;
        private final WeakReference<LottieAnimationView> loading_sign_apk;
        private final ProjectFilePaths.ExportType exportType;

        private ProjectBuilder builder;
        private boolean canceled = false;
        private boolean buildingAppBundle = false;
        private String signingKeystorePath = null;
        private char[] signingKeystorePassword = null;
        private String signingAliasName = null;
        private char[] signingAliasPassword = null;
        private String signingAlgorithm = null;
        private boolean signWithTestkey = false;

        public BuildingAsyncTask(ExportProjectActivity exportProjectActivity, ProjectFilePaths.ExportType exportType) {
            super(exportProjectActivity);
            this.exportType = exportType;
            activity = new WeakReference<>(exportProjectActivity);
            project_metadata = exportProjectActivity.project_metadata;
            loading_sign_apk = new WeakReference<>(exportProjectActivity.sign_apk_loading_anim);
            // Register as AsyncTask with dialog to Activity
            exportProjectActivity.addTask(this);
            // Make a simple ProgressDialog show and set its OnCancelListener
            exportProjectActivity.a((DialogInterface.OnCancelListener) this);
            // Allow user to use back button
            exportProjectActivity.progressDialog.setCancelable(false);
        }

        /**
         * pro.sketchware.core.BaseAsyncTask's doWork() - runs in background thread
         */
        @Override // pro.sketchware.core.BaseAsyncTask
        public void doWork() {
            if (canceled) {
                cancel(true);
                return;
            }

            var act = activity.get();
            if (act == null) return;
            String sc_id = act.sc_id;

            try {
                publishProgress(Helper.getResString(R.string.build_progress_deleting_temp));
                FileUtil.deleteFile(project_metadata.projectMyscPath);

                publishProgress(Helper.getResString(R.string.design_run_title_ready_to_build));
                EncryptedFileUtil oBVar = new EncryptedFileUtil();
                /* Check if /Internal storage/sketchware/signed_apk/ exists */
                if (!oBVar.e(SketchwarePaths.getSignedApkPath())) {
                    /* Doesn't exist yet, let's create it */
                    oBVar.f(SketchwarePaths.getSignedApkPath());
                }
                ProjectFileManager hCVar = new ProjectFileManager(sc_id);
                ResourceManager kCVar = new ResourceManager(sc_id);
                ProjectDataStore eCVar = new ProjectDataStore(sc_id);
                LibraryManager iCVar = new LibraryManager(sc_id);
                hCVar.i();
                kCVar.s();
                eCVar.g();
                eCVar.e();
                iCVar.i();
                if (canceled) {
                    cancel(true);
                    return;
                }
                File outputFile = new File(getCorrectResultFilename(project_metadata.releaseApkPath));
                if (outputFile.exists()) {
                    if (!outputFile.delete()) {
                        throw new IllegalStateException("Couldn't delete file " + outputFile.getAbsolutePath());
                    }
                }
                project_metadata.createBuildDirectories(getContext());
                if (canceled) {
                    cancel(true);
                    return;
                }
                project_metadata.extractAssetsToRes(getContext(), SketchwarePaths.getResourceZipPath("600"));
                if (canceled) {
                    cancel(true);
                    return;
                }
                if (MapValueHelper.a(ProjectListManager.getProjectById(sc_id), "custom_icon")) {
                    project_metadata.copyMipmapFolder(SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "mipmaps");
                    if (MapValueHelper.a(ProjectListManager.getProjectById(sc_id), "isIconAdaptive", false)) {
                        project_metadata.createLauncherIconXml("""
                                <?xml version="1.0" encoding="utf-8"?>
                                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android" >
                                <background android:drawable="@mipmap/ic_launcher_background"/>
                                <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
                                <monochrome android:drawable="@mipmap/ic_launcher_monochrome"/>
                                </adaptive-icon>""");
                    } else {
                        project_metadata.copyAppIcon(SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "icon.png");
                    }
                }
                project_metadata.deleteValuesV21Directory();
                kCVar.b(project_metadata.resDirectoryPath + File.separator + "drawable-xhdpi");
                kCVar.c(project_metadata.resDirectoryPath + File.separator + "raw");
                kCVar.a(project_metadata.assetsPath + File.separator + "fonts");

                builder = new ProjectBuilder(this, getContext(), project_metadata);
                builder.setBuildAppBundle(buildingAppBundle);

                project_metadata.initializeMetadata(iCVar, hCVar, eCVar, exportType);
                builder.buildBuiltInLibraryInformation();
                project_metadata.generateProjectFiles(hCVar, eCVar, iCVar, builder.getBuiltInLibraryManager());
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Check AAPT/AAPT2 */
                publishProgress(Helper.getResString(R.string.build_progress_extracting_aapt2));
                builder.maybeExtractAapt2();
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Check built-in libraries */
                publishProgress(Helper.getResString(R.string.build_progress_extracting_libraries));
                BuiltInLibraries.extractCompileAssets(this);
                if (canceled) {
                    cancel(true);
                    return;
                }

                builder.buildBuiltInLibraryInformation();

                publishProgress(Helper.getResString(R.string.build_progress_aapt2_running));
                builder.compileResources();
                if (canceled) {
                    cancel(true);
                    return;
                }

                KotlinCompilerBridge.compileKotlinCodeIfPossible(this, builder);
                if (canceled) {
                    cancel(true);
                    return;
                }

                publishProgress(Helper.getResString(R.string.build_progress_java_compiling));
                builder.compileJavaCode();
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Encrypt Strings in classes if enabled */
                StringfogHandler stringfogHandler = new StringfogHandler(project_metadata.sc_id);
                stringfogHandler.start(this, builder);
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Obfuscate classes if enabled */
                ProguardHandler proguardHandler = new ProguardHandler(project_metadata.sc_id);
                proguardHandler.start(this, builder);
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Create DEX file(s) */
                publishProgress(builder.getDxRunningText());
                builder.createDexFilesFromClasses();
                if (canceled) {
                    cancel(true);
                    return;
                }

                /* Merge DEX file(s) with libraries' dexes */
                publishProgress(Helper.getResString(R.string.build_progress_merging_dex));
                builder.getDexFilesReady();
                if (canceled) {
                    cancel(true);
                    return;
                }

                if (buildingAppBundle) {
                    AppBundleCompiler compiler = new AppBundleCompiler(builder);
                    publishProgress(Helper.getResString(R.string.build_progress_creating_module));
                    compiler.createModuleMainArchive();
                    publishProgress(Helper.getResString(R.string.build_progress_building_bundle));
                    compiler.buildBundle();

                    /* Sign the generated .aab file */
                    publishProgress(Helper.getResString(R.string.build_progress_signing_bundle));

                    String createdBundlePath = AppBundleCompiler.getDefaultAppBundleOutputFile(project_metadata).getAbsolutePath();
                    String signedAppBundleDirectoryPath = FileUtil.getExternalStorageDir()
                            + File.separator + "sketchware"
                            + File.separator + "signed_aab";
                    FileUtil.makeDir(signedAppBundleDirectoryPath);
                    String outputPath = signedAppBundleDirectoryPath + File.separator +
                            Uri.fromFile(new File(createdBundlePath)).getLastPathSegment();

                    if (signWithTestkey) {
                        ZipSigner signer = new ZipSigner();
                        signer.setKeymode(ZipSigner.KEY_TESTKEY);
                        signer.signZip(createdBundlePath, outputPath);
                    } else if (isResultJarSigningEnabled()) {
                        Security.addProvider(new BouncyCastleProvider());
                        CustomKeySigner.signZip(
                                new ZipSigner(),
                                signingKeystorePath,
                                signingKeystorePassword,
                                signingAliasName,
                                signingAliasPassword,
                                signingAlgorithm,
                                createdBundlePath,
                                outputPath
                        );
                    } else {
                        FileUtil.copyFile(createdBundlePath, getCorrectResultFilename(outputPath));
                    }
                } else {
                    publishProgress(Helper.getResString(R.string.build_progress_building_apk));
                    builder.buildApk();
                    if (canceled) {
                        cancel(true);
                        return;
                    }

                    publishProgress(Helper.getResString(R.string.build_progress_aligning_apk));
                    builder.runZipalign(builder.projectFilePaths.unsignedUnalignedApkPath, builder.projectFilePaths.unsignedAlignedApkPath);
                    if (canceled) {
                        cancel(true);
                        return;
                    }

                    publishProgress(Helper.getResString(R.string.build_progress_signing_apk));
                    String outputLocation = getCorrectResultFilename(builder.projectFilePaths.releaseApkPath);
                    if (signWithTestkey) {
                        TestkeySignBridge.signWithTestkey(builder.projectFilePaths.unsignedAlignedApkPath, outputLocation);
                    } else if (isResultJarSigningEnabled()) {
                        Security.addProvider(new BouncyCastleProvider());
                        CustomKeySigner.signZip(
                                new ZipSigner(),
                                SketchwarePaths.getKeystoreFilePath(),
                                signingKeystorePassword,
                                signingAliasName,
                                signingAliasPassword,
                                signingAlgorithm,
                                builder.projectFilePaths.unsignedAlignedApkPath,
                                outputLocation
                        );
                    } else {
                        FileUtil.copyFile(builder.projectFilePaths.unsignedAlignedApkPath, outputLocation);
                    }
                }
            } catch (Throwable throwable) {
                var errorAct = activity.get();
                if (throwable instanceof LoadKeystoreException &&
                        "Incorrect password, or integrity check failed.".equals(throwable.getMessage())) {
                    if (errorAct != null) errorAct.runOnUiThread(() -> SketchwareUtil.showAnErrorOccurredDialog(errorAct,
                            "Either an incorrect password was entered, or your key store is corrupt."));
                } else {
                    Log.e("AppExporter", throwable.getMessage(), throwable);
                    if (errorAct != null) errorAct.runOnUiThread(() -> SketchwareUtil.showAnErrorOccurredDialog(errorAct,
                            Log.getStackTraceString(throwable)));
                }

                cancel(true);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            var act = activity.get();
            if (act == null) return;
            if (!act.progressDialog.isCancelable()) {
                act.progressDialog.setCancelable(true);
                act.a((DialogInterface.OnCancelListener) this);
                publishProgress(Helper.getResString(R.string.build_progress_canceling));
                canceled = true;
            }
        }

        @Override
        public void onCancelled() {
            super.onCancelled();
            builder = null;
            var act = activity.get();
            if (act == null) return;
            act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Dismiss the ProgressDialog
            act.i();
            act.sign_apk_output_stage.setVisibility(View.GONE);
            LottieAnimationView loading_sign_apk = this.loading_sign_apk.get();
            if (loading_sign_apk != null) {
                if (loading_sign_apk.isAnimating()) {
                    loading_sign_apk.cancelAnimation();
                }
                loading_sign_apk.setVisibility(View.GONE);
            }
            act.sign_apk_button.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPreExecute() {
            super.onPreExecute();
            var act = activity.get();
            if (act == null) return;
            act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override // android.os.AsyncTask
        public void onProgressUpdate(String... strArr) {
            super.onProgressUpdate(strArr);
            // Update the ProgressDialog's text
            var act = activity.get();
            if (act == null) return;
            act.a(strArr[0]);
        }

        /**
         * pro.sketchware.core.BaseAsyncTask's onSuccess() - called on the UI thread after successful doWork()
         */
        @Override // pro.sketchware.core.BaseAsyncTask
        public void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Dismiss the ProgressDialog
            act.i();

            if (new File(getCorrectResultFilename(project_metadata.releaseApkPath)).exists()) {
                act.f(getCorrectResultFilename(project_metadata.projectName + "_release.apk"));
            }

            String aabFilename = getCorrectResultFilename(project_metadata.projectName + ".aab");
            if (buildingAppBundle && new File(Environment.getExternalStorageDirectory(), "sketchware" + File.separator + "signed_aab" + File.separator + aabFilename).exists()) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(act);
                dialog.setIcon(R.drawable.open_box_48);
                dialog.setTitle(Helper.getResString(R.string.export_aab_finished_title));
                dialog.setMessage(String.format(Helper.getResString(R.string.export_aab_finished_message), aabFilename));
                dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
                dialog.show();
            }
        }

        /**
         * Called by pro.sketchware.core.BaseAsyncTask if doWork() returned a non-empty String,
         * ergo, an error occurred.
         */
        @Override // pro.sketchware.core.BaseAsyncTask
        public void onError(String str) {
            var act = activity.get();
            if (act == null) return;
            act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // Dismiss the ProgressDialog
            act.i();
            SketchwareUtil.showAnErrorOccurredDialog(act, str);
            act.sign_apk_output_stage.setVisibility(View.GONE);
            LottieAnimationView loading_sign_apk = this.loading_sign_apk.get();
            if (loading_sign_apk != null) {
                if (loading_sign_apk.isAnimating()) {
                    loading_sign_apk.cancelAnimation();
                }
                loading_sign_apk.setVisibility(View.GONE);
            }
            act.sign_apk_button.setVisibility(View.VISIBLE);
        }

        public void enableAppBundleBuild() {
            buildingAppBundle = true;
        }

        /**
         * Configures parameters for JAR signing the result.
         * <p></p>
         * If {@link #signWithTestkey} is <code>true</code> though, the result will be signed
         * regardless of {@link #configureResultJarSigning(String, char[], String, char[], String)} and {@link #disableResultJarSigning()} calls.
         */
        public void configureResultJarSigning(String keystorePath, char[] keystorePassword, String aliasName, char[] aliasPassword, String signatureAlgorithm) {
            signingKeystorePath = keystorePath;
            signingKeystorePassword = keystorePassword;
            signingAliasName = aliasName;
            signingAliasPassword = aliasPassword;
            signingAlgorithm = signatureAlgorithm;
        }

        /**
         * Whether to sign the result with testkey or not.
         * Note that this value will always be prioritized over values set with {@link #configureResultJarSigning(String, char[], String, char[], String)}.
         */
        public void setSignWithTestkey(boolean signWithTestkey) {
            this.signWithTestkey = signWithTestkey;
        }

        /**
         * Disables JAR signing of the result. Equivalent to calling {@link #configureResultJarSigning(String, char[], String, char[], String)}
         * with <code>null</code> parameters.
         * <p></p>
         * If {@link #signWithTestkey} is <code>true</code> though, the result will be signed
         * regardless of {@link #configureResultJarSigning(String, char[], String, char[], String)} and {@link #disableResultJarSigning()} calls.
         */
        public void disableResultJarSigning() {
            signingKeystorePath = null;
            signingKeystorePassword = null;
            signingAliasName = null;
            signingAliasPassword = null;
            signingAlgorithm = null;
        }

        public boolean isResultJarSigningEnabled() {
            return signingKeystorePath != null && signingKeystorePassword != null &&
                    signingAliasName != null && signingAliasPassword != null && signingAlgorithm != null;
        }

        private String getCorrectResultFilename(String oldFormatFilename) {
            if (!isResultJarSigningEnabled() && !signWithTestkey) {
                if (buildingAppBundle) {
                    return oldFormatFilename.replace(".aab", ".unsigned.aab");
                } else {
                    return oldFormatFilename.replace("_release", "_release.unsigned");
                }
            } else {
                return oldFormatFilename;
            }
        }

        @Override
        public void onProgress(String progress, int step) {
            publishProgress(progress);
        }
    }
}
