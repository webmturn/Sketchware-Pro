package pro.sketchware.core.codegen;

import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

/** Checks custom-block templates using String arguments, as supplied by BlockInterpreter. */
public final class CodeTemplateValidator {
    public enum Kind { INVALID_FORMAT, STRING_CONVERSION, MISSING_PREVIOUS_ARGUMENT, NOT_ENOUGH_ARGUMENTS }

    public static final class Problem {
        public final Kind kind;
        public final String specifier;
        /** One-based position in the original template; zero for argument-count problems. */
        public final int position;
        public final String errorType;
        public final int requiredArguments;
        public final int availableArguments;

        private Problem(Kind kind, String specifier, int position, String errorType, int required, int available) {
            this.kind = kind;
            this.specifier = specifier;
            this.position = position;
            this.errorType = errorType;
            requiredArguments = required;
            availableArguments = available;
        }

        public String describe() {
            return switch (kind) {
                case INVALID_FORMAT -> "Invalid format '" + specifier + "' at position " + position + " (" + errorType + ").";
                case STRING_CONVERSION -> "Format '" + specifier + "' at position " + position
                        + " cannot format a String argument. Insert Java code arguments with %s; write %% for a literal percent sign.";
                case MISSING_PREVIOUS_ARGUMENT -> "Format '" + specifier + "' has no previous argument to reuse.";
                case NOT_ENOUGH_ARGUMENTS -> "Template requires " + requiredArguments + " arguments but only " + availableArguments + " are available.";
            };
        }
    }

    private CodeTemplateValidator() { }

    /** Returns the first detected problem without formatting the original template's output. */
    public static Problem findProblem(String template, int availableArguments) {
        if (template == null || template.indexOf('%') < 0) {
            return argumentProblem(new CodeTemplateArguments.Analysis(0, null), availableArguments);
        }
        CodeTemplateArguments.Counter arguments = new CodeTemplateArguments.Counter();
        Set<String> checkedFormats = null;
        Matcher matcher = CodeTemplateArguments.FORMAT_SPECIFIER.matcher(template);
        int end = 0;
        while ((end = template.indexOf('%', end)) >= 0) {
            // Keep one native matcher input. region() resets Android's ICU input for every
            // specifier, repeatedly paying for the full document. Check the next '%' as well
            // so find() cannot silently skip a malformed earlier specifier.
            if (!matcher.find() || matcher.start() != end) {
                return new Problem(Kind.INVALID_FORMAT, "%", end + 1, "MalformedFormatSpecifier", 0, 0);
            }
            // Bare general conversions accept String arguments, and bare %% / %n consume none.
            // Flags, widths, precisions and date conversions still use the runtime Formatter.
            if (!isBareGeneralConversion(matcher)) {
                String bounded = "%s%" + matcher.group(2) + bound(matcher.group(3), "1")
                        + (matcher.group(4) == null ? "" : "." + bound(matcher.group(4), "0"))
                        + (matcher.group(5) == null ? "" : matcher.group(5)) + matcher.group(6);
                if (checkedFormats == null) checkedFormats = new HashSet<>();
                // Normalized format validation is independent of argument selection and source
                // position. Repeated formats need one Formatter call within this check only.
                if (checkedFormats.add(bounded)) {
                    try {
                        String.format(Locale.ROOT, bounded, "", "");
                    } catch (IllegalFormatException failure) {
                        Kind kind = failure instanceof IllegalFormatConversionException ? Kind.STRING_CONVERSION : Kind.INVALID_FORMAT;
                        return new Problem(kind, matcher.group(), end + 1, failure.getClass().getSimpleName(), 0, 0);
                    }
                }
            }
            arguments.accept(matcher);
            end = matcher.end();
        }
        // Format errors retain precedence over missing/reused arguments anywhere in the text.
        return argumentProblem(arguments.result(), availableArguments);
    }

    private static Problem argumentProblem(CodeTemplateArguments.Analysis arguments, int availableArguments) {
        if (arguments.missingPreviousSpecifier != null) {
            return new Problem(Kind.MISSING_PREVIOUS_ARGUMENT, arguments.missingPreviousSpecifier, 0, null, 0, availableArguments);
        }
        if (arguments.requiredCount > availableArguments) {
            return new Problem(Kind.NOT_ENOUGH_ARGUMENTS, null, 0, null, arguments.requiredCount, availableArguments);
        }
        return null;
    }

    private static boolean isBareGeneralConversion(Matcher matcher) {
        return matcher.group(2).isEmpty() && matcher.group(3) == null && matcher.group(4) == null
                && matcher.group(5) == null && "sSbBhH%n".indexOf(matcher.group(6).charAt(0)) >= 0;
    }

    private static String bound(String number, String replacement) {
        if (number == null) return "";
        try {
            Integer.parseInt(number);
            return replacement;
        } catch (NumberFormatException overflow) {
            // Android's legacy Formatter ignores overflowing widths/precisions; newer runtimes
            // reject them. Keeping these digits preserves either runtime's validation safely.
            return number;
        }
    }
}
