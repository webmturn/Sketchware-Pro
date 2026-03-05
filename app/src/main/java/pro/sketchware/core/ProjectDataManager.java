package pro.sketchware.core;

/**
 * Singleton factory for obtaining project-scoped manager instances.
 * <p>
 * Each manager is cached and associated with a single project ID ({@code sc_id}).
 * If a different project ID is requested, the previous manager is closed and a new
 * one is created. All managers follow the same lifecycle:
 * <ol>
 *   <li>{@code getXxxManager(sc_id)} — creates or returns cached instance</li>
 *   <li>{@code closeXxxManager()} — persists data and releases the instance</li>
 * </ol>
 *
 * @see ProjectDataStore
 * @see ProjectFileManager
 * @see ResourceManager
 * @see LibraryManager
 */
public class ProjectDataManager {

    private static ProjectDataStore projectDataStore;
    private static ProjectFileManager projectFileManager;
    private static ResourceManager resourceManager;
    private static LibraryManager libraryManager;

    /**
     * Clears all cached manager instances without saving.
     * <p>
     * Unlike the individual {@code closeXxx()} methods, this does <b>not</b>
     * call {@code resetProject()} or {@code resetAll()} — data is discarded.
     */
    public static void clearAll() {
        projectDataStore = null;
        projectFileManager = null;
        resourceManager = null;
        libraryManager = null;
    }

    /**
     * Saves pending changes via {@link ProjectDataStore#resetProject()} and
     * releases the cached {@link ProjectDataStore} instance.
     *
     * @throws NullPointerException if no data manager is currently open
     */
    public static void closeDataManager() {
        projectDataStore.resetProject();
        projectDataStore = null;
    }

    /**
     * Saves pending changes via {@link ProjectFileManager#resetAll()} and
     * releases the cached {@link ProjectFileManager} instance.
     *
     * @throws NullPointerException if no file manager is currently open
     */
    public static void closeFileManager() {
        projectFileManager.resetAll();
        projectFileManager = null;
    }

    /**
     * Saves pending changes via {@link LibraryManager#resetAll()} and
     * releases the cached {@link LibraryManager} instance.
     *
     * @throws NullPointerException if no library manager is currently open
     */
    public static void closeLibraryManager() {
        libraryManager.resetAll();
        libraryManager = null;
    }

    /**
     * Saves pending changes via {@link ResourceManager#resetAll()} and
     * releases the cached {@link ResourceManager} instance.
     *
     * @throws NullPointerException if no resource manager is currently open
     */
    public static void closeResourceManager() {
        resourceManager.resetAll();
        resourceManager = null;
    }

    /**
     * Returns the cached {@link ProjectDataStore} for the given project,
     * creating one if necessary. Loads from backup if available.
     *
     * @param sc_id the project identifier (e.g. {@code "601"})
     * @return the project data store instance
     */
    public static synchronized ProjectDataStore getProjectDataManager(String sc_id) {
        return getProjectDataManager(sc_id, true);
    }

    /**
     * Returns the cached {@link ProjectDataStore} for the given project.
     *
     * @param sc_id the project identifier (e.g. {@code "601"})
     * @param load  if {@code true}, prefers backup files over data files;
     *              if {@code false}, always loads from the canonical data files
     * @return the project data store instance
     */
    public static synchronized ProjectDataStore getProjectDataManager(String sc_id, boolean load) {
        if (projectDataStore != null && !sc_id.equals(projectDataStore.projectId)) {
            closeDataManager();
        }
        if (projectDataStore == null) {
            projectDataStore = new ProjectDataStore(sc_id);
            if (!load) {
                projectDataStore.loadViewFromData();
                projectDataStore.loadLogicFromData();
            } else {
                if (projectDataStore.hasViewBackup()) {
                    projectDataStore.loadViewFromBackup();
                } else {
                    projectDataStore.loadViewFromData();
                }
                if (projectDataStore.hasLogicBackup()) {
                    projectDataStore.loadLogicFromBackup();
                } else {
                    projectDataStore.loadLogicFromData();
                }
            }
        }
        return projectDataStore;
    }

    /**
     * Returns the cached {@link ProjectFileManager} for the given project,
     * creating one if necessary. Loads from backup if available.
     *
     * @param sc_id the project identifier
     * @return the project file manager instance
     */
    public static synchronized ProjectFileManager getFileManager(String sc_id) {
        return getFileManager(sc_id, true);
    }

    /**
     * Returns the cached {@link ProjectFileManager} for the given project.
     *
     * @param sc_id the project identifier
     * @param load  if {@code true}, prefers backup files; if {@code false}, loads from data files
     * @return the project file manager instance
     */
    public static synchronized ProjectFileManager getFileManager(String sc_id, boolean load) {
        if (projectFileManager != null && !sc_id.equals(projectFileManager.projectId)) {
            closeFileManager();
        }
        if (projectFileManager == null) {
            projectFileManager = new ProjectFileManager(sc_id);
            if (!load) {
                projectFileManager.loadFromData();
            } else if (projectFileManager.hasBackup()) {
                projectFileManager.loadFromBackup();
            } else {
                projectFileManager.loadFromData();
            }
        }
        return projectFileManager;
    }

    /**
     * Returns the cached {@link LibraryManager} for the given project,
     * creating one if necessary. Loads from backup if available.
     *
     * @param sc_id the project identifier
     * @return the library manager instance
     */
    public static synchronized LibraryManager getLibraryManager(String sc_id) {
        return getLibraryManager(sc_id, true);
    }

    /**
     * Returns the cached {@link LibraryManager} for the given project.
     *
     * @param sc_id the project identifier
     * @param load  if {@code true}, prefers backup files; if {@code false}, loads from data files
     * @return the library manager instance
     */
    public static synchronized LibraryManager getLibraryManager(String sc_id, boolean load) {
        if (libraryManager != null && !sc_id.equals(libraryManager.projectId)) {
            closeLibraryManager();
        }
        if (libraryManager == null) {
            libraryManager = new LibraryManager(sc_id);
            if (!load) {
                libraryManager.loadFromData();
            } else if (libraryManager.hasBackup()) {
                libraryManager.loadFromBackup();
            } else {
                libraryManager.loadFromData();
            }
        }
        return libraryManager;
    }

    /**
     * Returns the cached {@link ResourceManager} for the given project,
     * creating one if necessary. Loads from backup if available.
     *
     * @param sc_id the project identifier
     * @return the resource manager instance
     */
    public static synchronized ResourceManager getResourceManager(String sc_id) {
        return getResourceManager(sc_id, true);
    }

    /**
     * Returns the cached {@link ResourceManager} for the given project.
     *
     * @param sc_id the project identifier
     * @param load  if {@code true}, prefers backup files; if {@code false}, loads from data files
     * @return the resource manager instance
     */
    public static synchronized ResourceManager getResourceManager(String sc_id, boolean load) {
        if (resourceManager != null && !sc_id.equals(resourceManager.projectId)) {
            closeResourceManager();
        }
        if (resourceManager == null) {
            resourceManager = new ResourceManager(sc_id);
            if (!load) {
                resourceManager.loadFromData();
            } else if (resourceManager.hasBackup()) {
                resourceManager.loadFromBackup();
            } else {
                resourceManager.loadFromData();
            }
        }
        return resourceManager;
    }
}
