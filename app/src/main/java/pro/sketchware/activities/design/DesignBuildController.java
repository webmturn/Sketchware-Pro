package pro.sketchware.activities.design;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

import pro.sketchware.R;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.build.BuildProgressReceiver;
import pro.sketchware.core.build.ProjectBuilder;
import pro.sketchware.core.build.compiler.KotlinCompilerBridge;
import pro.sketchware.core.exception.MissingFileException;
import pro.sketchware.core.exception.SimpleException;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.ProguardHandler;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.project.StringfogHandler;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.util.LogUtil;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.ThemeUtils;
import pro.sketchware.util.library.BuiltInLibraries;

/**
 * Runs the device-side project build and owns its progress UI, notification, and cancellation.
 *
 * <p>The build stages and their order are intentionally unchanged from the former nested
 * DesignActivity BuildTask. The controller keeps only a weak reference to the Activity.</p>
 */
final class DesignBuildController implements BuildProgressReceiver {
    public static final String ACTION_CANCEL_BUILD = "pro.sketchware.activities.design.ACTION_CANCEL_BUILD";
    private final WeakReference<DesignActivity> activityRef;
    private final String projectId;
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

    DesignBuildController(DesignActivity activity, String projectId) {
        activityRef = new WeakReference<>(activity);
        this.projectId = projectId;
        notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        btnRun = activity.findViewById(R.id.btn_run);
        btnOptions = activity.findViewById(R.id.btn_options);
        progressContainer = activity.findViewById(R.id.progress_container);
        progressText = activity.findViewById(R.id.progress_text);
        stepInfoText = activity.findViewById(R.id.progress_step_info);
        progressBar = activity.findViewById(R.id.progress);
    }

    private DesignActivity getActivity() {
        return activityRef.get();
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
            activity.onBuildControllerStarted();

            maybeShowNotification();
        });
    }

    private void doInBackground() {
        DesignActivity activity = getActivity();

        try {
            if (activity == null) return;
            var q = activity.getProjectFilePaths();
            var sc_id = projectId;
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
            Log.d("DesignBuildController", "Step 2 timing: copied resources (images=" + copyImagesDuration
                    + " ms, sounds=" + copySoundsDuration
                    + " ms, fonts=" + copyFontsDuration + " ms)");
            long builderInitializationStarted = System.currentTimeMillis();
            ProjectBuilder builder = new ProjectBuilder(this, activity.getApplicationContext(), q);
            long builderInitializationDuration = System.currentTimeMillis() - builderInitializationStarted;
            Log.d("DesignBuildController", "Step 2 timing: ProjectBuilder initialization took "
                    + builderInitializationDuration + " ms");

            var fileManager = ProjectDataManager.getFileManager(sc_id);
            var dataManager = ProjectDataManager.getProjectDataManager(sc_id);
            var libraryManager = ProjectDataManager.getLibraryManager(sc_id);
            long metadataInitializationStarted = System.currentTimeMillis();
            q.initializeMetadata(libraryManager, fileManager, dataManager);
            long metadataInitializationDuration = System.currentTimeMillis() - metadataInitializationStarted;
            Log.d("DesignBuildController", "Step 2 timing: initializeMetadata took "
                    + metadataInitializationDuration + " ms");
            long builtInLibraryInformationStarted = System.currentTimeMillis();
            builder.buildBuiltInLibraryInformation();
            long builtInLibraryInformationDuration = System.currentTimeMillis() - builtInLibraryInformationStarted;
            Log.d("DesignBuildController", "Step 2 timing: buildBuiltInLibraryInformation took "
                    + builtInLibraryInformationDuration + " ms, builtInLibraryCount="
                    + builder.getBuiltInLibraryManager().getLibraries().size());
            long generateProjectFilesStarted = System.currentTimeMillis();
            q.generateProjectFiles(fileManager, dataManager, libraryManager, builder.getBuiltInLibraryManager());
            long generateProjectFilesDuration = System.currentTimeMillis() - generateProjectFilesStarted;
            Log.d("DesignBuildController", "Step 2 timing: generateProjectFiles took "
                    + generateProjectFilesDuration + " ms");
            long incrementalPrecheckStarted = System.currentTimeMillis();
            pro.sketchware.core.build.IncrementalBuildCache buildCache =
                    new pro.sketchware.core.build.IncrementalBuildCache(q.binDirectoryPath);
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
            Log.d("DesignBuildController", "Incremental build precheck: mode=" + incrementalMode
                    + ", compiledClassesAvailable=" + compiledClassesAvailable
                    + ", cacheFileExists=" + cacheFileExists
                    + ", proguardShrinkingEnabled=" + proguardShrinkingEnabled
                    + ", classpathChanged=" + classpathChanged
                    + ", cacheMigrationRequired=" + cacheMigrationRequired
                    + ", classpathHash=" + Integer.toHexString(buildClasspath.hashCode())
                    + ", classpathLength=" + buildClasspath.length());
            Log.d("DesignBuildController", "Step 2 timing: build cache load + classpath + incremental precheck took "
                    + (System.currentTimeMillis() - incrementalPrecheckStarted) + " ms");
            builder.preloadedBuildCache = buildCache;
            long prepareBuildDirectoriesStarted = System.currentTimeMillis();
            if (incrementalMode) {
                Log.d("DesignBuildController", "Build cache strategy: incremental mode, cleaning only R.java directory");
                q.cleanRJavaOnly();
            } else {
                Log.d("DesignBuildController", "Build cache strategy: full rebuild, cleaning bin and R.java directories");
                q.cleanBuildCache();
            }
            q.prepareBuildDirectories();
            Log.d("DesignBuildController", "Step 2 timing: cache cleanup + prepareBuildDirectories took "
                    + (System.currentTimeMillis() - prepareBuildDirectoriesStarted) + " ms");
            Log.d("DesignBuildController", "Step 2 total timing: "
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
            LogUtil.e("DesignBuildController", "Failed to build project", tr);
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

            Log.d("DesignBuildController", step + " / " + totalSteps);
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
            activity.onBuildControllerFinished(this);
            updateRunButton(activity, false);
            activity.updateBottomMenu();
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
    }

    boolean isFinished() {
        return isBuildFinished;
    }

    boolean isCanceled() {
        return canceled;
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

        Intent cancelIntent = new Intent(DesignBuildController.ACTION_CANCEL_BUILD);
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
        btnRun.setText(Helper.getResString(isRunning ? R.string.design_stop : R.string.design_run));
        btnOptions.setEnabled(!isRunning);
        progressContainer.setVisibility(isRunning ? View.VISIBLE : View.GONE);
    }
}
