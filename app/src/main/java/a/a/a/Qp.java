package a.a.a;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class Qp extends Lp {
  public static Qp f;
  
  public static Qp g() {
    if (f == null) {
      synchronized (Qp.class) {
        if (f == null) {
          f = new Qp();
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
    if (this.e == null)
      a(); 
    int i = this.e.size();
    while (--i >= 0) {
      CollectionBean collectionBean = this.e.get(i);
      if (collectionBean.name.equals(paramProjectResourceBean.resName)) {
        collectionBean.name = paramString;
        break;
      } 
    } 
    if (paramBoolean)
      e(); 
  }
  
  public void a(String paramString, ProjectResourceBean paramProjectResourceBean) throws yy {
    a(paramString, paramProjectResourceBean, true);
  }
  
  public void a(String paramString, ProjectResourceBean paramProjectResourceBean, boolean paramBoolean) throws yy {
    if (this.e == null) a();
    ArrayList<String> duplicates = new ArrayList<String>();
    for (CollectionBean bean : this.e) {
      if (bean.name.equals(paramProjectResourceBean.resName)) {
        duplicates.add(bean.name);
      }
    }
    if (duplicates.size() > 0) {
      throw new yy("duplicate_name");
    }
    String resName = paramProjectResourceBean.resName;
    String dataName = resName;
    if (paramProjectResourceBean.resFullName.contains(".")) {
      String ext = paramProjectResourceBean.resFullName.substring(paramProjectResourceBean.resFullName.lastIndexOf('.'));
      dataName = resName + ext;
    }
    String destPath = this.b + java.io.File.separator + dataName;
    if (paramProjectResourceBean.savedPos == 1) {
      String srcPath = paramProjectResourceBean.resFullName;
      if (!this.c.e(srcPath)) {
        throw new yy("file_no_exist");
      }
      try {
        this.c.f(this.b);
        this.c.a(srcPath, destPath);
      } catch (java.io.IOException e) {
        throw new yy("fail_to_copy");
      }
    } else {
      String srcPath = wq.t() + java.io.File.separator + paramString + java.io.File.separator + paramProjectResourceBean.resFullName;
      if (!this.c.e(srcPath)) {
        throw new yy("file_no_exist");
      }
      try {
        this.c.f(this.b);
        this.c.a(srcPath, destPath);
      } catch (java.io.IOException e) {
        throw new yy("fail_to_copy");
      }
    }
    this.e.add(new CollectionBean(paramProjectResourceBean.resName, dataName));
    if (paramBoolean) e();
  }
  
  public void a(String paramString, boolean paramBoolean) {
    if (this.e == null)
      a(); 
    int i = this.e.size();
    while (true) {
      int j = i - 1;
      if (j >= 0) {
        CollectionBean collectionBean = this.e.get(j);
        i = j;
        if (collectionBean.name.equals(paramString)) {
          this.e.remove(j);
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.b);
          stringBuilder.append(File.separator);
          stringBuilder.append(collectionBean.data);
          String str = stringBuilder.toString();
          this.c.c(str);
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
    stringBuilder.append(wq.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("sound");
    stringBuilder.append(File.separator);
    stringBuilder.append("list");
    this.a = stringBuilder.toString();
    stringBuilder = new StringBuilder();
    stringBuilder.append(wq.a());
    stringBuilder.append(File.separator);
    stringBuilder.append("sound");
    stringBuilder.append(File.separator);
    stringBuilder.append("data");
    this.b = stringBuilder.toString();
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
    if (this.e == null)
      a(); 
    ArrayList<ProjectResourceBean> arrayList = new ArrayList();
    for (CollectionBean collectionBean : this.e)
      arrayList.add(new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, collectionBean.name, collectionBean.data)); 
    return arrayList;
  }
}
