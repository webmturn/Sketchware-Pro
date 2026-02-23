package pro.sketchware.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class UriPathResolver {
  public static String resolve(Context paramContext, Uri paramUri) {
    String result = null;
    if (android.provider.DocumentsContract.isDocumentUri(paramContext, paramUri)) {
      if (isExternalStorageDocument(paramUri)) {
        String[] split = android.provider.DocumentsContract.getDocumentId(paramUri).split(":");
        if ("primary".equalsIgnoreCase(split[0])) {
          result = android.os.Environment.getExternalStorageDirectory() + "/" + split[1];
        }
      } else if (isDownloadsDocument(paramUri)) {
        String docId = android.provider.DocumentsContract.getDocumentId(paramUri);
        if (!android.text.TextUtils.isEmpty(docId) && docId.startsWith("raw:")) {
          return docId.replaceFirst("raw:", "");
        }
        result = queryDataColumn(paramContext, android.content.ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue()), null, null);
      } else if (isMediaDocument(paramUri)) {
        String[] split = android.provider.DocumentsContract.getDocumentId(paramUri).split(":");
        String type = split[0];
        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        result = queryDataColumn(paramContext, contentUri, "_id=?", new String[]{split[1]});
      }
    } else if ("content".equalsIgnoreCase(paramUri.getScheme())) {
      result = queryDataColumn(paramContext, paramUri, null, null);
    } else if ("file".equalsIgnoreCase(paramUri.getScheme())) {
      result = paramUri.getPath();
    }
    if (result != null) {
      try {
        result = java.net.URLDecoder.decode(result, "UTF-8");
      } catch (Exception e) {
        return null;
      }
      if (new File(result).canRead()) {
        return result;
      }
    }
    return copyUriToCache(paramContext, paramUri);
  }
  
  public static String copyUriToCache(Context context, Uri uri) {
    try (InputStream is = context.getContentResolver().openInputStream(uri)) {
      if (is == null) return null;
      String fileName = "uri_" + uri.getLastPathSegment();
      if (fileName.length() > 80) fileName = fileName.substring(fileName.length() - 80);
      fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
      File cacheFile = new File(context.getCacheDir(), fileName);
      try (OutputStream os = new FileOutputStream(cacheFile)) {
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) {
          os.write(buf, 0, len);
        }
      }
      return cacheFile.getAbsolutePath();
    } catch (Exception e) {
      Log.e("UriPathResolver", "copyUriToCache failed: " + uri, e);
      return null;
    }
  }
  
  public static String queryDataColumn(Context paramContext, Uri paramUri, String paramString, String[] paramArrayOfString) {
    android.database.Cursor cursor = null;
    try {
      cursor = paramContext.getContentResolver().query(paramUri, new String[]{"_data"}, paramString, paramArrayOfString, null);
      if (cursor != null && cursor.moveToFirst()) {
        String result = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
        return result;
      }
    } catch (Exception e) {
      Log.e("ERROR", e.getMessage(), e);
    } finally {
      if (cursor != null) cursor.close();
    }
    return null;
  }
  
  public static boolean isDownloadsDocument(Uri paramUri) {
    return "com.android.providers.downloads.documents".equals(paramUri.getAuthority());
  }
  
  public static boolean isExternalStorageDocument(Uri paramUri) {
    return "com.android.externalstorage.documents".equals(paramUri.getAuthority());
  }
  
  public static boolean isMediaDocument(Uri paramUri) {
    return "com.android.providers.media.documents".equals(paramUri.getAuthority());
  }
}
