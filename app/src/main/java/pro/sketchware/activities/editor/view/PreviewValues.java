package pro.sketchware.activities.editor.view;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.util.LogUtil;
import pro.sketchware.util.XmlUtil;

public final class PreviewValues {
    public record Style(String base, Map<String, String> attributes) {}

    private final Context context;
    private final File defaults;
    private final File night;
    private final ColorResourceResolver colors;
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Style> styles = new HashMap<>();
    private boolean loaded;

    public PreviewValues(Context context, File defaults, File night, ColorResourceResolver colors) {
        this.context = context;
        this.defaults = defaults;
        this.night = night;
        this.colors = colors;
    }

    public Map<String, String> resolveAttributes(Map<String, String> attributes) {
        Map<String, String> resolved = new HashMap<>();
        attributes.forEach((name, value) -> resolved.put(name, resolve(value)));
        return resolved;
    }

    public Integer color(String value) {
        return colors.resolveColorInt(context, value, 3, night != null);
    }

    void removeAttributesOverriddenBy(Map<String, String> attributes, int style) {
        if (style == 0) return;
        Resources resources = context.getResources();
        Resources.Theme overlay = resources.newTheme();
        overlay.applyStyle(style, true);
        var value = new TypedValue();
        attributes
                .keySet()
                .removeIf(
                        name -> {
                            int attr =
                                    resources.getIdentifier(name, "attr", context.getPackageName());
                            return attr != 0 && overlay.resolveAttribute(attr, value, false);
                        });
    }

    String resolveColor(String value, Map<String, String> theme) {
        String color = resolve(value);
        var seen = new HashSet<String>();
        while (color != null && color.startsWith("?") && seen.add(color)) {
            String attr = color.startsWith("?attr/") ? color.substring(6) : color.substring(1);
            String next = theme.get(attr);
            if (next == null) break;
            color = resolve(next);
        }
        return color;
    }

    private String resolve(String value) {
        if (value == null) return null;
        String resolved = value.trim();
        if (!resolved.startsWith("@")) return resolved;
        var seen = new HashSet<String>();
        while (resolved.startsWith("@") && seen.add(resolved)) {
            int separator = resolved.indexOf('/');
            if (separator == -1) return resolved;
            boolean framework = resolved.startsWith("@android:");
            String type = resolved.substring(framework ? "@android:".length() : 1, separator);
            switch (type) {
                case "bool", "integer", "dimen", "color" -> {}
                default -> {
                    return resolved;
                }
            }
            if (framework) {
                return type.equals("color") ? resolved : frameworkValue(resolved);
            }
            load();
            String next = values.get(resolved);
            if (next == null) return resolved;
            resolved = next;
        }
        return resolved;
    }

    public Style resolveStyle(String name) {
        Map<String, String> attributes = new HashMap<>();
        String base = name == null ? "" : name.trim();
        var seen = new HashSet<String>();
        while (base.startsWith("@style/") && seen.add(base)) {
            load();
            Style style = styles.get(base);
            if (style == null) break;
            style.attributes().forEach(attributes::putIfAbsent);
            base = style.base();
        }
        return new Style(base, attributes);
    }

    private String frameworkValue(String reference) {
        int id = context.getResources().getIdentifier(reference.substring(1), null, null);
        if (id == 0) return reference;
        var value = new TypedValue();
        try {
            context.getResources().getValue(id, value, true);
        } catch (Resources.NotFoundException ignored) {
            return reference;
        }
        if (value.type == TypedValue.TYPE_INT_HEX) return Integer.toString(value.data);
        if (value.type != TypedValue.TYPE_STRING) {
            CharSequence literal = value.coerceToString();
            if (literal != null) return literal.toString();
        }
        return reference;
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        if (defaults == null && night == null) return;
        try {
            DocumentBuilder parser = XmlUtil.newSecureDocumentBuilderFactory().newDocumentBuilder();
            readVariants(defaults, parser, false);
            readVariants(night, parser, true);
        } catch (ParserConfigurationException error) {
            LogUtil.w("PreviewValues", "Could not create resource XML parser", error);
        }
    }

    private void readVariants(File directory, DocumentBuilder parser, boolean nightVariant) {
        if (directory == null) return;
        readDirectory(
                directory,
                parser,
                colors.loadColorsXmlIfChanged(new File(directory, "colors.xml"), nightVariant));

        File parent = directory.getParentFile();
        File[] siblings = parent == null ? null : parent.listFiles(File::isDirectory);
        if (siblings == null) return;
        Pattern version = Pattern.compile(Pattern.quote(directory.getName()) + "-v([0-9]+)");
        var compatible = new TreeMap<Integer, File>();
        for (File sibling : siblings) {
            var match = version.matcher(sibling.getName());
            if (!match.matches()) continue;
            try {
                int sdk = Integer.parseInt(match.group(1));
                if (sdk > 0 && sdk <= Build.VERSION.SDK_INT) compatible.put(sdk, sibling);
            } catch (NumberFormatException ignored) {
                // This directory does not name an Android API level.
            }
        }
        for (File variant : compatible.values()) readDirectory(variant, parser, null);
    }

    private void readDirectory(File directory, DocumentBuilder parser, Element colorsXml) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        if (files == null) return;
        for (File file : files) {
            try {
                Element root =
                        colorsXml != null && file.getName().equals("colors.xml")
                                ? colorsXml
                                : parser.parse(file).getDocumentElement();
                if (root == null) continue;
                for (var child = root.getFirstChild();
                        child != null;
                        child = child.getNextSibling()) {
                    if (child instanceof Element entry) readEntry(entry);
                }
            } catch (IOException | SAXException error) {
                LogUtil.w("PreviewValues", "Could not read " + file, error);
            }
        }
    }

    private void readEntry(Element entry) {
        String name = entry.getAttribute("name");
        String type = entry.getTagName();
        if (type.equals("item")) type = entry.getAttribute("type");
        switch (type) {
            case "bool", "integer", "dimen", "color" ->
                    values.put("@" + type + "/" + name, entry.getTextContent().trim());
            case "style" -> styles.put("@style/" + name, readStyle(entry, name));
        }
    }

    private Style readStyle(Element entry, String name) {
        String parent = entry.getAttribute("parent").trim();
        if (parent.startsWith("android:style/")) {
            parent = "@" + parent;
        } else if (!parent.isEmpty() && !parent.startsWith("@") && !parent.startsWith("?")) {
            parent = "@style/" + parent;
        } else if (!entry.hasAttribute("parent") && name.contains(".")) {
            parent = "@style/" + name.substring(0, name.lastIndexOf('.'));
        }
        Map<String, String> attributes = new HashMap<>();
        for (var child = entry.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element item && item.getTagName().equals("item")) {
                attributes.put(item.getAttribute("name"), item.getTextContent().trim());
            }
        }
        return new Style(parent, attributes);
    }
}
