package pro.sketchware.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands the backup template into a filename stem; the caller adds the SWB extension. */
public final class BackupFilenameFormatter {
    private static final Pattern TIME_PATTERN = Pattern.compile("\\$time\\((.*?)\\)");

    private BackupFilenameFormatter() {}

    public static String format(String template, String projectName, String versionName,
                                String versionCode, String packageName, long timeMillis) {
        String filename = template.replace("$projectName", projectName)
                .replace("$versionCode", versionCode)
                .replace("$versionName", versionName)
                .replace("$pkgName", packageName)
                .replace("$timeInMs", String.valueOf(timeMillis));
        Matcher matcher = TIME_PATTERN.matcher(template);
        while (matcher.find()) {
            String date = new SimpleDateFormat(matcher.group(1), Locale.ENGLISH).format(new Date(timeMillis));
            filename = filename.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(date));
        }
        return filename;
    }
}
