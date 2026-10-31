// ImportIconActivity.java
package pro.sketchware.activities.importicon;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.activities.importicon.adapters.IconAdapter;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.BlockConstants;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.resources.ColorResourceResolver;
import pro.sketchware.core.validation.ResourceNameValidator;
import pro.sketchware.databinding.DialogFilterIconsLayoutBinding;
import pro.sketchware.databinding.DialogSaveIconBinding;
import pro.sketchware.databinding.ImportIconBinding;
import pro.sketchware.graphics.SvgUtils;
import pro.sketchware.util.Helper;
import pro.sketchware.util.PropertiesUtil;
import pro.sketchware.util.SketchToast;
import pro.sketchware.util.UIHelper;
import pro.sketchware.util.io.ZipUtil;
import pro.sketchware.widgets.ColorPickerDialog;

public class ImportIconActivity extends BaseAppCompatActivity implements IconAdapter.OnIconSelectedListener {

    private static final String PREFS_ICON_PACK = "icon_pack";
    private static final String KEY_SELECTED_FAMILY = "selected_family";
    private static final String KEY_SELECTED_STYLE = "selected_style";
    private static final String ICON_FAMILY_MATERIAL = "svg";
    private static final String ICON_FAMILY_SYMBOLS = "symbols";
    private static final String ICON_TYPE_OUTLINE = "outline";
    private static final String ICON_TYPE_SHARP = "sharp";
    private static final String ICON_TYPE_TWO_TONE = "twotone";
    private static final String ICON_TYPE_ROUND = "round";
    private static final String ICON_TYPE_BASELINE = "baseline";
    private MenuItem search;
    private SearchView searchView;
    private final OnBackPressedCallback searchViewCloser = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            if (search.isActionViewExpanded()) {
                search.collapseActionView();
                searchView.setQuery("", true);
            } else {
                getOnBackPressedDispatcher().onBackPressed();
            }
        }
    };

    private ImportIconBinding binding;

    private String sc_id;
    private IconAdapter adapter = null;
    private ArrayList<String> alreadyAddedImageNames;
    private SvgUtils svgUtils;
    private MaterialSymbolsPack symbolsPack;
    private String selected_icon_family = ICON_FAMILY_MATERIAL;
    private String selected_icon_type = ICON_TYPE_ROUND;
    private int selected_color = Color.parseColor("#9E9E9E");
    private String selected_color_hex = "#9E9E9E";
    private List<Pair<String, String>> allIconPaths = new ArrayList<>();
    private List<Pair<String, String>> icons = new ArrayList<>();
    private String currentQuery = "";
    private int currentPage = 0;

    private boolean isLoading = false;
    private boolean isLastPage = false;

    private int getGridLayoutColumnCount() {
        return (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density) / 80;
    }

    private boolean doExtractedIconsExist() {
        return new File(SketchwarePaths.getExtractedIconPackStoreLocation(), ICON_FAMILY_MATERIAL).isDirectory();
    }

    private void extractIcons() {
        ZipUtil.extractAssetZip(this, "icons" + File.separator + "icon_pack.zip", SketchwarePaths.getExtractedIconPackStoreLocation());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (binding.imageList.getLayoutManager() instanceof GridLayoutManager manager) {
            manager.setSpanCount(getGridLayoutColumnCount());
        }
        binding.imageList.requestLayout();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ImportIconBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ColorPickerDialog colorpicker = new ColorPickerDialog(this, 0xFF9E9E9E, false, false);
        svgUtils = new SvgUtils(this);
        Toolbar toolbar = binding.toolbar.toolbar;
        binding.toolbar.layoutMainLogo.setVisibility(View.GONE);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.design_manager_icon_actionbar_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            if (!UIHelper.isClickThrottled()) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        sc_id = getIntent().getStringExtra("sc_id");
        alreadyAddedImageNames = getIntent().getStringArrayListExtra("imageNames");

        var preferences = getSharedPreferences(PREFS_ICON_PACK, MODE_PRIVATE);
        selected_icon_family = preferences.getString(KEY_SELECTED_FAMILY, ICON_FAMILY_MATERIAL);
        selected_icon_type = preferences.getString(KEY_SELECTED_STYLE, ICON_TYPE_ROUND);
        boolean material = ICON_FAMILY_MATERIAL.equals(selected_icon_family);
        boolean symbols = ICON_FAMILY_SYMBOLS.equals(selected_icon_family);
        boolean supportedStyle = ICON_TYPE_OUTLINE.equals(selected_icon_type)
                || ICON_TYPE_ROUND.equals(selected_icon_type) || ICON_TYPE_SHARP.equals(selected_icon_type)
                || (material && (ICON_TYPE_BASELINE.equals(selected_icon_type) || ICON_TYPE_TWO_TONE.equals(selected_icon_type)));
        if ((!material && !symbols) || !supportedStyle) {
            selected_icon_family = ICON_FAMILY_MATERIAL;
            selected_icon_type = ICON_TYPE_ROUND;
        }
        BackgroundTasks.runIoIfAlive(TaskHost.of(this), "ImportIconActivity$LegacySymbolsCleanup",
                MaterialSymbolsPack::removeLegacyFiles, null, null);

        binding.imageList.setLayoutManager(new GridLayoutManager(getBaseContext(), getGridLayoutColumnCount()));
        adapter = new IconAdapter(this, selected_icon_type, selected_color, this);
        binding.imageList.setAdapter(adapter);
        showLoadingDialog();

        binding.filterIconsButton.setOnClickListener(v -> showFilterDialog());

        binding.imageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoading || isLastPage) return;

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        // Reached the end, load more items
                        isLoading = true;
                        loadMoreItems();
                    }
                }
            }
        });


        new Handler(Looper.getMainLooper()).postDelayed(() -> new InitialIconLoader(this).execute(), 300L);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_import_icon, menu);
        search = menu.findItem(R.id.menu_find);
        searchView = (SearchView) search.getActionView();
        searchView.setQueryHint(getString(R.string.common_word_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterIcons(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_find) {
            searchViewCloser.setEnabled(true);
            getOnBackPressedDispatcher().addCallback(this, searchViewCloser);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadIcons() {
        new IconLoader(this).execute();
    }

    private List<Pair<String, String>> loadIconPaths(String family) throws IOException {
        List<Pair<String, String>> loadedIcons = new ArrayList<>();
        String iconPackStoreLocation = SketchwarePaths.getExtractedIconPackStoreLocation() + File.separator + family;
        try (Stream<Path> iconFiles = Files.list(Paths.get(iconPackStoreLocation))) {
            iconFiles.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .forEach(folderName -> loadedIcons.add(new Pair<>(
                            folderName,
                            Paths.get(iconPackStoreLocation, folderName).toString()
                    )));
        }

        return loadedIcons;
    }

    private void loadMoreItems() {
        if (!currentQuery.isEmpty()) {
            isLoading = false;
            return;
        }
        int ITEMS_PER_PAGE = 40;
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allIconPaths.size());

        if (start < end) {
            List<Pair<String, String>> newItems = allIconPaths.subList(start, end);
            icons.addAll(newItems);
            currentPage++;
        }
        adapter.submitList(new ArrayList<>(icons));
        isLastPage = end >= allIconPaths.size();
        isLoading = false;
    }


    private void filterIcons(String query) {
        currentQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        icons.clear();
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
        if (currentQuery.isEmpty()) {
            loadMoreItems();
            return;
        }

        for (Pair<String, String> icon : allIconPaths) {
            if (icon.first.toLowerCase(Locale.ROOT).contains(currentQuery)) {
                icons.add(icon);
            }
        }
        isLastPage = true;
        adapter.submitList(new ArrayList<>(icons));
    }

    private void showFilterDialog() {
        DialogFilterIconsLayoutBinding dialogBinding = DialogFilterIconsLayoutBinding.inflate(getLayoutInflater());

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.import_icon_filter_title)
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.common_word_apply, null)
                .create();
        dialog.setView(dialogBinding.getRoot());

        dialogBinding.selectColour.setText(selected_color_hex);
        dialogBinding.selectColour.setBackgroundColor(selected_color);

        dialogBinding.selectColour.setTextColor(PropertiesUtil.getContrastTextColor(selected_color));
        dialogBinding.selectColour.setOnClickListener(view -> {
            ColorPickerDialog colorPicker = new ColorPickerDialog(this, selected_color_hex, false, false, sc_id);
            colorPicker.setColorPickerCallback(new ColorPickerDialog.OnColorPickedListener() {
                @Override
                public void onColorPicked(int color) {
                    selected_color = color;
                    selected_color_hex = "#" + String.format("%06X", color & (0x00FFFFFF));
                    dialogBinding.selectColour.setText(selected_color_hex);
                    adapter.setSelectedColor(selected_color);

                    dialogBinding.selectColour.setBackgroundColor(selected_color);
                    dialogBinding.selectColour.setTextColor(PropertiesUtil.getContrastTextColor(selected_color));
                }

                @Override
                public void onResourceColorPicked(String resourceName, int color) {
                    selected_color = color;
                    selected_color_hex = "@color/" + resourceName;
                    dialogBinding.selectColour.setText(selected_color_hex);
                    adapter.setSelectedColor(selected_color);

                    dialogBinding.selectColour.setBackgroundColor(selected_color);
                    dialogBinding.selectColour.setTextColor(PropertiesUtil.getContrastTextColor(selected_color));
                }
            });
            colorPicker.materialColorAttr((attr, attrColor) -> {
                attr = "?attr/" + attr;
                selected_color = PropertiesUtil.parseColor(new ColorResourceResolver(sc_id).getColorValue(getApplicationContext(), attr, 3));
                selected_color_hex = attr;
                dialogBinding.selectColour.setText(selected_color_hex);
                adapter.setSelectedColor(selected_color);

                dialogBinding.selectColour.setBackgroundColor(selected_color);
                dialogBinding.selectColour.setTextColor(PropertiesUtil.getContrastTextColor(selected_color));
            });
            colorPicker.showAtLocation(view, Gravity.CENTER, 0, 0);
        });

        switch (selected_icon_type) {
            case ICON_TYPE_OUTLINE -> dialogBinding.chipGroupStyle.check(R.id.chip_outline);
            case ICON_TYPE_BASELINE -> dialogBinding.chipGroupStyle.check(R.id.chip_baseline);
            case ICON_TYPE_SHARP -> dialogBinding.chipGroupStyle.check(R.id.chip_sharp);
            case ICON_TYPE_TWO_TONE -> dialogBinding.chipGroupStyle.check(R.id.chip_twotone);
            case ICON_TYPE_ROUND -> dialogBinding.chipGroupStyle.check(R.id.chip_round);
        }
        dialogBinding.chipGroupFamily.check(ICON_FAMILY_SYMBOLS.equals(selected_icon_family)
                ? R.id.chip_material_symbols : R.id.chip_material_icons);
        updateFilterStyles(dialogBinding);
        dialogBinding.chipGroupFamily.setOnCheckedStateChangeListener((group, checkedIds) -> updateFilterStyles(dialogBinding));

        dialog.setOnShowListener(dialogInterface -> {

            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String family = dialogBinding.chipGroupFamily.getCheckedChipId() == R.id.chip_material_symbols
                        ? ICON_FAMILY_SYMBOLS : ICON_FAMILY_MATERIAL;
                int style = dialogBinding.chipGroupStyle.getCheckedChipId();
                String type = ICON_TYPE_ROUND;
                if (style == R.id.chip_outline) type = ICON_TYPE_OUTLINE;
                else if (style == R.id.chip_twotone) type = ICON_TYPE_TWO_TONE;
                else if (style == R.id.chip_baseline) type = ICON_TYPE_BASELINE;
                else if (style == R.id.chip_sharp) type = ICON_TYPE_SHARP;
                applyIconFilter(family, type);
                dialogInterface.dismiss();

            });
        });

        dialog.show();

    }

    private void updateFilterStyles(DialogFilterIconsLayoutBinding dialogBinding) {
        boolean symbols = dialogBinding.chipGroupFamily.getCheckedChipId() == R.id.chip_material_symbols;
        dialogBinding.chipBaseline.setVisibility(symbols ? View.GONE : View.VISIBLE);
        dialogBinding.chipTwotone.setVisibility(symbols ? View.GONE : View.VISIBLE);
        int style = dialogBinding.chipGroupStyle.getCheckedChipId();
        if (symbols && (style == R.id.chip_baseline || style == R.id.chip_twotone)) {
            dialogBinding.chipGroupStyle.check(R.id.chip_outline);
        }
    }

    private void applyIconFilter(String family, String type) {
        boolean familyChanged = !selected_icon_family.equals(family);
        boolean symbolsStyleChanged = ICON_FAMILY_SYMBOLS.equals(family) && !type.equals(selected_icon_type);
        if (familyChanged || symbolsStyleChanged) {
            new IconLoader(this, family, type, familyChanged).execute();
        } else {
            updateIcons(type);
        }
    }

    private void updateIcons(String type) {
        selected_icon_type = type;
        adapter.setSource(symbolsPack, selected_icon_type);
        getSharedPreferences(PREFS_ICON_PACK, MODE_PRIVATE).edit()
                .putString(KEY_SELECTED_FAMILY, selected_icon_family).putString(KEY_SELECTED_STYLE, type).apply();
    }

    private void showSaveDialog(Pair<String, String> icon) {
        String type = selected_icon_type;
        int color = selected_color;
        String colorHex = selected_color_hex;
        String iconPath = icon.second + File.separator + type + ".svg";
        MaterialSymbolsPack selectedSymbols = symbolsPack;
        String iconName = "icon_" + icon.first + "_" + type;
        DialogSaveIconBinding dialogBinding = DialogSaveIconBinding.inflate(getLayoutInflater());
        ResourceNameValidator iconNameValidator = new ResourceNameValidator(getApplicationContext(), dialogBinding.textInputLayout,
                BlockConstants.RESERVED_KEYWORDS, alreadyAddedImageNames);

        var dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.common_word_save)
                .setNegativeButton(R.string.common_word_cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.common_word_save, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((AlertDialog) dialogInterface).getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                if (iconNameValidator.isValid()) {
                    File source = new File(iconPath);
                    if (selectedSymbols != null) {
                        try {
                            source = selectedSymbols.exportIcon(icon.first);
                        } catch (IOException e) {
                            dialogBinding.sourceError.setVisibility(View.VISIBLE);
                            return;
                        }
                    }
                    if (!source.isFile() || !source.canRead() || source.length() == 0) {
                        dialogBinding.sourceError.setVisibility(View.VISIBLE);
                        return;
                    }
                    Intent intent = new Intent();
                    intent.putExtra("iconName", Helper.getText(dialogBinding.inputText));
                    intent.putExtra("iconPath", source.getAbsolutePath());

                    intent.putExtra("iconColor", color);
                    intent.putExtra("iconColorHex", colorHex);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } else {
                    return;
                }
                dialogInterface.dismiss();
            });
        });

        if (selectedSymbols == null) {
            svgUtils.loadImage(dialogBinding.icon, iconPath);
        } else {
            svgUtils.loadImage(dialogBinding.icon, selectedSymbols.getSvg(icon.first), selectedSymbols.getCacheKey(icon.first));
        }
        dialogBinding.icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        dialogBinding.licenceInfo.setOnClickListener(v -> {
            Uri webpage = Uri.parse("https://www.apache.org/licenses/LICENSE-2.0.txt");
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });
        dialogBinding.inputText.setText(iconName);
        dialog.setView(dialogBinding.getRoot());
        dialog.show();
    }

    @Override
    public void onIconSelected(int position) {
        if (adapter == null || position < 0 || position >= adapter.getCurrentList().size()) return;
        if (!UIHelper.isClickThrottled()) {
            showSaveDialog(adapter.getCurrentList().get(position));
        }
    }

    private static class InitialIconLoader {
        private final WeakReference<ImportIconActivity> activity;

        public InitialIconLoader(ImportIconActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        public void execute() {
            var activity = this.activity.get();
            if (activity == null) return;
            BackgroundTasks.runSerialIfAlive(TaskHost.of(activity), "ImportIconActivity$InitialIconLoader",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.reloadIcons();
        }

        private void doWork() {
            var activity = this.activity.get();
            if (activity == null) return;
            if (ICON_FAMILY_MATERIAL.equals(activity.selected_icon_family) && !activity.doExtractedIconsExist()) {
                activity.extractIcons();
            }
        }

        private void onError(Throwable error) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            SketchToast.warning(activity, buildErrorMessage(error), 1).show();
        }

        private String buildErrorMessage(Throwable error) {
            String errorMessage = error != null ? error.getMessage() : null;
            if (errorMessage == null || errorMessage.isEmpty()) {
                return Helper.getResString(R.string.common_error_an_error_occurred);
            }
            return Helper.getResString(R.string.common_error_an_error_occurred) + "[" + errorMessage + "]";
        }

    }

    private static class IconLoader {
        private final WeakReference<ImportIconActivity> activity;
        private final String family;
        private final String type;
        private final boolean reloadCatalog;
        private List<Pair<String, String>> loadedIcons;
        private MaterialSymbolsPack loadedSymbols;

        public IconLoader(ImportIconActivity activity) {
            this(activity, activity.selected_icon_family, activity.selected_icon_type, true);
        }

        public IconLoader(ImportIconActivity activity, String family, String type, boolean reloadCatalog) {
            this.activity = new WeakReference<>(activity);
            this.family = family;
            this.type = type;
            this.reloadCatalog = reloadCatalog;
        }

        public void execute() {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.showLoadingDialog();
            BackgroundTasks.runSerialIfAlive(TaskHost.of(activity), "ImportIconActivity$IconLoader",
                    this::doWork, this::onSuccess, this::onError);
        }

        private void onSuccess() {
            var activity = this.activity.get();
            if (activity == null) return;
            if (!family.equals(activity.selected_icon_family)) {
                activity.adapter.submitList(null);
            }
            activity.selected_icon_family = family;
            activity.symbolsPack = loadedSymbols;
            activity.updateIcons(type);
            if (reloadCatalog) {
                activity.allIconPaths = loadedIcons;
                activity.filterIcons(activity.currentQuery);
            }
            activity.dismissLoadingDialog();
        }

        private void doWork() throws IOException {
            var activity = this.activity.get();
            if (activity == null) return;
            if (ICON_FAMILY_SYMBOLS.equals(family)) {
                loadedSymbols = MaterialSymbolsPack.load(activity, type);
            } else if (ICON_FAMILY_MATERIAL.equals(family) && !activity.doExtractedIconsExist()) {
                activity.extractIcons();
            }
            if (reloadCatalog) {
                // The build verifies that all three bundled styles have the same icon names.
                loadedIcons = loadedSymbols != null ? loadedSymbols.getIconPaths() : activity.loadIconPaths(family);
            }
        }

        private void onError(Throwable error) {
            var activity = this.activity.get();
            if (activity == null) return;
            activity.dismissLoadingDialog();
            SketchToast.warning(activity, buildErrorMessage(error), 1).show();
        }

        private String buildErrorMessage(Throwable error) {
            String errorMessage = error != null ? error.getMessage() : null;
            if (errorMessage == null || errorMessage.isEmpty()) {
                return Helper.getResString(R.string.common_error_an_error_occurred);
            }
            return Helper.getResString(R.string.common_error_an_error_occurred) + "[" + errorMessage + "]";
        }
    }
}
