package pro.sketchware.util.locale;

import android.content.Context;
import pro.sketchware.R;

/** Resolves labels at the UI boundary; property values and code-generation constants stay canonical. */
public final class PropertyDisplayNames {
    private PropertyDisplayNames() {}

    public static String get(Context context, String property, String label) {
        if (label == null) return null;
        if ("property_gravity".equals(property) || "property_layout_gravity".equals(property)) {
            String[] parts = label.split(", ");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = resolve(context, gravityResource(parts[i]), parts[i]);
            }
            return String.join(", ", parts);
        }
        int resource = switch (property) {
            case "property_layout_width", "property_layout_height" -> switch (label) {
                case "match_parent" -> R.string.property_option_match_parent;
                case "wrap_content" -> R.string.property_option_wrap_content;
                default -> 0;
            };
            case "property_orientation" -> switch (label) {
                case "none" -> R.string.property_option_none;
                case "horizontal" -> R.string.property_option_horizontal;
                case "vertical" -> R.string.property_option_vertical;
                default -> 0;
            };
            case "property_text_style" -> switch (label) {
                case "normal" -> R.string.property_option_normal;
                case "bold" -> R.string.property_option_bold;
                case "italic" -> R.string.property_option_italic;
                case "bold|italic" -> R.string.property_option_bold_italic;
                default -> 0;
            };
            case "property_ime_option" -> switch (label) {
                case "normal" -> R.string.property_option_default;
                case "none" -> R.string.property_option_none;
                case "go" -> R.string.property_option_go;
                case "search" -> R.string.property_option_search;
                case "send" -> R.string.property_option_send;
                case "next" -> R.string.property_option_next;
                case "done" -> R.string.property_option_done;
                default -> 0;
            };
            case "property_spinner_mode" -> switch (label) {
                case "dialog" -> R.string.property_option_dialog;
                case "dropdown" -> R.string.property_option_dropdown;
                default -> 0;
            };
            case "property_choice_mode" -> switch (label) {
                case "none" -> R.string.property_option_none;
                case "single" -> R.string.property_option_single;
                case "multi" -> R.string.property_option_multi;
                default -> 0;
            };
            case "property_first_day_of_week" -> switch (label) {
                case "Sunday" -> R.string.property_option_sunday;
                case "Monday" -> R.string.property_option_monday;
                case "Tuesday" -> R.string.property_option_tuesday;
                case "Wednesday" -> R.string.property_option_wednesday;
                case "Thursday" -> R.string.property_option_thursday;
                case "Friday" -> R.string.property_option_friday;
                case "Saturday" -> R.string.property_option_saturday;
                default -> 0;
            };
            case "property_input_type" -> switch (label) {
                case "text" -> R.string.property_option_text;
                case "numberSigned" -> R.string.property_option_number_signed;
                case "numberDecimal" -> R.string.property_option_number_decimal;
                case "numberSigned|numberDecimal" -> R.string.property_option_number_signed_decimal;
                case "phone" -> R.string.property_option_phone;
                case "textPassword" -> R.string.property_option_password;
                default -> 0;
            };
            case "property_progressbar_style" -> switch (label) {
                case "Circle" -> R.string.property_option_circle;
                case "Horizontal" -> R.string.property_option_horizontal_progress;
                default -> 0;
            };
            case "property_scale_type" -> switch (label) {
                case "FIT_XY" -> R.string.property_option_fit_xy;
                case "FIT_START" -> R.string.property_option_fit_start;
                case "FIT_CENTER" -> R.string.property_option_fit_center;
                case "FIT_END" -> R.string.property_option_fit_end;
                case "CENTER" -> R.string.property_option_center;
                case "CENTER_CROP" -> R.string.property_option_center_crop;
                case "CENTER_INSIDE" -> R.string.property_option_center_inside;
                default -> 0;
            };
            case "property_ad_size" -> switch (label) {
                case "BANNER" -> R.string.property_option_ad_banner;
                case "MEDIUM_RECTANGLE" -> R.string.property_option_ad_medium_rectangle;
                case "LARGE_BANNER" -> R.string.property_option_ad_large_banner;
                case "SMART_BANNER" -> R.string.property_option_ad_smart_banner;
                default -> 0;
            };
            case "property_indeterminate" -> switch (label) {
                case "true" -> R.string.property_option_true;
                case "false" -> R.string.property_option_false;
                default -> 0;
            };
            default -> 0;
        };
        return resolve(context, resource, label);
    }

    private static int gravityResource(String label) {
        return switch (label) {
            case "none" -> R.string.property_option_none;
            case "left" -> R.string.gravity_left;
            case "right" -> R.string.gravity_right;
            case "top" -> R.string.gravity_top;
            case "bottom" -> R.string.gravity_bottom;
            case "center_horizontal" -> R.string.gravity_center_horizontal;
            case "center_vertical" -> R.string.gravity_center_vertical;
            default -> 0;
        };
    }

    private static String resolve(Context context, int resource, String fallback) {
        if (resource == 0) return fallback;
        String override = LanguageOverrideManager.getInstance().getOverride(resource);
        return override != null ? override : context.getString(resource);
    }
}
