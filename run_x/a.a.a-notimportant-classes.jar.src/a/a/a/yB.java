package a.a.a;

import java.util.Map;

public class yB {
  public static int a(Map<String, Object> paramMap, String paramString, int paramInt) {
    Object object = a(paramMap, paramString, Integer.valueOf(paramInt));
    return (object instanceof Integer) ? ((Integer)a(paramMap, paramString, Integer.valueOf(paramInt))).intValue() : ((object instanceof Double) ? ((Double)a(paramMap, paramString, Integer.valueOf(paramInt))).intValue() : paramInt);
  }
  
  public static Object a(Map<String, Object> paramMap, String paramString, Object paramObject) {
    if (paramMap == null)
      return paramObject; 
    paramMap = (Map<String, Object>)paramMap.get(paramString);
    return (paramMap == null) ? paramObject : paramMap;
  }
  
  public static boolean a(Map<String, Object> paramMap, String paramString) {
    return ((Boolean)a(paramMap, paramString, Boolean.valueOf(false))).booleanValue();
  }
  
  public static boolean a(Map<String, Object> paramMap, String paramString, boolean paramBoolean) {
    return ((Boolean)a(paramMap, paramString, Boolean.valueOf(paramBoolean))).booleanValue();
  }
  
  public static int b(Map<String, Object> paramMap, String paramString) {
    return a(paramMap, paramString, -1);
  }
  
  public static String c(Map<String, Object> paramMap, String paramString) {
    return (String)a(paramMap, paramString, "");
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\yB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */