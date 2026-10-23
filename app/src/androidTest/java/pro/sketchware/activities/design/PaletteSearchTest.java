package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import static pro.sketchware.activities.design.PaletteCountIndexTest.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import pro.sketchware.activities.editor.block.BlocksManager;

@RunWith(AndroidJUnit4.class)
public class PaletteSearchTest {
    @Test public void cancelledReaderKeepsItsMapsButOrdinaryDraggingDoesNotCopy() throws Exception {
        try (Fixture f = new Fixture("[]", 3)) {
            java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<java.util.concurrent.ExecutorService> executor = new java.util.concurrent.atomic.AtomicReference<>();
            try {
                f.run(a -> {
                    executor.set((java.util.concurrent.ExecutorService) field(a, "SEARCH_EXECUTOR"));
                    executor.get().execute(() -> {
                        try { assertTrue(release.await(10, java.util.concurrent.TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    });
                    ArrayList<HashMap<String, Object>> blocks = new ArrayList<>();
                    for (int i = 0; i < 2000; i++) {
                        HashMap<String, Object> block = new HashMap<>();
                        block.put("palette", "10"); block.put("name", "sample"); blocks.add(block);
                    }
                    setField(a, "all_blocks_list", blocks);
                    invoke(a, "refreshBlockCounts", new Class[0]);
                    invoke(a, "filterPalettes", new Class[]{String.class}, "sample");
                    invoke(a, "filterPalettes", new Class[]{String.class}, "");
                    var snapshotMap = blocks.get(0);
                    invoke(a, "swapRelatedBlocks", new Class[]{double.class, double.class}, 9d, 10d);
                    assertNotSame(snapshotMap, blocks.get(0));
                    assertEquals("10", snapshotMap.get("palette"));
                    assertEquals("9", blocks.get(0).get("palette"));
                });
            } finally { release.countDown(); }
            executor.get().submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
            f.run(a -> {
                @SuppressWarnings("unchecked") var blocks = (ArrayList<HashMap<String, Object>>) field(a, "all_blocks_list");
                var before = blocks.get(0);
                invoke(a, "swapRelatedBlocks", new Class[]{double.class, double.class}, 9d, 10d);
                assertSame(before, blocks.get(0));
                assertEquals("10", before.get("palette"));
            });
        }
    }

    @Test public void largeSearchRunsOffMainAndSupersededResultsCannotReturn() throws Exception {
        try (Fixture f = new Fixture("[]", 3)) {
            java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>> blocker = new java.util.concurrent.atomic.AtomicReference<>();
            try {
                f.run(a -> {
                    var executor = (java.util.concurrent.ExecutorService) field(a, "SEARCH_EXECUTOR");
                    blocker.set(executor.submit(() -> {
                        try { assertTrue(release.await(10, java.util.concurrent.TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }));
                    ArrayList<HashMap<String, Object>> blocks = new ArrayList<>();
                    for (int i = 0; i < 5000; i++) {
                        HashMap<String, Object> block = new HashMap<>() {
                            @Override public Object get(Object key) {
                                assertNotSame("Large search must not inspect maps on main", android.os.Looper.getMainLooper(), android.os.Looper.myLooper());
                                return super.get(key);
                            }
                        };
                        block.put("palette", "10"); block.put("spec", "latest");
                        blocks.add(block);
                    }
                    setField(a, "all_blocks_list", blocks);
                    setField(a, "isSearchActive", true);
                    setField(a, "currentQuery", "missing");
                    invoke(a, "filterPalettes", new Class[]{String.class}, "missing");
                    assertEquals(true, field(a, "searchInProgress"));
                    setField(a, "currentQuery", "");
                    invoke(a, "filterPalettes", new Class[]{String.class}, "");
                    assertEquals(3, ((ArrayList<?>) field(a, "filtered_palette_indices")).size());
                    setField(a, "currentQuery", "latest");
                    invoke(a, "filterPalettes", new Class[]{String.class}, "latest");
                });
            } finally { release.countDown(); }
            blocker.get().get(10, java.util.concurrent.TimeUnit.SECONDS);
            awaitSearch(f);
            f.run(a -> assertEquals(java.util.Collections.singletonList(1), field(a, "filtered_palette_indices")));
        }
    }

    static void awaitSearch(Fixture f) {
        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
        java.util.concurrent.atomic.AtomicBoolean busy = new java.util.concurrent.atomic.AtomicBoolean(true);
        do {
            f.run(a -> busy.set((boolean) field(a, "searchInProgress")));
            if (!busy.get()) return;
            android.os.SystemClock.sleep(10);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Search did not finish");
    }

    @Test public void searchMatchesFormerResultsAndSourcePositions() throws Exception {
        try (Fixture f = new Fixture("[]", 20)) {
            f.run(a -> {
                @SuppressWarnings("unchecked")
                var palettes = (ArrayList<HashMap<String, Object>>) field(a, "pallet_listmap");
                palettes.set(2, null);
                palettes.get(4).put("name", 17);
                palettes.get(5).put("name", "MiXeD İ I");
                ArrayList<HashMap<String, Object>> blocks = new ArrayList<>();
                blocks.add(null);
                Object[] values = {null, "bad", "NaN", "Infinity", "-1", "9.5", "8", "29",
                        "9", "9.0", 10, 11d, " 12 ", "1.3e1", "14", "28"};
                Random random = new Random(3741);
                for (int i = 0; i < 999; i++) {
                    HashMap<String, Object> block = new HashMap<>();
                    block.put("palette", values[random.nextInt(values.length)]);
                    if (i % 3 != 0) block.put("name", i % 2 == 0 ? "NeEdLe " + i : 123);
                    if (i % 5 != 0) block.put("spec", "SPEC " + i);
                    blocks.add(block);
                }
                setField(a, "all_blocks_list", blocks);
                Locale original = Locale.getDefault();
                try {
                    Locale.setDefault(new Locale("tr", "TR"));
                    for (String query : new String[]{"", "p", "P1", "needle", "SPEC", "null", "123", "İ", "i", "missing"}) {
                        ArrayList<Integer> expected = formerSearch(a, palettes, blocks, query);
                        setField(a, "isSearchActive", true);
                        setField(a, "currentQuery", query);
                        invoke(a, "filterPalettes", new Class[]{String.class}, query);
                        assertEquals(query, expected, field(a, "filtered_palette_indices"));
                        @SuppressWarnings("unchecked")
                        var actual = (ArrayList<HashMap<String, Object>>) field(a, "filtered_pallet_list");
                        for (int i = 0; i < expected.size(); i++) {
                            assertSame(palettes.get(expected.get(i)), actual.get(i));
                            assertEquals(expected.get(i), invoke(a, "getDisplayedSourceIndex", new Class[]{int.class}, i));
                        }
                    }
                } finally { Locale.setDefault(original); }
            });
        }
    }

    @Test public void missingQueryReadsEachBlockOnlyOnceAndPaletteNamesSkipBlocks() throws Exception {
        try (Fixture f = new Fixture("[]", 100)) {
            f.run(a -> {
                AtomicInteger reads = new AtomicInteger();
                ArrayList<HashMap<String, Object>> blocks = new ArrayList<>();
                for (int i = 0; i < 1000; i++) {
                    HashMap<String, Object> block = new HashMap<>();
                    block.put("palette", Integer.toString(9 + i % 100));
                    blocks.add(new CountingMap(block, reads));
                }
                setField(a, "all_blocks_list", blocks);
                invoke(a, "filterPalettes", new Class[]{String.class}, "not-present");
                assertEquals(1000, reads.get());
                assertTrue(((ArrayList<?>) field(a, "filtered_palette_indices")).isEmpty());
                reads.set(0);
                invoke(a, "filterPalettes", new Class[]{String.class}, "p");
                assertEquals(0, reads.get());
                assertEquals(100, ((ArrayList<?>) field(a, "filtered_palette_indices")).size());
                setField(a, "all_blocks_list", null);
                invoke(a, "filterPalettes", new Class[]{String.class}, "p19");
                assertEquals(java.util.Collections.singletonList(19), field(a, "filtered_palette_indices"));
            });
        }
    }

    // Deliberately retain the previous nested search as a compatibility oracle.
    private static ArrayList<Integer> formerSearch(BlocksManager a,
            ArrayList<HashMap<String, Object>> palettes, ArrayList<HashMap<String, Object>> blocks, String query) {
        ArrayList<Integer> result = new ArrayList<>();
        String lower = query.toLowerCase(Locale.ROOT);
        for (int i = 0; i < palettes.size(); i++) {
            String name = (String) invoke(a, "getPaletteName", new Class[]{java.util.Map.class, int.class}, palettes.get(i), i);
            if (name.toLowerCase(Locale.ROOT).contains(lower)) { result.add(i); continue; }
            for (var block : blocks) {
                if (block == null || block.get("palette") == null) continue;
                double palette;
                try { palette = Double.parseDouble(block.get("palette").toString()); }
                catch (NumberFormatException e) { continue; }
                if (palette == i + 9 && (String.valueOf(block.get("name")).toLowerCase(Locale.ROOT).contains(lower)
                        || String.valueOf(block.get("spec")).toLowerCase(Locale.ROOT).contains(lower))) {
                    result.add(i); break;
                }
            }
        }
        return result;
    }
}
