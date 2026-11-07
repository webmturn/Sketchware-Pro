package pro.sketchware.activities.editor.makeblock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MoreblockResourceFilesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void copiesAllResourcesWithoutChangingTheirSources() throws Exception {
        Path first = Files.write(folder.newFile("image.png").toPath(), new byte[] {1, 2});
        Path second = Files.write(folder.newFile("font.ttf").toPath(), new byte[] {3, 4});
        Path output = folder.newFolder("output").toPath();

        MoreblockResourceFiles.copyAll(
                List.of(
                        new MoreblockResourceFiles.Copy(first, output.resolve("image.png")),
                        new MoreblockResourceFiles.Copy(second, output.resolve("font.ttf"))));

        assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(output.resolve("image.png")));
        assertArrayEquals(new byte[] {3, 4}, Files.readAllBytes(output.resolve("font.ttf")));
        assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(first));
        assertArrayEquals(new byte[] {3, 4}, Files.readAllBytes(second));
    }

    @Test
    public void missingLaterSourceRemovesAllFilesCreatedByTheBatch() throws Exception {
        Path first = Files.write(folder.newFile("image.png").toPath(), new byte[] {1});
        Path output = folder.newFolder("output").toPath();
        Path keep = Files.write(output.resolve("keep"), new byte[] {9});

        assertThrows(
                IOException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(
                                        new MoreblockResourceFiles.Copy(
                                                first, output.resolve("image.png")),
                                        new MoreblockResourceFiles.Copy(
                                                folder.getRoot().toPath().resolve("missing.ttf"),
                                                output.resolve("font.ttf")))));

        assertFalse(Files.exists(output.resolve("image.png")));
        assertFalse(Files.exists(output.resolve("font.ttf")));
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(keep));
        assertEquals(1, output.toFile().list().length);
    }

    @Test
    public void existingTargetIsPreservedAndEarlierNewFilesAreRemoved() throws Exception {
        Path source = Files.write(folder.newFile("source").toPath(), new byte[] {1});
        Path output = folder.newFolder("output").toPath();
        Path existing = Files.write(output.resolve("existing"), new byte[] {9});

        assertThrows(
                IOException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(
                                        new MoreblockResourceFiles.Copy(
                                                source, output.resolve("first")),
                                        new MoreblockResourceFiles.Copy(source, existing))));

        assertFalse(Files.exists(output.resolve("first")));
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(existing));
        assertEquals(1, output.toFile().list().length);
    }

    @Test
    public void readFailureRemovesTheCurrentIncompleteFile() throws Exception {
        Path source = folder.newFolder("directory").toPath();
        Path output = folder.newFolder("output").toPath();
        Path target = output.resolve("font.ttf");

        assertThrows(
                IOException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(new MoreblockResourceFiles.Copy(source, target))));

        assertFalse(Files.exists(target));
        assertEquals(0, output.toFile().list().length);
    }

    @Test
    public void duplicateTargetsFailWithoutLeavingANewFile() throws Exception {
        Path source = Files.write(folder.newFile("source").toPath(), new byte[] {1});
        Path output = folder.newFolder("output").toPath();
        Path target = output.resolve("same");

        assertThrows(
                IOException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(
                                        new MoreblockResourceFiles.Copy(source, target),
                                        new MoreblockResourceFiles.Copy(source, target))));

        assertFalse(Files.exists(target));
        assertEquals(0, output.toFile().list().length);
    }

    @Test
    public void cancellingBeforeCopyCreatesNothing() throws Exception {
        Path source = Files.write(folder.newFile("source").toPath(), new byte[] {1});
        Path output = folder.newFolder("output").toPath();

        assertThrows(
                CancellationException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(
                                        new MoreblockResourceFiles.Copy(
                                                source, output.resolve("image"))),
                                () -> true));

        assertEquals(0, output.toFile().list().length);
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(source));
    }

    @Test
    public void cancellingDuringCopyRemovesThePartialFile() throws Exception {
        byte[] content = new byte[65536];
        Path source = Files.write(folder.newFile("source").toPath(), content);
        Path target = folder.newFolder("output").toPath().resolve("image");
        AtomicLong copied = new AtomicLong();

        assertThrows(
                CancellationException.class,
                () ->
                        MoreblockResourceFiles.copyAll(
                                List.of(new MoreblockResourceFiles.Copy(source, target)),
                                () -> {
                                    try {
                                        copied.set(Files.size(target));
                                        return copied.get() > 0;
                                    } catch (NoSuchFileException absent) {
                                        return false;
                                    } catch (IOException failure) {
                                        throw new UncheckedIOException(failure);
                                    }
                                }));

        assertTrue(copied.get() > 0 && copied.get() < content.length);
        assertFalse(Files.exists(target));
        assertArrayEquals(content, Files.readAllBytes(source));
    }

    @Test
    public void copiedFilesCanBeRolledBackWhenTheProjectRejectsTheImport() throws Exception {
        Path source = Files.write(folder.newFile("source").toPath(), new byte[] {1});
        Path output = folder.newFolder("output").toPath();
        Path existing = Files.write(output.resolve("keep"), new byte[] {9});
        Path target = output.resolve("image");

        List<Path> created =
                MoreblockResourceFiles.copyAll(
                        List.of(new MoreblockResourceFiles.Copy(source, target)));
        assertEquals(List.of(target), created);
        MoreblockResourceFiles.rollback(created);

        assertFalse(Files.exists(target));
        assertArrayEquals(new byte[] {9}, Files.readAllBytes(existing));
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(source));
    }
}
