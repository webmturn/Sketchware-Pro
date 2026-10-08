package pro.sketchware.util.apk;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.apksigner.ApkSignerTool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import pro.sketchware.core.library.BuiltInLibraries;

public class ApkSigner {

    private static final Object SIGNING_OUTPUT_LOCK = new Object();

    /**
     * Sign an APK with testkey.
     *
     * @param inputPath  The APK file to sign
     * @param outputPath File to output the signed APK to
     * @param callback   Callback for System.out during signing. May be null
     */
    public void signWithTestKey(@NonNull String inputPath, @NonNull String outputPath, @Nullable LogCallback callback) {
        try (LogWriter logger = new LogWriter(callback)) {
            long savedTimeMillis = System.currentTimeMillis();
            File testkeyDirectory = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "testkey");

            List<String> args = Arrays.asList(
                    "sign",
                    "--in",
                    inputPath,
                    "--out",
                    outputPath,
                    "--key",
                    new File(testkeyDirectory, "testkey.pk8").getAbsolutePath(),
                    "--cert",
                    new File(testkeyDirectory, "testkey.x509.pem").getAbsolutePath()
            );

            logger.write("Signing an APK file with these arguments: " + args + "\n");
            runWithOutput(logger, () -> {
                try {
                    ApkSignerTool.main(args.toArray(new String[0]));
                } catch (Exception e) {
                    LogCallback.errorCount.incrementAndGet();
                    logger.write("An error occurred while trying to sign the APK file " + inputPath +
                            " and outputting it to " + outputPath + ": " + e.getMessage() + "\n" +
                            "Stack trace: " + Log.getStackTraceString(e));
                }
                logger.write("Signing an APK file took " + (System.currentTimeMillis() - savedTimeMillis) + " ms\n");
            });
        } catch (IOException e) {
            Log.e("ApkSigner", e.getMessage(), e);
        }
    }

    public void signWithKeyStore(@NonNull String inputFilePath, @NonNull String outputFilePath,
                                 @NonNull String keyStorePath, @NonNull String keyStorePassword,
                                 @NonNull String keyStoreKeyAlias, @NonNull String keyPassword, @Nullable LogCallback callback) {
        try (LogWriter logger = new LogWriter(callback)) {
            long savedTimeMillis = System.currentTimeMillis();

            List<String> args = Arrays.asList(
                    "sign",
                    "--in",
                    inputFilePath,
                    "--out",
                    outputFilePath,
                    "--ks",
                    keyStorePath,
                    "--ks-pass",
                    "pass:" + keyStorePassword,
                    "--ks-key-alias",
                    keyStoreKeyAlias,
                    "--key-pass",
                    "pass:" + keyPassword
            );

            logger.write("Signing an APK with a JKS keystore\n");
            runWithOutput(logger, () -> {
                try {
                    ApkSignerTool.main(args.toArray(new String[0]));
                } catch (Exception e) {
                    LogCallback.errorCount.incrementAndGet();
                    logger.write("Failed to sign APK with JKS keystore: " + Log.getStackTraceString(e));
                }
                logger.write("Signing an APK took " + (System.currentTimeMillis() - savedTimeMillis) + " ms\n");
            });
        } catch (IOException e) {
            Log.e("ApkSigner", e.getMessage(), e);
        }
    }

    static void runWithOutput(LogWriter logger, Runnable signing) throws IOException {
        // System.out is shared by all signer instances, including signing without a callback.
        synchronized (SIGNING_OUTPUT_LOCK) {
            if (logger.isLoggingDisabled()) {
                signing.run();
                return;
            }
            PrintStream oldOut = System.out;
            try (PrintStream stream = new PrintStream(logger, false, StandardCharsets.UTF_8.name())) {
                System.setOut(stream);
                try {
                    signing.run();
                } finally {
                    System.setOut(oldOut);
                }
            }
        }
    }

    public interface LogCallback {
        AtomicInteger errorCount = new AtomicInteger(0);

        void onNewLineLogged(String line);
    }

    static class LogWriter extends OutputStream {

        private final LogCallback mCallback;
        private final ByteArrayOutputStream mCache = new ByteArrayOutputStream();

        LogWriter(LogCallback callback) {
            mCallback = callback;
        }

        @Override
        public void write(int b) {
            if (isLoggingDisabled()) return;

            mCache.write(b);
            if (b == '\n') dispatchPendingLine();
        }

        private void write(String s) {
            if (isLoggingDisabled()) return;

            for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
                write(b);
            }
        }

        @Override
        public void close() {
            dispatchPendingLine();
        }

        private void dispatchPendingLine() {
            if (isLoggingDisabled() || mCache.size() == 0) return;
            String line = new String(mCache.toByteArray(), StandardCharsets.UTF_8);
            mCache.reset();
            mCallback.onNewLineLogged(line);
        }

        private boolean isLoggingDisabled() {
            return mCallback == null;
        }
    }
}
