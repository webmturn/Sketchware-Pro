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
  
  public LibraryManager(String paramString) {
    this.projectId = paramString;
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
  
  public void setAdmob(ProjectLibraryBean paramProjectLibraryBean) {
    this.admob = paramProjectLibraryBean;
  }
  
  public void parseLibraryData(BufferedReader paramBufferedReader) throws java.io.IOException {
    try {
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
  
  public final void writeToFile(String paramString) {
    StringBuffer stringBuffer = new StringBuffer();
    serializeLibraries(stringBuffer);
    try {
      byte[] arrayOfByte = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(paramString, arrayOfByte);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void parseLibrarySection(String paramString1, String paramString2) {
    if (paramString2.length() <= 0)
      return; 
    BufferedReader bufferedReader = null;
    try {
      bufferedReader = new BufferedReader(new StringReader(paramString2));
      ProjectLibraryBean bean = this.gson.fromJson(paramString2, ProjectLibraryBean.class);
      if (paramString1.equals("firebaseDB")) {
        this.firebaseDB = bean;
      } else if (paramString1.equals("compat")) {
        this.compat = bean;
      } else if (paramString1.equals("admob")) {
        this.admob = bean;
      } else if (paramString1.equals("googleMap")) {
        this.googleMap = bean;
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public final void serializeLibraries(StringBuffer paramStringBuffer) {
    if (this.firebaseDB != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("firebaseDB");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.gson.toJson(this.firebaseDB, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.compat != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("compat");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.gson.toJson(this.compat, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.admob != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("admob");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.gson.toJson(this.admob, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
    if (this.googleMap != null) {
      paramStringBuffer.append("@");
      paramStringBuffer.append("googleMap");
      paramStringBuffer.append("\n");
      paramStringBuffer.append(this.gson.toJson(this.googleMap, ProjectLibraryBean.class));
      paramStringBuffer.append("\n");
    } 
  }
  
  public ProjectLibraryBean getAdmob() {
    return this.admob;
  }
  
  public void setCompat(ProjectLibraryBean paramProjectLibraryBean) {
    this.compat = paramProjectLibraryBean;
  }
  
  public ProjectLibraryBean getCompat() {
    return this.compat;
  }
  
  public void setFirebaseDB(ProjectLibraryBean paramProjectLibraryBean) {
    this.firebaseDB = paramProjectLibraryBean;
  }
  
  public ProjectLibraryBean getFirebaseDB() {
    return this.firebaseDB;
  }
  
  public void setGoogleMap(ProjectLibraryBean paramProjectLibraryBean) {
    this.googleMap = paramProjectLibraryBean;
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
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(arrayOfByte);
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
      byte[] arrayOfByte = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(arrayOfByte);
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
