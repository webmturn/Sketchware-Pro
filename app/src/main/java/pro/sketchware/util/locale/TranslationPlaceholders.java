package pro.sketchware.util.locale;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class TranslationPlaceholders {
    private static final Pattern SPECIFIER =
            Pattern.compile(
                    "%([1-9][0-9]*\\$)?([-#+ 0,(<]*)([0-9]*)(\\.[0-9]+)?"
                            + "([tT][a-zA-Z]|[bBhHsScCdoxXeEfgGaA%n])");

    private TranslationPlaceholders() {}

    static boolean matches(String source, String translation) {
        try {
            Set<String> expected = arguments(source, false);
            return expected.equals(arguments(translation, !expected.isEmpty()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Set<String> arguments(String text, boolean requireValidPercents) {
        var arguments = new HashSet<String>();
        var matcher = SPECIFIER.matcher(text);
        int implicit = 0;
        int previous = 0;
        int end = 0;
        while (matcher.find()) {
            int nextPercent = text.indexOf('%', end);
            if (requireValidPercents && nextPercent >= 0 && nextPercent < matcher.start()) {
                throw new IllegalArgumentException("Invalid format specifier");
            }
            end = matcher.end();
            String conversion = matcher.group(5);
            String flags = matcher.group(2);
            if (flags.contains(" ") && !"doxXeEfgGaA".contains(conversion) && !requireValidPercents)
                continue;
            String position = matcher.group(1);
            String width = matcher.group(3);
            String precision = matcher.group(4);
            if ("%".equals(conversion) || "n".equals(conversion)) {
                // Plain %% and %n are safe to add. Keep modifiers tied to the source format.
                if (matcher.group().length() > 2) {
                    arguments.add(matcher.group());
                }
                continue;
            }
            if (flags.contains("<")) {
                if (previous == 0
                        || position != null
                        || flags.indexOf('<') != flags.lastIndexOf('<')) {
                    throw new IllegalArgumentException("Missing previous format argument");
                }
            } else {
                previous =
                        position == null
                                ? ++implicit
                                : Integer.parseInt(position.substring(0, position.length() - 1));
            }
            flags = flags.replace("<", "");
            arguments.add(
                    previous
                            + ":"
                            + flags
                            + width
                            + (precision == null ? "" : precision)
                            + (conversion.length() == 1
                                    ? conversion.toLowerCase(Locale.ROOT)
                                    : "t" + conversion.substring(1)));
        }
        if (requireValidPercents && text.indexOf('%', end) >= 0) {
            throw new IllegalArgumentException("Invalid format specifier");
        }
        return arguments;
    }
}
