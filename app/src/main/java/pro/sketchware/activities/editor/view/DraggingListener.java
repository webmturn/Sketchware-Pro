package pro.sketchware.activities.editor.view;

public interface DraggingListener {
    boolean isAdmobEnabled();

    void onDragStarted();

    boolean isGoogleMapEnabled();

    void onDragEnded();
}
