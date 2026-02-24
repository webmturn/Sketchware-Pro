package pro.sketchware.core;

public class ProjectDataManager {

    public static ProjectDataStore projectDataStore;
    public static ProjectFileManager projectFileManager;
    public static ResourceManager resourceManager;
    public static LibraryManager libraryManager;

    /** Clears all cached manager instances. */
    public static void clearAll() {
        projectDataStore = null;
        projectFileManager = null;
        resourceManager = null;
        libraryManager = null;
    }

    /** Saves and closes the project data manager (ProjectDataStore). */
    public static void closeDataManager() {
        projectDataStore.resetProject();
        projectDataStore = null;
    }

    /** Saves and closes the project file manager (ProjectFileManager). */
    public static void closeFileManager() {
        projectFileManager.resetAll();
        projectFileManager = null;
    }

    /** Saves and closes the library manager (LibraryManager). */
    public static void closeLibraryManager() {
        libraryManager.resetAll();
        libraryManager = null;
    }

    /** Saves and closes the resource manager (ResourceManager). */
    public static void closeResourceManager() {
        resourceManager.resetAll();
        resourceManager = null;
    }

    /** Gets or creates the project data manager (ProjectDataStore) for the given project. */
    public static synchronized ProjectDataStore getProjectDataManager(String sc_id) {
        return getProjectDataManager(sc_id, true);
    }

    /** Gets or creates the project data manager (ProjectDataStore) with load option. */
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

    /** Gets or creates the project file manager (ProjectFileManager) for the given project. */
    public static synchronized ProjectFileManager getFileManager(String sc_id) {
        return getFileManager(sc_id, true);
    }

    /** Gets or creates the project file manager (ProjectFileManager) with load option. */
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

    /** Gets or creates the library manager (LibraryManager) for the given project. */
    public static synchronized LibraryManager getLibraryManager(String sc_id) {
        return getLibraryManager(sc_id, true);
    }

    /** Gets or creates the library manager (LibraryManager) with load option. */
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

    /** Gets or creates the resource manager (ResourceManager) for the given project. */
    public static synchronized ResourceManager getResourceManager(String sc_id) {
        return getResourceManager(sc_id, true);
    }

    /** Gets or creates the resource manager (ResourceManager) with load option. */
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
