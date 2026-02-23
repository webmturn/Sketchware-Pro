package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.ArrayList;

public abstract class BaseCollectionManager {
  public String collectionFilePath;
  
  public String dataDirPath;
  
  public EncryptedFileUtil fileUtil;
  
  public Gson gson;
  
  public ArrayList<CollectionBean> collections;
  
  public BaseCollectionManager() {
    a();
  }
  
  public void a() {
    b();
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).create();
    c();
  }
  
  public abstract void b();
  
  public void c() {
    this.collections = new ArrayList<CollectionBean>();
    java.io.BufferedReader reader = null;
    try {
      String content = this.fileUtil.g(this.collectionFilePath);
      reader = new java.io.BufferedReader(new java.io.StringReader(content));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.length() <= 0) continue;
        CollectionBean bean = this.gson.fromJson(line, CollectionBean.class);
        String path = this.dataDirPath + java.io.File.separator + bean.data;
        if (this.fileUtil.e(path)) {
          this.collections.add(bean);
        }
      }
    } catch (java.io.IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception ignored) {}
    }
  }
  
  public void d() {
    ArrayList<CollectionBean> arrayList = this.collections;
    if (arrayList != null) {
      arrayList.clear();
      this.collections = null;
    } 
  }
  
  public void e() {
    if (this.collections == null)
      return; 
    StringBuilder stringBuilder = new StringBuilder(1024);
    for (CollectionBean collectionBean : this.collections) {
      stringBuilder.append(this.gson.toJson(collectionBean));
      stringBuilder.append("\n");
    } 
    try {
      this.fileUtil.b(this.collectionFilePath);
      this.fileUtil.b(this.collectionFilePath, stringBuilder.toString());
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
