package pro.sketchware.core.build;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private static final int CACHE_FORMAT_VERSION = 2;

    private final String cacheFilePath;

    private Map<String, Long> fileHashes = new HashMap<>();
    private Map<String, String> compiledClassBasePaths = new HashMap<>();
    private long classpathHash = 0L;
    private long rJavaHash = 0L;
    private Map<String, Long> rJavaFileHashes = new HashMap<>();
    private boolean requiresFullRebuildMigration = false;

    private static class CacheData {
        int cacheFormatVersion = 0;
        Map<String, Long> fileHashes = new HashMap<>();
        Map<String, String> compiledClassBasePaths = new HashMap<>();
        long classpathHash = 0L;
        long rJavaHash = 0L;
        Map<String, Long> rJavaFileHashes = new HashMap<>();
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
        fileHashes = new HashMap<>();
        compiledClassBasePaths = new HashMap<>();
        classpathHash = 0L;
        rJavaHash = 0L;
        rJavaFileHashes = new HashMap<>();
        requiresFullRebuildMigration = false;
        if (!hasCacheFile()) return;
        try {
            String content = FileUtil.readFileIfExist(cacheFilePath);
            if (content.isEmpty()) return;
            CacheData data = new Gson().fromJson(content, CacheData.class);
            if (data != null) {
                fileHashes = data.fileHashes != null ? data.fileHashes : new HashMap<>();
                compiledClassBasePaths = data.compiledClassBasePaths != null ? data.compiledClassBasePaths : new HashMap<>();
                classpathHash = data.classpathHash;
                rJavaHash = data.rJavaHash;
                rJavaFileHashes = data.rJavaFileHashes != null ? data.rJavaFileHashes : new HashMap<>();
                requiresFullRebuildMigration = shouldRequireFullRebuildMigration(data.cacheFormatVersion);
                if (requiresFullRebuildMigration) {
                    LogUtil.d(TAG, "Loaded incompatible or incomplete build cache metadata; next build will do a one-time full rebuild migration");
                }
            }
        } catch (Exception e) {
            LogUtil.w(TAG, "Failed to read build hash cache, will do full recompile: " + e.getMessage());
        }
    }

    /** Persists the current hash state to disk after a successful build. */
    public void save() {
        CacheData data = new CacheData();
        data.cacheFormatVersion = CACHE_FORMAT_VERSION;
        data.fileHashes = fileHashes;
        data.compiledClassBasePaths = compiledClassBasePaths;
        data.classpathHash = classpathHash;
        data.rJavaHash = rJavaHash;
        data.rJavaFileHashes = rJavaFileHashes;
        FileUtil.writeFile(cacheFilePath, new Gson().toJson(data));
        requiresFullRebuildMigration = false;
    }

    /** Deletes the cache file, forcing a full recompile on the next build. */
    public void invalidate() {
        FileUtil.deleteFile(cacheFilePath);
        fileHashes.clear();
        compiledClassBasePaths.clear();
        classpathHash = 0L;
        rJavaHash = 0L;
        rJavaFileHashes.clear();
        requiresFullRebuildMigration = false;
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
        markFileClean(javaFile, null);
    }

    public void markFileClean(File javaFile, String compiledClassBasePath) {
        fileHashes.put(javaFile.getAbsolutePath(), computeFileCRC32(javaFile));
        if (compiledClassBasePath == null || compiledClassBasePath.isEmpty()) {
            compiledClassBasePaths.remove(javaFile.getAbsolutePath());
        } else {
            compiledClassBasePaths.put(javaFile.getAbsolutePath(), compiledClassBasePath);
        }
    }

    public void clearTrackedJavaSources() {
        fileHashes.clear();
        compiledClassBasePaths.clear();
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
        compiledClassBasePaths.remove(absolutePath);
    }

    public String getStoredCompiledClassBasePath(String absolutePath) {
        return compiledClassBasePaths.get(absolutePath);
    }

    public boolean requiresFullRebuildMigration() {
        return requiresFullRebuildMigration;
    }

    private boolean shouldRequireFullRebuildMigration(int cacheFormatVersion) {
        return !fileHashes.isEmpty()
                && (cacheFormatVersion != CACHE_FORMAT_VERSION
                || compiledClassBasePaths.size() < fileHashes.size());
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

    public boolean isRJavaFileChanged(String rJavaDirectoryPath, String relativeFilePath) {
        Map<String, Long> currentHashes = computeRelativeFileCRC32s(new File(rJavaDirectoryPath));
        Long stored = rJavaFileHashes.get(relativeFilePath);
        Long current = currentHashes.get(relativeFilePath);
        if (stored == null) {
            return current != null;
        }
        return !stored.equals(current);
    }

    public ArrayList<String> describeRJavaChanges(String rJavaDirectoryPath, int maxEntries) {
        Map<String, Long> currentHashes = computeRelativeFileCRC32s(new File(rJavaDirectoryPath));
        ArrayList<String> descriptions = new ArrayList<>();

        ArrayList<String> currentPaths = new ArrayList<>(currentHashes.keySet());
        currentPaths.sort(String::compareTo);
        for (String relativePath : currentPaths) {
            Long stored = rJavaFileHashes.get(relativePath);
            Long current = currentHashes.get(relativePath);
            if (stored == null) {
                descriptions.add("new:" + relativePath);
            } else if (!stored.equals(current)) {
                descriptions.add("changed:" + relativePath);
            }
        }

        ArrayList<String> previousPaths = new ArrayList<>(rJavaFileHashes.keySet());
        previousPaths.sort(String::compareTo);
        for (String relativePath : previousPaths) {
            if (!currentHashes.containsKey(relativePath)) {
                descriptions.add("deleted:" + relativePath);
            }
        }

        if (maxEntries > 0 && descriptions.size() > maxEntries) {
            ArrayList<String> limited = new ArrayList<>(descriptions.subList(0, maxEntries));
            limited.add("...+" + (descriptions.size() - maxEntries) + " more");
            return limited;
        }

        return descriptions;
    }

    /** Stores the current CRC32 of the R.java directory. */
    public void storeRJavaHash(String rJavaDirectoryPath) {
        File rJavaDirectory = new File(rJavaDirectoryPath);
        rJavaHash = computeDirectoryCRC32(rJavaDirectory);
        rJavaFileHashes = computeRelativeFileCRC32s(rJavaDirectory);
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
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        crc.update(bytes, 0, bytes.length);
        return crc.getValue();
    }

    private static long computeDirectoryCRC32(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return 0L;
        List<File> files = FileUtil.listFilesRecursively(directory, ".java");
        files.sort((a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()));
        CRC32 crc = new CRC32();
        String rootPath = directory.getAbsolutePath();
        for (File file : files) {
            String relativePath = toRelativePath(rootPath, file);
            if (shouldIgnoreRJavaRelativePath(relativePath)) {
                continue;
            }
            updateCrcWithUtf8(crc, relativePath);
            crc.update(0);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) crc.update(buf, 0, len);
                crc.update(0);
            } catch (IOException e) {
                LogUtil.d("IncrementalBuildCache", "Failed to compute CRC for " + file.getName() + ": " + e.getMessage());
            }
        }
        return crc.getValue();
    }

    private static Map<String, Long> computeRelativeFileCRC32s(File directory) {
        Map<String, Long> hashes = new HashMap<>();
        if (!directory.exists() || !directory.isDirectory()) return hashes;

        String rootPath = directory.getAbsolutePath();
        List<File> files = FileUtil.listFilesRecursively(directory, ".java");
        files.sort((a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()));
        for (File file : files) {
            String relativePath = toRelativePath(rootPath, file);
            if (shouldIgnoreRJavaRelativePath(relativePath)) {
                continue;
            }
            hashes.put(relativePath, computeFileCRC32(file));
        }

        return hashes;
    }

    private static String toRelativePath(String rootPath, File file) {
        String absolutePath = file.getAbsolutePath();
        String relativePath = absolutePath.substring(rootPath.length());
        if (relativePath.startsWith(File.separator)) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

    private static void updateCrcWithUtf8(CRC32 crc, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        crc.update(bytes, 0, bytes.length);
    }

    private static boolean shouldIgnoreRJavaRelativePath(String relativePath) {
        return "R.java".equals(relativePath);
    }
}
