package pro.sketchware.activities.editor.manage.image.fragments;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ImageSaveFiles implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(ImageSaveFiles.class.getName());

    private record Original(Path target, Path backup) {}

    private final Path stagingParent;
    private final Map<Path, Path> targetsByStagedFile = new LinkedHashMap<>();
    private final List<Original> originals = new ArrayList<>();
    private final List<Path> createdDirectories = new ArrayList<>();
    private Path staging;
    private boolean keepStaging;

    ImageSaveFiles(Path stagingParent) {
        this.stagingParent = stagingParent;
    }

    Path stage(Path target) throws IOException {
        // The SVG converter derives the drawable name from the staged source filename.
        Path file = stagingDirectory().resolve(target.getFileName());
        Path previousTarget = targetsByStagedFile.putIfAbsent(file, target);
        if (previousTarget != null) {
            throw new IOException(
                    "Image output name already staged: " + previousTarget + " and " + target);
        }
        return file;
    }

    void publish() throws IOException {
        for (var file : targetsByStagedFile.entrySet()) {
            Path staged = file.getKey();
            Path target = file.getValue();
            createDirectory(target.getParent());
            rememberOriginal(target);
            try {
                Files.move(
                        staged,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    void rememberOriginal(Path target) throws IOException {
        BasicFileAttributes attributes;
        try {
            attributes =
                    Files.readAttributes(
                            target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException missing) {
            originals.add(new Original(target, null));
            return;
        }
        if (!attributes.isRegularFile()) {
            throw new IOException("Image save target is not a regular file: " + target);
        }
        Path backup = Files.createTempFile(stagingDirectory(), "previous-", ".tmp");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        originals.add(new Original(target, backup));
    }

    void rollback(Exception failure) {
        for (int i = originals.size() - 1; i >= 0; i--) {
            Original original = originals.get(i);
            try {
                if (original.backup() == null) {
                    Files.deleteIfExists(original.target());
                } else {
                    Files.copy(
                            original.backup(),
                            original.target(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException | RuntimeException error) {
                keepStaging = true;
                failure.addSuppressed(
                        new IOException(
                                "Could not restore "
                                        + original.target()
                                        + " from "
                                        + original.backup(),
                                error));
            }
        }
        for (int i = createdDirectories.size() - 1; i >= 0; i--) {
            try {
                Files.deleteIfExists(createdDirectories.get(i));
            } catch (IOException | RuntimeException error) {
                failure.addSuppressed(error);
            }
        }
    }

    private Path stagingDirectory() throws IOException {
        if (staging == null) {
            staging =
                    Files.createTempDirectory(
                            Files.createDirectories(stagingParent), ".image-save-");
        }
        return staging;
    }

    private void createDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            createDirectory(directory.getParent());
            Files.createDirectory(directory);
            createdDirectories.add(directory);
        }
    }

    @Override
    public void close() {
        if (staging == null || keepStaging) {
            return;
        }
        try {
            try (var files = Files.newDirectoryStream(staging)) {
                for (Path file : files) {
                    Files.deleteIfExists(file);
                }
            }
            Files.deleteIfExists(staging);
        } catch (IOException | RuntimeException error) {
            LOG.log(Level.WARNING, "Could not remove " + staging, error);
        }
    }
}
