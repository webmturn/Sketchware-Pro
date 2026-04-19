package pro.sketchware.core;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary;
import pro.sketchware.utility.FileUtil;

public class LocalLibraryManifestMerger {
    private static final String TAG = "LocalLibraryManifestMerger";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String TOOLS_NAMESPACE = "http://schemas.android.com/tools";

    public static String mergeLocalLibraryManifests(String manifestXml, String projectId, String packageName) {
        if (manifestXml == null || manifestXml.trim().isEmpty()) {
            return manifestXml;
        }

        ArrayList<String> manifestPaths = new ManageLocalLibrary(projectId).getManifestPaths();
        if (manifestPaths.isEmpty()) {
            return manifestXml;
        }

        try {
            Document generatedManifest = parseXml(manifestXml);
            Element manifestElement = generatedManifest.getDocumentElement();
            if (manifestElement == null || !"manifest".equals(manifestElement.getTagName())) {
                return manifestXml;
            }

            Element applicationElement = findDirectChild(manifestElement, "application");
            if (applicationElement == null) {
                return manifestXml;
            }

            boolean changed = false;
            for (String manifestPath : manifestPaths) {
                changed |= mergeLibraryManifest(generatedManifest, manifestElement, applicationElement, manifestPath, packageName);
            }

            return changed ? toXml(generatedManifest) : manifestXml;
        } catch (Exception e) {
            Log.w(TAG, "Failed to merge local library manifests for project " + projectId, e);
            return manifestXml;
        }
    }

    private static boolean mergeLibraryManifest(Document generatedManifest, Element manifestElement,
                                                Element applicationElement, String manifestPath,
                                                String packageName) {
        if (manifestPath == null || manifestPath.trim().isEmpty()) {
            return false;
        }

        File manifestFile = new File(manifestPath);
        if (!manifestFile.isFile()) {
            return false;
        }

        try {
            Document libraryManifest = parseXml(manifestFile);
            Element libraryManifestElement = libraryManifest.getDocumentElement();
            if (libraryManifestElement == null || !"manifest".equals(libraryManifestElement.getTagName())) {
                return false;
            }

            String libraryPackageName = resolveLibraryPackageName(libraryManifestElement, manifestFile);
            boolean changed = mergeManifestChildren(generatedManifest, manifestElement, applicationElement,
                    libraryManifestElement, packageName, libraryPackageName);

            Element libraryApplicationElement = findDirectChild(libraryManifestElement, "application");
            if (libraryApplicationElement != null) {
                changed |= mergeApplicationAttributes(generatedManifest, applicationElement,
                        libraryApplicationElement, packageName);
                changed |= mergeApplicationChildren(generatedManifest, applicationElement,
                        libraryApplicationElement, packageName, libraryPackageName);
            }
            return changed;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse local library manifest: " + manifestPath, e);
            return false;
        }
    }

    private static boolean mergeManifestChildren(Document generatedManifest, Element manifestElement,
                                                 Element applicationElement, Element libraryManifestElement,
                                                 String packageName, String libraryPackageName) throws Exception {
        boolean changed = false;
        for (Element childElement : getDirectChildElements(libraryManifestElement)) {
            String tagName = childElement.getTagName();
            if ("uses-permission".equals(tagName)
                    || "uses-permission-sdk-23".equals(tagName)
                    || "uses-feature".equals(tagName)) {
                Element importedElement = importElement(generatedManifest, childElement, packageName, libraryPackageName);
                if (!containsEquivalentChild(manifestElement, importedElement)) {
                    manifestElement.insertBefore(importedElement, applicationElement);
                    changed = true;
                }
            } else if ("queries".equals(tagName)) {
                changed |= mergeQueries(generatedManifest, manifestElement, applicationElement, childElement, packageName, libraryPackageName);
            }
        }
        return changed;
    }

    private static boolean mergeQueries(Document generatedManifest, Element manifestElement,
                                        Element applicationElement, Element libraryQueriesElement,
                                        String packageName, String libraryPackageName) throws Exception {
        Element targetQueriesElement = findDirectChild(manifestElement, "queries");
        boolean createdQueriesElement = false;
        if (targetQueriesElement == null) {
            targetQueriesElement = generatedManifest.createElement("queries");
            createdQueriesElement = true;
        }

        boolean changed = false;
        for (Element libraryQueryChild : getDirectChildElements(libraryQueriesElement)) {
            Element importedQueryChild = importElement(generatedManifest, libraryQueryChild, packageName, libraryPackageName);
            if (!containsEquivalentChild(targetQueriesElement, importedQueryChild)) {
                targetQueriesElement.appendChild(importedQueryChild);
                changed = true;
            }
        }

        if (createdQueriesElement && changed) {
            manifestElement.insertBefore(targetQueriesElement, applicationElement);
        }
        return changed;
    }

    private static boolean mergeApplicationChildren(Document generatedManifest, Element applicationElement,
                                                    Element libraryApplicationElement, String packageName,
                                                    String libraryPackageName) throws Exception {
        boolean changed = false;
        for (Element childElement : getDirectChildElements(libraryApplicationElement)) {
            String tagName = childElement.getTagName();
            if (!"provider".equals(tagName)
                    && !"service".equals(tagName)
                    && !"receiver".equals(tagName)
                    && !"activity".equals(tagName)
                    && !"activity-alias".equals(tagName)
                    && !"meta-data".equals(tagName)
                    && !"uses-library".equals(tagName)) {
                continue;
            }

            Element importedElement = importElement(generatedManifest, childElement, packageName, libraryPackageName);
            if ("provider".equals(tagName)) {
                String incomingAuthorities = getAndroidAttribute(importedElement, "authorities");
                if (!incomingAuthorities.isEmpty()) {
                    Element conflictingProvider = findProviderByAuthorities(applicationElement, incomingAuthorities);
                    if (conflictingProvider != null) {
                        String existingProviderName = normalizeManifestClassName(
                                getAndroidAttribute(conflictingProvider, "name"),
                                getManifestPackageName(conflictingProvider)
                        );
                        String incomingProviderName = normalizeManifestClassName(
                                getAndroidAttribute(importedElement, "name"),
                                getManifestPackageName(importedElement)
                        );
                        if (!existingProviderName.equals(incomingProviderName)) {
                            Log.w(TAG, "Skipping local library provider due to conflicting authorities '"
                                    + incomingAuthorities + "': existing=" + existingProviderName
                                    + ", incoming=" + incomingProviderName);
                            continue;
                        }
                    }
                }
            }
            Element existingElement = findEquivalentChild(applicationElement, importedElement);
            if (existingElement == null) {
                applicationElement.appendChild(importedElement);
                changed = true;
            } else {
                changed |= mergeAttributes(existingElement, importedElement);
                changed |= mergeNestedChildren(existingElement, importedElement);
            }
        }
        return changed;
    }

    private static boolean mergeNestedChildren(Element targetElement, Element sourceElement) throws Exception {
        boolean changed = false;
        for (Element sourceChild : getDirectChildElements(sourceElement)) {
            Element existingChild = findEquivalentChild(targetElement, sourceChild);
            if (existingChild == null) {
                targetElement.appendChild(targetElement.getOwnerDocument().importNode(sourceChild, true));
                changed = true;
            } else {
                changed |= mergeAttributes(existingChild, sourceChild);
                changed |= mergeNestedChildren(existingChild, sourceChild);
            }
        }
        return changed;
    }

    /**
     * Merges attributes from {@code sourceElement} onto {@code targetElement} using
     * existing-wins semantics: attributes already present on the target are preserved
     * (a warning is logged on value conflict), and only attributes missing from the
     * target are copied over. {@code tools:*} attributes and namespace declarations
     * are ignored.
     *
     * @return whether {@code targetElement} was modified
     */
    private static boolean mergeAttributes(Element targetElement, Element sourceElement) {
        boolean changed = false;
        NamedNodeMap sourceAttributes = sourceElement.getAttributes();
        if (sourceAttributes == null) {
            return false;
        }
        for (int i = 0; i < sourceAttributes.getLength(); i++) {
            Node sourceAttr = sourceAttributes.item(i);
            String namespaceUri = sourceAttr.getNamespaceURI();
            String localName = sourceAttr.getLocalName();
            String nodeName = sourceAttr.getNodeName();
            String value = sourceAttr.getNodeValue();

            if ("tools".equals(sourceAttr.getPrefix())
                    || TOOLS_NAMESPACE.equals(namespaceUri)
                    || (nodeName != null && nodeName.startsWith("tools:"))
                    || "xmlns".equals(nodeName)
                    || (nodeName != null && nodeName.startsWith("xmlns:"))) {
                continue;
            }

            boolean hasOnTarget;
            String existingValue;
            if (namespaceUri != null && localName != null) {
                hasOnTarget = targetElement.hasAttributeNS(namespaceUri, localName);
                existingValue = targetElement.getAttributeNS(namespaceUri, localName);
            } else {
                hasOnTarget = targetElement.hasAttribute(nodeName);
                existingValue = targetElement.getAttribute(nodeName);
            }

            if (hasOnTarget) {
                if (existingValue != null && !existingValue.equals(value)) {
                    Log.w(TAG, "Keeping existing attribute '" + nodeName + "=\"" + existingValue
                            + "\"' on <" + targetElement.getTagName() + ">; ignoring local library value \""
                            + value + "\"");
                }
                continue;
            }

            if (namespaceUri != null && localName != null) {
                String prefix = sourceAttr.getPrefix();
                String qualifiedName = (prefix == null || prefix.isEmpty()) ? localName : prefix + ":" + localName;
                targetElement.setAttributeNS(namespaceUri, qualifiedName, value);
            } else {
                targetElement.setAttribute(nodeName, value);
            }
            changed = true;
        }
        return changed;
    }

    /**
     * Merges attributes declared on the local library's {@code <application>} element
     * (e.g. {@code android:name}, {@code android:theme}, {@code android:supportsRtl})
     * onto the generated {@code <application>} element. Placeholders such as
     * {@code ${applicationId}} are resolved against the project package name; any
     * {@code tools:*} attributes or namespace declarations are ignored.
     */
    private static boolean mergeApplicationAttributes(Document generatedManifest, Element applicationElement,
                                                      Element libraryApplicationElement, String packageName) {
        Element shallowCopy = (Element) generatedManifest.importNode(libraryApplicationElement, false);
        replacePlaceholders(shallowCopy, packageName);
        stripToolsAttributes(shallowCopy);
        return mergeAttributes(applicationElement, shallowCopy);
    }

    private static boolean containsEquivalentChild(Element parentElement, Element candidateElement) throws Exception {
        return findEquivalentChild(parentElement, candidateElement) != null;
    }

    private static Element findProviderByAuthorities(Element applicationElement, String authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return null;
        }

        for (Element childElement : getDirectChildElements(applicationElement)) {
            if (!"provider".equals(childElement.getTagName())) {
                continue;
            }
            if (authorities.equals(getAndroidAttribute(childElement, "authorities"))) {
                return childElement;
            }
        }
        return null;
    }

    private static Element findEquivalentChild(Element parentElement, Element candidateElement) throws Exception {
        String candidateKey = getElementKey(candidateElement);
        for (Element childElement : getDirectChildElements(parentElement)) {
            if (!candidateElement.getTagName().equals(childElement.getTagName())) {
                continue;
            }
            if (candidateKey.equals(getElementKey(childElement))) {
                return childElement;
            }
        }
        return null;
    }

    private static String getElementKey(Element element) throws Exception {
        String tagName = element.getTagName();
        if ("uses-permission".equals(tagName) || "uses-permission-sdk-23".equals(tagName)) {
            String permissionName = getAndroidAttribute(element, "name");
            return permissionName.isEmpty() ? serializeNode(element) : permissionName;
        }
        if ("uses-feature".equals(tagName)) {
            String featureName = getAndroidAttribute(element, "name");
            if (!featureName.isEmpty()) {
                return featureName;
            }
            String glEsVersion = getAndroidAttribute(element, "glEsVersion");
            return glEsVersion.isEmpty() ? serializeNode(element) : glEsVersion;
        }
        if ("provider".equals(tagName)) {
            String providerName = normalizeManifestClassName(getAndroidAttribute(element, "name"), getManifestPackageName(element));
            String authorities = getAndroidAttribute(element, "authorities");
            if (!providerName.isEmpty() || !authorities.isEmpty()) {
                return providerName + "|" + authorities;
            }
            return serializeNode(element);
        }
        if ("service".equals(tagName)
                || "receiver".equals(tagName)
                || "activity".equals(tagName)
                || "activity-alias".equals(tagName)
                || "meta-data".equals(tagName)
                || "uses-library".equals(tagName)) {
            String name = getAndroidAttribute(element, "name");
            if ("service".equals(tagName)
                    || "receiver".equals(tagName)
                    || "activity".equals(tagName)
                    || "activity-alias".equals(tagName)) {
                name = normalizeManifestClassName(name, getManifestPackageName(element));
            }
            return name.isEmpty() ? serializeNode(element) : name;
        }
        return serializeNode(element);
    }

    private static String getManifestPackageName(Element element) {
        if (element == null) {
            return "";
        }

        Node currentNode = element;
        while (currentNode != null) {
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element currentElement = (Element) currentNode;
                if ("manifest".equals(currentElement.getTagName())) {
                    return currentElement.getAttribute("package");
                }
            }
            currentNode = currentNode.getParentNode();
        }

        return "";
    }

    private static String resolveLibraryPackageName(Element manifestElement, File manifestFile) {
        String manifestPackageName = manifestElement.getAttribute("package");
        if (manifestPackageName != null) {
            manifestPackageName = manifestPackageName.trim();
        }
        if (manifestPackageName != null && !manifestPackageName.isEmpty()) {
            return manifestPackageName;
        }

        File parentDirectory = manifestFile.getParentFile();
        if (parentDirectory == null) {
            return "";
        }

        File configFile = new File(parentDirectory, "config");
        if (!configFile.isFile()) {
            return "";
        }

        try {
            String configPackageName = FileUtil.readFile(configFile.getAbsolutePath());
            return configPackageName == null ? "" : configPackageName.trim();
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to resolve local library package from config: " + configFile.getAbsolutePath(), e);
            return "";
        }
    }

    private static String normalizeManifestClassName(String className, String packageName) {
        if (className == null || className.isEmpty() || packageName == null || packageName.isEmpty()) {
            return className == null ? "" : className;
        }
        if (className.startsWith(".")) {
            return packageName + className;
        }
        if (className.indexOf('.') == -1) {
            return packageName + "." + className;
        }
        return className;
    }

    private static String getAndroidAttribute(Element element, String attributeName) {
        String value = element.getAttributeNS(ANDROID_NAMESPACE, attributeName);
        if (!value.isEmpty()) {
            return value;
        }

        value = element.getAttribute("android:" + attributeName);
        if (!value.isEmpty()) {
            return value;
        }

        return element.getAttribute(attributeName);
    }

    private static Element importElement(Document generatedManifest, Element sourceElement,
                                         String packageName, String libraryPackageName) {
        Element importedElement = (Element) generatedManifest.importNode(sourceElement, true);
        replacePlaceholders(importedElement, packageName);
        qualifyRelativeComponentNames(importedElement, libraryPackageName);
        stripToolsAttributes(importedElement);
        return importedElement;
    }

    private static void qualifyRelativeComponentNames(Element element, String libraryPackageName) {
        if (libraryPackageName != null && !libraryPackageName.isEmpty()) {
            String tagName = element.getTagName();
            if ("provider".equals(tagName)
                    || "service".equals(tagName)
                    || "receiver".equals(tagName)
                    || "activity".equals(tagName)) {
                qualifyAndroidAttribute(element, "name", libraryPackageName);
            } else if ("activity-alias".equals(tagName)) {
                qualifyAndroidAttribute(element, "name", libraryPackageName);
                qualifyAndroidAttribute(element, "targetActivity", libraryPackageName);
            }
        }

        for (Element childElement : getDirectChildElements(element)) {
            qualifyRelativeComponentNames(childElement, libraryPackageName);
        }
    }

    private static void qualifyAndroidAttribute(Element element, String attributeName, String packageName) {
        String value = getAndroidAttribute(element, attributeName);
        if (value.isEmpty()) {
            return;
        }

        String qualifiedValue = qualifyManifestClassName(value, packageName);
        if (element.hasAttributeNS(ANDROID_NAMESPACE, attributeName)) {
            element.setAttributeNS(ANDROID_NAMESPACE, "android:" + attributeName, qualifiedValue);
        } else if (element.hasAttribute("android:" + attributeName)) {
            element.setAttribute("android:" + attributeName, qualifiedValue);
        } else if (element.hasAttribute(attributeName)) {
            element.setAttribute(attributeName, qualifiedValue);
        }
    }

    private static String qualifyManifestClassName(String value, String packageName) {
        if (value == null || value.isEmpty() || packageName == null || packageName.isEmpty()) {
            return value;
        }
        if (value.startsWith(".")) {
            return packageName + value;
        }
        if (value.indexOf('.') == -1) {
            return packageName + "." + value;
        }
        return value;
    }

    private static void replacePlaceholders(Node node, String packageName) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attributeNode = attributes.item(i);
                attributeNode.setNodeValue(replaceKnownPlaceholders(attributeNode.getNodeValue(), packageName));
            }
        } else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            node.setNodeValue(replaceKnownPlaceholders(node.getNodeValue(), packageName));
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            replacePlaceholders(childNodes.item(i), packageName);
        }
    }

    private static String replaceKnownPlaceholders(String value, String packageName) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("${applicationId}", packageName)
                .replace("${packageName}", packageName);
    }

    private static void stripToolsAttributes(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = attributes.getLength() - 1; i >= 0; i--) {
            Node attributeNode = attributes.item(i);
            String prefix = attributeNode.getPrefix();
            String namespaceUri = attributeNode.getNamespaceURI();
            String nodeName = attributeNode.getNodeName();
            if ("tools".equals(prefix)
                    || TOOLS_NAMESPACE.equals(namespaceUri)
                    || nodeName.startsWith("tools:")) {
                if (attributeNode.getNamespaceURI() != null && attributeNode.getLocalName() != null) {
                    attributes.removeNamedItemNS(attributeNode.getNamespaceURI(), attributeNode.getLocalName());
                } else {
                    attributes.removeNamedItem(attributeNode.getNodeName());
                }
            }
        }

        for (Element childElement : getDirectChildElements(element)) {
            stripToolsAttributes(childElement);
        }
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilder builder = newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        document.normalizeDocument();
        removeEmptyTextNodes(document.getDocumentElement());
        return document;
    }

    private static Document parseXml(File file) throws Exception {
        DocumentBuilder builder = newDocumentBuilder();
        Document document = builder.parse(file);
        document.normalizeDocument();
        removeEmptyTextNodes(document.getDocumentElement());
        return document;
    }

    private static DocumentBuilder newDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }

    private static void removeEmptyTextNodes(Node node) {
        if (node == null) {
            return;
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.TEXT_NODE && childNode.getNodeValue().trim().isEmpty()) {
                node.removeChild(childNode);
            } else {
                removeEmptyTextNodes(childNode);
            }
        }
    }

    private static Element findDirectChild(Element parentElement, String tagName) {
        for (Element childElement : getDirectChildElements(parentElement)) {
            if (tagName.equals(childElement.getTagName())) {
                return childElement;
            }
        }
        return null;
    }

    private static ArrayList<Element> getDirectChildElements(Element parentElement) {
        ArrayList<Element> childElements = new ArrayList<>();
        NodeList childNodes = parentElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) childNode);
            }
        }
        return childElements;
    }

    private static String toXml(Document document) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        String xml = stringWriter.toString().trim();
        return xml.replace("\r\n", "\n").replace("\n", "\r\n") + "\r\n";
    }

    private static String serializeNode(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
        return stringWriter.toString().replace("\r", "").replace("\n", "").trim();
    }
}
