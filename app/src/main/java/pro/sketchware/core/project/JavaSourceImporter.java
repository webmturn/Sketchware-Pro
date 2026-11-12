package pro.sketchware.core.project;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Imports Java/Kotlin source without truncating the old destination if reading or writing fails.
 */
public final class JavaSourceImporter {
    private JavaSourceImporter() {}

    public static void importFile(File source, File destination, String packageName)
            throws IOException {
        Path sourcePath = source.toPath();
        if (!Files.isRegularFile(sourcePath)) throw new IOException("Source is not a regular file");
        if (!destination.isDirectory())
            throw new IOException("Destination directory is unavailable");

        StringBuilder text = new StringBuilder();
        try (Reader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) text.append(buffer, 0, count);
        }
        String content = text.toString();
        PackageName declaration = findPackage(content, source.getName().endsWith(".kt"));
        if (declaration != null) {
            if (packageName == null || packageName.isEmpty())
                throw new IOException("Missing destination package");
            content =
                    content.substring(0, declaration.start())
                            + packageName
                            + content.substring(declaration.end());
        }

        Path target = destination.toPath().resolve(source.getName());
        if (Files.isDirectory(target)) throw new IOException("Destination is a directory");
        Path pending = Files.createTempFile(destination.toPath(), ".source-import-", ".tmp");
        try {
            Files.write(pending, content.getBytes(StandardCharsets.UTF_8));
            Files.move(
                    pending,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    public static String getPackageName(String content, boolean kotlin) {
        try {
            PackageName declaration = findPackage(content, kotlin);
            return declaration == null ? "" : declaration.name();
        } catch (IOException invalidDeclaration) {
            return "";
        }
    }

    private static PackageName findPackage(String content, boolean kotlin) throws IOException {
        SourceText source = kotlin ? new SourceText(content, null) : SourceText.forJava(content);
        var header = new SourceHeader(source.text(), kotlin);
        while (header.take('@')) {
            if (!header.skipAnnotation()) return null;
        }
        if (!"package".equals(header.identifier())) return null;
        PackageName name = header.qualifiedName();
        if (name == null || name.name().isEmpty())
            throw new IOException("Invalid package declaration");
        if (!kotlin && !header.take(';'))
            throw new IOException("Missing semicolon after package declaration");
        return new PackageName(
                source.rawOffset(name.start()), source.rawOffset(name.end()), name.name());
    }

    private record PackageName(int start, int end, String name) {}

    private record SourceText(String text, int[] rawOffsets) {
        int rawOffset(int offset) {
            return rawOffsets == null ? offset : rawOffsets[offset];
        }

        static SourceText forJava(String raw) {
            if (!raw.contains("\\u")) return new SourceText(raw, null);

            // Java expands Unicode escapes before recognizing comments and identifiers.
            // Keep the original offsets so importing only replaces the package name.
            var decoded = new StringBuilder(raw.length());
            int[] offsets = new int[raw.length() + 1];
            int backslashes = 0;
            boolean previousEscape = false;
            for (int i = 0; i < raw.length(); ) {
                offsets[decoded.length()] = i;
                char ch = raw.charAt(i++);
                boolean escaped = false;
                if (ch == '\\'
                        && (previousEscape || backslashes % 2 == 0)
                        && i < raw.length()
                        && raw.charAt(i) == 'u') {
                    int hexStart = i + 1;
                    while (hexStart < raw.length() && raw.charAt(hexStart) == 'u') hexStart++;
                    int codeUnit = hexCodeUnit(raw, hexStart);
                    if (codeUnit >= 0) {
                        ch = (char) codeUnit;
                        i = hexStart + 4;
                        escaped = true;
                    }
                }
                decoded.append(ch);
                backslashes = ch == '\\' ? backslashes + 1 : 0;
                previousEscape = escaped;
            }
            offsets[decoded.length()] = raw.length();
            return new SourceText(decoded.toString(), offsets);
        }

        private static int hexCodeUnit(String raw, int start) {
            if (raw.length() - start < 4) return -1;
            int value = 0;
            for (int i = start; i < start + 4; i++) {
                char ch = raw.charAt(i);
                int digit = ch < 128 ? Character.digit(ch, 16) : -1;
                if (digit < 0) return -1;
                value = value * 16 + digit;
            }
            return value;
        }
    }

    /** Reads only the source header, where the package and any preceding annotations can occur. */
    private static final class SourceHeader {
        private final String source;
        private final boolean kotlin;
        private int position;

        SourceHeader(String source, boolean kotlin) {
            this.source = source;
            this.kotlin = kotlin;
            if (source.startsWith("\uFEFF")) position++;
            if (kotlin && source.startsWith("#!", position)) skipLine();
        }

        private PackageName qualifiedName() {
            skipTrivia();
            int start = position;
            String part = identifier();
            if (part == null) return null;
            var name = new StringBuilder(part);
            int end = position;
            while (take('.')) {
                part = identifier();
                if (part == null) return null;
                name.append('.').append(part);
                end = position;
            }
            return new PackageName(start, end, name.toString());
        }

        private String identifier() {
            skipTrivia();
            if (position >= source.length()) return null;
            int start = position;
            if (kotlin && source.charAt(position) == '`') {
                int end = source.indexOf('`', position + 1);
                if (end < 0) return null;
                position = end + 1;
                return source.substring(start + 1, end);
            }
            int codePoint = source.codePointAt(position);
            if (!Character.isJavaIdentifierStart(codePoint)) return null;
            do {
                position += Character.charCount(codePoint);
                if (position >= source.length()) break;
                codePoint = source.codePointAt(position);
            } while (Character.isJavaIdentifierPart(codePoint));
            return source.substring(start, position);
        }

        private boolean skipAnnotation() {
            PackageName name = qualifiedName();
            if (name == null) return false;
            if (kotlin && name.name().equals("file") && take(':')) {
                if (take('[')) return skipGroup('[', ']');
                if (qualifiedName() == null) return false;
            }
            return !take('(') || skipGroup('(', ')');
        }

        private boolean skipGroup(char open, char close) {
            int depth = 1;
            while (position < source.length()) {
                skipTrivia();
                if (position >= source.length()) break;
                char token = source.charAt(position);
                if (token == '"' || token == '\'' || (kotlin && token == '`')) {
                    skipQuoted();
                } else {
                    position++;
                    if (token == open) depth++;
                    if (token == close && --depth == 0) return true;
                }
            }
            return false;
        }

        private void skipQuoted() {
            String delimiter =
                    source.startsWith("\"\"\"", position)
                            ? "\"\"\""
                            : String.valueOf(source.charAt(position));
            boolean escapes = !kotlin || delimiter.length() == 1 && !delimiter.equals("`");
            position += delimiter.length();
            while (position < source.length()) {
                if (source.startsWith(delimiter, position)) {
                    position += delimiter.length();
                    return;
                }
                if (kotlin && delimiter.charAt(0) == '"' && source.startsWith("${", position)) {
                    position += 2;
                    skipGroup('{', '}');
                } else {
                    if (escapes && source.charAt(position) == '\\') position++;
                    position++;
                }
            }
        }

        private boolean take(char token) {
            skipTrivia();
            if (position >= source.length() || source.charAt(position) != token) return false;
            position++;
            return true;
        }

        private void skipTrivia() {
            while (position < source.length()) {
                if (Character.isWhitespace(source.charAt(position))) {
                    position++;
                } else if (source.startsWith("//", position)) {
                    skipLine();
                } else if (source.startsWith("/*", position)) {
                    skipBlockComment();
                } else {
                    return;
                }
            }
        }

        private void skipLine() {
            while (position < source.length()
                    && source.charAt(position) != '\r'
                    && source.charAt(position) != '\n') position++;
        }

        private void skipBlockComment() {
            position += 2;
            int depth = 1;
            while (position < source.length() && depth > 0) {
                if (kotlin && source.startsWith("/*", position)) {
                    depth++;
                    position += 2;
                } else if (source.startsWith("*/", position)) {
                    depth--;
                    position += 2;
                } else {
                    position++;
                }
            }
        }
    }
}
