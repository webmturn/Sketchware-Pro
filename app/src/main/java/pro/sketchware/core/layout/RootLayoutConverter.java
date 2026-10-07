package pro.sketchware.core.layout;

import android.util.Pair;

import java.util.Map;

import pro.sketchware.beans.RootLayoutBean;
import pro.sketchware.beans.ViewBean;

/** Converts root metadata without owning any project storage. */
public final class RootLayoutConverter {
    private RootLayoutConverter() {
    }

    public static RootLayoutBean toRoot(Pair<String, Map<String, String>> root) {
        return new RootLayoutBean(root.first, root.second);
    }

    public static ViewBean toBean(RootLayoutBean root) {
        ViewBean viewBean = new ViewBean("root", ViewBeanParser.getViewTypeByClassName(root.getClassName()));
        viewBean.convert = root.getClassName();
        new ViewBeanFactory(viewBean).applyAttributes(root.getAttributes());
        return viewBean;
    }
}
