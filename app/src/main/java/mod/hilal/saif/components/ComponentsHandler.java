package mod.hilal.saif.components;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.besome.sketch.beans.ComponentBean;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import pro.sketchware.core.codegen.ComponentCodeGenerator;
import pro.sketchware.core.SketchwarePaths;
import mod.hey.studios.util.Helper;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.model.CustomComponent;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ComponentsHandler {

    private static final int ASYNC_TASK_COMPONENT_ID = 36;
    private static final String ASYNC_TASK_TYPE_NAME = "AsyncTask";
    private static final Gson GSON = new Gson();
    private static ArrayList<CustomComponent> cachedCustomComponents = readCustomComponents();

    /**
     * This is a utility class, don't instantiate it
     */
    private ComponentsHandler() {
    }

    private static final class ComponentMatch {

        private final int index;
        private final CustomComponent component;

        private ComponentMatch(int index, CustomComponent component) {
            this.index = index;
            this.component = component;
        }
    }

    private static void reportNullComponent(int index) {
        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), index));
    }

    private static int getComponentIdOrReport(CustomComponent component, int index, int errorResId) {
        int idVal = component.getIdAsInt();
        if (idVal == -1) {
            SketchwareUtil.toastError(String.format(Helper.getResString(errorResId), index + 1), Toast.LENGTH_LONG);
        }
        return idVal;
    }

    private static ComponentMatch findComponentById(int id) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            if (component.getIdAsInt() == id) {
                return new ComponentMatch(i, component);
            }
        }
        return null;
    }

    private static ComponentMatch findComponentByTypeName(String typeName) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            if (typeName.equals(component.getTypeName())) {
                return new ComponentMatch(i, component);
            }
        }
        return null;
    }

    private static ComponentMatch findComponentByName(String name) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            if (name.equals(component.getName())) {
                return new ComponentMatch(i, component);
            }
        }
        return null;
    }

    private static ComponentMatch findComponentByVarName(String varName) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            if (varName.equals(component.getVarName())) {
                return new ComponentMatch(i, component);
            }
        }
        return null;
    }

    private static ArrayList<CustomComponent> parseCustomComponents(String content) throws JsonSyntaxException {
        return GSON.fromJson(content, new TypeToken<ArrayList<CustomComponent>>(){}.getType());
    }

    /**
     * Called at {@link ComponentBean#getComponentTypeByTypeName(String)}.
     */
    public static int getIdByTypeName(String name) {
        if (name.equals(ASYNC_TASK_TYPE_NAME)) {
            return ASYNC_TASK_COMPONENT_ID;
        }

        ComponentMatch match = findComponentByTypeName(name);
        if (match == null) {
            return -1;
        }
        int idVal = getComponentIdOrReport(match.component, match.index, R.string.component_error_invalid_id_in);
        return idVal != -1 ? idVal : -1;
    }

    /**
     * Called at {@link ComponentBean#getComponentTypeName(int)}.
     */
    public static String getTypeNameById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return ASYNC_TASK_TYPE_NAME;
        }

        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getTypeName() : "";
    }

    /**
     * Called at {@link ComponentBean#getComponentName(Context, int)}.
     */
    public static String getNameById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return ASYNC_TASK_TYPE_NAME;
        }

        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getName() : "component";
    }

    /**
     * Called at {@link ComponentBean#getIconResource(int)}.
     */
    public static int getIconById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return R.drawable.ic_cycle_color_48dp;
        }

        ComponentMatch match = findComponentById(id);
        if (match == null) {
            return R.drawable.color_new_96;
        }
        try {
            return OldResourceIdMapper.getDrawableFromOldResourceId(Integer.parseInt(match.component.getIcon()));
        } catch (NumberFormatException e) {
            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_invalid_icon), match.index + 1), Toast.LENGTH_LONG);
            return R.drawable.color_new_96;
        }
    }

    /**
     * @return Descriptions of Components, both built-in ones and Custom Components'
     */
    public static String getDescription(int id) {
        int componentBeanDescriptionResId = ComponentBean.getDescStrResource(id);
        if (componentBeanDescriptionResId != 0) {
            return SketchApplication.getContext().getString(componentBeanDescriptionResId);
        } else {
            return getCustomDescription(id);
        }
    }

    /**
     * @return Component description of a Custom Component
     */
    public static String getCustomDescription(int id) {
        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getDescription() : "new component";
    }

    /**
     * Called at {@link ComponentBean#getComponentDocsUrlByTypeName(int)}.
     */
    public static String getDocsUrlById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return "";
        }

        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getUrl() : "";
    }

    /**
     * Called at {@link ComponentBean#buildClassInfo()}.
     */
    public static String getBuildClassById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return ASYNC_TASK_TYPE_NAME;
        }

        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getBuildClass() : "";
    }

    /**
     * Adds Custom Components to available Components section.
     * Used at {@link com.besome.sketch.editor.component.AddComponentBottomSheet#onCreate(Bundle)}.
     */
    public static void addCustomComponents(ArrayList<ComponentBean> list) {
        list.add(new ComponentBean(ASYNC_TASK_COMPONENT_ID));

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            int idVal = getComponentIdOrReport(component, i, R.string.component_error_invalid_id_for);
            if (idVal != -1) {
                list.add(new ComponentBean(idVal));
            }
        }
    }

    public static String getVarNameById(int id) {
        if (id == ASYNC_TASK_COMPONENT_ID) {
            return "#";
        }

        ComponentMatch match = findComponentById(id);
        return match != null ? match.component.getVarName() : "";
    }

    /**
     * @param name The desired Custom Component's <code>typeName</code>
     * @return A Custom Component's <code>class</code>
     */
    @NonNull
    public static String getClassByTypeName(@NonNull String name) {
        if (name.equals(ASYNC_TASK_TYPE_NAME)) {
            return "Component.AsyncTask";
        }

        ComponentMatch match = findComponentByTypeName(name);
        return match != null ? match.component.getClassName() : "Component";
    }

    /**
     * Used at {@link pro.sketchware.core.codegen.ComponentCodeGenerator#getExtraVar(String, String, ComponentCodeGenerator.AccessModifier, String...)}
     * to get Custom Components' fields.
     */
    public static String getExtraVar(String name, String code, String varName) {
        ComponentMatch match = findComponentByName(name);
        if (match == null) {
            return code;
        }
        String additionalVar = match.component.getAdditionalVar();
        return TextUtils.isEmpty(additionalVar) ? code : code + "\r\n" + additionalVar.replace("###", varName);
    }

    public static String getDefineExtraVar(String name, String varName) {
        ComponentMatch match = findComponentByName(name);
        if (match == null) {
            return "";
        }
        String defineAdditionalVar = match.component.getDefineAdditionalVar();
        return TextUtils.isEmpty(defineAdditionalVar) ? "" : defineAdditionalVar.replace("###", varName);
    }

    public static void getImports(String name, ArrayList<String> arrayList) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component == null) {
                reportNullComponent(i);
                continue;
            }
            if (name.equals(component.getVarName())) {
                String imports = component.getImports();
                if (!imports.isEmpty()) {
                    arrayList.addAll(Arrays.asList(imports.split("\n")));
                }
            }
        }
    }

    public static String getPath() {
        return SketchwarePaths.getCustomComponent();
    }

    /**
     * @return List of Custom Components. Will never return null, but will warn the user about
     * an invalid Custom Components JSON file.
     */
    private static ArrayList<CustomComponent> readCustomComponents() {
        ArrayList<CustomComponent> data;
        if (FileUtil.isExistFile(getPath())) {
            String content = FileUtil.readFile(getPath());
            try {
                data = parseCustomComponents(content);
            } catch (JsonSyntaxException e) {
                data = new ArrayList<>();
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_read_failed), e.getMessage()));
            }
            if (data == null) {
                SketchwareUtil.toastError(Helper.getResString(R.string.component_error_invalid_file));
                data = new ArrayList<>();
            }
        } else {
            data = new ArrayList<>();
        }

        return data;
    }

    public static void refreshCachedCustomComponents() {
        cachedCustomComponents = readCustomComponents();
    }

    public static boolean isValidComponent(CustomComponent component) {
        return !component.getName().isEmpty()
                && !component.getId().isEmpty()
                && !component.getIcon().isEmpty()
                && !component.getVarName().isEmpty()
                && !component.getTypeName().isEmpty()
                && !component.getBuildClass().isEmpty()
                && !component.getClassName().isEmpty()
                && !component.getDescription().isEmpty()
                && !component.getUrl().isEmpty();
    }

    public static boolean isValidComponentList(List<CustomComponent> list) {
        for (CustomComponent component : list) {
            if (!isValidComponent(component)) {
                return false;
            }
        }
        return true;
    }

    public static Pair<Optional<String>, List<CustomComponent>> readComponents(String filePath) {
        String content = FileUtil.readFile(filePath);
        if (content.isEmpty() || content.equals("[]")) {
            return new Pair<>(Optional.of(Helper.getResString(R.string.common_message_selected_file_empty)), Collections.emptyList());
        }

        List<CustomComponent> components;
        try {
            components = parseCustomComponents(content);
        } catch (JsonSyntaxException e) {
            return new Pair<>(Optional.of(Helper.getResString(R.string.publish_message_dialog_invalid_json)), Collections.emptyList());
        }
        if (components == null || components.isEmpty() || !isValidComponentList(components)) {
            return new Pair<>(Optional.of(Helper.getResString(R.string.publish_message_dialog_invalid_json)), Collections.emptyList());
        }

        return new Pair<>(Optional.empty(), components);
    }
}
