package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManager;
import pro.sketchware.activities.editor.block.BlocksManagerDetailsActivity;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.project.ProjectDataManager;

/** Real import/manager actions on owned files, including malformed JSON and preserved null slots. */
@RunWith(AndroidJUnit4.class)
public class CustomBlockPaletteIntegrityTest {
    private static final Instrumentation INSTRUMENTATION = InstrumentationRegistry.getInstrumentation();
    private static final Gson GSON = new Gson();
    private static final String PALETTES = "[null,{\"name\":\"A\",\"color\":\"#1976D2\"},{\"name\":\"B\",\"color\":\"#E91E63\"}]";
    private static final String BLOCKS = "[null,{\"name\":\"slot\",\"palette\":\"9\"},{\"name\":\"a\",\"palette\":\"10\"},{\"name\":\"b\",\"spec\":\"needle\",\"palette\":\"11\"},{\"name\":\"bin\",\"palette\":\"-1\"},{\"unrelated\":\"keep\"}]";
    private static final String TARGET = "[{\"name\":\"Target\",\"color\":\"#1976D2\"}]";

    @Test public void malformedBlocksAreNotOverwritten() throws Exception { rejects("[{\"keep\":", TARGET); }
    @Test public void blockObjectIsNotOverwritten() throws Exception { rejects("{\"keep\":\"data\"}", TARGET); }
    @Test public void scalarBlockEntryIsNotOverwritten() throws Exception { rejects("[17]", TARGET); }
    @Test public void malformedPalettesAreNotOverwritten() throws Exception { rejects("[]", "[{\"name\":"); }
    @Test public void paletteObjectIsNotOverwritten() throws Exception { rejects("[]", "{\"keep\":\"data\"}"); }
    @Test public void scalarPaletteEntryIsNotOverwritten() throws Exception { rejects("[]", "[false]"); }
    @Test public void trailingJsonIsNotOverwritten() throws Exception { rejects("[] {}", TARGET); }

    private static void rejects(String blocks, String palettes) throws Exception {
        try (var fixture = new CustomBlockPaletteConfigurationTest.Fixture()) {
            write(fixture.blocks, blocks); write(fixture.palettes, palettes);
            long blockTime = fixture.blocks.lastModified(), paletteTime = fixture.palettes.lastModified();
            fixture.importDialog();
            onMain(() -> {
                View root = root(); assertNotNull(root.findViewById(android.R.id.message));
                assertNull(find(root, ListView.class)); assertNull(root.findViewById(R.id.nameEditText));
            });
            click(android.R.id.button1);
            assertEquals(blocks, read(fixture.blocks)); assertEquals(palettes, read(fixture.palettes));
            assertEquals(blockTime, fixture.blocks.lastModified()); assertEquals(paletteTime, fixture.palettes.lastModified());
        }
    }

    @Test public void directoryDestinationIsRejected() throws Exception {
        try (var fixture = new CustomBlockPaletteConfigurationTest.Fixture()) {
            write(fixture.palettes, TARGET); assertTrue(fixture.blocks.delete()); assertTrue(fixture.blocks.mkdir());
            fixture.importDialog(); onMain(() -> assertNotNull(root().findViewById(android.R.id.message)));
            click(android.R.id.button1); assertTrue(fixture.blocks.isDirectory()); assertEquals(TARGET, read(fixture.palettes));
        }
    }
    @Test public void missingDestinationCanBeInitialized() throws Exception { initializes(null); }
    @Test public void emptyDestinationCanBeInitialized() throws Exception { initializes(""); }
    @Test public void whitespaceDestinationCanBeInitialized() throws Exception { initializes(" \n\t "); }

    private static void initializes(String content) throws Exception {
        try (var fixture = new CustomBlockPaletteConfigurationTest.Fixture()) {
            if (content == null) assertTrue(fixture.blocks.delete()); else write(fixture.blocks, content);
            write(fixture.palettes, TARGET); fixture.importDialog(); click(android.R.id.button1);
            List<HashMap<String, Object>> saved = maps(fixture.blocks);
            assertEquals(1, saved.size()); assertEquals(fixture.name, saved.get(0).get("name"));
            assertEquals("9", saved.get(0).get("palette")); assertEquals(TARGET, read(fixture.palettes));
        }
    }

    @Test public void failedImportKeepsSelectionAndAllowsRetry() throws Exception {
        try (var fixture = new CustomBlockPaletteConfigurationTest.Fixture()) {
            ProjectDataManager.reloadProjectFromData(fixture.project.id());
            var chain = new java.util.ArrayList<BlockBean>();
            chain.add(new BlockBean("1", "probe", " ", fixture.name));
            ProjectDataManager.getProjectDataManager(fixture.project.id()).putBlocks(
                    ProjectFileBean.DEFAULT_JAVA_NAME, "onCreate_initializeLogic", chain);
            assertTrue(ProjectDataManager.getProjectDataManager(fixture.project.id()).saveAllData());
            BlockLoader.invalidateProjectCache(); write(fixture.blocks, "[{\"keep\":"); write(fixture.palettes, TARGET);
            action(fixture.host, activity -> new CustomBlocksDialog().show(activity, fixture.project.id()));
            await(() -> mainValue(() -> {
                View used = rootWith(R.id.subtitle); if (used == null) return false;
                RecyclerView recycler = used.findViewById(R.id.recyclerView);
                return recycler.getAdapter() != null && recycler.findViewHolderForAdapterPosition(0) != null;
            }));
            onMain(() -> {
                RecyclerView recycler = rootWith(R.id.subtitle).findViewById(R.id.recyclerView);
                assertTrue(recycler.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.transparentOverlay).performClick());
            }); click(android.R.id.button1);
            onMain(() -> {
                assertNotNull(root().findViewById(android.R.id.message));
                assertNotNull(rootWith(R.id.subtitle));
            }); click(android.R.id.button1);
            onMain(() -> {
                RecyclerView recycler = rootWith(R.id.subtitle).findViewById(R.id.recyclerView);
                assertTrue(((CheckBox)recycler.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.checkBox)).isChecked());
            });
            assertEquals("[{\"keep\":", read(fixture.blocks)); write(fixture.blocks, "[]");
            click(android.R.id.button1); onMain(() -> assertNull(rootWith(R.id.subtitle)));
            click(android.R.id.button1); assertEquals(fixture.name, maps(fixture.blocks).get(0).get("name"));
        }
    }

    @Test public void nullRecordsDisplayWithoutWritingFiles() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            action(manager, activity -> {
                RecyclerView recycler = activity.findViewById(R.id.paletteRecycler);
                assertEquals(3, recycler.getAdapter().getItemCount());
                BlocksManager.PaletteAdapter.ViewHolder holder = (BlocksManager.PaletteAdapter.ViewHolder) recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(holder); assertFalse(holder.itemBinding.title.getText().toString().isEmpty());
                assertTrue(holder.itemBinding.sub.getText().toString().contains("1"));
                holder.itemBinding.backgroundCard.performClick();
            });
            assertEquals(PALETTES, read(fixture.palettes)); assertEquals(BLOCKS, read(fixture.blocks));
        }
    }

    @Test public void editRepairsNullSlotWithoutChangingIds() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            menu(manager, 0, 0); input("Recovered", "1976D2"); click(android.R.id.button1);
            assertEquals(3, maps(fixture.palettes).size()); assertEquals("Recovered", maps(fixture.palettes).get(0).get("name"));
            assertEquals(BLOCKS, read(fixture.blocks));
        }
    }
    @Test public void searchEditUsesOriginalPalettePosition() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            search(manager, "needle"); menu(manager, 0, 0); input("Edited B", "1976D2"); click(android.R.id.button1);
            assertNull(maps(fixture.palettes).get(0)); assertEquals("A", maps(fixture.palettes).get(1).get("name"));
            assertEquals("Edited B", maps(fixture.palettes).get(2).get("name")); assertEquals(BLOCKS, read(fixture.blocks));
        }
    }
    @Test public void searchInsertShiftsOnlyFollowingIds() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            search(manager, "needle"); menu(manager, 0, 2); input("Inserted", "1976D2"); click(android.R.id.button1);
            List<HashMap<String, Object>> palettes = maps(fixture.palettes); assertEquals(4, palettes.size()); assertNull(palettes.get(0));
            assertEquals("Inserted", palettes.get(2).get("name")); assertEquals("B", palettes.get(3).get("name"));
            assertBlockIds(fixture.blocks, "9", "10", "12", "-1");
        }
    }
    @Test public void searchDeleteUsesOriginalPalettePosition() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            search(manager, "needle"); menu(manager, 0, 1); click(android.R.id.button1);
            assertEquals(2, maps(fixture.palettes).size()); assertNull(maps(fixture.palettes).get(0));
            List<HashMap<String, Object>> blocks = maps(fixture.blocks); assertEquals(5, blocks.size()); assertNull(blocks.get(0));
            assertEquals("9", blocks.get(1).get("palette")); assertEquals("10", blocks.get(2).get("palette"));
            assertEquals("-1", blocks.get(3).get("palette")); assertEquals("keep", blocks.get(4).get("unrelated"));
        }
    }
    @Test public void explicitNullSlotDeletionShiftsIds() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            menu(manager, 0, 1); click(android.R.id.button1);
            assertEquals("A", maps(fixture.palettes).get(0).get("name")); assertEquals("B", maps(fixture.palettes).get(1).get("name"));
            List<HashMap<String, Object>> blocks = maps(fixture.blocks); assertEquals(5, blocks.size()); assertNull(blocks.get(0));
            assertEquals("9", blocks.get(1).get("palette")); assertEquals("10", blocks.get(2).get("palette"));
            assertEquals("-1", blocks.get(3).get("palette")); assertEquals("keep", blocks.get(4).get("unrelated"));
        }
    }
    @Test public void movingPastNullSlotPreservesNullBlock() throws Exception {
        try (var fixture = managerFixture(); var manager = ActivityScenario.launch(BlocksManager.class)) {
            action(manager, activity -> {
                RecyclerView recycler = activity.findViewById(R.id.paletteRecycler);
                ItemTouchHelper helper = (ItemTouchHelper)field(activity, "itemTouchHelper");
                ItemTouchHelper.Callback callback = (ItemTouchHelper.Callback)field(helper, "mCallback");
                RecyclerView.ViewHolder from = recycler.findViewHolderForAdapterPosition(1), to = recycler.findViewHolderForAdapterPosition(0);
                assertNotNull(from); assertNotNull(to); assertTrue(callback.onMove(recycler, from, to)); callback.clearView(recycler, from);
            });
            assertEquals("A", maps(fixture.palettes).get(0).get("name")); assertNull(maps(fixture.palettes).get(1));
            assertBlockIds(fixture.blocks, "10", "9", "11", "-1");
        }
    }
    @Test public void validPaletteDetailsSkipNullBlocksWithoutWriting() throws Exception {
        try (var fixture = managerFixture()) {
            // Details render valid definitions; the null entry stays at its original file position.
            HashMap<String, Object> valid = new HashMap<>(); valid.put("name", fixture.name); valid.put("spec", "probe");
            valid.put("type", " "); valid.put("color", "#1976D2"); valid.put("code", ""); valid.put("palette", "10");
            String content = "[null," + GSON.toJson(valid) + "]"; write(fixture.blocks, content);
            Intent intent = new Intent(INSTRUMENTATION.getTargetContext(), BlocksManagerDetailsActivity.class);
            intent.putExtra("position", "10"); intent.putExtra("dirP", fixture.palettes.getAbsolutePath()); intent.putExtra("dirB", fixture.blocks.getAbsolutePath());
            try (var details = ActivityScenario.<BlocksManagerDetailsActivity>launch(intent)) {
                action(details, activity -> assertEquals(1, ((ListView)activity.findViewById(R.id.block_list)).getAdapter().getCount()));
                assertEquals(PALETTES, read(fixture.palettes)); assertEquals(content, read(fixture.blocks));
            }
        }
    }

    private static CustomBlockPaletteConfigurationTest.Fixture managerFixture() throws Exception {
        var fixture = new CustomBlockPaletteConfigurationTest.Fixture(); write(fixture.palettes, PALETTES); write(fixture.blocks, BLOCKS); return fixture;
    }
    private static void assertBlockIds(File file, String... ids) throws Exception {
        List<HashMap<String, Object>> blocks = maps(file); assertEquals(6, blocks.size()); assertNull(blocks.get(0));
        for (int i = 0; i < ids.length; i++) assertEquals(ids[i], blocks.get(i + 1).get("palette"));
        assertEquals("keep", blocks.get(5).get("unrelated"));
    }
    private static void search(ActivityScenario<BlocksManager> manager, String query) {
        action(manager, activity -> {
            androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(R.id.toolbar);
            android.view.MenuItem item = toolbar.getMenu().findItem(4); assertNotNull(item); assertTrue(item.expandActionView());
            ((SearchView)item.getActionView()).setQuery(query, false);
        }); INSTRUMENTATION.waitForIdleSync();
        action(manager, activity -> assertEquals(1, ((RecyclerView)activity.findViewById(R.id.paletteRecycler)).getAdapter().getItemCount()));
    }
    private static void menu(ActivityScenario<BlocksManager> manager, int position, int choice) {
        action(manager, activity -> {
            RecyclerView recycler = activity.findViewById(R.id.paletteRecycler);
            var holder = (BlocksManager.PaletteAdapter.ViewHolder)recycler.findViewHolderForAdapterPosition(position);
            assertNotNull(holder); assertTrue(holder.itemBinding.backgroundCard.performLongClick());
        }); INSTRUMENTATION.waitForIdleSync();
        onMain(() -> {
            ListView popup = null;
            for (View view : WindowInspector.getGlobalWindowViews()) {
                ListView list = find(view, ListView.class);
                if (view.isShown() && list != null && list.getAdapter() != null && list.getAdapter().getCount() == 3) popup = list;
            }
            assertNotNull(popup); assertTrue(popup.performItemClick(popup.getChildAt(choice), choice, popup.getAdapter().getItemId(choice)));
        }); INSTRUMENTATION.waitForIdleSync();
    }
    private static void input(String name, String hex) { onMain(() -> {((EditText)root().findViewById(R.id.nameEditText)).setText(name); ((EditText)root().findViewById(R.id.colorEditText)).setText(hex);}); }
    private static Object field(Object target, String name) { try {Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);} catch (Exception e) {throw new AssertionError(e);} }
    private static <A extends Activity> void action(ActivityScenario<A> scenario, Consumer<A> action) {
        AtomicReference<Throwable> error = new AtomicReference<>(); scenario.onActivity(activity -> {try {action.accept(activity);} catch(Throwable e) {error.set(e);}});
        if (error.get() != null) throw new AssertionError(error.get()); INSTRUMENTATION.waitForIdleSync();
    }
    private static void onMain(Runnable action) { AtomicReference<Throwable> error = new AtomicReference<>(); INSTRUMENTATION.runOnMainSync(() -> {try {action.run();} catch(Throwable e) {error.set(e);}}); if (error.get()!=null) throw new AssertionError(error.get()); }
    private static boolean mainValue(BooleanSupplier action) { boolean[] result = {false}; onMain(() -> result[0] = action.getAsBoolean()); return result[0]; }
    private static void await(BooleanSupplier action) { long until = SystemClock.uptimeMillis() + 15000; while(!action.getAsBoolean() && SystemClock.uptimeMillis() < until) SystemClock.sleep(50); assertTrue(action.getAsBoolean()); }
    private static View rootWith(int id) { List<View> roots = WindowInspector.getGlobalWindowViews(); for(int i=roots.size()-1;i>=0;i--) if(roots.get(i).isShown() && roots.get(i).findViewById(id)!=null) return roots.get(i); return null; }
    private static View root() { return rootWith(android.R.id.button1); }
    private static <T extends View> T find(View view, Class<T> type) { if(type.isInstance(view)) return type.cast(view); if(view instanceof ViewGroup group) for(int i=0;i<group.getChildCount();i++) {T found=find(group.getChildAt(i),type); if(found!=null) return found;} return null; }
    private static void click(int id) { onMain(() -> {assertNotNull(root()); assertTrue(root().findViewById(id).performClick());}); INSTRUMENTATION.waitForIdleSync(); }
    private static String read(File file) throws Exception {return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);}
    private static void write(File file, String value) throws Exception {Files.write(file.toPath(),value.getBytes(StandardCharsets.UTF_8));}
    private static List<HashMap<String,Object>> maps(File file) throws Exception {return GSON.fromJson(read(file),new TypeToken<List<HashMap<String,Object>>>(){}.getType());}
}
