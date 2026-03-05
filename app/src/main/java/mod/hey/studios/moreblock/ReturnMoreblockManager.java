package mod.hey.studios.moreblock;

import android.util.Pair;

import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.chip.ChipGroup;

import java.util.Iterator;

import pro.sketchware.R;

/**
 * Utility class for managing MoreBlocks with return values.
 * <p>
 * MoreBlock specs can include a return type annotation in square brackets
 * (e.g. {@code "getData[String] %s.key"}). This class provides methods to:
 * <ul>
 *   <li>Extract the return type from a spec ({@link #getMbType(String)}, {@link #getMbTypeCode(String)})</li>
 *   <li>Extract the function name ({@link #getMbName(String)})</li>
 *   <li>Determine the block shape character ({@link #getMoreblockChar(String)})</li>
 *   <li>Inject/remove type annotations ({@link #injectMbType(String, String, String)})</li>
 *   <li>List MoreBlocks in the palette ({@link #listMoreblocks(Iterator, LogicEditorActivity)})</li>
 * </ul>
 *
 * @see mod.hey.studios.moreblock.MoreblockValidator
 */
public class ReturnMoreblockManager {

    /**
     * Returns a display title for the logic editor, replacing the type annotation
     * with a human-readable "(returns TYPE)" suffix.
     *
     * @param spec the MoreBlock spec string
     * @return the formatted title string
     */
    public static String getLogicEditorTitle(String spec) {
        return spec.replaceAll("\\[.*]", " (returns " + getMbTypeList(spec) + ")");
    }

    /**
     * Returns the statement terminator for a MoreBlock call.
     *
     * @param spec the block type character (space for void, other for return type)
     * @return {@code ";"} for void MoreBlocks, empty string for expression MoreBlocks
     */
    public static String getMbEnd(String spec) {
        if (spec.equals(" ")) {
            return ";";
        } else {
            return "";
        }
    }

    /**
     * Extracts the function name from a MoreBlock spec, stripping the
     * return type annotation {@code [type]} if present.
     *
     * @param spec the full MoreBlock spec
     * @return the function name portion
     */
    public static String getMbName(String spec) {
        String name = spec;

        if (spec.contains("[") && spec.contains("]")) {
            name = spec.replaceAll("\\[.*]", "");
        }

        return name;
    }

    /**
     * Extracts the raw return type annotation from a MoreBlock spec.
     * Returns {@code "void"} if no type annotation is present.
     *
     * @param spec the full MoreBlock spec
     * @return the type string (e.g. {@code "String"}, {@code "a|Map"}, {@code "void"})
     */
    public static String getMbType(String spec) {
        try {
            if (!spec.contains("]") || !spec.contains("[")) return "void";

            return spec.substring(spec.indexOf("[") + 1, spec.lastIndexOf("]"));
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

    /**
     * Extracts the display-friendly return type name from a MoreBlock spec.
     * For compound types like {@code "a|Map"}, returns just {@code "Map"}.
     *
     * @param spec the full MoreBlock spec
     * @return the display type name, or {@code "void"} if none
     */
    public static String getMbTypeList(String spec) {
        try {
            if (!spec.contains("]") || !spec.contains("["))
                return "void";

            String substring = spec.substring(spec.indexOf("[") + 1, spec.lastIndexOf("]"));
            if (substring.contains("|")) {
                substring = substring.split("\\|")[1];
            }

            return substring;
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

    /**
     * Converts the MoreBlock return type annotation to its Java code equivalent.
     * <ul>
     *   <li>{@code "a|Map"} → {@code "HashMap<String, Object>"}</li>
     *   <li>{@code "l|List String"} → {@code "ArrayList<String>"}</li>
     *   <li>{@code "l|List Map"} → {@code "ArrayList<HashMap<String, Object>>"}</li>
     *   <li>Other compound types use the last segment after {@code "|"}</li>
     * </ul>
     *
     * @param spec the full MoreBlock spec
     * @return the Java type string for code generation
     */
    public static String getMbTypeCode(String spec) {
        try {
            if (!spec.contains("]") || !spec.contains("[")) return "void";

            String substring = spec.substring(spec.indexOf("[") + 1, spec.lastIndexOf("]"));
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


    /**
     * Iterates over all MoreBlock definitions and adds them to the logic editor's
     * palette as draggable block templates.
     *
     * @param it                  iterator over (name, spec) pairs of MoreBlock definitions
     * @param logicEditorActivity the logic editor to add palette blocks to
     */
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

    /**
     * Returns the block shape type character based on the MoreBlock's return type.
     * <ul>
     *   <li>{@code " "} (space) → void (statement block)</li>
     *   <li>{@code "s"} → String expression</li>
     *   <li>{@code "d"} → double expression</li>
     *   <li>{@code "b"} → boolean expression</li>
     *   <li>Compound type (e.g. {@code "a|Map"}) → returned as-is</li>
     * </ul>
     *
     * @param spec the full MoreBlock spec
     * @return the block type character
     */
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

    /**
     * Injects the return type annotation back into a MoreBlock name.
     * Converts type characters to their full type names
     * (e.g. {@code "s"} → {@code "String"}).
     *
     * @param name     the current spec string
     * @param mbName   the function name to locate in the spec
     * @param typeChar the type character ({@code "s"}, {@code "d"}, {@code "b"}, or compound)
     * @return the spec with {@code [type]} annotation injected after the function name
     */
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
