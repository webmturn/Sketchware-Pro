package pro.sketchware.core;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import pro.sketchware.R;

public class PresetLayoutFactory {
  public static int getListItemPresetIcon(String presetName) {
    byte b;
    if (presetName.hashCode() == 1424216003 && presetName.equals("Basic List Item")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? -1 : R.drawable.activity_preset_1;
  }
  
  public static ViewBean createDefaultTextView() {
    ViewBean viewBean = new ViewBean("textview1", 4);
    viewBean.parent = "root";
    viewBean.index = 0;
    viewBean.preParentType = 0;
    viewBean.name = "textview1";
    viewBean.text.text = "TextView";
    LayoutBean layoutBean = viewBean.layout;
    layoutBean.paddingTop = 8;
    layoutBean.paddingBottom = 8;
    layoutBean.paddingLeft = 8;
    layoutBean.paddingRight = 8;
    return viewBean;
  }
  
  public static ArrayList<ProjectFileBean> getListItemPresets() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(createBasicListItemFile());
    return arrayList;
  }
  
  public static ArrayList<ViewBean> getListItemPresetViews(String presetName) {
    byte b;
    if (presetName.hashCode() == 1424216003 && presetName.equals("Basic List Item")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? new ArrayList<ViewBean>() : createBasicListItemViews();
  }
  
  public static int getDrawerPresetIcon(String presetName) {
    byte b;
    if (presetName.hashCode() == 920477027 && presetName.equals("Basic Drawer")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? -1 : R.drawable.activity_preset_1;
  }
  
  public static ArrayList<ProjectFileBean> getDrawerPresets() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(createBasicDrawerFile());
    return arrayList;
  }
  
  public static ArrayList<ProjectFileBean> getActivityPresets() {
    ArrayList<ProjectFileBean> arrayList = new ArrayList<>();
    arrayList.add(createEmptyActivityFile());
    arrayList.add(createBasicActivityFile());
    arrayList.add(createTextActivityFile());
    return arrayList;
  }
  
  public static ArrayList<ViewBean> getDrawerPresetViews(String presetName) {
    byte b;
    if (presetName.hashCode() == 920477027 && presetName.equals("Basic Drawer")) {
      b = 0;
    } else {
      b = -1;
    } 
    return (b != 0) ? new ArrayList<ViewBean>() : createBasicDrawerViews();
  }
  
  public static int getActivityPresetIcon(String presetName) {
    switch (presetName) {
      case "Empty Activity":
        return R.drawable.activity_preset_4;
      case "Basic Activity":
      case "Text Activity":
        return R.drawable.activity_preset_1;
      default:
        return -1;
    }
  }
  
  public static ProjectFileBean createBasicActivityFile() {
    return new ProjectFileBean(0, null, "Basic Activity", 0, 0, true, false, false, false);
  }
  
  public static ArrayList<ViewBean> createBasicActivityViews() {
    return new ArrayList<ViewBean>();
  }
  
  public static ArrayList<ViewBean> getActivityPresetViews(String presetName) {
    switch (presetName) {
      case "Empty Activity":
        return createEmptyActivityViews();
      case "Basic Activity":
        return createBasicActivityViews();
      case "Text Activity":
        return createTextActivityViews();
      default:
        return new ArrayList<ViewBean>();
    }
  }
  
  public static ProjectFileBean createBasicDrawerFile() {
    return new ProjectFileBean(2, null, "Basic Drawer");
  }
  
  public static ArrayList<ViewBean> createBasicDrawerViews() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    return arrayList;
  }
  
  public static ProjectFileBean createBasicListItemFile() {
    return new ProjectFileBean(1, null, "Basic List Item");
  }
  
  public static ArrayList<ViewBean> createBasicListItemViews() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    return arrayList;
  }
  
  public static ProjectFileBean createEmptyActivityFile() {
    return new ProjectFileBean(0, null, "Empty Activity", 0, 0, false, true, false, false);
  }
  
  public static ArrayList<ViewBean> createEmptyActivityViews() {
    return new ArrayList<ViewBean>();
  }
  
  public static ProjectFileBean createTextActivityFile() {
    return new ProjectFileBean(0, null, "Text Activity", 0, 0, true, false, false, false);
  }
  
  public static ArrayList<ViewBean> createTextActivityViews() {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    arrayList.add(createDefaultTextView());
    return arrayList;
  }
}
