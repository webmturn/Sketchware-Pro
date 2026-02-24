package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class FontCollectionManager extends BaseCollectionManager {
  public static FontCollectionManager instance;
  
  public static FontCollectionManager getInstance() {
    if (instance == null) {
      synchronized (FontCollectionManager.class) {
        if (instance == null) {
          instance = new FontCollectionManager();
        }
      }
    }
    return instance;
  }
  
  public ProjectResourceBean getResourceByName(String name) {
    for (ProjectResourceBean projectResourceBean : getResources()) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void renameResource(ProjectResourceBean resourceBean, String newName, boolean flag) {
    if (this.collections == null)
      initialize(); 
    int i = this.collections.size();
    while (--i >= 0) {
      CollectionBean collectionBean = this.collections.get(i);
      if (collectionBean.name.equals(resourceBean.resName)) {
        collectionBean.name = newName;
        break;
      } 
    } 
    if (flag)
      saveCollections(); 
  }
  
  public void addResource(String sourcePath, ProjectResourceBean resourceBean) throws CompileException {
    addResource(sourcePath, resourceBean, true);
  }
  
  public void addResource(String sourcePath, ProjectResourceBean resourceBean, boolean flag) throws CompileException {
    if (this.collections == null) initialize();
    ArrayList<String> duplicates = new ArrayList<String>();
    for (CollectionBean bean : this.collections) {
      if (bean.name.equals(resourceBean.resName)) {
        duplicates.add(bean.name);
      }
    }
    if (duplicates.size() > 0) {
      throw new CompileException("duplicate_name");
    }
    String resName = resourceBean.resName;
    String dataName = resName;
    if (resourceBean.resFullName.contains(".")) {
      String ext = resourceBean.resFullName.substring(resourceBean.resFullName.lastIndexOf('.'));
      dataName = resName + ext;
    }
    String destPath = this.dataDirPath + java.io.File.separator + dataName;
    if (resourceBean.savedPos == 1) {
      String srcPath = resourceBean.resFullName;
      if (!this.fileUtil.exists(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.mkdirs(this.dataDirPath);
        this.fileUtil.copyFile(srcPath, destPath);
      } catch (java.io.IOException e) {
        throw new CompileException("fail_to_copy");
      }
    } else {
      String srcPath = SketchwarePaths.getSoundsPath() + java.io.File.separator + sourcePath + java.io.File.separator + resourceBean.resFullName;
      if (!this.fileUtil.exists(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.mkdirs(this.dataDirPath);
        this.fileUtil.copyFile(srcPath, destPath);
      } catch (java.io.IOException e) {
        throw new CompileException("fail_to_copy");
      }
    }
    this.collections.add(new CollectionBean(resourceBean.resName, dataName));
    if (flag) saveCollections();
  }
  
  public void removeResource(String input, boolean flag) {
    if (this.collections == null)
      initialize(); 
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        CollectionBean collectionBean = this.collections.get(j);
        i = j;
        if (collectionBean.name.equals(input)) {
          this.collections.remove(j);
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(collectionBean.data);
          String str = stringBuilder.toString();
          this.fileUtil.deleteFileByPath(str);
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
    stringBuilder.append("sound");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("sound");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.dataDirPath = stringBuilder.toString();
  }
  
  public boolean hasResource(String name) {
    Iterator<ProjectResourceBean> iterator = getResources().iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public void clearCollections() {
    super.clearCollections();
    instance = null;
  }
  
  public ArrayList<ProjectResourceBean> getResources() {
    if (this.collections == null)
      initialize(); 
    ArrayList<ProjectResourceBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, collectionBean.name, collectionBean.data)); 
    return arrayList;
  }
}
