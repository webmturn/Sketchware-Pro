package pro.sketchware.core.codegen;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import pro.sketchware.util.Helper;
import pro.sketchware.core.codegen.BuiltInBlockDefinitions;
import pro.sketchware.R;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.settings.ApplicationSettings;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.SketchwareUtil;

public class ExtraBlockFile {

    /** Default location. Use {@link #getExtraBlocksDataFile()} for the active configuration. */
    public static final File EXTRA_BLOCKS_DATA_FILE = new File(SketchwarePaths.getBlockManagerBlockFilePath());
    /** Default location. Use {@link #getExtraBlocksPaletteFile()} for the active configuration. */
    public static final File EXTRA_BLOCKS_PALETTE_FILE = new File(SketchwarePaths.getBlockManagerPaletteFilePath());

    public static ArrayList<HashMap<String, Object>> buildInBlocks = new ArrayList<>();
    private static final ArrayList<HashMap<String, Object>> cachedExtraBlocks = new ArrayList<>();
    private static final HashMap<String, ArrayList<HashMap<String, Object>>> cachedExtraBlocksByPalette = new HashMap<>();
    private static final ArrayList<HashMap<String, Object>> cachedPaletteBlocks = new ArrayList<>();
    private static long cachedExtraBlocksLastModified = Long.MIN_VALUE;
    private static long cachedExtraBlocksLength = Long.MIN_VALUE;
    private static long cachedPaletteBlocksLastModified = Long.MIN_VALUE;
    private static long cachedPaletteBlocksLength = Long.MIN_VALUE;
    private static File cachedExtraBlocksFile;
    private static File cachedPaletteBlocksFile;
    private static byte[] cachedExtraBlocksDigest;
    private static byte[] cachedPaletteBlocksDigest;
    private static long revision;

    public static File getExtraBlocksDataFile() {
        return getConfiguredFile(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH);
    }

    public static File getExtraBlocksPaletteFile() {
        return getConfiguredFile(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH);
    }

    private static File getConfiguredFile(String settingKey) {
        return new File(SketchwarePaths.resolveExternalStorageRelativePath(ApplicationSettings.getString(
                settingKey, (String) ApplicationSettings.getDefaultValue(settingKey))));
    }

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
        revision++;
        cachedExtraBlocksDigest = null;
        cachedPaletteBlocksDigest = null;
        cachedExtraBlocksLastModified = Long.MIN_VALUE;
        cachedExtraBlocksLength = Long.MIN_VALUE;
        cachedPaletteBlocksLastModified = Long.MIN_VALUE;
        cachedPaletteBlocksLength = Long.MIN_VALUE;
        cachedExtraBlocksFile = null;
        cachedPaletteBlocksFile = null;
        cachedExtraBlocks.clear();
        cachedExtraBlocksByPalette.clear();
        cachedPaletteBlocks.clear();
    }

    static synchronized long revision() {
        return revision;
    }

    /** Checks content, including replacements that preserve length and modification time. */
    static synchronized void refreshChangedFiles() throws IOException {
        File blocksFile = getExtraBlocksDataFile();
        File palettesFile = getExtraBlocksPaletteFile();
        // Unchanged files need a bounded-buffer hash, without decoding/allocating the entire JSON.
        byte[] blocksDigest = digestFile(blocksFile);
        byte[] palettesDigest = digestFile(palettesFile);
        boolean blocksChanged = !blocksFile.equals(cachedExtraBlocksFile) || !Arrays.equals(blocksDigest, cachedExtraBlocksDigest);
        boolean palettesChanged = !palettesFile.equals(cachedPaletteBlocksFile) || !Arrays.equals(palettesDigest, cachedPaletteBlocksDigest);
        // Finish checked reads before updating either cache. Hash the actual parsed text as well,
        // since an external writer may have replaced the file between the check and this read.
        String blocks = blocksChanged ? readChecked(blocksFile) : null;
        String palettes = palettesChanged ? readChecked(palettesFile) : null;
        if (blocksChanged) {
            loadExtraBlocksCache(blocksFile, blocks, digest(blocks));
        } else {
            cachedExtraBlocksLastModified = blocksFile.exists() ? blocksFile.lastModified() : -1L;
            cachedExtraBlocksLength = blocksFile.exists() ? blocksFile.length() : 0L;
        }
        if (palettesChanged) {
            loadPaletteBlocksCache(palettesFile, palettes, digest(palettes));
        } else {
            cachedPaletteBlocksLastModified = palettesFile.exists() ? palettesFile.lastModified() : -1L;
            cachedPaletteBlocksLength = palettesFile.exists() ? palettesFile.length() : 0L;
        }
    }

    private static String readChecked(File file) throws IOException {
        if (!file.isFile()) return "[]";
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return content.isEmpty() ? "[]" : content;
    }

    private static byte[] digest(String content) {
        return newDigest().digest(content.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] digestFile(File file) throws IOException {
        if (!file.isFile()) return digest("[]");
        MessageDigest digest = newDigest();
        boolean hasContent = false;
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] buffer = new byte[32 * 1024];
            for (int read; (read = stream.read(buffer)) != -1;) {
                digest.update(buffer, 0, read);
                hasContent |= read > 0;
            }
        }
        return hasContent ? digest.digest() : digest("[]");
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void ensureExtraBlocksCache() {
        File file = getExtraBlocksDataFile();
        long lastModified = file.exists() ? file.lastModified() : -1L;
        long length = file.exists() ? file.length() : 0L;
        if (file.equals(cachedExtraBlocksFile) && lastModified == cachedExtraBlocksLastModified && length == cachedExtraBlocksLength) {
            return;
        }

        String json = readJsonFile(file);
        loadExtraBlocksCache(file, json, digest(json));
    }

    private static void loadExtraBlocksCache(File file, String json, byte[] contentDigest) {
        cachedExtraBlocksFile = file;
        cachedExtraBlocksLastModified = file.exists() ? file.lastModified() : -1L;
        cachedExtraBlocksLength = file.exists() ? file.length() : 0L;
        cachedExtraBlocksDigest = contentDigest;
        revision++;
        cachedExtraBlocks.clear();
        cachedExtraBlocksByPalette.clear();

        ArrayList<HashMap<String, Object>> extraBlocks;
        try {
            extraBlocks = new Gson().fromJson(json, Helper.TYPE_MAP_LIST);
            if (extraBlocks == null) extraBlocks = new ArrayList<>();
            extraBlocks.removeIf(java.util.Objects::isNull);
        } catch (com.google.gson.JsonSyntaxException e) {
            extraBlocks = new ArrayList<>();
        }

        buildInBlocks.clear();
        BuiltInBlockDefinitions.builtInBlocks(buildInBlocks);
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
        File file = getExtraBlocksPaletteFile();
        long lastModified = file.exists() ? file.lastModified() : -1L;
        long length = file.exists() ? file.length() : 0L;
        if (file.equals(cachedPaletteBlocksFile) && lastModified == cachedPaletteBlocksLastModified && length == cachedPaletteBlocksLength) {
            return;
        }

        String json = readJsonFile(file);
        loadPaletteBlocksCache(file, json, digest(json));
    }

    private static void loadPaletteBlocksCache(File file, String json, byte[] contentDigest) {
        cachedPaletteBlocksFile = file;
        cachedPaletteBlocksLastModified = file.exists() ? file.lastModified() : -1L;
        cachedPaletteBlocksLength = file.exists() ? file.length() : 0L;
        cachedPaletteBlocksDigest = contentDigest;
        revision++;
        cachedPaletteBlocks.clear();

        try {
            ArrayList<HashMap<String, Object>> paletteBlocks = new Gson().fromJson(json, Helper.TYPE_MAP_LIST);
            if (paletteBlocks != null) {
                cachedPaletteBlocks.addAll(paletteBlocks);
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_load_palette), e));
        }
    }

    /**
     * @return Non-empty content of {@link #getExtraBlocksDataFile()},
     * as cases of <code>""</code> as file content return <code>"[]"</code>
     */
    public static String getExtraBlockFile() {
        return readJsonFile(getExtraBlocksDataFile());
    }

    public static String getPaletteBlockFile() {
        return readJsonFile(getExtraBlocksPaletteFile());
    }

    private static String readJsonFile(File file) {
        String fileContent;
        if (file.exists() && !(fileContent = FileUtil.readFile(file.getAbsolutePath())).isEmpty()) {
            return fileContent;
        }
        return "[]";
    }

    public static String getExtraBlockJson() {
        return "[]";
    }
}
