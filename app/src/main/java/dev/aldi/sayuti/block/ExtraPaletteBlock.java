package dev.aldi.sayuti.block;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.util.Pair;

import androidx.annotation.ColorInt;

import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.LogicEditorActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import pro.sketchware.core.LayoutGenerator;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.BuildConfig;
import pro.sketchware.core.BlockColorMapper;
import mod.agus.jcoderz.beans.ViewBeans;
import mod.hey.studios.editor.view.IdGenerator;
import mod.hey.studios.util.Helper;
import mod.hey.studios.moreblock.ReturnMoreblockManager;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.hilal.saif.blocks.BlocksHandler;
import mod.pranav.viewbinding.ViewBindingBuilder;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.blocks.ExtraBlocks;
import pro.sketchware.control.logic.LogicClickListener;
import pro.sketchware.utility.CustomVariableUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExtraPaletteBlock {

    private final String eventName;
    private final String javaName;
    private final String xmlName;
    private final String sc_id;

    private final LogicClickListener clickListener;
    private final ExtraBlocks extraBlocks;
    private final FileResConfig frc;
    private final HashMap<String, Object> mapSave = new HashMap<>();
    private final ProjectFileBean projectFile;
    private final Boolean isViewBindingEnabled;
    public LogicEditorActivity logicEditor;

    public ExtraPaletteBlock(LogicEditorActivity logicEditorActivity, Boolean isViewBindingEnabled) {
        logicEditor = logicEditorActivity;
        eventName = logicEditorActivity.eventName;

        projectFile = logicEditor.projectFile;
        javaName = projectFile.getJavaName();
        xmlName = projectFile.getXmlName();
        sc_id = logicEditor.scId;
        this.isViewBindingEnabled = isViewBindingEnabled;

        frc = new FileResConfig(sc_id);
        extraBlocks = new ExtraBlocks(logicEditor);
        clickListener = new LogicClickListener(logicEditor);
    }

    private boolean isWidgetUsed(String str) {
        if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_EVERY_SINGLE_BLOCK)) {
            return true;
        }

        if (mapSave.containsKey(str)) {
            Object strValueInMapSave = mapSave.get(str);
            if (strValueInMapSave instanceof Boolean) {
                return (boolean) strValueInMapSave;
            } else {
                return false;
            }
        }
        if (eventName.equals("onBindCustomView")) {
            var ProjectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
            var view = ProjectDataStore.getViewBean(xmlName, logicEditor.id);
            if (view == null) {
                // in case the View's in a Drawer
                view = ProjectDataStore.getViewBean("_drawer_" + xmlName, logicEditor.id);
            }
            String customView = view.customView;
            if (customView != null && !customView.isEmpty()) {
                for (ViewBean viewBean : ProjectDataManager.getProjectDataManager(sc_id).getViews(ProjectFileBean.getXmlName(customView))) {
                    if (viewBean.getClassInfo().matchesType(str)) {
                        mapSave.put(str, true);
                        return true;
                    }
                }
            }
        } else if (ProjectDataManager.getProjectDataManager(sc_id).hasViewMatchingType(xmlName, str)) {
            mapSave.put(str, true);
            return true;
        }
        mapSave.put(str, false);
        return false;
    }

    /*
     * ExtraPaletteBlock#f(FieldBlockView) moved to mod.w3wide.menu.ExtraMenuBean#defineMenuSelector(FieldBlockView)
     * for better block menu selections and to add new stuff easily.
     */

    public boolean hasExtraComponent(String str, String str2) {
        return switch (str) {
            case "circleimageview" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_CIRCLEIMAGEVIEW, str2);
            case "asynctask" -> ProjectDataManager.getProjectDataManager(sc_id).hasComponent(javaName, 36, str2);
            case "otpview" -> ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_OTPVIEW, str2);
            case "lottie" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_LOTTIEANIMATIONVIEW, str2);
            case "phoneauth" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE, str2);
            case "codeview" -> ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_CODEVIEW, str2);
            case "recyclerview" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_RECYCLERVIEW, str2);
            case "googlelogin" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN, str2);
            case "youtubeview" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_YOUTUBEPLAYERVIEW, str2);
            case "signinbutton" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_SIGNINBUTTON, str2);
            case "cardview" -> ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_CARDVIEW, str2);
            case "radiogroup" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_RADIOGROUP, str2);
            case "textinputlayout" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_TEXTINPUTLAYOUT, str2);
            case "collapsingtoolbar" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_COLLAPSINGTOOLBARLAYOUT, str2);
            case "cloudmessage" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasComponent(javaName, ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE, str2);
            case "datepicker" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_DATEPICKER, str2);
            case "customVar" -> ProjectDataManager.getProjectDataManager(sc_id).hasVariable(xmlName, 5, str2);
            case "timepicker" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_WIDGET_TIMEPICKER, str2);
            case "swiperefreshlayout" ->
                    ProjectDataManager.getProjectDataManager(sc_id).hasViewOfType(xmlName, ViewBeans.VIEW_TYPE_LAYOUT_SWIPEREFRESHLAYOUT, str2);
            default -> true;
        };
    }

    /**
     * @see ReturnMoreblockManager#listMoreblocks(Iterator, LogicEditorActivity)
     */
    private void moreBlocks() {
        ReturnMoreblockManager.listMoreblocks(ProjectDataManager.getProjectDataManager(sc_id).getMoreBlocks(javaName).iterator(), logicEditor);
    }

    private void variables() {
        ArrayList<String> booleanVariables = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 0);
        for (int i = 0; i < booleanVariables.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_boolean), getTitleBgColor());

            logicEditor.createPaletteBlockWithSpec(booleanVariables.get(i), "b", "getVar").setTag(booleanVariables.get(i));
        }

        ArrayList<String> numberVariables = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 1);
        for (int i = 0; i < numberVariables.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_number), getTitleBgColor());

            logicEditor.createPaletteBlockWithSpec(numberVariables.get(i), "d", "getVar").setTag(numberVariables.get(i));
        }

        ArrayList<String> stringVariables = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 2);
        for (int i = 0; i < stringVariables.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_string), getTitleBgColor());

            logicEditor.createPaletteBlockWithSpec(stringVariables.get(i), "s", "getVar").setTag(stringVariables.get(i));
        }

        ArrayList<String> mapVariables = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 3);
        for (int i = 0; i < mapVariables.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_map), getTitleBgColor());

            logicEditor.createPaletteBlockWithSpec(mapVariables.get(i), "a", "getVar").setTag(mapVariables.get(i));
        }

        ArrayList<String> customVariables = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 5);
        for (int i = 0; i < customVariables.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_custom_variable), getTitleBgColor());

            String[] split = customVariables.get(i).split(" ");
            if (split.length > 1) {
                logicEditor.createPaletteBlockWithComponent(split[1], "v", split[0], "getVar").setTag(customVariables.get(i));
            } else {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.extra_block_error_invalid_var), i + 1, customVariables.get(i)));
            }
        }

        ArrayList<String> customVariables2 = ProjectDataManager.getProjectDataManager(sc_id).getVariableNamesByType(javaName, 6);
        for (int i = 0; i < customVariables2.size(); i++) {
            if (i == 0) logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_custom_variable), getTitleBgColor());

            String variable = customVariables2.get(i);
            String variableType = CustomVariableUtil.getVariableType(variable);
            String variableName = CustomVariableUtil.getVariableName(variable);
            if (variableType != null && variableName != null) {
                String type = switch (variableType) {
                    case "boolean", "Boolean" -> "b";
                    case "String" -> "s";
                    case "double", "Double", "int", "Integer", "float", "Float", "long", "Long",
                         "short", "Short" -> "d";
                    default -> "v";
                };
                logicEditor.createPaletteBlockWithComponent(variableName, type, variableType, "getVar").setTag(variable);
            } else {
                logicEditor.addPaletteCategory(String.format(Helper.getResString(R.string.logic_editor_invalid_label), variable), getColor(logicEditor, R.attr.colorError));
            }
        }
        BlocksHandler.primaryBlocksA(
                logicEditor,
                extraBlocks.isVariableUsed(0),
                extraBlocks.isVariableUsed(1),
                extraBlocks.isVariableUsed(2),
                extraBlocks.isVariableUsed(3)
        );
        blockCustomViews();
        blockDrawer();
        blockEvents();
        extraBlocks.eventBlocks();
        blockComponents();
    }

    private void blockComponents() {
        ArrayList<ComponentBean> components = ProjectDataManager.getProjectDataManager(sc_id).getComponents(javaName);
        for (int i = 0, componentsSize = components.size(); i < componentsSize; i++) {
            ComponentBean component = components.get(i);

            if (i == 0) {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_components), getTitleBgColor());
            }

            if (component.type != 27) {
                logicEditor.createPaletteBlockWithComponent(component.componentId, "p", ComponentBean.getComponentTypeName(component.type), "getVar").setTag(component.componentId);
            }
        }
    }

    private void blockCustomViews() {
        if (eventName.equals("onBindCustomView")) {
            String viewId = logicEditor.id;
            var ProjectDataStore = ProjectDataManager.getProjectDataManager(sc_id);
            ViewBean viewBean = ProjectDataStore.getViewBean(xmlName, viewId);
            if (viewBean == null) {
                // Event is of a Drawer View
                viewBean = ProjectDataStore.getViewBean("_drawer_" + xmlName, viewId);
            }
            String viewBeanCustomView = viewBean.customView;
            if (viewBeanCustomView != null && !viewBeanCustomView.isEmpty()) {
                ArrayList<ViewBean> customViews = ProjectDataManager.getProjectDataManager(sc_id).getViews(ProjectFileBean.getXmlName(viewBeanCustomView));
                for (int i = 0, customViewsSize = customViews.size(); i < customViewsSize; i++) {
                    ViewBean customView = customViews.get(i);

                    if (i == 0) {
                        logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_custom_views), getTitleBgColor());
                    }

                    if (!customView.convert.equals("include")) {
                        String typeName = customView.convert.isEmpty() ? ViewBean.getViewTypeName(customView.type) : IdGenerator.getLastPath(customView.convert);
                        String resultId = isViewBindingEnabled ? "binding." + ViewBindingBuilder.generateParameterFromId(customView.id) : customView.id;
                        logicEditor.createPaletteBlockWithComponent(resultId, "v", typeName, "getVar").setTag(resultId);
                    }
                }
            }
            logicEditor.createPaletteBlock(" ", "notifyDataSetChanged");
            logicEditor.createPaletteBlock("c", "viewOnClick");
            logicEditor.createPaletteBlock("c", "viewOnLongClick");
            logicEditor.createPaletteBlock("c", "checkboxOnChecked");
            logicEditor.createPaletteBlock("b", "checkboxIsChecked");
            return;
        }
        ArrayList<ViewBean> views = ProjectDataManager.getProjectDataManager(sc_id).getViews(xmlName);
        for (int i = 0, viewsSize = views.size(); i < viewsSize; i++) {
            ViewBean view = views.get(i);
            Set<String> toNotAdd = new LayoutGenerator(new BuildConfig(), projectFile).readAttributesToReplace(view);

            if (i == 0) {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_views), getTitleBgColor());
            }

            if (!view.convert.equals("include")) {
                if (!toNotAdd.contains("android:id")) {
                    String typeName = view.convert.isEmpty() ? ViewBean.getViewTypeName(view.type) : IdGenerator.getLastPath(view.convert);
                    logicEditor.createPaletteBlockWithComponent(isViewBindingEnabled ? "binding." + ViewBindingBuilder.generateParameterFromId(view.id) : view.id, "v", typeName, "getVar").setTag(isViewBindingEnabled ? "binding." + ViewBindingBuilder.generateParameterFromId(view.id) : view.id);
                }
            }
        }
    }

    private void blockDrawer() {
        if (projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
            ArrayList<ViewBean> drawerViews = ProjectDataManager.getProjectDataManager(sc_id).getViews(projectFile.getDrawerXmlName());
            if (drawerViews != null) {
                for (int i = 0, drawerViewsSize = drawerViews.size(); i < drawerViewsSize; i++) {
                    ViewBean drawerView = drawerViews.get(i);

                    if (i == 0) {
                        logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_drawer_views), getTitleBgColor());
                    }

                    if (!drawerView.convert.equals("include")) {
                        String id = "_drawer_" + drawerView.id;
                        String typeName = drawerView.convert.isEmpty() ? ViewBean.getViewTypeName(drawerView.type) : IdGenerator.getLastPath(drawerView.convert);
                        logicEditor.createPaletteBlockWithComponent(isViewBindingEnabled ? "binding.drawer." + ViewBindingBuilder.generateParameterFromId(drawerView.id) : id, "v", typeName, "getVar").setTag(id);
                    }
                }
            }
        }
    }

    private void blockEvents() {
        switch (eventName) {
            case "onTabAdded", "onTabLayoutNewTabAdded" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_fragment_tablayout), getTitleBgColor());
                logicEditor.createPaletteBlock("f", "returnTitle");
            }
            case "onFragmentAdded" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_fragment_tablayout), getTitleBgColor());
                logicEditor.createPaletteBlock("f", "returnFragment");
            }
            case "onScrollChanged" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_listview), getTitleBgColor());
                logicEditor.createPaletteBlock("d", "listscrollparam");
                logicEditor.createPaletteBlock("d", "getLastVisiblePosition");
            }
            case "onScrollChanged2" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_recyclerview), getTitleBgColor());
                logicEditor.createPaletteBlock("d", "recyclerscrollparam");
            }
            case "onPageChanged" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_viewpager), getTitleBgColor());
                logicEditor.createPaletteBlock("d", "pagerscrollparam");
            }
            case "onCreateOptionsMenu" -> {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_menu), getTitleBgColor());
                logicEditor.createPaletteBlock(" ", "menuInflater");
                logicEditor.createPaletteBlock(" ", "menuAddItem");
                logicEditor.createPaletteBlock(" ", "menuAddMenuItem");
                logicEditor.createPaletteBlock("c", "menuAddSubmenu");
                logicEditor.createPaletteBlock(" ", "submenuAddItem");
            }
            default -> {
            }
        }
    }

    private void list() {
        for (Pair<Integer, String> list : ProjectDataManager.getProjectDataManager(sc_id).getListVariables(javaName)) {
            int type = list.first;
            String name = list.second;

            switch (type) {
                case 1, 2, 3 -> logicEditor.createPaletteBlockWithComponent(name, "l", BlockColorMapper.getListTypeName(type), "getVar").setTag(name);
                default -> {
                    String variableName = CustomVariableUtil.getVariableName(name);
                    if (variableName != null) {
                        logicEditor.createPaletteBlockWithComponent(variableName, "l", "List", "getVar").setTag(name);
                    } else {
                        logicEditor.addPaletteCategory(String.format(Helper.getResString(R.string.logic_editor_invalid_label), name), getColor(logicEditor, R.attr.colorError));
                    }
                }
            }
        }

        BlocksHandler.primaryBlocksB(
                logicEditor,
                extraBlocks.isListUsed(1),
                extraBlocks.isListUsed(2),
                extraBlocks.isListUsed(3)
        );
    }

    public void setBlock(int paletteId, int paletteColor) {
        // Remove previous palette's blocks
        logicEditor.paletteBlock.clearAll();

        if (eventName.equals("Import")) {
            if (paletteId == 3) {
                logicEditor.createPaletteBlock(" ", "addSourceDirectly");
            } else {
                logicEditor.addPaletteCategory(Helper.getResString(R.string.extra_menu_import_hint), getTitleBgColor());
                logicEditor.createPaletteBlock(" ", "customImport");
                logicEditor.createPaletteBlock(" ", "customImport2");
            }
            return;
        }

        switch (paletteId) {
            case -1:
                String filePath = FileUtil.getExternalStorageDir().concat("/.sketchware/data/").concat(sc_id.concat("/files/resource/values/strings.xml"));
                ArrayList<HashMap<String, Object>> StringsListMap = new ArrayList<>();
                StringsEditorManager stringsEditorManager = new StringsEditorManager();
                stringsEditorManager.convertXmlStringsToListMap(FileUtil.readFileIfExist(filePath), StringsListMap);

                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_open_resources_editor), "openResourcesEditor");

                logicEditor.createPaletteBlock("s", "getResString");
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_saved_res_strings), getTitleBgColor());
                if (!stringsEditorManager.isXmlStringsExist(StringsListMap, "app_name")) {
                    logicEditor.createPaletteBlockWithSpec("app_name", "s", "getResStr").setTag("S98ZCSapp_name");
                }

                for (int i = 0; i < StringsListMap.size(); i++) {
                    String key = StringsListMap.get(i).get("key").toString();
                    logicEditor.createPaletteBlockWithSpec(key, "s", "getResStr").setTag("S98ZCS" + key);
                }
                return;
            case 0:
                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_add_variable), "variableAdd");
                logicEditor.addPaletteLabelWithListener(Helper.getResString(R.string.logic_editor_panel_button_add_custom_variable), "variableAddNew", clickListener);
                logicEditor.addPaletteLabelWithListener(Helper.getResString(R.string.logic_editor_panel_button_remove_variable), "variableRemove", clickListener);
                variables();
                return;

            case 1:
                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_add_list), "listAdd");
                logicEditor.addPaletteLabelWithListener(Helper.getResString(R.string.logic_editor_panel_button_add_custom_list), "listAddCustom", clickListener);
                logicEditor.addPaletteLabelWithListener(Helper.getResString(R.string.logic_editor_panel_button_remove_list), "listRemove", clickListener);
                list();
                return;

            case 2:
                BlocksHandler.primaryBlocksC(logicEditor);
                return;

            case 3:
                BlocksHandler.primaryBlocksD(logicEditor);
                return;

            case 4:
                logicEditor.createPaletteBlock("d", "mathGetDip");
                logicEditor.createPaletteBlock("d", "mathGetDisplayWidth");
                logicEditor.createPaletteBlock("d", "mathGetDisplayHeight");
                logicEditor.createPaletteBlock("d", "mathPi");
                logicEditor.createPaletteBlock("d", "mathE");
                logicEditor.createPaletteBlock("d", "mathPow");
                logicEditor.createPaletteBlock("d", "mathMin");
                logicEditor.createPaletteBlock("d", "mathMax");
                logicEditor.createPaletteBlock("d", "mathSqrt");
                logicEditor.createPaletteBlock("d", "mathAbs");
                logicEditor.createPaletteBlock("d", "mathRound");
                logicEditor.createPaletteBlock("d", "mathCeil");
                logicEditor.createPaletteBlock("d", "mathFloor");
                logicEditor.createPaletteBlock("d", "mathSin");
                logicEditor.createPaletteBlock("d", "mathCos");
                logicEditor.createPaletteBlock("d", "mathTan");
                logicEditor.createPaletteBlock("d", "mathAsin");
                logicEditor.createPaletteBlock("d", "mathAcos");
                logicEditor.createPaletteBlock("d", "mathAtan");
                logicEditor.createPaletteBlock("d", "mathExp");
                logicEditor.createPaletteBlock("d", "mathLog");
                logicEditor.createPaletteBlock("d", "mathLog10");
                logicEditor.createPaletteBlock("d", "mathToRadian");
                logicEditor.createPaletteBlock("d", "mathToDegree");
                return;

            case 5:
                extraBlocks.fileBlocks();
                logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_fileutil_blocks), getTitleBgColor());
                if (!frc.getAssetsFile().isEmpty()) {
                    logicEditor.createPaletteBlock(" ", "getAssetFile");
                    logicEditor.createPaletteBlock("s", "copyAssetFile");
                }
                logicEditor.createPaletteBlock("s", "fileutilread");
                logicEditor.createPaletteBlock(" ", "fileutilwrite");
                logicEditor.createPaletteBlock(" ", "fileutilcopy");
                logicEditor.createPaletteBlock(" ", "fileutilcopydir");
                logicEditor.createPaletteBlock(" ", "fileutilmove");
                logicEditor.createPaletteBlock(" ", "fileutildelete");
                logicEditor.createPaletteBlock(" ", "renameFile");
                logicEditor.createPaletteBlock("b", "fileutilisexist");
                logicEditor.createPaletteBlock(" ", "fileutilmakedir");
                logicEditor.createPaletteBlock(" ", "fileutillistdir");
                logicEditor.createPaletteBlock("b", "fileutilisdir");
                logicEditor.createPaletteBlock("b", "fileutilisfile");
                logicEditor.createPaletteBlock("d", "fileutillength");
                logicEditor.createPaletteBlock("b", "fileutilStartsWith");
                logicEditor.createPaletteBlock("b", "fileutilEndsWith");
                logicEditor.createPaletteBlock("s", "fileutilGetLastSegmentPath");
                logicEditor.createPaletteBlock("s", "getExternalStorageDir");
                logicEditor.createPaletteBlock("s", "getPackageDataDir");
                logicEditor.createPaletteBlock("s", "getPublicDir");
                logicEditor.createPaletteBlock(" ", "resizeBitmapFileRetainRatio");
                logicEditor.createPaletteBlock(" ", "resizeBitmapFileToSquare");
                logicEditor.createPaletteBlock(" ", "resizeBitmapFileToCircle");
                logicEditor.createPaletteBlock(" ", "resizeBitmapFileWithRoundedBorder");
                logicEditor.createPaletteBlock(" ", "cropBitmapFileFromCenter");
                logicEditor.createPaletteBlock(" ", "rotateBitmapFile");
                logicEditor.createPaletteBlock(" ", "scaleBitmapFile");
                logicEditor.createPaletteBlock(" ", "skewBitmapFile");
                logicEditor.createPaletteBlock(" ", "setBitmapFileColorFilter");
                logicEditor.createPaletteBlock(" ", "setBitmapFileBrightness");
                logicEditor.createPaletteBlock(" ", "setBitmapFileContrast");
                logicEditor.createPaletteBlock("d", "getJpegRotate");
                return;

            case 6:
                logicEditor.createPaletteBlock(" ", "setEnable");
                logicEditor.createPaletteBlock("b", "getEnable");
                logicEditor.createPaletteBlock(" ", "setVisible");
                logicEditor.createPaletteBlock("b", "checkViewVisibility");
                logicEditor.createPaletteBlock(" ", "setElevation");
                logicEditor.createPaletteBlock(" ", "setRotate");
                logicEditor.createPaletteBlock("d", "getRotate");
                logicEditor.createPaletteBlock(" ", "setAlpha");
                logicEditor.createPaletteBlock("d", "getAlpha");
                logicEditor.createPaletteBlock(" ", "setTranslationX");
                logicEditor.createPaletteBlock("d", "getTranslationX");
                logicEditor.createPaletteBlock(" ", "setTranslationY");
                logicEditor.createPaletteBlock("d", "getTranslationY");
                logicEditor.createPaletteBlock(" ", "setScaleX");
                logicEditor.createPaletteBlock("d", "getScaleX");
                logicEditor.createPaletteBlock(" ", "setScaleY");
                logicEditor.createPaletteBlock("d", "getScaleY");
                logicEditor.createPaletteBlock("d", "getLocationX");
                logicEditor.createPaletteBlock("d", "getLocationY");
                logicEditor.createPaletteBlock("d", "getHeight");
                logicEditor.createPaletteBlock("d", "getWidth");
                logicEditor.createPaletteBlock(" ", "requestFocus");
                logicEditor.createPaletteBlock(" ", "removeView");
                logicEditor.createPaletteBlock(" ", "removeViews");
                logicEditor.createPaletteBlock(" ", "addView");
                logicEditor.createPaletteBlock("v", "viewGetChildAt");
                logicEditor.createPaletteBlock(" ", "addViews");
                logicEditor.createPaletteBlock(" ", "setGravity");
                logicEditor.createPaletteBlock(" ", "setColorFilterView");
                logicEditor.createPaletteBlock(" ", "setBgColor");
                logicEditor.createPaletteBlock(" ", "setBgResource");
                logicEditor.createPaletteBlock(" ", "setBgDrawable");
                logicEditor.createPaletteBlock(" ", "setStrokeView");
                logicEditor.createPaletteBlock(" ", "setCornerRadiusView");
                logicEditor.createPaletteBlock(" ", "setGradientBackground");
                logicEditor.createPaletteBlock(" ", "setRadiusAndStrokeView");
            {
                boolean editTextUsed = isWidgetUsed("EditText")
                        || extraBlocks.isCustomVarUsed("EditText");
                boolean textViewUsed = isWidgetUsed("TextView")
                        || extraBlocks.isCustomVarUsed("TextView") || editTextUsed;
                boolean compoundButtonUsed = isWidgetUsed("CompoundButton")
                        || extraBlocks.isCustomVarUsed("CompoundButton")
                        || extraBlocks.isCustomVarUsed("CheckBox")
                        || extraBlocks.isCustomVarUsed("RadioButton")
                        || extraBlocks.isCustomVarUsed("Switch")
                        || extraBlocks.isCustomVarUsed("ToggleButton");
                boolean autoCompleteTextViewUsed = isWidgetUsed("AutoCompleteTextView")
                        || extraBlocks.isCustomVarUsed("AutoCompleteTextView");
                boolean multiAutoCompleteTextViewUsed = isWidgetUsed("MultiAutoCompleteTextView")
                        || extraBlocks.isCustomVarUsed("MultiAutoCompleteTextView");
                boolean imageViewUsed = isWidgetUsed("ImageView") || isWidgetUsed("CircleImageView")
                        || extraBlocks.isCustomVarUsed("ImageView");
                boolean ratingBarUsed = isWidgetUsed("RatingBar")
                        || extraBlocks.isCustomVarUsed("RatingBar");
                boolean seekBarUsed = isWidgetUsed("SeekBar")
                        || extraBlocks.isCustomVarUsed("SeekBar");
                boolean progressBarUsed = isWidgetUsed("ProgressBar")
                        || extraBlocks.isCustomVarUsed("ProgressBar");
                boolean videoViewUsed = isWidgetUsed("VideoView")
                        || extraBlocks.isCustomVarUsed("VideoView");
                boolean webViewUsed = isWidgetUsed("WebView")
                        || extraBlocks.isCustomVarUsed("WebView");

                if (textViewUsed || compoundButtonUsed || autoCompleteTextViewUsed
                        || multiAutoCompleteTextViewUsed || imageViewUsed || ratingBarUsed
                        || seekBarUsed || progressBarUsed || videoViewUsed || webViewUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_widgets), getTitleBgColor());

                    if (textViewUsed) {
                        logicEditor.createPaletteBlock(" ", "setText");
                        logicEditor.createPaletteBlock("s", "getText");
                        logicEditor.createPaletteBlock(" ", "setTypeface");
                        logicEditor.createPaletteBlock(" ", "setTextColor");
                        logicEditor.createPaletteBlock(" ", "setTextSize");
                    }

                    if (editTextUsed) {
                        logicEditor.createPaletteBlock(" ", "setHint");
                        logicEditor.createPaletteBlock(" ", "setHintTextColor");
                        logicEditor.createPaletteBlock(" ", "EditTextdiableSuggestion");
                        logicEditor.createPaletteBlock(" ", "EditTextLines");
                        logicEditor.createPaletteBlock(" ", "EditTextSingleLine");
                        logicEditor.createPaletteBlock(" ", "EditTextShowError");
                        logicEditor.createPaletteBlock(" ", "EditTextSelectAll");
                        logicEditor.createPaletteBlock(" ", "EditTextSetSelection");
                        logicEditor.createPaletteBlock(" ", "EditTextSetMaxLines");
                        logicEditor.createPaletteBlock("d", "EdittextGetselectionStart");
                        logicEditor.createPaletteBlock("d", "EdittextGetselectionEnd");
                    }

                    if (compoundButtonUsed) {
                        logicEditor.createPaletteBlock(" ", "setChecked");
                        logicEditor.createPaletteBlock("b", "getChecked");
                    }

                    if (autoCompleteTextViewUsed) {
                        logicEditor.createPaletteBlock(" ", "autoComSetData");
                    }

                    if (multiAutoCompleteTextViewUsed) {
                        logicEditor.createPaletteBlock(" ", "multiAutoComSetData");
                        logicEditor.createPaletteBlock(" ", "setThreshold");
                        logicEditor.createPaletteBlock(" ", "setTokenizer");
                    }

                    if (imageViewUsed) {
                        logicEditor.createPaletteBlock(" ", "setImage");
                        logicEditor.createPaletteBlock(" ", "setImageCustomRes");
                        logicEditor.createPaletteBlock(" ", "setImageIdentifier");
                        logicEditor.createPaletteBlock(" ", "setImageFilePath");
                        logicEditor.createPaletteBlock(" ", "setImageUrl");
                        logicEditor.createPaletteBlock(" ", "setColorFilter");
                    }

                    if (ratingBarUsed) {
                        logicEditor.createPaletteBlock("d", "getRating");
                        logicEditor.createPaletteBlock(" ", "setRating");
                        logicEditor.createPaletteBlock(" ", "setNumStars");
                        logicEditor.createPaletteBlock(" ", "setStepSize");
                    }

                    if (seekBarUsed) {
                        logicEditor.createPaletteBlock(" ", "seekBarSetProgress");
                        logicEditor.createPaletteBlock("d", "seekBarGetProgress");
                        logicEditor.createPaletteBlock(" ", "seekBarSetMax");
                        logicEditor.createPaletteBlock("d", "seekBarGetMax");
                    }

                    if (progressBarUsed) {
                        logicEditor.createPaletteBlock(" ", "progressBarSetIndeterminate");
                    }

                    if (videoViewUsed) {
                        logicEditor.createPaletteBlock(" ", "videoviewSetVideoUri");
                        logicEditor.createPaletteBlock(" ", "videoviewStart");
                        logicEditor.createPaletteBlock(" ", "videoviewPause");
                        logicEditor.createPaletteBlock(" ", "videoviewStop");
                        logicEditor.createPaletteBlock("b", "videoviewIsPlaying");
                        logicEditor.createPaletteBlock("b", "videoviewCanPause");
                        logicEditor.createPaletteBlock("b", "videoviewCanSeekForward");
                        logicEditor.createPaletteBlock("b", "videoviewCanSeekBackward");
                        logicEditor.createPaletteBlock("d", "videoviewGetCurrentPosition");
                        logicEditor.createPaletteBlock("d", "videoviewGetDuration");
                    }

                    if (webViewUsed) {
                        logicEditor.createPaletteBlock(" ", "webViewLoadUrl");
                        logicEditor.createPaletteBlock("s", "webViewGetUrl");
                        logicEditor.createPaletteBlock("d", "webviewGetProgress");
                        logicEditor.createPaletteBlock(" ", "webViewSetCacheMode");
                        logicEditor.createPaletteBlock("b", "webViewCanGoBack");
                        logicEditor.createPaletteBlock("b", "webViewCanGoForward");
                        logicEditor.createPaletteBlock(" ", "webViewGoBack");
                        logicEditor.createPaletteBlock(" ", "webViewGoForward");
                        logicEditor.createPaletteBlock(" ", "webViewClearCache");
                        logicEditor.createPaletteBlock(" ", "webViewClearHistory");
                        logicEditor.createPaletteBlock(" ", "webViewStopLoading");
                        logicEditor.createPaletteBlock(" ", "webViewZoomIn");
                        logicEditor.createPaletteBlock(" ", "webViewZoomOut");
                    }
                }
            }
            {
                boolean inOnBindCustomView = eventName.equals("onBindCustomView");
                boolean spinnerUsed = isWidgetUsed("Spinner");
                boolean listViewUsed = isWidgetUsed("ListView");
                boolean recyclerViewUsed = isWidgetUsed("RecyclerView");
                boolean gridViewUsed = isWidgetUsed("GridView") || extraBlocks.isCustomVarUsed("GridView");
                boolean viewPagerUsed = isWidgetUsed("ViewPager");

                if (spinnerUsed || listViewUsed || recyclerViewUsed || gridViewUsed || viewPagerUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_list), getTitleBgColor());

                    if (spinnerUsed) {
                        logicEditor.createPaletteBlock(" ", "spnSetData");
                        logicEditor.createPaletteBlock(" ", "spnSetCustomViewData");
                        logicEditor.createPaletteBlock(" ", "spnRefresh");
                        logicEditor.createPaletteBlock(" ", "spnSetSelection");
                        logicEditor.createPaletteBlock("d", "spnGetSelection");
                    }

                    if (!inOnBindCustomView) {
                        if (listViewUsed) {
                            logicEditor.createPaletteBlock(" ", "listSetData");
                            logicEditor.createPaletteBlock(" ", "listSetCustomViewData");
                            logicEditor.createPaletteBlock(" ", "listRefresh");
                            logicEditor.createPaletteBlock(" ", "refreshingList");
                            logicEditor.createPaletteBlock(" ", "listSmoothScrollTo");
                            logicEditor.createPaletteBlock(" ", "listViewSetSelection");
                            logicEditor.createPaletteBlock(" ", "listSetTranscriptMode");
                            logicEditor.createPaletteBlock(" ", "listSetStackFromBottom");
                            logicEditor.createPaletteBlock(" ", "ListViewAddHeader");
                            logicEditor.createPaletteBlock(" ", "listViewRemoveHeader");
                            logicEditor.createPaletteBlock(" ", "ListViewAddFooter");
                            logicEditor.createPaletteBlock(" ", "listViewRemoveFooter");
                        }

                        if (recyclerViewUsed) {
                            logicEditor.createPaletteBlock(" ", "recyclerSetCustomViewData");
                            logicEditor.createPaletteBlock(" ", "recyclerSetLayoutManager");
                            logicEditor.createPaletteBlock(" ", "recyclerSetLayoutManagerHorizontal");
                            logicEditor.createPaletteBlock(" ", "recyclerSetHasFixedSize");
                            logicEditor.createPaletteBlock(" ", "recyclerSmoothScrollToPosition");
                            logicEditor.createPaletteBlock(" ", "recyclerScrollToPositionWithOffset");
                        }

                        if (gridViewUsed) {
                            logicEditor.createPaletteBlock(" ", "gridSetCustomViewData");
                            logicEditor.createPaletteBlock(" ", "gridSetNumColumns");
                            logicEditor.createPaletteBlock(" ", "gridSetColumnWidth");
                            logicEditor.createPaletteBlock(" ", "gridSetVerticalSpacing");
                            logicEditor.createPaletteBlock(" ", "gridSetHorizontalSpacing");
                            logicEditor.createPaletteBlock(" ", "gridSetStretchMode");
                        }

                        if (viewPagerUsed) {
                            logicEditor.createPaletteBlock(" ", "pagerSetCustomViewData");
                            logicEditor.createPaletteBlock(" ", "pagerSetFragmentAdapter");
                            logicEditor.createPaletteBlock("d", "pagerGetOffscreenPageLimit");
                            logicEditor.createPaletteBlock(" ", "pagerSetOffscreenPageLimit");
                            logicEditor.createPaletteBlock("d", "pagerGetCurrentItem");
                            logicEditor.createPaletteBlock(" ", "pagerSetCurrentItem");
                            logicEditor.createPaletteBlock(" ", "ViewPagerNotifyOnDtatChange");
                        }
                    } else {
                        logicEditor.createPaletteBlock(" ", "setRecyclerViewLayoutParams");
                    }
                }
            }
            {
                boolean drawerUsed = projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER);
                boolean fabUsed = projectFile.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB);
                boolean bottomNavigationViewUsed = isWidgetUsed("BottomNavigationView");
                boolean swipeRefreshLayoutUsed = isWidgetUsed("SwipeRefreshLayout");
                boolean cardViewUsed = isWidgetUsed("CardView");
                boolean tabLayoutUsed = isWidgetUsed("TabLayout");
                boolean textInputLayoutUsed = isWidgetUsed("TextInputLayout") || extraBlocks.isCustomVarUsed("TextInputLayout");

                if (drawerUsed || fabUsed || bottomNavigationViewUsed || swipeRefreshLayoutUsed || cardViewUsed || tabLayoutUsed || textInputLayoutUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_androidx_components), getTitleBgColor());

                    if (drawerUsed) {
                        logicEditor.createPaletteBlock("b", "isDrawerOpen");
                        logicEditor.createPaletteBlock(" ", "openDrawer");
                        logicEditor.createPaletteBlock(" ", "closeDrawer");
                    }

                    if (fabUsed) {
                        logicEditor.createPaletteBlock(" ", "fabIcon");
                        logicEditor.createPaletteBlock(" ", "fabSize");
                        logicEditor.createPaletteBlock(" ", "fabVisibility");
                    }

                    if (bottomNavigationViewUsed) {
                        logicEditor.createPaletteBlock(" ", "bottomMenuAddItem");
                    }

                    if (swipeRefreshLayoutUsed) {
                        logicEditor.createPaletteBlock("c", "onSwipeRefreshLayout");
                        logicEditor.createPaletteBlock(" ", "setRefreshing");
                    }

                    if (cardViewUsed) {
                        logicEditor.createPaletteBlock(" ", "setCardBackgroundColor");
                        logicEditor.createPaletteBlock(" ", "setCardRadius");
                        logicEditor.createPaletteBlock(" ", "setCardElevation");
                        logicEditor.createPaletteBlock(" ", "setPreventCornerOverlap");
                        logicEditor.createPaletteBlock(" ", "setUseCompatPadding");
                    }

                    if (tabLayoutUsed) {
                        logicEditor.createPaletteBlock(" ", "addTab");
                        logicEditor.createPaletteBlock(" ", "setupWithViewPager");
                        logicEditor.createPaletteBlock(" ", "setInlineLabel");
                        logicEditor.createPaletteBlock(" ", "setTabTextColors");
                        logicEditor.createPaletteBlock(" ", "setTabRippleColor");
                        logicEditor.createPaletteBlock(" ", "setSelectedTabIndicatorColor");
                        logicEditor.createPaletteBlock(" ", "setSelectedTabIndicatorHeight");
                    }

                    if (textInputLayoutUsed) {
                        logicEditor.createPaletteBlock(" ", "tilSetBoxBgColor");
                        logicEditor.createPaletteBlock(" ", "tilSetBoxStrokeColor");
                        logicEditor.createPaletteBlock(" ", "tilSetBoxBgMode");
                        logicEditor.createPaletteBlock(" ", "tilSetBoxCornerRadii");
                        logicEditor.createPaletteBlock(" ", "tilSetError");
                        logicEditor.createPaletteBlock(" ", "tilSetErrorEnabled");
                        logicEditor.createPaletteBlock(" ", "tilSetCounterEnabled");
                        logicEditor.createPaletteBlock(" ", "tilSetCounterMaxLength");
                        logicEditor.createPaletteBlock("d", "tilGetCounterMaxLength");
                    }
                }
            }
            {
                boolean waveSideBarUsed = isWidgetUsed("WaveSideBar");
                boolean badgeViewUsed = isWidgetUsed("BadgeView");
                boolean bubbleLayoutUsed = isWidgetUsed("BubbleLayout");
                boolean patternLockViewUsed = isWidgetUsed("PatternLockView");
                boolean codeViewUsed = isWidgetUsed("CodeView");
                boolean lottieAnimationViewUsed = isWidgetUsed("LottieAnimationView");
                boolean otpViewUsed = isWidgetUsed("OTPView");

                if (waveSideBarUsed || badgeViewUsed || bubbleLayoutUsed || patternLockViewUsed || codeViewUsed || lottieAnimationViewUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_library), getTitleBgColor());

                    if (otpViewUsed) {
                        logicEditor.createPaletteBlock(" ", "otpViewSetFieldCount");
                        logicEditor.createPaletteBlock(" ", "otpViewSetOTPText");
                        logicEditor.createPaletteBlock("s", "otpViewGetOTPText");
                        logicEditor.createPaletteBlock("c", "otpViewSetOTPListener");
                    }

                    if (waveSideBarUsed) {
                        logicEditor.createPaletteBlock(" ", "setCustomLetter");
                    }

                    if (badgeViewUsed) {
                        logicEditor.createPaletteBlock("d", "getBadgeCount");
                        logicEditor.createPaletteBlock(" ", "setBadgeNumber");
                        logicEditor.createPaletteBlock(" ", "setBadgeString");
                        logicEditor.createPaletteBlock(" ", "setBadgeBackground");
                        logicEditor.createPaletteBlock(" ", "setBadgeTextColor");
                        logicEditor.createPaletteBlock(" ", "setBadgeTextSize");
                    }

                    if (bubbleLayoutUsed) {
                        logicEditor.createPaletteBlock(" ", "setBubbleColor");
                        logicEditor.createPaletteBlock(" ", "setBubbleStrokeColor");
                        logicEditor.createPaletteBlock(" ", "setBubbleStrokeWidth");
                        logicEditor.createPaletteBlock(" ", "setBubbleCornerRadius");
                        logicEditor.createPaletteBlock(" ", "setBubbleArrowHeight");
                        logicEditor.createPaletteBlock(" ", "setBubbleArrowWidth");
                        logicEditor.createPaletteBlock(" ", "setBubbleArrowPosition");
                    }

                    if (patternLockViewUsed) {
                        logicEditor.createPaletteBlock("s", "patternToString");
                        logicEditor.createPaletteBlock("s", "patternToMD5");
                        logicEditor.createPaletteBlock("s", "patternToSha1");
                        logicEditor.createPaletteBlock(" ", "patternSetDotCount");
                        logicEditor.createPaletteBlock(" ", "patternSetNormalStateColor");
                        logicEditor.createPaletteBlock(" ", "patternSetCorrectStateColor");
                        logicEditor.createPaletteBlock(" ", "patternSetWrongStateColor");
                        logicEditor.createPaletteBlock(" ", "patternSetViewMode");
                        logicEditor.createPaletteBlock(" ", "patternLockClear");
                    }

                    if (codeViewUsed) {
                        logicEditor.createPaletteBlock(" ", "codeviewSetCode");
                        logicEditor.createPaletteBlock(" ", "codeviewSetLanguage");
                        logicEditor.createPaletteBlock(" ", "codeviewSetTheme");
                        logicEditor.createPaletteBlock(" ", "codeviewApply");
                    }

                    if (lottieAnimationViewUsed) {
                        logicEditor.createPaletteBlock(" ", "lottieSetAnimationFromAsset");
                        logicEditor.createPaletteBlock(" ", "lottieSetAnimationFromJson");
                        logicEditor.createPaletteBlock(" ", "lottieSetAnimationFromUrl");
                        logicEditor.createPaletteBlock(" ", "lottieSetRepeatCount");
                        logicEditor.createPaletteBlock(" ", "lottieSetSpeed");
                    }
                }
            }
            {
                boolean signInButtonUsed = isWidgetUsed("SignInButton");
                boolean youtubePlayerViewUsed = isWidgetUsed("YoutubePlayerView");
                boolean adMobUsed = "Y".equals(ProjectDataManager.getLibraryManager(sc_id).getAdmob().useYn);
                boolean mapViewUsed = isWidgetUsed("MapView");

                if (signInButtonUsed || youtubePlayerViewUsed || adMobUsed || mapViewUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_google), getTitleBgColor());

                    if (signInButtonUsed) {
                        logicEditor.createPaletteBlock(" ", "signInButtonSetColorScheme");
                        logicEditor.createPaletteBlock(" ", "signInButtonSetSize");
                    }

                    if (youtubePlayerViewUsed) {
                        logicEditor.createPaletteBlock(" ", "YTPVLifecycle");
                        logicEditor.createPaletteBlock("c", "YTPVSetListener");
                    }

                    if (adMobUsed) {
                        logicEditor.createPaletteBlock(" ", "bannerAdViewLoadAd");
                    }

                    if (mapViewUsed) {
                        logicEditor.createPaletteBlock(" ", "mapViewSetMapType");
                        logicEditor.createPaletteBlock(" ", "mapViewMoveCamera");
                        logicEditor.createPaletteBlock(" ", "mapViewZoomTo");
                        logicEditor.createPaletteBlock(" ", "mapViewZoomIn");
                        logicEditor.createPaletteBlock(" ", "mapViewZoomOut");
                        logicEditor.createPaletteBlock(" ", "mapViewAddMarker");
                        logicEditor.createPaletteBlock(" ", "mapViewSetMarkerInfo");
                        logicEditor.createPaletteBlock(" ", "mapViewSetMarkerPosition");
                        logicEditor.createPaletteBlock(" ", "mapViewSetMarkerColor");
                        logicEditor.createPaletteBlock(" ", "mapViewSetMarkerIcon");
                        logicEditor.createPaletteBlock(" ", "mapViewSetMarkerVisible");
                    }
                }
            }
            {
                boolean timePickerUsed = isWidgetUsed("TimePicker");
                boolean calendarViewUsed = isWidgetUsed("CalendarView");

                if (timePickerUsed || calendarViewUsed) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_date_time), getTitleBgColor());

                    if (timePickerUsed) {
                        logicEditor.createPaletteBlock(" ", "timepickerSetHour");
                        logicEditor.createPaletteBlock(" ", "timepickerSetMinute");
                        logicEditor.createPaletteBlock(" ", "timepickerSetCurrentHour");
                        logicEditor.createPaletteBlock(" ", "timepickerSetCurrentMinute");
                        logicEditor.createPaletteBlock(" ", "timepickerSetIs24Hour");
                    }

                    if (calendarViewUsed) {
                        logicEditor.createPaletteBlock(" ", "calendarViewSetDate");
                        logicEditor.createPaletteBlock(" ", "calendarViewSetMinDate");
                        logicEditor.createPaletteBlock(" ", "calnedarViewSetMaxDate");
                    }
                }
            }
            logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_function), getTitleBgColor());
            logicEditor.createPaletteBlock(" ", "performClick");
            logicEditor.createPaletteBlock("c", "viewOnClick");
            logicEditor.createPaletteBlock("c", "viewOnLongClick");
            logicEditor.createPaletteBlock("c", "viewOnTouch");
            logicEditor.createPaletteBlock("c", "showSnackbar");
            return;

            case 7:
                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_add_component), "componentAdd");
                logicEditor.createPaletteBlock(" ", "changeStatebarColour");
                logicEditor.createPaletteBlock(" ", "LightStatusBar");
                logicEditor.createPaletteBlock(" ", "showKeyboard");
                logicEditor.createPaletteBlock(" ", "hideKeyboard");
                logicEditor.createPaletteBlock(" ", "doToast");
                logicEditor.createPaletteBlock(" ", "copyToClipboard");
                logicEditor.createPaletteBlock(" ", "setTitle");
                logicEditor.createPaletteBlock("b", "intentHasExtra");
                logicEditor.createPaletteBlock("s", "intentGetString");
                logicEditor.createPaletteBlock("f", "finishActivity");
                logicEditor.createPaletteBlock("f", "finishAffinity");
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_INTENT)
                        || extraBlocks.isCustomVarUsed("Intent")) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_intent), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "intentSetAction");
                    logicEditor.createPaletteBlock(" ", "intentSetData");
                    logicEditor.createPaletteBlock(" ", "intentSetType");
                    logicEditor.createPaletteBlock(" ", "intentSetScreen");
                    logicEditor.createPaletteBlock(" ", "launchApp");
                    logicEditor.createPaletteBlock(" ", "intentPutExtra");
                    logicEditor.createPaletteBlock(" ", "intentRemoveExtra");
                    logicEditor.createPaletteBlock(" ", "intentSetFlags");
                    logicEditor.createPaletteBlock(" ", "startActivity");
                    logicEditor.createPaletteBlock(" ", "startActivityWithChooser");
                }
                if (!frc.getBroadcastFile().isEmpty()) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_broadcast), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "sendBroadcast");
                }
                if (!frc.getServiceFile().isEmpty()) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_service), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "startService");
                    logicEditor.createPaletteBlock(" ", "stopService");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SHAREDPREF)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_shared_preferences), getTitleBgColor());
                    logicEditor.createPaletteBlock("b", "fileContainsData");
                    logicEditor.createPaletteBlock("s", "fileGetData");
                    logicEditor.createPaletteBlock(" ", "fileSetData");
                    logicEditor.createPaletteBlock(" ", "fileRemoveData");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_date_picker_dialog), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "datePickerDialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_time_picker_dialog), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "timePickerDialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_CALENDAR)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_calendar), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "calendarGetNow");
                    logicEditor.createPaletteBlock(" ", "calendarAdd");
                    logicEditor.createPaletteBlock(" ", "calendarSet");
                    logicEditor.createPaletteBlock("s", "calendarFormat");
                    logicEditor.createPaletteBlock("d", "calendarDiff");
                    logicEditor.createPaletteBlock("d", "calendarGetTime");
                    logicEditor.createPaletteBlock(" ", "calendarSetTime");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_VIBRATOR)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_vibrator), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "vibratorAction");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TIMERTASK)
                        || extraBlocks.isCustomVarUsed("Timer")) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_timer), getTitleBgColor());
                    logicEditor.createPaletteBlock("c", "timerAfter");
                    logicEditor.createPaletteBlock("c", "timerEvery");
                    logicEditor.createPaletteBlock(" ", "timerCancel");
                }
                if (extraBlocks.isComponentUsed(36)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_async_task), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "AsyncTaskExecute");
                    logicEditor.createPaletteBlock(" ", "AsyncTaskPublishProgress");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_DIALOG)
                        || extraBlocks.isCustomVarUsed("Dialog")) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_dialog), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "dialogSetTitle");
                    logicEditor.createPaletteBlock(" ", "Dialog SetIcon");
                    logicEditor.createPaletteBlock(" ", "dialogSetMessage");
                    logicEditor.createPaletteBlock("c", "dialogOkButton");
                    logicEditor.createPaletteBlock("c", "dialogCancelButton");
                    logicEditor.createPaletteBlock("c", "dialogNeutralButton");
                    logicEditor.createPaletteBlock(" ", "dialogShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_media_player), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "mediaplayerCreate");
                    logicEditor.createPaletteBlock(" ", "mediaplayerStart");
                    logicEditor.createPaletteBlock(" ", "mediaplayerPause");
                    logicEditor.createPaletteBlock(" ", "mediaplayerSeek");
                    logicEditor.createPaletteBlock("d", "mediaplayerGetCurrent");
                    logicEditor.createPaletteBlock("d", "mediaplayerGetDuration");
                    logicEditor.createPaletteBlock("b", "mediaplayerIsPlaying");
                    logicEditor.createPaletteBlock(" ", "mediaplayerSetLooping");
                    logicEditor.createPaletteBlock("b", "mediaplayerIsLooping");
                    logicEditor.createPaletteBlock(" ", "mediaplayerReset");
                    logicEditor.createPaletteBlock(" ", "mediaplayerRelease");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SOUNDPOOL)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_sound_pool), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "soundpoolCreate");
                    logicEditor.createPaletteBlock("d", "soundpoolLoad");
                    logicEditor.createPaletteBlock("d", "soundpoolStreamPlay");
                    logicEditor.createPaletteBlock(" ", "soundpoolStreamStop");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_object_animator), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetTarget");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetProperty");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetValue");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetFromTo");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetDuration");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetRepeatMode");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetRepeatCount");
                    logicEditor.createPaletteBlock(" ", "objectanimatorSetInterpolator");
                    logicEditor.createPaletteBlock(" ", "objectanimatorStart");
                    logicEditor.createPaletteBlock(" ", "objectanimatorCancel");
                    logicEditor.createPaletteBlock("b", "objectanimatorIsRunning");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_firebase), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "firebaseAdd");
                    logicEditor.createPaletteBlock(" ", "firebasePush");
                    logicEditor.createPaletteBlock("s", "firebaseGetPushKey");
                    logicEditor.createPaletteBlock(" ", "firebaseDelete");
                    logicEditor.createPaletteBlock("c", "firebaseGetChildren");
                    logicEditor.createPaletteBlock(" ", "firebaseStartListen");
                    logicEditor.createPaletteBlock(" ", "firebaseStopListen");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_firebase_auth), getTitleBgColor());
                    logicEditor.createPaletteBlock("b", "firebaseauthIsLoggedIn");
                    logicEditor.createPaletteBlock("s", "firebaseauthGetCurrentUser");
                    logicEditor.createPaletteBlock("s", "firebaseauthGetUid");
                    logicEditor.createPaletteBlock(" ", "firebaseauthCreateUser");
                    logicEditor.createPaletteBlock(" ", "firebaseauthSignInUser");
                    logicEditor.createPaletteBlock(" ", "firebaseauthSignInAnonymously");
                    logicEditor.createPaletteBlock(" ", "firebaseauthResetPassword");
                    logicEditor.createPaletteBlock(" ", "firebaseauthSignOutUser");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_GYROSCOPE)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_gyroscope), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "gyroscopeStartListen");
                    logicEditor.createPaletteBlock(" ", "gyroscopeStopListen");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_admob_interstitial), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "interstitialAdLoad");
                    logicEditor.createPaletteBlock(" ", "interstitialAdShow");
                    logicEditor.createPaletteBlock("b", "interstitialAdIsLoaded");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_rewarded_video_ad), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "rewardedVideoAdLoad");
                    logicEditor.createPaletteBlock(" ", "rewardedVideoAdShow");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_firebase_storage), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "firebasestorageUploadFile");
                    logicEditor.createPaletteBlock(" ", "firebasestorageDownloadFile");
                    logicEditor.createPaletteBlock(" ", "firebasestorageDelete");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_CAMERA)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_camera), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "camerastarttakepicture");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_FILE_PICKER)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_file_picker), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "filepickerstartpickfiles");
                    logicEditor.createPaletteBlock(" ", "imageCrop");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_request_network), getTitleBgColor());
                    logicEditor.createPaletteBlock("b", "isConnected");
                    logicEditor.createPaletteBlock(" ", "requestnetworkSetParams");
                    logicEditor.createPaletteBlock(" ", "requestnetworkSetHeaders");
                    logicEditor.createPaletteBlock(" ", "requestnetworkStartRequestNetwork");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_text_to_speech), getTitleBgColor());
                    logicEditor.createPaletteBlock("b", "textToSpeechIsSpeaking");
                    logicEditor.createPaletteBlock(" ", "textToSpeechSetPitch");
                    logicEditor.createPaletteBlock(" ", "textToSpeechSetSpeechRate");
                    logicEditor.createPaletteBlock(" ", "textToSpeechSpeak");
                    logicEditor.createPaletteBlock(" ", "textToSpeechStop");
                    logicEditor.createPaletteBlock(" ", "textToSpeechShutdown");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_speech_to_text), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "speechToTextStartListening");
                    logicEditor.createPaletteBlock(" ", "speechToTextStopListening");
                    logicEditor.createPaletteBlock(" ", "speechToTextShutdown");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_bluetooth), getTitleBgColor());
                    logicEditor.createPaletteBlock("b", "bluetoothConnectIsBluetoothEnabled");
                    logicEditor.createPaletteBlock("b", "bluetoothConnectIsBluetoothActivated");
                    logicEditor.createPaletteBlock("s", "bluetoothConnectGetRandomUuid");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectReadyConnection");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectReadyConnectionToUuid");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectStartConnection");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectStartConnectionToUuid");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectStopConnection");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectSendData");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectActivateBluetooth");
                    logicEditor.createPaletteBlock(" ", "bluetoothConnectGetPairedDevices");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_location_manager), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "locationManagerRequestLocationUpdates");
                    logicEditor.createPaletteBlock(" ", "locationManagerRemoveUpdates");
                }
                if (extraBlocks.isComponentUsed(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG)
                        || extraBlocks.isCustomVarUsed("ProgressDialog")
                        || eventName.equals("onPreExecute") || eventName.equals("onProgressUpdate")
                        || eventName.equals("onPostExecute")) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_progress_dialog), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "progressdialogCreate");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetTitle");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetMessage");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetMax");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetProgress");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetCancelable");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetCanceledOutside");
                    logicEditor.createPaletteBlock(" ", "progressdialogSetStyle");
                    logicEditor.createPaletteBlock(" ", "progressdialogShow");
                    logicEditor.createPaletteBlock(" ", "progressdialogDismiss");
                    return;
                }
                return;

            case 8:
                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_create_block), "blockAdd");
                logicEditor.addPaletteLabel(Helper.getResString(R.string.logic_editor_panel_button_import_collection), "blockImport");
                if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)) {
                    logicEditor.createPaletteBlock(" ", "customToast");
                    logicEditor.createPaletteBlock(" ", "customToastWithIcon");
                }
                moreBlocks();
                if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_SHOW_BUILT_IN_BLOCKS)) {
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_command_blocks), getTitleBgColor());
                    logicEditor.createPaletteBlock("c", "CommandBlockJava");
                    logicEditor.addDeprecatedBlock(Helper.getResString(R.string.logic_editor_deprecated_xml_command), "c", "CommandBlockXML");
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_permission_command_blocks), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "addPermission");
                    logicEditor.createPaletteBlock(" ", "removePermission");
                    logicEditor.addPaletteCategory(Helper.getResString(R.string.logic_editor_category_other_command_blocks), getTitleBgColor());
                    logicEditor.createPaletteBlock(" ", "addCustomVariable");
                    logicEditor.createPaletteBlock(" ", "addInitializer");
                    return;
                }
                return;

            default:
                int paletteIndex = -1, paletteBlocks = 0;
                ArrayList<HashMap<String, Object>> extraBlockData = ExtraBlockFile.getExtraBlockData();
                for (int i = 0, extraBlockDataSize = extraBlockData.size(); i < extraBlockDataSize; i++) {
                    HashMap<String, Object> map = extraBlockData.get(i);

                    Object palette = map.get("palette");
                    if (palette instanceof String paletteString) {

                        if (paletteString.equals(String.valueOf(paletteId))) {
                            if (paletteIndex == -1) paletteIndex = Integer.parseInt(paletteString);
                            paletteBlocks++;

                            Object type = map.get("type");
                            if (type instanceof String typeString) {

                                if (typeString.equals("h")) {
                                    Object spec = map.get("spec");
                                    if (spec instanceof String specString) {
                                        logicEditor.addPaletteCategory(specString, getTitleBgColor());
                                    } else {
                                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.extra_block_error_invalid_spec), paletteBlocks));
                                    }
                                } else {
                                    Object name = map.get("name");
                                    if (name instanceof String nameString) {

                                        Object typeName = map.get("typeName");
                                        if (typeName instanceof String typeNameString) {

                                            logicEditor.createPaletteBlockWithComponent("", typeString, typeNameString, nameString);
                                        } else {
                                            logicEditor.createPaletteBlockWithComponent("", typeString, "", nameString);
                                        }
                                    } else {
                                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.extra_block_error_invalid_block_name), paletteBlocks));
                                    }
                                }
                            } else {
                                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.extra_block_error_invalid_block_type), paletteBlocks));
                            }
                        }
                    } else {
                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.extra_block_error_invalid_block_palette), paletteBlocks));
                    }
                }
                break;
        }
    }

    private @ColorInt int getTitleBgColor() {
        return getColor(logicEditor, isDarkThemeEnabled(logicEditor) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse);
    }
}
