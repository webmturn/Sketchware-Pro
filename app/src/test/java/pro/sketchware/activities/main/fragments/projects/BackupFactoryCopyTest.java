package pro.sketchware.activities.main.fragments.projects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
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
    public void unsupportedAtomicMoveFallsBackForNewAndExistingFiles() throws Exception {
        for (boolean existing : new boolean[] {false, true}) {
            Path directory = folder.newFolder().toPath();
            Path pending = Files.write(directory.resolve("pending"), new byte[] {1, 2});
            Path target = directory.resolve("target");
            if (existing) Files.write(target, new byte[] {9});
            int[] attempts = {0};

            BackupFactory.replaceFile(
                    pending,
                    target,
                    (source, destination, options) -> {
                        attempts[0]++;
                        rejectAtomicMove(source, destination, options);
                        Files.move(source, destination, options);
                    });

            assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(target));
            assertFalse(Files.exists(pending));
            assertEquals(2, attempts[0]);
            assertEquals(1, directory.toFile().list().length);
        }
    }

    @Test
    public void otherMoveFailuresDoNotTriggerFallback() throws Exception {
        Path pending = Files.write(folder.getRoot().toPath().resolve("pending"), new byte[] {1});
        Path target = Files.write(folder.getRoot().toPath().resolve("target"), new byte[] {9});
        IOException expected = new IOException("Move denied");
        int[] attempts = {0};

        IOException failure =
                assertThrows(
                        IOException.class,
                        () ->
                                BackupFactory.replaceFile(
                                        pending,
                                        target,
                                        (source, destination, options) -> {
                                            attempts[0]++;
                                            throw expected;
                                        }));

        assertSame(expected, failure);
        assertEquals(1, attempts[0]);
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(target));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test
    public void failedNonAtomicReplacementRestoresThePreviousContent() throws Exception {
        Path pending = Files.write(folder.getRoot().toPath().resolve("pending"), new byte[] {1});
        Path target = Files.write(folder.getRoot().toPath().resolve("target"), new byte[] {9});
        IOException expected = new IOException("Replacement interrupted");

        IOException failure =
                assertThrows(
                        IOException.class,
                        () ->
                                BackupFactory.replaceFile(
                                        pending,
                                        target,
                                        (source, destination, options) -> {
                                            rejectAtomicMove(source, destination, options);
                                            Files.write(destination, new byte[0]);
                                            throw expected;
                                        }));

        assertSame(expected, failure);
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(target));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(pending));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test
    public void failedNonAtomicMoveRemovesAPartialNewTarget() throws Exception {
        Path pending = Files.write(folder.getRoot().toPath().resolve("pending"), new byte[] {1});
        Path target = folder.getRoot().toPath().resolve("target");

        assertThrows(
                IOException.class,
                () ->
                        BackupFactory.replaceFile(
                                pending,
                                target,
                                (source, destination, options) -> {
                                    rejectAtomicMove(source, destination, options);
                                    Files.write(destination, new byte[] {0});
                                    throw new IOException("Move interrupted");
                                }));

        assertFalse(Files.exists(target));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(pending));
        assertEquals(1, folder.getRoot().list().length);
    }

    @Test
    public void failedRollbackKeepsTheCompletePreviousFileForRecovery() throws Exception {
        Path pending = Files.write(folder.getRoot().toPath().resolve("pending"), new byte[] {1});
        Path target = Files.write(folder.getRoot().toPath().resolve("target"), new byte[] {9});

        IOException failure =
                assertThrows(
                        IOException.class,
                        () ->
                                BackupFactory.replaceFile(
                                        pending,
                                        target,
                                        (source, destination, options) -> {
                                            rejectAtomicMove(source, destination, options);
                                            Files.delete(destination);
                                            Files.createDirectory(destination);
                                            Files.write(
                                                    destination.resolve("blocked"), new byte[] {0});
                                            throw new IOException("Move interrupted");
                                        }));

        List<Path> previous;
        try (Stream<Path> paths = Files.list(folder.getRoot().toPath())) {
            previous =
                    paths.filter(
                                    path ->
                                            path.getFileName()
                                                    .toString()
                                                    .startsWith(".backup-previous-"))
                            .toList();
        }
        assertEquals(1, previous.size());
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(previous.get(0)));
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0].getMessage().contains(previous.get(0).toString()));
    }

    @Test
    public void fallbackRefusesToReplaceADirectoryWithAFile() throws Exception {
        Path pending = Files.write(folder.getRoot().toPath().resolve("pending"), new byte[] {1});
        Path target = folder.newFolder("target").toPath();
        Path keep = Files.write(target.resolve("keep"), new byte[] {9});

        assertThrows(
                IOException.class,
                () ->
                        BackupFactory.replaceFile(
                                pending,
                                target,
                                (source, destination, options) -> {
                                    rejectAtomicMove(source, destination, options);
                                    Files.move(source, destination, options);
                                }));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(keep));
        assertEquals(2, folder.getRoot().list().length);
    }

    private static void rejectAtomicMove(Path source, Path target, CopyOption... options)
            throws AtomicMoveNotSupportedException {
        if (Arrays.asList(options).contains(StandardCopyOption.ATOMIC_MOVE)) {
            throw new AtomicMoveNotSupportedException(
                    source.toString(),
                    target.toString(),
                    "Test provider does not support atomic moves");
        }
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
