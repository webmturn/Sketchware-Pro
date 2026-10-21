package pro.sketchware.core.library;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pro.sketchware.core.build.ProjectBuilder;

/**
 * A class to keep track of a project's built-in libraries.
 */

public class BuiltInLibraryManager {

    private final Set<String> processedLibraries = new HashSet<>();
    private final ArrayList<BuiltInLibrary> libraries = new ArrayList<>();
    private final List<BuiltInLibraries.BuiltInLibrary> excludedLibraries;

    public BuiltInLibraryManager(String projectId) {
        var config = ExcludeBuiltInLibrariesConfig.readConfig(projectId);
        excludedLibraries = config != null && Boolean.TRUE.equals(config.first)
                ? config.second : List.of();
    }

    /**
     * Add a built-in library to the project libraries list.
     * Won't add a library if it's in the list already,
     * or it got excluded with {@link pro.sketchware.activities.editor.manage.library.ExcludeBuiltInLibrariesActivity}.
     *
     * @param libraryName The built-in library's name, e.g. material-1.0.0
     */
    public void addLibrary(String libraryName) {
        if (processedLibraries.contains(libraryName)) {
            return;
        }
        Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(libraryName);
        //noinspection SimplifyOptionalCallChains because #isEmpty() isn't available on Android.
        if (!library.isPresent() || !excludedLibraries.contains(library.get())) {
            Log.d(ProjectBuilder.TAG, "Added built-in library \"" + libraryName + "\" to project's dependencies");
            libraries.add(new BuiltInLibrary(libraryName));
        }
        // Excluded libraries also participate in cycles and shared dependency paths.
        processedLibraries.add(libraryName);
        addDependencies(libraryName);
    }

    private void addDependencies(String libraryName) {
        for (String libraryDependency : BuiltInLibraryUtils.getKnownDependencies(libraryName)) {
            addLibrary(libraryDependency);
        }
    }

    public boolean containsLibrary(String libraryName) {
        Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(libraryName);
        //noinspection SimplifyOptionalCallChains because #isEmpty() isn't available on Android.
        if (!library.isPresent()) {
            return false;
        }
        return libraries.contains(new BuiltInLibrary(library.get().getName()));
    }

    /**
     * @return {@link BuiltInLibraryManager#libraries}
     */
    public ArrayList<BuiltInLibrary> getLibraries() {
        return libraries;
    }
}
