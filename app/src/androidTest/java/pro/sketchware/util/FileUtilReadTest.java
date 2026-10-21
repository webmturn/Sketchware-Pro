package pro.sketchware.util;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Bundle;
import android.os.Debug;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import pro.sketchware.core.build.IncrementalBuildCache;

/** Both public readers retain the same decoding, complete content and read-only behavior. */
@RunWith(AndroidJUnit4.class)
public class FileUtilReadTest {
    @Test public void unicodeLineEndingsAndBufferBoundariesRemainComplete() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path file = workspace.root.resolve("text.txt");
            String pattern = "\uFEFF中文😀é\u0000\\path\r\n\tend\n";
            for (int length : new int[]{0, 1, 1023, 1024, 1025, 2047, 2048, 8191, 8192, 8193, 131079}) {
                String input = pattern.repeat((length + pattern.length() - 1) / pattern.length()).substring(0, length);
                byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
                Files.write(file, bytes);
                Files.setLastModifiedTime(file, FileTime.fromMillis(1600000000000L));
                assertBoth(new String(bytes, Charset.defaultCharset()), file);
                assertArrayEquals(bytes, Files.readAllBytes(file));
                assertEquals(1600000000000L, Files.getLastModifiedTime(file).toMillis());
            }
        }
    }

    @Test public void malformedUtf8KeepsTheExistingReplacementDecoding() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            byte[] bytes = new byte[4099]; Arrays.fill(bytes, (byte) 'a');
            for (int index : new int[]{1022, 2046, 4096}) {
                bytes[index] = (byte) 0xe4; bytes[index + 1] = (byte) 0xb8; bytes[index + 2] = (byte) 0xff;
            }
            Path file = workspace.root.resolve("malformed.txt"); Files.write(file, bytes);
            assertBoth(new String(bytes, Charset.defaultCharset()), file);
        }
    }

    @Test public void absentEmptyAndDirectoryInputsStayEmptyWithoutCreatingFiles() throws Exception {
        assertEquals("", FileUtil.readFile(null)); assertEquals("", FileUtil.readFileIfExist(null));
        assertEquals("", FileUtil.readFile("")); assertEquals("", FileUtil.readFileIfExist(""));
        try (Workspace workspace = new Workspace(false)) {
            Path missing = workspace.root.resolve("missing.txt"); assertBoth("", missing); assertFalse(Files.exists(missing));
            Path empty = Files.createFile(workspace.root.resolve("empty.txt")); assertBoth("", empty);
            assertBoth("", workspace.root); assertTrue(Files.isDirectory(workspace.root));
            try (Stream<Path> entries = Files.list(workspace.root)) { assertEquals(1L, entries.count()); }
        }
    }

    @Test public void zeroLengthProcFileIsStillReadToEof() throws Exception {
        Path proc = Path.of("/proc/self/cmdline");
        assertTrue(Files.isRegularFile(proc)); assertEquals(0L, Files.size(proc));
        String expected = new String(Files.readAllBytes(proc), Charset.defaultCharset());
        assertTrue(expected, expected.contains("pro.sketchware"));
        assertBoth(expected, proc);
    }

    @Test public void regularFileSymlinksAreReadAndDanglingLinksStayEmpty() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path target = workspace.root.resolve("target.txt"), link = workspace.root.resolve("link.txt");
            String expected = "linked 中文😀\r\n";
            Files.write(target, expected.getBytes(StandardCharsets.UTF_8));
            Files.createSymbolicLink(link, target);
            assertBoth(expected, link);
            Files.delete(target); assertBoth("", link);
            assertTrue(Files.isSymbolicLink(link));
        }
    }

    @Test public void incrementalBuildCacheReloadsHashesAndRecoversFromInvalidContent() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path source = workspace.root.resolve("源码 Example.java");
            Path rDirectory = Files.createDirectory(workspace.root.resolve("gen"));
            Files.write(source, "class Example { String text = \"中文😀\"; }".getBytes(StandardCharsets.UTF_8));
            Path rPackage = Files.createDirectory(rDirectory.resolve("test"));
            Files.write(rPackage.resolve("R.java"), "package test; class R {}".getBytes(StandardCharsets.UTF_8));
            IncrementalBuildCache cache = new IncrementalBuildCache(workspace.root.toString());
            String classBase = workspace.root.resolve("classes/Example").toString();
            cache.markFileClean(source.toFile(), classBase); cache.storeClasspath("platform.jar:lib.jar");
            cache.storeRJavaHash(rDirectory.toString()); cache.save();
            Path json = workspace.root.resolve("build_hashes.json"); byte[] original = Files.readAllBytes(json);
            long modified = Files.getLastModifiedTime(json).toMillis();
            IncrementalBuildCache loaded = new IncrementalBuildCache(workspace.root.toString()); loaded.load();
            assertFalse(loaded.requiresFullRebuildMigration()); assertFalse(loaded.isDirtyFile(source.toFile()));
            assertEquals(classBase, loaded.getStoredCompiledClassBasePath(source.toString()));
            assertFalse("Classpath must survive reload", loaded.isClasspathChanged("platform.jar:lib.jar"));
            assertFalse("Packaged R.java must survive reload", loaded.isRJavaChanged(rDirectory.toString()));
            assertEquals(1, loaded.getAllCachedFilePaths().size());
            assertArrayEquals(original, Files.readAllBytes(json)); assertEquals(modified, Files.getLastModifiedTime(json).toMillis());
            Files.write(source, "class Example { int changed; }".getBytes(StandardCharsets.UTF_8));
            assertTrue(loaded.isDirtyFile(source.toFile()));
            Files.write(json, "{invalid".getBytes(StandardCharsets.UTF_8)); loaded.load();
            assertTrue(loaded.getAllCachedFilePaths().isEmpty()); assertTrue(loaded.isDirtyFile(source.toFile()));
        }
    }

    /** Timings and ART process allocation deltas are observations, never flaky pass thresholds. */
    @Test(timeout = 120000) public void benchmarkWarmTextReads() throws Exception {
        try (Workspace workspace = new Workspace(true)) {
            int[] sizes = {1024, 128 * 1024, 4 * 1024 * 1024};
            int[] batchSizes = {32, 4, 1};
            Path[] files = new Path[sizes.length]; String[] expected = new String[sizes.length];
            String pattern = "{\"source\":\"Example.java\",\"hash\":123456789}\r\n";
            for (int i = 0; i < sizes.length; i++) {
                expected[i] = pattern.repeat((sizes[i] + pattern.length() - 1) / pattern.length()).substring(0, sizes[i]);
                files[i] = workspace.root.resolve("text-" + sizes[i] + ".json");
                Files.write(files[i], expected[i].getBytes(StandardCharsets.UTF_8));
            }
            long[][] times = new long[6][9], allocations = new long[6][9];
            for (int round = -10; round < 9; round++) {
                for (int offset = 0; offset < 6; offset++) {
                    int operation = Math.floorMod(round + offset, 6), index = operation % 3, batch = batchSizes[index];
                    String value = null, path = files[index].toString();
                    long allocatedBefore = allocatedBytes(); long start = System.nanoTime();
                    for (int n = 0; n < batch; n++) value = operation < 3 ? FileUtil.readFile(path) : FileUtil.readFileIfExist(path);
                    long elapsed = System.nanoTime() - start; long allocatedAfter = allocatedBytes();
                    if (round >= 0) {
                        times[operation][round] = elapsed / batch;
                        allocations[operation][round] = allocatedBefore < 0 || allocatedAfter < 0 ? -1 : (allocatedAfter - allocatedBefore) / batch;
                    }
                    assertEquals(expected[index], value);
                }
            }
            Bundle report = new Bundle();
            report.putString("text_read_workload", "external app storage, warm caches, 10 warmup rounds + 9 samples; 1KiB/128KiB/4MiB ASCII; batches 32/4/1; per-call batch averages");
            report.putString("text_read_allocations", "ART process-wide bytes-allocated deltas per call, not retained or peak heap; -1 means unavailable");
            for (int operation = 0; operation < 6; operation++) {
                String name = (operation < 3 ? "read_" : "read_if_exist_") + sizes[operation % 3];
                report.putString(name + "_samples_ns", Arrays.toString(times[operation]));
                report.putString(name + "_median_ms", Double.toString(median(times[operation]) / 1_000_000.0));
                report.putString(name + "_allocated_bytes", Arrays.toString(allocations[operation]));
                report.putString(name + "_median_allocated_bytes", Long.toString(median(allocations[operation])));
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, report);
        }
    }

    private static void assertBoth(String expected, Path file) {
        assertEquals("readFile: " + file, expected, FileUtil.readFile(file.toString()));
        assertEquals("readFileIfExist: " + file, expected, FileUtil.readFileIfExist(file.toString()));
    }
    private static long allocatedBytes() {
        try { String value = Debug.getRuntimeStat("art.gc.bytes-allocated"); return value == null ? -1 : Long.parseLong(value); }
        catch (RuntimeException unavailable) { return -1; }
    }
    private static long median(long[] samples) { long[] sorted = samples.clone(); Arrays.sort(sorted); return sorted[sorted.length / 2]; }
    private static final class Workspace implements AutoCloseable {
        final Path parent, root;
        Workspace(boolean external) throws Exception {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            File directory = external ? context.getExternalFilesDir(null) : context.getCacheDir(); assertNotNull(directory);
            parent = directory.getCanonicalFile().toPath(); root = Files.createTempDirectory(parent, "file-util-read-");
        }
        @Override public void close() throws Exception {
            assertEquals(parent, root.toRealPath().getParent());
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
            }
        }
    }
}
