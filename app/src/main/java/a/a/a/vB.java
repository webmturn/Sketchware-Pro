package a.a.a;

import com.google.gson.Gson;
import java.util.HashMap;

public class vB {
  public static String a(HashMap<String, Object> paramHashMap) {
    return (new Gson()).toJson(paramHashMap);
  }
  
  public static HashMap<String, Object> a(String paramString) {
    return (HashMap<String, Object>)(new Gson()).fromJson(paramString, (new uB()).getType());
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\vB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */