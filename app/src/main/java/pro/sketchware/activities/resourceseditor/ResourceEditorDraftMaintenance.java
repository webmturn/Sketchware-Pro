package pro.sketchware.activities.resourceseditor;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import pro.sketchware.util.LogUtil;

/** Background collection shares the draft queue, yielding between small groups of sessions. */
final class ResourceEditorDraftMaintenance {
    private static final int BATCH_SIZE = 8;
    private final Executor io;
    private final Pruner pruner;
    private final Consumer<RuntimeException> failureLogger;
    private final Map<String, Sweep> pending = new HashMap<>();

    interface Pruner { void prune(File directory, long cutoff); }

    ResourceEditorDraftMaintenance(Executor io, Set<String> activeSessions) {
        this(io, (directory, cutoff) -> {
            ResourceEditorDraftStore.pruneClosedSession(directory, activeSessions, cutoff);
            ResourceEditorDraftOrphans.pruneDirectory(directory, activeSessions, cutoff);
        }, failure -> LogUtil.w("ResourceDrafts", "Draft maintenance deferred", failure));
    }

    // Tests use a manual executor and independent pruning work, without touching the controller queue.
    ResourceEditorDraftMaintenance(Executor io, Pruner pruner, Consumer<RuntimeException> failureLogger) {
        this.io = io;
        this.pruner = pruner;
        this.failureLogger = failureLogger;
    }

    void afterOpening(CompletableFuture<?> opening, File root) {
        // Do not return this dependent future as opening: recovery must never wait for collection.
        opening.thenRun(() -> schedule(root));
    }

    void schedule(File root) {
        String key = root.toPath().toAbsolutePath().normalize().toString();
        Sweep sweep = new Sweep(root, key);
        synchronized (pending) {
            if (pending.containsKey(key)) return;
            pending.put(key, sweep);
        }
        enqueue(sweep, sweep::enumerate);
    }

    private void enqueue(Sweep sweep, Runnable task) {
        try {
            io.execute(task);
        } catch (RuntimeException failure) {
            finish(sweep);
            failureLogger.accept(failure);
        }
    }

    private void finish(Sweep sweep) {
        synchronized (pending) { pending.remove(sweep.key, sweep); }
    }

    private final class Sweep {
        final File root;
        final String key;
        File[] candidates;
        long cutoff;
        int next;

        Sweep(File root, String key) { this.root = root; this.key = key; }

        void enumerate() {
            try {
                if (Files.isSymbolicLink(root.toPath())) { finish(this); return; }
                // No File::isDirectory filter: stat and eligibility checks belong in the batches.
                candidates = root.listFiles();
                cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                if (candidates == null || candidates.length == 0) { finish(this); return; }
                enqueue(this, this::pruneBatch);
            } catch (RuntimeException failure) {
                finish(this);
                failureLogger.accept(failure);
            }
        }

        void pruneBatch() {
            try {
                if (Files.isSymbolicLink(root.toPath())) { finish(this); return; }
                int end = Math.min(next + BATCH_SIZE, candidates.length);
                while (next < end) {
                    File directory = candidates[next++];
                    try {
                        // Eligibility is checked now, not when the root was enumerated.
                        pruner.prune(directory, cutoff);
                    } catch (RuntimeException failure) {
                        failureLogger.accept(failure);
                    }
                }
                if (next == candidates.length) finish(this);
                else enqueue(this, this::pruneBatch);
            } catch (RuntimeException failure) {
                finish(this);
                failureLogger.accept(failure);
            }
        }
    }
}
