package pro.sketchware.activities.editor.manage.image.fragments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ImageSaveFilesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void rejectsTwoTargetsWithTheSameFileNameBeforePublishing() throws IOException {
        Path root = folder.getRoot().toPath();
        Path first = root.resolve("images/icon.png");
        Path second = root.resolve("thumbs/icon.png");
        Path staged;
        try (var files = new ImageSaveFiles(root)) {
            staged = write(files.stage(first), "first image");

            IOException collision = assertThrows(IOException.class, () -> files.stage(second));
            assertTrue(collision.getMessage().contains(first.toString()));
            assertTrue(collision.getMessage().contains(second.toString()));
            assertThrows(IOException.class, () -> files.stage(first));
            assertEquals("first image", read(staged));
            assertFalse(Files.exists(first));
            assertFalse(Files.exists(second));
        }
        assertFalse(Files.exists(staged.getParent()));
    }

    @Test
    public void publishMovesFilesAndCloseRemovesStaging() throws IOException {
        Path root = folder.getRoot().toPath();
        Path image = root.resolve("images/icon.xml");
        Path svg = root.resolve("converted-vectors/icon.svg");
        Path staged;
        try (var files = new ImageSaveFiles(root)) {
            staged = write(files.stage(image), "vector");
            write(files.stage(svg), "source");
            files.publish();
        }
        assertEquals("vector", read(image));
        assertEquals("source", read(svg));
        assertFalse(Files.exists(staged.getParent()));
    }

    @Test
    public void failedPublishRestoresOverwrittenFilesAndRemovesNewDirectories() throws IOException {
        Path root = folder.getRoot().toPath();
        Path original = write(folder.newFile("original.png").toPath(), "old");
        Path added = root.resolve("converted-vectors/new.svg");
        Path blocked = folder.newFolder("blocked.xml").toPath();
        Path staged;
        try (var files = new ImageSaveFiles(root)) {
            staged = write(files.stage(original), "changed");
            write(files.stage(added), "added");
            write(files.stage(blocked), "blocked");

            IOException failure = assertThrows(IOException.class, files::publish);
            assertEquals("changed", read(original));
            assertEquals("added", read(added));
            files.rollback(failure);
            assertEquals(0, failure.getSuppressed().length);
        }
        assertEquals("old", read(original));
        assertFalse(Files.exists(added.getParent()));
        assertTrue(Files.isDirectory(blocked));
        assertFalse(Files.exists(staged.getParent()));
    }

    private static Path write(Path path, String content) throws IOException {
        return Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
