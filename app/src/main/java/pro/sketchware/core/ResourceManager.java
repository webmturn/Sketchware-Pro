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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

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

  private boolean imagesBackedUp;
  private boolean soundsBackedUp;
  private boolean fontsBackedUp;
  
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

  private String decryptFileToString(String filePath) throws IOException {
    byte[] fileBytes = fileUtil.readFileBytes(filePath);
    if (fileBytes == null && new File(filePath).length() > 0L)
      throw new IOException("Failed to read resource data file: " + filePath);
    return fileUtil.decryptToString(fileBytes);
  }

  private byte[] encryptStringForStorage(String content, String filePath) throws IOException {
    try {
      return fileUtil.encryptString(content);
    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to encrypt resource data file: " + filePath, e);
    }
  }

  private void loadFromPath(String resourcePath, String sourceName) {
    try (BufferedReader bufferedReader = new BufferedReader(new StringReader(decryptFileToString(resourcePath)))) {
      parseResourceData(bufferedReader);
    } catch (IOException | RuntimeException e) {
      Log.w("ResourceManager", "Failed to load " + sourceName + " for project " + projectId + " from " + resourcePath, e);
    }
  }
  
  public void parseResourceData(BufferedReader reader) throws IOException {
    ArrayList<ProjectResourceBean> parsedImages = new ArrayList<>();
    ArrayList<ProjectResourceBean> parsedSounds = new ArrayList<>();
    ArrayList<ProjectResourceBean> parsedFonts = new ArrayList<>();
    parseResourceData(reader, parsedImages, parsedSounds, parsedFonts);
    images = parsedImages;
    sounds = parsedSounds;
    fonts = parsedFonts;
  }

  private void parseResourceData(BufferedReader reader,
      ArrayList<ProjectResourceBean> targetImages,
      ArrayList<ProjectResourceBean> targetSounds,
      ArrayList<ProjectResourceBean> targetFonts) throws IOException {
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
            parseResourceSection(sectionName, contentBuffer.toString(), targetImages, targetSounds, targetFonts);
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
        parseResourceSection(sectionName, contentBuffer.toString(), targetImages, targetSounds, targetFonts);
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
        String sourcePath = fontDirPath + File.separator + projectResourceBean.resFullName;
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase(Locale.ROOT);
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (IOException e) {
          Log.w("ResourceManager", "Failed to copy font: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void parseResourceSection(String key, String value) {
    parseResourceSection(key, value, images, sounds, fonts);
  }

  private void parseResourceSection(String key, String value,
      ArrayList<ProjectResourceBean> targetImages,
      ArrayList<ProjectResourceBean> targetSounds,
      ArrayList<ProjectResourceBean> targetFonts) {
    if (value.trim().length() <= 0) return;
    try (BufferedReader reader = new BufferedReader(new StringReader(value))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().length() <= 0 || line.trim().charAt(0) != '{') continue;
        ProjectResourceBean bean = gson.fromJson(line, ProjectResourceBean.class);
        if (key.equals("images")) {
          targetImages.add(bean);
        } else if (key.equals("sounds")) {
          targetSounds.add(bean);
        } else if (key.equals("fonts")) {
          targetFonts.add(bean);
        }
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ResourceManager", "Failed to parse resource section: " + key, e);
    }
  }
  
  public final void serializeResources(StringBuilder buffer) {
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
    backupFonts();
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
        String sourcePath = imageDirPath + File.separator + projectResourceBean.resFullName;
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase(Locale.ROOT);
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (IOException e) {
          Log.w("ResourceManager", "Failed to copy image: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void setImages(ArrayList<ProjectResourceBean> list) {
    backupImages();
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
        String sourcePath = soundDirPath + File.separator + projectResourceBean.resFullName;
        String destPath = destDir + File.separator + projectResourceBean.resFullName.toLowerCase(Locale.ROOT);
        try {
          fileUtil.copyFile(sourcePath, destPath);
        } catch (IOException e) {
          Log.w("ResourceManager", "Failed to copy sound: " + projectResourceBean.resFullName, e);
        } 
      } 
    } 
  }
  
  public void setSounds(ArrayList<ProjectResourceBean> list) {
    backupSounds();
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
    if (fontsBackedUp || projectId.isEmpty()) return;
    String tempPath = SketchwarePaths.getTempFontsPath(projectId);
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(fontDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
      fontsBackedUp = true;
    } catch (IOException e) {
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
    if (imagesBackedUp || projectId.isEmpty()) return;
    String tempPath = SketchwarePaths.getTempImagesPath(projectId);
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(imageDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
      imagesBackedUp = true;
    } catch (IOException e) {
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
    if (soundsBackedUp || projectId.isEmpty()) return;
    String tempPath = SketchwarePaths.getTempSoundsPath(projectId);
    try {
      fileUtil.deleteDirectoryByPath(tempPath);
      File sourceDir = new File(soundDirPath);
      File destDir = new File(tempPath);
      fileUtil.copyDirectory(sourceDir, destDir);
      soundsBackedUp = true;
    } catch (IOException e) {
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
    String tempImagesPath = SketchwarePaths.getTempImagesPath(projectId);
    String tempSoundsPath = SketchwarePaths.getTempSoundsPath(projectId);
    String tempFontsPath = SketchwarePaths.getTempFontsPath(projectId);
    fileUtil.deleteDirectoryByPath(tempImagesPath);
    fileUtil.deleteDirectoryByPath(tempSoundsPath);
    fileUtil.deleteDirectoryByPath(tempFontsPath);
    resetBackupState();
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
    return hasDistinctBackup(
        SketchwarePaths.getBackupPath(projectId) + File.separator + "resource",
        SketchwarePaths.getDataPath(projectId) + File.separator + "resource");
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
    String resourcePath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    if (!hasUsableBackup(resourcePath))
      return; 
    loadFromPath(resourcePath, "resource backup");
  }
  
  public void loadFromData() {
    String dataPath = SketchwarePaths.getDataPath(projectId) + File.separator + "resource";
    if (!fileUtil.exists(dataPath))
      return; 
    loadFromPath(dataPath, "resource data");
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
  
  /**
   * Ensures all resource types are backed up before any modification.
   * Called lazily — only when resources are about to change.
   */
  public void ensureBackedUp() {
    backupImages();
    backupSounds();
    backupFonts();
  }

  /** Resets backup flags after save, so next modification cycle gets a fresh backup. */
  public void resetBackupState() {
    imagesBackedUp = false;
    soundsBackedUp = false;
    fontsBackedUp = false;
  }

  /** Returns true if any resource type has been backed up via lazy backup (i.e., resources were modified). */
  public boolean hasLazyBackup() {
    return imagesBackedUp || soundsBackedUp || fontsBackedUp;
  }

  public void restoreFontsFromTemp() {
    String tempPath = SketchwarePaths.getTempFontsPath(projectId);
    File sourceDir = new File(tempPath);
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      Log.w("ResourceManager", "Skipped restoring fonts from missing temp dir: " + tempPath);
      return;
    }
    try {
      fileUtil.deleteDirectory(new File(fontDirPath));
      File destDir = new File(fontDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (IOException e) {
      Log.w("ResourceManager", "Failed to restore fonts from temp", e);
    } 
  }
  
  public void restoreImagesFromTemp() {
    String tempPath = SketchwarePaths.getTempImagesPath(projectId);
    File sourceDir = new File(tempPath);
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      Log.w("ResourceManager", "Skipped restoring images from missing temp dir: " + tempPath);
      return;
    }
    try {
      fileUtil.deleteDirectory(new File(imageDirPath));
      File destDir = new File(imageDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (IOException e) {
      Log.w("ResourceManager", "Failed to restore images from temp", e);
    } 
  }
  
  public void restoreSoundsFromTemp() {
    String tempPath = SketchwarePaths.getTempSoundsPath(projectId);
    File sourceDir = new File(tempPath);
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      Log.w("ResourceManager", "Skipped restoring sounds from missing temp dir: " + tempPath);
      return;
    }
    try {
      fileUtil.deleteDirectory(new File(soundDirPath));
      File destDir = new File(soundDirPath);
      fileUtil.copyDirectory(sourceDir, destDir);
    } catch (IOException e) {
      Log.w("ResourceManager", "Failed to restore sounds from temp", e);
    } 
  }
  
  public boolean saveToData() {
    String resourcePath = SketchwarePaths.getDataPath(projectId) + File.separator + "resource";
    StringBuilder contentBuffer = new StringBuilder();
    serializeResources(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(resourcePath, encryptStringForStorage(contentBuffer.toString(), resourcePath));
      if (!saved)
        Log.w("ResourceManager", "Failed to write resource data for project " + projectId + " to " + resourcePath);
      if (saved)
        deleteBackup();
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.w("ResourceManager", "Failed to save resource data for project " + projectId + " to " + resourcePath, e);
    } 
    return false;
  }
  
  public boolean saveToBackup() {
    String backupPath = SketchwarePaths.getBackupPath(projectId) + File.separator + "resource";
    StringBuilder contentBuffer = new StringBuilder();
    serializeResources(contentBuffer);
    try {
      boolean saved = fileUtil.writeBytes(backupPath, encryptStringForStorage(contentBuffer.toString(), backupPath));
      if (!saved)
        Log.w("ResourceManager", "Failed to write resource backup for project " + projectId + " to " + backupPath);
      return saved;
    } catch (IOException | RuntimeException e) {
      Log.w("ResourceManager", "Failed to save resource backup for project " + projectId + " to " + backupPath, e);
    } 
    return false;
  }
}
