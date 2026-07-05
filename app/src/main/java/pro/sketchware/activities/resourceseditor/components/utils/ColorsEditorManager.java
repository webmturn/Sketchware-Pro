package pro.sketchware.activities.resourceseditor.components.utils;

import static pro.sketchware.activities.design.DesignActivity.sc_id;
import static pro.sketchware.util.ProjectFile.getDefaultColor;

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
import pro.sketchware.core.resources.ColorModel;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.SketchApplication;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.XmlUtil;

public class ColorsEditorManager extends ColorResourceResolver {

    public String contentPath;
    public boolean isDataLoadingFailed;
    public boolean isDefaultVariant = true;

    public HashMap<String, String> defaultColors;
    public HashMap<Integer, String> notesMap = new HashMap<>();

    public ColorsEditorManager() {
        this(sc_id);
    }

    public ColorsEditorManager(String projectId) {
        super(projectId);
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

            if (isDefaultVariant && defaultColors != null && getProjectId() != null) {
                HashMap<String, Object> metadata = ProjectListManager.getProjectById(getProjectId());
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
                XmlUtil.saveXml(SketchwarePaths.getDataPath(getProjectId()) + "/files/resource/values/colors.xml", convertListToXml(colorList, notesMap));
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
