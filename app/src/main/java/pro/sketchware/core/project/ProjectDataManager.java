package pro.sketchware.core.project;

/**
 * Singleton factory for obtaining project-scoped manager instances.
 * <p>
 * Each manager is cached and associated with a single project ID ({@code sc_id}).
 * If a different project ID is requested, the previous manager is closed and a new
 * one is created. All managers follow the same lifecycle:
 * <ol>
 *   <li>{@code getXxxManager(sc_id)} — creates or returns cached instance</li>
 *   <li>{@code closeXxxManager()} — persists pending changes to backup and releases the instance</li>
 * </ol>
 *
 * @see ProjectDataStore
 * @see ProjectFileManager
 * @see ResourceManager
 * @see LibraryManager
 */
public class ProjectDataManager {

    private static volatile ProjectDataStore projectDataStore;
    private static volatile ProjectFileManager projectFileManager;
    private static volatile ResourceManager resourceManager;
    private static volatile LibraryManager libraryManager;

    /** Loads all metadata before allowing a save or resource cleanup. */
    public static synchronized boolean canSaveProject(String projectId) {
        ProjectDataStore data = getProjectDataManager(projectId);
        ProjectFileManager files = getFileManager(projectId);
        ResourceManager resources = getResourceManager(projectId);
        LibraryManager libraries = getLibraryManager(projectId);
        return data.canSave() && files.canSave() && resources.canSave() && libraries.canSave();
    }

    // Do not acquire the factory lock here: closeDataManager holds it while joining save workers.
    static boolean canSaveLoadedProject(String projectId) {
        ProjectDataStore data = projectDataStore;
        ProjectFileManager files = projectFileManager;
        ResourceManager resources = resourceManager;
        LibraryManager libraries = libraryManager;
        return (data == null || !projectId.equals(data.projectId) || data.canSave())
                && (files == null || !projectId.equals(files.projectId) || files.canSave())
                && (resources == null || !projectId.equals(resources.projectId) || resources.canSave())
                && (libraries == null || !projectId.equals(libraries.projectId) || libraries.canSave());
    }

    public static synchronized boolean hasProjectBackup(String projectId) {
        return getProjectDataManager(projectId).hasViewBackup()
                || getProjectDataManager(projectId).hasLogicBackup()
                || getFileManager(projectId).hasBackup()
                || getResourceManager(projectId).hasBackup()
                || getLibraryManager(projectId).hasBackup();
    }

    /** Recovery stays in memory; failed backups never authorize writing over the original. */
    public static synchronized boolean loadProjectBackup(String projectId) {
        ProjectDataStore data = getProjectDataManager(projectId);
        ProjectFileManager files = getFileManager(projectId);
        ResourceManager resources = getResourceManager(projectId);
        LibraryManager libraries = getLibraryManager(projectId);
        if (data.hasViewBackup()) data.loadViewFromBackup();
        if (data.hasLogicBackup()) data.loadLogicFromBackup();
        if (files.hasBackup()) files.loadFromBackup();
        if (resources.hasBackup()) resources.loadFromBackup();
        if (libraries.hasBackup()) libraries.loadFromBackup();
        return canSaveProject(projectId);
    }

    /** A fresh editor must check canonical files even if a UI helper already opened a backup. */
    public static synchronized void reloadProjectFromData(String projectId) {
        ProjectDataStore data = getProjectDataManager(projectId, false);
        ProjectFileManager files = getFileManager(projectId, false);
        ResourceManager resources = getResourceManager(projectId, false);
        LibraryManager libraries = getLibraryManager(projectId, false);
        data.loadViewFromData();
        data.loadLogicFromData();
        files.loadFromData();
        resources.loadFromData();
        libraries.loadFromData();
    }

    private static void requireProjectId(String sc_id) {
        if (sc_id == null || sc_id.isEmpty()) {
            throw new IllegalArgumentException("sc_id must not be null or empty");
        }
    }

    private static IllegalStateException createManagerCloseFailure(String managerName, String currentProjectId, String requestedProjectId) {
        return new IllegalStateException("Failed to save " + managerName + " backup for project "
                + currentProjectId + " before switching to project " + requestedProjectId);
    }

    /**
     * Clears all cached manager instances without saving.
     * <p>
     * Unlike the individual {@code closeXxx()} methods, this does not persist
     * pending changes to backup or data files.
     */
    public static synchronized void clearAll() {
        projectDataStore = null;
        projectFileManager = null;
        resourceManager = null;
        libraryManager = null;
    }

    /**
     * Discards unsaved changes for all cached manager instances and releases them.
     * <p>
     * This removes backup files and temporary resource copies so discarded edits
     * are not offered for restore later.
     */
    public static synchronized void discardAll() {
        if (projectDataStore != null) {
            projectDataStore.deleteBackupFiles();
        }
        if (projectFileManager != null) {
            projectFileManager.deleteBackup();
        }
        if (resourceManager != null) {
            resourceManager.deleteBackup();
            resourceManager.deleteTempDirs();
        }
        if (libraryManager != null) {
            libraryManager.deleteBackup();
        }
        clearAll();
    }

    /**
     * Saves pending changes via {@link ProjectDataStore#saveAllBackup()} and
     * releases the cached {@link ProjectDataStore} instance.
     * Does nothing if no data manager is currently open.
     */
    public static synchronized boolean closeDataManager() {
        if (projectDataStore != null) {
            if (!canSaveLoadedProject(projectDataStore.projectId)) return false;
            if (!projectDataStore.saveAllBackup()) {
                return false;
            }
            projectDataStore = null;
        }
        return true;
    }

    /**
     * Saves pending changes via {@link ProjectFileManager#saveToBackup()} and
     * releases the cached {@link ProjectFileManager} instance.
     * Does nothing if no file manager is currently open.
     */
    public static synchronized boolean closeFileManager() {
        if (projectFileManager != null) {
            if (!canSaveLoadedProject(projectFileManager.projectId)) return false;
            if (!projectFileManager.saveToBackup()) {
                return false;
            }
            projectFileManager = null;
        }
        return true;
    }

    /**
     * Saves pending changes via {@link LibraryManager#saveToBackup()} and
     * releases the cached {@link LibraryManager} instance.
     * Does nothing if no library manager is currently open.
     */
    public static synchronized boolean closeLibraryManager() {
        if (libraryManager != null) {
            if (!canSaveLoadedProject(libraryManager.projectId)) return false;
            if (!libraryManager.saveToBackup()) {
                return false;
            }
            libraryManager = null;
        }
        return true;
    }

    /**
     * Saves pending changes via {@link ResourceManager#saveToBackup()} and
     * releases the cached {@link ResourceManager} instance.
     * Does nothing if no resource manager is currently open.
     */
    public static synchronized boolean closeResourceManager() {
        if (resourceManager != null) {
            if (!canSaveLoadedProject(resourceManager.projectId)) return false;
            if (!resourceManager.saveToBackup()) {
                return false;
            }
            resourceManager = null;
        }
        return true;
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
        requireProjectId(sc_id);
        if (projectDataStore != null && !sc_id.equals(projectDataStore.projectId)) {
            String currentProjectId = projectDataStore.projectId;
            if (!closeDataManager()) {
                throw createManagerCloseFailure("project data", currentProjectId, sc_id);
            }
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
        requireProjectId(sc_id);
        if (projectFileManager != null && !sc_id.equals(projectFileManager.projectId)) {
            String currentProjectId = projectFileManager.projectId;
            if (!closeFileManager()) {
                throw createManagerCloseFailure("project file", currentProjectId, sc_id);
            }
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
        requireProjectId(sc_id);
        if (libraryManager != null && !sc_id.equals(libraryManager.projectId)) {
            String currentProjectId = libraryManager.projectId;
            if (!closeLibraryManager()) {
                throw createManagerCloseFailure("library", currentProjectId, sc_id);
            }
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
        requireProjectId(sc_id);
        if (resourceManager != null && !sc_id.equals(resourceManager.projectId)) {
            String currentProjectId = resourceManager.projectId;
            if (!closeResourceManager()) {
                throw createManagerCloseFailure("resource", currentProjectId, sc_id);
            }
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
