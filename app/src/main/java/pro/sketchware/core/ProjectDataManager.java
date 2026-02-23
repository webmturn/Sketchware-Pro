package pro.sketchware.core;

public class ProjectDataManager {

    public static ProjectDataStore projectDataStore;
    public static ProjectFileManager projectFileManager;
    public static ResourceManager resourceManager;
    public static LibraryManager libraryManager;

    public static void a() {
        projectDataStore = null;
        projectFileManager = null;
        resourceManager = null;
        libraryManager = null;
    }

    public static void b() {
        projectDataStore.i();
        projectDataStore = null;
    }

    public static void c() {
        projectFileManager.k();
        projectFileManager = null;
    }

    public static void d() {
        libraryManager.j();
        libraryManager = null;
    }

    public static void e() {
        resourceManager.t();
        resourceManager = null;
    }

    public static synchronized ProjectFileManager b(String str) {
        return b(str, true);
    }

    public static synchronized LibraryManager c(String str) {
        return c(str, true);
    }

    public static synchronized ResourceManager d(String str) {
        return d(str, true);
    }

    public static synchronized ProjectFileManager b(String str, boolean z) {
        if (projectFileManager != null && !str.equals(projectFileManager.projectId)) {
            c();
        }
        if (projectFileManager == null) {
            projectFileManager = new ProjectFileManager(str);
            if (!z) {
                projectFileManager.i();
            } else if (projectFileManager.g()) {
                projectFileManager.h();
            } else {
                projectFileManager.i();
            }
        }
        return projectFileManager;
    }

    public static synchronized LibraryManager c(String str, boolean z) {
        if (libraryManager != null && !str.equals(libraryManager.projectId)) {
            d();
        }
        if (libraryManager == null) {
            libraryManager = new LibraryManager(str);
            if (!z) {
                libraryManager.i();
            } else if (libraryManager.g()) {
                libraryManager.h();
            } else {
                libraryManager.i();
            }
        }
        return libraryManager;
    }

    public static synchronized ResourceManager d(String str, boolean z) {
        if (resourceManager != null && !str.equals(resourceManager.projectId)) {
            e();
        }
        if (resourceManager == null) {
            resourceManager = new ResourceManager(str);
            if (!z) {
                resourceManager.s();
            } else if (resourceManager.q()) {
                resourceManager.r();
            } else {
                resourceManager.s();
            }
        }
        return resourceManager;
    }

    public static synchronized ProjectDataStore a(String str) {
        return a(str, true);
    }

    public static synchronized ProjectDataStore a(String str, boolean z) {
        if (projectDataStore != null && !str.equals(projectDataStore.projectId)) {
            b();
        }
        if (projectDataStore == null) {
            projectDataStore = new ProjectDataStore(str);
            if (!z) {
                projectDataStore.g();
                projectDataStore.e();
            } else {
                if (projectDataStore.d()) {
                    projectDataStore.h();
                } else {
                    projectDataStore.g();
                }
                if (projectDataStore.c()) {
                    projectDataStore.f();
                } else {
                    projectDataStore.e();
                }
            }
        }
        return projectDataStore;
    }

    // ===== Descriptive aliases (originals kept for JAR compatibility) =====

    /** Clears all cached manager instances. */
    public static void clearAll() {
        a();
    }

    /** Saves and closes the project data manager (ProjectDataStore). */
    public static void closeDataManager() {
        b();
    }

    /** Saves and closes the project file manager (ProjectFileManager). */
    public static void closeFileManager() {
        c();
    }

    /** Saves and closes the library manager (LibraryManager). */
    public static void closeLibraryManager() {
        d();
    }

    /** Saves and closes the resource manager (ResourceManager). */
    public static void closeResourceManager() {
        e();
    }

    /** Gets or creates the project data manager (ProjectDataStore) for the given project. */
    public static synchronized ProjectDataStore getProjectDataManager(String sc_id) {
        return a(sc_id);
    }

    /** Gets or creates the project data manager (ProjectDataStore) with load option. */
    public static synchronized ProjectDataStore getProjectDataManager(String sc_id, boolean load) {
        return a(sc_id, load);
    }

    /** Gets or creates the project file manager (ProjectFileManager) for the given project. */
    public static synchronized ProjectFileManager getFileManager(String sc_id) {
        return b(sc_id);
    }

    /** Gets or creates the project file manager (ProjectFileManager) with load option. */
    public static synchronized ProjectFileManager getFileManager(String sc_id, boolean load) {
        return b(sc_id, load);
    }

    /** Gets or creates the library manager (LibraryManager) for the given project. */
    public static synchronized LibraryManager getLibraryManager(String sc_id) {
        return c(sc_id);
    }

    /** Gets or creates the library manager (LibraryManager) with load option. */
    public static synchronized LibraryManager getLibraryManager(String sc_id, boolean load) {
        return c(sc_id, load);
    }

    /** Gets or creates the resource manager (ResourceManager) for the given project. */
    public static synchronized ResourceManager getResourceManager(String sc_id) {
        return d(sc_id);
    }

    /** Gets or creates the resource manager (ResourceManager) with load option. */
    public static synchronized ResourceManager getResourceManager(String sc_id, boolean load) {
        return d(sc_id, load);
    }
}
