package a.a.a;

public class xq {
  public static boolean a(String paramString) {
    return true;
  }
  
  public static boolean b(String paramString) {
    try {
      if (Integer.valueOf(paramString).intValue() >= Integer.valueOf("200").intValue()) {
        int i = Integer.valueOf(paramString).intValue();
        int j = Integer.valueOf("600").intValue();
        if (i < j)
          return true; 
      } 
    } catch (Exception exception) {}
    return false;
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\xq.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */