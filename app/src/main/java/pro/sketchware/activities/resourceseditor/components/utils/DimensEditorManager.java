package pro.sketchware.activities.resourceseditor.components.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import pro.sketchware.activities.resourceseditor.components.models.DimenModel;

public class DimensEditorManager {

    public String contentPath;
    public boolean isDataLoadingFailed;
    public HashMap<Integer, String> notesMap = new HashMap<>();

    public void parseDimensXML(ArrayList<DimenModel> dimenList, String dimenXml) {
        isDataLoadingFailed = false;
        try {
            dimenList.clear();
            notesMap.clear();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(dimenXml)));
            document.getDocumentElement().normalize();

            NodeList childNodes = document.getDocumentElement().getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);

                if (node.getNodeType() == Node.COMMENT_NODE) {
                    notesMap.put(dimenList.size(), node.getNodeValue().trim());
                } else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("dimen")) {
                    Element element = (Element) node;
                    String dimenName = element.getAttribute("name");
                    String dimenValue = element.getTextContent().trim();
                    dimenList.add(new DimenModel(dimenName, dimenValue));
                }
            }
        } catch (Exception e) {
            isDataLoadingFailed = !dimenXml.trim().isEmpty();
        }
    }

    public String convertListToXml(ArrayList<DimenModel> dimenList, HashMap<Integer, String> notesMap) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<resources>\n");

        for (int i = 0; i < dimenList.size(); i++) {
            if (notesMap.containsKey(i)) {
                xmlBuilder.append("    <!--").append(notesMap.get(i)).append("-->\n");
            }

            DimenModel dimenModel = dimenList.get(i);
            xmlBuilder.append("    <dimen name=\"").append(dimenModel.getDimenName()).append("\">")
                    .append(dimenModel.getDimenValue()).append("</dimen>\n");
        }

        xmlBuilder.append("</resources>");
        return xmlBuilder.toString();
    }
}
