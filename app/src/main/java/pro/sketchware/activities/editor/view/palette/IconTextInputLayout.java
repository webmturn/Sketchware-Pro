package pro.sketchware.activities.editor.view.palette;

import android.content.Context;
import android.view.ViewGroup;

import pro.sketchware.beans.ViewBean;
import pro.sketchware.activities.editor.view.AndroidxOrMaterialView;
import pro.sketchware.activities.editor.view.palette.IconBase;

import pro.sketchware.beans.ViewBeans;
import pro.sketchware.R;

public class IconTextInputLayout extends IconBase implements AndroidxOrMaterialView {

    public IconTextInputLayout(Context context) {
        super(context);
        setWidgetImage(R.drawable.ic_mtrl_edittext);
        setWidgetName("TextInputLayout");
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT;
        viewBean.layout.orientation = VERTICAL;
        viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
        viewBean.layout.paddingLeft = 0;
        viewBean.layout.paddingTop = 0;
        viewBean.layout.paddingRight = 0;
        viewBean.layout.paddingBottom = 0;
        viewBean.convert = "com.google.android.material.textfield.TextInputLayout";
        viewBean.inject = "style=\"@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox\"";
        return viewBean;
    }
}
