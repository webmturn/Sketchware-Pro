package pro.sketchware.core;

import com.google.gson.Gson;
import java.util.HashMap;

public class GsonMapHelper {
  private static final Gson GSON = new Gson();

  public static String toJson(HashMap<String, Object> valueMap) {
    return GSON.toJson(valueMap);
  }
  
  public static HashMap<String, Object> fromJson(String jsonText) {
    return (HashMap<String, Object>)GSON.fromJson(jsonText, (new HashMapTypeToken()).getType());
  }
}
