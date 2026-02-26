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
  public static volatile BlockCollectionManager instance;
  
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
  
  public BlockCollectionBean getBlockByName(String name) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(name))
        return new BlockCollectionBean(collectionBean.name, ProjectDataParser.parseBlockBeans(this.blockGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void renameBlock(String key, String value, boolean flag) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(key)) {
        collectionBean.name = value;
        break;
      } 
    } 
    if (flag)
      saveCollections(); 
  }
  
  public void addBlock(String input, ArrayList<BlockBean> list, boolean flag) throws CompileException {
    if (this.collections == null)
      initialize(); 
    if (this.blockGson == null)
      initializeGson(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(input))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder contentBuilder = new StringBuilder();
    for (int bi = 0; bi < list.size(); bi++) {
      BlockBean blockBean = list.get(bi);
      contentBuilder.append(this.blockGson.toJson(blockBean));
      contentBuilder.append("\n");
    }
    String serializedData = contentBuilder.toString();
    this.collections.add(new CollectionBean(input, serializedData));
    if (flag)
      saveCollections(); 
  }
  
  public void removeBlock(String name, boolean flag) {
    int size = this.collections.size();
    while (true) {
      int idx = size - 1;
      if (idx >= 0) {
        size = idx;
        if (((CollectionBean)this.collections.get(idx)).name.equals(name)) {
          this.collections.remove(idx);
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
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(SketchwarePaths.getCollectionPath());
    pathBuilder.append(File.separator);
    pathBuilder.append("block");
    pathBuilder.append(File.separator);
    pathBuilder.append("list");
    this.collectionFilePath = pathBuilder.toString();
    pathBuilder = new StringBuilder();
    pathBuilder.append(SketchwarePaths.getCollectionPath());
    pathBuilder.append(File.separator);
    pathBuilder.append("block");
    pathBuilder.append(File.separator);
    pathBuilder.append("data");
    this.dataDirPath = pathBuilder.toString();
  }
  
  public ArrayList<BlockCollectionBean> getBlocks() {
    if (this.collections == null)
      initialize(); 
    if (this.blockGson == null)
      initializeGson(); 
    ArrayList<BlockCollectionBean> blockCollections = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      blockCollections.add(new BlockCollectionBean(collectionBean.name, ProjectDataParser.parseBlockBeans(this.blockGson, collectionBean.data))); 
    return blockCollections;
  }
  
  public ArrayList<String> getBlockNames() {
    if (this.collections == null)
      initialize(); 
    ArrayList<String> blockNames = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext())
      blockNames.add(((CollectionBean)iterator.next()).name); 
    return blockNames;
  }
  
  public final void initializeGson() {
    this.blockGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
