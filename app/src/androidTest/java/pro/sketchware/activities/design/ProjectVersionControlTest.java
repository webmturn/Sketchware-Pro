package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.projects.MyProjectSettingActivity;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.dialogs.VersionDialog;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ProjectVersionControlTest {
    private static final int MAX_CODE = 2_100_000_000;

    @Test(timeout = 30000)
    public void invalidCodesKeepTheDialogOpenAndNeverReachTheSaveCallback() throws Exception {
        try (Project project = new Project()) {
            AtomicInteger saves = new AtomicInteger();
            project.scenario.onActivity(activity -> new VersionDialog(activity, "1", "1.0",
                    (code, name) -> saves.incrementAndGet()).show());
            for (String invalid : new String[]{"0", "", "-1", "2100000001", "2147483647",
                    "9223372036854775808", "1.5", "not_a_number"}) {
                Thread.sleep(150);
                project.scenario.onActivity(activity -> {
                    codeInput().setText(invalid);
                    dialogView(android.R.id.button1).performClick();
                });
                idle();
                project.scenario.onActivity(activity -> {
                    assertEquals(invalid, 0, saves.get());
                    assertTrue("Invalid input must remain editable: " + invalid, hasAdvancedDialog());
                    assertNotNull(((TextInputLayout) dialogView(R.id.tilVersionCode)).getError());
                });
            }
            project.scenario.onActivity(activity -> dialogView(android.R.id.button2).performClick());
            idle();
        }
    }

    @Test(timeout = 30000)
    public void invalidNameAndPostfixRemainEditable() throws Exception {
        try (Project project = new Project()) {
            AtomicInteger saves = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                new VersionDialog(activity, "5", "1.0", (code, name) -> saves.incrementAndGet()).show();
                ((EditText) dialogView(R.id.version_name1)).setText("");
                dialogView(android.R.id.button1).performClick();
            });
            idle();
            project.scenario.onActivity(activity -> {
                assertEquals(0, saves.get());
                assertTrue(hasAdvancedDialog());
                assertNotNull(((TextInputLayout) dialogView(R.id.tilVersionName)).getError());
                ((EditText) dialogView(R.id.version_name1)).setText("1.0");
                ((EditText) dialogView(R.id.version_name2)).setText("bad postfix!");
                dialogView(android.R.id.button1).performClick();
            });
            idle();
            project.scenario.onActivity(activity -> {
                assertEquals(0, saves.get());
                assertTrue(hasAdvancedDialog());
                assertNotNull(((TextInputLayout) dialogView(R.id.tilVersionNameExtra)).getError());
                dialogView(android.R.id.button2).performClick();
            });
            idle();
        }
    }

    @Test(timeout = 30000)
    public void inclusiveBoundsAndTrimmedCodesCanBeConfirmed() throws Exception {
        try (Project project = new Project()) {
            for (String input : new String[]{"1", "2100000000", " 00042 "}) {
                Thread.sleep(150);
                AtomicReference<String> saved = new AtomicReference<>();
                project.scenario.onActivity(activity -> {
                    new VersionDialog(activity, "1", "1.0", (code, name) -> saved.set(code + ":" + name)).show();
                    codeInput().setText(input);
                    ((EditText) dialogView(R.id.version_name2)).setText("beta_1");
                    dialogView(android.R.id.button1).performClick();
                });
                idle();
                assertEquals(Integer.parseInt(input.trim()) + ":1.0 beta_1", saved.get());
                project.scenario.onActivity(activity -> assertFalse(hasAdvancedDialog()));
            }
        }
    }

    @Test(timeout = 30000)
    public void cancelDoesNotChangeTheProjectOrParentFields() throws Exception {
        try (Project project = new Project()) {
            var before = project.fixture.snapshot();
            project.scenario.onActivity(activity -> {
                showAdvanced(activity);
                codeInput().setText("2100000000");
                dialogView(android.R.id.button2).performClick();
            });
            idle();
            project.scenario.onActivity(activity -> {
                assertEquals("1", activity.binding.verCode.getText().toString());
                assertEquals("1.0", activity.binding.verName.getText().toString());
            });
            project.fixture.assertUnchanged(before);
        }
    }

    @Test(timeout = 30000)
    public void legacyPickerBoundsAreSafeAndCancelPreservesImportedValues() throws Exception {
        try (Project project = new Project()) {
            for (String input : new String[]{"0", "-1", "2100000000", "2147483647", "not_a_number"}) {
                project.scenario.onActivity(activity -> {
                    activity.binding.verCode.setText(input);
                    showLegacy(activity);
                    NumberPicker picker = (NumberPicker) dialogView(R.id.version_code);
                    assertTrue(picker.getMinValue() >= 1);
                    assertTrue(picker.getMaxValue() <= MAX_CODE);
                    assertTrue(picker.getValue() >= 1 && picker.getValue() <= MAX_CODE);
                    dialogView(android.R.id.button2).performClick();
                    assertEquals(input, activity.binding.verCode.getText().toString());
                });
                idle();
            }
            assertEquals("1", ProjectListManager.getProjectById(project.fixture.id).get("sc_ver_code"));
        }
    }

    @Test(timeout = 30000)
    public void legacyPickerHandlesLargeVersionNamePartsWithoutOverflow() throws Exception {
        try (Project project = new Project()) {
            for (String name : new String[]{Integer.MAX_VALUE + "." + Integer.MAX_VALUE, ".", "-1.-2"}) {
                project.scenario.onActivity(activity -> {
                    activity.binding.verName.setText(name);
                    showLegacy(activity);
                    NumberPicker first = (NumberPicker) dialogView(R.id.version_name1);
                    NumberPicker second = (NumberPicker) dialogView(R.id.version_name2);
                    if (name.startsWith(Integer.toString(Integer.MAX_VALUE))) {
                        assertEquals(Integer.MAX_VALUE, first.getValue());
                        assertEquals(Integer.MAX_VALUE, first.getMaxValue());
                        assertEquals(Integer.MAX_VALUE, second.getValue());
                        assertEquals(Integer.MAX_VALUE, second.getMaxValue());
                    } else {
                        assertEquals(1, first.getValue());
                        assertEquals(0, second.getValue());
                    }
                    dialogView(android.R.id.button2).performClick();
                    assertEquals(name, activity.binding.verName.getText().toString());
                });
                idle();
            }
        }
    }

    @Test(timeout = 30000)
    public void projectSaveRejectsAnInvalidImportedCodeWithoutWritingMetadata() throws Exception {
        try (Project project = new Project()) {
            var before = project.fixture.snapshot();
            project.scenario.onActivity(activity -> {
                activity.binding.verCode.setText("0");
                activity.binding.okButton.performClick();
            });
            Thread.sleep(500);
            assertEquals(Lifecycle.State.RESUMED, project.scenario.getState());
            assertEquals("1", ProjectListManager.getProjectById(project.fixture.id).get("sc_ver_code"));
            project.fixture.assertUnchanged(before);
        }
    }

    @Test(timeout = 30000)
    public void correctingTheCodeCanPersistTheUpperBound() throws Exception {
        try (Project project = new Project()) {
            Thread.sleep(150);
            project.scenario.onActivity(activity -> {
                showAdvanced(activity);
                codeInput().setText("2100000000");
                dialogView(android.R.id.button1).performClick();
            });
            idle();
            project.scenario.onActivity(activity -> {
                assertEquals("2100000000", activity.binding.verCode.getText().toString());
                activity.binding.okButton.performClick();
            });
            ProjectEditorPersistenceTest.waitUntil(() -> "2100000000".equals(
                    ProjectListManager.getProjectById(project.fixture.id).get("sc_ver_code")));
        }
    }

    @Test(timeout = 30000)
    public void projectSaveNormalizesAValidImportedCode() throws Exception {
        try (Project project = new Project()) {
            project.scenario.onActivity(activity -> {
                activity.binding.verCode.setText(" 00042 ");
                activity.binding.okButton.performClick();
            });
            ProjectEditorPersistenceTest.waitUntil(() -> "42".equals(
                    ProjectListManager.getProjectById(project.fixture.id).get("sc_ver_code")));
        }
    }

    private static void showAdvanced(MyProjectSettingActivity activity) {
        new VersionDialog(activity, activity.binding.verCode.getText().toString(),
                activity.binding.verName.getText().toString(), (code, name) -> {
                    activity.binding.verCode.setText(code);
                    activity.binding.verName.setText(name);
                }).show();
    }

    private static void idle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void showLegacy(MyProjectSettingActivity activity) {
        try {
            Method method = MyProjectSettingActivity.class.getDeclaredMethod("showOldVersionControlDialog");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("The legacy version picker must open safely", e);
        }
    }

    private static EditText codeInput() {
        return (EditText) dialogView(R.id.version_code);
    }

    private static boolean hasAdvancedDialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.version_name1) instanceof EditText) return true;
        }
        return false;
    }

    private static View dialogView(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.version_code) != null) {
                View result = root.findViewById(id);
                if (result != null) return result;
            }
        }
        throw new AssertionError("Version dialog view missing: " + id);
    }

    private static final class Project implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture;
        final ActivityScenario<MyProjectSettingActivity> scenario;

        Project() throws Exception {
            fixture = new ProjectEditorPersistenceTest.Fixture();
            try {
                fixture.seed(false, false);
                Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        MyProjectSettingActivity.class);
                intent.putExtra("sc_id", fixture.id);
                intent.putExtra("is_update", true);
                scenario = ActivityScenario.launch(intent);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            } catch (Exception | AssertionError e) {
                fixture.close();
                throw e;
            }
        }

        @Override public void close() throws Exception {
            scenario.close();
            fixture.close();
        }
    }
}
