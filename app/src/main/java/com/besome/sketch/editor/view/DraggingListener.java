package com.besome.sketch.editor.view;

public interface DraggingListener {
    boolean isAdmobEnabled();

    void onDragStarted();

    boolean isGoogleMapEnabled();

    void onDragEnded();
}
