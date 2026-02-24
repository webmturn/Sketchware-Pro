package com.besome.sketch.editor.manage.library;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;


import pro.sketchware.core.MapValueHelper;

public class ProjectComparator implements Comparator<HashMap<String, Object>> {

    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_ID = 2;
    public static final int SORT_ORDER_ASCENDING = 4;
    public static final int SORT_ORDER_DESCENDING = 8;
    public static final int DEFAULT = SORT_BY_ID | SORT_ORDER_DESCENDING;

    private int sortBy = 0;
    private String pinned_scid;

    public ProjectComparator() {
    }

    public ProjectComparator(int sortBy, String pinned_scid) {
        this.sortBy = sortBy;
        this.pinned_scid = pinned_scid;
    }

    @Override
    public int compare(HashMap<String, Object> first, HashMap<String, Object> second) {
        boolean isSortOrderAscending = (sortBy & SORT_ORDER_ASCENDING) == SORT_ORDER_ASCENDING;

        if (Objects.equals(pinned_scid, MapValueHelper.getString(first, "sc_id"))) {
            return -1;
        } else if (Objects.equals(pinned_scid, MapValueHelper.getString(second, "sc_id"))) {
            return 1;
        }

        if ((sortBy & SORT_BY_ID) == SORT_BY_ID) {
            return Integer.compare(
                    Integer.parseInt(MapValueHelper.getString(first, "sc_id")),
                    Integer.parseInt(MapValueHelper.getString(second, "sc_id"))) * (isSortOrderAscending ? 1 : -1);
        } else if ((sortBy & SORT_BY_NAME) == SORT_BY_NAME) {
            return MapValueHelper.getString(first, "my_ws_name").compareTo(MapValueHelper.getString(second, "my_ws_name")) * (isSortOrderAscending ? 1 : -1);
        } else {
            return Integer.compare(
                    Integer.parseInt(MapValueHelper.getString(first, "sc_id")),
                    Integer.parseInt(MapValueHelper.getString(second, "sc_id"))) * -1;
        }
    }
}
