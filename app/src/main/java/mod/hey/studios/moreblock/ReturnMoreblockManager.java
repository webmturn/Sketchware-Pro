package mod.hey.studios.moreblock;

import android.util.Pair;

import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.chip.ChipGroup;

import java.util.Iterator;

import pro.sketchware.R;

public class ReturnMoreblockManager {

    public static String getLogicEditorTitle(String str) {
        return str.replaceAll("\\[.*]", " (returns " + getMbTypeList(str) + ")");
    }

    public static String getMbEnd(String str) {
        if (str.equals(" ")) {
            return ";";
        } else {
            return "";
        }
    }

    public static String getMbName(String str) {
        String name = str;

        if (str.contains("[") && str.contains("]")) {
            name = str.replaceAll("\\[.*]", "");
        }

        return name;
    }

    public static String getMbType(String str) {
        try {
            if (!str.contains("]") || !str.contains("[")) return "void";

            return str.substring(str.indexOf("[") + 1, str.lastIndexOf("]"));
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

    public static String getMbTypeList(String str) {
        try {
            if (!str.contains("]") || !str.contains("["))
                return "void";

            String substring = str.substring(str.indexOf("[") + 1, str.lastIndexOf("]"));
            if (substring.contains("|")) {
                substring = substring.split("\\|")[1];
            }

            return substring;
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

    public static String getMbTypeCode(String str) {
        try {
            if (!str.contains("]") || !str.contains("[")) return "void";

            String substring = str.substring(str.indexOf("[") + 1, str.lastIndexOf("]"));
            if (substring.contains("|")) {
                String[] split = substring.split("\\|");

                if (substring.equals("a|Map")) {
                    substring = "HashMap<String, Object>";

                } else if (substring.equals("l|List String")) {
                    substring = "ArrayList<String>";

                } else if (substring.equals("l|List Map")) {
                    substring = "ArrayList<HashMap<String, Object>>";

                } else if (split.length == 2) {
                    substring = split[1];

                } else if (split.length == 3) {
                    substring = split[2];
                }
            }

            return substring;
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

    public static String getMbTypeFromChipGroup(ChipGroup chipGroup) {
        String type;
        int checkedChipId = chipGroup.getCheckedChipId();
        if (checkedChipId == R.id.radio_mb_type_string) {
            type = "s";
        } else if (checkedChipId == R.id.radio_mb_type_number) {
            type = "d";
        } else if (checkedChipId == R.id.radio_mb_type_boolean) {
            type = "b";
        } else if (checkedChipId == R.id.radio_mb_type_map) {
            type = "a|Map";
        } else if (checkedChipId == R.id.radio_mb_type_liststring) {
            type = "l|List String";
        } else if (checkedChipId == R.id.radio_mb_type_listmap) {
            type = "l|List Map";
        } else if (checkedChipId == R.id.radio_mb_type_view) {
            type = "v|View";
        } else {
            type = " ";
        }
        return type;
    }


    public static void listMoreblocks(Iterator<Pair<String, String>> it, LogicEditorActivity logicEditorActivity) {
        while (it.hasNext()) {
            String spec = it.next().second;
            String moreblockChar = getMoreblockChar(spec);

            if (moreblockChar.contains("|")) {
                String[] split = moreblockChar.split("\\|");
                logicEditorActivity.createPaletteBlockWithComponent(getMbName(spec), split[0], split[1], "definedFunc").setTag(getMbName(spec));
            } else {
                logicEditorActivity.createPaletteBlockWithSpec(getMbName(spec), moreblockChar, "definedFunc").setTag(getMbName(spec));
            }
        }
    }

    public static String getMoreblockChar(String spec) {
        String moreBlockChar;
        String mbType = getMbType(spec);

        if (mbType.equals("void")) {
            moreBlockChar = " ";
        } else if (mbType.equals("String")) {
            moreBlockChar = "s";
        } else if (mbType.equals("double")) {
            moreBlockChar = "d";
        } else if (mbType.equals("boolean")) {
            moreBlockChar = "b";
        } else if (mbType.contains("|")) {
            return mbType;
        } else {
            moreBlockChar = " ";
        }

        return moreBlockChar;
    }

    public static String getMoreblockType(String spec) {
        String type = getMoreblockChar(spec);
        if (!type.contains("|")) return type;
        String[] splits = type.split("\\|");
        return splits[0];
    }

    public static String injectMbType(String name, String mbName, String typeChar) {
        String result = name;

        if (!typeChar.equals(" ")) {
            if (typeChar.contains("|")) {
                result = injectToMbName(result, mbName, typeChar);

            } else if (typeChar.equals("s")) {
                result = injectToMbName(result, mbName, "String");

            } else if (typeChar.equals("d")) {
                result = injectToMbName(result, mbName, "double");

            } else if (typeChar.equals("b")) {
                result = injectToMbName(result, mbName, "boolean");
            }
        }

        return result;

    }

    public static String injectToMbName(String name, String mbName, String typeName) {
        String replaceFirst;

        if (name.equals(mbName)) {
            replaceFirst = name + "[" + typeName + "]";
        } else {
            replaceFirst = name.replaceFirst(mbName, mbName + "[" + typeName + "]");
        }

        return replaceFirst;
    }

    public static String getPreviewType(String typeChar) {
        if (typeChar.contains("|")) return "a";

        return typeChar;
    }

    public static String getMbNameWithTypeFromSpec(String spec) {
        if (spec.contains("[") && spec.contains("]")) {
            return spec.substring(0, spec.lastIndexOf(']') + 1);
        } else {
            String name = spec;

            if (spec.contains(" ")) {
                name = name.substring(0, name.indexOf(' '));
            }

            return name;
        }
    }
}
