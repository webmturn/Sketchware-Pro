package pro.sketchware.core;

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
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    str = stringBuilder.toString();
    this.fileUtil.deleteFileByPath(str);
  }
  
  public void setAdmob(ProjectLibraryBean libraryBean) {
    this.admob = libraryBean;
  }
  
  public void parseLibraryData(BufferedReader reader) throws java.io.IOException {
    try {
      StringBuffer stringBuffer = new StringBuffer();
      String str = "";
      while (true) {
        String str1 = reader.readLine();
        if (str1 != null) {
          if (str1.length() <= 0)
            continue; 
          if (str1.charAt(0) == '@') {
            StringBuffer stringBuffer1 = stringBuffer;
            if (str.length() > 0) {
              parseLibrarySection(str, stringBuffer.toString());
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
          parseLibrarySection(str, stringBuffer.toString()); 
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
    StringBuffer stringBuffer = new StringBuffer();
    serializeLibraries(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
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
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
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
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    String str2 = stringBuilder.toString();
    return this.fileUtil.exists(str2);
  }
  
  public void loadFromBackup() {
    initializeDefaults();
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("library");
    str1 = stringBuilder1.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseLibraryData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void loadFromData() {
    initializeDefaults();
    String str1 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("library");
    str1 = stringBuilder1.toString();
    if (!this.fileUtil.exists(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseLibraryData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void resetAll() {
    this.projectId = "";
    initializeDefaults();
  }
  
  public void saveToBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    writeToFile(stringBuilder.toString());
  }
  
  public void saveToData() {
    String str = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("library");
    writeToFile(stringBuilder.toString());
    deleteBackup();
  }
}
