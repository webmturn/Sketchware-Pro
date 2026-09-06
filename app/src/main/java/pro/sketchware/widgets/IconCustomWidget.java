package pro.sketchware.widgets;

import static pro.sketchware.beans.ViewBean.getViewTypeResId;

import android.content.Context;
import android.view.ViewGroup;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.beans.ViewBeans;
import pro.sketchware.activities.editor.view.palette.IconBase;

import java.util.HashMap;
import java.util.Objects;

public class IconCustomWidget extends IconBase {

    private final HashMap<String, Object> widgetMap = new HashMap<>();
    private int type;
    private String Title;

    public IconCustomWidget(Context context) {
        super(context);
    }

    public IconCustomWidget(HashMap<String, Object> map, Context context) {
        super(context);
        if (map != null) {
            widgetMap.putAll(map);
        }
        Object typeValue = widgetMap.get("type");
        if (typeValue instanceof Number) {
            type = ((Number) typeValue).intValue();
        } else {
            try {
                type = Integer.parseInt(String.valueOf(typeValue));
            } catch (NumberFormatException e) {
                type = ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW;
            }
        }
        Object titleValue = widgetMap.get("title");
        Title = titleValue != null ? titleValue.toString() : "";
        setWidgetImage(getViewTypeResId(type));
        setWidgetName(Title);
    }

    @Override
    public ViewBean getBean() {
        ViewBean viewBean = new ViewBean();
        viewBean.type = type;
        LayoutBean layoutBean = viewBean.layout;
        layoutBean.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutBean.paddingLeft = 8;
        layoutBean.paddingTop = 8;
        layoutBean.paddingRight = 8;
        layoutBean.paddingBottom = 8;
        Object nameValue = widgetMap.get("name");
        viewBean.convert = nameValue != null ? nameValue.toString() : "";

        switch (viewBean.type) {
            case ViewBean.VIEW_TYPE_LAYOUT_LINEAR,
                    ViewBean.VIEW_TYPE_LAYOUT_RELATIVE,
                    ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW:
                layoutBean.width = ViewGroup.LayoutParams.MATCH_PARENT;
                viewBean.layout.orientation = VERTICAL;
                break;
            case ViewBean.VIEW_TYPE_WIDGET_BUTTON,
                    ViewBean.VIEW_TYPE_WIDGET_SWITCH,
                    ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW,
                    ViewBean.VIEW_TYPE_WIDGET_CHECKBOX,
                    ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON,
                    ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON:
                viewBean.text.text = Title;
                break;
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT:
                viewBean.text.hint = Title;
                break;
            case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW:
                viewBean.image.resName = "";
                break;
            case ViewBean.VIEW_TYPE_WIDGET_WEBVIEW,
                    ViewBean.VIEW_TYPE_WIDGET_SPINNER,
                    ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW,
                    ViewBean.VIEW_TYPE_WIDGET_SEEKBAR,
                    ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW,
                    ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT:
                viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
            case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR:
                viewBean.text.text = Title;
                layoutBean.width = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
        }
        Object injectValue = widgetMap.get("inject");
        viewBean.inject = injectValue != null ? injectValue.toString() : "";
        viewBean.isCustomWidget = true;
        return viewBean;
    }
}
