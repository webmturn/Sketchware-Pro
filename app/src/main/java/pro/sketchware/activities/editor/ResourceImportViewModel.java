package pro.sketchware.activities.editor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.AssetImporter;
import pro.sketchware.util.LogUtil;

/** Retains a resource import without retaining an Activity or its views. */
public final class ResourceImportViewModel extends ViewModel {
    record ImportState(boolean running, List<String> failedFiles) {}

    private final MutableLiveData<ImportState> state = new MutableLiveData<>();

    LiveData<ImportState> state() { return state; }

    void start(TaskHost host, List<? extends File> files, File destination) {
        ImportState current = state.getValue();
        if (files.isEmpty() || (current != null && current.running())) return;
        List<File> sources = List.copyOf(files);
        state.setValue(new ImportState(true, List.of()));
        BackgroundTasks.runSerial(host, "ResourceImport", () -> {
            ArrayList<String> failures = new ArrayList<>();
            for (File source : sources) {
                try {
                    AssetImporter.importEntry(source, destination);
                } catch (Exception e) {
                    LogUtil.e("ResourceImport", "Failed to import " + source.getName(), e);
                    failures.add(source.getName());
                }
            }
            // Deliver completion even when the original Activity has been destroyed.
            state.postValue(new ImportState(false, List.copyOf(failures)));
        }, null, null);
    }

    void clearCompleted() {
        ImportState current = state.getValue();
        if (current != null && !current.running()) state.setValue(null);
    }
}
