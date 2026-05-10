package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;

import pro.sketchware.beans.ViewBean;
import pro.sketchware.activities.editor.view.AndroidxOrMaterialView;
import pro.sketchware.activities.editor.view.palette.IconBase;

import pro.sketchware.beans.ViewBeans;
import pro.sketchware.R;

public class IconRecyclerView extends IconBase implements AndroidxOrMaterialView {

    public IconRecyclerView(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_list);
        setWidgetName("RecyclerView");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW;
        viewBean.layout.orientation = VERTICAL;
        viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        viewBean.layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        viewBean.layout.paddingLeft = 8;
        viewBean.layout.paddingTop = 8;
        viewBean.layout.paddingRight = 8;
        viewBean.layout.paddingBottom = 8;
        viewBean.convert = "androidx.recyclerview.widget.RecyclerView";
        return viewBean;
    }
}
