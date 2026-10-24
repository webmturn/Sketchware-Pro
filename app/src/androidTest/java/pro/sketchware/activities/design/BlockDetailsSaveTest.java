package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import static pro.sketchware.activities.design.PaletteCountIndexTest.*;

import android.content.Intent;
import android.os.FileObserver;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.ListView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManagerDetailsActivity;
import pro.sketchware.activities.editor.block.BlocksManager;
import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.util.GsonUtils;

@RunWith(AndroidJUnit4.class)
public class BlockDetailsSaveTest {
    static final String ROWS = "[null,{\"name\":\"A\",\"spec\":\"a\",\"type\":\" \",\"palette\":\"9\"},"
            + "{\"name\":\"Other\",\"spec\":\"other\",\"type\":\" \",\"palette\":\"10\"},"
            + "{\"name\":\"B\",\"spec\":\"b\",\"type\":\" \",\"palette\":\"9\"},"
            + "{\"name\":\"Recycled\",\"spec\":\"r\",\"type\":\" \",\"palette\":\"-1\"}]";

    @Test public void repeatedEditsKeepGlobalReferencesAndSavedModelsConsistent() throws Exception {
        try (var f = new Fixture(ROWS)) {
            f.edit("_swapitems", new Class[]{int.class, int.class}, 1, 3);
            f.run(a -> {
                assertEquals("B", maps(a).get(1).get("name"));
                assertEquals(java.util.List.of(1, 3), field(a, "reference_list"));
            });
            f.edit("_duplicateBlock", new Class[]{int.class}, 1);
            f.run(a -> {
                assertEquals(java.util.List.of(1, 2, 4), field(a, "reference_list"));
                assertTrue(maps(a).get(2).get("name").toString().startsWith("B_copy"));
            });
            f.edit("_deleteBlock", new Class[]{int.class}, 2);
            f.edit("_moveToRecycleBin", new Class[]{int.class}, 1);
            f.run(a -> assertEquals(java.util.List.of(3), field(a, "reference_list")));
            f.edit("moveBlockToPalette", new Class[]{int.class, int.class}, 4, 9);
            f.run(a -> {
                assertEquals(java.util.List.of(3, 4), field(a, "reference_list"));
                assertNull(maps(a).get(0));
                assertEquals("10", maps(a).get(2).get("palette"));
                f.assertSaved(a);
            });
        }
    }

    @Test public void savingDoesNotRereadTheBlockFile() throws Exception {
        try (var f = new Fixture(ROWS)) {
            AtomicInteger reads = new AtomicInteger();
            // ACCESS distinguishes reading data from canonical-path metadata opens.
            FileObserver observer = new FileObserver(f.directory.getAbsolutePath(), FileObserver.ACCESS) {
                @Override public void onEvent(int event, String path) {
                    if ("blocks.json".equals(path)) reads.incrementAndGet();
                }
            };
            observer.startWatching();
            try {
                Files.readAllBytes(f.blocks.toPath());
                long deadline = SystemClock.uptimeMillis() + 5000;
                while (reads.get() == 0 && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10);
                assertTrue("The fixture observer must be working", reads.get() > 0);
                SystemClock.sleep(100);
                reads.set(0);
                f.edit("_swapitems", new Class[]{int.class, int.class}, 1, 3);
                SystemClock.sleep(150);
                assertEquals("A saved model must not be reread", 0, reads.get());
            } finally { observer.stopWatching(); }
            f.run(f::assertSaved);
        }
    }

    @Test public void failedWriteKeepsSourceModelsAndViewThenAllowsRetry() throws Exception {
        try (var f = new Fixture(ROWS)) {
            byte[] original = Files.readAllBytes(f.blocks.toPath());
            assertTrue(f.directory.setWritable(false, false));
            try {
                f.run(a -> {
                    Object model = field(a, "all_blocks_list");
                    invoke(a, "_moveToRecycleBin", new Class[]{int.class}, 1);
                    assertSame(model, field(a, "all_blocks_list"));
                    assertEquals("9", maps(a).get(1).get("palette"));
                    assertEquals(java.util.List.of(1, 3), field(a, "reference_list"));
                });
                long deadline = SystemClock.uptimeMillis() + 10000;
                while (pending() != null && field(pending(), "failure") == null && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10);
                assertNotNull(pending()); assertNotNull(field(pending(), "failure"));
                assertArrayEquals(original, Files.readAllBytes(f.blocks.toPath()));
            } finally { assertTrue(f.directory.setWritable(true, true)); }
            f.run(a -> {
                assertTrue(((androidx.appcompat.app.AlertDialog) field(a, "saveErrorDialog"))
                        .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick());
            });
            PaletteOrderSaveTest.awaitSave();
            f.run(a -> {
                assertEquals("-1", maps(a).get(1).get("palette"));
                f.assertSaved(a);
            });
            try (var paths = Files.list(f.directory.toPath())) {
                assertFalse(paths.anyMatch(p -> p.getFileName().toString().startsWith(".sketchware-")));
            }
        }
    }

    @Test public void resumeReloadsExternalChangesAndSavingInvalidatesEqualStampCache() throws Exception {
        try (var f = new Fixture(ROWS)) {
            assertEquals("A", ExtraBlockFile.getExtraBlockData("9").get(0).get("name"));
            long stamp = f.blocks.lastModified(), size = f.blocks.length();
            f.edit("_swapitems", new Class[]{int.class, int.class}, 1, 3);
            assertEquals(size, f.blocks.length()); assertTrue(f.blocks.setLastModified(stamp));
            assertEquals("B", ExtraBlockFile.getExtraBlockData("9").get(0).get("name"));
            f.manager.moveToState(Lifecycle.State.CREATED);
            f.write("[{\"name\":\"External\",\"spec\":\"external\",\"type\":\" \",\"palette\":\"9\"}]");
            f.manager.moveToState(Lifecycle.State.RESUMED);
            f.run(a -> {
                assertEquals(1, maps(a).size());
                assertEquals("External", maps(a).get(0).get("name"));
                assertEquals(1, ((ListView) a.findViewById(R.id.block_list)).getAdapter().getCount());
            });
        }
    }

    @Test public void importChoicesSaveWithoutChangingTheImportSource() throws Exception {
        try (var f = new Fixture(ROWS)) {
            for (int button : new int[]{android.R.id.button1, android.R.id.button2, android.R.id.button3}) {
                f.write(ROWS);
                @SuppressWarnings("unchecked") ArrayList<HashMap<String, Object>> imported = GsonUtils.getGson().fromJson(
                        "[{\"name\":\"Import1\",\"spec\":\"one\",\"type\":\" \",\"palette\":\"12\"},"
                                + "{\"name\":\"Import2\",\"spec\":\"two\",\"type\":\" \",\"palette\":\"13\"}]",
                        pro.sketchware.util.Helper.TYPE_MAP_LIST);
                String original = GsonUtils.getGson().toJson(imported);
                f.run(a -> {
                    invoke(a, "_refreshLists", new Class[0]);
                    invoke(a, "_importBlocks", new Class[]{ArrayList.class}, imported);
                });
                f.run(a -> {
                    View root = dialog(); assertNotNull(root);
                    ListView list = findList(root); assertNotNull(list);
                    list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
                    assertTrue(list.isItemChecked(0));
                    assertTrue(root.findViewById(button).performClick());
                });
                PaletteOrderSaveTest.awaitSave();
                f.run(a -> {
                    assertEquals(button == android.R.id.button3 ? 7 : 6, maps(a).size());
                    f.assertSaved(a);
                });
                assertEquals(original, GsonUtils.getGson().toJson(imported));
            }
        }
    }

    @Test public void blockedSaveGuardsBothPagesAndSurvivesRecreation() throws Exception {
        try (var f = new Fixture(ROWS)) {
            var release = new java.util.concurrent.CountDownLatch(1);
            byte[] original = Files.readAllBytes(f.blocks.toPath());
            java.lang.reflect.Field writerField = BlocksManager.class.getDeclaredField("ORDER_WRITER"); writerField.setAccessible(true);
            ((java.util.concurrent.ExecutorService) writerField.get(null)).execute(() -> {
                try { assertTrue(release.await(15, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            ActivityScenario<BlocksManager> main = null;
            try {
              try {
                f.run(a -> invoke(a, "_swapitems", new Class[]{int.class, int.class}, 1, 3));
                f.run(a -> {
                    Object first = pending();
                    invoke(a, "_deleteBlock", new Class[]{int.class}, 2);
                    assertSame("A queued callback must not start a second save", first, pending());
                    assertEquals(5, maps(a).size());
                });
                assertArrayEquals(original, Files.readAllBytes(f.blocks.toPath()));
                InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                f.run(a -> assertFalse(a.isFinishing()));
                SystemClock.sleep(150);
                f.manager.recreate();
                f.run(a -> {
                    assertTrue(maps(a).isEmpty());
                    assertTrue(((android.app.Dialog) field(a, "saveDialog")).isShowing());
                    assertNotNull(((android.app.Dialog) field(a, "saveDialog")).findViewById(R.id.anim_sketchware));
                });
                Intent mainIntent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), BlocksManager.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                main = ActivityScenario.launch(mainIntent);
                assertNotEquals("Opening a second task must not clear the details task", Lifecycle.State.DESTROYED, f.manager.getState());
                main.onActivity(a -> {
                    assertTrue(((ArrayList<?>) field(a, "all_blocks_list")).isEmpty());
                    assertTrue(((android.app.Dialog) field(a, "orderSaveDialog")).isShowing());
                });
              } finally { release.countDown(); }
                PaletteOrderSaveTest.awaitSave();
                main.onActivity(a -> assertEquals("B", ((HashMap<?, ?>) ((ArrayList<?>) field(a, "all_blocks_list")).get(1)).get("name")));
            } finally { release.countDown(); if (main != null) main.close(); }
            f.run(a -> {
                assertEquals("B", maps(a).get(1).get("name")); f.assertSaved(a);
                var progress = (android.app.Dialog) field(a, "saveDialog");
                assertFalse(progress.isShowing());
                assertFalse(((com.airbnb.lottie.LottieAnimationView) progress.findViewById(R.id.anim_sketchware)).isAnimating());
            });
        }
    }

    @Test public void savedSnapshotOmitsNullFieldsWithoutChangingTheOldRecord() throws Exception {
        try (var f = new Fixture(ROWS)) {
            String nullable = ROWS.replace("\"name\":\"A\"", "\"name\":\"A\",\"color\":null,\"imports\":null");
            Files.write(f.blocks.toPath(), nullable.getBytes(StandardCharsets.UTF_8));
            AtomicReference<HashMap<String, Object>> original = new AtomicReference<>();
            f.run(a -> {
                invoke(a, "_refreshLists", new Class[0]); original.set(maps(a).get(1));
                assertTrue(original.get().containsKey("color"));
            });
            f.edit("_swapitems", new Class[]{int.class, int.class}, 1, 3);
            f.run(a -> {
                assertFalse(maps(a).get(3).containsKey("color"));
                assertFalse(maps(a).get(3).containsKey("imports"));
                assertTrue(original.get().containsKey("color"));
                f.assertSaved(a);
            });
        }
    }

    private static Object pending() {
        try { var f = BlocksManager.class.getDeclaredField("pendingOrderSave"); f.setAccessible(true); return f.get(null); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @SuppressWarnings("unchecked") static ArrayList<HashMap<String, Object>> maps(BlocksManagerDetailsActivity a) {
        return (ArrayList<HashMap<String, Object>>) field(a, "all_blocks_list");
    }
    private static View dialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) if (root.isShown() && root.findViewById(android.R.id.button1) != null) return root;
        return null;
    }
    private static ListView findList(View view) {
        if (view instanceof ListView list) return list;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            ListView list = findList(group.getChildAt(i)); if (list != null) return list;
        }
        return null;
    }
    static final class Fixture implements AutoCloseable {
        final CustomBlockPaletteConfigurationTest.Fixture files;
        final File directory, blocks, palettes;
        final ActivityScenario<BlocksManagerDetailsActivity> manager;
        Fixture(String rows) throws Exception {
            files = new CustomBlockPaletteConfigurationTest.Fixture();
            directory = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir().toPath(), "block-details-").toFile();
            blocks = new File(directory, "blocks.json"); palettes = new File(directory, "palettes.json");
            write(rows);
            Files.write(palettes.toPath(), "[{\"name\":\"P0\",\"color\":\"#1976D2\"},{\"name\":\"P1\",\"color\":\"#E91E63\"}]".getBytes(StandardCharsets.UTF_8));
            files.configure(new File("/mnt/.." + blocks.getAbsolutePath()), new File("/mnt/.." + palettes.getAbsolutePath()));
            ExtraBlockFile.invalidateCache();
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), BlocksManagerDetailsActivity.class);
            intent.putExtra("position", "9"); intent.putExtra("dirB", blocks.getAbsolutePath()); intent.putExtra("dirP", palettes.getAbsolutePath());
            manager = ActivityScenario.launch(intent);
            run(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        void write(String rows) throws Exception {
            Files.write(blocks.toPath(), GsonUtils.getGson().toJson(JsonParser.parseString(rows)).getBytes(StandardCharsets.UTF_8));
        }
        void run(Consumer<BlocksManagerDetailsActivity> action) {
            AtomicReference<Throwable> error = new AtomicReference<>();
            try { manager.onActivity(a -> { try { action.accept(a); } catch (Throwable t) { error.set(t); } }); }
            catch (RuntimeException e) { throw new AssertionError("Details activity state: " + manager.getState(), e); }
            if (error.get() != null) throw new AssertionError(error.get());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
        void edit(String name, Class<?>[] types, Object... args) {
            run(a -> invoke(a, name, types, args));
            PaletteOrderSaveTest.awaitSave();
        }
        void assertSaved(BlocksManagerDetailsActivity a) {
            try { assertEquals(JsonParser.parseString(GsonUtils.getGson().toJson(maps(a))), JsonParser.parseString(new String(Files.readAllBytes(blocks.toPath()), StandardCharsets.UTF_8))); }
            catch (Exception e) { throw new AssertionError(e); }
        }
        @Override public void close() throws Exception {
            try { manager.close(); PaletteOrderSaveTest.awaitSave(); }
            finally { Files.deleteIfExists(blocks.toPath()); Files.deleteIfExists(palettes.toPath()); Files.delete(directory.toPath()); files.close(); }
        }
    }
}
