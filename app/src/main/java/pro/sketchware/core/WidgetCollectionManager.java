package pro.sketchware.core;

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
  public static WidgetCollectionManager instance;
  
  public Gson widgetGson = null;
  
  public WidgetCollectionManager() {
    i();
  }
  
  public static WidgetCollectionManager h() {
    if (instance == null) {
      synchronized (WidgetCollectionManager.class) {
        if (instance == null) {
          instance = new WidgetCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public WidgetCollectionBean a(String paramString) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(paramString))
        return new WidgetCollectionBean(collectionBean.name, ProjectDataParser.b(this.widgetGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void a(String paramString1, String paramString2, boolean paramBoolean) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(paramString1)) {
        collectionBean.name = paramString2;
        break;
      } 
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, ArrayList<ViewBean> paramArrayList, boolean paramBoolean) throws CompileException {
    if (this.collections == null)
      a(); 
    if (this.widgetGson == null)
      i(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(paramString))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder stringBuilder = new StringBuilder();
    for (int vi = 0; vi < paramArrayList.size(); vi++) {
      ViewBean viewBean = paramArrayList.get(vi);
      stringBuilder.append(this.widgetGson.toJson(viewBean));
      stringBuilder.append("\n");
    }
    String str = stringBuilder.toString();
    this.collections.add(new CollectionBean(paramString, str));
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, boolean paramBoolean) {
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((CollectionBean)this.collections.get(j)).name.equals(paramString)) {
          this.collections.remove(j);
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
    stringBuilder.append(SketchwarePaths.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("widget");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("widget");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.dataDirPath = stringBuilder.toString();
  }
  
  public void c() {
    this.collections = new ArrayList<CollectionBean>();
    BufferedReader bufferedReader = null;
    try {
      if (this.fileUtil.e(this.collectionFilePath)) {
        String str = this.fileUtil.g(this.collectionFilePath);
        bufferedReader = new BufferedReader(new StringReader(str));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          if (line.length() <= 0)
            continue; 
          CollectionBean collectionBean = (CollectionBean)this.gson.fromJson(line, CollectionBean.class);
          this.collections.add(collectionBean);
        }
      }
    } catch (IOException iOException) {
      iOException.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public ArrayList<WidgetCollectionBean> f() {
    if (this.collections == null)
      a(); 
    if (this.widgetGson == null)
      i(); 
    ArrayList<WidgetCollectionBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new WidgetCollectionBean(collectionBean.name, ProjectDataParser.b(this.widgetGson, collectionBean.data))); 
    return arrayList;
  }
  
  public ArrayList<String> g() {
    if (this.collections == null)
      a(); 
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext())
      arrayList.add(((CollectionBean)iterator.next()).name); 
    return arrayList;
  }
  
  public final void i() {
    this.widgetGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
