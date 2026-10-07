package pro.sketchware.beans;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root layout metadata using the existing view_root JSON representation. */
public class RootLayoutBean {
    @SerializedName("class_name")
    private String className;

    @SerializedName("attributes")
    private Map<String, String> attrs;

    public RootLayoutBean() {
    }

    public RootLayoutBean(String className, Map<String, String> attrs) {
        this.className = className;
        this.attrs = attrs;
    }

    public static RootLayoutBean createDefault() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("android:layout_width", "match_parent");
        attrs.put("android:layout_height", "match_parent");
        attrs.put("android:orientation", "vertical");
        return new RootLayoutBean("LinearLayout", attrs);
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getAttributes() {
        return attrs;
    }
}
