package dev.aldi.sayuti.editor.manage;

import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.deleteSelectedLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getAllLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibFile;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.rewriteLocalLibFile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.card.MaterialCardView;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.core.BaseAsyncTask;
import pro.sketchware.core.UIHelper;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ManageLocallibrariesBinding;
import pro.sketchware.databinding.ViewItemLocalLibBinding;
import pro.sketchware.databinding.ViewItemLocalLibSearchBinding;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class ManageLocalLibraryActivity extends BaseAppCompatActivity {
    private final LibraryAdapter adapter = new LibraryAdapter();
    private final SearchAdapter searchAdapter = new SearchAdapter();
    private ArrayList<HashMap<String, Object>> projectUsedLibs;
    private boolean notAssociatedWithProject;
    private boolean searchBarExpanded;
    private BuildSettings buildSettings;
    private ManageLocallibrariesBinding binding;
    private String scId;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ManageLocallibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UI.addWindowInsetToMargin(binding.searchBar, WindowInsetsCompat.Type.displayCutout(), true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.contextualToolbarContainer, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.librariesList, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.searchList, true, false, true, true);
        UI.addWindowInsetToMargin(binding.downloadLibraryButton, WindowInsetsCompat.Type.systemBars(), false, false, false, true);

        if (getIntent().hasExtra("sc_id")) {
            scId = Objects.requireNonNull(getIntent().getStringExtra("sc_id"));
            buildSettings = new BuildSettings(scId);
            notAssociatedWithProject = scId.equals("system");
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
        binding.searchList.setAdapter(searchAdapter);

        binding.searchBar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // Step 2: orphan cleanup menu on SearchBar (available in both project and global view)
        binding.searchBar.inflateMenu(R.menu.menu_search_bar_local_libraries);
        binding.searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_manage_repositories) {
                showManageRepositoriesDialog();
                return true;
            } else if (item.getItemId() == R.id.action_clean_orphans) {
                showOrphanCleanupDialog();
                return true;
            } else if (item.getItemId() == R.id.action_rebuild_import_indices) {
                rebuildAllImportIndices();
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
                        dismissLoadingDialog();
                        adapter.isSelectionModeEnabled = false;
                        runLoadLocalLibrariesTask();
                        collapseContextualToolbar();
                        if (retained.isEmpty()) {
                            SketchwareUtil.toast(Helper.getResString(R.string.toast_deleted_successfully));
                        } else {
                            new MaterialAlertDialogBuilder(ManageLocalLibraryActivity.this)
                                    .setTitle(Helper.getResString(R.string.toast_deleted_successfully))
                                    .setMessage(retained.size() + " shared sub-dependenc"
                                            + (retained.size() == 1 ? "y was" : "ies were")
                                            + " kept because other libraries also use them:\n\n"
                                            + String.join("\n", retained))
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

        binding.searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                searchAdapter.filter(getAdapterLocalLibraries(), value);
            }

            @Override
            public void onTextChanged(CharSequence newText, int start, int before, int count) {
            }
        });

        runLoadLocalLibrariesTask();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchBarExpanded) {
                    hideContextualToolbarAndClearSelection();
                } else if (binding.searchView.isShowing()) {
                    binding.searchView.hide();
                } else {
                    finish();
                }
            }
        });
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
        if (collapseContextualToolbar()) {
            setLocalLibrariesSelected(false);
        }
    }

    public void setLocalLibrariesSelected(boolean selected) {
        for (LocalLibrary library : getAdapterLocalLibraries()) {
            library.setSelected(selected);
        }
        adapter.notifyDataSetChanged();
    }

    private void expandContextualToolbar() {
        searchBarExpanded = true;
        binding.searchBar.expand(binding.contextualToolbarContainer, binding.appBarLayout);
    }

    private boolean collapseContextualToolbar() {
        searchBarExpanded = false;
        return binding.searchBar.collapse(binding.contextualToolbarContainer, binding.appBarLayout);
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
            adapter.setLocalLibraries(localLibraries);
            binding.noContentLayout.setVisibility(localLibraries.isEmpty() ? View.VISIBLE : View.GONE);
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
                        dependency = (String) libraryMap.get("dependency");
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
        intent.putExtra(SubDependenciesActivity.EXTRA_SC_ID, scId);

        List<LocalLibrary.DepInfo> allDeps = library.getAllDependencies();
        ArrayList<String> folders = new ArrayList<>(allDeps.size());
        ArrayList<String> coords = new ArrayList<>(allDeps.size());
        boolean[] builtIn = new boolean[allDeps.size()];
        int[] depths = new int[allDeps.size()];
        ArrayList<String> parents = new ArrayList<>(allDeps.size());
        for (int i = 0; i < allDeps.size(); i++) {
            LocalLibrary.DepInfo dep = allDeps.get(i);
            folders.add(dep.folderName);
            coords.add(dep.groupId + ":" + dep.artifactId + ":" + dep.version);
            builtIn[i] = dep.builtIn;
            depths[i] = dep.depth;
            parents.add(dep.parentCoord);
        }
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_FOLDERS, folders);
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_COORDS, coords);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_BUILTIN, builtIn);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_DEPTHS, depths);
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_PARENTS, parents);
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

    private static class LoadLocalLibrariesTask extends BaseAsyncTask {
        private final WeakReference<ManageLocalLibraryActivity> activity;

        public LoadLocalLibrariesTask(ManageLocalLibraryActivity activity) {
            super(activity);
            this.activity = new WeakReference<>(activity);
            activity.addTask(this);
        }

        @Override
        public void onSuccess() {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        @Override
        public void onError(String idk) {
            var act = activity.get();
            if (act == null) return;
            act.dismissLoadingDialog();
        }

        @Override
        public void doWork() {
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

            binding.libraryName.setText(library.getName());
            // Step 3: show dep count badge for root libraries (all deps including built-in)
            String sizeText = library.getSize();
            if (library.isRootLibrary() && !library.getAllDependencies().isEmpty()) {
                int depCount = library.getAllDependencies().size();
                sizeText += " \u00b7 " + depCount + (depCount == 1 ? " dep" : " deps");
            }
            // Step 6: show version conflict indicator
            if (conflictingLibNames.contains(library.getName())) {
                sizeText += " \u00b7 \u26a0 version conflict";
            }
            binding.librarySize.setText(sizeText);
            binding.libraryName.setSelected(true);
            bindSelectedState(binding.card, library);

            // Long press → enter multi-select mode
            View.OnLongClickListener enterSelectionMode = v -> {
                if (isSelectionModeEnabled) {
                    return false;
                }
                isSelectionModeEnabled = true;
                toggleLocalLibrary(binding.card, library, onLocalLibrarySelectedStateChangedListener);
                return true;
            };

            // Click on name/size area → open sub-dependencies screen (or toggle selection)
            View.OnClickListener openSubDeps = v -> {
                if (isSelectionModeEnabled) {
                    toggleLocalLibrary(binding.card, library, onLocalLibrarySelectedStateChangedListener);
                } else {
                    showSubDependencies(library);
                }
            };
            binding.libraryName.setOnClickListener(openSubDeps);
            binding.librarySize.setOnClickListener(openSubDeps);
            binding.libraryName.setOnLongClickListener(enterSelectionMode);
            binding.librarySize.setOnLongClickListener(enterSelectionMode);

            // Card click/long-press for areas not covered by name/size/switch
            binding.card.setOnClickListener(v -> {
                if (isSelectionModeEnabled) {
                    toggleLocalLibrary(binding.card, library, onLocalLibrarySelectedStateChangedListener);
                } else {
                    showSubDependencies(library);
                }
            });
            binding.card.setOnLongClickListener(enterSelectionMode);

            binding.materialSwitch.setChecked(false);
            if (!notAssociatedWithProject) {

                binding.materialSwitch.setOnClickListener(v -> onItemClicked(binding, library.getName()));

                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (library.getName().equals(String.valueOf(libraryMap.get("name")))) {
                        binding.materialSwitch.setChecked(true);
                    }
                }
            } else {
                binding.materialSwitch.setEnabled(false);
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
            card.setChecked(library.isSelected());
        }

        private void onItemClicked(ViewItemLocalLibBinding binding, String name) {
            toggleLibrary(binding.materialSwitch.isChecked(), name);
        }

        public List<LocalLibrary> getLocalLibraries() {
            return localLibraries;
        }

        public void setLocalLibraries(List<LocalLibrary> localLibraries) {
            this.localLibraries.clear();
            this.localLibraries.addAll(localLibraries);
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
            notifyDataSetChanged();
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

    public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private final List<LocalLibrary> filteredLocalLibraries = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            var binding = ViewItemLocalLibSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            var binding = holder.binding;
            var library = filteredLocalLibraries.get(position);

            binding.libraryName.setText(library.getName());
            binding.librarySize.setText(library.getSize());
            binding.libraryName.setSelected(true);

            binding.materialSwitch.setChecked(false);
            if (!notAssociatedWithProject) {

                binding.getRoot().setOnClickListener(v -> binding.materialSwitch.performClick());

                binding.materialSwitch.setOnClickListener(v -> {
                    onItemClicked(binding, library.getName());
                    adapter.notifyItemChanged(position);
                });

                for (Map<String, Object> libraryMap : projectUsedLibs) {
                    if (library.getName().equals(String.valueOf(libraryMap.get("name")))) {
                        binding.materialSwitch.setChecked(true);
                    }
                }
            } else {
                binding.materialSwitch.setEnabled(false);
            }
        }

        @Override
        public int getItemCount() {
            return filteredLocalLibraries.isEmpty() ? 0 : filteredLocalLibraries.size();
        }

        private void onItemClicked(ViewItemLocalLibSearchBinding binding, String name) {
            toggleLibrary(binding.materialSwitch.isChecked(), name);
        }

        public void filter(List<LocalLibrary> localLibraries, String query) {
            filteredLocalLibraries.clear();
            if (query.isEmpty()) {
                filteredLocalLibraries.addAll(localLibraries);
            } else {
                for (LocalLibrary library : localLibraries) {
                    if (library.getName().toLowerCase().contains(query.toLowerCase())) {
                        filteredLocalLibraries.add(library);
                    }
                }
            }
            // Sorts the filtered search results to ensure enabled libraries still appear at the top.
            filteredLocalLibraries.sort((lib1, lib2) -> {
                boolean isEnabled1 = isUsedLibrary(lib1.getName());
                boolean isEnabled2 = isUsedLibrary(lib2.getName());
                return Boolean.compare(isEnabled2, isEnabled1);
            });

            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final ViewItemLocalLibSearchBinding binding;

            public ViewHolder(@NonNull ViewItemLocalLibSearchBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    // ==================== Repository Management ====================

    private Runnable repoDialogRefresh;

    private static final String REPOSITORIES_JSON_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/.sketchware/libs/repositories.json";

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
                        pro.sketchware.utility.FileUtil.readFile(file.getAbsolutePath()),
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
        pro.sketchware.utility.FileUtil.writeFile(file.getAbsolutePath(), new Gson().toJson(defaults));
    }

    private void saveCustomRepos(ArrayList<HashMap<String, Object>> repos) {
        File file = new File(REPOSITORIES_JSON_PATH);
        file.getParentFile().mkdirs();
        pro.sketchware.utility.FileUtil.writeFile(file.getAbsolutePath(), new Gson().toJson(repos));
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
