package pro.sketchware.lib.highlighter;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;

/** Immutable, view-independent match index; scrolling reuses the full-document analysis. */
final class HighlightAnalysis {
    final String content;
    private final int[][] matches;
    private final int[] colors;

    private HighlightAnalysis(String content, int[][] matches, int[] colors) {
        this.content = content;
        this.matches = matches;
        this.colors = colors;
    }

    static HighlightAnalysis scan(String content, List<SyntaxScheme> syntax, BooleanSupplier cancelled) {
        int[][] matches = new int[syntax.size()][];
        int[] colors = new int[syntax.size()];
        for (int i = 0; i < syntax.size(); i++) {
            if (cancelled.getAsBoolean()) return null;
            Ints offsets = new Ints();
            if (syntax.get(i).pattern.flags() == 0
                    && syntax.get(i).pattern.pattern().equals("/\\*[\\s\\S]*?\\*/|//.*")) {
                if (!comments(content, offsets, cancelled)) return null;
                matches[i] = offsets.toArray();
                colors[i] = syntax.get(i).color;
                continue;
            }
            Matcher matcher = syntax.get(i).pattern.matcher(content);
            while (matcher.find()) {
                if (cancelled.getAsBoolean()) return null;
                if (matcher.start() != matcher.end()) {
                    offsets.add(matcher.start());
                    offsets.add(matcher.end());
                }
            }
            matches[i] = offsets.toArray();
            colors[i] = syntax.get(i).color;
        }
        return cancelled.getAsBoolean() ? null : new HighlightAnalysis(content, matches, colors);
    }

    /** Avoid quadratic regex retries for a long sequence of unclosed block-comment openers. */
    private static boolean comments(String text, Ints offsets, BooleanSupplier cancelled) {
        boolean noBlockEnd = false;
        for (int i = 0; i + 1 < text.length(); i++) {
            if ((i & 1023) == 0 && cancelled.getAsBoolean()) return false;
            if (text.charAt(i) != '/') continue;
            if (text.charAt(i + 1) == '*' && !noBlockEnd) {
                int end = i + 2;
                while (end + 1 < text.length() && !(text.charAt(end) == '*' && text.charAt(end + 1) == '/')) {
                    if ((end & 1023) == 0 && cancelled.getAsBoolean()) return false;
                    end++;
                }
                if (end + 1 < text.length()) {
                    offsets.add(i);
                    offsets.add(end + 2);
                    i = end + 1;
                } else noBlockEnd = true;
            } else if (text.charAt(i + 1) == '/') {
                int end = i + 2;
                while (end < text.length() && !lineTerminator(text.charAt(end))) {
                    if ((end & 1023) == 0 && cancelled.getAsBoolean()) return false;
                    end++;
                }
                offsets.add(i);
                offsets.add(end);
                i = end - 1;
            }
        }
        return !cancelled.getAsBoolean();
    }

    private static boolean lineTerminator(char character) {
        return character == '\n' || character == '\r' || character == '\u0085'
                || character == '\u2028' || character == '\u2029';
    }

    int[] window(int start, int end) {
        Ints result = new Ints();
        if (start >= end) return result.toArray();
        for (int rule = 0; rule < matches.length; rule++) {
            int[] offsets = matches[rule];
            int low = 0;
            int high = offsets.length / 2;
            // Matches within one rule cannot overlap, so both their starts and ends are sorted.
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (offsets[middle * 2 + 1] <= start) low = middle + 1;
                else high = middle;
            }
            for (int i = low * 2; i < offsets.length && offsets[i] < end; i += 2) {
                // Include comments/strings that start outside the viewport, with their original ranges.
                result.add(offsets[i]);
                result.add(offsets[i + 1]);
                result.add(colors[rule]);
            }
        }
        return result.toArray();
    }

    private static final class Ints {
        private int[] values = new int[64];
        private int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }

        int[] toArray() { return Arrays.copyOf(values, size); }
    }
}
