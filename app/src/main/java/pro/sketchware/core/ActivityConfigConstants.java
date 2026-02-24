package pro.sketchware.core;

public class ActivityConfigConstants {
  public static final String[] THEME_OPTIONS = new String[] { "Default", "NoActionBar", "FullScreen" };
  
  public static final String[] ORIENTATION_OPTIONS = new String[] { "Portrait", "Landscape", "Both" };
  
  public static String getKeyboardSettingName(int value) {
    String str;
    value &= 0xF;
    if (value == 1) {
      str = "stateVisible";
    } else if (value == 2) {
      str = "stateHidden";
    } else {
      str = "";
    } 
    return str;
  }
}
