package pro.sketchware.activities.editor.makeblock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

final class MoreblockResourceFiles {
    record Copy(Path source, Path target) {}

    private MoreblockResourceFiles() {}

    static List<Path> copyAll(List<Copy> copies) throws IOException {
        return copyAll(copies, () -> false);
    }

    static List<Path> copyAll(List<Copy> copies, BooleanSupplier cancelled) throws IOException {
        ArrayList<Path> created = new ArrayList<>();
        byte[] buffer = new byte[8192];
        try {
            for (Copy copy : copies) {
                checkCancelled(cancelled);
                Files.createDirectories(copy.target().getParent());
                try (InputStream input = Files.newInputStream(copy.source());
                        OutputStream output =
                                Files.newOutputStream(
                                        copy.target(),
                                        StandardOpenOption.CREATE_NEW,
                                        StandardOpenOption.WRITE)) {
                    created.add(copy.target());
                    int count;
                    while (true) {
                        checkCancelled(cancelled);
                        count = input.read(buffer);
                        if (count == -1) break;
                        output.write(buffer, 0, count);
                    }
                }
            }
            checkCancelled(cancelled);
            return List.copyOf(created);
        } catch (IOException | RuntimeException failure) {
            try {
                rollback(created);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    static void rollback(List<Path> created) throws IOException {
        IOException failure = null;
        for (int i = created.size() - 1; i >= 0; i--) {
            try {
                Files.deleteIfExists(created.get(i));
            } catch (IOException | RuntimeException cleanupFailure) {
                if (failure == null)
                    failure = new IOException("Could not remove imported resources");
                failure.addSuppressed(cleanupFailure);
            }
        }
        if (failure != null) throw failure;
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException("MoreBlock import cancelled");
    }
}
