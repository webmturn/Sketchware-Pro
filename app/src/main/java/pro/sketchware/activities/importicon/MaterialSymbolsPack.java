package pro.sketchware.activities.importicon;

import android.content.Context;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

import pro.sketchware.core.project.SketchwarePaths;

/** Reads the selected bundled style without extracting thousands of SVG files. */
public final class MaterialSymbolsPack {
    private final String style;
    private final File exportRoot;
    private final Map<String, String> icons;

    private MaterialSymbolsPack(Context context, String style, Map<String, String> icons) {
        this.style = style;
        this.icons = icons;
        exportRoot = new File(context.getFilesDir(), "icon_import/symbols");
    }

    public static MaterialSymbolsPack load(Context context, String style) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(context.getAssets().open("icons/material_symbols_" + style + ".zip"))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.getName().equals("symbols.json")) continue;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = zip.read(buffer)) != -1) output.write(buffer, 0, count);
                JSONObject catalog = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
                Map<String, String> icons = new TreeMap<>();
                for (var names = catalog.keys(); names.hasNext();) {
                    String name = names.next();
                    icons.put(name, catalog.getString(name));
                }
                return new MaterialSymbolsPack(context, style, icons);
            }
        } catch (JSONException e) {
            throw new IOException("Invalid Material Symbols catalog", e);
        }
        throw new IOException("Missing Material Symbols catalog");
    }

    public List<Pair<String, String>> getIconPaths() {
        List<Pair<String, String>> paths = new ArrayList<>(icons.size());
        for (String name : icons.keySet()) {
            paths.add(Pair.create(name, new File(exportRoot, name).getAbsolutePath()));
        }
        return paths;
    }

    public byte[] getSvg(String name) {
        return icons.get(name).getBytes(StandardCharsets.UTF_8);
    }

    public String getCacheKey(String name) {
        return "material_symbols/" + style + "/" + name;
    }

    public File exportIcon(String name) throws IOException {
        Path output = new File(new File(exportRoot, name), style + ".svg").toPath();
        Files.createDirectories(output.getParent());
        Files.write(output, getSvg(name));
        return output.toFile();
    }

    /** Removes only the derived files created by the previous full-extraction implementation. */
    public static void removeLegacyFiles() throws IOException {
        Path legacy = new File(SketchwarePaths.getExtractedIconPackStoreLocation(), "symbols").toPath();
        if (!Files.exists(legacy)) return;
        Files.walkFileTree(legacy, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
