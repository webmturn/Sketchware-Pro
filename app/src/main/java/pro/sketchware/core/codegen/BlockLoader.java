package pro.sketchware.core.codegen;

import pro.sketchware.core.block.DynamicPaletteProvider;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

import com.google.android.material.color.MaterialColors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
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
    private static long loadedRevision = -1;
    private static Integer loadedPrimaryColor;

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

        ArrayList<ExtraBlockInfo> loaded = new ArrayList<>();
        HashMap<String, ExtraBlockInfo> byName = new HashMap<>();
        HashMap<Integer, Integer> harmonizedColors = new HashMap<>();
        ValidationCache validation = new ValidationCache();

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
                        Integer harmonized = harmonizedColors.get(parsedColor);
                        if (harmonized == null) {
                            harmonized = MaterialColors.harmonize(parsedColor, primaryColor);
                            if (harmonizedColors.size() < 256) harmonizedColors.put(parsedColor, harmonized);
                        }
                        info.setColor(harmonized);
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

            validation.validate(info, "global extra blocks");
            loaded.add(info);
            byName.put(info.getName(), info);
        }
        blocks = loaded;
        blocksByName = byName;
        loadedRevision = ExtraBlockFile.revision();
        loadedPrimaryColor = primaryColor;
    }

    /** Reuse the published objects only after checking both source files and the theme. */
    public static void refreshIfChanged() {
        try {
            ExtraBlockFile.refreshChangedFiles();
        } catch (IOException failure) {
            LogUtil.w("BlockLoader", "Could not verify custom block sources", failure);
            refresh();
            return;
        }
        boolean sameTheme = loadedPrimaryColor == null || loadedPrimaryColor == MaterialColors.getColor(
                new ContextThemeWrapper(SketchApplication.getContext(), R.style.Theme_SketchwarePro),
                R.attr.colorPrimary, MaterialColors.class.getCanonicalName());
        if (blocksByName == null || loadedRevision != ExtraBlockFile.revision() || !sameTheme) loadCustomBlocks();
        // Project definitions have separate writers; preserve the existing refresh contract.
        invalidateProjectCache();
    }

    private record ValidationKey(String code, int available) { }

    /** Bounded memoization within one load; every distinct definition is still validated. */
    private static final class ValidationCache {
        final HashMap<String, Integer> counts = new HashMap<>();
        final HashMap<ValidationKey, CodeTemplateValidator.Problem> problems = new HashMap<>();

        void validate(ExtraBlockInfo info, String source) {
            Integer count = counts.get(info.getSpec());
            if (count == null) {
                count = countSpecParams(info.getSpec());
                if (counts.size() < 256) counts.put(info.getSpec(), count);
            }
            ValidationKey key = new ValidationKey(info.getCode(), count + 2);
            CodeTemplateValidator.Problem problem;
            if (problems.containsKey(key)) problem = problems.get(key);
            else {
                problem = CodeTemplateValidator.findProblem(key.code(), key.available());
                if (problems.size() < 256) problems.put(key, problem);
            }
            logProblem(info, source, problem);
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
        logProblem(info, source, problem);
    }

    private static void logProblem(ExtraBlockInfo info, String source, CodeTemplateValidator.Problem problem) {
        if (problem != null) {
            LogUtil.w("BlockLoader", String.format("Block '%s' (%s): %s String.format will fail at runtime.",
                    info.getName(), source, problem.describe()));
        }
    }
}
