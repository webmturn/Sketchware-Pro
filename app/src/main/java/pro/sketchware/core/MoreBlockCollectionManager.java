package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class MoreBlockCollectionManager extends BaseCollectionManager {
  public static MoreBlockCollectionManager instance;
  
  public Gson moreBlockGson = null;
  
  public MoreBlockCollectionManager() {
    i();
  }
  
  public static MoreBlockCollectionManager h() {
    if (instance == null) {
      synchronized (MoreBlockCollectionManager.class) {
        if (instance == null) {
          instance = new MoreBlockCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public MoreBlockCollectionBean a(String paramString) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(paramString))
        return new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, ProjectDataParser.a(this.moreBlockGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void a(String paramString1, String paramString2, ArrayList<BlockBean> paramArrayList, boolean paramBoolean) throws CompileException {
    if (this.collections == null)
      a(); 
    if (this.moreBlockGson == null)
      i(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(paramString1))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder stringBuilder = new StringBuilder();
    for (int bi = 0; bi < paramArrayList.size(); bi++) {
      BlockBean blockBean = paramArrayList.get(bi);
      stringBuilder.append(this.moreBlockGson.toJson(blockBean));
      stringBuilder.append("\n");
    }
    String str = stringBuilder.toString();
    this.collections.add(new CollectionBean(paramString1, str, paramString2));
    if (paramBoolean)
      e(); 
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
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("more_block");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
  }
  
  public ArrayList<MoreBlockCollectionBean> f() {
    if (this.collections == null)
      a(); 
    if (this.moreBlockGson == null)
      i(); 
    ArrayList<MoreBlockCollectionBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, ProjectDataParser.a(this.moreBlockGson, collectionBean.data))); 
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
    this.moreBlockGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
