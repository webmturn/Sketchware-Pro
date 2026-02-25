package pro.sketchware.core;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedFileUtil {
  public boolean encryptionEnabled = false;
  
  public EncryptedFileUtil() {
    this(false);
  }
  
  public EncryptedFileUtil(boolean flag) {
    this.encryptionEnabled = flag;
  }
  
  public long getAssetFileSize(Context context, String value) {
    try (InputStream inputStream = context.getAssets().open(value)) {
      return inputStream.available();
    } catch (IOException e) {
      e.printStackTrace();
      return -1L;
    }
  }
  
  public String decryptToString(byte[] data) throws Exception {
    return new String(decrypt(data), "UTF-8");
  }
  
  public void copyAssetFile(Context context, String key, String value) {
    android.content.res.AssetManager assets = context.getAssets();
    int sepIdx = value.lastIndexOf(File.separator);
    if (sepIdx > 0) {
      mkdirs(value.substring(0, sepIdx));
    }
    java.io.InputStream is = null;
    java.io.FileOutputStream fos = null;
    try {
      is = assets.open(key);
      fos = new java.io.FileOutputStream(value, false);
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
      if (this.encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "assetFile =>" + value + " copy success.");
      }
    } catch (IOException e) {
    } finally {
      if (is != null) try { is.close(); } catch (IOException e) { e.printStackTrace(); }
      if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public void deleteDirectory(File file) {
    deleteRecursive(file, true);
  }
  
  public void copyDirectory(File srcFile, File destFile) throws IOException {
    StringBuilder errorBuilder;
    if (srcFile.isDirectory()) {
      if (destFile.exists() || destFile.mkdirs()) {
        String[] parts = srcFile.list();
        if (parts != null)
          for (int b = 0; b < parts.length; b++)
            copyDirectory(new File(srcFile, parts[b]), new File(destFile, parts[b]));  
        return;
      } 
      errorBuilder = new StringBuilder();
      errorBuilder.append("Cannot create dir ");
      errorBuilder.append(destFile.getAbsolutePath());
      throw new IOException(errorBuilder.toString());
    } 
    copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
  }
  
  public void deleteRecursive(File dir, boolean flag) {
    if (dir.exists()) {
      File[] files = dir.listFiles();
      if (files != null) {
        int i = files.length;
        for (int b = 0; b < i; b++) {
          File file = files[b];
          if (file.isDirectory())
            deleteDirectory(file); 
          if (file.isFile())
            if (file.delete()) {
              if (this.encryptionEnabled) {
                String tag = EncryptedFileUtil.class.getSimpleName();
                StringBuilder logBuilder = new StringBuilder();
                logBuilder.append("Delete file success.");
                logBuilder.append(file.getAbsolutePath());
                Log.d(tag, logBuilder.toString());
              } 
            } else if (this.encryptionEnabled) {
              String tag = EncryptedFileUtil.class.getSimpleName();
              StringBuilder logBuilder = new StringBuilder();
              logBuilder.append("Delete file failed.");
              logBuilder.append(file.getAbsolutePath());
              Log.d(tag, logBuilder.toString());
            }  
        } 
      } 
      if (flag)
        dir.delete(); 
    } 
  }
  
  public void recreateDirectory(String value) {
    deleteDirectoryByPath(value);
    mkdirs(value);
  }
  
  public void copyFile(String key, String value) throws IOException {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try {
      fis = new FileInputStream(key);
      fos = new FileOutputStream(value, false);
      byte[] buf = new byte[1024];
      if (this.encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "src=" + key + ",dest=" + value);
      }
      int len;
      while ((len = fis.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
    } finally {
      if (fis != null) try { fis.close(); } catch (IOException e) { e.printStackTrace(); }
      if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public void deleteRecursiveByPath(String value, boolean flag) {
    deleteRecursive(new File(value), flag);
  }
  
  public void writeBytes(String value, byte[] data) {
    int i = value.lastIndexOf(File.separator);
    if (i > 0)
      mkdirs(value.substring(0, i)); 
    File file = new File(value);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } 
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      fos.write(data);
      fos.flush();
    } catch (IOException e) {
    } finally {
      if (fos != null) try { fos.close(); } catch (IOException e) {}
    }
  }
  
  public String readAssetFile(Context context, String value) {
    StringBuilder sb = new StringBuilder();
    java.io.BufferedReader reader = null;
    try {
      java.io.InputStream is = context.getAssets().open(value.trim());
      reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "utf-8"));
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append("\r\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) try { reader.close(); } catch (IOException e) { e.printStackTrace(); }
    }
    return sb.toString();
  }
  
  public void deleteFile(File file) {
    file.delete();
  }
  
  public void deleteDirectoryByPath(String value) {
    deleteRecursiveByPath(value, true);
  }
  
  public void writeText(String key, String value) {
    int i = key.lastIndexOf(File.separator);
    if (i > 0)
      mkdirs(key.substring(0, i)); 
    File file = new File(key);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } 
    FileWriter fw = null;
    try {
      fw = new FileWriter(file, false);
      fw.write(value);
      fw.flush();
    } catch (IOException e) {
    } finally {
      if (fw != null) try { fw.close(); } catch (IOException e) {}
    }
  }
  
  public byte[] decrypt(byte[] data) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    byte[] bytes = "sketchwaresecure".getBytes();
    cipher.init(2, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
    return cipher.doFinal(data);
  }
  
  public String readFileContent(File file) {
    StringBuilder sb = new StringBuilder();
    java.io.FileReader reader = null;
    try {
      reader = new java.io.FileReader(file);
      char[] buf = new char[1024];
      int len;
      while ((len = reader.read(buf)) > 0) {
        sb.append(new String(buf, 0, len));
      }
    } catch (IOException e) {
    } finally {
      if (reader != null) try { reader.close(); } catch (Exception e) {}
    }
    return sb.toString();
  }
  
  public void deleteFileByPath(String value) {
    deleteFile(new File(value));
  }
  
  public byte[] encrypt(byte[] data) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    byte[] bytes = "sketchwaresecure".getBytes();
    cipher.init(1, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
    return cipher.doFinal(data);
  }
  
  public byte[] encryptString(String value) throws Exception {
    return encrypt(value.getBytes("UTF-8"));
  }
  
  public boolean exists(String value) {
    return (new File(value)).exists();
  }
  
  public boolean mkdirs(String value) {
    return !exists(value) ? (new File(value)).mkdirs() : false;
  }
  
  public String readFile(String value) {
    return readFileContent(new File(value));
  }
  
  public byte[] readFileBytes(String value) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(new File(value));
      int size = fis.available();
      if (size > 0) {
        byte[] data = new byte[size];
        fis.read(data);
        return data;
      }
    } catch (IOException e) {
    } finally {
      if (fis != null) try { fis.close(); } catch (IOException e) {}
    }
    return null;
  }
}
