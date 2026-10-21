package pro.sketchware.core.library;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pro.sketchware.core.library.BuiltInLibraries.BuiltInLibrary;
import pro.sketchware.core.project.SketchwarePaths;

/** Checks the actual build dependency selection against persisted, disposable project settings. */
@RunWith(AndroidJUnit4.class)
public class BuiltInLibraryExclusionTest {
    private File projectDirectory, configFile;
    private String projectId;
    private byte[] expectedBytes;
    private long expectedModified;

    @Before public void prepare() throws Exception {
        projectId = "__builtin_exclusion_" + UUID.randomUUID();
        projectDirectory = new File(SketchwarePaths.getDataPath(projectId));
        assertFalse("Never overwrite an existing project", projectDirectory.exists());
        assertTrue(projectDirectory.mkdir());
        configFile = ExcludeBuiltInLibrariesConfig.getConfigPath(projectId);
    }

    @After public void cleanup() throws Exception {
        if (projectDirectory != null && projectDirectory.exists()) {
            assertEquals(projectId, projectDirectory.getName());
            assertFalse(Files.isSymbolicLink(projectDirectory.toPath()));
            assertEquals(projectDirectory.getCanonicalFile(), configFile.getCanonicalFile().getParentFile());
            Files.deleteIfExists(configFile.toPath());
            Files.delete(projectDirectory.toPath());
        }
    }

    @Test public void disabledExclusionIncludesTheExplicitlyRequestedLibrary() throws Exception {
        configure(false, BuiltInLibraries.GSON);
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.GSON);
        assertTrue("Disabling exclusions must restore the requested built-in library",
                manager.containsLibrary(BuiltInLibraries.GSON));
        assertConfigUnchanged();
    }

    @Test public void disabledExclusionPreservesTheNormalTransitiveDependencyList() throws Exception {
        List<String> normal = names(resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertTrue(normal.contains(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertTrue("Fixture must include the transitive library", normal.contains(BuiltInLibraries.ANDROIDX_CORE));
        configure(false, BuiltInLibraries.ANDROIDX_APPCOMPAT, BuiltInLibraries.ANDROIDX_CORE);
        assertEquals("A disabled saved list must not alter build dependency selection", normal,
                names(resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT)));
        assertConfigUnchanged();
    }

    @Test public void enabledExclusionOmitsTheRootButRetainsItsDependencies() throws Exception {
        configure(true, BuiltInLibraries.ANDROIDX_APPCOMPAT);
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        assertFalse(manager.containsLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertTrue("Excluding a library must still expand its required dependencies",
                manager.containsLibrary(BuiltInLibraries.ANDROIDX_CORE));
        assertConfigUnchanged();
    }

    @Test public void enabledExclusionOmitsATransitiveLibraryButRetainsItsChildren() throws Exception {
        configure(true, BuiltInLibraries.ANDROIDX_CORE);
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        assertTrue(manager.containsLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertFalse(manager.containsLibrary(BuiltInLibraries.ANDROIDX_CORE));
        String[] children = BuiltInLibraryUtils.getKnownDependencies(BuiltInLibraries.ANDROIDX_CORE);
        assertTrue("Fixture must exercise an excluded library with children", children.length > 0);
        for (String child : children) assertTrue(child, manager.containsLibrary(child));
        assertConfigUnchanged();
    }

    @Test public void missingConfigurationDoesNotExcludeOrCreateAConfigFile() {
        assertFalse(configFile.exists());
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        assertTrue(manager.containsLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertTrue(manager.containsLibrary(BuiltInLibraries.ANDROIDX_CORE));
        assertFalse("Dependency selection must not create an optional config file", configFile.exists());
    }

    @Test public void subsequentBuildsHonorFlagChangesWhileRetainingTheSavedList() throws Exception {
        for (boolean enabled : new boolean[]{true, false, true}) {
            configure(enabled, BuiltInLibraries.GSON);
            BuiltInLibraryManager manager = resolve(BuiltInLibraries.GSON);
            assertEquals("Each build must honor the current exclusion flag", !enabled,
                    manager.containsLibrary(BuiltInLibraries.GSON));
            var config = ExcludeBuiltInLibrariesConfig.readConfig(projectId);
            assertNotNull(config);
            assertEquals(Boolean.valueOf(enabled), config.first);
            assertEquals(1, config.second.size());
            assertEquals(BuiltInLibraries.GSON, config.second.get(0).getName());
            assertConfigUnchanged();
        }
    }

    private void configure(boolean enabled, String... names) throws Exception {
        ArrayList<BuiltInLibrary> selection = new ArrayList<>();
        for (String name : names) selection.add(BuiltInLibrary.ofName(name).orElseThrow());
        ExcludeBuiltInLibrariesConfig.saveConfig(projectId, enabled, selection);
        Files.setLastModifiedTime(configFile.toPath(), FileTime.fromMillis(1_000_000_000_000L));
        expectedBytes = Files.readAllBytes(configFile.toPath());
        expectedModified = configFile.lastModified();
    }

    private BuiltInLibraryManager resolve(String root) {
        BuiltInLibraryManager manager = new BuiltInLibraryManager(projectId);
        manager.addLibrary(root);
        return manager;
    }

    private static List<String> names(BuiltInLibraryManager manager) {
        ArrayList<String> names = new ArrayList<>();
        for (pro.sketchware.core.library.BuiltInLibrary library : manager.getLibraries()) names.add(library.getName());
        return names;
    }

    private void assertConfigUnchanged() throws Exception {
        assertArrayEquals(expectedBytes, Files.readAllBytes(configFile.toPath()));
        assertEquals("Selecting build inputs must not rewrite exclusion settings", expectedModified,
                configFile.lastModified());
    }
}
