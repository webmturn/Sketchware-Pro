package pro.sketchware.core.build;

public interface BuildProgressReceiver {
    void onProgress(String progress, int step);
}
