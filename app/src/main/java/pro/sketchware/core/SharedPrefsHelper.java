package pro.sketchware.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class SharedPrefsHelper {
  public SharedPreferences prefs;
  
  public SharedPreferences.Editor editor;
  
  public SharedPrefsHelper(Context context, String value) {
    this.prefs = context.getSharedPreferences(value, 0);
    this.editor = this.prefs.edit();
  }
  
  public int getInt(String value, int index) {
    return this.prefs.getInt(value, index);
  }
  
  public String getString(String key, String value) {
    return this.prefs.getString(key, value);
  }
  
  public void put(String value, Object obj) {
    put(value, obj, true);
  }
  
  public void put(String value, Object obj, boolean flag) {
    if (obj instanceof String) {
      this.editor.putString(value, (String)obj);
    } else if (obj instanceof Integer) {
      this.editor.putInt(value, ((Integer)obj).intValue());
    } else if (obj instanceof Long) {
      this.editor.putLong(value, ((Long)obj).longValue());
    } else if (obj instanceof Boolean) {
      this.editor.putBoolean(value, ((Boolean)obj).booleanValue());
    } 
    if (flag)
      this.editor.commit(); 
  }
  
  public void putMap(String value, HashMap<String, Object> hashMap) {
    put(value, GsonMapHelper.toJson(hashMap));
  }
  
  public boolean clearAll() {
    this.editor.clear();
    return this.editor.commit();
  }
  
  public boolean remove(String value) {
    this.editor.remove(value);
    return this.editor.commit();
  }
  
  public boolean getBoolean(String value, boolean flag) {
    return this.prefs.getBoolean(value, flag);
  }
  
  public boolean commit() {
    return this.editor.commit();
  }
  
  public boolean contains(String value) {
    return this.prefs.contains(value);
  }
  
  public HashMap<String, Object> getAll() {
    HashMap<Object, Object> hashMap;
    try {
      hashMap = (HashMap)this.prefs.getAll();
    } catch (Exception exception) {
      hashMap = new HashMap<Object, Object>();
    } 
    return (HashMap)hashMap;
  }
  
  public boolean getBooleanDefault(String value) {
    return this.prefs.getBoolean(value, false);
  }
  
  public int getIntDefault(String value) {
    return getInt(value, 0);
  }
  
  public long getLong(String value) {
    return this.prefs.getLong(value, 0L);
  }
  
  public String getStringDefault(String value) {
    return getString(value, "");
  }
  
  public HashMap<String, Object> getMap(String value) {
    value = getStringDefault(value);
    return value.isEmpty() ? new HashMap<String, Object>() : GsonMapHelper.fromJson(value);
  }
}
