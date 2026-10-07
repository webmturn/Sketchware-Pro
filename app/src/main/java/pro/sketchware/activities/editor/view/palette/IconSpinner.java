package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;

import pro.sketchware.R;

public class IconSpinner extends IconBase {
    public IconSpinner(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_spinner);
        setWidgetName("Spinner");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        viewBean.type = ViewBean.VIEW_TYPE_WIDGET_SPINNER;
        layoutBean.width = LayoutBean.LAYOUT_MATCH_PARENT;
        layoutBean.height = LayoutBean.LAYOUT_WRAP_CONTENT;
        viewBean.convert = "Spinner";
        return viewBean;
    }
}
