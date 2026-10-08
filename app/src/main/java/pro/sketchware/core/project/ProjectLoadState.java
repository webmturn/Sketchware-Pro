package pro.sketchware.core.project;

import java.io.IOException;

/** Keeps failed or incomplete reads from being persisted as valid project data. */
final class ProjectLoadState {
    private volatile IOException failure;
    private volatile int loadingReads;

    boolean canSave() {
        return loadingReads == 0 && failure == null;
    }

    IOException getFailure() {
        return failure;
    }

    void failed(Throwable error) {
        failure = error instanceof IOException ioException
                ? ioException : new IOException("Invalid project data", error);
    }

    synchronized void read(ReadOperation operation) throws IOException {
        loadingReads++;
        try {
            operation.read();
            failure = null;
        } catch (IOException | RuntimeException error) {
            failed(error);
            throw failure;
        } finally {
            loadingReads--;
        }
    }

    @FunctionalInterface
    interface ReadOperation {
        void read() throws IOException;
    }
}
