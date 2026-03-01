package dev.aldi.sayuti.editor.manage;

import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.deleteSelectedLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getAllLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibFile;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.rewriteLocalLibFile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

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
            if (item.getItemId() == R.id.action_clean_orphans) {
                showOrphanCleanupDialog();
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

    /** Opens a new screen listing the sub-dependencies of a root library. */
    private void showSubDependencies(LocalLibrary library) {
        Intent intent = new Intent(this, SubDependenciesActivity.class);
        intent.putExtra(SubDependenciesActivity.EXTRA_ROOT_LIB_NAME, library.getName());
        intent.putExtra(SubDependenciesActivity.EXTRA_SC_ID, scId);

        List<LocalLibrary.DepInfo> allDeps = library.getAllDependencies();
        ArrayList<String> folders = new ArrayList<>(allDeps.size());
        ArrayList<String> coords = new ArrayList<>(allDeps.size());
        boolean[] builtIn = new boolean[allDeps.size()];
        for (int i = 0; i < allDeps.size(); i++) {
            LocalLibrary.DepInfo dep = allDeps.get(i);
            folders.add(dep.folderName);
            coords.add(dep.groupId + ":" + dep.artifactId + ":" + dep.version);
            builtIn[i] = dep.builtIn;
        }
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_FOLDERS, folders);
        intent.putStringArrayListExtra(SubDependenciesActivity.EXTRA_DEP_COORDS, coords);
        intent.putExtra(SubDependenciesActivity.EXTRA_DEP_BUILTIN, builtIn);
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

    @Override
    public void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
