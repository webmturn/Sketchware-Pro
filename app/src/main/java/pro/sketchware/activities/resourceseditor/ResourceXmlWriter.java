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
        replace(target, stream -> {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    checksum == null ? stream : new DigestOutputStream(stream, checksum), StandardCharsets.UTF_8), 65536)) {
                output.write(writer);
                writer.flush();
                stream.getFD().sync();
            }
        });
    }

    static void writeBytes(File target, BytesOutput output, MessageDigest checksum) throws IOException {
        replace(target, stream -> {
            try (OutputStream buffered = new BufferedOutputStream(
                    checksum == null ? stream : new DigestOutputStream(stream, checksum), 65536)) {
                output.write(buffered);
                buffered.flush();
                stream.getFD().sync();
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
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (temp.exists()) temp.delete();
        }
    }
}
