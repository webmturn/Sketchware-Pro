package dev.aldi.sayuti.block;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import mod.hey.studios.util.Helper;
import mod.hilal.saif.blocks.BlocksHandler;
import pro.sketchware.R;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class ExtraBlockFile {

    public static final File EXTRA_BLOCKS_DATA_FILE = new File(SketchwarePaths.getBlockManagerBlockFilePath());
    public static final File EXTRA_BLOCKS_PALETTE_FILE = new File(SketchwarePaths.getBlockManagerPaletteFilePath());

    public static ArrayList<HashMap<String, Object>> buildInBlocks = new ArrayList<>();
    private static final ArrayList<HashMap<String, Object>> cachedExtraBlocks = new ArrayList<>();
    private static final HashMap<String, ArrayList<HashMap<String, Object>>> cachedExtraBlocksByPalette = new HashMap<>();
    private static final ArrayList<HashMap<String, Object>> cachedPaletteBlocks = new ArrayList<>();
    private static long cachedExtraBlocksLastModified = Long.MIN_VALUE;
    private static long cachedExtraBlocksLength = Long.MIN_VALUE;
    private static long cachedPaletteBlocksLastModified = Long.MIN_VALUE;
    private static long cachedPaletteBlocksLength = Long.MIN_VALUE;

    public static synchronized ArrayList<HashMap<String, Object>> getExtraBlockData() {
        ensureExtraBlocksCache();
        return new ArrayList<>(cachedExtraBlocks);
    }

    public static synchronized ArrayList<HashMap<String, Object>> getExtraBlockData(String paletteId) {
        ensureExtraBlocksCache();
        ArrayList<HashMap<String, Object>> extraBlocks = cachedExtraBlocksByPalette.get(paletteId);
        return extraBlocks == null ? new ArrayList<>() : new ArrayList<>(extraBlocks);
    }

    public static synchronized ArrayList<HashMap<String, Object>> getPaletteBlockData() {
        ensurePaletteBlocksCache();
        return new ArrayList<>(cachedPaletteBlocks);
    }

    public static synchronized void invalidateCache() {
        cachedExtraBlocksLastModified = Long.MIN_VALUE;
        cachedExtraBlocksLength = Long.MIN_VALUE;
        cachedPaletteBlocksLastModified = Long.MIN_VALUE;
        cachedPaletteBlocksLength = Long.MIN_VALUE;
        cachedExtraBlocks.clear();
        cachedExtraBlocksByPalette.clear();
        cachedPaletteBlocks.clear();
    }

    private static void ensureExtraBlocksCache() {
        long lastModified = EXTRA_BLOCKS_DATA_FILE.exists() ? EXTRA_BLOCKS_DATA_FILE.lastModified() : -1L;
        long length = EXTRA_BLOCKS_DATA_FILE.exists() ? EXTRA_BLOCKS_DATA_FILE.length() : 0L;
        if (lastModified == cachedExtraBlocksLastModified && length == cachedExtraBlocksLength) {
            return;
        }

        cachedExtraBlocksLastModified = lastModified;
        cachedExtraBlocksLength = length;
        cachedExtraBlocks.clear();
        cachedExtraBlocksByPalette.clear();

        ArrayList<HashMap<String, Object>> extraBlocks;
        try {
            extraBlocks = new Gson().fromJson(getExtraBlockFile(), Helper.TYPE_MAP_LIST);
            if (extraBlocks == null) extraBlocks = new ArrayList<>();
        } catch (com.google.gson.JsonSyntaxException e) {
            extraBlocks = new ArrayList<>();
        }

        buildInBlocks.clear();
        BlocksHandler.builtInBlocks(buildInBlocks);
        extraBlocks.addAll(buildInBlocks);

        cachedExtraBlocks.addAll(extraBlocks);
        for (HashMap<String, Object> extraBlock : extraBlocks) {
            Object palette = extraBlock.get("palette");
            if (!(palette instanceof String paletteId)) {
                continue;
            }
            cachedExtraBlocksByPalette
                    .computeIfAbsent(paletteId, unused -> new ArrayList<>())
                    .add(extraBlock);
        }
    }

    private static void ensurePaletteBlocksCache() {
        long lastModified = EXTRA_BLOCKS_PALETTE_FILE.exists() ? EXTRA_BLOCKS_PALETTE_FILE.lastModified() : -1L;
        long length = EXTRA_BLOCKS_PALETTE_FILE.exists() ? EXTRA_BLOCKS_PALETTE_FILE.length() : 0L;
        if (lastModified == cachedPaletteBlocksLastModified && length == cachedPaletteBlocksLength) {
            return;
        }

        cachedPaletteBlocksLastModified = lastModified;
        cachedPaletteBlocksLength = length;
        cachedPaletteBlocks.clear();

        try {
            ArrayList<HashMap<String, Object>> paletteBlocks = new Gson().fromJson(getPaletteBlockFile(), Helper.TYPE_MAP_LIST);
            if (paletteBlocks != null) {
                cachedPaletteBlocks.addAll(paletteBlocks);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_load_palette), e));
        }
    }

    /**
     * @return Non-empty content of {@link ExtraBlockFile#EXTRA_BLOCKS_DATA_FILE},
     * as cases of <code>""</code> as file content return <code>"[]"</code>
     */
    public static String getExtraBlockFile() {
        String fileContent;

        if (EXTRA_BLOCKS_DATA_FILE.exists() && !(fileContent = FileUtil.readFile(EXTRA_BLOCKS_DATA_FILE.getAbsolutePath())).isEmpty()) {
            return fileContent;
        } else {
            return "[]";
        }
    }

    public static String getPaletteBlockFile() {
        String fileContent;
        if (EXTRA_BLOCKS_PALETTE_FILE.exists() && !(fileContent = FileUtil.readFile(EXTRA_BLOCKS_PALETTE_FILE.getAbsolutePath())).isEmpty()) {
            return fileContent;
        }
        return "[]";
    }

    public static String getExtraBlockJson() {
        return "[]";
    }
}
