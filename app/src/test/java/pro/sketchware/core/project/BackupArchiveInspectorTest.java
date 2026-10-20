package pro.sketchware.core.project;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupArchiveInspectorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void explicitLibraryDirectoryIsRecognized() throws Exception {
        assertTrue(BackupArchiveInspector.containsEntry(archive(Map.of("local_libs/", new byte[0])), "local_libs"));
    }

    @Test public void implicitDirectoriesAreRecognizedUsingZipSeparators() throws Exception {
        assertTrue(BackupArchiveInspector.containsEntry(archive(Map.of("local_libs/example/config", new byte[]{1})), "local_libs"));
    }

    @Test public void anExactRootEntryKeepsTheExistingMatchingBehavior() throws Exception {
        assertTrue(BackupArchiveInspector.containsEntry(archive(Map.of("local_libs", new byte[]{1})), "local_libs"));
    }

    @Test public void similarNamesNestedFoldersAndPayloadTextAreNotMatches() throws Exception {
        File archive = archive(Map.of("local_libs_copy/config", new byte[]{1}, "data/local_libs/config", new byte[]{2},
                "data/project", "local_libs/example".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertFalse(BackupArchiveInspector.containsEntry(archive, "local_libs"));
    }

    @Test public void anEmptyZipHasNoIntegratedLibraries() throws Exception {
        assertFalse(BackupArchiveInspector.containsEntry(archive(Map.of()), "local_libs"));
    }

    @Test public void missingAndInvalidArchivesAreErrorsRatherThanNegativeMatches() throws Exception {
        assertThrows(IOException.class, () -> BackupArchiveInspector.containsEntry(new File(folder.getRoot(), "missing.swb"), "local_libs"));
        File invalid = folder.newFile("invalid.swb");
        Files.write(invalid.toPath(), new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> BackupArchiveInspector.containsEntry(invalid, "local_libs"));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(invalid.toPath()));
    }

    @Test public void searchingNamesDoesNotDecodeEarlierPayloads() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("data/large", new byte[65536]);
        entries.put("local_libs/example/config", new byte[]{1});
        File archive = archive(entries);
        byte[] bytes = Files.readAllBytes(archive.toPath());
        int payload = 30 + ((bytes[26] & 255) | (bytes[27] & 255) << 8)
                + ((bytes[28] & 255) | (bytes[29] & 255) << 8);
        bytes[payload] = 7; // Reserved DEFLATE block. The ZIP directory itself is intact.
        Files.write(archive.toPath(), bytes);
        assertTrue(BackupArchiveInspector.containsEntry(archive, "local_libs"));
        assertFalse(BackupArchiveInspector.containsEntry(archive, "absent"));
    }

    private File archive(Map<String, byte[]> entries) throws Exception {
        File archive = folder.newFile("backup.swb");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive.toPath()))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return archive;
    }
}
