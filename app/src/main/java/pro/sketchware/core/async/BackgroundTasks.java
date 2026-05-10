package pro.sketchware.core.async;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import mod.jbk.util.LogUtil;

public final class BackgroundTasks {
    private static final int IO_THREAD_COUNT = calculateIoThreadCount();
    private static final long SLOW_TASK_LOG_THRESHOLD_MS = 100L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO_EXECUTOR =
            Executors.newFixedThreadPool(IO_THREAD_COUNT, new NamedThreadFactory("Sketchware-IO"));
    private static final ExecutorService SERIAL_EXECUTOR =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("Sketchware-Serial"));

    private BackgroundTasks() {
    }

    /**
     * IO-bound task count scales with core count but is capped to avoid thread-switching
     * overhead on high-core devices. Formula: max(2, min(cores * 2, 8)).
     *
     * <ul>
     *   <li>Dual-core: 4 threads</li>
     *   <li>Quad-core: 8 threads</li>
     *   <li>Octa-core and up: 8 threads (capped)</li>
     * </ul>
     */
    private static int calculateIoThreadCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(2, Math.min(cores * 2, 8));
    }

    public static void runIo(TaskHost host, String tag, ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        execute(IO_EXECUTOR, host, tag, () -> {
            work.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError);
    }

    public static void runIoIfAlive(TaskHost host, String tag, ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        execute(IO_EXECUTOR, host, tag, () -> {
            work.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError, true);
    }

    public static void runSerial(TaskHost host, String tag, ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        execute(SERIAL_EXECUTOR, host, tag, () -> {
            work.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError);
    }

    public static void runSerialIfAlive(TaskHost host, String tag, ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        execute(SERIAL_EXECUTOR, host, tag, () -> {
            work.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError, true);
    }

    public static <T> void callIo(TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        execute(IO_EXECUTOR, host, tag, work, onSuccess, onError);
    }

    public static <T> void callIoIfAlive(TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        execute(IO_EXECUTOR, host, tag, work, onSuccess, onError, true);
    }

    public static <T> void callSerial(TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        execute(SERIAL_EXECUTOR, host, tag, work, onSuccess, onError);
    }

    public static ExecutorService createSingleThreadExecutor(String threadNamePrefix) {
        String safeThreadNamePrefix = threadNamePrefix != null && !threadNamePrefix.isEmpty()
                ? threadNamePrefix
                : "Sketchware-Worker";
        return Executors.newSingleThreadExecutor(new NamedThreadFactory(safeThreadNamePrefix));
    }

    public static void postMain(Runnable action) {
        if (action == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            MAIN_HANDLER.post(action);
        }
    }

    private static <T> void execute(ExecutorService executor, TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        execute(executor, host, tag, work, onSuccess, onError, false);
    }

    private static <T> void execute(ExecutorService executor, TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError, boolean requireAliveBeforeStart) {
        if (executor == null || host == null || work == null) {
            throw new IllegalArgumentException("Executor, host, and work must not be null");
        }
        String safeTag = tag != null && !tag.isEmpty() ? tag : "BackgroundTasks";
        long enqueuedAt = SystemClock.elapsedRealtime();
        Runnable task = () -> {
            long startedAt = SystemClock.elapsedRealtime();
            long queueWaitMs = startedAt - enqueuedAt;
            if (requireAliveBeforeStart && !host.isAlive()) {
                if (queueWaitMs >= SLOW_TASK_LOG_THRESHOLD_MS) {
                    LogUtil.d(safeTag, "Skipped stale task after queueWait=" + queueWaitMs + "ms executor=" + getExecutorName(executor));
                }
                return;
            }
            try {
                T result = work.get();
                if (onSuccess != null) {
                    host.postToUi(() -> onSuccess.accept(result));
                }
            } catch (Exception e) {
                LogUtil.e(safeTag, "Background task failed", e);
                if (onError != null) {
                    host.postToUi(() -> onError.accept(e));
                }
            } finally {
                long finishedAt = SystemClock.elapsedRealtime();
                long executionMs = finishedAt - startedAt;
                long totalMs = finishedAt - enqueuedAt;
                if (queueWaitMs >= SLOW_TASK_LOG_THRESHOLD_MS || executionMs >= SLOW_TASK_LOG_THRESHOLD_MS || totalMs >= SLOW_TASK_LOG_THRESHOLD_MS) {
                    LogUtil.d(safeTag, "Task timing executor=" + getExecutorName(executor)
                            + " queue=" + queueWaitMs + "ms exec=" + executionMs + "ms total=" + totalMs + "ms thread=" + Thread.currentThread().getName());
                }
            }
        };
        executor.execute(task);
    }

    private static String getExecutorName(ExecutorService executor) {
        if (executor == IO_EXECUTOR) {
            return "io";
        }
        if (executor == SERIAL_EXECUTOR) {
            return "serial";
        }
        return "custom";
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger nextThreadNumber = new AtomicInteger(1);
        private final String threadNamePrefix;

        private NamedThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, threadNamePrefix + "-" + nextThreadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
