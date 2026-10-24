package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.io.StringReader;
import java.security.MessageDigest;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.util.HashMap;
import pro.sketchware.R;
import pro.sketchware.activities.editor.block.BlocksManager;
import pro.sketchware.activities.editor.block.BlocksManagerCreatorActivity;

@RunWith(AndroidJUnit4.class)
public class BlockCreatorAsyncTest {
    static final String BLOCKS = "[{\"name\":\"BlockA\",\"spec\":\"Consume %s.value\",\"type\":\" \",\"typeName\":\"\",\"code\":\"OLD_A\",\"palette\":\"9\",\"unknown\":\"keep\"},{\"name\":\"BlockB\",\"spec\":\"Other\",\"type\":\" \",\"code\":\"OLD_B\",\"palette\":\"9\"}]";

    @Test public void blockedLoadingReturnsToMainAndPublishesOnlyAfterLoad() throws Exception {
        try (Blocker blocked = new Blocker(BlocksManagerCreatorActivity.class, "LOADER"); Fixture f = new Fixture(BLOCKS, "edit")) {
            f.run(a -> { assertFalse((boolean) field(a, "loaded")); assertFalse(f.view(a, "save").isEnabled()); });
            f.run(a -> assertFalse(a.isFinishing()));
            blocked.release(); f.awaitLoad();
            f.run(a -> { assertEquals("BlockA", f.text(a, "name")); assertEquals("OLD_A", f.text(a, "code")); });
        }
    }

    @Test public void recreationBeforeFirstLoadStillPopulatesOriginalFields() throws Exception {
        try (Blocker blocked = new Blocker(BlocksManagerCreatorActivity.class, "LOADER"); Fixture f = new Fixture(BLOCKS, "edit")) {
            f.scenario.recreate(); blocked.release(); f.awaitLoad();
            f.run(a -> { assertEquals("BlockA", f.text(a, "name")); assertEquals("OLD_A", f.text(a, "code")); });
        }
    }

    @Test public void firstLoadTracksSelectedRecordWhenLibraryMovesBeforeRead() throws Exception {
        try (Blocker blocked = new Blocker(BlocksManagerCreatorActivity.class, "LOADER"); Fixture f = new Fixture(BLOCKS, "edit")) {
            var array = com.google.gson.JsonParser.parseString(BLOCKS).getAsJsonArray();
            var moved = new com.google.gson.JsonArray(); moved.add(array.get(1)); moved.add(array.get(0)); f.write(moved.toString());
            blocked.release(); f.awaitLoad();
            f.run(a -> { assertEquals("BlockA", f.text(a, "name")); f.set(a, "code", "USER_A"); f.save(a); });
            PaletteOrderSaveTest.awaitSave();
            var result = com.google.gson.JsonParser.parseString(f.read()).getAsJsonArray();
            assertEquals("OLD_B", result.get(0).getAsJsonObject().get("code").getAsString());
            assertEquals("USER_A", result.get(1).getAsJsonObject().get("code").getAsString());
        }
    }

    @Test public void addingTracksSelectedPaletteWhenItMovesBeforeLoad() throws Exception {
        try (Blocker blocked = new Blocker(BlocksManagerCreatorActivity.class, "LOADER"); Fixture f = new Fixture(BLOCKS, "add")) {
            Files.write(f.palettes.toPath(), ("[{\"name\":\"Other\",\"color\":\"#E91E63\"}," + Fixture.PALETTE + "]").getBytes(StandardCharsets.UTF_8));
            f.write(BLOCKS.replace("\"9\"", "\"10\"")); blocked.release(); f.awaitLoad();
            f.run(a -> { f.set(a, "name", "NewBlock"); f.set(a, "spec", "New"); f.set(a, "code", "NEW"); f.save(a); });
            PaletteOrderSaveTest.awaitSave();
            var rows = com.google.gson.JsonParser.parseString(f.read()).getAsJsonArray();
            assertEquals("10", rows.get(2).getAsJsonObject().get("palette").getAsString());
        }
    }

    @Test public void paletteChangesWhileEditingCannotRedirectNewBlock() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "add")) {
            f.awaitLoad(); Files.write(f.palettes.toPath(), ("[" + Fixture.PALETTE.replace("Palette", "Different") + "]").getBytes(StandardCharsets.UTF_8));
            f.run(a -> { f.set(a, "name", "NewBlock"); f.set(a, "spec", "New"); f.set(a, "code", "NEW"); f.save(a); });
            waitFor(() -> pending() != null && !(boolean) field(pending(), "running"));
            assertEquals(BLOCKS, f.read()); clickPositive(); waitFor(() -> pending() == null);
        }
    }

    @Test public void saveWaitsForWriterAndSurvivesRecreationWithoutNavigatingEarly() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad();
            Object[] receipt = {null};
            try (Blocker blocked = new Blocker(BlocksManager.class, "ORDER_WRITER")) {
                f.run(a -> { receipt[0] = field(a, "saveState"); f.set(a, "code", "USER_EDIT"); f.save(a); a.getOnBackPressedDispatcher().onBackPressed(); assertFalse(a.isFinishing()); });
                assertEquals(BLOCKS, f.read());
                f.scenario.recreate();
                f.run(a -> { assertFalse(a.isFinishing()); assertSame(receipt[0], field(a, "saveState")); });
                blocked.release(); PaletteOrderSaveTest.awaitSave();
            }
            long deadline = android.os.SystemClock.uptimeMillis() + 30000;
            while (f.scenario.getState() != Lifecycle.State.DESTROYED && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(10);
            assertEquals(Lifecycle.State.DESTROYED, f.scenario.getState());
            assertTrue((boolean) field(receipt[0], "saved"));
            assertTrue(f.read().contains("USER_EDIT")); assertTrue(f.read().contains("keep")); assertTrue(f.read().contains("OLD_B"));
        }
    }

    @Test public void failedAtomicWriteKeepsSourceAndRetrySavesCandidate() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); var original = Files.getPosixFilePermissions(f.directory.toPath());
            try {
                Files.setPosixFilePermissions(f.directory.toPath(), Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
                f.run(a -> { f.set(a, "code", "RETRY_EDIT"); f.save(a); });
                waitFor(() -> pending() != null && !(boolean) field(pending(), "running"));
                assertEquals(BLOCKS, f.read()); f.run(a -> assertFalse(a.isFinishing()));
            } finally { Files.setPosixFilePermissions(f.directory.toPath(), original); }
            clickPositive(); PaletteOrderSaveTest.awaitSave();
            assertTrue(f.read().contains("RETRY_EDIT"));
        }
    }

    @Test public void saveRebasesOtherRecordsInsteadOfOverwritingTheirNewContent() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); f.write(BLOCKS.replace("OLD_B", "EXTERNAL_B"));
            f.run(a -> { f.set(a, "code", "USER_EDIT"); f.save(a); }); PaletteOrderSaveTest.awaitSave();
            assertTrue(f.read().contains("USER_EDIT")); assertTrue(f.read().contains("EXTERNAL_B"));
        }
    }

    @Test public void changedTargetShowsConflictAndKeepsInputsAndExternalSource() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); String external = BLOCKS.replace("OLD_A", "EXTERNAL_A"); f.write(external);
            f.run(a -> { f.set(a, "code", "USER_EDIT"); f.save(a); });
            waitFor(() -> pending() != null && !(boolean) field(pending(), "running"));
            assertEquals(external, f.read());
            clickPositive(); waitFor(() -> pending() == null);
            f.run(a -> { assertEquals("USER_EDIT", f.text(a, "code")); assertFalse(f.view(a, "save").isEnabled()); assertFalse(a.isFinishing()); });
        }
    }

    @Test public void recreatedLoadKeepsRestoredFormText() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); f.run(a -> f.set(a, "code", "UNSAVED_INPUT"));
            try (Blocker blocked = new Blocker(BlocksManagerCreatorActivity.class, "LOADER")) {
                f.scenario.recreate(); blocked.release(); f.awaitLoad();
            }
            f.run(a -> assertEquals("UNSAVED_INPUT", f.text(a, "code"))); assertEquals(BLOCKS, f.read());
        }
    }

    @Test public void recreatedEditorTracksMovedRecordInsteadOfOldPosition() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); f.run(a -> f.set(a, "code", "USER_A"));
            var array = com.google.gson.JsonParser.parseString(BLOCKS).getAsJsonArray();
            var moved = new com.google.gson.JsonArray(); moved.add(array.get(1)); moved.add(array.get(0)); f.write(moved.toString());
            f.scenario.recreate(); f.awaitLoad();
            f.run(a -> { assertEquals("USER_A", f.text(a, "code")); f.save(a); }); PaletteOrderSaveTest.awaitSave();
            var result = com.google.gson.JsonParser.parseString(f.read()).getAsJsonArray();
            assertEquals("OLD_B", result.get(0).getAsJsonObject().get("code").getAsString());
            assertEquals("USER_A", result.get(1).getAsJsonObject().get("code").getAsString());
        }
    }

    @Test public void targetChangedDuringRecreationCannotReceiveRestoredInputs() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); f.run(a -> f.set(a, "code", "USER_A"));
            String external = BLOCKS.replace("OLD_A", "EXTERNAL_A"); f.write(external);
            f.scenario.recreate(); f.awaitLoad();
            f.run(a -> { assertEquals("USER_A", f.text(a, "code")); assertFalse(f.view(a, "save").isEnabled()); });
            assertEquals(external, f.read()); clickPositive();
        }
    }

    @Test public void legacyRestoredFormWithoutIdentityCannotOverwriteCurrentRecord() throws Exception {
        try (Fixture f = new Fixture(BLOCKS, "edit")) {
            f.awaitLoad(); f.run(a -> {
                f.set(a, "code", "LEGACY_INPUT");
                a.getIntent().removeExtra("block_editor_anchor");
                try { Field anchor = a.getClass().getDeclaredField("anchorDigest"); anchor.setAccessible(true); anchor.set(a, null); }
                catch(Exception e){throw new AssertionError(e);}
            });
            f.scenario.recreate(); f.awaitLoad();
            f.run(a -> { assertEquals("LEGACY_INPUT", f.text(a, "code")); assertFalse(f.view(a, "save").isEnabled()); });
            assertEquals(BLOCKS, f.read()); clickPositive();
        }
    }

    @Test public void addingFirstBlockCreatesOnlyTheCheckedNewLibrary() throws Exception {
        try (Fixture f = new Fixture(null, "add")) {
            f.awaitLoad(); assertFalse(f.file.exists());
            f.run(a -> { f.set(a, "name", "First"); f.set(a, "spec", "First"); f.set(a, "code", "FIRST"); f.save(a); });
            PaletteOrderSaveTest.awaitSave(); assertTrue(f.read().contains("FIRST"));
        }
    }

    @Test public void addAndInsertUseSameCheckedBackgroundWriter() throws Exception {
        for (String mode : List.of("add", "insert")) try (Fixture f = new Fixture(BLOCKS, mode)) {
            f.awaitLoad(); f.run(a -> { f.set(a, "name", "NewBlock"); f.set(a, "spec", "New"); f.set(a, "code", "NEW_CODE"); f.save(a); });
            PaletteOrderSaveTest.awaitSave(); String result = f.read(); assertTrue(result.contains("NewBlock"));
            assertTrue(result.contains("OLD_A")); assertTrue(result.contains("OLD_B"));
            if (mode.equals("insert")) assertTrue(result.indexOf("NewBlock") < result.indexOf("BlockA"));
        }
    }

    static Object field(Object owner, String name) {
        try { Field f = (owner instanceof Class<?> type ? type : owner.getClass()).getDeclaredField(name); f.setAccessible(true); return f.get(owner instanceof Class<?> ? null : owner); }
        catch (NoSuchFieldException old) { if (name.equals("loaded")) return true; throw new AssertionError(old); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    static Object pending() { return field(BlocksManager.class, "pendingOrderSave"); }
    static void waitFor(BooleanSupplier condition) {
        long end = android.os.SystemClock.uptimeMillis() + 30000;
        while (android.os.SystemClock.uptimeMillis() < end) {
            boolean[] done = {false}; InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> done[0] = condition.getAsBoolean());
            if (done[0]) return; android.os.SystemClock.sleep(10);
        }
        fail("Timed out waiting for editor task");
    }
    private static void clickPositive() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            List<View> roots = WindowInspector.getGlobalWindowViews();
            for (int i = roots.size()-1; i>=0; i--) { View button = roots.get(i).findViewById(android.R.id.button1); if (button != null && button.isShown()) { button.performClick(); return; } }
            fail("No confirmation dialog");
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    static final class Blocker implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        Blocker(Class<?> owner, String name) throws Exception {
            ((ExecutorService) field(owner, name)).execute(() -> { entered.countDown(); try { released.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
        }
        void release() { released.countDown(); }
        @Override public void close() { release(); }
    }
    static final class Fixture implements AutoCloseable {
        static final String PALETTE = "{\"name\":\"Palette\",\"color\":\"#1976D2\"}";
        final File directory, file, palettes;
        final ActivityScenario<BlocksManagerCreatorActivity> scenario;
        Fixture(String source, String mode) throws Exception {
            var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            directory = new File(context.getCacheDir(), "BlockCreator."+UUID.randomUUID()); assertTrue(directory.mkdir());
            file = new File(directory, "blocks.json"); if (source != null) write(source);
            palettes = new File(directory, "palettes.json"); Files.write(palettes.toPath(), ("[" + PALETTE + "]").getBytes(StandardCharsets.UTF_8));
            Intent intent = new Intent(context, BlocksManagerCreatorActivity.class).putExtra("mode", mode).putExtra("path", file.getAbsolutePath())
                    .putExtra("color", "#1976D2").putExtra("pos", "0").putExtra("pallet", "9");
            intent.putExtra("block_editor_palette_path", palettes.getAbsolutePath());
            intent.putExtra("block_editor_palette_anchor", identity(PALETTE));
            if (!mode.equals("add") && source != null) {
                JsonReader reader = new JsonReader(new StringReader(source)); reader.beginArray();
                HashMap<String,Object> anchor = pro.sketchware.util.GsonUtils.getGson().fromJson(reader, new TypeToken<HashMap<String,Object>>(){}.getType()); reader.close();
                intent.putExtra("block_editor_anchor", identity(pro.sketchware.util.GsonUtils.getGson().toJson(anchor)));
                if (anchor.get("name") instanceof String name) intent.putExtra("block_editor_anchor_name", name);
            }
            scenario = ActivityScenario.launch(intent); run(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
        View view(BlocksManagerCreatorActivity a, String fieldName) { return (View) BlockCreatorAsyncTest.field(BlockCreatorAsyncTest.field(a, "binding"), fieldName); }
        void set(BlocksManagerCreatorActivity a, String name, String value) { ((EditText)view(a,name)).setText(value); }
        String text(BlocksManagerCreatorActivity a, String name) { return ((EditText)view(a,name)).getText().toString(); }
        void save(BlocksManagerCreatorActivity a) { assertTrue(view(a,"save").isEnabled()); assertTrue(view(a,"save").performClick()); }
        void run(Consumer<BlocksManagerCreatorActivity> action) { scenario.onActivity(action::accept); }
        void awaitLoad() {
            long deadline=android.os.SystemClock.uptimeMillis()+30000;
            while(android.os.SystemClock.uptimeMillis()<deadline) {
                boolean[] value={false};run(a->value[0]=(boolean)field(a,"loaded"));
                if(value[0])return;android.os.SystemClock.sleep(10);
            }
            fail("Editor load timed out");
        }
        void write(String content) throws Exception { Files.write(file.toPath(),content.getBytes(StandardCharsets.UTF_8)); }
        String read() throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
        static byte[] identity(String json) throws Exception {
            HashMap<String,Object> map = pro.sketchware.util.GsonUtils.getGson().fromJson(json, new TypeToken<HashMap<String,Object>>(){}.getType());
            return MessageDigest.getInstance("SHA-256").digest(pro.sketchware.util.GsonUtils.getGson().toJson(map).getBytes(StandardCharsets.UTF_8));
        }
        @Override public void close() throws Exception {
            // Failed candidates belong to this fixture only; do not leave a global test guard behind.
            var save=pending();
            if(save!=null) {
                waitFor(() -> !(boolean)field(save,"running"));
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> { try {
                    Method m=BlocksManager.class.getDeclaredMethod("abandonEditorSave",save.getClass());m.setAccessible(true);m.invoke(null,save);
                } catch(Exception e){throw new AssertionError(e);} });
            }
            scenario.close(); assertEquals(directory.getCanonicalFile(),file.getCanonicalFile().getParentFile());
            Files.deleteIfExists(file.toPath()); Files.deleteIfExists(palettes.toPath()); Files.delete(directory.toPath());
        }
    }
}
