package pro.sketchware.activities.settings.fragments.language;

import android.util.Xml;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class TranslationXml {
    record Document(String name, String fallbackTag, Map<String, String> strings) {}

    private TranslationXml() {}

    static Document read(InputStream input) throws IOException, XmlPullParserException {
        var parser = Xml.newPullParser();
        parser.setInput(input, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "resources");
        String name = parser.getAttributeValue(null, "name");
        String fallback = parser.getAttributeValue(null, "fallback");
        var strings = new TreeMap<String, String>();
        for (int event = parser.next();
                event != XmlPullParser.END_DOCUMENT;
                event = parser.next()) {
            if (event != XmlPullParser.START_TAG
                    || parser.getDepth() != 2
                    || !"string".equals(parser.getName())) continue;
            String key = parser.getAttributeValue(null, "name");
            var text = new StringBuilder();
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.END_TAG && parser.getDepth() == 2) break;
                if (event == XmlPullParser.TEXT) text.append(parser.getText());
            }
            parser.require(XmlPullParser.END_TAG, null, "string");
            if (key != null) {
                String value = text.toString();
                strings.put(key, decodeAndroidEscapes(value));
            }
        }
        return new Document(name, fallback, strings);
    }

    static String write(Document document) throws IOException {
        var output = new StringWriter();
        var xml = Xml.newSerializer();
        xml.setOutput(output);
        xml.startDocument("UTF-8", null);
        xml.startTag(null, "resources");
        xml.attribute(null, "fallback", document.fallbackTag());
        if (document.name() != null) xml.attribute(null, "name", document.name());
        for (var entry : new TreeMap<>(document.strings()).entrySet()) {
            xml.text("\n    ");
            xml.startTag(null, "string");
            xml.attribute(null, "name", entry.getKey());
            xml.text(encodeAndroidEscapes(entry.getValue()));
            xml.endTag(null, "string");
        }
        xml.text("\n");
        xml.endTag(null, "resources");
        xml.endDocument();
        return output.toString();
    }

    private static String encodeAndroidEscapes(String value) {
        String escaped =
                value.replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\"", "\\\"")
                        .replace("\r", "\\u000d")
                        .replace("\n", "\\n")
                        .replace("\t", "\\t");
        String trimmed = escaped.trim();
        if (trimmed.startsWith("@") || trimmed.startsWith("?")) {
            int start = escaped.indexOf(trimmed);
            return escaped.substring(0, start) + "\\" + escaped.substring(start);
        }
        return escaped;
    }

    private static String decodeAndroidEscapes(String value) {
        var result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char next = value.charAt(i);
            if (next == '\\' && i + 1 < value.length()) {
                switch (value.charAt(i + 1)) {
                    case 'n' -> next = '\n';
                    case 'r' -> next = '\r';
                    case 't' -> next = '\t';
                    case '\\', '\'', '"', '@', '?' -> next = value.charAt(i + 1);
                    case 'u' -> {
                        if (i + 5 < value.length()) {
                            try {
                                result.append(
                                        (char) Integer.parseInt(value.substring(i + 2, i + 6), 16));
                                i += 5;
                                continue;
                            } catch (NumberFormatException ignored) {
                                // Keep unknown escapes exactly as written by older packs.
                            }
                        }
                        result.append(next);
                        continue;
                    }
                    default -> {
                        result.append(next);
                        continue;
                    }
                }
                i++;
            }
            result.append(next);
        }
        return result.toString();
    }
}
