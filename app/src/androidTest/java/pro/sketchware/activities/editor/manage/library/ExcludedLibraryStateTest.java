package pro.sketchware.activities.editor.manage.library;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.core.library.BuiltInLibraries;
import pro.sketchware.core.library.BuiltInLibraries.BuiltInLibrary;
import pro.sketchware.core.library.ExcludeBuiltInLibrariesConfig;
import pro.sketchware.core.project.SketchwarePaths;

/** Observes the real creation/restore boundary; all configuration writes use an owned project. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class ExcludedLibraryStateTest {
    private final Application app = (Application) InstrumentationRegistry.getInstrumentation()
            .getTargetContext().getApplicationContext();
    private final BuiltInLibrary first = BuiltInLibrary.ofName(BuiltInLibraries.ANDROIDX_MULTIDEX).orElseThrow();
    private final BuiltInLibrary second = BuiltInLibrary.ofName(BuiltInLibraries.PLAY_CORE_COMMON).orElseThrow();
    private final BuiltInLibrary[] originalOrder = BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES.clone();
    private final AtomicReference<EarlyState> earlyState = new AtomicReference<>();
    private final AtomicReference<Throwable> restoreFailure = new AtomicReference<>();
    private boolean exerciseSwitchRestore, dropSavedList, parcelSavedList;
    private File projectDirectory, configFile;
    private String projectId;
    private byte[] originalConfig;
    private long originalModified;
    private ActivityScenario<ExcludeBuiltInLibrariesActivity> scenario;

    private final Application.ActivityLifecycleCallbacks observer = new Application.ActivityLifecycleCallbacks() {
        @Override public void onActivityPreCreated(Activity activity, Bundle state) {
            if (!(activity instanceof ExcludeBuiltInLibrariesActivity) || state == null) return;
            if (dropSavedList) {
                state.remove("excludedLibraryNames");
                dropSavedList = false;
            }
            if (parcelSavedList) {
                ArrayList<BuiltInLibrary> selected = state.getParcelableArrayList("excludedLibraryNames");
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.writeTypedList(selected);
                    parcel.setDataPosition(0);
                    state.putParcelableArrayList("excludedLibraryNames",
                            parcel.createTypedArrayList(BuiltInLibrary.CREATOR));
                } finally {
                    parcel.recycle();
                }
                parcelSavedList = false;
            }
        }

        @Override public void onActivityPostCreated(Activity activity, Bundle state) {
            if (!(activity instanceof ExcludeBuiltInLibrariesActivity)) return;
            try {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                List<BuiltInLibrary> libraries = selected(activity);
                earlyState.set(new EarlyState(enabled(activity), libraries == null ? null : names(libraries)));
                if (exerciseSwitchRestore) {
                    // This is the same listener path used when Android restores a checked switch.
                    ((MaterialSwitch) activity.findViewById(R.id.lib_switch)).setChecked(true);
                }
            } catch (Throwable error) {
                // Baseline failures must be reported by JUnit, not crash the application process.
                restoreFailure.set(error);
            }
        }

        @Override public void onActivityCreated(Activity a, Bundle b) {}
        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityResumed(Activity a) {}
        @Override public void onActivityPaused(Activity a) {}
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        @Override public void onActivityDestroyed(Activity a) {}
    };

    @Before public void prepare() throws Exception {
        projectId = "__excluded_library_state_" + UUID.randomUUID();
        projectDirectory = new File(SketchwarePaths.getDataPath(projectId));
        assertFalse("Never overwrite a user project", projectDirectory.exists());
        assertTrue(projectDirectory.mkdir());
        configFile = ExcludeBuiltInLibrariesConfig.getConfigPath(projectId);
        ExcludeBuiltInLibrariesConfig.saveConfig(projectId, true, List.of(first));
        Files.setLastModifiedTime(configFile.toPath(), FileTime.fromMillis(1_000_000_000_000L));
        originalConfig = Files.readAllBytes(configFile.toPath());
        originalModified = configFile.lastModified();
        app.registerActivityLifecycleCallbacks(observer);
    }

    @After public void cleanup() throws Exception {
        try {
            if (scenario != null) scenario.close();
        } finally {
            app.unregisterActivityLifecycleCallbacks(observer);
            System.arraycopy(originalOrder, 0, BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES, 0, originalOrder.length);
            if (projectDirectory != null && projectDirectory.exists()) {
                assertEquals(projectId, projectDirectory.getName());
                assertFalse(Files.isSymbolicLink(projectDirectory.toPath()));
                assertEquals(projectDirectory.getCanonicalFile(), configFile.getCanonicalFile().getParentFile());
                Files.deleteIfExists(configFile.toPath());
                Files.delete(projectDirectory.toPath());
            }
        }
    }

    @Test public void stateIsInitializedBeforeFrameworkViewRestoration() throws Exception {
        launch();
        assertReadyBeforeRestore(true, List.of(first.getName()));
        assertDiskUnchanged();
    }

    @Test public void switchRestorationCannotObserveAnUninitializedLibraryList() throws Exception {
        exerciseSwitchRestore = true;
        launch();
        assertNull("Restoring the switch after onCreate must not throw", restoreFailure.get());
        assertReadyBeforeRestore(true, List.of(first.getName()));
        assertDiskUnchanged();
    }

    @Test public void unsavedSelectionAndSwitchSurviveConsecutiveRecreations() throws Exception {
        launch();
        assertReadyBeforeRestore(true, List.of(first.getName()));
        pick(second, true);
        check(activity -> activity.findViewById(R.id.layout_switch_card).performClick());
        for (int i = 0; i < 2; i++) {
            scenario.recreate();
            settle();
            check(activity -> {
                assertFalse(enabled(activity));
                assertFalse(((MaterialSwitch) activity.findViewById(R.id.lib_switch)).isChecked());
                assertEquals(2, selected(activity).size());
                assertTrue(names(selected(activity)).containsAll(List.of(first.getName(), second.getName())));
                assertEquals(View.GONE, activity.findViewById(R.id.actual_content).getVisibility());
            });
            assertDiskUnchanged();
        }
        check(activity -> {
            activity.findViewById(R.id.layout_switch_card).performClick();
            assertTrue(enabled(activity));
            String description = ((TextView) activity.findViewById(R.id.item_desc)).getText().toString();
            assertTrue(description.contains(first.getName()));
            assertTrue(description.contains(second.getName()));
        });
        assertDiskUnchanged();
    }

    @Test public void savedStateWithoutLibraryListUsesAnEmptySelection() throws Exception {
        launch();
        assertReadyBeforeRestore(true, List.of(first.getName()));
        dropSavedList = true;
        scenario.recreate();
        settle();
        assertReadyBeforeRestore(true, List.of());
        check(activity -> {
            assertTrue(enabled(activity));
            assertTrue(selected(activity).isEmpty());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.no_content).getVisibility());
        });
        assertDiskUnchanged();
    }

    @Test public void missingConfigurationOpensDisabledWithoutCreatingAFile() throws Exception {
        Files.delete(configFile.toPath());
        launch();
        assertReadyBeforeRestore(false, List.of());
        check(activity -> {
            assertFalse(((MaterialSwitch) activity.findViewById(R.id.lib_switch)).isChecked());
            assertTrue(selected(activity).isEmpty());
        });
        assertFalse(configFile.exists());
    }

    @Test public void parcelledSelectionRemainsCheckedAndUnchangedBackDoesNotWrite() throws Exception {
        launch();
        assertReadyBeforeRestore(true, List.of(first.getName()));
        parcelSavedList = true;
        scenario.recreate();
        settle();
        showPicker(first);
        check(activity -> {
            RecyclerView list = dialogView(R.id.recyclerView);
            assertTrue("A selection restored from Parcel must still be checked in the picker",
                    ((CheckBox) list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.chk_select)).isChecked());
            dialogView(android.R.id.button2).performClick();
            activity.getOnBackPressedDispatcher().onBackPressed();
        });
        waitUntil(() -> scenario.getState() == Lifecycle.State.DESTROYED);
        assertDiskUnchanged();
    }

    @Test public void changedRestoredStateIsSavedOnlyWhenLeaving() throws Exception {
        launch();
        assertReadyBeforeRestore(true, List.of(first.getName()));
        check(activity -> activity.findViewById(R.id.layout_switch_card).performClick());
        scenario.recreate();
        settle();
        check(activity -> assertFalse(enabled(activity)));
        assertDiskUnchanged();
        check(activity -> activity.getOnBackPressedDispatcher().onBackPressed());
        waitUntil(() -> scenario.getState() == Lifecycle.State.DESTROYED);
        var saved = ExcludeBuiltInLibrariesConfig.readConfig(projectId);
        assertNotNull(saved);
        assertFalse(saved.first);
        assertEquals(List.of(first.getName()), names(saved.second));
    }

    private void launch() {
        scenario = ActivityScenario.launch(new Intent(app, ExcludeBuiltInLibrariesActivity.class)
                .putExtra("sc_id", projectId));
        settle();
    }

    private void assertReadyBeforeRestore(boolean expectedEnabled, List<String> expectedNames) {
        EarlyState state = earlyState.get();
        assertNotNull("Creation observer must run before framework state restoration", state);
        assertNotNull("Library selection must be initialized before Android restores view state", state.names);
        assertEquals(expectedEnabled, state.enabled);
        assertEquals(expectedNames, state.names);
    }

    private void pick(BuiltInLibrary library, boolean selected) throws Exception {
        showPicker(library);
        check(activity -> {
            RecyclerView list = dialogView(R.id.recyclerView);
            CheckBox checkbox = list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.chk_select);
            assertNotEquals(selected, checkbox.isChecked());
            checkbox.performClick();
            dialogView(android.R.id.button1).performClick();
        });
        settle();
    }

    private void showPicker(BuiltInLibrary library) throws Exception {
        check(activity -> activity.findViewById(R.id.exclude_library).performClick());
        settle();
        check(activity -> ((EditText) dialogView(R.id.searchInput)).setText(library.getName()));
        settle();
        check(activity -> {
            RecyclerView list = dialogView(R.id.recyclerView);
            assertEquals(1, list.getAdapter().getItemCount());
            assertNotNull(list.findViewHolderForAdapterPosition(0));
        });
    }

    private void assertDiskUnchanged() throws Exception {
        assertArrayEquals(originalConfig, Files.readAllBytes(configFile.toPath()));
        assertEquals("Opening and restoring must not rewrite configuration", originalModified, configFile.lastModified());
    }

    private static boolean enabled(Activity activity) throws Exception {
        return (boolean) field(activity, "isExcludingEnabled");
    }

    @SuppressWarnings("unchecked")
    private static List<BuiltInLibrary> selected(Activity activity) throws Exception {
        return (List<BuiltInLibrary>) field(activity, "excludedLibraries");
    }

    private static Object field(Activity activity, String name) throws Exception {
        Field field = ExcludeBuiltInLibrariesActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(activity);
    }

    private static List<String> names(List<BuiltInLibrary> libraries) {
        ArrayList<String> names = new ArrayList<>();
        for (BuiltInLibrary library : libraries) names.add(library.getName());
        return names;
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T dialogView(int id) {
        for (View root : WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.searchInput) != null && root.findViewById(id) != null) {
                return (T) root.findViewById(id);
            }
        }
        throw new AssertionError("Library picker control is missing: " + id);
    }

    private static void settle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private interface Check { void run(ExcludeBuiltInLibrariesActivity activity) throws Exception; }

    private void check(Check check) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        scenario.onActivity(activity -> {
            try { check.run(activity); }
            catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Exception exception) throw exception;
        if (error != null) throw new AssertionError(error);
    }

    private interface Condition { boolean ready() throws Exception; }

    private static void waitUntil(Condition condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.ready()) return;
            Thread.sleep(50);
        }
        fail("Library settings operation did not complete");
    }

    private static final class EarlyState {
        final boolean enabled;
        final List<String> names;
        EarlyState(boolean enabled, List<String> names) { this.enabled = enabled; this.names = names; }
    }
}
