package pro.sketchware.activities.design;

import android.os.Bundle;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.R;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectContentSnapshot;
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
    private static final String STATE_SAVED_CONTENT = "project_saved_content";
    private static final String STATE_RESTORED_BACKUP = "project_restored_backup";

    private final WeakReference<DesignActivity> activityRef;
    private final String projectId;
    private volatile String savedContent;
    private volatile boolean restoredBackup;
    private boolean ready;
    private boolean checkingClose;

    DesignProjectController(DesignActivity activity, String projectId) {
        activityRef = new WeakReference<>(activity);
        this.projectId = projectId;
    }

    void load(Bundle savedInstanceState) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ready = false;
        savedContent = savedInstanceState == null ? null : savedInstanceState.getString(STATE_SAVED_CONTENT);
        restoredBackup = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESTORED_BACKUP);
        activity.onProjectLoadStarted();
        activity.showProjectLoadingDialog();
        BackgroundTasks.runIoIfAlive(
                TaskHost.of(activity),
                "DesignActivity$ProjectLoader",
                () -> loadInBackground(savedInstanceState),
                () -> onProjectLoaded(savedInstanceState),
                error -> onProjectLoadFailed());
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
                () -> saveProjectDataAndCleanup(true),
                result -> onProjectSaved(result, false),
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
                () -> saveProjectDataAndCleanup(false),
                result -> onProjectSaved(result, true),
                error -> onProjectSaveFailed());
    }

    void saveState(Bundle state) {
        state.putString(STATE_SAVED_CONTENT, savedContent);
        state.putBoolean(STATE_RESTORED_BACKUP, restoredBackup);
    }

    void onBackupRestored() {
        // Restoring is an unsaved user choice, including recovery from unreadable originals.
        restoredBackup = true;
    }

    void requestClose(boolean automaticSave) {
        DesignActivity activity = getActivity();
        if (activity == null || !ready || checkingClose || activity.isProjectDataRestoring()) return;
        checkingClose = true;
        activity.showProjectLoadingDialog();
        BackgroundTasks.callIoIfAlive(TaskHost.of(activity), "DesignActivity$ProjectChanges", () -> {
            if (!ProjectDataManager.canSaveProject(projectId)) return CloseState.UNREADABLE;
            // These states must be saved conservatively; no comparison can authorize discard.
            if (restoredBackup || savedContent == null) return CloseState.CHANGED;
            String current = ProjectContentSnapshot.capture(projectId);
            if (!savedContent.equals(current)) {
                return CloseState.CHANGED;
            }
            return CloseState.UNCHANGED;
        }, state -> onCloseChecked(state, automaticSave), error -> onCloseCheckFailed(error, automaticSave));
    }

    private void onCloseChecked(CloseState state, boolean automaticSave) {
        checkingClose = false;
        DesignActivity activity = getActivity();
        if (activity == null) return;
        activity.dismissProjectLoadingDialog();
        if (state == CloseState.UNREADABLE) {
            onProjectLoadFailed();
        } else if (state == CloseState.UNCHANGED) {
            // Cleanup only after the live Activity receives the result, so recreation cannot
            // let an old comparison discard the new editor's manager instances.
            ProjectDataManager.discardAll();
            activity.finish();
        } else if (automaticSave) {
            saveAndClose();
        } else {
            activity.showSaveBeforeQuittingDialog();
        }
    }

    private void onCloseCheckFailed(Throwable error, boolean automaticSave) {
        checkingClose = false;
        DesignActivity activity = getActivity();
        if (activity == null) return;
        activity.dismissProjectLoadingDialog();
        recordSnapshotFailure("Project change check failed", error);
        // An uncertain comparison must never authorize discarding edits.
        if (!ProjectDataManager.canSaveProject(projectId)) onProjectLoadFailed();
        else if (automaticSave) saveAndClose();
        else activity.showSaveBeforeQuittingDialog();
    }

    private void recordSnapshotFailure(String message, Throwable error) {
        DesignActivity activity = getActivity();
        if (activity != null) activity.recordProjectControllerFailure(message,
                error instanceof RuntimeException runtime ? runtime : new RuntimeException(error));
    }

    private enum CloseState { UNCHANGED, CHANGED, UNREADABLE }

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
        if (!ProjectDataManager.canSaveProject(projectId)) {
            onProjectLoadFailed();
            return;
        }
        activity.updateBottomMenu();
        activity.refresh();
        if (savedInstanceState == null && !restoredBackup) {
            BackgroundTasks.callIoIfAlive(TaskHost.of(activity), "DesignActivity$ProjectBaseline",
                    () -> ProjectContentSnapshot.capture(projectId), content -> {
                        savedContent = content;
                        finishLoading(savedInstanceState);
                    }, error -> {
                        recordSnapshotFailure("Project baseline failed", error);
                        finishLoading(savedInstanceState);
                    });
        } else {
            finishLoading(savedInstanceState);
        }
    }

    private void finishLoading(Bundle savedInstanceState) {
        DesignActivity activity = getActivity();
        if (activity == null) return;
        ready = true;
        activity.onProjectDataLoaded();
        activity.dismissProjectLoadingDialog();
        if (savedInstanceState == null) {
            activity.checkForUnsavedProjectData();
        }
    }

    private void onProjectLoadFailed() {
        ready = false;
        DesignActivity activity = getActivity();
        if (activity != null) {
            activity.dismissProjectLoadingDialog();
            activity.showProjectLoadFailure();
        }
    }

    void restoreBackup() {
        DesignActivity activity = getActivity();
        if (activity == null) return;
        activity.showProjectLoadingDialog();
        BackgroundTasks.callIoIfAlive(TaskHost.of(activity), "DesignActivity$BackupLoader", () -> {
            if (!ProjectDataManager.loadProjectBackup(projectId)) return false;
            activity.loadProject(true);
            return true;
        }, restored -> {
            if (restored) {
                onBackupRestored();
                onProjectLoaded(new Bundle());
            } else {
                onProjectLoadFailed();
            }
        }, error -> onProjectLoadFailed());
    }

    private void onProjectSaved(SaveResult result, boolean closeAfterSave) {
        DesignActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (result.saved()) {
            savedContent = result.content();
            restoredBackup = false;
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
        if (!ProjectDataManager.canSaveProject(projectId)) return;
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

    private SaveResult saveProjectDataAndCleanup(boolean updateBaseline) {
        boolean dataSaved = saveProjectDataToFiles(projectId);
        if (dataSaved) {
            ProjectDataManager.getResourceManager(projectId).deleteTempDirs();
            // A completed save that immediately closes the editor needs no new baseline.
            if (!updateBaseline) return new SaveResult(true, null);
            try {
                return new SaveResult(true, ProjectContentSnapshot.capture(projectId));
            } catch (IOException | RuntimeException error) {
                recordSnapshotFailure("Saved project baseline failed", error);
                // A failed comparison must not turn a completed write into a failed save.
                return new SaveResult(true, null);
            }
        }
        return new SaveResult(false, null);
    }

    private record SaveResult(boolean saved, String content) { }

    /** Persists views, logic, files, resources, and libraries in parallel. */
    private static boolean saveProjectDataToFiles(String projectId) {
        if (!ProjectDataManager.canSaveProject(projectId)) return false;
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
        if (!ProjectDataManager.canSaveProject(projectId)) return;
        if (!restoredBackup && savedContent != null) {
            try {
                if (savedContent.equals(ProjectContentSnapshot.capture(projectId))) return;
            } catch (IOException | RuntimeException error) {
                // Uncertain resource content must not prevent backing up healthy metadata.
                recordSnapshotFailure("Unsaved project change check failed", error);
            }
        }
        ProjectDataStore projectDataStore =
                ProjectDataManager.getProjectDataManager(projectId);
        synchronized (projectDataStore) {
            projectDataStore.saveAllBackup();
        }
    }
}
