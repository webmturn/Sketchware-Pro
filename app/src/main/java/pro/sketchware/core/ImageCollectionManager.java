package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.beans.SelectableBean;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ImageCollectionManager extends BaseCollectionManager {
  public static volatile ImageCollectionManager instance;
  
  public static ImageCollectionManager getInstance() {
    if (instance == null) {
      synchronized (ImageCollectionManager.class) {
        if (instance == null) {
          instance = new ImageCollectionManager();
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
    ArrayList<String> duplicateNames = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections) {
      for (ProjectResourceBean projectResourceBean : list) {
        if (collectionBean.name.equals(projectResourceBean.resName))
          duplicateNames.add(collectionBean.name); 
      } 
    } 
    if (duplicateNames.size() <= 0) {
      ArrayList<String> failedNames = new ArrayList<>();
      for (ProjectResourceBean projectResourceBean : list) {
        String resolvedPath;
        if (((SelectableBean)projectResourceBean).savedPos == 0) {
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(SketchwarePaths.getImagesPath());
          pathBuilder.append(File.separator);
          pathBuilder.append(input);
          pathBuilder.append(File.separator);
          pathBuilder.append(projectResourceBean.resFullName);
          resolvedPath = pathBuilder.toString();
        } else {
          resolvedPath = projectResourceBean.resFullName;
        } 
        if (!this.fileUtil.exists(resolvedPath))
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
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(this.dataDirPath);
          pathBuilder.append(File.separator);
          pathBuilder.append(fileName);
          String destPath = pathBuilder.toString();
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
            for (String path : processedPaths)
              this.fileUtil.deleteFileByPath(path);  
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
    CompileException.setErrorDetails(duplicateNames);
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
          String filePath = collectionBean.data;
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(this.dataDirPath);
          pathBuilder.append(File.separator);
          pathBuilder.append(filePath);
          filePath = pathBuilder.toString();
          this.fileUtil.deleteFileByPath(filePath);
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
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(SketchwarePaths.getCollectionPath());
    pathBuilder.append(File.separator);
    pathBuilder.append("image");
    pathBuilder.append(File.separator);
    pathBuilder.append("list");
    this.collectionFilePath = pathBuilder.toString();
    pathBuilder = new StringBuilder();
    pathBuilder.append(SketchwarePaths.getCollectionPath());
    pathBuilder.append(File.separator);
    pathBuilder.append("image");
    pathBuilder.append(File.separator);
    pathBuilder.append("data");
    this.dataDirPath = pathBuilder.toString();
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
    ArrayList<ProjectResourceBean> resources = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      resources.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, collectionBean.name, collectionBean.data)); 
    return resources;
  }
}
