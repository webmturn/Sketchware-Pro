package com.besome.sketch.beans;

import pro.sketchware.core.ReflectiveToString;

public class HistoryBean extends ReflectiveToString {
    public static final int ACTION_TYPE_ADD = 0;
    public static final int ACTION_TYPE_REMOVE = 2;
    public static final int ACTION_TYPE_UPDATE = 1;
    public int actionType;
    public ReflectiveToString currentData;
    public ReflectiveToString prevData;

    public HistoryBean(int i, ReflectiveToString nAVar, ReflectiveToString nAVar2) {
        actionType = i;
        prevData = nAVar;
        currentData = nAVar2;
    }

    public int getActionType() {
        return actionType;
    }

    public ReflectiveToString getCurrentData() {
        return currentData;
    }

    public ReflectiveToString getPrevData() {
        return prevData;
    }
}
