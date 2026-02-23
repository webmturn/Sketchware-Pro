package pro.sketchware.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;

public class SharedPrefsHelper {
  public SharedPreferences a;
  
  public SharedPreferences.Editor b;
  
  public SharedPrefsHelper(Context paramContext, String paramString) {
    this.a = paramContext.getSharedPreferences(paramString, 0);
    this.b = this.a.edit();
  }
  
  public int a(String paramString, int paramInt) {
    return this.a.getInt(paramString, paramInt);
  }
  
  public String a(String paramString1, String paramString2) {
    return this.a.getString(paramString1, paramString2);
  }
  
  public void a(String paramString, Object paramObject) {
    a(paramString, paramObject, true);
  }
  
  public void a(String paramString, Object paramObject, boolean paramBoolean) {
    if (paramObject instanceof String) {
      this.b.putString(paramString, (String)paramObject);
    } else if (paramObject instanceof Integer) {
      this.b.putInt(paramString, ((Integer)paramObject).intValue());
    } else if (paramObject instanceof Long) {
      this.b.putLong(paramString, ((Long)paramObject).longValue());
    } else if (paramObject instanceof Boolean) {
      this.b.putBoolean(paramString, ((Boolean)paramObject).booleanValue());
    } 
    if (paramBoolean)
      this.b.commit(); 
  }
  
  public void a(String paramString, HashMap<String, Object> paramHashMap) {
    a(paramString, GsonMapHelper.a(paramHashMap));
  }
  
  public boolean a() {
    this.b.clear();
    return this.b.commit();
  }
  
  public boolean a(String paramString) {
    this.b.remove(paramString);
    return this.b.commit();
  }
  
  public boolean a(String paramString, boolean paramBoolean) {
    return this.a.getBoolean(paramString, paramBoolean);
  }
  
  public boolean b() {
    return this.b.commit();
  }
  
  public boolean b(String paramString) {
    return this.a.contains(paramString);
  }
  
  public HashMap<String, Object> c() {
    HashMap<Object, Object> hashMap;
    try {
      hashMap = (HashMap)this.a.getAll();
    } catch (Exception exception) {
      hashMap = new HashMap<Object, Object>();
    } 
    return (HashMap)hashMap;
  }
  
  public boolean c(String paramString) {
    return this.a.getBoolean(paramString, false);
  }
  
  public int d(String paramString) {
    return a(paramString, 0);
  }
  
  public long e(String paramString) {
    return this.a.getLong(paramString, 0L);
  }
  
  public String f(String paramString) {
    return a(paramString, "");
  }
  
  public HashMap<String, Object> g(String paramString) {
    paramString = f(paramString);
    return paramString.isEmpty() ? new HashMap<String, Object>() : GsonMapHelper.a(paramString);
  }
}
