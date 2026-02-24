package pro.sketchware.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class UriPathResolver {
  public static String resolve(Context context, Uri uri) {
    String result = null;
    if (android.provider.DocumentsContract.isDocumentUri(context, uri)) {
      if (isExternalStorageDocument(uri)) {
        String[] split = android.provider.DocumentsContract.getDocumentId(uri).split(":");
        if ("primary".equalsIgnoreCase(split[0])) {
          result = android.os.Environment.getExternalStorageDirectory() + "/" + split[1];
        }
      } else if (isDownloadsDocument(uri)) {
        String docId = android.provider.DocumentsContract.getDocumentId(uri);
        if (!android.text.TextUtils.isEmpty(docId) && docId.startsWith("raw:")) {
          return docId.replaceFirst("raw:", "");
        }
        result = queryDataColumn(context, android.content.ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue()), null, null);
      } else if (isMediaDocument(uri)) {
        String[] split = android.provider.DocumentsContract.getDocumentId(uri).split(":");
        String type = split[0];
        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        result = queryDataColumn(context, contentUri, "_id=?", new String[]{split[1]});
      }
    } else if ("content".equalsIgnoreCase(uri.getScheme())) {
      result = queryDataColumn(context, uri, null, null);
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      result = uri.getPath();
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
    return copyUriToCache(context, uri);
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
  
  public static String queryDataColumn(Context context, Uri uri, String input, String[] strings) {
    android.database.Cursor cursor = null;
    try {
      cursor = context.getContentResolver().query(uri, new String[]{"_data"}, input, strings, null);
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
  
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }
  
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }
  
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }
}
