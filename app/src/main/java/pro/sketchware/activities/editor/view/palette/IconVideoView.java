package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import pro.sketchware.R;
import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;

public class IconVideoView extends IconBase {

    public IconVideoView(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_video);
        setWidgetName("VideoView");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        viewBean.text.text = getName();
        viewBean.convert = getName();
        return viewBean;
    }
}
