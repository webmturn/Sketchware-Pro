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
import java.util.Map;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ref.WeakReference;

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
import pro.sketchware.activities.resourceseditor.components.fragments.ResourceEditorFragment;
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
    /** Valid legacy pages that are not yet covered by a durable commit; the only retry source. */
    private Bundle pendingDrafts;
    /** Raw legacy pages that could not be parsed. Kept as evidence, never migrated again. */
    private Bundle pendingCorruptDrafts;
    private String legacyCorruptMaterial;
    private boolean legacyRestoreIncomplete;
    private boolean legacyCorruptIncomplete;
    private boolean legacyBudgetFailureShown;
    private int legacyPagesRemaining;
    private boolean legacyNeedsProtect;
    private boolean legacyFailureShown;
    private ResourceEditorDraftStore currentStore;
    private ResourceEditorDraftController draftController;
    private ResourceEditorDraftState.Snapshot[] restoredDrafts;
    private String savedDraftSession;
    private int sourceEditorPage = -1;
    private boolean draftFailureShown;
    private boolean restoreReferenceUnsafe;
    private final Handler resourceHandler = new Handler(Looper.getMainLooper());
    private final Runnable loadEditorsTask = this::loadPendingEditors;
    private final Runnable prewriteDraftTask = this::prewriteDrafts;
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
            pendingCorruptDrafts = savedInstanceState.getBundle("resource_legacy_corrupt");
            legacyCorruptMaterial = savedInstanceState.getString("resource_legacy_material");
            if (legacyCorruptMaterial != null && !legacyCorruptMaterial.matches("[0-9a-f]{64}")) {
                legacyCorruptMaterial = null;
            }
            legacyRestoreIncomplete = savedInstanceState.getBoolean("resource_legacy_unprotected", false);
            legacyCorruptIncomplete = savedInstanceState.getBoolean("resource_legacy_material_unprotected", false);
            legacyPagesRemaining = pendingDrafts == null ? 0 : pendingDrafts.size();
            savedDraftSession = savedInstanceState.getString("resource_draft_session");
            sourceEditorPage = savedInstanceState.getInt("resource_source_page", -1);
            sourceReloadPending = savedInstanceState.getBoolean("resource_source_reload", false);
        }

        setupUI();
        initializeManagers();
        initializeEditors(savedInstanceState);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(editorLifecycleCallbacks, false);
        setupListeners();
        setupBackPressedHandler();
        initializeBackgroundTask(savedInstanceState == null ? "" : savedInstanceState.getString("resource_variant", ""));
        if (legacyRestoreIncomplete || legacyCorruptIncomplete) {
            SketchwareUtil.toastError(getString(R.string.resources_editor_legacy_restore_incomplete));
        }
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
        if (this.variant != null && !this.variant.equals(variant)) {
            pendingDrafts = null;
            pendingCorruptDrafts = null;
            legacyCorruptMaterial = null;
            legacyRestoreIncomplete = false;
            legacyCorruptIncomplete = false;
            legacyBudgetFailureShown = false;
            legacyPagesRemaining = 0;
            legacyNeedsProtect = false;
            savedDraftSession = null;
            restoredDrafts = null;
            restoreReferenceUnsafe = false;
            resourceHandler.removeCallbacks(prewriteDraftTask);
            draftController.detach(true);
            draftController = null;
        }
        this.variant = variant;
        stringsFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "strings.xml");
        colorsFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "colors.xml");
        stylesFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "styles.xml");
        themesFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "themes.xml");
        arrayFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "arrays.xml");
        dimensFilePath = SketchwarePaths.getProjectResourceValuesFilePath(sc_id, variant, "dimens.xml");
        if (draftController == null) openDraftSession();

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
                || !getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED) || !areEditorsReady()
                || draftController == null || !draftController.opening.isDone()
                || draftController.opening.isCompletedExceptionally()) return;
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
        onResourceDraftChanged();
    }

    private ResourceEditorDraftState.Owner[] draftOwners() {
        Fragment[] fragments = editors();
        ResourceEditorDraftState.Owner[] owners = new ResourceEditorDraftState.Owner[fragments.length];
        for (int i = 0; i < fragments.length; i++) owners[i] = (ResourceEditorDraftState.Owner) fragments[i];
        return owners;
    }

    private void openDraftSession() {
        draftController = new ResourceEditorDraftController(this, sc_id, variant, savedDraftSession,
                new String[]{stringsFilePath, colorsFilePath, stylesFilePath, themesFilePath, arrayFilePath, dimensFilePath},
                pendingDrafts);
        ResourceEditorDraftController controller = draftController;
        WeakReference<ResourcesEditorActivity> reference = new WeakReference<>(this);
        controller.opening.whenComplete((loaded, failure) -> new Handler(Looper.getMainLooper()).post(() -> {
            ResourcesEditorActivity activity = reference.get();
            if (activity == null || activity.isDestroyed() || activity.isFinishing() || activity.draftController != controller) return;
            if (failure != null) {
                pro.sketchware.util.LogUtil.e("ResourceDrafts", "Unable to restore draft", failure);
                SketchwareUtil.toastError(activity.getString(R.string.resources_editor_draft_restore_failed));
                controller.detach(false);
                activity.savedDraftSession = null;
                activity.openDraftSession();
            } else {
                activity.restoredDrafts = loaded.pages;
                activity.applyLegacyResult(loaded.legacy);
                activity.schedulePendingLoad();
            }
        }));
    }

    public void onResourceDraftChanged() {
        if (!editorsLoaded || isFinishing() || isDestroyed()) return;
        resourceHandler.removeCallbacks(prewriteDraftTask);
        resourceHandler.postDelayed(prewriteDraftTask, 250);
    }

    private void prewriteDrafts() {
        if (!editorsLoaded || isFinishing() || isDestroyed()) return;
        ResourceEditorDraftController controller = draftController;
        WeakReference<ResourcesEditorActivity> reference = new WeakReference<>(this);
        try {
            if (controller.isWriting()) {
                onResourceDraftChanged();
                return;
            }
            controller.prewrite(draftOwners()).whenComplete((unused, failure) -> new Handler(Looper.getMainLooper()).post(() -> {
                ResourcesEditorActivity activity = reference.get();
                if (activity == null || activity.isDestroyed() || activity.isFinishing() || activity.draftController != controller) return;
                if (failure != null) activity.reportDraftFailure();
                else activity.draftFailureShown = false;
            }));
        } catch (RuntimeException failure) { reportDraftFailure(); }
    }

    private void reportDraftFailure() {
        if (!draftFailureShown) SketchwareUtil.toastError(getString(R.string.resources_editor_draft_save_failed));
        draftFailureShown = true;
    }

    private boolean protectDrafts() {
        resourceHandler.removeCallbacks(prewriteDraftTask);
        if (checkpointDrafts()) {
            draftFailureShown = false;
            return true;
        }
        reportDraftFailure();
        return false;
    }

    /** A successful commit also makes every loaded legacy page durable. */
    private boolean checkpointDrafts() {
        if (draftController == null) return false;
        boolean protectedNow = draftController.checkpoint(draftOwners());
        if (protectedNow && editorsLoaded && legacyNeedsProtect) releaseLegacySource();
        return protectedNow;
    }

    private void releaseLegacySource() {
        if (pendingDrafts != null) {
            legacyRestoreIncomplete = false;
            legacyBudgetFailureShown = false;
        }
        pendingDrafts = null;
        legacyPagesRemaining = 0;
        legacyNeedsProtect = false;
    }

    private void applyLegacyResult(ResourceEditorDraftStore.LegacyImport result) {
        if (result == null) {
            persistCorruptMaterial();
            return;
        }
        boolean hadCorrupt = !result.corrupt.isEmpty();
        mergeCorrupt(result.corrupt);
        stripCorruptFromPending();
        if (persistCorruptMaterial()) result.corrupt.clear();
        if (result.state == ResourceEditorDraftStore.LEGACY_MIGRATED
                || result.state == ResourceEditorDraftStore.LEGACY_SUPERSEDED) {
            releaseLegacySource();
        } else if (result.state == ResourceEditorDraftStore.LEGACY_PARTIAL) {
            releaseLegacySource();
            if (hadCorrupt) {
                SketchwareUtil.toastError(getString(R.string.resources_editor_legacy_corrupt));
            }
        } else if (result.state == ResourceEditorDraftStore.LEGACY_DEFERRED
                || result.state == ResourceEditorDraftStore.LEGACY_FAILED) {
            if (result.state == ResourceEditorDraftStore.LEGACY_FAILED) reportLegacyFailure();
        }
    }

    private boolean persistCorruptMaterial() {
        if (pendingCorruptDrafts == null || pendingCorruptDrafts.isEmpty()) return true;
        if (draftController == null) return false;
        String material = draftController.archiveLegacyCorrupt(pendingCorruptDrafts);
        if (material == null) return false;
        legacyCorruptMaterial = material;
        pendingCorruptDrafts = null;
        legacyCorruptIncomplete = false;
        return true;
    }

    private void saveLegacyFallback(Bundle state) {
        persistCorruptMaterial();
        if (legacyCorruptMaterial != null) state.putString("resource_legacy_material", legacyCorruptMaterial);
        int omitted = ResourceEditorLegacyState.writeFallback(state, pendingDrafts, pendingCorruptDrafts);
        if (pendingDrafts != null && (omitted & ResourceEditorLegacyState.DRAFT_OMITTED) == 0) {
            legacyRestoreIncomplete = false;
        }
        if (pendingCorruptDrafts != null && (omitted & ResourceEditorLegacyState.CORRUPT_OMITTED) == 0) {
            legacyCorruptIncomplete = false;
        }
        if ((omitted & ResourceEditorLegacyState.DRAFT_OMITTED) != 0) legacyRestoreIncomplete = true;
        if ((omitted & ResourceEditorLegacyState.CORRUPT_OMITTED) != 0) legacyCorruptIncomplete = true;
        if (legacyRestoreIncomplete) state.putBoolean("resource_legacy_unprotected", true);
        if (legacyCorruptIncomplete) state.putBoolean("resource_legacy_material_unprotected", true);
        if (omitted != 0 && !legacyBudgetFailureShown) {
            SketchwareUtil.toastError(getString(R.string.resources_editor_legacy_state_too_large));
            legacyBudgetFailureShown = true;
        }
    }

    private void mergeCorrupt(Bundle corrupt) {
        if (corrupt == null || corrupt.isEmpty()) return;
        if (pendingCorruptDrafts == null) pendingCorruptDrafts = new Bundle();
        pendingCorruptDrafts.putAll(corrupt);
    }

    /** Corrupt pages are evidence only; retrying them can never succeed. */
    private void stripCorruptFromPending() {
        if (pendingDrafts == null || pendingCorruptDrafts == null || pendingCorruptDrafts.isEmpty()) {
            legacyPagesRemaining = pendingDrafts == null ? 0 : pendingDrafts.size();
            return;
        }
        Bundle retry = new Bundle();
        for (String key : pendingDrafts.keySet()) {
            if (!pendingCorruptDrafts.containsKey(key)) {
                retry.putBundle(key, pendingDrafts.getBundle(key));
            }
        }
        pendingDrafts = retry.isEmpty() ? null : retry;
        legacyPagesRemaining = pendingDrafts == null ? 0 : pendingDrafts.size();
    }

    private void reportLegacyFailure() {
        if (!legacyFailureShown) {
            SketchwareUtil.toastError(getString(R.string.resources_editor_legacy_migration_failed));
        }
        legacyFailureShown = true;
    }

    private void loadDataInBackground() {
        ResourceEditorDraftController.Loaded loaded = draftController.opening.join();
        ResourceEditorDraftStore store = loaded.store;
        currentStore = store;
        if (restoredDrafts == null) restoredDrafts = loaded.pages;
        legacyPagesRemaining = pendingDrafts == null ? 0 : pendingDrafts.size();
        Fragment[] fragments = editors();
        for (int i = 0; i < fragments.length; i++) {
            ((ResourceEditorFragment) fragments[i]).restoreDraftRevision(store.revision(i));
        }
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
        boolean xmlConflict = false;
        boolean[] committed = new boolean[ResourceEditorTabs.COUNT];
        for (int i = 0; i < committed.length; i++) committed[i] = store.wasXmlCommitted(i);
        for (boolean value : committed) xmlConflict |= value;
        if (xmlConflict) SketchwareUtil.toast(Helper.getResString(R.string.resources_editor_draft_xml_conflict_loaded));
        // Legacy is released only after a durable commit covers it, not merely because the UI
        // finished loading it. Until then the Bundle stays the retry source.
        if (pendingDrafts != null && legacyPagesRemaining == 0) legacyNeedsProtect = true;
        restoredDrafts = null;
        java.util.Arrays.fill(loaded.pages, null);
        checkForInvalidResources();
    }

    private boolean restoreDraft(ResourceEditorDraftState.Owner editor, int position, String filePath) {
        if (restoredDrafts != null && restoredDrafts[position] != null) {
            editor.restoreSnapshot(restoredDrafts[position], filePath);
            settleLegacyPage(position);
            return true;
        }
        // A proven XML commit outranks any legacy content on the same page.
        if (currentStore != null && currentStore.wasXmlCommitted(position)) {
            settleLegacyPage(position);
            return false;
        }
        Bundle draft = legacyDraft(position);
        if (draft == null) return false;
        try {
            editor.restoreDraftState(draft, filePath);
        } catch (RuntimeException failure) {
            pro.sketchware.util.LogUtil.w("ResourceDrafts", "Unreadable legacy draft page " + position, failure);
            if (pendingCorruptDrafts == null) pendingCorruptDrafts = new Bundle();
            pendingCorruptDrafts.putBundle(Integer.toString(position), new Bundle(draft));
            settleLegacyPage(position);
            pendingDrafts.remove(Integer.toString(position));
            return false;
        }
        settleLegacyPage(position);
        return true;
    }

    private Bundle legacyDraft(int position) {
        return pendingDrafts == null ? null : pendingDrafts.getBundle(Integer.toString(position));
    }

    private void settleLegacyPage(int position) {
        if (pendingDrafts != null && pendingDrafts.containsKey(Integer.toString(position))) {
            legacyPagesRemaining = Math.max(0, legacyPagesRemaining - 1);
        }
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

    private void requestSourceEditor(int page) {
        if (restoreReferenceUnsafe) {
            showUnsafeNavigationDialog(() -> attemptSave(() -> openSourceEditor(page)));
        } else openSourceEditor(page);
    }

    private void openSourceEditor(int page) {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] paths = {stringsFilePath, colorsFilePath, stylesFilePath, themesFilePath, arrayFilePath, dimensFilePath};
        ResourceEditorFragment editor = (ResourceEditorFragment) editors()[page];
        if (!editor.hasDraftChanges() && !new java.io.File(paths[page]).isFile()) {
            // A generated clean model still needs protection before creating a missing XML.
            editor.markDraftChanged();
        }
        if (!protectDrafts()) {
            // Explicit confirmation saves all XML through the existing intent/CLOSED protocol.
            // After rotation, no old dirty target remains to compete with source edits.
            showXmlOnlyDialog(() -> goToCodeEditor(names[page], paths[page]));
            return;
        }
        if (savePage(page) == ResourceEditorSaveResult.XmlWrite.FAILED) {
            SketchwareUtil.toastError(getString(R.string.resources_editor_xml_save_failed));
            return;
        }
        if (protectDrafts()) goToCodeEditor(names[page], paths[page]);
    }

    private void goToCodeEditor(String title, String contentPath) {
        builder = null;
        isComingFromSrcCodeEditor = true;
        String[] paths = {stringsFilePath, colorsFilePath, stylesFilePath, themesFilePath, arrayFilePath, dimensFilePath};
        for (int i = 0; i < paths.length; i++) if (paths[i].equals(contentPath)) sourceEditorPage = i;
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
                .setPositiveButton(R.string.resources_open_code_editor, (dialog, which) -> {
                    builder = null;
                    String[] paths = {stringsFilePath, colorsFilePath, stylesFilePath, themesFilePath, arrayFilePath, dimensFilePath};
                    for (int page = 0; page < paths.length; page++) {
                        if (paths[page].equals(contentPath)) { requestSourceEditor(page); break; }
                    }
                })
                .setNegativeButton(Helper.getResString(R.string.common_word_exit), (dialogInterface, i) -> finish())
                .setCancelable(false)
                .create()
                .show();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (restoreReferenceUnsafe) {
                    showUnsafeNavigationDialog(null);
                    return;
                }
                ArrayList<String> unsavedFiles = getUnsavedFilesList();

                if (!unsavedFiles.isEmpty()) {
                    String files = formatUnsavedFilesList(unsavedFiles);
                    String message = String.format(Helper.getResString(R.string.resources_editor_unsaved_changes_message), files);

                    new MaterialAlertDialogBuilder(ResourcesEditorActivity.this)
                            .setTitle(Helper.getResString(R.string.common_word_warning))
                            .setMessage(message)
                            .setPositiveButton(Helper.getResString(R.string.design_quit_button_save_and_exit), (dialog, which) -> saveAndExit())
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
        int currentItem = sourceEditorPage >= 0 ? sourceEditorPage : binding.viewPager.getCurrentItem();

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
        sourceEditorPage = -1;
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
        resourceHandler.removeCallbacks(prewriteDraftTask);
        if (draftController != null) draftController.detach(isFinishing());
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
            attemptSave(null);
        } else if (id == R.id.action_select_variant) {
            if (restoreReferenceUnsafe) {
                showUnsafeNavigationDialog(() -> attemptSave(this::changeTheVariantDialog));
                return true;
            }
            changeTheVariantDialog();
        } else if (id == R.id.action_get_from_variant) {
            if (restoreReferenceUnsafe) {
                showUnsafeNavigationDialog(() -> attemptSave(null));
                return true;
            }
            importFromDefaultVariant();
        } else if (id == R.id.action_open_editor) {
            requestSourceEditor(binding.viewPager.getCurrentItem());
            return true;
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
        outState.putInt("resource_source_page", sourceEditorPage);
        Fragment[] editors = editors();
        for (int i = 0; i < editors.length; i++) {
            if (editors[i].isAdded()) getSupportFragmentManager().putFragment(outState, "resource_editor_" + i, editors[i]);
        }
        if (editorsLoaded) {
            if (protectDrafts()) restoreReferenceUnsafe = false;
        } else if (pendingDrafts != null && !pendingDrafts.isEmpty() && draftController != null) {
            // Retry before writing state; a successful import releases the Bundle below.
            applyLegacyResult(draftController.retryLegacy(pendingDrafts));
        }
        boolean legacyPending = pendingDrafts != null && !pendingDrafts.isEmpty();
        if (draftController == null) {
            if (legacyPending && savedDraftSession != null) {
                outState.putString("resource_draft_session", savedDraftSession);
            }
            saveLegacyFallback(outState);
            return;
        }
        if (!legacyPending && draftController.hasCheckpoint()) {
            // Only a session with a durable manifest may be referenced as the recovery point.
            outState.putString("resource_draft_session", draftController.session);
        } else if (legacyPending && savedDraftSession != null && draftController.hasCheckpoint()) {
            // Dual source: keep the pre-existing session for its own pages, while the Bundle
            // remains the source for pages the session does not cover.
            outState.putString("resource_draft_session", savedDraftSession);
        } else if (!editorsLoaded && savedDraftSession != null) {
            outState.putString("resource_draft_session", savedDraftSession);
        }
        saveLegacyFallback(outState);
    }

    private ResourceEditorSaveResult.XmlWrite savePage(int page) {
        try {
            return switch (page) {
                case ResourceEditorTabs.STRINGS -> stringsEditor.saveStringsFile();
                case ResourceEditorTabs.COLORS -> colorsEditor.saveColorsFile();
                case ResourceEditorTabs.STYLES -> stylesEditor.saveStylesFile();
                case ResourceEditorTabs.THEMES -> themesEditor.saveThemesFile();
                case ResourceEditorTabs.ARRAYS -> arraysEditor.saveArraysFile();
                case ResourceEditorTabs.DIMENS -> dimensEditor.saveDimensFile();
                default -> throw new IllegalArgumentException("Invalid resource page");
            };
        } catch (RuntimeException failure) {
            pro.sketchware.util.LogUtil.e("ResourceEditor", "Unable to save resource page", failure);
            return ResourceEditorSaveResult.XmlWrite.FAILED;
        }
    }

    private ResourceEditorSaveResult saveEditorsChanges() {
        resourceHandler.removeCallbacks(prewriteDraftTask);
        if (!checkpointDrafts()) {
            reportDraftFailure();
            return ResourceEditorSaveResult.draftFailure();
        }
        restoreReferenceUnsafe = false;
        ResourceEditorSaveResult.XmlWrite[] pages = new ResourceEditorSaveResult.XmlWrite[ResourceEditorTabs.COUNT];
        for (int i = 0; i < pages.length; i++) pages[i] = savePage(i);
        boolean allXmlSaved = true;
        for (ResourceEditorSaveResult.XmlWrite page : pages) allXmlSaved &= page != ResourceEditorSaveResult.XmlWrite.FAILED;
        updateProjectMetadata(pages[ResourceEditorTabs.STRINGS] != ResourceEditorSaveResult.XmlWrite.FAILED,
                pages[ResourceEditorTabs.COLORS] != ResourceEditorSaveResult.XmlWrite.FAILED);
        boolean cleanConfirmed = checkpointDrafts();
        ResourceEditorSaveResult.DraftState draftState = cleanConfirmed && allXmlSaved
                ? ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED
                : ResourceEditorSaveResult.DraftState.CONTENT_PROTECTED;
        ResourceEditorSaveResult result = new ResourceEditorSaveResult(pages, draftState, true, false);
        if (result.failedPages > 0) SketchwareUtil.toastError(getString(R.string.resources_editor_xml_save_failed));
        else if (draftState == ResourceEditorSaveResult.DraftState.CLEAN_CONFIRMED)
            SketchwareUtil.toast(Helper.getResString(R.string.toast_save_completed));
        else SketchwareUtil.toast(Helper.getResString(R.string.resources_editor_xml_saved_restore_dirty));
        return result;
    }

    private void attemptSave(@Nullable Runnable onSuccess) {
        ResourceEditorSaveResult result = saveEditorsChanges();
        if (result.canSaveXmlOnly) {
            showXmlOnlyDialog(onSuccess);
            return;
        }
        if (result.navigationAllowed) {
            restoreReferenceUnsafe = false;
            if (onSuccess != null) onSuccess.run();
        }
    }

    private void saveAndExit() {
        if (restoreReferenceUnsafe) {
            showUnsafeNavigationDialog(null);
            return;
        }
        attemptSave(() -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void showUnsafeNavigationDialog(@Nullable Runnable retrySuccess) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.resources_editor_unsafe_navigation_title))
                .setMessage(getString(R.string.resources_editor_unsafe_navigation_message))
                .setPositiveButton(getString(R.string.resources_editor_unsafe_retry), (dialog, which) -> {
                    if (retrySuccess != null) retrySuccess.run();
                    else attemptSave(() -> {
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .setNegativeButton(getString(R.string.resources_editor_unsafe_discard), (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .setNeutralButton(getString(R.string.common_word_cancel), null)
                .setCancelable(false)
                .show();
    }

    private void showXmlOnlyDialog(@Nullable Runnable onSuccess) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.resources_editor_draft_xml_only_title))
                .setMessage(getString(R.string.resources_editor_draft_xml_only_message))
                .setPositiveButton(getString(R.string.resources_editor_draft_xml_only_confirm),
                        (dialog, which) -> performXmlOnlySave(onSuccess))
                .setNegativeButton(getString(R.string.common_word_cancel), null)
                .setCancelable(false)
                .show();
    }

    /**
     * Explicit degraded path: write the project XML, then close the old draft session so it cannot
     * restore older content. Only completes when every requested XML was written.
     */
    private void performXmlOnlySave(@Nullable Runnable onSuccess) {
        resourceHandler.removeCallbacks(prewriteDraftTask);
        String[] xml = new String[ResourceEditorTabs.COUNT];
        Map<Integer, String> hashes = new HashMap<>();
        try {
            for (int i = 0; i < xml.length; i++) {
                if (!((ResourceEditorFragment) editors()[i]).hasDraftChanges()) continue;
                xml[i] = renderPageXml(i);
                hashes.put(i, sha256(xml[i]));
            }
        } catch (RuntimeException failure) {
            restoreReferenceUnsafe = true;
            SketchwareUtil.toastError(getString(R.string.resources_editor_xml_prepare_failed));
            return;
        }
        if (!hashes.isEmpty() && !draftController.writeXmlCommitIntent(hashes)) {
            restoreReferenceUnsafe = true;
            SketchwareUtil.toastError(getString(R.string.resources_editor_xml_intent_failed));
            return;
        }
        ResourceEditorSaveResult.XmlWrite[] pages = new ResourceEditorSaveResult.XmlWrite[ResourceEditorTabs.COUNT];
        for (int i = 0; i < pages.length; i++) {
            pages[i] = xml[i] == null ? savePage(i) : writePageXml(i, xml[i]);
        }
        updateProjectMetadata(pages[ResourceEditorTabs.STRINGS] != ResourceEditorSaveResult.XmlWrite.FAILED,
                pages[ResourceEditorTabs.COLORS] != ResourceEditorSaveResult.XmlWrite.FAILED);
        boolean allXmlSaved = true;
        for (ResourceEditorSaveResult.XmlWrite page : pages) allXmlSaved &= page != ResourceEditorSaveResult.XmlWrite.FAILED;
        if (!allXmlSaved) {
            restoreReferenceUnsafe = true;
            SketchwareUtil.toastError(getString(R.string.resources_editor_xml_partial_saved));
            return;
        }
        if (!draftController.discardSession()) {
            restoreReferenceUnsafe = true;
            SketchwareUtil.toastError(getString(R.string.resources_editor_xml_saved_restore_unsafe));
            return;
        }
        draftController = null;
        savedDraftSession = null;
        restoredDrafts = null;
        pendingDrafts = null;
        legacyPagesRemaining = 0;
        legacyNeedsProtect = false;
        restoreReferenceUnsafe = false;
        draftFailureShown = false;
        openDraftSession();
        SketchwareUtil.toast(Helper.getResString(R.string.resources_editor_xml_saved_draft_discarded));
        if (onSuccess != null) onSuccess.run();
    }

    private String renderPageXml(int page) {
        return ((ResourceEditorFragment) editors()[page]).renderXml();
    }

    private ResourceEditorSaveResult.XmlWrite writePageXml(int page, String xml) {
        ResourceEditorFragment editor = (ResourceEditorFragment) editors()[page];
        return editor.writePreparedXml(editor.resourcePath(), xml);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private void updateProjectMetadata(boolean stringsSaved, boolean colorsSaved) {
        if (variant.isEmpty()) {
            HashMap<String, Object> metadata = projectFilePaths.metadata;

            if (colorsSaved) for (ColorModel color : colorsEditor.colorList) {
                if (colorsEditor.defaultColors.containsKey(color.getColorName())) {
                    metadata.put(colorsEditor.defaultColors.get(color.getColorName()), PropertiesUtil.parseColor(colorsEditor.colorsEditorManager.getColorValue(getApplicationContext(), color.getColorValue(), 3)));
                }
            }

            if (stringsSaved) for (HashMap<String, Object> map : stringsEditor.listmap) {
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
        if (restoreReferenceUnsafe) {
            showUnsafeNavigationDialog(() -> attemptSave(this::changeTheVariantDialog));
            return;
        }
        ArrayList<String> unsavedFiles = getUnsavedFilesList();

        if (!unsavedFiles.isEmpty()) {
            String files = formatUnsavedFilesList(unsavedFiles);
            String message = String.format(Helper.getResString(R.string.resources_editor_unsaved_changes_message_variant_change), files);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(Helper.getResString(R.string.common_word_warning))
                    .setMessage(message)
                    .setPositiveButton(Helper.getResString(R.string.common_word_save), (dialog, which) -> {
                        attemptSave(this::changeTheVariantDialog);
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
        if (restoreReferenceUnsafe) {
            showUnsafeNavigationDialog(() -> attemptSave(this::changeTheVariantDialog));
            return;
        }
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
