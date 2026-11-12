package pro.sketchware.util;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands the backup template into a filename stem; the caller adds the SWB extension. */
public final class BackupFilenameFormatter {
    public static final String EXTENSION = "swb";
    private static final int MAX_FILENAME_BYTES = 255;
    private static final Pattern TIME_PATTERN = Pattern.compile("\\$time\\((.*?)\\)");

    private BackupFilenameFormatter() {}

    public static boolean isValidFilenameStem(String stem) {
        return !stem.trim().isEmpty()
                && !stem.equals(".")
                && !stem.equals("..")
                && stem.indexOf('/') < 0
                && stem.indexOf('\\') < 0
                && stem.chars().noneMatch(Character::isISOControl);
    }

    public static boolean isArchiveFilenameTooLong(String stem) {
        return (stem + "." + EXTENSION).getBytes(StandardCharsets.UTF_8).length
                > MAX_FILENAME_BYTES;
    }

    public static String archiveFilename(String stem, int copy) {
        String suffix = (copy == 1 ? "" : "_" + copy) + "." + EXTENSION;
        byte[] bytes = stem.getBytes(StandardCharsets.UTF_8);
        int limit = MAX_FILENAME_BYTES - suffix.getBytes(StandardCharsets.UTF_8).length;
        if (bytes.length <= limit) return stem + suffix;

        // Move back to the start of a character if the limit falls inside it.
        while ((bytes[limit] & 0xc0) == 0x80) limit--;
        return new String(bytes, 0, limit, StandardCharsets.UTF_8) + suffix;
    }

    public static String format(
            String template,
            String projectName,
            String versionName,
            String versionCode,
            String packageName,
            long timeMillis) {
        String filename =
                template.replace("$projectName", projectName)
                        .replace("$versionCode", versionCode)
                        .replace("$versionName", versionName)
                        .replace("$pkgName", packageName)
                        .replace("$timeInMs", String.valueOf(timeMillis));
        Matcher matcher = TIME_PATTERN.matcher(template);
        while (matcher.find()) {
            String date =
                    new SimpleDateFormat(matcher.group(1), Locale.ENGLISH)
                            .format(new Date(timeMillis));
            filename =
                    filename.replaceFirst(
                            Pattern.quote(matcher.group()), Matcher.quoteReplacement(date));
        }
        return filename;
    }
}
