package pro.sketchware.core.layout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import pro.sketchware.beans.ViewBean;

/** The padding attributes emitted by the generator, shared with the editor preview. */
public final class PaddingAttributes {
    private PaddingAttributes() {}

    public static Set<String> attributesToReplace(ViewBean bean) {
        return InjectAttributeHandler.attributesToReplace(
                new InjectAttributeHandler(bean).getNamespacedAttributes());
    }

    public static Map<String, String> generated(ViewBean bean, boolean card) {
        return generated(bean, card, new InjectAttributeHandler(bean).getNamespacedAttributes());
    }

    private static Map<String, String> generated(
            ViewBean bean, boolean card, Map<String, String> injected) {
        Map<String, String> attributes = new LinkedHashMap<>();
        String prefix = card ? "app:contentPadding" : "android:padding";
        int[] values = {
            bean.layout.paddingLeft,
            bean.layout.paddingTop,
            bean.layout.paddingRight,
            bean.layout.paddingBottom
        };
        if (values[0] > 0
                && values[0] == values[1]
                && values[0] == values[2]
                && values[0] == values[3]) {
            attributes.put(prefix, values[0] + "dp");
        } else {
            String[] sides = {"Left", "Top", "Right", "Bottom"};
            for (int i = 0; i < sides.length; i++) {
                if (values[i] > 0) {
                    attributes.put(prefix + sides[i], values[i] + "dp");
                }
            }
        }
        InjectAttributeHandler.removeOverriddenAttributes(attributes, injected);
        return attributes;
    }

    public static Map<String, String> effective(ViewBean bean, boolean card) {
        Map<String, String> injected = new InjectAttributeHandler(bean).getNamespacedAttributes();
        Map<String, String> result = generated(bean, card, injected);
        String prefix = card ? "app:contentPadding" : "android:padding";
        injected.forEach(
                (name, value) -> {
                    if (name.equals(prefix) || name.startsWith(prefix)) result.put(name, value);
                });
        return result;
    }
}
