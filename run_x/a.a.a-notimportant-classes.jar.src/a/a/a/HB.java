package a.a.a;

import android.content.Context;
import android.net.Uri;

public final class HB {
  public static String a(Context paramContext, Uri paramUri) {
    String result = null;
    if (android.provider.DocumentsContract.isDocumentUri(paramContext, paramUri)) {
      if (b(paramUri)) {
        String[] split = android.provider.DocumentsContract.getDocumentId(paramUri).split(":");
        if ("primary".equalsIgnoreCase(split[0])) {
          result = android.os.Environment.getExternalStorageDirectory() + "/" + split[1];
        }
      } else if (a(paramUri)) {
        String docId = android.provider.DocumentsContract.getDocumentId(paramUri);
        if (!android.text.TextUtils.isEmpty(docId) && docId.startsWith("raw:")) {
          return docId.replaceFirst("raw:", "");
        }
        result = a(paramContext, android.content.ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId).longValue()), null, null);
      } else if (c(paramUri)) {
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
        result = a(paramContext, contentUri, "_id=?", new String[]{split[1]});
      }
    } else if ("content".equalsIgnoreCase(paramUri.getScheme())) {
      result = a(paramContext, paramUri, null, null);
    } else if ("file".equalsIgnoreCase(paramUri.getScheme())) {
      result = paramUri.getPath();
    }
    if (result != null) {
      try {
        result = java.net.URLDecoder.decode(result, "UTF-8");
      } catch (Exception e) {
        return null;
      }
      return result;
    }
    return null;
  }
  
  public static String a(Context paramContext, Uri paramUri, String paramString, String[] paramArrayOfString) {
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
  
  public static boolean a(Uri paramUri) {
    return "com.android.providers.downloads.documents".equals(paramUri.getAuthority());
  }
  
  public static boolean b(Uri paramUri) {
    return "com.android.externalstorage.documents".equals(paramUri.getAuthority());
  }
  
  public static boolean c(Uri paramUri) {
    return "com.android.providers.media.documents".equals(paramUri.getAuthority());
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\HB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */