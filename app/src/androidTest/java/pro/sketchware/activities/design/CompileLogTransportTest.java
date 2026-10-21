package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.tools.CompileLogActivity;
import pro.sketchware.core.build.CompileErrorSaver;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.UIHelper;

/** Actual error navigation and viewer state; all logs belong to disposable projects. */
@RunWith(AndroidJUnit4.class)
public class CompileLogTransportTest {
    private static final String LARGE_LOG = "开始\nerror: synthetic diagnostic\n"
            + "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.\n".repeat(16384)
            + "末尾 diagnostic preserved\n";
    private SharedPreferences preferences;
    private boolean hadWrap, wrap, hadMono, mono;
    private long previousClickTime;

    @Before public void preserveViewerPreferences() {
        String name = CompileLogActivity.class.getName().substring(context().getPackageName().length() + 1);
        preferences = context().getSharedPreferences(name, Context.MODE_PRIVATE);
        hadWrap = preferences.contains("wrapped_text");
        wrap = preferences.getBoolean("wrapped_text", false);
        hadMono = preferences.contains("use_monospaced_font");
        mono = preferences.getBoolean("use_monospaced_font", true);
        previousClickTime = UIHelper.lastClickTime;
    }

    @After public void restoreViewerPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        if (hadWrap) editor.putBoolean("wrapped_text", wrap); else editor.remove("wrapped_text");
        if (hadMono) editor.putBoolean("use_monospaced_font", mono); else editor.remove("use_monospaced_font");
        assertTrue(editor.commit());
        UIHelper.lastClickTime = previousClickTime;
    }

    @Test public void largeCompileErrorNavigatesWithOnlyAProjectReference() throws Exception {
        try (var project = project(); var scenario = project.launch(); var capture = new NavigationCapture()) {
            readyEditor(scenario);
            Intent intent = errorNavigation(scenario, capture, LARGE_LOG);
            int bytes = intentBytes(intent);
            status("compile_log_navigation_bytes", bytes);
            assertEquals(project.id, intent.getStringExtra("sc_id"));
            assertEquals(CompileLogActivity.class.getName(), intent.getComponent().getClassName());
            assertEquals(Intent.FLAG_ACTIVITY_SINGLE_TOP, intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP);
            assertFalse("Log bodies must not be sent through Activity navigation", intent.hasExtra("error"));
            assertTrue("The navigation payload must stay small even for a large log", bytes < 4096);
            assertEquals(LARGE_LOG, new CompileErrorSaver(project.id).getLogsFromFile());
        }
    }

    @Test public void navigationPayloadDoesNotGrowWithTheLogBody() throws Exception {
        try (var project = project(); var scenario = project.launch(); var capture = new NavigationCapture()) {
            readyEditor(scenario);
            int smallBytes = intentBytes(errorNavigation(scenario, capture, "small error"));
            int largeBytes = intentBytes(errorNavigation(scenario, capture, LARGE_LOG));
            status("compile_log_small_intent_bytes", smallBytes);
            status("compile_log_large_intent_bytes", largeBytes);
            assertEquals("Only the project reference should travel, independent of body size", smallBytes, largeBytes);
            assertEquals(LARGE_LOG, new CompileErrorSaver(project.id).getLogsFromFile());
        }
    }

    @Test public void viewerUsesTheSavedLogRatherThanAnOutdatedLegacyExtra() throws Exception {
        try (var project = project()) {
            String current = "error: 最新的磁盘日志\nsecond line\n";
            CompileErrorSaver saver = new CompileErrorSaver(project.id);
            saver.writeLogsToFile(current);
            try (var viewer = ActivityScenario.<CompileLogActivity>launch(viewerIntent(project.id)
                    .putExtra("error", "outdated legacy body"))) {
                check(viewer, activity -> {
                    keepAwake(activity);
                    assertEquals(current, log(activity).getText().toString());
                });
                viewer.recreate();
                check(viewer, activity -> assertEquals(current, log(activity).getText().toString()));
                assertEquals(current, saver.getLogsFromFile());
            }
        }
    }

    @Test public void selectedLargeLogKeepsSavedStateSmallAndReloadsAfterRecreation() throws Exception {
        try (var project = project()) {
            CompileErrorSaver saver = new CompileErrorSaver(project.id);
            saver.writeLogsToFile(LARGE_LOG);
            try (var viewer = ActivityScenario.<CompileLogActivity>launch(viewerIntent(project.id))) {
                Bundle state = new Bundle();
                check(viewer, activity -> {
                    keepAwake(activity);
                    assertEquals(LARGE_LOG, log(activity).getText().toString());
                    assertTrue(log(activity).getText() instanceof Spannable);
                    Selection.setSelection((Spannable) log(activity).getText(), 0, 12);
                    instrumentation().callActivityOnSaveInstanceState(activity, state);
                });
                int bytes = bundleBytes(state);
                status("compile_log_selected_saved_state_bytes", bytes);
                assertTrue("Selected log text must not be copied into the Activity saved state", bytes < 65536);
                viewer.recreate();
                check(viewer, activity -> {
                    keepAwake(activity);
                    assertEquals(LARGE_LOG, log(activity).getText().toString());
                    assertTrue(log(activity).isTextSelectable());
                    assertEquals(0, log(activity).getSelectionStart());
                    assertEquals(12, log(activity).getSelectionEnd());
                });
                assertEquals(LARGE_LOG, saver.getLogsFromFile());
            }
        }
    }

    @Test public void clearingTheLogRemainsEmptyAfterRecreation() throws Exception {
        try (var project = project()) {
            CompileErrorSaver saver = new CompileErrorSaver(project.id);
            saver.writeLogsToFile("error: disposable log\n");
            try (var viewer = ActivityScenario.<CompileLogActivity>launch(viewerIntent(project.id))) {
                check(viewer, activity -> {
                    keepAwake(activity);
                    assertEquals("error: disposable log\n", log(activity).getText().toString());
                    activity.findViewById(R.id.clearButton).performClick();
                    assertEquals(View.VISIBLE, activity.findViewById(R.id.noContentLayout).getVisibility());
                    assertEquals(View.GONE, activity.findViewById(R.id.optionsLayout).getVisibility());
                });
                assertFalse(saver.logFileExists());
                viewer.recreate();
                check(viewer, activity -> assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.noContentLayout).getVisibility()));
                assertFalse(saver.logFileExists());
            }
        }
    }

    @Test public void missingLogShowsAnEmptyPageWithoutCreatingAFile() throws Exception {
        try (var project = project(); var viewer = ActivityScenario.<CompileLogActivity>launch(
                viewerIntent(project.id).putExtra("showingLastError", true))) {
            check(viewer, activity -> {
                keepAwake(activity);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.noContentLayout).getVisibility());
                assertEquals(View.GONE, activity.findViewById(R.id.optionsLayout).getVisibility());
                assertEquals(activity.getString(R.string.compile_log_title_last),
                        ((com.google.android.material.appbar.MaterialToolbar) activity.findViewById(R.id.topAppBar))
                                .getTitle().toString());
            });
            assertFalse(Files.exists(java.nio.file.Paths.get(SketchwarePaths.getProjectCompileLogPath(project.id))));
        }
    }

    private static ProjectEditorPersistenceTest.Fixture project() throws Exception {
        var project = new ProjectEditorPersistenceTest.Fixture();
        try { project.seed(false, false); return project; }
        catch (Exception | AssertionError failure) { project.close(); throw failure; }
    }

    private static Intent viewerIntent(String projectId) {
        return new Intent(context(), CompileLogActivity.class).putExtra("sc_id", projectId);
    }

    private static void readyEditor(ActivityScenario<DesignActivity> scenario) throws Exception {
        check(scenario, CompileLogTransportTest::keepAwake);
        ProjectEditorPersistenceTest.waitForEditor(scenario);
    }

    private static Intent errorNavigation(ActivityScenario<DesignActivity> scenario,
                                          NavigationCapture capture, String error) throws Exception {
        capture.intent.set(null);
        check(scenario, activity -> activity.indicateCompileErrorOccurred(error));
        AtomicReference<View> shownAction = new AtomicReference<>();
        ProjectEditorPersistenceTest.waitUntil(() -> {
            check(scenario, activity -> {
                View action = activity.findViewById(com.google.android.material.R.id.snackbar_action);
                if (action != null && action.isShown()) shownAction.set(action);
            });
            return shownAction.get() != null;
        });
        check(scenario, activity -> {
            UIHelper.lastClickTime = 0;
            assertTrue(shownAction.get().performClick());
        });
        assertNotNull("The Show action must navigate to the compile log", capture.intent.get());
        ProjectEditorPersistenceTest.waitUntil(() -> {
            AtomicReference<Boolean> detached = new AtomicReference<>(false);
            check(scenario, activity -> detached.set(!shownAction.get().isAttachedToWindow()));
            return detached.get();
        });
        return capture.intent.get();
    }

    private static final class NavigationCapture implements AutoCloseable {
        final AtomicReference<Intent> intent = new AtomicReference<>();
        final Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent target) {
                if (target.getComponent() != null
                        && CompileLogActivity.class.getName().equals(target.getComponent().getClassName())) {
                    intent.set(new Intent(target));
                    // Inspect the real navigation before Binder, avoiding a baseline process crash.
                    return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null);
                }
                return null;
            }
        };
        NavigationCapture() { instrumentation().addMonitor(monitor); }
        @Override public void close() { instrumentation().removeMonitor(monitor); }
    }

    private static TextView log(CompileLogActivity activity) { return activity.findViewById(R.id.tv_compile_log); }
    private static void keepAwake(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    private static Context context() { return instrumentation().getTargetContext(); }
    private static Instrumentation instrumentation() { return InstrumentationRegistry.getInstrumentation(); }

    private static int intentBytes(Intent intent) {
        Parcel parcel = Parcel.obtain();
        try { intent.writeToParcel(parcel, 0); return parcel.dataSize(); }
        finally { parcel.recycle(); }
    }

    private static int bundleBytes(Bundle state) {
        Parcel parcel = Parcel.obtain();
        try { state.writeToParcel(parcel, 0); return parcel.dataSize(); }
        finally { parcel.recycle(); }
    }

    private static void status(String key, int value) {
        Bundle status = new Bundle();
        status.putInt(key, value);
        instrumentation().sendStatus(0, status);
    }

    private interface Check<T extends Activity> { void run(T activity) throws Exception; }
    private static <T extends Activity> void check(ActivityScenario<T> scenario, Check<T> check) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        scenario.onActivity(activity -> {
            try { check.run(activity); }
            catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Exception exception) throw exception;
        if (error != null) throw new AssertionError(error);
    }
}
