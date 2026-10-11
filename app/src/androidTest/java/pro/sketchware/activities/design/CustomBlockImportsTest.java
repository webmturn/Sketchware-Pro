package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.BackupFactory;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.codegen.ActivityCodeGenerator;
import pro.sketchware.core.codegen.BlockInterpreter;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;

/** Imports follow the selected code through real loading, archives and palette conversion. */
@RunWith(AndroidJUnit4.class)
public class CustomBlockImportsTest {
    private static final Gson GSON = new Gson();
    private static final Context CONTEXT = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final String GLOBAL_IMPORT = "java.security.MessageDigest";
    private static final String PROJECT_IMPORT = "java.util.GregorianCalendar";

    @Test public void globalImportsSurviveBackupAndPaletteCopy() throws Exception { roundTrip("global-only"); }
    @Test public void projectImportsSurviveBackupAndPaletteCopy() throws Exception { roundTrip("project-only"); }
    @Test public void globalImportsShadowProjectImports() throws Exception { roundTrip("different-definition"); }
    @Test public void emptyGlobalImportsDoNotFallBackToProjectImports() throws Exception { roundTrip("empty-global"); }
    @Test public void duplicateGlobalNamesUseLastDefinitionsImports() throws Exception { roundTrip("duplicate-global"); }
    @Test public void duplicateProjectNamesUseLastDefinitionsImports() throws Exception { roundTrip("duplicate-project"); }
    @Test public void legacyDefinitionWithoutImportsStillRestores() throws Exception { roundTrip("legacy-project"); }
    @Test public void explicitNullImportsStillRestore() throws Exception { roundTrip("null-project"); }
    @Test public void repeatedAndBlankImportLinesRetainExistingDeduplication() throws Exception { roundTrip("repeated-imports"); }

    private static void roundTrip(String mode) throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
             CustomBlockBackupTest.OwnedProject source = new CustomBlockBackupTest.OwnedProject();
             CustomBlockBackupTest.OwnedProject restored = new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            String name = "BackupRt" + scope.token;
            HashMap<String, Object> global = definition(name, "GLOBAL", GLOBAL_IMPORT);
            HashMap<String, Object> local = definition(name, "PROJECT", PROJECT_IMPORT);
            boolean hasGlobal = !List.of("project-only", "duplicate-project", "legacy-project", "null-project").contains(mode);
            boolean hasProject = !List.of("global-only", "duplicate-global", "repeated-imports").contains(mode);
            if (mode.equals("empty-global")) global.put("imports", "");
            if (mode.equals("legacy-project")) local.remove("imports");
            if (mode.equals("null-project")) local.put("imports", null);
            if (mode.equals("repeated-imports")) global.put("imports", GLOBAL_IMPORT + "\n\n" + GLOBAL_IMPORT);
            List<HashMap<String, Object>> globals = hasGlobal
                    ? (mode.equals("duplicate-global") ? List.of(local, global) : List.of(global)) : List.of();
            scope.loadGlobals(globals);
            if (hasProject) {
                List<HashMap<String, Object>> locals = mode.equals("duplicate-project") ? List.of(global, local) : List.of(local);
                ArrayList<HashMap<String, Object>> projectDefinitions = new ArrayList<>();
                for (HashMap<String, Object> definition : locals) {
                    HashMap<String, Object> projectDefinition = new HashMap<>(definition);
                    projectDefinition.put("color", 0xff1976d2);
                    projectDefinitions.add(projectDefinition);
                }
                Files.write(new File(SketchwarePaths.getProjectCustomBlocksPath(source.id())).toPath(),
                        new GsonBuilder().serializeNulls().create().toJson(projectDefinitions).getBytes(StandardCharsets.UTF_8));
            }
            BlockBean bean = new BlockBean("1", "show %s.value", " ", name);
            bean.parameters.add("中文 value");
            ArrayList<BlockBean> chain = new ArrayList<>(List.of(bean));
            ProjectDataManager.reloadProjectFromData(source.id());
            ProjectDataManager.getProjectDataManager(source.id()).putBlocks(
                    ProjectFileBean.DEFAULT_JAVA_NAME, "onCreate_initializeLogic", chain);
            assertTrue(ProjectDataManager.getProjectDataManager(source.id()).saveAllData());
            BlockLoader.invalidateProjectCache();
            HashMap<String, Object> selected = hasGlobal ? global : local;
            String expectedImports = selected.get("imports") == null ? "" : selected.get("imports").toString();
            Set<String> expectedLines = new HashSet<>();
            if (!expectedImports.isEmpty()) expectedLines.add(expectedImports.split("\n")[0]);
            assertEquals(mode, expectedLines, importsForActivity(source.id()));
            String before = generate(source.id(), chain);
            assertTrue(before, before.contains(hasGlobal ? "GLOBAL" : "PROJECT"));

            BackupFactory backup = new BackupFactory(source.id());
            backup.setBackupCustomBlocks(true);
            backup.backup(new ContextThemeWrapper(CONTEXT, R.style.Theme_SketchwarePro), name);
            assertNotNull(backup.getError(), backup.getOutFile());
            List<ExtraBlockInfo> exported;
            try (ZipFile zip = new ZipFile(backup.getOutFile());
                 InputStream input = zip.getInputStream(zip.getEntry("data/custom_blocks"))) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                exported = GSON.fromJson(new String(output.toByteArray(), StandardCharsets.UTF_8),
                        new TypeToken<List<ExtraBlockInfo>>() {}.getType());
            }
            assertEquals(1, exported.size());
            assertEquals(expectedImports, exported.get(0).getImports());
            BackupFactory restore = new BackupFactory(restored.id());
            restore.restore(backup.getOutFile());
            assertTrue(restore.getError(), restore.isRestoreSuccess());
            scope.loadGlobals(List.of());
            assertTrue(BlockLoader.getBlockInfo(name).isMissing);
            ProjectDataManager.clearAll(); ProjectDataManager.reloadProjectFromData(restored.id());
            ArrayList<BlockBean> reread = ProjectDataManager.getProjectDataManager(restored.id()).getBlocks(
                    ProjectFileBean.DEFAULT_JAVA_NAME, "onCreate_initializeLogic");
            assertEquals(ProjectDataStore.gson.toJson(chain), ProjectDataStore.gson.toJson(reread));
            assertEquals(before, generate(restored.id(), reread));
            ExtraBlockInfo saved = BlockLoader.getBlockFromProject(restored.id(), name);
            assertFalse(saved.isMissing);
            assertEquals(expectedImports, saved.getImports());
            assertEquals(mode, expectedLines, importsForActivity(restored.id()));

            ArrayList<HashMap<String, Object>> copied = new ArrayList<>();
            Method copy = CustomBlocksDialog.class.getDeclaredMethod("addBlocksToList",
                    CustomBlocksManager.class, ArrayList.class, ArrayList.class, int.class);
            copy.setAccessible(true);
            copy.invoke(new CustomBlocksDialog(), new CustomBlocksManager(
                    new ContextThemeWrapper(CONTEXT, R.style.Theme_SketchwarePro), restored.id()), reread, copied, 9);
            assertEquals(1, copied.size());
            assertEquals(saved.getCode(), copied.get(0).get("code"));
            assertEquals(expectedImports.isEmpty() ? null : expectedImports, copied.get(0).get("imports"));
            scope.loadGlobals(copied);
            assertFalse(BlockLoader.getBlockInfo(name).isMissing);
            assertEquals(expectedImports, BlockLoader.getBlockInfo(name).getImports());
            assertEquals(mode, expectedLines, importsForActivity(restored.id()));
        }
    }

    private static HashMap<String, Object> definition(String name, String label, String imports) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name); map.put("spec", "show %s.value"); map.put("spec2", "otherwise");
        map.put("code", "setTitle(\"" + label + "|\" + %1$s);");
        map.put("color", "#1976D2"); map.put("palette", "0"); map.put("imports", imports);
        return map;
    }

    private static BuildConfig config(String id) {
        BuildConfig config = new BuildConfig(); config.sc_id = id; config.packageName = "test.backup.imports";
        return config;
    }

    private static String generate(String id, ArrayList<BlockBean> chain) {
        return new BlockInterpreter("MainActivity", config(id), chain, false).interpretBlocks();
    }

    @SuppressWarnings("unchecked") private static Set<String> importsForActivity(String id) throws Exception {
        ActivityCodeGenerator generator = new ActivityCodeGenerator(config(id),
                new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"),
                ProjectDataManager.getProjectDataManager(id));
        for (String methodName : List.of("addImportsForBlocks", "removeExtraImports")) {
            Method method = ActivityCodeGenerator.class.getDeclaredMethod(methodName);
            method.setAccessible(true); method.invoke(generator);
        }
        Field imports = ActivityCodeGenerator.class.getDeclaredField("imports"); imports.setAccessible(true);
        return new HashSet<>((ArrayList<String>) imports.get(generator));
    }
}
