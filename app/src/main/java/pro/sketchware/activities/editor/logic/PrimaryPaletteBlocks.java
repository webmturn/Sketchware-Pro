package pro.sketchware.activities.editor.logic;

import androidx.annotation.ColorInt;

import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.settings.ConfigActivity;
import pro.sketchware.util.Helper;
import pro.sketchware.util.ThemeUtils;

public final class PrimaryPaletteBlocks {

    private PrimaryPaletteBlocks() {
    }

    private static boolean showAll() {
        return ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_ALWAYS_SHOW_BLOCKS)
                || ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK);
    }

    private static boolean showBuiltIn() {
        return ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)
                || ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK);
    }

    public static void primaryBlocksA(
            LogicEditorActivity logicEditorActivity,
            boolean isBoolUsed,
            boolean isIntUsed,
            boolean isStrUsed,
            boolean isMapUsed) {
        logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_blocks), getTitleBgColor(logicEditorActivity));
        if (showAll() || isBoolUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarBoolean");
        }
        if (showAll() || isIntUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarInt");
            logicEditorActivity.createPaletteBlock(" ", "increaseInt");
            logicEditorActivity.createPaletteBlock(" ", "decreaseInt");
        }
        if (showAll() || isStrUsed) {
            logicEditorActivity.createPaletteBlock(" ", "setVarString");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.createPaletteBlock(" ", "mapCreateNew");
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_put_values), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock(" ", "mapPut");
        }
        if (showBuiltIn() && (showAll() || isMapUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutNumber");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutNumber2");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutBoolean");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutMap");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutListstr");
            logicEditorActivity.createPaletteBlock(" ", "hashmapPutListmap");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_get_values), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("s", "mapGet");
        }
        if (showBuiltIn() && (showAll() || isMapUsed)) {
            logicEditorActivity.createPaletteBlock("d", "hashmapGetNumber");
            logicEditorActivity.createPaletteBlock("b", "hashmapGetBoolean");
            logicEditorActivity.createPaletteBlock("a", "hashmapGetMap");
            logicEditorActivity.createPaletteBlockWithComponent("", "l", "List String", "hashmapListstr");
            logicEditorActivity.createPaletteBlockWithComponent("", "l", "List Map", "hashmapGetListmap");
        }
        if (showAll() || isMapUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map_general), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "mapIsEmpty");
            logicEditorActivity.createPaletteBlock("b", "mapContainKey");
            logicEditorActivity.createPaletteBlock("b", "mapContainValue");
            logicEditorActivity.createPaletteBlock("d", "mapSize");
            logicEditorActivity.createPaletteBlock(" ", "mapRemoveKey");
            logicEditorActivity.createPaletteBlock(" ", "mapClear");
            logicEditorActivity.createPaletteBlock(" ", "mapGetAllKeys");
        }
    }

    public static void primaryBlocksB(
            LogicEditorActivity logicEditorActivity,
            boolean isListNumUsed,
            boolean isListStrUsed,
            boolean isListMapUsed) {
        String eventName = logicEditorActivity.eventName;
        boolean inOnBindCustomViewEvent = eventName.equals("onBindCustomView");
        boolean inOnFilesPickedEvent = eventName.equals("onFilesPicked");
        if (showAll() || isListNumUsed) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_number), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListInt");
            logicEditorActivity.createPaletteBlock("d", "getAtListInt");
            logicEditorActivity.createPaletteBlock("d", "indexListInt");
            logicEditorActivity.createPaletteBlock(" ", "addListInt");
            logicEditorActivity.createPaletteBlock(" ", "insertListInt");
        }
        if (showBuiltIn() && (showAll() || isListNumUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "setAtPosListnum");
        }
        if (showBuiltIn() && (showAll() || isListNumUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "sortListnum");
        }
        if (showAll() || isListStrUsed || inOnFilesPickedEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_string), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListStr");
            logicEditorActivity.createPaletteBlock("d", "indexListStr");
            logicEditorActivity.createPaletteBlock("s", "getAtListStr");
            logicEditorActivity.createPaletteBlock(" ", "addListStr");
            logicEditorActivity.createPaletteBlock(" ", "insertListStr");
        }
        if (showBuiltIn() && (showAll() || isListStrUsed)) {
            logicEditorActivity.createPaletteBlock(" ", "setAtPosListstr");
        }
        if (showAll() || isListStrUsed) {
            logicEditorActivity.createPaletteBlock(" ", "sortList");
        }
        if (showAll() || isListMapUsed || inOnBindCustomViewEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list_map), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock("b", "containListMap");
            logicEditorActivity.createPaletteBlock("s", "getAtListMap");
            if (showBuiltIn()) {
                logicEditorActivity.createPaletteBlock("a", "getMapAtPosListmap");
            }
            logicEditorActivity.createPaletteBlock(" ", "addListMap");
            logicEditorActivity.createPaletteBlock(" ", "insertListMap");
            logicEditorActivity.createPaletteBlock(" ", "setListMap");
            logicEditorActivity.createPaletteBlock(" ", "setMapAtPosListmap");
        }
        if (showAll() || isListMapUsed) {
            logicEditorActivity.createPaletteBlock(" ", "addMapToList");
            logicEditorActivity.createPaletteBlock(" ", "insertMapToList");
            logicEditorActivity.createPaletteBlock(" ", "getMapInList");
            logicEditorActivity.createPaletteBlock(" ", "deleteMapFromListmap");
            logicEditorActivity.createPaletteBlock(" ", "sortListmap");
        }
        if (showAll()
                || isListMapUsed
                || isListStrUsed
                || isListNumUsed
                || inOnBindCustomViewEvent
                || inOnFilesPickedEvent) {
            logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_general), getTitleBgColor(logicEditorActivity));
            logicEditorActivity.createPaletteBlock(" ", "listAddAll");
            logicEditorActivity.createPaletteBlock("d", "lengthList");
            logicEditorActivity.createPaletteBlock(" ", "deleteList");
            logicEditorActivity.createPaletteBlock(" ", "clearList");
            logicEditorActivity.createPaletteBlock(" ", "reverseList");
            if (showBuiltIn()) {
                logicEditorActivity.createPaletteBlock(" ", "shuffleList");
                logicEditorActivity.createPaletteBlock(" ", "swapInList");
            }
        }
    }

    public static void primaryBlocksC(LogicEditorActivity logicEditorActivity) {
        logicEditorActivity.createPaletteBlock("c", "repeat");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("c", "repeatKnownNum");
            logicEditorActivity.createPaletteBlock("c", "RepeatKnownNumDescending");
        }
        logicEditorActivity.createPaletteBlock("c", "forever");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("c", "whileLoop");
            logicEditorActivity.createPaletteBlock("c", "forEachStr");
            logicEditorActivity.createPaletteBlock("c", "forEachNum");
            logicEditorActivity.createPaletteBlock("c", "forEachMap");
        }
        logicEditorActivity.createPaletteBlock("c", "if");
        logicEditorActivity.createPaletteBlock("e", "ifElse");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("e", "ifElseChain");
            logicEditorActivity.createPaletteBlock("c", "elseIf");
            logicEditorActivity.createPaletteBlock("c", "elseBlock");
            logicEditorActivity.createPaletteBlock("b", "instanceOfOperator");
            logicEditorActivity.createPaletteBlock("b", "isEmpty");
            logicEditorActivity.createPaletteBlock("c", "switchStr");
            logicEditorActivity.createPaletteBlock(" ", "caseStrAnd");
            logicEditorActivity.createPaletteBlock("c", "caseStr");
            logicEditorActivity.createPaletteBlock("c", "switchNum");
            logicEditorActivity.createPaletteBlock(" ", "caseNumAnd");
            logicEditorActivity.createPaletteBlock("c", "caseNum");
            logicEditorActivity.createPaletteBlock("c", "defaultSwitch");
            logicEditorActivity.createPaletteBlock("e", "tryCatch");
            logicEditorActivity.createPaletteBlock("c", "catchBlock");
            logicEditorActivity.createPaletteBlock("c", "finallyBlock");
            logicEditorActivity.createPaletteBlock("s", "getExceptionMessage");
            logicEditorActivity.createPaletteBlock("s", "ternaryString");
            logicEditorActivity.createPaletteBlock("d", "ternaryNumber");
            logicEditorActivity.createPaletteBlock("f", "returnString");
            logicEditorActivity.createPaletteBlock("f", "returnNumber");
            logicEditorActivity.createPaletteBlock("f", "returnBoolean");
            logicEditorActivity.createPaletteBlock("f", "returnMap");
            logicEditorActivity.createPaletteBlock("f", "returnListStr");
            logicEditorActivity.createPaletteBlock("f", "returnListMap");
            logicEditorActivity.createPaletteBlock("f", "returnView");
            logicEditorActivity.createPaletteBlock("f", "break");
            logicEditorActivity.createPaletteBlock("f", "continue");
        }
    }

    public static void primaryBlocksD(LogicEditorActivity logicEditorActivity) {
        logicEditorActivity.createPaletteBlock("b", "true");
        logicEditorActivity.createPaletteBlock("b", "false");
        logicEditorActivity.createPaletteBlock("b", "<");
        logicEditorActivity.createPaletteBlock("b", "=");
        logicEditorActivity.createPaletteBlock("b", ">");
        logicEditorActivity.createPaletteBlock("b", "&&");
        logicEditorActivity.createPaletteBlock("b", "||");
        logicEditorActivity.createPaletteBlock("b", "not");
        logicEditorActivity.createPaletteBlock("d", "+");
        logicEditorActivity.createPaletteBlock("d", "-");
        logicEditorActivity.createPaletteBlock("d", "*");
        logicEditorActivity.createPaletteBlock("d", "/");
        logicEditorActivity.createPaletteBlock("d", "%");
        logicEditorActivity.createPaletteBlock("d", "random");
        logicEditorActivity.createPaletteBlock("d", "stringLength");
        logicEditorActivity.createPaletteBlock("s", "stringJoin");
        logicEditorActivity.createPaletteBlock("d", "stringIndex");
        logicEditorActivity.createPaletteBlock("d", "stringLastIndex");
        logicEditorActivity.createPaletteBlock("s", "stringSub");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("s", "stringSubSingle");
        }
        logicEditorActivity.createPaletteBlock("b", "stringEquals");
        logicEditorActivity.createPaletteBlock("b", "stringContains");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("b", "stringMatches");
        }
        logicEditorActivity.createPaletteBlock("s", "stringReplace");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock("s", "stringReplaceFirst");
            logicEditorActivity.createPaletteBlock("s", "stringReplaceAll");
            logicEditorActivity.createPaletteBlock("s", "reverse");
            logicEditorActivity.createPaletteBlock("s", "html");
        }
        logicEditorActivity.createPaletteBlock("s", "trim");
        logicEditorActivity.createPaletteBlock("s", "toUpperCase");
        logicEditorActivity.createPaletteBlock("s", "toLowerCase");
        logicEditorActivity.createPaletteBlock("d", "toNumber");
        logicEditorActivity.createPaletteBlock("d", "strParseInteger");
        logicEditorActivity.createPaletteBlock("d", "toHashCode");
        logicEditorActivity.createPaletteBlock("s", "toString");
        logicEditorActivity.createPaletteBlock("s", "toStringWithDecimal");
        logicEditorActivity.createPaletteBlock("s", "toStringFormat");
        logicEditorActivity.createPaletteBlock(" ", "strToMap");
        logicEditorActivity.createPaletteBlock("s", "mapToStr");
        logicEditorActivity.createPaletteBlock(" ", "strToListMap");
        logicEditorActivity.createPaletteBlock("s", "listMapToStr");
        if (showBuiltIn()) {
            logicEditorActivity.createPaletteBlock(" ", "GsonStringToListString");
            logicEditorActivity.createPaletteBlock(" ", "GsonStringToListNumber");
            logicEditorActivity.createPaletteBlock("s", "GsonListTojsonString");
            logicEditorActivity.createPaletteBlock(" ", "stringSplitToList");
        }
        logicEditorActivity.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_add_source_directly), getTitleBgColor(logicEditorActivity));
        logicEditorActivity.createPaletteBlock(" ", "addSourceDirectly");
        logicEditorActivity.createPaletteBlock("b", "asdBoolean");
        logicEditorActivity.createPaletteBlock("d", "asdNumber");
        logicEditorActivity.createPaletteBlock("s", "asdString");
    }

    private static @ColorInt int getTitleBgColor(LogicEditorActivity logicEditorActivity) {
        return ThemeUtils.getColor(logicEditorActivity, ThemeUtils.isDarkThemeEnabled(logicEditorActivity) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse);
    }
}