package pro.sketchware.core;

import android.util.Log;

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
  
  public ResourceManager(String projectId) {
    this(projectId,
        SketchwarePaths.getImagesPath() + File.separator + projectId,
        SketchwarePaths.getSoundsPath() + File.separator + projectId,
        SketchwarePaths.getFontsResourcePath() + File.separator + projectId);
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
            parseResourceSection(sectionName, contentBuffer.toString());
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
        parseResourceSection(sectionName, contentBuffer.toString()); 
      return;
    } 
  }
  
  public void copyFontsToDir(String destDir) {
    ArrayList<ProjectResourceBean> fontsList = this.fonts;
    if (fontsList != null && fontsList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        StringBuilder sourceBuilder = new StringBuilder();
        sourceBuilder.append(this.fontDirPath);
        sourceBuilder.append(File.separator);
        sourceBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String sourcePath = sourceBuilder.toString();
        StringBuilder destBuilder = new StringBuilder();
        destBuilder.append(destDir);
        destBuilder.append(File.separator);
        destBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String destPath = destBuilder.toString();
        try {
          this.fileUtil.copyFile(sourcePath, destPath);
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
  
  public void copyImagesToDir(String destDir) {
    ArrayList<ProjectResourceBean> imagesList = this.images;
    if (imagesList != null && imagesList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.images) {
        StringBuilder sourceBuilder = new StringBuilder();
        sourceBuilder.append(this.imageDirPath);
        sourceBuilder.append(File.separator);
        sourceBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String sourcePath = sourceBuilder.toString();
        StringBuilder destBuilder = new StringBuilder();
        destBuilder.append(destDir);
        destBuilder.append(File.separator);
        destBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String destPath = destBuilder.toString();
        try {
          this.fileUtil.copyFile(sourcePath, destPath);
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
  
  public void copySoundsToDir(String destDir) {
    ArrayList<ProjectResourceBean> soundsList = this.sounds;
    if (soundsList != null && soundsList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        StringBuilder sourceBuilder = new StringBuilder();
        sourceBuilder.append(this.soundDirPath);
        sourceBuilder.append(File.separator);
        sourceBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String sourcePath = sourceBuilder.toString();
        StringBuilder destBuilder = new StringBuilder();
        destBuilder.append(destDir);
        destBuilder.append(File.separator);
        destBuilder.append(projectResourceBean.resFullName.toLowerCase());
        String destPath = destBuilder.toString();
        try {
          this.fileUtil.copyFile(sourcePath, destPath);
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  public void setSounds(ArrayList<ProjectResourceBean> list) {
    this.sounds = list;
  }
  
  public String getFontPath(String name) {
    ArrayList<ProjectResourceBean> fontsList = this.fonts;
    if (fontsList != null && fontsList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.fonts) {
        if (projectResourceBean.resName.equals(name)) {
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(this.fontDirPath);
          pathBuilder.append(File.separator);
          pathBuilder.append(projectResourceBean.resFullName);
          return pathBuilder.toString();
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
  
  public ProjectResourceBean getFontBean(String name) {
    for (ProjectResourceBean projectResourceBean : this.fonts) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupFonts() {
    String tempPath = SketchwarePaths.getTempFontsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(this.fontDirPath);
      File destDir = new File(tempPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String getImagePath(String name) {
    ArrayList<ProjectResourceBean> imagesList = this.images;
    if (imagesList != null && imagesList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(name)) {
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(this.imageDirPath);
          pathBuilder.append(File.separator);
          pathBuilder.append(projectResourceBean.resFullName);
          return pathBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void backupImages() {
    String tempPath = SketchwarePaths.getTempImagesPath();
    try {
      this.fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(this.imageDirPath);
      File destDir = new File(tempPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public ProjectResourceBean getImageBean(String name) {
    for (ProjectResourceBean projectResourceBean : this.images) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupSounds() {
    String tempPath = SketchwarePaths.getTempSoundsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(this.soundDirPath);
      File destDir = new File(tempPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public int getImageResType(String name) {
    ArrayList<ProjectResourceBean> imagesList = this.images;
    if (imagesList != null && imagesList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.images) {
        if (projectResourceBean.resName.equals(name))
          return projectResourceBean.resType; 
      }  
    return -1;
  }
  
  public void deleteTempDirs() {
    String tempImagesPath = SketchwarePaths.getTempImagesPath();
    String tempSoundsPath = SketchwarePaths.getTempSoundsPath();
    String tempFontsPath = SketchwarePaths.getTempFontsPath();
    try {
      this.fileUtil.deleteDirectoryByPath(tempImagesPath);
      this.fileUtil.deleteDirectoryByPath(tempSoundsPath);
      this.fileUtil.deleteDirectoryByPath(tempFontsPath);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public String getSoundPath(String name) {
    ArrayList<ProjectResourceBean> soundsList = this.sounds;
    if (soundsList != null && soundsList.size() > 0)
      for (ProjectResourceBean projectResourceBean : this.sounds) {
        if (projectResourceBean.resName.equals(name)) {
          StringBuilder pathBuilder = new StringBuilder();
          pathBuilder.append(this.soundDirPath);
          pathBuilder.append(File.separator);
          pathBuilder.append(projectResourceBean.resFullName);
          return pathBuilder.toString();
        } 
      }  
    return "";
  }
  
  public void deleteBackup() {
    String backupPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("resource");
    backupPath = pathBuilder.toString();
    this.fileUtil.deleteFileByPath(backupPath);
  }
  
  public ProjectResourceBean getSoundBean(String name) {
    for (ProjectResourceBean projectResourceBean : this.sounds) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public String getFontDirPath() {
    return this.fontDirPath;
  }
  
  public ArrayList<String> getFontNames() {
    ArrayList<String> fontNames = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext())
      fontNames.add(((ProjectResourceBean)iterator.next()).resName); 
    return fontNames;
  }
  
  public boolean hasFont(String name) {
    Iterator<ProjectResourceBean> iterator = this.fonts.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public String getImageDirPath() {
    return this.imageDirPath;
  }
  
  public boolean hasImage(String name) {
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> getImageNames() {
    ArrayList<String> imageNames = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.images.iterator();
    while (iterator.hasNext())
      imageNames.add(((ProjectResourceBean)iterator.next()).resName); 
    return imageNames;
  }
  
  public boolean hasSound(String name) {
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext()) {
      if (((ProjectResourceBean)iterator.next()).resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public String getSoundDirPath() {
    return this.soundDirPath;
  }
  
  public ArrayList<String> getSoundNames() {
    ArrayList<String> soundNames = new ArrayList<>();
    Iterator<ProjectResourceBean> iterator = this.sounds.iterator();
    while (iterator.hasNext())
      soundNames.add(((ProjectResourceBean)iterator.next()).resName); 
    return soundNames;
  }
  
  public boolean hasBackup() {
    String backupPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("resource");
    backupPath = pathBuilder.toString();
    return this.fileUtil.exists(backupPath);
  }
  
  public void loadFromBackup() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String backupDir = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupDir);
    pathBuilder.append(File.separator);
    pathBuilder.append("resource");
    String resourcePath = pathBuilder.toString();
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(resourcePath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      parseResourceData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { Log.w("ResourceManager", "Failed to close reader", e); }
    }
  }
  
  public void loadFromData() {
    this.images = new ArrayList<ProjectResourceBean>();
    this.sounds = new ArrayList<ProjectResourceBean>();
    this.fonts = new ArrayList<ProjectResourceBean>();
    String dataPath = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder sourceBuilder = new StringBuilder();
    sourceBuilder.append(dataPath);
    sourceBuilder.append(File.separator);
    sourceBuilder.append("resource");
    dataPath = sourceBuilder.toString();
    if (!this.fileUtil.exists(dataPath))
      return; 
    BufferedReader bufferedReader = null;
    try {
      byte[] bytes = this.fileUtil.readFileBytes(dataPath);
      String decryptedData = this.fileUtil.decryptToString(bytes);
      bufferedReader = new BufferedReader(new StringReader(decryptedData));
      parseResourceData(bufferedReader);
    } catch (Exception exception) {
      exception.printStackTrace();
    } finally {
      if (bufferedReader != null) try { bufferedReader.close(); } catch (Exception e) { Log.w("ResourceManager", "Failed to close reader", e); }
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
    String tempPath = SketchwarePaths.getTempFontsPath();
    try {
      this.fileUtil.deleteDirectory(new File(this.fontDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(this.fontDirPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void restoreImagesFromTemp() {
    String tempPath = SketchwarePaths.getTempImagesPath();
    try {
      this.fileUtil.deleteDirectory(new File(this.imageDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(this.imageDirPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void restoreSoundsFromTemp() {
    String tempPath = SketchwarePaths.getTempSoundsPath();
    try {
      this.fileUtil.deleteDirectory(new File(this.soundDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(this.soundDirPath);
      this.fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
  
  public void saveToData() {
    String dataDir = SketchwarePaths.getDataPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(dataDir);
    pathBuilder.append(File.separator);
    pathBuilder.append("resource");
    String resourcePath = pathBuilder.toString();
    StringBuffer contentBuffer = new StringBuffer();
    serializeResources(contentBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(contentBuffer.toString());
      this.fileUtil.writeBytes(resourcePath, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
    deleteBackup();
  }
  
  public void saveToBackup() {
    String backupPath = SketchwarePaths.getBackupPath(this.projectId);
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append(backupPath);
    pathBuilder.append(File.separator);
    pathBuilder.append("resource");
    backupPath = pathBuilder.toString();
    StringBuffer contentBuffer = new StringBuffer();
    serializeResources(contentBuffer);
    try {
      byte[] bytes = this.fileUtil.encryptString(contentBuffer.toString());
      this.fileUtil.writeBytes(backupPath, bytes);
    } catch (Exception exception) {
      exception.printStackTrace();
    } 
  }
}
