package pro.sketchware.core;

public class VersionCodeValidator {
  public static boolean isValid(String input) {
    return true;
  }
  
  public static boolean isInRange(String input) {
    try {
      if (Integer.valueOf(input).intValue() >= Integer.valueOf("200").intValue()) {
        int inputValue = Integer.valueOf(input).intValue();
        int maxValue = Integer.valueOf("600").intValue();
        if (inputValue < maxValue)
          return true; 
      } 
    } catch (Exception exception) {
      android.util.Log.w("VersionCodeValidator", "Failed to validate version code: " + input, exception);
    }
    return false;
  }
}
