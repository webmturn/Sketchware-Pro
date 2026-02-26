package pro.sketchware.core;

import android.util.Log;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
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
    this.projectId = value;
    this.fileUtil = new EncryptedFileUtil();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
    initializeDefaults();
  }
  
  public void deleteBackup() {
    String backupPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    backupPath = pathBuilder.toString();
    this.fileUtil.deleteFileByPath(backupPath);
  }
  
  public void setAdmob(ProjectLibraryBean libraryBean) {
    this.admob = libraryBean;
  }
  
  public void parseLibraryData(BufferedReader reader) throws java.io.IOException {
    try {
      StringBuffer contentBuffer = new StringBuffer();
      String sectionName = "";
      while (true) {
        String line = reader.readLine();
        if (line != null) {
          if (line.length() <= 0)
            continue; 
          if (line.charAt(0) == '@') {
            StringBuffer tempBuffer = contentBuffer;
            if (sectionName.length() > 0) {
              parseLibrarySection(sectionName, contentBuffer.toString());
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
        if (sectionName.length() > 0)
          parseLibrarySection(sectionName, contentBuffer.toString()); 
        if (this.firebaseDB == null)
          this.firebaseDB = new ProjectLibraryBean(0); 
        if (this.compat == null)
          this.compat = new ProjectLibraryBean(1); 
        if (this.admob == null)
          this.admob = new ProjectLibraryBean(2); 
        if (this.googleMap == null)
          this.googleMap = new ProjectLibraryBean(3); 
        return;
      } 
    } catch (Exception exception) {
      throw exception;
    } 
  }
  
  public final void writeToFile(String value) {
    StringBuffer contentBuffer = new StringBuffer();
    serializeLibraries(contentBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(contentBuffer.toString());
      this.fileUtil.writeBytes(value, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void parseLibrarySection(String key, String value) {
    if (value.length() <= 0)
      return; 
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new StringReader(value));
      ProjectLibraryBean bean = this.gson.fromJson(value, ProjectLibraryBean.class);
      if (key.equals("firebaseDB")) {
        this.firebaseDB = bean;
      } else if (key.equals("compat")) {
        this.compat = bean;
      } else if (key.equals("admob")) {
        this.admob = bean;
      } else if (key.equals("googleMap")) {
        this.googleMap = bean;
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { Log.w("LibraryManager", "Failed to close reader", e); }
    }
  }
  
  public final void serializeLibraries(StringBuffer buffer) {
    if (this.firebaseDB != null) {
      buffer.append("@");
      buffer.append("firebaseDB");
      buffer.append("\n");
      buffer.append(this.gson.toJson(this.firebaseDB, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (this.compat != null) {
      buffer.append("@");
      buffer.append("compat");
      buffer.append("\n");
      buffer.append(this.gson.toJson(this.compat, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (this.admob != null) {
      buffer.append("@");
      buffer.append("admob");
      buffer.append("\n");
      buffer.append(this.gson.toJson(this.admob, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
    if (this.googleMap != null) {
      buffer.append("@");
      buffer.append("googleMap");
      buffer.append("\n");
      buffer.append(this.gson.toJson(this.googleMap, ProjectLibraryBean.class));
      buffer.append("\n");
    } 
  }
  
  public ProjectLibraryBean getAdmob() {
    return this.admob;
  }
  
  public void setCompat(ProjectLibraryBean libraryBean) {
    this.compat = libraryBean;
  }
  
  public ProjectLibraryBean getCompat() {
    return this.compat;
  }
  
  public void setFirebaseDB(ProjectLibraryBean libraryBean) {
    this.firebaseDB = libraryBean;
  }
  
  public ProjectLibraryBean getFirebaseDB() {
    return this.firebaseDB;
  }
  
  public void setGoogleMap(ProjectLibraryBean libraryBean) {
    this.googleMap = libraryBean;
  }
  
  public ProjectLibraryBean getGoogleMap() {
    return this.googleMap;
  }
  
  public final void initializeDefaults() {
    this.firebaseDB = new ProjectLibraryBean(0);
    this.compat = new ProjectLibraryBean(1);
    this.admob = new ProjectLibraryBean(2);
    this.googleMap = new ProjectLibraryBean(3);
  }
  
  public boolean hasBackup() {
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupDir);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    String libraryPath = pathBuilder.toString();
    return this.fileUtil.exists(libraryPath);
  }
  
  public void loadFromBackup() {
    initializeDefaults();
    String basePath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(basePath);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    basePath = pathBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(basePath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      parseLibraryData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { Log.w("LibraryManager", "Failed to close reader", e); }
    }
  }
  
  public void loadFromData() {
    initializeDefaults();
    String dataPath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(dataPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    dataPath = pathBuilder.toString();
    if (!this.fileUtil.exists(dataPath))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(dataPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      parseLibraryData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { Log.w("LibraryManager", "Failed to close reader", e); }
    }
  }
  
  public void resetAll() {
    this.projectId = "";
    initializeDefaults();
  }
  
  public void saveToBackup() {
    String basePath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(basePath);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    writeToFile(pathBuilder.toString());
  }
  
  public void saveToData() {
    String basePath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(basePath);
    pathBuilder.append(File.separator);
    pathBuilder.append("library");
    writeToFile(pathBuilder.toString());
    deleteBackup();
  }
}
