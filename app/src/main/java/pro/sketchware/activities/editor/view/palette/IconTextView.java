package pro.sketchware.activities.editor.view.palette;

import android.content.Context;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;

import pro.sketchware.R;

public class IconTextView extends IconBase {
    public IconTextView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetImage(R.drawable.ic_mtrl_formattext);
        setWidgetName("TextView");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = 4;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        viewBean.text.text = getName();
        viewBean.convert = "TextView";
        return viewBean;
    }
}
