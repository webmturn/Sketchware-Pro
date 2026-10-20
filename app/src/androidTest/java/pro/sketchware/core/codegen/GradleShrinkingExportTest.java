package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
public class GradleShrinkingExportTest {
    @Test public void enabledExportReflectsEitherLocalShrinkerEngine() throws Exception {
        try (Project project = new Project()) {
            for (boolean r8 : new boolean[]{false, true}) {
                project.write("{\"enabled\":\"true\",\"r8\":\"" + r8 + "\"}");
                assertMinify(project.generate(false), true);
                project.assertUnchanged();
            }
        }
    }

    @Test public void anEnabledEngineDoesNotEnableDisabledShrinking() throws Exception {
        try (Project project = new Project()) {
            project.write("{\"enabled\":\"false\",\"r8\":\"true\",\"debug\":\"true\"}");
            assertMinify(project.generate(false), false);
            project.assertUnchanged();
        }
    }

    @Test public void absentSettingsStayAbsentAndExportWithShrinkingDisabled() throws Exception {
        try (Project project = new Project()) {
            assertFalse(project.config.exists());
            assertMinify(project.generate(false), false);
            assertFalse(project.config.exists());
            assertFalse(new File(SketchwarePaths.getProjectProguardRulesPath(project.id)).exists());
        }
    }

    @Test public void incompleteAndInvalidSettingsDoNotBreakExportOrGetRewritten() throws Exception {
        try (Project project = new Project()) {
            for (String content : new String[]{"", " \n\t ", "null", "{}", "{\"debug\":true}",
                    "[{broken", "[]", "{\"enabled\":null}", "{\"enabled\":{}}"}) {
                project.write(content);
                assertMinify(project.generate(false), false);
                project.assertUnchanged();
            }
        }
    }

    @Test public void togglingShrinkingChangesOnlyTheMinifyDirective() throws Exception {
        try (Project project = new Project()) {
            project.write("{\"enabled\":\"true\"}");
            String enabled = project.generate(true);
            assertMinify(enabled, true);
            assertTrue(enabled.contains("viewBinding true"));
            assertTrue(enabled.contains("proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'"));
            project.write("{\"enabled\":\"false\"}");
            String disabled = project.generate(true);
            assertMinify(disabled, false);
            assertEquals(enabled.replace("minifyEnabled true", "minifyEnabled false"), disabled);
            project.assertUnchanged();
        }
    }

    @Test public void writingGradleFilesPreservesEnabledShrinkSetting() throws Exception {
        try (Project project = new Project()) {
            project.write("{\"enabled\":\"true\",\"r8\":\"false\"}");
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", project.id);
            metadata.put("my_sc_pkg_name", "test.gradle.shrinking");
            metadata.put("my_ws_name", "GradleShrinkingTest");
            metadata.put("my_app_name", "GradleShrinkingTest");
            metadata.put("sc_ver_code", "1");
            metadata.put("sc_ver_name", "1.0");
            ProjectFilePaths paths = new ProjectFilePaths(context(), project.output.toString(), metadata);
            paths.buildConfig.packageName = "test.gradle.shrinking";
            paths.buildConfig.versionCode = "1";
            paths.buildConfig.versionName = "1.0";
            paths.generateGradleFiles();
            String gradle = new String(Files.readAllBytes(project.output.resolve("app/build.gradle")), StandardCharsets.UTF_8);
            assertMinify(gradle, true);
            assertTrue(Files.isRegularFile(project.output.resolve("settings.gradle")));
            assertTrue(Files.isRegularFile(project.output.resolve("build.gradle")));
            assertTrue(Files.isRegularFile(project.output.resolve("gradle.properties")));
            project.assertUnchanged();
        }
    }

    private static void assertMinify(String gradle, boolean expected) {
        var directive = Pattern.compile("\\bminifyEnabled\\s+(true|false)\\b").matcher(gradle);
        assertTrue("The release configuration must contain minifyEnabled", directive.find());
        assertEquals(Boolean.toString(expected), directive.group(1));
        assertFalse("Only one minify directive should be generated", directive.find());
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static final class Project implements AutoCloseable {
        final String id = "__gradle_shrinking_" + UUID.randomUUID();
        final File config = new File(SketchwarePaths.getProjectProguardConfigPath(id));
        final Path root = config.getParentFile().toPath();
        final Path output = Files.createTempDirectory(context().getCacheDir().toPath(), "gradle_shrinking_");
        final ArrayList<File> createdParents = new ArrayList<>();
        byte[] original;
        long modified;

        Project() throws Exception {
            assertEquals(id, root.getFileName().toString());
            assertFalse("Do not overwrite an existing project", Files.exists(root));
            createParents(root.toFile().getParentFile());
            Files.createDirectory(root);
        }

        void createParents(File directory) throws Exception {
            if (directory.isDirectory()) return;
            createParents(directory.getParentFile());
            Files.createDirectory(directory.toPath());
            createdParents.add(directory);
        }

        void write(String content) throws Exception {
            original = content.getBytes(StandardCharsets.UTF_8);
            Files.write(config.toPath(), original);
            Files.setLastModifiedTime(config.toPath(), FileTime.fromMillis(1600000000000L));
            modified = config.lastModified();
        }

        String generate(boolean viewBinding) {
            BuildConfig metadata = new BuildConfig();
            metadata.sc_id = id;
            metadata.packageName = "test.gradle.shrinking";
            metadata.versionCode = "1";
            metadata.versionName = "1.0";
            return GradleFileGenerator.getBuildGradleString(36, 26, "36", metadata, viewBinding);
        }

        void assertUnchanged() throws Exception {
            assertArrayEquals(original, Files.readAllBytes(config.toPath()));
            assertEquals(modified, config.lastModified());
            assertFalse("Export must not initialize shrinker rules", new File(
                    SketchwarePaths.getProjectProguardRulesPath(id)).exists());
        }

        @Override public void close() throws Exception {
            assertEquals(id, root.getFileName().toString());
            assertEquals(context().getCacheDir().getCanonicalFile(), output.toFile().getCanonicalFile().getParentFile());
            for (Path owned : new Path[]{root, output}) {
                try (Stream<Path> paths = Files.walk(owned)) {
                    for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                        assertTrue(path.startsWith(owned));
                        Files.delete(path);
                    }
                }
            }
            for (int i = createdParents.size() - 1; i >= 0; i--) Files.delete(createdParents.get(i).toPath());
        }
    }
}
