package pro.sketchware.util;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RunWith(AndroidJUnit4.class)
public class FileUtilIoTest {
    @Test public void binaryCopiesHandleEmptyFilesBufferBoundariesAndTruncation() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path source = workspace.root.resolve("source.bin");
            Path target = workspace.root.resolve("target.bin");
            for (int size : new int[]{0, 1, 1023, 1024, 2047, 2048, 8191, 8192, 32767, 32768, 32769, 1048583}) {
                byte[] content = data(size);
                Files.write(source, content);
                Files.write(target, new byte[size + 11]);
                FileUtil.copyFile(source.toString(), target.toString());
                assertArrayEquals("copyFile size=" + size, content, Files.readAllBytes(target));
                Files.write(target, new byte[size + 11]);
                FileUtil.copyDirectory(source.toFile(), target.toFile());
                assertArrayEquals("copyDirectory size=" + size, content, Files.readAllBytes(target));
            }
        }
    }

    @Test public void recursiveCopyMergesNestedFilesAndKeepsEmptyAndUnrelatedFolders() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path source = Files.createDirectories(workspace.root.resolve("source/nested/empty")).getParent().getParent();
            byte[] xml = "<?xml version=\"1.0\"?>\r\n<data>中文 &amp; 😀</data>\r\n".getBytes(StandardCharsets.UTF_8);
            Files.write(source.resolve("nested/data.xml"), xml);
            Files.write(source.resolve("large.bin"), data(1048583));
            Path target = Files.createDirectories(workspace.root.resolve("target/nested")).getParent();
            Files.write(target.resolve("keep.bin"), new byte[]{42});
            Files.write(target.resolve("nested/data.xml"), new byte[xml.length + 20]);
            FileUtil.copyDirectory(source.toFile(), target.toFile());
            assertArrayEquals(xml, Files.readAllBytes(target.resolve("nested/data.xml")));
            assertArrayEquals(data(1048583), Files.readAllBytes(target.resolve("large.bin")));
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target.resolve("keep.bin")));
            assertTrue(Files.isDirectory(target.resolve("nested/empty")));
        }
    }

    @Test public void zipExtractionKeepsEntryBoundariesAndLeavesTheInputOpen() throws Exception {
        byte[][] contents = {new byte[0], data(65539), "第二个非空条目\r\n".getBytes(StandardCharsets.UTF_8)};
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < contents.length; i++) {
                zip.putNextEntry(new ZipEntry(i + ".bin"));
                zip.write(contents[i]);
                zip.closeEntry();
            }
        }
        try (Workspace workspace = new Workspace(false);
             TrackingZipInput input = new TrackingZipInput(bytes.toByteArray())) {
            for (int i = 0; i < contents.length; i++) {
                assertEquals(i + ".bin", input.getNextEntry().getName());
                Path output = workspace.root.resolve(i + ".bin");
                FileUtil.extractFileFromZip(input, output.toFile());
                assertFalse("Caller owns the archive stream", input.closed);
                assertArrayEquals(contents[i], Files.readAllBytes(output));
            }
            assertNull(input.getNextEntry());
        }
    }

    @Test public void missingSourcesAndDestinationErrorsKeepTheirExistingBehavior() throws Exception {
        try (Workspace workspace = new Workspace(false)) {
            Path missing = workspace.root.resolve("missing.bin");
            Path target = workspace.root.resolve("target.bin");
            Files.write(target, new byte[]{42});
            FileUtil.copyFile(missing.toString(), target.toString());
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target));
            try {
                FileUtil.copyDirectory(missing.toFile(), target.toFile());
                fail("The throwing copy API must still report unavailable sources");
            } catch (IOException expected) {}
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target));
            Path directory = Files.createDirectory(workspace.root.resolve("directory"));
            try {
                FileUtil.copyDirectory(target.toFile(), directory.toFile());
                fail("A directory cannot be overwritten by a file");
            } catch (IOException expected) {}
            assertTrue(Files.isDirectory(directory));
        }
    }

    @Test public void emptyAndNullPathsAreIgnoredByExistenceAndCreationHelpers() throws Exception {
        assertFalse(FileUtil.isExistFile(null));
        assertFalse(FileUtil.isExistFile(""));
        FileUtil.createNewFileIfNotPresent(null);
        FileUtil.createNewFileIfNotPresent("");
        FileUtil.makeDir(null);
        FileUtil.makeDir("");
        assertEquals("", FileUtil.readFile(null));
        assertEquals("", FileUtil.readFileIfExist(null));
        try (Workspace workspace = new Workspace(false)) {
            Path target = workspace.root.resolve("nested/keep.txt");
            FileUtil.createNewFileIfNotPresent(target.toString());
            Files.write(target, new byte[]{42});
            FileUtil.createNewFileIfNotPresent(target.toString());
            FileUtil.makeDir(target.getParent().toString());
            assertTrue(FileUtil.isExistFile(target.toString()));
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(target));
        }
    }

    /** Reports timings without a flaky speed threshold; all final files must match the source hash. */
    @Test(timeout = 120000) public void benchmarkLargeCopiesOnExternalStorage() throws Exception {
        try (Workspace workspace = new Workspace(true)) {
            byte[] content = data(16 * 1024 * 1024 + 7);
            Path source = workspace.root.resolve("source.bin");
            Path archive = workspace.root.resolve("source.zip");
            Files.write(source, content);
            CRC32 crc = new CRC32();
            crc.update(content);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
                ZipEntry entry = new ZipEntry("data.bin");
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(content.length);
                entry.setCrc(crc.getValue());
                zip.putNextEntry(entry);
                zip.write(content);
                zip.closeEntry();
            }
            Path[] targets = {workspace.root.resolve("file.bin"), workspace.root.resolve("directory.bin"), workspace.root.resolve("zip.bin")};
            long[][] times = new long[3][9];
            for (int round = -2; round < 9; round++) {
                for (int offset = 0; offset < 3; offset++) {
                    int operation = Math.floorMod(round + offset, 3);
                    long elapsed;
                    if (operation == 2) {
                        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
                            assertNotNull(zip.getNextEntry());
                            long start = System.nanoTime();
                            FileUtil.extractFileFromZip(zip, targets[operation].toFile());
                            elapsed = System.nanoTime() - start;
                        }
                    } else {
                        long start = System.nanoTime();
                        if (operation == 0) FileUtil.copyFile(source.toString(), targets[operation].toString());
                        else FileUtil.copyDirectory(source.toFile(), targets[operation].toFile());
                        elapsed = System.nanoTime() - start;
                    }
                    if (round >= 0) times[operation][round] = elapsed;
                }
            }
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
            for (Path target : targets) {
                assertArrayEquals(hash, digest(target));
                assertEquals(content.length, Files.size(target));
            }
            Bundle result = new Bundle();
            result.putString("io_benchmark", "16MiB+7 bytes, external app storage, warm cache, 2 warmups + 9 samples, no fsync");
            String[] names = {"copy_file", "copy_directory_file", "extract_zip_entry"};
            for (int i = 0; i < times.length; i++) {
                result.putString(names[i] + "_samples_ns", Arrays.toString(times[i]));
                long[] sorted = times[i].clone();
                Arrays.sort(sorted);
                result.putString(names[i] + "_median_ms", Double.toString(sorted[4] / 1_000_000.0));
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, result);
        }
    }

    private static byte[] digest(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return digest.digest();
    }

    private static byte[] data(int size) {
        byte[] data = new byte[size];
        new Random(0x42751).nextBytes(data);
        return data;
    }

    private static final class TrackingZipInput extends ZipInputStream {
        boolean closed;
        TrackingZipInput(byte[] bytes) { super(new ByteArrayInputStream(bytes)); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    private static final class Workspace implements AutoCloseable {
        final Path parent;
        final Path root;
        Workspace(boolean external) throws IOException {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            File directory = external ? context.getExternalFilesDir(null) : context.getCacheDir();
            assertNotNull(directory);
            parent = directory.getCanonicalFile().toPath();
            root = Files.createTempDirectory(parent, "file-util-io-");
        }
        @Override public void close() throws IOException {
            assertEquals(parent, root.toRealPath().getParent());
            try (var entries = Files.walk(root)) {
                for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(entry);
            }
        }
    }
}
