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
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.widgets.ColorPickerDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

    @Override
    public void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ActivityBlocksManagerBinding.inflate(getLayoutInflater());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setContentView(binding.getRoot());

        UI.addWindowInsetToPadding(binding.background, WindowInsetsCompat.Type.systemBars(), false, false, false, true);

        initialize();
    }

    @Override
    public void onStop() {
        super.onStop();
        BlockLoader.refresh();
    }

    private void initialize() {
        activity = this;

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.paletteRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.paletteRecycler.setAdapter(new PaletteAdapter());
        binding.fab.setOnClickListener(v -> showPaletteDialog(false, null, null, "#ffffff", null));

        readSettings();
        refreshList();
        recycleBin(binding.recycleBinCard);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (hasActiveSearchQuery()) return false;
                oldPos = viewHolder.getBindingAdapterPosition();
                newPos = target.getBindingAdapterPosition();
                if (oldPos == RecyclerView.NO_POSITION || newPos == RecyclerView.NO_POSITION) return false;

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
                FileUtil.writeFile(blocks_dir, getGson().toJson(all_blocks_list));
                FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
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
        filtered_pallet_list.clear();
        filtered_palette_indices.clear();
        if (query.isEmpty()) {
            for (int i = 0; i < pallet_listmap.size(); i++) {
                filtered_pallet_list.add(pallet_listmap.get(i));
                filtered_palette_indices.add(i);
            }
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            for (int i = 0; i < pallet_listmap.size(); i++) {
                HashMap<String, Object> palette = pallet_listmap.get(i);
                String name = getPaletteName(palette, i).toLowerCase(Locale.ROOT);
                if (name.contains(lowerQuery)) {
                    filtered_pallet_list.add(palette);
                    filtered_palette_indices.add(i);
                    continue;
                }
                // Also search block names/specs within this palette
                int paletteIndex = i + 9;
                if (all_blocks_list != null) {
                    for (HashMap<String, Object> block : all_blocks_list) {
                        double pv = getPaletteValueDouble(block);
                        if (pv == paletteIndex) {
                            String blockName = String.valueOf(block.get("name")).toLowerCase(Locale.ROOT);
                            String blockSpec = String.valueOf(block.get("spec")).toLowerCase(Locale.ROOT);
                            if (blockName.contains(lowerQuery) || blockSpec.contains(lowerQuery)) {
                                filtered_pallet_list.add(palette);
                                filtered_palette_indices.add(i);
                                break;
                            }
                        }
                    }
                }
            }
        }
        Objects.requireNonNull(binding.paletteRecycler.getAdapter()).notifyDataSetChanged();
        if (query.isEmpty()) {
            refreshCount();
        } else {
            binding.paletteCount.setText(Helper.getResString(R.string.block_manager_search_result_format, filtered_pallet_list.size()));
        }
    }

    private double getPaletteValueDouble(Map<String, Object> block) {
        if (block == null) return Double.NaN;
        Object paletteObj = block.get("palette");
        if (paletteObj == null) return Double.NaN;
        try {
            return Double.parseDouble(paletteObj.toString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
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
        readSettings();
        refreshList();
        refreshCount();
    }

    private void showBlockConfigurationDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setIcon(R.drawable.ic_folder_48dp);
        dialog.setTitle(R.string.blocks_config_title);

        DialogBlockConfigurationBinding dialogBinding = DialogBlockConfigurationBinding.inflate(getLayoutInflater());

        dialogBinding.palettesPath.setText(SketchwarePaths.toExternalStorageRelativePath(pallet_dir));
        dialogBinding.blocksPath.setText(SketchwarePaths.toExternalStorageRelativePath(blocks_dir));

        dialog.setView(dialogBinding.getRoot());

        dialog.setPositiveButton(Helper.getResString(R.string.common_word_save), (view, which) -> {
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH, Objects.requireNonNull(dialogBinding.palettesPath.getText()).toString());
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH, Objects.requireNonNull(dialogBinding.blocksPath.getText()).toString());

            readSettings();
            refreshList();
            BlockLoader.refresh();
            view.dismiss();
        });

        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);

        dialog.setNeutralButton(R.string.common_word_defaults, (view, which) -> {
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
            if (position < 0 || position >= pallet_listmap.size()) {
                SketchwareUtil.toastError(Helper.getResString(R.string.common_error_an_error_occurred));
                return;
            }
            pallet_listmap.remove(position);
            draggedView = null;
            moveRelatedBlocksToRecycleBin(position + 9);
            removeRelatedBlocks(position + 9);
            FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
            refreshList();
            v.dismiss();
        });
        dialog.setNegativeButton(R.string.common_word_cancel, null);
        dialog.show();
    }

    private void readSettings() {
        pallet_dir = ExtraBlockFile.getExtraBlocksPaletteFile().getAbsolutePath();
        blocks_dir = ExtraBlockFile.getExtraBlocksDataFile().getAbsolutePath();

        try {
            if (FileUtil.isExistFile(blocks_dir) && isValidJson(FileUtil.readFile(blocks_dir))) {
                try {
                    all_blocks_list = getGson().fromJson(FileUtil.readFile(blocks_dir), Helper.TYPE_MAP_LIST);

                    if (all_blocks_list != null) {
                        return;
                    }
                    // fall-through to shared handler
                } catch (JsonParseException e) {
                    // fall-through to shared handler
                }

                SketchwareUtil.showFailedToParseJsonDialog(this, new File(blocks_dir), Helper.getResString(R.string.blocks_custom_blocks), v -> readSettings());
            }
        } finally {
            // Also rebuild when a failed/missing read keeps the previous model or loads null.
            refreshBlockCounts();
        }
    }

    private Boolean isValidJson(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() || element.isJsonArray();
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private void refreshList() {
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
        List<Map<String, Object>> newBlocks = new LinkedList<>();
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (Double.isNaN(paletteValue) || paletteValue != _p) {
                if (!Double.isNaN(paletteValue) && paletteValue > _p) {
                    HashMap<String, Object> m = all_blocks_list.get(i);
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
        String fromPalette = String.valueOf((long) f);
        String toPalette = String.valueOf((long) s);
        for (Map<String, Object> block : all_blocks_list) {
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

            block.put("palette", newPalette);
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
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (!Double.isNaN(paletteValue) && (paletteValue > _p || paletteValue == _p)) {
                all_blocks_list.get(i).put("palette", String.valueOf((long) (paletteValue + 1)));
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(all_blocks_list));
        readSettings();
        refreshList();
    }

    private void moveRelatedBlocksToRecycleBin(double _p) {
        for (int i = 0; i < all_blocks_list.size(); i++) {
            double paletteValue = getPaletteValue(all_blocks_list.get(i));
            if (!Double.isNaN(paletteValue) && paletteValue == _p) {
                all_blocks_list.get(i).put("palette", "-1");
            }
        }
        FileUtil.writeFile(blocks_dir, getGson().toJson(all_blocks_list));
        readSettings();
    }

    private void emptyRecyclebin() {
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
                    HashMap<String, Object> palette = pallet_listmap.get(oldPosition);
                    if (palette == null) {
                        palette = new HashMap<>();
                        pallet_listmap.set(oldPosition, palette);
                    }
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
                                        pallet_listmap.remove(selectedSourceIndex);
                                        FileUtil.writeFile(pallet_dir, getGson().toJson(pallet_listmap));
                                        removeRelatedBlocks(selectedSourceIndex + 9);
                                        refreshList();
                                    })
                                    .setNegativeButton(R.string.common_word_cancel, null)
                                    .setNeutralButton(R.string.block_move_to_bin, (dialog, which) -> {
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

