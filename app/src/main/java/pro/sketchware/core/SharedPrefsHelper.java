package pro.sketchware.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class SharedPrefsHelper {
  public SharedPreferences prefs;
  
  public SharedPreferences.Editor editor;
  
  public SharedPrefsHelper(Context context, String value) {
    prefs = context.getSharedPreferences(value, 0);
    editor = prefs.edit();
  }
  
  public int getInt(String value, int index) {
    return prefs.getInt(value, index);
  }
  
  public String getString(String key, String value) {
    return prefs.getString(key, value);
  }
  
  public void put(String key, Object data) {
    put(key, data, true);
  }
  
  public void put(String key, Object data, boolean flag) {
    if (data instanceof String) {
      editor.putString(key, (String)data);
    } else if (data instanceof Integer) {
      editor.putInt(key, ((Integer)data).intValue());
    } else if (data instanceof Long) {
      editor.putLong(key, ((Long)data).longValue());
    } else if (data instanceof Boolean) {
      editor.putBoolean(key, ((Boolean)data).booleanValue());
    } 
    if (flag)
      editor.commit(); 
  }
  
  public void putMap(String key, HashMap<String, Object> map) {
    put(key, GsonMapHelper.toJson(map));
  }
  
  public boolean clearAll() {
    editor.clear();
    return editor.commit();
  }
  
  public boolean remove(String value) {
    editor.remove(value);
    return editor.commit();
  }
  
  public boolean getBoolean(String value, boolean flag) {
    return prefs.getBoolean(value, flag);
  }
  
  public boolean commit() {
    return editor.commit();
  }
  
  public boolean contains(String value) {
    return prefs.contains(value);
  }
  
  public HashMap<String, Object> getAll() {
    HashMap<Object, Object> result;
    try {
      result = (HashMap)prefs.getAll();
    } catch (Exception exception) {
      result = new HashMap<>();
    } 
    return (HashMap)result;
  }
  
  public boolean getBooleanDefault(String value) {
    return prefs.getBoolean(value, false);
  }
  
  public int getIntDefault(String value) {
    return getInt(value, 0);
  }
  
  public long getLong(String value) {
    return prefs.getLong(value, 0L);
  }
  
  public String getStringDefault(String value) {
    return getString(value, "");
  }
  
  public HashMap<String, Object> getMap(String value) {
    value = getStringDefault(value);
    return value.isEmpty() ? new HashMap<>() : GsonMapHelper.fromJson(value);
  }
}
