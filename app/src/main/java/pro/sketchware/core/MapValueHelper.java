package pro.sketchware.core;

import java.util.Map;

public class MapValueHelper {
  public static int get(Map<String, Object> map, String key, int index) {
    Object object = get(map, key, Integer.valueOf(index));
    return (object instanceof Integer) ? ((Integer)get(map, key, Integer.valueOf(index))).intValue() : ((object instanceof Double) ? ((Double)get(map, key, Integer.valueOf(index))).intValue() : index);
  }
  
  public static Object get(Map<String, Object> map, String key, Object obj) {
    if (map == null)
      return obj; 
    Object result = map.get(key);
    return (result == null) ? obj : result;
  }
  
  public static boolean get(Map<String, Object> map, String key) {
    return ((Boolean)get(map, key, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean get(Map<String, Object> map, String key, boolean flag) {
    return ((Boolean)get(map, key, Boolean.valueOf(flag))).booleanValue();
  }
  
  public static int getInt(Map<String, Object> map, String key) {
    return get(map, key, -1);
  }
  
  public static String getString(Map<String, Object> map, String key) {
    return (String)get(map, key, "");
  }
}
