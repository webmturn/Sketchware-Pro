package pro.sketchware.util;

public class XmlUtil {
    public static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'")
                .replace("\n", "&#10;")
                .replace("\r", "&#13;");
    }

    public static String replaceXml(String text) {
        return text.replace("<?xml version=\"1.0\" encoding=\"utf-8\"?>", "")
                .replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "")
                .replace("\t", "");
    }

    public static void saveXml(String path, String xml) {
        FileUtil.writeFile(path, xml);
    }

}
