package pro.sketchware.activities.editor.manage.font;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.editor.manage.ManageCollectionActivity;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.exception.CompileException;
import pro.sketchware.core.project.BaseCollectionManager;
import pro.sketchware.core.project.FontCollectionManager;
import pro.sketchware.util.UIHelper;

@RunWith(AndroidJUnit4.class)
public class FontCollectionEntryTest {
    @Test(timeout = 30000)
    public void collectionEntryRejectsExistingNamesBeforeSave() throws Exception {
        try (Fixture fixture = new Fixture("existing_font", "another_font");
             Entry entry = new Entry()) {
            byte[] listBefore = Files.readAllBytes(fixture.list);
            for (String name : new String[]{"existing_font", "another_font"}) {
                onMain(() -> {
                    setPickedFont(entry.activity, fixture.pickedFont);
                    input(entry.activity).setText(name);
                    assertDuplicateError(entry.activity);
                    entry.activity.findViewById(R.id.common_dialog_ok_button).performClick();
                    assertFalse("Duplicate save closed the dialog", entry.activity.isFinishing());
                    assertDuplicateError(entry.activity);
                });
                assertArrayEquals(listBefore, Files.readAllBytes(fixture.list));
                assertEquals(2, fixture.manager.getResources().size());
            }
            onMain(() -> {
                input(entry.activity).setText("new_font");
                assertNull(layout(entry.activity).getError());
                input(entry.activity).setText("existing_font");
                assertDuplicateError(entry.activity);
            });
        }
    }

    @Test(timeout = 30000)
    public void collectionEntryAcceptsUniqueNameAndPersistsIt() throws Exception {
        try (Fixture fixture = new Fixture("existing_font"); Entry entry = new Entry()) {
            onMain(() -> {
                assertTrue(entry.activity.getIntent().getBooleanExtra("add_to_collection", false));
                CheckBox checkbox = entry.activity.findViewById(R.id.add_to_collection_checkbox);
                assertTrue(checkbox.isChecked());
                assertFalse(checkbox.isEnabled());
                assertEquals("f10_isolated", entry.activity.getIntent().getStringExtra("sc_id"));
                setPickedFont(entry.activity, fixture.pickedFont);
                input(entry.activity).setText("unique_font");
                assertNull(layout(entry.activity).getError());
                entry.activity.findViewById(R.id.common_dialog_ok_button).performClick();
                assertTrue(entry.activity.isFinishing());
            });
            fixture.manager.loadCollections();
            assertEquals(2, fixture.manager.getResources().size());
            ProjectResourceBean saved = fixture.manager.getResourceByName("unique_font");
            assertNotNull(saved);
            assertArrayEquals(Files.readAllBytes(fixture.pickedFont.toPath()),
                    Files.readAllBytes(fixture.data.resolve(saved.resFullName)));
        }
    }

    @Test(timeout = 30000)
    public void recreationKeepsCollectionDuplicateChecks() throws Exception {
        try (Fixture fixture = new Fixture("existing_font"); Entry entry = new Entry()) {
            AddFontActivity original = entry.activity;
            onMain(original::recreate);
            long deadline = SystemClock.uptimeMillis() + 10000;
            AtomicReference<AddFontActivity> recreated = new AtomicReference<>();
            while (recreated.get() == null && SystemClock.uptimeMillis() < deadline) {
                onMain(() -> {
                    for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                        if (activity instanceof AddFontActivity fontActivity && activity != original) {
                            recreated.set(fontActivity);
                        }
                    }
                });
                if (recreated.get() == null) SystemClock.sleep(50);
            }
            assertNotNull("Font dialog did not recreate", recreated.get());
            entry.activity = recreated.get();
            onMain(() -> {
                input(entry.activity).setText("existing_font");
                assertDuplicateError(entry.activity);
                input(entry.activity).setText("unique_font");
                assertNull(layout(entry.activity).getError());
            });
            assertEquals(1, fixture.manager.getResources().size());
        }
    }

    @Test(timeout = 30000)
    public void emptyCollectionAcceptsANameAndCancelKeepsItEmpty() throws Exception {
        try (Fixture fixture = new Fixture(); Entry entry = new Entry()) {
            onMain(() -> {
                ArrayList<String> names = entry.activity.getIntent().getStringArrayListExtra("font_names");
                assertNotNull(names);
                assertTrue(names.isEmpty());
                input(entry.activity).setText("first_font");
                assertNull(layout(entry.activity).getError());
                entry.activity.findViewById(R.id.common_dialog_cancel_button).performClick();
                assertTrue(entry.activity.isFinishing());
            });
            assertTrue(fixture.manager.getResources().isEmpty());
            assertFalse(Files.exists(fixture.list));
        }
    }

    private static void assertDuplicateError(AddFontActivity activity) {
        CharSequence error = layout(activity).getError();
        assertNotNull("Collection entry allowed an existing font name", error);
        assertEquals(activity.getString(R.string.common_message_name_unavailable), error.toString());
    }

    private static EditText input(AddFontActivity activity) {
        return activity.findViewById(R.id.ed_input);
    }

    private static TextInputLayout layout(AddFontActivity activity) {
        return activity.findViewById(R.id.ti_input);
    }

    private static void setPickedFont(AddFontActivity activity, File file) {
        // Test name/save behavior with an existing font; the SAF picker is outside this regression.
        setField(AddFontActivity.class, activity, "fontUri", Uri.fromFile(file));
        setField(AddFontActivity.class, activity, "validFontPicked", true);
    }

    private static void setField(Class<?> type, Object target, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Instrumentation instrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    private static Context context() {
        return instrumentation().getTargetContext();
    }

    private static void onMain(Runnable action) {
        instrumentation().runOnMainSync(action);
    }

    private static class Entry implements AutoCloseable {
        final Instrumentation.ActivityMonitor monitor;
        final ActivityScenario<ManageCollectionActivity> scenario;
        AddFontActivity activity;

        Entry() {
            Intent intent = new Intent(context(), ManageCollectionActivity.class);
            intent.putExtra("sc_id", "f10_isolated");
            scenario = ActivityScenario.launch(intent);
            monitor = instrumentation().addMonitor(AddFontActivity.class.getName(), null, false);
            try {
                scenario.onActivity(collection -> {
                    UIHelper.lastClickTime = 0;
                    NavigationRailView categories = collection.findViewById(R.id.category_list);
                    categories.setSelectedItemId(R.id.font);
                    collection.findViewById(R.id.fab).performClick();
                });
                Activity launched = instrumentation().waitForMonitorWithTimeout(monitor, 10000);
                assertTrue("Collection FAB did not open AddFontActivity", launched instanceof AddFontActivity);
                activity = (AddFontActivity) launched;
                instrumentation().waitForIdleSync();
                onMain(() -> {
                    Object extra = activity.getIntent().getExtras().get("font_names");
                    ArrayList<?> names = (ArrayList<?>) extra;
                    Bundle status = new Bundle();
                    status.putString("f10_entry", "font_names size=" + names.size() + ", firstType="
                            + (names.isEmpty() ? "empty" : names.get(0).getClass().getName()));
                    instrumentation().sendStatus(0, status);
                });
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
        }

        @Override
        public void close() {
            if (activity != null) onMain(() -> {
                if (!activity.isDestroyed()) activity.finish();
            });
            instrumentation().waitForIdleSync();
            scenario.close();
            instrumentation().removeMonitor(monitor);
        }
    }

    private static class Fixture implements AutoCloseable {
        final Path root;
        final Path list;
        final Path data;
        final File pickedFont;
        final FontCollectionManager manager;
        final FontCollectionManager original;
        final long previousClickTime;

        Fixture(String... existingNames) throws Exception {
            original = FontCollectionManager.getInstance();
            manager = new FontCollectionManager();
            previousClickTime = UIHelper.lastClickTime;
            root = Files.createTempDirectory(context().getCacheDir().toPath(), "f10_font_entry_");
            list = root.resolve("list");
            data = root.resolve("data");
            pickedFont = root.resolve("picked.ttf").toFile();
            try {
                File[] systemFonts = new File("/system/fonts").listFiles((dir, name) -> name.endsWith(".ttf"));
                assertNotNull(systemFonts);
                assertTrue(systemFonts.length > 0);
                Files.copy(systemFonts[0].toPath(), pickedFont.toPath(), StandardCopyOption.REPLACE_EXISTING);
                setField(BaseCollectionManager.class, manager, "collectionFilePath", list.toString());
                setField(BaseCollectionManager.class, manager, "dataDirPath", data.toString());
                setField(BaseCollectionManager.class, manager, "collections", new ArrayList<>());
                onMain(() -> setField(FontCollectionManager.class, null, "instance", manager));
                for (String name : existingNames) manager.addResource("f10_isolated", resource(name));
                // Confirm the existing persistence guard separately from the page's validation.
                if (existingNames.length > 0) {
                    try {
                        manager.addResource("f10_isolated", resource(existingNames[0]));
                        fail("Collection manager allowed a duplicate");
                    } catch (CompileException expected) {
                        assertEquals("duplicate_name", expected.getMessage());
                    }
                }
            } catch (Exception | Error e) {
                try {
                    close();
                } catch (Exception cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
                throw e;
            }
        }

        private ProjectResourceBean resource(String name) {
            ProjectResourceBean bean = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                    name, pickedFont.getAbsolutePath());
            bean.savedPos = 1;
            return bean;
        }

        @Override
        public void close() throws Exception {
            try {
                onMain(() -> {
                    setField(FontCollectionManager.class, null, "instance", original);
                    UIHelper.lastClickTime = previousClickTime;
                });
            } finally {
                try (Stream<Path> paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
            assertFalse("Isolated font fixture was not removed", Files.exists(root));
            Bundle status = new Bundle();
            status.putString("f10_fixture_cleanup", root + ": removed");
            instrumentation().sendStatus(0, status);
        }
    }
}
