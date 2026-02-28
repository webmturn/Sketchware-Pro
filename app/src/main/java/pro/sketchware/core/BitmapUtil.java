package pro.sketchware.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtil {
  public static int calculateSampleSize(BitmapFactory.Options options, int x, int y) {
    int outWidth = options.outWidth;
    int outHeight = options.outHeight;
    int sampleSize = 1;
    int candidate = 1;
    if (outHeight > y || outWidth > x) {
      outHeight /= 2;
      outWidth /= 2;
      while (true) {
        sampleSize = candidate;
        if (outHeight / candidate >= y) {
          sampleSize = candidate;
          if (outWidth / candidate >= x) {
            candidate *= 2;
            continue;
          } 
        } 
        break;
      } 
    } 
    return sampleSize;
  }
  
  public static int getExifRotation(String filePath) throws IOException {
    int orientation = (new ExifInterface(filePath)).getAttributeInt("Orientation", -1);
    if (orientation != 3) {
      if (orientation != 6) {
        if (orientation != 8) {
          orientation = 0;
        } else {
          orientation = 270;
        } 
      } else {
        orientation = 90;
      } 
    } else {
      orientation = 180;
    } 
    return orientation;
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
  
  public static Bitmap decodeWithSampleSize(String filePath, int index) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = index;
    return BitmapFactory.decodeFile(filePath, options);
  }
  
  public static Bitmap decodeSampledBitmap(String filePath, int x, int y) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(filePath, options);
    options.inSampleSize = calculateSampleSize(options, x, y);
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(filePath, options);
  }
  
  public static void processAndSaveBitmap(String key, String value, int x, int y, int width) {
    Bitmap bitmap = decodeSampledBitmap(key, 512, 512);
    int rotation = 0;
    try {
      rotation = getExifRotation(key);
    } catch (IOException e) {
      Log.w("BitmapUtil", "Failed to get EXIF rotation: " + key, e);
    }
    Bitmap rotated = bitmap;
    if (rotation > 0) {
      rotated = rotateBitmap(bitmap, rotation);
    }
    Bitmap result = scaleAndRotateBitmap(rotated, x, y, width);
    try (FileOutputStream fos = new FileOutputStream(new File(value))) {
      result.compress(Bitmap.CompressFormat.PNG, 100, fos);
      fos.flush();
      result.recycle();
    } catch (Exception e) {
      Log.w("BitmapUtil", "Failed to save bitmap: " + value, e);
    }
  }
}
