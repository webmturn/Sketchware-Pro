package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class ImageCollectionManager extends BaseCollectionManager {
  public static ImageCollectionManager f;
  
  public static ImageCollectionManager g() {
    if (f == null) {
      synchronized (ImageCollectionManager.class) {
        if (f == null) {
          f = new ImageCollectionManager();
        }
      }
    }
    return f;
  }
  
  public ProjectResourceBean a(String paramString) {
    for (ProjectResourceBean projectResourceBean : f()) {
      if (projectResourceBean.resName.equals(paramString))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void a(ProjectResourceBean paramProjectResourceBean, String paramString, boolean paramBoolean) {
    int i = this.collections.size();
    while (--i >= 0) {
      CollectionBean collectionBean = this.collections.get(i);
      if (collectionBean.name.equals(paramProjectResourceBean.resName)) {
        collectionBean.name = paramString;
        break;
      } 
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, ProjectResourceBean paramProjectResourceBean) throws CompileException {
    a(paramString, paramProjectResourceBean, true);
  }
  
  public void a(String paramString, ProjectResourceBean paramProjectResourceBean, boolean paramBoolean) throws CompileException {
    if (this.collections == null) a();
    ArrayList<String> duplicates = new ArrayList<String>();
    for (CollectionBean bean : this.collections) {
      if (bean.name.equals(paramProjectResourceBean.resName)) {
        duplicates.add(bean.name);
      }
    }
    if (duplicates.size() > 0) {
      throw new CompileException("duplicate_name");
    }
    String resName = paramProjectResourceBean.resName;
    String dataName = resName;
    if (paramProjectResourceBean.resFullName.contains(".")) {
      String ext = paramProjectResourceBean.resFullName.substring(paramProjectResourceBean.resFullName.lastIndexOf('.'));
      dataName = resName + ext;
    }
    String destPath = this.dataDirPath + java.io.File.separator + dataName;
    if (paramProjectResourceBean.savedPos == 1) {
      String srcPath = paramProjectResourceBean.resFullName;
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
      String srcPath = SketchwarePaths.getFontsResourcePath() + java.io.File.separator + paramString + java.io.File.separator + paramProjectResourceBean.resFullName;
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
    this.collections.add(new CollectionBean(paramProjectResourceBean.resName, dataName));
    if (paramBoolean) e();
  }
  
  public void a(String paramString, boolean paramBoolean) {
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        CollectionBean collectionBean = this.collections.get(j);
        i = j;
        if (collectionBean.name.equals(paramString)) {
          this.collections.remove(j);
          String str = collectionBean.data;
          EncryptedFileUtil EncryptedFileUtil = this.fileUtil;
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(str);
          EncryptedFileUtil.deleteFileByPath(stringBuilder.toString());
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
    stringBuilder.append("font");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.getCollectionPath());
    stringBuilder.append(File.separator);
    stringBuilder.append("font");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.dataDirPath = stringBuilder.toString();
  }
  
  public boolean b(String paramString) {
    Iterator<ProjectResourceBean> iterator = f().iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(paramString))
        return true; 
    } 
    return false;
  }
  
  public void d() {
    super.d();
    f = null;
  }
  
  public ArrayList<ProjectResourceBean> f() {
    if (this.collections == null)
      a(); 
    ArrayList<ProjectResourceBean> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections)
      arrayList.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, collectionBean.name, collectionBean.data)); 
    return arrayList;
  }
}
