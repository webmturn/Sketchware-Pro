package pro.sketchware.activities.editor.manage.library;

import static pro.sketchware.core.library.LocalLibrariesUtil.buildEnabledRootDependencyState;
import static pro.sketchware.core.library.LocalLibrariesUtil.createLibraryMap;
import static pro.sketchware.core.library.LocalLibrariesUtil.getLocalLibraries;
import static pro.sketchware.core.library.LocalLibrariesUtil.rewriteLocalLibFile;
import static pro.sketchware.util.FileUtil.formatFileSize;
import static pro.sketchware.util.FileUtil.getFileSize;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.gson.Gson;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.R;
import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.core.library.DependencyTreeModel;
import pro.sketchware.core.library.DependencyTreeModel.Node;
import pro.sketchware.core.library.DependencyTreeModel.Row;
import pro.sketchware.core.library.LocalLibrariesUtil;
import pro.sketchware.core.library.LocalLibrary;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ActivitySubDependenciesBinding;
import pro.sketchware.databinding.DialogDependencyDetailsBinding;
import pro.sketchware.databinding.ItemLibraryDependencyBinding;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class SubDependenciesActivity extends BaseAppCompatActivity {
    public static final String EXTRA_ROOT_LIB_NAME = "root_lib_name";
    public static final String EXTRA_ROOT_COORDINATE = "root_coordinate";
    public static final String EXTRA_SC_ID = "sc_id";
    public static final String EXTRA_DEP_FOLDERS = "dep_folders";
    public static final String EXTRA_DEP_COORDS = "dep_coords";
    public static final String EXTRA_DEP_BUILTIN = "dep_builtin";
    public static final String EXTRA_DEP_DEPTHS = "dep_depths";
    public static final String EXTRA_DEP_PARENTS = "dep_parents";
    public static final String EXTRA_DEP_PARENT_LISTS = "dep_parent_lists";
    public static final String EXTRA_DEP_HIERARCHY_KNOWN = "dep_hierarchy_known";
    public static final String VIEW_PREFERENCES = "local_library_browser";
    public static final String LEVEL_VIEW_PREFERENCE = "level_view";

    private ActivitySubDependenciesBinding binding;
    private DialogDependencyDetailsBinding detailBinding;
    private BottomSheetDialog detailsDialog;
    private Row detailRow;
    private String scId, rootName, rootCoordinate;
    private ArrayList<HashMap<String, Object>> projectUsedLibs;
    private boolean notAssociatedWithProject, levelView, loaded;
    private final SubDepAdapter adapter = new SubDepAdapter();
    private final ExecutorService dependencyStateExecutor = Executors.newSingleThreadExecutor();
    private Map<String, List<String>> enabledRootLibrariesBySubDependency = Collections.emptyMap();
    private Map<String, List<String>> subDependenciesByEnabledRootLibrary = Collections.emptyMap();
    private boolean dependencyGuardReady = true;
    private DependencyTreeModel model;
    private final Set<String> expanded = new HashSet<>(), expandedGroups = new HashSet<>();
    private final ArrayList<String> browsePath = new ArrayList<>();
    private final ArrayList<String> focusedPath = new ArrayList<>();
    private String focusedCoordinate = "";
    private Parcelable restoredScroll;

    @Override public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivitySubDependenciesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UI.addSystemWindowInsetToPadding(binding.getRoot(), true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.toolbar, false, true, false, false);
        UI.addSystemWindowInsetToPadding(binding.dependenciesList, false, false, false, true);

        Intent intent = getIntent();
        rootName = intent.getStringExtra(EXTRA_ROOT_LIB_NAME);
        if (rootName == null) rootName = "";
        rootCoordinate = intent.getStringExtra(EXTRA_ROOT_COORDINATE);
        if (rootCoordinate == null) rootCoordinate = "";
        scId = intent.getStringExtra(EXTRA_SC_ID);
        notAssociatedWithProject = scId == null || scId.equals("system");
        dependencyGuardReady = notAssociatedWithProject;
        levelView = getSharedPreferences(VIEW_PREFERENCES, MODE_PRIVATE)
                .getBoolean(LEVEL_VIEW_PREFERENCE, false);
        if (savedInstanceState != null) {
            levelView = savedInstanceState.getBoolean("level_view", levelView);
            restoreList(savedInstanceState, "expanded", expanded);
            restoreList(savedInstanceState, "groups", expandedGroups);
            ArrayList<String> path = savedInstanceState.getStringArrayList("path");
            if (path != null) browsePath.addAll(path);
            focusedCoordinate = savedInstanceState.getString("focus", "");
            ArrayList<String> focusPath = savedInstanceState.getStringArrayList("focus_path");
            if (focusPath != null) focusedPath.addAll(focusPath);
            restoredScroll = savedInstanceState.getParcelable("scroll");
        }
        updateRootHeader();
        binding.toolbar.setNavigationOnClickListener(v -> goUpOrFinish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.dependency_switch_view) {
                switchDisplayMode();
                return true;
            }
            if (item.getItemId() == R.id.dependency_expand_all && loaded) {
                boolean collapse = allExpanded();
                expanded.clear();
                expandedGroups.clear();
                if (!collapse) {
                    expandedGroups.add(DependencyTreeModel.ROOT);
                    for (Node node : model.allNodes()) {
                        if (!model.children(node.coordinate).isEmpty()) {
                            expanded.add(node.coordinate);
                            expandedGroups.add(node.coordinate);
                        }
                    }
                }
                refreshRows();
                return true;
            }
            return false;
        });
        binding.dependencyUp.setOnClickListener(v -> goUpOrFinish());
        binding.dependencyPath.setOnClickListener(v -> choosePath());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goUpOrFinish(); }
        });
        binding.dependenciesList.setAdapter(adapter);
        binding.dependenciesList.addItemDecoration(new BranchDecoration());
        updateToolbar();
        if (!notAssociatedWithProject) {
            projectUsedLibs = getLocalLibraries(scId);
            preloadDependencyState();
        }
        loadDependencies(intent);
    }

    private static void restoreList(Bundle state, String key, Set<String> target) {
        ArrayList<String> values = state.getStringArrayList(key);
        if (values != null) target.addAll(values);
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putBoolean("level_view", levelView);
        state.putStringArrayList("expanded", new ArrayList<>(expanded));
        state.putStringArrayList("groups", new ArrayList<>(expandedGroups));
        state.putStringArrayList("path", new ArrayList<>(browsePath));
        state.putString("focus", focusedCoordinate);
        state.putStringArrayList("focus_path", new ArrayList<>(focusedPath));
        state.putParcelable("scroll", binding.dependenciesList.getLayoutManager().onSaveInstanceState());
        super.onSaveInstanceState(state);
    }

    @Override public void onDestroy() {
        if (detailsDialog != null) detailsDialog.dismiss();
        dependencyStateExecutor.shutdownNow();
        super.onDestroy();
    }

    private void loadDependencies(Intent intent) {
        ArrayList<String> folders = intent.getStringArrayListExtra(EXTRA_DEP_FOLDERS);
        ArrayList<String> coords = intent.getStringArrayListExtra(EXTRA_DEP_COORDS);
        ArrayList<String> parents = intent.getStringArrayListExtra(EXTRA_DEP_PARENTS);
        Bundle parentLists = intent.getBundleExtra(EXTRA_DEP_PARENT_LISTS);
        boolean[] builtIn = intent.getBooleanArrayExtra(EXTRA_DEP_BUILTIN);
        boolean[] known = intent.getBooleanArrayExtra(EXTRA_DEP_HIERARCHY_KNOWN);
        int[] depths = intent.getIntArrayExtra(EXTRA_DEP_DEPTHS);
        // Older callers identify the root through the direct dependencies' parent.
        if (rootCoordinate.isEmpty() && parents != null && depths != null) {
            for (int i = 0; i < Math.min(parents.size(), depths.length); i++) {
                if (depths[i] == 1 && parents.get(i) != null) {
                    rootCoordinate = parents.get(i);
                    break;
                }
            }
        }
        final String root = rootCoordinate;
        dependencyStateExecutor.execute(() -> {
            List<Node> entries = new ArrayList<>();
            if (folders != null) {
                for (int i = 0; i < folders.size(); i++) {
                    if (Thread.currentThread().isInterrupted()) return;
                    String folder = folders.get(i);
                    if (folder == null || folder.isEmpty()) continue;
                    String coord = coords != null && i < coords.size() && coords.get(i) != null ? coords.get(i) : folder;
                    boolean internal = builtIn != null && i < builtIn.length && builtIn[i];
                    List<String> incoming = parentLists != null ? parentLists.getStringArrayList(coord) : null;
                    if (incoming == null) incoming = new ArrayList<>();
                    else incoming = new ArrayList<>(incoming);
                    if (incoming.isEmpty() && parents != null && i < parents.size() && parents.get(i) != null) {
                        incoming.add(parents.get(i));
                    }
                    incoming.removeIf(value -> value == null || value.isEmpty());
                    boolean relationKnown = known != null && i < known.length ? known[i] : !incoming.isEmpty();
                    File dir = internal ? null : resolveLibDir(folder);
                    entries.add(new Node(coord, folder, internal, dir != null,
                            dir == null ? "" : formatFileSize(getFileSize(dir)), incoming, relationKnown));
                }
            }
            DependencyTreeModel result = new DependencyTreeModel(root, entries);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                model = result;
                loaded = true;
                updateRootHeader();
                browsePath.removeIf(id -> model.node(id) == null);
                focusedPath.removeIf(id -> model.node(id) == null);
                int internal = 0, local = 0, missing = 0;
                for (Node node : model.allNodes()) {
                    if (node.builtIn) internal++;
                    else if (node.available) local++;
                    else missing++;
                }
                String stats = getString(R.string.dependency_browser_summary, local, internal, missing);
                if (model.hasHierarchy()) {
                    stats = getString(R.string.dependency_browser_direct_count,
                            model.directCount(), model.allNodes().size()) + "\n" + stats;
                }
                binding.dependencySummary.setText(stats);
                binding.dependencyRelationshipNote.setVisibility(!entries.isEmpty()
                        && (!model.hasHierarchy() || model.isIncomplete()) ? View.VISIBLE : View.GONE);
                binding.dependencyRelationshipNote.setText(!model.hasHierarchy()
                        ? R.string.dependency_browser_flat_note : R.string.dependency_browser_incomplete_note);
                refreshRows();
                if (restoredScroll != null) {
                    binding.dependenciesList.getLayoutManager().onRestoreInstanceState(restoredScroll);
                    restoredScroll = null;
                }
            });
        });
    }

    private String currentParent() {
        return browsePath.isEmpty() ? "" : browsePath.get(browsePath.size() - 1);
    }

    private void switchDisplayMode() {
        levelView = !levelView;
        getSharedPreferences(VIEW_PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(LEVEL_VIEW_PREFERENCE, levelView).apply();
        if (loaded) {
            if (levelView && !focusedCoordinate.isEmpty()) {
                List<String> path = new ArrayList<>(focusedPath.isEmpty()
                        ? model.pathTo(focusedCoordinate) : focusedPath);
                if (model.children(focusedCoordinate).isEmpty() && !path.isEmpty()) path.remove(path.size() - 1);
                browsePath.clear();
                browsePath.addAll(path);
            }
            refreshRows();
            binding.dependenciesList.scrollToPosition(0);
        } else updateToolbar();
        SketchwareUtil.toast(getString(levelView ? R.string.dependency_browser_levels : R.string.dependency_browser_tree));
    }

    private void updateToolbar() {
        MenuItem toggle = binding.toolbar.getMenu().findItem(R.id.dependency_switch_view);
        int label = levelView ? R.string.dependency_browser_switch_tree : R.string.dependency_browser_switch_levels;
        toggle.setIcon(levelView ? R.drawable.ic_dependency_tree : R.drawable.ic_dependency_levels);
        toggle.setTitle(label);
        toggle.setContentDescription(getString(label));
        toggle.setTooltipText(getString(label));
        MenuItem expand = binding.toolbar.getMenu().findItem(R.id.dependency_expand_all);
        expand.setVisible(loaded && !levelView && model.allNodes().stream()
                .anyMatch(node -> node.builtIn || !model.children(node.coordinate).isEmpty()));
        expand.setTitle(loaded && allExpanded() ? R.string.dependency_browser_collapse_all : R.string.dependency_browser_expand_all);
    }

    private boolean allExpanded() {
        if (!loaded || !expandedGroups.contains("")) return false;
        for (Node node : model.allNodes()) {
            if (!model.children(node.coordinate).isEmpty()
                    && (!expanded.contains(node.coordinate) || !expandedGroups.contains(node.coordinate))) return false;
        }
        return true;
    }

    private void refreshRows() {
        if (!loaded) return;
        adapter.setItems(model.rows(browsePath, !levelView, expanded, expandedGroups));
        binding.noContentLayout.setVisibility(adapter.items.isEmpty() ? View.VISIBLE : View.GONE);
        String mode = getString(levelView ? R.string.dependency_browser_levels : R.string.dependency_browser_tree);
        int count = model.children(currentParent()).size();
        binding.dependencySectionTitle.setText(model.hasHierarchy() && !(browsePath.isEmpty() && model.isIncomplete())
                ? getString(R.string.dependency_browser_direct_section, mode, count)
                : getResources().getQuantityString(R.plurals.dependency_browser_section, count, mode, count));
        binding.dependencyPathBar.setVisibility(browsePath.isEmpty() ? View.GONE : View.VISIBLE);
        String path = readablePath(browsePath);
        binding.dependencyPath.setText(path);
        binding.dependencyPath.setContentDescription(getString(R.string.dependency_browser_choose_path) + ": " + path);
        updateToolbar();
        binding.dependenciesList.invalidateItemDecorations();
    }

    private String rootDisplayName() {
        String[] parts = rootCoordinate.split(":", -1);
        int marker = rootName.lastIndexOf("-v");
        return parts.length == 3 ? parts[1] : marker > 0 ? rootName.substring(0, marker) : rootName;
    }

    private void updateRootHeader() {
        String[] parts = rootCoordinate.split(":", -1);
        int marker = rootName.lastIndexOf("-v");
        String version = parts.length == 3 ? parts[2] : marker > 0 ? rootName.substring(marker + 2) : "";
        binding.rootLibraryName.setText(rootDisplayName() + (version.isEmpty() ? "" : "  " + version));
        binding.rootLibraryCoordinate.setText(parts.length == 3 ? parts[0] : rootCoordinate);
        binding.rootLibraryCoordinate.setVisibility(rootCoordinate.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String readablePath(List<String> path) {
        List<String> labels = new ArrayList<>();
        labels.add(rootDisplayName());
        for (String id : path) {
            Node node = model.node(id);
            if (node != null) labels.add(node.name);
        }
        return String.join(" → ", labels);
    }

    private void goUpOrFinish() {
        if (browsePath.isEmpty()) { finish(); return; }
        browsePath.remove(browsePath.size() - 1);
        focusedCoordinate = currentParent();
        focusedPath.clear();
        focusedPath.addAll(browsePath);
        refreshRows();
        binding.dependenciesList.scrollToPosition(0);
    }

    private void choosePath() {
        if (!loaded || browsePath.isEmpty()) return;
        List<String> labels = new ArrayList<>();
        labels.add(rootDisplayName());
        for (String id : browsePath) labels.add(model.node(id).name);
        new MaterialAlertDialogBuilder(this).setTitle(R.string.dependency_browser_choose_path)
                .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                    while (browsePath.size() > index) browsePath.remove(browsePath.size() - 1);
                    focusedCoordinate = currentParent();
                    focusedPath.clear();
                    focusedPath.addAll(browsePath);
                    refreshRows();
                    binding.dependenciesList.scrollToPosition(0);
                }).show();
    }

    private void clickRow(Row row) {
        if (row.isGroup()) {
            if (!expandedGroups.add(row.parent)) expandedGroups.remove(row.parent);
        } else {
            focusedCoordinate = row.node.coordinate;
            focusedPath.clear();
            focusedPath.addAll(row.path);
            if (row.reference || model.children(row.node.coordinate).isEmpty()) {
                showDetails(row);
                return;
            }
            if (levelView || row.depth >= 3) {
                browsePath.clear();
                browsePath.addAll(row.path);
                binding.dependenciesList.scrollToPosition(0);
            } else if (!expanded.add(row.node.coordinate)) expanded.remove(row.node.coordinate);
        }
        refreshRows();
    }

    private File resolveLibDir(String name) {
        File primary = new File(SketchwarePaths.getLocalLibsDir(), name);
        if (primary.isDirectory()) return primary;
        File fallback = new File(SketchwarePaths.getLocalLibsFallbackDir(), name);
        return fallback.isDirectory() ? fallback : null;
    }

    private String nodeState(Node node) {
        if (node.builtIn) return getString(R.string.library_built_in);
        if (!node.available) return getString(R.string.dependency_browser_missing);
        return getString(R.string.dependency_browser_local) + " · " + node.size;
    }

    private void showDetails(Row row) {
        if (row.isGroup()) return;
        focusedCoordinate = row.node.coordinate;
        focusedPath.clear();
        focusedPath.addAll(row.path);
        if (detailsDialog != null) detailsDialog.dismiss();
        detailRow = row;
        detailBinding = DialogDependencyDetailsBinding.inflate(getLayoutInflater());
        detailsDialog = new BottomSheetDialog(this);
        detailsDialog.setContentView(detailBinding.getRoot());
        detailBinding.dependencyDetailName.setText(row.node.name);
        detailBinding.dependencyDetailCoordinate.setText(row.node.coordinate);
        detailBinding.dependencyDetailState.setText(nodeState(row.node));
        detailBinding.dependencyDetailState.setTextColor(MaterialColors.getColor(detailBinding.getRoot(),
                !row.node.available && !row.node.builtIn ? androidx.appcompat.R.attr.colorError
                        : com.google.android.material.R.attr.colorOnSurfaceVariant));
        boolean knownPath = model.hasKnownPath(row.path);
        detailBinding.dependencyDetailPath.setText(knownPath ? readablePath(row.path)
                : getString(R.string.dependency_browser_unknown_path));
        detailBinding.dependencyDetailParents.setVisibility(row.node.parents.size() > 1 ? View.VISIBLE : View.GONE);
        detailBinding.dependencyDetailParents.setText(getString(R.string.dependency_browser_parents,
                String.join("\n", row.node.parents)));
        detailBinding.dependencyCopyCoordinate.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.dependency_browser_copy_coordinate),
                    row.node.coordinate));
            SketchwareUtil.toast(getString(R.string.dependency_browser_copied));
        });
        configureProjectControl(row.node);
        UI.addSystemWindowInsetToPadding(detailBinding.getRoot(), false, false, false, true);
        detailsDialog.setOnDismissListener(dialog -> {
            detailBinding = null;
            detailRow = null;
            detailsDialog = null;
        });
        detailsDialog.show();
    }

    private void configureProjectControl(Node node) {
        if (detailBinding == null) return;
        boolean visible = !notAssociatedWithProject && !node.builtIn;
        detailBinding.dependencyProjectEnabled.setVisibility(visible ? View.VISIBLE : View.GONE);
        detailBinding.dependencyProjectReason.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        List<String> requiredBy = getEnabledRootLibrariesDependingOn(node.folderName);
        boolean used = isUsedLibrary(node.folderName);
        detailBinding.dependencyProjectEnabled.setChecked(used);
        detailBinding.dependencyProjectEnabled.setEnabled(dependencyGuardReady && (used || node.available)
                && (!used || requiredBy.isEmpty()));
        String reason = !dependencyGuardReady ? getString(R.string.dependency_browser_checking)
                : !node.available && !used ? getString(R.string.dependency_browser_missing_reason)
                : !requiredBy.isEmpty() ? getString(R.string.dependency_browser_required, String.join("\n", requiredBy)) : "";
        detailBinding.dependencyProjectReason.setText(reason);
        detailBinding.dependencyProjectReason.setVisibility(reason.isEmpty() ? View.GONE : View.VISIBLE);
        detailBinding.dependencyProjectEnabled.setOnClickListener(v -> {
            boolean checked = detailBinding.dependencyProjectEnabled.isChecked();
            if (!toggleLibrary(checked, node.folderName)) detailBinding.dependencyProjectEnabled.setChecked(!checked);
            configureProjectControl(node);
        });
    }

    private boolean isUsedLibrary(String libraryName) {
        if (!notAssociatedWithProject && projectUsedLibs != null) {
            for (Map<String, Object> libraryMap : projectUsedLibs) {
                if (libraryName.equals(String.valueOf(libraryMap.get("name")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean toggleLibrary(boolean isChecked, String name) {
        if (projectUsedLibs == null) return false;
        if (!isChecked) {
            List<String> dependentRoots = getEnabledRootLibrariesDependingOn(name);
            if (!dependentRoots.isEmpty()) {
                showDependentRootLibrariesDialog(name, dependentRoots);
                return false;
            }
            projectUsedLibs.removeIf(m -> name.equals(String.valueOf(m.get("name"))));
        } else {
            if (!isUsedLibrary(name)) {
                projectUsedLibs.add(createLibraryMap(name, null));
            }
        }
        rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
        updateDependencyGuardCache(name, isChecked);
        return true;
    }

    private List<String> getEnabledRootLibrariesDependingOn(String subDependencyName) {
        List<String> dependentRoots = enabledRootLibrariesBySubDependency.get(subDependencyName);
        if (dependentRoots == null) {
            return Collections.emptyList();
        }
        return dependentRoots;
    }

    private void showDependentRootLibrariesDialog(String libraryName, List<String> dependentRoots) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.common_word_warning)
                .setMessage(getString(
                        R.string.sub_dependencies_disable_required_message,
                        libraryName,
                        String.join("\n", dependentRoots)))
                .setPositiveButton(R.string.common_word_ok, null)
                .show();
    }


    private class SubDepAdapter extends RecyclerView.Adapter<SubDepAdapter.ViewHolder> {
        private final List<Row> items = new ArrayList<>();
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemLibraryDependencyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Row row = items.get(position);
            ItemLibraryDependencyBinding item = holder.binding;
            ViewGroup.LayoutParams indent = item.dependencyIndent.getLayoutParams();
            indent.width = dp(row.depth * 16);
            item.dependencyIndent.setLayoutParams(indent);
            boolean expandable = row.isGroup() || !row.reference && !model.children(row.node.coordinate).isEmpty();
            boolean open = row.isGroup() ? expandedGroups.contains(row.parent) : expanded.contains(row.node.coordinate) && !levelView && row.depth < 3;
            item.dependencyExpandIcon.setVisibility(expandable ? View.VISIBLE : View.INVISIBLE);
            item.dependencyExpandIcon.setRotation(open ? 90 : 0);
            if (row.isGroup()) {
                item.dependencyName.setText(getString(R.string.dependency_browser_builtin_group, row.builtInCount));
                item.dependencyMetadata.setText(R.string.dependency_browser_builtin_provided);
                item.getRoot().setContentDescription(item.dependencyName.getText() + ", " +
                        getString(open ? R.string.dependency_browser_collapse_node : R.string.dependency_browser_expand_node,
                                getString(R.string.library_built_in)));
            } else {
                Node node = row.node;
                SpannableStringBuilder title = new SpannableStringBuilder(node.name);
                if (!node.version.isEmpty()) {
                    int start = title.length();
                    title.append("  ").append(node.version);
                    title.setSpan(new RelativeSizeSpan(0.75f), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    title.setSpan(new TypefaceSpan("sans-serif"), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    title.setSpan(new ForegroundColorSpan(MaterialColors.getColor(item.getRoot(),
                            com.google.android.material.R.attr.colorOnSurfaceVariant)), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                item.dependencyName.setText(title);
                List<String> metadata = new ArrayList<>();
                if (!node.group.isEmpty()) metadata.add(node.group);
                metadata.add(nodeState(node));
                if (row.reference) metadata.add(getString(R.string.dependency_browser_shared));
                if (model.hasHierarchy() && !model.hasKnownPath(row.path)) {
                    metadata.add(getString(R.string.dependency_browser_unattached));
                }
                if (!row.reference && expandable) {
                    int count = model.children(node.coordinate).size();
                    metadata.add(getResources().getQuantityString(R.plurals.dependency_browser_children, count, count));
                }
                SpannableStringBuilder description = new SpannableStringBuilder(String.join(" · ", metadata));
                if (!node.available && !node.builtIn) {
                    String missing = getString(R.string.dependency_browser_missing);
                    int start = description.toString().indexOf(missing);
                    if (start >= 0) description.setSpan(new ForegroundColorSpan(MaterialColors.getColor(item.getRoot(),
                            androidx.appcompat.R.attr.colorError)), start, start + missing.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                item.dependencyMetadata.setText(description);
                String action = expandable ? getString(levelView || row.depth >= 3 ? R.string.dependency_browser_enter_node
                        : open ? R.string.dependency_browser_collapse_node : R.string.dependency_browser_expand_node, node.name)
                        : getString(R.string.dependency_browser_details, node.name);
                item.getRoot().setContentDescription(node.coordinate + ", " + item.dependencyMetadata.getText() + ", " + action);
            }
            item.getRoot().setOnClickListener(v -> clickRow(row));
            item.getRoot().setOnLongClickListener(row.isGroup() ? null : v -> {
                showDetails(row);
                return true;
            });
            item.getRoot().setLongClickable(!row.isGroup());
            if (row.isGroup()) {
                ViewCompat.removeAccessibilityAction(item.getRoot(),
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK);
            } else {
                ViewCompat.replaceAccessibilityAction(item.getRoot(),
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                        getString(R.string.dependency_browser_details, row.node.name),
                        (view, arguments) -> view.performLongClick());
            }
        }
        @Override public int getItemCount() { return items.size(); }
        void setItems(List<Row> rows) { items.clear(); items.addAll(rows); notifyDataSetChanged(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            final ItemLibraryDependencyBinding binding;
            ViewHolder(ItemLibraryDependencyBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private class BranchDecoration extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        @Override public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView list, @NonNull RecyclerView.State state) {
            paint.setColor(MaterialColors.getColor(list, com.google.android.material.R.attr.colorOutlineVariant));
            paint.setStrokeWidth(dp(1));
            boolean rtl = list.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            for (int i = 0; i < list.getChildCount(); i++) {
                View view = list.getChildAt(i);
                int position = list.getChildAdapterPosition(view);
                if (position < 0 || position >= adapter.items.size()) continue;
                Row row = adapter.items.get(position);
                float middle = (view.getTop() + view.getBottom()) / 2f;
                float start = rtl ? view.getRight() - view.getPaddingStart() - dp(16)
                        : view.getLeft() + view.getPaddingStart() + dp(16);
                float direction = rtl ? -1 : 1;
                for (int j = 0; j < row.depth - 1; j++) {
                    if (j + 1 < row.ancestorLines.size() && row.ancestorLines.get(j + 1)) {
                        float x = start + direction * dp(j * 16);
                        canvas.drawLine(x, view.getTop(), x, view.getBottom(), paint);
                    }
                }
                if (row.depth > 0) {
                    float x = start + direction * dp((row.depth - 1) * 16);
                    canvas.drawLine(x, view.getTop(), x, row.last ? middle : view.getBottom(), paint);
                    canvas.drawLine(x, middle, x + direction * dp(9), middle, paint);
                }
                boolean open = row.isGroup() ? expandedGroups.contains(row.parent) :
                        !levelView && !row.reference && row.depth < 3 && expanded.contains(row.node.coordinate)
                                && !model.children(row.node.coordinate).isEmpty();
                if (open) {
                    float x = start + direction * dp(row.depth * 16);
                    canvas.drawLine(x, middle + dp(12), x, view.getBottom(), paint);
                }
            }
        }
    }

    private void preloadDependencyState() {
        dependencyStateExecutor.execute(() -> {
            LocalLibrariesUtil.EnabledRootDependencyState dependencyState =
                    buildEnabledRootDependencyState(projectUsedLibs);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                enabledRootLibrariesBySubDependency =
                        copyDependencyMap(dependencyState.getRootLibrariesBySubDependency());
                subDependenciesByEnabledRootLibrary =
                        copyDependencyMap(dependencyState.getSubDependenciesByRootLibrary());
                dependencyGuardReady = true;
                adapter.notifyDataSetChanged();
                if (detailsDialog != null && detailRow != null) configureProjectControl(detailRow.node);
            });
        });
    }

    private void updateDependencyGuardCache(String libraryName, boolean enabled) {
        List<String> subDependencies = enabled
                ? getRootLibrarySubDependencies(libraryName)
                : subDependenciesByEnabledRootLibrary.remove(libraryName);

        if (!enabled && subDependencies == null) {
            subDependencies = getRootLibrarySubDependencies(libraryName);
        }
        if (subDependencies == null || subDependencies.isEmpty()) {
            return;
        }

        if (enabled) {
            subDependenciesByEnabledRootLibrary.put(libraryName, new ArrayList<>(subDependencies));
            for (String subDependency : subDependencies) {
                List<String> dependentRoots = enabledRootLibrariesBySubDependency
                        .computeIfAbsent(subDependency, key -> new ArrayList<>());
                if (!dependentRoots.contains(libraryName)) {
                    dependentRoots.add(libraryName);
                }
            }
            return;
        }

        for (String subDependency : subDependencies) {
            List<String> dependentRoots = enabledRootLibrariesBySubDependency.get(subDependency);
            if (dependentRoots == null) {
                continue;
            }
            dependentRoots.remove(libraryName);
            if (dependentRoots.isEmpty()) {
                enabledRootLibrariesBySubDependency.remove(subDependency);
            }
        }
    }

    private List<String> getRootLibrarySubDependencies(String libraryName) {
        File libraryDir = resolveLibDir(libraryName);
        if (libraryDir == null) {
            return null;
        }

        LocalLibrary library = LocalLibrary.fromFile(libraryDir);
        if (!library.isRootLibrary()) {
            return null;
        }

        return new ArrayList<>(library.getSubDependencyNames());
    }

    private Map<String, List<String>> copyDependencyMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }
}
