package pro.sketchware.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class SharedPrefsHelper {
  public SharedPreferences prefs;
  
  public SharedPreferences.Editor editor;
  
  public SharedPrefsHelper(Context paramContext, String paramString) {
    this.prefs = paramContext.getSharedPreferences(paramString, 0);
    this.editor = this.prefs.edit();
  }
  
  public int getInt(String paramString, int paramInt) {
    return this.prefs.getInt(paramString, paramInt);
  }
  
  public String getString(String paramString1, String paramString2) {
    return this.prefs.getString(paramString1, paramString2);
  }
  
  public void put(String paramString, Object paramObject) {
    put(paramString, paramObject, true);
  }
  
  public void put(String paramString, Object paramObject, boolean paramBoolean) {
    if (paramObject instanceof String) {
      this.editor.putString(paramString, (String)paramObject);
    } else if (paramObject instanceof Integer) {
      this.editor.putInt(paramString, ((Integer)paramObject).intValue());
    } else if (paramObject instanceof Long) {
      this.editor.putLong(paramString, ((Long)paramObject).longValue());
    } else if (paramObject instanceof Boolean) {
      this.editor.putBoolean(paramString, ((Boolean)paramObject).booleanValue());
    } 
    if (paramBoolean)
      this.editor.commit(); 
  }
  
  public void putMap(String paramString, HashMap<String, Object> paramHashMap) {
    put(paramString, GsonMapHelper.toJson(paramHashMap));
  }
  
  public boolean clearAll() {
    this.editor.clear();
    return this.editor.commit();
  }
  
  public boolean remove(String paramString) {
    this.editor.remove(paramString);
    return this.editor.commit();
  }
  
  public boolean getBoolean(String paramString, boolean paramBoolean) {
    return this.prefs.getBoolean(paramString, paramBoolean);
  }
  
  public boolean commit() {
    return this.editor.commit();
  }
  
  public boolean contains(String paramString) {
    return this.prefs.contains(paramString);
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
  
  public boolean getBooleanDefault(String paramString) {
    return this.prefs.getBoolean(paramString, false);
  }
  
  public int getIntDefault(String paramString) {
    return getInt(paramString, 0);
  }
  
  public long getLong(String paramString) {
    return this.prefs.getLong(paramString, 0L);
  }
  
  public String getStringDefault(String paramString) {
    return getString(paramString, "");
  }
  
  public HashMap<String, Object> getMap(String paramString) {
    paramString = getStringDefault(paramString);
    return paramString.isEmpty() ? new HashMap<String, Object>() : GsonMapHelper.fromJson(paramString);
  }
}
