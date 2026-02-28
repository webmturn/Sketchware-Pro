package pro.sketchware.core;

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
  public static void extractAssetZip(Context context, String key, String value) {
    byte[] buf = new byte[1024];
    String destDir = value;
    if (!destDir.endsWith(File.separator)) {
      destDir = destDir + File.separator;
    }
    File dir = new File(destDir);
    if (!dir.exists()) {
      if (!dir.mkdirs()) dir.mkdir();
    }
    try (BufferedInputStream bis = new BufferedInputStream(context.getAssets().open(key));
         ZipInputStream zis = new ZipInputStream(bis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          try (FileOutputStream fos = new FileOutputStream(file, false)) {
            int len;
            while ((len = zis.read(buf, 0, buf.length)) > 0) {
              fos.write(buf, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (Exception e) {
            Log.w("ZipUtil", "Failed to extract entry: " + entry.getName(), e);
          }
        }
      }
    } catch (Exception e) {
      Log.e("ZipUtil", "Failed to extract asset zip: " + key, e);
      throw new RuntimeException(e);
    }
  }
  
  public int addDirectoryToZip(String basePath, File dir, ZipOutputStream zipOut, ArrayList<String> list) {
    File[] files = dir.listFiles();
    if (files == null) return 0;
    if (files.length == 0) {
      String absPath = dir.getAbsolutePath();
      addFileToZip(basePath, absPath.substring(basePath.length(), absPath.length()), zipOut);
    }
    int count = 0;
    for (File file : files) {
      if (file.isDirectory()) {
        addDirectoryToZip(basePath, file, zipOut, list);
      }
      if (file.isFile()) {
        String absPath = file.getAbsolutePath();
        String relPath = absPath.substring(basePath.length(), absPath.length());
        boolean excluded = false;
        for (String exclude : list) {
          if (absPath.contains(exclude)) {
            excluded = true;
            break;
          }
        }
        if (!excluded && addFileToZip(basePath, relPath, zipOut)) {
          count++;
        }
      }
    }
    return count;
  }
  
  public void extractZipStream(InputStream inputStream, String destPath) {
    byte[] buf = new byte[1024];
    String destDir = destPath;
    if (!destDir.endsWith(File.separator)) {
      destDir = destDir + File.separator;
    }
    File dir = new File(destDir);
    if (!dir.exists()) dir.mkdirs();
    try (BufferedInputStream bis = new BufferedInputStream(inputStream);
         ZipInputStream zis = new ZipInputStream(bis)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          try (FileOutputStream fos = new FileOutputStream(file, false)) {
            int len;
            while ((len = zis.read(buf, 0, buf.length)) > 0) {
              fos.write(buf, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (Exception e) {
            Log.w("ZipUtil", "Failed to extract entry: " + entry.getName(), e);
          }
        }
      }
    } catch (IOException e) {
      Log.w("ZipUtil", "Failed to extract zip stream", e);
    }
  }
  
  public void extractZipFile(String key, String value) throws FileNotFoundException {
    extractZipStream(new FileInputStream(key), value);
  }
  
  public void createZipFile(String zipFilePath, ArrayList<String> list1, ArrayList<String> list2) {
    try (FileOutputStream fos = new FileOutputStream(zipFilePath);
         ZipOutputStream zos = new ZipOutputStream(fos)) {
      for (String path : list1) {
        addDirectoryToZip(path, new File(path), zos, list2);
      }
    } catch (Exception e) {
      Log.w("ZipUtil", "Failed to create zip file: " + zipFilePath, e);
    }
  }
  
  public boolean addFileToZip(String key, String value, ZipOutputStream zipOut) {
    File file = new File(key + value);
    if (!file.isFile()) {
      try { if (zipOut != null) zipOut.closeEntry(); } catch (IOException e) { Log.w("ZipUtil", "Failed to close zip entry", e); }
      return false;
    }
    try (FileInputStream fis = new FileInputStream(file);
         BufferedInputStream bis = new BufferedInputStream(fis)) {
      ZipEntry entry = new ZipEntry(value);
      zipOut.putNextEntry(entry);
      byte[] buf = new byte[1024];
      int len;
      while ((len = bis.read(buf)) > 0) {
        zipOut.write(buf, 0, len);
      }
    } catch (Exception e) {
      Log.w("ZipUtil", "Failed to add file to zip: " + key + value, e);
    } finally {
      try { if (zipOut != null) zipOut.closeEntry(); } catch (IOException e) { Log.w("ZipUtil", "Failed to close zip entry", e); }
    }
    return true;
  }
  
  public byte[] readFileToBytes(String filePath) {
    try (FileInputStream fis = new FileInputStream(new File(filePath));
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[1024];
      int len;
      while ((len = fis.read(buf)) > 0) {
        baos.write(buf, 0, len);
      }
      return baos.toByteArray();
    } catch (Exception e) {
      Log.w("ZipUtil", "Failed to read file to bytes: " + filePath, e);
    }
    return null;
  }
}
