package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class ResourceEditorDraftMaintenanceTest {
    @Test public void openingCompletesBeforeEnumerationAndCleanup() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(19);
        List<File> cleaned = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> cleaned.add(directory), failures::add);
        CompletableFuture<String> opening = CompletableFuture.supplyAsync(() -> "restored", queue);
        maintenance.afterOpening(opening, root);

        queue.runNext();
        assertEquals("restored", opening.join());
        assertEquals(0, root.enumerations);
        assertTrue(cleaned.isEmpty());
        queue.drain();
        assertEquals(1, root.enumerations);
        assertEquals(19, cleaned.size());
        assertTrue(failures.isEmpty());
    }

    @Test public void failedOpeningDoesNotScheduleMaintenance() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(1);
        CompletableFuture<String> opening = new CompletableFuture<>();
        new ResourceEditorDraftMaintenance(queue, (directory, cutoff) -> fail(), failure -> fail())
                .afterOpening(opening, root);
        opening.completeExceptionally(new IllegalStateException("Cannot restore"));
        assertTrue(queue.tasks.isEmpty());
        assertEquals(0, root.enumerations);
    }

    @Test public void queuedCheckpointRunsBetweenBatches() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(25);
        List<String> events = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> {
                    events.add(directory.getName());
                    if (events.size() == 1) queue.execute(() -> events.add("checkpoint"));
                }, failure -> fail(failure.toString()));
        maintenance.schedule(root);
        queue.runNext(); // Enumerating does not stat each candidate or start a batch.
        assertTrue(events.isEmpty());
        queue.runNext();
        assertEquals(8, events.size());
        queue.runNext();
        assertEquals("checkpoint", events.get(8));
        queue.drain();
        assertEquals(26, events.size());
        assertEquals("24", events.get(25));
    }

    @Test public void repeatedRootRequestsCoalesceUntilTheSweepFinishes() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(19);
        List<File> cleaned = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> cleaned.add(directory), failure -> fail(failure.toString()));
        maintenance.schedule(root);
        maintenance.schedule(new File(root.getPath()));
        assertEquals(1, queue.tasks.size());
        queue.runNext();
        queue.runNext();
        maintenance.schedule(root);
        assertEquals(1, queue.tasks.size());
        queue.drain();
        assertEquals(19, cleaned.size());
        assertEquals(1, root.enumerations);
        maintenance.schedule(root);
        queue.drain();
        assertEquals(38, cleaned.size());
        assertEquals(2, root.enumerations);
    }

    @Test public void candidateFailureDoesNotStopOtherCandidatesOrFutureSweeps() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(10);
        List<File> cleaned = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> {
                    if (directory.getName().equals("0")) throw new IllegalStateException("Unreadable session");
                    cleaned.add(directory);
                }, failures::add);
        maintenance.schedule(root);
        queue.drain();
        assertEquals(9, cleaned.size());
        assertEquals(1, failures.size());
        maintenance.schedule(root);
        queue.drain();
        assertEquals(18, cleaned.size());
        assertEquals(2, failures.size());
    }

    @Test public void enumerationFailureReleasesTheRootForRetry() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(1);
        root.failEnumeration = true;
        List<File> cleaned = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> cleaned.add(directory), failures::add);
        maintenance.schedule(root);
        queue.drain();
        assertTrue(cleaned.isEmpty());
        assertEquals(1, failures.size());
        root.failEnumeration = false;
        maintenance.schedule(root);
        queue.drain();
        assertEquals(1, cleaned.size());
        assertEquals(2, root.enumerations);
    }

    @Test public void rejectedContinuationReleasesTheRootForRetry() {
        QueueExecutor queue = new QueueExecutor();
        Root root = new Root(10);
        List<File> cleaned = new ArrayList<>();
        List<RuntimeException> failures = new ArrayList<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue,
                (directory, cutoff) -> cleaned.add(directory), failures::add);
        maintenance.schedule(root);
        queue.runNext();
        queue.rejectNext = true;
        queue.runNext();
        assertEquals(8, cleaned.size());
        assertEquals(1, failures.size());
        assertTrue(queue.tasks.isEmpty());
        maintenance.schedule(root);
        queue.drain();
        assertEquals(18, cleaned.size());
        assertEquals(2, root.enumerations);
    }

    private static final class QueueExecutor implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        boolean rejectNext;
        @Override public void execute(Runnable command) {
            if (rejectNext) { rejectNext = false; throw new RejectedExecutionException("Injected"); }
            tasks.addLast(command);
        }
        void runNext() { tasks.removeFirst().run(); }
        void drain() { while (!tasks.isEmpty()) runNext(); }
    }

    private static final class Root extends File {
        final File[] children;
        int enumerations;
        boolean failEnumeration;
        Root(int count) {
            super("draft-maintenance-test-root");
            children = new File[count];
            for (int i = 0; i < count; i++) children[i] = new File(this, Integer.toString(i));
        }
        @Override public File[] listFiles() {
            enumerations++;
            if (failEnumeration) throw new SecurityException("Injected");
            return children;
        }
    }
}
