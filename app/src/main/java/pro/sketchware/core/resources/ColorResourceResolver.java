package pro.sketchware.core.resources;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;

import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import pro.sketchware.R;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.XmlUtil;
import pro.sketchware.util.library.Material3LibraryManager;

@SuppressLint("DiscouragedApi")
public class ColorResourceResolver {

    public final String defaultHexColor = "#00000000";

    private final ArrayList<ColorModel> resColorsList = new ArrayList<>();
    private final ArrayList<ColorModel> resColorsNightList = new ArrayList<>();
    private final String projectId;
    private final Material3LibraryManager material3LibraryManager;

    public ColorResourceResolver(String projectId) {
        this.projectId = projectId;
        material3LibraryManager = projectId == null || projectId.isEmpty() ? null : new Material3LibraryManager(projectId);
        loadProjectColors();
    }

    public final void initialize() {
        loadProjectColors();
    }

    private void loadProjectColors() {
        if (projectId == null || projectId.isEmpty()) {
            resColorsList.clear();
            resColorsNightList.clear();
            return;
        }
        loadColorsFromPath(SketchwarePaths.getProjectResourceValuesFilePath(projectId, "colors.xml"), false);
        loadColorsFromPath(SketchwarePaths.getProjectResourceValuesFilePath(projectId, "-night", "colors.xml"), true);
    }

    public void loadColorsFromPath(String filePath, boolean isNightVariant) {
        parseColorElements(
                isNightVariant ? resColorsNightList : resColorsList,
                FileUtil.readFileIfExist(filePath)
        );
    }

    public ArrayList<ColorModel> getResColorsList() {
        return resColorsList;
    }

    public String getColorValue(Context context, String colorValue, int referencingLimit) {
        return getColorValue(context, colorValue, referencingLimit, false);
    }

    public String getColorValue(Context context, String colorValue, int referencingLimit, boolean isNightVariant) {
        Integer resolvedColor = resolveColorInt(context, colorValue, referencingLimit, isNightVariant);
        return resolvedColor != null ? formatColor(resolvedColor) : defaultHexColor;
    }

    public Integer resolveColorInt(Context context, String colorValue, int referencingLimit) {
        return resolveColorInt(context, colorValue, referencingLimit, false);
    }

    public Integer resolveColorInt(Context context, String colorValue, int referencingLimit, boolean isNightVariant) {
        if (colorValue == null || referencingLimit <= 0) {
            return null;
        }

        if (colorValue.startsWith("#")) {
            return PropertiesUtil.isHexColor(colorValue) ? PropertiesUtil.parseColor(colorValue) : null;
        }
        if (colorValue.startsWith("?attr/") || colorValue.startsWith("?")) {
            String attrName = colorValue.startsWith("?attr/") ? colorValue.substring(6) : colorValue.substring(1);
            return getColorIntFromAttrs(context, attrName, referencingLimit - 1, isNightVariant);
        }
        if (colorValue.startsWith("@color/")) {
            return getColorIntFromXml(context, colorValue.substring(7), referencingLimit - 1, isNightVariant);
        } else if (colorValue.startsWith("@android:color/")) {
            return getColorIntFromSystem(colorValue, context);
        }
        return null;
    }

    public String getColorValueFromAttrs(Context context, String attrName, int referencingLimit, boolean isNightVariant) {
        Integer resolvedColor = getColorIntFromAttrs(context, attrName, referencingLimit, isNightVariant);
        return resolvedColor != null ? formatColor(resolvedColor) : defaultHexColor;
    }

    public Integer getColorIntFromAttrs(Context context, String attrName, int referencingLimit, boolean isNightVariant) {
        if (material3LibraryManager == null) {
            return null;
        }
        try {
            int attrId = context.getResources().getIdentifier(attrName, "attr", context.getPackageName());

            if (attrId != 0 && referencingLimit > 0) {
                Context themedContext;
                if (isNightVariant) {
                    if (material3LibraryManager.isDynamicColorsEnabled()) {
                        themedContext = new ContextThemeWrapper(context, R.style.ThemeOverlay_SketchwarePro_ViewEditor_Material3_Dark);
                    } else {
                        themedContext = new ContextThemeWrapper(context, R.style.ThemeOverlay_SketchwarePro_ViewEditor_Material3_NON_DYNAMIC_Dark);
                    }
                } else {
                    if (material3LibraryManager.isDynamicColorsEnabled()) {
                        themedContext = new ContextThemeWrapper(context, R.style.ThemeOverlay_SketchwarePro_ViewEditor_Material3_Light);
                    } else {
                        themedContext = new ContextThemeWrapper(context, R.style.ThemeOverlay_SketchwarePro_ViewEditor_Material3_NON_DYNAMIC_Light);
                    }
                }
                return MaterialColors.getColor(themedContext, attrId, "getColorValue");
            }
        } catch (Exception e) {
            Log.w("ColorResourceResolver", "Failed to get Material color for attribute: " + attrName, e);
        }
        return null;
    }

    public void parseColorsXML(ArrayList<ColorModel> colorList, String colorXml) {
        parseColorElements(colorList, colorXml);
    }

    private void parseColorElements(ArrayList<ColorModel> colorList, String colorXml) {
        try {
            colorList.clear();
            DocumentBuilderFactory factory = XmlUtil.newSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(colorXml)));
            document.getDocumentElement().normalize();

            NodeList childNodes = document.getDocumentElement().getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("color")) {
                    Element element = (Element) node;
                    colorList.add(new ColorModel(
                            element.getAttribute("name"),
                            element.getTextContent().trim()
                    ));
                }
            }
        } catch (Exception ignored) {
            colorList.clear();
        }
    }

    protected String getProjectId() {
        return projectId;
    }

    protected String formatColor(int colorInt) {
        return Color.alpha(colorInt) != 0xFF
                ? String.format("#%08X", colorInt)
                : String.format("#%06X", (0xFFFFFF & colorInt));
    }

    private Integer getColorIntFromSystem(String colorValue, Context context) {
        String colorName = colorValue.substring(15);
        int colorId = context.getResources().getIdentifier(colorName, "color", "android");
        if (colorId == 0) {
            return null;
        }
        try {
            return ContextCompat.getColor(context, colorId);
        } catch (android.content.res.Resources.NotFoundException e) {
            return null;
        }
    }

    private Integer getColorIntFromXml(Context context, String colorName, int referencingLimit, boolean isNightVariant) {
        if (isNightVariant) {
            for (ColorModel colorModel : resColorsNightList) {
                if (colorModel.getColorName().equals(colorName)) {
                    return resolveColorInt(context, colorModel.getColorValue(), referencingLimit, true);
                }
            }

            for (ColorModel colorModel : resColorsList) {
                if (colorModel.getColorName().equals(colorName)) {
                    return resolveColorInt(context, colorModel.getColorValue(), referencingLimit, true);
                }
            }
        } else {
            for (ColorModel colorModel : resColorsList) {
                if (colorModel.getColorName().equals(colorName)) {
                    return resolveColorInt(context, colorModel.getColorValue(), referencingLimit, false);
                }
            }
        }

        return null;
    }
}
