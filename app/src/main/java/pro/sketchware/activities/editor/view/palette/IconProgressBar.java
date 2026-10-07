package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;

import pro.sketchware.R;

public class IconProgressBar extends IconBase {
    public IconProgressBar(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_progress_bar);
        setWidgetName("ProgressBar");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        layoutBean.width = LayoutBean.LAYOUT_MATCH_PARENT;
        viewBean.text.text = getName();
        viewBean.convert = "ProgressBar";
        return viewBean;
    }
}
