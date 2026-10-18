package pro.sketchware.activities.resourceseditor;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pro.sketchware.util.LogUtil;

/** UI-thread coordinator; disk tasks never wait for a UI callback or retain an editor. */
final class ResourceEditorDraftController {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> new Thread(r, "resource-drafts"));
    private static final Map<String, Gate> OWNERS = new ConcurrentHashMap<>();
    static final class Gate {
        volatile long owner;
        volatile boolean closed;
    }
    static final class Loaded {
        final ResourceEditorDraftStore store;
        final ResourceEditorDraftState.Snapshot[] pages;
        Loaded(ResourceEditorDraftStore store, ResourceEditorDraftState.Snapshot[] pages) {
            this.store = store; this.pages = pages;
        }
    }

    final String session;
    final CompletableFuture<Loaded> opening;
    private CompletableFuture<Void> writing;
    private long[] requestedRevisions;
    private boolean[] requestedDirty;
    private boolean detached;
    private final Gate gate;
    private final long owner;

    ResourceEditorDraftController(Context context, String project, String variant, String savedSession) {
        this(context, project, variant, savedSession, null);
    }

    ResourceEditorDraftController(Context context, String project, String variant, String savedSession,
                                   String[] xmlPaths) {
        session = savedSession == null ? UUID.randomUUID().toString() : savedSession;
        gate = OWNERS.computeIfAbsent(session, ignored -> new Gate());
        synchronized (gate) { owner = ++gate.owner; }
        File root = new File(context.getApplicationContext().getNoBackupFilesDir(), "resource-editor-drafts");
        opening = CompletableFuture.supplyAsync(() -> {
            try {
                ResourceEditorDraftStore store = new ResourceEditorDraftStore(root, session, project, variant, gate, owner,
                        point -> ResourceEditorDraftStore.testFault.accept(point), xmlPaths);
                Loaded loaded = new Loaded(store, store.open(savedSession != null));
                ResourceEditorDraftStore.pruneClosedSessions(root);
                return loaded;
            } catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, IO);
    }

    CompletableFuture<Void> prewrite(ResourceEditorDraftState.Owner[] editors) {
        if (detached) return CompletableFuture.completedFuture(null);
        Loaded loaded = opening.join(); // Activity only calls this after opening has completed.
        long[] revisions = new long[editors.length];
        boolean[] dirty = new boolean[editors.length];
        for (int i = 0; i < editors.length; i++) {
            revisions[i] = editors[i].draftRevision(); dirty[i] = editors[i].hasDraftChanges();
        }
        if (Arrays.equals(requestedRevisions, revisions) && Arrays.equals(requestedDirty, dirty)
                && writing != null && !writing.isCompletedExceptionally()) return writing;
        Map<Integer, ResourceEditorDraftState.Snapshot> changes = new HashMap<>();
        boolean unchanged = loaded.store.isDurable();
        for (int i = 0; i < editors.length; i++) {
            boolean same = loaded.store.protectedPage(i, revisions[i], dirty[i]);
            unchanged &= same;
            if (dirty[i] && !same) changes.put(i, editors[i].snapshotDraft(loaded.store.incrementalRevision(i)));
        }
        requestedRevisions = revisions;
        requestedDirty = dirty;
        if (unchanged) return writing = CompletableFuture.completedFuture(null);
        // Locals below contain only independent snapshots and storage state, never fragments.
        return writing = CompletableFuture.runAsync(() -> {
            try { loaded.store.commit(revisions, dirty, changes); }
            catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        }, IO);
    }

    boolean checkpoint(ResourceEditorDraftState.Owner[] editors) {
        try {
            prewrite(editors).get();
            return true;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.e("ResourceDrafts", "Draft checkpoint interrupted", failure);
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to protect latest draft", failure);
        }
        return false;
    }

    boolean migrateLegacy(android.os.Bundle drafts) {
        try {
            CompletableFuture.runAsync(() -> {
                Loaded loaded = opening.join();
                long[] revisions = new long[ResourceEditorTabs.COUNT];
                boolean[] dirty = new boolean[ResourceEditorTabs.COUNT];
                Map<Integer, ResourceEditorDraftState.Snapshot> pages = new HashMap<>();
                for (int i = 0; i < revisions.length; i++) {
                    android.os.Bundle page = drafts.getBundle(Integer.toString(i));
                    if (page != null) {
                        revisions[i] = 1; dirty[i] = true;
                        pages.put(i, ResourceEditorDraftState.fromLegacy(page, ResourceEditorDraftStore.modelClass(i)));
                    }
                }
                try { loaded.store.commit(revisions, dirty, pages); }
                catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }, IO).get();
            return true;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to migrate legacy draft", failure);
        }
        return false;
    }

    boolean hasCheckpoint() {
        return opening.isDone() && !opening.isCompletedExceptionally() && opening.join().store.isDurable();
    }

    boolean isWriting() { return writing != null && !writing.isDone(); }

    /** Persists the per-page XML commit intent before the XML-only path writes project files. */
    boolean writeXmlCommitIntent(Map<Integer, String> hashes) {
        if (detached) return false;
        try {
            Loaded loaded = opening.join();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return loaded.store.writeXmlCommitIntent(hashes);
                } catch (IOException failure) {
                    throw new java.util.concurrent.CompletionException(failure);
                }
            }, IO).get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.e("ResourceDrafts", "XML commit intent interrupted", failure);
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to write XML commit intent", failure);
        }
        return false;
    }

    /**
     * Closes the old session after its XML has been saved, so a stale saved-state reference can
     * no longer restore the older draft. Returns false if the CLOSED manifest could not be written.
     */
    boolean discardSession() {
        if (detached) return false;
        try {
            Loaded loaded = opening.join();
            CompletableFuture.runAsync(() -> {
                try {
                    loaded.store.close();
                } catch (IOException failure) {
                    throw new java.util.concurrent.CompletionException(failure);
                }
            }, IO).get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.e("ResourceDrafts", "Draft discard interrupted", failure);
            return false;
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to discard draft session", failure);
            return false;
        }
        detached = true;
        releaseOwnership();
        return true;
    }

    void detach(boolean close) {
        if (detached) return;
        detached = true;
        if (close) {
            opening.thenAcceptAsync(loaded -> {
                try { loaded.store.close(); }
                catch (IOException failure) { LogUtil.w("ResourceDrafts", "Unable to close draft session", failure); }
                finally { releaseOwnership(); }
            }, IO);
        } else releaseOwnership();
    }

    private void releaseOwnership() {
        synchronized (gate) {
            if (gate.owner == owner) {
                ++gate.owner;
                OWNERS.remove(session, gate);
            }
        }
    }
}
