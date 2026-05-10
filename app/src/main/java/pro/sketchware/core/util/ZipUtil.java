package pro.sketchware.core.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
  public static void extractAssetZip(Context context, String assetPath, String destinationPath) {
    byte[] buffer = new byte[1024];
    String destinationDir = destinationPath;
    if (!destinationDir.endsWith(File.separator)) {
      destinationDir = destinationDir + File.separator;
    }
    File dir = new File(destinationDir);
    if (!dir.exists()) {
      if (!dir.mkdirs()) dir.mkdir();
    }
    try (BufferedInputStream bis = new BufferedInputStream(context.getAssets().open(assetPath));
         ZipInputStream zis = new ZipInputStream(bis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destinationDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          try (FileOutputStream fos = new FileOutputStream(file, false)) {
            int len;
            while ((len = zis.read(buffer, 0, buffer.length)) > 0) {
              fos.write(buffer, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (IOException | RuntimeException e) {
            Log.w("ZipUtil", "Failed to extract entry: " + entry.getName(), e);
          }
        }
      }
    } catch (IOException | RuntimeException e) {
      Log.e("ZipUtil", "Failed to extract asset zip: " + assetPath, e);
      throw new RuntimeException(e);
    }
  }
  
  public int addDirectoryToZip(String basePath, File directory, ZipOutputStream zipOutputStream, ArrayList<String> excludedPaths) {
    File[] files = directory.listFiles();
    if (files == null) return 0;
    if (files.length == 0) {
      String absPath = directory.getAbsolutePath();
      addFileToZip(basePath, absPath.substring(basePath.length(), absPath.length()), zipOutputStream);
    }
    int count = 0;
    for (File file : files) {
      if (file.isDirectory()) {
        addDirectoryToZip(basePath, file, zipOutputStream, excludedPaths);
      }
      if (file.isFile()) {
        String absPath = file.getAbsolutePath();
        String relPath = absPath.substring(basePath.length(), absPath.length());
        boolean excluded = false;
        for (String exclude : excludedPaths) {
          if (absPath.contains(exclude)) {
            excluded = true;
            break;
          }
        }
        if (!excluded && addFileToZip(basePath, relPath, zipOutputStream)) {
          count++;
        }
      }
    }
    return count;
  }
  
  public void extractZipStream(InputStream inputStream, String destinationPath) {
    byte[] buffer = new byte[1024];
    String destinationDir = destinationPath;
    if (!destinationDir.endsWith(File.separator)) {
      destinationDir = destinationDir + File.separator;
    }
    File dir = new File(destinationDir);
    if (!dir.exists()) dir.mkdirs();
    try (BufferedInputStream bis = new BufferedInputStream(inputStream);
         ZipInputStream zis = new ZipInputStream(bis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destinationDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          try (FileOutputStream fos = new FileOutputStream(file, false)) {
            int len;
            while ((len = zis.read(buffer, 0, buffer.length)) > 0) {
              fos.write(buffer, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (IOException | RuntimeException e) {
            Log.w("ZipUtil", "Failed to extract entry: " + entry.getName(), e);
          }
        }
      }
    } catch (IOException e) {
      Log.w("ZipUtil", "Failed to extract zip stream", e);
    }
  }
  
  public void extractZipFile(String zipFilePath, String destinationPath) throws FileNotFoundException {
    extractZipStream(new FileInputStream(zipFilePath), destinationPath);
  }
  
  public void createZipFile(String zipFilePath, ArrayList<String> sourceDirectoryPaths, ArrayList<String> excludedPaths) {
    try (FileOutputStream fos = new FileOutputStream(zipFilePath);
         ZipOutputStream zos = new ZipOutputStream(fos)) {
      for (String sourceDirectoryPath : sourceDirectoryPaths) {
        addDirectoryToZip(sourceDirectoryPath, new File(sourceDirectoryPath), zos, excludedPaths);
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ZipUtil", "Failed to create zip file: " + zipFilePath, e);
    }
  }
  
  public boolean addFileToZip(String basePath, String relativePath, ZipOutputStream zipOutputStream) {
    File file = new File(basePath + relativePath);
    if (!file.isFile()) {
      try { if (zipOutputStream != null) zipOutputStream.closeEntry(); } catch (IOException e) { Log.w("ZipUtil", "Failed to close zip entry", e); }
      return false;
    }
    try (FileInputStream fis = new FileInputStream(file);
         BufferedInputStream bis = new BufferedInputStream(fis)) {
      ZipEntry entry = new ZipEntry(relativePath);
      zipOutputStream.putNextEntry(entry);
      byte[] buffer = new byte[1024];
      int len;
      while ((len = bis.read(buffer)) > 0) {
        zipOutputStream.write(buffer, 0, len);
      }
    } catch (IOException | RuntimeException e) {
      Log.w("ZipUtil", "Failed to add file to zip: " + basePath + relativePath, e);
    } finally {
      try { if (zipOutputStream != null) zipOutputStream.closeEntry(); } catch (IOException e) { Log.w("ZipUtil", "Failed to close zip entry", e); }
    }
    return true;
  }
  
  public byte[] readFileToBytes(String filePath) {
    try (FileInputStream fis = new FileInputStream(new File(filePath));
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) > 0) {
        baos.write(buffer, 0, len);
      }
      return baos.toByteArray();
    } catch (IOException | RuntimeException e) {
      Log.w("ZipUtil", "Failed to read file to bytes: " + filePath, e);
    }
    return null;
  }
}
