package pro.sketchware.activities.editor.manage.view;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;

import pro.sketchware.R;
import pro.sketchware.beans.ProjectFileBean;

@RunWith(AndroidJUnit4.class)
public class AddViewNamingTest {
    private static final int[] TYPES = {R.id.select_fragment, R.id.select_dialogfragment,
            R.id.select_bottomsheetdialogfragment};
    private static final String[] SUFFIXES = {"_fragment", "_dialog_fragment", "_bottomdialog_fragment"};

    @Test(timeout = 30000)
    public void fragmentNamesWithAndWithoutSuffixProduceTheSameFileName() {
        for (int i = 0; i < TYPES.length; i++) {
            for (String input : new String[]{"profile", "profile" + SUFFIXES[i]}) {
                try (ActivityScenario<AddViewActivity> scenario = create()) {
                    fill(scenario, TYPES[i], input);
                    save(scenario);
                    assertEquals("profile" + SUFFIXES[i], result(scenario).fileName);
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void existingFinalNamesAreRejectedForEveryFragmentType() {
        for (int i = 0; i < TYPES.length; i++) {
            try (ActivityScenario<AddViewActivity> scenario = create("profile" + SUFFIXES[i])) {
                fill(scenario, TYPES[i], "profile");
                save(scenario);
                assertRejected(scenario);
                fill(scenario, TYPES[i], "profile" + SUFFIXES[i]);
                save(scenario);
                assertRejected(scenario);
            }
        }
    }

    @Test(timeout = 30000)
    public void changingTypeRevalidatesTheFinalNameWithoutRetyping() {
        try (ActivityScenario<AddViewActivity> scenario = create("profile_fragment")) {
            fill(scenario, R.id.select_activity, "profile");
            scenario.onActivity(activity -> assertNull(nameLayout(activity).getError()));
            scenario.onActivity(activity -> type(activity).check(R.id.select_fragment));
            scenario.onActivity(activity -> assertNotNull(nameLayout(activity).getError()));
            save(scenario);
            assertRejected(scenario);
            scenario.onActivity(activity -> type(activity).check(R.id.select_dialogfragment));
            scenario.onActivity(activity -> assertNull(nameLayout(activity).getError()));
            save(scenario);
            assertEquals("profile_dialog_fragment", result(scenario).fileName);
        }
    }

    @Test(timeout = 30000)
    public void aDifferentFileTypeMayUseTheSameBaseName() {
        try (ActivityScenario<AddViewActivity> scenario = create("profile")) {
            fill(scenario, R.id.select_activity, "profile");
            save(scenario);
            assertRejected(scenario);
            scenario.onActivity(activity -> type(activity).check(R.id.select_fragment));
            scenario.onActivity(activity -> assertNull(nameLayout(activity).getError()));
            save(scenario);
            assertEquals("profile_fragment", result(scenario).fileName);
        }
    }

    @Test(timeout = 30000)
    public void editingHidesOnlyTheTypeSuffixAndPreservesTheStoredIdentity() {
        for (String fileName : new String[]{"profile_fragment", "profile_dialog_fragment",
                "profile_bottomdialog_fragment", "profile_fragment_fragment",
                "profile_fragment_tools", "profile"}) {
            ProjectFileBean original = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, fileName);
            Intent intent = intent().putExtra("request_code", AddViewActivity.REQUEST_CODE_EDIT)
                    .putExtra("project_file", original);
            try (ActivityScenario<AddViewActivity> scenario = launch(intent)) {
                String display = fileName.equals("profile_fragment_fragment") ? "profile_fragment"
                        : fileName.equals("profile_fragment_tools") ? fileName : "profile";
                scenario.onActivity(activity -> {
                    assertEquals(display, name(activity).getText().toString());
                    assertFalse(name(activity).isEnabled());
                    ((MaterialButtonToggleGroup) activity.findViewById(R.id.screenOrientationSelector))
                            .check(R.id.select_landscape);
                });
                save(scenario);
                ProjectFileBean edited = result(scenario);
                assertEquals(fileName, edited.fileName);
                assertEquals(original.getJavaName(), edited.getJavaName());
                assertEquals(original.getXmlName(), edited.getXmlName());
                assertEquals(ProjectFileBean.ORIENTATION_LANDSCAPE, edited.orientation);
            }
        }
    }

    @Test(timeout = 30000)
    public void existingIdentifierRulesStillApply() {
        try (ActivityScenario<AddViewActivity> scenario = create()) {
            for (String input : new String[]{"", "ab", "_fragment", "UpperCase", "has space", "class", "main"}) {
                fill(scenario, R.id.select_bottomsheetdialogfragment, input);
                save(scenario);
                assertEquals(input, Lifecycle.State.RESUMED, scenario.getState());
            }
            fill(scenario, R.id.select_activity, "profile");
            save(scenario);
            assertEquals("profile", result(scenario).fileName);
        }
    }

    @Test(timeout = 30000)
    public void maximumLengthIncludesTheAutomaticallyAddedSuffix() {
        for (int i = 0; i < TYPES.length; i++) {
            try (ActivityScenario<AddViewActivity> scenario = create()) {
                String longestBase = "p".repeat(100 - SUFFIXES[i].length());
                fill(scenario, TYPES[i], longestBase + "p");
                save(scenario);
                assertRejected(scenario);
                fill(scenario, TYPES[i], longestBase);
                save(scenario);
                assertEquals(longestBase + SUFFIXES[i], result(scenario).fileName);
            }
        }
    }

    @Test(timeout = 30000)
    public void recreatedInputKeepsItsTypeAndDoesNotDuplicateTheSuffix() {
        try (ActivityScenario<AddViewActivity> scenario = create()) {
            fill(scenario, R.id.select_bottomsheetdialogfragment, "profile_bottomdialog_fragment");
            scenario.recreate();
            keepScreenOn(scenario);
            scenario.onActivity(activity -> {
                assertEquals(R.id.select_bottomsheetdialogfragment, type(activity).getCheckedButtonId());
                assertEquals("profile_bottomdialog_fragment", name(activity).getText().toString());
            });
            save(scenario);
            assertEquals("profile_bottomdialog_fragment", result(scenario).fileName);
        }
    }

    @Test(timeout = 30000)
    public void cancellingNeverReturnsAFile() {
        try (ActivityScenario<AddViewActivity> scenario = create()) {
            fill(scenario, R.id.select_fragment, "profile_fragment");
            scenario.onActivity(activity -> activity.findViewById(R.id.btn_cancel).performClick());
            awaitFinish(scenario);
            Instrumentation.ActivityResult result = scenario.getResult();
            assertEquals(Activity.RESULT_CANCELED, result.getResultCode());
            assertNull(result.getResultData());
        }
    }

    private static Intent intent(String... existing) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), AddViewActivity.class)
                .putStringArrayListExtra("screen_names", new ArrayList<>(Arrays.asList(existing)));
    }

    private static ActivityScenario<AddViewActivity> create(String... existing) {
        return launch(intent(existing));
    }

    private static ActivityScenario<AddViewActivity> launch(Intent intent) {
        ActivityScenario<AddViewActivity> scenario = ActivityScenario.launchActivityForResult(intent);
        keepScreenOn(scenario);
        return scenario;
    }

    private static void keepScreenOn(ActivityScenario<AddViewActivity> scenario) {
        scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    private static EditText name(AddViewActivity activity) {
        return activity.findViewById(R.id.ed_name);
    }

    private static TextInputLayout nameLayout(AddViewActivity activity) {
        return activity.findViewById(R.id.ti_name);
    }

    private static MaterialButtonToggleGroup type(AddViewActivity activity) {
        return activity.findViewById(R.id.viewTypeSelector);
    }

    private static void fill(ActivityScenario<AddViewActivity> scenario, int type, String name) {
        scenario.onActivity(activity -> {
            type(activity).check(type);
            name(activity).setText(name);
        });
    }

    private static void save(ActivityScenario<AddViewActivity> scenario) {
        boolean[] finishing = {false};
        scenario.onActivity(activity -> {
            activity.findViewById(R.id.btn_save).performClick();
            finishing[0] = activity.isFinishing();
        });
        if (finishing[0]) awaitFinish(scenario);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void awaitFinish(ActivityScenario<AddViewActivity> scenario) {
        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
        while (scenario.getState() != Lifecycle.State.DESTROYED
                && android.os.SystemClock.uptimeMillis() < deadline) android.os.SystemClock.sleep(25);
        assertEquals("The result activity must finish", Lifecycle.State.DESTROYED, scenario.getState());
    }

    private static void assertRejected(ActivityScenario<AddViewActivity> scenario) {
        assertEquals("A duplicate or invalid filename must stay editable", Lifecycle.State.RESUMED, scenario.getState());
        scenario.onActivity(activity -> assertNotNull(nameLayout(activity).getError()));
    }

    private static ProjectFileBean result(ActivityScenario<AddViewActivity> scenario) {
        Instrumentation.ActivityResult result = scenario.getResult();
        assertEquals(Activity.RESULT_OK, result.getResultCode());
        assertNotNull(result.getResultData());
        result.getResultData().setExtrasClassLoader(ProjectFileBean.class.getClassLoader());
        ProjectFileBean file = result.getResultData().getParcelableExtra("project_file");
        assertNotNull(file);
        return file;
    }
}
