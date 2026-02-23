package com.besome.sketch.beans;

import pro.sketchware.core.ReflectiveToString;

public class CollapsibleBean extends ReflectiveToString {
    public int buttonPressed = -1;
    public boolean isCollapsed = true;
    public boolean isConfirmation = false;
    public boolean isSelected = false;

    public void initValue() {
        isCollapsed = true;
        isConfirmation = false;
        isSelected = false;
        buttonPressed = -1;
    }
}
