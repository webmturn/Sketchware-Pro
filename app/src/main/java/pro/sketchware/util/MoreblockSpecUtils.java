package pro.sketchware.util;

public class MoreblockSpecUtils {

    public static String getMbEnd(String spec) {
        if (spec.equals(" ")) {
            return ";";
        } else {
            return "";
        }
    }

    public static String getMbName(String spec) {
        String name = spec;

        if (spec.contains("[") && spec.contains("]")) {
            name = spec.replaceAll("\\[.*]", "");
        }

        return name;
    }

    public static String getMbType(String spec) {
        try {
            if (!spec.contains("]") || !spec.contains("[")) return "void";

            return spec.substring(spec.indexOf("[") + 1, spec.lastIndexOf("]"));
        } catch (StringIndexOutOfBoundsException e) {
            return "void";
        }
    }

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
