package pro.sketchware.activities.design;

import static org.junit.Assert.*;
import static pro.sketchware.activities.design.PaletteCountIndexTest.*;

import android.os.FileObserver;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.activities.editor.block.BlocksManager;

@RunWith(AndroidJUnit4.class)
public class PaletteLoadingTest {
    @Test public void firstEntryReadsEachFileOnlyOnce() throws Exception {
        try (var files = new CustomBlockPaletteConfigurationTest.Fixture()) {
            // Internal storage supplies reliable inotify events; emulated external
            // storage can suppress them on devices that route reads through FUSE.
            File directory = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getCacheDir().toPath(), "palette-loading-").toFile();
            File blocks = new File(directory, "blocks.json");
            File palettes = new File(directory, "palettes.json");
            Files.write(blocks.toPath(), BLOCKS.getBytes(StandardCharsets.UTF_8));
            Files.write(palettes.toPath(),
                    "[{\"name\":\"P0\",\"color\":\"#1976D2\"}]".getBytes(StandardCharsets.UTF_8));
            // The configurable storage-path resolver recognizes /mnt paths. This
            // equivalent alias lets the real loader read our private fixture files.
            files.configure(new File("/mnt/.." + blocks.getAbsolutePath()),
                    new File("/mnt/.." + palettes.getAbsolutePath()));
            try (var reads = new Reads(blocks, palettes)) {
                reads.startWatching();
                try (var manager = ActivityScenario.launch(BlocksManager.class)) {
                    manager.onActivity(a -> {
                        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        counts(a, 2, 1, 1, 1);
                        assertRow(a, 0, 2);
                    });
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    reads.awaitQuiet();
                    assertEquals("First entry must read the block file once", 1, reads.blocks.get());
                    assertEquals("First entry must read the palette file once", 1, reads.palettes.get());
                    reads.stopWatching();
                }
            } finally {
                Files.deleteIfExists(blocks.toPath());
                Files.deleteIfExists(palettes.toPath());
                Files.delete(directory.toPath());
            }
        }
    }

    @Test public void resumeReloadsEvenWhenFileSizeAndTimestampAreUnchanged() throws Exception {
        try (var f = new Fixture("[{\"palette\":\"9\"}]", 3)) {
            long stamp = f.files.blocks.lastModified();
            long length = f.files.blocks.length();
            f.manager.moveToState(Lifecycle.State.CREATED);
            f.write("[{\"palette\":\"8\"}]");
            assertEquals(length, f.files.blocks.length());
            assertTrue(f.files.blocks.setLastModified(stamp));
            f.manager.moveToState(Lifecycle.State.RESUMED);
            f.run(a -> {
                counts(a, 0, 0, 0, 0);
                assertEquals(1, count(a, 8));
                assertRow(a, 0, 0);
            });
        }
    }

    @Test public void invalidReloadKeepsModelsAndOffersRecoveryWithoutWritingSource() throws Exception {
        try (var f = new Fixture(BLOCKS, 3)) {
            for (String invalid : new String[]{"[", "{}", "[1]"}) {
                f.write(invalid);
                f.run(a -> {
                    Object original = field(a, "all_blocks_list");
                    invoke(a, "readSettings", new Class[0]);
                    assertSame(original, field(a, "all_blocks_list"));
                    counts(a, 2, 1, 1, 1);
                    View dialog = recoveryDialog();
                    assertNotNull("Invalid JSON must offer recovery", dialog);
                    assertNotNull(dialog.findViewById(android.R.id.button1));
                    assertTrue(dialog.findViewById(android.R.id.button2).performClick());
                });
                assertEquals(invalid, new String(Files.readAllBytes(f.files.blocks.toPath()), StandardCharsets.UTF_8));
            }
            f.write("[]");
            f.run(a -> {
                invoke(a, "readSettings", new Class[0]);
                counts(a, 0, 0, 0, 0);
                assertNull(recoveryDialog());
            });
        }
    }

    @Test public void emptyAndNullFilesKeepTheExistingModels() throws Exception {
        try (var f = new Fixture(BLOCKS, 3)) {
            for (String empty : new String[]{"", " \n", "null"}) {
                f.write(empty);
                f.run(a -> {
                    Object original = field(a, "all_blocks_list");
                    invoke(a, "readSettings", new Class[0]);
                    assertSame(original, field(a, "all_blocks_list"));
                    counts(a, 2, 1, 1, 1);
                    assertNull(recoveryDialog());
                });
            }
        }
    }

    private static View recoveryDialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.isShown() && root.findViewById(android.R.id.button2) != null) return root;
        }
        return null;
    }

    /** Observe both open and close events so repeated opens cannot coalesce in inotify. */
    private static final class Reads extends FileObserver implements AutoCloseable {
        final AtomicInteger blocks = new AtomicInteger();
        final AtomicInteger palettes = new AtomicInteger();
        private final String blocksName;
        private final String palettesName;
        private final AtomicInteger closed = new AtomicInteger();
        private volatile long lastEvent;

        Reads(File blocks, File palettes) {
            super(blocks.getParent(), OPEN | CLOSE_NOWRITE);
            assertEquals(blocks.getParent(), palettes.getParent());
            blocksName = blocks.getName();
            palettesName = palettes.getName();
        }

        @Override public void onEvent(int event, String path) {
            if (!blocksName.equals(path) && !palettesName.equals(path)) return;
            if ((event & OPEN) != 0) {
                (blocksName.equals(path) ? blocks : palettes).incrementAndGet();
            }
            if ((event & CLOSE_NOWRITE) != 0) closed.incrementAndGet();
            lastEvent = SystemClock.uptimeMillis();
        }

        void awaitQuiet() {
            long deadline = SystemClock.uptimeMillis() + 5000;
            do {
                if (blocks.get() > 0 && palettes.get() > 0
                        && closed.get() == blocks.get() + palettes.get()
                        && SystemClock.uptimeMillis() - lastEvent >= 100) return;
                SystemClock.sleep(10);
            } while (SystemClock.uptimeMillis() < deadline);
            fail("File read events did not settle: blocks=" + blocks.get()
                    + ", palettes=" + palettes.get() + ", closed=" + closed.get());
        }

        @Override public void close() { stopWatching(); }
    }
}
