package pro.sketchware.model;

import com.google.gson.annotations.SerializedName;

/**
 * POJO representing a custom component definition, serialized to/from
 * {@code component.json} via Gson.
 */
public class CustomComponent {

    @SerializedName("name")
    private String name = "";

    @SerializedName("id")
    private String id = "";

    @SerializedName("icon")
    private String icon = "";

    @SerializedName("varName")
    private String varName = "";

    @SerializedName("typeName")
    private String typeName = "";

    @SerializedName("buildClass")
    private String buildClass = "";

    /** The fully-qualified Java class name, serialized as "class". */
    @SerializedName("class")
    private String className = "";

    @SerializedName("description")
    private String description = "";

    @SerializedName("url")
    private String url = "";

    @SerializedName("additionalVar")
    private String additionalVar = "";

    @SerializedName("defineAdditionalVar")
    private String defineAdditionalVar = "";

    @SerializedName("imports")
    private String imports = "";

    public CustomComponent() {
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIdAsInt() {
        try {
            return Integer.parseInt(getId());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getIcon() {
        return icon != null ? icon : "";
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVarName() {
        return varName != null ? varName : "";
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getTypeName() {
        return typeName != null ? typeName : "";
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getBuildClass() {
        return buildClass != null ? buildClass : "";
    }

    public void setBuildClass(String buildClass) {
        this.buildClass = buildClass;
    }

    public String getClassName() {
        return className != null ? className : "";
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdditionalVar() {
        return additionalVar != null ? additionalVar : "";
    }

    public void setAdditionalVar(String additionalVar) {
        this.additionalVar = additionalVar;
    }

    public String getDefineAdditionalVar() {
        return defineAdditionalVar != null ? defineAdditionalVar : "";
    }

    public void setDefineAdditionalVar(String defineAdditionalVar) {
        this.defineAdditionalVar = defineAdditionalVar;
    }

    public String getImports() {
        return imports != null ? imports : "";
    }

    public void setImports(String imports) {
        this.imports = imports;
    }
}
