package pro.sketchware.utility;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("unused")
public class FileUtil {
    public static long getFileSize(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }

        if (!file.isDirectory()) {
            return file.length();
        }

        List<File> dirs = new LinkedList<>();
        dirs.add(file);

        long result = 0;
        while (!dirs.isEmpty()) {
            File dir = dirs.remove(0);
            if (!dir.exists())
                continue;
            File[] listFiles = dir.listFiles();
            if (listFiles == null)
                continue;
            for (File child : listFiles) {
                if (child.isDirectory()) {
                    dirs.add(child);
                } else {
                    result += child.length();
                }
            }
        }

        return result;
    }

    public static String formatFileSize(long size) {
        return formatFileSize(size, false);
    }

    public static String formatFileSize(long size, boolean removeZero) {
        String[] units = {"B", "KiB", "MiB", "GiB"};
        float value = size;
        int unitIndex = 0;

        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        if (removeZero && (value - (int) value) * 10 == 0) {
            return String.format("%d %s", (int) value, units[unitIndex]);
        } else {
            return String.format("%.1f %s", value, units[unitIndex]);
        }
    }

    public static boolean renameFile(String oldPath, String newPath) {
        return new File(oldPath).renameTo(new File(newPath));
    }

    /**
     * @return A filename without its extension,
     * e.g. "FileUtil" for "FileUtil.java", or "FileUtil" for "/sdcard/Documents/FileUtil.java"
     */
    public static String getFileNameNoExtension(String filePath) {
        if (filePath.trim().isEmpty()) return "";

        int lastPos = filePath.lastIndexOf('.');
        int lastSep = filePath.lastIndexOf(File.separator);

        if (lastSep == -1) {
            return (lastPos == -1 ? filePath : filePath.substring(0, lastPos));
        } else if (lastPos == -1 || lastSep > lastPos) {
            return filePath.substring(lastSep + 1);
        }
        return filePath.substring(lastSep + 1, lastPos);
    }

    /**
     * @return A file's filename extension,
     * e.g. "java" for "/sdcard/Documents/FileUtil.java", but "" for "/sdcard/Documents/fileWithoutExtension"
     */
    public static String getFileExtension(String filePath) {
        if (filePath.isEmpty()) return "";

        int last = filePath.lastIndexOf('.');
        int lastSep = filePath.lastIndexOf(File.separator);

        if (last == -1 || lastSep >= last) return "";
        return filePath.substring(last + 1);
    }

    public static void createNewFileIfNotPresent(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }

        File file = new File(path);

        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }
    }

    public static String readFile(String path) {
        createNewFileIfNotPresent(path);

        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(path)) {
            char[] buff = new char[1024];
            int length;

            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }

        return sb.toString();
    }

    public static String readFileIfExist(String path) {
        if (!isExistFile(path)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (FileReader fr = new FileReader(path)) {
            char[] buff = new char[1024];
            int length;

            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }

        return sb.toString();
    }

    public static void writeFile(String path, String content) {
        createNewFileIfNotPresent(path);

        try (FileWriter fileWriter = new FileWriter(path, false)) {
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }
    }

    public static void copyFile(String sourcePath, String destPath) {
        if (!isExistFile(sourcePath)) return;
        createNewFileIfNotPresent(destPath);

        try (FileInputStream fis = new FileInputStream(sourcePath);
             FileOutputStream fos = new FileOutputStream(destPath, false)) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }
    }

    /**
     * Copies an entire directory, recursively.
     *
     * @param source   The directory whose contents to copy.
     * @param copyInto The directory to copy files into.
     * @throws IOException Thrown when something goes wrong while copying.
     */
    public static void copyDirectory(File source, File copyInto) throws IOException {
        if (!source.isDirectory()) {
            File parentFile = copyInto.getParentFile();
            if (parentFile == null || parentFile.exists() || parentFile.mkdirs()) {
                try (FileInputStream fileInputStream = new FileInputStream(source);
                     FileOutputStream fileOutputStream = new FileOutputStream(copyInto)) {
                    byte[] buffer = new byte[2048];
                    while (true) {
                        int read = fileInputStream.read(buffer);
                        if (read <= 0) {
                            return;
                        }
                        fileOutputStream.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    Log.e("FileUtil", "Error copying file " + source.getAbsolutePath() + " to " + copyInto.getAbsolutePath(), e);
                    throw e;
                }
            } else {
                throw new IOException("Cannot create dir " + parentFile.getAbsolutePath());
            }
        } else if (copyInto.exists() || copyInto.mkdirs()) {
            String[] list = source.list();
            if (list != null) {
                for (String s : list) {
                    copyDirectory(new File(source, s), new File(copyInto, s));
                }
            }
        } else {
            throw new IOException("Cannot create dir " + copyInto.getAbsolutePath());
        }
    }

    public static void extractFileFromZip(InputStream inputStream, File file) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            while (true) {
                int read = inputStream.read(buffer);
                if (read > 0) {
                    outputStream.write(buffer, 0, read);
                } else {
                    return;
                }
            }
        }
    }

    public static void moveFile(String sourcePath, String destPath) {
        copyFile(sourcePath, destPath);
        deleteFile(sourcePath);
    }

    public static void deleteFile(String path) {
        File file = new File(path);

        if (!file.exists()) return;

        if (file.isFile()) {
            if (!file.delete()) {
                Log.e("FileUtil", "Failed to delete file: " + file.getAbsolutePath());
            }
            return;
        }

        File[] fileArr = file.listFiles();

        if (fileArr != null) {
            for (File subFile : fileArr) {
                if (subFile.isDirectory()) {
                    deleteFile(subFile.getAbsolutePath());
                }

                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }

        if (!file.delete()) {
            Log.e("FileUtil", "Failed to delete directory: " + file.getAbsolutePath());
        }
    }

    public static boolean isExistFile(String path) {
        return new File(path).exists();
    }

    public static void makeDir(String path) {
        if (!isExistFile(path)) {
            try {
                new File(path).mkdirs();
            } catch (SecurityException e) {
                Log.e("FileUtil", "Error creating directory: " + path, e);
            }
        }
    }

    public static void listDir(String path, ArrayList<String> list) {
        File[] listFiles;
        File dir = new File(path);
        if (dir.exists() && !dir.isFile() && (listFiles = dir.listFiles()) != null && listFiles.length > 0 && list != null) {
            list.clear();
            for (File file : listFiles) {
                list.add(file.getAbsolutePath());
            }
        }
    }

    public static void listDirAsFile(String path, ArrayList<File> list) {
        File[] listFiles;
        File dir = new File(path);
        if (dir.exists() && !dir.isFile() && (listFiles = dir.listFiles()) != null && listFiles.length > 0 && list != null) {
            list.clear();
            Collections.addAll(list, listFiles);
        }
    }

    /**
     * @return List of files that have the filename extension {@code extension}.
     */
    public static ArrayList<String> listFiles(String dir, String extension) {
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>();
        listDir(dir, files);
        for (String filePath : files) {
            if (filePath.endsWith(extension) && isFile(filePath)) {
                list.add(filePath);
            }
        }
        return list;
    }

    @NonNull
    public static List<File> listFilesRecursively(@NonNull File directory, @Nullable String optionalFilenameExtension) {
        List<File> files = new LinkedList<>();

        File[] directoryFiles = directory.listFiles();
        if (directoryFiles != null) {
            for (File file : directoryFiles) {
                if (file.isFile()) {
                    if (optionalFilenameExtension != null && file.getName().endsWith(optionalFilenameExtension)) {
                        files.add(file);
                    }
                } else {
                    files.addAll(listFilesRecursively(file, optionalFilenameExtension));
                }
            }
        }

        return files;
    }

    public static boolean isDirectory(String path) {
        if (!isExistFile(path)) {
            return false;
        }
        return new File(path).isDirectory();
    }

    public static boolean isFile(String path) {
        if (!isExistFile(path)) {
            return false;
        }
        return new File(path).isFile();
    }

    public static long getFileLength(String path) {
        if (!isExistFile(path)) {
            return 0;
        }
        return new File(path).length();
    }

    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getPackageDataDir(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            return context.getFilesDir().getAbsolutePath();
        }
        return dir.getAbsolutePath();
    }

    public static String getPublicDir(String type) {
        return Environment.getExternalStoragePublicDirectory(type).getAbsolutePath();
    }

    public static String convertUriToFilePath(Context context, Uri uri) {
        String path = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    path = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);

                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                }

                try {
                    Uri contentUri = ContentUris
                            .withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                    path = getDataColumn(context, contentUri, null, null);
                } catch (NumberFormatException e) {
                    Log.w("FileUtil", "Non-numeric download document id: " + id);
                }
            } else if (isMediaDocument(uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String[] split = docId.split(":");
                String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                String[] selectionArgs = {
                        split[1]
                };

                path = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            path = getDataColumn(context, uri, null, null);
        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }

        if (path != null) {
            try {
                return URLDecoder.decode(path, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;

        final String column = MediaStore.Images.Media.DATA;
        String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w("FileUtil", "Failed to query column '" + column + "' from uri: " + uri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static void saveBitmap(Bitmap bitmap, String destPath) {
        FileUtil.createNewFileIfNotPresent(destPath);

        try (FileOutputStream out = new FileOutputStream(destPath)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Log.e("FileUtil", e.getMessage(), e);
        }
    }

    public static Bitmap getScaledBitmap(String filePath, int maxSize) {
        int scaledWidth;
        Bitmap decodeFile = BitmapFactory.decodeFile(filePath);
        if (decodeFile == null) {
            return null;
        }
        int width = decodeFile.getWidth();
        int height = decodeFile.getHeight();
        int scaledHeight = maxSize;
        if (width > height) {
            scaledHeight = maxSize * height / width;
            scaledWidth = maxSize;
        } else {
            scaledWidth = width * maxSize / height;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(decodeFile, scaledWidth, scaledHeight, true);
        if (scaled != decodeFile) {
            decodeFile.recycle();
        }
        return scaled;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampleBitmapFromPath(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static void resizeBitmapFileRetainRatio(String fromPath, String destPath, int max) {
        if (isExistFile(fromPath)) {
            Bitmap scaled = getScaledBitmap(fromPath, max);
            saveBitmap(scaled, destPath);
            scaled.recycle();
        }
    }

    public static void resizeBitmapFileToSquare(String fromPath, String destPath, int max) {
        if (isExistFile(fromPath)) {
            Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
            if (decodeFile == null) return;
            Bitmap scaled = Bitmap.createScaledBitmap(decodeFile, max, max, true);
            if (scaled != decodeFile) {
                decodeFile.recycle();
            }
            saveBitmap(scaled, destPath);
            scaled.recycle();
        }
    }

    public static void resizeBitmapFileToCircle(String fromPath, String destPath) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Bitmap createBitmap = Bitmap.createBitmap(decodeFile.getWidth(), decodeFile.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, decodeFile.getWidth(), decodeFile.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(-12434878);
        canvas.drawCircle((float) (decodeFile.getWidth() / 2), (float) (decodeFile.getHeight() / 2), (float) (decodeFile.getWidth() / 2), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(decodeFile, rect, rect, paint);
        decodeFile.recycle();
        saveBitmap(createBitmap, destPath);
        createBitmap.recycle();
    }

    public static void resizeBitmapFileWithRoundedBorder(String fromPath, String destPath, int pixels) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Bitmap createBitmap = Bitmap.createBitmap(decodeFile.getWidth(), decodeFile.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, decodeFile.getWidth(), decodeFile.getHeight());
        RectF rectF = new RectF(rect);
        float f = (float) pixels;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(-12434878);
        canvas.drawRoundRect(rectF, f, f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(decodeFile, rect, rect, paint);
        decodeFile.recycle();
        saveBitmap(createBitmap, destPath);
        createBitmap.recycle();
    }

    public static void cropBitmapFileFromCenter(String fromPath, String destPath, int w, int h) {
        if (!isExistFile(fromPath)) return;
        Bitmap src = BitmapFactory.decodeFile(fromPath);
        if (src == null) return;

        int width = src.getWidth();
        int height = src.getHeight();

        if (width < w && height < h) {
            src.recycle();
            return;
        }

        int x = 0;
        int y = 0;

        if (width > w)
            x = (width - w) / 2;

        if (height > h)
            y = (height - h) / 2;

        int cw = w;
        int ch = h;

        if (w > width)
            cw = width;

        if (h > height)
            ch = height;

        Bitmap bitmap = Bitmap.createBitmap(src, x, y, cw, ch);
        if (bitmap != src) {
            src.recycle();
        }
        saveBitmap(bitmap, destPath);
        bitmap.recycle();
    }

    public static void rotateBitmapFile(String fromPath, String destPath, float angle) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotated = Bitmap.createBitmap(decodeFile, 0, 0, decodeFile.getWidth(), decodeFile.getHeight(), matrix, true);
        if (rotated != decodeFile) {
            decodeFile.recycle();
        }
        saveBitmap(rotated, destPath);
        rotated.recycle();
    }

    public static void scaleBitmapFile(String fromPath, String destPath, float x, float y) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Matrix matrix = new Matrix();
        matrix.postScale(x, y);
        Bitmap scaled = Bitmap.createBitmap(decodeFile, 0, 0, decodeFile.getWidth(), decodeFile.getHeight(), matrix, true);
        if (scaled != decodeFile) {
            decodeFile.recycle();
        }
        saveBitmap(scaled, destPath);
        scaled.recycle();
    }

    public static void skewBitmapFile(String fromPath, String destPath, float x, float y) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Matrix matrix = new Matrix();
        matrix.postSkew(x, y);
        Bitmap skewed = Bitmap.createBitmap(decodeFile, 0, 0, decodeFile.getWidth(), decodeFile.getHeight(), matrix, true);
        if (skewed != decodeFile) {
            decodeFile.recycle();
        }
        saveBitmap(skewed, destPath);
        skewed.recycle();
    }

    public static void setBitmapFileColorFilter(String fromPath, String destPath, int color) {
        if (!isExistFile(fromPath)) return;

        Bitmap decodeFile = BitmapFactory.decodeFile(fromPath);
        if (decodeFile == null) return;
        Bitmap createBitmap = Bitmap.createBitmap(decodeFile, 0, 0, decodeFile.getWidth() - 1, decodeFile.getHeight() - 1);
        if (createBitmap != decodeFile) {
            decodeFile.recycle();
        }
        Paint paint = new Paint();
        paint.setColorFilter(new LightingColorFilter(color, 1));
        new Canvas(createBitmap).drawBitmap(createBitmap, 0.0f, 0.0f, paint);
        saveBitmap(createBitmap, destPath);
        createBitmap.recycle();
    }

    public static void setBitmapFileBrightness(String fromPath, String destPath, float brightness) {
        if (!isExistFile(fromPath)) return;

        Bitmap src = BitmapFactory.decodeFile(fromPath);
        if (src == null) return;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        1, 0, 0, 0, brightness,
                        0, 1, 0, 0, brightness,
                        0, 0, 1, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        src.recycle();
        saveBitmap(bitmap, destPath);
        bitmap.recycle();
    }

    public static void setBitmapFileContrast(String fromPath, String destPath, float contrast) {
        if (!isExistFile(fromPath)) return;

        Bitmap src = BitmapFactory.decodeFile(fromPath);
        if (src == null) return;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, 0,
                        0, contrast, 0, 0, 0,
                        0, 0, contrast, 0, 0,
                        0, 0, 0, 1, 0
                });

        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        src.recycle();
        saveBitmap(bitmap, destPath);
        bitmap.recycle();
    }

    public static int getJpegRotate(String filePath) {
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int iOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

            rotate = switch (iOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                default -> rotate;
            };
        } catch (IOException e) {
            return 0;
        }

        return rotate;
    }

    public static File createNewPictureFile(Context context) {
        File dcimDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        if (dcimDir == null) {
            dcimDir = new File(context.getFilesDir(), Environment.DIRECTORY_DCIM);
            dcimDir.mkdirs();
        }
        return new File(dcimDir.getAbsolutePath() + File.separator + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date()) + ".jpg");
    }

    public static byte[] readFromInputStream(InputStream stream) {
        int available;

        try {
            available = stream.available();
        } catch (IOException e) {
            available = 0;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[Math.max(available, 1024)];

        try {
            for (int len = stream.read(buffer); len != -1; len = stream.read(buffer)) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            return new byte[0];
        }

        return outputStream.toByteArray();
    }

    /**
     * Write bytes to a file.
     *
     * @param target The file to write the data to. Note that it'll get created, even parent directories
     * @param data   The data in bytes to write to. {@link FileUtil#readFromInputStream(InputStream)}
     *               for example, reads bytes
     * @throws IOException Thrown when any exception occurs while operating
     */
    public static void writeBytes(File target, byte[] data) throws IOException {
        if (!target.exists()) {
            File parentFile = target.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
        }
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target))) {
            outputStream.write(data);
            outputStream.flush();
        }
    }

    public static void extractZipTo(ZipInputStream input, String outPath) throws IOException {
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        String canonicalOutDir = outDir.getCanonicalPath() + File.separator;

        ZipEntry entry = input.getNextEntry();
        while (entry != null) {
            File destFile = new File(outPath, entry.getName());
            if (!destFile.getCanonicalPath().startsWith(canonicalOutDir)) {
                throw new IOException("Zip entry outside target dir: " + entry.getName());
            }

            if (!entry.isDirectory()) {
                destFile.getParentFile().mkdirs();
                writeBytes(destFile, readFromInputStream(input));
            }
            input.closeEntry();
            entry = input.getNextEntry();
        }
        input.close();
    }

    /**
     * Asks the user to grant the current app {@link android.Manifest.permission#MANAGE_EXTERNAL_STORAGE}.
     * Will silently ignore cases where a screen to manage that permission doesn't exist, except on
     * devices with an API level of 29 or lower.
     *
     * @throws AssertionError Thrown if the device's API level is 29 or lower
     */
    public static void requestAllFilesAccessPermission(Context context) {
        if (Build.VERSION.SDK_INT > 29) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e("FileUtil", "Activity to manage apps' all files access permission not found!");
                }
            }
        } else {
            throw new AssertionError("Not on an API level 30 or higher device!");
        }
    }

    /**
     * Checks if a provided file is image or not. I don't know if it throws any exceptions
     * TODO: Find a better solution if available
     */
    public static boolean isImageFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }
}
