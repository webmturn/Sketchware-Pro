package pro.sketchware.activities.resourceseditor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.activities.editor.code.SrcCodeEditor;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.activities.resourceseditor.components.adapters.EditorsAdapter;
import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.ColorsEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.DimensEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.StringsEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.StylesEditor;
import pro.sketchware.activities.resourceseditor.components.fragments.ThemesEditor;
import pro.sketchware.core.resources.ColorModel;
import pro.sketchware.databinding.ResourcesEditorImportDialogBinding;
import pro.sketchware.databinding.ResourcesEditorsActivityBinding;
import pro.sketchware.databinding.ResourcesVariantSelectorDialogBinding;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;
import pro.sketchware.util.XmlUtil;

public class ResourcesEditorActivity extends BaseAppCompatActivity {

    private final String variantFullNameStarts = "values-";
    public ProjectFilePaths projectFilePaths;
    public boolean isComingFromSrcCodeEditor;
    public String sc_id;
    public String variant;
    public String stringsFilePath;
    public String colorsFilePath;
    public String stylesFilePath;
    public String themesFilePath;
    public String arrayFilePath;
    public String dimensFilePath;
    public StringsEditor stringsEditor;
    public ColorsEditor colorsEditor;
    public StylesEditor stylesEditor;
    public ThemesEditor themesEditor;
    public ArraysEditor arraysEditor;
    public DimensEditor dimensEditor;
    private ResourcesEditorsActivityBinding binding;
    private MaterialAlertDialogBuilder builder;
    private int currentTabPosition = ResourceEditorTabs.STRINGS;
    private TabLayoutMediator tabMediator;
    private boolean editorsLoaded;
    private String resourceSearchQuery = "";
    private boolean initialLoadPending;
    private boolean sourceReloadPending;
    private Bundle pendingDrafts;
    private final Handler resourceHandler = new Handler(Looper.getMainLooper());
    private final Runnable loadEditorsTask = this::loadPendingEditors;
    private final FragmentManager.FragmentLifecycleCallbacks editorLifecycleCallbacks =
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentViewCreated(@NonNull FragmentManager manager, @NonNull Fragment fragment,
                                                  @NonNull View view, @Nullable Bundle savedInstanceState) {
                    schedulePendingLoad();
                }
            };

    public static String escapeXml(String text) {
        return XmlUtil.escapeXml(text);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentTabPosition = savedInstanceState.getInt("current_tab_position", ResourceEditorTabs.STRINGS);
            resourceSearchQuery = savedInstanceState.getString("resource_search_query", "");
            pendingDrafts = savedInstanceState.getBundle("resource_editor_drafts");
            sourceReloadPending = savedInstanceState.getBoolean("resource_source_reload", false);
        }

        setupUI();
        initializeManagers();
        initializeEditors(savedInstanceState);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(editorLifecycleCallbacks, false);
        setupListeners();
        setupBackPressedHandler();
        initializeBackgroundTask(savedInstanceState == null ? "" : savedInstanceState.getString("resource_variant", ""));
    }

    private void setupUI() {
        binding = ResourcesEditorsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);
        binding.viewPager.setOffscreenPageLimit(ResourceEditorTabs.COUNT - 1);
    }

    private void initializeManagers() {
        sc_id = getIntent().getStringExtra("sc_id");

        projectFilePaths = new ProjectFilePaths(getApplicationContext(), sc_id);
        projectFilePaths.initializeMetadata(ProjectDataManager.getLibraryManager(sc_id), ProjectDataManager.getFileManager(sc_id), ProjectDataManager.getProjectDataManager(sc_id));
    }

    private void initializeEditors(Bundle savedInstanceState) {
        stringsEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.STRINGS, StringsEditor.class, StringsEditor::new);
        colorsEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.COLORS, ColorsEditor.class, ColorsEditor::new);
        stylesEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.STYLES, StylesEditor.class, StylesEditor::new);
        themesEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.THEMES, ThemesEditor.class, ThemesEditor::new);
        arraysEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.ARRAYS, ArraysEditor.class, ArraysEditor::new);
        dimensEditor = restoreEditor(savedInstanceState, ResourceEditorTabs.DIMENS, DimensEditor.class, DimensEditor::new);
    }

    private <T extends Fragment> T restoreEditor(Bundle savedState, int position, Class<T> type, Supplier<T> create) {
        String key = "resource_editor_" + position;
        Fragment restored = savedState != null && savedState.containsKey(key)
                ? getSupportFragmentManager().getFragment(savedState, key) : null;
        // Older saved states only contain the pager's fragment state, without our explicit references.
        if (restored == null) {
            for (Fragment fragment : getSupportFragmentManager().getFragments()) {
                if (type.isInstance(fragment)) {
                    restored = fragment;
                    break;
                }
            }
        }
        return restored == null ? create.get() : type.cast(restored);
    }

    private Fragment[] editors() {
        return new Fragment[]{stringsEditor, colorsEditor, stylesEditor, themesEditor, arraysEditor, dimensEditor};
    }

    private boolean areEditorsReady() {
        for (Fragment editor : editors()) {
            if (editor == null || !editor.isAdded() || editor.getActivity() != this || editor.getView() == null) return false;
        }
        return true;
    }

    private void initializeBackgroundTask(String variant) {
        if (this.variant != null && !this.variant.equals(variant)) pendingDrafts = null;
        this.variant = variant;
        stringsFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "strings.xml");
        colorsFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "colors.xml");
        stylesFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "styles.xml");
        themesFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "themes.xml");
        arrayFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "arrays.xml");
        dimensFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "dimens.xml");

        if (binding.viewPager.getAdapter() == null) setupViewPager();
        else updateTabTitles();
        startBackgroundTask();
    }

    private void setupListeners() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        binding.fab.setOnClickListener(view -> {
            if (!editorsLoaded) return;
            int currentItem = binding.viewPager.getCurrentItem();
            switch (currentItem) {
                case ResourceEditorTabs.STRINGS -> stringsEditor.showAddStringDialog();
                case ResourceEditorTabs.COLORS -> colorsEditor.showColorEditDialog(null, -1);
                case ResourceEditorTabs.STYLES -> stylesEditor.showAddStyleDialog();
                case ResourceEditorTabs.THEMES -> themesEditor.showAddThemeDialog();
                case ResourceEditorTabs.ARRAYS -> arraysEditor.showAddArrayDialog();
                case ResourceEditorTabs.DIMENS -> dimensEditor.showDimenEditDialog(null, -1);
            }
        });
    }

    private void startBackgroundTask() {
        initialLoadPending = true;
        editorsLoaded = false;
        showLoadingDialog();
        schedulePendingLoad();
    }

    private void schedulePendingLoad() {
        resourceHandler.removeCallbacks(loadEditorsTask);
        if ((!initialLoadPending && !sourceReloadPending) || isFinishing() || isDestroyed()
                || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED) || !areEditorsReady()) return;
        resourceHandler.post(loadEditorsTask);
    }

    private void loadPendingEditors() {
        if (isFinishing() || isDestroyed() || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)
                || !areEditorsReady()) return;
        if (initialLoadPending) loadDataInBackground();
        if (sourceReloadPending) reloadFromCodeEditor();
        initialLoadPending = false;
        sourceReloadPending = false;
        editorsLoaded = true;
        applyResourceSearch();
        dismissLoadingDialog();
    }

    private void loadDataInBackground() {
        if (!restoreDraft(stringsEditor, ResourceEditorTabs.STRINGS, stringsFilePath))
            stringsEditor.updateStringsList(stringsFilePath, 0, false);
        if (!restoreDraft(colorsEditor, ResourceEditorTabs.COLORS, colorsFilePath))
            colorsEditor.updateColorsList(colorsFilePath, 0, false);
        if (!restoreDraft(stylesEditor, ResourceEditorTabs.STYLES, stylesFilePath))
            stylesEditor.updateStylesList(stylesFilePath, 0, false);
        if (!restoreDraft(themesEditor, ResourceEditorTabs.THEMES, themesFilePath))
            themesEditor.updateThemesList(themesFilePath, 0, false);
        if (!restoreDraft(arraysEditor, ResourceEditorTabs.ARRAYS, arrayFilePath))
            arraysEditor.updateArraysList(arrayFilePath, 0, false);
        if (!restoreDraft(dimensEditor, ResourceEditorTabs.DIMENS, dimensFilePath))
            dimensEditor.updateDimensList(dimensFilePath, 0, false);
        pendingDrafts = null;
        checkForInvalidResources();
    }

    private boolean restoreDraft(ResourceEditorDraftState.Owner editor, int position, String filePath) {
        Bundle draft = pendingDrafts == null ? null : pendingDrafts.getBundle(Integer.toString(position));
        if (draft == null) return false;
        editor.restoreDraftState(draft, filePath);
        return true;
    }

    public void checkForInvalidResources() {
        if (!areEditorsReady() || isFinishing() || isDestroyed()) return;
        if (stringsEditor.stringsEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("strings.xml", stringsFilePath);
            return;
        }
        if (colorsEditor.colorsEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("colors.xml", colorsFilePath);
            return;
        }
        if (stylesEditor.stylesEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("styles.xml", stylesFilePath);
            return;
        }
        if (themesEditor.themesEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("themes.xml", themesFilePath);
            return;
        }

        if (arraysEditor.arraysEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("arrays.xml", arrayFilePath);
            return;
        }

        if (dimensEditor.dimensEditorManager != null && dimensEditor.dimensEditorManager.isDataLoadingFailed) {
            showLoadFailedDialog("dimens.xml", dimensFilePath);
        }
    }

    private void goToCodeEditor(String title, String contentPath) {
        builder = null;
        isComingFromSrcCodeEditor = true;
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), SrcCodeEditor.class);
        intent.putExtra("title", title);
        intent.putExtra("content", contentPath);
        startActivity(intent);
    }

    private void showLoadFailedDialog(String title, String contentPath) {
        if (builder != null) return;
        builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(Helper.getResString(R.string.resources_manager_xml_load_failed_title))
                .setMessage(String.format(Helper.getResString(R.string.resources_manager_xml_load_failed_message), title))
                .setPositiveButton(R.string.resources_open_code_editor, (dialog, which) -> goToCodeEditor(title, contentPath))
                .setNegativeButton(Helper.getResString(R.string.common_word_exit), (dialogInterface, i) -> finish())
                .setCancelable(false)
                .create()
                .show();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                ArrayList<String> unsavedFiles = getUnsavedFilesList();

                if (!unsavedFiles.isEmpty()) {
                    String files = formatUnsavedFilesList(unsavedFiles);
                    String message = String.format(Helper.getResString(R.string.resources_editor_unsaved_changes_message), files);

                    new MaterialAlertDialogBuilder(ResourcesEditorActivity.this)
                            .setTitle(Helper.getResString(R.string.common_word_warning))
                            .setMessage(message)
                            .setPositiveButton(Helper.getResString(R.string.design_quit_button_save_and_exit), (dialog, which) -> {
                                saveEditorsChanges();
                                finish();
                            })
                            .setNegativeButton(Helper.getResString(R.string.common_word_exit), (dialog, which) -> finish())
                            .setNeutralButton(Helper.getResString(R.string.common_word_cancel), null)
                            .create()
                            .show();
                    return;
                }

                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private String formatUnsavedFilesList(ArrayList<String> unsavedFiles) {
        if (unsavedFiles.size() == 1) {
            return unsavedFiles.get(0);
        } else if (unsavedFiles.size() == 2) {
            return String.join(" and ", unsavedFiles);
        } else {
            String lastFile = unsavedFiles.remove(unsavedFiles.size() - 1);
            return String.join(", ", unsavedFiles) + ", and " + lastFile;
        }
    }

    private ArrayList<String> getUnsavedFilesList() {
        ArrayList<String> unsavedFiles = new ArrayList<>();

        if (stringsEditor.hasUnsavedChanges) {
            unsavedFiles.add("strings.xml");
        }
        if (colorsEditor.hasUnsavedChanges) {
            unsavedFiles.add("colors.xml");
        }
        if (stylesEditor.hasUnsavedChanges) {
            unsavedFiles.add("styles.xml");
        }
        if (themesEditor.hasUnsavedChanges) {
            unsavedFiles.add("themes.xml");
        }
        if (arraysEditor.hasUnsavedChanges) {
            unsavedFiles.add("arrays.xml");
        }
        if (dimensEditor.hasUnsavedChanges) {
            unsavedFiles.add("dimens.xml");
        }
        return unsavedFiles;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isComingFromSrcCodeEditor) {
            sourceReloadPending = true;
            isComingFromSrcCodeEditor = false;
        }
        schedulePendingLoad();
    }

    private void applyResourceSearch() {
        if (!editorsLoaded) return;
        int currentItem = binding.viewPager.getCurrentItem();
        if (currentItem == ResourceEditorTabs.STRINGS) {
            stringsEditor.adapter.filter(resourceSearchQuery);
        } else if (currentItem == ResourceEditorTabs.COLORS) {
            colorsEditor.adapter.filter(resourceSearchQuery);
        } else if (currentItem == ResourceEditorTabs.STYLES) {
            stylesEditor.adapter.filter(resourceSearchQuery);
        } else if (currentItem == ResourceEditorTabs.THEMES) {
            themesEditor.adapter.filter(resourceSearchQuery);
        } else if (currentItem == ResourceEditorTabs.ARRAYS) {
            arraysEditor.adapter.filter(resourceSearchQuery);
        } else if (currentItem == ResourceEditorTabs.DIMENS) {
            if (dimensEditor.adapter != null) dimensEditor.adapter.filter(resourceSearchQuery);
        }
    }

    private void reloadFromCodeEditor() {
        int currentItem = binding.viewPager.getCurrentItem();

        if (currentItem == ResourceEditorTabs.STRINGS || stringsEditor.stringsEditorManager.isDataLoadingFailed) {
            stringsEditor.updateStringsList(stringsFilePath, 0, false);
        }

        if (currentItem == ResourceEditorTabs.COLORS || colorsEditor.colorsEditorManager.isDataLoadingFailed) {
            colorsEditor.updateColorsList(colorsFilePath, 0, false);
        }

        if (currentItem == ResourceEditorTabs.STYLES || stylesEditor.stylesEditorManager.isDataLoadingFailed) {
            stylesEditor.updateStylesList(stylesFilePath, 0, false);
        }

        if (currentItem == ResourceEditorTabs.THEMES || themesEditor.themesEditorManager.isDataLoadingFailed) {
            themesEditor.updateThemesList(themesFilePath, 0, false);
        }

        if (currentItem == ResourceEditorTabs.ARRAYS || arraysEditor.arraysEditorManager.isDataLoadingFailed) {
            arraysEditor.updateArraysList(arrayFilePath, 0, false);
        }

        if (currentItem == ResourceEditorTabs.DIMENS || (dimensEditor.dimensEditorManager != null && dimensEditor.dimensEditorManager.isDataLoadingFailed)) {
            dimensEditor.updateDimensList(dimensFilePath, 0, false);
        }
        checkForInvalidResources();
    }

    @Override
    protected void onStop() {
        resourceHandler.removeCallbacks(loadEditorsTask);
        dismissLoadingDialog();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        resourceHandler.removeCallbacks(loadEditorsTask);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(editorLifecycleCallbacks);
        if (tabMediator != null) tabMediator.detach();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.resources_editor_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    resourceSearchQuery = newText.toLowerCase().trim();
                    applyResourceSearch();
                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }
            });
            searchView.setQuery(resourceSearchQuery, false);
        }
        MenuItem getFromVariantItem = menu.findItem(R.id.action_get_from_variant);
        if (getFromVariantItem != null) {
            getFromVariantItem.setVisible(!variant.isEmpty());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (!editorsLoaded) return super.onOptionsItemSelected(item);
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveEditorsChanges();
        } else if (id == R.id.action_select_variant) {
            changeTheVariantDialog();
        } else if (id == R.id.action_get_from_variant) {
            importFromDefaultVariant();
        } else if (id != R.id.action_search) {
            int currentItem = binding.viewPager.getCurrentItem();
            switch (currentItem) {
                case ResourceEditorTabs.STRINGS -> {
                    stringsEditor.hasUnsavedChanges = true;
                    stringsEditor.saveStringsFile();
                    goToCodeEditor("strings.xml", stringsFilePath);
                }
                case ResourceEditorTabs.COLORS -> {
                    colorsEditor.hasUnsavedChanges = true;
                    colorsEditor.saveColorsFile();
                    goToCodeEditor("colors.xml", colorsFilePath);
                }
                case ResourceEditorTabs.STYLES -> {
                    stylesEditor.hasUnsavedChanges = true;
                    stylesEditor.saveStylesFile();
                    goToCodeEditor("styles.xml", stylesFilePath);
                }
                case ResourceEditorTabs.THEMES -> {
                    themesEditor.hasUnsavedChanges = true;
                    themesEditor.saveThemesFile();
                    goToCodeEditor("themes.xml", themesFilePath);
                }
                case ResourceEditorTabs.ARRAYS -> {
                    arraysEditor.hasUnsavedChanges = true;
                    arraysEditor.saveArraysFile();
                    goToCodeEditor("arrays.xml", arrayFilePath);
                }
                case ResourceEditorTabs.DIMENS -> {
                    dimensEditor.hasUnsavedChanges = true;
                    dimensEditor.saveDimensFile();
                    goToCodeEditor("dimens.xml", dimensFilePath);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_tab_position", binding.viewPager.getCurrentItem());
        outState.putString("resource_variant", variant);
        outState.putString("resource_search_query", resourceSearchQuery);
        outState.putBoolean("resource_source_reload", isComingFromSrcCodeEditor || sourceReloadPending);
        Bundle drafts = pendingDrafts == null ? new Bundle() : new Bundle(pendingDrafts);
        Fragment[] editors = editors();
        for (int i = 0; i < editors.length; i++) {
            if (editors[i].isAdded()) getSupportFragmentManager().putFragment(outState, "resource_editor_" + i, editors[i]);
            if (editorsLoaded) {
                Bundle draft = ((ResourceEditorDraftState.Owner) editors[i]).saveDraftState();
                if (draft != null) drafts.putBundle(Integer.toString(i), draft);
            }
        }
        outState.putBundle("resource_editor_drafts", drafts);
    }

    private void saveEditorsChanges() {
        stringsEditor.saveStringsFile();
        colorsEditor.saveColorsFile();
        stylesEditor.saveStylesFile();
        themesEditor.saveThemesFile();
        arraysEditor.saveArraysFile();
        dimensEditor.saveDimensFile();
        updateProjectMetadata();
        SketchwareUtil.toast(Helper.getResString(R.string.toast_save_completed));
    }

    private void updateProjectMetadata() {
        if (variant.isEmpty()) {
            HashMap<String, Object> metadata = projectFilePaths.metadata;

            for (ColorModel color : colorsEditor.colorList) {
                if (colorsEditor.defaultColors.containsKey(color.getColorName())) {
                    metadata.put(colorsEditor.defaultColors.get(color.getColorName()), PropertiesUtil.parseColor(colorsEditor.colorsEditorManager.getColorValue(getApplicationContext(), color.getColorValue(), 3)));
                }
            }

            for (HashMap<String, Object> map : stringsEditor.listmap) {
                Object key = map.get("key");
                if ("app_name".equals(String.valueOf(key))) {
                    Object text = map.get("text");
                    metadata.put("my_app_name", text != null ? text.toString() : "");
                }
            }

            ProjectListManager.saveProject(sc_id, metadata);
        }
    }

    private void setupViewPager() {
        EditorsAdapter adapter = new EditorsAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(currentTabPosition, false);

        tabMediator = new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(resourceTabTitle(position)));
        tabMediator.attach();
        UI.animateLayoutChanges(binding.viewPager);
    }

    private String resourceTabTitle(int position) {
        return switch (position) {
            case ResourceEditorTabs.STRINGS -> "strings";
            case ResourceEditorTabs.COLORS -> "colors";
            case ResourceEditorTabs.STYLES -> "styles";
            case ResourceEditorTabs.THEMES -> "themes";
            case ResourceEditorTabs.ARRAYS -> "arrays";
            case ResourceEditorTabs.DIMENS -> "dimens";
            default -> throw new IllegalArgumentException("Invalid resource tab: " + position);
        } + variant + ".xml";
    }

    private void updateTabTitles() {
        for (int i = 0; i < ResourceEditorTabs.COUNT; i++) {
            Objects.requireNonNull(binding.tabLayout.getTabAt(i)).setText(resourceTabTitle(i));
        }
        invalidateOptionsMenu();
    }

    private void changeTheVariantDialog() {
        ArrayList<String> unsavedFiles = getUnsavedFilesList();

        if (!unsavedFiles.isEmpty()) {
            String files = formatUnsavedFilesList(unsavedFiles);
            String message = String.format(Helper.getResString(R.string.resources_editor_unsaved_changes_message_variant_change), files);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(Helper.getResString(R.string.common_word_warning))
                    .setMessage(message)
                    .setPositiveButton(Helper.getResString(R.string.common_word_save), (dialog, which) -> {
                        saveEditorsChanges();
                        changeTheVariantDialog();
                    })
                    .setNegativeButton(Helper.getResString(R.string.common_word_cancel), null)
                    .create()
                    .show();
            return;
        }

        ArrayList<String> resourcesDir = new ArrayList<>();
        FileUtil.listDir(SketchwarePaths.getProjectResourcePath(sc_id), resourcesDir);

        ArrayList<String> variants = extractVariants(resourcesDir);
        AtomicInteger selectedChoice = new AtomicInteger(variants.indexOf("values" + variant));

        ResourcesVariantSelectorDialogBinding binding = ResourcesVariantSelectorDialogBinding.inflate(getLayoutInflater());
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_word_select_variant));

        setupDialog(binding, dialog, variants, selectedChoice);

        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.setView(binding.getRoot());
        dialog.show();
    }

    private void setupDialog(ResourcesVariantSelectorDialogBinding binding, MaterialAlertDialogBuilder dialog, ArrayList<String> variants, AtomicInteger selectedChoice) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, variants);
        binding.listView.setAdapter(adapter);
        binding.listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        binding.listView.setItemChecked(selectedChoice.get(), true);

        binding.listView.setOnItemClickListener((parent, view, which, id) -> {
            selectedChoice.set(which);
            binding.input.setText("");
            binding.input.clearFocus();
        });

        if (!(selectedChoice.get() >= 0 && selectedChoice.get() < variants.size() && variants.get(selectedChoice.get()).equals("values" + variant))) {
            binding.input.setText("values" + variant);
        }
        binding.input.setText(variantFullNameStarts);

        binding.input.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                String inputText = s.toString();
                if (!inputText.startsWith(variantFullNameStarts)) {
                    isUpdating = true;
                    binding.input.setText(variantFullNameStarts);
                    binding.input.setSelection(variantFullNameStarts.length());
                    isUpdating = false;
                } else {
                    String newText = inputText.trim();
                    if (!newText.equals(variantFullNameStarts)) {
                        for (int i = 0; i < binding.listView.getCount(); i++) {
                            binding.listView.setItemChecked(i, false);
                        }
                    } else {
                        if (selectedChoice.get() >= 0 && selectedChoice.get() < binding.listView.getCount()) {
                            binding.listView.setItemChecked(selectedChoice.get(), true);
                        }
                    }
                }
            }
        });

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (d, which) -> saveVariant(binding, variants, selectedChoice));
    }

    private void saveVariant(ResourcesVariantSelectorDialogBinding binding, ArrayList<String> variants, AtomicInteger selectedChoice) {
        currentTabPosition = this.binding.viewPager.getCurrentItem();
        String newVariant = Objects.requireNonNull(binding.input.getText()).toString().trim();
        if (!newVariant.equals(variantFullNameStarts)) {
            if (newVariant.startsWith(variantFullNameStarts)) {
                initializeBackgroundTask(newVariant.replace("values", ""));
            } else {
                SketchwareUtil.toastError(Helper.getResString(R.string.error_invalid_variant));
            }
        } else {
            initializeBackgroundTask(variants.get(selectedChoice.get()).replace("values", ""));
        }
    }

    public ArrayList<String> extractVariants(ArrayList<String> resourcesDir) {
        ArrayList<String> variants = new ArrayList<>();
        for (String dir : resourcesDir) {
            String regex = "(values(-[^/]+)*)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(dir);

            if (matcher.find()) {
                variants.add(matcher.group(1));
            }
        }
        return variants;
    }

    private void importFromDefaultVariant() {
        ResourcesEditorImportDialogBinding binding = ResourcesEditorImportDialogBinding.inflate(getLayoutInflater());
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setView(binding.getRoot());
        dialog.setTitle(Helper.getResString(R.string.common_word_import_variant));

        ArrayList<String> resourcesFileNames = new ArrayList<>(List.of(
                "strings.xml",
                "colors.xml",
                "styles.xml",
                "themes.xml",
                "arrays.xml",
                "dimens.xml"
        ));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, resourcesFileNames);

        ListView listView = binding.resourcesListView;
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (d, which) -> {
            SparseBooleanArray selectedItems = listView.getCheckedItemPositions();
            boolean isSkippingMode = binding.chipMergeAndSkip.isChecked();
            boolean isMergeAndReplace = binding.chipMergeAndReplace.isChecked();
            int updateMode = isMergeAndReplace ? 2 : isSkippingMode ? 1 : 0;
            for (int i = 0; i < resourcesFileNames.size(); i++) {
                if (selectedItems.get(i)) {
                    switch (i) {
                        case ResourceEditorTabs.STRINGS ->
                                stringsEditor.updateStringsList(stringsFilePath.replace(variant, ""), updateMode, true);
                        case ResourceEditorTabs.COLORS ->
                                colorsEditor.updateColorsList(colorsFilePath.replace(variant, ""), updateMode, true);
                        case ResourceEditorTabs.STYLES ->
                                stylesEditor.updateStylesList(stylesFilePath.replace(variant, ""), updateMode, true);
                        case ResourceEditorTabs.THEMES ->
                                themesEditor.updateThemesList(themesFilePath.replace(variant, ""), updateMode, true);
                        case ResourceEditorTabs.ARRAYS ->
                                arraysEditor.updateArraysList(arrayFilePath.replace(variant, ""), updateMode, true);
                        case ResourceEditorTabs.DIMENS ->
                                dimensEditor.updateDimensList(dimensFilePath.replace(variant, ""), updateMode, true);
                    }
                }
            }
        });

        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

}
