package pro.sketchware.core.build.compiler;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.android.tools.r8.CompilationMode;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Small wrapper to run D8 programmatically. This is intentionally minimal — it
 * collects input program files and runs D8 to produce dex outputs in the
 * specified output directory. The method throws exceptions on failure so the
 * caller can surface errors to the build log.
 */
public final class D8Compiler {

    private D8Compiler() { }

    /**
     * Run D8 on the provided list of input files (jars/dirs) and write dex
     * output into outputDir.
     *
     * @param inputFiles list of jar/dir inputs
     * @param outputDir output directory for .dex files (created if missing)
     * @param minify when true, use RELEASE/opt mode (R8 behavior); otherwise DEBUG
     * @throws Exception on failure
     */
    public static void runD8(List<File> inputFiles, File outputDir, boolean minify) throws Exception {
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("inputFiles must not be empty");
        }

        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IllegalStateException("Failed to create output dir: " + outputDir);
            }
        }

        D8Command.Builder builder = D8Command.builder();

        for (File f : inputFiles) {
            Path p = f.toPath();
            builder.addProgramFiles(p);
        }

        builder.setOutput(outputDir.toPath(), OutputMode.DexIndexed);
        builder.setMode(minify ? CompilationMode.RELEASE : CompilationMode.DEBUG);

        // Run D8 — any exception is propagated to the caller to be logged by the build
        D8.run(builder.build());
    }
}
