package pro.sketchware.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class NinePatchDecoder {
  public static Bitmap decode(InputStream inputStream) throws Exception {
    Field field;
    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
    byte[] arrayOfByte = buildNinePatchChunk(bitmap);
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
  
  public static Bitmap decodeFile(String input) throws Exception {
    try (FileInputStream fileInputStream = new FileInputStream(input)) {
      return decode(fileInputStream);
    }
  }
  
  public static void extractPadding(Bitmap bitmap, byte[] data) {
    int[] intValues = new int[bitmap.getWidth() - 2];
    bitmap.getPixels(intValues, 0, intValues.length, 1, bitmap.getHeight() - 1, intValues.length, 1);
    boolean bool = false;
    int i;
    for (i = 0; i < intValues.length; i++) {
      if (-16777216 == intValues[i]) {
        putIntLE(data, 12, i);
        break;
      } 
    } 
    for (i = intValues.length - 1; i >= 0; i--) {
      if (-16777216 == intValues[i]) {
        putIntLE(data, 16, intValues.length - i - 2);
        break;
      } 
    } 
    intValues = new int[bitmap.getHeight() - 2];
    bitmap.getPixels(intValues, 0, 1, bitmap.getWidth() - 1, 0, 1, intValues.length);
    for (i = 0; i < intValues.length; i++) {
      if (-16777216 == intValues[i]) {
        putIntLE(data, 20, i);
        break;
      } 
    } 
    for (i = intValues.length - 1; i >= 0; i--) {
      if (-16777216 == intValues[i]) {
        putIntLE(data, 24, intValues.length - i - 2);
        break;
      } 
    } 
  }
  
  public static void writeIntLE(OutputStream outputStream, int value) throws java.io.IOException {
    outputStream.write(value >> 0 & 0xFF);
    outputStream.write(value >> 8 & 0xFF);
    outputStream.write(value >> 16 & 0xFF);
    outputStream.write(value >> 24 & 0xFF);
  }
  
  public static void putIntLE(byte[] data, int x, int y) {
    data[x + 0] = (byte)(y >> 0);
    data[x + 1] = (byte)(y >> 8);
    data[x + 2] = (byte)(y >> 16);
    data[x + 3] = (byte)(y >> 24);
  }
  
  public static byte[] buildNinePatchChunk(Bitmap bitmap) throws java.io.IOException {
    int n;
    int m;
    int i = bitmap.getWidth();
    int j = bitmap.getHeight();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int k;
    for (k = 0; k < 32; k++)
      byteArrayOutputStream.write(0); 
    k = i - 2;
    int[] intValues = new int[k];
    bitmap.getPixels(intValues, 0, i, 1, 0, k, 1);
    if (intValues[0] == -16777216) {
      m = 1;
    } else {
      m = 0;
    } 
    if (intValues[intValues.length - 1] == -16777216) {
      n = 1;
    } else {
      n = 0;
    } 
    int i1 = intValues.length;
    i = 0;
    int i2 = 0;
    int i7 = 0;
    for (k = 0; i < i1; k = i7) {
      int i6 = i2;
      i7 = k;
      if (i2 != intValues[i]) {
        i7 = k + 1;
        writeIntLE(byteArrayOutputStream, i);
        i1 = intValues[i];
      } 
      i++;
      i2 = i6;
    } 
    i = k;
    if (n != 0) {
      i = k + 1;
      writeIntLE(byteArrayOutputStream, intValues.length);
    } 
    int i4 = i + 1;
    k = i4;
    if (m != 0)
      k = i4 - 1; 
    int m1 = k;
    if (n != 0)
      m1 = k - 1; 
    k = j - 2;
    intValues = new int[k];
    bitmap.getPixels(intValues, 0, 1, 0, 1, 1, k);
    if (intValues[0] == -16777216) {
      i4 = 1;
    } else {
      i4 = 0;
    } 
    if (intValues[intValues.length - 1] == -16777216) {
      n = 1;
    } else {
      n = 0;
    } 
    int i5 = intValues.length;
    int i3 = 0;
    j = 0;
    for (k = 0; i3 < i5; k = i2) {
      i1 = j;
      i2 = k;
      if (j != intValues[i3]) {
        i2 = k + 1;
        writeIntLE(byteArrayOutputStream, i3);
        i1 = intValues[i3];
      } 
      i3++;
      j = i1;
    } 
    i3 = k;
    if (n != 0) {
      i3 = k + 1;
      writeIntLE(byteArrayOutputStream, intValues.length);
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
        writeIntLE(byteArrayOutputStream, 1);
        k++;
        continue;
      } 
      byte[] arrayOfByte = byteArrayOutputStream.toByteArray();
      arrayOfByte[0] = 1;
      arrayOfByte[1] = (byte)i;
      arrayOfByte[2] = (byte)i3;
      arrayOfByte[3] = (byte)n;
      extractPadding(bitmap, arrayOfByte);
      return arrayOfByte;
    } 
  }
}
