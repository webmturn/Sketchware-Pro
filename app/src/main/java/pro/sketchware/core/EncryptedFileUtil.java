package pro.sketchware.core;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import mod.hilal.saif.activities.tools.ConfigActivity;

/**
 * Low-level file I/O utility used throughout Sketchware Pro for reading and writing
 * project data files. Supports optional AES-128-CBC encryption using a fixed key
 * ({@code "sketchwaresecure"}).
 * <p>
 * Key features:
 * <ul>
 *   <li>Atomic writes via tmp-file + rename pattern ({@link #writeText}, {@link #writeBytes})</li>
 *   <li>Auto-detect encrypted vs plaintext on read ({@link #decryptToString})</li>
 *   <li>Recursive directory operations ({@link #deleteDirectory}, {@link #copyDirectory})</li>
 *   <li>Asset file extraction ({@link #copyAssetFile})</li>
 * </ul>
 *
 * @see ProjectDataStore
 * @see ConfigActivity#SETTING_PROJECT_DATA_ENCRYPTION
 */
public class EncryptedFileUtil {
  public boolean encryptionEnabled = false;
  
  public EncryptedFileUtil() {
    this(false);
  }
  
  public EncryptedFileUtil(boolean encryptionEnabled) {
    this.encryptionEnabled = encryptionEnabled;
  }
  
  /**
   * Returns the size of an asset file in bytes.
   *
   * @param context the Android context
   * @param value   the asset path relative to {@code assets/}
   * @return the file size in bytes, or {@code -1} on error
   */
  public long getAssetFileSize(Context context, String value) {
    if (context == null || value == null || value.isEmpty()) return -1L;
    try (InputStream inputStream = context.getAssets().open(value)) {
      return inputStream.available();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to get asset file size: " + value, e);
      return -1L;
    }
  }
  
  /**
   * Decrypts the given byte array to a UTF-8 string.
   * Auto-detects plaintext: if decryption fails, returns the data as-is.
   *
   * @param data the encrypted (or plaintext) byte array
   * @return the decoded string, or empty string if data is null
   * @throws Exception if both decryption and plaintext fallback fail
   */
  public String decryptToString(byte[] data) {
    if (data == null) return "";
    // Auto-detect: try decryption first, fall back to plaintext
    try {
      return new String(decrypt(data), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      // Decryption failed — data is likely plaintext
      return new String(data, StandardCharsets.UTF_8);
    }
  }
  
  /**
   * Copies an asset file to local storage, creating parent directories as needed.
   *
   * @param context the Android context
   * @param key     the asset path relative to {@code assets/}
   * @param value   the destination file path on local storage
   */
  public void copyAssetFile(Context context, String key, String value) {
    if (key == null || key.isEmpty() || value == null || value.isEmpty()) return;
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
    if (value == null || value.isEmpty()) return;
    deleteDirectoryByPath(value);
    mkdirs(value);
  }
  
  public void copyFile(String key, String value) throws IOException {
    if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
      throw new IOException("copyFile: null or empty path (src=" + key + ", dst=" + value + ")");
    }
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
    if (value == null || value.isEmpty()) return;
    deleteRecursive(new File(value), flag);
  }
  
  /**
   * Atomically writes raw bytes to a file using a tmp-file + rename strategy.
   * Creates parent directories if they don't exist.
   *
   * @param value the destination file path
   * @param data  the bytes to write
   * @return {@code true} if the write succeeded
   */
  public boolean writeBytes(String value, byte[] data) {
    if (value == null || value.isEmpty() || data == null) return false;
    int separatorIdx = value.lastIndexOf(File.separator);
    if (separatorIdx > 0)
      mkdirs(value.substring(0, separatorIdx)); 
    File file = new File(value);
    File tmpFile = new File(value + ".tmp");
    try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
      fos.write(data);
      fos.getFD().sync();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to write bytes", e);
      tmpFile.delete();
      return false;
    }
    if (!tmpFile.renameTo(file)) {
      // renameTo can fail across filesystems; fall back to delete+rename
      file.delete();
      if (!tmpFile.renameTo(file)) {
        Log.e("EncryptedFileUtil", "Atomic rename failed for: " + value);
        tmpFile.delete();
        return false;
      }
    }
    return true;
  }
  
  public String readAssetFile(Context context, String value) {
    if (value == null || value.isEmpty()) return "";
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
    if (value == null || value.isEmpty()) return;
    deleteRecursiveByPath(value, true);
  }
  
  /**
   * Atomically writes a UTF-8 text string to a file using a tmp-file + rename strategy.
   * Creates parent directories if they don't exist.
   *
   * @param key   the destination file path
   * @param value the text content to write
   * @return {@code true} if the write succeeded
   */
  public boolean writeText(String key, String value) {
    if (key == null || key.isEmpty()) return false;
    int separatorIdx = key.lastIndexOf(File.separator);
    if (separatorIdx > 0)
      mkdirs(key.substring(0, separatorIdx)); 
    File file = new File(key);
    File tmpFile = new File(key + ".tmp");
    try (FileOutputStream fos = new FileOutputStream(tmpFile);
         Writer fw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
      fw.write(value);
      fw.flush();
      fos.getFD().sync();
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to write text", e);
      tmpFile.delete();
      return false;
    }
    if (!tmpFile.renameTo(file)) {
      file.delete();
      if (!tmpFile.renameTo(file)) {
        Log.e("EncryptedFileUtil", "Atomic rename failed for: " + key);
        tmpFile.delete();
        return false;
      }
    }
    return true;
  }
  
  /**
   * Decrypts data using AES-128-CBC with the fixed Sketchware key.
   *
   * @param data the ciphertext bytes
   * @return the decrypted plaintext bytes
   * @throws Exception if decryption fails
   */
  public byte[] decrypt(byte[] data) throws GeneralSecurityException {
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
    if (value == null || value.isEmpty()) return;
    deleteFile(new File(value));
  }
  
  /**
   * Encrypts data using AES-128-CBC with the fixed Sketchware key.
   *
   * @param data the plaintext bytes
   * @return the ciphertext bytes
   * @throws Exception if encryption fails
   */
  public byte[] encrypt(byte[] data) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    byte[] bytes = "sketchwaresecure".getBytes();
    cipher.init(1, new SecretKeySpec(bytes, "AES"), new IvParameterSpec(bytes));
    return cipher.doFinal(data);
  }
  
  /**
   * Encrypts a string for storage. Returns plaintext UTF-8 bytes if
   * encryption is disabled in {@link ConfigActivity} settings.
   *
   * @param value the string to encrypt
   * @return encrypted bytes, or plaintext UTF-8 bytes if encryption is disabled
   * @throws Exception if encryption fails
   */
  public byte[] encryptString(String value) throws GeneralSecurityException {
    if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PROJECT_DATA_ENCRYPTION)) {
      // Encryption disabled — write as plaintext UTF-8 bytes
      return value.getBytes(StandardCharsets.UTF_8);
    }
    return encrypt(value.getBytes(StandardCharsets.UTF_8));
  }
  
  public boolean exists(String value) {
    return value != null && !value.isEmpty() && new File(value).exists();
  }
  
  public boolean mkdirs(String value) {
    return value != null && !value.isEmpty() && !exists(value) && new File(value).mkdirs();
  }
  
  public String readFile(String value) {
    if (value == null || value.isEmpty()) return "";
    return readFileContent(new File(value));
  }
  
  /**
   * Reads the entire contents of a file as a byte array.
   *
   * @param value the file path to read
   * @return the file contents, or {@code null} if the file is empty or an error occurs
   */
  public byte[] readFileBytes(String value) {
    if (value == null || value.isEmpty()) return null;
    try (FileInputStream fis = new FileInputStream(value);
         ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[4096];
      int len;
      while ((len = fis.read(buf)) != -1) {
        bos.write(buf, 0, len);
      }
      byte[] data = bos.toByteArray();
      return data.length > 0 ? data : null;
    } catch (IOException e) {
      Log.w("EncryptedFileUtil", "Failed to read file bytes: " + value, e);
    }
    return null;
  }
}
