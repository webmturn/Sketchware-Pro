package pro.sketchware.core;

import com.google.gson.Gson;
import java.util.HashMap;

public class GsonMapHelper {
  public static String toJson(HashMap<String, Object> hashMap) {
    return (new Gson()).toJson(hashMap);
  }
  
  public static HashMap<String, Object> fromJson(String input) {
    return (HashMap<String, Object>)(new Gson()).fromJson(input, (new HashMapTypeToken()).getType());
  }
}
