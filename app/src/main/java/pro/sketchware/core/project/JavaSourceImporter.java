package pro.sketchware.core.project;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;

/** Imports Java/Kotlin source without truncating the old destination if reading or writing fails. */
public final class JavaSourceImporter {
    private static final String PACKAGE_DECL_REGEX = "package (.*?);?\\n";

    private JavaSourceImporter() {}

    public static void importFile(File source, File destination, String packageName) throws IOException {
        Path sourcePath = source.toPath();
        if (!Files.isRegularFile(sourcePath)) throw new IOException("Source is not a regular file");
        if (!destination.isDirectory()) throw new IOException("Destination directory is unavailable");

        StringBuilder text = new StringBuilder();
        try (Reader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) text.append(buffer, 0, count);
        }
        String content = text.toString();
        if (content.contains("package ")) {
            if (packageName == null || packageName.isEmpty()) throw new IOException("Missing destination package");
            String declaration = "package " + packageName + (source.getName().endsWith(".java") ? ";" : "") + "\n";
            content = content.replaceFirst(PACKAGE_DECL_REGEX, Matcher.quoteReplacement(declaration));
        }

        Path target = destination.toPath().resolve(source.getName());
        if (Files.isDirectory(target)) throw new IOException("Destination is a directory");
        Path pending = Files.createTempFile(destination.toPath(), ".source-import-", ".tmp");
        try {
            Files.write(pending, content.getBytes(StandardCharsets.UTF_8));
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
