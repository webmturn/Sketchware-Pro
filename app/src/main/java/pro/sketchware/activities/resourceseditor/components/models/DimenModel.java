package pro.sketchware.activities.resourceseditor.components.models;

public class DimenModel {
    private String dimenName;
    private String dimenValue;

    public DimenModel(String dimenName, String dimenValue) {
        this.dimenName = dimenName;
        this.dimenValue = dimenValue;
    }

    public String getDimenName() {
        return dimenName;
    }

    public void setDimenName(String dimenName) {
        this.dimenName = dimenName;
    }

    public String getDimenValue() {
        return dimenValue;
    }

    public void setDimenValue(String dimenValue) {
        this.dimenValue = dimenValue;
    }
}
