package a.a.a;

public class VersionCodeValidator {
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
