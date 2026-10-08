package pro.sketchware.util.apk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.apksigner.PasswordRetriever;
import com.android.apksigner.SignerParams;
import com.android.apksig.apk.ApkFormatException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;

import org.conscrypt.OpenSSLProvider;

import pro.sketchware.core.library.BuiltInLibraries;

public class ApkSigner {

    private static final Object SIGNING_OUTPUT_LOCK = new Object();

    /**
     * Sign an APK with testkey.
     *
     * @param inputPath  The APK file to sign
     * @param outputPath File to output the signed APK to
     * @param callback   Callback for System.out during signing. May be null
     * @throws IOException if an input or output file cannot be read or written
     * @throws GeneralSecurityException if the key or APK cannot be signed
     */
    public void signWithTestKey(@NonNull String inputPath, @NonNull String outputPath,
                                @Nullable LogCallback callback) throws IOException, GeneralSecurityException {
        File testkeyDirectory = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "testkey");
        SignerParams params = new SignerParams();
        params.setKeyFile(new File(testkeyDirectory, "testkey.pk8").getAbsolutePath());
        params.setCertFile(new File(testkeyDirectory, "testkey.x509.pem").getAbsolutePath());
        sign(new File(inputPath), new File(outputPath), params, "testkey", callback);
    }

    public void signWithKeyStore(@NonNull String inputFilePath, @NonNull String outputFilePath,
                                 @NonNull String keyStorePath, @NonNull String keyStorePassword,
                                 @NonNull String keyStoreKeyAlias, @NonNull String keyPassword,
                                 @Nullable LogCallback callback) throws IOException, GeneralSecurityException {
        SignerParams params = new SignerParams();
        params.setKeystoreFile(keyStorePath);
        params.setKeystorePasswordSpec("pass:" + keyStorePassword);
        params.setKeystoreKeyAlias(keyStoreKeyAlias);
        params.setKeyPasswordSpec("pass:" + keyPassword);
        sign(new File(inputFilePath), new File(outputFilePath), params, keyStoreKeyAlias, callback);
    }

    private static void sign(File input, File output, SignerParams params, String signatureName,
                             LogCallback callback) throws IOException, GeneralSecurityException {
        try (LogWriter logger = new LogWriter(callback)) {
            long savedTimeMillis = System.currentTimeMillis();
            logger.writeLine("Signing an APK: " + input);
            try {
                runWithOutput(logger, () -> {
                    try {
                        // Preserve the CLI's optional provider and password/key-loading behavior.
                        try {
                            Security.addProvider(new OpenSSLProvider());
                        } catch (UnsatisfiedLinkError ignored) {
                            // The CLI also uses the platform providers when native Conscrypt is unavailable.
                        }
                        params.setName("signer #1");
                        try (PasswordRetriever passwords = new PasswordRetriever()) {
                            params.loadPrivateKeyAndCerts(passwords);
                        }
                        var signer = new com.android.apksig.ApkSigner.SignerConfig.Builder(
                                signatureName, params.getPrivateKey(), params.getCerts(), false).build();
                        signApk(input, output, signer);
                    } catch (Exception error) {
                        StringWriter trace = new StringWriter();
                        error.printStackTrace(new PrintWriter(trace));
                        logger.writeLine("Failed to sign APK: " + trace);
                        throw error;
                    } finally {
                        logger.writeLine("Signing an APK took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
                    }
                });
            } catch (IOException | GeneralSecurityException | RuntimeException error) {
                throw error;
            } catch (ApkFormatException error) {
                throw new IOException("Invalid APK: " + error.getMessage(), error);
            } catch (Exception error) {
                throw new GeneralSecurityException("APK signing failed", error);
            }
        }
    }

    private static void signApk(File input, File output, com.android.apksig.ApkSigner.SignerConfig signer) throws Exception {
        boolean inPlace = input.getCanonicalPath().equals(output.getCanonicalPath());
        File signed = inPlace ? File.createTempFile("apksigner", ".apk") : output;
        try {
            File v4Signature = new File(output.getCanonicalPath() + ".idsig");
            Files.deleteIfExists(v4Signature.toPath());
            // Match the defaults of our bundled apksigner CLI, including the v4 sidecar.
            new com.android.apksig.ApkSigner.Builder(List.of(signer))
                    .setInputApk(input)
                    .setOutputApk(signed)
                    .setOtherSignersSignaturesPreserved(false)
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .setV4SigningEnabled(true)
                    .setV4ErrorReportingEnabled(false)
                    .setForceSourceStampOverwrite(false)
                    .setVerityEnabled(false)
                    .setDebuggableApkPermitted(true)
                    .setV4SignatureOutputFile(v4Signature)
                    .build().sign();
            if (inPlace) Files.move(signed.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (inPlace) signed.delete();
        }
    }

    static void runWithOutput(LogWriter logger, SigningOperation signing) throws Exception {
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

    @FunctionalInterface
    interface SigningOperation {
        void run() throws Exception;
    }

    public interface LogCallback {
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

        private void writeLine(String message) {
            if (isLoggingDisabled()) return;
            if (mCache.size() > 0) write('\n');
            write(message);
            if (!message.endsWith("\n")) write('\n');
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
