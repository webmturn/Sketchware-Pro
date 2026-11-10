package pro.sketchware.core.layout;

import java.util.Map;
import pro.sketchware.beans.LayoutBean;
import pro.sketchware.util.XmlUtil;

public final class MaterialButtonAttributes {
    private MaterialButtonAttributes() {}

    public static String generatedBackgroundTint(LayoutBean layout, Map<String, String> injected) {
        if (layout.backgroundColor == 0xffffff
                || (layout.backgroundResource != null
                        && !"NONE".equalsIgnoreCase(layout.backgroundResource))
                || InjectAttributeHandler.attributesToReplace(injected)
                        .contains("app:backgroundTint")) {
            return null;
        }
        for (String name : injected.keySet()) {
            if (InjectAttributeHandler.localName(name).equals("backgroundTint")) return null;
        }

        String color = layout.backgroundResColor;
        if (color == null) {
            return layout.backgroundColor == 0
                    ? "@android:color/transparent"
                    : XmlUtil.formatColor(layout.backgroundColor);
        }
        return color.startsWith("?") || color.startsWith("@color/") ? color : "@color/" + color;
    }
}
