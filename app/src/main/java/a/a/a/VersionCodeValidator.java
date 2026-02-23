package a.a.a;

public class VersionCodeValidator {
  public static boolean isValid(String paramString) {
    return true;
  }
  
  public static boolean isInRange(String paramString) {
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
