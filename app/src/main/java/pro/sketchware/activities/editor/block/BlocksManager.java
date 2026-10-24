package pro.sketchware.activities.editor.block;

import pro.sketchware.core.settings.ApplicationSettings;


import static pro.sketchware.util.GsonUtils.getGson;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.widgets.ColorPickerDialog;
import pro.sketchware.widgets.LoadingDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.util.Helper;
import pro.sketchware.R;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ActivityBlocksManagerBinding;
import pro.sketchware.databinding.DialogBlockConfigurationBinding;
import pro.sketchware.databinding.DialogPaletteBinding;
import pro.sketchware.databinding.PalletCustomviewBinding;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.SketchwareUtil;
import pro.sketchware.util.UI;

public class BlocksManager extends BaseAppCompatActivity {

    boolean isDialogShowing;
    View draggedView;
    private ArrayList<HashMap<String, Object>> all_blocks_list = new ArrayList<>();
    private final Map<String, Integer> blockCountsByPalette = new HashMap<>();
    private String blocks_dir;
    private String pallet_dir;
    private int oldPos;
    private int newPos;
    private Activity activity;
    private ArrayList<HashMap<String, Object>> pallet_listmap = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> filtered_pallet_list = new ArrayList<>();
    private ArrayList<Integer> filtered_palette_indices = new ArrayList<>();
    private boolean isSearchActive = false;
    private String currentQuery = "";
    private ItemTouchHelper itemTouchHelper;
    private ActivityBlocksManagerBinding binding;
    private Vibrator vibrator;
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicInteger MODEL_READERS = new AtomicInteger();
    private static final ArrayList<WeakReference<BlocksManager>> MANAGERS = new ArrayList<>();
    private SearchCancellation searchTask;
    private long searchGeneration;
    private boolean searchInProgress;

    private static final class SearchCancellation {
        volatile boolean cancelled;
    }

    private static HashMap<String, Object> editableBlock(HashMap<String, Object> block, boolean copy) {
        return copy ? new HashMap<>(block) : block;
    }

    private static final ExecutorService ORDER_WRITER = Executors.newSingleThreadExecutor();
    // Main-thread state: at most one save can own the global palette files at a time.
    static OrderSave pendingOrderSave;
    private boolean paletteOrderChanged;
    private boolean reloadAfterOrderSave;
    private LoadingDialog orderSaveDialog;
    private AlertDialog orderSaveErrorDialog;

    static final class OrderSave {
        final String blocksPath;
        final String palettesPath;
        final ArrayList<HashMap<String, Object>> blocks;
        final ArrayList<HashMap<String, Object>> palettes;
        final ArrayList<WeakReference<Activity>> observers = new ArrayList<>();
        final ArrayList<WeakReference<Activity>> indicatorRequests = new ArrayList<>();
        final int successMessage;
        long startedAt;
        boolean running;
        boolean refreshBlockLoader;
        Exception failure;

        OrderSave(BlocksManager owner) {
            blocksPath = owner.blocks_dir;
            palettesPath = owner.pallet_dir;
            blocks = new ArrayList<>(owner.all_blocks_list);
            palettes = new ArrayList<>(owner.pallet_listmap);
            successMessage = 0;
        }

        OrderSave(String path, ArrayList<HashMap<String, Object>> updated, int successMessage) {
            blocksPath = path;
            palettesPath = null;
            blocks = new ArrayList<>(updated);
            palettes = null;
            this.successMessage = successMessage;
        }
    }

    static void saveBlockDetails(String path, ArrayList<HashMap<String, Object>> updated, int message) {
        if (pendingOrderSave != null) throw new IllegalStateException("A block save is already pending");
        pendingOrderSave = new OrderSave(path, updated, message);
        // A manager in another window must reload before it can write its old model.
        for (WeakReference<BlocksManager> reference : MANAGERS) {
            BlocksManager manager = reference.get();
            if (manager != null && !manager.isDestroyed() && !manager.isFinishing()) {
                manager.reloadAfterOrderSave = true;
                pendingOrderSave.observers.add(new WeakReference<>(manager));
            }
        }
        startOrderSave(pendingOrderSave);
    }

    private void savePaletteOrder() {
        if (!paletteOrderChanged || pendingOrderSave != null) return;
        paletteOrderChanged = false;
        OrderSave save = new OrderSave(this);
        pendingOrderSave = save;
        startOrderSave(save);
        observeOrderSave();
    }

    static void startOrderSave(OrderSave save) {
        save.running = true;
        save.failure = null;
        save.startedAt = SystemClock.uptimeMillis();
        save.indicatorRequests.clear();
        MODEL_READERS.incrementAndGet();
        ORDER_WRITER.execute(() -> {
            Exception failure = null;
            try {
                if (save.palettes == null) {
                    // Match the former JSON round trip without parsing the whole file again.
                    // Copy nullable records so a failed save cannot alter the displayed model.
                    for (int i = 0; i < save.blocks.size(); i++) {
                        HashMap<String, Object> block = save.blocks.get(i);
                        if (block != null && block.containsValue(null)) {
                            HashMap<String, Object> normalized = new HashMap<>(block);
                            normalized.values().removeIf(Objects::isNull);
                            save.blocks.set(i, normalized);
                        }
                    }
                }
                // Serialize both snapshots before changing either file; the JSON format stays unchanged.
                String blocks = getGson().toJson(save.blocks);
                String palettes = save.palettes == null ? null : getGson().toJson(save.palettes);
                writeOrderFiles(save, blocks, palettes);
            } catch (Exception e) {
                failure = e;
            } finally {
                MODEL_READERS.decrementAndGet();
            }
            Exception result = failure;
            MAIN.post(() -> {
                save.running = false;
                save.failure = result;
                if (result == null) {
                    ExtraBlockFile.invalidateCache();
                    pendingOrderSave = null;
                }
                for (WeakReference<Activity> observer : new ArrayList<>(save.observers)) {
                    Activity owner = observer.get();
                    if (owner != null && !owner.isDestroyed() && !owner.isFinishing()) {
                        if (owner instanceof BlocksManager manager) manager.onOrderSaveCompleted(save);
                        else if (owner instanceof BlocksManagerDetailsActivity details) details.onBlockSaveCompleted(save);
                    }
                }
                // onStop must not reload files while an order save is still writing them.
                if (result == null && save.refreshBlockLoader) BlockLoader.refreshIfChanged();
            });
        });
    }

    private static void writeOrderFiles(OrderSave save, String blocks, String palettes) throws IOException {
        Path blocksPath = new File(save.blocksPath).getCanonicalFile().toPath();
        Path palettesPath = save.palettesPath == null ? null : new File(save.palettesPath).getCanonicalFile().toPath();
        if (blocksPath.equals(palettesPath)) throw new IOException("Blocks and palettes share a file");
        Path stagedBlocks = null;
        Path stagedPalettes = null;
        try {
            // Finish both temporary files before replacing either original. Legacy readers always
            // see a complete base file and do not need to understand AtomicFile backup names.
            stagedBlocks = stageOrderFile(blocksPath, blocks);
            if (palettesPath != null) stagedPalettes = stageOrderFile(palettesPath, palettes);
            Files.move(stagedBlocks, blocksPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            stagedBlocks = null;
            if (palettesPath != null) {
                Files.move(stagedPalettes, palettesPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                stagedPalettes = null;
            }
        } finally {
            try {
                if (stagedBlocks != null) Files.deleteIfExists(stagedBlocks);
            } finally {
                if (stagedPalettes != null) Files.deleteIfExists(stagedPalettes);
            }
        }
    }

    private static Path stageOrderFile(Path target, String json) throws IOException {
        if (Files.exists(target) && !Files.isRegularFile(target)) throw new IOException("Not a regular file: " + target);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".sketchware-order-", ".tmp");
        boolean complete = false;
        try {
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                output.write(json.getBytes(StandardCharsets.UTF_8));
                output.getFD().sync();
            }
            complete = true;
            return temporary;
        } finally {
            if (!complete) Files.deleteIfExists(temporary);
        }
    }

    private void observeOrderSave() {
        OrderSave save = pendingOrderSave;
        if (save == null || isFinishing() || isDestroyed()) return;
        boolean observed = false;
        for (WeakReference<Activity> observer : save.observers) {
            if (observer.get() == this) observed = true;
        }
        if (!observed) save.observers.add(new WeakReference<>(this));
        if (!save.running) {
            onOrderSaveCompleted(save);
            return;
        }
        if (orderSaveErrorDialog != null) {
            orderSaveErrorDialog.dismiss();
            orderSaveErrorDialog = null;
        }
        if (delayDetailsIndicator(save, this)) return;
        if (orderSaveDialog == null) {
            orderSaveDialog = new LoadingDialog(this);
            ((TextView) orderSaveDialog.findViewById(R.id.tv_progress)).setText(R.string.common_word_please_wait);
        }
        orderSaveDialog.show();
        orderSaveDialog.resumeAnimation();
    }

    static boolean delayDetailsIndicator(OrderSave save, Activity owner) {
        if (save.palettes != null) return false;
        long remaining = 100 - (SystemClock.uptimeMillis() - save.startedAt);
        if (remaining <= 0) return false;
        for (var reference : save.indicatorRequests) if (reference.get() == owner) return true;
        WeakReference<Activity> reference = new WeakReference<>(owner);
        save.indicatorRequests.add(reference);
        MAIN.postDelayed(() -> {
            Activity activity = reference.get();
            if (pendingOrderSave != save || !save.running || activity == null
                    || activity.isDestroyed() || activity.isFinishing()) return;
            if (activity instanceof androidx.lifecycle.LifecycleOwner lifecycleOwner
                    && !lifecycleOwner.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return;
            if (activity instanceof BlocksManager manager) manager.observeOrderSave();
            else if (activity instanceof BlocksManagerDetailsActivity details) details.observeSave();
        }, remaining);
        return true;
    }

    private void onOrderSaveCompleted(OrderSave save) {
        if (orderSaveDialog != null) {
            orderSaveDialog.pauseAnimation();
            orderSaveDialog.hide();
        }
        if (save.failure != null) {
            if (save.palettes == null && !getLifecycle().getCurrentState()
                    .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return;
            // Keep the snapshot and guard until both files are saved; never silently navigate away.
            if (orderSaveErrorDialog == null) {
                orderSaveErrorDialog = new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.common_message_save_failed)
                        .setPositiveButton(R.string.store_retry, (dialog, which) -> {
                            orderSaveErrorDialog = null;
                            if (pendingOrderSave == save && !save.running) startOrderSave(save);
                            observeOrderSave();
                        }).setCancelable(false).show();
            }
        } else {
            if (orderSaveErrorDialog != null) {
                orderSaveErrorDialog.dismiss();
                orderSaveErrorDialog = null;
            }
            if (reloadAfterOrderSave) {
                // A stopped parent will reload in onResume; do not parse a large
                // library behind the details page as part of its save completion.
                if (save.palettes == null && !getLifecycle().getCurrentState()
                        .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return;
                reloadAfterOrderSave = false;
                readSettings();
                refreshList();
            }
        }
    }

    private boolean waitForOrderSave() {
        if (pendingOrderSave == null) return false;
        reloadAfterOrderSave = true;
        observeOrderSave();
        return true;
    }

    private void cancelPaletteSearch() {
        searchGeneration++;
        if (searchTask != null) searchTask.cancelled = true;
        searchTask = null;
        searchInProgress = false;
    }

    @Override
    public void onDestroy() {
        MANAGERS.removeIf(reference -> reference.get() == null || reference.get() == this);
        cancelPaletteSearch();
        if (orderSaveDialog != null) {
            orderSaveDialog.cancelAnimation();
            orderSaveDialog.dismiss();
        }
        if (orderSaveErrorDialog != null) orderSaveErrorDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        MANAGERS.add(new WeakReference<>(this));
        binding = ActivityBlocksManagerBinding.inflate(getLayoutInflater());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setContentView(binding.getRoot());

        UI.addWindowInsetToPadding(binding.background, WindowInsetsCompat.Type.systemBars(), false, false, false, true);

        initialize();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (pendingOrderSave != null) pendingOrderSave.refreshBlockLoader = true;
        else BlockLoader.refreshIfChanged();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (pendingOrderSave != null) {
            observeOrderSave();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void initialize() {
        activity = this;
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pendingOrderSave != null) {
                    observeOrderSave();
                    return;
                }
                setEnabled(false);
                try {
                    getOnBackPressedDispatcher().onBackPressed();
                } finally {
                    setEnabled(true);
                }
            }
        });

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.paletteRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.paletteRecycler.setAdapter(new PaletteAdapter());
        binding.fab.setOnClickListener(v -> showPaletteDialog(false, null, null, "#ffffff", null));

        // onResume loads once on entry and reloads changes made by child pages.
        recycleBin(binding.recycleBinCard);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (hasActiveSearchQuery() || pendingOrderSave != null) return false;
                oldPos = viewHolder.getBindingAdapterPosition();
                newPos = target.getBindingAdapterPosition();
                if (oldPos == RecyclerView.NO_POSITION || newPos == RecyclerView.NO_POSITION || oldPos == newPos) return false;

                Collections.swap(pallet_listmap, oldPos, newPos);

                Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyItemMoved(oldPos, newPos);
                swapRelatedBlocks(oldPos + 9, newPos + 9);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int action) {
                if (action == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.7f);
                    draggedView = viewHolder.itemView;
                }
                super.onSelectedChanged(viewHolder, action);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                viewHolder.itemView.setAlpha(1f);
                savePaletteOrder();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (pendingOrderSave != null) return;
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    binding.background.setClipChildren(!isItNearTrash(draggedView, binding.recycleBin));
                    if (isItInTrash(draggedView, binding.recycleBin)) {
                        int pos = viewHolder.getBindingAdapterPosition();
                        binding.recycleBinCard.setAlpha(0.5f);
                        if (!isCurrentlyActive && pos != RecyclerView.NO_POSITION && pos < pallet_listmap.size() && !isDialogShowing) {
                            vibrator.vibrate(40L);
                            showMoveToBinDialog(pos);
                            isDialogShowing = true;
                        }
                        return;
                    }
                }
                binding.recycleBinCard.setAlpha(1f);
                isDialogShowing = false;
            }

        });

        itemTouchHelper.attachToRecyclerView(binding.paletteRecycler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.add(Menu.NONE, MENU_SEARCH, Menu.NONE, R.string.common_word_search);
        searchItem.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_search_white_24dp));
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        SearchView searchView = new SearchView(this);
        searchView.setQueryHint(Helper.getResString(R.string.block_manager_search_hint));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchItem.setActionView(searchView);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                filterPalettes(newText);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                isSearchActive = true;
                binding.fab.hide();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                isSearchActive = false;
                currentQuery = "";
                filterPalettes("");
                binding.fab.show();
                return true;
            }
        });

        menu.add(Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.common_word_settings).setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_settings)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    private boolean hasActiveSearchQuery() {
        return isSearchActive && !currentQuery.isEmpty();
    }

    private ArrayList<HashMap<String, Object>> getDisplayedPalettes() {
        return hasActiveSearchQuery() ? filtered_pallet_list : pallet_listmap;
    }

    private int getDisplayedSourceIndex(int displayPosition) {
        return hasActiveSearchQuery() ? filtered_palette_indices.get(displayPosition) : displayPosition;
    }

    private void filterPalettes(String query) {
        cancelPaletteSearch();
        filtered_pallet_list.clear();
        filtered_palette_indices.clear();
        if (query.isEmpty()) {
            for (int i = 0; i < pallet_listmap.size(); i++) {
                filtered_pallet_list.add(pallet_listmap.get(i));
                filtered_palette_indices.add(i);
            }
            Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyDataSetChanged();
            refreshCount();
            return;
        }
        String[] names = new String[pallet_listmap.size()];
        for (int i = 0; i < names.length; i++) names[i] = getPaletteName(pallet_listmap.get(i), i);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        if (all_blocks_list == null || all_blocks_list.size() <= 1000) {
            publishPaletteSearch(findPaletteMatches(names, all_blocks_list, lowerQuery, null));
            return;
        }
        // Mutators replace changed maps below, so this cheap list copy stays stable off the UI thread.
        ArrayList<HashMap<String, Object>> blocks = new ArrayList<>(all_blocks_list);
        long generation = searchGeneration;
        searchInProgress = true;
        Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyDataSetChanged();
        binding.paletteCount.setText(R.string.common_word_please_wait);
        SearchCancellation cancellation = new SearchCancellation();
        searchTask = cancellation;
        MODEL_READERS.incrementAndGet();
        SEARCH_EXECUTOR.execute(() -> {
            ArrayList<Integer> matches;
            try {
                matches = findPaletteMatches(names, blocks, lowerQuery, cancellation);
            } finally {
                MODEL_READERS.decrementAndGet();
            }
            MAIN.post(() -> {
                if (generation != searchGeneration || isDestroyed()) return;
                searchInProgress = false;
                searchTask = null;
                publishPaletteSearch(matches);
            });
        });
    }

    private static ArrayList<Integer> findPaletteMatches(String[] names,
            ArrayList<HashMap<String, Object>> blocks, String lowerQuery, SearchCancellation cancellation) {
        boolean[] matches = new boolean[names.length];
        int unmatched = matches.length;
        for (int i = 0; i < names.length; i++) {
            if (names[i].toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                matches[i] = true;
                unmatched--;
            }
        }
        if (unmatched > 0 && blocks != null) {
            Map<String, Integer> paletteIndices = new HashMap<>();
            int visited = 0;
            for (HashMap<String, Object> block : blocks) {
                if ((visited++ & 127) == 0 && cancellation != null && cancellation.cancelled) return new ArrayList<>();
                if (block == null) continue;
                Object palette = block.get("palette");
                if (palette == null) continue;
                String key = palette.toString();
                Integer index = paletteIndices.get(key);
                if (index == null) {
                    index = -1;
                    try {
                        double value = Double.parseDouble(key);
                        if (value >= 9 && value < 9d + matches.length && value == Math.floor(value)) {
                            index = (int) (value - 9);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    // Query-local only: edits and reloads cannot leave a stale numeric mapping.
                    paletteIndices.put(key, index);
                }
                if (index < 0 || matches[index]) continue;
                String blockName = String.valueOf(block.get("name")).toLowerCase(Locale.ROOT);
                String blockSpec = String.valueOf(block.get("spec")).toLowerCase(Locale.ROOT);
                if (blockName.contains(lowerQuery) || blockSpec.contains(lowerQuery)) {
                    matches[index] = true;
                    if (--unmatched == 0) break;
                }
            }
        }
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < matches.length; i++) if (matches[i]) result.add(i);
        return result;
    }

    private void publishPaletteSearch(ArrayList<Integer> matches) {
        filtered_pallet_list.clear();
        filtered_palette_indices.clear();
        for (int index : matches) {
            filtered_pallet_list.add(pallet_listmap.get(index));
            filtered_palette_indices.add(index);
        }
        Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyDataSetChanged();
        binding.paletteCount.setText(Helper.getResString(R.string.block_manager_search_result_format, matches.size()));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == MENU_SETTINGS) {
            showBlockConfigurationDialog();
        } else {
            return false;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (waitForOrderSave()) return;
        reloadAfterOrderSave = false;
        readSettings();
        refreshList();
        refreshCount();
    }

    private void showBlockConfigurationDialog() {
        if (waitForOrderSave()) return;
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.ic_folder_48dp);
        dialog.setTitle(R.string.blocks_config_title);

        DialogBlockConfigurationBinding dialogBinding = DialogBlockConfigurationBinding.inflate(getLayoutInflater());

        dialogBinding.palettesPath.setText(SketchwarePaths.toExternalStorageRelativePath(pallet_dir));
        dialogBinding.blocksPath.setText(SketchwarePaths.toExternalStorageRelativePath(blocks_dir));

        dialog.setView(dialogBinding.getRoot());

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (view, which) -> {
            if (waitForOrderSave()) return;
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH, Objects.requireNonNull(dialogBinding.palettesPath.getText()).toString());
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH, Objects.requireNonNull(dialogBinding.blocksPath.getText()).toString());

            readSettings();
            refreshList();
            BlockLoader.refresh();
            view.dismiss();
        });

        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);

        dialog.setNeutralButton(R.string.common_word_defaults, (view, which) -> {
            if (waitForOrderSave()) return;
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH, ApplicationSettings.getDefaultValue(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH));
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH, ApplicationSettings.getDefaultValue(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH));

            readSettings();
            refreshList();
            BlockLoader.refresh();
            view.dismiss();
        });

        dialog.show();
    }

    private void showMoveToBinDialog(int position) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(activity);
        dialog.setIcon(R.drawable.ic_mtrl_delete);
        dialog.setTitle(R.string.block_move_to_bin);
        dialog.setMessage(R.string.common_message_confirm);
        dialog.setPositiveButton(R.string.common_word_yes, (v, which) -> {
            movePaletteToRecycleBin(position);
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    private void movePaletteToRecycleBin(int position) {
        if (waitForOrderSave()) return;
        if (position < 0 || position >= pallet_listmap.size()) {
            SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
            return;
        }
        cancelPaletteSearch();
        boolean copy = MODEL_READERS.get() != 0;
        double removedPalette = position + 9;
        for (int i = 0; i < all_blocks_list.size(); i++) {
            HashMap<String, Object> block = all_blocks_list.get(i);
            double value = getPaletteValue(block);
            if (value == removedPalette || value > removedPalette) {
                HashMap<String, Object> changed = editableBlock(block, copy);
                changed.put("palette", value == removedPalette ? "-1" : String.valueOf((long) (value - 1)));
                all_blocks_list.set(i, changed);
            }
        }
        pallet_listmap.remove(position);
        draggedView = null;
        refreshBlockCounts();
        refreshPaletteViews();
        // Reuse the snapshot writer's navigation, recreation and failed-save guards.
        paletteOrderChanged = true;
        savePaletteOrder();
    }

    private void readSettings() {
        if (waitForOrderSave()) return;
        cancelPaletteSearch();
        pallet_dir = ExtraBlockFile.getExtraBlocksPaletteFile().getAbsolutePath();
        blocks_dir = ExtraBlockFile.getExtraBlocksDataFile().getAbsolutePath();

        try {
            if (FileUtil.isExistFile(blocks_dir)) {
                String json = FileUtil.readFile(blocks_dir);
                try {
                    // Parsing the expected model also validates its JSON shape. Keep the
                    // current model for empty/null files and publish only a complete parse.
                    ArrayList<HashMap<String, Object>> loaded = getGson().fromJson(json, Helper.TYPE_MAP_LIST);
                    if (loaded != null) all_blocks_list = loaded;
                } catch (JsonParseException e) {
                    SketchwareUtil.showFailedToParseJsonDialog(this, new File(blocks_dir), Helper.getResString(R.string.blocks_custom_blocks), v -> readSettings());
                }
            }
        } finally {
            // Also rebuild when a failed/missing read keeps the previous model or loads null.
            refreshBlockCounts();
        }
    }

    private void refreshList() {
        if (waitForOrderSave()) return;
        cancelPaletteSearch();
        parsePaletteJson:
        {
            String paletteJsonContent;
            if (FileUtil.isExistFile(pallet_dir) && !(paletteJsonContent = FileUtil.readFile(pallet_dir)).isEmpty()) {
                try {
                    pallet_listmap = getGson().fromJson(paletteJsonContent, Helper.TYPE_MAP_LIST);

                    if (pallet_listmap != null) {
                        break parsePaletteJson;
                    }
                    // fall-through to shared handler
                } catch (JsonParseException e) {
                    // fall-through to shared handler
                }

                SketchwareUtil.showFailedToParseJsonDialog(this, new File(pallet_dir), Helper.getResString(R.string.blocks_custom_block_palettes), v -> refreshList());
            }
            pallet_listmap = new ArrayList<>();
        }

        refreshPaletteViews();
    }

    private void refreshPaletteViews() {
        filtered_pallet_list.clear();
        filtered_palette_indices.clear();
        if (hasActiveSearchQuery()) {
            filterPalettes(currentQuery);
        } else {
            Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyDataSetChanged();
            refreshCount();
        }
    }

    private double getN(double _p) {
        return blockCountsByPalette.getOrDefault(String.valueOf((long) _p), 0);
    }

    private void refreshBlockCounts() {
        blockCountsByPalette.clear();
        if (all_blocks_list != null) {
            for (Map<String, Object> block : all_blocks_list) {
                if (block != null) {
                    // Preserve the original string comparison (e.g. "9" differs from "9.0").
                    blockCountsByPalette.merge(String.valueOf(block.get("palette")), 1, Integer::sum);
                }
            }
        }
        binding.recycleSub.setText(Helper.getResString(R.string.blocks_count_format, (long) getN(-1)));
    }

    private void refreshCount() {
        if (pallet_listmap.isEmpty()) {
            binding.paletteCount.setText(Helper.getResString(R.string.blocks_no_palettes));
        } else {
            binding.paletteCount.setText(Helper.getResString(R.string.blocks_palette_count_format, pallet_listmap.size()));
        }
    }

    private void recycleBin(View view) {
        view.setOnClickListener(v -> {
            if (waitForOrderSave()) return;
            Intent intent = new Intent(getApplicationContext(), BlocksManagerDetailsActivity.class);
            intent.putExtra("position", "-1");
            intent.putExtra("dirB", blocks_dir);
            intent.putExtra("dirP", pallet_dir);
            startActivity(intent);
        });
        view.setOnLongClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.blocks_recycle_bin_title)
                    .setMessage(Helper.getResString(R.string.blocks_recycle_bin_confirm))
                    .setPositiveButton(R.string.common_word_empty, (dialog, which) -> emptyRecyclebin())
                    .setNegativeButton(R.string.common_word_cancel, null)
                    .show();
            return true;
        });
    }

    private double getPaletteValue(Map<String, Object> block) {
        if (block == null) return Double.NaN;
        Object paletteObj = block.get("palette");
        if (paletteObj == null) return Double.NaN;
        try {
            return Double.parseDouble(paletteObj.toString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private void removeRelatedBlocks(double _p) {
        // New readers start only on main; during this loop existing readers can only finish.
        boolean copy = MODEL_READERS.get() != 0;
        List<Map<String, Object>> newBlocks = new LinkedList<>();
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (Double.isNaN(paletteValue) || paletteValue != _p) {
                if (!Double.isNaN(paletteValue) && paletteValue > _p) {
                    HashMap<String, Object> m = editableBlock(all_blocks_list.get(i), copy);
                    m.put("palette", String.valueOf((long) (paletteValue - 1)));
                    newBlocks.add(m);
                } else {
                    newBlocks.add(all_blocks_list.get(i));
                }
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(newBlocks));
        readSettings();
    }

    private void swapRelatedBlocks(double f, double s) {
        // New readers start only on main; during this loop existing readers can only finish.
        boolean copy = MODEL_READERS.get() != 0;
        paletteOrderChanged = true;
        String fromPalette = String.valueOf((long) f);
        String toPalette = String.valueOf((long) s);
        for (int i = 0; i < all_blocks_list.size(); i++) {
            HashMap<String, Object> block = all_blocks_list.get(i);
            if (block == null) continue;
            Object paletteObj = block.get("palette");
            if (paletteObj == null) continue;

            String oldPalette = paletteObj.toString();
            String newPalette;
            if ("TEMP_SWAP".equals(paletteObj)) {
                // Preserve how the former second pass handled a persisted swap marker.
                newPalette = toPalette;
            } else {
                double paletteValue;
                try {
                    paletteValue = Double.parseDouble(oldPalette);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (paletteValue == f) {
                    newPalette = toPalette;
                } else if (paletteValue == s) {
                    newPalette = fromPalette;
                } else {
                    continue;
                }
            }

            HashMap<String, Object> changed = editableBlock(block, copy);
            changed.put("palette", newPalette);
            all_blocks_list.set(i, changed);
            if (!oldPalette.equals(newPalette)) {
                int remaining = blockCountsByPalette.get(oldPalette) - 1;
                if (remaining == 0) {
                    blockCountsByPalette.remove(oldPalette);
                } else {
                    blockCountsByPalette.put(oldPalette, remaining);
                }
                blockCountsByPalette.merge(newPalette, 1, Integer::sum);
            }
        }
        // Counts follow each changed record; dragging never changes the recycle-bin count.
    }

    private void insertBlocksAt(double _p) {
        // New readers start only on main; during this loop existing readers can only finish.
        boolean copy = MODEL_READERS.get() != 0;
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (!Double.isNaN(paletteValue) && (paletteValue > _p || paletteValue == _p)) {
                HashMap<String, Object> changed = editableBlock(all_blocks_list.get(i), copy);
                changed.put("palette", String.valueOf((long) (paletteValue + 1)));
                all_blocks_list.set(i, changed);
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(all_blocks_list));
        readSettings();
        refreshList();
    }

    private void moveRelatedBlocksToRecycleBin(double _p) {
        // New readers start only on main; during this loop existing readers can only finish.
        boolean copy = MODEL_READERS.get() != 0;
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (!Double.isNaN(paletteValue) && paletteValue == _p) {
                HashMap<String, Object> changed = editableBlock(all_blocks_list.get(i), copy);
                changed.put("palette", "-1");
                all_blocks_list.set(i, changed);
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(all_blocks_list));
        readSettings();
    }

    private void emptyRecyclebin() {
        if (waitForOrderSave()) return;
        List<Map<String, Object>> newBlocks = new LinkedList<>();
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (Double.isNaN(paletteValue) || paletteValue != -1) {
                newBlocks.add(all_blocks_list.get(i));
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(newBlocks));
        readSettings();
        refreshList();
    }

    private void showPaletteDialog(boolean isEditing, Integer oldPosition, String oldName, String oldColor, Integer insertAtPosition) {
        if (waitForOrderSave()) return;
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.icon_style_white_96);
        dialog.setTitle(!isEditing ? R.string.blocks_title_create_palette : R.string.blocks_title_edit_palette);

        var paletteBinding = DialogPaletteBinding.inflate(getLayoutInflater());

        if (isEditing) {
            paletteBinding.nameEditText.setText(oldName);
            paletteBinding.colorEditText.setText(oldColor.replace("#", ""));
        }

        paletteBinding.openColorPalette.setOnClickListener(v1 -> {
            ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, 0xFFFFFFFF, false, false);
            colorPickerDialog.setColorPickerCallback(new ColorPickerDialog.OnColorPickedListener() {
                @Override
                public void onColorPicked(int colorInt) {
                    paletteBinding.colorEditText.setText(String.format("%06X", colorInt & 0x00FFFFFF));
                }

                @Override
                public void onResourceColorPicked(String resourceName, int color) {

                }
            });
            colorPickerDialog.showAtLocation(paletteBinding.openColorPalette, Gravity.CENTER, 0, 0);
        });

        dialog.setView(paletteBinding.getRoot());

        Helper.addClearErrorOnTextChangeTextWatcher(paletteBinding.name);
        Helper.addClearErrorOnTextChangeTextWatcher(paletteBinding.color);
        dialog.setPositiveButton(R.string.common_word_save, null);
        dialog.setNegativeButton(R.string.cancel, null);
        var shown = dialog.show();
        shown.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (waitForOrderSave()) return;
            String nameInput = Objects.requireNonNull(paletteBinding.nameEditText.getText()).toString();
            String colorInput = Objects.requireNonNull(paletteBinding.colorEditText.getText()).toString();

            if (nameInput.isEmpty()) {
                paletteBinding.name.setError(Helper.getResString(R.string.error_name_empty));
                return;
            }
            // add hash for the color 
            colorInput = "#" + colorInput;

            if (!PropertiesUtil.isHexColor(colorInput)) {
                paletteBinding.color.setError(Helper.getResString(R.string.blocks_error_valid_hex));
                return;
            }

            if (PropertiesUtil.isHexColor(colorInput)) {
                Color.parseColor(colorInput);
                if (!isEditing) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("name", nameInput);
                    map.put("color", colorInput);

                    if (insertAtPosition == null) {
                        pallet_listmap.add(map);
                        FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                        readSettings();
                        refreshList();
                    } else {
                        pallet_listmap.add(insertAtPosition, map);
                        FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                        insertBlocksAt(insertAtPosition + 9);
                    }
                } else {
                    HashMap<String, Object> oldPalette = pallet_listmap.get(oldPosition);
                    HashMap<String, Object> palette = oldPalette == null ? new HashMap<>() : new HashMap<>(oldPalette);
                    pallet_listmap.set(oldPosition, palette);
                    palette.put("name", nameInput);
                    palette.put("color", colorInput);
                    FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                    readSettings();
                    refreshList();
                }
                shown.dismiss();
            }
        });

    }


    private boolean isItInTrash(View draggedView, View trash) {
        if (draggedView == null) return false;

        int[] trashLocation = new int[2];
        trash.getLocationOnScreen(trashLocation);

        int[] draggedLocation = new int[2];
        draggedView.getLocationOnScreen(draggedLocation);

        int draggedY = draggedLocation[1];

        return draggedY <= trashLocation[1] + draggedView.getMeasuredHeight() / 2 && draggedY >= trashLocation[1] - draggedView.getMeasuredHeight() / 2;
    }

    private boolean isItNearTrash(View draggedView, View trash) {
        if (draggedView == null) return false;

        int[] trashLocation = new int[2];
        trash.getLocationOnScreen(trashLocation);

        int[] draggedLocation = new int[2];
        draggedView.getLocationOnScreen(draggedLocation);

        int draggedY = draggedLocation[1];

        return draggedY <= trashLocation[1] + draggedView.getMeasuredHeight() * 2 / 2 && draggedY >= trashLocation[1] - draggedView.getMeasuredHeight() * 2 / 2;
    }


    private static final int MENU_SEARCH = 4;
    private static final int MENU_SETTINGS = 0;
    private static final int PALETTE_MENU_EDIT = 1;
    private static final int PALETTE_MENU_DELETE = 2;
    private static final int PALETTE_MENU_INSERT = 3;

    private boolean hasPaletteName(Map<String, Object> palette) {
        return palette != null && palette.get("name") instanceof String;
    }

    private String getPaletteName(Map<String, Object> palette, int sourceIndex) {
        return hasPaletteName(palette) ? (String) palette.get("name")
                : Helper.getResString(R.string.blocks_error_invalid_name, sourceIndex + 1);
    }

    public class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.ViewHolder> {

        public PaletteAdapter() {
        }

        @NonNull
        @Override
        public PaletteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            PalletCustomviewBinding itemBinding = PalletCustomviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new PaletteAdapter.ViewHolder(itemBinding);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull PaletteAdapter.ViewHolder holder, int position) {
            ArrayList<HashMap<String, Object>> displayedPalettes = getDisplayedPalettes();
            HashMap<String, Object> palette = displayedPalettes.get(position);
            int sourceIndex = getDisplayedSourceIndex(position);
            String paletteColorValue = palette != null ? String.valueOf(palette.get("color")) : "#8a55d7";
            int backgroundColor = PropertiesUtil.parseColor(paletteColorValue);

            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemBinding.title.setText(getPaletteName(palette, sourceIndex));
            holder.itemBinding.sub.setText(Helper.getResString(R.string.blocks_count_format, (long) getN(sourceIndex + 9)));
            holder.itemBinding.color.setBackgroundColor(backgroundColor);
            holder.itemBinding.dragHandler.setVisibility(hasActiveSearchQuery() ? View.GONE : View.VISIBLE);

            holder.itemBinding.backgroundCard.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(BlocksManager.this, holder.itemBinding.color);
                Menu menu = popup.getMenu();
                menu.add(Menu.NONE, PALETTE_MENU_EDIT, Menu.NONE, R.string.blocks_menu_edit);
                menu.add(Menu.NONE, PALETTE_MENU_DELETE, Menu.NONE, R.string.blocks_menu_delete);
                menu.add(Menu.NONE, PALETTE_MENU_INSERT, Menu.NONE, R.string.blocks_menu_insert);
                popup.setOnMenuItemClickListener(item -> {
                    int pos = holder.getAbsoluteAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return false;
                    }
                    int selectedSourceIndex = getDisplayedSourceIndex(pos);
                    HashMap<String, Object> selectedPalette = pallet_listmap.get(selectedSourceIndex);
                    switch (item.getItemId()) {
                        case PALETTE_MENU_EDIT:
                            showPaletteDialog(true, selectedSourceIndex,
                                    hasPaletteName(selectedPalette) ? (String) selectedPalette.get("name") : "",
                                    selectedPalette != null ? String.valueOf(selectedPalette.get("color")) : "#8a55d7", null);
                            break;

                        case PALETTE_MENU_DELETE:
                            new MaterialAlertDialogBuilder(BlocksManager.this)
                                    .setTitle(getPaletteName(selectedPalette, selectedSourceIndex))
                                    .setMessage(R.string.blocks_remove_palette_msg)
                                    .setPositiveButton(R.string.blocks_remove_permanently, (dialog, which) -> {
                                        if (waitForOrderSave()) return;
                                        pallet_listmap.remove(selectedSourceIndex);
                                        FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                                        removeRelatedBlocks(selectedSourceIndex + 9);
                                        refreshList();
                                    })
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setNeutralButton(R.string.block_move_to_bin, (dialog, which) -> {
                                        if (waitForOrderSave()) return;
                                        moveRelatedBlocksToRecycleBin(selectedSourceIndex + 9);
                                        pallet_listmap.remove(selectedSourceIndex);
                                        FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                                        removeRelatedBlocks(selectedSourceIndex + 9);
                                        refreshList();
                                    }).show();
                            break;

                        case PALETTE_MENU_INSERT:
                            showPaletteDialog(false, null, null, null, selectedSourceIndex);
                            break;

                        default:
                    }
                    return true;
                });
                popup.show();

                return true;
            });

            holder.itemBinding.dragHandler.setOnTouchListener((v, event) -> {
                if (hasActiveSearchQuery()) {
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(holder);
                }

                return false;
            });

            holder.itemBinding.backgroundCard.setOnClickListener(v -> {
                if (waitForOrderSave()) return;
                int pos = holder.getAbsoluteAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }
                int selectedSourceIndex = getDisplayedSourceIndex(pos);
                if (!hasPaletteName(pallet_listmap.get(selectedSourceIndex))) {
                    SketchwareUtil.toastError(getPaletteName(pallet_listmap.get(selectedSourceIndex), selectedSourceIndex));
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), BlocksManagerDetailsActivity.class);
                intent.putExtra("position", String.valueOf((long) (selectedSourceIndex + 9)));
                intent.putExtra("dirB", blocks_dir);
                intent.putExtra("dirP", pallet_dir);
                startActivity(intent);
            });

        }

        @Override
        public int getItemCount() {
            return getDisplayedPalettes().size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public PalletCustomviewBinding itemBinding;

            public ViewHolder(PalletCustomviewBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }
}

