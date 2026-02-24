package mod.hey.studios.editor.manage.block;

public class ExtraBlockInfo {

    public transient boolean isMissing;
    private String code = "";
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

    public int getColor() {
        return color;
    }

    public void setColor(int i) {
        color = i;
    }

    public int getPaletteColor() {
        return paletteColor;
    }

    public void setPaletteColor(int i) {
        paletteColor = i;
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
