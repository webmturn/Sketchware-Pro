package pro.sketchware.core;

import com.besome.sketch.beans.ProjectResourceBean;
import com.bumptech.glide.signature.StringSignature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

public class ResourceManager {
  public static StringSignature cacheSignature;
  
  public ArrayList<ProjectResourceBean> images;
  
  public ArrayList<ProjectResourceBean> sounds;
  
  public ArrayList<ProjectResourceBean> fonts;
  
  public String imageDirPath = "";
  
  public String soundDirPath = "";
  
  public String fontDirPath = "";
  
  public EncryptedFileUtil fileUtil;
  
  public String projectId;
  
  public Gson gson;
  
  public ResourceManager(String str) {
    this(str,
        SketchwarePaths.getImagesPath() + File.separator + str,
        SketchwarePaths.getSoundsPath() + File.separator + str,
        SketchwarePaths.getFontsResourcePath() + File.separator + str);
  }
  
  public ResourceManager(String key, String value, String extra, String tag) {
    this.imageDirPath = value;
    this.soundDirPath = extra;
    this.fontDirPath = tag;
    this.projectId = key;
    refreshCacheSignature();
    this.fileUtil = new EncryptedFileUtil(false);
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    this.gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
  }
  
  public static StringSignature getCacheSignature() {
    if (cacheSignature == null)
      refreshCacheSignature(); 
    return cacheSignature;
  }
  
  public static void refreshCacheSignature() {
    cacheSignature = new StringSignature(String.valueOf(System.currentTimeMillis()));
  }
  
  public void cleanupAllResources() {
    cleanupUnusedImages();
    cleanupUnusedSounds();
    cleanupUnusedFonts();
  }
  
  public void parseResourceData(BufferedReader reader) throws java.io.IOException {
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
            parseResourceSection(str, stringBuffer.toString());
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
        parseResourceSection(str, stringBuffer.toString()); 
      return;
    } 
  }
  
  public void copyFontsToDir(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.fonts;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(str);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.fontDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.copyFile(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void parseResourceSection(String key, String value) {
    if (value.trim().length() <= 0) return;
    java.io.BufferedReader reader = null;
    try {
      reader = new java.io.BufferedReader(new java.io.StringReader(value));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0 || line.trim().charAt(0) != '{') continue;
        ProjectResourceBean bean = this.gson.fromJson(line, ProjectResourceBean.class);
        if (key.equals("images")) {
          this.images.add(bean);
        } else if (key.equals("sounds")) {
          this.sounds.add(bean);
        } else if (key.equals("fonts")) {
          this.fonts.add(bean);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) { e.printStackTrace(); }
    }
  }
  
  public final void serializeResources(StringBuffer buffer) {
    buffer.append("@");
    buffer.append("images");
    buffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.images) {
      buffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
    buffer.append("@");
    buffer.append("sounds");
    buffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.sounds) {
      buffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
    buffer.append("@");
    buffer.append("fonts");
    buffer.append("\n");
    for (ProjectResourceBean projectResourceBean : this.fonts) {
      buffer.append(this.gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
  }
  
  public void setFonts(ArrayList<ProjectResourceBean> list) {
    this.fonts = list;
  }
  
  public void cleanupUnusedFonts() {
    File[] files = new File(this.fontDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.fonts) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public void copyImagesToDir(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(str);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.images) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.imageDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.copyFile(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void setImages(ArrayList<ProjectResourceBean> list) {
    this.images = list;
  }
  
  public void cleanupUnusedImages() {
    File[] files = new File(this.imageDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.images) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public void copySoundsToDir(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.sounds;
    if (arrayList != null && arrayList.size() > 0) {
      File file = new File(str);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.soundDirPath);
        stringBuilder1.append(File.separator);
        stringBuilder1.append(projectResourceBean.resFullName.toLowerCase());
        String str2 = stringBuilder1.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(str);
        stringBuilder2.append(File.separator);
        stringBuilder2.append(projectResourceBean.resFullName.toLowerCase());
        String str1 = stringBuilder2.toString();
        try {
          this.fileUtil.copyFile(str2, str1);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void setSounds(ArrayList<ProjectResourceBean> list) {
    this.sounds = list;
  }
  
  public String getFontPath(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.fonts;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        if (projectResourceBean.resName.equals(str)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.fontDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void cleanupUnusedSounds() {
    File[] files = new File(this.soundDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : this.sounds) {
          if (bean.resFullName.equals(file.getName())) {
            found = true;
            break;
          }
        }
        if (!found) file.delete();
      }
    }
  }
  
  public ProjectResourceBean getFontBean(String str) {
    for (ProjectResourceBean projectResourceBean : this.fonts) {
      if (projectResourceBean.resName.equals(str))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupFonts() {
    String str = SketchwarePaths.getTempFontsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.fontDirPath);
      File file2 = new File(str);
      oB1.copyDirectory(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String getImagePath(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(str)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.imageDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void backupImages() {
    String str = SketchwarePaths.getTempImagesPath();
    try {
      this.fileUtil.deleteDirectoryByPath(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.imageDirPath);
      File file2 = new File(str);
      oB1.copyDirectory(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ProjectResourceBean getImageBean(String str) {
    for (ProjectResourceBean projectResourceBean : this.images) {
      if (projectResourceBean.resName.equals(str))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupSounds() {
    String str = SketchwarePaths.getTempSoundsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(str);
      EncryptedFileUtil oB1 = this.fileUtil;
      File file1 = new File(this.soundDirPath);
      File file2 = new File(str);
      oB1.copyDirectory(file1, file2);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public int getImageResType(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.images;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(str))
          return projectResourceBean.resType; 
      }  
    return -1;
  }
  
  public void deleteTempDirs() {
    String str1 = SketchwarePaths.getTempImagesPath();
    String str2 = SketchwarePaths.getTempSoundsPath();
    String str3 = SketchwarePaths.getTempFontsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(str1);
      this.fileUtil.deleteDirectoryByPath(str2);
      this.fileUtil.deleteDirectoryByPath(str3);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String getSoundPath(String str) {
    ArrayList<ProjectResourceBean> arrayList = this.sounds;
    if (arrayList != null && arrayList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        if (projectResourceBean.resName.equals(str)) {
          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(this.soundDirPath);
          stringBuilder.append(File.separator);
          stringBuilder.append(projectResourceBean.resFullName);
          return stringBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void deleteBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    this.fileUtil.deleteFileByPath(str);
  }
  
  public ProjectResourceBean getSoundBean(String str) {
    for (ProjectResourceBean projectResourceBean : this.sounds) {
      if (projectResourceBean.resName.equals(str))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public String getFontDirPath() {
    return this.fontDirPath;
  }
  
  public ArrayList<String> getFontNames() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean hasFont(String str) {
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(str))
        return true; 
    } 
    return false;
  }
  
  public String getImageDirPath() {
    return this.imageDirPath;
  }
  
  public boolean hasImage(String str) {
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(str))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> getImageNames() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean hasSound(String str) {
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(str))
        return true; 
    } 
    return false;
  }
  
  public String getSoundDirPath() {
    return this.soundDirPath;
  }
  
  public ArrayList<String> getSoundNames() {
    ArrayList<String> arrayList = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext())
      arrayList.add(((ProjectResourceBean)iterator.next()).resName); 
    return arrayList;
  }
  
  public boolean hasBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    return this.fileUtil.exists(str);
  }
  
  public void loadFromBackup() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String str1 = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    String str2 = stringBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(str2);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseResourceData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void loadFromData() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String str1 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder1 = new StringBuilder();
    stringBuilder1.append(str1);
    stringBuilder1.append(File.separator);
    stringBuilder1.append("resource");
    str1 = stringBuilder1.toString();
    if (!this.fileUtil.exists(str1))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(str1);
      String str = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(str));
      parseResourceData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) {}
    }
  }
  
  public void resetAll() {
    this.projectId = "";
    this.imageDirPath = "";
    this.soundDirPath = "";
    this.fontDirPath = "";
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
  }
  
  public void restoreFontsFromTemp() {
    String str = SketchwarePaths.getTempFontsPath();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.fontDirPath);
      oB1.deleteDirectory(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.fontDirPath);
      oB1.copyDirectory(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void restoreImagesFromTemp() {
    String str = SketchwarePaths.getTempImagesPath();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.imageDirPath);
      oB1.deleteDirectory(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.imageDirPath);
      oB1.copyDirectory(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void restoreSoundsFromTemp() {
    String str = SketchwarePaths.getTempSoundsPath();
    try {
      EncryptedFileUtil oB1 = this.fileUtil;
      File file2 = new File(this.soundDirPath);
      oB1.deleteDirectory(file2);
      oB1 = this.fileUtil;
      file2 = new File(str);
      File file1 = new File(this.soundDirPath);
      oB1.copyDirectory(file2, file1);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void saveToData() {
    String str1 = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str1);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    String str2 = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    serializeResources(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(str2, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
    deleteBackup();
  }
  
  public void saveToBackup() {
    String str = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(str);
    stringBuilder.append(File.separator);
    stringBuilder.append("resource");
    str = stringBuilder.toString();
    StringBuffer stringBuffer = new StringBuffer();
    serializeResources(stringBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(stringBuffer.toString());
      this.fileUtil.writeBytes(str, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
