package com.besome.sketch.projects;


import android.util.Log;
import static mod.hey.studios.util.ProjectFile.getDefaultColor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.besome.sketch.lib.ui.ColorPickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.core.DeviceUtil;
import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.VariableNameValidator;
import pro.sketchware.core.ProjectListManager;
import pro.sketchware.core.UIHelper;
import pro.sketchware.core.DateTimeUtil;
import pro.sketchware.core.EncryptedFileUtil;
import pro.sketchware.core.ViewUtil;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.core.MapValueHelper;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.util.Helper;
import mod.hey.studios.util.ProjectFile;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.activities.iconcreator.IconCreatorActivity;
import pro.sketchware.control.VersionDialog;
import pro.sketchware.databinding.MyprojectSettingBinding;
import pro.sketchware.lib.validator.AppNameValidator;
import pro.sketchware.lib.validator.PackageNameValidator;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class MyProjectSettingActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> openIconCreator;
    private final String[] themeColorKeys = {"color_accent", "color_primary", "color_primary_dark", "color_control_highlight", "color_control_normal"};
    private final String[] themeColorLabels = {"colorAccent", "colorPrimary", "colorPrimaryDark", "colorControlHighlight", "colorControlNormal"};
    private final int[] projectThemeColors = new int[themeColorKeys.length];
    public MyprojectSettingBinding binding;
    private PackageNameValidator projectPackageNameValidator;
    private VariableNameValidator projectNameValidator;
    private AppNameValidator projectAppNameValidator;
    private boolean projectHasCustomIcon = false;
    private boolean updatingExistingProject = false;
    private int projectVersionCode = 1;
    private int projectVersionNameFirstPart;
    private int projectVersionNameSecondPart;
    private boolean shownPackageNameChangeWarning;
    private boolean isIconAdaptive;
    private Bitmap icon;
    private String sc_id;

    private ThemePresetAdapter themePresetAdapter;

    public static void saveBitmapTo(Bitmap bitmap, String path) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
        } catch (IOException e) {
            Log.e("MyProjectSettingActivity", "Failed to save bitmap to " + path, e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openIconCreator = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getParcelableExtra("appIco") != null) {
                            icon = data.getParcelableExtra("appIco");
                            isIconAdaptive = data.getBooleanExtra("isIconAdaptive", false);
                            binding.appIcon.setImageBitmap(icon);
                            projectHasCustomIcon = true;
                        }
                    }
                });
        binding = MyprojectSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(arg0 -> getOnBackPressedDispatcher().onBackPressed());

        if (!isStoragePermissionGranted()) finish();

        sc_id = getIntent().getStringExtra("sc_id");
        updatingExistingProject = getIntent().getBooleanExtra("is_update", false);

        binding.verCode.setSelected(true);
        binding.verName.setSelected(true);


        binding.appIconLayout.setOnClickListener(this);
        binding.verCodeHolder.setOnClickListener(this);
        binding.verNameHolder.setOnClickListener(this);
        binding.imgThemeColorHelp.setOnClickListener(this);
        binding.okButton.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);

        initializeThemePresets();

        binding.tilAppName.setHint(Helper.getResString(R.string.myprojects_settings_hint_enter_application_name));
        binding.tilPackageName.setHint(Helper.getResString(R.string.myprojects_settings_hint_enter_package_name));
        binding.tilProjectName.setHint(Helper.getResString(R.string.myprojects_settings_hint_enter_project_name));

        projectAppNameValidator = new AppNameValidator(getApplicationContext(), binding.tilAppName);
        projectPackageNameValidator = new PackageNameValidator(getApplicationContext(), binding.tilPackageName);
        projectNameValidator = new VariableNameValidator(getApplicationContext(), binding.tilProjectName);
        binding.tilPackageName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (!shownPackageNameChangeWarning && !Helper.getText((EditText) v).trim().contains("com.my.newproject")) {
                    showPackageNameChangeWarning();
                }
            }
        });

        projectThemeColors[0] = getDefaultColor(ProjectFile.COLOR_ACCENT);
        projectThemeColors[1] = getDefaultColor(ProjectFile.COLOR_PRIMARY);
        projectThemeColors[2] = getDefaultColor(ProjectFile.COLOR_PRIMARY_DARK);
        projectThemeColors[3] = getDefaultColor(ProjectFile.COLOR_CONTROL_HIGHLIGHT);
        projectThemeColors[4] = getDefaultColor(ProjectFile.COLOR_CONTROL_NORMAL);

        for (int i = 0; i < themeColorKeys.length; i++) {
            ThemeColorView colorView = new ThemeColorView(this, i);
            colorView.name.setText(themeColorLabels[i]);
            colorView.color.setBackgroundColor(Color.WHITE);
            binding.layoutThemeColors.addView(colorView);
            colorView.setOnClickListener(v -> {
                if (!UIHelper.isClickThrottled()) {
                    pickColor(v, (Integer) v.getTag());
                }
            });
        }
        if (updatingExistingProject) {
            /* Set the dialog's title & save button label */
            binding.toolbar.setTitle(R.string.project_settings_title);
            HashMap<String, Object> metadata = ProjectListManager.getProjectById(sc_id);
            binding.etPackageName.setText(MapValueHelper.getString(metadata, "my_sc_pkg_name"));
            binding.etProjectName.setText(MapValueHelper.getString(metadata, "my_ws_name"));
            binding.etAppName.setText(MapValueHelper.getString(metadata, "my_app_name"));
            binding.okButton.setText(Helper.getResString(R.string.project_save_changes));
            projectVersionCode = parseInt(MapValueHelper.getString(metadata, "sc_ver_code"), 1);
            parseVersion(MapValueHelper.getString(metadata, "sc_ver_name"));
            binding.verCode.setText(MapValueHelper.getString(metadata, "sc_ver_code"));
            binding.verName.setText(MapValueHelper.getString(metadata, "sc_ver_name"));
            projectHasCustomIcon = MapValueHelper.get(metadata, "custom_icon");
            if (projectHasCustomIcon) {
                binding.appIcon.setImageURI(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", getCustomIcon()));
            }

            for (int i = 0; i < themeColorKeys.length; i++) {
                projectThemeColors[i] = MapValueHelper.get(metadata, themeColorKeys[i], projectThemeColors[i]);
            }
        } else {
            /* Set the dialog's title & create button label */
            String newProjectName = getIntent().getStringExtra("my_ws_name");
            String newProjectPackageName = getIntent().getStringExtra("my_sc_pkg_name");
            if (sc_id == null || sc_id.isEmpty()) {
                sc_id = ProjectListManager.getNextProjectId();
                newProjectName = ProjectListManager.getNextWorkspaceName();
                newProjectPackageName = "com.my." + newProjectName.toLowerCase();
            }
            binding.etPackageName.setText(newProjectPackageName);
            binding.etProjectName.setText(newProjectName);
            binding.etAppName.setText(getIntent().getStringExtra("my_app_name"));

            String newProjectVersionCode = getIntent().getStringExtra("sc_ver_code");
            String newProjectVersionName = getIntent().getStringExtra("sc_ver_name");
            if (newProjectVersionCode == null || newProjectVersionCode.isEmpty()) {
                newProjectVersionCode = "1";
            }
            if (newProjectVersionName == null || newProjectVersionName.isEmpty()) {
                newProjectVersionName = "1.0";
            }
            projectVersionCode = parseInt(newProjectVersionCode, 1);
            parseVersion(newProjectVersionName);
            binding.verCode.setText(newProjectVersionCode);
            binding.verName.setText(newProjectVersionName);
            projectHasCustomIcon = getIntent().getBooleanExtra("custom_icon", false);
            if (projectHasCustomIcon) {
                binding.appIcon.setImageURI(FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", getCustomIcon()));
            }
        }
        syncThemeColors();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.app_icon_layout) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), IconCreatorActivity.class);
            intent.putExtra("sc_id", sc_id);
            openIconCreator.launch(intent);
        } else if (id == R.id.ok_button) {
            UIHelper.disableTemporarily(v);
            if (isInputValid()) {
                new SaveProjectAsyncTask(getApplicationContext()).execute();
                if (icon != null) saveBitmapTo(icon, getCustomIconPath());
            }
        } else if (id == R.id.cancel) {
            finish();
        } else if (id == R.id.img_theme_color_help) {
            animateLayoutChanges(binding.getRoot());
            if (binding.imgColorGuide.getVisibility() == View.VISIBLE) {
                binding.imgColorGuide.setVisibility(View.GONE);
            } else {
                binding.imgColorGuide.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.ver_code_holder || id == R.id.ver_name_holder) {
            if (ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_USE_NEW_VERSION_CONTROL)) {
                new VersionDialog(this).show();
            } else {
                showOldVersionControlDialog();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isStoragePermissionGranted()) {
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EncryptedFileUtil fileUtil = new EncryptedFileUtil();
        fileUtil.mkdirs(SketchwarePaths.getIconsPath() + File.separator + sc_id);
        fileUtil.mkdirs(SketchwarePaths.getImagesPath() + File.separator + sc_id);
        fileUtil.mkdirs(SketchwarePaths.getSoundsPath() + File.separator + sc_id);
        fileUtil.mkdirs(SketchwarePaths.getFontsResourcePath() + File.separator + sc_id);
        File o = getCustomIcon();
        if (!o.exists()) {
            try {
                o.createNewFile();
            } catch (IOException e) {
                Log.e("MyProjectSettingActivity", e.getMessage(), e);
            }
        }
    }

    private void showOldVersionControlDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.numbers_48);
        dialog.setTitle(Helper.getResString(R.string.myprojects_settings_version_control_title));
        View view = ViewUtil.inflateLayout(getApplicationContext(), R.layout.property_popup_version_control);
        ((TextView) view.findViewById(R.id.tv_code)).setText(Helper.getResString(R.string.myprojects_settings_version_control_title_code));
        ((TextView) view.findViewById(R.id.tv_name)).setText(Helper.getResString(R.string.myprojects_settings_version_control_title_name));

        NumberPicker versionCodePicker = view.findViewById(R.id.version_code);
        NumberPicker versionNameFirstPartPicker = view.findViewById(R.id.version_name1);
        NumberPicker versionNameSecondPartPicker = view.findViewById(R.id.version_name2);

        versionCodePicker.setWrapSelectorWheel(false);
        versionNameFirstPartPicker.setWrapSelectorWheel(false);
        versionNameSecondPartPicker.setWrapSelectorWheel(false);

        int versionCode = Integer.parseInt(Helper.getText(binding.verCode));
        int versionCodeMinimum = versionCode - 5;
        int versionNameFirstPartMinimum = 1;
        if (versionCodeMinimum <= 0) {
            versionCodeMinimum = 1;
        }
        versionCodePicker.setMinValue(versionCodeMinimum);
        versionCodePicker.setMaxValue(versionCode + 5);
        versionCodePicker.setValue(versionCode);

        String[] split = Helper.getText(binding.verName).split("\\.");
        AtomicInteger projectNewVersionNameFirstPart = new AtomicInteger(parseInt(split[0], 1));
        AtomicInteger projectNewVersionNameSecondPart = new AtomicInteger(parseInt(split[1], 0));
        if (projectNewVersionNameFirstPart.get() - 5 > 0) {
            versionNameFirstPartMinimum = projectNewVersionNameFirstPart.get() - 5;
        }
        versionNameFirstPartPicker.setMinValue(versionNameFirstPartMinimum);
        versionNameFirstPartPicker.setMaxValue(projectNewVersionNameFirstPart.get() + 5);
        versionNameFirstPartPicker.setValue(projectNewVersionNameFirstPart.get());

        versionNameSecondPartPicker.setMinValue(Math.max(projectNewVersionNameSecondPart.get() - 20, 0));
        versionNameSecondPartPicker.setMaxValue(projectNewVersionNameSecondPart.get() + 20);
        versionNameSecondPartPicker.setValue(projectNewVersionNameSecondPart.get());
        dialog.setView(view);

        versionCodePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (oldVal > newVal && newVal < projectVersionCode) {
                picker.setValue(projectVersionCode);
            }
        });
        versionNameFirstPartPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            projectNewVersionNameFirstPart.set(newVal);
            if (oldVal > newVal) {
                if (newVal < projectVersionNameFirstPart) {
                    versionCodePicker.setValue(projectVersionNameFirstPart);
                }
                if (projectNewVersionNameFirstPart.get() == projectVersionNameFirstPart || projectNewVersionNameSecondPart.get() <= projectVersionNameSecondPart) {
                    versionNameSecondPartPicker.setValue(projectVersionNameSecondPart);
                    projectNewVersionNameSecondPart.set(projectVersionNameSecondPart);
                }
            }
        });
        versionNameSecondPartPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            projectNewVersionNameSecondPart.set(newVal);
            if (oldVal > newVal && newVal < projectVersionNameSecondPart && projectNewVersionNameFirstPart.get() < projectVersionNameFirstPart) {
                picker.setValue(projectVersionNameSecondPart);
            }
        });
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (v, which) -> {
            if (!UIHelper.isClickThrottled()) {
                binding.verCode.setText(String.valueOf(versionCodePicker.getValue()));
                binding.verName.setText(projectNewVersionNameFirstPart + "." + projectNewVersionNameSecondPart);
                v.dismiss();
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private void syncThemeColors() {
        for (int i = 0; i < projectThemeColors.length; i++) {
            ((ThemeColorView) binding.layoutThemeColors.getChildAt(i)).color.setBackgroundColor(projectThemeColors[i]);
        }
    }

    private void parseVersion(String toParse) {
        try {
            String[] split = toParse.split("\\.");
            projectVersionNameFirstPart = parseInt(split[0], 1);
            projectVersionNameSecondPart = parseInt(split[1], 0);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            Log.w("MyProjectSettingActivity", "Failed to parse version: " + toParse, e);
        }
    }

    private void pickColor(View anchorView, int colorIndex) {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, projectThemeColors[colorIndex], false, false);
        colorPickerDialog.setColorPickerCallback(new ColorPickerDialog.OnColorPickedListener() {
            @Override
            public void onColorPicked(int var1) {
                projectThemeColors[colorIndex] = var1;
                syncThemeColors();
                themePresetAdapter.unselectThePreviousTheme(-1);
            }

            @Override
            public void onResourceColorPicked(String var1, int var2) {
                projectThemeColors[colorIndex] = var2;
                syncThemeColors();
                themePresetAdapter.unselectThePreviousTheme(-1);
            }
        });
        colorPickerDialog.showAtLocation(anchorView, Gravity.CENTER, 0, 0);
    }

    private void showResetIconConfirmation() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_word_settings));
        dialog.setIcon(R.drawable.default_icon);
        dialog.setMessage(Helper.getResString(R.string.myprojects_settings_confirm_reset_icon));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_reset), (v, which) -> {
            binding.appIcon.setImageResource(R.drawable.default_icon);
            projectHasCustomIcon = false;
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private File getCustomIcon() {
        return new File(getCustomIconPath());
    }

    private String getCustomIconPath() {
        return SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "icon.png";
    }

    private String getTempIconsFolderPath(String foldername) {
        return SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + foldername;
    }

    private String getIconsFolderPath() {
        return SketchwarePaths.getIconsPath() + File.separator + sc_id + File.separator + "mipmaps" + File.separator;
    }

    private boolean isInputValid() {
        return projectPackageNameValidator.isValid() && projectNameValidator.isValid() && projectAppNameValidator.isValid();
    }

    private void showPackageNameChangeWarning() {
        shownPackageNameChangeWarning = true;
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_word_warning));
        dialog.setIcon(R.drawable.break_warning_96_red);
        dialog.setMessage(Helper.getResString(R.string.myprojects_settings_message_package_rename));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException unused) {
            return fallback;
        }
    }

    private void animateLayoutChanges(View view) {
        var autoTransition = new AutoTransition();
        autoTransition.setDuration((short) 200);
        TransitionManager.beginDelayedTransition((ViewGroup) view, autoTransition);
    }

    private void initializeThemePresets() {
        List<ThemeManager.ThemePreset> themePresets = Arrays.asList(ThemeManager.getThemePresets());

        themePresetAdapter = new ThemePresetAdapter(this, themePresets, (theme, position) -> applyTheme(theme));

        binding.btnGenerateRandomTheme.setOnClickListener(v -> {
            themePresetAdapter.unselectThePreviousTheme(-1);
            generateRandomTheme();
        });

        binding.btnReset.setOnClickListener(v -> {
            applyTheme(ThemeManager.getDefault());
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.layoutThemePresets.setLayoutManager(layoutManager);

        binding.layoutThemePresets.setAdapter(themePresetAdapter);
    }

    private void generateRandomTheme() {
        ThemeManager.ThemePreset randomTheme = ThemeManager.generateRandomTheme();
        applyTheme(randomTheme);

        SketchwareUtil.toast(Helper.getResString(R.string.theme_random_generated));
    }

    private void applyTheme(ThemeManager.ThemePreset theme) {
        projectThemeColors[0] = theme.colorAccent;
        projectThemeColors[1] = theme.colorPrimary;
        projectThemeColors[2] = theme.colorPrimaryDark;
        projectThemeColors[3] = theme.colorControlHighlight;
        projectThemeColors[4] = theme.colorControlNormal;

        syncThemeColors();
    }

    private static class ThemeColorView extends LinearLayout {

        private TextView color;
        private TextView name;

        public ThemeColorView(Context context, int tag) {
            super(context);
            initialize(context, tag);
        }

        private void initialize(Context context, int tag) {
            setTag(tag);
            ViewUtil.inflateLayoutInto(context, this, R.layout.myproject_color);
            color = findViewById(R.id.color);
            name = findViewById(R.id.name);
        }
    }

    private class SaveProjectAsyncTask extends BaseAsyncTask {

        public SaveProjectAsyncTask(Context context) {
            super(context);
            addTask(this);
            showLoadingDialog();
        }

        @Override
        public void onSuccess() {
            dismissLoadingDialog();
            Intent intent = getIntent();
            intent.putExtra("sc_id", sc_id);
            intent.putExtra("is_new", !updatingExistingProject);
            intent.putExtra("index", intent.getIntExtra("index", -1));
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public void doWork() {
            HashMap<String, Object> data = new HashMap<>();
            data.put("sc_id", sc_id);
            data.put("my_sc_pkg_name", Helper.getText(binding.etPackageName));
            data.put("my_ws_name", Helper.getText(binding.etProjectName));
            data.put("my_app_name", Helper.getText(binding.etAppName));
            if (updatingExistingProject) {
                data.put("custom_icon", projectHasCustomIcon);
                data.put("isIconAdaptive", isIconAdaptive);
                data.put("sc_ver_code", Helper.getText(binding.verCode));
                data.put("sc_ver_name", Helper.getText(binding.verName));
                data.put("sketchware_ver", DeviceUtil.getVersionCode(getApplicationContext()));
                for (int i = 0; i < themeColorKeys.length; i++) {
                    data.put(themeColorKeys[i], projectThemeColors[i]);
                }
                ProjectListManager.updateProject(sc_id, data);
                updateProjectResourcesContents(data);
            } else {
                data.put("my_sc_reg_dt", new DateTimeUtil().formatCurrentTime("yyyyMMddHHmmss"));
                data.put("custom_icon", projectHasCustomIcon);
                data.put("isIconAdaptive", isIconAdaptive);
                data.put("sc_ver_code", Helper.getText(binding.verCode));
                data.put("sc_ver_name", Helper.getText(binding.verName));
                data.put("sketchware_ver", DeviceUtil.getVersionCode(getApplicationContext()));
                for (int i = 0; i < themeColorKeys.length; i++) {
                    data.put(themeColorKeys[i], projectThemeColors[i]);
                }
                ProjectListManager.saveProject(sc_id, data);
                updateProjectResourcesContents(data);
                SketchwarePaths.clearPreferenceData(getApplicationContext(), sc_id);
                new EncryptedFileUtil().deleteDirectoryByPath(SketchwarePaths.getDataPath(sc_id));
                ProjectSettings projectSettings = new ProjectSettings(sc_id);
                projectSettings.setValue(ProjectSettings.SETTING_NEW_XML_COMMAND, ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
                projectSettings.setValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, ProjectSettings.SETTING_GENERIC_VALUE_TRUE);

            }
            try {
                FileUtil.deleteFile(getTempIconsFolderPath("mipmaps" + File.separator));
                FileUtil.copyDirectory(new File(getTempIconsFolderPath("temp_icons" + File.separator)), new File(getIconsFolderPath()));
                FileUtil.deleteFile(getTempIconsFolderPath("temp_icons" + File.separator));
            } catch (IOException e) {
                Log.e("MyProjectSettingActivity", e.getMessage(), e);
            }

        }

        private void updateProjectResourcesContents(HashMap<String, Object> data) {
            String baseDir = SketchwarePaths.getDataPath(sc_id) + "/files/resource/values/";
            String stringsFilePath = baseDir + "strings.xml";
            String colorsFilePath = baseDir + "colors.xml";
            String newAppName = Objects.requireNonNull(data.get("my_app_name")).toString();

            if (FileUtil.isExistFile(stringsFilePath)) {
                String xmlContent = FileUtil.readFile(stringsFilePath);
                xmlContent = xmlContent.replaceAll("(<string\\s+name=\"app_name\">)(.*?)(</string>)", "$1" + newAppName + "$3");
                FileUtil.writeFile(stringsFilePath, xmlContent);
            }

            if (FileUtil.isExistFile(colorsFilePath)) {
                String xmlContent = FileUtil.readFile(colorsFilePath);
                for (int i = 0; i < themeColorKeys.length; i++) {
                    String colorName = themeColorLabels[i];
                    String newColor = String.format("#%06X", (0xFFFFFF & projectThemeColors[i]));
                    xmlContent = xmlContent.replaceAll("(<color\\s+name=\"" + colorName + "\">)(.*?)(</color>)", "$1" + newColor + "$3");
                }
                FileUtil.writeFile(colorsFilePath, xmlContent);
            }

        }

        @Override
        public void onError(String errorMessage) {
            dismissLoadingDialog();
        }

    }
}
