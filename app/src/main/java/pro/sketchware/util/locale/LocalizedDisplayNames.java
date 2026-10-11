package pro.sketchware.util.locale;

import android.content.Context;
import static pro.sketchware.SketchApplication.getContext;
import pro.sketchware.R;
import pro.sketchware.core.block.view.BlockColorMapper;

/** Translates display labels without changing identifiers stored in project data. */
public final class LocalizedDisplayNames {
    private LocalizedDisplayNames() {}

    public static String get(String canonicalName) {
        return get(getContext(), canonicalName);
    }

    /** Resolves built-in selector IDs before translating; custom menu names stay verbatim. */
    public static String getMenuDisplayName(Context context, String menuId) {
        String name = BlockColorMapper.getComponentDisplayName(menuId);
        return switch (menuId) {
            case "varInt", "varStr", "varBool", "varMap", "list", "listInt", "listStr",
                 "listMap", "listBool", "view", "color", "resource", "customViews",
                 "firebase", "firebaseauth" -> get(context, name);
            default -> name;
        };
    }

    public static String get(Context context, String canonicalName) {
        if (canonicalName == null) return null;
        int resource = switch (canonicalName) {
            case "Number" -> R.string.display_name_number;
            case "String" -> R.string.display_name_string;
            case "Boolean" -> R.string.display_name_boolean;
            case "Map" -> R.string.display_name_map;
            case "List" -> R.string.display_name_list;
            case "View" -> R.string.display_name_view;
            case "List Number" -> R.string.display_name_list_number;
            case "List String" -> R.string.display_name_list_string;
            case "List Map" -> R.string.display_name_list_map;
            case "List Boolean" -> R.string.display_name_list_boolean;
            case "Color" -> R.string.display_name_color;
            case "Image" -> R.string.display_name_image;
            case "CustomView" -> R.string.display_name_customview;
            case "Basic Activity" -> R.string.display_name_basic_activity;
            case "Basic List Item" -> R.string.display_name_basic_list_item;
            case "Basic Drawer" -> R.string.display_name_basic_drawer;
            case "Empty Activity" -> R.string.display_name_empty_activity;
            case "Text Activity" -> R.string.display_name_text_activity;
            case "Firebase DB" -> R.string.logic_editor_category_firebase;
            case "Firebase Auth" -> R.string.logic_editor_category_firebase_auth;
            case "Firebase Storage" -> R.string.logic_editor_category_firebase_storage;
            case "Cloud Message" -> R.string.logic_editor_category_cloud_message;
            case "Google Login" -> R.string.logic_editor_category_google_sign_in;
            case "PhoneAuth" -> R.string.logic_editor_category_phone_auth;
            case "Material Purple" -> R.string.display_name_material_purple;
            case "Material Blue" -> R.string.display_name_material_blue;
            case "Material Green" -> R.string.display_name_material_green;
            case "Material Red" -> R.string.display_name_material_red;
            case "Material Orange" -> R.string.display_name_material_orange;
            case "Material Teal" -> R.string.display_name_material_teal;
            case "Material Indigo" -> R.string.display_name_material_indigo;
            case "Material Pink" -> R.string.display_name_material_pink;
            case "Dark Purple" -> R.string.display_name_dark_purple;
            case "Dark Blue" -> R.string.display_name_dark_blue;
            case "Light Purple" -> R.string.display_name_light_purple;
            case "Light Blue" -> R.string.display_name_light_blue;
            case "Random Theme" -> R.string.display_name_random_theme;
            case "Default" -> R.string.display_name_default;
            case "Select typeview:" -> R.string.display_name_select_typeview;
            default -> 0;
        };
        if (resource == 0) return canonicalName;
        String override = LanguageOverrideManager.getInstance().getOverride(resource);
        return override != null ? override : context.getString(resource);
    }
}
