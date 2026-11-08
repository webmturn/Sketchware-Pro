package pro.sketchware.core.layout;

import java.util.LinkedHashMap;
import java.util.Map;
import pro.sketchware.beans.ViewBean;

public final class ProgressBarAttributes {
    private ProgressBarAttributes() {}

    public static Map<String, String> generated(ViewBean bean, Map<String, String> injected) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (bean.progress > ViewBean.DEFAULT_PROGRESS) {
            attributes.put("android:progress", Integer.toString(bean.progress));
        }
        if (bean.max != ViewBean.DEFAULT_MAX) {
            attributes.put("android:max", Integer.toString(bean.max));
        }
        if (bean.indeterminate != null && !bean.indeterminate.isEmpty()) {
            attributes.put("android:indeterminate", bean.indeterminate);
        }
        if (bean.progressStyle != null && !bean.progressStyle.isEmpty()) {
            attributes.put("style", bean.progressStyle);
        }

        InjectAttributeHandler.removeOverriddenAttributes(attributes, injected);
        return attributes;
    }
}
