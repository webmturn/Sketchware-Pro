package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.ViewBean;

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
        viewBean.type = ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW;
        viewBean.convert = "ImageView";
        viewBean.image.resName = resourceName;
        return viewBean;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String name) {
        resourceName = name;
    }
}
