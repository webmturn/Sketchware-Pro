package pro.sketchware.activities.resourceseditor;

import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/** Checked, same-directory atomic replacement. Never truncates the existing resource on failure. */
public final class ResourceXmlWriter {
    private ResourceXmlWriter() { }

    /** Optional test observer; the normal path does not time or wrap individual writes. */
    @androidx.annotation.VisibleForTesting
    static volatile java.util.function.BiConsumer<String, Long> testTimings;

    interface Output { void write(Writer writer) throws IOException; }
    interface BytesOutput { void write(OutputStream stream) throws IOException; }
    private interface AtomicOutput { void write(FileOutputStream stream) throws IOException; }

    public static void write(String path, String xml) throws IOException {
        if (path == null) throw new IOException("Missing resource path");
        write(new File(path), writer -> writer.write(xml));
    }

    static void write(File target, Output output) throws IOException {
        write(target, output, null);
    }

    static void write(File target, Output output, MessageDigest checksum) throws IOException {
        var observer = testTimings;
        String prefix = target.getName().equals("manifest.json") ? "manifest." : "payload.";
        replace(target, stream -> {
            OutputStream raw = observed(stream, observer, prefix);
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    checksum == null ? raw : new DigestOutputStream(raw, checksum), StandardCharsets.UTF_8), 65536)) {
                output.write(writer);
                timed(writer::flush, observer, prefix + "flushNs");
                timed(() -> stream.getFD().sync(), observer, prefix + "syncNs");
            }
        });
    }

    static void writeBytes(File target, BytesOutput output, MessageDigest checksum) throws IOException {
        var observer = testTimings;
        String prefix = target.getName().equals("manifest.json") ? "manifest." : "payload.";
        replace(target, stream -> {
            OutputStream raw = observed(stream, observer, prefix);
            try (OutputStream buffered = new BufferedOutputStream(
                    checksum == null ? raw : new DigestOutputStream(raw, checksum), 65536)) {
                output.write(buffered);
                timed(buffered::flush, observer, prefix + "flushNs");
                timed(() -> stream.getFD().sync(), observer, prefix + "syncNs");
            }
        });
    }

    private static void replace(File target, AtomicOutput output) throws IOException {
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent == null || (!parent.mkdirs() && !parent.isDirectory())) {
            throw new IOException("Unable to create resource directory");
        }
        File temp = File.createTempFile(".resource-", ".tmp", parent);
        try {
            try (FileOutputStream stream = new FileOutputStream(temp)) {
                output.write(stream);
            }
            timed(() -> Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING), testTimings,
                    target.getName().equals("manifest.json") ? "manifest.moveNs" : "payload.moveNs");
        } finally {
            if (temp.exists()) temp.delete();
        }
    }

    private interface TimedOutput { void run() throws IOException; }

    private static void timed(TimedOutput output, java.util.function.BiConsumer<String, Long> observer,
                              String phase) throws IOException {
        if (observer == null) { output.run(); return; }
        long start = System.nanoTime();
        try { output.run(); }
        finally { observer.accept(phase, System.nanoTime() - start); }
    }

    private static OutputStream observed(FileOutputStream stream,
                                         java.util.function.BiConsumer<String, Long> observer, String prefix) {
        if (observer == null) return stream;
        return new OutputStream() {
            @Override public void write(int value) throws IOException {
                timed(() -> stream.write(value), observer, prefix + "writeNs");
                observer.accept(prefix + "writeBytes", 1L);
            }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                timed(() -> stream.write(bytes, offset, length), observer, prefix + "writeNs");
                observer.accept(prefix + "writeBytes", (long) length);
            }
            @Override public void close() throws IOException { stream.close(); }
        };
    }
}
