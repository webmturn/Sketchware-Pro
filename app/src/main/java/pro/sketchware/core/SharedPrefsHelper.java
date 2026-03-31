package pro.sketchware.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class SharedPrefsHelper {
  public SharedPreferences prefs;
  
  public SharedPreferences.Editor editor;
  
  public SharedPrefsHelper(Context context, String prefsName) {
    prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    editor = prefs.edit();
  }
  
  public int getInt(String preferenceKey, int defaultValue) {
    return prefs.getInt(preferenceKey, defaultValue);
  }
  
  public String getString(String preferenceKey, String defaultValue) {
    return prefs.getString(preferenceKey, defaultValue);
  }
  
  public void put(String preferenceKey, Object storedValue) {
    put(preferenceKey, storedValue, true);
  }
  
  public void put(String preferenceKey, Object storedValue, boolean applyImmediately) {
    if (storedValue instanceof String) {
      editor.putString(preferenceKey, (String)storedValue);
    } else if (storedValue instanceof Integer) {
      editor.putInt(preferenceKey, ((Integer)storedValue).intValue());
    } else if (storedValue instanceof Long) {
      editor.putLong(preferenceKey, ((Long)storedValue).longValue());
    } else if (storedValue instanceof Boolean) {
      editor.putBoolean(preferenceKey, ((Boolean)storedValue).booleanValue());
    } 
    if (applyImmediately)
      editor.apply(); 
  }
  
  public void putMap(String preferenceKey, HashMap<String, Object> valueMap) {
    put(preferenceKey, GsonMapHelper.toJson(valueMap));
  }
  
  public boolean clearAll() {
    editor.clear();
    editor.apply();
    return true;
  }
  
  public boolean remove(String preferenceKey) {
    editor.remove(preferenceKey);
    editor.apply();
    return true;
  }
  
  public boolean getBoolean(String preferenceKey, boolean defaultValue) {
    return prefs.getBoolean(preferenceKey, defaultValue);
  }
  
  public boolean commit() {
    editor.apply();
    return true;
  }
  
  public boolean contains(String preferenceKey) {
    return prefs.contains(preferenceKey);
  }
  
  public HashMap<String, Object> getAll() {
    HashMap<Object, Object> allEntries;
    try {
      allEntries = (HashMap)prefs.getAll();
    } catch (Exception exception) {
      allEntries = new HashMap<>();
    } 
    return (HashMap)allEntries;
  }
  
  public boolean getBooleanDefault(String preferenceKey) {
    return prefs.getBoolean(preferenceKey, false);
  }
  
  public int getIntDefault(String preferenceKey) {
    return getInt(preferenceKey, 0);
  }
  
  public long getLong(String preferenceKey) {
    return prefs.getLong(preferenceKey, 0L);
  }
  
  public String getStringDefault(String preferenceKey) {
    return getString(preferenceKey, "");
  }
  
  public HashMap<String, Object> getMap(String preferenceKey) {
    String jsonText = getStringDefault(preferenceKey);
    return jsonText.isEmpty() ? new HashMap<>() : GsonMapHelper.fromJson(jsonText);
  }
}
