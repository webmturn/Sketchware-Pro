package pro.sketchware.core.codegen;
import pro.sketchware.core.ClassInfo;

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
 * can work with. Handles tag name 鈫?type resolution, attribute parsing, nesting,
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
        TAG_TO_TYPE.put("MaterialCardView", ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW);
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
        // Map view ID 鈫?type for setting parentType on children
        Map<String, Integer> idToType = new HashMap<>();
        // Track child count per parent for setting index (ordering within parent)
        Map<String, Integer> childCountPerParent = new HashMap<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // Skip <merge> tags 鈥?flatten their children
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
                    warnings.add("Unknown tag '" + tagName + "' 鈫?imported as LinearLayout with convert='" + tagName + "'");
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
                    // FQN for a known type 鈥?set convert if different from default ClassInfo name
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
     * e.g., "androidx.cardview.widget.CardView" 鈫?"CardView"
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
    private static void parseAttributes(XmlPullParser parser, ViewBean viewBean, List<String> warnings,
                                           Map<String, String> dimenMap) {
        StringBuilder injectBuilder = new StringBuilder();
        if (viewBean.inject != null && !viewBean.inject.isEmpty()) {
            injectBuilder.append(viewBean.inject);
        }

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String namespaceUri = parser.getAttributeNamespace(i);
            String attrName = parser.getAttributeName(i);
            String attrValue = parser.getAttributeValue(i);
            String attrPrefix = parser.getAttributePrefix(i);

            // Skip xmlns declarations and android:id (handled separately)
            if ("id".equals(attrName) && "http://schemas.android.com/apk/res/android".equals(namespaceUri)) {
                continue;
            }

            boolean handled = tryParseLayoutAttribute(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap)
                    || tryParseTextAttribute(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap)
                    || tryParseImageAttribute(attrName, attrValue, viewBean)
                    || tryParseViewAttribute(attrName, attrValue, viewBean);

            if (!handled) {
                // Unrecognized attribute 鈫?inject
                // Always prefix with \n 鈥?inject is concatenated directly after
                // xmlns:tools="..." in InjectAttributeHandler/readAttributesToReplace,
                // so it MUST start with whitespace for valid XML parsing.
                String qualifiedName;
                if (attrPrefix != null && !attrPrefix.isEmpty()) {
                    qualifiedName = attrPrefix + ":" + attrName;
                } else if (namespaceUri != null && !namespaceUri.isEmpty()) {
                    qualifiedName = attrName;
                } else {
                    qualifiedName = attrName;
                }
                injectBuilder.append("\n").append(qualifiedName).append("=\"").append(attrValue).append("\"");
            }
        }

        String inject = injectBuilder.toString();
        viewBean.inject = inject.isEmpty() ? null : inject;
    }

    // ====================== Layout Attributes ======================

    private static boolean tryParseLayoutAttribute(String attrName, String attrValue, ViewBean viewBean,
                                                    StringBuilder injectBuilder, List<String> warnings,
                                                    Map<String, String> dimenMap) {
        switch (attrName) {
            case "layout_width":
                return parseDimensionToLayout(attrName, attrValue, viewBean, true, injectBuilder, warnings, dimenMap);
            case "layout_height":
                return parseDimensionToLayout(attrName, attrValue, viewBean, false, injectBuilder, warnings, dimenMap);
            case "orientation":
                viewBean.layout.orientation = "horizontal".equals(attrValue)
                        ? LayoutBean.ORIENTATION_HORIZONTAL
                        : LayoutBean.ORIENTATION_VERTICAL;
                return true;
            case "gravity":
                viewBean.layout.gravity = parseGravity(attrValue);
                return true;
            case "layout_gravity":
                viewBean.layout.layoutGravity = parseGravity(attrValue);
                return true;
            case "layout_weight":
                viewBean.layout.weight = parseDpValue(attrValue, 0);
                return true;
            case "cardElevation":
                if (viewBean.type == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW) {
                    viewBean.layout.elevation = parseCardElevationOrInject(attrValue, injectBuilder, warnings, dimenMap);
                    return true;
                }
                return false;
            case "elevation":
                viewBean.layout.elevation = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "weightSum":
                viewBean.layout.weightSum = parseDpValue(attrValue, 0);
                return true;
            case "layout_marginLeft":
            case "layout_marginStart":
                viewBean.layout.marginLeft = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginTop":
                viewBean.layout.marginTop = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginRight":
            case "layout_marginEnd":
                viewBean.layout.marginRight = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_marginBottom":
                viewBean.layout.marginBottom = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "layout_margin":
                int margin = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                viewBean.layout.marginLeft = margin;
                viewBean.layout.marginTop = margin;
                viewBean.layout.marginRight = margin;
                viewBean.layout.marginBottom = margin;
                return true;
            case "paddingLeft":
            case "paddingStart":
                viewBean.layout.paddingLeft = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingTop":
                viewBean.layout.paddingTop = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingRight":
            case "paddingEnd":
                viewBean.layout.paddingRight = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "paddingBottom":
                viewBean.layout.paddingBottom = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                return true;
            case "padding":
                int padding = parseDpOrInject(attrName, attrValue, viewBean, injectBuilder, warnings, dimenMap);
                viewBean.layout.paddingLeft = padding;
                viewBean.layout.paddingTop = padding;
                viewBean.layout.paddingRight = padding;
                viewBean.layout.paddingBottom = padding;
                return true;
            case "cardBackgroundColor":
                if (viewBean.type == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW) {
                    return parseColorBackedBackground(attrValue, viewBean);
                }
                return false;
            case "backgroundTint":
                if (viewBean.type == ViewBeans.VIEW_TYPE_WIDGET_MATERIALBUTTON) {
                    return parseColorBackedBackground(attrValue, viewBean);
                }
                return false;
            case "contentScrim":
                if (viewBean.type == ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT) {
                    return parseColorBackedBackground(attrValue, viewBean);
                }
                return false;
            case "background":
                return parseBackground(attrValue, viewBean);
            default:
                return false;
        }
    }

    /**
     * Parses layout_width or layout_height. Handles match_parent, wrap_content, dp values,
     * and @dimen/ references (鏂规 B: inject override).
     */
    private static boolean parseDimensionToLayout(String attrName, String attrValue, ViewBean viewBean,
                                                   boolean isWidth, StringBuilder injectBuilder,
                                                   List<String> warnings, Map<String, String> dimenMap) {
        if ("match_parent".equals(attrValue) || "fill_parent".equals(attrValue)) {
            if (isWidth) viewBean.layout.width = ViewGroup.LayoutParams.MATCH_PARENT;
            else viewBean.layout.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else if ("wrap_content".equals(attrValue)) {
            if (isWidth) viewBean.layout.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            else viewBean.layout.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (attrValue.startsWith("@dimen/")) {
            // Try to resolve via dimenMap first
            if (dimenMap != null) {
                String resolved = dimenMap.get(attrValue.substring("@dimen/".length()));
                if (resolved != null) {
                    int dp = parseDpValue(resolved, ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (isWidth) viewBean.layout.width = dp;
                    else viewBean.layout.height = dp;
                    return true;
                }
            }
            // Fallback: inject override
            injectBuilder.append("\n").append("android:").append(attrName).append("=\"").append(attrValue).append("\"");
            warnings.add("@dimen/ reference '" + attrValue + "' for " + attrName
                    + " preserved in inject (bean defaults to wrap_content)");
        } else {
            int dp = parseDpValue(attrValue, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (isWidth) viewBean.layout.width = dp;
            else viewBean.layout.height = dp;
        }
        return true;
    }

    /**
     * Parses a dp/px value or @dimen/ reference. If @dimen/, tries dimenMap first,
     * then falls back to inject override and returns 0.
     */
    private static int parseDpOrInject(String attrName, String attrValue, ViewBean viewBean,
                                        StringBuilder injectBuilder, List<String> warnings,
                                        Map<String, String> dimenMap) {
        if (attrValue.startsWith("@dimen/")) {
            String dimenName = attrValue.substring("@dimen/".length());
            if (dimenMap != null && dimenMap.containsKey(dimenName)) {
                return parseDpValue(dimenMap.get(dimenName), 0);
            }
            // Fallback: inject override
            injectBuilder.append("\n").append("android:").append(attrName).append("=\"").append(attrValue).append("\"");
            warnings.add("@dimen/ reference '" + attrValue + "' for " + attrName + " preserved in inject");
            return 0;
        }
        return parseDpValue(attrValue, 0);
    }

    private static int parseCardElevationOrInject(String attrValue, StringBuilder injectBuilder,
                                                  List<String> warnings, Map<String, String> dimenMap) {
        if (attrValue.startsWith("@dimen/")) {
            String dimenName = attrValue.substring("@dimen/".length());
            if (dimenMap != null && dimenMap.containsKey(dimenName)) {
                return parseDpValue(dimenMap.get(dimenName), 0);
            }
            injectBuilder.append("\n").append("app:cardElevation=\"").append(attrValue).append("\"");
            warnings.add("@dimen/ reference '" + attrValue + "' for cardElevation preserved in inject");
            return 0;
        }
        return parseDpValue(attrValue, 0);
    }

    /**
     * Parses a dimens.xml string into a map of dimen name 鈫?value string.
     * Example: {@code <dimen name="toolbar_height">56dp</dimen>} 鈫?{"toolbar_height": "56dp"}
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
    private static int parseDpValue(String dimensionValue, int defaultValue) {
        if (dimensionValue == null || dimensionValue.isEmpty()) return defaultValue;
        // Strip unit suffixes
        String numericPortion = dimensionValue.replaceAll("(dp|dip|sp|px|pt|in|mm)$", "");
        try {
            return (int) Float.parseFloat(numericPortion);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBackground(String backgroundValue, ViewBean viewBean) {
        if (backgroundValue.startsWith("#")) {
            viewBean.layout.backgroundColor = parseColorValue(backgroundValue);
            return true;
        } else if (backgroundValue.startsWith("@color/") || backgroundValue.startsWith("?attr/") || backgroundValue.startsWith("?")) {
            viewBean.layout.backgroundColor = 1; // non-default to trigger LayoutGenerator output
            viewBean.layout.backgroundResColor = backgroundValue;
            return true;
        } else if (backgroundValue.startsWith("@drawable/") || backgroundValue.startsWith("@mipmap/")) {
            String resName = backgroundValue.substring(backgroundValue.indexOf('/') + 1);
            viewBean.layout.backgroundResource = resName;
            return true;
        }
        // Other background values (e.g., @android:color/) 鈥?fall through to inject
        return false;
    }

    private static boolean parseColorBackedBackground(String backgroundValue, ViewBean viewBean) {
        if (backgroundValue.startsWith("#")) {
            viewBean.layout.backgroundColor = parseColorValue(backgroundValue);
            viewBean.layout.backgroundResColor = null;
            return true;
        }
        if (backgroundValue.startsWith("@color/") || backgroundValue.startsWith("?attr/") || backgroundValue.startsWith("?")) {
            viewBean.layout.backgroundColor = 1;
            viewBean.layout.backgroundResColor = backgroundValue;
            return true;
        }
        if ("@android:color/transparent".equals(backgroundValue)) {
            viewBean.layout.backgroundColor = 0;
            viewBean.layout.backgroundResColor = null;
            return true;
        }
        return false;
    }

    // ====================== Text Attributes ======================

    private static boolean tryParseTextAttribute(String attrName, String attrValue, ViewBean viewBean,
                                                   StringBuilder injectBuilder, List<String> warnings,
                                                   Map<String, String> dimenMap) {
        switch (attrName) {
            case "text":
                viewBean.text.text = attrValue;
                return true;
            case "textSize":
                if (attrValue.startsWith("@dimen/")) {
                    String dimenName = attrValue.substring("@dimen/".length());
                    if (dimenMap != null && dimenMap.containsKey(dimenName)) {
                        viewBean.text.textSize = parseDpValue(dimenMap.get(dimenName), 12);
                    } else {
                        // Fallback: inject override
                        injectBuilder.append("\n").append("android:textSize=\"").append(attrValue).append("\"");
                        warnings.add("@dimen/ reference '" + attrValue + "' for textSize preserved in inject");
                    }
                } else {
                    viewBean.text.textSize = parseDpValue(attrValue, 12);
                }
                return true;
            case "textColor":
                if (attrValue.startsWith("#")) {
                    viewBean.text.textColor = parseColorValue(attrValue);
                } else if (attrValue.startsWith("@color/") || attrValue.startsWith("?attr/") || attrValue.startsWith("?")) {
                    viewBean.text.textColor = 1; // non-default to trigger output
                    viewBean.text.resTextColor = attrValue;
                } else {
                    return false; // e.g., @android:color/black 鈫?fall through to inject
                }
                return true;
            case "textColorHint":
                if (attrValue.startsWith("#")) {
                    viewBean.text.hintColor = parseColorValue(attrValue);
                } else if (attrValue.startsWith("@color/") || attrValue.startsWith("?attr/") || attrValue.startsWith("?")) {
                    viewBean.text.hintColor = 1;
                    viewBean.text.resHintColor = attrValue;
                } else {
                    return false; // fall through to inject
                }
                return true;
            case "hint":
                viewBean.text.hint = attrValue;
                return true;
            case "textStyle":
                viewBean.text.textType = parseTextStyle(attrValue);
                return true;
            case "singleLine":
                viewBean.text.singleLine = "true".equals(attrValue) ? 1 : 0;
                return true;
            case "lines":
                viewBean.text.line = parseDpValue(attrValue, 0);
                return true;
            case "maxLines":
                viewBean.text.line = parseDpValue(attrValue, 0);
                return true;
            default:
                return false;
        }
    }

    private static int parseTextStyle(String textStyleValue) {
        if (textStyleValue == null) return TextBean.TEXT_TYPE_NORMAL;
        return switch (textStyleValue) {
            case "bold" -> TextBean.TEXT_TYPE_BOLD;
            case "italic" -> TextBean.TEXT_TYPE_ITALIC;
            case "bold|italic", "italic|bold" -> TextBean.TEXT_TYPE_BOLDITALIC;
            default -> TextBean.TEXT_TYPE_NORMAL;
        };
    }

    // ====================== Image Attributes ======================

    private static boolean tryParseImageAttribute(String attrName, String attrValue, ViewBean viewBean) {
        switch (attrName) {
            case "src":
            case "srcCompat":
                if (attrValue.startsWith("@drawable/")) {
                    viewBean.image.resName = attrValue.substring("@drawable/".length());
                    return true;
                } else if (attrValue.startsWith("@mipmap/")) {
                    viewBean.image.resName = attrValue.substring("@mipmap/".length());
                    return true;
                }
                return false; // other src values (e.g., @color/) 鈫?fall through to inject
            case "scaleType":
                viewBean.image.scaleType = parseScaleType(attrValue);
                return true;
            case "rotation":
                viewBean.image.rotate = parseDpValue(attrValue, 0);
                return true;
            default:
                return false;
        }
    }

    private static String parseScaleType(String scaleTypeValue) {
        if (scaleTypeValue == null) return ImageBean.SCALE_TYPE_CENTER;
        return switch (scaleTypeValue) {
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

    private static boolean tryParseViewAttribute(String attrName, String attrValue, ViewBean viewBean) {
        switch (attrName) {
            case "alpha":
                try {
                    viewBean.alpha = Float.parseFloat(attrValue);
                } catch (NumberFormatException ignored) {
                }
                return true;
            case "clickable":
                viewBean.clickable = "true".equals(attrValue) ? 1 : 0;
                return true;
            case "enabled":
                viewBean.enabled = "true".equals(attrValue) ? 1 : 0;
                return true;
            case "checked":
                viewBean.checked = "true".equals(attrValue) ? 1 : 0;
                return true;
            default:
                return false;
        }
    }

    // ====================== Utility ======================

    private static int parseGravity(String gravityValue) {
        if (gravityValue == null || gravityValue.isEmpty()) return LayoutBean.GRAVITY_NONE;
        int gravity = 0;
        for (String part : gravityValue.split("\\|")) {
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
    private static int parseColorValue(String colorString) {
        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException e) {
            return 0xFFFFFFFF;
        }
    }
}
