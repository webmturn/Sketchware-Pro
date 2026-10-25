package pro.sketchware.activities.main.activities;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.Lifecycle;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import pro.sketchware.core.settings.ApplicationSettings;
import pro.sketchware.R;
import pro.sketchware.util.ThrottleTimer;
import pro.sketchware.widgets.MessageBottomSheetDialog;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class StartupPromptTest {
    @Test(timeout = 30000) public void updatePrecedesStorageAndPermissionFlowDoesNotStackDialogs() throws Exception {
        withStartup(scenario -> {
            waitFor(() -> {
                AtomicBoolean enabled = new AtomicBoolean();
                scenario.onActivity(activity -> enabled.set(((MessageBottomSheetDialog) current(activity)).getPositiveButton().isEnabled()));
                return enabled.get();
            });
            scenario.onActivity(activity -> {
                assertEquals(1, dialogCount(activity));
                assertTrue(current(activity) instanceof MessageBottomSheetDialog);
                // Model leaving for the changelog screen; permissions must wait for return.
                set(activity, "waitingForStartupActivity", true);
                current(activity).dismiss();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> assertNull(current(activity)));
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            waitFor(() -> hasDialog(scenario));
            scenario.onActivity(activity -> {
                assertEquals(1, dialogCount(activity));
                assertFalse(current(activity) instanceof MessageBottomSheetDialog);
                // Simulate an asynchronous system request without opening or accepting permissions.
                set(activity, "waitingForStoragePermission", true);
                current(activity).dismiss();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                assertEquals(0, dialogCount(activity));
                assertNull(current(activity));
                activity.onRequestPermissionsResult(9501,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        new int[]{PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_DENIED});
                assertEquals(1, dialogCount(activity));
                assertNull(current(activity)); // The base permission denial dialog is still active.
                View cancel = WindowInspector.getGlobalWindowViews().stream()
                        .map(root -> root.<View>findViewById(android.R.id.button2)).filter(view -> view != null && view.isShown())
                        .findFirst().orElseThrow();
                assertTrue(cancel.performClick());
            });
            waitFor(() -> {
                AtomicBoolean complete = new AtomicBoolean();
                scenario.onActivity(activity -> complete.set(current(activity) == null && dialogCount(activity) == 0));
                return complete.get();
            });
        });
    }

    @Test(timeout = 20000) public void recreationLeavesOnlyOneStartupDialog() throws Exception {
        withStartup(scenario -> {
            scenario.recreate();
            waitFor(() -> hasDialog(scenario));
            scenario.onActivity(activity -> {
                assertEquals(1, dialogCount(activity));
                assertNotNull(current(activity));
                assertTrue(current(activity) instanceof MessageBottomSheetDialog);
            });
        });
    }

    @Test(timeout = 20000) public void backupEntrySkipsUpdateAndStartsWithStorage() throws Exception {
        withStartup(true, scenario -> scenario.onActivity(activity -> {
            assertNotNull(current(activity));
            assertFalse(current(activity) instanceof MessageBottomSheetDialog);
            assertEquals(1, dialogCount(activity));
            assertTrue((boolean) field(activity, "startupStorageChecked"));
        }));
    }

    @Test(timeout = 20000) public void projectRefreshDoesNotShowAPermissionNoticeBehindTheFirstPrompt() throws Exception {
        withStartup(true, scenario -> scenario.onActivity(activity -> {
            activity.refreshProjects();
            activity.showStoragePermissionRequiredSnackbar();
            assertNotNull(current(activity));
            assertFalse("Do not report denial, or show a second prompt, before a choice", noticeShown(activity));
        }));
    }

    @Test(timeout = 20000) public void dismissedExplanationShowsNeutralAccessMessageAndHidesDuringRequest() throws Exception {
        withStartup(true, scenario -> {
            scenario.onActivity(activity -> current(activity).cancel());
            waitFor(() -> hasNotice(scenario));
            scenario.onActivity(activity -> {
                TextView text = activity.findViewById(com.google.android.material.R.id.snackbar_text);
                assertEquals(activity.getString(R.string.common_message_permission_need_load_project), text.getText().toString());
                TextView action = activity.findViewById(com.google.android.material.R.id.snackbar_action);
                assertEquals(activity.getString(R.string.main_storage_permission_grant), action.getText().toString());
                set(activity, "waitingForStoragePermission", true);
                activity.showStoragePermissionRequiredSnackbar();
            });
            waitFor(() -> !hasNotice(scenario));
        });
    }

    @Test(timeout = 20000) public void grantingAccessClearsTheMissingPermissionNotice() throws Exception {
        withStartup(true, scenario -> {
            scenario.onActivity(activity -> current(activity).cancel());
            waitFor(() -> hasNotice(scenario));
            scenario.onActivity(activity -> {
                activity.storageGranted = true;
                activity.onStoragePermissionGranted(9501);
            });
            waitFor(() -> !hasNotice(scenario));
        });
    }

    private static boolean hasNotice(ActivityScenario<StartupPromptTestActivity> scenario) {
        AtomicBoolean showing = new AtomicBoolean();
        scenario.onActivity(activity -> showing.set(noticeShown(activity)));
        return showing.get();
    }

    private static boolean noticeShown(MainActivity activity) {
        View notice = activity.findViewById(com.google.android.material.R.id.snackbar_text);
        return notice != null && notice.isShown();
    }

    private static boolean isUpdateShowing(ActivityScenario<StartupPromptTestActivity> scenario) {
        AtomicBoolean showing = new AtomicBoolean();
        scenario.onActivity(activity -> showing.set(current(activity) instanceof MessageBottomSheetDialog));
        return showing.get();
    }

    private static boolean hasDialog(ActivityScenario<StartupPromptTestActivity> scenario) {
        AtomicBoolean showing = new AtomicBoolean();
        scenario.onActivity(activity -> showing.set(current(activity) != null));
        return showing.get();
    }

    private static Dialog current(MainActivity activity) { return (Dialog) field(activity, "startupDialog"); }

    private static Object field(MainActivity activity, String name) {
        try { Field field = MainActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void set(MainActivity activity, String name, Object value) {
        try { Field field = MainActivity.class.getDeclaredField(name); field.setAccessible(true); field.set(activity, value); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static int dialogCount(MainActivity activity) {
        int count = 0;
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.isShown() && root != activity.getWindow().getDecorView()) count++;
        }
        return count;
    }

    private static void waitFor(BooleanSupplier condition) {
        long end = SystemClock.uptimeMillis() + 15000;
        do { if (condition.getAsBoolean()) return; SystemClock.sleep(50); } while (SystemClock.uptimeMillis() < end);
        fail("Startup dialog did not advance");
    }

    private static void withStartup(Body body) throws Exception {
        withStartup(false, body);
    }

    private static void withStartup(boolean backupEntry, Body body) throws Exception {
        boolean reminder = ApplicationSettings.isSettingEnabled(ApplicationSettings.SETTING_CRITICAL_UPDATE_REMINDER);
        boolean throttled = ThrottleTimer.isThrottled;
        ApplicationSettings.setSetting(ApplicationSettings.SETTING_CRITICAL_UPDATE_REMINDER, false);
        ThrottleTimer.isThrottled = false;
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), StartupPromptTestActivity.class);
        if (backupEntry) intent.setAction(Intent.ACTION_VIEW);
        try (ActivityScenario<StartupPromptTestActivity> scenario = ActivityScenario.launch(intent)) {
            waitFor(() -> hasDialog(scenario));
            // A real recent crash, if present, takes priority. Dismiss only its explanatory notice.
            scenario.onActivity(activity -> {
                if (!(boolean) field(activity, "startupChangelogChecked")) current(activity).cancel();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            waitFor(() -> hasDialog(scenario));
            if (!backupEntry) waitFor(() -> isUpdateShowing(scenario));
            body.run(scenario);
        } finally {
            ApplicationSettings.setSetting(ApplicationSettings.SETTING_CRITICAL_UPDATE_REMINDER, reminder);
            ThrottleTimer.isThrottled = throttled;
        }
    }

    private interface Body { void run(ActivityScenario<StartupPromptTestActivity> scenario) throws Exception; }
}
