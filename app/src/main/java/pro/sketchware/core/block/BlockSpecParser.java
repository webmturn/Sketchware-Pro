package pro.sketchware.core.block;

import java.util.ArrayList;
import java.util.regex.Pattern;

import pro.sketchware.util.format.FormatUtil;

/** Interprets parameter tokens using the same escaping rules as the block preview. */
public final class BlockSpecParser {
    private static final Pattern INVALID_MENU_PARAMETER = Pattern.compile("^%m(?!\\.[\\w]+)");

    private BlockSpecParser() {
    }

    /** Returns the original tokens that create boolean, number, string, or menu fields. */
    public static ArrayList<String> parseParameters(String spec) {
        ArrayList<String> parameters = new ArrayList<>();
        if (spec == null || spec.isEmpty()) return parameters;
        for (String token : FormatUtil.parseBlockSpec(spec)) {
            if (token.length() >= 2 && token.charAt(0) == '%') {
                switch (token.charAt(1)) {
                    case 'b', 'd', 's', 'm' -> parameters.add(token);
                }
            }
        }
        return parameters;
    }

    /**
     * Returns the offset immediately after the first malformed, unescaped %m, or -1.
     * Keeps the existing menu-name rule and the preview error's position convention.
     */
    public static int findInvalidMenuParameterEnd(String spec) {
        if (spec == null || spec.isEmpty()) return -1;
        FormatUtil.StringScanner scanner = new FormatUtil.StringScanner(spec);
        while (!scanner.isAtEnd()) {
            scanner.skipSpaces();
            int start = scanner.position;
            String token = scanner.nextToken();
            if (INVALID_MENU_PARAMETER.matcher(token).find()) return start + 2;
        }
        return -1;
    }
}
