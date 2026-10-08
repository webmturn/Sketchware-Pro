package pro.sketchware.core.layout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import pro.sketchware.beans.ViewBean;

/** The padding attributes emitted by the generator, shared with the editor preview. */
public final class PaddingAttributes {
    private PaddingAttributes() { }

    public static Set<String> attributesToReplace(ViewBean bean) {
        return attributesToReplace(new InjectAttributeHandler(bean).getNamespacedAttributes());
    }

    private static Set<String> attributesToReplace(Map<String, String> injected) {
        String replace = injected.get("tools:replace");
        return replace == null ? Set.of() : new HashSet<>(Arrays.asList(replace.split("\\s*,\\s*")));
    }

    public static Map<String, String> generated(ViewBean bean, boolean card) {
        return generated(bean, card, new InjectAttributeHandler(bean).getNamespacedAttributes());
    }

    private static Map<String, String> generated(ViewBean bean, boolean card, Map<String, String> injected) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Set<String> replaced = attributesToReplace(injected);
        Set<String> injectedNames = new HashSet<>();
        injected.keySet().forEach(name -> injectedNames.add(name.substring(name.indexOf(':') + 1)));
        String prefix = card ? "app:contentPadding" : "android:padding";
        int[] values = {bean.layout.paddingLeft, bean.layout.paddingTop,
                bean.layout.paddingRight, bean.layout.paddingBottom};
        if (values[0] > 0 && values[0] == values[1] && values[0] == values[2] && values[0] == values[3]) {
            add(attributes, prefix, values[0], replaced, injectedNames);
        } else {
            String[] sides = {"Left", "Top", "Right", "Bottom"};
            for (int i = 0; i < sides.length; i++) {
                add(attributes, prefix + sides[i], values[i], replaced, injectedNames);
            }
        }
        return attributes;
    }

    private static void add(Map<String, String> result, String name, int value,
                            Set<String> replaced, Set<String> injectedNames) {
        if (value > 0 && !replaced.contains(name) && !injectedNames.contains(name.substring(name.indexOf(':') + 1))) {
            result.put(name, value + "dp");
        }
    }

    public static Map<String, String> effective(ViewBean bean, boolean card) {
        Map<String, String> injected = new InjectAttributeHandler(bean).getNamespacedAttributes();
        Map<String, String> result = generated(bean, card, injected);
        String prefix = card ? "app:contentPadding" : "android:padding";
        injected.forEach((name, value) -> {
            if (name.equals(prefix) || name.startsWith(prefix)) result.put(name, value);
        });
        return result;
    }
}
