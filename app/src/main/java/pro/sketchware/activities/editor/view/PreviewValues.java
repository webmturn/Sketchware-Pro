package pro.sketchware.activities.editor.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.util.LogUtil;
import pro.sketchware.util.XmlUtil;

public final class PreviewValues {
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    public record Style(String base, Map<String, String> attributes) {}

    private final Context context;
    private final File defaults;
    private final File night;
    private final ColorResourceResolver colors;
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Style> styles = new HashMap<>();
    private final Map<String, Element> selectors = new HashMap<>();
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

    String resolveThemeValue(String value, Map<String, String> theme) {
        String resolved = resolve(value);
        var seen = new HashSet<String>();
        while (resolved != null && resolved.startsWith("?") && seen.add(resolved)) {
            String next = theme.get(attributeName(resolved));
            if (next == null) break;
            resolved = resolve(next);
        }
        return resolved;
    }

    private static String attributeName(String reference) {
        return reference.substring(reference.startsWith("?attr/") ? 6 : 1);
    }

    int styleResource(String reference) {
        Resources resources = context.getResources();
        if (reference.startsWith("@style/")) {
            return resources.getIdentifier(
                    reference.substring(7), "style", context.getPackageName());
        }
        if (reference.startsWith("?")) {
            int attribute =
                    resources.getIdentifier(
                            attributeName(reference), "attr", context.getPackageName());
            var value = new TypedValue();
            if (attribute != 0
                    && context.getTheme().resolveAttribute(attribute, value, true)
                    && value.resourceId != 0
                    && resources.getResourceTypeName(value.resourceId).equals("style")) {
                return value.resourceId;
            }
        }
        return 0;
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

    ColorStateList colorSelector(
            String reference,
            Map<String, String> theme,
            Function<String, Integer> colorForReference) {
        return colorSelector(reference, theme, colorForReference, new HashSet<>());
    }

    private ColorStateList colorSelector(
            String reference,
            Map<String, String> theme,
            Function<String, Integer> colorForReference,
            Set<String> seen) {
        if (defaults == null
                || reference == null
                || !reference.matches("@color/[a-z0-9_]+")
                || !seen.add(reference)) {
            return null;
        }
        try {
            String name = reference.substring(7);
            if (!selectors.containsKey(name)) selectors.put(name, readSelector(name));
            Element selector = selectors.get(name);
            if (selector == null) return null;

            var stateSpecs = new ArrayList<int[]>();
            var itemColors = new ArrayList<Integer>();
            // ColorStateList uses the first matching item, so keep the XML order.
            for (var child = selector.getFirstChild();
                    child != null;
                    child = child.getNextSibling()) {
                if (!(child instanceof Element item) || !item.getTagName().equals("item")) continue;
                String colorReference =
                        resolveThemeValue(item.getAttributeNS(ANDROID_NS, "color"), theme);
                Integer color = colorForReference.apply(colorReference);
                if (color == null) {
                    ColorStateList nested =
                            colorSelector(colorReference, theme, colorForReference, seen);
                    if (nested == null) continue;
                    // A selector item uses one color. A nested selector supplies its default.
                    color = nested.getDefaultColor();
                }
                stateSpecs.add(selectorStates(item, theme));
                itemColors.add(applySelectorAlpha(color, item, theme));
            }
            if (itemColors.isEmpty()) return null;
            return new ColorStateList(
                    stateSpecs.toArray(new int[0][]),
                    itemColors.stream().mapToInt(Integer::intValue).toArray());
        } finally {
            seen.remove(reference);
        }
    }

    private int[] selectorStates(Element item, Map<String, String> theme) {
        var attributes = item.getAttributes();
        int[] states = new int[attributes.getLength()];
        int count = 0;
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!ANDROID_NS.equals(attribute.getNamespaceURI())
                    || !attribute.getLocalName().startsWith("state_")) continue;
            int id =
                    context.getResources()
                            .getIdentifier(attribute.getLocalName(), "attr", "android");
            if (id != 0) {
                String value = resolveThemeValue(attribute.getValue(), theme);
                states[count++] = Boolean.parseBoolean(value) ? id : -id;
            }
        }
        return Arrays.copyOf(states, count);
    }

    private int applySelectorAlpha(int color, Element item, Map<String, String> theme) {
        String alpha = item.getAttributeNS(ANDROID_NS, "alpha");
        if (alpha.isEmpty()) return color;
        try {
            float multiplier = Float.parseFloat(resolveThemeValue(alpha, theme));
            int opacity = Math.max(0, Math.min(255, Math.round(Color.alpha(color) * multiplier)));
            return (color & 0x00ffffff) | (opacity << 24);
        } catch (NumberFormatException ignored) {
            return color;
        }
    }

    private Element readSelector(String name) {
        File root = defaults.getParentFile();
        File file = night == null ? null : colorFile(new File(root, "color-night"), name);
        if (file == null) file = colorFile(new File(root, "color"), name);
        if (file == null) return null;
        try {
            var factory = XmlUtil.newSecureDocumentBuilderFactory();
            factory.setNamespaceAware(true);
            Element element = factory.newDocumentBuilder().parse(file).getDocumentElement();
            return element != null && element.getTagName().equals("selector") ? element : null;
        } catch (IOException | SAXException | ParserConfigurationException error) {
            LogUtil.w("PreviewValues", "Could not read " + file, error);
            return null;
        }
    }

    private static File colorFile(File directory, String name) {
        String filename = name + ".xml";
        for (File variant : versionedDirectories(directory).descendingMap().values()) {
            File file = new File(variant, filename);
            if (file.isFile()) return file;
        }
        File file = new File(directory, filename);
        return file.isFile() ? file : null;
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
        for (File variant : versionedDirectories(directory).values()) {
            readDirectory(variant, parser, null);
        }
    }

    private static TreeMap<Integer, File> versionedDirectories(File directory) {
        var compatible = new TreeMap<Integer, File>();
        File parent = directory.getParentFile();
        File[] siblings = parent == null ? null : parent.listFiles(File::isDirectory);
        if (siblings == null) return compatible;
        Pattern version = Pattern.compile(Pattern.quote(directory.getName()) + "-v([0-9]+)");
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
        return compatible;
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
