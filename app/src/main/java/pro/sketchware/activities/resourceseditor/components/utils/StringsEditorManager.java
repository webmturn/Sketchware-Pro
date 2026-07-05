package pro.sketchware.activities.resourceseditor.components.utils;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.StringResourceResolver;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.util.XmlUtil;

public class StringsEditorManager extends StringResourceResolver {

    public boolean isDefaultVariant = true;
    public String sc_id;

    @Override
    public void convertXmlStringsToListMap(final String xmlString, final ArrayList<HashMap<String, Object>> listMap) {
        super.convertXmlStringsToListMap(xmlString, listMap);
        if (isDataLoadingFailed || !isDefaultVariant || hasAppNameKey) {
            return;
        }
        if (sc_id == null) {
            return;
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("key", "app_name");
        map.put("text", MapValueHelper.getString(ProjectListManager.getProjectById(sc_id), "my_app_name"));
        listMap.add(0, map);
        XmlUtil.saveXml(SketchwarePaths.getDataPath(sc_id) + "/files/resource/values/strings.xml", convertListMapToXmlStrings(listMap, notesMap));
    }
}
