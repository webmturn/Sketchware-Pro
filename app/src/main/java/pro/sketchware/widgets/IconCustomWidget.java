package pro.sketchware.widgets;

import static com.besome.sketch.beans.ViewBean.getViewTypeResId;

import android.content.Context;
import android.view.ViewGroup;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.palette.IconBase;

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
            case 0, 1, 2:
                layoutBean.width = ViewGroup.LayoutParams.MATCH_PARENT;
                viewBean.layout.orientation = VERTICAL;
                break;
            case 3, 13, 4, 11, 19, 41:
                viewBean.text.text = Title;
                break;
            case 5:
                viewBean.text.hint = Title;
                break;
            case 6:
                viewBean.image.resName = "";
                break;
            case 7, 10, 12, 14, 36, 39:
                viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
            case 8:
                viewBean.text.text = Title;
                layoutBean.width = -1;
                break;
        }
        Object injectValue = widgetMap.get("inject");
        viewBean.inject = injectValue != null ? injectValue.toString() : "";
        viewBean.isCustomWidget = true;
        return viewBean;
    }
}