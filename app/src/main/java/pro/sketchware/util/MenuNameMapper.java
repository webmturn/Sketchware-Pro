package pro.sketchware.util;

import pro.sketchware.R;

public class MenuNameMapper {

    public static String getName(String menuName) {
        return switch (menuName) {
            case "image" -> Helper.getResString(R.string.menu_name_custom_image);
            case "til_box_mode" -> Helper.getResString(R.string.menu_name_box_mode);
            case "fabsize" -> Helper.getResString(R.string.menu_name_fab_size);
            case "fabvisible" -> Helper.getResString(R.string.menu_name_fab_visible);
            case "menuaction" -> Helper.getResString(R.string.menu_name_menu_action);
            case "porterduff" -> Helper.getResString(R.string.menu_name_porterduff_mode);
            case "transcriptmode" -> Helper.getResString(R.string.menu_name_transcript_mode);
            case "listscrollparam", "recyclerscrollparam", "pagerscrollparam" -> Helper.getResString(R.string.menu_name_scroll_param);
            case "gridstretchmode" -> Helper.getResString(R.string.menu_name_stretch_mode);
            case "gravity_v" -> Helper.getResString(R.string.menu_name_gravity_vertical);
            case "gravity_h" -> Helper.getResString(R.string.menu_name_gravity_horizontal);
            case "gravity_t" -> Helper.getResString(R.string.menu_name_gravity_toast);
            case "patternviewmode" -> Helper.getResString(R.string.menu_name_pattern_mode);
            case "styleprogress" -> Helper.getResString(R.string.menu_name_progress_style);
            case "cv_theme" -> Helper.getResString(R.string.menu_name_theme);
            case "cv_language" -> Helper.getResString(R.string.menu_name_language);
            case "import" -> Helper.getResString(R.string.common_word_import);
            default -> menuName;
        };
    }
}
