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

import pro.sketchware.core.ComponentCodeGenerator;
import mod.hey.studios.util.Helper;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.model.CustomComponent;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
//responsible code :
//ComponentBean == sketchware / beans
//Manage components == agus /component
//Manage events components == agus/editor/event
//TypeVarComponent == agus / lib
//TypeClassComponent == agus/lib
//importClass== dev.aldi.sayuti.editor.manage

public class ComponentsHandler {

    private static ArrayList<CustomComponent> cachedCustomComponents = readCustomComponents();

    /**
     * This is a utility class, don't instantiate it
     */
    private ComponentsHandler() {
    }

    /**
     * Called at {@link ComponentBean#getComponentTypeByTypeName(String)}.
     */
    // give typeName and return id
    public static int getIdByTypeName(String name) {
        if (name.equals("AsyncTask")) {
            return 36;
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (name.equals(component.getTypeName())) {
                    int idVal = component.getIdAsInt();
                    if (idVal != -1) {
                        return idVal;
                    } else {
                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_invalid_id_in), i + 1), Toast.LENGTH_LONG);
                        break;
                    }
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return -1;
    }

    /**
     * Called at {@link ComponentBean#getComponentTypeName(int)}.
     */
    // give id and return typeName
    public static String getTypeNameById(int id) {
        if (id == 36) {
            return "AsyncTask";
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    return component.getTypeName();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i), Toast.LENGTH_LONG);
            }
        }

        return "";
    }

    /**
     * Called at {@link ComponentBean#getComponentName(Context, int)}.
     */
    // give id and return name
    public static String getNameById(int id) {
        if (id == 36) {
            return "AsyncTask";
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    return component.getName();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }
        return "component";
    }

    /**
     * Called at {@link ComponentBean#getIconResource(int)}.
     */
    // give id and return icon
    public static int getIconById(int id) {
        if (id == 36) {
            return R.drawable.ic_cycle_color_48dp;
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    try {
                        return OldResourceIdMapper.getDrawableFromOldResourceId(Integer.parseInt(component.getIcon()));
                    } catch (NumberFormatException e) {
                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_invalid_icon), i + 1), Toast.LENGTH_LONG);
                        break;
                    }
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return R.drawable.color_new_96;
    }

    /**
     * @return Descriptions of Components, both built-in ones and Custom Components'
     */
    // give id and return description
    //goto ComponentAddActivity
    //remove lines: 2303 to 2307
    //call this method using v0 as id and move result to v0
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
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    return component.getDescription();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return "new component";
    }

    /**
     * Called at {@link ComponentBean#getComponentDocsUrlByTypeName(int)}.
     */
    // give id and return docs url
    public static String getDocsUrlById(int id) {
        if (id != 36) {
            for (int i = 0; i < cachedCustomComponents.size(); i++) {
                CustomComponent component = cachedCustomComponents.get(i);
                if (component != null) {
                    if (component.getIdAsInt() == id) {
                        return component.getUrl();
                    }
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
                }
            }
        }

        return "";
    }

    /**
     * Called at {@link ComponentBean#buildClassInfo()}.
     */
    public static String getBuildClassById(int id) {
        if (id == 36) {
            return "AsyncTask";
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    return component.getBuildClass();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return "";
    }

    // mod section

    /**
     * Adds Custom Components to available Components section.
     * Used at {@link com.besome.sketch.editor.component.AddComponentBottomSheet#onCreate(Bundle)}.
     */
    // add components to sk
    //structure : list.add(new ComponentBean(27));
    public static void addCustomComponents(ArrayList<ComponentBean> list) {
        list.add(new ComponentBean(36));

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                int idVal = component.getIdAsInt();
                if (idVal != -1) {
                    list.add(new ComponentBean(idVal));
                } else {
                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_invalid_id_for), i + 1), Toast.LENGTH_LONG);
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }
    }

    public static String getVarNameById(int id) {
        if (id == 36) {
            return "#";
        }

        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (component.getIdAsInt() == id) {
                    return component.getVarName();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return "";
    }

    /**
     * @param name The desired Custom Component's <code>typeName</code>
     * @return A Custom Component's <code>class</code>
     */
    @NonNull
    public static String getClassByTypeName(@NonNull String name) {
        if (name.equals("AsyncTask")) {
            return "Component.AsyncTask";
        }

        for (int i = 0, customComponentsSize = cachedCustomComponents.size(); i < customComponentsSize; i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (name.equals(component.getTypeName())) {
                    return component.getClassName();
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return "Component";
    }

    /**
     * Used at {@link pro.sketchware.core.ComponentCodeGenerator#getExtraVar(String, String, ComponentCodeGenerator.AccessModifier, String...)}
     * to get Custom Components' fields.
     */
    public static String getExtraVar(String name, String code, String varName) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (name.equals(component.getName())) {
                    String additionalVar = component.getAdditionalVar();
                    if (TextUtils.isEmpty(additionalVar)) {
                        return code;
                    } else {
                        return code + "\r\n" + additionalVar.replace("###", varName);
                    }
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return code;
    }

    // define extra variable
    public static String getDefineExtraVar(String name, String varName) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (name.equals(component.getName())) {
                    String defineAdditionalVar = component.getDefineAdditionalVar();
                    if (TextUtils.isEmpty(defineAdditionalVar)) {
                        break;
                    } else {
                        return defineAdditionalVar.replace("###", varName);
                    }
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }

        return "";
    }

    public static void getImports(String name, ArrayList<String> arrayList) {
        for (int i = 0; i < cachedCustomComponents.size(); i++) {
            CustomComponent component = cachedCustomComponents.get(i);
            if (component != null) {
                if (name.equals(component.getVarName())) {
                    String imports = component.getImports();
                    if (!imports.isEmpty()) {
                        arrayList.addAll(Arrays.asList(imports.split("\n")));
                    }
                }
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.component_error_null), i));
            }
        }
    }

    public static String getPath() {
        return FileUtil.getExternalStorageDir().concat("/.sketchware/data/system/component.json");
    }

    /**
     * @return List of Custom Components. Will never return null, but will warn the user about
     * an invalid Custom Components JSON file.
     */
    private static ArrayList<CustomComponent> readCustomComponents() {
        ArrayList<CustomComponent> data;
        if (FileUtil.isExistFile(getPath())) {
            try {
                data = new Gson().fromJson(FileUtil.readFile(getPath()),
                        new TypeToken<ArrayList<CustomComponent>>(){}.getType());
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
            components = new Gson().fromJson(content,
                    new TypeToken<ArrayList<CustomComponent>>(){}.getType());
        } catch (JsonSyntaxException e) {
            return new Pair<>(Optional.of(Helper.getResString(R.string.publish_message_dialog_invalid_json)), Collections.emptyList());
        }
        if (components == null || components.isEmpty() || !isValidComponentList(components)) {
            return new Pair<>(Optional.of(Helper.getResString(R.string.publish_message_dialog_invalid_json)), Collections.emptyList());
        }

        return new Pair<>(Optional.empty(), components);
    }
}
