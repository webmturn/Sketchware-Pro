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
  
  public ProjectFileManager(String str) {
    this.projectId = str;
    this.fileUtil = new EncryptedFileUtil();
    this.xmlNames = new ArrayList<String>();
    this.javaNames = new ArrayList<String>();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    initializeDefaults();
    refreshNameLists();
  }
  
  public ProjectFileBean getActivityByJavaName(String str) {
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.getJavaName().equals(str))
        return projectFileBean; 
    } 
    return null;
  }
  
  public void deleteBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    str = stringBuilder.toString();
    this.fileUtil.deleteFileByPath(str);
  }
  
  public void addFile(int index, String str) {
    ProjectFileBean projectFileBean = new ProjectFileBean(index, str);
    if (index == 0) {
      this.activities.add(projectFileBean);
    } else {
      this.customViews.add(projectFileBean);
    } 
  }
  
  public void syncWithLibrary(LibraryManager paramiC) {
    ProjectLibraryBean projectLibraryBean = paramiC.getCompat();
    if (projectLibraryBean != null && projectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.hasActivityOption(4)) {
        removeFile(2, projectFileBean.getDrawerName());
        projectFileBean.setActivityOptions(1);
      } 
      if (projectFileBean.hasActivityOption(8))
        projectFileBean.setActivityOptions(1); 
    } 
    refreshNameLists();
  }
  
  public void addProjectFile(ProjectFileBean fileBean) {
    if (fileBean.fileType == 0) {
      this.activities.add(fileBean);
    } else {
      this.customViews.add(fileBean);
    } 
  }
  
  public void parseFileData(BufferedReader reader) throws java.io.IOException {
    StringBuffer stringBuffer = new StringBuffer();
    String str = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.length() <= 0)
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer stringBuffer1 = stringBuffer;
          if (str.length() > 0) {
            parseFileSection(str, stringBuffer.toString());
            stringBuffer1 = new StringBuffer();
          } 
          str = line.substring(1);
          stringBuffer = stringBuffer1;
          continue;
        } 
        stringBuffer.append(line);
        stringBuffer.append("\n");
        continue;
      } 
      if (str.length() > 0)
        parseFileSection(str, stringBuffer.toString()); 
      refreshNameLists();
      return;
    } 
  }
  
  public void parseFileSection(String key, String value) {
    ProjectFileBean projectFileBean = null;
    if (key.equals("activity")) {
      key = value;
      if (value.length() <= 0)
        return; 
      while (true) {
        int i = key.indexOf("\n");
        if (i < 0 || key.charAt(0) != '{')
          break; 
        value = key.substring(0, i);
        ProjectFileBean projectFileBean1 = (ProjectFileBean)this.gson.fromJson(value, ProjectFileBean.class);
        projectFileBean1.setOptionsByTheme();
        if (projectFileBean1.fileName.equals("main")) {
          String str = null;
          Iterator<ProjectFileBean> iterator = this.activities.iterator();
          while (true) {
            value = str;
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
        if (i >= key.length() - 1)
          break; 
        key = key.substring(i + 1);
      } 
    } else if (key.equals("customview")) {
      if (value.length() <= 0)
        return; 
      this.customViews = new ArrayList<ProjectFileBean>();
      String str = value;
      while (true) {
        int i = str.indexOf("\n");
        if (i < 0 || str.charAt(0) != '{')
          break; 
        key = str.substring(0, i);
        ProjectFileBean projectFileBean1 = (ProjectFileBean)this.gson.fromJson(key, ProjectFileBean.class);
        projectFileBean1.setOptionsByTheme();
        this.customViews.add(projectFileBean1);
        if (i >= str.length() - 1)
          break; 
        str = str.substring(i + 1);
      } 
    } 
  }
  
  public final void serializeFiles(StringBuffer buffer) {
    buffer.append("@");
    buffer.append("activity");
    buffer.append("\n");
    ArrayList<ProjectFileBean> arrayList = this.activities;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        buffer.append(this.gson.toJson(projectFileBean, ProjectFileBean.class));
        buffer.append("\n");
      }  
    buffer.append("@");
    buffer.append("customview");
    buffer.append("\n");
    arrayList = this.customViews;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        buffer.append(this.gson.toJson(projectFileBean, ProjectFileBean.class));
        buffer.append("\n");
      }  
  }
  
  public void setActivities(ArrayList<ProjectFileBean> list) {
    this.activities = list;
  }
  
  public ProjectFileBean getFileByXmlName(String str) {
    for (ProjectFileBean projectFileBean : this.activities) {
      if (projectFileBean.getXmlName().equals(str))
        return projectFileBean; 
    } 
    ArrayList<ProjectFileBean> arrayList = this.customViews;
    if (arrayList != null)
      for (ProjectFileBean projectFileBean : arrayList) {
        if (projectFileBean.getXmlName().equals(str))
          return projectFileBean; 
      }  
    return null;
  }
  
  public ArrayList<ProjectFileBean> getActivities() {
    if (this.activities == null)
      this.activities = new ArrayList<ProjectFileBean>(); 
    return this.activities;
  }
  
  public void removeFile(int index, String str) {
    if (index == 0) {
      for (ProjectFileBean projectFileBean : this.activities) {
        if (projectFileBean.fileType == index && projectFileBean.fileName.equals(str)) {
          this.activities.remove(projectFileBean);
          break;
        } 
      } 
    } else {
      for (ProjectFileBean projectFileBean : this.customViews) {
        if (projectFileBean.fileType == index && projectFileBean.fileName.equals(str)) {
          this.customViews.remove(projectFileBean);
          break;
        } 
      } 
    } 
  }
  
  public void setCustomViews(ArrayList<ProjectFileBean> list) {
    this.customViews = list;
  }
  
  public ArrayList<ProjectFileBean> getCustomViews() {
    if (this.customViews == null)
      this.customViews = new ArrayList<ProjectFileBean>(); 
    return this.customViews;
  }
  
  public boolean hasJavaName(String str) {
    Iterator<String> iterator = this.javaNames.iterator();
    while (iterator.hasNext()) {
      if (str.equals(iterator.next()))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> getJavaNames() {
    return this.javaNames;
  }
  
  public boolean hasXmlName(String str) {
    Iterator<String> iterator = this.xmlNames.iterator();
    while (iterator.hasNext()) {
      if (str.equals(iterator.next()))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> getXmlNames() {
    return this.xmlNames;
  }
  
  public final void writeToFile(String str) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeFiles(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(str, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public final void initializeDefaults() {
    this.activities = new ArrayList<ProjectFileBean>();
    this.customViews = new ArrayList<ProjectFileBean>();
    addFile(0, "main");
  }
  
  public boolean hasBackup() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(backupDir);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    String filePath = stringBuilder.toString();
    return this.fileUtil.exists(filePath);
  }
  
  public void loadFromBackup() {
    initializeDefaults();
    String backupPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(backupPath);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    String filePath = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(filePath);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseFileData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
    refreshNameLists();
  }
  
  public void loadFromData() {
    initializeDefaults();
    String dataPath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(dataPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("file");
    dataPath = pathBuilder.toString();
    if (!this.fileUtil.exists(dataPath))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(dataPath);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseFileData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void refreshNameLists() {
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
  
  public void resetAll() {
    this.projectId = "";
    this.xmlNames = new ArrayList<String>();
    this.javaNames = new ArrayList<String>();
    initializeDefaults();
  }
  
  public void saveToBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    writeToFile(stringBuilder.toString());
  }
  
  public void saveToData() {
    String str = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("file");
    writeToFile(stringBuilder.toString());
    deleteBackup();
  }
}
