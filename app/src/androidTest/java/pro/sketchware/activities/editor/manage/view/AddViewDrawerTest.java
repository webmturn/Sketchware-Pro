package pro.sketchware.activities.editor.manage.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.StringRes;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.util.Helper;

@RunWith(AndroidJUnit4.class)
public class AddViewDrawerTest {
    @Test(timeout = 30000)
    public void drawerCanBeEnabledAndSavedWithoutAToolbar() {
        Intent intent =
                new Intent(
                                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                                AddViewActivity.class)
                        .putExtra("request_code", AddViewActivity.REQUEST_CODE_EDIT)
                        .putExtra(
                                "project_file",
                                new ProjectFileBean(
                                        ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
        try (ActivityScenario<AddViewActivity> scenario =
                ActivityScenario.launchActivityForResult(intent)) {
            scenario.onActivity(
                    activity ->
                            activity.getWindow()
                                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            try {
                scenario.onActivity(
                        activity -> {
                            option(activity, R.string.editor_preview_toolbar).performClick();
                            option(activity, R.string.editor_preview_drawer).performClick();
                        });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(
                        activity -> {
                            assertFalse(
                                    option(activity, R.string.editor_preview_toolbar).isChecked());
                            assertTrue(
                                    option(activity, R.string.editor_preview_drawer).isChecked());
                            option(activity, R.string.editor_preview_toolbar).performClick();
                            option(activity, R.string.editor_preview_toolbar).performClick();
                        });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(
                        activity -> {
                            assertFalse(
                                    option(activity, R.string.editor_preview_toolbar).isChecked());
                            assertTrue(
                                    option(activity, R.string.editor_preview_drawer).isChecked());
                            activity.findViewById(R.id.btn_save).performClick();
                        });
            } finally {
                if (scenario.getState() == Lifecycle.State.RESUMED) {
                    scenario.onActivity(Activity::finish);
                }
                long deadline = SystemClock.uptimeMillis() + 5000;
                while (scenario.getState() != Lifecycle.State.DESTROYED
                        && SystemClock.uptimeMillis() < deadline) {
                    SystemClock.sleep(25);
                }
                assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
            }

            var result = scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            Intent data = result.getResultData();
            assertNotNull(data);
            data.setExtrasClassLoader(ProjectFileBean.class.getClassLoader());
            ProjectFileBean file = data.getParcelableExtra("project_file");
            assertNotNull(file);
            assertEquals(ProjectFileBean.OPTION_ACTIVITY_DRAWER, file.options);
        }
    }

    private static CheckBox option(AddViewActivity activity, @StringRes int label) {
        RecyclerView features = activity.findViewById(R.id.feature_types);
        String title = Helper.getResString(label);
        for (int i = 0; i < features.getChildCount(); i++) {
            View row = features.getChildAt(i);
            TextView name = row.findViewById(R.id.tv_name);
            if (title.contentEquals(name.getText())) {
                return row.findViewById(R.id.checkbox);
            }
        }
        throw new AssertionError("Option is missing: " + title);
    }
}
