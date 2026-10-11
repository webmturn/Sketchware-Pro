package pro.sketchware.core.codegen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Analyzes argument selection without formatting or allocating the template's output. */
final class CodeTemplateArguments {
    static final Pattern FORMAT_SPECIFIER = Pattern.compile(
            "%(\\d+\\$)?([-#+ 0,(<]*)(\\d+)?(?:\\.(\\d+))?([tT])?([a-zA-Z%])");
    private static final int ORDINARY_INDEX = 0;
    private static final int PREVIOUS_INDEX = -1;

    static final class Analysis {
        final int requiredCount;
        final String missingPreviousSpecifier;

        Analysis(int requiredCount, String missingPreviousSpecifier) {
            this.requiredCount = requiredCount;
            this.missingPreviousSpecifier = missingPreviousSpecifier;
        }
    }

    private CodeTemplateArguments() {
    }

    static Analysis analyze(String template) {
        int ordinaryIndex = 0, previousIndex = 0, requiredCount = 0;
        String missingPrevious = null;
        if (template == null || template.isEmpty()) return new Analysis(0, null);
        Matcher matcher = FORMAT_SPECIFIER.matcher(template);
        while (matcher.find()) {
            String conversion = matcher.group(6);
            if (matcher.group(5) == null && (conversion.equals("%") || conversion.equals("n"))) continue;
            int index = argumentIndex(matcher.group(1));
            if (matcher.group(2).indexOf('<') >= 0) index = PREVIOUS_INDEX;
            if (index == PREVIOUS_INDEX) {
                if (previousIndex == 0 && missingPrevious == null) missingPrevious = matcher.group();
                index = previousIndex;
            } else if (index == ORDINARY_INDEX) {
                index = ++ordinaryIndex;
            }
            previousIndex = index;
            requiredCount = Math.max(requiredCount, index);
        }
        return new Analysis(requiredCount, missingPrevious);
    }

    private static int argumentIndex(String index) {
        if (index == null) return ORDINARY_INDEX;
        try {
            return Integer.parseInt(index.substring(0, index.length() - 1));
        } catch (NumberFormatException ignored) {
            // Android's legacy Formatter treats an overflowing index as a previous-argument reference.
            // Index zero selects the next ordinary argument; neither behavior rejects block loading.
            return PREVIOUS_INDEX;
        }
    }
}
