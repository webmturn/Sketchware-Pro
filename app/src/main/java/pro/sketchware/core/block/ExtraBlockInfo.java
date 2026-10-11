package pro.sketchware.core.block;

public class ExtraBlockInfo {

    public transient boolean isMissing;
    private String code = "";
    // Optional in older project definitions and backups.
    private String imports;
    private int color = 0;
    private String name = "";
    private int paletteColor = 0;
    private String spec = "";
    private String spec2 = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getImports() {
        return imports == null ? "" : imports;
    }

    public void setImports(String imports) {
        this.imports = imports;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getPaletteColor() {
        return paletteColor;
    }

    public void setPaletteColor(int paletteColor) {
        this.paletteColor = paletteColor;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getSpec2() {
        return spec2;
    }

    public void setSpec2(String spec2) {
        this.spec2 = spec2;
    }
}
