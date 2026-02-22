package pro.sketchware.menu;

import android.net.Uri;
import android.util.Pair;

import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.editor.LogicEditorActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import a.a.a.Ss;
import a.a.a.eC;
import a.a.a.ProjectDataManager;
import a.a.a.BlockConstants;
import a.a.a.SketchwarePaths;
import mod.agus.jcoderz.editor.manage.block.makeblock.BlockMenu;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.CustomVariableUtil;
import pro.sketchware.utility.FileUtil;

public class DefaultExtraMenuBean {

    private final LogicEditorActivity logicEditor;
    private final eC projectDataManager;
    private final String sc_id;

    public DefaultExtraMenuBean(LogicEditorActivity logicEditor) {
        this.logicEditor = logicEditor;
        sc_id = logicEditor.scId;
        projectDataManager = ProjectDataManager.getProjectDataManager(sc_id);
    }

    public static String getName(String menuName) {
        return switch (menuName) {
            case "image" -> Helper.getResString(R.string.menu_name_custom_image);
            case "til_box_mode" -> Helper.getResString(R.string.menu_name_box_mode);
            case "fabsize" -> Helper.getResString(R.string.menu_name_fab_size);
            case "fabvisible" -> Helper.getResString(R.string.menu_name_fab_visible);
            case "menuaction" -> Helper.getResString(R.string.menu_name_menu_action);
            case "porterduff" -> Helper.getResString(R.string.menu_name_porterduff_mode);
            case "transcriptmode" -> Helper.getResString(R.string.menu_name_transcript_mode);
            case "listscrollparam", "recyclerscrollparam", "pagerscrollparam" -> Helper.getResString(R.string.menu_name_scroll_param);
            case "gridstretchmode" -> Helper.getResString(R.string.menu_name_stretch_mode);
            case "gravity_v" -> Helper.getResString(R.string.menu_name_gravity_vertical);
            case "gravity_h" -> Helper.getResString(R.string.menu_name_gravity_horizontal);
            case "gravity_t" -> Helper.getResString(R.string.menu_name_gravity_toast);
            case "patternviewmode" -> Helper.getResString(R.string.menu_name_pattern_mode);
            case "styleprogress" -> Helper.getResString(R.string.menu_name_progress_style);
            case "cv_theme" -> Helper.getResString(R.string.menu_name_theme);
            case "cv_language" -> Helper.getResString(R.string.menu_name_language);
            case "import" -> Helper.getResString(R.string.common_word_import);
            default -> menuName;
        };
    }

    public Pair<String, ArrayList<String>> getMenu(Ss menu) {
        var javaName = logicEditor.M.getJavaName();
        var menuName = menu.getMenuName();
        ArrayList<String> menus = new ArrayList<>();
        String title;
        Pair<String, String[]> menuPair = BlockMenu.getMenu(menuName);
        title = menuPair.first;
        menus = new ArrayList<>(Arrays.asList(menuPair.second));
        for (String s : projectDataManager.e(javaName, 5)) {
            Matcher matcher2 = Pattern.compile("^(\\w+)[\\s]+(\\w+)").matcher(s);
            while (matcher2.find()) {
                if (menuName.equals(matcher2.group(1))) {
                    title = String.format(Helper.getResString(R.string.menu_select_variable_format), matcher2.group(1));
                    menus.add(matcher2.group(2));
                }
            }
        }
        for (String variable : projectDataManager.e(javaName, 6)) {
            String variableType = CustomVariableUtil.getVariableType(variable);
            String variableName = CustomVariableUtil.getVariableName(variable);
            if (menuName.equals(variableType)) {
                title = String.format(Helper.getResString(R.string.menu_select_variable_format), variableType);
                menus.add(variableName);
            }
        }
        for (ComponentBean componentBean : projectDataManager.e(javaName)) {
            if (componentBean.type > 36
                    && menuName.equals(ComponentBean.getComponentTypeName(componentBean.type))) {
                title = String.format(Helper.getResString(R.string.menu_select_component_format), ComponentBean.getComponentTypeName(componentBean.type));
                menus.add(componentBean.componentId);
            }
        }
        switch (menuName) {
            case "LayoutParam" -> {
                title = Helper.getResString(R.string.menu_select_layout_params);
                menus.addAll(Helper.createStringList("MATCH_PARENT", "WRAP_CONTENT"));
            }
            case "Command" -> {
                title = Helper.getResString(R.string.menu_select_command);
                menus.addAll(
                        Helper.createStringList(
                                "insert",
                                "add",
                                "replace",
                                "find-replace",
                                "find-replace-first",
                                "find-replace-all"));
            }
            // This is meant to be a built-in menu including the cases below, but Aldi implemented it as a file, which is why, in some cases, certain menus appear empty.
            //start
            case "menu", "layout", "anim", "drawable" -> {
                String path = getPath(sc_id, menuName);
                title = String.format(Helper.getResString(R.string.menu_select_format), menuName);
                if (menuName.equals("layout")) {
                    for (String name : ProjectDataManager.getFileManager(sc_id).e()) {
                        menus.add(name.substring(0, name.indexOf(".xml")));
                    }
                }
                for (String file : FileUtil.listFiles(path, ".xml")) {
                    menus.add(getFilename(file, ".xml"));
                }
            }
            case "image" -> {
                String path = getPath(sc_id, "drawable-xhdpi");
                title = Helper.getResString(R.string.menu_select_image);
                for (String drawable_xhdpi : FileUtil.listFiles(path, "")) {
                    if (drawable_xhdpi.contains(".png") || drawable_xhdpi.contains(".jpg")) {
                        menus.add(
                                getFilename(
                                        drawable_xhdpi,
                                        drawable_xhdpi.contains(".png") ? ".png" : ".jpg"));
                    }
                }
            }
            case "til_box_mode" -> {
                title = Helper.getResString(R.string.menu_select_box_mode);
                menus.addAll(Arrays.asList(BlockConstants.TIL_BOX_MODE));
            }
            case "fabsize" -> {
                title = Helper.getResString(R.string.menu_select_fab_size);
                menus.addAll(Arrays.asList(BlockConstants.FAB_SIZE));
            }
            case "fabvisible" -> {
                title = Helper.getResString(R.string.menu_select_fab_visibility);
                menus.addAll(Arrays.asList(BlockConstants.FAB_VISIBLE));
            }
            case "menuaction" -> {
                title = Helper.getResString(R.string.menu_select_menu_action);
                menus.addAll(Arrays.asList(BlockConstants.MENU_ACTION));
            }
            case "porterduff" -> {
                title = Helper.getResString(R.string.menu_select_porterduff_mode);
                menus.addAll(Arrays.asList(BlockConstants.PORTER_DUFF));
            }
            case "transcriptmode" -> {
                title = Helper.getResString(R.string.menu_select_transcript_mode);
                menus.addAll(Arrays.asList(BlockConstants.TRANSCRIPT_MODE));
            }
            // idk, but it seems this isn't used anywhere, yet it was included in the menu file.
            case "listscrollparam" -> {
                title = Helper.getResString(R.string.menu_select_scroll_param);
                menus.addAll(Arrays.asList(BlockConstants.LIST_SCROLL_STATES));
            }
            // same with listscrollparam
            case "recyclerscrollparam", "pagerscrollparam" -> {
                title = Helper.getResString(R.string.menu_select_scroll_param);
                menus.addAll(Arrays.asList(BlockConstants.RECYCLER_SCROLL_STATES));
            }
            case "gridstretchmode" -> {
                title = Helper.getResString(R.string.menu_select_stretch_mode);
                menus.addAll(Arrays.asList(BlockConstants.GRID_STRETCH_MODE));
            }
            case "gravity_v" -> {
                title = Helper.getResString(R.string.menu_select_gravity_vertical);
                menus.addAll(Arrays.asList(BlockConstants.GRAVITY_VERTICAL));
            }
            case "gravity_h" -> {
                title = Helper.getResString(R.string.menu_select_gravity_horizontal);
                menus.addAll(Arrays.asList(BlockConstants.GRAVITY_HORIZONTAL));
            }
            case "gravity_t" -> {
                title = Helper.getResString(R.string.menu_select_gravity_toast);
                menus.addAll(Arrays.asList(BlockConstants.GRAVITY_TOAST));
            }
            case "patternviewmode" -> {
                title = Helper.getResString(R.string.menu_select_patternview_mode);
                menus.addAll(Arrays.asList(BlockConstants.PATTERNVIEW_MODE));
            }
            case "styleprogress" -> {
                title = Helper.getResString(R.string.menu_select_progress_style);
                menus.addAll(Arrays.asList(BlockConstants.PROGRESS_STYLE));
            }
            case "cv_theme" -> {
                title = Helper.getResString(R.string.menu_select_theme);
                menus.addAll(Arrays.asList(BlockConstants.CODEVIEW_THEME));
            }
            case "cv_language" -> {
                title = Helper.getResString(R.string.menu_select_language);
                menus.addAll(Arrays.asList(BlockConstants.CODEVIEW_LANGUAGE));
            }
            case "import" -> {
                title = Helper.getResString(R.string.menu_select_language);
                menus.addAll(Arrays.asList(BlockConstants.IMPORT_CLASS_PATH));
            }
            //end
        }
        return new Pair<>(title, menus);
    }

    private String getPath(String sc_id, String name) {
        return SketchwarePaths.getDataPath(sc_id) + "/files/resource/" + name + "/";
    }

    private String getFilename(String filePath, String filenameExtensionToCutOff) {
        String lastPathSegment = Uri.parse(filePath).getLastPathSegment();
        return lastPathSegment.substring(0, lastPathSegment.indexOf(filenameExtensionToCutOff));
    }
}
