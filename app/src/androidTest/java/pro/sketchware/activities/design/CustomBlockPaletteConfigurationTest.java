package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.block.BlocksManager;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.core.block.DynamicPaletteProvider;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.core.project.LibraryManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ProjectFileManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.settings.ApplicationSettings;

/** Exercises real import and configuration buttons against independently owned files. */
@RunWith(AndroidJUnit4.class)
public class CustomBlockPaletteConfigurationTest {
    private static final Instrumentation INSTRUMENTATION = InstrumentationRegistry.getInstrumentation();
    private static final Context CONTEXT = INSTRUMENTATION.getTargetContext();
    private static final Gson GSON = new Gson();
    private static final String LEGAL = "[{\"name\":\"A\",\"color\":\"#1976D2\"},{\"name\":\"B\",\"color\":\"#E91E63\"}]";
    private static final String FILTERED = "[{\"name\":17},{\"name\":\"A\",\"color\":\"#1976D2\"},{\"color\":\"#1976D2\"},{\"name\":\"B\",\"color\":\"#E91E63\"},{\"name\":false}]";

    @Test public void legalRecordsKeepExistingDefault() throws Exception { importTo(LEGAL, "[]", -1, 1, 10); }
    @Test public void nullBlockListImportsNormally() throws Exception { importTo(LEGAL, "null", -1, 1, 10); }
    @Test public void hiddenRecordsKeepDefaultPaletteId() throws Exception { importTo(FILTERED, "[]", -1, 1, 12); }
    @Test public void hiddenRecordsKeepExplicitPaletteId() throws Exception { importTo(FILTERED, "[]", 0, 1, 10); }
    @Test public void nullPaletteEntryKeepsFollowingId() throws Exception {
        importTo("[null,{\"name\":\"B\",\"color\":\"#1976D2\"}]", "[]", -1, 0, 10);
    }

    private static void importTo(String palettes, String blocks, int choice, int checked, int expectedId) throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.palettes, palettes); write(fixture.blocks, blocks); fixture.importDialog();
            onMain(() -> {
                ListView list = findList(dialogRoot()); assertNotNull(list);
                assertEquals(checked, list.getCheckedItemPosition());
                if (choice >= 0) list.performItemClick(list.getChildAt(choice), choice, list.getAdapter().getItemId(choice));
            });
            click(android.R.id.button1);
            List<HashMap<String, Object>> saved = maps(fixture.blocks);
            assertEquals(1, saved.size()); assertEquals(Integer.toString(expectedId), saved.get(0).get("palette"));
            assertEquals("setTitle(\"CONFIG\");", saved.get(0).get("code"));
            assertEquals("java.util.Locale", saved.get(0).get("imports"));
            assertEquals(palettes, read(fixture.palettes));
            assertEquals(1, ExtraBlockFile.getExtraBlockData(Integer.toString(expectedId)).size());
            assertTrue(new DynamicPaletteProvider().getPaletteSelector().stream().anyMatch(item -> Integer.valueOf(expectedId).equals(item.get("index"))));
            assertFalse(BlockLoader.getBlockInfo(fixture.name).isMissing);
        }
    }

    @Test public void nullPaletteListOpensCreation() throws Exception { createPalette("null", 9); }
    @Test public void allHiddenRecordsOpenCreationAndKeepPositions() throws Exception {
        createPalette("[{\"name\":17},{\"color\":\"#1976D2\"}]", 11);
    }
    private static void createPalette(String original, int expectedId) throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.palettes, original); fixture.importDialog();
            onMain(() -> {
                View root = dialogRoot(); assertNotNull(root);
                ((EditText)root.findViewById(R.id.nameEditText)).setText("Created");
                ((EditText)root.findViewById(R.id.colorEditText)).setText("1976D2");
            }); click(android.R.id.button1);
            assertEquals(Integer.toString(expectedId), maps(fixture.blocks).get(0).get("palette"));
            List<HashMap<String, Object>> saved = maps(fixture.palettes);
            assertEquals("Created", saved.get(saved.size()-1).get("name"));
            assertEquals(expectedId-8, saved.size());
            assertTrue(new DynamicPaletteProvider().getPaletteSelector().stream().anyMatch(item -> Integer.valueOf(expectedId).equals(item.get("index"))));
        }
    }

    @Test public void cancelKeepsBothFilesUnchanged() throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.palettes, FILTERED); fixture.importDialog();
            onMain(() -> {
                ListView list = findList(dialogRoot()); list.performItemClick(list.getChildAt(0),0,list.getAdapter().getItemId(0));
                assertTrue(dialogRoot().findViewById(android.R.id.button1) != null);
                // Back cancels the target chooser; its negative button intentionally opens creation.
            });
            INSTRUMENTATION.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            INSTRUMENTATION.waitForIdleSync();
            assertEquals(FILTERED, read(fixture.palettes)); assertEquals("[]", read(fixture.blocks));
        }
    }

    @Test public void nullBlockRecordsDoNotBreakLoading() throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.palettes, LEGAL);
            write(fixture.blocks, "[null,"+GSON.toJson(global(fixture.name,"PATH_A"))+"]");
            ExtraBlockFile.invalidateCache(); BlockLoader.refresh();
            assertEquals("setTitle(\"PATH_A\");", BlockLoader.getBlockInfo(fixture.name).getCode());
            assertEquals(1, ExtraBlockFile.getExtraBlockData("9").size());
        }
    }

    @Test public void actualPathSettingsUpdateLoaderImmediately() throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.blocks, GSON.toJson(List.of(global(fixture.name,"PATH_A")))); write(fixture.palettes, LEGAL);
            try (ActivityScenario<BlocksManager> manager = ActivityScenario.launch(BlocksManager.class)) {
                setPaths(manager, fixture.blocks, fixture.palettes);
                assertEquals(fixture.blocks.getCanonicalFile(), ExtraBlockFile.getExtraBlocksDataFile().getCanonicalFile());
                assertEquals(fixture.palettes.getCanonicalFile(), ExtraBlockFile.getExtraBlocksPaletteFile().getCanonicalFile());
                assertEquals("setTitle(\"PATH_A\");", BlockLoader.getBlockInfo(fixture.name).getCode());
                assertEquals(2, new DynamicPaletteProvider().getPaletteSelector().size());
                File next = new File(fixture.scope.backups,"blocks-B.json"); write(next,GSON.toJson(List.of(global(fixture.name,"PATH_B"))));
                setPaths(manager, next, fixture.palettes);
                assertEquals("setTitle(\"PATH_B\");", BlockLoader.getBlockInfo(fixture.name).getCode());
            }
        }
    }

    @Test public void equalFileStampsDoNotHideChangedPaths() throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.blocks,GSON.toJson(List.of(global(fixture.name,"PATH_A"))));
            write(fixture.palettes,"[{\"name\":\"Path A\",\"color\":\"#1976D2\"}]");
            ExtraBlockFile.invalidateCache();
            assertEquals("setTitle(\"PATH_A\");", ExtraBlockFile.getExtraBlockData("9").get(0).get("code"));
            assertEquals("Path A", ExtraBlockFile.getPaletteBlockData().get(0).get("name"));
            File nextBlocks=new File(fixture.scope.backups,"blocks-B.json"), nextPalettes=new File(fixture.scope.backups,"palettes-B.json");
            write(nextBlocks,read(fixture.blocks).replace("PATH_A","PATH_B")); write(nextPalettes,read(fixture.palettes).replace("Path A","Path B"));
            assertEquals(fixture.blocks.length(),nextBlocks.length()); assertEquals(fixture.palettes.length(),nextPalettes.length());
            assertTrue(nextBlocks.setLastModified(fixture.blocks.lastModified())); assertTrue(nextPalettes.setLastModified(fixture.palettes.lastModified()));
            fixture.configure(nextBlocks,nextPalettes);
            assertEquals("setTitle(\"PATH_B\");", ExtraBlockFile.getExtraBlockData("9").get(0).get("code"));
            assertEquals("Path B", ExtraBlockFile.getPaletteBlockData().get(0).get("name"));
        }
    }

    @Test public void defaultsSwitchBackWithoutWritingDefaultFiles() throws Exception {
        try (Fixture fixture = new Fixture()) {
            write(fixture.blocks,GSON.toJson(List.of(global(fixture.name,"PATH_A")))); write(fixture.palettes,LEGAL);
            File defaultBlocks=new File(SketchwarePaths.getBlockManagerBlockFilePath()), defaultPalettes=new File(SketchwarePaths.getBlockManagerPaletteFilePath());
            byte[] blocks=defaultBlocks.exists()?Files.readAllBytes(defaultBlocks.toPath()):null;
            byte[] palettes=defaultPalettes.exists()?Files.readAllBytes(defaultPalettes.toPath()):null;
            long blockTime=defaultBlocks.lastModified(), paletteTime=defaultPalettes.lastModified();
            try(ActivityScenario<BlocksManager> manager=ActivityScenario.launch(BlocksManager.class)) {
                setPaths(manager,fixture.blocks,fixture.palettes); assertFalse(BlockLoader.getBlockInfo(fixture.name).isMissing);
                activityAction(manager,CustomBlockPaletteConfigurationTest::showConfiguration); click(android.R.id.button3);
                assertEquals(defaultBlocks.getCanonicalFile(),ExtraBlockFile.getExtraBlocksDataFile().getCanonicalFile());
                assertEquals(defaultPalettes.getCanonicalFile(),ExtraBlockFile.getExtraBlocksPaletteFile().getCanonicalFile());
                assertTrue(BlockLoader.getBlockInfo(fixture.name).isMissing);
            }
            assertEquals(blocks!=null,defaultBlocks.exists()); assertEquals(palettes!=null,defaultPalettes.exists());
            if(blocks!=null)assertArrayEquals(blocks,Files.readAllBytes(defaultBlocks.toPath()));
            if(palettes!=null)assertArrayEquals(palettes,Files.readAllBytes(defaultPalettes.toPath()));
            assertEquals(blockTime,defaultBlocks.lastModified()); assertEquals(paletteTime,defaultPalettes.lastModified());
        }
    }

    @Test public void configuredBlockChangesInvalidateGeneratedSourceKey() throws Exception {
        try(Fixture fixture=new Fixture()) {
            ProjectFilePaths paths=new ProjectFilePaths(CONTEXT, fixture.project.id());
            Method key=ProjectFilePaths.class.getDeclaredMethod("computeCodeGenCacheKey",ProjectFileManager.class,ProjectDataStore.class,LibraryManager.class);key.setAccessible(true);
            write(fixture.blocks,GSON.toJson(List.of(global(fixture.name,"PATH_A"))));
            String first=(String)key.invoke(paths,null,null,null);
            write(fixture.blocks,GSON.toJson(List.of(global(fixture.name,"PATH_B"))));
            assertNotEquals(first,key.invoke(paths,null,null,null));
        }
    }

    private static HashMap<String,Object> global(String name,String title) {
        HashMap<String,Object> map=new HashMap<>();map.put("name",name);map.put("spec","probe");map.put("type"," ");
        map.put("code","setTitle(\""+title+"\");");map.put("color","#1976D2");map.put("palette","9");return map;
    }
    static final class Fixture implements AutoCloseable {
        final ActivityScenario<PermissionResultTestActivity> host;
        final CustomBlockBackupTest.Scope scope;
        final CustomBlockBackupTest.OwnedProject project;
        final Map<Field,Object> extraState=new LinkedHashMap<>();
        final File blocks,palettes;
        final String name;
        Fixture() throws Exception {
            host=ActivityScenario.launch(PermissionResultTestActivity.class);
            host.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            scope=new CustomBlockBackupTest.Scope();
            for(Field f:ExtraBlockFile.class.getDeclaredFields()) {
                if(!Modifier.isStatic(f.getModifiers())||f.isSynthetic())continue;
                f.setAccessible(true);Object value=f.get(null);
                if(value instanceof List list)value=new ArrayList<>(list);
                else if(value instanceof Map map)value=new HashMap<>(map);
                extraState.put(f,value);
            }
            project=new CustomBlockBackupTest.OwnedProject(); project.fixture.seed(false,false);
            name="PaletteConfig"+scope.token;blocks=new File(scope.backups,"blocks.json");palettes=new File(scope.backups,"palettes.json");
            ExtraBlockInfo info=new ExtraBlockInfo();info.setName(name);info.setSpec("probe");info.setCode("setTitle(\"CONFIG\");");info.setImports("java.util.Locale");info.setColor(0xff1976d2);
            write(new File(SketchwarePaths.getProjectCustomBlocksPath(project.id())),GSON.toJson(List.of(info)));
            configure(blocks,palettes);write(blocks,"[]");write(palettes,"[]");ExtraBlockFile.invalidateCache();BlockLoader.refresh();
        }
        void configure(File blocks,File palettes) {
            synchronized(scope.store) {
                scope.settings.put(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_BLOCK_FILE_PATH,blocks.getAbsolutePath());
                scope.settings.put(ApplicationSettings.SETTING_BLOCKMANAGER_DIRECTORY_PALETTE_FILE_PATH,palettes.getAbsolutePath());
            }
        }
        void importDialog() throws Exception {
            Method method=CustomBlocksDialog.class.getDeclaredMethod("importAll",Context.class,CustomBlocksManager.class,ArrayList.class);method.setAccessible(true);
            BlockBean bean=new BlockBean("1","probe"," ",name);bean.color=0xff1976d2;
            activityAction(host,activity -> {
                try{method.invoke(new CustomBlocksDialog(),activity,new CustomBlocksManager(activity,project.id()),new ArrayList<>(List.of(bean)));}
                catch(Exception e){throw new RuntimeException(e);}
            }); INSTRUMENTATION.waitForIdleSync();
        }
        @SuppressWarnings({"rawtypes","unchecked"}) @Override public void close() throws Exception {
            try {
                PaletteOrderSaveTest.awaitSave();
                onMain(() -> {View root=dialogRoot();if(root!=null && root.findViewById(android.R.id.button2)!=null)root.findViewById(android.R.id.button2).performClick();});
                host.close(); project.close();
            } finally {
                for(Map.Entry<Field,Object> entry:extraState.entrySet()) {
                    Object current=entry.getKey().get(null), original=entry.getValue();
                    if(current instanceof List list){list.clear();list.addAll((List)original);}
                    else if(current instanceof Map map){map.clear();map.putAll((Map)original);}
                    else if(!Modifier.isFinal(entry.getKey().getModifiers()))entry.getKey().set(null,original);
                }
                scope.close();
            }
        }
    }
    private static void setPaths(ActivityScenario<BlocksManager> scenario,File blocks,File palettes) {
        activityAction(scenario,CustomBlockPaletteConfigurationTest::showConfiguration);
        onMain(() -> {
            View root=dialogRoot();assertNotNull(root);
            ((EditText)root.findViewById(R.id.blocksPath)).setText(SketchwarePaths.toExternalStorageRelativePath(blocks.getAbsolutePath()));
            ((EditText)root.findViewById(R.id.palettesPath)).setText(SketchwarePaths.toExternalStorageRelativePath(palettes.getAbsolutePath()));
        });click(android.R.id.button1);
    }
    private static void showConfiguration(Activity activity) {
        try {Method method=BlocksManager.class.getDeclaredMethod("showBlockConfigurationDialog");method.setAccessible(true);method.invoke(activity);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static <A extends Activity> void activityAction(ActivityScenario<A> scenario,Consumer<A> action) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        scenario.onActivity(activity -> {try{action.accept(activity);}catch(Throwable e){failure.set(e);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private static void onMain(Runnable action) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        INSTRUMENTATION.runOnMainSync(() -> {try{action.run();}catch(Throwable e){failure.set(e);}});
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
    private static View dialogRoot() {
        List<View> roots=WindowInspector.getGlobalWindowViews();
        for(int i=roots.size()-1;i>=0;i--){View root=roots.get(i);if(root.isShown() && root.findViewById(android.R.id.button1)!=null)return root;}return null;
    }
    private static ListView findList(View view) {
        if(view instanceof ListView list)return list;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){ListView list=findList(group.getChildAt(i));if(list!=null)return list;}return null;
    }
    private static void click(int button) {onMain(() -> {assertNotNull(dialogRoot());assertTrue(dialogRoot().findViewById(button).performClick());});INSTRUMENTATION.waitForIdleSync();}
    private static List<HashMap<String,Object>> maps(File file) throws Exception {return GSON.fromJson(read(file),new TypeToken<List<HashMap<String,Object>>>(){}.getType());}
    private static String read(File file) throws Exception {return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);}
    private static void write(File file,String content) throws Exception {Files.write(file.toPath(),content.getBytes(StandardCharsets.UTF_8));}
}
