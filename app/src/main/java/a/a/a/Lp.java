package a.a.a;

import com.besome.sketch.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;

public abstract class Lp {
  public String a;
  
  public String b;
  
  public oB c;
  
  public Gson d;
  
  public ArrayList<CollectionBean> e;
  
  public Lp() {
    a();
  }
  
  public void a() {
    b();
    this.c = new oB();
    this.d = (new GsonBuilder()).create();
    c();
  }
  
  public abstract void b();
  
  public void c() {
    this.e = new ArrayList<CollectionBean>();
    java.io.BufferedReader reader = null;
    try {
      String content = this.c.g(this.a);
      reader = new java.io.BufferedReader(new java.io.StringReader(content));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.length() <= 0) continue;
        CollectionBean bean = this.d.fromJson(line, CollectionBean.class);
        String path = this.b + java.io.File.separator + bean.data;
        if (this.c.e(path)) {
          this.e.add(bean);
        }
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception ignored) {}
    }
  }
  
  public void d() {
    ArrayList<CollectionBean> arrayList = this.e;
    if (arrayList != null) {
      arrayList.clear();
      this.e = null;
    } 
  }
  
  public void e() {
    if (this.e == null)
      return; 
    StringBuilder stringBuilder = new StringBuilder(1024);
    for (CollectionBean collectionBean : this.e) {
      stringBuilder.append(this.d.toJson(collectionBean));
      stringBuilder.append("\n");
    } 
    try {
      this.c.b(this.a);
      this.c.b(this.a, stringBuilder.toString());
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
