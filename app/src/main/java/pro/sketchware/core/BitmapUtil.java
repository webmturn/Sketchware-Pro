package pro.sketchware.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class BitmapUtil {
  public static int a(BitmapFactory.Options paramOptions, int paramInt1, int paramInt2) {
    int i = paramOptions.outWidth;
    int j = paramOptions.outHeight;
    int k = 1;
    int m = 1;
    if (j > paramInt2 || i > paramInt1) {
      j /= 2;
      i /= 2;
      while (true) {
        k = m;
        if (j / m >= paramInt2) {
          k = m;
          if (i / m >= paramInt1) {
            m *= 2;
            continue;
          } 
        } 
        break;
      } 
    } 
    return k;
  }
  
  public static int a(String paramString) throws java.io.IOException {
    int i = (new ExifInterface(paramString)).getAttributeInt("Orientation", -1);
    if (i != 3) {
      if (i != 6) {
        if (i != 8) {
          i = 0;
        } else {
          i = 270;
        } 
      } else {
        i = 90;
      } 
    } else {
      i = 180;
    } 
    return i;
  }
  
  public static Bitmap a(Bitmap paramBitmap, int paramInt) {
    Matrix matrix = new Matrix();
    matrix.postRotate(paramInt);
    return Bitmap.createBitmap(paramBitmap, 0, 0, paramBitmap.getWidth(), paramBitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap a(Bitmap paramBitmap, int paramInt1, int paramInt2, int paramInt3) {
    Matrix matrix = new Matrix();
    matrix.setScale(paramInt2, paramInt3);
    matrix.postRotate(paramInt1);
    return Bitmap.createBitmap(paramBitmap, 0, 0, paramBitmap.getWidth(), paramBitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap a(String paramString, int paramInt) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = paramInt;
    return BitmapFactory.decodeFile(paramString, options);
  }
  
  public static Bitmap a(String paramString, int paramInt1, int paramInt2) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(paramString, options);
    options.inSampleSize = a(options, paramInt1, paramInt2);
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(paramString, options);
  }
  
  public static void a(String paramString1, String paramString2, int paramInt1, int paramInt2, int paramInt3) {
    Bitmap bitmap = a(paramString1, 512, 512);
    int rotation = 0;
    try {
      rotation = a(paramString1);
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    Bitmap rotated = bitmap;
    if (rotation > 0) {
      rotated = a(bitmap, rotation);
    }
    Bitmap result = a(rotated, paramInt1, paramInt2, paramInt3);
    java.io.FileOutputStream fos = null;
    try {
      fos = new java.io.FileOutputStream(new java.io.File(paramString2));
      result.compress(Bitmap.CompressFormat.PNG, 100, fos);
      fos.flush();
      result.recycle();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (fos != null) try { fos.close(); } catch (Exception ignored) {}
    }
  }
}
