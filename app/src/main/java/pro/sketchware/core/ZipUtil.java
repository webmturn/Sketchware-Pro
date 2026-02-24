package pro.sketchware.core;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
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
    java.io.BufferedInputStream bis = null;
    java.util.zip.ZipInputStream zis = null;
    try {
      bis = new java.io.BufferedInputStream(context.getAssets().open(key));
      zis = new java.util.zip.ZipInputStream(bis);
      java.util.zip.ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          FileOutputStream fos = null;
          try {
            fos = new FileOutputStream(file, false);
            int len;
            while ((len = zis.read(buf, 0, buf.length)) > 0) {
              fos.write(buf, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if (fos != null) fos.close();
          }
        }
      }
    } catch (Exception e) {
      android.util.Log.e("DEBUG", e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      if (zis != null) try { zis.close(); } catch (IOException e) { e.printStackTrace(); }
      if (bis != null) try { bis.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public int addDirectoryToZip(String str, File paramFile, ZipOutputStream paramZipOutputStream, ArrayList<String> list) {
    File[] files = paramFile.listFiles();
    if (files == null) return 0;
    if (files.length == 0) {
      String absPath = paramFile.getAbsolutePath();
      addFileToZip(str, absPath.substring(str.length(), absPath.length()), paramZipOutputStream);
    }
    int count = 0;
    for (File file : files) {
      if (file.isDirectory()) {
        addDirectoryToZip(str, file, paramZipOutputStream, list);
      }
      if (file.isFile()) {
        String absPath = file.getAbsolutePath();
        String relPath = absPath.substring(str.length(), absPath.length());
        boolean excluded = false;
        for (String exclude : list) {
          if (absPath.contains(exclude)) {
            excluded = true;
            break;
          }
        }
        if (!excluded && addFileToZip(str, relPath, paramZipOutputStream)) {
          count++;
        }
      }
    }
    return count;
  }
  
  public void extractZipStream(InputStream paramInputStream, String str) {
    byte[] buf = new byte[1024];
    String destDir = str;
    if (!destDir.endsWith(File.separator)) {
      destDir = destDir + File.separator;
    }
    File dir = new File(destDir);
    if (!dir.exists()) dir.mkdirs();
    java.io.BufferedInputStream bis = null;
    java.util.zip.ZipInputStream zis = null;
    try {
      bis = new java.io.BufferedInputStream(paramInputStream);
      zis = new java.util.zip.ZipInputStream(bis);
      java.util.zip.ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String filePath = destDir + entry.getName();
        File file = new File(filePath);
        if (entry.isDirectory()) {
          if (!file.isDirectory()) file.mkdirs();
        } else {
          File parent = file.getParentFile();
          if (parent != null && !parent.isDirectory()) parent.mkdirs();
          FileOutputStream fos = null;
          try {
            fos = new FileOutputStream(file, false);
            int len;
            while ((len = zis.read(buf, 0, buf.length)) > 0) {
              fos.write(buf, 0, len);
            }
            zis.closeEntry();
            fos.flush();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if (fos != null) fos.close();
          }
        }
      }
    } catch (java.io.FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (bis != null) try { bis.close(); } catch (IOException e) { e.printStackTrace(); }
      if (zis != null) try { zis.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public void extractZipFile(String key, String value) throws java.io.FileNotFoundException {
    extractZipStream(new FileInputStream(key), value);
  }
  
  public void createZipFile(String str, ArrayList<String> paramArrayList1, ArrayList<String> paramArrayList2) {
    FileOutputStream fos = null;
    ZipOutputStream zos = null;
    try {
      fos = new FileOutputStream(str);
      zos = new ZipOutputStream(fos);
      Iterator<String> iterator = paramArrayList1.iterator();
      while (iterator.hasNext()) {
        String path = iterator.next();
        addDirectoryToZip(path, new File(path), zos, paramArrayList2);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (zos != null) try { zos.close(); } catch (IOException e) { e.printStackTrace(); }
      if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public boolean addFileToZip(String key, String value, ZipOutputStream paramZipOutputStream) {
    File file = new File(key + value);
    if (!file.isFile()) {
      try { if (paramZipOutputStream != null) paramZipOutputStream.closeEntry(); } catch (IOException e) { e.printStackTrace(); }
      return false;
    }
    FileInputStream fis = null;
    java.io.BufferedInputStream bis = null;
    try {
      fis = new FileInputStream(file);
      bis = new java.io.BufferedInputStream(fis);
      java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(value);
      paramZipOutputStream.putNextEntry(entry);
      byte[] buf = new byte[1024];
      int len;
      while ((len = bis.read(buf)) > 0) {
        paramZipOutputStream.write(buf, 0, len);
      }
    } catch (Exception e) {
    } finally {
      try { if (paramZipOutputStream != null) paramZipOutputStream.closeEntry(); } catch (IOException e) { e.printStackTrace(); }
      if (fis != null) try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
      if (bis != null) try { bis.close(); } catch (IOException e) { e.printStackTrace(); }
    }
    return true;
  }
  
  public byte[] readFileToBytes(String str) {
    ByteArrayOutputStream baos = null;
    try {
      baos = new ByteArrayOutputStream();
      FileInputStream fis = new FileInputStream(new File(str));
      byte[] buf = new byte[1024];
      int len;
      while ((len = fis.read(buf)) > 0) {
        baos.write(buf, 0, len);
      }
      fis.close();
      return baos.toByteArray();
    } catch (Exception e) {
    } finally {
      if (baos != null) try { baos.close(); } catch (Exception e) {}
    }
    return null;
  }
}
