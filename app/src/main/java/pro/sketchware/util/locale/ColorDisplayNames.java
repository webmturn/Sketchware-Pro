package pro.sketchware.util.locale;

import android.content.Context;
import pro.sketchware.R;

/** Used only for built-in palette labels, never for user-defined color resource names or codes. */
public final class ColorDisplayNames {
    private ColorDisplayNames() {}

    public static String getGroup(Context context, String name) {
        int resource = switch (name) {
            case "CUSTOM" -> R.string.picker_color_group_custom;
            case "Material 3 Colors" -> R.string.picker_color_group_material3;
            case "RED" -> R.string.picker_color_group_red;
            case "PINK" -> R.string.picker_color_group_pink;
            case "PURPLE" -> R.string.picker_color_group_purple;
            case "DEEP PURPLE" -> R.string.picker_color_group_deep_purple;
            case "INDIGO" -> R.string.picker_color_group_indigo;
            case "BLUE" -> R.string.picker_color_group_blue;
            case "LIGHT BLUE" -> R.string.picker_color_group_light_blue;
            case "CYAN" -> R.string.picker_color_group_cyan;
            case "TEAL" -> R.string.picker_color_group_teal;
            case "GREEN" -> R.string.picker_color_group_green;
            case "LIGHT GREEN" -> R.string.picker_color_group_light_green;
            case "LIME" -> R.string.picker_color_group_lime;
            case "YELLOW" -> R.string.picker_color_group_yellow;
            case "AMBER" -> R.string.picker_color_group_amber;
            case "ORANGE" -> R.string.picker_color_group_orange;
            case "DEEP ORANGE" -> R.string.picker_color_group_deep_orange;
            case "BROWN" -> R.string.picker_color_group_brown;
            case "GREY" -> R.string.picker_color_group_grey;
            case "BLUE GREY" -> R.string.picker_color_group_blue_grey;
            case "BLACK" -> R.string.picker_color_group_black;
            case "WHITE" -> R.string.picker_color_group_white;
            case "TRANSPARENT" -> R.string.color_transparent;
            case "NONE" -> R.string.color_none;
            default -> 0;
        };
        return resolve(context, resource, name);
    }

    public static String getAttributeTitle(Context context, String group) {
        int resource = switch (group) {
            case "Surface" -> R.string.picker_color_attrs_surface;
            case "Primary" -> R.string.picker_color_attrs_primary;
            case "Secondary" -> R.string.picker_color_attrs_secondary;
            case "Tertiary" -> R.string.picker_color_attrs_tertiary;
            case "Error" -> R.string.picker_color_attrs_error;
            case "Custom" -> R.string.picker_color_attrs_custom;
            default -> 0;
        };
        return resolve(context, resource, group + " Colors :");
    }

    private static String resolve(Context context, int resource, String fallback) {
        if (resource == 0) return fallback;
        String override = LanguageOverrideManager.getInstance().getOverride(resource);
        return override != null ? override : context.getString(resource);
    }
}
