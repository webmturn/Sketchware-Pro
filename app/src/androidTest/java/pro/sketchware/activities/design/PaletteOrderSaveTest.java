package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import static pro.sketchware.activities.design.PaletteCountIndexTest.*;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManager;

@RunWith(AndroidJUnit4.class)
public class PaletteOrderSaveTest {
    @Test public void savedOrderInvalidatesCachesEvenWhenFileStampsMatch() throws Exception {
        try (Fixture f = new Fixture("[{\"name\":\"left\",\"palette\":\"9\"},{\"name\":\"right\",\"palette\":\"10\"}]", 3)) {
            long blocksStamp = f.files.blocks.lastModified();
            long palettesStamp = f.files.palettes.lastModified();
            long blocksSize = f.files.blocks.length();
            CountDownLatch restored = new CountDownLatch(1);
            // The save preserves pretty JSON; match the baseline formatting and warm its stamp cache.
            f.run(a -> {
                pro.sketchware.util.FileUtil.writeFile(f.files.blocks.getAbsolutePath(),
                        pro.sketchware.util.GsonUtils.getGson().toJson(field(a, "all_blocks_list")));
                pro.sketchware.core.codegen.ExtraBlockFile.invalidateCache();
                assertEquals("left", pro.sketchware.core.codegen.ExtraBlockFile.getExtraBlockData("9").get(0).get("name"));
            });
            blocksStamp = f.files.blocks.lastModified();
            blocksSize = f.files.blocks.length();
            final long originalBlocksStamp = blocksStamp;
            f.run(a -> {
                RecyclerView list = a.findViewById(R.id.paletteRecycler);
                var from = list.findViewHolderForAdapterPosition(0);
                var callback = callback(a);
                assertTrue(callback.onMove(list, from, list.findViewHolderForAdapterPosition(1)));
                callback.clearView(list, from);
                ((ExecutorService) field(a, "ORDER_WRITER")).execute(() -> {
                    assertTrue(f.files.blocks.setLastModified(originalBlocksStamp));
                    assertTrue(f.files.palettes.setLastModified(palettesStamp));
                    restored.countDown();
                });
            });
            assertTrue(restored.await(10, TimeUnit.SECONDS));
            awaitSave();
            assertEquals(blocksSize, f.files.blocks.length());
            f.run(a -> assertEquals("right", pro.sketchware.core.codegen.ExtraBlockFile.getExtraBlockData("9").get(0).get("name")));
        }
    }

    @Test public void directoryTargetIsNotReportedSavedAndRetryPreservesPalettes() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, 3)) {
            File blocks = new File(f.files.scope.backups, "order-blocks.json");
            assertTrue(blocks.mkdir());
            byte[] originalPalettes = Files.readAllBytes(f.files.palettes.toPath());
            f.files.configure(blocks, f.files.palettes);
            AtomicReference<Object> save = new AtomicReference<>();
            f.run(a -> {
                setField(a, "blocks_dir", blocks.getAbsolutePath());
                RecyclerView list = a.findViewById(R.id.paletteRecycler);
                var from = list.findViewHolderForAdapterPosition(0);
                var callback = callback(a);
                assertTrue(callback.onMove(list, from, list.findViewHolderForAdapterPosition(1)));
                callback.clearView(list, from);
                save.set(field(a, "pendingOrderSave"));
            });
            await(() -> !(boolean) field(save.get(), "running"));
            assertNotNull(field(save.get(), "failure"));
            assertArrayEquals(originalPalettes, Files.readAllBytes(f.files.palettes.toPath()));
            assertTrue(blocks.isDirectory());
            Files.delete(blocks.toPath());
            f.run(a -> ((androidx.appcompat.app.AlertDialog) field(a, "orderSaveErrorDialog"))
                    .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
            awaitSave();
            assertTrue(blocks.isFile());
            f.run(a -> counts(a, 1, 2, 1, 1));
        }
    }

    @Test public void unchangedReleaseDoesNotWriteOrStartSave() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, 3)) {
            byte[] blocks = Files.readAllBytes(f.files.blocks.toPath());
            long stamp = f.files.blocks.lastModified();
            f.run(a -> {
                RecyclerView list = a.findViewById(R.id.paletteRecycler);
                var holder = list.findViewHolderForAdapterPosition(0);
                var callback = callback(a);
                assertFalse(callback.onMove(list, holder, holder));
                callback.clearView(list, holder);
                assertNull(field(a, "pendingOrderSave"));
            });
            assertEquals(stamp, f.files.blocks.lastModified());
            assertArrayEquals(blocks, Files.readAllBytes(f.files.blocks.toPath()));
        }
    }

    @Test public void blockedWriterKeepsMainResponsiveAndRecreationWaitsForSavedOrder() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, 3)) {
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Object> save = new AtomicReference<>();
            byte[] original = Files.readAllBytes(f.files.blocks.toPath());
            try {
                f.run(a -> {
                    ((ExecutorService) field(a, "ORDER_WRITER")).execute(() -> {
                        try { assertTrue(release.await(15, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    });
                    RecyclerView list = a.findViewById(R.id.paletteRecycler);
                    var from = list.findViewHolderForAdapterPosition(0);
                    var to = list.findViewHolderForAdapterPosition(1);
                    var callback = callback(a);
                    assertTrue(callback.onMove(list, from, to));
                    callback.clearView(list, from);
                    save.set(field(a, "pendingOrderSave"));
                    assertNotNull(save.get());
                    assertTrue(((android.app.Dialog) field(a, "orderSaveDialog")).isShowing());
                    assertNotNull(((android.app.Dialog) field(a, "orderSaveDialog")).findViewById(R.id.anim_sketchware));
                    assertFalse(callback.onMove(list, from, to));
                    callback.clearView(list, from);
                    assertSame(save.get(), field(a, "pendingOrderSave"));
                });
                assertArrayEquals(original, Files.readAllBytes(f.files.blocks.toPath()));
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                f.run(a -> assertFalse("Back must wait for the pending save", a.isFinishing()));
                // A UI heartbeat and a real recreation both finish while the writer is blocked.
                f.manager.recreate();
                f.run(a -> {
                    assertSame(save.get(), field(a, "pendingOrderSave"));
                    assertTrue(((ArrayList<?>) field(a, "all_blocks_list")).isEmpty());
                    assertTrue(((android.app.Dialog) field(a, "orderSaveDialog")).isShowing());
                    assertEquals(true, field(save.get(), "refreshBlockLoader"));
                });
            } finally { release.countDown(); }
            awaitSave();
            f.run(a -> {
                @SuppressWarnings("unchecked") var palettes = (ArrayList<HashMap<String, Object>>) field(a, "pallet_listmap");
                assertEquals("P1", palettes.get(0).get("name"));
                assertEquals("P0", palettes.get(1).get("name"));
                counts(a, 1, 2, 1, 1);
                var progress = (android.app.Dialog) field(a, "orderSaveDialog");
                assertFalse(progress.isShowing());
                assertFalse(((com.airbnb.lottie.LottieAnimationView) progress.findViewById(R.id.anim_sketchware)).isAnimating());
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            long deadline = android.os.SystemClock.uptimeMillis() + 10000;
            while (f.manager.getState() != androidx.lifecycle.Lifecycle.State.DESTROYED
                    && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(10);
            assertEquals("Back must work again after saving", androidx.lifecycle.Lifecycle.State.DESTROYED, f.manager.getState());
        }
    }

    @Test public void secondFileFailureKeepsSnapshotAndRetryCompletesBothFiles() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, 3)) {
            File parent = new File(f.files.scope.backups, "order-parent");
            Files.write(parent.toPath(), "blocked".getBytes(StandardCharsets.UTF_8));
            File palettes = new File(parent, "palettes.json");
            f.files.configure(f.files.blocks, palettes);
            AtomicReference<Object> save = new AtomicReference<>();
            byte[] originalBlocks = Files.readAllBytes(f.files.blocks.toPath());
            f.run(a -> {
                setField(a, "pallet_dir", palettes.getAbsolutePath());
                RecyclerView list = a.findViewById(R.id.paletteRecycler);
                var callback = callback(a);
                var from = list.findViewHolderForAdapterPosition(0);
                assertTrue(callback.onMove(list, from, list.findViewHolderForAdapterPosition(1)));
                callback.clearView(list, from);
                save.set(field(a, "pendingOrderSave"));
            });
            await(() -> !(boolean) field(save.get(), "running"));
            f.run(a -> {
                assertSame(save.get(), field(a, "pendingOrderSave"));
                assertNotNull(field(save.get(), "failure"));
                assertTrue(((androidx.appcompat.app.AlertDialog) field(a, "orderSaveErrorDialog")).isShowing());
                invoke(a, "readSettings", new Class[0]);
                assertEquals("P1", ((HashMap<?, ?>) ((ArrayList<?>) field(a, "pallet_listmap")).get(0)).get("name"));
            });
            assertArrayEquals(originalBlocks, Files.readAllBytes(f.files.blocks.toPath()));
            try (var children = Files.list(f.files.blocks.toPath().getParent())) {
                assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".sketchware-order-")));
            }
            Files.delete(parent.toPath());
            assertTrue(parent.mkdir());
            f.run(a -> ((androidx.appcompat.app.AlertDialog) field(a, "orderSaveErrorDialog"))
                    .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
            awaitSave();
            f.run(a -> {
                assertNull(field(a, "orderSaveErrorDialog"));
                assertFalse(((android.app.Dialog) field(a, "orderSaveDialog")).isShowing());
                counts(a, 1, 2, 1, 1);
                assertEquals("P1", ((HashMap<?, ?>) ((ArrayList<?>) field(a, "pallet_listmap")).get(0)).get("name"));
            });
            assertTrue(palettes.isFile());
        }
    }

    static ItemTouchHelper.Callback callback(BlocksManager a) {
        return (ItemTouchHelper.Callback) field(field(a, "itemTouchHelper"), "mCallback");
    }

    public static void awaitSave() {
        await(() -> {
            try {
                var field = BlocksManager.class.getDeclaredField("pendingOrderSave");
                field.setAccessible(true);
                return field.get(null) == null;
            } catch (NoSuchFieldException oldApk) { return true; }
            catch (Exception e) { throw new AssertionError(e); }
        });
    }

    private static void await(java.util.function.BooleanSupplier condition) {
        long deadline = android.os.SystemClock.uptimeMillis() + 30000;
        AtomicBoolean done = new AtomicBoolean();
        do {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> done.set(condition.getAsBoolean()));
            if (done.get()) return;
            android.os.SystemClock.sleep(10);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Order save did not reach expected state");
    }
}
