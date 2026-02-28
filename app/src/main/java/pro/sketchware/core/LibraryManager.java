package pro.sketchware.core;

import android.util.Log;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

public class LibraryManager {
  public String projectId;
  
  public EncryptedFileUtil fileUtil;
  
  public ProjectLibraryBean firebaseDB;
  
  public ProjectLibraryBean compat;
  
  public ProjectLibraryBean admob;
  
  public ProjectLibraryBean googleMap;
  
  public Gson gson;
  
  public LibraryManager(String value) {
    projectId = value;
    fileUtil = new EncryptedFileUtil();
    gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    initializeDefaults();
  }
  
  public void deleteBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "library";
    fileUtil.deleteFileByPath(backupPath);
  }
  
  public void setAdmob(ProjectLibraryBean libraryBean) {
    admob = libraryBean;
  }
  
  public void parseLibraryData(BufferedReader reader) throws IOException {
    try {
      StringBuilder contentBuffer = new StringBuilder();
      String sectionName = "";
      while (true) {
        String line = reader.readLine();
        if (line != null) {
          if (line.length() <= 0)
            continue; 
          if (line.charAt(0) == '@') {
            StringBuilder tempBuffer = contentBuffer;
            if (sectionName.length() > 0) {
              parseLibrarySection(sectionName, contentBuffer.toString());
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
        if (sectionName.length() > 0)
          parseLibrarySection(sectionName, contentBuffer.toString()); 
        if (firebaseDB == null)
          firebaseDB = new ProjectLibraryBean(0); 
        if (compat == null)
          compat = new ProjectLibraryBean(1); 
        if (admob == null)
          admob = new ProjectLibraryBean(2); 
        if (googleMap == null)
          googleMap = new ProjectLibraryBean(3); 
        return;
      } 
    } catch (Exception exception) {
      throw exception;
    } 
  }
  
  public final void writeToFile(String value) {
    StringBuilder contentBuffer = new StringBuilder();
    serializeLibraries(contentBuffer);
    try {
      byte[] bytes = fileUtil.encryptString(contentBuffer.toString());
      fileUtil.writeBytes(value, bytes);
    } catch (Exception e) {
      Log.w("LibraryManager", "Failed to write file", e);
    } 
  }
  
  public void parseLibrarySection(String key, String value) {
    if (value.length() <= 0)
      return; 
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(value))) {
      ProjectLibraryBean bean = gson.fromJson(value, ProjectLibraryBean.class);
      if (key.equals("firebaseDB")) {
        bean.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE;
        firebaseDB = bean;
      } else if (key.equals("compat")) {
        bean.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_COMPAT;
        compat = bean;
      } else if (key.equals("admob")) {
        bean.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_ADMOB;
        admob = bean;
      } else if (key.equals("googleMap")) {
        bean.libType = ProjectLibraryBean.PROJECT_LIB_TYPE_GOOGLE_MAP;
        googleMap = bean;
      }
    } catch (Exception e) {
      Log.w("LibraryManager", "Failed to parse library section: " + key, e);
    }
  }
  
  public final void serializeLibraries(StringBuilder buffer) {
    if (firebaseDB != null) {
      buffer.append("@firebaseDB\n");
      buffer.append(gson.toJson(firebaseDB, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (compat != null) {
      buffer.append("@compat\n");
      buffer.append(gson.toJson(compat, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (admob != null) {
      buffer.append("@admob\n");
      buffer.append(gson.toJson(admob, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (googleMap != null) {
      buffer.append("@googleMap\n");
      buffer.append(gson.toJson(googleMap, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
  }
  
  public ProjectLibraryBean getAdmob() {
    return admob;
  }
  
  public void setCompat(ProjectLibraryBean libraryBean) {
    compat = libraryBean;
  }
  
  public ProjectLibraryBean getCompat() {
    return compat;
  }
  
  public void setFirebaseDB(ProjectLibraryBean libraryBean) {
    firebaseDB = libraryBean;
  }
  
  public ProjectLibraryBean getFirebaseDB() {
    return firebaseDB;
  }
  
  public void setGoogleMap(ProjectLibraryBean libraryBean) {
    googleMap = libraryBean;
  }
  
  public ProjectLibraryBean getGoogleMap() {
    return googleMap;
  }
  
  public final void initializeDefaults() {
    firebaseDB = new ProjectLibraryBean(0);
    compat = new ProjectLibraryBean(1);
    admob = new ProjectLibraryBean(2);
    googleMap = new ProjectLibraryBean(3);
  }
  
  public boolean hasBackup() {
    String libraryPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "library";
    return fileUtil.exists(libraryPath);
  }
  
  public void loadFromBackup() {
    initializeDefaults();
    String basePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "library";
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(basePath))))) {
      parseLibraryData(bufferedReader);
    } catch (Exception e) {
      Log.w("LibraryManager", "Failed to load from backup", e);
    }
  }
  
  public void loadFromData() {
    initializeDefaults();
    String dataPath = SketchwarePaths.getDataPath(projectId) + File.separator + "library";
    if (!fileUtil.exists(dataPath))
      return; 
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(dataPath))))) {
      parseLibraryData(bufferedReader);
    } catch (Exception e) {
      Log.w("LibraryManager", "Failed to load from data", e);
    }
  }
  
  public void resetAll() {
    projectId = "";
    initializeDefaults();
  }
  
  public void saveToBackup() {
    writeToFile(SketchwarePaths.getBackupPath(projectId) + File.separator + "library");
  }
  
  public void saveToData() {
    writeToFile(SketchwarePaths.getDataPath(projectId) + File.separator + "library");
    deleteBackup();
  }
}
