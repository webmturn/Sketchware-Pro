package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.beans.SelectableBean;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class SoundCollectionManager extends BaseCollectionManager {
  public static SoundCollectionManager instance;
  
  public static SoundCollectionManager getInstance() {
    if (instance == null) {
      synchronized (SoundCollectionManager.class) {
        if (instance == null) {
          instance = new SoundCollectionManager();
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
    String dataName = resourceBean.resName;
    if (resourceBean.isNinePatch()) {
      dataName = dataName + ".9.png";
    } else {
      dataName = dataName + ".png";
    }
    String destPath = this.dataDirPath + java.io.File.separator + dataName;
    if (resourceBean.savedPos == 1) {
      String srcPath = resourceBean.resFullName;
      if (!this.fileUtil.exists(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.mkdirs(this.dataDirPath);
        BitmapUtil.processAndSaveBitmap(srcPath, destPath, resourceBean.rotate, resourceBean.flipHorizontal, resourceBean.flipVertical);
      } catch (Exception e) {
        throw new CompileException("fail_to_copy");
      }
    } else {
      String srcPath = SketchwarePaths.getImagesPath() + java.io.File.separator + sourcePath + java.io.File.separator + resourceBean.resFullName;
      if (!this.fileUtil.exists(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.mkdirs(this.dataDirPath);
        this.fileUtil.copyFile(srcPath, destPath);
      } catch (Exception e) {
        throw new CompileException("fail_to_copy");
      }
    }
    this.collections.add(new CollectionBean(resourceBean.resName, dataName));
    if (flag) saveCollections();
  }
  
  public void addResources(String input, ArrayList<ProjectResourceBean> list, boolean flag) throws CompileException {
    if (this.collections == null)
      initialize(); 
    ArrayList<String> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections) {
      for (ProjectResourceBean projectResourceBean : list) {
        if (collectionBean.name.equals(projectResourceBean.resName))
          arrayList.add(collectionBean.name); 
      } 
    } 
    if (arrayList.size() <= 0) {
      ArrayList<String> failedNames = new ArrayList<>();
      for (ProjectResourceBean projectResourceBean : list) {
        String str;
        if (((SelectableBean)projectResourceBean).savedPos == 0) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(SketchwarePaths.getImagesPath());
          stringBuilder.append(File.separator);
          stringBuilder.append(input);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          str = stringBuilder.toString();
        } else {
          str = projectResourceBean.resFullName;
        } 
        if (!this.fileUtil.exists(str))
          failedNames.add(projectResourceBean.resName); 
      } 
      if (failedNames.size() <= 0) {
        failedNames = new ArrayList<String>();
        ArrayList<String> processedPaths = new ArrayList<>();
        for (ProjectResourceBean projectResourceBean : list) {
          String sourcePath;
          String fileName = projectResourceBean.resName;
          if (projectResourceBean.isNinePatch()) {
            StringBuilder innerBuilder = new StringBuilder();
            innerBuilder.append(fileName);
            innerBuilder.append(".9.png");
            fileName = innerBuilder.toString();
          } else {
            StringBuilder innerBuilder = new StringBuilder();
            innerBuilder.append(fileName);
            innerBuilder.append(".png");
            fileName = innerBuilder.toString();
          } 
          if (((SelectableBean)projectResourceBean).savedPos == 0) {
            StringBuilder innerBuilder = new StringBuilder();
            innerBuilder.append(SketchwarePaths.getImagesPath());
            innerBuilder.append(File.separator);
            innerBuilder.append(input);
            innerBuilder.append(File.separator);
            innerBuilder.append(projectResourceBean.resFullName);
            sourcePath = innerBuilder.toString();
          } else {
            sourcePath = projectResourceBean.resFullName;
          } 
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(fileName);
          String destPath = stringBuilder.toString();
          try {
            this.fileUtil.mkdirs(this.dataDirPath);
            BitmapUtil.processAndSaveBitmap(sourcePath, destPath, projectResourceBean.rotate, projectResourceBean.flipHorizontal, projectResourceBean.flipVertical);
            ArrayList<CollectionBean> collectionsList = this.collections;
            CollectionBean collectionBean = new CollectionBean(projectResourceBean.resName, fileName);
            collectionsList.add(collectionBean);
            processedPaths.add(destPath);
          } catch (Exception iOException) {
            failedNames.add(projectResourceBean.resName);
          } 
        } 
        if (failedNames.size() > 0) {
          CompileException yy2 = new CompileException("fail_to_copy");
          yy2.setErrorDetails(failedNames);
          if (processedPaths.size() > 0)
            for (String str : processedPaths)
              this.fileUtil.deleteFileByPath(str);  
          throw yy2;
        } 
        if (flag)
          saveCollections(); 
        return;
      } 
      CompileException yy1 = new CompileException("file_no_exist");
      yy1.setErrorDetails(failedNames);
      throw yy1;
    } 
    CompileException CompileException = new CompileException("duplicate_name");
    CompileException.setErrorDetails(arrayList);
    throw CompileException;
  }
  
  public void removeResource(String input, boolean flag) {
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        CollectionBean collectionBean = this.collections.get(j);
        i = j;
        if (collectionBean.name.equals(input)) {
          String str = collectionBean.data;
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(str);
          str = stringBuilder.toString();
          this.fileUtil.deleteFileByPath(str);
          this.collections.remove(j);
          i = j;
        } 
        continue;
      } 
      if (flag)
        saveCollections(); 
      return;
    } 
  }
  
  public void initializePaths() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("image");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("image");
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
