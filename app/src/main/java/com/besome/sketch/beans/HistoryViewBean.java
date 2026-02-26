package com.besome.sketch.beans;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import pro.sketchware.core.ReflectiveToString;

public class HistoryViewBean extends ReflectiveToString {
    public static final int ACTION_TYPE_ADD = 0;
    public static final int ACTION_TYPE_MOVE = 3;
    public static final int ACTION_TYPE_REMOVE = 2;
    public static final int ACTION_TYPE_UPDATE = 1;
    public static final int ACTION_TYPE_OVERRIDE = 4;
    public int actionType;
    public ArrayList<ViewBean> addedData;
    public ViewBean currentUpdateData;
    public ViewBean moveData;
    public ViewBean prevUpdateData;
    public ArrayList<ViewBean> removedData;

    public void actionAdd(ArrayList<ViewBean> arrayList) {
        actionType = 0;
        addedData = new ArrayList<>();
        for (ViewBean bean : arrayList) {
            ViewBean viewBean = new ViewBean();
            viewBean.copy(bean);
            addedData.add(viewBean);
        }
    }

    public void actionMove(ViewBean viewBean) {
        actionType = 3;
        moveData = new ViewBean();
        moveData.copy(viewBean);
    }

    public void actionRemove(ArrayList<ViewBean> arrayList) {
        actionType = 2;
        removedData = new ArrayList<>();
        for (ViewBean bean : arrayList) {
            ViewBean viewBean = new ViewBean();
            viewBean.copy(bean);
            removedData.add(viewBean);
        }
    }

    public void actionUpdate(ViewBean prevViewBean, ViewBean currentViewBean) {
        actionType = 1;
        prevUpdateData = new ViewBean();
        prevUpdateData.copy(prevViewBean);
        currentUpdateData = new ViewBean();
        currentUpdateData.copy(currentViewBean);
    }

    public void actionOverride(ArrayList<ViewBean> addedBeans, ArrayList<ViewBean> removedBeans) {
        actionAdd(addedBeans);
        actionRemove(removedBeans);
        actionType = ACTION_TYPE_OVERRIDE;
    }

    public void copy(HistoryViewBean historyViewBean) {
        actionType = historyViewBean.actionType;
        if (historyViewBean.prevUpdateData != null) {
            prevUpdateData = new ViewBean();
            prevUpdateData.copy(historyViewBean.prevUpdateData);
        }
        if (historyViewBean.currentUpdateData != null) {
            currentUpdateData = new ViewBean();
            currentUpdateData.copy(historyViewBean.currentUpdateData);
        }
        if (historyViewBean.moveData != null) {
            moveData = new ViewBean();
            moveData.copy(historyViewBean.moveData);
        }
        if (historyViewBean.addedData != null) {
            addedData = new ArrayList<>();
            for (ViewBean addedDatum : historyViewBean.addedData) {
                ViewBean viewBean = new ViewBean();
                viewBean.copy(addedDatum);
                addedData.add(viewBean);
            }
        }
        if (historyViewBean.removedData != null) {
            removedData = new ArrayList<>();
            for (ViewBean removedDatum : historyViewBean.removedData) {
                ViewBean copiedBean = new ViewBean();
                copiedBean.copy(removedDatum);
                removedData.add(copiedBean);
            }
        }
    }

    public int getActionType() {
        return actionType;
    }

    public ArrayList<ViewBean> getAddedData() {
        return addedData;
    }

    public ViewBean getCurrentUpdateData() {
        return currentUpdateData;
    }

    public ViewBean getMovedData() {
        return moveData;
    }

    public ViewBean getPrevUpdateData() {
        return prevUpdateData;
    }

    public ArrayList<ViewBean> getRemovedData() {
        return removedData;
    }

    @Override
    @NonNull
    public HistoryViewBean clone() {
        HistoryViewBean historyViewBean = new HistoryViewBean();
        historyViewBean.copy(this);
        return historyViewBean;
    }
}
