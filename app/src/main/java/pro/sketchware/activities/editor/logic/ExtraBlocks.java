package pro.sketchware.activities.editor.logic;

import android.util.Pair;

import pro.sketchware.activities.editor.LogicEditorActivity;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.activities.settings.ConfigActivity;

public class ExtraBlocks {

    private static final Pattern CUSTOM_VAR_PATTERN = Pattern.compile("^(\\w+)[\\s]+(\\w+)");

    private final String eventName;
    private final String javaName;
    private final LogicEditorActivity logicEditor;
    private final ProjectDataStore projectDataManager;
    private Set<String> customVarTypesCache;

    public ExtraBlocks(LogicEditorActivity logicEditor) {
        eventName = logicEditor.eventName;
        this.logicEditor = logicEditor;
        javaName = logicEditor.projectFile.getJavaName();
        projectDataManager = ProjectDataManager.getProjectDataManager(logicEditor.scId);
    }

    public boolean isVariableUsed(int varId) {
        for (Pair<Integer, String> entry : projectDataManager.getVariables(javaName)) {
            if (entry.first == varId) return true;
        }
        return false;
    }

    public boolean isListUsed(int listId) {
        for (Pair<Integer, String> entry : projectDataManager.getListVariables(javaName)) {
            if (entry.first == listId) return true;
        }
        return false;
    }

    public boolean isComponentUsed(int componentId) {
        return projectDataManager.hasComponentOfType(javaName, componentId) || ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK);
    }

    public void invalidateCustomVarCache() {
        customVarTypesCache = null;
    }

    public boolean isCustomVarUsed(String variable) {
        if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK)) {
            return true;
        }
        if (customVarTypesCache == null) {
            customVarTypesCache = new HashSet<>();
            for (String variableName : projectDataManager.getVariableNamesByType(javaName, 5)) {
                Matcher matcher = CUSTOM_VAR_PATTERN.matcher(variableName);
                if (matcher.find()) {
                    customVarTypesCache.add(matcher.group(1));
                }
            }
        }
        return customVarTypesCache.contains(variable);
    }

    public void eventBlocks() {
        if (eventName.equals("onCreateOptionsMenu")) {
            logicEditor.addPaletteCategory("Menu Item", 0xff555555);
            logicEditor.createPaletteBlock(" ", "menuItemSetVisible");
            logicEditor.createPaletteBlock(" ", "menuItemSetEnabled");
            logicEditor.createPaletteBlock("v", "menuFindItem");
        }
    }

    public void fileBlocks() {
        if (isCustomVarUsed("File")) {
            logicEditor.addPaletteCategory("File Blocks", 0xff555555);
            logicEditor.createPaletteBlock("b", "fileCanExecute");
            logicEditor.createPaletteBlock("b", "fileCanRead");
            logicEditor.createPaletteBlock("b", "fileCanWrite");
            logicEditor.createPaletteBlock("s", "fileGetName");
            logicEditor.createPaletteBlock("s", "fileGetParent");
            logicEditor.createPaletteBlock("s", "fileGetPath");
            logicEditor.createPaletteBlock("b", "fileIsHidden");
        }
    }
}
