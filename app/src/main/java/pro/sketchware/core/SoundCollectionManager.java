package pro.sketchware.core;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import com.besome.sketch.beans.SelectableBean;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class SoundCollectionManager extends BaseCollectionManager {
  public static SoundCollectionManager f;
  
  public static SoundCollectionManager g() {
    if (f == null) {
      synchronized (SoundCollectionManager.class) {
        if (f == null) {
          f = new SoundCollectionManager();
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
    String dataName = paramProjectResourceBean.resName;
    if (paramProjectResourceBean.isNinePatch()) {
      dataName = dataName + ".9.png";
    } else {
      dataName = dataName + ".png";
    }
    String destPath = this.dataDirPath + java.io.File.separator + dataName;
    if (paramProjectResourceBean.savedPos == 1) {
      String srcPath = paramProjectResourceBean.resFullName;
      if (!this.fileUtil.e(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.f(this.dataDirPath);
        BitmapUtil.processAndSaveBitmap(srcPath, destPath, paramProjectResourceBean.rotate, paramProjectResourceBean.flipHorizontal, paramProjectResourceBean.flipVertical);
      } catch (Exception e) {
        throw new CompileException("fail_to_copy");
      }
    } else {
      String srcPath = SketchwarePaths.g() + java.io.File.separator + paramString + java.io.File.separator + paramProjectResourceBean.resFullName;
      if (!this.fileUtil.e(srcPath)) {
        throw new CompileException("file_no_exist");
      }
      try {
        this.fileUtil.f(this.dataDirPath);
        this.fileUtil.a(srcPath, destPath);
      } catch (Exception e) {
        throw new CompileException("fail_to_copy");
      }
    }
    this.collections.add(new CollectionBean(paramProjectResourceBean.resName, dataName));
    if (paramBoolean) e();
  }
  
  public void a(String paramString, ArrayList<ProjectResourceBean> paramArrayList, boolean paramBoolean) throws CompileException {
    if (this.collections == null)
      a(); 
    ArrayList<String> arrayList = new ArrayList<>();
    for (CollectionBean collectionBean : this.collections) {
      for (ProjectResourceBean projectResourceBean : paramArrayList) {
        if (collectionBean.name.equals(projectResourceBean.resName))
          arrayList.add(collectionBean.name); 
      } 
    } 
    if (arrayList.size() <= 0) {
      ArrayList<String> arrayList1 = new ArrayList<>();
      for (ProjectResourceBean projectResourceBean : paramArrayList) {
        String str;
        if (((SelectableBean)projectResourceBean).savedPos == 0) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(SketchwarePaths.g());
          stringBuilder.append(File.separator);
          stringBuilder.append(paramString);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          str = stringBuilder.toString();
        } else {
          str = projectResourceBean.resFullName;
        } 
        if (!this.fileUtil.e(str))
          arrayList1.add(projectResourceBean.resName); 
      } 
      if (arrayList1.size() <= 0) {
        arrayList1 = new ArrayList<String>();
        ArrayList<String> arrayList2 = new ArrayList<>();
        for (ProjectResourceBean projectResourceBean : paramArrayList) {
          String str2;
          String str1 = projectResourceBean.resName;
          if (projectResourceBean.isNinePatch()) {
            StringBuilder stringBuilder1 = new StringBuilder();
            stringBuilder1.append(str1);
            stringBuilder1.append(".9.png");
            str1 = stringBuilder1.toString();
          } else {
            StringBuilder stringBuilder1 = new StringBuilder();
            stringBuilder1.append(str1);
            stringBuilder1.append(".png");
            str1 = stringBuilder1.toString();
          } 
          if (((SelectableBean)projectResourceBean).savedPos == 0) {
            StringBuilder stringBuilder1 = new StringBuilder();
            stringBuilder1.append(SketchwarePaths.g());
            stringBuilder1.append(File.separator);
            stringBuilder1.append(paramString);
            stringBuilder1.append(File.separator);
            stringBuilder1.append(projectResourceBean.resFullName);
            str2 = stringBuilder1.toString();
          } else {
            str2 = projectResourceBean.resFullName;
          } 
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(str1);
          String str3 = stringBuilder.toString();
          try {
            this.fileUtil.f(this.dataDirPath);
            BitmapUtil.processAndSaveBitmap(str2, str3, projectResourceBean.rotate, projectResourceBean.flipHorizontal, projectResourceBean.flipVertical);
            ArrayList<CollectionBean> arrayList3 = this.collections;
            CollectionBean collectionBean = new CollectionBean(projectResourceBean.resName, str1);
            arrayList3.add(collectionBean);
            arrayList2.add(str3);
          } catch (Exception iOException) {
            arrayList1.add(projectResourceBean.resName);
          } 
        } 
        if (arrayList1.size() > 0) {
          CompileException yy2 = new CompileException("fail_to_copy");
          yy2.a(arrayList1);
          if (arrayList2.size() > 0)
            for (String str : arrayList2)
              this.fileUtil.c(str);  
          throw yy2;
        } 
        if (paramBoolean)
          e(); 
        return;
      } 
      CompileException yy1 = new CompileException("file_no_exist");
      yy1.a(arrayList1);
      throw yy1;
    } 
    CompileException CompileException = new CompileException("duplicate_name");
    CompileException.a(arrayList);
    throw CompileException;
  }
  
  public void a(String paramString, boolean paramBoolean) {
    int i = this.collections.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        CollectionBean collectionBean = this.collections.get(j);
        i = j;
        if (collectionBean.name.equals(paramString)) {
          String str = collectionBean.data;
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.dataDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(str);
          str = stringBuilder.toString();
          this.fileUtil.c(str);
          this.collections.remove(j);
          i = j;
        } 
        continue;
      } 
      if (paramBoolean)
        e(); 
      return;
    } 
  }
  
  public void b() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("image");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.collectionFilePath = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(SketchwarePaths.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("image");
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
