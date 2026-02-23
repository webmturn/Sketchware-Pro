package pro.sketchware.core;

import com.google.gson.Gson;
import java.util.HashMap;

public class GsonMapHelper {
  public static String a(HashMap<String, Object> paramHashMap) {
    return (new Gson()).toJson(paramHashMap);
  }
  
  public static HashMap<String, Object> a(String paramString) {
    return (HashMap<String, Object>)(new Gson()).fromJson(paramString, (new HashMapTypeToken()).getType());
  }
}
