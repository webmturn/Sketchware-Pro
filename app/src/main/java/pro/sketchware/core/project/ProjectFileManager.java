package pro.sketchware.core.project;

import android.util.Log;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import pro.sketchware.core.util.EncryptedFileUtil;

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
  
  public final void initializeDefaults() {
    activities = new ArrayList<>();
    customViews = new ArrayList<>();
    addFile(0, "main");
  }

  private String decryptFileToString(String filePath) throws IOException {
    byte[] fileBytes = fileUtil.readFileBytes(filePath);
    if (fileBytes == null && new File(filePath).length() > 0L)
      throw new IOException("Failed to read project file metadata: " + filePath);
    return fileUtil.decryptToString(fileBytes);
  }

  private byte[] encryptStringForStorage(String content, String filePath) throws IOException {
    try {
      return fileUtil.encryptString(content);
    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to encrypt project file metadata: " + filePath, e);
    }
  }

  private void loadFromPath(String filePath, String sourceName) {
    try (BufferedReader reader = new BufferedReader(new StringReader(decryptFileToString(filePath)))) {
      parseFileData(reader);
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectFileManager", "Failed to load " + sourceName + " for project " + projectId + " from " + filePath, e);
    }
  }

  public void addFile(int fileType, String fileName) {
    ProjectFileBean projectFileBean = new ProjectFileBean(fileType, fileName);
    if (fileType == 0) {
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
      if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
        removeFile(ProjectFileBean.PROJECT_FILE_TYPE_DRAWER, projectFileBean.getDrawerName());
        projectFileBean.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
      } 
      if (projectFileBean.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB))
        projectFileBean.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR); 
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
  
  public void parseFileData(BufferedReader reader) throws IOException {
    ArrayList<ProjectFileBean> parsedActivities = new ArrayList<>();
    ArrayList<ProjectFileBean> parsedCustomViews = new ArrayList<>();
    parsedActivities.add(new ProjectFileBean(0, "main"));
    StringBuilder contentBuffer = new StringBuilder();
    String sectionName = "";
    while (true) {
      String line = reader.readLine();
      if (line != null) {
        if (line.isEmpty())
          continue; 
        if (line.charAt(0) == '@') {
          StringBuilder tempBuffer = contentBuffer;
          if (!sectionName.isEmpty()) {
            parseFileSection(sectionName, contentBuffer.toString(), parsedActivities, parsedCustomViews);
            tempBuffer = new StringBuilder();
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
        parseFileSection(sectionName, contentBuffer.toString(), parsedActivities, parsedCustomViews);
      activities = parsedActivities;
      customViews = parsedCustomViews;
      refreshNameLists();
      return;
    } 
  }
  
  private void parseFileSection(String sectionName, String content,
      ArrayList<ProjectFileBean> targetActivities,
      ArrayList<ProjectFileBean> targetCustomViews) {
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
          for (ProjectFileBean activityFile : targetActivities) {
            if (activityFile.fileName.equals("main")) {
              existingMain = activityFile;
              break;
            }
          }
          if (existingMain != null) {
            existingMain.copy(parsedBean);
          } else {
            targetActivities.add(0, parsedBean);
          } 
        } else {
          targetActivities.add(parsedBean);
        } 
        if (newlineIdx >= remaining.length() - 1)
          break; 
        remaining = remaining.substring(newlineIdx + 1);
      } 
    } else if (sectionName.equals("customview")) {
      if (content.isEmpty())
        return; 
      targetCustomViews.clear();
      String remaining = content;
      while (true) {
        int newlineIdx = remaining.indexOf("\n");
        if (newlineIdx < 0 || remaining.charAt(0) != '{')
          break; 
        String jsonLine = remaining.substring(0, newlineIdx);
        ProjectFileBean parsedBean = gson.fromJson(jsonLine, ProjectFileBean.class);
        parsedBean.setOptionsByTheme();
        targetCustomViews.add(parsedBean);
        if (newlineIdx >= remaining.length() - 1)
          break; 
        remaining = remaining.substring(newlineIdx + 1);
      } 
    } 
  }

  public void parseFileSection(String sectionName, String content) {
    parseFileSection(sectionName, content, activities, customViews);
  }
  
  public final void serializeFiles(StringBuilder buffer) {
    buffer.append("@activity\n");
    if (activities != null)
      for (ProjectFileBean projectFileBean : activities)
        buffer.append(gson.toJson(projectFileBean, ProjectFileBean.class)).append("\n");
    buffer.append("@customview\n");
    if (customViews != null)
      for (ProjectFileBean projectFileBean : customViews)
        buffer.append(gson.toJson(projectFileBean, ProjectFileBean.class)).append("\n");
  }
  
  public void setActivities(ArrayList<ProjectFileBean> activities) {
    this.activities = activities;
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
  
  public void removeFile(int fileType, String fileName) {
    if (fileType == 0) {
      for (ProjectFileBean projectFileBean : activities) {
        if (projectFileBean.fileType == fileType && projectFileBean.fileName.equals(fileName)) {
          activities.remove(projectFileBean);
          break;
        } 
      } 
    } else {
      for (ProjectFileBean projectFileBean : customViews) {
        if (projectFileBean.fileType == fileType && projectFileBean.fileName.equals(fileName)) {
          customViews.remove(projectFileBean);
          break;
        } 
      } 
    } 
  }
  
  public void setCustomViews(ArrayList<ProjectFileBean> customViews) {
    this.customViews = customViews;
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

  public final boolean writeToFile(String filePath) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeFiles(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(filePath, encryptStringForStorage(contentBuffer.toString(), filePath));
      if (!saved)
        Log.e("ProjectFileManager", "Failed to write file data for project " + projectId + " to " + filePath);
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.e("ProjectFileManager", "Failed to write file data for project " + projectId + " to " + filePath, e);
    }
    return false;
  }

  public boolean hasBackup() {
    return hasDistinctBackup(
        SketchwarePaths.getBackupPath(projectId) + File.separator + "file",
        SketchwarePaths.getDataPath(projectId) + File.separator + "file");
  }

  private boolean hasDistinctBackup(String backupPath, String dataPath) {
    if (!fileUtil.exists(backupPath))
      return false;
    byte[] backupBytes = fileUtil.readFileBytes(backupPath);
    if (backupBytes == null || backupBytes.length == 0)
      return false;
    byte[] dataBytes = fileUtil.readFileBytes(dataPath);
    return dataBytes == null || !Arrays.equals(backupBytes, dataBytes);
  }

  private boolean hasUsableBackup(String backupPath) {
    if (!fileUtil.exists(backupPath))
      return false;
    byte[] backupBytes = fileUtil.readFileBytes(backupPath);
    return backupBytes != null && backupBytes.length > 0;
  }

  public void loadFromBackup() {
    String filePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "file";
    if (!hasUsableBackup(filePath))
      return; 
    loadFromPath(filePath, "file backup");
  }

  public void loadFromData() {
    String filePath = SketchwarePaths.getDataPath(projectId) + File.separator + "file";
    if (!fileUtil.exists(filePath))
      return; 
    loadFromPath(filePath, "file data");
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

  public boolean saveToBackup() {
    return writeToFile(SketchwarePaths.getBackupPath(projectId) + File.separator + "file");
  }

  public boolean saveToData() {
    boolean saved = writeToFile(SketchwarePaths.getDataPath(projectId) + File.separator + "file");
    if (saved)
      deleteBackup();
    return saved;
  }
}
