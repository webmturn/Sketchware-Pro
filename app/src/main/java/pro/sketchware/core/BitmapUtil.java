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
  public static int calculateSampleSize(BitmapFactory.Options options, int targetWidth, int targetHeight) {
    int outWidth = options.outWidth;
    int outHeight = options.outHeight;
    int sampleSize = 1;
    int candidate = 1;
    if (outHeight > targetHeight || outWidth > targetWidth) {
      outHeight /= 2;
      outWidth /= 2;
      while (true) {
        sampleSize = candidate;
        if (outHeight / candidate >= targetHeight) {
          sampleSize = candidate;
          if (outWidth / candidate >= targetWidth) {
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
  
  public static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
    Matrix matrix = new Matrix();
    matrix.postRotate(rotationDegrees);
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap scaleAndRotateBitmap(Bitmap bitmap, int rotationDegrees, int scaleX, int scaleY) {
    Matrix matrix = new Matrix();
    matrix.setScale(scaleX, scaleY);
    matrix.postRotate(rotationDegrees);
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
  }
  
  public static Bitmap decodeWithSampleSize(String filePath, int sampleSize) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = sampleSize;
    return BitmapFactory.decodeFile(filePath, options);
  }
  
  public static Bitmap decodeSampledBitmap(String filePath, int targetWidth, int targetHeight) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(filePath, options);
    options.inSampleSize = calculateSampleSize(options, targetWidth, targetHeight);
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile(filePath, options);
  }
  
  public static void processAndSaveBitmap(String sourcePath, String destinationPath, int rotationDegrees, int scaleX, int scaleY) {
    Bitmap bitmap = decodeSampledBitmap(sourcePath, 512, 512);
    if (bitmap == null) {
      Log.w("BitmapUtil", "Failed to decode bitmap: " + sourcePath);
      return;
    }
    int rotation = 0;
    try {
      rotation = getExifRotation(sourcePath);
    } catch (IOException e) {
      Log.w("BitmapUtil", "Failed to get EXIF rotation: " + sourcePath, e);
    }
    Bitmap rotated = bitmap;
    if (rotation > 0) {
      rotated = rotateBitmap(bitmap, rotation);
    }
    Bitmap result = scaleAndRotateBitmap(rotated, rotationDegrees, scaleX, scaleY);
    try (FileOutputStream fos = new FileOutputStream(new File(destinationPath))) {
      result.compress(Bitmap.CompressFormat.PNG, 100, fos);
      fos.flush();
      result.recycle();
    } catch (Exception e) {
      Log.w("BitmapUtil", "Failed to save bitmap: " + destinationPath, e);
    }
  }
}
