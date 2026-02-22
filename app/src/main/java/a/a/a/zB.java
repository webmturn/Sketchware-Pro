package a.a.a;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class zB {
  public static Bitmap a(InputStream paramInputStream) throws Exception {
    Field field;
    Bitmap bitmap = BitmapFactory.decodeStream(paramInputStream);
    byte[] arrayOfByte = a(bitmap);
    if (NinePatch.isNinePatchChunk(arrayOfByte)) {
      Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 1, 1, bitmap.getWidth() - 2, bitmap.getHeight() - 2);
      bitmap.recycle();
      field = bitmap1.getClass().getDeclaredField("mNinePatchChunk");
      field.setAccessible(true);
      field.set(bitmap1, arrayOfByte);
      return bitmap1;
    } 
    return bitmap;
  }
  
  public static Bitmap a(String paramString) throws Exception {
    FileInputStream fileInputStream = new FileInputStream(paramString);
    Bitmap bitmap = a(fileInputStream);
    fileInputStream.close();
    return bitmap;
  }
  
  public static void a(Bitmap paramBitmap, byte[] paramArrayOfbyte) {
    int[] arrayOfInt = new int[paramBitmap.getWidth() - 2];
    paramBitmap.getPixels(arrayOfInt, 0, arrayOfInt.length, 1, paramBitmap.getHeight() - 1, arrayOfInt.length, 1);
    boolean bool = false;
    int i;
    for (i = 0; i < arrayOfInt.length; i++) {
      if (-16777216 == arrayOfInt[i]) {
        a(paramArrayOfbyte, 12, i);
        break;
      } 
    } 
    for (i = arrayOfInt.length - 1; i >= 0; i--) {
      if (-16777216 == arrayOfInt[i]) {
        a(paramArrayOfbyte, 16, arrayOfInt.length - i - 2);
        break;
      } 
    } 
    arrayOfInt = new int[paramBitmap.getHeight() - 2];
    paramBitmap.getPixels(arrayOfInt, 0, 1, paramBitmap.getWidth() - 1, 0, 1, arrayOfInt.length);
    for (i = 0; i < arrayOfInt.length; i++) {
      if (-16777216 == arrayOfInt[i]) {
        a(paramArrayOfbyte, 20, i);
        break;
      } 
    } 
    for (i = arrayOfInt.length - 1; i >= 0; i--) {
      if (-16777216 == arrayOfInt[i]) {
        a(paramArrayOfbyte, 24, arrayOfInt.length - i - 2);
        break;
      } 
    } 
  }
  
  public static void a(OutputStream paramOutputStream, int paramInt) throws java.io.IOException {
    paramOutputStream.write(paramInt >> 0 & 0xFF);
    paramOutputStream.write(paramInt >> 8 & 0xFF);
    paramOutputStream.write(paramInt >> 16 & 0xFF);
    paramOutputStream.write(paramInt >> 24 & 0xFF);
  }
  
  public static void a(byte[] paramArrayOfbyte, int paramInt1, int paramInt2) {
    paramArrayOfbyte[paramInt1 + 0] = (byte)(paramInt2 >> 0);
    paramArrayOfbyte[paramInt1 + 1] = (byte)(paramInt2 >> 8);
    paramArrayOfbyte[paramInt1 + 2] = (byte)(paramInt2 >> 16);
    paramArrayOfbyte[paramInt1 + 3] = (byte)(paramInt2 >> 24);
  }
  
  public static byte[] a(Bitmap paramBitmap) throws java.io.IOException {
    int n;
    int m;
    int i = paramBitmap.getWidth();
    int j = paramBitmap.getHeight();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int k;
    for (k = 0; k < 32; k++)
      byteArrayOutputStream.write(0); 
    k = i - 2;
    int[] arrayOfInt = new int[k];
    paramBitmap.getPixels(arrayOfInt, 0, i, 1, 0, k, 1);
    if (arrayOfInt[0] == -16777216) {
      m = 1;
    } else {
      m = 0;
    } 
    if (arrayOfInt[arrayOfInt.length - 1] == -16777216) {
      n = 1;
    } else {
      n = 0;
    } 
    int i1 = arrayOfInt.length;
    i = 0;
    int i2 = 0;
    int i7 = 0;
    for (k = 0; i < i1; k = i7) {
      int i6 = i2;
      i7 = k;
      if (i2 != arrayOfInt[i]) {
        i7 = k + 1;
        a(byteArrayOutputStream, i);
        i6 = arrayOfInt[i];
      } 
      i++;
      i2 = i6;
    } 
    i = k;
    if (n != 0) {
      i = k + 1;
      a(byteArrayOutputStream, arrayOfInt.length);
    } 
    int i4 = i + 1;
    k = i4;
    if (m != 0)
      k = i4 - 1; 
    int m1 = k;
    if (n != 0)
      m1 = k - 1; 
    k = j - 2;
    arrayOfInt = new int[k];
    paramBitmap.getPixels(arrayOfInt, 0, 1, 0, 1, 1, k);
    if (arrayOfInt[0] == -16777216) {
      i4 = 1;
    } else {
      i4 = 0;
    } 
    if (arrayOfInt[arrayOfInt.length - 1] == -16777216) {
      n = 1;
    } else {
      n = 0;
    } 
    int i5 = arrayOfInt.length;
    int i3 = 0;
    j = 0;
    for (k = 0; i3 < i5; k = i2) {
      i1 = j;
      i2 = k;
      if (j != arrayOfInt[i3]) {
        i2 = k + 1;
        a(byteArrayOutputStream, i3);
        i1 = arrayOfInt[i3];
      } 
      i3++;
      j = i1;
    } 
    i3 = k;
    if (n != 0) {
      i3 = k + 1;
      a(byteArrayOutputStream, arrayOfInt.length);
    } 
    i2 = i3 + 1;
    k = i2;
    if (i4 != 0)
      k = i2 - 1; 
    i4 = k;
    if (n != 0)
      i4 = k - 1; 
    k = 0;
    while (true) {
      int i6 = m1 * i4;
      if (k < i6) {
        a(byteArrayOutputStream, 1);
        k++;
        continue;
      } 
      byte[] arrayOfByte = byteArrayOutputStream.toByteArray();
      arrayOfByte[0] = 1;
      arrayOfByte[1] = (byte)i;
      arrayOfByte[2] = (byte)i3;
      arrayOfByte[3] = (byte)n;
      a(paramBitmap, arrayOfByte);
      return arrayOfByte;
    } 
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\zB.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */