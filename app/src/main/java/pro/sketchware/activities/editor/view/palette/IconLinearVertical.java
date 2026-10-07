package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;

import pro.sketchware.R;

public class IconLinearVertical extends IconBase {
    public IconLinearVertical(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_view_vertical);
        setWidgetName("Linear(V)");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.orientation = LayoutBean.ORIENTATION_VERTICAL;
        layoutBean.height = LayoutBean.LAYOUT_MATCH_PARENT;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        viewBean.convert = "LinearLayout";
        return viewBean;
    }
}
