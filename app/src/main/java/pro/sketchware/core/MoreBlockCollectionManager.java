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
    initializeGson();
  }
  
  public static MoreBlockCollectionManager getInstance() {
    if (instance == null) {
      synchronized (MoreBlockCollectionManager.class) {
        if (instance == null) {
          instance = new MoreBlockCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public MoreBlockCollectionBean getMoreBlockByName(String name) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(name))
        return new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, ProjectDataParser.parseBlockBeans(this.moreBlockGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void addMoreBlock(String key, String value, ArrayList<BlockBean> list, boolean flag) throws CompileException {
    if (this.collections == null)
      initialize(); 
    if (this.moreBlockGson == null)
      initializeGson(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(key))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder stringBuilder = new StringBuilder();
    for (int bi = 0; bi < list.size(); bi++) {
      BlockBean blockBean = list.get(bi);
      stringBuilder.append(this.moreBlockGson.toJson(blockBean));
      stringBuilder.append("\n");
    }
    String str = stringBuilder.toString();
    this.collections.add(new CollectionBean(key, str, value));
    if (flag)
      saveCollections(); 
  }
  
  public void renameMoreBlock(String key, String value, boolean flag) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(key)) {
        collectionBean.name = value;
        break;
      } 
    } 
    if (flag)
      saveCollections(); 
  }
  
  public void removeMoreBlock(String name, boolean flag) {
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        i = j;
        if (((CollectionBean)this.collections.get(j)).name.equals(name)) {
          this.collections.remove(j);
          break;
        } 
        continue;
      } 
      break;
    } 
    if (flag)
      saveCollections(); 
  }
  
  public void initializePaths() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("more_block");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
  }
  
  public ArrayList<MoreBlockCollectionBean> getMoreBlocks() {
    if (this.collections == null)
      initialize(); 
    if (this.moreBlockGson == null)
      initializeGson(); 
    ArrayList<MoreBlockCollectionBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new MoreBlockCollectionBean(collectionBean.name, collectionBean.reserved1, ProjectDataParser.parseBlockBeans(this.moreBlockGson, collectionBean.data))); 
    return arrayList;
  }
  
  public ArrayList<String> getMoreBlockNames() {
    if (this.collections == null)
      initialize(); 
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext())
      arrayList.add(((CollectionBean)iterator.next()).name); 
    return arrayList;
  }
  
  public final void initializeGson() {
    this.moreBlockGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
