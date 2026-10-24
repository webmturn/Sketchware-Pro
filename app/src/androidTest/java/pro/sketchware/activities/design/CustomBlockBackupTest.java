package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.BackupFactory;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.codegen.BlockInterpreter;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.settings.ApplicationSettings;

/** Real archive/restore regression tests; all project and archive paths are independently owned. */
@RunWith(AndroidJUnit4.class)
public class CustomBlockBackupTest {
    private static final Gson GSON = new Gson();
    private static final Instrumentation INSTRUMENTATION = InstrumentationRegistry.getInstrumentation();
    private static final Context CONTEXT = INSTRUMENTATION.getTargetContext();

    @Test public void globalOnlyDefinitionSurvivesRestore() throws Exception { roundTrip("global-only"); }
    @Test public void projectOnlyDefinitionSurvivesRestore() throws Exception { roundTrip("project-only"); }
    @Test public void identicalDefinitionsSurviveRestore() throws Exception { roundTrip("same-definition"); }
    @Test public void globalDefinitionWinsOverDifferentProjectDefinition() throws Exception { roundTrip("different-definition"); }
    @Test public void uncheckedOptionKeepsExistingProjectDefinition() throws Exception { roundTrip("exclude-used"); }
    @Test public void missingDefinitionRemainsMissingWithoutLosingLogic() throws Exception { roundTrip("missing-definition"); }
    @Test public void legitimateEmptyGlobalDefinitionIsRetained() throws Exception { roundTrip("empty-definition"); }
    @Test public void duplicateProjectDefinitionsMatchGeneratorSelection() throws Exception { roundTrip("duplicate-project"); }

    private static void roundTrip(String mode) throws Exception {
        try (Scope scope = new Scope(); OwnedProject source = new OwnedProject(); OwnedProject restored = new OwnedProject()) {
            source.fixture.seed(false, false);
            String name = "BackupRt" + scope.token;
            String spec = "show \\%s.literal %s.value";
            ExtraBlockInfo global = definition(name, spec, "setTitle(\"GLOBAL|\" + %1$s + \"|%%s\");", 0xff1976d2);
            ExtraBlockInfo local = definition(name, spec, "setTitle(\"PROJECT|\" + %1$s + \"|%%s\");", 0xffe91e63);
            boolean hasGlobal = !List.of("project-only", "missing-definition", "duplicate-project").contains(mode);
            boolean hasProject = !List.of("global-only", "missing-definition").contains(mode);
            boolean includeUsed = !mode.equals("exclude-used");
            if (mode.equals("same-definition")) local = definition(name, spec, global.getCode(), global.getColor());
            if (mode.equals("empty-definition")) global.setCode("");
            if (hasGlobal) scope.globalMap().put(name, global);
            if (hasProject) {
                List<ExtraBlockInfo> definitions = mode.equals("duplicate-project") ? List.of(global, local) : List.of(local);
                Files.write(new File(SketchwarePaths.getProjectCustomBlocksPath(source.id())).toPath(),
                        GSON.toJson(definitions).getBytes(StandardCharsets.UTF_8));
            }
            ArrayList<BlockBean> chain = new ArrayList<>();
            BlockBean bean = new BlockBean("1", spec, " ", name);
            bean.parameters.add("中文 value");
            chain.add(bean);
            String persistedBlocks = ProjectDataStore.gson.toJson(chain);
            ProjectDataManager.reloadProjectFromData(source.id());
            ProjectDataManager.getProjectDataManager(source.id()).putBlocks(
                    ProjectFileBean.DEFAULT_JAVA_NAME, "onCreate_initializeLogic", chain);
            assertTrue(ProjectDataManager.getProjectDataManager(source.id()).saveAllData());
            BlockLoader.invalidateProjectCache();
            String before = generate(source.id(), chain);

            BackupFactory backup = new BackupFactory(source.id());
            backup.setBackupCustomBlocks(includeUsed);
            backup.backup(new ContextThemeWrapper(CONTEXT, R.style.Theme_SketchwarePro), name);
            assertNotNull(backup.getError(), backup.getOutFile());
            assertTrue(backup.getOutFile().getCanonicalPath().startsWith(scope.backups.getCanonicalPath() + File.separator));
            List<ExtraBlockInfo> exported;
            try (ZipFile zip = new ZipFile(backup.getOutFile()); InputStream input = zip.getInputStream(zip.getEntry("data/custom_blocks"))) {
                exported = GSON.fromJson(new String(readAll(input), StandardCharsets.UTF_8),
                        new TypeToken<List<ExtraBlockInfo>>() {}.getType());
            }
            assertEquals(mode.equals("missing-definition") ? 0 : 1, exported.size());

            BackupFactory restore = new BackupFactory(restored.id());
            restore.restore(backup.getOutFile());
            assertTrue(restore.getError(), restore.isRestoreSuccess());
            ProjectDataManager.clearAll();
            ProjectDataManager.reloadProjectFromData(restored.id());
            assertTrue(ProjectDataManager.canSaveProject(restored.id()));
            ArrayList<BlockBean> reread = ProjectDataManager.getProjectDataManager(restored.id()).getBlocks(
                    ProjectFileBean.DEFAULT_JAVA_NAME, "onCreate_initializeLogic");
            assertEquals(persistedBlocks, ProjectDataStore.gson.toJson(reread));
            scope.globalMap().remove(name);
            BlockLoader.invalidateProjectCache();
            String after = generate(restored.id(), reread);
            ExtraBlockInfo saved = BlockLoader.getBlockFromProject(restored.id(), name);
            assertEquals(mode, mode.equals("missing-definition"), saved.isMissing);
            if (!saved.isMissing) {
                ExtraBlockInfo expected = hasGlobal && includeUsed ? global : local;
                assertEquals(GSON.toJson(expected), GSON.toJson(exported.get(0)));
                assertEquals(GSON.toJson(expected), GSON.toJson(saved));
            }
            if (includeUsed) assertEquals(mode, before, after);
            else {
                assertTrue(before.contains("GLOBAL|"));
                assertTrue(after.contains("PROJECT|"));
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mode", mode);
            row.put("unchangedCode", before.equals(after));
            row.put("restoredIsMissing", saved.isMissing);
            row.put("exportedDefinitions", exported.size());
            row.put("logicAndParametersPreserved", true);
            status("backup_rt_regression_row", GSON.toJson(row));
        }
    }

    private static ExtraBlockInfo definition(String name, String spec, String code, int color) {
        ExtraBlockInfo info = new ExtraBlockInfo();
        info.setName(name); info.setSpec(spec); info.setCode(code);
        info.setColor(color); info.setPaletteColor(0xff8a55d7); info.setSpec2("otherwise");
        return info;
    }

    private static String generate(String id, ArrayList<BlockBean> chain) {
        BuildConfig config = new BuildConfig(); config.sc_id = id;
        return new BlockInterpreter("MainActivity", config, chain, false).interpretBlocks();
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    static final class OwnedProject implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture;
        final File icons;

        OwnedProject() throws Exception {
            ArrayList<ProjectEditorPersistenceTest.Fixture> skipped = new ArrayList<>();
            ProjectEditorPersistenceTest.Fixture selected;
            try {
                while (true) {
                    ProjectEditorPersistenceTest.Fixture candidate = new ProjectEditorPersistenceTest.Fixture();
                    // Fixture reserves the icon directory too; only the separate codegen cache
                    // can still belong to an earlier project with this otherwise unused id.
                    if (new File(CONTEXT.getCacheDir(), "codegenCache/" + candidate.id).exists()) {
                        skipped.add(candidate);
                        status("backup_rt_owned_reservation", candidate.id);
                    } else { selected = candidate; break; }
                }
            } finally {
                for (ProjectEditorPersistenceTest.Fixture candidate : skipped) {
                    candidate.close(); status("backup_rt_cleaned_reservation", candidate.id);
                }
            }
            fixture = selected;
            icons = new File(SketchwarePaths.getIconsPath(), fixture.id);
            status("backup_rt_owned_project", fixture.id);
        }

        String id() { return fixture.id; }

        @Override public void close() throws Exception {
            fixture.close();
            if (icons.exists()) deleteOwned(icons, new File(SketchwarePaths.getIconsPath()));
            File generated = new File(CONTEXT.getCacheDir(), "codegenCache/" + id());
            if (generated.exists()) deleteOwned(generated, generated.getParentFile());
            status("backup_rt_cleaned_project", id());
        }
    }

    /** Redirects backup settings in memory without writing the user's settings file. */
    static final class Scope implements AutoCloseable {
        final String token = UUID.randomUUID().toString().replace("-", "");
        final File output = new File(CONTEXT.getCacheDir(), "BackupRt." + token);
        final File backups = new File(Environment.getExternalStorageDirectory(), ".sketchware/BackupRt." + token);
        final Map<Field, Object> loaderState = new LinkedHashMap<>();
        final Object store;
        final Field settingsFile;
        final Map<String, Object> settings, originalSettings;
        final File actualSettings;
        final byte[] settingsBytes;
        final long settingsMtime;
        final ArrayList<HashMap<String, Object>> extraData;
        final ArrayList<HashMap<String, Object>> originalExtraData;
        final HashMap<String, ArrayList<HashMap<String, Object>>> extraByPalette;
        final HashMap<String, ArrayList<HashMap<String, Object>>> originalExtraByPalette;

        @SuppressWarnings("unchecked") Scope() throws Exception {
            BlockLoader.getBlockInfo("BackupRt.initialize." + token);
            for (Field field : BlockLoader.class.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                field.setAccessible(true); loaderState.put(field, field.get(null));
            }
            extraData = (ArrayList<HashMap<String, Object>>) field(ExtraBlockFile.class, "cachedExtraBlocks").get(null);
            originalExtraData = new ArrayList<>(extraData);
            extraByPalette = (HashMap<String, ArrayList<HashMap<String, Object>>>) field(ExtraBlockFile.class, "cachedExtraBlocksByPalette").get(null);
            originalExtraByPalette = new HashMap<>(extraByPalette);
            ApplicationSettings.getBackupPath();
            store = field(Class.forName("pro.sketchware.core.settings.ApplicationSettings$Holder"), "STORE").get(null);
            settingsFile = field(store.getClass(), "file"); actualSettings = (File) settingsFile.get(store);
            settings = (Map<String, Object>) field(store.getClass(), "settings").get(store);
            originalSettings = new HashMap<>(settings);
            settingsBytes = Files.readAllBytes(actualSettings.toPath()); settingsMtime = actualSettings.lastModified();
            assertFalse(output.exists()); assertTrue(output.mkdir());
            status("backup_rt_owned_cache", output.getName());
            assertFalse(backups.exists()); assertTrue(backups.mkdir());
            status("backup_rt_owned_archives", backups.getName());
            settingsFile.set(store, new File(output, "settings.json"));
            synchronized (store) {
                settings.put(ApplicationSettings.SETTING_BACKUP_DIRECTORY, backups.getAbsolutePath());
                settings.put(ApplicationSettings.SETTING_BACKUP_FILENAME, "$projectName");
            }
            assertEquals(backups.getCanonicalFile(), new File(BackupFactory.getBackupDir()).getCanonicalFile());
            Field lookup = field(BlockLoader.class, "blocksByName");
            lookup.set(null, new HashMap<>((Map<String, ExtraBlockInfo>) lookup.get(null)));
        }

        @SuppressWarnings("unchecked") HashMap<String, ExtraBlockInfo> globalMap() throws Exception {
            return (HashMap<String, ExtraBlockInfo>) field(BlockLoader.class, "blocksByName").get(null);
        }

        void loadGlobals(List<HashMap<String, Object>> definitions) {
            extraData.clear(); extraData.addAll(definitions);
            extraByPalette.clear();
            BlockLoader.refresh();
        }

        @Override public void close() throws Exception {
            ProjectDataManager.clearAll();
            extraData.clear(); extraData.addAll(originalExtraData);
            extraByPalette.clear(); extraByPalette.putAll(originalExtraByPalette);
            for (Map.Entry<Field, Object> state : loaderState.entrySet()) state.getKey().set(null, state.getValue());
            synchronized (store) {
                settings.clear(); settings.putAll(originalSettings); settingsFile.set(store, actualSettings);
            }
            assertArrayEquals(settingsBytes, Files.readAllBytes(actualSettings.toPath()));
            assertEquals(settingsMtime, actualSettings.lastModified());
            deleteOwned(backups, new File(Environment.getExternalStorageDirectory(), ".sketchware"));
            status("backup_rt_cleaned_archives", backups.getName());
            deleteOwned(output, CONTEXT.getCacheDir());
            status("backup_rt_cleaned_cache", output.getName());
            status("backup_rt_state_restored", "true");
        }
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }

    private static void deleteOwned(File directory, File allowedRoot) throws Exception {
        Path path = directory.getCanonicalFile().toPath();
        assertTrue(path.startsWith(allowedRoot.getCanonicalFile().toPath()));
        assertNotEquals(path, allowedRoot.getCanonicalFile().toPath());
        try (Stream<Path> files = Files.walk(path)) {
            for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(file);
        }
    }

    private static void status(String key, String value) {
        Bundle data = new Bundle(); data.putString(key, value); INSTRUMENTATION.sendStatus(0, data);
    }
}
