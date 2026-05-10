package pro.sketchware.activities.editor.component;

import static pro.sketchware.utility.GsonUtils.getGson;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import pro.sketchware.core.project.SketchwarePaths;
import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.IconSelectorDialog;
import mod.hilal.saif.components.ComponentsHandler;
import mod.jbk.util.LogUtil;
import mod.jbk.util.OldResourceIdMapper;
import pro.sketchware.model.CustomComponent;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageCustomComponentAddBinding;
import pro.sketchware.tools.ComponentHelper;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class AddCustomComponentActivity extends BaseAppCompatActivity implements View.OnClickListener {

    private final String path = SketchwarePaths.getCustomComponent();
    private boolean isEditMode = false;
    private int position = 0;
    private ManageCustomComponentAddBinding binding;

    @Override
    public void onCreate(Bundle _savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(_savedInstanceState);
        binding = ManageCustomComponentAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void init() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        binding.toolbar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.btnCancel.setOnClickListener(Helper.getBackPressedClickListener(this));
        if (getIntent().hasExtra("pos")) {
            isEditMode = true;
            position = getIntent().getIntExtra("pos", 0);
        }
        getViewsById();
        if (isEditMode) {
            setTitle(Helper.getResString(R.string.event_title_edit_component));
            fillUp();
        } else {
            setTitle(Helper.getResString(R.string.event_title_add_new_component));
            initializeHelper();
        }

        UI.addWindowInsetToPadding(binding.content, WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime() | WindowInsetsCompat.Type.displayCutout(), true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.appBarLayout, true, true, true, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, Helper.getResString(R.string.common_word_import));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 0) {
            showFilePickerDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fillUp() {
        if (FileUtil.isExistFile(path)) {
            try {
                ArrayList<CustomComponent> list = getGson().fromJson(FileUtil.readFile(path),
                        new TypeToken<ArrayList<CustomComponent>>(){}.getType());
                if (list == null || position < 0 || position >= list.size()) return;
                CustomComponent map = list.get(position);
                setupViews(map);
            } catch (JsonSyntaxException e) {
                LogUtil.w("AddCustomComponentActivity", "Failed to parse custom component JSON", e);
            }
        }
    }

    private void getViewsById() {
        binding.btnSave.setOnClickListener(this);
        binding.tilComponentIcon.setEndIconOnClickListener(v -> showIconSelectorDialog());
    }

    private void initializeHelper() {
        binding.componentName.addTextChangedListener(new ComponentHelper(new EditText[]{binding.componentBuildClass, binding.componentVariableName, binding.componentTypeName, binding.componentTypeClass}, binding.componentTypeClass));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_save) {
            if (!isImportantFieldsEmpty()) {
                if (OldResourceIdMapper.isValidIconId(Helper.getText(binding.componentIcon))) {
                    save();
                } else {
                    SketchwareUtil.toastError(Helper.getResString(R.string.invalid_icon_id));
                    binding.componentIcon.requestFocus();
                }
            } else {
                SketchwareUtil.toastError(Helper.getResString(R.string.invalid_required_fields));
            }
        }
    }

    private void setupViews(CustomComponent map) {
        binding.componentName.setText(map.getName());
        binding.componentId.setText(map.getId());
        binding.componentIcon.setText(map.getIcon());
        binding.componentVariableName.setText(map.getVarName());
        binding.componentTypeName.setText(map.getTypeName());
        binding.componentBuildClass.setText(map.getBuildClass());
        binding.componentTypeClass.setText(map.getClassName());
        binding.componentDescription.setText(map.getDescription());
        binding.componentDocUrl.setText(map.getUrl());
        binding.componentAddVar.setText(map.getAdditionalVar());
        binding.componentDefAddVar.setText(map.getDefineAdditionalVar());
        binding.componentImports.setText(map.getImports());
    }

    private void showIconSelectorDialog() {
        new IconSelectorDialog(this, binding.componentIcon).show();
    }

    private boolean isImportantFieldsEmpty() {
        return Helper.getText(binding.componentName).isEmpty()
                || Helper.getText(binding.componentId).isEmpty()
                || Helper.getText(binding.componentIcon).isEmpty()
                || Helper.getText(binding.componentTypeName).isEmpty()
                || Helper.getText(binding.componentVariableName).isEmpty()
                || Helper.getText(binding.componentTypeClass).isEmpty()
                || Helper.getText(binding.componentBuildClass).isEmpty();
    }

    private void save() {
        ArrayList<CustomComponent> list = new ArrayList<>();
        if (FileUtil.isExistFile(path)) {
            try {
                list = getGson().fromJson(FileUtil.readFile(path),
                        new TypeToken<ArrayList<CustomComponent>>(){}.getType());
                if (list == null) {
                    list = new ArrayList<>();
                }
            } catch (JsonSyntaxException e) {
                LogUtil.w("AddCustomComponentActivity", "Failed to parse custom component JSON", e);
            }
        }
        CustomComponent map = new CustomComponent();
        if (isEditMode) {
            if (position < 0 || position >= list.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            map = list.get(position);
        }
        map.setName(Helper.getText(binding.componentName));
        map.setId(Helper.getText(binding.componentId));
        map.setIcon(Helper.getText(binding.componentIcon));
        map.setVarName(Helper.getText(binding.componentVariableName));
        map.setTypeName(Helper.getText(binding.componentTypeName));
        map.setBuildClass(Helper.getText(binding.componentBuildClass));
        map.setClassName(Helper.getText(binding.componentTypeClass));
        map.setDescription(Helper.getText(binding.componentDescription));
        map.setUrl(Helper.getText(binding.componentDocUrl));
        map.setAdditionalVar(Helper.getText(binding.componentAddVar));
        map.setDefineAdditionalVar(Helper.getText(binding.componentDefAddVar));
        map.setImports(Helper.getText(binding.componentImports));
        if (!isEditMode) {
            list.add(map);
        }
        FileUtil.writeFile(path, getGson().toJson(list));
        SketchwareUtil.toast(Helper.getResString(R.string.common_word_saved));
        finish();
    }

    private void showFilePickerDialog() {
        FilePickerOptions options = new FilePickerOptions();
        options.setExtensions(new String[]{"json"});
        options.setTitle(Helper.getResString(R.string.component_select_json));

        FilePickerCallback callback = new FilePickerCallback() {
            @Override
            public void onFileSelected(File file) {
                selectComponentToImport(file.getAbsolutePath());
            }
        };

        FilePickerDialogFragment dialogFragment = new FilePickerDialogFragment(options, callback);
        dialogFragment.show(getSupportFragmentManager(), "filePickerDialog");
    }

    private void selectComponentToImport(String path) {
        var readResult = ComponentsHandler.readComponents(path);
        if (readResult.first.isPresent()) {
            SketchwareUtil.toastError(readResult.first.get());
            return;
        }
        var components = readResult.second;

        var componentNames = components.stream()
                .map(CustomComponent::getName)
                .collect(Collectors.toList());
        if (componentNames.size() > 1) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(Helper.getResString(R.string.logic_editor_title_select_component));
            var choiceToImport = new AtomicInteger(-1);
            var listView = new ListView(this);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, componentNames);
            listView.setAdapter(arrayAdapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setDivider(null);
            listView.setDividerHeight(0);
            listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> choiceToImport.set(position));
            dialog.setView(listView);
            dialog.setPositiveButton(Helper.getResString(R.string.common_word_import), (v, which) -> {
                int position = choiceToImport.get();
                if (position == -1) {
                    SketchwareUtil.toastError(Helper.getResString(R.string.invalid_component));
                    v.dismiss();
                    return;
                }
                CustomComponent component = components.get(position);
                if (ComponentsHandler.isValidComponent(component)) {
                    setupViews(component);
                } else {
                    SketchwareUtil.toastError(Helper.getResString(R.string.invalid_component));
                }
                v.dismiss();
            });
            dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
            dialog.show();
        } else {
            CustomComponent component = components.get(0);
            if (ComponentsHandler.isValidComponent(component)) {
                setupViews(component);
            } else {
                SketchwareUtil.toastError(Helper.getResString(R.string.invalid_component));
            }
        }
    }
}
