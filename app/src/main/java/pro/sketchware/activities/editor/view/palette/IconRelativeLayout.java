package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;

import pro.sketchware.R;

public class IconRelativeLayout extends IconBase {
    public IconRelativeLayout(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_view_relative);
        setWidgetName("RelativeLayout");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBean.VIEW_TYPE_LAYOUT_RELATIVE;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.width = -1;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        viewBean.convert = "RelativeLayout";
        return viewBean;
    }
}
