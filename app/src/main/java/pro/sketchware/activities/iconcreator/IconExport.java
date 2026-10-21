package pro.sketchware.activities.iconcreator;

import android.graphics.Bitmap;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Exports only captured values; no Activity or View may be accessed by the worker. */
final class IconExport {
    private static final String[] DENSITIES = {
            "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"
    };

    record Snapshot(File dataDirectory, File iconDirectory, File cacheDirectory,
                    Map<String, Object> settings, Map<String, Bitmap> layers,
                    String texturePath, String iconPath, Bitmap croppedIcon, boolean adaptive) {
        Snapshot {
            settings = Map.copyOf(settings);
            layers = Map.copyOf(layers);
        }

        Bitmap launcher() { return layers.get("ic_launcher.png"); }
    }

    private record Output(Path source, Path target) {}

    private IconExport() {}

    static void write(Snapshot snapshot) throws IOException {
        Path staging = Files.createTempDirectory(snapshot.cacheDirectory().toPath(), "icon-export-");
        try {
            List<Output> outputs = new ArrayList<>();
            for (Map.Entry<String, Bitmap> layer : snapshot.layers().entrySet()) {
                Path encoded = staging.resolve(layer.getKey());
                encode(layer.getValue(), encoded);
                for (String density : DENSITIES) {
                    outputs.add(new Output(encoded, snapshot.iconDirectory().toPath()
                            .resolve(density).resolve(layer.getKey())));
                }
            }
            stageSource(snapshot.texturePath(), "texture.png", snapshot, staging, outputs);
            if (snapshot.croppedIcon() != null) {
                Path encoded = staging.resolve("app_icon.png");
                encode(snapshot.croppedIcon(), encoded);
                outputs.add(new Output(encoded, snapshot.dataDirectory().toPath().resolve("app_icon.png")));
            } else {
                stageSource(snapshot.iconPath(), "app_icon.png", snapshot, staging, outputs);
            }
            Path settings = staging.resolve("data.json");
            Files.write(settings, new Gson().toJson(snapshot.settings()).getBytes(StandardCharsets.UTF_8));
            outputs.add(new Output(settings, snapshot.dataDirectory().toPath().resolve("data.json")));

            // Read and encode everything, and check destinations, before replacing any existing file.
            for (Output output : outputs) {
                Files.createDirectories(output.target().getParent());
                if (Files.exists(output.target(), LinkOption.NOFOLLOW_LINKS)
                        && !Files.isRegularFile(output.target(), LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Icon destination is not a regular file: " + output.target());
                }
            }
            for (Output output : outputs) {
                Path pending = Files.createTempFile(output.target().getParent(), ".icon-export-", ".tmp");
                try {
                    Files.copy(output.source(), pending, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(pending, output.target(), StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(pending);
                }
            }
        } finally {
            try (var entries = Files.walk(staging)) {
                for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(entry);
            }
        }
    }

    private static void stageSource(String source, String name, Snapshot snapshot,
                                    Path staging, List<Output> outputs) throws IOException {
        if (source == null) return;
        if (!Files.isRegularFile(new File(source).toPath())) throw new IOException("Icon source is unavailable: " + source);
        Path staged = staging.resolve(name);
        Files.copy(new File(source).toPath(), staged);
        outputs.add(new Output(staged, snapshot.dataDirectory().toPath().resolve(name)));
    }

    private static void encode(Bitmap bitmap, Path target) throws IOException {
        try (OutputStream output = Files.newOutputStream(target)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("PNG encoding failed");
            }
        }
    }
}
