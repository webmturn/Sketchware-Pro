package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.google.gson.internal.UnsafeAllocator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.build.ProjectBuilder;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.build.ViewBindingBuilder;
import pro.sketchware.core.codegen.ActivityCodeGenerator;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectSettings;

/** Compiles production binding output with the real platform, then binds real Android views. */
@RunWith(AndroidJUnit4.class)
public class ViewBindingGenerationTest {
    private static final String PACKAGE = "test.viewbinding.generated";
    private static final int STATUS = 0x7f0f1001, LEFT = 0x7f0f1002, RIGHT = 0x7f0f1003, INNER = 0x7f0f1004;
    private static Path root, androidJar;

    @BeforeClass public static void prepare() throws Exception {
        root = Files.createTempDirectory(context().getCacheDir().toPath(), "binding_generation_");
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

    @AfterClass public static void cleanup() throws Exception { deleteOwned(root, context().getCacheDir().toPath()); }

    @Test public void localAndExportedActivitiesAndFragmentsImportBindingPackage() throws Exception {
        try (Project project = new Project()) {
            project.settings.setValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, "true");
            for (String layout : new String[]{"main", "profile_fragment"}) {
                for (boolean export : new boolean[]{false, true}) {
                    String code = project.activity(layout, export);
                    assertTrue(layout + "/export=" + export, code.contains("import " + PACKAGE + ".databinding.*;"));
                    assertTrue(code.contains("private " + ViewBindingBuilder.generateFileNameForLayout(layout) + " binding;"));
                }
            }
        }
    }

    @Test public void disabledBindingKeepsBothGenerationModesFreeOfBindingImports() throws Exception {
        try (Project project = new Project()) {
            project.settings.setValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, "false");
            for (boolean export : new boolean[]{false, true}) {
                String code = project.activity("main", export);
                assertFalse(code.contains(PACKAGE + ".databinding"));
                assertFalse(code.contains("MainBinding binding"));
                assertTrue(code.contains("setContentView(R.layout.main)"));
            }
        }
    }

    @Test public void ordinaryBindingCompilesInItsDeclaredDirectoryAndBindsViews() throws Exception {
        try (Project project = new Project()) {
            project.layout("main", "<TextView android:id=\"@+id/status\"/>");
            project.generate();
            String source = project.binding("MainBinding");
            assertTrue(source.contains("package " + PACKAGE + ".databinding;"));
            assertTrue(source.contains("import " + PACKAGE + ".R;"));
            ClassLoader loader = project.compileAndLoad();
            onMain(() -> {
                LinearLayout view = new LinearLayout(context());
                TextView status = status(); view.addView(status);
                Object binding = bind(loader, "MainBinding", view);
                assertSame(view, binding.getClass().getMethod("getRoot").invoke(binding));
                assertSame(status, binding.getClass().getField("status").get(binding));
            });
        }
    }

    @Test public void repeatedIncludeBindingsResolveTheNewPackageAndKeepSeparateRoots() throws Exception {
        try (Project project = new Project()) {
            project.layout("panel", "<TextView android:id=\"@+id/status\"/>");
            project.layout("main", "<include android:id=\"@+id/left_panel\" layout=\"@layout/panel\"/>"
                    + "<include android:id=\"@+id/right_panel\" layout=\"@layout/panel\"/>");
            project.generate();
            ClassLoader loader = project.compileAndLoad();
            onMain(() -> {
                LinearLayout view = new LinearLayout(context());
                LinearLayout left = panel(LEFT), right = panel(RIGHT); view.addView(left); view.addView(right);
                Object binding = bind(loader, "MainBinding", view);
                Object leftBinding = binding.getClass().getField("leftPanel").get(binding);
                Object rightBinding = binding.getClass().getField("rightPanel").get(binding);
                assertEquals(PACKAGE + ".databinding.PanelBinding", leftBinding.getClass().getName());
                assertSame(left, leftBinding.getClass().getMethod("getRoot").invoke(leftBinding));
                assertSame(right, rightBinding.getClass().getMethod("getRoot").invoke(rightBinding));
                assertSame(left.getChildAt(0), leftBinding.getClass().getField("status").get(leftBinding));
                assertSame(right.getChildAt(0), rightBinding.getClass().getField("status").get(rightBinding));
            });
        }
    }

    @Test public void nestedIncludesCompileAndBindAcrossAllGeneratedClasses() throws Exception {
        try (Project project = new Project()) {
            project.layout("drawer", "<TextView android:id=\"@+id/status\"/>");
            project.layout("panel", "<include android:id=\"@+id/inner_panel\" layout=\"@layout/drawer\"/>");
            project.layout("main", "<include android:id=\"@+id/left_panel\" layout=\"@layout/panel\"/>");
            project.generate();
            ClassLoader loader = project.compileAndLoad();
            onMain(() -> {
                LinearLayout view = new LinearLayout(context()), middle = new LinearLayout(context()), inner = panel(INNER);
                middle.setId(LEFT); middle.addView(inner); view.addView(middle);
                Object binding = bind(loader, "MainBinding", view);
                Object panelBinding = binding.getClass().getField("leftPanel").get(binding);
                Object drawerBinding = panelBinding.getClass().getField("innerPanel").get(panelBinding);
                assertEquals(PACKAGE + ".databinding.DrawerBinding", drawerBinding.getClass().getName());
                assertSame(inner, drawerBinding.getClass().getMethod("getRoot").invoke(drawerBinding));
                assertSame(inner.getChildAt(0), drawerBinding.getClass().getField("status").get(drawerBinding));
            });
        }
    }

    @Test public void missingRequiredViewsStillFailBinding() throws Exception {
        try (Project project = new Project()) {
            project.layout("main", "<TextView android:id=\"@+id/status\"/>"); project.generate();
            ClassLoader loader = project.compileAndLoad();
            onMain(() -> {
                var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> bind(loader, "MainBinding", new LinearLayout(context())));
                assertTrue(error.getCause() instanceof IllegalStateException);
            });
        }
    }

    private static final class Project implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        final Path directory = Files.createTempDirectory(root, "project_");
        final ProjectSettings settings = new ProjectSettings(fixture.id);
        final ProjectFilePaths paths;

        Project() throws Exception {
            fixture.seed(false, false); ProjectDataManager.reloadProjectFromData(fixture.id);
            HashMap<String, Object> metadata = new HashMap<>();
            metadata.put("sc_id", fixture.id); metadata.put("my_sc_pkg_name", PACKAGE);
            metadata.put("my_ws_name", "BindingTest"); metadata.put("my_app_name", "BindingTest");
            metadata.put("sc_ver_code", "1"); metadata.put("sc_ver_name", "1.0");
            paths = new ProjectFilePaths(context(), directory.toString(), metadata);
            Files.createDirectories(Path.of(paths.layoutFilesPath));
        }

        String activity(String layout, boolean export) {
            BuildConfig config = new BuildConfig(); config.sc_id = fixture.id; config.packageName = PACKAGE;
            ProjectFileBean file = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, layout); file.options = 0;
            return new ActivityCodeGenerator(config, file, ProjectDataManager.getProjectDataManager(fixture.id))
                    .generateCode(export, fixture.id, false);
        }

        void layout(String name, String children) throws Exception {
            Files.write(Path.of(paths.layoutFilesPath, name + ".xml"), ("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                    + children + "</LinearLayout>").getBytes(StandardCharsets.UTF_8));
        }

        void generate() throws Exception {
            settings.setValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING, "true");
            // Avoid the normal constructor resetting the user's debug log and System.out.
            ProjectBuilder builder = UnsafeAllocator.INSTANCE.newInstance(ProjectBuilder.class);
            builder.projectFilePaths = paths; builder.settings = settings;
            builder.generateViewBinding();
        }

        Path bindingDirectory() { return Path.of(paths.javaFilesPath, PACKAGE.replace('.', '/'), "databinding"); }
        String binding(String name) throws Exception { return new String(Files.readAllBytes(bindingDirectory().resolve(name + ".java")), StandardCharsets.UTF_8); }

        ClassLoader compileAndLoad() throws Exception {
            Path javaRoot = Path.of(paths.javaFilesPath), appPackage = javaRoot.resolve(PACKAGE.replace('.', '/'));
            Files.createDirectories(appPackage);
            Files.write(appPackage.resolve("R.java"), ("package " + PACKAGE + "; public final class R {"
                    + "public static final class layout { public static final int main=1,panel=2,drawer=3; }"
                    + "public static final class id { public static final int status=" + STATUS + ",left_panel=" + LEFT
                    + ",right_panel=" + RIGHT + ",inner_panel=" + INNER + "; }}").getBytes(StandardCharsets.UTF_8));
            // Compile a caller in the app package too: it must resolve the same namespace used by ActivityCodeGenerator.
            Files.write(appPackage.resolve("BindingUse.java"), ("package " + PACKAGE + "; import " + PACKAGE
                    + ".databinding.*; public final class BindingUse { MainBinding binding; }").getBytes(StandardCharsets.UTF_8));
            Path classes = Files.createDirectory(directory.resolve("classes"));
            List<String> args = new ArrayList<>(List.of("-1.7", "-proc:none", "-bootclasspath", androidJar.toString(), "-d", classes.toString()));
            try (Stream<Path> sources = Files.walk(javaRoot)) {
                sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> args.add(path.toString()));
            }
            StringWriter diagnostics = new StringWriter();
            var ecj = new org.eclipse.jdt.internal.compiler.batch.Main(new PrintWriter(diagnostics), new PrintWriter(diagnostics), false, null, null);
            boolean compiled = ecj.compile(args.toArray(new String[0]));
            assertTrue(diagnostics.toString(), compiled);
            Path dex = Files.createDirectory(directory.resolve("dex"));
            List<Path> bytecode;
            try (Stream<Path> files = Files.walk(classes)) { bytecode = files.filter(path -> path.toString().endsWith(".class")).collect(Collectors.toList()); }
            D8.run(D8Command.builder().addProgramFiles(bytecode).addLibraryFiles(androidJar).setMinApiLevel(26)
                    .setOutput(dex, OutputMode.DexIndexed).build());
            return new DexClassLoader(dex.resolve("classes.dex").toString(), directory.toString(), null, context().getClassLoader());
        }

        @Override public void close() throws Exception {
            try { fixture.close(); } finally { deleteOwned(directory, root); }
        }
    }

    private static Object bind(ClassLoader loader, String name, View view) throws Exception {
        return loader.loadClass(PACKAGE + ".databinding." + name).getMethod("bind", View.class).invoke(null, view);
    }
    private static TextView status() { TextView status = new TextView(context()); status.setId(STATUS); return status; }
    private static LinearLayout panel(int id) { LinearLayout panel = new LinearLayout(context()); panel.setId(id); panel.addView(status()); return panel; }
    private static void onMain(CheckedRunnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
    private interface CheckedRunnable { void run() throws Exception; }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static void deleteOwned(Path directory, Path parent) throws Exception {
        Path canonical = directory.toFile().getCanonicalFile().toPath();
        assertEquals(parent.toFile().getCanonicalFile().toPath(), canonical.getParent());
        try (Stream<Path> files = Files.walk(canonical)) {
            for (Path path : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }
}
