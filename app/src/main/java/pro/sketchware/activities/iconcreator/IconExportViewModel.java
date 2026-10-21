package pro.sketchware.activities.iconcreator;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.util.LogUtil;

/** Keeps one export and its result across Activity recreation without retaining its UI. */
public final class IconExportViewModel extends ViewModel {
    record State(boolean running, Bitmap launcher, boolean adaptive, boolean failed) {}

    private final MutableLiveData<State> state = new MutableLiveData<>();

    LiveData<State> state() { return state; }

    boolean isRunning() { return state.getValue() != null && state.getValue().running(); }

    void start(TaskHost host, IconExport.Snapshot snapshot) {
        if (isRunning()) return;
        state.setValue(new State(true, null, false, false));
        BackgroundTasks.runSerial(host, "IconExport", () -> {
            boolean failed = false;
            try {
                IconExport.write(snapshot);
            } catch (Exception e) {
                LogUtil.e("IconExport", "Failed to export project icon", e);
                failed = true;
            } finally {
                for (Bitmap bitmap : snapshot.layers().values()) {
                    if (bitmap != snapshot.launcher()) bitmap.recycle();
                }
                if (snapshot.croppedIcon() != null) snapshot.croppedIcon().recycle();
            }
            state.postValue(new State(false, failed ? null : snapshot.launcher(), snapshot.adaptive(), failed));
        }, null, null);
    }

    void clearFailure() {
        State current = state.getValue();
        if (current != null && current.failed()) state.setValue(null);
    }
}
