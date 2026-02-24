package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.BlockCollectionBean;
import com.besome.sketch.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class BlockCollectionManager extends BaseCollectionManager {
  public static BlockCollectionManager instance;
  
  public Gson blockGson = null;
  
  public BlockCollectionManager() {
    initializeGson();
  }
  
  public static BlockCollectionManager getInstance() {
    if (instance == null) {
      synchronized (BlockCollectionManager.class) {
        if (instance == null) {
          instance = new BlockCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public BlockCollectionBean getBlockByName(String paramString) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(paramString))
        return new BlockCollectionBean(collectionBean.name, ProjectDataParser.parseBlockBeans(this.blockGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void renameBlock(String paramString1, String paramString2, boolean paramBoolean) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(paramString1)) {
        collectionBean.name = paramString2;
        break;
      } 
    } 
    if (paramBoolean)
      saveCollections(); 
  }
  
  public void addBlock(String paramString, ArrayList<BlockBean> paramArrayList, boolean paramBoolean) throws CompileException {
    if (this.collections == null)
      initialize(); 
    if (this.blockGson == null)
      initializeGson(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(paramString))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder stringBuilder = new StringBuilder();
    for (int bi = 0; bi < paramArrayList.size(); bi++) {
      BlockBean blockBean = paramArrayList.get(bi);
      stringBuilder.append(this.blockGson.toJson(blockBean));
      stringBuilder.append("\n");
    }
    String str = stringBuilder.toString();
    this.collections.add(new CollectionBean(paramString, str));
    if (paramBoolean)
      saveCollections(); 
  }
  
  public void removeBlock(String paramString, boolean paramBoolean) {
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
      saveCollections(); 
  }
  
  public void initializePaths() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("block");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("block");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.dataDirPath = stringBuilder.toString();
  }
  
  public ArrayList<BlockCollectionBean> getBlocks() {
    if (this.collections == null)
      initialize(); 
    if (this.blockGson == null)
      initializeGson(); 
    ArrayList<BlockCollectionBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new BlockCollectionBean(collectionBean.name, ProjectDataParser.parseBlockBeans(this.blockGson, collectionBean.data))); 
    return arrayList;
  }
  
  public ArrayList<String> getBlockNames() {
    if (this.collections == null)
      initialize(); 
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext())
      arrayList.add(((CollectionBean)iterator.next()).name); 
    return arrayList;
  }
  
  public final void initializeGson() {
    this.blockGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
