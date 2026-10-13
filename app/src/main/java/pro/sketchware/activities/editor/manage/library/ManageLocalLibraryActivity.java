package pro.sketchware.activities.editor.manage.library;

import static pro.sketchware.core.library.LocalLibrariesUtil.createLibraryMap;
import static pro.sketchware.core.library.LocalLibrariesUtil.deleteSelectedLocalLibraries;
import static pro.sketchware.core.library.LocalLibrariesUtil.getAllLocalLibraries;
import static pro.sketchware.core.library.LocalLibrariesUtil.getLocalLibFile;
import static pro.sketchware.core.library.LocalLibrariesUtil.getLocalLibraries;
import static pro.sketchware.core.library.LocalLibrariesUtil.rewriteLocalLibFile;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.activities.editor.manage.library.downloader.LibraryDownloaderDialogFragment;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.util.UIHelper;
import pro.sketchware.core.build.BuildSettings;
import pro.sketchware.core.project.ProjectSettings;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageLocallibrariesBinding;
import pro.sketchware.databinding.ViewItemLocalLibBinding;
import pro.sketchware.core.library.LocalLibrariesUtil;
import pro.sketchware.core.library.LocalLibrary;
import pro.sketchware.core.library.LocalLibraryImportPackageIndex;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class ManageLocalLibraryActivity extends BaseAppCompatActivity {
    private final LibraryAdapter adapter = new LibraryAdapter();
    private ArrayList<HashMap<String, Object>> projectUsedLibs;
    private boolean notAssociatedWithProject;
    private boolean contextualToolbarExpanded;
    private MenuItem searchMenuItem;
    private SearchView librarySearchView;
    private String librarySearchQuery = "";
    private BuildSettings buildSettings;
    private ManageLocallibrariesBinding binding;
    private ProjectSettings projectSettings;
    private String scId;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageLocallibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UI.addWindowInsetToMargin(binding.libraryToolbar, WindowInsetsCompat.Type.displayCutout(), true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.contextualToolbarContainer, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.librariesList, true, false, true, true);
        UI.addWindowInsetToMargin(binding.downloadLibraryButton, WindowInsetsCompat.Type.systemBars(), false, false, false, true);

        scId = getIntent().getStringExtra("sc_id");
        if (scId == null) scId = "system";
        buildSettings = new BuildSettings(scId);
        notAssociatedWithProject = scId.equals("system");
        if (!notAssociatedWithProject) {
            projectSettings = new ProjectSettings(scId);
        }

        adapter.setOnLocalLibrarySelectedStateChangedListener(item -> {
            long selectedItemCount = getSelectedLocalLibrariesCount();
            if (selectedItemCount > 0 && adapter.isSelectionModeEnabled) {
                binding.contextualToolbar.setTitle(String.valueOf(selectedItemCount));
                expandContextualToolbar();
            } else {
                adapter.isSelectionModeEnabled = false;
                collapseContextualToolbar();
            }
        });

        binding.librariesList.setAdapter(adapter);

        binding.libraryToolbar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.libraryToolbar.inflateMenu(R.menu.menu_search_bar_local_libraries);
        searchMenuItem = binding.libraryToolbar.getMenu().findItem(R.id.action_search_local_libraries);
        librarySearchView = (SearchView) searchMenuItem.getActionView();
        librarySearchView.setMaxWidth(Integer.MAX_VALUE);
        librarySearchView.setQueryHint(getString(R.string.local_library_manager_search));
        librarySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return true; }
            @Override public boolean onQueryTextChange(String query) {
                librarySearchQuery = query.trim();
                adapter.filter(librarySearchQuery);
                return true;
            }
        });
        if (savedInstanceState != null && savedInstanceState.getBoolean("library_search_expanded")) {
            searchMenuItem.expandActionView();
            librarySearchView.setQuery(savedInstanceState.getString("library_search_query", ""), false);
        }
        MenuItem manifestMergeMenuItem = binding.libraryToolbar.getMenu().findItem(R.id.action_toggle_local_library_manifest_merge);
        if (manifestMergeMenuItem != null) {
            manifestMergeMenuItem.setVisible(!notAssociatedWithProject);
            manifestMergeMenuItem.setChecked(isLocalLibraryManifestMergeEnabled());
        }
        binding.libraryToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_manage_repositories) {
                showManageRepositoriesDialog();
                return true;
            } else if (item.getItemId() == R.id.action_clean_orphans) {
                showOrphanCleanupDialog();
                return true;
            } else if (item.getItemId() == R.id.action_rebuild_import_indices) {
                rebuildAllImportIndices();
                return true;
            } else if (item.getItemId() == R.id.action_toggle_local_library_manifest_merge) {
                toggleLocalLibraryManifestMerge(item);
                return true;
            }
            return false;
        });

        binding.contextualToolbar.setNavigationOnClickListener(v -> hideContextualToolbarAndClearSelection());
        binding.contextualToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_select_all) {
                setLocalLibrariesSelected(true);
                binding.contextualToolbar.setTitle(String.valueOf(getSelectedLocalLibrariesCount()));
                return true;
            } else if (id == R.id.action_delete_selected_local_libraries) {
                showLoadingDialog();
                executorService.execute(() -> {
                    // Step 1: smart deletion — returns sub-deps kept because shared by other libs
                    List<String> retained = deleteSelectedLocalLibraries(
                            scId, adapter.getLocalLibraries(), projectUsedLibs);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        dismissLoadingDialog();
                        adapter.isSelectionModeEnabled = false;
                        runLoadLocalLibrariesTask();
                        collapseContextualToolbar();
                        if (retained.isEmpty()) {
                            SketchwareUtil.toast(Helper.getResString(R.string.toast_deleted_successfully));
                        } else {
                            new MaterialAlertDialogBuilder(ManageLocalLibraryActivity.this)
                                    .setTitle(Helper.getResString(R.string.toast_deleted_successfully))
                                    .setMessage(Helper.getResString(R.string.library_shared_dependencies_retained, retained.size(), String.join("\n", retained)))
                                    .setPositiveButton(Helper.getResString(R.string.common_word_ok), null)
                                    .show();
                        }
                    });
                });

                return true;
            }
            return false;
        });

        binding.downloadLibraryButton.setOnClickListener(v -> {
            if (getSupportFragmentManager().findFragmentByTag("library_downloader_dialog") != null) {
                return;
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean("notAssociatedWithProject", notAssociatedWithProject);
            bundle.putSerializable("buildSettings", buildSettings);
            bundle.putString("localLibFile", getLocalLibFile(scId).getAbsolutePath());

            LibraryDownloaderDialogFragment fragment = new LibraryDownloaderDialogFragment();
            fragment.setArguments(bundle);
            fragment.setOnLibraryDownloadedTask(this::runLoadLocalLibrariesTask);
            fragment.show(getSupportFragmentManager(), "library_downloader_dialog");
        });

        runLoadLocalLibrariesTask();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (contextualToolbarExpanded) {
                    hideContextualToolbarAndClearSelection();
                } else if (searchMenuItem.isActionViewExpanded()) {
                    searchMenuItem.collapseActionView();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("library_search_expanded", searchMenuItem.isActionViewExpanded());
        outState.putString("library_search_query", librarySearchQuery);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh after returning from SubDependenciesActivity (user may have toggled sub-deps)
        if (!notAssociatedWithProject) {
            projectUsedLibs = getLocalLibraries(scId);
            adapter.notifyDataSetChanged();
        }
    }

    private void runLoadLocalLibrariesTask() {
        showLoadingDialog();
        new LoadLocalLibrariesTask(this).execute();
    }

    private List<LocalLibrary> getAdapterLocalLibraries() {
        return adapter.getLocalLibraries();
    }

    private void hideContextualToolbarAndClearSelection() {
        adapter.isSelectionModeEnabled = false;
        collapseContextualToolbar();
        setLocalLibrariesSelected(false);
    }

    public void setLocalLibrariesSelected(boolean selected) {
        for (LocalLibrary library : selected ? adapter.getVisibleLibraries() : getAdapterLocalLibraries()) {
            library.setSelected(selected);
        }
        adapter.notifyDataSetChanged();
    }

    private void expandContextualToolbar() {
        contextualToolbarExpanded = true;
        binding.contextualToolbarContainer.setVisibility(View.VISIBLE);
        binding.libraryToolbar.setVisibility(View.INVISIBLE);
        binding.librarySummary.setVisibility(View.INVISIBLE);
        librarySearchView.clearFocus();
        android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
    }

    private void collapseContextualToolbar() {
        contextualToolbarExpanded = false;
        binding.contextualToolbarContainer.setVisibility(View.GONE);
        binding.libraryToolbar.setVisibility(View.VISIBLE);
        binding.librarySummary.setVisibility(View.VISIBLE);
    }

    private long getSelectedLocalLibrariesCount() {
        long count = 0;
        for (LocalLibrary library : getAdapterLocalLibraries()) {
            if (library.isSelected()) {
                count++;
            }
        }
        return count;
    }

    private boolean isLocalLibraryManifestMergeEnabled() {
        return projectSettings != null
                && ProjectSettings.SETTING_GENERIC_VALUE_TRUE.equals(projectSettings.getValue(
                ProjectSettings.SETTING_ENABLE_LOCAL_LIBRARY_MANIFEST_MERGE,
                ProjectSettings.SETTING_GENERIC_VALUE_FALSE));
    }

    private void toggleLocalLibraryManifestMerge(MenuItem item) {
        if (projectSettings == null) {
            return;
        }

        boolean enabled = !item.isChecked();
        item.setChecked(enabled);
        projectSettings.setValue(
                ProjectSettings.SETTING_ENABLE_LOCAL_LIBRARY_MANIFEST_MERGE,
                enabled ? ProjectSettings.SETTING_GENERIC_VALUE_TRUE : ProjectSettings.SETTING_GENERIC_VALUE_FALSE);
        SketchwareUtil.toast(Helper.getResString(enabled
                ? R.string.local_library_manager_manifest_merge_enabled
                : R.string.local_library_manager_manifest_merge_disabled));
    }

    // This method is running from the background thread.
    // So, every UI operation must be called inside `runOnUiThread`.
    private void loadLibraries() {
        var localLibraries = getAllLocalLibraries();
        if (!notAssociatedWithProject) {
            projectUsedLibs = getLocalLibraries(scId);
        }

        //This code helps in sorting the list of local libraries to display enabled libraries first.
        localLibraries.sort((lib1, lib2) -> {
            boolean isEnabled1 = isUsedLibrary(lib1.getName());
            boolean isEnabled2 = isUsedLibrary(lib2.getName());
            return Boolean.compare(isEnabled2, isEnabled1);
        });

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            adapter.setLocalLibraries(localLibraries);
        });
    }

    private boolean isUsedLibrary(String libraryName) {
        if (!notAssociatedWithProject) {
            for (Map<String, Object> libraryMap : projectUsedLibs) {
                if (libraryName.equals(String.valueOf(libraryMap.get("name")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleLibrary(boolean isChecked, String name) {
        LocalLibrary lib = findLibraryByName(name);
        if (!isChecked) {
            // Remove the library itself
            projectUsedLibs.removeIf(m -> name.equals(String.valueOf(m.get("name"))));
            // Step 4: cascade disable — remove exclusive sub-deps
            if (lib != null && lib.isRootLibrary()) {
                Set<String> sharedByOthers = getSubDepsSharedByOtherEnabledLibs(name);
                for (String subDep : lib.getSubDependencyNames()) {
                    if (!sharedByOthers.contains(subDep)) {
                        projectUsedLibs.removeIf(m -> subDep.equals(String.valueOf(m.get("name"))));
                    }
                }
            }
        } else {
            // Add the library itself (avoid duplicate)
            if (!isUsedLibrary(name)) {
                String dependency = null;
                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (name.equals(String.valueOf(libraryMap.get("name")))) {
                        Object dependencyValue = libraryMap.get("dependency");
                        dependency = dependencyValue != null ? dependencyValue.toString() : null;
                        break;
                    }
                }
                projectUsedLibs.add(createLibraryMap(name, dependency));
            }
            // Step 4: cascade enable — also enable missing sub-deps
            if (lib != null && lib.isRootLibrary()) {
                for (String subDep : LocalLibrariesUtil.getMissingSubDeps(lib, projectUsedLibs)) {
                    projectUsedLibs.add(createLibraryMap(subDep, null));
                }
            }
        }
        rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
        // Visually update the list so cascade-toggled sub-deps reflect their new state
        adapter.notifyDataSetChanged();
    }

    /** Step 2: Shows a dialog listing orphaned libraries with option to delete them. */
    private void showOrphanCleanupDialog() {
        List<LocalLibrary> orphans = LocalLibrariesUtil.getOrphanLibraries();
        if (orphans.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_orphan_libs_title)
                    .setMessage(R.string.dialog_orphan_libs_none)
                    .setPositiveButton(R.string.common_word_ok, null)
                    .show();
            return;
        }
        StringBuilder names = new StringBuilder();
        for (LocalLibrary lib : orphans) names.append(lib.getName()).append('\n');
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_orphan_libs_title)
                .setMessage(Helper.getResString(R.string.dialog_orphan_libs_body)
                        + "\n\n" + names)
                .setPositiveButton(R.string.common_word_delete, (d, w) -> {
                    showLoadingDialog();
                    executorService.execute(() -> {
                        for (LocalLibrary lib : orphans) {
                            lib.setSelected(true);
                        }
                        deleteSelectedLocalLibraries(scId, new ArrayList<>(orphans), projectUsedLibs);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            dismissLoadingDialog();
                            SketchwareUtil.toast(Helper.getResString(R.string.toast_deleted_successfully));
                            runLoadLocalLibrariesTask();
                        });
                    });
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void rebuildAllImportIndices() {
        showLoadingDialog();
        executorService.execute(() -> {
            List<LocalLibrary> libraries = adapter.getLocalLibraries();
            int rebuilt = 0;
            for (LocalLibrary lib : libraries) {
                File dir = LocalLibrariesUtil.getLocalLibraryDirectory(lib.getName());
                if (new File(dir, "classes.jar").isFile()) {
                    LocalLibraryImportPackageIndex.rebuildPackages(dir);
                    rebuilt++;
                }
            }
            int count = rebuilt;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                dismissLoadingDialog();
                SketchwareUtil.toast(String.format(
                        Helper.getResString(R.string.dialog_rebuild_indices_done), count));
            });
        });
    }

    /** Opens a new screen listing the sub-dependencies of a root library. */
    private void showSubDependencies(LocalLibrary library) {
        Intent intent = new Intent(this, SubDependenciesActivity.class);
        intent.putExtra(SubDependenciesActivity.EXTRA_ROOT_LIB_NAME, library.getName());
        intent.putExtra(SubDependenciesActivity.EXTRA_ROOT_COORDINATE, library.getCoordinate());
        intent.putExtra(SubDependenciesActivity.EXTRA_SC_ID, scId);

        List<LocalLibrary.DepInfo> allDeps = library.getAllDependencies();
        ArrayList<String> folders = new ArrayList<>(allDeps.size());
        ArrayList<String> coords = new ArrayList<>(allDeps.size());
        boolean[] builtIn = new boolean[allDeps.size()];
        int[] depths = new int[allDeps.size()];
        ArrayList<String> parents = new ArrayList<>(allDeps.size());
        Bundle parentLists = new Bundle();
        boolean[] hierarchyKnown = new boolean[allDeps.size()];
        for (int i = 0; i < allDeps.size(); i++) {
            LocalLibrary.DepInfo dep = allDeps.get(i);
            folders.add(dep.folderName);
            coords.add(dep.groupId + ":" + dep.artifactId + ":" + dep.version);
            builtIn[i] = dep.builtIn;
            depths[i] = dep.depth;
            parents.add(dep.parentCoord);
            parentLists.putStringArrayList(coords.get(i), new ArrayList<>(dep.parentCoords));
            hierarchyKnown[i] = dep.hierarchyKnown;
        }
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_FOLDERS, folders);
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_COORDS, coords);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_BUILTIN, builtIn);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_DEPTHS, depths);
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_PARENTS, parents);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_PARENT_LISTS, parentLists);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_HIERARCHY_KNOWN, hierarchyKnown);
        startActivity(intent);
    }

    /** Returns the library object with the given name, or null if not found. */
    @Nullable
    private LocalLibrary findLibraryByName(String name) {
        for (LocalLibrary lib : adapter.getLocalLibraries()) {
            if (lib.getName().equals(name)) return lib;
        }
        return null;
    }

    /**
     * Returns the set of sub-dependency names that are also referenced by other
     * currently-enabled root libraries (excluding the one being disabled).
     */
    private Set<String> getSubDepsSharedByOtherEnabledLibs(String excludingLibName) {
        Set<String> shared = new java.util.HashSet<>();
        for (LocalLibrary other : adapter.getLocalLibraries()) {
            if (!other.isRootLibrary()) continue;
            if (other.getName().equals(excludingLibName)) continue;
            if (!isUsedLibrary(other.getName())) continue;
            shared.addAll(other.getSubDependencyNames());
        }
        return shared;
    }

    public interface OnLocalLibrarySelectedStateChangedListener {
        void invoke(LocalLibrary library);
    }

    private static class LoadLocalLibrariesTask {
        private final WeakReference<ManageLocalLibraryActivity> activity;

        public LoadLocalLibrariesTask(ManageLocalLibraryActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        public void execute() {
            var act = activity.get();
            if (act == null) return;
            BackgroundTasks.runSerialIfAlive(TaskHost.of(act), "ManageLocalLibraryActivity$LoadLocalLibrariesTask",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        private void onError(Throwable error) {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        private void doWork() {
            var act = activity.get();
            if (act == null) return;
            try {
                act.loadLibraries();
            } catch (Exception e) {
                Log.e("ManageLocalLibraryActivity", e.getMessage(), e);
            }
        }
    }

    public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {
        private final List<LocalLibrary> allLocalLibraries = new ArrayList<>();
        private final List<LocalLibrary> localLibraries = new ArrayList<>();
        /** Step 6: names of libraries that have version conflicts (multiple versions of same artifact). */
        private final Set<String> conflictingLibNames = new HashSet<>();
        public boolean isSelectionModeEnabled;
        private @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener;

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ViewItemLocalLibBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            var binding = holder.binding;
            var library = localLibraries.get(position);

            SpannableStringBuilder title = new SpannableStringBuilder(library.getName());
            String[] coordinate = library.getCoordinate().split(":", -1);
            if (coordinate.length == 3 && !coordinate[0].isEmpty() && !coordinate[1].isEmpty()
                    && !coordinate[2].isEmpty()
                    && library.getName().equals(coordinate[1] + "-v" + coordinate[2])) {
                title.clear();
                title.append(coordinate[1]);
                int start = title.length();
                title.append("  ").append(coordinate[2]);
                title.setSpan(new RelativeSizeSpan(13f / 16), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setSpan(new TypefaceSpan("sans-serif"), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                title.setSpan(new ForegroundColorSpan(MaterialColors.getColor(binding.card,
                        com.google.android.material.R.attr.colorOnSurfaceVariant)), start, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            binding.libraryName.setText(title);
            List<String> metadata = new ArrayList<>();
            metadata.add(library.getSize());
            int depCount = library.getAllDependencies().size();
            if (depCount > 0) metadata.add(getResources().getQuantityString(
                    R.plurals.local_library_manager_dependencies, depCount, depCount));
            if (conflictingLibNames.contains(library.getName())) metadata.add(getString(R.string.local_library_manager_conflict));
            List<String> displayMetadata = new ArrayList<>();
            for (String part : metadata) displayMetadata.add(unbrokenMetadata(part));
            binding.librarySize.setText(String.join(" ·\u00a0", displayMetadata));
            binding.card.setContentDescription(library.getName() + ", " + String.join(", ", metadata));
            bindSelectedState(binding.card, library);

            View.OnLongClickListener enterSelectionMode = v -> {
                isSelectionModeEnabled = true;
                toggleLocalLibrary(binding.card, library, onLocalLibrarySelectedStateChangedListener);
                return true;
            };

            View.OnClickListener openSubDeps = v -> {
                if (isSelectionModeEnabled) {
                    toggleLocalLibrary(binding.card, library, onLocalLibrarySelectedStateChangedListener);
                } else {
                    showSubDependencies(library);
                }
            };
            binding.card.setOnClickListener(openSubDeps);
            binding.card.setOnLongClickListener(enterSelectionMode);
            ViewCompat.replaceAccessibilityAction(binding.card,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                    getString(R.string.local_library_manager_select, library.getName()),
                    (view, arguments) -> view.performLongClick());

            boolean projectControl = !notAssociatedWithProject && !isSelectionModeEnabled;
            binding.libraryControls.setVisibility(isSelectionModeEnabled || projectControl || depCount > 0 ? View.VISIBLE : View.GONE);
            binding.libraryChevron.setVisibility(!isSelectionModeEnabled && !projectControl && depCount > 0 ? View.VISIBLE : View.GONE);
            binding.librarySelected.setVisibility(isSelectionModeEnabled
                    ? library.isSelected() ? View.VISIBLE : View.INVISIBLE : View.GONE);
            binding.materialSwitch.setVisibility(projectControl ? View.VISIBLE : View.GONE);
            binding.materialSwitch.setEnabled(projectControl);
            binding.materialSwitch.setChecked(isUsedLibrary(library.getName()));
            binding.materialSwitch.setContentDescription(getString(R.string.local_library_manager_use_in_project, library.getName()));
            binding.materialSwitch.setOnClickListener(null);
            if (projectControl) {
                binding.materialSwitch.setOnClickListener(v -> onItemClicked(binding, library.getName()));
            }
        }

        @Override
        public int getItemCount() {
            return localLibraries.isEmpty() ? 0 : localLibraries.size();
        }

        public void setOnLocalLibrarySelectedStateChangedListener(
                @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener) {
            this.onLocalLibrarySelectedStateChangedListener = onLocalLibrarySelectedStateChangedListener;
        }

        private void toggleLocalLibrary(MaterialCardView card, LocalLibrary library,
                                        @Nullable OnLocalLibrarySelectedStateChangedListener onLocalLibrarySelectedStateChangedListener) {
            library.setSelected(!library.isSelected());
            bindSelectedState(card, library);
            if (onLocalLibrarySelectedStateChangedListener != null) {
                onLocalLibrarySelectedStateChangedListener.invoke(library);
            }
            notifyDataSetChanged();
            if (library.isSelected() && isUsedLibrary(library.getName())) {
                new MaterialAlertDialogBuilder(ManageLocalLibraryActivity.this)
                        .setTitle(R.string.common_word_warning)
                        .setMessage(String.format(Helper.getResString(R.string.library_remove_warning_msg), library.getName()))
                        .setPositiveButton(Helper.getResString(R.string.common_word_yes), (dialog, which) -> dialog.dismiss())
                        .setNegativeButton(Helper.getResString(R.string.common_word_cancel), (dialog, which) -> {
                            toggleLocalLibrary(card, library, onLocalLibrarySelectedStateChangedListener);
                            dialog.dismiss();
                        })
                        .show();
            }
        }

        private void bindSelectedState(MaterialCardView card, LocalLibrary library) {
            card.setCheckable(true);
            card.setChecked(isSelectionModeEnabled && library.isSelected());
            card.setCheckable(isSelectionModeEnabled);
            card.setCardBackgroundColor(isSelectionModeEnabled && library.isSelected()
                    ? MaterialColors.getColor(card, com.google.android.material.R.attr.colorSecondaryContainer)
                    : Color.TRANSPARENT);
        }

        private void onItemClicked(ViewItemLocalLibBinding binding, String name) {
            toggleLibrary(binding.materialSwitch.isChecked(), name);
        }

        public List<LocalLibrary> getLocalLibraries() {
            return allLocalLibraries;
        }

        public List<LocalLibrary> getVisibleLibraries() {
            return localLibraries;
        }

        public void setLocalLibraries(List<LocalLibrary> localLibraries) {
            allLocalLibraries.clear();
            allLocalLibraries.addAll(localLibraries);
            // Step 6: detect version conflicts (multiple libs with same artifact ID)
            conflictingLibNames.clear();
            Map<String, List<String>> artifactToLibs = new HashMap<>();
            for (LocalLibrary lib : localLibraries) {
                String artifactId = extractArtifactId(lib.getName());
                artifactToLibs.computeIfAbsent(artifactId, k -> new ArrayList<>()).add(lib.getName());
            }
            for (List<String> versions : artifactToLibs.values()) {
                if (versions.size() > 1) conflictingLibNames.addAll(versions);
            }
            filter(librarySearchQuery);
        }

        public void filter(String query) {
            String search = query.toLowerCase(Locale.ROOT);
            localLibraries.clear();
            for (LocalLibrary library : allLocalLibraries) {
                if (library.getName().toLowerCase(Locale.ROOT).contains(search)
                        || library.getCoordinate().toLowerCase(Locale.ROOT).contains(search)) {
                    localLibraries.add(library);
                }
            }
            notifyDataSetChanged();
            int count = allLocalLibraries.size();
            binding.librarySummary.setText(query.isEmpty() ? getResources().getQuantityString(
                    R.plurals.local_library_manager_count, count,
                    getString(notAssociatedWithProject ? R.string.local_library_manager_scope_global
                            : R.string.local_library_manager_scope_project), count)
                    : getString(R.string.local_library_manager_search_count, localLibraries.size(), count));
            boolean noMatches = !query.isEmpty() && count > 0;
            binding.libraryEmptyTitle.setText(noMatches ? R.string.local_library_manager_no_matches
                    : R.string.local_library_manager_no_libraries_title);
            binding.libraryEmptyBody.setText(noMatches ? R.string.local_library_manager_no_matches_body
                    : R.string.local_library_manager_no_libraries_body);
            binding.noContentLayout.setVisibility(localLibraries.isEmpty() ? View.VISIBLE : View.GONE);
        }

        /**
         * Strips the "-v{version}" suffix from a library folder name to get the artifact ID.
         * E.g. "okhttp-android-v5.1.0" → "okhttp-android".
         */
        private static String extractArtifactId(String libName) {
            int vIdx = libName.lastIndexOf("-v");
            if (vIdx > 0 && vIdx + 2 < libName.length()
                    && Character.isDigit(libName.charAt(vIdx + 2))) {
                return libName.substring(0, vIdx);
            }
            return libName;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ViewItemLocalLibBinding binding;

            public ViewHolder(@NonNull ViewItemLocalLibBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    /** Keep each short metadata value together when a row wraps on narrow screens. */
    private static String unbrokenMetadata(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) result.append('\u2060');
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    // ==================== Repository Management ====================

    private Runnable repoDialogRefresh;

    private static final String REPOSITORIES_JSON_PATH = SketchwarePaths.getRepositoriesJsonPath();

    private static final String[] BUILTIN_REPOS = {
            "Maven Central", "Google Maven", "Jitpack", "Sonatype Snapshots"
    };

    private ArrayList<HashMap<String, Object>> loadCustomRepos() {
        File file = new File(REPOSITORIES_JSON_PATH);
        if (!file.exists()) {
            // Seed with DependencyResolver's defaults so they aren't lost
            // if the user adds a custom repo before ever downloading a library
            seedDefaultRepos(file);
        }
        if (file.exists()) {
            try {
                ArrayList<HashMap<String, Object>> list = new Gson().fromJson(
                        pro.sketchware.util.FileUtil.readFile(file.getAbsolutePath()),
                        Helper.TYPE_MAP_LIST);
                if (list != null) return list;
            } catch (Exception e) {
                Log.e("ManageLocalLibrary", "Failed to parse repositories.json", e);
            }
        }
        return new ArrayList<>();
    }

    private void seedDefaultRepos(File file) {
        ArrayList<HashMap<String, Object>> defaults = new ArrayList<>();
        String[][] defaultEntries = {
                {"HortanWorks", "https://repo.hortonworks.com/content/repositories/releases"},
                {"Atlassian", "https://maven.atlassian.com/content/repositories/atlassian-public"},
                {"JCenter", "https://jcenter.bintray.com"},
                {"Sonatype", "https://oss.sonatype.org/content/repositories/releases"},
                {"Spring Plugins", "https://repo.spring.io/plugins-release"},
                {"Spring Milestone", "https://repo.spring.io/libs-milestone"},
                {"Apache Maven", "https://repo.maven.apache.org/maven2"},
        };
        for (String[] entry : defaultEntries) {
            HashMap<String, Object> repo = new HashMap<>();
            repo.put("name", entry[0]);
            repo.put("url", entry[1]);
            defaults.add(repo);
        }
        file.getParentFile().mkdirs();
        pro.sketchware.util.FileUtil.writeFile(file.getAbsolutePath(), new Gson().toJson(defaults));
    }

    private void saveCustomRepos(ArrayList<HashMap<String, Object>> repos) {
        File file = new File(REPOSITORIES_JSON_PATH);
        file.getParentFile().mkdirs();
        pro.sketchware.util.FileUtil.writeFile(file.getAbsolutePath(), new Gson().toJson(repos));
    }

    private void showManageRepositoriesDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_repositories, null);
        TextView builtinRepos = dialogView.findViewById(R.id.builtin_repos);
        LinearLayout customReposContainer = dialogView.findViewById(R.id.custom_repos_container);
        TextView emptyMessage = dialogView.findViewById(R.id.empty_message);
        ImageButton btnAdd = dialogView.findViewById(R.id.btn_add_repo);

        // Show built-in repos
        StringBuilder builtinText = new StringBuilder();
        for (String name : BUILTIN_REPOS) {
            if (builtinText.length() > 0) builtinText.append('\n');
            builtinText.append("• ").append(name);
        }
        builtinRepos.setText(builtinText);

        // Wrap in ScrollView for large lists
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_repo_title)
                .setView(scrollView)
                .setPositiveButton(R.string.common_word_ok, null)
                .create();

        // Populate custom repos
        Runnable refreshCustomRepos = () -> {
            customReposContainer.removeAllViews();
            ArrayList<HashMap<String, Object>> repos = loadCustomRepos();
            emptyMessage.setVisibility(repos.isEmpty() ? View.VISIBLE : View.GONE);
            for (int i = 0; i < repos.size(); i++) {
                final int index = i;
                HashMap<String, Object> repo = repos.get(i);
                String name = repo.get("name") instanceof String ? (String) repo.get("name") : "";
                String url = repo.get("url") instanceof String ? (String) repo.get("url") : "";

                View itemView = LayoutInflater.from(this).inflate(R.layout.item_repository, customReposContainer, false);
                ((TextView) itemView.findViewById(R.id.repo_name)).setText(name);
                ((TextView) itemView.findViewById(R.id.repo_url)).setText(url);

                itemView.findViewById(R.id.btn_edit).setOnClickListener(v ->
                        showAddEditRepoDialog(name, url, index));
                itemView.findViewById(R.id.btn_delete).setOnClickListener(v ->
                        showDeleteRepoDialog(name, index));

                customReposContainer.addView(itemView);
            }
        };
        refreshCustomRepos.run();
        repoDialogRefresh = refreshCustomRepos;

        btnAdd.setOnClickListener(v -> showAddEditRepoDialog("", "", -1));

        dialog.setOnDismissListener(d -> repoDialogRefresh = null);
        dialog.show();
    }

    private void refreshRepoDialog() {
        if (repoDialogRefresh != null) {
            repoDialogRefresh.run();
        }
    }

    private void showAddEditRepoDialog(String currentName, String currentUrl, int editIndex) {
        boolean isEdit = editIndex >= 0;

        // Build dialog with two TextInputLayouts
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, (int) (16 * getResources().getDisplayMetrics().density), padding, 0);

        TextInputLayout nameLayout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        nameLayout.setHint(getString(R.string.dialog_repo_name_hint));
        nameLayout.setPlaceholderText(getString(R.string.dialog_repo_name_placeholder));
        TextInputEditText nameInput = new TextInputEditText(nameLayout.getContext());
        nameInput.setText(currentName);
        nameInput.setSingleLine(true);
        nameLayout.addView(nameInput);

        TextInputLayout urlLayout = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        urlLayout.setHint(getString(R.string.dialog_repo_url_hint));
        urlLayout.setPlaceholderText(getString(R.string.dialog_repo_url_placeholder));
        TextInputEditText urlInput = new TextInputEditText(urlLayout.getContext());
        urlInput.setText(currentUrl);
        urlInput.setSingleLine(true);
        urlLayout.addView(urlInput);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        layout.addView(nameLayout, params);
        layout.addView(urlLayout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        androidx.appcompat.app.AlertDialog addEditDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? R.string.dialog_repo_edit_title : R.string.dialog_repo_add_title)
                .setView(layout)
                .setPositiveButton(R.string.common_word_save, null)
                .setNegativeButton(R.string.common_word_cancel, null)
                .create();

        addEditDialog.show();

        // Override positive button to prevent auto-dismiss on validation failure
        addEditDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            nameLayout.setError(null);
            urlLayout.setError(null);

            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";

            if (name.isEmpty()) {
                nameLayout.setError(getString(R.string.dialog_repo_error_name_required));
                return;
            }
            if (url.isEmpty()) {
                urlLayout.setError(getString(R.string.dialog_repo_error_url_required));
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                urlLayout.setError(getString(R.string.dialog_repo_error_url_invalid));
                return;
            }
            // Remove trailing slash
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

            ArrayList<HashMap<String, Object>> repos = loadCustomRepos();
            HashMap<String, Object> entry = new HashMap<>();
            entry.put("name", name);
            entry.put("url", url);

            if (isEdit && editIndex < repos.size()) {
                repos.set(editIndex, entry);
            } else {
                repos.add(entry);
            }
            saveCustomRepos(repos);
            refreshRepoDialog();
            addEditDialog.dismiss();
        });
    }

    private void showDeleteRepoDialog(String repoName, int index) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(String.format(getString(R.string.dialog_repo_delete_confirm), repoName))
                .setPositiveButton(R.string.common_word_delete, (d, w) -> {
                    ArrayList<HashMap<String, Object>> repos = loadCustomRepos();
                    if (index < repos.size()) {
                        repos.remove(index);
                        saveCustomRepos(repos);
                        refreshRepoDialog();
                    }
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    @Override
    public void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
