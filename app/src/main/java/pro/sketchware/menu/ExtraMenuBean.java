package pro.sketchware.menu;

import static android.text.TextUtils.isEmpty;
import static pro.sketchware.utility.SketchwareUtil.getDip;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.besome.sketch.beans.AdTestDeviceBean;
import com.besome.sketch.beans.AdUnitBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.core.FieldBlockView;
import pro.sketchware.core.ProjectDataStore;
import pro.sketchware.core.ProjectDataManager;
import pro.sketchware.core.BlockConstants;
import pro.sketchware.core.ViewUtil;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import dev.pranav.filepicker.SelectionMode;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.hilal.saif.asd.AsdDialog;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.utils.StringsEditorManager;
import pro.sketchware.lib.base.BaseTextWatcher;
import pro.sketchware.lib.highlighter.SimpleHighlighter;
import pro.sketchware.utility.CustomVariableUtil;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileResConfig;
import pro.sketchware.utility.FileUtil;

public class ExtraMenuBean {

    public static final int VARIABLE_TYPE_BOOLEAN = 0;
    public static final int VARIABLE_TYPE_NUMBER = 1;
    public static final int VARIABLE_TYPE_MAP = 3;
    public static final int VARIABLE_TYPE_STRING = 2;

    public static final int LIST_TYPE_NUMBER = 1;
    public static final int LIST_TYPE_MAP = 3;
    public static final int LIST_TYPE_STRING = 2;

    public static final String[] adSize = {"AUTO_HEIGHT", "BANNER", "FLUID", "FULL_BANNER", "FULL_WIDTH", "INVALID", "LARGE_BANNER", "LEADERBOARD", "MEDIUM_RECTANGLE", "SEARCH", "SMART_BANNER", "WIDE_SKYSCRAPER"};
    public static final String[] intentKey = {"EXTRA_ALLOW_MULTIPLE", "EXTRA_EMAIL", "EXTRA_INDEX", "EXTRA_INTENT", "EXTRA_PHONE_NUMBER", "EXTRA_STREAM", "EXTRA_SUBJECT", "EXTRA_TEXT", "EXTRA_TITLE"};
    public static final String[] pixelFormat = {"OPAQUE", "RGBA_1010102", "RGBA_8888", "RGBA_F16", "RGBX_8888", "RGB_565", "RGB_888", "TRANSLUCENT", "TRANSPARENT", "UNKNOWN"};
    public static final String[] patternFlags = {"CANON_EQ", "CASE_INSENSITIVE", "COMMENTS", "DOTALL", "LITERAL", "MULTILINE", "UNICODE_CASE", "UNIX_LINES"};
    public static final String[] permission = {"CAMERA", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "RECORD_AUDIO", "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS", "READ_PHONE_STATE", "CALL_PHONE", "READ_CALENDAR", "WRITE_CALENDAR", "BLUETOOTH", "BLUETOOTH_ADMIN"};

    private final String ASSETS_PATH = FileUtil.getExternalStorageDir() + "/.sketchware/data/%s/files/assets/";
    private final String NATIVE_PATH = FileUtil.getExternalStorageDir() + "/.sketchware/data/%s/files/native_libs/";
    private final DefaultExtraMenuBean defaultExtraMenu;
    private final FilePathUtil fpu;
    private final FileResConfig frc;
    private final LogicEditorActivity logicEditor;
    private final FilePickerOptions mOptions = new FilePickerOptions();
    private final ProjectDataStore projectDataManager;
    private final String sc_id;
    private final String javaName;

    public ExtraMenuBean(LogicEditorActivity logicA) {
        logicEditor = logicA;
        sc_id = logicA.scId;
        fpu = new FilePathUtil();
        frc = new FileResConfig(logicA.scId);
        defaultExtraMenu = new DefaultExtraMenuBean(logicA);
        projectDataManager = ProjectDataManager.getProjectDataManager(logicA.scId);
        javaName = logicA.projectFile.getJavaName();
    }

    public static void setupSearchView(View view, ViewGroup viewGroup) {
        if (viewGroup.getChildCount() == 0) {
            return;
        }
        EditText searchInput = view.findViewById(R.id.searchInput);
        TextInputLayout textInputLayout = view.findViewById(R.id.searchInputLayout);
        textInputLayout.setVisibility(View.VISIBLE);
        searchInput.addTextChangedListener(new BaseTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String filterText = s.toString().toLowerCase();
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View childView = viewGroup.getChildAt(i);
                    if (childView instanceof TextView textView) {
                        String itemText = Helper.getText(textView).toLowerCase();
                        if (itemText.contains(filterText)) {
                            textView.setVisibility(View.VISIBLE);
                        } else {
                            textView.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
    }

    private void codeMenu(FieldBlockView menu) {
        AsdDialog asdDialog = new AsdDialog(logicEditor);
        asdDialog.setContent(menu.getArgValue().toString());
        asdDialog.show();
        asdDialog.setOnSaveClickListener(logicEditor, false, menu, asdDialog);
        asdDialog.setOnCancelClickListener(asdDialog);
    }

    public void defineMenuSelector(FieldBlockView ss) {
        String menuType = ss.blockType;
        String menuName = ss.getMenuName();

        switch (menuType) {
            case "d":
                logicEditor.showNumberOrStringInput(ss, true);
                break;

            case "s":
                switch (menuName) {
                    case "intentData":
                        logicEditor.showIntentDataInput(ss);
                        return;

                    case "url":
                        logicEditor.showStringInput(ss);
                        return;

                    case "inputCode":
                        codeMenu(ss);
                        return;

                    case "import":
                        asdDialog(ss, Helper.getResString(R.string.extra_menu_import_hint));
                        return;

                    default:
                        asdDialog(ss, null);
                }
                break;

            case "m":
                switch (menuName) {
                    case "resource":
                        logicEditor.pickImage(ss, "property_image");
                        return;

                    case "resource_bg":
                        logicEditor.pickImage(ss, "property_background_resource");
                        return;

                    case "sound":
                        logicEditor.showSoundPicker(ss);
                        return;

                    case "font":
                        logicEditor.showFontPicker(ss);
                        return;

                    case "typeface":
                        logicEditor.showTypefaceSelector(ss);
                        return;

                    case "color":
                        logicEditor.showColorPicker(ss);
                        return;

                    case "view":
                    case "textview":
                    case "edittext":
                    case "imageview":
                    case "listview":
                    case "spinner":
                    case "listSpn":
                    case "webview":
                    case "checkbox":
                    case "switch":
                    case "seekbar":
                    case "calendarview":
                    case "compoundButton":
                    case "materialButton":
                    case "adview":
                    case "progressbar":
                    case "mapview":
                    case "radiobutton":
                    case "ratingbar":
                    case "searchview":
                    case "videoview":
                    case "gridview":
                    case "actv":
                    case "mactv":
                    case "tablayout":
                    case "viewpager":
                    case "bottomnavigation":
                    case "badgeview":
                    case "patternview":
                    case "sidebar":
                    case "recyclerview":
                    case "cardview":
                    case "collapsingtoolbar":
                    case "textinputlayout":
                    case "swiperefreshlayout":
                    case "radiogroup":
                    case "lottie":
                    case "otpview":
                    case "signinbutton":
                    case "youtubeview":
                    case "codeview":
                    case "datepicker":
                    case "timepicker":
                        logicEditor.showViewSelector(ss);
                        return;

                    case "Assets":
                    case "NativeLib":
                        pathSelectorMenu(ss);
                        return;

                    default:
                        defaultMenus(ss);
                }
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void defaultMenus(FieldBlockView menu) {
        String menuName = menu.getMenuName();
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        View rootView = ViewUtil.inflateLayout(logicEditor, R.layout.property_popup_selector_single);
        ViewGroup viewGroup = rootView.findViewById(R.id.rg_content);
        ArrayList<String> menus = new ArrayList<>();
        String title;
        switch (menuName) {
            case "varInt":
                title = logicEditor.getString(R.string.logic_editor_title_select_variable_number);
                menus = getVarMenus(VARIABLE_TYPE_NUMBER);
                break;

            case "varBool":
                title = logicEditor.getString(R.string.logic_editor_title_select_variable_boolean);
                menus = getVarMenus(VARIABLE_TYPE_BOOLEAN);
                break;

            case "varStr":
                title = logicEditor.getString(R.string.logic_editor_title_select_variable_string);
                menus = getVarMenus(VARIABLE_TYPE_STRING);
                break;

            case "varMap":
                title = logicEditor.getString(R.string.logic_editor_title_select_variable_map);
                menus = getVarMenus(VARIABLE_TYPE_MAP);
                break;

            case "listInt":
                title = logicEditor.getString(R.string.logic_editor_title_select_list_number);
                menus = getListMenus(LIST_TYPE_NUMBER);
                break;

            case "listStr":
                title = logicEditor.getString(R.string.logic_editor_title_select_list_string);
                menus = getListMenus(LIST_TYPE_STRING);
                break;

            case "listMap":
                title = logicEditor.getString(R.string.logic_editor_title_select_list_map);
                menus = getListMenus(LIST_TYPE_MAP);
                break;

            case "list":
                title = logicEditor.getString(R.string.logic_editor_title_select_list);
                for (String variable : projectDataManager.getListNames(javaName)) {
                    String variableName = CustomVariableUtil.getVariableName(variable);
                    menus.add(variableName != null ? variableName : variable);
                }
                break;

            case "intent":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_intent);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_INTENT);
                break;

            case "file":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_file);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_SHAREDPREF);
                break;

            case "intentAction":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_intent_action);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.INTENT_ACTIONS));
                break;

            case "intentFlags":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_intent_flags);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.INTENT_FLAGS));
                break;

            case "calendar":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_calendar);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_CALENDAR);
                break;

            case "calendarField":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_calendar_field);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.CALENDAR_FIELDS));
                break;

            case "vibrator":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_vibrator);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_VIBRATOR);
                break;

            case "timer":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_timer);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_TIMERTASK);
                break;

            case "firebase":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_firebase);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE);
                break;

            case "firebaseauth":
                title = logicEditor.getString(R.string.logic_editor_component_firebaseauth_title_select_firebase_auth);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH);
                break;

            case "firebasestorage":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_firebasestorage);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE_STORAGE);
                break;

            case "dialog":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_dialog);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_DIALOG);
                break;

            case "mediaplayer":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_mediaplayer);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_MEDIAPLAYER);
                break;

            case "soundpool":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_soundpool);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_SOUNDPOOL);
                break;

            case "objectanimator":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_objectanimator);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_OBJECTANIMATOR);
                break;

            case "aniRepeatMode":
                title = logicEditor.getString(R.string.logic_editor_title_select_animator_repeat_mode);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.REPEAT_MODES));
                break;

            case "aniInterpolator":
                title = logicEditor.getString(R.string.logic_editor_title_select_animator_interpolator);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.INTERPOLATOR_TYPES));
                break;

            case "visible":
                title = logicEditor.getString(R.string.logic_editor_title_select_visibility);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.VISIBILITY_OPTIONS));
                break;

            case "cacheMode":
                title = logicEditor.getString(R.string.logic_editor_title_select_cache_mode);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.CACHE_MODES));
                break;

            case "animatorproperty":
                title = logicEditor.getString(R.string.logic_editor_title_select_animator_target_property);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.ANIMATION_PROPERTIES));
                break;

            case "gyroscope":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_gyroscope);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_GYROSCOPE);
                break;

            case "interstitialad":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_interstitialad);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD);
                break;

            case "camera":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_camera);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_CAMERA);
                break;

            case "filepicker":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_filepicker);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FILE_PICKER);
                break;

            case "directoryType":
                title = logicEditor.getString(R.string.logic_editor_title_select_directory_type);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.STORAGE_DIRECTORIES));
                break;

            case "requestnetwork":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_request_network);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_REQUEST_NETWORK);
                break;

            case "method":
                title = logicEditor.getString(R.string.logic_editor_title_request_network_method);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.HTTP_METHODS));
                break;

            case "requestType":
                title = logicEditor.getString(R.string.logic_editor_title_request_network_request_type);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.REQUEST_TYPES));
                break;

            case "texttospeech":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_text_to_speech);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_TEXT_TO_SPEECH);
                break;

            case "speechtotext":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_speech_to_text);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_SPEECH_TO_TEXT);
                break;

            case "bluetoothconnect":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_bluetooth_connect);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_BLUETOOTH_CONNECT);
                break;

            case "locationmanager":
                title = logicEditor.getString(R.string.logic_editor_title_select_component_location_manager);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_LOCATION_MANAGER);
                break;

            case "videoad":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_REWARDED_VIDEO_AD);
                break;

            case "progressdialog":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_PROGRESS_DIALOG);
                break;

            case "datepickerdialog":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_DATE_PICKER_DIALOG);
                break;

            case "asynctask":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(36);
                break;

            case "timepickerdialog":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_TIME_PICKER_DIALOG);
                break;

            case "notification":
                title = logicEditor.getString(R.string.logic_editor_title_select_component);
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_NOTIFICATION);
                break;

            case "fragmentAdapter":
                title = "Select a FragmentAdapter Component";
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FRAGMENT_ADAPTER);
                break;

            case "phoneauth":
                title = "Select a FirebasePhone Component";
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE);
                break;

            case "cloudmessage":
                title = "Select a CloudMessage Component";
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE);
                break;

            case "googlelogin":
                title = "Select a FirebaseGoogle Component";
                menus = getComponentMenus(ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN);
                break;

            case "providerType":
                title = logicEditor.getString(R.string.logic_editor_title_location_manager_provider_type);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.LOCATION_PROVIDERS));
                break;

            case "mapType":
                title = logicEditor.getString(R.string.logic_editor_title_mapview_map_type);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.MAP_TYPES));
                break;

            case "markerColor":
                title = logicEditor.getString(R.string.logic_editor_title_mapview_marker_color);
                menus = new ArrayList<>(Arrays.asList(BlockConstants.MARKER_HUES));
                break;

            case "service":
                title = "Select a Background Service";
                if (FileUtil.isExistFile(fpu.getManifestService(sc_id))) {
                    menus = frc.getServiceManifestList();
                }
                break;

            case "broadcast":
                title = "Select a Broadcast Receiver";
                if (FileUtil.isExistFile(fpu.getManifestBroadcast(sc_id))) {
                    menus = frc.getBroadcastManifestList();
                }
                break;

            case "activity":
                ArrayList<String> activityMenu = new ArrayList<>();
                title = logicEditor.getString(R.string.logic_editor_title_select_activity);
                for (ProjectFileBean projectFileBean : ProjectDataManager.getFileManager(sc_id).getActivities()) {
                    activityMenu.add(projectFileBean.getActivityName());
                }
                for (String activity : activityMenu) {
                    viewGroup.addView(logicEditor.createRadioButton(activity));
                }
                activityMenu = new ArrayList<>();
                if (FileUtil.isExistFile(fpu.getManifestJava(sc_id))) {
                    for (String activity : frc.getJavaManifestList()) {
                        if (activity.contains(".")) {
                            activityMenu.add(activity.substring(1 + activity.lastIndexOf(".")));
                        }
                    }
                    if (!activityMenu.isEmpty()) {
                        TextView txt = new TextView(logicEditor);
                        txt.setText(logicEditor.getString(R.string.logic_custom_activities));
                        txt.setPadding((int) getDip(2), (int) getDip(4), (int) getDip(4), (int) getDip(4));
                        txt.setTextSize(14f);
                        viewGroup.addView(txt);
                    }
                    for (String activity : activityMenu) {
                        viewGroup.addView(logicEditor.createRadioButton(activity));
                    }
                }
                setupSearchView(rootView, viewGroup);
                break;

            case "customViews":
                title = "Select a Custom View";
                for (ProjectFileBean projectFileBean : ProjectDataManager.getFileManager(sc_id).getCustomViews()) {
                    menus.add(projectFileBean.fileName);
                }
                break;

            case "SignButtonColor":
                title = "Select a SignInButton Color";
                menus.add("COLOR_AUTO");
                menus.add("COLOR_DARK");
                menus.add("COLOR_LIGHT");
                break;

            case "SignButtonSize":
                title = "Select SignInButton Size";
                menus.add("SIZE_ICON_ONLY");
                menus.add("SIZE_STANDARD");
                menus.add("SIZE_WIDE");
                break;

            case "ResString":
                title = "Select a ResString";

                String filePath = FileUtil.getExternalStorageDir().concat("/.sketchware/data/").concat(sc_id.concat("/files/resource/values/strings.xml"));
                ArrayList<HashMap<String, Object>> StringsListMap = new ArrayList<>();
                StringsEditorManager stringsEditorManager = new StringsEditorManager();
                stringsEditorManager.convertXmlStringsToListMap(FileUtil.readFileIfExist(filePath), StringsListMap);

                if (!stringsEditorManager.isXmlStringsExist(StringsListMap, "app_name")) {
                    menus.add("R.string.app_name");
                }
                for (HashMap<String, Object> map : StringsListMap) {
                    menus.add("R.string." + map.get("key"));
                }

                break;
            case "ResStyle":
            case "ResColor":
            case "ResArray":
            case "ResDimen":
            case "ResBool":
            case "ResInteger":
            case "ResAttr":
            case "ResXml":
                title = Helper.getResString(R.string.menu_deprecated_title);
                dialog.setMessage(Helper.getResString(R.string.menu_deprecated_msg));
                break;

            case "AdUnit":
                dialog.setIcon(R.drawable.unit_96);
                title = "Select an Ad Unit";
                for (AdUnitBean bean : ProjectDataManager.getLibraryManager(sc_id).admob.adUnits) {
                    menus.add(bean.id);
                }
                break;

            case "TestDevice":
                dialog.setIcon(R.drawable.ic_test_device_48dp);
                title = "Select a Test device";
                for (AdTestDeviceBean testDevice : ProjectDataManager.getLibraryManager(sc_id).admob.testDevices) {
                    menus.add(testDevice.deviceId);
                }
                break;

            case "IntentKey":
                title = "Select an Intent key";
                menus.addAll(new ArrayList<>(Arrays.asList(intentKey)));
                break;

            case "PatternFlag":
                title = "Select a Pattern Flags";
                menus.addAll(new ArrayList<>(Arrays.asList(patternFlags)));
                break;

            case "Permission":
                title = "Select a Permission";
                menus.addAll(new ArrayList<>(Arrays.asList(permission)));
                break;

            case "AdSize":
                title = "Select an Ad size";
                menus.addAll(new ArrayList<>(Arrays.asList(adSize)));
                break;

            case "PixelFormat":
                title = "Select a PixelFormat";
                menus.addAll(new ArrayList<>(Arrays.asList(pixelFormat)));
                break;

            case "Variable":
                title = "Select a Variable";
                for (Pair<Integer, String> integerStringPair : projectDataManager.getVariables(javaName)) {
                    String variable = integerStringPair.second;
                    String variableName = CustomVariableUtil.getVariableName(variable);
                    menus.add(variableName != null ? variableName : variable);
                }
                break;

            case "Component":
                title = "Select a Component";
                for (ComponentBean componentBean : projectDataManager.getComponents(javaName)) {
                    menus.add(componentBean.componentId);
                }
                break;

            case "CustomVar":
                title = "Select a Custom Variable";
                for (String s : projectDataManager.getVariableNamesByType(javaName, 5)) {
                    Matcher matcher = Pattern.compile("^(\\w+)[\\s]+(\\w+)").matcher(s);
                    while (matcher.find()) {
                        menus.add(matcher.group(2));
                    }
                }
                for (String variable : projectDataManager.getVariableNamesByType(javaName, 6)) {
                    String variableName = CustomVariableUtil.getVariableName(variable);
                    menus.add(variableName != null ? variableName : variable);
                }
                break;

            default:
                Pair<String, ArrayList<String>> menuPair = defaultExtraMenu.getMenu(menu);
                title = menuPair.first;
                menus = new ArrayList<>(menuPair.second);
        }

        for (String menuArg : menus) {
            viewGroup.addView(logicEditor.createRadioButton(menuArg));
        }
        setupSearchView(rootView, viewGroup);
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (viewGroup.getChildAt(i) instanceof RadioButton rb) {
                if (menu.getArgValue().toString().equals(Helper.getText(rb))) {
                    rb.setChecked(true);
                    break;
                }
            }
        }

        dialog.setTitle(title);
        dialog.setView(rootView);
        dialog.setPositiveButton(R.string.common_word_select, (v, which) -> {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (viewGroup.getChildAt(i) instanceof RadioButton rb) {
                    if (rb.isChecked()) {
                        logicEditor.setFieldValue(menu, Helper.getText(rb));
                    }
                }
            }
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.setNeutralButton(R.string.common_word_code_editor, (v, which) -> {
            AsdDialog editor = new AsdDialog(logicEditor);
            editor.setContent(menu.getArgValue().toString());
            editor.show();
            editor.setOnSaveClickListener(logicEditor, false, menu, editor);
            editor.setOnCancelClickListener(editor);
            v.dismiss();
        });
        dialog.show();
    }

    private ArrayList<String> getVarMenus(int type) {
        return projectDataManager.getVariableNamesByType(javaName, type);
    }

    private ArrayList<String> getListMenus(int type) {
        return projectDataManager.getListNamesByType(javaName, type);
    }

    private ArrayList<String> getComponentMenus(int type) {
        return projectDataManager.getComponentIdsByType(javaName, type);
    }

    private void asdDialog(FieldBlockView ss, String message) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle(R.string.logic_editor_title_enter_string_value);

        if (!isEmpty(message)) dialog.setMessage(message);

        View root = ViewUtil.inflateLayout(logicEditor, R.layout.property_popup_input_text);
        EditText edittext = root.findViewById(R.id.ed_input);
        edittext.setImeOptions(EditorInfo.IME_ACTION_NONE);

        if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_USE_ASD_HIGHLIGHTER)) {
            new SimpleHighlighter(edittext);
        }
        edittext.setText(ss.getArgValue().toString());
        dialog.setView(root);

        dialog.setPositiveButton(R.string.common_word_save, (v, which) -> {
            String content = Helper.getText(edittext);
            if (!content.isEmpty() && content.charAt(0) == '@') {
                content = " " + content;
            }
            logicEditor.setFieldValue(ss, content);
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.setNeutralButton(R.string.common_word_code_editor, (v, which) -> {
            AsdDialog asdDialog = new AsdDialog(logicEditor);
            asdDialog.setContent(Helper.getText(edittext));
            asdDialog.show();
            asdDialog.setOnSaveClickListener(logicEditor, false, ss, asdDialog);
            asdDialog.setOnCancelClickListener(asdDialog);
            v.dismiss();
        });
        dialog.show();
    }

    private void pathSelectorMenu(FieldBlockView ss) {
        String menuName = ss.getMenuName();
        ArrayList<String> markedPath = new ArrayList<>();

        mOptions.setSelectionMode(SelectionMode.BOTH);
        String path = null;
        if (menuName.equals("Assets")) {
            mOptions.setTitle(Helper.getResString(R.string.menu_select_asset));
            path = String.format(ASSETS_PATH, sc_id);
            markedPath.add(0, path + ss.getArgValue().toString());
        } else if (menuName.equals("NativeLib")) {
            mOptions.setTitle(Helper.getResString(R.string.menu_select_native_lib));
            path = String.format(NATIVE_PATH, sc_id);
            markedPath.add(0, path + ss.getArgValue().toString());
        }
        String[] pathSegments = path.split("/");
        String splitter = pathSegments[pathSegments.length - 1];
        mOptions.setInitialDirectory(path);
        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFileSelected(File file) {
                String[] parts = file.getAbsolutePath().split(splitter, 2);
                logicEditor.setFieldValue(ss, parts.length > 1 ? parts[1] : "");
            }
        };
        FilePickerDialogFragment fpd = new FilePickerDialogFragment(mOptions, callback);
        fpd.show(logicEditor.getSupportFragmentManager(), "filePicker");
    }
}
