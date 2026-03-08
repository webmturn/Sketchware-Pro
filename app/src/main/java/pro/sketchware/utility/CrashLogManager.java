package pro.sketchware.utility;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import pro.sketchware.SketchApplication;

/**
 * Manages crash log files: writing, reading, listing, and cleanup.
 * <p>
 * Crash logs are stored in {@code <app-external-files>/crash_logs/} as plain text files
 * named {@code crash_<timestamp>.txt}. Only the most recent {@link #MAX_CRASH_LOGS}
 * files are kept; older ones are automatically pruned.
 */
public final class CrashLogManager {

    private static final String TAG = "CrashLogManager";
    private static final String CRASH_DIR_NAME = "crash_logs";
    private static final String CRASH_FILE_PREFIX = "crash_";
    private static final String CRASH_FILE_SUFFIX = ".txt";
    private static final int MAX_CRASH_LOGS = 10;

    private CrashLogManager() {
    }

    /**
     * Returns the crash log directory, creating it if necessary.
     */
    @NonNull
    public static File getCrashDir() {
        File dir = new File(SketchApplication.getContext().getExternalFilesDir(null), CRASH_DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Writes a crash report to a new file synchronously.
     * This method is designed to be called from the uncaught exception handler,
     * so it avoids allocations and complex logic as much as possible.
     *
     * @param thread    the thread that crashed
     * @param throwable the uncaught exception
     * @return the crash file path, or null if writing failed
     */
    @Nullable
    public static String writeCrashLog(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            File crashDir = getCrashDir();
            String fileName = CRASH_FILE_PREFIX + System.currentTimeMillis() + CRASH_FILE_SUFFIX;
            File crashFile = new File(crashDir, fileName);

            String report = buildCrashReport(thread, throwable);

            // Use low-level file I/O to minimize failure risk in crash context
            try (FileOutputStream fos = new FileOutputStream(crashFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(report);
                writer.flush();
                fos.getFD().sync();
            }

            pruneOldLogs(crashDir);
            return crashFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write crash log", e);
            return null;
        }
    }

    /**
     * Builds a human-readable crash report string with device info and stack trace.
     */
    @NonNull
    public static String buildCrashReport(@NonNull Thread thread, @NonNull Throwable throwable) {
        StringBuilder sb = new StringBuilder(2048);

        // Timestamp
        sb.append("Crash Time: ");
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(new Date()));
        sb.append('\n');

        // App info
        try {
            PackageInfo info = SketchApplication.getContext().getPackageManager()
                    .getPackageInfo(SketchApplication.getContext().getPackageName(), 0);
            sb.append("App Version: ").append(info.versionName)
                    .append(" (").append(info.versionCode).append(")\n");
            long apkSize = new File(info.applicationInfo.sourceDir).length();
            sb.append("APK Size: ")
                    .append(Formatter.formatFileSize(SketchApplication.getContext(), apkSize))
                    .append(" (").append(apkSize).append(" B)\n");
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        // Device info
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        sb.append("Brand: ").append(Build.BRAND).append('\n');
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");

        // Memory info
        Runtime rt = Runtime.getRuntime();
        sb.append("Memory: free=").append(rt.freeMemory() / 1024).append("KB")
                .append(", total=").append(rt.totalMemory() / 1024).append("KB")
                .append(", max=").append(rt.maxMemory() / 1024).append("KB\n");

        // Thread info
        sb.append("Thread: ").append(thread.getName())
                .append(" (id=").append(thread.getId()).append(")\n");

        // Stack trace
        sb.append("\n--- Stack Trace ---\n");
        sb.append(Log.getStackTraceString(throwable));

        return sb.toString();
    }

    /**
     * Returns the most recent crash log file, or null if none exists.
     */
    @Nullable
    public static File getLatestCrashLog() {
        File[] logs = listCrashLogs();
        return (logs != null && logs.length > 0) ? logs[0] : null;
    }

    /**
     * Returns all crash log files sorted by most recent first.
     */
    @Nullable
    public static File[] listCrashLogs() {
        File dir = getCrashDir();
        File[] files = dir.listFiles((d, name) ->
                name.startsWith(CRASH_FILE_PREFIX) && name.endsWith(CRASH_FILE_SUFFIX));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files;
    }

    /**
     * Deletes all crash log files.
     */
    public static void clearAll() {
        File[] logs = listCrashLogs();
        if (logs == null) return;
        for (File f : logs) {
            f.delete();
        }
    }

    /**
     * Keeps only the most recent {@link #MAX_CRASH_LOGS} files, deleting older ones.
     */
    private static void pruneOldLogs(@NonNull File crashDir) {
        File[] files = crashDir.listFiles((d, name) ->
                name.startsWith(CRASH_FILE_PREFIX) && name.endsWith(CRASH_FILE_SUFFIX));
        if (files == null || files.length <= MAX_CRASH_LOGS) return;
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (int i = MAX_CRASH_LOGS; i < files.length; i++) {
            files[i].delete();
        }
    }
}
