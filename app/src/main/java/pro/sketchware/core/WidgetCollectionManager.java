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
  public static volatile WidgetCollectionManager instance;
  
  public Gson widgetGson = null;
  
  public WidgetCollectionManager() {
    initializeGson();
  }
  
  public static WidgetCollectionManager getInstance() {
    if (instance == null) {
      synchronized (WidgetCollectionManager.class) {
        if (instance == null) {
          instance = new WidgetCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public WidgetCollectionBean getWidgetByName(String name) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(name))
        return new WidgetCollectionBean(collectionBean.name, ProjectDataParser.parseViewBeans(this.widgetGson, collectionBean.data)); 
    } 
    return null;
  }
  
  public void renameWidget(String key, String value, boolean flag) {
    for (CollectionBean collectionBean : this.collections) {
      if (collectionBean.name.equals(key)) {
        collectionBean.name = value;
        break;
      } 
    } 
    if (flag)
      saveCollections(); 
  }
  
  public void addWidget(String input, ArrayList<ViewBean> list, boolean flag) throws CompileException {
    if (this.collections == null)
      initialize(); 
    if (this.widgetGson == null)
      initializeGson(); 
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext()) {
      if (!((CollectionBean)iterator.next()).name.equals(input))
        continue; 
      throw new CompileException("duplicate_name");
    } 
    StringBuilder contentBuilder = new StringBuilder();
    for (int vi = 0; vi < list.size(); vi++) {
      ViewBean viewBean = list.get(vi);
      contentBuilder.append(this.widgetGson.toJson(viewBean));
      contentBuilder.append("\n");
    }
    String serializedData = contentBuilder.toString();
    this.collections.add(new CollectionBean(input, serializedData));
    if (flag)
      saveCollections(); 
  }
  
  public void removeWidget(String name, boolean flag) {
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
    pathBuilder.append("widget");
    pathBuilder.append(File.separator);
    pathBuilder.append("list");
    this.collectionFilePath = pathBuilder.toString();
    pathBuilder = new StringBuilder();
    pathBuilder.append(SketchwarePaths.getCollectionPath());
    pathBuilder.append(File.separator);
    pathBuilder.append("widget");
    pathBuilder.append(File.separator);
    pathBuilder.append("data");
    this.dataDirPath = pathBuilder.toString();
  }
  
  public void loadCollections() {
    this.collections = new ArrayList<CollectionBean>();
    BufferedReader bufferedReader = null;
    try {
      if (this.fileUtil.exists(this.collectionFilePath)) {
        String fileContent = this.fileUtil.readFile(this.collectionFilePath);
        bufferedReader = new BufferedReader(new StringReader(fileContent));
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
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { android.util.Log.w("WidgetCollectionManager", "Failed to close reader", e); }
    }
  }
  
  public ArrayList<WidgetCollectionBean> getWidgets() {
    if (this.collections == null)
      initialize(); 
    if (this.widgetGson == null)
      initializeGson(); 
    ArrayList<WidgetCollectionBean> widgetCollections = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      widgetCollections.add(new WidgetCollectionBean(collectionBean.name, ProjectDataParser.parseViewBeans(this.widgetGson, collectionBean.data))); 
    return widgetCollections;
  }
  
  public ArrayList<String> getWidgetNames() {
    if (this.collections == null)
      initialize(); 
    ArrayList<String> widgetNames = new ArrayList<>();
    Iterator<CollectionBean> iterator = this.collections.iterator();
    while (iterator.hasNext())
      widgetNames.add(((CollectionBean)iterator.next()).name); 
    return widgetNames;
  }
  
  public final void initializeGson() {
    this.widgetGson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
}
