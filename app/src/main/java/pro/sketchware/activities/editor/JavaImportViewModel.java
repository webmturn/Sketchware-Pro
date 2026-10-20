package pro.sketchware.activities.editor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.JavaSourceImporter;
import pro.sketchware.util.LogUtil;

/** Retains an import across configuration changes without retaining an Activity or its views. */
public final class JavaImportViewModel extends ViewModel {
    record ImportState(boolean running, List<String> failedFiles) {}

    private final MutableLiveData<ImportState> state = new MutableLiveData<>();

    LiveData<ImportState> state() { return state; }

    void start(TaskHost host, List<? extends File> files, File destination, String packageName) {
        ImportState current = state.getValue();
        if (files.isEmpty() || (current != null && current.running())) return;
        List<File> sources = List.copyOf(files);
        state.setValue(new ImportState(true, List.of()));
        BackgroundTasks.runSerial(host, "JavaSourceImport", () -> {
            ArrayList<String> failures = new ArrayList<>();
            for (File source : sources) {
                try {
                    JavaSourceImporter.importFile(source, destination, packageName);
                } catch (Exception e) {
                    LogUtil.e("JavaSourceImport", "Failed to import " + source.getName(), e);
                    failures.add(source.getName());
                }
            }
            // Completion belongs to the ViewModel, even when the original Activity was destroyed.
            state.postValue(new ImportState(false, List.copyOf(failures)));
        }, null, null);
    }

    void clearCompleted() {
        ImportState current = state.getValue();
        if (current != null && !current.running()) state.setValue(null);
    }
}
