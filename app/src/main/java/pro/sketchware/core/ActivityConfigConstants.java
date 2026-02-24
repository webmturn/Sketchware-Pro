package pro.sketchware.core;

public class ActivityConfigConstants {
  public static final String[] THEME_OPTIONS = new String[] { "Default", "NoActionBar", "FullScreen" };
  
  public static final String[] ORIENTATION_OPTIONS = new String[] { "Portrait", "Landscape", "Both" };
  
  public static String getKeyboardSettingName(int value) {
    String settingName;
    value &= 0xF;
    if (value == 1) {
      settingName = "stateVisible";
    } else if (value == 2) {
      settingName = "stateHidden";
    } else {
      settingName = "";
    } 
    return settingName;
  }
}
