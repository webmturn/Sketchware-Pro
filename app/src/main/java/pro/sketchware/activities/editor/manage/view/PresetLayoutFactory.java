package pro.sketchware.activities.editor.manage.view;

import pro.sketchware.beans.LayoutBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import java.util.ArrayList;
import pro.sketchware.R;

public class PresetLayoutFactory {
  public static int getListItemPresetIcon(String presetName) {
    return "Basic List Item".equals(presetName) ? R.drawable.activity_preset_1 : -1;
  }
  
  public static ViewBean createDefaultTextView() {
    ViewBean viewBean = new ViewBean("textview1", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
    viewBean.parent = "root";
    viewBean.index = 0;
    viewBean.preParentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
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
    ArrayList<ProjectFileBean> presets = new ArrayList<>();
    presets.add(createBasicListItemFile());
    return presets;
  }
  
  public static ArrayList<ViewBean> getListItemPresetViews(String presetName) {
    return "Basic List Item".equals(presetName) ? createBasicListItemViews() : new ArrayList<ViewBean>();
  }
  
  public static int getDrawerPresetIcon(String presetName) {
    return "Basic Drawer".equals(presetName) ? R.drawable.activity_preset_1 : -1;
  }
  
  public static ArrayList<ProjectFileBean> getDrawerPresets() {
    ArrayList<ProjectFileBean> presets = new ArrayList<>();
    presets.add(createBasicDrawerFile());
    return presets;
  }
  
  public static ArrayList<ProjectFileBean> getActivityPresets() {
    ArrayList<ProjectFileBean> presets = new ArrayList<>();
    presets.add(createEmptyActivityFile());
    presets.add(createBasicActivityFile());
    presets.add(createTextActivityFile());
    return presets;
  }
  
  public static ArrayList<ViewBean> getDrawerPresetViews(String presetName) {
    return "Basic Drawer".equals(presetName) ? createBasicDrawerViews() : new ArrayList<ViewBean>();
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
    return new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, null, "Basic Activity",
        ProjectFileBean.ORIENTATION_PORTRAIT, ProjectFileBean.KEYBOARD_STATE_UNSPECIFIED, true, false, false, false);
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
    return new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_DRAWER, null, "Basic Drawer");
  }
  
  public static ArrayList<ViewBean> createBasicDrawerViews() {
    ArrayList<ViewBean> views = new ArrayList<>();
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    return views;
  }
  
  public static ProjectFileBean createBasicListItemFile() {
    return new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, null, "Basic List Item");
  }
  
  public static ArrayList<ViewBean> createBasicListItemViews() {
    ArrayList<ViewBean> views = new ArrayList<>();
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    return views;
  }
  
  public static ProjectFileBean createEmptyActivityFile() {
    return new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, null, "Empty Activity",
        ProjectFileBean.ORIENTATION_PORTRAIT, ProjectFileBean.KEYBOARD_STATE_UNSPECIFIED, false, true, false, false);
  }
  
  public static ArrayList<ViewBean> createEmptyActivityViews() {
    return new ArrayList<ViewBean>();
  }
  
  public static ProjectFileBean createTextActivityFile() {
    return new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, null, "Text Activity",
        ProjectFileBean.ORIENTATION_PORTRAIT, ProjectFileBean.KEYBOARD_STATE_UNSPECIFIED, true, false, false, false);
  }
  
  public static ArrayList<ViewBean> createTextActivityViews() {
    ArrayList<ViewBean> views = new ArrayList<>();
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    views.add(createDefaultTextView());
    return views;
  }
}
