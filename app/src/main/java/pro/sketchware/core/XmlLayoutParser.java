package pro.sketchware.core;

import android.graphics.Color;
import android.view.ViewGroup;

import com.besome.sketch.beans.ImageBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.TextBean;
import com.besome.sketch.beans.ViewBean;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mod.agus.jcoderz.beans.ViewBeans;

/**
 * Parses Android layout XML into a list of {@link ViewBean} objects that Sketchware-Pro
 * can work with. Handles tag name → type resolution, attribute parsing, nesting,
 * resource references ({@code @color/}, {@code @string/}, {@code @dimen/}), and
 * unknown attribute fallback to {@code viewBean.inject}.
 */
public class XmlLayoutParser {

    /**
     * Maps Android XML class names (simple names) to ViewBean type constants.
     * Built from ViewBean.buildClassInfo() and ViewBeans.views BiMap.
     */
    private static final Map<String, Integer> TAG_TO_TYPE = new HashMap<>();

    static {
        // Core types (ViewBean 0-18)
        TAG_TO_TYPE.put("LinearLayout", ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        TAG_TO_TYPE.put("RelativeLayout", ViewBean.VIEW_TYPE_LAYOUT_RELATIVE);
        TAG_TO_TYPE.put("HorizontalScrollView", ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW);
        TAG_TO_TYPE.put("Button", ViewBean.VIEW_TYPE_WIDGET_BUTTON);
        TAG_TO_TYPE.put("TextView", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
        TAG_TO_TYPE.put("EditText", ViewBean.VIEW_TYPE_WIDGET_EDITTEXT);
        TAG_TO_TYPE.put("ImageView", ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
        TAG_TO_TYPE.put("WebView", ViewBean.VIEW_TYPE_WIDGET_WEBVIEW);
        TAG_TO_TYPE.put("ProgressBar", ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR);
        TAG_TO_TYPE.put("ListView", ViewBean.VIEW_TYPE_WIDGET_LISTVIEW);
        TAG_TO_TYPE.put("Spinner", ViewBean.VIEW_TYPE_WIDGET_SPINNER);
        TAG_TO_TYPE.put("CheckBox", ViewBean.VIEW_TYPE_WIDGET_CHECKBOX);
        TAG_TO_TYPE.put("ScrollView", ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW);
        TAG_TO_TYPE.put("Switch", ViewBean.VIEW_TYPE_WIDGET_SWITCH);
        TAG_TO_TYPE.put("SeekBar", ViewBean.VIEW_TYPE_WIDGET_SEEKBAR);
        TAG_TO_TYPE.put("CalendarView", ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW);
        TAG_TO_TYPE.put("FloatingActionButton", ViewBean.VIEW_TYPE_WIDGET_FAB);
        TAG_TO_TYPE.put("AdView", ViewBean.VIEW_TYPE_WIDGET_ADVIEW);
        TAG_TO_TYPE.put("MapView", ViewBean.VIEW_TYPE_WIDGET_MAPVIEW);

        // Extended types (ViewBeans 19-48)
        TAG_TO_TYPE.put("RadioButton", ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON);
        TAG_TO_TYPE.put("RatingBar", ViewBeans.VIEW_TYPE_WIDGET_RATINGBAR);
        TAG_TO_TYPE.put("VideoView", ViewBeans.VIEW_TYPE_WIDGET_VIDEOVIEW);
        TAG_TO_TYPE.put("SearchView", ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW);
        TAG_TO_TYPE.put("AutoCompleteTextView", ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW);
        TAG_TO_TYPE.put("MultiAutoCompleteTextView", ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW);
        TAG_TO_TYPE.put("GridView", ViewBeans.VIEW_TYPE_WIDGET_GRIDVIEW);
        TAG_TO_TYPE.put("AnalogClock", ViewBeans.VIEW_TYPE_WIDGET_ANALOGCLOCK);
        TAG_TO_TYPE.put("DatePicker", ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER);
        TAG_TO_TYPE.put("TimePicker", ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER);
        TAG_TO_TYPE.put("DigitalClock", ViewBeans.VIEW_TYPE_WIDGET_DIGITALCLOCK);
        TAG_TO_TYPE.put("TabLayout", ViewBeans.VIEW_TYPE_LAYOUT_TABLAYOUT);
        TAG_TO_TYPE.put("ViewPager", ViewBeans.VIEW_TYPE_LAYOUT_VIEWPAGER);
        TAG_TO_TYPE.put("BottomNavigationView", ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW);
        TAG_TO_TYPE.put("BadgeView", ViewBeans.VIEW_TYPE_WIDGET_BADGEVIEW);
        TAG_TO_TYPE.put("PatternLockView", ViewBeans.VIEW_TYPE_WIDGET_PATTERNLOCKVIEW);
        TAG_TO_TYPE.put("WaveSideBar", ViewBeans.VIEW_TYPE_WIDGET_WAVESIDEBAR);
        TAG_TO_TYPE.put("CardView", ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW);
        TAG_TO_TYPE.put("CollapsingToolbarLayout", ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT);
        TAG_TO_TYPE.put("TextInputLayout", ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT);
        TAG_TO_TYPE.put("SwipeRefreshLayout", ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT);
        TAG_TO_TYPE.put("RadioGroup", ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP);
        TAG_TO_TYPE.put("MaterialButton", ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON);
        TAG_TO_TYPE.put("SignInButton", ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON);
        TAG_TO_TYPE.put("CircleImageView", ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW);
        TAG_TO_TYPE.put("LottieAnimationView", ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW);
        TAG_TO_TYPE.put("YoutubePlayerView", ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW);
        TAG_TO_TYPE.put("OTPView", ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW);
        TAG_TO_TYPE.put("CodeView", ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW);
        TAG_TO_TYPE.put("RecyclerView", ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW);
    }

    /**
     * Result of parsing an XML layout.
     */
    public static class ParseResult {
        public final List<ViewBean> viewBeans;
        public final List<String> warnings;

        ParseResult(List<ViewBean> viewBeans, List<String> warnings) {
            this.viewBeans = viewBeans;
            this.warnings = warnings;
        }
    }

    /**
     * Parses an Android layout XML string into a list of ViewBeans.
     *
     * @param xml the layout XML string to parse
     * @return a ParseResult containing the list of ViewBeans and any warnings
     * @throws XmlPullParserException if the XML is malformed
     * @throws IOException if an I/O error occurs
     */
    public static ParseResult parse(String xml) throws XmlPullParserException, IOException {
        return parse(xml, null);
    }

    /**
     * Parses an Android layout XML string into a list of ViewBeans,
     * optionally resolving {@code @dimen/} references using the provided dimens.xml.
     *
     * @param xml      the layout XML string to parse
     * @param dimesXml optional dimens.xml content for resolving @dimen/ references (may be null)
     * @return a ParseResult containing the list of ViewBeans and any warnings
     * @throws XmlPullParserException if the XML is malformed
     * @throws IOException if an I/O error occurs
     */
    public static ParseResult parse(String xml, String dimesXml) throws XmlPullParserException, IOException {
        Map<String, String> dimenMap = null;
        if (dimesXml != null && !dimesXml.trim().isEmpty()) {
            dimenMap = parseDimens(dimesXml);
        }
        List<ViewBean> result = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Integer> idCounters = new HashMap<>();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));

        // Stack to track parent IDs for nesting
        List<String> parentStack = new ArrayList<>();
        // Map view ID → type for setting parentType on children
        Map<String, Integer> idToType = new HashMap<>();
        // Track child count per parent for setting index (ordering within parent)
        Map<String, Integer> childCountPerParent = new HashMap<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // Skip <merge> tags — flatten their children
                if ("merge".equals(tagName)) {
                    parentStack.add("root");
                    eventType = parser.next();
                    continue;
                }

                // Resolve tag name to ViewBean type
                String simpleName = getSimpleName(tagName);
                Integer typeId = TAG_TO_TYPE.get(simpleName);
                boolean isKnownType = typeId != null;

                if (typeId == null) {
                    typeId = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
                    warnings.add("Unknown tag '" + tagName + "' → imported as LinearLayout with convert='" + tagName + "'");
                }

                // Generate unique ID
                String baseName = simpleName.isEmpty() ? "view" : simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
                int count = idCounters.getOrDefault(baseName, 0) + 1;
                idCounters.put(baseName, count);
                String viewId = baseName + count;

                // Extract explicit ID before advancing past START_TAG
                String explicitId = getExplicitId(parser);

                // Create ViewBean
                ViewBean bean = new ViewBean(explicitId != null ? explicitId : viewId, typeId);

                // Set convert for unknown types (preserves original FQN)
                if (!isKnownType) {
                    bean.convert = tagName;
                } else if (tagName.contains(".") && !simpleName.equals(tagName)) {
                    // FQN for a known type — set convert if different from default ClassInfo name
                    bean.convert = tagName;
                }

                // Set parent, parentType, and index
                String parentId;
                if (!parentStack.isEmpty()) {
                    parentId = parentStack.get(parentStack.size() - 1);
                    bean.parent = parentId;
                    bean.parentType = idToType.getOrDefault(parentId, ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
                } else {
                    parentId = "root";
                    bean.parent = "root";
                    bean.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
                }
                int childIndex = childCountPerParent.getOrDefault(parentId, 0);
                bean.index = childIndex;
                childCountPerParent.put(parentId, childIndex + 1);

                // Parse attributes
                parseAttributes(parser, bean, warnings, dimenMap);

                result.add(bean);

                // Track this view's type and push as parent for children
                idToType.put(bean.id, typeId);
                parentStack.add(bean.id);

            } else if (eventType == XmlPullParser.END_TAG) {
                if (!parentStack.isEmpty()) {
                    parentStack.remove(parentStack.size() - 1);
                }
            }

            eventType = parser.next();
        }

        return new ParseResult(result, warnings);
    }

    /**
     * Extracts the simple class name from a potentially fully-qualified name.
     * e.g., "androidx.cardview.widget.CardView" → "CardView"
     */
    private static String getSimpleName(String tagName) {
        int dot = tagName.lastIndexOf('.');
        return dot >= 0 ? tagName.substring(dot + 1) : tagName;
    }

    /**
     * Extracts the android:id value, stripping @+id/ or @id/ prefix.
     * Must be called before parser advances past START_TAG.
     */
    private static String getExplicitId(XmlPullParser parser) {
        String idValue = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "id");
        if (idValue == null) return null;
        if (idValue.startsWith("@+id/")) return idValue.substring(5);
        if (idValue.startsWith("@id/")) return idValue.substring(4);
        return idValue;
    }

    /**
     * Parses all XML attributes into the appropriate ViewBean fields.
     * Unrecognized attributes are appended to viewBean.inject.
     */
    private static void parseAttributes(XmlPullParser parser, ViewBean bean, List<String> warnings,
                                           Map<String, String> dimenMap) {
        StringBuilder injectBuilder = new StringBuilder();
        if (bean.inject != null && !bean.inject.isEmpty()) {
            injectBuilder.append(bean.inject);
        }

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String ns = parser.getAttributeNamespace(i);
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            String prefix = parser.getAttributePrefix(i);

            // Skip xmlns declarations and android:id (handled separately)
            if ("id".equals(name) && "http://schemas.android.com/apk/res/android".equals(ns)) {
                continue;
            }

            boolean handled = tryParseLayoutAttribute(name, value, bean, injectBuilder, warnings, dimenMap)
                    || tryParseTextAttribute(name, value, bean, injectBuilder, warnings, dimenMap)
                    || tryParseImageAttribute(name, value, bean)
                    || tryParseViewAttribute(name, value, bean);

            if (!handled) {
                // Unrecognized attribute → inject
                // Always prefix with \n — inject is concatenated directly after
                // xmlns:tools="..." in InjectAttributeHandler/readAttributesToReplace,
                // so it MUST start with whitespace for valid XML parsing.
                String qualifiedName;
                if (prefix != null && !prefix.isEmpty()) {
                    qualifiedName = prefix + ":" + name;
                } else if (ns != null && !ns.isEmpty()) {
                    qualifiedName = name;
                } else {
                    qualifiedName = name;
                }
                injectBuilder.append("\n").append(qualifiedName).append("=\"").append(value).append("\"");
            }
        }

        String inject = injectBuilder.toString();
        bean.inject = inject.isEmpty() ? null : inject;
    }

    // ====================== Layout Attributes ======================

    private static boolean tryParseLayoutAttribute(String name, String value, ViewBean bean,
                                                    StringBuilder injectBuilder, List<String> warnings,
                                                    Map<String, String> dimenMap) {
        switch (name) {
            case "layout_width":
                return parseDimensionToLayout(name, value, bean, true, injectBuilder, warnings, dimenMap);
            case "layout_height":
                return parseDimensionToLayout(name, value, bean, false, injectBuilder, warnings, dimenMap);
            case "orientation":
                bean.layout.orientation = "horizontal".equals(value)
                        ? LayoutBean.ORIENTATION_HORIZONTAL
                        : LayoutBean.ORIENTATION_VERTICAL;
                return true;
            case "gravity":
                bean.layout.gravity = parseGravity(value);
                return true;
            case "layout_gravity":
                bean.layout.layoutGravity = parseGravity(value);
                return true;
            case "layout_weight":
                bean.layout.weight = parseDpValue(value, 0);
                return true;
            case "elevation":
                bean.layout.elevation = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "weightSum":
                bean.layout.weightSum = parseDpValue(value, 0);
                return true;
            case "layout_marginLeft":
            case "layout_marginStart":
                bean.layout.marginLeft = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginTop":
                bean.layout.marginTop = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginRight":
            case "layout_marginEnd":
                bean.layout.marginRight = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginBottom":
                bean.layout.marginBottom = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_margin":
                int margin = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                bean.layout.marginLeft = margin;
                bean.layout.marginTop = margin;
                bean.layout.marginRight = margin;
                bean.layout.marginBottom = margin;
                return true;
            case "paddingLeft":
            case "paddingStart":
                bean.layout.paddingLeft = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingTop":
                bean.layout.paddingTop = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingRight":
            case "paddingEnd":
                bean.layout.paddingRight = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingBottom":
                bean.layout.paddingBottom = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                return true;
            case "padding":
                int padding = parseDpOrInject(name, value, bean, injectBuilder, warnings, dimenMap);
                bean.layout.paddingLeft = padding;
                bean.layout.paddingTop = padding;
                bean.layout.paddingRight = padding;
                bean.layout.paddingBottom = padding;
                return true;
            case "background":
                return parseBackground(value, bean);
            default:
                return false;
        }
    }

    /**
     * Parses layout_width or layout_height. Handles match_parent, wrap_content, dp values,
     * and @dimen/ references (方案 B: inject override).
     */
    private static boolean parseDimensionToLayout(String attrName, String value, ViewBean bean,
                                                   boolean isWidth, StringBuilder injectBuilder,
                                                   List<String> warnings, Map<String, String> dimenMap) {
        if ("match_parent".equals(value) || "fill_parent".equals(value)) {
            if (isWidth) bean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
            else bean.layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else if ("wrap_content".equals(value)) {
            if (isWidth) bean.layout.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            else bean.layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (value.startsWith("@dimen/")) {
            // Try to resolve via dimenMap first
            if (dimenMap != null) {
                String resolved = dimenMap.get(value.substring("@dimen/".length()));
                if (resolved != null) {
                    int dp = parseDpValue(resolved, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (isWidth) bean.layout.width = dp;
                    else bean.layout.height = dp;
                    return true;
                }
            }
            // Fallback: inject override
            injectBuilder.append("\n").append("android:").append(attrName).append("=\"").append(value).append("\"");
            warnings.add("@dimen/ reference '" + value + "' for " + attrName
                    + " preserved in inject (bean defaults to wrap_content)");
        } else {
            int dp = parseDpValue(value, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (isWidth) bean.layout.width = dp;
            else bean.layout.height = dp;
        }
        return true;
    }

    /**
     * Parses a dp/px value or @dimen/ reference. If @dimen/, tries dimenMap first,
     * then falls back to inject override and returns 0.
     */
    private static int parseDpOrInject(String attrName, String value, ViewBean bean,
                                        StringBuilder injectBuilder, List<String> warnings,
                                        Map<String, String> dimenMap) {
        if (value.startsWith("@dimen/")) {
            String dimenName = value.substring("@dimen/".length());
            if (dimenMap != null && dimenMap.containsKey(dimenName)) {
                return parseDpValue(dimenMap.get(dimenName), 0);
            }
            // Fallback: inject override
            injectBuilder.append("\n").append("android:").append(attrName).append("=\"").append(value).append("\"");
            warnings.add("@dimen/ reference '" + value + "' for " + attrName + " preserved in inject");
            return 0;
        }
        return parseDpValue(value, 0);
    }

    /**
     * Parses a dimens.xml string into a map of dimen name → value string.
     * Example: {@code <dimen name="toolbar_height">56dp</dimen>} → {"toolbar_height": "56dp"}
     */
    public static Map<String, String> parseDimens(String dimesXml) throws XmlPullParserException, IOException {
        Map<String, String> dimenMap = new HashMap<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(dimesXml));

        String currentName = null;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "dimen".equals(parser.getName())) {
                currentName = parser.getAttributeValue(null, "name");
            } else if (eventType == XmlPullParser.TEXT && currentName != null) {
                dimenMap.put(currentName, parser.getText().trim());
                currentName = null;
            } else if (eventType == XmlPullParser.END_TAG) {
                currentName = null;
            }
            eventType = parser.next();
        }
        return dimenMap;
    }

    /**
     * Parses a dimension value like "16dp", "16px", "16sp", "16" to an integer.
     */
    private static int parseDpValue(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        // Strip unit suffixes
        String num = value.replaceAll("(dp|dip|sp|px|pt|in|mm)$", "");
        try {
            return (int) Float.parseFloat(num);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBackground(String value, ViewBean bean) {
        if (value.startsWith("#")) {
            bean.layout.backgroundColor = parseColorValue(value);
            return true;
        } else if (value.startsWith("@color/") || value.startsWith("?attr/") || value.startsWith("?")) {
            bean.layout.backgroundColor = 1; // non-default to trigger LayoutGenerator output
            bean.layout.backgroundResColor = value;
            return true;
        } else if (value.startsWith("@drawable/") || value.startsWith("@mipmap/")) {
            String resName = value.substring(value.indexOf('/') + 1);
            bean.layout.backgroundResource = resName;
            return true;
        }
        // Other background values (e.g., @android:color/) — fall through to inject
        return false;
    }

    // ====================== Text Attributes ======================

    private static boolean tryParseTextAttribute(String name, String value, ViewBean bean,
                                                   StringBuilder injectBuilder, List<String> warnings,
                                                   Map<String, String> dimenMap) {
        switch (name) {
            case "text":
                bean.text.text = value;
                return true;
            case "textSize":
                if (value.startsWith("@dimen/")) {
                    String dimenName = value.substring("@dimen/".length());
                    if (dimenMap != null && dimenMap.containsKey(dimenName)) {
                        bean.text.textSize = parseDpValue(dimenMap.get(dimenName), 12);
                    } else {
                        // Fallback: inject override
                        injectBuilder.append("\n").append("android:textSize=\"").append(value).append("\"");
                        warnings.add("@dimen/ reference '" + value + "' for textSize preserved in inject");
                    }
                } else {
                    bean.text.textSize = parseDpValue(value, 12);
                }
                return true;
            case "textColor":
                if (value.startsWith("#")) {
                    bean.text.textColor = parseColorValue(value);
                } else if (value.startsWith("@color/") || value.startsWith("?attr/") || value.startsWith("?")) {
                    bean.text.textColor = 1; // non-default to trigger output
                    bean.text.resTextColor = value;
                } else {
                    return false; // e.g., @android:color/black → fall through to inject
                }
                return true;
            case "textColorHint":
                if (value.startsWith("#")) {
                    bean.text.hintColor = parseColorValue(value);
                } else if (value.startsWith("@color/") || value.startsWith("?attr/") || value.startsWith("?")) {
                    bean.text.hintColor = 1;
                    bean.text.resHintColor = value;
                } else {
                    return false; // fall through to inject
                }
                return true;
            case "hint":
                bean.text.hint = value;
                return true;
            case "textStyle":
                bean.text.textType = parseTextStyle(value);
                return true;
            case "singleLine":
                bean.text.singleLine = "true".equals(value) ? 1 : 0;
                return true;
            case "lines":
                bean.text.line = parseDpValue(value, 0);
                return true;
            case "maxLines":
                bean.text.line = parseDpValue(value, 0);
                return true;
            default:
                return false;
        }
    }

    private static int parseTextStyle(String value) {
        if (value == null) return TextBean.TEXT_TYPE_NORMAL;
        return switch (value) {
            case "bold" -> TextBean.TEXT_TYPE_BOLD;
            case "italic" -> TextBean.TEXT_TYPE_ITALIC;
            case "bold|italic", "italic|bold" -> TextBean.TEXT_TYPE_BOLDITALIC;
            default -> TextBean.TEXT_TYPE_NORMAL;
        };
    }

    // ====================== Image Attributes ======================

    private static boolean tryParseImageAttribute(String name, String value, ViewBean bean) {
        switch (name) {
            case "src":
            case "srcCompat":
                if (value.startsWith("@drawable/")) {
                    bean.image.resName = value.substring("@drawable/".length());
                    return true;
                } else if (value.startsWith("@mipmap/")) {
                    bean.image.resName = value.substring("@mipmap/".length());
                    return true;
                }
                return false; // other src values (e.g., @color/) → fall through to inject
            case "scaleType":
                bean.image.scaleType = parseScaleType(value);
                return true;
            case "rotation":
                bean.image.rotate = parseDpValue(value, 0);
                return true;
            default:
                return false;
        }
    }

    private static String parseScaleType(String value) {
        if (value == null) return ImageBean.SCALE_TYPE_CENTER;
        return switch (value) {
            case "center" -> ImageBean.SCALE_TYPE_CENTER;
            case "fitXY" -> ImageBean.SCALE_TYPE_FIT_XY;
            case "fitStart" -> ImageBean.SCALE_TYPE_FIT_START;
            case "fitEnd" -> ImageBean.SCALE_TYPE_FIT_END;
            case "centerCrop" -> ImageBean.SCALE_TYPE_CENTER_CROP;
            case "centerInside" -> ImageBean.SCALE_TYPE_CENTER_INSIDE;
            case "fitCenter" -> ImageBean.SCALE_TYPE_FIT_CENTER;
            default -> ImageBean.SCALE_TYPE_CENTER;
        };
    }

    // ====================== View Attributes ======================

    private static boolean tryParseViewAttribute(String name, String value, ViewBean bean) {
        switch (name) {
            case "alpha":
                try {
                    bean.alpha = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
                return true;
            case "clickable":
                bean.clickable = "true".equals(value) ? 1 : 0;
                return true;
            case "enabled":
                bean.enabled = "true".equals(value) ? 1 : 0;
                return true;
            case "checked":
                bean.checked = "true".equals(value) ? 1 : 0;
                return true;
            default:
                return false;
        }
    }

    // ====================== Utility ======================

    private static int parseGravity(String value) {
        if (value == null || value.isEmpty()) return LayoutBean.GRAVITY_NONE;
        int gravity = 0;
        for (String part : value.split("\\|")) {
            gravity |= switch (part.trim()) {
                case "center" -> LayoutBean.GRAVITY_CENTER;
                case "center_horizontal" -> LayoutBean.GRAVITY_CENTER_HORIZONTAL;
                case "center_vertical" -> LayoutBean.GRAVITY_CENTER_VERTICAL;
                case "left", "start" -> LayoutBean.GRAVITY_LEFT;
                case "right", "end" -> LayoutBean.GRAVITY_RIGHT;
                case "top" -> LayoutBean.GRAVITY_TOP;
                case "bottom" -> LayoutBean.GRAVITY_BOTTOM;
                default -> 0;
            };
        }
        return gravity;
    }

    /**
     * Parses a hex color string (#RGB, #RRGGBB, #AARRGGBB) to an integer.
     * Returns the full ARGB color value (alpha preserved).
     */
    private static int parseColorValue(String value) {
        try {
            return Color.parseColor(value);
        } catch (IllegalArgumentException e) {
            return 0xFFFFFFFF;
        }
    }
}
