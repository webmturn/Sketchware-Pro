package pro.sketchware.core.validation;

/** Shared bounds for a generated application's versionCode. */
public final class ProjectVersionCode {
    public static final int MIN = 1;
    public static final int MAX = 2_100_000_000;

    private ProjectVersionCode() {
    }

    /** Returns zero for invalid input, without truncating or wrapping oversized numbers. */
    public static int parse(String value) {
        try {
            long code = Long.parseLong(value == null ? "" : value.trim());
            return code >= MIN && code <= MAX ? (int) code : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** A safe initial picker value; opening or cancelling a picker must not overwrite its source. */
    public static int clampForPicker(String value) {
        try {
            long code = Long.parseLong(value == null ? "" : value.trim());
            return (int) Math.max(MIN, Math.min(MAX, code));
        } catch (NumberFormatException e) {
            return MIN;
        }
    }
}
