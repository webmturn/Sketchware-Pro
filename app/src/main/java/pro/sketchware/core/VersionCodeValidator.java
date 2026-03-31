package pro.sketchware.core;

import mod.jbk.util.LogUtil;

public class VersionCodeValidator {
  public static boolean isValid(String versionCodeText) {
    return true;
  }
  
  public static boolean isInRange(String versionCodeText) {
    try {
      if (Integer.valueOf(versionCodeText).intValue() >= Integer.valueOf("200").intValue()) {
        int versionCode = Integer.valueOf(versionCodeText).intValue();
        int maxVersionCode = Integer.valueOf("600").intValue();
        if (versionCode < maxVersionCode)
          return true; 
      } 
    } catch (Exception e) {
      LogUtil.w("VersionCodeValidator", "Failed to validate version code: " + versionCodeText, e);
    }
    return false;
  }
}
