package pro.sketchware.model;

import com.google.gson.annotations.SerializedName;

/**
 * POJO representing a custom listener definition, serialized to/from
 * {@code listeners.json} via Gson.
 */
public class CustomListener {

    @SerializedName("name")
    private String name = "";

    @SerializedName("code")
    private String code = "";

    /** "true" if this listener is an independent class/method, "false" otherwise. */
    @SerializedName("s")
    private String independentFlag = "";

    @SerializedName("imports")
    private String imports = "";

    public CustomListener() {
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIndependentFlag() {
        return independentFlag != null ? independentFlag : "";
    }

    public void setIndependentFlag(String independentFlag) {
        this.independentFlag = independentFlag;
    }

    public boolean isIndependent() {
        return "true".equals(independentFlag);
    }

    public String getImports() {
        return imports != null ? imports : "";
    }

    public void setImports(String imports) {
        this.imports = imports;
    }
}
