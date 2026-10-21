package pro.sketchware.core.build.compiler;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElementBuilder;
import com.google.gson.internal.UnsafeAllocator;
import com.google.protobuf.MessageLite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import pro.sketchware.core.build.BuildSettings;
import pro.sketchware.core.build.ProjectBuilder;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.library.BuiltInLibraryManager;
import pro.sketchware.core.library.ManageLocalLibrary;
import pro.sketchware.core.project.ProjectSettings;
import pro.sketchware.core.project.ProguardHandler;
import pro.sketchware.core.project.SketchwarePaths;

/** Runs the actual shrinkers and bundletool, using only independently owned build inputs. */
@RunWith(AndroidJUnit4.class)
public class AppBundleMappingTest {
    private static final String ENTRY = "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map";
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static final byte[] ASSET = "unchanged bundle payload".getBytes(StandardCharsets.UTF_8);
    private static Path root, androidJar, compiled, globalRules;
    private static byte[] originalGlobalRules;
    private static long originalGlobalRulesTime;
    private static String originalAndroidRules, originalDefaultRules;

    @BeforeClass public static void prepare() throws Exception {
        root = Files.createTempDirectory(context().getCacheDir().toPath(), "bundle_mapping_");
        globalRules = Path.of(SketchwarePaths.getAndroidProguardRulesPath());
        if (Files.exists(globalRules)) {
            originalGlobalRules = Files.readAllBytes(globalRules);
            originalGlobalRulesTime = Files.getLastModifiedTime(globalRules).toMillis();
        }
        // Loading this class can initialize the default global rules. Restore them after the suite.
        originalAndroidRules = ProguardHandler.ANDROID_PROGUARD_RULES_PATH;
        originalDefaultRules = ProguardHandler.DEFAULT_PROGUARD_RULES_PATH;
        Path emptyRules = root.resolve("android-rules.pro");
        Files.write(emptyRules, new byte[0]);
        ProguardHandler.ANDROID_PROGUARD_RULES_PATH = emptyRules.toString();

        androidJar = root.resolve("android.jar");
        try (ZipInputStream zip = new ZipInputStream(context().getAssets().open("libs/android.jar.zip"))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith("android.jar")) {
                    Files.copy(zip, androidJar);
                    break;
                }
            }
        }
        assertTrue("Use the app's actual compile platform", Files.isRegularFile(androidJar));
        Path source = root.resolve("MappingProbe.java");
        Files.write(source, ("package test.mapping; public class MappingProbe { "
                + "public static int value() { return 7; } }").getBytes(StandardCharsets.UTF_8));
        compiled = Files.createDirectory(root.resolve("compiled"));
        StringWriter diagnostics = new StringWriter();
        var ecj = new org.eclipse.jdt.internal.compiler.batch.Main(
                new PrintWriter(diagnostics), new PrintWriter(diagnostics), false, null, null);
        assertTrue(diagnostics.toString(), ecj.compile(new String[]{"-1.7", "-proc:none", "-bootclasspath",
                androidJar.toString(), "-d", compiled.toString(), source.toString()}));
    }

    @AfterClass public static void cleanup() throws Exception {
        ProguardHandler.ANDROID_PROGUARD_RULES_PATH = originalAndroidRules;
        ProguardHandler.DEFAULT_PROGUARD_RULES_PATH = originalDefaultRules;
        if (originalGlobalRules == null) {
            Files.deleteIfExists(globalRules);
        } else {
            assertArrayEquals(originalGlobalRules, Files.readAllBytes(globalRules));
            assertEquals(originalGlobalRulesTime, Files.getLastModifiedTime(globalRules).toMillis());
        }
        deleteOwned(root, context().getCacheDir().toPath());
    }

    @Test public void r8MappingIsPackagedWithDebugFilesDisabled() throws Exception {
        try (Project project = new Project()) {
            project.config(true, true, false);
            project.builder.runR8();
            byte[] mapping = project.currentMapping();
            assertTrue(new String(mapping, StandardCharsets.UTF_8).contains("test.mapping.MappingProbe"));
            project.bundle(mapping);
            assertFalse(project.builder.proguard.isDebugFilesEnabled());
        }
    }

    @Test public void proguardDebugMappingIsStillPackaged() throws Exception {
        try (Project project = new Project()) {
            project.config(true, false, true);
            project.builder.runProguard();
            project.bundle(project.currentMapping());
        }
    }

    @Test public void proguardWithoutDebugCannotReuseThePreviousMapping() throws Exception {
        try (Project project = new Project()) {
            project.config(true, false, true);
            project.builder.runProguard();
            assertTrue(Files.isRegularFile(project.mapping));
            project.config(true, false, false);
            project.builder.runProguard();
            assertFalse("This run does not produce a mapping; discard the old one", Files.exists(project.mapping));
            project.bundle(null);
        }
    }

    @Test public void r8ReplacesThePreviousMappingWithCurrentOutput() throws Exception {
        try (Project project = new Project()) {
            project.config(true, true, true);
            Files.write(project.mapping, "OLD MAPPING".getBytes(StandardCharsets.UTF_8));
            project.builder.runR8();
            byte[] mapping = project.currentMapping();
            assertFalse(new String(mapping, StandardCharsets.UTF_8).contains("OLD MAPPING"));
            project.bundle(mapping);
        }
    }

    @Test public void disabledShrinkingIgnoresALeftoverMapping() throws Exception {
        try (Project project = new Project()) {
            project.config(false, true, true);
            Files.write(project.mapping, "OLD MAPPING".getBytes(StandardCharsets.UTF_8));
            project.bundle(null);
            assertEquals("OLD MAPPING", new String(project.currentMapping(), StandardCharsets.UTF_8));
        }
    }

    @Test public void absentMappingDoesNotBreakBundleCreation() throws Exception {
        try (Project project = new Project()) {
            project.config(true, false, true);
            project.bundle(null);
        }
    }

    @Test public void aDirectoryIsNotAddedAsMappingMetadata() throws Exception {
        try (Project project = new Project()) {
            project.config(true, true, true);
            Files.createDirectory(project.mapping);
            project.bundle(null);
            assertTrue(Files.isDirectory(project.mapping));
        }
    }

    @Test public void failedShrinkersInvalidateThePreviousMapping() throws Exception {
        for (boolean r8 : new boolean[]{false, true}) {
            try (Project project = new Project()) {
                project.config(true, r8, true);
                Files.write(project.mapping, "OLD MAPPING".getBytes(StandardCharsets.UTF_8));
                Files.write(project.rules, "-invalid-test-option".getBytes(StandardCharsets.UTF_8));
                assertThrows(java.io.IOException.class, () -> {
                    if (r8) project.builder.runR8(); else project.builder.runProguard();
                });
                assertFalse("Failed " + (r8 ? "R8" : "ProGuard") + " must not leave an old mapping", Files.exists(project.mapping));
            }
        }
    }

    private static final class Project implements AutoCloseable {
        final Path directory = Files.createTempDirectory(root, "project_");
        final Path config = directory.resolve("proguard.json");
        final Path rules = directory.resolve("rules.pro");
        final String id = "__bundle_mapping_" + UUID.randomUUID();
        final ProjectBuilder builder;
        final Path mapping;

        Project() throws Exception {
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", id); metadata.put("my_sc_pkg_name", "test.mapping");
            metadata.put("my_ws_name", "MappingTest"); metadata.put("my_app_name", "MappingTest");
            metadata.put("sc_ver_code", "1"); metadata.put("sc_ver_name", "1.0");
            ProjectFilePaths paths = new ProjectFilePaths(context(), directory.toString(), metadata);
            Files.createDirectories(Path.of(paths.compiledClassesPath, "test/mapping"));
            Files.copy(compiled.resolve("test/mapping/MappingProbe.class"),
                    Path.of(paths.compiledClassesPath, "test/mapping/MappingProbe.class"));
            Files.write(Path.of(paths.proguardAaptRules), new byte[0]);
            Files.write(rules, ("-dontoptimize\n-dontwarn\n-dontnote\n"
                    + "-keep,allowobfuscation class test.mapping.MappingProbe { public static int value(); }\n")
                    .getBytes(StandardCharsets.UTF_8));
            ProguardHandler.DEFAULT_PROGUARD_RULES_PATH = rules.toString();

            // Avoid ProjectBuilder's constructor resetting the user's debug log and System.out.
            // Supply the actual dependencies used by runR8/runProguard/buildBundle instead.
            builder = UnsafeAllocator.INSTANCE.newInstance(ProjectBuilder.class);
            builder.projectFilePaths = paths;
            builder.androidJarPath = androidJar.toString();
            builder.builtInLibraryManager = new BuiltInLibraryManager(id);
            builder.localLibraryManager = new ManageLocalLibrary(id);
            builder.settings = new ProjectSettings(id);
            set(builder.settings, ProjectSettings.class, "settings", new HashMap<>(Map.of("min_sdk", "26")));
            builder.buildSettings = new BuildSettings(id);
            set(builder.buildSettings, ProjectSettings.class, "settings", new HashMap<>(Map.of("no_http_legacy", "true")));
            builder.proguard = UnsafeAllocator.INSTANCE.newInstance(ProguardHandler.class);
            set(builder.proguard, ProguardHandler.class, "config_path", config.toString());
            set(builder.proguard, ProguardHandler.class, "fm_config_path", directory.resolve("full-mode.json").toString());
            mapping = Path.of(paths.proguardMappingPath);
            config(false, false, false);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(directory.resolve("bin/module-main.zip")))) {
                zip.putNextEntry(new ZipEntry("manifest/AndroidManifest.xml"));
                zip.write(manifest()); zip.closeEntry();
                zip.putNextEntry(new ZipEntry("assets/probe.txt"));
                zip.write(ASSET); zip.closeEntry();
            }
        }

        void config(boolean enabled, boolean r8, boolean debug) throws Exception {
            Files.write(config, ("{\"enabled\":\"" + enabled + "\",\"r8\":\"" + r8
                    + "\",\"debug\":\"" + debug + "\"}").getBytes(StandardCharsets.UTF_8));
            Files.setLastModifiedTime(config, FileTime.fromMillis(1600000000000L));
        }

        byte[] currentMapping() throws Exception { return Files.readAllBytes(mapping); }

        void bundle(byte[] expectedMapping) throws Exception {
            byte[] settingsBefore = Files.readAllBytes(config);
            new AppBundleCompiler(builder).buildBundle();
            try (ZipFile zip = new ZipFile(AppBundleCompiler.getDefaultAppBundleOutputFile(builder.projectFilePaths))) {
                ZipEntry entry = zip.getEntry(ENTRY);
                if (expectedMapping == null) assertNull(entry);
                else {
                    assertNotNull("Bundle must carry the current mapping", entry);
                    try (InputStream input = zip.getInputStream(entry)) { assertArrayEquals(expectedMapping, read(input)); }
                }
                assertNotNull(zip.getEntry("base/manifest/AndroidManifest.xml"));
                try (InputStream input = zip.getInputStream(zip.getEntry("base/assets/probe.txt"))) { assertArrayEquals(ASSET, read(input)); }
                assertNull("Mapping is metadata, not application payload", zip.getEntry("base/assets/proguard.map"));
            }
            assertArrayEquals(settingsBefore, Files.readAllBytes(config));
            assertEquals(1600000000000L, Files.getLastModifiedTime(config).toMillis());
        }

        @Override public void close() throws Exception { deleteOwned(directory, root); }
    }

    private static byte[] manifest() throws Exception {
        XmlProtoElementBuilder manifest = XmlProtoElementBuilder.create("manifest");
        manifest.addNamespaceDeclaration("android", ANDROID);
        manifest.getOrCreateAttribute("package").setValueAsString("test.mapping");
        manifest.getOrCreateAndroidAttribute("versionCode", 0x0101021b).setValueAsDecimalInteger(1);
        manifest.getOrCreateChildElement("application").getOrCreateAndroidAttribute("hasCode", 0x0101000c)
                .setValueAsBoolean(false);
        // The AAPT message classes are runtime dependencies of bundletool, not app compile dependencies.
        MessageLite proto = (MessageLite) XmlProtoElementBuilder.class.getMethod("toXmlNode").invoke(manifest);
        return proto.toByteArray();
    }

    private static byte[] read(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }
    private static void set(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void deleteOwned(Path directory, Path parent) throws Exception {
        Path canonical = directory.toFile().getCanonicalFile().toPath();
        assertEquals(parent.toFile().getCanonicalFile().toPath(), canonical.getParent());
        try (Stream<Path> paths = Files.walk(canonical)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
