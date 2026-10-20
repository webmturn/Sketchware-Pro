package pro.sketchware.core.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Merges asset folders, replacing each file only after its entire copy succeeds. */
public final class AssetImporter {
    private AssetImporter() {}

    public static void importEntry(File source, File destination) throws IOException {
        Path selected = source.toPath();
        if (Files.isSymbolicLink(selected)) throw new IOException("Symbolic links cannot be imported");
        Path sourcePath = selected.toRealPath();
        Path destinationPath = destination.toPath().toRealPath();
        if (!Files.isDirectory(destinationPath)) throw new IOException("Destination directory is unavailable");
        String name = source.getName();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) throw new IOException("Invalid asset name");
        Path target = destinationPath.resolve(name);
        if (Files.isSymbolicLink(target)) throw new IOException("Destination is a symbolic link");
        if (sourcePath.equals(target)) return;
        if (Files.isDirectory(sourcePath)
                && (target.startsWith(sourcePath) || sourcePath.startsWith(target))) {
            throw new IOException("Source and destination folders overlap");
        }

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            private Path targetOf(Path entry) { return target.resolve(sourcePath.relativize(entry)); }

            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path output = targetOf(dir);
                if (Files.isSymbolicLink(output)) throw new IOException("Destination is a symbolic link");
                if (!Files.exists(output, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(output);
                else if (!Files.isDirectory(output, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Destination is not a directory");
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isRegularFile()) throw new IOException("Source is not a regular file");
                copyFile(file, targetOf(file));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyFile(Path source, Path target) throws IOException {
        if (Files.isSymbolicLink(target) || Files.isDirectory(target)) throw new IOException("Destination is not a regular file");
        Path pending = Files.createTempFile(target.getParent(), ".asset-import-", ".tmp");
        try {
            Files.copy(source, pending, StandardCopyOption.REPLACE_EXISTING);
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
