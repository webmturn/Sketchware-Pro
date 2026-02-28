package pro.sketchware.core;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import mod.hilal.saif.activities.tools.ConfigActivity;

public class EncryptedFileUtil {
  public boolean encryptionEnabled = false;
  
  public EncryptedFileUtil() {
    this(false);
  }
  
  public EncryptedFileUtil(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
  }
  
  public long getAssetFileSize(Context context, String value) {
    try (InputStream inputStream = context.getAssets().open(value)) {
      return inputStream.available();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to get asset file size: " + value, e);
      return -1L;
    }
  }
  
  public String decryptToString(byte[] data) throws Exception {
    if (data == null) return "";
    // Auto-detect: try decryption first, fall back to plaintext
    try {
      return new String(decrypt(data), StandardCharsets.UTF_8);
    } catch (Exception e) {
      // Decryption failed — data is likely plaintext
      return new String(data, StandardCharsets.UTF_8);
    }
  }
  
  public void copyAssetFile(Context context, String key, String value) {
    int sepIdx = value.lastIndexOf(File.separator);
    if (sepIdx > 0) {
      mkdirs(value.substring(0, sepIdx));
    }
    try (InputStream is = context.getAssets().open(key);
         FileOutputStream fos = new FileOutputStream(value, false)) {
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
      if (encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "assetFile =>" + value + " copy success.");
      }
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to copy asset file: " + key, e);
    }
  }
  
  public void deleteDirectory(File file) {
    deleteRecursive(file, true);
  }
  
  public void copyDirectory(File srcFile, File destFile) throws IOException {
    if (srcFile.isDirectory()) {
      if (destFile.exists() || destFile.mkdirs()) {
        String[] parts = srcFile.list();
        if (parts != null)
          for (String part : parts)
            copyDirectory(new File(srcFile, part), new File(destFile, part));
        return;
      } 
      throw new IOException("Cannot create dir " + destFile.getAbsolutePath());
    } 
    if (!srcFile.exists()) {
      Log.w("EncryptedFileUtil", "copyDirectory: source file does not exist: " + srcFile.getAbsolutePath());
      return;
    }
    copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
  }
  
  public void deleteRecursive(File dir, boolean deleteRoot) {
    if (dir.exists()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory())
            deleteDirectory(file); 
          if (file.isFile()) {
            if (file.delete()) {
              if (encryptionEnabled)
                Log.d("EncryptedFileUtil", "Delete file success." + file.getAbsolutePath());
            } else if (encryptionEnabled) {
              Log.d("EncryptedFileUtil", "Delete file failed." + file.getAbsolutePath());
            }
          }
        } 
      } 
      if (deleteRoot)
        dir.delete(); 
    } 
  }
  
  public void recreateDirectory(String value) {
    deleteDirectoryByPath(value);
    mkdirs(value);
  }
  
  public void copyFile(String key, String value) throws IOException {
    try (FileInputStream fis = new FileInputStream(key);
         FileOutputStream fos = new FileOutputStream(value, false)) {
      byte[] buf = new byte[1024];
      if (encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "src=" + key + ",dest=" + value);
      }
      int len;
      while ((len = fis.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
    }
  }
  
  public void deleteRecursiveByPath(String value, boolean flag) {
    deleteRecursive(new File(value), flag);
  }
  
  public void writeBytes(String value, byte[] data) {
    int separatorIdx = value.lastIndexOf(File.separator);
    if (separatorIdx > 0)
      mkdirs(value.substring(0, separatorIdx)); 
    File file = new File(value);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { Log.w("EncryptedFileUtil", "Failed to create file", e); } 
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(data);
      fos.flush();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to write bytes", e);
    }
  }
  
  public String readAssetFile(Context context, String value) {
    StringBuilder sb = new StringBuilder();
    try (InputStream is = context.getAssets().open(value.trim());
         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append("\r\n");
      }
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to read asset file: " + value, e);
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
    int separatorIdx = key.lastIndexOf(File.separator);
    if (separatorIdx > 0)
      mkdirs(key.substring(0, separatorIdx)); 
    File file = new File(key);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { Log.w("EncryptedFileUtil", "Failed to create file", e); } 
    try (FileWriter fw = new FileWriter(file, false)) {
      fw.write(value);
      fw.flush();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to write text", e);
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
    try (FileReader reader = new FileReader(file)) {
      char[] buf = new char[1024];
      int len;
      while ((len = reader.read(buf)) > 0) {
        sb.append(buf, 0, len);
      }
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to read file content", e);
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
    if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PROJECT_DATA_ENCRYPTION)) {
      // Encryption disabled — write as plaintext UTF-8 bytes
      return value.getBytes(StandardCharsets.UTF_8);
    }
    return encrypt(value.getBytes("UTF-8"));
  }
  
  public boolean exists(String value) {
    return new File(value).exists();
  }
  
  public boolean mkdirs(String value) {
    return !exists(value) && new File(value).mkdirs();
  }
  
  public String readFile(String value) {
    return readFileContent(new File(value));
  }
  
  public byte[] readFileBytes(String value) {
    try (FileInputStream fis = new FileInputStream(value)) {
      int size = fis.available();
      if (size > 0) {
        byte[] data = new byte[size];
        fis.read(data);
        return data;
      }
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to read file bytes: " + value, e);
    }
    return null;
  }
}
