package pro.sketchware.core.build.compiler;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElementBuilder;
import com.google.gson.internal.UnsafeAllocator;
import com.google.protobuf.MessageLite;

import dalvik.system.DexClassLoader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import mod.agus.jcoderz.dex.Dex;
import pro.sketchware.core.build.ProjectBuilder;
import pro.sketchware.core.build.BuildSettings;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.library.BuiltInLibraryManager;
import pro.sketchware.core.library.ManageLocalLibrary;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectSettings;
import pro.sketchware.core.project.ProguardHandler;
import pro.sketchware.core.project.SketchwarePaths;

/** Real ECJ and D8 inputs, isolated in cache; never starts the user's build log. */
@RunWith(AndroidJUnit4.class)
public class DexMergeTest {
    private static Path root, androidJar;
    private static File largeFirst, largeSecond;
    private static Path globalRules;
    private static byte[] originalGlobalRules;
    private static long originalGlobalRulesTime;

    @BeforeClass public static void prepare() throws Exception {
        root = Files.createTempDirectory(context().getCacheDir().toPath(), "dex_merge_test_");
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
        assertTrue(Files.isRegularFile(androidJar));
        globalRules = Path.of(SketchwarePaths.getAndroidProguardRulesPath());
        if (Files.exists(globalRules)) {
            originalGlobalRules = Files.readAllBytes(globalRules);
            originalGlobalRulesTime = Files.getLastModifiedTime(globalRules).toMillis();
        }
        // Keep the large inputs for all cases, inside this suite's private cache directory.
        Workspace large = new Workspace(21);
        largeFirst = large.dex("LargeFirst", 33000, 31);
        largeSecond = large.dex("LargeSecond", 33000, 32);
        assertTrue(new Dex(largeFirst).getTableOfContents().methodIds.size
                + new Dex(largeSecond).getTableOfContents().methodIds.size > 65535);
    }

    @AfterClass public static void cleanup() throws Exception {
        if (globalRules != null) {
            if (originalGlobalRules == null) Files.deleteIfExists(globalRules);
            else {
                assertArrayEquals(originalGlobalRules, Files.readAllBytes(globalRules));
                assertEquals(originalGlobalRulesTime, Files.getLastModifiedTime(globalRules).toMillis());
            }
        }
        if (root != null) deleteOwned(root, context().getCacheDir().toPath());
    }

    @Test public void modernD8InputsMergeAndRemainExecutable() throws Exception {
        try (Workspace workspace = new Workspace(26)) {
            File first = workspace.dex("First", 400, 11);
            File second = workspace.dex("Second", 400, 22);
            workspace.merge(List.of(first, second));
            assertEquals(2, classNames(workspace.outputs()).size());
            assertEquals(11, workspace.value("First", "m399"));
            assertEquals(22, workspace.value("Second", "m399"));
        }
    }

    @Test public void duplicateClassesKeepTheFirstInputDefinition() throws Exception {
        try (Workspace workspace = new Workspace(26)) {
            File first = workspace.dex("Duplicate", 1, 11);
            File second = workspace.dex("Duplicate", 1, 22);
            workspace.merge(List.of(first, second));
            assertEquals(List.of("Ltest/merge/Duplicate;"), classNames(workspace.outputs()));
            assertEquals(11, workspace.value("Duplicate", "m0"));
            workspace.merge(List.of(second, first));
            assertEquals(22, workspace.value("Duplicate", "m0"));
        }
    }

    @Test public void legacyMinSdkStillProducesReadableDex() throws Exception {
        try (Workspace workspace = new Workspace(19)) {
            workspace.merge(List.of(workspace.dex("LegacyFirst", 2, 11), workspace.dex("LegacySecond", 2, 22)));
            assertEquals(2, classNames(workspace.outputs()).size());
            assertEquals(11, workspace.value("LegacyFirst", "m1"));
            assertEquals(22, workspace.value("LegacySecond", "m1"));
        }
    }

    @Test public void removingInputsRemovesObsoleteDexFromApkAndBundle() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            workspace.input(largeFirst, "first.dex");
            Path removedInput = workspace.input(largeSecond, "second.dex");
            workspace.builder.getDexFilesReady();
            assertEquals(2, workspace.outputs().size());
            Files.delete(removedInput);
            workspace.builder.getDexFilesReady();
            assertEquals(List.of("Ltest/merge/LargeFirst;"), classNames(workspace.outputs()));
            workspace.resources();
            workspace.builder.buildApk();
            workspace.assertArchive(Path.of(workspace.builder.projectFilePaths.unsignedUnalignedApkPath), "");
            AppBundleCompiler bundle = new AppBundleCompiler(workspace.builder);
            bundle.createModuleMainArchive();
            bundle.buildBundle();
            workspace.assertArchive(AppBundleCompiler.getDefaultAppBundleOutputFile(workspace.builder.projectFilePaths).toPath(), "base/dex/");
        }
    }

    @Test public void cacheHitRequiresEverySecondaryDexAndRejectsExtraOutput() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            workspace.input(largeFirst, "first.dex");
            workspace.input(largeSecond, "second.dex");
            workspace.builder.getDexFilesReady();
            Path second = workspace.output.resolve("classes2.dex");
            assertTrue(Files.isRegularFile(second));
            byte[] fingerprint = Files.readAllBytes(workspace.fingerprint());
            long firstTime = Files.getLastModifiedTime(workspace.output.resolve("classes.dex")).toMillis();
            long secondTime = Files.getLastModifiedTime(second).toMillis();
            android.os.SystemClock.sleep(25);
            workspace.builder.getDexFilesReady();
            assertArrayEquals(fingerprint, Files.readAllBytes(workspace.fingerprint()));
            assertEquals(firstTime, Files.getLastModifiedTime(workspace.output.resolve("classes.dex")).toMillis());
            assertEquals(secondTime, Files.getLastModifiedTime(second).toMillis());
            Files.delete(second);
            workspace.builder.getDexFilesReady();
            assertTrue("Missing secondary output must be regenerated", Files.isRegularFile(second));
            assertEquals(32, workspace.value("LargeSecond", "m32999"));
            Files.copy(largeSecond.toPath(), workspace.output.resolve("classes17.dex"));
            workspace.builder.getDexFilesReady();
            assertFalse(Files.exists(workspace.output.resolve("classes17.dex")));
            assertEquals(2, workspace.outputs().size());
            Files.write(workspace.output.resolve("classes.dex"), new byte[]{1, 2, 3});
            workspace.builder.getDexFilesReady();
            assertEquals(List.of("Ltest/merge/LargeFirst;", "Ltest/merge/LargeSecond;"), classNames(workspace.outputs()));
        }
    }

    @Test public void failedMergePreservesPublishedFilesAndInvalidatesCache() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            workspace.merge(List.of(workspace.dex("Published", 1, 7)));
            Path unrelated = workspace.output.resolve("mapping.txt");
            byte[] metadata = "unrelated build metadata".getBytes(StandardCharsets.UTF_8);
            Files.write(unrelated, metadata);
            byte[] previous = Files.readAllBytes(workspace.output.resolve("classes.dex"));
            Files.write(workspace.fingerprint(), "old cache".getBytes(StandardCharsets.UTF_8));
            File missing = workspace.directory.resolve("missing.dex").toFile();
            assertThrows(Exception.class, () -> workspace.merge(List.of(largeFirst, largeSecond, missing)));
            assertArrayEquals("A failed later chunk must not replace the first published DEX", previous,
                    Files.readAllBytes(workspace.output.resolve("classes.dex")));
            assertFalse("Failed output must not be reusable as cached", Files.exists(workspace.fingerprint()));
            assertEquals(1, workspace.outputs().size());
            workspace.assertNoStaging();
            workspace.merge(List.of(largeFirst, largeSecond));
            assertEquals(2, workspace.outputs().size());
            assertEquals(32, workspace.value("LargeSecond", "m32999"));
            assertArrayEquals(metadata, Files.readAllBytes(unrelated));
        }
    }

    @Test public void debugSingleInputDoesNotPackagePreviousReleaseDex() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            workspace.input(workspace.dex("DebugCurrent", 1, 9), "current.dex");
            Files.copy(largeFirst.toPath(), workspace.output.resolve("classes.dex"));
            Files.copy(largeSecond.toPath(), workspace.output.resolve("classes2.dex"));
            workspace.builder.projectFilePaths.buildConfig.isDebugBuild = true;
            workspace.builder.getDexFilesReady();
            workspace.resources();
            workspace.builder.buildApk();
            try (ZipFile apk = new ZipFile(workspace.builder.projectFilePaths.unsignedUnalignedApkPath)) {
                assertNotNull(apk.getEntry("classes.dex"));
                assertNull("The unmerged debug path must not fall back to stale merged DEX files", apk.getEntry("classes2.dex"));
                Path extracted = workspace.directory.resolve("debug.dex");
                try (var input = apk.getInputStream(apk.getEntry("classes.dex"))) { Files.copy(input, extracted); }
                assertEquals(List.of("Ltest/merge/DebugCurrent;"), classNames(List.of(extracted.toFile())));
            }
        }
    }

    @Test public void debugInputsStayInOrderAcrossPackagingRetriesAndReleaseSwitch() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            workspace.input(workspace.dex("DebugFirst", 1, 11), "first.dex");
            workspace.input(workspace.dex("DebugSecond", 1, 22), "second.dex");
            workspace.builder.projectFilePaths.buildConfig.isDebugBuild = true;
            workspace.builder.getDexFilesReady();
            workspace.resources();
            for (int retry = 0; retry < 2; retry++) {
                workspace.builder.buildApk();
                try (ZipFile apk = new ZipFile(workspace.builder.projectFilePaths.unsignedUnalignedApkPath)) {
                    for (int number = 1; number <= 2; number++) {
                        ZipEntry entry = apk.getEntry(number == 1 ? "classes.dex" : "classes2.dex");
                        assertNotNull("Both debug inputs must survive packaging retry " + retry, entry);
                        Path extracted = Files.createTempFile(workspace.directory, "debug_order_", ".dex");
                        try (var input = apk.getInputStream(entry)) { Files.copy(input, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                        assertEquals(List.of(number == 1 ? "Ltest/merge/DebugFirst;" : "Ltest/merge/DebugSecond;"), classNames(List.of(extracted.toFile())));
                    }
                }
            }
            workspace.builder.projectFilePaths.buildConfig.isDebugBuild = false;
            workspace.builder.getDexFilesReady();
            workspace.builder.buildApk();
            try (ZipFile apk = new ZipFile(workspace.builder.projectFilePaths.unsignedUnalignedApkPath)) {
                assertNull(apk.getEntry("classes2.dex"));
                Path extracted = workspace.directory.resolve("release.dex");
                try (var input = apk.getInputStream(apk.getEntry("classes.dex"))) { Files.copy(input, extracted); }
                assertEquals(List.of("Ltest/merge/DebugFirst;", "Ltest/merge/DebugSecond;"), classNames(List.of(extracted.toFile())));
            }
        }
    }

    @Test public void emptyDebugInputDoesNotReuseOldReleaseOutput() throws Exception {
        try (Workspace workspace = new Workspace(21)) {
            Files.copy(largeFirst.toPath(), workspace.output.resolve("classes.dex"));
            workspace.builder.projectFilePaths.buildConfig.isDebugBuild = true;
            assertThrows(java.io.IOException.class, workspace.builder::getDexFilesReady);
        }
    }

    private static final class Workspace implements AutoCloseable {
        final Path directory = Files.createTempDirectory(root, "case_");
        final Path output = Files.createDirectory(directory.resolve("output"));
        final ProjectBuilder builder;
        final int minApi;

        Workspace(int minApi) throws Exception {
            this.minApi = minApi;
            builder = UnsafeAllocator.INSTANCE.newInstance(ProjectBuilder.class);
            builder.settings = UnsafeAllocator.INSTANCE.newInstance(ProjectSettings.class);
            set(builder.settings, ProjectSettings.class, "settings", new HashMap<>(Map.of("min_sdk", String.valueOf(minApi))));
            builder.projectFilePaths = UnsafeAllocator.INSTANCE.newInstance(ProjectFilePaths.class);
            ProjectFilePaths paths = builder.projectFilePaths;
            String id = "__dex_merge_" + UUID.randomUUID();
            set(paths, ProjectFilePaths.class, "sc_id", id);
            set(paths, ProjectFilePaths.class, "binDirectoryPath", output.toString());
            set(paths, ProjectFilePaths.class, "classesDexPath", output.resolve("classes.dex").toString());
            set(paths, ProjectFilePaths.class, "unsignedUnalignedApkPath", output.resolve("test.apk").toString());
            set(paths, ProjectFilePaths.class, "resourcesApkPath", output.resolve("resources.ap_").toString());
            set(paths, ProjectFilePaths.class, "projectName", "DexMergeTest");
            set(paths, ProjectFilePaths.class, "proguardMappingPath", output.resolve("mapping.txt").toString());
            set(paths, ProjectFilePaths.class, "buildConfig", UnsafeAllocator.INSTANCE.newInstance(BuildConfig.class));
            builder.buildSettings = UnsafeAllocator.INSTANCE.newInstance(BuildSettings.class);
            set(builder.buildSettings, ProjectSettings.class, "settings", new HashMap<>(Map.of("no_http_legacy", "true")));
            builder.builtInLibraryManager = UnsafeAllocator.INSTANCE.newInstance(BuiltInLibraryManager.class);
            set(builder.builtInLibraryManager, BuiltInLibraryManager.class, "libraries", new ArrayList<>());
            builder.localLibraryManager = UnsafeAllocator.INSTANCE.newInstance(ManageLocalLibrary.class);
            builder.localLibraryManager.list = new ArrayList<>();
            set(builder, ProjectBuilder.class, "dexesToAddButNotMerge", new ArrayList<>());
            Files.createDirectory(output.resolve("dex"));
        }

        Path input(File source, String name) throws Exception {
            Path input = output.resolve("dex").resolve(name);
            Files.copy(source.toPath(), input);
            return input;
        }

        Path fingerprint() { return output.resolve("dex_merge_fingerprint"); }

        void resources() throws Exception {
            XmlProtoElementBuilder manifest = XmlProtoElementBuilder.create("manifest");
            manifest.addNamespaceDeclaration("android", "http://schemas.android.com/apk/res/android");
            manifest.getOrCreateAttribute("package").setValueAsString("test.merge");
            manifest.getOrCreateAndroidAttribute("versionCode", 0x0101021b).setValueAsDecimalInteger(1);
            manifest.getOrCreateChildElement("application");
            MessageLite proto = (MessageLite) XmlProtoElementBuilder.class.getMethod("toXmlNode").invoke(manifest);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(Path.of(builder.projectFilePaths.resourcesApkPath)))) {
                zip.putNextEntry(new ZipEntry("AndroidManifest.xml")); zip.write(proto.toByteArray()); zip.closeEntry();
                zip.putNextEntry(new ZipEntry("assets/probe.txt")); zip.write("keep asset".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
            builder.proguard = UnsafeAllocator.INSTANCE.newInstance(ProguardHandler.class);
            Path config = directory.resolve("proguard.json");
            Files.write(config, "{\"enabled\":\"false\"}".getBytes(StandardCharsets.UTF_8));
            set(builder.proguard, ProguardHandler.class, "config_path", config.toString());
        }

        void assertArchive(Path path, String dexPrefix) throws Exception {
            try (ZipFile zip = new ZipFile(path.toFile())) {
                assertNotNull(zip.getEntry(dexPrefix + "classes.dex"));
                assertNull(zip.getEntry(dexPrefix + "classes2.dex"));
                Path extracted = Files.createTempFile(directory, "archive_", ".dex");
                try (var input = zip.getInputStream(zip.getEntry(dexPrefix + "classes.dex"))) { Files.copy(input, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
                assertEquals(List.of("Ltest/merge/LargeFirst;"), classNames(List.of(extracted.toFile())));
            }
        }

        void assertNoStaging() throws Exception {
            try (Stream<Path> children = Files.list(output)) {
                assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".dex-merge-")));
            }
        }

        File dex(String className, int methods, int value) throws Exception {
            Path input = Files.createTempDirectory(directory, "input_");
            StringBuilder java = new StringBuilder("package test.merge; public class ").append(className).append(" {");
            // Exercise compact debug information with identical source positions in many methods.
            for (int index = 0; index < methods; index++) {
                java.append("public static int m").append(index).append("(){return ").append(value).append(";}");
            }
            java.append('}');
            Path source = input.resolve(className + ".java");
            Files.write(source, java.toString().getBytes(StandardCharsets.UTF_8));
            Path classes = Files.createDirectory(input.resolve("classes"));
            StringWriter diagnostics = new StringWriter();
            var ecj = new org.eclipse.jdt.internal.compiler.batch.Main(new PrintWriter(diagnostics), new PrintWriter(diagnostics), false, null, null);
            boolean compiled = ecj.compile(new String[]{"-1.7", "-g", "-proc:none", "-bootclasspath", androidJar.toString(),
                    "-d", classes.toString(), source.toString()});
            assertTrue(diagnostics.toString(), compiled);
            Path dex = Files.createDirectory(input.resolve("dex"));
            D8.run(D8Command.builder().setMode(CompilationMode.RELEASE).setMinApiLevel(minApi).setIntermediate(true)
                    .addLibraryFiles(androidJar).addProgramFiles(classes.resolve("test/merge/" + className + ".class"))
                    .setOutput(dex, OutputMode.DexIndexed).build());
            return dex.resolve("classes.dex").toFile();
        }

        void merge(List<File> inputs) throws Exception {
            Method merge = ProjectBuilder.class.getDeclaredMethod("dexLibraries", File.class, List.class);
            merge.setAccessible(true);
            try {
                merge.invoke(builder, output.toFile(), inputs);
            } catch (InvocationTargetException error) {
                if (error.getCause() instanceof Exception) throw (Exception) error.getCause();
                throw error;
            }
        }

        List<File> outputs() {
            File[] files = output.toFile().listFiles((directory, name) -> name.matches("classes(?:[0-9]+)?\\.dex"));
            assertNotNull(files);
            Arrays.sort(files, Comparator.comparing(File::getName));
            return Arrays.asList(files);
        }

        int value(String className, String method) throws Exception {
            String dexPath = outputs().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator));
            ClassLoader loader = new DexClassLoader(dexPath, directory.toString(), null, context().getClassLoader());
            return (Integer) loader.loadClass("test.merge." + className).getMethod(method).invoke(null);
        }

        @Override public void close() throws Exception { deleteOwned(directory, root); }
    }

    private static List<String> classNames(List<File> files) throws Exception {
        List<String> names = new ArrayList<>();
        for (File file : files) {
            Dex dex = new Dex(file);
            dex.classDefs().forEach(definition -> names.add(dex.typeNames().get(definition.getTypeIndex())));
        }
        names.sort(String::compareTo);
        return names;
    }

    private static void set(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static void deleteOwned(Path directory, Path parent) throws Exception {
        Path canonical = directory.toFile().getCanonicalFile().toPath();
        assertEquals(parent.toFile().getCanonicalFile().toPath(), canonical.getParent());
        try (Stream<Path> files = Files.walk(canonical)) {
            for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
