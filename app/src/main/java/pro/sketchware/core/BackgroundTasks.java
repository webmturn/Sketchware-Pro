package pro.sketchware.core;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import mod.jbk.util.LogUtil;

public final class BackgroundTasks {
    private static final int IO_THREAD_COUNT = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors(), 4));
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(IO_THREAD_COUNT, new NamedThreadFactory("Sketchware-IO"));
    private static final ExecutorService SERIAL_EXECUTOR = Executors.newSingleThreadExecutor(new NamedThreadFactory("Sketchware-Serial"));

    private BackgroundTasks() {
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

    public static <T> void callIo(TaskHost host, String tag, ThrowingSupplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        execute(IO_EXECUTOR, host, tag, work, onSuccess, onError);
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
        if (executor == null || host == null || work == null) {
            throw new IllegalArgumentException("Executor, host, and work must not be null");
        }
        String safeTag = tag != null && !tag.isEmpty() ? tag : "BackgroundTasks";
        executor.execute(() -> {
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
            }
        });
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
