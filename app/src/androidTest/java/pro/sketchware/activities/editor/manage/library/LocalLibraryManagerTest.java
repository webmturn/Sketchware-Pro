package pro.sketchware.activities.editor.manage.library;

import static org.junit.Assert.*;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.core.library.LocalLibrariesUtil;
import pro.sketchware.core.library.LocalLibrary;
import pro.sketchware.core.project.SketchwarePaths;

/** Tests browsing and project controls with disposable private library/project fixtures. */
@RunWith(AndroidJUnit4.class)
public class LocalLibraryManagerTest {
    private static final String PREFIX = "__local_library_ui_review";
    private static final String ROOT_FOLDER = PREFIX + "-root-v1";
    private static final String CUSTOM_FOLDER = PREFIX + "_custom_V_2.0";
    private static final String OTHER_FOLDER = PREFIX + "-other-v3";
    private static final String SC_ID = "__local_library_ui_review_project";
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final List<File> fixtureFiles = new ArrayList<>(), fixtureDirs = new ArrayList<>();
    private File fallbackRoot, projectDir, projectLibraries;
    private boolean createdFallbackRoot, createdProjectDir;
    private ActivityScenario<ManageLocalLibraryActivity> scenario;

    @Before public void prepare() throws Exception {
        fallbackRoot = new File(context.getExternalFilesDir(null), "local_libs");
        createdFallbackRoot = !fallbackRoot.exists();
        for (String folder : List.of(ROOT_FOLDER, CUSTOM_FOLDER, OTHER_FOLDER)) {
            assertFalse("Do not overwrite a shared library", new File(SketchwarePaths.getLocalLibsDir(), folder).exists());
            File dir = new File(fallbackRoot, folder);
            assertFalse("Do not overwrite a private library", dir.exists());
            assertTrue(dir.mkdirs());
            fixtureDirs.add(dir);
            write(new File(dir, "fixture.txt"), "library fixture");
        }
        write(new File(fixtureDirs.get(0), "maven-coordinate"), "review:" + PREFIX + "-root:1");
        write(new File(fixtureDirs.get(0), "dependency-tree.json"),
                "[{\"groupId\":\"review\",\"artifactId\":\"review-builtin\",\"version\":\"1\","
                + "\"builtIn\":true,\"parent\":\"review:" + PREFIX + "-root:1\"}]");
        write(new File(fixtureDirs.get(1), "maven-coordinate"), "review:renamed-custom:2.0");
        write(new File(fixtureDirs.get(2), "maven-coordinate"), "review:" + PREFIX + "-other:3");
        projectLibraries = LocalLibrariesUtil.getLocalLibFile(SC_ID);
        projectDir = projectLibraries.getParentFile();
        assertFalse("Do not overwrite an existing project", projectDir.exists());
        assertTrue(projectDir.mkdirs());
        createdProjectDir = true;
        write(projectLibraries, "[]");
    }

    @After public void restore() throws Exception {
        if (scenario != null) scenario.close();
        for (File file : fixtureFiles) Files.deleteIfExists(file.toPath());
        for (File dir : fixtureDirs) Files.deleteIfExists(dir.toPath());
        if (createdFallbackRoot) Files.deleteIfExists(fallbackRoot.toPath());
        if (createdProjectDir) Files.deleteIfExists(projectDir.toPath());
    }

    @Test public void globalRowsPreserveCustomNamesAndKeepDownloadCentered() {
        launch(false);
        search(ROOT_FOLDER, 1);
        scenario.onActivity(a -> {
            assertEquals(a.getString(R.string.local_library_manager_title), toolbar(a).getTitle());
            View row = row(a, 0);
            assertEquals(PREFIX + "-root  1", ((TextView) row.findViewById(R.id.library_name)).getText().toString());
            assertEquals(View.GONE, row.findViewById(R.id.material_switch).getVisibility());
            assertEquals(View.VISIBLE, row.findViewById(R.id.library_chevron).getVisibility());
            String metadata = ((TextView) row.findViewById(R.id.library_size)).getText().toString().replace("\u2060", "");
            assertTrue(metadata.contains(a.getResources().getQuantityString(R.plurals.local_library_manager_dependencies, 1, 1)));
            View button = a.findViewById(R.id.download_library_button);
            View parent = (View) button.getParent();
            assertEquals(parent.getWidth() / 2f, (button.getLeft() + button.getRight()) / 2f, 2f);
            assertTrue(button.getTop() > parent.getHeight() / 2);
        });
        search(CUSTOM_FOLDER, 1);
        scenario.onActivity(a -> assertEquals(CUSTOM_FOLDER,
                ((TextView) row(a, 0).findViewById(R.id.library_name)).getText().toString()));
    }

    @Test public void tappingFilteredLibraryTextOpensDependencies() {
        launch(false);
        search(ROOT_FOLDER, 1);
        openAndCloseDependencies("system");
    }

    @Test public void projectRowTapDoesNotToggleButIndependentSwitchPersists() throws Exception {
        launch(true);
        search(ROOT_FOLDER, 1);
        openAndCloseDependencies(SC_ID);
        assertEquals("[]", new String(Files.readAllBytes(projectLibraries.toPath()), StandardCharsets.UTF_8));
        scenario.onActivity(a -> {
            MaterialSwitch toggle = row(a, 0).findViewById(R.id.material_switch);
            assertEquals(View.VISIBLE, toggle.getVisibility());
            assertFalse(toggle.isChecked());
            toggle.performClick();
        });
        settle();
        assertTrue(new String(Files.readAllBytes(projectLibraries.toPath()), StandardCharsets.UTF_8).contains(ROOT_FOLDER));
        scenario.onActivity(a -> assertTrue(((MaterialSwitch) row(a, 0).findViewById(R.id.material_switch)).isChecked()));
    }

    @Test public void filteredMultiSelectionKeepsQueryAndSelectsOnlyVisibleLibraries() {
        launch(false);
        search(PREFIX + "-", 2);
        scenario.onActivity(a -> assertTrue(row(a, 0).findViewById(R.id.card).performLongClick()));
        settle();
        scenario.onActivity(a -> {
            assertEquals(View.VISIBLE, a.findViewById(R.id.contextual_toolbar_container).getVisibility());
            MaterialToolbar contextual = a.findViewById(R.id.contextual_toolbar);
            contextual.getMenu().performIdentifierAction(R.id.action_select_all, 0);
            assertEquals("2", contextual.getTitle().toString());
            ManageLocalLibraryActivity.LibraryAdapter adapter = (ManageLocalLibraryActivity.LibraryAdapter) list(a).getAdapter();
            assertFalse(adapter.getLocalLibraries().stream().filter(lib -> lib.getName().equals(CUSTOM_FOLDER)).findFirst().get().isSelected());
            a.getOnBackPressedDispatcher().onBackPressed();
            assertEquals(PREFIX + "-", searchView(a).getQuery().toString());
            assertEquals(2, list(a).getAdapter().getItemCount());
            assertTrue(adapter.getLocalLibraries().stream().noneMatch(LocalLibrary::isSelected));
        });
        settle();
        scenario.onActivity(a -> assertFalse(((MaterialCardView) row(a, 0).findViewById(R.id.card)).isChecked()));
    }

    @Test public void searchSurvivesRecreationAndClearsEmptyResults() {
        launch(false);
        search(ROOT_FOLDER, 1);
        scenario.recreate();
        awaitRows(1);
        scenario.onActivity(a -> {
            assertTrue(toolbar(a).getMenu().findItem(R.id.action_search_local_libraries).isActionViewExpanded());
            assertEquals(ROOT_FOLDER, searchView(a).getQuery().toString());
        });
        search(PREFIX + "_does_not_exist", 0);
        scenario.onActivity(a -> {
            assertEquals(View.VISIBLE, a.findViewById(R.id.no_content_layout).getVisibility());
            assertEquals(a.getString(R.string.local_library_manager_no_matches),
                    ((TextView) a.findViewById(R.id.library_empty_title)).getText().toString());
            toolbar(a).getMenu().findItem(R.id.action_search_local_libraries).collapseActionView();
        });
        settle();
        scenario.onActivity(a -> {
            assertTrue(list(a).getAdapter().getItemCount() >= 3);
            assertEquals(View.GONE, a.findViewById(R.id.no_content_layout).getVisibility());
        });
    }

    private void write(File file, String text) throws Exception {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        fixtureFiles.add(file);
    }
    private void launch(boolean project) {
        scenario = ActivityScenario.launch(new Intent(context, ManageLocalLibraryActivity.class).putExtra("sc_id", project ? SC_ID : "system"));
        scenario.onActivity(a -> a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] loaded = {false};
            scenario.onActivity(a -> loaded[0] = ((ManageLocalLibraryActivity.LibraryAdapter) list(a).getAdapter()).getLocalLibraries().size() >= 3);
            if (loaded[0]) { settle(); return; }
            SystemClock.sleep(30);
        }
        fail("Libraries did not load");
    }
    private void search(String query, int expectedRows) {
        scenario.onActivity(a -> {
            toolbar(a).getMenu().findItem(R.id.action_search_local_libraries).expandActionView();
            searchView(a).setQuery(query, false);
            list(a).scrollToPosition(0);
        });
        awaitRows(expectedRows);
    }
    private void awaitRows(int count) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] ready = {false};
            scenario.onActivity(a -> ready[0] = list(a).getAdapter().getItemCount() == count
                    && ((ManageLocalLibraryActivity.LibraryAdapter) list(a).getAdapter()).getLocalLibraries().size() >= 3
                    && !list(a).hasPendingAdapterUpdates()
                    && (count == 0 || list(a).findViewHolderForAdapterPosition(0) != null));
            if (ready[0]) { settle(); return; }
            SystemClock.sleep(30);
        }
        fail("Unexpected filtered row count");
    }
    private void openAndCloseDependencies(String expectedProject) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(SubDependenciesActivity.class.getName(), null, false);
        try {
            int[] point = new int[2];
            scenario.onActivity(a -> {
                View text = row(a, 0).findViewById(R.id.library_name);
                text.getLocationOnScreen(point);
                point[0] += text.getWidth() / 2;
                point[1] += text.getHeight() / 2;
            });
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, point[0], point[1], 0);
            MotionEvent up = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, point[0], point[1], 0);
            instrumentation.sendPointerSync(down);
            instrumentation.sendPointerSync(up);
            down.recycle(); up.recycle();
            android.app.Activity child = instrumentation.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull("Tapping library text should navigate to dependencies", child);
            assertEquals(ROOT_FOLDER, child.getIntent().getStringExtra(SubDependenciesActivity.EXTRA_ROOT_LIB_NAME));
            assertEquals(expectedProject, child.getIntent().getStringExtra(SubDependenciesActivity.EXTRA_SC_ID));
            instrumentation.runOnMainSync(child::finish);
            settle();
        } finally { instrumentation.removeMonitor(monitor); }
    }
    private static RecyclerView list(ManageLocalLibraryActivity a) { return a.findViewById(R.id.libraries_list); }
    private static View row(ManageLocalLibraryActivity a, int position) { return list(a).findViewHolderForAdapterPosition(position).itemView; }
    private static MaterialToolbar toolbar(ManageLocalLibraryActivity a) { return a.findViewById(R.id.library_toolbar); }
    private static SearchView searchView(ManageLocalLibraryActivity a) { return (SearchView) toolbar(a).getMenu().findItem(R.id.action_search_local_libraries).getActionView(); }
    private void settle() { instrumentation.waitForIdleSync(); }
}
