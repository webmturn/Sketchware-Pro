package pro.sketchware.core.library;

import static org.junit.Assert.*;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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
    private final BuiltInLibrary[] originalCatalog = BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES.clone();

    @Before public void prepare() throws Exception {
        projectId = "__builtin_exclusion_" + UUID.randomUUID();
        projectDirectory = new File(SketchwarePaths.getDataPath(projectId));
        assertFalse("Never overwrite an existing project", projectDirectory.exists());
        assertTrue(projectDirectory.mkdir());
        configFile = ExcludeBuiltInLibrariesConfig.getConfigPath(projectId);
    }

    @After public void cleanup() throws Exception {
        try {
            if (projectDirectory != null && projectDirectory.exists()) {
                assertEquals(projectId, projectDirectory.getName());
                assertFalse(Files.isSymbolicLink(projectDirectory.toPath()));
                assertEquals(projectDirectory.getCanonicalFile(), configFile.getCanonicalFile().getParentFile());
                Files.deleteIfExists(configFile.toPath());
                Files.delete(projectDirectory.toPath());
            }
        } finally {
            System.arraycopy(originalCatalog, 0, BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES,
                    0, originalCatalog.length);
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

    @Test public void mutuallyExcludedRealLibrariesDoNotCauseUnboundedRecursion() throws Exception {
        assertTrue(List.of(BuiltInLibraryUtils.getKnownDependencies(BuiltInLibraries.ANDROIDX_CORE))
                .contains(BuiltInLibraries.ANDROIDX_CORE_KTX));
        assertTrue(List.of(BuiltInLibraryUtils.getKnownDependencies(BuiltInLibraries.ANDROIDX_CORE_KTX))
                .contains(BuiltInLibraries.ANDROIDX_CORE));
        List<String> expected = names(resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        expected.removeAll(List.of(BuiltInLibraries.ANDROIDX_CORE, BuiltInLibraries.ANDROIDX_CORE_KTX));
        configure(true, BuiltInLibraries.ANDROIDX_CORE, BuiltInLibraries.ANDROIDX_CORE_KTX);
        BuiltInLibraryManager manager;
        try {
            manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        } catch (StackOverflowError expectedBeforeFix) {
            status("excluded_dependency_cycle_overflow", "captured");
            fail("The real core/core-ktx exclusion cycle must terminate without overflowing the build thread");
            return;
        }
        assertEquals("Only the selected libraries should be removed; dependency order must remain stable",
                expected, names(manager));
        assertConfigUnchanged();
    }

    @Test public void sharedExcludedDependencyIsExpandedOnlyOnce() throws Exception {
        List<String> expected = names(resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        expected.remove(BuiltInLibraries.ANDROIDX_CORE);
        CountingLibrary core = observe(BuiltInLibraries.ANDROIDX_CORE);
        configure(true, BuiltInLibraries.ANDROIDX_CORE);
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        status("excluded_core_expansions", Integer.toString(core.expansions));
        assertEquals(expected, names(manager));
        assertEquals("An excluded common dependency must not be expanded for every incoming path", 1,
                core.expansions);
        assertConfigUnchanged();
    }

    @Test public void requestingAnExcludedRootAgainDoesNotExpandItsDependenciesAgain() throws Exception {
        CountingLibrary root = observe(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        configure(true, BuiltInLibraries.ANDROIDX_APPCOMPAT);
        BuiltInLibraryManager manager = resolve(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        List<String> firstResult = names(manager);
        manager.addLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT);
        status("excluded_root_expansions", Integer.toString(root.expansions));
        assertEquals(firstResult, names(manager));
        assertFalse(manager.containsLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT));
        assertEquals("Repeated roots must preserve the result without repeating the excluded subtree", 1,
                root.expansions);
        assertConfigUnchanged();
    }

    private CountingLibrary observe(String name) {
        for (int i = 0; i < BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES.length; i++) {
            BuiltInLibrary original = BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES[i];
            if (name.equals(original.getName())) {
                CountingLibrary counted = new CountingLibrary(original);
                BuiltInLibraries.KNOWN_BUILT_IN_LIBRARIES[i] = counted;
                return counted;
            }
        }
        throw new AssertionError("The real catalog entry is missing: " + name);
    }

    private static final class CountingLibrary extends BuiltInLibrary {
        int expansions;
        CountingLibrary(BuiltInLibrary source) {
            super(source.getName(), source.getDependencyNames(), source.getPackageName().orElse(null));
        }
        @Override public List<String> getDependencyNames() {
            expansions++;
            return super.getDependencyNames();
        }
    }

    private static void status(String name, String value) {
        Bundle status = new Bundle();
        status.putString(name, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
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
