package pro.sketchware.model;

import com.google.gson.annotations.SerializedName;

/**
 * POJO representing a custom event definition, serialized to/from
 * {@code events.json} via Gson.
 */
public class CustomEvent {

    @SerializedName("name")
    private String name = "";

    @SerializedName("var")
    private String var = "";

    @SerializedName("listener")
    private String listener = "";

    @SerializedName("icon")
    private String icon = "";

    @SerializedName("description")
    private String description = "";

    @SerializedName("parameters")
    private String parameters = "";

    @SerializedName("code")
    private String code = "";

    @SerializedName("headerSpec")
    private String headerSpec = "";

    public CustomEvent() {
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVar() {
        return var != null ? var : "";
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getListener() {
        return listener != null ? listener : "";
    }

    public void setListener(String listener) {
        this.listener = listener;
    }

    public String getIcon() {
        return icon != null ? icon : "";
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParameters() {
        return parameters != null ? parameters : "";
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHeaderSpec() {
        return headerSpec != null ? headerSpec : "";
    }

    public void setHeaderSpec(String headerSpec) {
        this.headerSpec = headerSpec;
    }
}
