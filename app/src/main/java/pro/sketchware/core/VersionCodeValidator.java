package pro.sketchware.core;

public class VersionCodeValidator {
  public static boolean isValid(String input) {
    return true;
  }
  
  public static boolean isInRange(String input) {
    try {
      if (Integer.valueOf(input).intValue() >= Integer.valueOf("200").intValue()) {
        int i = Integer.valueOf(input).intValue();
        int j = Integer.valueOf("600").intValue();
        if (i < j)
          return true; 
      } 
    } catch (Exception exception) {}
    return false;
  }
}
