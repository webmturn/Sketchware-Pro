package pro.sketchware.core.project;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssetImporterTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void binaryAndEmptyFilesKeepTheirExactBytes() throws Exception {
        File destination = folder.newFolder();
        File binary = folder.newFile("data.bin");
        byte[] bytes = new byte[65539];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        Files.write(binary.toPath(), bytes);
        Files.write(destination.toPath().resolve("data.bin"), new byte[]{42});
        AssetImporter.importEntry(binary, destination);
        AssetImporter.importEntry(folder.newFile("empty"), destination);
        assertArrayEquals(bytes, Files.readAllBytes(destination.toPath().resolve("data.bin")));
        assertArrayEquals(bytes, Files.readAllBytes(binary.toPath()));
        assertEquals(0, Files.size(destination.toPath().resolve("empty")));
        assertEquals(2, destination.listFiles().length);
    }

    @Test public void directoriesMergeAndKeepEmptyAndUnselectedChildren() throws Exception {
        File source = folder.newFolder("bundle");
        Files.createDirectories(source.toPath().resolve("nested/empty"));
        Files.write(source.toPath().resolve("nested/data.bin"), new byte[]{0, -1});
        File destination = folder.newFolder();
        Path output = destination.toPath().resolve("bundle");
        Files.createDirectories(output.resolve("nested"));
        Files.write(output.resolve("nested/data.bin"), new byte[]{42});
        Files.write(output.resolve("keep"), new byte[]{7});
        AssetImporter.importEntry(source, destination);
        assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(output.resolve("nested/data.bin")));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(output.resolve("keep")));
        assertTrue(Files.isDirectory(output.resolve("nested/empty")));
    }

    @Test public void importingFilesAndFoldersInPlaceIsANoOp() throws Exception {
        File source = folder.newFolder("bundle");
        Path file = source.toPath().resolve("keep");
        Files.write(file, new byte[]{0, -1});
        AssetImporter.importEntry(file.toFile(), source);
        AssetImporter.importEntry(source, source.getParentFile());
        assertArrayEquals(new byte[]{0, -1}, Files.readAllBytes(file));
        assertEquals(1, source.listFiles().length);
    }

    @Test public void aDirectoryCannotBeCopiedIntoItsOwnDescendants() throws Exception {
        File source = folder.newFolder("bundle");
        Path nested = Files.createDirectory(source.toPath().resolve("nested"));
        assertThrows(IOException.class, () -> AssetImporter.importEntry(source, nested.toFile()));
        assertFalse(Files.exists(nested.resolve("bundle")));
    }

    @Test public void aDirectoryCannotBeMergedIntoItsAncestor() throws Exception {
        File parent = folder.newFolder("bundle");
        Path source = Files.createDirectories(parent.toPath().resolve("nested/bundle"));
        Files.write(parent.toPath().resolve("keep"), new byte[]{7});
        assertThrows(IOException.class, () -> AssetImporter.importEntry(source.toFile(), parent.getParentFile()));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(parent.toPath().resolve("keep")));
        assertEquals(0, source.toFile().listFiles().length);
    }

    @Test public void missingSourcesDoNotReplaceExistingFiles() throws Exception {
        File destination = folder.newFolder();
        Path target = destination.toPath().resolve("missing");
        Files.write(target, new byte[]{7});
        assertThrows(IOException.class, () -> AssetImporter.importEntry(new File(folder.getRoot(), "missing"), destination));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(target));
        assertEquals(1, destination.listFiles().length);
    }

    @Test public void removedDestinationFoldersAreNotRecreated() throws Exception {
        File source = folder.newFile("data.bin");
        File missing = new File(folder.getRoot(), "missing");
        assertThrows(IOException.class, () -> AssetImporter.importEntry(source, missing));
        assertFalse(missing.exists());
    }

    @Test public void fileAndDirectoryConflictsKeepTheExistingContents() throws Exception {
        File destination = folder.newFolder();
        File file = folder.newFile("data");
        Path existingDirectory = Files.createDirectory(destination.toPath().resolve("data"));
        Files.write(existingDirectory.resolve("keep"), new byte[]{7});
        assertThrows(IOException.class, () -> AssetImporter.importEntry(file, destination));
        assertArrayEquals(new byte[]{7}, Files.readAllBytes(existingDirectory.resolve("keep")));
        File directory = folder.newFolder("bundle");
        Path existingFile = destination.toPath().resolve("bundle");
        Files.write(existingFile, new byte[]{42});
        assertThrows(IOException.class, () -> AssetImporter.importEntry(directory, destination));
        assertArrayEquals(new byte[]{42}, Files.readAllBytes(existingFile));
        assertEquals(2, destination.listFiles().length);
    }
}
