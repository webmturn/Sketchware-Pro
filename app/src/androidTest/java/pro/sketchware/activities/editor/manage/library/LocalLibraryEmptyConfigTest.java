package pro.sketchware.activities.editor.manage.library;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inspector.WindowInspector;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.appbar.MaterialToolbar;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.core.library.LocalLibrariesUtil;
import pro.sketchware.core.library.ManageLocalLibrary;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.Helper;

@RunWith(AndroidJUnit4.class)
public class LocalLibraryEmptyConfigTest {
    private static final String[] EMPTY_CONTENTS = {"", " \r\n\t ", "null", "\n null\t", "\uFEFFnull"};

    @Test(timeout = 30000)
    public void buildLoaderRetainsBlankAndNullConfigurations() throws Exception {
        try (Project project = new Project()) {
            for (String content : EMPTY_CONTENTS) {
                project.write(content);
                ManageLocalLibrary libraries = new ManageLocalLibrary(project.id);
                assertNotNull(libraries.list);
                assertTrue(libraries.list.isEmpty());
                assertTrue(libraries.getAssets().isEmpty());
                assertTrue(libraries.getDexLocalLibrary().isEmpty());
                assertTrue(libraries.getImportLocalLibrary().isEmpty());
                project.assertUnchanged();
            }
        }
    }

    @Test(timeout = 30000)
    public void uiAndBackupReaderRetainsBlankAndNullConfigurations() throws Exception {
        try (Project project = new Project()) {
            for (String content : EMPTY_CONTENTS) {
                project.write(content);
                var libraries = LocalLibrariesUtil.getLocalLibraries(project.id);
                assertNotNull("UI and backup callers must receive a list", libraries);
                assertTrue(libraries.isEmpty());
                project.assertUnchanged();
            }
        }
    }

    @Test(timeout = 30000)
    public void absentConfigurationsStayAbsentAfterReads() throws Exception {
        try (Project project = new Project()) {
            assertTrue(new ManageLocalLibrary(project.id).list.isEmpty());
            assertFalse(project.config.exists());
            assertTrue(LocalLibrariesUtil.getLocalLibraries(project.id).isEmpty());
            assertFalse("Reading an optional configuration must not create it", project.config.exists());
        }
    }

    @Test(timeout = 30000)
    public void explicitEmptyArrayKeepsItsBytesAndTimestamp() throws Exception {
        try (Project project = new Project()) {
            project.write("\n [] \t");
            assertTrue(new ManageLocalLibrary(project.id).list.isEmpty());
            assertTrue(LocalLibrariesUtil.getLocalLibraries(project.id).isEmpty());
            project.assertUnchanged();
        }
    }

    @Test(timeout = 30000)
    public void malformedJsonStillWarnsAndRetainsTheFile() throws Exception {
        try (Project project = new Project();
             ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            project.write("[{broken");
            AtomicReference<ManageLocalLibrary> result = new AtomicReference<>();
            String warning = Helper.getResString(R.string.local_lib_error_invalid_config);
            AccessibilityEvent event = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeAndWaitForEvent(() -> result.set(new ManageLocalLibrary(project.id)),
                            candidate -> candidate.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                                    && candidate.getText().toString().contains(warning), 8000);
            event.recycle();
            assertNotNull(result.get());
            assertTrue(result.get().list.isEmpty());
            assertTrue(LocalLibrariesUtil.getLocalLibraries(project.id).isEmpty());
            project.assertUnchanged();
        }
    }

    @Test(timeout = 30000)
    @SdkSuppress(minSdkVersion = 29)
    public void projectLibraryScreenHandlesNullWithoutRewritingConfiguration() throws Exception {
        try (Project project = new Project()) {
            project.write("\n null \t");
            Intent intent = new Intent(context(), ManageLocalLibraryActivity.class).putExtra("sc_id", project.id);
            try (ActivityScenario<ManageLocalLibraryActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
                long deadline = SystemClock.uptimeMillis() + 10000;
                boolean[] loading = {true};
                do {
                    scenario.onActivity(activity -> {
                        loading[0] = false;
                        for (View root : WindowInspector.getGlobalWindowViews()) {
                            View animation = root.findViewById(R.id.anim_sketchware);
                            if (animation != null && animation.isShown()) loading[0] = true;
                        }
                    });
                    if (loading[0]) SystemClock.sleep(50);
                } while (loading[0] && SystemClock.uptimeMillis() < deadline);
                assertFalse("Library loading must finish", loading[0]);
                scenario.onActivity(activity -> {
                    MaterialToolbar toolbar = activity.findViewById(R.id.library_toolbar);
                    var search = toolbar.getMenu().findItem(R.id.action_search_local_libraries);
                    search.expandActionView();
                    ((SearchView) search.getActionView()).setQuery("missing_" + UUID.randomUUID(), false);
                    RecyclerView list = activity.findViewById(R.id.libraries_list);
                    assertEquals(0, list.getAdapter().getItemCount());
                    search.collapseActionView();
                    assertFalse(activity.isFinishing());
                });
            }
            project.assertUnchanged();
        }
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static final class Project implements AutoCloseable {
        final String id = "__empty_library_" + UUID.randomUUID();
        final File config = new File(SketchwarePaths.getProjectLocalLibraryPath(id));
        final Path root = config.getParentFile().toPath();
        final ArrayList<File> createdParents = new ArrayList<>();
        byte[] original;
        long modified;

        Project() throws Exception {
            assertEquals(id, root.getFileName().toString());
            assertFalse("Do not overwrite a user project", Files.exists(root));
            createParents(root.toFile().getParentFile());
            Files.createDirectory(root);
        }

        private void createParents(File directory) throws Exception {
            if (directory.isDirectory()) return;
            createParents(directory.getParentFile());
            Files.createDirectory(directory.toPath());
            createdParents.add(directory);
        }

        void write(String content) throws Exception {
            original = content.getBytes(StandardCharsets.UTF_8);
            Files.write(config.toPath(), original);
            Files.setLastModifiedTime(config.toPath(), FileTime.fromMillis(1600000000000L));
            modified = config.lastModified();
        }

        void assertUnchanged() throws Exception {
            assertTrue("Loading must not delete the source configuration", config.isFile());
            assertArrayEquals(original, Files.readAllBytes(config.toPath()));
            assertEquals("Loading must not rewrite the source configuration", modified, config.lastModified());
        }

        @Override public void close() throws Exception {
            assertEquals(id, root.getFileName().toString());
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    assertTrue(path.startsWith(root));
                    Files.delete(path);
                }
            }
            for (int i = createdParents.size() - 1; i >= 0; i--) Files.delete(createdParents.get(i).toPath());
        }
    }
}
