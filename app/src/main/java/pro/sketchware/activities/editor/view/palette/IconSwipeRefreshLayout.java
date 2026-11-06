package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;
import pro.sketchware.R;
import pro.sketchware.activities.editor.view.AndroidxOrMaterialView;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;

public class IconSwipeRefreshLayout extends IconBase implements AndroidxOrMaterialView {

    public IconSwipeRefreshLayout(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_refresh);
        setWidgetName("SwipeRefreshLayout");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT;
        viewBean.layout.orientation = VERTICAL;
        viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        viewBean.layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        viewBean.layout.paddingLeft = 8;
        viewBean.layout.paddingTop = 8;
        viewBean.layout.paddingRight = 8;
        viewBean.layout.paddingBottom = 8;
        viewBean.convert = "androidx.swiperefreshlayout.widget.SwipeRefreshLayout";
        return viewBean;
    }
}
