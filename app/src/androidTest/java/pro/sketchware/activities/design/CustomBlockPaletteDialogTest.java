package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.beans.BlockBean;

/** Real palette fields and buttons; all output goes to independently owned cache files. */
@RunWith(AndroidJUnit4.class)
public class CustomBlockPaletteDialogTest {
    private static final Instrumentation INSTRUMENTATION = InstrumentationRegistry.getInstrumentation();
    private static final Context CONTEXT = INSTRUMENTATION.getTargetContext();
    private static final Gson GSON = new Gson();

    @Test public void sixDigitColorCreatesPalette() throws Exception { createPalette("1976D2"); }
    @Test public void eightDigitColorCreatesPalette() throws Exception { createPalette("801976d2"); }

    private static void createPalette(String hex) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.input("Palette", hex);
            fixture.click(AlertDialog.BUTTON_POSITIVE);
            fixture.assertSaved(hex);
        }
    }

    @Test public void invalidInputsCanBeCorrectedBeforeSave() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.input("", "1976D2");
            fixture.click(AlertDialog.BUTTON_POSITIVE);
            fixture.assertRejected(R.id.name);
            fixture.input("Palette", "");
            fixture.click(AlertDialog.BUTTON_POSITIVE);
            fixture.assertRejected(R.id.color);
            fixture.input("Palette", "12345");
            fixture.click(AlertDialog.BUTTON_POSITIVE);
            fixture.assertRejected(R.id.color);
            fixture.input("Palette", "1976D2");
            fixture.click(AlertDialog.BUTTON_POSITIVE);
            fixture.assertSaved("1976D2");
        }
    }

    @Test public void cancelLeavesBothFilesUnchanged() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.input("Palette", "1976D2");
            fixture.click(AlertDialog.BUTTON_NEGATIVE);
            fixture.assertUnchanged();
            onMain(() -> assertNull(dialogRoot()));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final File output = new File(CONTEXT.getCacheDir(), "PaletteCreate." + UUID.randomUUID());
        final File palettes = new File(output, "palettes.json"), blocks = new File(output, "blocks.json");
        final ActivityScenario<PermissionResultTestActivity> host;

        Fixture() throws Exception {
            assertFalse(output.exists()); assertTrue(output.mkdir());
            Files.write(palettes.toPath(), "[]".getBytes(StandardCharsets.UTF_8));
            Files.write(blocks.toPath(), "[]".getBytes(StandardCharsets.UTF_8));
            host = ActivityScenario.launch(PermissionResultTestActivity.class);
            Method show = CustomBlocksDialog.class.getDeclaredMethod("showCreatePaletteDialog", Context.class,
                    ArrayList.class, String.class, CustomBlocksManager.class, ArrayList.class,
                    ArrayList.class, ArrayList.class, String.class);
            show.setAccessible(true);
            AtomicReference<Throwable> error = new AtomicReference<>();
            host.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                try {
                    show.invoke(new CustomBlocksDialog(), activity, new ArrayList<HashMap<String, Object>>(),
                            palettes.getAbsolutePath(), null, new ArrayList<BlockBean>(),
                            new ArrayList<HashMap<String, Object>>(), new ArrayList<HashMap<String, Object>>(), blocks.getAbsolutePath());
                } catch (Throwable failure) { error.set(failure); }
            });
            if (error.get() != null) { close(); throw new AssertionError(error.get()); }
            INSTRUMENTATION.waitForIdleSync();
        }

        void input(String name, String hex) {
            onMain(() -> {
                View root = dialogRoot(); assertNotNull(root);
                ((EditText) root.findViewById(R.id.nameEditText)).setText(name);
                EditText color = root.findViewById(R.id.colorEditText); color.setText(hex);
                assertEquals("Use the actual hex-only field", hex, color.getText().toString());
            });
        }

        void click(int button) {
            onMain(() -> {
                View root = dialogRoot(); assertNotNull(root);
                assertTrue(root.findViewById(button == AlertDialog.BUTTON_POSITIVE ? android.R.id.button1 : android.R.id.button2).performClick());
            });
            INSTRUMENTATION.waitForIdleSync();
        }

        void assertRejected(int field) throws Exception {
            onMain(() -> {
                View root = dialogRoot(); assertNotNull("Invalid input keeps the dialog open", root);
                assertNotNull(((TextInputLayout) root.findViewById(field)).getError());
            });
            assertUnchanged();
        }

        void assertUnchanged() throws Exception {
            assertEquals("[]", read(palettes));
            assertEquals("[]", read(blocks));
        }

        void assertSaved(String hex) throws Exception {
            onMain(() -> assertNull("Successful save closes the dialog", dialogRoot()));
            List<HashMap<String, Object>> saved = GSON.fromJson(read(palettes),
                    new TypeToken<List<HashMap<String, Object>>>() {}.getType());
            assertEquals(1, saved.size()); assertEquals("Palette", saved.get(0).get("name"));
            assertEquals("#" + hex, saved.get(0).get("color"));
            assertEquals(Color.parseColor("#" + hex), Color.parseColor((String) saved.get(0).get("color")));
            assertEquals("[]", read(blocks));
        }

        @Override public void close() throws Exception {
            try {
                onMain(() -> { View root = dialogRoot(); if (root != null) root.findViewById(android.R.id.button2).performClick(); });
                host.close();
            } finally {
                assertEquals(CONTEXT.getCacheDir().getCanonicalFile(), output.getCanonicalFile().getParentFile());
                assertEquals(output.getCanonicalFile(), palettes.getCanonicalFile().getParentFile());
                assertEquals(output.getCanonicalFile(), blocks.getCanonicalFile().getParentFile());
                Files.delete(palettes.toPath()); Files.delete(blocks.toPath()); Files.delete(output.toPath());
            }
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static View dialogRoot() {
        List<View> roots = WindowInspector.getGlobalWindowViews();
        for (int i = roots.size() - 1; i >= 0; i--) {
            View root = roots.get(i);
            if (root.isShown() && root.findViewById(R.id.nameEditText) != null && root.findViewById(android.R.id.button1) != null) return root;
        }
        return null;
    }

    private static void onMain(Runnable action) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        INSTRUMENTATION.runOnMainSync(() -> { try { action.run(); } catch (Throwable failure) { error.set(failure); } });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
