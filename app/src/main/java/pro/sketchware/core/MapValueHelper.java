package pro.sketchware.core;

import java.util.Map;

public class MapValueHelper {
  public static int get(Map<String, Object> paramMap, String paramString, int paramInt) {
    Object object = get(paramMap, paramString, Integer.valueOf(paramInt));
    return (object instanceof Integer) ? ((Integer)get(paramMap, paramString, Integer.valueOf(paramInt))).intValue() : ((object instanceof Double) ? ((Double)get(paramMap, paramString, Integer.valueOf(paramInt))).intValue() : paramInt);
  }
  
  public static Object get(Map<String, Object> paramMap, String paramString, Object paramObject) {
    if (paramMap == null)
      return paramObject; 
    Object result = paramMap.get(paramString);
    return (result == null) ? paramObject : result;
  }
  
  public static boolean get(Map<String, Object> paramMap, String paramString) {
    return ((Boolean)get(paramMap, paramString, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean get(Map<String, Object> paramMap, String paramString, boolean paramBoolean) {
    return ((Boolean)get(paramMap, paramString, Boolean.valueOf(paramBoolean))).booleanValue();
  }
  
  public static int getInt(Map<String, Object> paramMap, String paramString) {
    return get(paramMap, paramString, -1);
  }
  
  public static String getString(Map<String, Object> paramMap, String paramString) {
    return (String)get(paramMap, paramString, "");
  }
}
