package mod.hilal.saif.blocks;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;

public final class BlockTypeUtils {

    private static final List<String> SELECTABLE_BLOCK_TYPES = Collections.unmodifiableList(Arrays.asList(
            " ", "c", "e", "s", "b", "d", "v", "a", "f", "l", "p", "h"
    ));

    private BlockTypeUtils() {
    }

    public static List<String> getSelectableBlockTypes() {
        return SELECTABLE_BLOCK_TYPES;
    }

    public static String normalizeStoredBlockType(Context context, String rawType) {
        if (rawType == null) {
            return " ";
        }

        if (rawType.equals(" ")) {
            return " ";
        }

        String normalizedValue = rawType.trim();
        if (normalizedValue.isEmpty()) {
            return " ";
        }

        for (String selectableType : SELECTABLE_BLOCK_TYPES) {
            if (!selectableType.equals(" ") && selectableType.equals(normalizedValue)) {
                return selectableType;
            }
        }

        String lowerValue = normalizedValue.toLowerCase(Locale.ROOT);
        if (isRegularDisplayLabel(context, normalizedValue, lowerValue)) {
            return " ";
        }

        for (String selectableType : SELECTABLE_BLOCK_TYPES) {
            if (selectableType.equals(" ")) {
                continue;
            }
            if (normalizedValue.equals(getDisplayBlockTypeLabel(context, selectableType))) {
                return selectableType;
            }
        }

        return normalizedValue;
    }

    public static String getDisplayBlockTypeLabel(Context context, String storedType) {
        return switch (normalizeStoredBlockType(context, storedType)) {
            case " " -> context.getString(R.string.blocks_type_regular);
            case "c" -> context.getString(R.string.blocks_type_if);
            case "e" -> context.getString(R.string.blocks_type_if_else);
            case "s" -> context.getString(R.string.blocks_type_string);
            case "b" -> context.getString(R.string.blocks_type_boolean);
            case "d" -> context.getString(R.string.blocks_type_number);
            case "v" -> context.getString(R.string.blocks_type_variable);
            case "a" -> context.getString(R.string.blocks_type_map);
            case "f" -> context.getString(R.string.blocks_type_stop);
            case "l" -> context.getString(R.string.blocks_type_list);
            case "p" -> context.getString(R.string.blocks_type_component);
            case "h" -> context.getString(R.string.blocks_type_header);
            default -> storedType == null ? "" : storedType.trim();
        };
    }

    private static boolean isRegularDisplayLabel(Context context, String normalizedValue, String lowerValue) {
        return normalizedValue.equals(context.getString(R.string.block_type_regular))
                || normalizedValue.equals(context.getString(R.string.blocks_type_regular))
                || lowerValue.equals("regular");
    }
}
