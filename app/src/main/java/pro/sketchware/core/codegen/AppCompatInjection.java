package pro.sketchware.core.codegen;

import com.google.gson.Gson;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.layout.InjectAttributeHandler;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.util.xml.XmlBuilder;

public class AppCompatInjection {

    // maps sc_id to (activity filename mapped to (list of injections))
    private static final Map<String, Map<String, List<? extends Map<String, Object>>>> INJECTIONS =
            new HashMap<>();

    private final String sc_id;
    private final ProjectFileBean projectFile;

    public AppCompatInjection(String projectId, ProjectFileBean projectFileBean) {
        sc_id = projectId;
        projectFile = projectFileBean;
    }

    public static String getDefaultActivityInjections() {
        return "[{\"type\":\"toolbar\",\"value\":\"android:layout_width=\\\"match_parent\\\"\"},{\"type\":\"toolbar\",\"value\":\"android:layout_height=\\\"?attr/actionBarSize\\\"\"},{\"type\":\"toolbar\",\"value\":\"android:background=\\\"?attr/colorPrimary\\\"\"},{\"type\":\"toolbar\",\"value\":\"app:popupTheme=\\\"@style/AppTheme.PopupOverlay\\\"\"},{\"type\":\"appbarlayout\",\"value\":\"android:layout_width=\\\"match_parent\\\"\"},{\"type\":\"appbarlayout\",\"value\":\"android:layout_height=\\\"wrap_content\\\"\"},{\"type\":\"appbarlayout\",\"value\":\"android:theme=\\\"@style/AppTheme.AppBarOverlay\\\"\"},{\"type\":\"coordinatorlayout\",\"value\":\"android:layout_width=\\\"match_parent\\\"\"},{\"type\":\"coordinatorlayout\",\"value\":\"android:layout_height=\\\"match_parent\\\"\"},{\"type\":\"drawerlayout\",\"value\":\"android:layout_width=\\\"match_parent\\\"\"},{\"type\":\"drawerlayout\",\"value\":\"android:layout_height=\\\"match_parent\\\"\"},{\"type\":\"drawerlayout\",\"value\":\"tools:openDrawer=\\\"start\\\"\"},{\"type\":\"navigationdrawer\",\"value\":\"android:layout_width=\\\"320dp\\\"\"},{\"type\":\"navigationdrawer\",\"value\":\"android:layout_height=\\\"match_parent\\\"\"},{\"type\":\"navigationdrawer\",\"value\":\"android:layout_gravity=\\\"start\\\"\"},{\"type\":\"navigationdrawer\",\"value\":\"android:background=\\\"#EEEEEE\\\"\"}]";
    }

    private static List<? extends Map<String, Object>> readAppCompatInjections(
            String sc_id, String activityFilename) {
        String toParse;

        File injectionFile =
                new File(SketchwarePaths.getAppCompatInjectionPath(sc_id, activityFilename));
        String fileContent;
        if (injectionFile.exists()
                && !(fileContent = FileUtil.readFile(injectionFile.getAbsolutePath())).isEmpty()) {
            toParse = fileContent;
        } else {
            toParse = getDefaultActivityInjections();
        }

        try {
            return new Gson().fromJson(toParse, Helper.TYPE_MAP_LIST);
        } catch (com.google.gson.JsonSyntaxException e) {
            return new Gson().fromJson(getDefaultActivityInjections(), Helper.TYPE_MAP_LIST);
        }
    }

    public static synchronized void invalidate(String projectId, String activityName) {
        var project = INJECTIONS.get(projectId);
        if (project != null) project.remove(activityName);
    }

    private static synchronized List<? extends Map<String, Object>> getInjections(
            String projectId, String activityName) {
        return INJECTIONS
                .computeIfAbsent(projectId, key -> new HashMap<>())
                .computeIfAbsent(activityName, name -> readAppCompatInjections(projectId, name));
    }

    public void inject(XmlBuilder xmlBuilder, String injectionType) {
        forEachAttribute(injectionType, xmlBuilder::addAttributeValue);
    }

    public Map<String, String> getAttributes(String injectionType) {
        var xml = new StringJoiner(" ");
        forEachAttribute(injectionType, xml::add);
        return InjectAttributeHandler.parseNamespacedAttributes(xml.toString());
    }

    private void forEachAttribute(String injectionType, Consumer<String> useAttribute) {
        if (injectionType == null
                || !(projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR)
                        || projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)
                        || projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB))) {
            return;
        }
        var injections = getInjections(sc_id, projectFile.fileName);
        if (injections == null) return;
        for (Map<String, Object> injection : injections) {
            if (injectionType.equalsIgnoreCase(String.valueOf(injection.get("type")))
                    && injection.get("value") instanceof String value) {
                useAttribute.accept(value);
            }
        }
    }
}
