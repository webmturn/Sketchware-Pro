package pro.sketchware.core;

public class ProjectDataManager {

    public static ProjectDataStore a;
    public static ProjectFileManager b;
    public static ResourceManager c;
    public static LibraryManager d;

    public static void a() {
        a = null;
        b = null;
        c = null;
        d = null;
    }

    public static void b() {
        a.i();
        a = null;
    }

    public static void c() {
        b.k();
        b = null;
    }

    public static void d() {
        d.j();
        d = null;
    }

    public static void e() {
        c.t();
        c = null;
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
        if (b != null && !str.equals(b.projectId)) {
            c();
        }
        if (b == null) {
            b = new ProjectFileManager(str);
            if (!z) {
                b.i();
            } else if (b.g()) {
                b.h();
            } else {
                b.i();
            }
        }
        return b;
    }

    public static synchronized LibraryManager c(String str, boolean z) {
        if (d != null && !str.equals(d.projectId)) {
            d();
        }
        if (d == null) {
            d = new LibraryManager(str);
            if (!z) {
                d.i();
            } else if (d.g()) {
                d.h();
            } else {
                d.i();
            }
        }
        return d;
    }

    public static synchronized ResourceManager d(String str, boolean z) {
        if (c != null && !str.equals(c.projectId)) {
            e();
        }
        if (c == null) {
            c = new ResourceManager(str);
            if (!z) {
                c.s();
            } else if (c.q()) {
                c.r();
            } else {
                c.s();
            }
        }
        return c;
    }

    public static synchronized ProjectDataStore a(String str) {
        return a(str, true);
    }

    public static synchronized ProjectDataStore a(String str, boolean z) {
        if (a != null && !str.equals(a.projectId)) {
            b();
        }
        if (a == null) {
            a = new ProjectDataStore(str);
            if (!z) {
                a.g();
                a.e();
            } else {
                if (a.d()) {
                    a.h();
                } else {
                    a.g();
                }
                if (a.c()) {
                    a.f();
                } else {
                    a.e();
                }
            }
        }
        return a;
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
