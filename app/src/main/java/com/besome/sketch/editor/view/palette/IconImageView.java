package com.besome.sketch.editor.view.palette;

import android.content.Context;

import com.besome.sketch.beans.ViewBean;

import pro.sketchware.R;

public class IconImageView extends IconBase {
    public String resourceName = "";

    public IconImageView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_image);
        setWidgetName("ImageView");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = 6;
        viewBean.convert = "ImageView";
        viewBean.image.resName = resourceName;
        return viewBean;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String str) {
        resourceName = str;
    }
}
