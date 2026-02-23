package a.a.a;

public class ActivityConfigConstants {
  public static final String[] a = new String[] { "Default", "NoActionBar", "FullScreen" };
  
  public static final String[] b = new String[] { "Portrait", "Landscape", "Both" };
  
  public static String a(int paramInt) {
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
