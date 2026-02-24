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
  
  public EncryptedFileUtil(boolean paramBoolean) {
    this.encryptionEnabled = paramBoolean;
  }
  
  public long getAssetFileSize(Context paramContext, String paramString) {
    long l;
    InputStream inputStream1 = null;
    InputStream inputStream2 = null;
    try {
      InputStream inputStream = paramContext.getAssets().open(paramString);
      inputStream2 = inputStream;
      inputStream1 = inputStream;
      int i = inputStream.available();
      long l1 = i;
      l = l1;
      if (inputStream != null)
        try {
          inputStream.close();
          l = l1;
        } catch (IOException iOException) {
          l = l1;
        }  
    } catch (IOException iOException) {
      inputStream2 = inputStream1;
      iOException.printStackTrace();
      if (inputStream1 != null)
        try {
          inputStream1.close();
        } catch (IOException iOException1) {} 
      l = -1L;
    } finally {}
    return l;
  }
  
  public String decryptToString(byte[] paramArrayOfbyte) throws Exception {
    return new String(decrypt(paramArrayOfbyte), "UTF-8");
  }
  
  public void copyAssetFile(Context paramContext, String paramString1, String paramString2) {
    android.content.res.AssetManager assets = paramContext.getAssets();
    int sepIdx = paramString2.lastIndexOf(File.separator);
    if (sepIdx > 0) {
      mkdirs(paramString2.substring(0, sepIdx));
    }
    java.io.InputStream is = null;
    java.io.FileOutputStream fos = null;
    try {
      is = assets.open(paramString1);
      fos = new java.io.FileOutputStream(paramString2, false);
      byte[] buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
        fos.write(buf, 0, len);
      }
      if (this.encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "assetFile =>" + paramString2 + " copy success.");
      }
    } catch (IOException e) {
    } finally {
      if (is != null) try { is.close(); } catch (IOException e) { e.printStackTrace(); }
      if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
    }
  }
  
  public void deleteDirectory(File paramFile) {
    deleteRecursive(paramFile, true);
  }
  
  public void copyDirectory(File paramFile1, File paramFile2) throws IOException {
    StringBuilder stringBuilder;
    if (paramFile1.isDirectory()) {
      if (paramFile2.exists() || paramFile2.mkdirs()) {
        String[] arrayOfString = paramFile1.list();
        if (arrayOfString != null)
          for (int b = 0; b < arrayOfString.length; b++)
            copyDirectory(new File(paramFile1, arrayOfString[b]), new File(paramFile2, arrayOfString[b]));  
        return;
      } 
      stringBuilder = new StringBuilder();
      stringBuilder.append("Cannot create dir ");
      stringBuilder.append(paramFile2.getAbsolutePath());
      throw new IOException(stringBuilder.toString());
    } 
    copyFile(paramFile1.getAbsolutePath(), paramFile2.getAbsolutePath());
  }
  
  public void deleteRecursive(File paramFile, boolean paramBoolean) {
    if (paramFile.exists()) {
      File[] arrayOfFile = paramFile.listFiles();
      if (arrayOfFile != null) {
        int i = arrayOfFile.length;
        for (int b = 0; b < i; b++) {
          File file = arrayOfFile[b];
          if (file.isDirectory())
            deleteDirectory(file); 
          if (file.isFile())
            if (file.delete()) {
              if (this.encryptionEnabled) {
                String str = EncryptedFileUtil.class.getSimpleName();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Delete file success.");
                stringBuilder.append(file.getAbsolutePath());
                Log.d(str, stringBuilder.toString());
              } 
            } else if (this.encryptionEnabled) {
              String str = EncryptedFileUtil.class.getSimpleName();
              StringBuilder stringBuilder = new StringBuilder();
              stringBuilder.append("Delete file failed.");
              stringBuilder.append(file.getAbsolutePath());
              Log.d(str, stringBuilder.toString());
            }  
        } 
      } 
      if (paramBoolean)
        paramFile.delete(); 
    } 
  }
  
  public void recreateDirectory(String paramString) {
    deleteDirectoryByPath(paramString);
    mkdirs(paramString);
  }
  
  public void copyFile(String paramString1, String paramString2) throws IOException {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try {
      fis = new FileInputStream(paramString1);
      fos = new FileOutputStream(paramString2, false);
      byte[] buf = new byte[1024];
      if (this.encryptionEnabled) {
        Log.d(getClass().getSimpleName(), "src=" + paramString1 + ",dest=" + paramString2);
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
  
  public void deleteRecursiveByPath(String paramString, boolean paramBoolean) {
    deleteRecursive(new File(paramString), paramBoolean);
  }
  
  public void writeBytes(String paramString, byte[] paramArrayOfbyte) {
    int i = paramString.lastIndexOf(File.separator);
    if (i > 0)
      mkdirs(paramString.substring(0, i)); 
    File file = new File(paramString);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } 
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      fos.write(paramArrayOfbyte);
      fos.flush();
    } catch (IOException e) {
    } finally {
      if (fos != null) try { fos.close(); } catch (IOException e) {}
    }
  }
  
  public String readAssetFile(Context paramContext, String paramString) {
    StringBuilder sb = new StringBuilder();
    java.io.BufferedReader reader = null;
    try {
      java.io.InputStream is = paramContext.getAssets().open(paramString.trim());
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
  
  public void deleteFile(File paramFile) {
    paramFile.delete();
  }
  
  public void deleteDirectoryByPath(String paramString) {
    deleteRecursiveByPath(paramString, true);
  }
  
  public void writeText(String paramString1, String paramString2) {
    int i = paramString1.lastIndexOf(File.separator);
    if (i > 0)
      mkdirs(paramString1.substring(0, i)); 
    File file = new File(paramString1);
    if (!file.exists())
      try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } 
    FileWriter fw = null;
    try {
      fw = new FileWriter(file, false);
      fw.write(paramString2);
      fw.flush();
    } catch (IOException e) {
    } finally {
      if (fw != null) try { fw.close(); } catch (IOException e) {}
    }
  }
  
  public byte[] decrypt(byte[] paramArrayOfbyte) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    byte[] arrayOfByte = "sketchwaresecure".getBytes();
    cipher.init(2, new SecretKeySpec(arrayOfByte, "AES"), new IvParameterSpec(arrayOfByte));
    return cipher.doFinal(paramArrayOfbyte);
  }
  
  public String readFileContent(File paramFile) {
    StringBuilder sb = new StringBuilder();
    java.io.FileReader reader = null;
    try {
      reader = new java.io.FileReader(paramFile);
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
  
  public void deleteFileByPath(String paramString) {
    deleteFile(new File(paramString));
  }
  
  public byte[] encrypt(byte[] paramArrayOfbyte) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    byte[] arrayOfByte = "sketchwaresecure".getBytes();
    cipher.init(1, new SecretKeySpec(arrayOfByte, "AES"), new IvParameterSpec(arrayOfByte));
    return cipher.doFinal(paramArrayOfbyte);
  }
  
  public byte[] encryptString(String paramString) throws Exception {
    return encrypt(paramString.getBytes("UTF-8"));
  }
  
  public boolean exists(String paramString) {
    return (new File(paramString)).exists();
  }
  
  public boolean mkdirs(String paramString) {
    return !exists(paramString) ? (new File(paramString)).mkdirs() : false;
  }
  
  public String readFile(String paramString) {
    return readFileContent(new File(paramString));
  }
  
  public byte[] readFileBytes(String paramString) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(new File(paramString));
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
