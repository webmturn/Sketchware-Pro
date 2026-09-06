package pro.sketchware.activities.design;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import pro.sketchware.beans.ProjectFileBean;

/** Activity-scoped state shared by the design editor tabs. */
public final class DesignEditorViewModel extends ViewModel {

    private final MutableLiveData<ProjectFileBean> activeProjectFile = new MutableLiveData<>();

    public LiveData<ProjectFileBean> activeProjectFile() {
        return activeProjectFile;
    }

    @Nullable
    public ProjectFileBean getActiveProjectFile() {
        return activeProjectFile.getValue();
    }

    public void selectProjectFile(@NonNull ProjectFileBean projectFile) {
        activeProjectFile.setValue(projectFile);
    }

    /** Re-emits the current file after its backing project data has changed. */
    public void refreshActiveProjectFile() {
        ProjectFileBean projectFile = activeProjectFile.getValue();
        if (projectFile != null) {
            activeProjectFile.setValue(projectFile);
        }
    }
}
