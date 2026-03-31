package mod.hey.studios.editor.manage.block.v2;

import static com.google.android.material.color.MaterialColors.harmonizeWithPrimary;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.aldi.sayuti.block.ExtraBlockFile;
import mod.agus.jcoderz.editor.manage.block.palette.PaletteSelector;
import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.util.Helper;
import mod.jbk.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.core.SketchwarePaths;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

/**
 * An optimized Custom Blocks loader.
 *
 * @since v6.3.0
 */
public class BlockLoader {

    private static ArrayList<ExtraBlockInfo> blocks;
    /** Fast name → ExtraBlockInfo lookup, built alongside {@link #blocks}. */
    private static HashMap<String, ExtraBlockInfo> blocksByName;

    /** Per-project custom block cache: sc_id → (block_name → ExtraBlockInfo). */
    private static String cachedProjectId;
    private static HashMap<String, ExtraBlockInfo> projectBlockCache;

    // Matches %s, %d, %b, %m in spec strings
    private static final Pattern SPEC_PARAM_PATTERN = Pattern.compile("%[sdbm]");
    // Matches String.format placeholders: %s, %1$s, %2$s, etc.
    private static final Pattern CODE_PLACEHOLDER_PATTERN = Pattern.compile("%(\\d+\\$)?s");

    static {
        loadCustomBlocks();
    }

    public static ExtraBlockInfo getBlockInfo(String block_name) {
        if (blocksByName == null) {
            loadCustomBlocks();
        }
        ExtraBlockInfo info = blocksByName.get(block_name);
        if (info != null) {
            return info;
        }
        ExtraBlockInfo missing = new ExtraBlockInfo();
        missing.setName(block_name);
        missing.isMissing = true;
        return missing;
    }

    public static ExtraBlockInfo getBlockFromProject(String sc_id, String block_name) {
        if (projectBlockCache == null || !sc_id.equals(cachedProjectId)) {
            loadProjectBlocks(sc_id);
        }
        ExtraBlockInfo cached = projectBlockCache.get(block_name);
        if (cached != null) {
            return cached;
        }
        ExtraBlockInfo missing = new ExtraBlockInfo();
        missing.setName(block_name);
        missing.isMissing = true;
        return missing;
    }

    private static void loadProjectBlocks(String sc_id) {
        cachedProjectId = sc_id;
        projectBlockCache = new HashMap<>();
        File customBlocksConfig = new File(SketchwarePaths.getProjectCustomBlocksPath(sc_id));
        if (customBlocksConfig.exists()) {
            try {
                ArrayList<ExtraBlockInfo> extraBlocks = new Gson().fromJson(
                        FileUtil.readFile(customBlocksConfig.getAbsolutePath()),
                        new TypeToken<ArrayList<ExtraBlockInfo>>() {
                        }.getType());
                if (extraBlocks != null) {
                    for (ExtraBlockInfo info : extraBlocks) {
                        validateBlock(info, "project " + sc_id);
                        projectBlockCache.put(info.getName(), info);
                    }
                }
            } catch (Exception e) {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_get_custom), sc_id, e.getMessage()));
            }
        }
    }

    /**
     * Invalidates the project-level custom block cache.
     * Call this when custom blocks are modified (e.g. in the block editor).
     */
    public static void invalidateProjectCache() {
        cachedProjectId = null;
        projectBlockCache = null;
    }

    private static void loadCustomBlocks() {
        ArrayList<HashMap<String, Object>> palettes = new PaletteSelector().getPaletteSelector();

        blocks = new ArrayList<>();
        blocksByName = new HashMap<>();

        ArrayList<HashMap<String, Object>> arrList = ExtraBlockFile.getExtraBlockData();

        for (int i = 0; i < arrList.size(); i++) {
            HashMap<String, Object> map = arrList.get(i);

            if (!map.containsKey("name")) {
                continue;
            }

            ExtraBlockInfo info = new ExtraBlockInfo();

            Object name = map.get("name");

            if (name instanceof String) {
                info.setName((String) name);
            } else {
                info.setName("");
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_name), i + 1));
                continue;
            }

            if (map.containsKey("spec")) {
                Object spec = map.get("spec");

                if (spec instanceof String) {
                    info.setSpec((String) spec);
                }
            }

            if (map.containsKey("spec2")) {
                Object spec2 = map.get("spec2");

                if (spec2 instanceof String) {
                    info.setSpec2((String) spec2);
                }
            }

            if (map.containsKey("code")) {
                Object code = map.get("code");

                if (code instanceof String) {
                    info.setCode((String) code);
                }
            }

            if (map.containsKey("color")) {
                Object color = map.get("color");

                if (color instanceof String) {
                    try {
                        Context context = new ContextThemeWrapper(SketchApplication.getContext(), R.style.Theme_SketchwarePro);
                        int harmonizedColor = harmonizeWithPrimary(context, Color.parseColor((String) color));
                        info.setColor(harmonizedColor);
                    } catch (IllegalArgumentException e) {
                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_color), i + 1));
                        continue;
                    }
                }
            } else {
                if (!map.containsKey("palette")) {
                    continue;
                } else {
                    Object mapPalette = map.get("palette");
                    if (mapPalette instanceof String) {
                        try {
                            int mapPaletteNumber = Integer.parseInt((String) mapPalette);

                            for (int j = 0, palettesSize = palettes.size(); j < palettesSize; j++) {
                                HashMap<String, Object> palette = palettes.get(j);
                                Object paletteIndex = palette.get("index");

                                if (paletteIndex instanceof Integer) {
                                    int indexInt = (Integer) paletteIndex;

                                    if (mapPaletteNumber == indexInt) {
                                        Object paletteColor = palette.get("color");

                                        if (paletteColor instanceof Integer) {
                                            try {
                                                info.setPaletteColor((Integer) paletteColor);
                                            } catch (IllegalArgumentException e) {
                                                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_palette_color), j + 1));
                                            }
                                        } else {
                                            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_palette_color_type), j + 1));
                                        }
                                    }
                                } else {
                                    SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_palette_index_type), j + 1));
                                }
                            }
                        } catch (NumberFormatException e) {
                            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_palette_number), i + 1));
                            continue;
                        }
                    } else {
                        SketchwareUtil.toastError(String.format(Helper.getResString(R.string.block_error_invalid_palette_number_type), i + 1));
                        continue;
                    }
                }
            }

            validateBlock(info, "global extra blocks");
            blocks.add(info);
            blocksByName.put(info.getName(), info);
        }
    }

    /**
     * Still used in {@link pro.sketchware.core.BlockView}, so it must exist (for now).
     */
    public static void log(String message) {
        LogUtil.d("BlockLoader", message);
    }

    public static void refresh() {
        loadCustomBlocks();
        invalidateProjectCache();
    }

    /**
     * Counts the number of parameters in a block spec string.
     * Parameters are denoted by %s, %d, %b, or %m.
     */
    static int countSpecParams(String spec) {
        if (spec == null || spec.isEmpty()) return 0;
        int count = 0;
        Matcher matcher = SPEC_PARAM_PATTERN.matcher(spec);
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Counts the number of String.format placeholders in a code template.
     * Placeholders are %s or positional like %1$s.
     */
    static int countCodePlaceholders(String code) {
        if (code == null || code.isEmpty()) return 0;
        int count = 0;
        Matcher matcher = CODE_PLACEHOLDER_PATTERN.matcher(code);
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Validates that a block's code template placeholders don't exceed
     * the available parameters (spec params + 2 for subStack1/subStack2).
     * Logs a warning if there's a mismatch.
     *
     * @param info   the block to validate
     * @param source description of where the block was loaded from
     */
    private static void validateBlock(ExtraBlockInfo info, String source) {
        int specParams = countSpecParams(info.getSpec());
        int codePlaceholders = countCodePlaceholders(info.getCode());
        // getCodeExtraBlock always adds subStack1 + subStack2 to params
        int availableParams = specParams + 2;
        if (codePlaceholders > availableParams) {
            LogUtil.w("BlockLoader",
                    String.format("Block '%s' (%s) has %d code placeholders but only %d available params " +
                                    "(spec=%d + 2 substacks). String.format will fail at runtime.",
                            info.getName(), source, codePlaceholders, availableParams, specParams));
        }
    }
}
