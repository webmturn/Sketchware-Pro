package pro.sketchware.core;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import mod.jbk.util.LogUtil;
import pro.sketchware.utility.FileUtil;

/**
 * Tracks CRC32 hashes of Java source files and classpath between builds,
 * enabling incremental Java compilation (skip recompiling unchanged files).
 * <p>
 * Cache is stored as JSON at {@code bin/build_hashes.json}.
 * A missing or corrupt cache file forces a full recompile on the next build.
 */
public class IncrementalBuildCache {

    private static final String TAG = "IncrementalBuildCache";
    private static final String CACHE_FILE_NAME = "build_hashes.json";

    private final String cacheFilePath;

    private Map<String, Long> fileHashes = new HashMap<>();
    private long classpathHash = 0L;
    private long rJavaHash = 0L;

    private static class CacheData {
        Map<String, Long> fileHashes = new HashMap<>();
        long classpathHash = 0L;
        long rJavaHash = 0L;
    }

    public IncrementalBuildCache(String binDirectoryPath) {
        this.cacheFilePath = binDirectoryPath + File.separator + CACHE_FILE_NAME;
    }

    /** Returns true if a cache file from a previous successful build exists. */
    public boolean hasCacheFile() {
        return FileUtil.isExistFile(cacheFilePath);
    }

    /** Loads stored hashes from the cache file. No-op if the file doesn't exist. */
    public void load() {
        if (!hasCacheFile()) return;
        try {
            String content = FileUtil.readFileIfExist(cacheFilePath);
            if (content.isEmpty()) return;
            CacheData data = new Gson().fromJson(content, CacheData.class);
            if (data != null) {
                fileHashes = data.fileHashes != null ? data.fileHashes : new HashMap<>();
                classpathHash = data.classpathHash;
                rJavaHash = data.rJavaHash;
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "Failed to read build hash cache, will do full recompile: " + e.getMessage());
            fileHashes = new HashMap<>();
            classpathHash = 0L;
            rJavaHash = 0L;
        }
    }

    /** Persists the current hash state to disk after a successful build. */
    public void save() {
        CacheData data = new CacheData();
        data.fileHashes = fileHashes;
        data.classpathHash = classpathHash;
        data.rJavaHash = rJavaHash;
        FileUtil.writeFile(cacheFilePath, new Gson().toJson(data));
    }

    /** Deletes the cache file, forcing a full recompile on the next build. */
    public void invalidate() {
        FileUtil.deleteFile(cacheFilePath);
        fileHashes.clear();
        classpathHash = 0L;
        rJavaHash = 0L;
    }

    /**
     * Returns true if the given Java file's content has changed since the last
     * successful build (or if it was never seen before).
     */
    public boolean isDirtyFile(File javaFile) {
        Long stored = fileHashes.get(javaFile.getAbsolutePath());
        if (stored == null) return true;
        return stored != computeFileCRC32(javaFile);
    }

    /** Records the current CRC32 of a Java file as "clean" in the in-memory map. */
    public void markFileClean(File javaFile) {
        fileHashes.put(javaFile.getAbsolutePath(), computeFileCRC32(javaFile));
    }

    /**
     * Returns a snapshot of all absolute file paths currently stored in the hash cache.
     * Used to detect source files that existed in a previous build but have since been deleted.
     */
    public Set<String> getAllCachedFilePaths() {
        return new HashSet<>(fileHashes.keySet());
    }

    /** Removes a single path from the in-memory hash cache (does not save to disk). */
    public void removeFromCache(String absolutePath) {
        fileHashes.remove(absolutePath);
    }

    /** Returns true if the classpath has changed since the last successful build. */
    public boolean isClasspathChanged(String currentClasspath) {
        return classpathHash != computeStringCRC32(currentClasspath);
    }

    /** Stores the current classpath hash for comparison on the next build. */
    public void storeClasspath(String classpath) {
        classpathHash = computeStringCRC32(classpath);
    }

    /**
     * Returns true if any file in the R.java directory has changed since the
     * last successful build.  A change here means resource IDs may have been
     * reassigned, so all Activity classes must be fully recompiled.
     */
    public boolean isRJavaChanged(String rJavaDirectoryPath) {
        long current = computeDirectoryCRC32(new File(rJavaDirectoryPath));
        return rJavaHash == 0L || rJavaHash != current;
    }

    /** Stores the current CRC32 of the R.java directory. */
    public void storeRJavaHash(String rJavaDirectoryPath) {
        rJavaHash = computeDirectoryCRC32(new File(rJavaDirectoryPath));
    }

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    public static long computeFileCRC32(File file) {
        if (!file.exists()) return -1L;
        try (FileInputStream fis = new FileInputStream(file)) {
            CRC32 crc = new CRC32();
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) crc.update(buf, 0, len);
            return crc.getValue();
        } catch (IOException e) {
            return -1L;
        }
    }

    private static long computeStringCRC32(String s) {
        if (s == null) return 0L;
        CRC32 crc = new CRC32();
        crc.update(s.getBytes());
        return crc.getValue();
    }

    private static long computeDirectoryCRC32(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return 0L;
        List<File> files = FileUtil.listFilesRecursively(directory, null);
        files.sort((a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()));
        CRC32 crc = new CRC32();
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) crc.update(buf, 0, len);
            } catch (IOException e) {
                LogUtil.d("IncrementalBuildCache", "Failed to compute CRC for " + file.getName() + ": " + e.getMessage());
            }
        }
        return crc.getValue();
    }
}
