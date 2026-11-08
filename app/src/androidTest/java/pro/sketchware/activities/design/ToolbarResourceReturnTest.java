package pro.sketchware.activities.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.accessibilityservice.AccessibilityService;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager.widget.ViewPager;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.io.EncryptedFileUtil;

@RunWith(AndroidJUnit4.class)
public class ToolbarResourceReturnTest {
    @Test(timeout = 30000)
    public void returningFromResourcesOpenedOnTheEventTabRefreshesToolbar() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            var file = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
            file.setActivityOptions(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR);
            var compat = ProjectDataManager.getLibraryManager(fixture.id).getCompat();
            compat.configurations.put("material3", true);
            compat.configurations.put("theme", "Light");
            var encryption = new EncryptedFileUtil();
            Files.write(
                    new File(fixture.data, "file").toPath(),
                    encryption.encryptString(
                            "@activity\n" + ProjectDataStore.gson.toJson(file) + "\n@customview\n",
                            true));
            Files.write(
                    new File(fixture.data, "library").toPath(),
                    encryption.encryptString(
                            "@compat\n" + ProjectDataStore.gson.toJson(compat) + "\n", true));

            try (var scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(
                        activity -> {
                            ((ViewPager) activity.findViewById(R.id.viewpager))
                                    .setCurrentItem(1, false);
                            activity.toResourceEditor();
                        });
                ProjectEditorPersistenceTest.waitUntil(
                        () -> {
                            var root = instrumentation.getUiAutomation().getRootInActiveWindow();
                            return root != null
                                    && !root.findAccessibilityNodeInfosByText("styles.xml")
                                            .isEmpty();
                        });
                String styles =
                        SketchwarePaths.getProjectResourceValuesFilePath(fixture.id, "styles.xml");
                FileUtil.writeFile(
                        styles,
                        """
                        <resources>
                            <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
                                <item name="colorPrimary">#007FAC</item>
                            </style>
                        </resources>
                        """);
                assertTrue(new File(styles).isFile());
                assertTrue(
                        instrumentation
                                .getUiAutomation()
                                .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK));
                ProjectEditorPersistenceTest.waitUntil(
                        () -> scenario.getState() == Lifecycle.State.RESUMED);
                instrumentation.waitForIdleSync();

                scenario.onActivity(
                        activity -> {
                            ((ViewPager) activity.findViewById(R.id.viewpager))
                                    .setCurrentItem(0, false);
                            var titles = new ArrayList<View>();
                            activity.findViewById(R.id.view_editor)
                                    .findViewsWithText(
                                            titles,
                                            activity.getString(R.string.editor_preview_toolbar),
                                            View.FIND_VIEWS_WITH_TEXT);
                            assertEquals(1, titles.size());
                            View toolbar = (View) titles.get(0).getParent();
                            assertEquals(
                                    0xff007fac,
                                    ((ColorDrawable) toolbar.getBackground()).getColor());
                        });
            }
        }
    }
}
