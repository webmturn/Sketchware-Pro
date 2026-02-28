package pro.sketchware.core;

import android.util.Log;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;

public class ProjectFileManager {
  public ArrayList<String> xmlNames;
  
  public ArrayList<String> javaNames;
  
  public ArrayList<ProjectFileBean> activities;
  
  public ArrayList<ProjectFileBean> customViews;
  
  public String projectId;
  
  public EncryptedFileUtil fileUtil;
  
  public Gson gson;
  
  public ProjectFileManager(String projectId) {
    this.projectId = projectId;
    fileUtil = new EncryptedFileUtil();
    xmlNames = new ArrayList<>();
    javaNames = new ArrayList<>();
    gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    initializeDefaults();
    refreshNameLists();
  }
  
  public ProjectFileBean getActivityByJavaName(String javaName) {
    for (ProjectFileBean projectFileBean : activities) {
      if (projectFileBean.getJavaName().equals(javaName))
        return projectFileBean; 
    } 
    return null;
  }
  
  public void deleteBackup() {
    fileUtil.deleteFileByPath(SketchwarePaths.getBackupPath(projectId) + File.separator + "file");
  }
  
  public void addFile(int index, String fileName) {
    ProjectFileBean projectFileBean = new ProjectFileBean(index, fileName);
    if (index == 0) {
      activities.add(projectFileBean);
    } else {
      customViews.add(projectFileBean);
    } 
  }
  
  public void syncWithLibrary(LibraryManager libraryManager) {
    ProjectLibraryBean projectLibraryBean = libraryManager.getCompat();
    if (projectLibraryBean != null && projectLibraryBean.useYn.equals("Y"))
      return; 
    for (ProjectFileBean projectFileBean : activities) {
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
      activities.add(fileBean);
    } else {
      customViews.add(fileBean);
    } 
  }
  
  public void parseFileData(BufferedReader reader) throws java.io.IOException {
    StringBuffer contentBuffer = new StringBuffer();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.isEmpty())
          continue; 
        if (line.charAt(0) == '@') {
          StringBuffer tempBuffer = contentBuffer;
          if (!sectionName.isEmpty()) {
            parseFileSection(sectionName, contentBuffer.toString());
            tempBuffer = new StringBuffer();
          } 
          sectionName = line.substring(1);
          contentBuffer = tempBuffer;
          continue;
        } 
        contentBuffer.append(line);
        contentBuffer.append("\n");
        continue;
      } 
      if (!sectionName.isEmpty())
        parseFileSection(sectionName, contentBuffer.toString()); 
      refreshNameLists();
      return;
    } 
  }
  
  public void parseFileSection(String sectionName, String content) {
    if (sectionName.equals("activity")) {
      if (content.isEmpty())
        return; 
      String remaining = content;
      while (true) {
        int newlineIdx = remaining.indexOf("\n");
        if (newlineIdx < 0 || remaining.charAt(0) != '{')
          break; 
        String jsonLine = remaining.substring(0, newlineIdx);
        ProjectFileBean parsedBean = gson.fromJson(jsonLine, ProjectFileBean.class);
        parsedBean.setOptionsByTheme();
        if (parsedBean.fileName.equals("main")) {
          ProjectFileBean existingMain = null;
          for (ProjectFileBean fb : activities) {
            if (fb.fileName.equals("main")) {
              existingMain = fb;
              break;
            }
          }
          if (existingMain != null) {
            existingMain.copy(parsedBean);
          } else {
            activities.add(0, parsedBean);
          } 
        } else {
          activities.add(parsedBean);
        } 
        if (newlineIdx >= remaining.length() - 1)
          break; 
        remaining = remaining.substring(newlineIdx + 1);
      } 
    } else if (sectionName.equals("customview")) {
      if (content.isEmpty())
        return; 
      customViews = new ArrayList<>();
      String remaining = content;
      while (true) {
        int newlineIdx = remaining.indexOf("\n");
        if (newlineIdx < 0 || remaining.charAt(0) != '{')
          break; 
        String jsonLine = remaining.substring(0, newlineIdx);
        ProjectFileBean parsedBean = gson.fromJson(jsonLine, ProjectFileBean.class);
        parsedBean.setOptionsByTheme();
        customViews.add(parsedBean);
        if (newlineIdx >= remaining.length() - 1)
          break; 
        remaining = remaining.substring(newlineIdx + 1);
      } 
    } 
  }
  
  public final void serializeFiles(StringBuffer buffer) {
    buffer.append("@activity\n");
    if (activities != null)
      for (ProjectFileBean projectFileBean : activities)
        buffer.append(gson.toJson(projectFileBean, ProjectFileBean.class)).append("\n");
    buffer.append("@customview\n");
    if (customViews != null)
      for (ProjectFileBean projectFileBean : customViews)
        buffer.append(gson.toJson(projectFileBean, ProjectFileBean.class)).append("\n");
  }
  
  public void setActivities(ArrayList<ProjectFileBean> list) {
    activities = list;
  }
  
  public ProjectFileBean getFileByXmlName(String xmlName) {
    for (ProjectFileBean projectFileBean : activities) {
      if (projectFileBean.getXmlName().equals(xmlName))
        return projectFileBean; 
    } 
    if (customViews != null)
      for (ProjectFileBean projectFileBean : customViews) {
        if (projectFileBean.getXmlName().equals(xmlName))
          return projectFileBean; 
      }  
    return null;
  }
  
  public ArrayList<ProjectFileBean> getActivities() {
    if (activities == null)
      activities = new ArrayList<>(); 
    return activities;
  }
  
  public void removeFile(int index, String fileName) {
    if (index == 0) {
      for (ProjectFileBean projectFileBean : activities) {
        if (projectFileBean.fileType == index && projectFileBean.fileName.equals(fileName)) {
          activities.remove(projectFileBean);
          break;
        } 
      } 
    } else {
      for (ProjectFileBean projectFileBean : customViews) {
        if (projectFileBean.fileType == index && projectFileBean.fileName.equals(fileName)) {
          customViews.remove(projectFileBean);
          break;
        } 
      } 
    } 
  }
  
  public void setCustomViews(ArrayList<ProjectFileBean> list) {
    customViews = list;
  }

  public ArrayList<ProjectFileBean> getCustomViews() {
    if (customViews == null)
      customViews = new ArrayList<>(); 
    return customViews;
  }

  public boolean hasJavaName(String javaName) {
    return javaNames.contains(javaName);
  }

  public ArrayList<String> getJavaNames() {
    return javaNames;
  }

  public boolean hasXmlName(String xmlName) {
    return xmlNames.contains(xmlName);
  }

  public ArrayList<String> getXmlNames() {
    return xmlNames;
  }

  public final void writeToFile(String filePath) {
    StringBuffer contentBuffer = new StringBuffer();
    serializeFiles(contentBuffer);
    try {
      fileUtil.writeBytes(filePath, fileUtil.encryptString(contentBuffer.toString()));
    } catch (Exception e) {
      Log.e("ProjectFileManager", "Failed to write file", e);
    } 
  }

  public final void initializeDefaults() {
    activities = new ArrayList<>();
    customViews = new ArrayList<>();
    addFile(0, "main");
  }

  public boolean hasBackup() {
    return fileUtil.exists(SketchwarePaths.getBackupPath(projectId) + File.separator + "file");
  }

  public void loadFromBackup() {
    initializeDefaults();
    String filePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "file";
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(filePath))))) {
      parseFileData(reader);
    } catch (Exception e) {
      Log.e("ProjectFileManager", "Failed to load backup", e);
    }
    refreshNameLists();
  }

  public void loadFromData() {
    initializeDefaults();
    String filePath = SketchwarePaths.getDataPath(projectId) + File.separator + "file";
    if (!fileUtil.exists(filePath))
      return; 
    try (BufferedReader reader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(filePath))))) {
      parseFileData(reader);
    } catch (Exception e) {
      Log.e("ProjectFileManager", "Failed to load data", e);
    }
  }

  public void refreshNameLists() {
    xmlNames.clear();
    javaNames.clear();
    for (ProjectFileBean projectFileBean : activities) {
      if (projectFileBean.fileType == 0) {
        if (projectFileBean.fileName.equals("main")) {
          xmlNames.add(0, projectFileBean.getXmlName());
          javaNames.add(0, projectFileBean.getJavaName());
          continue;
        } 
        xmlNames.add(projectFileBean.getXmlName());
        javaNames.add(projectFileBean.getJavaName());
      } 
    } 
    if (customViews != null)
      for (ProjectFileBean projectFileBean : customViews) {
        int fileType = projectFileBean.fileType;
        if (fileType == 1 || fileType == 2)
          xmlNames.add(projectFileBean.getXmlName()); 
      }  
  }

  public void resetAll() {
    projectId = "";
    xmlNames = new ArrayList<>();
    javaNames = new ArrayList<>();
    initializeDefaults();
  }

  public void saveToBackup() {
    writeToFile(SketchwarePaths.getBackupPath(projectId) + File.separator + "file");
  }

  public void saveToData() {
    writeToFile(SketchwarePaths.getDataPath(projectId) + File.separator + "file");
    deleteBackup();
  }
}
