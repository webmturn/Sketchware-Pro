package a.a.a;

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

public class KB {
  public static void a(Context paramContext, String paramString1, String paramString2) {
    byte[] buf = new byte[1024];
    String destDir = paramString2;
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
      bis = new java.io.BufferedInputStream(paramContext.getAssets().open(paramString1));
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
  
  public int a(String paramString, File paramFile, ZipOutputStream paramZipOutputStream, ArrayList<String> paramArrayList) {
    File[] files = paramFile.listFiles();
    if (files == null) return 0;
    if (files.length == 0) {
      String absPath = paramFile.getAbsolutePath();
      a(paramString, absPath.substring(paramString.length(), absPath.length()), paramZipOutputStream);
    }
    int count = 0;
    for (File file : files) {
      if (file.isDirectory()) {
        a(paramString, file, paramZipOutputStream, paramArrayList);
      }
      if (file.isFile()) {
        String absPath = file.getAbsolutePath();
        String relPath = absPath.substring(paramString.length(), absPath.length());
        boolean excluded = false;
        for (String exclude : paramArrayList) {
          if (absPath.contains(exclude)) {
            excluded = true;
            break;
          }
        }
        if (!excluded && a(paramString, relPath, paramZipOutputStream)) {
          count++;
        }
      }
    }
    return count;
  }
  
  public void a(InputStream paramInputStream, String paramString) {
    byte[] buf = new byte[1024];
    String destDir = paramString;
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
  
  public void a(String paramString1, String paramString2) throws java.io.FileNotFoundException {
    a(new FileInputStream(paramString1), paramString2);
  }
  
  public void a(String paramString, ArrayList<String> paramArrayList1, ArrayList<String> paramArrayList2) {
    FileOutputStream fos = null;
    ZipOutputStream zos = null;
    try {
      fos = new FileOutputStream(paramString);
      zos = new ZipOutputStream(fos);
      Iterator<String> iterator = paramArrayList1.iterator();
      while (iterator.hasNext()) {
        String path = iterator.next();
        a(path, new File(path), zos, paramArrayList2);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (zos != null) try { zos.close(); } catch (IOException e) { e.printStackTrace(); }
      if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public boolean a(String paramString1, String paramString2, ZipOutputStream paramZipOutputStream) {
    File file = new File(paramString1 + paramString2);
    if (!file.isFile()) {
      try { if (paramZipOutputStream != null) paramZipOutputStream.closeEntry(); } catch (IOException e) { e.printStackTrace(); }
      return false;
    }
    FileInputStream fis = null;
    java.io.BufferedInputStream bis = null;
    try {
      fis = new FileInputStream(file);
      bis = new java.io.BufferedInputStream(fis);
      java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(paramString2);
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
  
  public byte[] a(String paramString) {
    ByteArrayOutputStream baos = null;
    try {
      baos = new ByteArrayOutputStream();
      FileInputStream fis = new FileInputStream(new File(paramString));
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


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\KB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */