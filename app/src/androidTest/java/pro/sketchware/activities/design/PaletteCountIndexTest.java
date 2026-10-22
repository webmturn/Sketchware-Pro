package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import android.view.WindowManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManager;

@RunWith(AndroidJUnit4.class)
public class PaletteCountIndexTest {
    static final String BLOCKS="[{\"palette\":\"9\"},{\"palette\":\"9\"},{\"palette\":\"10\"},{\"palette\":\"11\"},{\"palette\":\"-1\"}]";

    @Test public void countsKeepRawPaletteRepresentationsAndNullHandling() throws Exception {
        try(Fixture f=new Fixture("[null,{}, {\"palette\":null},{\"palette\":\"9\"},{\"palette\":\"9\"},{\"palette\":9},{\"palette\":\"9.0\"},{\"palette\":\"10\"},{\"palette\":\"-1\"},{\"palette\":-1},{\"palette\":\"bad\"}]",3)) {
            f.run(a->{counts(a,2,1,0,1);assertRow(a,0,2);assertRow(a,1,1);assertRow(a,2,0);});
        }
    }
    @Test public void bindingRowsDoesNotRescanBlockMaps() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            f.run(a->{
                AtomicInteger reads=new AtomicInteger();
                @SuppressWarnings("unchecked") var original=(ArrayList<HashMap<String,Object>>)field(a,"all_blocks_list");
                ArrayList<HashMap<String,Object>> tracked=new ArrayList<>();
                for(var map:original)tracked.add(new CountingMap(map,reads));
                setField(a,"all_blocks_list",tracked);
                for(int i=0;i<12;i++)assertRow(a,i%3,i%3==0?2:1);
                assertEquals("Row binding must not inspect the block collection",0,reads.get());
            });
        }
    }
    @Test public void dragUpdatesCountsBeforeReloadingFromDisk() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            f.run(a->{
                RecyclerView recycler=a.findViewById(R.id.paletteRecycler);
                ItemTouchHelper helper=(ItemTouchHelper)field(a,"itemTouchHelper");
                var callback=(ItemTouchHelper.Callback)field(helper,"mCallback");
                var from=recycler.findViewHolderForAdapterPosition(0);var to=recycler.findViewHolderForAdapterPosition(1);
                assertNotNull(from);assertNotNull(to);assertTrue(callback.onMove(recycler,from,to));
                counts(a,1,2,1,1);assertRow(a,0,1);assertRow(a,1,2);
                callback.clearView(recycler,from);
            });
        }
    }
    @Test public void dragVisitsEachBlockOnceWithoutRefreshingRecycleText() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, 3)) {
            f.run(a -> {
                AtomicInteger reads = new AtomicInteger();
                AtomicInteger recycleChanges = new AtomicInteger();
                @SuppressWarnings("unchecked")
                var original = (ArrayList<HashMap<String, Object>>) field(a, "all_blocks_list");
                ArrayList<HashMap<String, Object>> tracked = new ArrayList<>();
                for (var map : original) tracked.add(new CountingMap(map, reads));
                setField(a, "all_blocks_list", tracked);
                android.widget.TextView recycle = a.findViewById(R.id.recycle_sub);
                recycle.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        recycleChanges.incrementAndGet();
                    }
                    @Override public void afterTextChanged(android.text.Editable s) {}
                });
                RecyclerView recycler = a.findViewById(R.id.paletteRecycler);
                ItemTouchHelper helper = (ItemTouchHelper) field(a, "itemTouchHelper");
                var callback = (ItemTouchHelper.Callback) field(helper, "mCallback");
                var from = recycler.findViewHolderForAdapterPosition(0);
                var to = recycler.findViewHolderForAdapterPosition(1);
                assertNotNull(from);
                assertNotNull(to);
                assertTrue(callback.onMove(recycler, from, to));
                assertEquals("A move must inspect each block only once", original.size(), reads.get());
                assertEquals("Dragging cannot change the recycle-bin count", 0, recycleChanges.get());
                counts(a, 1, 2, 1, 1);
                callback.clearView(recycler, from);
            });
        }
    }
    @Test public void repeatedSwapsKeepLegacyValuesAndIndexConsistent() throws Exception {
        String blocks = "[null,{}, {\"palette\":null},{\"palette\":\"9\"},"
                + "{\"palette\":\"9.0\"},{\"palette\":9},{\"palette\":\" 9 \"},"
                + "{\"palette\":\"9e0\"},{\"palette\":\"10\"},{\"palette\":\"10.0\"},"
                + "{\"palette\":10},{\"palette\":\"11\"},{\"palette\":\"-1\"},"
                + "{\"palette\":-1},{\"palette\":\"bad\"},{\"palette\":\"TEMP_SWAP\"}]";
        try (Fixture f = new Fixture(blocks, 3)) {
            f.run(a -> {
                @SuppressWarnings("unchecked")
                var actual = (ArrayList<HashMap<String, Object>>) field(a, "all_blocks_list");
                // A same-key swap must still normalize a non-String numeric value.
                actual.get(3).put("palette", 9L);
                actual.get(8).put("palette", 10);
                invoke(a, "refreshBlockCounts", new Class[0]);
                ArrayList<HashMap<String, Object>> expected = new ArrayList<>();
                for (var block : actual) expected.add(block == null ? null : new HashMap<>(block));
                for (int[] pair : new int[][]{{9,9},{9,10},{10,11},{11,10},{10,9},{9,10},{10,9}}) {
                    legacySwap(expected, pair[0], pair[1]);
                    invoke(a, "swapRelatedBlocks", new Class[]{double.class, double.class},
                            (double) pair[0], (double) pair[1]);
                    assertEquals("Swapping must preserve the former model transformation", expected, actual);
                    assertIndexMatchesModels(a, actual);
                    assertEquals(1, count(a, -1));
                }
                RecyclerView recycler = a.findViewById(R.id.paletteRecycler);
                ItemTouchHelper helper = (ItemTouchHelper) field(a, "itemTouchHelper");
                var callback = (ItemTouchHelper.Callback) field(helper, "mCallback");
                callback.clearView(recycler, recycler.findViewHolderForAdapterPosition(0));
                invoke(a, "readSettings", new Class[0]);
                // Existing Gson serialization omits null map values when writing the file.
                for (var block : expected) {
                    if (block != null && block.get("palette") == null) block.remove("palette");
                }
                assertEquals(expected, field(a, "all_blocks_list"));
                assertIndexMatchesModels(a, expected);
            });
        }
    }
    static void legacySwap(ArrayList<HashMap<String, Object>> blocks, double from, double to) {
        for (var block : blocks) {
            if (block == null || block.get("palette") == null) continue;
            double value;
            try { value = Double.parseDouble(block.get("palette").toString()); }
            catch (NumberFormatException e) { continue; }
            if (value == from) block.put("palette", "TEMP_SWAP");
            else if (value == to) block.put("palette", String.valueOf((long) from));
        }
        for (var block : blocks) {
            if (block != null && "TEMP_SWAP".equals(block.get("palette"))) {
                block.put("palette", String.valueOf((long) to));
            }
        }
    }
    static void assertIndexMatchesModels(BlocksManager activity, ArrayList<HashMap<String, Object>> blocks) {
        Map<String, Integer> expected = new HashMap<>();
        for (var block : blocks) {
            if (block != null) expected.merge(String.valueOf(block.get("palette")), 1, Integer::sum);
        }
        assertEquals("Index must contain exactly the current raw keys and counts", expected,
                field(activity, "blockCountsByPalette"));
    }
    @Test public void insertDeleteRecycleAndEmptyKeepCountsCurrent() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            f.run(a->{
                invoke(a,"insertBlocksAt",new Class[]{double.class},10d);
                assertEquals(0,count(a,10));assertEquals(1,count(a,11));assertEquals(1,count(a,12));
                invoke(a,"removeRelatedBlocks",new Class[]{double.class},10d);counts(a,2,1,1,1);
                invoke(a,"moveRelatedBlocksToRecycleBin",new Class[]{double.class},9d);counts(a,0,1,1,3);
                invoke(a,"emptyRecyclebin",new Class[0]);counts(a,0,1,1,0);assertRow(a,0,0);assertRow(a,1,1);
            });
        }
    }
    @Test public void resumeReloadsChangesFromOtherPages() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            f.manager.moveToState(Lifecycle.State.CREATED);
            f.write("[{\"palette\":\"10\"},{\"palette\":\"10\"},{\"palette\":\"-1\"}]");
            f.manager.moveToState(Lifecycle.State.RESUMED);
            f.run(a->{counts(a,0,2,0,1);assertRow(a,0,0);assertRow(a,1,2);});
        }
    }
    @Test public void missingAndEmptyReloadsAgreeWithCurrentModels() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            Files.delete(f.files.blocks.toPath());
            f.run(a->{invoke(a,"readSettings",new Class[0]);counts(a,2,1,1,1);});
            f.write("[]");
            f.run(a->{invoke(a,"readSettings",new Class[0]);counts(a,0,0,0,0);assertRow(a,0,0);});
        }
    }
    @Test public void switchingConfiguredBlockFileReplacesAllCounts() throws Exception {
        try(Fixture f=new Fixture(BLOCKS,3)) {
            var next=new java.io.File(f.files.scope.backups,"next-blocks.json");
            Files.write(next.toPath(),"[{\"palette\":\"11\"},{\"palette\":\"11\"},{\"palette\":\"-1\"},{\"palette\":\"-1\"}]".getBytes(StandardCharsets.UTF_8));
            f.files.configure(next,f.files.palettes);
            f.run(a->{invoke(a,"readSettings",new Class[0]);invoke(a,"refreshList",new Class[0]);counts(a,0,0,2,2);assertRow(a,2,2);});
        }
    }
    static void assertRow(BlocksManager a,int position,int expected) {
        RecyclerView recycler=a.findViewById(R.id.paletteRecycler);
        var adapter=(BlocksManager.PaletteAdapter)recycler.getAdapter();
        var holder=adapter.onCreateViewHolder(recycler,0);adapter.onBindViewHolder(holder,position);
        assertEquals(a.getString(R.string.blocks_count_format,(long)expected),holder.itemBinding.sub.getText().toString());
    }
    static void counts(BlocksManager a,int p9,int p10,int p11,int trash) {
        assertEquals(p9,count(a,9));assertEquals(p10,count(a,10));assertEquals(p11,count(a,11));assertEquals(trash,count(a,-1));
        assertEquals(a.getString(R.string.blocks_count_format,(long)trash),((android.widget.TextView)a.findViewById(R.id.recycle_sub)).getText().toString());
    }
    static int count(BlocksManager a,double palette){return ((Number)invoke(a,"getN",new Class[]{double.class},palette)).intValue();}
    static Object invoke(Object target,String name,Class<?>[] types,Object...args) {
        try{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}
        catch(Exception e){throw new AssertionError(e);}
    }
    static Object field(Object target,String name){try{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new AssertionError(e);}}
    static void setField(Object target,String name,Object value){try{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}catch(Exception e){throw new AssertionError(e);}}
    static final class CountingMap extends HashMap<String,Object> {
        final AtomicInteger reads;
        CountingMap(Map<String,Object> original,AtomicInteger reads){super(original);this.reads=reads;}
        @Override public Object get(Object key){if("palette".equals(key))reads.incrementAndGet();return super.get(key);}
    }
    static final class Fixture implements AutoCloseable {
        final CustomBlockPaletteConfigurationTest.Fixture files;
        final ActivityScenario<BlocksManager> manager;
        Fixture(String blocks,int paletteCount) throws Exception {
            files=new CustomBlockPaletteConfigurationTest.Fixture();write(blocks);
            StringBuilder palettes=new StringBuilder("[");
            for(int i=0;i<paletteCount;i++){if(i>0)palettes.append(',');palettes.append("{\"name\":\"P").append(i).append("\",\"color\":\"#1976D2\"}");}
            Files.write(files.palettes.toPath(),palettes.append(']').toString().getBytes(StandardCharsets.UTF_8));
            manager=ActivityScenario.launch(BlocksManager.class);
            run(a->a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        void write(String value)throws Exception{Files.write(files.blocks.toPath(),value.getBytes(StandardCharsets.UTF_8));}
        void run(Consumer<BlocksManager> action){AtomicReference<Throwable> failure=new AtomicReference<>();manager.onActivity(a->{try{action.accept(a);}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new AssertionError(failure.get());InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
        @Override public void close()throws Exception{try{manager.close();}finally{files.close();}}
    }
}
