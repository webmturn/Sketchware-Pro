package pro.sketchware.core;

import java.util.Map;

public class MapValueHelper {
  public static int get(Map<String, Object> map, String str, int index) {
    Object object = get(map, str, Integer.valueOf(index));
    return (object instanceof Integer) ? ((Integer)get(map, str, Integer.valueOf(index))).intValue() : ((object instanceof Double) ? ((Double)get(map, str, Integer.valueOf(index))).intValue() : index);
  }
  
  public static Object get(Map<String, Object> map, String str, Object obj) {
    if (map == null)
      return obj; 
    Object result = map.get(str);
    return (result == null) ? obj : result;
  }
  
  public static boolean get(Map<String, Object> map, String str) {
    return ((Boolean)get(map, str, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean get(Map<String, Object> map, String str, boolean flag) {
    return ((Boolean)get(map, str, Boolean.valueOf(flag))).booleanValue();
  }
  
  public static int getInt(Map<String, Object> map, String str) {
    return get(map, str, -1);
  }
  
  public static String getString(Map<String, Object> map, String str) {
    return (String)get(map, str, "");
  }
}
