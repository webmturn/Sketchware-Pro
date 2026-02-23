package pro.sketchware.core;

public class ActivityConfigConstants {
  public static final String[] THEME_OPTIONS = new String[] { "Default", "NoActionBar", "FullScreen" };
  
  public static final String[] ORIENTATION_OPTIONS = new String[] { "Portrait", "Landscape", "Both" };
  
  public static String getKeyboardSettingName(int paramInt) {
    String str;
    paramInt &= 0xF;
    if (paramInt == 1) {
      str = "stateVisible";
    } else if (paramInt == 2) {
      str = "stateHidden";
    } else {
      str = "";
    } 
    return str;
  }
}
