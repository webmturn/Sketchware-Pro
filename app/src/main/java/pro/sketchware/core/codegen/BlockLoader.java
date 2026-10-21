package pro.sketchware.core.codegen;

import pro.sketchware.core.block.DynamicPaletteProvider;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

import com.google.android.material.color.MaterialColors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.block.BlockSpecParser;
import pro.sketchware.util.Helper;
import pro.sketchware.util.LogUtil;
import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;

/**
 * An optimized Custom Blocks loader.
 *
 * @since v6.3.0
 */
public class BlockLoader {

    private static ArrayList<ExtraBlockInfo> blocks;
    /** Fast name to ExtraBlockInfo lookup, built alongside {@link #blocks}. */
    private static HashMap<String, ExtraBlockInfo> blocksByName;

    /** Per-project custom block cache: sc_id to (block_name to ExtraBlockInfo). */
    private static String cachedProjectId;
    private static HashMap<String, ExtraBlockInfo> projectBlockCache;

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
        ArrayList<HashMap<String, Object>> palettes = new DynamicPaletteProvider().getPaletteSelector();
        HashMap<Integer, Integer> paletteColors = new HashMap<>();
        for (HashMap<String, Object> palette : palettes) {
            if (palette.get("index") instanceof Integer index && palette.get("color") instanceof Integer color) {
                paletteColors.put(index, color);
            }
        }

        blocks = new ArrayList<>();
        blocksByName = new HashMap<>();

        ArrayList<HashMap<String, Object>> arrList = ExtraBlockFile.getExtraBlockData();
        Context colorContext = null;
        Integer primaryColor = null;

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

            Object imports = map.get("imports");
            if (imports != null) {
                info.setImports(imports.toString());
            }

            if (map.containsKey("color")) {
                Object color = map.get("color");

                if (color instanceof String) {
                    try {
                        int parsedColor = Color.parseColor((String) color);
                        if (primaryColor == null) {
                            if (colorContext == null) {
                                colorContext = new ContextThemeWrapper(SketchApplication.getContext(), R.style.Theme_SketchwarePro);
                            }
                            // Resolve lazily once per load; zero is a valid color, not a failure sentinel.
                            primaryColor = MaterialColors.getColor(colorContext, R.attr.colorPrimary,
                                    MaterialColors.class.getCanonicalName());
                        }
                        info.setColor(MaterialColors.harmonize(parsedColor, primaryColor));
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
                            Integer paletteColor = paletteColors.get(mapPaletteNumber);
                            if (paletteColor != null) info.setPaletteColor(paletteColor);
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
     * Still used in {@link pro.sketchware.core.block.view.BlockView}, so it must exist (for now).
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
        return BlockSpecParser.parseParameters(spec).size();
    }

    /**
     * Returns the argument count required by ordinary, explicit, and relative references.
     * Literal percent signs and line separators do not consume arguments.
     */
    static int countCodeArguments(String code) {
        return CodeTemplateArguments.analyze(code).requiredCount;
    }

    /**
     * Checks format syntax, String conversions, and available template arguments.
     * Warnings do not prevent loading or change code generation.
     *
     * @param info   the block to validate
     * @param source description of where the block was loaded from
     */
    private static void validateBlock(ExtraBlockInfo info, String source) {
        int specParams = countSpecParams(info.getSpec());
        // getCodeExtraBlock always adds subStack1 + subStack2 to params
        int availableParams = specParams + 2;
        CodeTemplateValidator.Problem problem = CodeTemplateValidator.findProblem(info.getCode(), availableParams);
        if (problem != null) {
            LogUtil.w("BlockLoader", String.format("Block '%s' (%s): %s String.format will fail at runtime.",
                    info.getName(), source, problem.describe()));
        }
    }
}
