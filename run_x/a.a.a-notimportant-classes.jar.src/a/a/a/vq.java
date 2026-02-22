package a.a.a;

public class vq {
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


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\vq.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */