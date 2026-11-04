package pro.sketchware.activities.projects;

import static org.junit.Assert.*;

import android.content.Intent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pro.sketchware.R;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.core.project.LibraryManager;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;

/** Never saves a project. The temporary library-cache replacement requires serial execution. */
@RunWith(AndroidJUnit4.class)
public class PackageNameWarningTest {
    private final List<File> resourceDirectories = new ArrayList<>();

    @After public void cleanUpResourceDirectories() {
        for (File directory : resourceDirectories) FileUtil.deleteFile(directory.getPath());
        for (File directory : resourceDirectories) {
            assertFalse("Test resource directory was not removed: " + directory, directory.exists());
        }
    }

    @Test public void packageInputShowsTheWarningOnlyOnce() throws Exception {
        try (var scenario = launch("example.project")) {
            focusPackage(scenario, "example.project");
            assertNotNull("Focusing the package input must show the warning", warningMessage());
            dismissWarning();
            focusPackage(scenario, "example.project");
            assertNull("The same form must not repeat the warning", warningMessage());
        }
    }

    @Test public void onlyTheExactDefaultPackageSkipsTheWarning() throws Exception {
        try (var scenario = launch("com.my.newproject")) {
            focusPackage(scenario, "com.my.newproject");
            assertNull(warningMessage());
            focusPackage(scenario, "com.my.newproject2");
            assertNotNull("A package containing the default name is still a different package", warningMessage());
            dismissWarning();
        }
    }

    @Test public void firebaseNoticeFollowsTheExistingProjectLibraryState() throws Exception {
        Field libraryField = ProjectDataManager.class.getDeclaredField("libraryManager");
        libraryField.setAccessible(true);
        Object original = libraryField.get(null);
        String id = newProjectId();
        LibraryManager libraries = new LibraryManager(id);
        try {
            libraryField.set(null, libraries);
            for (boolean enabled : new boolean[]{false, true}) {
                libraries.getFirebaseDB().useYn = enabled ? ProjectLibraryBean.LIB_USE_Y : ProjectLibraryBean.LIB_USE_N;
                try (var scenario = ActivityScenario.<MyProjectSettingActivity>launch(intent(id, "example.project")
                        .putExtra("is_update", true))) {
                    focusPackage(scenario, "example.project");
                    String expected = Helper.getResString(R.string.myprojects_settings_message_package_rename);
                    if (enabled) expected += "\n\n" + Helper.getResString(R.string.myprojects_settings_message_package_rename_firebase);
                    assertEquals(expected, warningMessage());
                    dismissWarning();
                }
            }
        } finally {
            libraryField.set(null, original);
        }
    }

    private String newProjectId() {
        String id = "__package_warning_" + UUID.randomUUID();
        for (String root : new String[]{SketchwarePaths.getIconsPath(), SketchwarePaths.getImagesPath(),
                SketchwarePaths.getSoundsPath(), SketchwarePaths.getFontsResourcePath()}) {
            File directory = new File(root, id);
            assertFalse("Test resource directory already exists: " + directory, directory.exists());
            resourceDirectories.add(directory);
        }
        return id;
    }

    private ActivityScenario<MyProjectSettingActivity> launch(String packageName) {
        return ActivityScenario.launch(intent(newProjectId(), packageName));
    }

    private static Intent intent(String id, String packageName) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), MyProjectSettingActivity.class)
                .putExtra("sc_id", id)
                .putExtra("my_sc_pkg_name", packageName).putExtra("my_ws_name", "PackageWarningTest")
                .putExtra("my_app_name", "PackageWarningTest");
    }

    private static void focusPackage(ActivityScenario<MyProjectSettingActivity> scenario, String packageName) {
        scenario.onActivity(activity -> {
            activity.binding.etAppName.requestFocus();
            activity.binding.etPackageName.setText(packageName);
            activity.binding.etPackageName.requestFocus();
        });
    }

    private static AccessibilityNodeInfo activeRoot() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        instrumentation.getUiAutomation().waitForIdle(100, 5000);
        return instrumentation.getUiAutomation().getRootInActiveWindow();
    }

    private static String warningMessage() throws Exception {
        AccessibilityNodeInfo root = activeRoot();
        if (root == null) return null;
        List<AccessibilityNodeInfo> messages = root.findAccessibilityNodeInfosByViewId("android:id/message");
        return messages.isEmpty() ? null : messages.get(0).getText().toString();
    }

    private static void dismissWarning() throws Exception {
        AccessibilityNodeInfo root = activeRoot();
        assertNotNull(root);
        List<AccessibilityNodeInfo> buttons = root.findAccessibilityNodeInfosByViewId("android:id/button1");
        assertEquals(1, buttons.size());
        assertTrue(buttons.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));
    }
}
