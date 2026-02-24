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
  
  public int a(String paramString, int paramInt) {
    return this.prefs.getInt(paramString, paramInt);
  }
  
  public String a(String paramString1, String paramString2) {
    return this.prefs.getString(paramString1, paramString2);
  }
  
  public void a(String paramString, Object paramObject) {
    a(paramString, paramObject, true);
  }
  
  public void a(String paramString, Object paramObject, boolean paramBoolean) {
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
  
  public void a(String paramString, HashMap<String, Object> paramHashMap) {
    a(paramString, GsonMapHelper.toJson(paramHashMap));
  }
  
  public boolean a() {
    this.editor.clear();
    return this.editor.commit();
  }
  
  public boolean a(String paramString) {
    this.editor.remove(paramString);
    return this.editor.commit();
  }
  
  public boolean a(String paramString, boolean paramBoolean) {
    return this.prefs.getBoolean(paramString, paramBoolean);
  }
  
  public boolean b() {
    return this.editor.commit();
  }
  
  public boolean b(String paramString) {
    return this.prefs.contains(paramString);
  }
  
  public HashMap<String, Object> c() {
    HashMap<Object, Object> hashMap;
    try {
      hashMap = (HashMap)this.prefs.getAll();
    } catch (Exception exception) {
      hashMap = new HashMap<Object, Object>();
    } 
    return (HashMap)hashMap;
  }
  
  public boolean c(String paramString) {
    return this.prefs.getBoolean(paramString, false);
  }
  
  public int d(String paramString) {
    return a(paramString, 0);
  }
  
  public long e(String paramString) {
    return this.prefs.getLong(paramString, 0L);
  }
  
  public String f(String paramString) {
    return a(paramString, "");
  }
  
  public HashMap<String, Object> g(String paramString) {
    paramString = f(paramString);
    return paramString.isEmpty() ? new HashMap<String, Object>() : GsonMapHelper.fromJson(paramString);
  }
}
