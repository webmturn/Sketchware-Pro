package pro.sketchware.activities.main.fragments.projects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BackupFactoryCopyTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void copiesNestedFilesAndSkipsNomedia() throws Exception {
        File source = folder.newFolder("source");
        File nested = new File(source, "nested");
        assertTrue(nested.mkdir());
        Files.write(new File(nested, "data").toPath(), new byte[] {1, 2, 3});
        Files.write(new File(source, ".nomedia").toPath(), new byte[0]);
        File destination = new File(folder.getRoot(), "destination");

        BackupFactory.copy(source, destination);

        assertArrayEquals(
                new byte[] {1, 2, 3},
                Files.readAllBytes(new File(destination, "nested/data").toPath()));
        assertFalse(new File(destination, ".nomedia").exists());
    }

    @Test
    public void missingSourceReportsFailureWithoutTruncatingDestination() throws Exception {
        File destination = folder.newFile("destination");
        Files.write(destination.toPath(), new byte[] {9});

        assertThrows(
                IOException.class,
                () -> BackupFactory.copy(new File(folder.getRoot(), "missing"), destination));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(destination.toPath()));
        assertEquals(1, folder.getRoot().list().length);
    }

    @Test
    public void fileBlockingADirectoryReportsFailure() throws Exception {
        File source = folder.newFolder("source");
        Files.write(new File(source, "data").toPath(), new byte[] {1});
        File destination = folder.newFile("destination");
        Files.write(destination.toPath(), new byte[] {9});

        assertThrows(IOException.class, () -> BackupFactory.copy(source, destination));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(destination.toPath()));
    }

    @Test
    public void unreadableDirectoryReportsFailure() throws Exception {
        File source =
                new File(folder.newFolder("source").getPath()) {
                    @Override
                    public String[] list() {
                        return null;
                    }
                };

        assertThrows(
                IOException.class,
                () -> BackupFactory.copy(source, new File(folder.getRoot(), "destination")));
    }

    @Test
    public void failureAfterOneCopiedFilePropagates() throws Exception {
        File source =
                new File(folder.newFolder("source").getPath()) {
                    @Override
                    public String[] list() {
                        return new String[] {"first", "missing"};
                    }
                };
        Files.write(new File(source, "first").toPath(), new byte[] {1});
        File destination = new File(folder.getRoot(), "destination");

        assertThrows(IOException.class, () -> BackupFactory.copy(source, destination));

        assertArrayEquals(
                new byte[] {1}, Files.readAllBytes(new File(destination, "first").toPath()));
        assertFalse(new File(destination, "missing").exists());
        assertEquals(1, destination.list().length);
    }

    @Test
    public void successfulCopyReplacesAnExistingFile() throws Exception {
        File source = folder.newFile("source");
        File destination = folder.newFile("destination");
        Files.write(source.toPath(), new byte[] {1, 2});
        Files.write(destination.toPath(), new byte[] {9});

        BackupFactory.copy(source, destination);

        assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(destination.toPath()));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test
    public void failedReplacementPreservesDirectoryAndRemovesPendingCopy() throws Exception {
        File source = folder.newFile("source");
        Files.write(source.toPath(), new byte[] {1});
        File destination = folder.newFolder("destination");
        File existing = new File(destination, "keep");
        Files.write(existing.toPath(), new byte[] {9});

        assertThrows(IOException.class, () -> BackupFactory.copy(source, destination));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(existing.toPath()));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test
    public void absentOptionalResourcesStillProduceANomediaDirectory() throws Exception {
        File destination = new File(folder.getRoot(), "optional");

        BackupFactory.copySafe(new File(folder.getRoot(), "missing"), destination);

        assertTrue(destination.isDirectory());
        assertTrue(new File(destination, ".nomedia").isFile());
    }

    @Test
    public void unreadableZipInputReportsFailure() throws Exception {
        File source =
                new File(folder.newFolder("source").getPath()) {
                    @Override
                    public File[] listFiles() {
                        return null;
                    }
                };

        assertThrows(
                IOException.class,
                () -> BackupFactory.zipFolder(source, new File(folder.getRoot(), "output.zip")));
    }
}
