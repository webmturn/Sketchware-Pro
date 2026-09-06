package pro.sketchware.activities.design;

import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.R;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.util.Helper;
import pro.sketchware.util.SketchToast;

/**
 * Coordinates loading, saving, closing, and temporary backup of the active design project.
 *
 * <p>The controller keeps only a weak Activity reference so background work cannot retain a
 * destroyed editor. UI rendering remains in {@link DesignActivity}; project persistence and its
 * asynchronous sequencing live here.</p>
 */
final class DesignProjectController {

    private final WeakReference<DesignActivity> activityRef;
    private final String projectId;

    DesignProjectController(DesignActivity activity, String projectId) {
        activityRef = new WeakReference<>(activity);
        this.projectId = projectId;
    }

    void load(Bundle savedInstanceState) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.showProjectLoadingDialog();
        BackgroundTasks.runIoIfAlive(
                TaskHost.of(activity),
                "DesignActivity$ProjectLoader",
                () -> loadInBackground(savedInstanceState),
                () -> onProjectLoaded(savedInstanceState),
                error -> dismissLoadingDialog());
    }

    void save() {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.showProjectLoadingDialog();
        BackgroundTasks.callIo(
                TaskHost.of(activity),
                "DesignActivity$ProjectSaver",
                this::saveProjectDataAndCleanup,
                dataSaved -> onProjectSaved(dataSaved, false),
                error -> onProjectSaveFailed());
    }

    void saveAndClose() {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.showProjectLoadingDialog();
        BackgroundTasks.callIo(
                TaskHost.of(activity),
                "DesignActivity$SaveChangesProjectCloser",
                this::saveProjectDataAndCleanup,
                dataSaved -> onProjectSaved(dataSaved, true),
                error -> onProjectSaveFailed());
    }

    void discardAndClose() {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.showProjectLoadingDialog();
        BackgroundTasks.runIo(
                TaskHost.of(activity),
                "DesignActivity$DiscardChangesProjectCloser",
                this::discardInBackground,
                this::finishAfterDiscard,
                error -> finishAfterDiscard());
    }

    void saveUnsavedChanges() {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        BackgroundTasks.runIo(
                TaskHost.of(activity),
                "DesignActivity$UnsavedChangesSaver",
                this::saveUnsavedChangesInBackground,
                null,
                this::recordSaveUnsavedChangesFailure);
    }

    private void recordSaveUnsavedChangesFailure(Throwable error) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        RuntimeException runtimeError = error instanceof RuntimeException runtimeException
                ? runtimeException
                : new RuntimeException(error);
        activity.recordProjectControllerFailure("UnsavedChangesSaver failed", runtimeError);
    }

    private DesignActivity getActivity() {
        return activityRef.get();
    }

    private void loadInBackground(Bundle savedInstanceState) {
        DesignActivity activity = getActivity();
        if (activity != null) {
            activity.loadProject(savedInstanceState != null);
        }
    }

    private void onProjectLoaded(Bundle savedInstanceState) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.updateBottomMenu();
        activity.refresh();
        activity.dismissProjectLoadingDialog();
        if (savedInstanceState == null) {
            activity.checkForUnsavedProjectData();
        }
    }

    private void onProjectSaved(boolean dataSaved, boolean closeAfterSave) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (dataSaved) {
            SketchToast.toast(
                    activity.getApplicationContext(),
                    Helper.getResString(R.string.common_message_complete_save),
                    SketchToast.TOAST_NORMAL).show();
            activity.saveVersionCodeInformationToProject();
            activity.dismissProjectLoadingDialog();
            if (closeAfterSave) {
                activity.finish();
            }
        } else {
            showSaveFailed(activity);
            activity.dismissProjectLoadingDialog();
        }
    }

    private void onProjectSaveFailed() {
        DesignActivity activity = getActivity();
        if (activity != null) {
            showSaveFailed(activity);
            activity.dismissProjectLoadingDialog();
        }
    }

    private static void showSaveFailed(DesignActivity activity) {
        SketchToast.toast(
                activity.getApplicationContext(),
                Helper.getResString(R.string.common_message_save_failed),
                SketchToast.TOAST_WARNING).show();
    }

    private void dismissLoadingDialog() {
        DesignActivity activity = getActivity();
        if (activity != null) {
            activity.dismissProjectLoadingDialog();
        }
    }

    private void discardInBackground() {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        try {
            ResourceManager resourceManager = ProjectDataManager.getResourceManager(projectId);
            if (resourceManager.hasLazyBackup()) {
                resourceManager.restoreImagesFromTemp();
                resourceManager.restoreSoundsFromTemp();
                resourceManager.restoreFontsFromTemp();
            }
            ProjectDataManager.discardAll();
        } catch (RuntimeException error) {
            activity.recordProjectControllerFailure(
                    "DiscardChangesProjectCloser cleanup failed", error);
        }
    }

    private void finishAfterDiscard() {
        DesignActivity activity = getActivity();
        if (activity != null) {
            activity.dismissProjectLoadingDialog();
            activity.finish();
        }
    }

    private boolean saveProjectDataAndCleanup() {
        boolean dataSaved = saveProjectDataToFiles(projectId);
        if (dataSaved) {
            ProjectDataManager.getResourceManager(projectId).deleteTempDirs();
        }
        return dataSaved;
    }

    /** Persists views, logic, files, resources, and libraries in parallel. */
    private static boolean saveProjectDataToFiles(String projectId) {
        ProjectDataManager.getResourceManager(projectId).cleanupAllResources();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CompletableFuture<Boolean> fileFuture = CompletableFuture.supplyAsync(
                    () -> ProjectDataManager.getFileManager(projectId).saveToData(), pool);
            CompletableFuture<Boolean> dataFuture = CompletableFuture.supplyAsync(
                    () -> ProjectDataManager.getProjectDataManager(projectId).saveAllData(), pool);
            CompletableFuture<Boolean> resourceFuture = CompletableFuture.supplyAsync(
                    () -> ProjectDataManager.getResourceManager(projectId).saveToData(), pool);
            CompletableFuture<Boolean> libraryFuture = CompletableFuture.supplyAsync(
                    () -> ProjectDataManager.getLibraryManager(projectId).saveToData(), pool);
            CompletableFuture.allOf(
                    fileFuture, dataFuture, resourceFuture, libraryFuture).join();
            return fileFuture.join()
                    && dataFuture.join()
                    && resourceFuture.join()
                    && libraryFuture.join();
        } finally {
            pool.shutdown();
        }
    }

    private void saveUnsavedChangesInBackground() {
        ProjectDataStore projectDataStore =
                ProjectDataManager.getProjectDataManager(projectId);
        synchronized (projectDataStore) {
            projectDataStore.saveAllBackup();
        }
    }
}
