package pro.sketchware.core;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

public class ProjectFileManager {
  public ArrayList<String> xmlNames;
  
  public ArrayList<String> javaNames;
  
  public ArrayList<ProjectFileBean> activities;
  
  public ArrayList<ProjectFileBean> customViews;
  
  public String projectId;
  
  public EncryptedFileUtil fileUtil;
  
  public Gson gson;
  
  public ProjectFileManager(String paramString) {
    this.projectId = paramString;
    this.fileUtil = new EncryptedFileUtil();
    this.xmlNames = new ArrayList<String>();
    this.javaNames = new ArrayList<String>();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    f();
    j();
  }
  
  public ProjectFileBean a(String paramString) {
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.getJavaName().equals(paramString))
        return projectFileBean; 
    } 
    return null;
  }
  
  public void a() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    str = stringBuilder.toString();
    this.fileUtil.deleteFileByPath(str);
  }
  
  public void a(int paramInt, String paramString) {
    ProjectFileBean projectFileBean = new ProjectFileBean(paramInt, paramString);
    if (paramInt == 0) {
      this.activities.add(projectFileBean);
    } else {
      this.customViews.add(projectFileBean);
    } 
  }
  
  public void a(LibraryManager paramiC) {
    ProjectLibraryBean projectLibraryBean = paramiC.c();
    if (projectLibraryBean != null && projectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.hasActivityOption(4)) {
        b(2, projectFileBean.getDrawerName());
        projectFileBean.setActivityOptions(1);
      } 
      if (projectFileBean.hasActivityOption(8))
        projectFileBean.setActivityOptions(1); 
    } 
    j();
  }
  
  public void a(ProjectFileBean paramProjectFileBean) {
    if (paramProjectFileBean.fileType == 0) {
      this.activities.add(paramProjectFileBean);
    } else {
      this.customViews.add(paramProjectFileBean);
    } 
  }
  
  public void a(BufferedReader paramBufferedReader) throws java.io.IOException {
    StringBuffer stringBuffer = new StringBuffer();
    String str = "";
    while (true) {
      String str1 = paramBufferedReader.readLine();
      if (str1 != null) {
        if (str1.length() <= 0)
          continue; 
        if (str1.charAt(0) == '@') {
          StringBuffer stringBuffer1 = stringBuffer;
          if (str.length() > 0) {
            a(str, stringBuffer.toString());
            stringBuffer1 = new StringBuffer();
          } 
          str = str1.substring(1);
          stringBuffer = stringBuffer1;
          continue;
        } 
        stringBuffer.append(str1);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0)
        a(str, stringBuffer.toString()); 
      j();
      return;
    } 
  }
  
  public void a(String paramString1, String paramString2) {
    ProjectFileBean projectFileBean = null;
    if (paramString1.equals("activity")) {
      paramString1 = paramString2;
      if (paramString2.length() <= 0)
        return; 
      while (true) {
        int i = paramString1.indexOf("\n");
        if (i < 0 || paramString1.charAt(0) != '{')
          break; 
        paramString2 = paramString1.substring(0, i);
        ProjectFileBean projectFileBean1 = (ProjectFileBean)this.gson.fromJson(paramString2, ProjectFileBean.class);
        projectFileBean1.setOptionsByTheme();
        if (projectFileBean1.fileName.equals("main")) {
          String str = null;
          Iterator<ProjectFileBean> iterator = this.activities.iterator();
          while (true) {
            paramString2 = str;
            if (iterator.hasNext()) {
              projectFileBean = iterator.next();
              if (projectFileBean.fileName.equals("main"))
                break; 
              continue;
            } 
            break;
          } 
          if (projectFileBean != null) {
            projectFileBean.copy(projectFileBean1);
          } else {
            this.activities.add(0, projectFileBean1);
          } 
        } else {
          this.activities.add(projectFileBean1);
        } 
        if (i >= paramString1.length() - 1)
          break; 
        paramString1 = paramString1.substring(i + 1);
      } 
    } else if (paramString1.equals("customview")) {
      if (paramString2.length() <= 0)
        return; 
      this.customViews = new ArrayList<ProjectFileBean>();
      String str = paramString2;
      while (true) {
        int i = str.indexOf("\n");
        if (i < 0 || str.charAt(0) != '{')
          break; 
        paramString1 = str.substring(0, i);
        ProjectFileBean projectFileBean1 = (ProjectFileBean)this.gson.fromJson(paramString1, ProjectFileBean.class);
        projectFileBean1.setOptionsByTheme();
        this.customViews.add(projectFileBean1);
        if (i >= str.length() - 1)
          break; 
        str = str.substring(i + 1);
      } 
    } 
  }
  
  public final void a(StringBuffer paramStringBuffer) {
    paramStringBuffer.append("@");
    paramStringBuffer.append("activity");
    paramStringBuffer.append("\n");
    ArrayList<ProjectFileBean> arrayList = this.activities;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        paramStringBuffer.append(this.gson.toJson(projectFileBean, ProjectFileBean.class));
        paramStringBuffer.append("\n");
      }  
    paramStringBuffer.append("@");
    paramStringBuffer.append("customview");
    paramStringBuffer.append("\n");
    arrayList = this.customViews;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        paramStringBuffer.append(this.gson.toJson(projectFileBean, ProjectFileBean.class));
        paramStringBuffer.append("\n");
      }  
  }
  
  public void a(ArrayList<ProjectFileBean> paramArrayList) {
    this.activities = paramArrayList;
  }
  
  public ProjectFileBean b(String paramString) {
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.getXmlName().equals(paramString))
        return projectFileBean; 
    } 
    ArrayList<ProjectFileBean> arrayList = this.customViews;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        if (projectFileBean.getXmlName().equals(paramString))
          return projectFileBean; 
      }  
    return null;
  }
  
  public ArrayList<ProjectFileBean> b() {
    if (this.activities == null)
      this.activities = new ArrayList<ProjectFileBean>(); 
    return this.activities;
  }
  
  public void b(int paramInt, String paramString) {
    if (paramInt == 0) {
      for (ProjectFileBean projectFileBean : this.activities) {
        if (projectFileBean.fileType == paramInt && projectFileBean.fileName.equals(paramString)) {
          this.activities.remove(projectFileBean);
          break;
        } 
      } 
    } else {
      for (ProjectFileBean projectFileBean : this.customViews) {
        if (projectFileBean.fileType == paramInt && projectFileBean.fileName.equals(paramString)) {
          this.customViews.remove(projectFileBean);
          break;
        } 
      } 
    } 
  }
  
  public void b(ArrayList<ProjectFileBean> paramArrayList) {
    this.customViews = paramArrayList;
  }
  
  public ArrayList<ProjectFileBean> c() {
    if (this.customViews == null)
      this.customViews = new ArrayList<ProjectFileBean>(); 
    return this.customViews;
  }
  
  public boolean c(String paramString) {
    Iterator<String> iterator = this.javaNames.iterator();
    while (iterator.hasNext()) {
      if (paramString.equals(iterator.next()))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> d() {
    return this.javaNames;
  }
  
  public boolean d(String paramString) {
    Iterator<String> iterator = this.xmlNames.iterator();
    while (iterator.hasNext()) {
      if (paramString.equals(iterator.next()))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> e() {
    return this.xmlNames;
  }
  
  public final void e(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    a(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public final void f() {
    this.activities = new ArrayList<ProjectFileBean>();
    this.customViews = new ArrayList<ProjectFileBean>();
    a(0, "main");
  }
  
  public boolean g() {
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    String str2 = stringBuilder.toString();
    return this.fileUtil.exists(str2);
  }
  
  public void h() {
    f();
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    String str2 = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
    j();
  }
  
  public void i() {
    f();
    String str1 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("file");
    str1 = stringBuilder1.toString();
    if (!this.fileUtil.exists(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(arrayOfByte);
      bufferedReader = new BufferedReader(new StringReader(str));
      a(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void j() {
    this.xmlNames.clear();
    this.javaNames.clear();
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.fileType == 0) {
        if (projectFileBean.fileName.equals("main")) {
          this.xmlNames.add(0, projectFileBean.getXmlName());
          this.javaNames.add(0, projectFileBean.getJavaName());
          continue;
        } 
        this.xmlNames.add(projectFileBean.getXmlName());
        this.javaNames.add(projectFileBean.getJavaName());
      } 
    } 
    ArrayList<ProjectFileBean> arrayList = this.customViews;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        int i = projectFileBean.fileType;
        if (i == 1 || i == 2)
          this.xmlNames.add(projectFileBean.getXmlName()); 
      }  
  }
  
  public void k() {
    this.projectId = "";
    this.xmlNames = new ArrayList<String>();
    this.javaNames = new ArrayList<String>();
    f();
  }
  
  public void l() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    e(stringBuilder.toString());
  }
  
  public void m() {
    String str = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    e(stringBuilder.toString());
    a();
  }
}
