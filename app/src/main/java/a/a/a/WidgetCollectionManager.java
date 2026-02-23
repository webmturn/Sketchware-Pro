package a.a.a;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.beans.WidgetCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

public class WidgetCollectionManager extends BaseCollectionManager {
  public static WidgetCollectionManager f;
  
  public Gson g = null;
  
  public WidgetCollectionManager() {
    i();
  }
  
  public static WidgetCollectionManager h() {
    if (f == null) {
      synchronized (WidgetCollectionManager.class) {
        if (f == null) {
          f = new WidgetCollectionManager();
        }
      }
    }
    return f;
  }
  
  public WidgetCollectionBean a(String paramString) {
    for (CollectionBean collectionBean : this.e) {
      if (collectionBean.name.equals(paramString))
        return new WidgetCollectionBean(collectionBean.name, gC.b(this.g, collectionBean.data)); 
    } 
    return null;
  }
  
  public void a(String paramString1, String paramString2, boolean paramBoolean) {
    for (CollectionBean collectionBean : this.e) {
      if (collectionBean.name.equals(paramString1)) {
        collectionBean.name = paramString2;
        break;
      } 
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, ArrayList<ViewBean> paramArrayList, boolean paramBoolean) throws yy {
    if (this.e == null)
      a(); 
    if (this.g == null)
      i(); 
    Iterator<CollectionBean> iterator = this.e.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(paramString))
        continue; 
      throw new yy("duplicate_name");
    } 
    StringBuilder stringBuilder = new StringBuilder();
    for (int vi = 0; vi < paramArrayList.size(); vi++) {
      ViewBean viewBean = paramArrayList.get(vi);
      stringBuilder.append(this.g.toJson(viewBean));
      stringBuilder.append("\n");
    }
    String str = stringBuilder.toString();
    this.e.add(new CollectionBean(paramString, str));
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, boolean paramBoolean) {
    int i = this.e.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((CollectionBean)this.e.get(j)).name.equals(paramString)) {
          this.e.remove(j);
          break;
        } 
        continue;
      } 
      break;
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void b() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(wq.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("widget");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.a = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(wq.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("widget");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.b = stringBuilder.toString();
  }
  
  public void c() {
    this.e = new ArrayList<CollectionBean>();
    BufferedReader bufferedReader = null;
    try {
      if (this.c.e(this.a)) {
        String str = this.c.g(this.a);
        bufferedReader = new BufferedReader(new StringReader(str));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          if (line.length() <= 0)
            continue; 
          CollectionBean collectionBean = (CollectionBean)this.d.fromJson(line, CollectionBean.class);
          this.e.add(collectionBean);
        }
      }
    } catch (IOException iOException) {
      iOException.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public ArrayList<WidgetCollectionBean> f() {
    if (this.e == null)
      a(); 
    if (this.g == null)
      i(); 
    ArrayList<WidgetCollectionBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.e)
      arrayList.add(new WidgetCollectionBean(collectionBean.name, gC.b(this.g, collectionBean.data))); 
    return arrayList;
  }
  
  public ArrayList<String> g() {
    if (this.e == null)
      a(); 
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.e.iterator();
    while (iterator.hasNext())
      arrayList.add(((CollectionBean)iterator.next()).name); 
    return arrayList;
  }
  
  public final void i() {
    this.g = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
