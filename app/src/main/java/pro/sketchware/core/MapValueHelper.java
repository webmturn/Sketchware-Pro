package pro.sketchware.core;

import java.util.Map;

public class MapValueHelper {
  public static int get(Map<String, Object> valueMap, String mapKey, int defaultValue) {
    Object storedValue = get(valueMap, mapKey, Integer.valueOf(defaultValue));
    if (storedValue instanceof Integer) return ((Integer) storedValue).intValue();
    if (storedValue instanceof Double) return ((Double) storedValue).intValue();
    return defaultValue;
  }
  
  public static Object get(Map<String, Object> valueMap, String mapKey, Object fallbackValue) {
    if (valueMap == null)
      return fallbackValue;
    Object storedValue = valueMap.get(mapKey);
    return (storedValue == null) ? fallbackValue : storedValue;
  }
  
  public static boolean get(Map<String, Object> valueMap, String mapKey) {
    return ((Boolean)get(valueMap, mapKey, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean get(Map<String, Object> valueMap, String mapKey, boolean defaultValue) {
    return ((Boolean)get(valueMap, mapKey, Boolean.valueOf(defaultValue))).booleanValue();
  }
  
  public static int getInt(Map<String, Object> valueMap, String mapKey) {
    return get(valueMap, mapKey, -1);
  }
  
  public static String getString(Map<String, Object> valueMap, String mapKey) {
    return (String)get(valueMap, mapKey, "");
  }
}
