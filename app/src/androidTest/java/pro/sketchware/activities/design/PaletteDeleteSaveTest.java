package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import static pro.sketchware.activities.design.PaletteCountIndexTest.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import pro.sketchware.util.GsonUtils;

@RunWith(AndroidJUnit4.class)
public class PaletteDeleteSaveTest {
    @Test public void deletionKeepsLegacyPaletteNumbersAndSavesOneFinalModel() throws Exception {
        String blocks = "[null,{}, {\"palette\":null},{\"palette\":\"9\"},{\"palette\":9},"
                + "{\"palette\":\"10\"},{\"palette\":\"10.0\"},{\"palette\":10},"
                + "{\"palette\":\"11\"},{\"palette\":\"11.5\"},{\"palette\":\"-1\"},{\"palette\":\"bad\"}]";
        try (var f = new Fixture(blocks, 3)) {
            f.run(a -> {
                invoke(a, "movePaletteToRecycleBin", new Class[]{int.class}, 1);
                counts(a, 1, 2, 0, 4);
                assertRow(a, 0, 1); assertRow(a, 1, 2);
                @SuppressWarnings("unchecked") var palettes = (ArrayList<HashMap<String, Object>>) field(a, "pallet_listmap");
                assertEquals(2, palettes.size()); assertEquals("P2", palettes.get(1).get("name"));
            });
            PaletteOrderSaveTest.awaitSave();
            f.run(a -> {
                assertEquals(JsonParser.parseString(GsonUtils.getGson().toJson(field(a, "all_blocks_list"))),
                        JsonParser.parseString(read(f.files.blocks)));
                assertEquals(JsonParser.parseString(GsonUtils.getGson().toJson(field(a, "pallet_listmap"))),
                        JsonParser.parseString(read(f.files.palettes)));
            });
        }
    }

    @Test public void blockedDeleteSaveRetainsOriginalFilesAndRecreationWaits() throws Exception {
        try (var f = new Fixture(BLOCKS, 3)) {
            CountDownLatch release = new CountDownLatch(1);
            byte[] blocks = Files.readAllBytes(f.files.blocks.toPath());
            byte[] palettes = Files.readAllBytes(f.files.palettes.toPath());
            try {
                f.run(a -> {
                    ((ExecutorService) field(a, "ORDER_WRITER")).execute(() -> {
                        try { assertTrue(release.await(15, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    });
                    invoke(a, "movePaletteToRecycleBin", new Class[]{int.class}, 1);
                    assertNotNull(field(a, "pendingOrderSave"));
                    counts(a, 2, 1, 0, 2);
                });
                assertArrayEquals(blocks, Files.readAllBytes(f.files.blocks.toPath()));
                assertArrayEquals(palettes, Files.readAllBytes(f.files.palettes.toPath()));
                f.manager.recreate();
                f.run(a -> {
                    assertTrue(((ArrayList<?>) field(a, "all_blocks_list")).isEmpty());
                    assertTrue(((android.app.Dialog) field(a, "orderSaveDialog")).isShowing());
                });
            } finally { release.countDown(); }
            PaletteOrderSaveTest.awaitSave();
            f.run(a -> {
                counts(a, 2, 1, 0, 2);
                assertEquals(2, ((ArrayList<?>) field(a, "pallet_listmap")).size());
                assertNull(field(a, "pendingOrderSave"));
            });
        }
    }

    @Test public void failedDeleteSaveKeepsOriginalFilesAndCanRetry() throws Exception {
        try (var f = new Fixture(BLOCKS, 3)) {
            var parent = new java.io.File(f.files.scope.backups, "delete-parent");
            Files.write(parent.toPath(), "blocked".getBytes(StandardCharsets.UTF_8));
            var target = new java.io.File(parent, "palettes.json");
            byte[] original = Files.readAllBytes(f.files.blocks.toPath());
            f.files.configure(f.files.blocks, target);
            f.run(a -> {
                setField(a, "pallet_dir", target.getAbsolutePath());
                invoke(a, "movePaletteToRecycleBin", new Class[]{int.class}, 1);
            });
            boolean[] failed = {false};
            long deadline = android.os.SystemClock.uptimeMillis() + 10000;
            do {
                f.run(a -> {
                    Object save = field(a, "pendingOrderSave");
                    assertNotNull(save);
                    failed[0] = field(save, "failure") != null && !(boolean) field(save, "running");
                });
                if (!failed[0]) android.os.SystemClock.sleep(10);
            } while (!failed[0] && android.os.SystemClock.uptimeMillis() < deadline);
            assertTrue("The failed snapshot must be retained", failed[0]);
            assertArrayEquals(original, Files.readAllBytes(f.files.blocks.toPath()));
            Files.delete(parent.toPath()); assertTrue(parent.mkdir());
            f.run(a -> {
                counts(a, 2, 1, 0, 2);
                assertTrue(((androidx.appcompat.app.AlertDialog) field(a, "orderSaveErrorDialog"))
                        .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
            });
            PaletteOrderSaveTest.awaitSave();
            f.run(a -> {
                assertNull(field(a, "pendingOrderSave"));
                counts(a, 2, 1, 0, 2);
                assertEquals(JsonParser.parseString(GsonUtils.getGson().toJson(field(a, "pallet_listmap"))),
                        JsonParser.parseString(read(target)));
            });
        }
    }

    private static String read(java.io.File file) {
        try { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new AssertionError(e); }
    }
}
