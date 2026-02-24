package pro.sketchware.core;

import java.util.Map;

public class MapValueHelper {
  public static int get(Map<String, Object> paramMap, String str, int index) {
    Object object = get(paramMap, str, Integer.valueOf(index));
    return (object instanceof Integer) ? ((Integer)get(paramMap, str, Integer.valueOf(index))).intValue() : ((object instanceof Double) ? ((Double)get(paramMap, str, Integer.valueOf(index))).intValue() : index);
  }
  
  public static Object get(Map<String, Object> paramMap, String str, Object paramObject) {
    if (paramMap == null)
      return paramObject; 
    Object result = paramMap.get(str);
    return (result == null) ? paramObject : result;
  }
  
  public static boolean get(Map<String, Object> paramMap, String str) {
    return ((Boolean)get(paramMap, str, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean get(Map<String, Object> paramMap, String str, boolean flag) {
    return ((Boolean)get(paramMap, str, Boolean.valueOf(flag))).booleanValue();
  }
  
  public static int getInt(Map<String, Object> paramMap, String str) {
    return get(paramMap, str, -1);
  }
  
  public static String getString(Map<String, Object> paramMap, String str) {
    return (String)get(paramMap, str, "");
  }
}
