package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.activities.editor.view.palette.IconBase;

import pro.sketchware.beans.ViewBeans;
import pro.sketchware.R;

public class IconDatePicker extends IconBase {

    public IconDatePicker(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_calendary_today);
        setWidgetName("DatePicker");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        layoutBean.width = ViewGroup.LayoutParams.MATCH_PARENT;
        viewBean.convert = getName();
        return viewBean;
    }
}
