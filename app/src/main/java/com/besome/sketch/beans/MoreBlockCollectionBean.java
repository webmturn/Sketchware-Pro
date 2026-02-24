package com.besome.sketch.beans;

import java.util.ArrayList;

public class MoreBlockCollectionBean extends SelectableBean {
    public ArrayList<BlockBean> blocks;
    public String name;
    public String spec;

    public MoreBlockCollectionBean(String name, String spec, ArrayList<BlockBean> blocks) {
        this.name = name;
        this.spec = spec;
        this.blocks = blocks;
    }
}
