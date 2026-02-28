package pro.sketchware.core;

import android.util.Log;

import com.besome.sketch.beans.ProjectResourceBean;
import com.bumptech.glide.signature.StringSignature;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

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
    imageDirPath = value;
    soundDirPath = extra;
    fontDirPath = tag;
    projectId = key;
    refreshCacheSignature();
    fileUtil = new EncryptedFileUtil(false);
    images = new ArrayList<>();
    sounds = new ArrayList<>();
    fonts = new ArrayList<>();
    gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
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
  
  public void parseResourceData(BufferedReader reader) throws IOException {
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
    ArrayList<ProjectResourceBean> fontsList = fonts;
    if (fontsList != null && fontsList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : fonts) {
        String sourcePath = fontDirPath + File.separator + projectResourceBean.resFullName.toLowerCase();
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase();
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (Exception e) {
          Log.w("ResourceManager", "Failed to copy font: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void parseResourceSection(String key, String value) {
    if (value.trim().length() <= 0) return;
    try (BufferedReader reader = new BufferedReader(new StringReader(value))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0 || line.trim().charAt(0) != '{') continue;
        ProjectResourceBean bean = gson.fromJson(line, ProjectResourceBean.class);
        if (key.equals("images")) {
          images.add(bean);
        } else if (key.equals("sounds")) {
          sounds.add(bean);
        } else if (key.equals("fonts")) {
          fonts.add(bean);
        }
      }
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to parse resource section: " + key, e);
    }
  }
  
  public final void serializeResources(StringBuffer buffer) {
    buffer.append("@images\n");
    for (ProjectResourceBean projectResourceBean : images) {
      buffer.append(gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
    buffer.append("@sounds\n");
    for (ProjectResourceBean projectResourceBean : sounds) {
      buffer.append(gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
    buffer.append("@fonts\n");
    for (ProjectResourceBean projectResourceBean : fonts) {
      buffer.append(gson.toJson(projectResourceBean, ProjectResourceBean.class));
      buffer.append("\n");
    } 
  }
  
  public void setFonts(ArrayList<ProjectResourceBean> list) {
    fonts = list;
  }
  
  public void cleanupUnusedFonts() {
    File[] files = new File(fontDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : fonts) {
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
    ArrayList<ProjectResourceBean> imagesList = images;
    if (imagesList != null && imagesList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : images) {
        String sourcePath = imageDirPath + File.separator + projectResourceBean.resFullName.toLowerCase();
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase();
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (Exception e) {
          Log.w("ResourceManager", "Failed to copy image: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void setImages(ArrayList<ProjectResourceBean> list) {
    images = list;
  }
  
  public void cleanupUnusedImages() {
    File[] files = new File(imageDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : images) {
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
    ArrayList<ProjectResourceBean> soundsList = sounds;
    if (soundsList != null && soundsList.size() > 0) {
      File file = new File(destDir);
      if (!file.exists())
        file.mkdirs(); 
      for (ProjectResourceBean projectResourceBean : sounds) {
        String sourcePath = soundDirPath + File.separator + projectResourceBean.resFullName.toLowerCase();
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase();
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (Exception e) {
          Log.w("ResourceManager", "Failed to copy sound: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void setSounds(ArrayList<ProjectResourceBean> list) {
    sounds = list;
  }
  
  public String getFontPath(String name) {
    ArrayList<ProjectResourceBean> fontsList = fonts;
    if (fontsList != null && fontsList.size() > 0)
      for (ProjectResourceBean projectResourceBean : fonts) {
        if (projectResourceBean.resName.equals(name))
          return fontDirPath + File.separator + projectResourceBean.resFullName;
      }  
    return "";
  }
  
  public void cleanupUnusedSounds() {
    File[] files = new File(soundDirPath).listFiles();
    if (files == null || files.length <= 0) return;
    for (File file : files) {
      file.isDirectory();
      if (file.isFile()) {
        boolean found = false;
        for (ProjectResourceBean bean : sounds) {
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
    for (ProjectResourceBean projectResourceBean : fonts) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupFonts() {
    String tempPath = SketchwarePaths.getTempFontsPath();
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(fontDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to backup fonts", e);
    } 
  }
  
  public String getImagePath(String name) {
    ArrayList<ProjectResourceBean> imagesList = images;
    if (imagesList != null && imagesList.size() > 0)
      for (ProjectResourceBean projectResourceBean : images) {
        if (projectResourceBean.resName.equals(name))
          return imageDirPath + File.separator + projectResourceBean.resFullName;
      }  
    return "";
  }
  
  public void backupImages() {
    String tempPath = SketchwarePaths.getTempImagesPath();
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(imageDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to backup images", e);
    } 
  }
  
  public ProjectResourceBean getImageBean(String name) {
    for (ProjectResourceBean projectResourceBean : images) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public void backupSounds() {
    String tempPath = SketchwarePaths.getTempSoundsPath();
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(soundDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to backup sounds", e);
    } 
  }
  
  public int getImageResType(String name) {
    ArrayList<ProjectResourceBean> imagesList = images;
    if (imagesList != null && imagesList.size() > 0)
      for (ProjectResourceBean projectResourceBean : images) {
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
      fileUtil.deleteDirectoryByPath(tempImagesPath);
      fileUtil.deleteDirectoryByPath(tempSoundsPath);
      fileUtil.deleteDirectoryByPath(tempFontsPath);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to delete temp dirs", e);
    } 
  }
  
  public String getSoundPath(String name) {
    ArrayList<ProjectResourceBean> soundsList = sounds;
    if (soundsList != null && soundsList.size() > 0)
      for (ProjectResourceBean projectResourceBean : sounds) {
        if (projectResourceBean.resName.equals(name))
          return soundDirPath + File.separator + projectResourceBean.resFullName;
      }  
    return "";
  }
  
  public void deleteBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    fileUtil.deleteFileByPath(backupPath);
  }
  
  public ProjectResourceBean getSoundBean(String name) {
    for (ProjectResourceBean projectResourceBean : sounds) {
      if (projectResourceBean.resName.equals(name))
        return projectResourceBean; 
    } 
    return null;
  }
  
  public String getFontDirPath() {
    return fontDirPath;
  }
  
  public ArrayList<String> getFontNames() {
    ArrayList<String> fontNames = new ArrayList<>();
    for (ProjectResourceBean bean : fonts)
      fontNames.add(bean.resName); 
    return fontNames;
  }
  
  public boolean hasFont(String name) {
    for (ProjectResourceBean bean : fonts) {
      if (bean.resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public String getImageDirPath() {
    return imageDirPath;
  }
  
  public boolean hasImage(String name) {
    for (ProjectResourceBean bean : images) {
      if (bean.resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public ArrayList<String> getImageNames() {
    ArrayList<String> imageNames = new ArrayList<>();
    for (ProjectResourceBean bean : images)
      imageNames.add(bean.resName); 
    return imageNames;
  }
  
  public boolean hasSound(String name) {
    for (ProjectResourceBean bean : sounds) {
      if (bean.resName.equals(name))
        return true; 
    } 
    return false;
  }
  
  public String getSoundDirPath() {
    return soundDirPath;
  }
  
  public ArrayList<String> getSoundNames() {
    ArrayList<String> soundNames = new ArrayList<>();
    for (ProjectResourceBean bean : sounds)
      soundNames.add(bean.resName); 
    return soundNames;
  }
  
  public boolean hasBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    return fileUtil.exists(backupPath);
  }
  
  public void loadFromBackup() {
    images = new ArrayList<>();
    sounds = new ArrayList<>();
    fonts = new ArrayList<>();
    String resourcePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(resourcePath))))) {
      parseResourceData(bufferedReader);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to load from backup", e);
    }
  }
  
  public void loadFromData() {
    images = new ArrayList<>();
    sounds = new ArrayList<>();
    fonts = new ArrayList<>();
    String dataPath = SketchwarePaths.getDataPath(projectId) + File.separator + "resource";
    if (!fileUtil.exists(dataPath))
      return; 
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(fileUtil.decryptToString(fileUtil.readFileBytes(dataPath))))) {
      parseResourceData(bufferedReader);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to load from data", e);
    }
  }
  
  public void resetAll() {
    projectId = "";
    imageDirPath = "";
    soundDirPath = "";
    fontDirPath = "";
    images = new ArrayList<>();
    sounds = new ArrayList<>();
    fonts = new ArrayList<>();
  }
  
  public void restoreFontsFromTemp() {
    String tempPath = SketchwarePaths.getTempFontsPath();
    try {
      fileUtil.deleteDirectory(new File(fontDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(fontDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to restore fonts from temp", e);
    } 
  }
  
  public void restoreImagesFromTemp() {
    String tempPath = SketchwarePaths.getTempImagesPath();
    try {
      fileUtil.deleteDirectory(new File(imageDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(imageDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to restore images from temp", e);
    } 
  }
  
  public void restoreSoundsFromTemp() {
    String tempPath = SketchwarePaths.getTempSoundsPath();
    try {
      fileUtil.deleteDirectory(new File(soundDirPath));
      File sourceDir = new File(tempPath);
      File destDir = new File(soundDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to restore sounds from temp", e);
    } 
  }
  
  public void saveToData() {
    String resourcePath = SketchwarePaths.getDataPath(projectId) + File.separator + "resource";
    StringBuffer contentBuffer = new StringBuffer();
    serializeResources(contentBuffer);
    try {
      byte[] bytes = fileUtil.encryptString(contentBuffer.toString());
      fileUtil.writeBytes(resourcePath, bytes);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to save to data", e);
    } 
    deleteBackup();
  }
  
  public void saveToBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    StringBuffer contentBuffer = new StringBuffer();
    serializeResources(contentBuffer);
    try {
      byte[] bytes = fileUtil.encryptString(contentBuffer.toString());
      fileUtil.writeBytes(backupPath, bytes);
    } catch (Exception e) {
      Log.w("ResourceManager", "Failed to save to backup", e);
    } 
  }
}
