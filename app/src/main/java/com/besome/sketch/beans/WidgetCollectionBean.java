package com.besome.sketch.beans;

import java.util.ArrayList;

public class WidgetCollectionBean extends SelectableBean {
    public String name;
    public ArrayList<ViewBean> widgets;

    public WidgetCollectionBean(String collectionName, ArrayList<ViewBean> widgetList) {
        name = collectionName;
        widgets = widgetList;
    }
}
