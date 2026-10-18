package pro.sketchware.activities.resourceseditor;

import android.content.Context;
import android.os.Bundle;

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
        ResourceEditorDraftStore.LegacyImport legacy;
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
        this(context, project, variant, savedSession, null, null);
    }

    ResourceEditorDraftController(Context context, String project, String variant, String savedSession,
                                   String[] xmlPaths) {
        this(context, project, variant, savedSession, xmlPaths, null);
    }

    ResourceEditorDraftController(Context context, String project, String variant, String savedSession,
                                   String[] xmlPaths, Bundle legacyDrafts) {
        session = savedSession == null ? UUID.randomUUID().toString() : savedSession;
        gate = OWNERS.computeIfAbsent(session, ignored -> new Gate());
        synchronized (gate) { owner = ++gate.owner; }
        File root = new File(context.getApplicationContext().getNoBackupFilesDir(), "resource-editor-drafts");
        opening = CompletableFuture.supplyAsync(() -> {
            try {
                ResourceEditorDraftStore store = new ResourceEditorDraftStore(root, session, project, variant, gate, owner,
                        point -> ResourceEditorDraftStore.testFault.accept(point), xmlPaths);
                Loaded loaded = new Loaded(store, store.open(savedSession != null));
                // Migrate before any editor or prewrite exists, so a late legacy commit can never
                // overwrite a newer model. Failure keeps the Bundle as the retry source.
                if (legacyDrafts != null) {
                    loaded.legacy = store.importLegacy(legacyDrafts);
                    for (int i = 0; i < loaded.pages.length; i++) {
                        if (loaded.pages[i] == null && loaded.legacy.pages.get(i) != null) {
                            loaded.pages[i] = loaded.legacy.pages.get(i);
                        }
                    }
                }
                ResourceEditorDraftStore.pruneClosedSessions(root);
                ResourceEditorDraftOrphans.prune(root, OWNERS.keySet());
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

    /**
     * Retries a failed legacy import while the editors are still loading. Runs on the serial
     * draft queue; callers must not use it once a live model could be newer than the Bundle.
     */
    ResourceEditorDraftStore.LegacyImport retryLegacy(Bundle drafts) {
        if (detached || drafts == null || drafts.isEmpty()) return null;
        try {
            Loaded loaded = opening.join();
            if (loaded.legacy != null && loaded.legacy.covered()) return loaded.legacy;
            ResourceEditorDraftStore.LegacyImport retried = CompletableFuture.supplyAsync(
                    () -> loaded.store.importLegacy(drafts), IO).get();
            loaded.legacy = retried;
            for (int i = 0; i < loaded.pages.length; i++) {
                if (loaded.pages[i] == null && retried.pages.get(i) != null) {
                    loaded.pages[i] = retried.pages.get(i);
                }
            }
            return retried;
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.e("ResourceDrafts", "Legacy draft retry interrupted", failure);
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to retry legacy draft migration", failure);
        }
        return null;
    }

    boolean hasCheckpoint() {
        return opening.isDone() && !opening.isCompletedExceptionally() && opening.join().store.isDurable();
    }

    String archiveLegacyCorrupt(Bundle corrupt) {
        if (detached || corrupt == null || corrupt.isEmpty()) return null;
        try {
            Loaded loaded = opening.join();
            return CompletableFuture.supplyAsync(() -> {
                try { return loaded.store.archiveLegacyCorrupt(corrupt); }
                catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }, IO).get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.e("ResourceDrafts", "Legacy material archival interrupted", failure);
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.e("ResourceDrafts", "Unable to archive legacy material", failure);
        }
        return null;
    }

    boolean isWriting() { return writing != null && !writing.isDone(); }

    String prepareLegacyXml(int page, String xml) throws IOException {
        if (detached) throw new IOException("Draft controller detached");
        try {
            Loaded loaded = opening.join();
            if (!loaded.store.hasLegacyXmlHistory()) return null;
            return CompletableFuture.supplyAsync(() -> {
                try { return loaded.store.prepareLegacyXml(page, xml); }
                catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }, IO).get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IOException("Legacy XML protection interrupted", failure);
        } catch (ExecutionException | RuntimeException failure) {
            throw new IOException("Unable to protect legacy XML source", failure);
        }
    }

    void confirmLegacyXml(int page, String token) {
        if (token == null || detached) return;
        try {
            Loaded loaded = opening.join();
            CompletableFuture.runAsync(() -> {
                try { loaded.store.confirmLegacyXml(page, token); }
                catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }, IO).get();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            LogUtil.w("ResourceDrafts", "Legacy XML confirmation interrupted; intent retained", failure);
        } catch (ExecutionException | RuntimeException failure) {
            LogUtil.w("ResourceDrafts", "Legacy XML confirmation deferred; intent retained", failure);
        }
    }

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
            opening.whenCompleteAsync((loaded, failure) -> {
                try { if (loaded != null) loaded.store.close(); }
                catch (IOException closeFailure) { LogUtil.w("ResourceDrafts", "Unable to close draft session", closeFailure); }
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
