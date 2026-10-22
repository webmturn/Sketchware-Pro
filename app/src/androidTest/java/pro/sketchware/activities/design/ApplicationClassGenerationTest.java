package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.InputSource;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import dalvik.system.DexClassLoader;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.codegen.ManifestGenerator;
import pro.sketchware.core.library.BuiltInLibraryManager;
import pro.sketchware.core.project.ProjectSettings;
import pro.sketchware.core.project.SketchwarePaths;

/** Compiles production Application/SketchLogger output and loads the manifest's class on Android. */
@RunWith(AndroidJUnit4.class)
public class ApplicationClassGenerationTest {
    private static final String PACKAGE = "test.application.generated";
    private static Path root, androidJar;

    @BeforeClass public static void prepare() throws Exception {
        root = Files.createTempDirectory(context().getCacheDir().toPath(), "application_generation_");
        androidJar = root.resolve("android.jar");
        try (ZipInputStream zip = new ZipInputStream(context().getAssets().open("libs/android.jar.zip"))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith("android.jar")) {
                    Files.copy(zip, androidJar); break;
                }
            }
        }
        assertTrue(Files.isRegularFile(androidJar));
    }

    @AfterClass public static void cleanup() throws Exception {
        deleteOwned(root, context().getCacheDir().toPath());
    }

    @Test public void defaultApplicationLoads() throws Exception {
        verify(".SketchApplication", PACKAGE + ".SketchApplication", false, false);
    }

    @Test public void simpleClassNameLoads() throws Exception {
        verify("MyApplication", PACKAGE + ".MyApplication", false, false);
    }

    @Test public void relativeSubpackageLoads() throws Exception {
        verify(".custom.MyApplication", PACKAGE + ".custom.MyApplication", false, false);
    }

    @Test public void relativePackageWithDollarSignLoads() throws Exception {
        verify(".custom$.MyApplication", PACKAGE + ".custom$.MyApplication", true, false);
    }

    @Test public void fullyQualifiedProjectClassLoads() throws Exception {
        verify(PACKAGE + ".MyApplication", PACKAGE + ".MyApplication", false, false);
    }

    @Test public void fullyQualifiedProjectSubpackageLoads() throws Exception {
        verify(PACKAGE + ".custom.MyApplication", PACKAGE + ".custom.MyApplication", false, false);
    }

    @Test public void fullClassNameOutsideProjectPackageIsNotPrefixed() throws Exception {
        verifyLibrary(false, false, false);
    }

    @Test public void libraryApplicationNeedsNoStaticContextGetterForLogging() throws Exception {
        verifyLibrary(true, false, false);
    }

    @Test public void libraryContextGetterRemainsOptionalAndCompatible() throws Exception {
        verifyLibrary(true, false, true);
    }

    @Test public void staleExternalApplicationAndLoggerAreReconciledWithLoggingOff() throws Exception {
        verifyLibrary(false, true, false);
    }

    @Test public void loggerUsesTheFullyQualifiedApplicationClass() throws Exception {
        verify(PACKAGE + ".custom.MyApplication", PACKAGE + ".custom.MyApplication", true, false);
    }

    @Test public void explicitDefaultClassWorksWithLogging() throws Exception {
        verify(PACKAGE + ".SketchApplication", PACKAGE + ".SketchApplication", true, false);
    }

    @Test public void existingRelativeSubpackageSourceIsPreserved() throws Exception {
        verify(".custom.MyApplication", PACKAGE + ".custom.MyApplication", true, true);
    }

    @Test public void existingFullyQualifiedProjectSourceIsPreserved() throws Exception {
        verify(PACKAGE + ".custom.MyApplication", PACKAGE + ".custom.MyApplication", true, true);
    }

    @Test public void existingExternalPackageSourceIsPreserved() throws Exception {
        verify("custom.MyApplication", "custom.MyApplication", true, true);
    }

    @Test public void addingCustomSourceRemovesOnlyItsPreviouslyGeneratedCopy() throws Exception {
        String name = PACKAGE + ".custom.MyApplication";
        try (Project project = new Project(name, name, true, true)) {
            Path stale = Path.of(project.paths.javaFilesPath, name.replace('.', '/') + ".java");
            Files.createDirectories(stale.getParent());
            Files.write(stale, "old generated content".getBytes(StandardCharsets.UTF_8));
            Path sentinel = Path.of(project.paths.javaFilesPath, "keep.txt");
            Files.write(sentinel, "keep".getBytes(StandardCharsets.UTF_8));
            project.paths.generateDebugFiles(context());
            assertFalse(Files.exists(stale));
            assertEquals("keep", read(sentinel));
            assertEquals(project.customContent, read(project.customFile));
            ClassLoader loader = project.compile();
            assertTrue(loader.loadClass(name).getField("CUSTOM_SOURCE").getBoolean(null));
        }
    }

    private static void verifyLibrary(boolean logging, boolean staleOutput, boolean getter) throws Exception {
        String name = "custom.MyApplication";
        try (Project project = new Project(name, name, logging, false)) {
            Path jar = project.library(getter);
            byte[] libraryBytes = Files.readAllBytes(jar);
            Path generatedApplication = Path.of(project.paths.javaFilesPath, "custom/MyApplication.java");
            Path loggerFile = Path.of(project.paths.javaFilesPath, PACKAGE.replace('.', '/'), "SketchLogger.java");
            if (staleOutput) {
                Files.createDirectories(generatedApplication.getParent());
                Files.write(generatedApplication, ("package custom; public class MyApplication extends android.app.Application {}")
                        .getBytes(StandardCharsets.UTF_8));
                Files.createDirectories(loggerFile.getParent());
                Files.write(loggerFile, ("package " + PACKAGE + "; public class SketchLogger {"
                        + " public static android.content.Context context() { return custom.MyApplication.getContext(); } }")
                        .getBytes(StandardCharsets.UTF_8));
            }
            project.paths.generateDebugFiles(context());
            assertEquals(name, manifestClassName(project.paths));
            assertFalse("A library's Application must not be generated again", Files.exists(generatedApplication));
            assertArrayEquals(libraryBytes, Files.readAllBytes(jar));
            Path caller = Path.of(project.paths.javaFilesPath, PACKAGE.replace('.', '/'), "UsesLibraryApplication.java");
            Files.createDirectories(caller.getParent());
            Files.write(caller, ("package " + PACKAGE + "; public class UsesLibraryApplication {"
                    + " public static boolean libraryApi() { return custom.MyApplication.LIBRARY_SOURCE; } }")
                    .getBytes(StandardCharsets.UTF_8));
            ClassLoader loader = project.compile(List.of(jar));
            Application application = new Instrumentation().newApplication(loader, name, context());
            assertTrue(application.getClass().getField("LIBRARY_SOURCE").getBoolean(null));
            assertEquals(true, loader.loadClass(PACKAGE + ".UsesLibraryApplication").getMethod("libraryApi").invoke(null));
            if (logging || staleOutput) {
                Class<?> logger = loader.loadClass(PACKAGE + ".SketchLogger");
                logger.getMethod("broadcastLog", String.class).invoke(null, "library without initialized context");
                if (getter) {
                    assertEquals(1, application.getClass().getField("GETTER_CALLS").getInt(null));
                    int[] broadcasts = {0};
                    application.getClass().getField("CONTEXT").set(null, new ContextWrapper(context()) {
                        @Override public void sendBroadcast(Intent intent) {
                            assertEquals("pro.sketchware.ACTION_NEW_DEBUG_LOG", intent.getAction());
                            assertEquals("ready", intent.getStringExtra("log"));
                            broadcasts[0]++;
                        }
                    });
                    logger.getMethod("broadcastLog", String.class).invoke(null, "ready");
                    logger.getMethod("broadcastLog", String.class).invoke(null, "ready");
                    assertEquals(2, broadcasts[0]);
                    assertEquals("Successful context lookup should be cached", 2,
                            application.getClass().getField("GETTER_CALLS").getInt(null));
                }
            }
        }
    }

    private static void verify(String setting, String expected, boolean logging, boolean customSource) throws Exception {
        try (Project project = new Project(setting, expected, logging, customSource)) {
            project.paths.generateDebugFiles(context());
            assertEquals(expected, manifestClassName(project.paths));
            Path generatedApplication = Path.of(project.paths.javaFilesPath, expected.replace('.', '/') + ".java");
            if (customSource) {
                assertFalse("Do not generate a second copy of a provided Application", Files.exists(generatedApplication));
                assertEquals(project.customContent, read(project.customFile));
            } else {
                assertTrue("Generate Application in its declared package", Files.isRegularFile(generatedApplication));
            }
            if (logging) {
                String logger = read(Path.of(project.paths.javaFilesPath, PACKAGE.replace('.', '/'), "SketchLogger.java"));
                assertTrue(logger, logger.contains("import " + expected + ";"));
            }
            ClassLoader loader = project.compile();
            Application application = new Instrumentation().newApplication(loader, expected, context());
            assertEquals(expected, application.getClass().getName());
            if (customSource) assertTrue(application.getClass().getField("CUSTOM_SOURCE").getBoolean(null));
            // Do not call onCreate(): the template intentionally installs an app-wide exception handler.
        }
    }

    private static String manifestClassName(ProjectFilePaths paths) throws Exception {
        ManifestGenerator generator = new ManifestGenerator(paths.buildConfig, new ArrayList<>(),
                new BuiltInLibraryManager(paths.sc_id));
        generator.setProjectFilePaths(paths);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var xml = factory.newDocumentBuilder().parse(new InputSource(new StringReader(generator.generateManifest())));
        var application = (org.w3c.dom.Element) xml.getElementsByTagName("application").item(0);
        String name = application.getAttributeNS("http://schemas.android.com/apk/res/android", "name");
        return name.startsWith(".") ? PACKAGE + name : name.indexOf('.') < 0 ? PACKAGE + "." + name : name;
    }

    private static final class Project implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        final Path output = Files.createTempDirectory(root, "project_");
        final Path customRoot = Path.of(SketchwarePaths.getProjectJavaPath(fixture.id));
        final ProjectFilePaths paths;
        Path customFile;
        String customContent;

        Project(String setting, String expected, boolean logging, boolean customSource) throws Exception {
            fixture.seed(false, false);
            ProjectSettings settings = new ProjectSettings(fixture.id);
            settings.setValue(ProjectSettings.SETTING_MINIMUM_SDK_VERSION, "26");
            settings.setValue(ProjectSettings.SETTING_APPLICATION_CLASS, setting);
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", fixture.id); metadata.put("my_sc_pkg_name", PACKAGE);
            metadata.put("my_ws_name", "ApplicationTest"); metadata.put("my_app_name", "ApplicationTest");
            metadata.put("sc_ver_code", "1"); metadata.put("sc_ver_name", "1.0");
            paths = new ProjectFilePaths(context(), output.toString(), metadata);
            paths.buildConfig.packageName = PACKAGE;
            paths.buildConfig.isDebugBuild = logging;
            Files.createDirectories(customRoot);
            if (customSource) {
                String relative = expected.startsWith(PACKAGE + ".") ? expected.substring(PACKAGE.length() + 1) : expected;
                customFile = customRoot.resolve(relative.replace('.', '/') + ".java");
                Files.createDirectories(customFile.getParent());
                int dot = expected.lastIndexOf('.');
                customContent = "package " + expected.substring(0, dot) + "; public class " + expected.substring(dot + 1)
                        + " extends android.app.Application { public static final boolean CUSTOM_SOURCE = true;"
                        + " public static android.content.Context getContext() { return null; } }";
                Files.write(customFile, customContent.getBytes(StandardCharsets.UTF_8));
            }
        }

        Path library(boolean getter) throws Exception {
            Path source = output.resolve("MyApplication.java");
            Files.write(source, ("package custom; public class MyApplication extends android.app.Application {"
                    + " public static final boolean LIBRARY_SOURCE=true;"
                    + (getter ? " public static int GETTER_CALLS; public static android.content.Context CONTEXT;"
                    + " public static android.content.Context getContext() { GETTER_CALLS++; return CONTEXT; }" : "")
                    + " }").getBytes(StandardCharsets.UTF_8));
            Path classes = Files.createDirectory(output.resolve("libraryClasses"));
            StringWriter diagnostics = new StringWriter();
            var ecj = new org.eclipse.jdt.internal.compiler.batch.Main(new PrintWriter(diagnostics),
                    new PrintWriter(diagnostics), false, null, null);
            assertTrue(diagnostics.toString(), ecj.compile(new String[]{"-1.7", "-proc:none", "-bootclasspath",
                    androidJar.toString(), "-d", classes.toString(), source.toString()}));
            Path classpath = Path.of(SketchwarePaths.getProjectClasspathPath(fixture.id));
            Files.createDirectories(classpath);
            Path jar = classpath.resolve("application-library.jar");
            try (JarOutputStream zip = new JarOutputStream(Files.newOutputStream(jar))) {
                zip.putNextEntry(new JarEntry("custom/MyApplication.class"));
                Files.copy(classes.resolve("custom/MyApplication.class"), zip);
                zip.closeEntry();
            }
            return jar;
        }

        ClassLoader compile() throws Exception { return compile(List.of()); }

        ClassLoader compile(List<Path> programJars) throws Exception {
            Path classes = Files.createDirectory(output.resolve("classes"));
            List<String> args = new ArrayList<>(List.of("-1.7", "-proc:none", "-bootclasspath", androidJar.toString(),
                    "-d", classes.toString()));
            if (!programJars.isEmpty()) {
                args.add("-classpath");
                args.add(programJars.stream().map(Path::toString).collect(Collectors.joining(java.io.File.pathSeparator)));
            }
            for (Path sourceRoot : List.of(Path.of(paths.javaFilesPath), customRoot)) {
                try (Stream<Path> files = Files.walk(sourceRoot)) {
                    files.filter(path -> path.toString().endsWith(".java")).forEach(path -> args.add(path.toString()));
                }
            }
            StringWriter diagnostics = new StringWriter();
            var ecj = new org.eclipse.jdt.internal.compiler.batch.Main(new PrintWriter(diagnostics),
                    new PrintWriter(diagnostics), false, null, null);
            assertTrue(diagnostics.toString(), ecj.compile(args.toArray(new String[0])));
            List<Path> bytecode;
            try (Stream<Path> files = Files.walk(classes)) {
                bytecode = files.filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList());
            }
            Path dex = Files.createDirectory(output.resolve("dex"));
            D8.run(D8Command.builder().addProgramFiles(bytecode).addProgramFiles(programJars).addLibraryFiles(androidJar).setMinApiLevel(26)
                    .setOutput(dex, OutputMode.DexIndexed).build());
            return new DexClassLoader(dex.resolve("classes.dex").toString(), output.toString(), null, context().getClassLoader());
        }

        @Override public void close() throws Exception {
            try { fixture.close(); } finally { deleteOwned(output, root); }
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static String read(Path path) throws Exception { return new String(Files.readAllBytes(path), StandardCharsets.UTF_8); }
    private static void deleteOwned(Path path, Path parent) throws Exception {
        if (path == null) return;
        assertTrue(path.toRealPath().startsWith(parent.toRealPath()));
        try (Stream<Path> files = Files.walk(path)) {
            for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
        }
    }
}
