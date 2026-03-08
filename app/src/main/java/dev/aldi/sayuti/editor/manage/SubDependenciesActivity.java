package dev.aldi.sayuti.editor.manage;

import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.createLibraryMap;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.getLocalLibraries;
import static dev.aldi.sayuti.editor.manage.LocalLibrariesUtil.rewriteLocalLibFile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.databinding.ActivitySubDependenciesBinding;
import pro.sketchware.databinding.ViewItemLocalLibBinding;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.UI;

import static pro.sketchware.utility.FileUtil.formatFileSize;
import static pro.sketchware.utility.FileUtil.getFileSize;

public class SubDependenciesActivity extends BaseAppCompatActivity {

    public static final String EXTRA_ROOT_LIB_NAME = "root_lib_name";
    public static final String EXTRA_SC_ID = "sc_id";
    public static final String EXTRA_DEP_FOLDERS = "dep_folders";
    public static final String EXTRA_DEP_COORDS = "dep_coords";
    public static final String EXTRA_DEP_BUILTIN = "dep_builtin";
    public static final String EXTRA_DEP_DEPTHS = "dep_depths";
    public static final String EXTRA_DEP_PARENTS = "dep_parents";

    private ActivitySubDependenciesBinding binding;
    private String scId;
    private ArrayList<HashMap<String, Object>> projectUsedLibs;
    private boolean notAssociatedWithProject;
    private final SubDepAdapter adapter = new SubDepAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivitySubDependenciesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UI.addSystemWindowInsetToPadding(binding.toolbar, true, true, true, false);
        UI.addSystemWindowInsetToPadding(binding.dependenciesList, true, false, true, true);

        Intent intent = getIntent();
        String rootLibName = intent.getStringExtra(EXTRA_ROOT_LIB_NAME);
        ArrayList<String> depFolders = intent.getStringArrayListExtra(EXTRA_DEP_FOLDERS);
        ArrayList<String> depCoords = intent.getStringArrayListExtra(EXTRA_DEP_COORDS);
        boolean[] depBuiltIn = intent.getBooleanArrayExtra(EXTRA_DEP_BUILTIN);
        int[] depDepths = intent.getIntArrayExtra(EXTRA_DEP_DEPTHS);
        ArrayList<String> depParents = intent.getStringArrayListExtra(EXTRA_DEP_PARENTS);
        scId = intent.getStringExtra(EXTRA_SC_ID);

        notAssociatedWithProject = scId == null || scId.equals("system");

        if (!notAssociatedWithProject) {
            projectUsedLibs = getLocalLibraries(scId);
        }

        binding.toolbar.setTitle(rootLibName);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.dependenciesList.setAdapter(adapter);

        if (depFolders == null || depFolders.isEmpty()) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
            binding.dependenciesList.setVisibility(View.GONE);
        } else {
            loadDependencies(rootLibName, depFolders, depCoords, depBuiltIn, depDepths, depParents);
        }
    }

    private void loadDependencies(String rootLibName, List<String> folders, List<String> coords,
                                  boolean[] builtIn, int[] depths, List<String> parents) {
        // Build raw items from intent data
        List<SubDepItem> rawItems = new ArrayList<>();
        boolean hasTreeData = (depths != null && parents != null && depths.length == folders.size());

        int builtInCount = 0;
        int downloadedCount = 0;

        for (int i = 0; i < folders.size(); i++) {
            String folder = folders.get(i);
            String coord = (coords != null && i < coords.size()) ? coords.get(i) : folder;
            boolean isBuiltIn = (builtIn != null && i < builtIn.length) && builtIn[i];
            int depth = hasTreeData ? depths[i] : 1;
            String parent = hasTreeData && i < parents.size() ? parents.get(i) : null;
            File dir = resolveLibDir(folder);
            String size = isBuiltIn ? "Built-in" : (dir != null ? formatFileSize(getFileSize(dir)) : "\u2014");

            rawItems.add(new SubDepItem(folder, coord, size, isBuiltIn, depth, parent));

            if (isBuiltIn) builtInCount++;
            else downloadedCount++;
        }

        // Set subtitle with summary stats
        binding.toolbar.setSubtitle(downloadedCount + " downloaded, " + builtInCount + " built-in");

        // If tree data is available, sort into DFS order for proper tree display
        List<SubDepItem> orderedItems;
        if (hasTreeData) {
            orderedItems = sortDfs(rawItems);
        } else {
            orderedItems = rawItems;
        }

        // Compute isLastChild for tree connectors
        computeLastChildFlags(orderedItems);

        adapter.setItems(orderedItems);
        binding.noContentLayout.setVisibility(orderedItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Sorts items into DFS order using the parent→children relationship.
     * Items without tree data or orphaned items are appended at the end.
     */
    private List<SubDepItem> sortDfs(List<SubDepItem> items) {
        // Build parent → children map (preserving insertion order)
        Map<String, List<SubDepItem>> childrenMap = new LinkedHashMap<>();
        for (SubDepItem item : items) {
            if (item.parentCoord != null) {
                childrenMap.computeIfAbsent(item.parentCoord, k -> new ArrayList<>()).add(item);
            }
        }

        List<SubDepItem> result = new ArrayList<>();
        java.util.Set<String> visited = new java.util.HashSet<>();

        // DFS from depth-1 items (direct children of root)
        for (SubDepItem item : items) {
            if (item.depth == 1) {
                dfsVisit(item, childrenMap, visited, result);
            }
        }

        // Append any orphaned items not reached by DFS
        for (SubDepItem item : items) {
            if (!visited.contains(item.coordinate)) {
                result.add(item);
            }
        }

        return result;
    }

    private void dfsVisit(SubDepItem item, Map<String, List<SubDepItem>> childrenMap,
                          java.util.Set<String> visited, List<SubDepItem> result) {
        if (!visited.add(item.coordinate)) return;
        result.add(item);
        List<SubDepItem> children = childrenMap.get(item.coordinate);
        if (children != null) {
            for (SubDepItem child : children) {
                dfsVisit(child, childrenMap, visited, result);
            }
        }
    }

    /**
     * Computes whether each item is the last child of its parent,
     * used for drawing └── vs ├── tree connectors.
     */
    private void computeLastChildFlags(List<SubDepItem> items) {
        // For each parent, find the index of its last child
        Map<String, Integer> lastChildIndex = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            SubDepItem item = items.get(i);
            if (item.parentCoord != null) {
                lastChildIndex.put(item.parentCoord, i);
            }
        }
        for (int i = 0; i < items.size(); i++) {
            SubDepItem item = items.get(i);
            if (item.parentCoord != null) {
                Integer lastIdx = lastChildIndex.get(item.parentCoord);
                item.isLastChild = (lastIdx != null && lastIdx == i);
            }
        }
    }

    private File resolveLibDir(String name) {
        File primary = new File(FilePathUtil.getLocalLibsDir(), name);
        if (primary.exists()) return primary;
        File fallback = new File(FilePathUtil.getLocalLibsFallbackDir(), name);
        if (fallback.exists()) return fallback;
        return null;
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

    private void toggleLibrary(boolean isChecked, String name) {
        if (projectUsedLibs == null) return;
        if (!isChecked) {
            projectUsedLibs.removeIf(m -> name.equals(String.valueOf(m.get("name"))));
        } else {
            if (!isUsedLibrary(name)) {
                projectUsedLibs.add(createLibraryMap(name, null));
            }
        }
        rewriteLocalLibFile(scId, new Gson().toJson(projectUsedLibs));
    }

    private static class SubDepItem {
        final String folderName;
        final String coordinate;
        final String size;
        final boolean builtIn;
        final int depth;
        final String parentCoord;
        boolean isLastChild;

        SubDepItem(String folderName, String coordinate, String size, boolean builtIn,
                   int depth, String parentCoord) {
            this.folderName = folderName;
            this.coordinate = coordinate;
            this.size = size;
            this.builtIn = builtIn;
            this.depth = depth;
            this.parentCoord = parentCoord;
        }
    }

    private class SubDepAdapter extends RecyclerView.Adapter<SubDepAdapter.ViewHolder> {
        private final List<SubDepItem> items = new ArrayList<>();
        private final float dpUnit;

        SubDepAdapter() {
            dpUnit = SubDependenciesActivity.this.getResources().getDisplayMetrics().density;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ViewItemLocalLibBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            var binding = holder.binding;
            var item = items.get(position);

            // Tree indentation: 20dp per depth level (depth 1 = no indent, depth 2 = 20dp, etc.)
            int indentPx = (int) ((item.depth - 1) * 20 * dpUnit);
            binding.card.setPadding(indentPx, 0, 0, 0);

            // Tree connector prefix
            String treePrefix = "";
            if (item.depth > 1) {
                treePrefix = item.isLastChild ? "\u2514\u2500 " : "\u251C\u2500 ";
            }

            // Line 1: [tree prefix] artifactId.  Line 2: groupId:version · size/Built-in
            String coord = item.coordinate;
            String[] parts = coord.split(":");
            if (parts.length == 3) {
                binding.libraryName.setText(treePrefix + parts[1]);
                binding.librarySize.setText(parts[0] + ":" + parts[2] + " \u00b7 " + item.size);
            } else {
                binding.libraryName.setText(treePrefix + coord);
                binding.librarySize.setText(item.size);
            }
            binding.libraryName.setSingleLine(true);
            binding.libraryName.setSelected(true);

            binding.card.setChecked(false);
            binding.card.setCheckable(false);
            binding.card.setOnClickListener(null);

            if (item.builtIn) {
                binding.card.setAlpha(0.6f);
                binding.materialSwitch.setVisibility(View.GONE);
                binding.materialSwitch.setOnClickListener(null);
            } else {
                binding.card.setAlpha(1.0f);
                binding.materialSwitch.setVisibility(View.VISIBLE);
                binding.materialSwitch.setEnabled(true);
                binding.materialSwitch.setChecked(false);
                if (!notAssociatedWithProject) {
                    binding.materialSwitch.setChecked(isUsedLibrary(item.folderName));
                    binding.materialSwitch.setOnClickListener(v ->
                            toggleLibrary(binding.materialSwitch.isChecked(), item.folderName));
                } else {
                    binding.materialSwitch.setEnabled(false);
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setItems(List<SubDepItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ViewItemLocalLibBinding binding;

            ViewHolder(@NonNull ViewItemLocalLibBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
