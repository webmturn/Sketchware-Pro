package pro.sketchware.activities.resourceseditor.components.utils;

import static com.besome.sketch.design.DesignActivity.sc_id;
import static mod.hey.studios.util.ProjectFile.getDefaultColor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;

import androidx.core.content.ContextCompat;

import com.besome.sketch.editor.manage.library.material3.Material3LibraryManager;
import com.google.android.material.color.MaterialColors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.util.MapValueHelper;
import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.activities.resourceseditor.components.models.ColorModel;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.PropertiesUtil;
import pro.sketchware.utility.XmlUtil;

@SuppressLint("DiscouragedApi")
public class ColorsEditorManager {

    public String contentPath;
    public boolean isDataLoadingFailed;
    public boolean isDefaultVariant = true;
    public final String defaultHexColor = "#00000000";

    public HashMap<String, String> defaultColors;
    public HashMap<Integer, String> notesMap = new HashMap<>();
    private final ArrayList<ColorModel> resColorsList = new ArrayList<>();
    private final ArrayList<ColorModel> resColorsNightList = new ArrayList<>();

    private final String projectId;
    private final Material3LibraryManager material3LibraryManager;

    public ColorsEditorManager() {
        this(sc_id);
    }

    public ColorsEditorManager(String projectId) {
        this.projectId = projectId;
        material3LibraryManager = new Material3LibraryManager(projectId);
        initialize();
    }

    public void initialize() {
        String filePath = SketchwarePaths.getDataPath(projectId) + "/files/resource/values/colors.xml";
        String filePathNight = SketchwarePaths.getDataPath(projectId) + "/files/resource/values-night/colors.xml";
        parseColorsXML(resColorsList, FileUtil.readFileIfExist(filePath));
        parseColorsXML(resColorsNightList, FileUtil.readFileIfExist(filePathNight));
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

    private String formatColor(int colorInt) {
        return Color.alpha(colorInt) != 0xFF
                ? String.format("#%08X", colorInt)
                : String.format("#%06X", (0xFFFFFF & colorInt));
    }

    public String getColorValueFromAttrs(Context context, String attrName, int referencingLimit, boolean isNightVariant) {
        Integer resolvedColor = getColorIntFromAttrs(context, attrName, referencingLimit, isNightVariant);
        return resolvedColor != null ? formatColor(resolvedColor) : defaultHexColor;
    }

    public Integer getColorIntFromAttrs(Context context, String attrName, int referencingLimit, boolean isNightVariant) {
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
            Log.w("ColorsEditorManager", "Failed to get Material color for attribute: " + attrName, e);
        }
        return null;
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

    public void parseColorsXML(ArrayList<ColorModel> colorList, String colorXml) {
        isDataLoadingFailed = false;
        ArrayList<String> foundPrimaryColors = new ArrayList<>();
        ArrayList<ColorModel> colorOrderList = new ArrayList<>();
        ArrayList<ColorModel> otherColors = new ArrayList<>();
        boolean hasChanges = false; // Flag to track changes

        try {
            colorList.clear();
            notesMap.clear();
            // Parse the XML using DocumentBuilder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(colorXml)));
            document.getDocumentElement().normalize();

            NodeList childNodes = document.getDocumentElement().getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);

                if (node.getNodeType() == Node.COMMENT_NODE) {
                    // Save comments in notesMap
                    notesMap.merge(colorList.size(), node.getNodeValue().trim(), (a, b) -> a + "\n" + b);
                } else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("color")) {
                    Element element = (Element) node;
                    String colorName = element.getAttribute("name");
                    String colorValue = element.getTextContent().trim();

                    if (PropertiesUtil.isHexColor(getColorValue(SketchApplication.getContext(), colorValue, 4))) {
                        ColorModel colorModel = new ColorModel(colorName, colorValue);
                        colorList.add(colorModel);

                        if (defaultColors != null && defaultColors.containsKey(colorName)) {
                            foundPrimaryColors.add(colorName);
                            colorOrderList.add(colorModel);
                        } else {
                            otherColors.add(colorModel);
                        }
                    }
                }
            }

            if (isDefaultVariant && defaultColors != null && projectId != null) {
                HashMap<String, Object> metadata = ProjectListManager.getProjectById(projectId);
                Set<String> missingKeys = new HashSet<>(defaultColors.keySet());
                foundPrimaryColors.forEach(missingKeys::remove);

                if (!missingKeys.isEmpty()) {
                    for (String missingColor : missingKeys) {
                        String colorHex = String.format("#%06X", MapValueHelper.get(metadata, defaultColors.get(missingColor), getDefaultColor(defaultColors.get(missingColor))) & 0xffffff);
                        ColorModel missingColorModel = new ColorModel(missingColor, colorHex);
                        colorOrderList.add(missingColorModel);
                        hasChanges = true;
                    }
                }
            }

            // Reorder colors
            ArrayList<ColorModel> previousColorList = new ArrayList<>(colorList); // Save the original list for comparison
            colorList.clear();
            colorList.addAll(colorOrderList);
            colorList.addAll(otherColors);

            if (!previousColorList.equals(colorList)) {
                hasChanges = true;
            }

            // Save the updated XML if changes are detected
            if (hasChanges) {
                XmlUtil.saveXml(SketchwarePaths.getDataPath(projectId) + "/files/resource/values/colors.xml", convertListToXml(colorList, notesMap));
            }

        } catch (Exception e) {
            isDataLoadingFailed = !colorXml.trim().isEmpty();
        }
    }

    public String convertListToXml(ArrayList<ColorModel> colorList, HashMap<Integer, String> notesMap) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<resources>\n");

        for (int i = 0; i < colorList.size(); i++) {
            if (notesMap.containsKey(i)) {
                for (String comment : notesMap.get(i).split("\n")) {
                    xmlBuilder.append("    <!-- ").append(comment).append(" -->\n");
                }
            }

            ColorModel colorModel = colorList.get(i);
            xmlBuilder.append("    <color name=\"").append(colorModel.getColorName()).append("\">")
                    .append(colorModel.getColorValue()).append("</color>\n");
        }

        if (notesMap.containsKey(colorList.size())) {
            for (String comment : notesMap.get(colorList.size()).split("\n")) {
                xmlBuilder.append("    <!-- ").append(comment).append(" -->\n");
            }
        }

        xmlBuilder.append("</resources>");
        return xmlBuilder.toString();
    }
}
