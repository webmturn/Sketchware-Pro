package pro.sketchware.core;

import static com.besome.sketch.editor.property.PropertyAttributesItem.RELATIVE_IDS;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.besome.sketch.beans.ImageBean;
import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.TextBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.manage.library.material3.Material3LibraryManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import dev.aldi.sayuti.editor.injection.AppCompatInjection;
import mod.agus.jcoderz.beans.ViewBeans;
import mod.jbk.util.LogUtil;
import pro.sketchware.managers.inject.InjectRootLayoutManager;
import pro.sketchware.utility.InjectAttributeHandler;
import pro.sketchware.xml.XmlBuilder;

@SuppressLint("RtlHardcoded")
public class LayoutGenerator {

    private final BuildConfig buildConfig;
    private final InjectRootLayoutManager rootManager;
    private final AppCompatInjection aci;
    private final ProjectFileBean projectFile;
    private ViewBean fab;
    private ArrayList<ViewBean> views;
    private XmlBuilder rootLayout = null;
    private XmlBuilder collapsingToolbarLayout = null;
    private boolean excludeAppCompat;

    public LayoutGenerator(BuildConfig buildConfig, ProjectFileBean projectFileBean) {
        this.buildConfig = buildConfig;
        projectFile = projectFileBean;
        rootManager = new InjectRootLayoutManager(buildConfig.sc_id);
        aci = new AppCompatInjection(buildConfig, projectFileBean);
    }

    public static String formatColor(int color) {
        int alpha = (color >> 24) & 0xff;

        if (alpha != 0xff) {
            return String.format("#%06X", color);
        } else {
            return String.format("#%08X", color);
        }
    }

    /**
     * @return The parameter String escaped properly for XML strings
     */
    private String escapeXML(String input) {
        CharBuffer buffer = CharBuffer.wrap(input);
        StringBuilder result = new StringBuilder(input.length());
        while (buffer.hasRemaining()) {
            char got = buffer.get();
            switch (got) {
                case '?' -> result.append("\\?");
                case '@' -> result.append("\\@");
                case '\"' -> result.append("&quot;");
                case '&' -> result.append("&amp;");
                case '<' -> result.append("&lt;");
                case '>' -> result.append("&gt;");
                case '\n' -> result.append("\\n");
                default -> result.append(got);
            }
        }
        return result.toString();
    }

    public void setExcludeAppCompat(boolean exclude) {
        excludeAppCompat = exclude;
    }

    private void writeRootLayout() {
        var root = rootManager.getLayoutByFileName(projectFile.getXmlName());
        XmlBuilder xmlTag = new XmlBuilder(root.getClassName());
        var rootAttributes = root.getAttributes();
        if (!rootAttributes.containsKey("android:layout_width")) {
            xmlTag.addAttribute("android", "layout_width", "match_parent");
        }
        if (!rootAttributes.containsKey("android:layout_height")) {
            xmlTag.addAttribute("android", "layout_height", "match_parent");
        }
        for (Map.Entry<String, String> entry : rootAttributes.entrySet()) {
            xmlTag.addAttribute(null, entry.getKey(), entry.getValue());
        }
        for (ViewBean viewBean : views) {
            String parent = viewBean.parent;
            if (parent == null || parent.isEmpty() || parent.equals("root")) {
                writeWidget(xmlTag, viewBean);
            }
        }
        if (!excludeAppCompat && buildConfig.isAppCompatEnabled) {
            if (projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)) {
                    if (!root.getAttributes().containsKey("app:layout_behavior")) {
                        xmlTag.addAttribute("app", "layout_behavior", "@string/appbar_scrolling_view_behavior");
                    }
                }
                if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)
                        || projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                    XmlBuilder coordinatorLayoutTag = new XmlBuilder("androidx.coordinatorlayout.widget.CoordinatorLayout");
                    coordinatorLayoutTag.addAttribute("android", "id", "@+id/_coordinator");
                    aci.inject(coordinatorLayoutTag, "CoordinatorLayout");
                    rootLayout = coordinatorLayoutTag;
                }
                if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)) {
                    Material3LibraryManager materialLibraryManager = new Material3LibraryManager(buildConfig.sc_id);

                    XmlBuilder toolbarTag = new XmlBuilder(
                            (materialLibraryManager.isMaterial3Enabled()) ? "com.google.android.material.appbar.MaterialToolbar" : "androidx.appcompat.widget.Toolbar"
                    );
                    toolbarTag.addAttribute("android", "id", "@+id/_toolbar");
                    aci.inject(toolbarTag, "Toolbar");
                    XmlBuilder appBarLayoutTag = new XmlBuilder("com.google.android.material.appbar.AppBarLayout");
                    appBarLayoutTag.addAttribute("android", "id", "@+id/_app_bar");
                    aci.inject(appBarLayoutTag, "AppBarLayout");
                    if (collapsingToolbarLayout != null) {
                        collapsingToolbarLayout.addChildNode(toolbarTag);
                        appBarLayoutTag.addChildNode(collapsingToolbarLayout);
                    } else {
                        appBarLayoutTag.addChildNode(toolbarTag);
                    }
                    rootLayout.addChildNode(appBarLayoutTag);
                    rootLayout.addChildNode(xmlTag);
                } else {
                    if (rootLayout == null) {
                        rootLayout = xmlTag;
                    } else {
                        rootLayout.addChildNode(xmlTag);
                    }
                }
                if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB)) {
                    writeFabView(rootLayout, fab);
                }
                if (fab.type == ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW) {
                    writeWidget(rootLayout, fab);
                }
                if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                    XmlBuilder drawerLayoutTag = new XmlBuilder("androidx.drawerlayout.widget.DrawerLayout");
                    drawerLayoutTag.addAttribute("android", "id", "@+id/_drawer");
                    aci.inject(drawerLayoutTag, "DrawerLayout");
                    drawerLayoutTag.addChildNode(rootLayout);
                    XmlBuilder linearLayoutTag = new XmlBuilder("LinearLayout");
                    linearLayoutTag.addAttribute("android", "id", "@+id/_nav_view");
                    aci.inject(linearLayoutTag, "NavigationDrawer");
                    XmlBuilder includeTag = new XmlBuilder("include", true);
                    includeTag.addAttribute("", "layout", "@layout/_drawer_" + projectFile.fileName);
                    includeTag.addAttribute("android", "id", "@+id/drawer");
                    linearLayoutTag.addChildNode(includeTag);
                    drawerLayoutTag.addChildNode(linearLayoutTag);
                    rootLayout = drawerLayoutTag;
                }
            } else {
                rootLayout = xmlTag;
            }
        } else {
            rootLayout = xmlTag;
        }
        rootLayout.addNamespaceDeclaration(0, "xmlns", "tools", "http://schemas.android.com/tools");
        rootLayout.addNamespaceDeclaration(0, "xmlns", "app", "http://schemas.android.com/apk/res-auto");
        rootLayout.addNamespaceDeclaration(0, "xmlns", "android", "http://schemas.android.com/apk/res/android");
    }

    private void writeBackgroundResource(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        String backgroundResource = viewBean.layout.backgroundResource;
        int type = viewBean.type;

        if (backgroundResource == null || "NONE".equalsIgnoreCase(backgroundResource)) {
            int backgroundColor = viewBean.layout.backgroundColor;
            String backgroundResColor = viewBean.layout.backgroundResColor;

            if (backgroundColor != 0xffffff) {
                if (backgroundColor != 0) {
                    int color = backgroundColor & 0xffffff;
                    if (xmlTag.getCleanRootElementName().equals("BottomAppBar")) {
                        if (!toNotAdd.contains("app:backgroundTint") && !injectHandler.contains("backgroundTint") && (backgroundResColor != null)) {
                            if (backgroundResColor.startsWith("?") || backgroundResColor.startsWith("@color/")) {
                                xmlTag.addAttribute("app", "backgroundTint", backgroundResColor);
                            } else {
                                xmlTag.addAttribute("app", "backgroundTint", "@color/" + backgroundResColor);
                            }
                        } else if (!toNotAdd.contains("app:backgroundTint") && !injectHandler.contains("backgroundTint")) {
                            xmlTag.addAttribute("app", "backgroundTint", formatColor(color));
                        }
                    } else if (type == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW) {
                        if (!toNotAdd.contains("app:cardBackgroundColor") && !injectHandler.contains("cardBackgroundColor") && backgroundResColor != null) {
                            if (backgroundResColor.startsWith("?") || backgroundResColor.startsWith("@color/")) {
                                xmlTag.addAttribute("app", "cardBackgroundColor", backgroundResColor);
                            } else {
                                xmlTag.addAttribute("app", "cardBackgroundColor", "@color/" + backgroundResColor);
                            }
                        } else if (!toNotAdd.contains("app:cardBackgroundColor") && !injectHandler.contains("cardBackgroundColor")) {
                            xmlTag.addAttribute("app", "cardBackgroundColor", formatColor(color));
                        }
                    } else if (xmlTag.getCleanRootElementName().equals("MaterialButton")) {
                        if (!toNotAdd.contains("app:backgroundTint") && !injectHandler.contains("backgroundTint")) {
                            xmlTag.addAttribute("app", "backgroundTint", backgroundResColor == null ? formatColor(color) : backgroundResColor);
                        }
                    } else if (xmlTag.getCleanRootElementName().equals("CollapsingToolbarLayout")) {
                        if (!toNotAdd.contains("app:contentScrim") && !injectHandler.contains("contentScrim") && backgroundResColor != null) {
                            if (backgroundResColor.startsWith("?") || backgroundResColor.startsWith("@color/")) {
                                xmlTag.addAttribute("app", "contentScrim", backgroundResColor);
                            } else {
                                xmlTag.addAttribute("app", "contentScrim", "@color/" + backgroundResColor);
                            }
                        } else if (!toNotAdd.contains("app:contentScrim") && !injectHandler.contains("contentScrim")) {
                            xmlTag.addAttribute("app", "contentScrim", formatColor(color));
                        }
                    } else {
                        if (!hasAttr("background", viewBean) && !toNotAdd.contains("android:background") && !injectHandler.contains("background") && backgroundResColor != null) {
                            if (backgroundResColor.startsWith("?") || backgroundResColor.startsWith("@color/")) {
                                xmlTag.addAttribute("android", "background", backgroundResColor);
                            } else {
                                xmlTag.addAttribute("android", "background", "@color/" + backgroundResColor);
                            }
                        } else if (!hasAttr("background", viewBean) && !toNotAdd.contains("android:background") && !injectHandler.contains("background")) {
                            xmlTag.addAttribute("android", "background", formatColor(color));
                        }
                    }
                } else if (xmlTag.getCleanRootElementName().equals("BottomAppBar")) {
                    if (!toNotAdd.contains("android:backgroundTint") && !injectHandler.contains("backgroundTint")) {
                        xmlTag.addAttribute("android", "backgroundTint", "@android:color/transparent");
                    }
                } else if (xmlTag.getCleanRootElementName().equals("CollapsingToolbarLayout")) {
                    if (!toNotAdd.contains("app:contentScrim") && !injectHandler.contains("contentScrim")) {
                        xmlTag.addAttribute("app", "contentScrim", "?attr/colorPrimary");
                    }
                } else if (type == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW) {
                    if (!toNotAdd.contains("app:cardBackgroundColor") && !injectHandler.contains("cardBackgroundColor")) {
                        xmlTag.addAttribute("app", "cardBackgroundColor", "@android:color/transparent");
                    }
                } else {
                    if (!toNotAdd.contains("android:background") && !injectHandler.contains("background")) {
                        xmlTag.addAttribute("android", "background", "@android:color/transparent");
                    }
                }
            }
        } else {
            if (!hasAttr("background", viewBean) && !toNotAdd.contains("android:background") && !injectHandler.contains("background")) {
                boolean isNinePatchBackground = backgroundResource.endsWith(".9");
                xmlTag.addAttribute("android", "background", "@drawable/" +
                        (isNinePatchBackground ? backgroundResource.replaceAll("\\.9", "") :
                                backgroundResource));
            }
        }
    }

    public void setViews(ArrayList<ViewBean> arrayList) {
        setViews(arrayList, null);
    }

    public void setViews(ArrayList<ViewBean> arrayList, ViewBean viewBean) {
        fab = viewBean;
        views = arrayList;
        writeRootLayout();
    }

    public String toXmlString() {
        return rootLayout.toCode();
    }

    private void writeWidget(XmlBuilder xmlTag, ViewBean viewBean) {
        viewBean.getClassInfo().getClassName();
        String convert = viewBean.convert;
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);

        XmlBuilder widgetTag = convert.isEmpty() ? new XmlBuilder(viewBean.getClassInfo().getClassName()) :
                new XmlBuilder(convert.replaceAll(" ", ""));
        if (convert.equals("include")) {
            if (!toNotAdd.contains("layout") && !injectHandler.contains("layout")) {
                widgetTag.addAttribute("", "layout", "@layout/" + viewBean.id);
            }
        } else {
            if (!toNotAdd.contains("android:id")) {
                widgetTag.addAttribute("android", "id", "@+id/" + viewBean.id);
            }
            int type = viewBean.type;
            if (projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW) {
                switch (type) {
                    case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW:
                    case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT:
                    case ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW:
                    case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR:
                    case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX:
                    case ViewBean.VIEW_TYPE_WIDGET_SWITCH:
                    case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR:
                    case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW:
                    case ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON:
                    case ViewBeans.VIEW_TYPE_WIDGET_SEARCHVIEW:
                    case ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW:
                    case ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW:
                    case ViewBeans.VIEW_TYPE_LAYOUT_BOTTOMNAVIGATIONVIEW:
                        if (!hasAttr("focusable", viewBean) && !toNotAdd.contains("android:focusable") && !injectHandler.contains("focusable")) {
                            widgetTag.addAttribute("android", "focusable", "false");
                        }
                        break;

                    default:
                        break;
                }
            }

            if (!toNotAdd.contains("android:layout_width") && !injectHandler.contains("layout_width")) {
                int width = viewBean.layout.width;
                if (width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    widgetTag.addAttribute("android", "layout_width", "match_parent");
                } else if (width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    widgetTag.addAttribute("android", "layout_width", "wrap_content");
                } else {
                    widgetTag.addAttribute("android", "layout_width", width + "dp");
                }
            }

            if (!toNotAdd.contains("android:layout_height") && !injectHandler.contains("layout_height")) {
                int height = viewBean.layout.height;
                if (height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    widgetTag.addAttribute("android", "layout_height", "match_parent");
                } else if (height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    widgetTag.addAttribute("android", "layout_height", "wrap_content");
                } else {
                    widgetTag.addAttribute("android", "layout_height", height + "dp");
                }
            }

            writeLayoutMargin(widgetTag, viewBean);
            if (type == ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW) {
                writeCardViewPadding(widgetTag, viewBean);
            } else {
                writeViewPadding(widgetTag, viewBean);
            }
            writeBackgroundResource(widgetTag, viewBean);
            if (viewBean.getClassInfo().matchesType("ViewGroup")) {
                writeViewGravity(widgetTag, viewBean);
            }
        }
        if (viewBean.getClassInfo().matchesType("LinearLayout") &&
                !widgetTag.getCleanRootElementName().matches("(BottomAppBar|NavigationView|Coordinator|Floating|Collaps|include)\\w*")) {
            if (!toNotAdd.contains("android:orientation") && !injectHandler.contains("orientation")) {
                int orientation = viewBean.layout.orientation;
                if (orientation == LinearLayout.HORIZONTAL) {
                    widgetTag.addAttribute("android", "orientation", "horizontal");
                } else if (orientation == LinearLayout.VERTICAL) {
                    widgetTag.addAttribute("android", "orientation", "vertical");
                }
            }

            if (!toNotAdd.contains("android:weightSum") && !injectHandler.contains("weightSum")) {
                int weightSum = viewBean.layout.weightSum;
                if (weightSum > 0) {
                    widgetTag.addAttribute("android", "weightSum", String.valueOf(weightSum));
                }
            }
        }
        if (viewBean.getClassInfo().matchesType("TextView")) {
            writeViewGravity(widgetTag, viewBean);
            writeTextAttributes(widgetTag, viewBean);
        }
        if (viewBean.getClassInfo().matchesType("ImageView")) {
            writeImgSrcAttr(widgetTag, viewBean);
            if (!widgetTag.toCode().contains(".")) {
                writeImageScaleType(widgetTag, viewBean);
            }
        }
        if (viewBean.getClassInfo().isExactType("SeekBar")) {
            writeViewGravity(widgetTag, viewBean);
        }
        if (viewBean.getClassInfo().isExactType("ProgressBar")) {
            writeViewGravity(widgetTag, viewBean);
        }
        if (viewBean.getClassInfo().isExactType("WaveSideBar")) {
            int textSize = viewBean.text.textSize;
            if (textSize > 0 && !toNotAdd.contains("app:sidebar_text_size")) {
                widgetTag.addAttribute("app", "sidebar_text_size", textSize + "sp");
            }

            int textColor = viewBean.text.textColor;
            String resTextColor = viewBean.text.resTextColor;
            if (textColor != 0 && !toNotAdd.contains("app:sidebar_text_color") && resTextColor != null) {
                if (resTextColor.startsWith("?") || resTextColor.startsWith("@color/")) {
                    widgetTag.addAttribute("app", "sidebar_text_color", resTextColor);
                } else {
                    widgetTag.addAttribute("app", "sidebar_text_color", "@color/" + resTextColor);
                }
            } else if (textColor != 0 && !toNotAdd.contains("app:sidebar_text_color")) {
                widgetTag.addAttribute("app", "sidebar_text_color", formatColor(textColor & 0xffffff));
            }
        }
        addCommonAttributes(widgetTag, viewBean);
        int parentViewType = viewBean.parentType;
        if (!viewBean.convert.equals("include")) {
            if (parentViewType == ViewBean.VIEW_TYPE_LAYOUT_LINEAR) {
                writeLayoutGravity(widgetTag, viewBean);
                int weight = viewBean.layout.weight;
                if (weight > 0 && !toNotAdd.contains("android:layout_weight") && !injectHandler.contains("layout_weight")) {
                    widgetTag.addAttribute("android", "layout_weight", String.valueOf(weight));
                }
            } else if (parentViewType == ViewBean.VIEW_TYPE_LAYOUT_HSCROLLVIEW || parentViewType == ViewBean.VIEW_TYPE_LAYOUT_VSCROLLVIEW) {
                writeLayoutGravity(widgetTag, viewBean);
            }
        }
        if (viewBean.getClassInfo().matchesType("ViewGroup")) {
            for (ViewBean bean : views) {
                if (bean.parent != null && bean.parent.equals(viewBean.id)) {
                    writeWidget(widgetTag, bean);
                }
            }
        }
        if (!viewBean.inject.isEmpty()) {
            widgetTag.addAttributeValue(viewBean.inject.replaceAll(" ", ""));
        }

        if (!viewBean.parentAttributes.isEmpty()) {
            viewBean.parentAttributes.forEach((key, value) -> {
                String[] parts = key.split(":");
                widgetTag.addAttribute(parts[0], parts[1], RELATIVE_IDS.contains(key) ? "@id/" + value : value);
            });
        }

        if (widgetTag.getCleanRootElementName().equals("CollapsingToolbarLayout")
                && buildConfig.isAppCompatEnabled
                && projectFile.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
            if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)) {
                if (collapsingToolbarLayout == null) {
                    collapsingToolbarLayout = widgetTag;
                    return;
                }
            }
        }
        // Adding tools:listitem allows the direct XML editor to recognize the customView
        // for ListView, GridView, Spinner, or RecyclerView.
        if ((viewBean.getClassInfo().isExactType("ListView")
                || viewBean.getClassInfo().isExactType("GridView")
                || viewBean.getClassInfo().isExactType("Spinner")
                || viewBean.getClassInfo().isExactType("RecyclerView")
                || viewBean.getClassInfo().isExactType("ViewPager"))
                && !injectHandler.contains("listitem")) {
            var customView = viewBean.customView;
            if (customView != null && !customView.isEmpty() && !customView.equals("none")) {
                widgetTag.addAttribute("tools", "listitem", "@layout/" + customView);
            }
        }
        xmlTag.addChildNode(widgetTag);
    }

    private void writeFabView(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        XmlBuilder floatingActionButtonTag = new XmlBuilder("com.google.android.material.floatingactionbutton.FloatingActionButton");
        if (!toNotAdd.contains("android:id")) {
            floatingActionButtonTag.addAttribute("android", "id", "@+id/" + viewBean.id);
        }
        if (!toNotAdd.contains("android:layout_width") && !injectHandler.contains("layout_width")) {
            floatingActionButtonTag.addAttribute("android", "layout_width", "wrap_content");
        }
        if (!toNotAdd.contains("android:layout_height") && !injectHandler.contains("layout_height")) {
            floatingActionButtonTag.addAttribute("android", "layout_height", "wrap_content");
        }
        writeLayoutMargin(floatingActionButtonTag, viewBean);
        writeLayoutGravity(floatingActionButtonTag, viewBean);

        String resName = viewBean.image.resName;
        if (resName != null && !resName.isEmpty() && !resName.equals("NONE") &&
                !toNotAdd.contains("app:srcCompat") && !injectHandler.contains("srcCompat")) {
            floatingActionButtonTag.addAttribute("app", "srcCompat", "@drawable/" + resName.toLowerCase());
        }
        if (viewBean.id.equals("_fab")) {
            aci.inject(floatingActionButtonTag, "FloatingActionButton");
        }
        addCommonAttributes(floatingActionButtonTag, viewBean);
        xmlTag.addChildNode(floatingActionButtonTag);
    }

    private void writeViewGravity(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        if (!toNotAdd.contains("android:gravity") && !injectHandler.contains("gravity")) {
            int gravity = viewBean.layout.gravity;
            if (gravity != Gravity.NO_GRAVITY) {
                String attrValue = "";
                int verticalGravity = gravity & Gravity.FILL_VERTICAL;
                int horizontalGravity = gravity & Gravity.FILL_HORIZONTAL;
                if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
                    attrValue = "center_horizontal";
                } else {
                    if ((horizontalGravity & Gravity.LEFT) == Gravity.LEFT) {
                        attrValue = "left";
                    }
                    if ((horizontalGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "right";
                    }
                }
                if (verticalGravity == Gravity.CENTER_VERTICAL) {
                    if (!attrValue.isEmpty()) {
                        attrValue += "|";
                    }
                    attrValue += "center_vertical";
                } else {
                    if ((verticalGravity & Gravity.TOP) == Gravity.TOP) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "top";
                    }
                    if ((verticalGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "bottom";
                    }
                }
                xmlTag.addAttribute("android", "gravity", attrValue);
            }
        }
    }

    private void writeImgSrcAttr(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        String resName = viewBean.image.resName;
        if (!resName.isEmpty() && !"NONE".equals(resName)) {
            String value = "@drawable/" + resName.toLowerCase();
            if (xmlTag.getCleanRootElementName().equals("FloatingActionButton")) {
                if (!toNotAdd.contains("app:srcCompat") && !injectHandler.contains("srcCompat")) {
                    xmlTag.addAttribute("app", "srcCompat", value);
                }
            } else {
                if (!toNotAdd.contains("android:src") && !injectHandler.contains("src")) {
                    xmlTag.addAttribute("android", "src", value);
                }
            }
        }
    }

    /**
     * @see ImageView.ScaleType
     */
    private void writeImageScaleType(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        if (!toNotAdd.contains("android:scaleType") && !injectHandler.contains("scaleType")) {
            if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_CENTER)) {
                xmlTag.addAttribute("android", "scaleType", "center");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_FIT_XY)) {
                xmlTag.addAttribute("android", "scaleType", "fitXY");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_FIT_START)) {
                xmlTag.addAttribute("android", "scaleType", "fitStart");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_FIT_END)) {
                xmlTag.addAttribute("android", "scaleType", "fitEnd");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_FIT_CENTER)) {
                xmlTag.addAttribute("android", "scaleType", "fitCenter");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_CENTER_CROP)) {
                xmlTag.addAttribute("android", "scaleType", "centerCrop");
            } else if (viewBean.image.scaleType.equals(ImageBean.SCALE_TYPE_CENTER_INSIDE)) {
                xmlTag.addAttribute("android", "scaleType", "centerInside");
            }
        }
    }

    /**
     * @see Gravity
     */
    private void writeLayoutGravity(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        if (!toNotAdd.contains("android:layout_gravity") && !injectHandler.contains("layout_gravity")) {
            int gravity = viewBean.layout.layoutGravity;
            if (gravity != Gravity.NO_GRAVITY) {
                String attrValue = "";
                int verticalGravity = gravity & Gravity.FILL_VERTICAL;
                int horizontalGravity = gravity & Gravity.FILL_HORIZONTAL;
                if (horizontalGravity == Gravity.CENTER_HORIZONTAL) {
                    attrValue = "center_horizontal";
                } else {
                    if ((horizontalGravity & Gravity.LEFT) == Gravity.LEFT) {
                        attrValue = "left";
                    }
                    if ((horizontalGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "right";
                    }
                }
                if (verticalGravity == Gravity.CENTER_VERTICAL) {
                    if (!attrValue.isEmpty()) {
                        attrValue += "|";
                    }
                    attrValue += "center_vertical";
                } else {
                    if ((verticalGravity & Gravity.TOP) == Gravity.TOP) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "top";
                    }
                    if ((verticalGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
                        if (!attrValue.isEmpty()) {
                            attrValue += "|";
                        }
                        attrValue += "bottom";
                    }
                }
                xmlTag.addAttribute("android", "layout_gravity", attrValue);
            }
        }
    }

    /**
     * @see ViewGroup.MarginLayoutParams
     */
    private void writeLayoutMargin(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        LayoutBean layoutBean = viewBean.layout;
        int marginLeft = layoutBean.marginLeft;
        int marginTop = layoutBean.marginTop;
        int marginRight = layoutBean.marginRight;
        int marginBottom = layoutBean.marginBottom;

        if (marginLeft == marginRight && marginTop == marginBottom
                && marginLeft == marginTop && marginLeft > 0) {
            if (!toNotAdd.contains("android:layout_margin") && !injectHandler.contains("layout_margin")) {
                xmlTag.addAttribute("android", "layout_margin", marginLeft + "dp");
            }
            return;
        }

        if (marginLeft > 0 && !toNotAdd.contains("android:layout_marginLeft") && !injectHandler.contains("layout_marginLeft")) {
            xmlTag.addAttribute("android", "layout_marginLeft", marginLeft + "dp");
        }
        if (viewBean.layout.marginTop > 0 && !toNotAdd.contains("android:layout_marginTop") && !injectHandler.contains("layout_marginTop")) {
            xmlTag.addAttribute("android", "layout_marginTop", viewBean.layout.marginTop + "dp");
        }
        if (marginRight > 0 && !toNotAdd.contains("android:layout_marginRight") && !injectHandler.contains("layout_marginRight")) {
            xmlTag.addAttribute("android", "layout_marginRight", marginRight + "dp");
        }
        if (marginBottom > 0 && !toNotAdd.contains("android:layout_marginBottom") && !injectHandler.contains("layout_marginBottom")) {
            xmlTag.addAttribute("android", "layout_marginBottom", marginBottom + "dp");
        }
    }

    /**
     * @see View#getPaddingLeft()
     * @see View#getPaddingTop()
     * @see View#getPaddingRight()
     * @see View#getPaddingBottom()
     */
    private void writeCardViewPadding(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        LayoutBean layoutBean = viewBean.layout;
        int paddingLeft = layoutBean.paddingLeft;
        int paddingTop = layoutBean.paddingTop;
        int paddingRight = layoutBean.paddingRight;
        int paddingBottom = layoutBean.paddingBottom;

        if (paddingLeft == paddingRight && paddingTop == paddingBottom
                && paddingLeft == paddingTop && paddingLeft > 0) {
            if (!toNotAdd.contains("app:contentPadding") && !injectHandler.contains("contentPadding")) {
                xmlTag.addAttribute("app", "contentPadding", paddingLeft + "dp");
            }
            return;
        }

        if (paddingLeft > 0 && !toNotAdd.contains("app:contentPaddingLeft") && !injectHandler.contains("contentPaddingLeft")) {
            xmlTag.addAttribute("app", "contentPaddingLeft", paddingLeft + "dp");
        }
        if (paddingTop > 0 && !toNotAdd.contains("app:contentPaddingTop") && !injectHandler.contains("contentPaddingTop")) {
            xmlTag.addAttribute("app", "contentPaddingTop", paddingTop + "dp");
        }
        if (paddingRight > 0 && !toNotAdd.contains("app:contentPaddingRight") && !injectHandler.contains("contentPaddingRight")) {
            xmlTag.addAttribute("app", "contentPaddingRight", paddingRight + "dp");
        }
        if (paddingBottom > 0 && !toNotAdd.contains("app:contentPaddingBottom") && !injectHandler.contains("contentPaddingBottom")) {
            xmlTag.addAttribute("app", "contentPaddingBottom", paddingBottom + "dp");
        }
    }

    /**
     * @see View#getPaddingLeft()
     * @see View#getPaddingTop()
     * @see View#getPaddingRight()
     * @see View#getPaddingBottom()
     */
    private void writeViewPadding(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        LayoutBean layoutBean = viewBean.layout;
        int paddingLeft = layoutBean.paddingLeft;
        int paddingTop = layoutBean.paddingTop;
        int paddingRight = layoutBean.paddingRight;
        int paddingBottom = layoutBean.paddingBottom;

        if (paddingLeft == paddingRight && paddingTop == paddingBottom
                && paddingLeft == paddingTop && paddingLeft > 0) {
            if (!toNotAdd.contains("android:padding") && !injectHandler.contains("padding")) {
                xmlTag.addAttribute("android", "padding", paddingLeft + "dp");
            }
            return;
        }

        if (paddingLeft > 0 && !toNotAdd.contains("android:paddingLeft") && !injectHandler.contains("paddingLeft")) {
            xmlTag.addAttribute("android", "paddingLeft", paddingLeft + "dp");
        }
        if (paddingTop > 0 && !toNotAdd.contains("android:paddingTop") && !injectHandler.contains("paddingTop")) {
            xmlTag.addAttribute("android", "paddingTop", paddingTop + "dp");
        }
        if (paddingRight > 0 && !toNotAdd.contains("android:paddingRight") && !injectHandler.contains("paddingRight")) {
            xmlTag.addAttribute("android", "paddingRight", paddingRight + "dp");
        }
        if (paddingBottom > 0 && !toNotAdd.contains("android:paddingBottom") && !injectHandler.contains("paddingBottom")) {
            xmlTag.addAttribute("android", "paddingBottom", paddingBottom + "dp");
        }
    }

    private void writeTextAttributes(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        String text = viewBean.text.text;
        if (text != null && !text.isEmpty() && !toNotAdd.contains("android:text") && !injectHandler.contains("text")) {
            if (text.startsWith("@")) {
                xmlTag.addAttribute("android", "text", text);
            } else {
                xmlTag.addAttribute("android", "text", escapeXML(text));
            }
        }

        int textSize = viewBean.text.textSize;
        if (textSize > 0 && !toNotAdd.contains("android:textSize") && !injectHandler.contains("textSize")) {
            xmlTag.addAttribute("android", "textSize", textSize + "sp");
        }
        if (!toNotAdd.contains("android:textStyle") && !injectHandler.contains("textStyle")) {
            int textType = viewBean.text.textType;
            if (textType == TextBean.TEXT_TYPE_BOLD) {
                xmlTag.addAttribute("android", "textStyle", "bold");
            } else if (textType == TextBean.TEXT_TYPE_ITALIC) {
                xmlTag.addAttribute("android", "textStyle", "italic");
            } else if (textType == TextBean.TEXT_TYPE_BOLDITALIC) {
                xmlTag.addAttribute("android", "textStyle", "bold|italic");
            }
        }
        if (viewBean.text.textColor != 0xffffff) {
            if (!hasAttr("textColor", viewBean) && !toNotAdd.contains("android:textColor") && !injectHandler.contains("textColor") && viewBean.text.resTextColor != null) {
                if (viewBean.text.resTextColor.startsWith("?") || viewBean.text.resTextColor.startsWith("@color/")) {
                    xmlTag.addAttribute("android", "textColor", viewBean.text.resTextColor);
                } else {
                    xmlTag.addAttribute("android", "textColor", "@color/" + viewBean.text.resTextColor);
                }
            } else if (!hasAttr("textColor", viewBean) && !toNotAdd.contains("android:textColor") && !injectHandler.contains("textColor")) {
                xmlTag.addAttribute("android", "textColor", formatColor(viewBean.text.textColor & 0xffffff));
            }
        }
        switch (viewBean.type) {
            case ViewBean.VIEW_TYPE_WIDGET_EDITTEXT:
            case ViewBeans.VIEW_TYPE_WIDGET_AUTOCOMPLETETEXTVIEW:
            case ViewBeans.VIEW_TYPE_WIDGET_MULTIAUTOCOMPLETETEXTVIEW:
                String hint = viewBean.text.hint;
                if (hint != null && !hint.isEmpty() && !toNotAdd.contains("android:hint") && !injectHandler.contains("hint")) {
                    if (hint.startsWith("@")) {
                        xmlTag.addAttribute("android", "hint", hint);
                    } else {
                        xmlTag.addAttribute("android", "hint", escapeXML(hint));
                    }
                }
                if (viewBean.text.hintColor != 0xffffff) {
                    if (!hasAttr("textColorHint", viewBean) && !toNotAdd.contains("android:textColorHint") && (viewBean.text.resHintColor != null)) {
                        if (viewBean.text.resHintColor.startsWith("?") || viewBean.text.resHintColor.startsWith("@color/")) {
                            xmlTag.addAttribute("android", "textColorHint", viewBean.text.resHintColor);
                        } else {
                            xmlTag.addAttribute("android", "textColorHint", "@color/" + viewBean.text.resHintColor);
                        }
                    } else if (!hasAttr("textColorHint", viewBean) && !toNotAdd.contains("android:textColorHint")) {
                        xmlTag.addAttribute("android", "textColorHint", formatColor(viewBean.text.hintColor & 0xffffff));
                    }
                }
                if (viewBean.text.singleLine != 0 && !toNotAdd.contains("android:singleLine") && !injectHandler.contains("singleLine")) {
                    xmlTag.addAttribute("android", "singleLine", "true");
                }

                int line = viewBean.text.line;
                if (line > 0 && !toNotAdd.contains("android:lines") && !injectHandler.contains("lines")) {
                    xmlTag.addAttribute("android", "lines", String.valueOf(line));
                }

                int inputType = viewBean.text.inputType;
                if (inputType != TextBean.INPUT_TYPE_TEXT && !toNotAdd.contains("android:inputType") && !injectHandler.contains("inputType")) {
                    xmlTag.addAttribute("android", "inputType", SketchwareConstants.getPropertyValueString("property_input_type", inputType));
                }

                int imeOption = viewBean.text.imeOption;
                if (imeOption != TextBean.IME_OPTION_NORMAL && !toNotAdd.contains("android:imeOptions") && !injectHandler.contains("imeOptions")) {
                    if (imeOption == TextBean.IME_OPTION_NONE) {
                        xmlTag.addAttribute("android", "imeOptions", "actionNone");
                    } else if (imeOption == TextBean.IME_OPTION_GO) {
                        xmlTag.addAttribute("android", "imeOptions", "actionGo");
                    } else if (imeOption == TextBean.IME_OPTION_SEARCH) {
                        xmlTag.addAttribute("android", "imeOptions", "actionSearch");
                    } else if (imeOption == TextBean.IME_OPTION_SEND) {
                        xmlTag.addAttribute("android", "imeOptions", "actionSend");
                    } else if (imeOption == TextBean.IME_OPTION_NEXT) {
                        xmlTag.addAttribute("android", "imeOptions", "actionNext");
                    } else if (imeOption == TextBean.IME_OPTION_DONE) {
                        xmlTag.addAttribute("android", "imeOptions", "actionDone");
                    }
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW:
                if (viewBean.text.singleLine != 0 && !toNotAdd.contains("android:singleLine") && !injectHandler.contains("singleLine")) {
                    xmlTag.addAttribute("android", "singleLine", "true");
                }
                line = viewBean.text.line;
                if (line > 0 && !toNotAdd.contains("android:lines") && !injectHandler.contains("lines")) {
                    xmlTag.addAttribute("android", "lines", String.valueOf(line));
                }
                break;
        }
    }

    private void addCommonAttributes(XmlBuilder xmlTag, ViewBean viewBean) {
        var injectHandler = new InjectAttributeHandler(viewBean);
        Set<String> toNotAdd = readAttributesToReplace(viewBean);
        if (viewBean.enabled == 0 && !toNotAdd.contains("android:enabled") && !injectHandler.contains("enabled")) {
            xmlTag.addAttribute("android", "enabled", "false");
        }
        if (viewBean.clickable == 0 && !toNotAdd.contains("android:clickable") && !injectHandler.contains("clickable")) {
            xmlTag.addAttribute("android", "clickable", "false");
        }
        int rotate = viewBean.image.rotate;
        if (rotate != 0 && !toNotAdd.contains("android:rotation") && !injectHandler.contains("rotation")) {
            xmlTag.addAttribute("android", "rotation", String.valueOf(rotate));
        }
        float alpha = viewBean.alpha;
        if (1.0f != alpha && !toNotAdd.contains("android:alpha") && !injectHandler.contains("alpha")) {
            xmlTag.addAttribute("android", "alpha", String.valueOf(alpha));
        }
        if (0.0f != viewBean.translationX && !toNotAdd.contains("android:translationX") && !injectHandler.contains("translationX")) {
            xmlTag.addAttribute("android", "translationX", viewBean.translationX + "dp");
        }
        if (0.0f != viewBean.translationY && !toNotAdd.contains("android:translationY") && !injectHandler.contains("translationY")) {
            xmlTag.addAttribute("android", "translationY", viewBean.translationY + "dp");
        }
        float scaleX = viewBean.scaleX;
        if (1.0f != scaleX && !toNotAdd.contains("android:scaleX") && !injectHandler.contains("scaleX")) {
            xmlTag.addAttribute("android", "scaleX", String.valueOf(scaleX));
        }
        float scaleY = viewBean.scaleY;
        if (1.0f != scaleY && !toNotAdd.contains("android:scaleY") && !injectHandler.contains("scaleY")) {
            xmlTag.addAttribute("android", "scaleY", String.valueOf(scaleY));
        }

        switch (viewBean.type) {
            case ViewBean.VIEW_TYPE_WIDGET_CHECKBOX:
            case ViewBean.VIEW_TYPE_WIDGET_SWITCH:
            case ViewBeans.VIEW_TYPE_WIDGET_RADIOBUTTON:
                if (viewBean.checked == 1 && !toNotAdd.contains("android:checked") && !injectHandler.contains("checked")) {
                    xmlTag.addAttribute("android", "checked", "true");
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_SEEKBAR:
                int progress = viewBean.progress;
                if (progress > ViewBean.DEFAULT_PROGRESS && !toNotAdd.contains("android:progress") && !injectHandler.contains("progress")) {
                    xmlTag.addAttribute("android", "progress", String.valueOf(progress));
                }

                int max = viewBean.max;
                if (max != ViewBean.DEFAULT_MAX && !toNotAdd.contains("android:max") && !injectHandler.contains("max")) {
                    xmlTag.addAttribute("android", "max", String.valueOf(max));
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_CALENDARVIEW:
                int firstDayOfWeek = viewBean.firstDayOfWeek;
                if (firstDayOfWeek != 1 && !toNotAdd.contains("android:firstDayOfWeek") && !injectHandler.contains("firstDayOfWeek")) {
                    xmlTag.addAttribute("android", "firstDayOfWeek", String.valueOf(firstDayOfWeek));
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_SPINNER:
                int spinnerMode = viewBean.spinnerMode;
                if (!toNotAdd.contains("android:spinnerMode") && !injectHandler.contains("spinnerMode")) {
                    if (spinnerMode == ViewBean.SPINNER_MODE_DROPDOWN) {
                        xmlTag.addAttribute("android", "spinnerMode", "dropdown");
                    } else if (spinnerMode == ViewBean.SPINNER_MODE_DIALOG) {
                        xmlTag.addAttribute("android", "spinnerMode", "dialog");
                    }
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_LISTVIEW:
                int dividerHeight = viewBean.dividerHeight;
                if (dividerHeight != 1 && !toNotAdd.contains("android:dividerHeight") && !injectHandler.contains("dividerHeight")) {
                    xmlTag.addAttribute("android", "dividerHeight", dividerHeight + "dp");
                }
                if (dividerHeight == 0 && !toNotAdd.contains("android:divider") && !injectHandler.contains("divider")) {
                    xmlTag.addAttribute("android", "divider", "@null");
                }

                if (!toNotAdd.contains("android:choiceMode") && !injectHandler.contains("choiceMode")) {
                    var value = switch (viewBean.choiceMode) {
                        case ViewBean.CHOICE_MODE_NONE -> "none";
                        case ViewBean.CHOICE_MODE_SINGLE -> "singleChoice";
                        case ViewBean.CHOICE_MODE_MULTI -> "multipleChoice";
                        default -> "";
                    };
                    if (!value.isEmpty()) {
                        xmlTag.addAttribute("android", "choiceMode", value);
                    }
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_ADVIEW:
                String adSize = viewBean.adSize;
                if (!toNotAdd.contains("app:adSize") && !injectHandler.contains("adSize")) {
                    if (adSize == null || adSize.isEmpty()) {
                        xmlTag.addAttribute("app", "adSize", "SMART_BANNER");
                    } else {
                        xmlTag.addAttribute("app", "adSize", adSize);
                    }
                }

                if (!toNotAdd.contains("app:adUnitId") && !injectHandler.contains("adUnitId")) {
                    if (buildConfig.isDebugBuild) {
                        xmlTag.addAttribute("app", "adUnitId", "ca-app-pub-3940256099942544/6300978111");
                    } else {
                        xmlTag.addAttribute("app", "adUnitId", buildConfig.bannerAdUnitId);
                    }
                }
                break;

            case ViewBean.VIEW_TYPE_WIDGET_PROGRESSBAR:
                progress = viewBean.progress;
                if (progress > ViewBean.DEFAULT_PROGRESS && !toNotAdd.contains("android:progress") && !injectHandler.contains("progress")) {
                    xmlTag.addAttribute("android", "progress", String.valueOf(progress));
                }

                max = viewBean.max;
                if (max != ViewBean.DEFAULT_MAX && !toNotAdd.contains("android:max") && !injectHandler.contains("max")) {
                    xmlTag.addAttribute("android", "max", String.valueOf(max));
                }

                String indeterminate = viewBean.indeterminate;
                if (indeterminate != null && !indeterminate.isEmpty() && !toNotAdd.contains("android:indeterminate") && !injectHandler.contains("indeterminate")) {
                    xmlTag.addAttribute("android", "indeterminate", indeterminate);
                }
                String progressStyle = viewBean.progressStyle;
                if (progressStyle != null && !progressStyle.isEmpty() && !toNotAdd.contains("style") && !injectHandler.contains("style")) {
                    xmlTag.addAttribute(null, "style", progressStyle);
                }
                break;
        }
    }

    /**
     * check whether the attribute (attrName) is injected to the ViewBean or not.
     */
    private boolean hasAttr(String attrName, ViewBean bean) {
        String inject = bean.inject;
        if (inject == null || inject.isEmpty()) return false;
        return Pattern.compile("(android|app) *?: *?" + attrName).matcher(inject).find();
    }

    public Set<String> readAttributesToReplace(ViewBean viewBean) {
        Set<String> toReplace = new HashSet<>();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader("<tag xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
                    "xmlns:app=\"http://schemas.android.com/apk/res-auto\" " +
                    "xmlns:tools=\"http://schemas.android.com/tools\"" +
                    viewBean.inject + "></tag>"));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if ("http://schemas.android.com/tools".equals(parser.getAttributeNamespace(i)) &&
                                "replace".equals(parser.getAttributeName(i))) {
                            toReplace.addAll(Arrays.asList(parser.getAttributeValue(i).split("\\s*,\\s*")));
                        }
                    }
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            LogUtil.e("pro.sketchware.core.Ox", "Failed to parse inject property of View " + viewBean.id, e);
        }

        return toReplace;
    }
}
