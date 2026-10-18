package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import pro.sketchware.activities.resourceseditor.ResourcesEditorActivity;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;

/**
 * Failure-before-fix evidence. Uses only APIs present in the pre-task-1 APK: the old save method
 * returns boolean and refuses to write XML when draft protection fails.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ResourceEditorDraftSaveBeforeTest {
    @Test
    public void draftFailureBlocksXmlWriteInBeforeBuild() throws Exception {
        try {
            Class.forName("pro.sketchware.activities.resourceseditor.ResourceEditorSaveResult");
            org.junit.Assume.assumeTrue("This test only applies to the pre-task-1 build", false);
        } catch (ClassNotFoundException expected) {
            // Old build: no explicit save result types, so the legacy boolean path is exercised.
        }
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            seedResources(fixture.id);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            File xml = new File(SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, "strings.xml"));
            byte[] original = Files.readAllBytes(xml.toPath());
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    ResourcesEditorActivity.class).putExtra("sc_id", fixture.id);
            try (ActivityScenario<ResourcesEditorActivity> scenario = ActivityScenario.launch(intent)) {
                awaitLoaded(scenario);
                String[] session = new String[1];
                on(scenario, a -> {
                    a.stringsEditor.addString("before_a", "A", "");
                    call(a, "protectDrafts");
                    session[0] = session(a);
                    File pages = new File(new File(a.getNoBackupFilesDir(), "resource-editor-drafts"),
                            session[0] + "/pages");
                    assertTrue("draft page directory must exist", pages.isDirectory());
                    assertTrue("must be able to make the test draft directory read-only",
                            pages.setWritable(false, false));
                });
                try {
                    on(scenario, a -> {
                        a.stringsEditor.addString("before_b", "B", "");
                        Object result = call(a, "saveEditorsChanges");
                        assertTrue("before build must block XML when draft protection fails",
                                Boolean.FALSE.equals(result));
                    });
                    assertArrayEquals("before build leaves the XML unchanged", original,
                            Files.readAllBytes(xml.toPath()));
                } finally {
                    on(scenario, a -> {
                        File pages = new File(new File(a.getNoBackupFilesDir(), "resource-editor-drafts"),
                                session[0] + "/pages");
                        pages.setWritable(true, false);
                    });
                }
            }
        }
    }

    private static void on(ActivityScenario<ResourcesEditorActivity> scenario, Action action) {
        scenario.onActivity(activity -> {
            try {
                action.run(activity);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    private interface Action {
        void run(ResourcesEditorActivity activity) throws Exception;
    }

    private static void seedResources(String id) throws Exception {
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        String[] content = {
                "<resources><string name=\"app_name\">SaveTest</string><string name=\"probe\">base</string></resources>",
                "<resources><color name=\"probe_color\">#336699</color></resources>",
                "<resources><style name=\"ProbeStyle\"><item name=\"android:textColor\">#336699</item></style></resources>",
                "<resources><style name=\"ProbeTheme\" parent=\"Theme.AppCompat.Light.NoActionBar\"><item name=\"colorPrimary\">#336699</item></style></resources>",
                "<resources><string-array name=\"probe_array\"><item>one</item></string-array></resources>",
                "<resources><dimen name=\"probe_size\">12dp</dimen></resources>"
        };
        for (int i = 0; i < names.length; i++) {
            File file = new File(SketchwarePaths.getProjectResourceValuesFilePath(id, names[i]));
            assertTrue(file.getParentFile().mkdirs() || file.getParentFile().isDirectory());
            Files.write(file.toPath(), content[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void awaitLoaded(ActivityScenario<ResourcesEditorActivity> scenario) throws Exception {
        Field loaded = ResourcesEditorActivity.class.getDeclaredField("editorsLoaded");
        loaded.setAccessible(true);
        long deadline = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            on(scenario, a -> {
                try {
                    ready[0] = loaded.getBoolean(a) && a.stringsEditor.listmap.stream()
                            .anyMatch(row -> "probe".equals(row.get("key")) && "base".equals(row.get("text")));
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            if (ready[0]) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                return;
            }
            SystemClock.sleep(20);
        }
        fail("Editors did not load");
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static String session(ResourcesEditorActivity activity) throws Exception {
        Field controller = ResourcesEditorActivity.class.getDeclaredField("draftController");
        controller.setAccessible(true);
        Object value = controller.get(activity);
        assertNotNull(value);
        Field session = value.getClass().getDeclaredField("session");
        session.setAccessible(true);
        return (String) session.get(value);
    }
}
