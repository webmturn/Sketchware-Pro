package pro.sketchware.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class BitmapUtil {
  public static int calculateSampleSize(BitmapFactory.Options options, int x, int y) {
    int i = options.outWidth;
    int j = options.outHeight;
    int k = 1;
    int m = 1;
    if (j > y || i > x) {
      j /= 2;
      i /= 2;
      while (true) {
        k = m;
        if (j / m >= y) {
          k = m;
          if (i / m >= x) {
            m *= 2;
            continue;
          } 
        } 
        break;
      } 
    } 
    return k;
  }
  
  public static int getExifRotation(String str) throws java.io.IOException {
    int i = (new ExifInterface(str)).getAttributeInt("Orientation", -1);
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
  
  public static Bitmap rotateBitmap(Bitmap bitmap, int index) {
    Matrix matrix = new Matrix();
    matrix.postRotate(index);
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap scaleAndRotateBitmap(Bitmap bitmap, int x, int y, int width) {
    Matrix matrix = new Matrix();
    matrix.setScale(y, width);
    matrix.postRotate(x);
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap decodeWithSampleSize(String str, int index) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = index;
    return BitmapFactory.decodeFile(str, options);
  }
  
  public static Bitmap decodeSampledBitmap(String str, int x, int y) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(str, options);
    options.inSampleSize = calculateSampleSize(options, x, y);
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(str, options);
  }
  
  public static void processAndSaveBitmap(String key, String value, int x, int y, int width) {
    Bitmap bitmap = decodeSampledBitmap(key, 512, 512);
    int rotation = 0;
    try {
      rotation = getExifRotation(key);
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    Bitmap rotated = bitmap;
    if (rotation > 0) {
      rotated = rotateBitmap(bitmap, rotation);
    }
    Bitmap result = scaleAndRotateBitmap(rotated, x, y, width);
    java.io.FileOutputStream fos = null;
    try {
      fos = new java.io.FileOutputStream(new java.io.File(value));
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
