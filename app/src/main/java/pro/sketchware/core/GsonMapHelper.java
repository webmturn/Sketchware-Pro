package pro.sketchware.core;

import com.google.gson.Gson;
import java.util.HashMap;

public class GsonMapHelper {
  private static final Gson GSON = new Gson();

  public static String toJson(HashMap<String, Object> map) {
    return GSON.toJson(map);
  }
  
  public static HashMap<String, Object> fromJson(String input) {
    return (HashMap<String, Object>)GSON.fromJson(input, (new HashMapTypeToken()).getType());
  }
}
