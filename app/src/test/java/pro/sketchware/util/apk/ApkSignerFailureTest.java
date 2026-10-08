package pro.sketchware.util.apk;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exercises real key loading in-process: failed keys must throw, never exit the caller. */
public class ApkSignerFailureTest {
    private static final String PASSWORD = "signing-test-password";
    private static final String ALIAS = "testkey";
    private Path directory;
    private File keystore;
    private File input;
    private File output;
    private PrintStream originalOut;

    @Before public void createFixture() throws Exception {
        originalOut = System.out;
        directory = Files.createTempDirectory("apk-signer-test");
        keystore = directory.resolve("fixture.keystore").toFile();
        input = directory.resolve("malformed.apk").toFile();
        output = directory.resolve("signed.apk").toFile();
        Files.write(input.toPath(), new byte[]{1, 2, 3});
        Certificate certificate;
        byte[] key;
        try (var stream = getClass().getResourceAsStream("/keys/testkey.x509.pem")) {
            assertNotNull(stream);
            certificate = CertificateFactory.getInstance("X.509").generateCertificate(stream);
        }
        try (var stream = getClass().getResourceAsStream("/keys/testkey.pk8")) {
            assertNotNull(stream);
            key = stream.readAllBytes();
        }
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, PASSWORD.toCharArray());
        store.setKeyEntry(ALIAS, KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key)),
                PASSWORD.toCharArray(), new Certificate[]{certificate});
        try (var stream = Files.newOutputStream(keystore.toPath())) {
            store.store(stream, PASSWORD.toCharArray());
        }
    }

    @After public void cleanFixture() throws Exception {
        System.setOut(originalOut);
        if (directory != null) {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    @Test public void wrongStorePasswordThrowsAndLogsWithoutExposingPassword() {
        List<String> lines = new ArrayList<>();
        assertThrows(IOException.class, () -> sign("wrong-store-password", ALIAS, PASSWORD, lines::add));
        assertSame(originalOut, System.out);
        String log = String.join("", lines);
        assertTrue(log, log.contains("Failed to sign APK"));
        assertTrue(log, log.contains("Signing an APK took"));
        assertFalse(log.contains("wrong-store-password"));
        assertFalse(output.exists());
    }

    @Test public void wrongKeyPasswordThrowsWithoutCallback() {
        assertThrows(IOException.class, () -> sign(PASSWORD, ALIAS, "wrong-key-password", null));
        assertSame(originalOut, System.out);
        assertFalse(output.exists());
    }

    @Test public void missingAliasThrowsWithoutExiting() {
        assertThrows(GeneralSecurityException.class, () -> sign(PASSWORD, "missing-alias", PASSWORD, null));
        assertSame(originalOut, System.out);
        assertFalse(output.exists());
    }

    @Test public void missingKeystoreThrowsWithoutExiting() throws Exception {
        Files.delete(keystore.toPath());
        assertThrows(IOException.class, () -> sign(PASSWORD, ALIAS, PASSWORD, null));
        assertSame(originalOut, System.out);
    }

    @Test public void corruptKeystoreThrowsWithoutExiting() throws Exception {
        Files.write(keystore.toPath(), new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> sign(PASSWORD, ALIAS, PASSWORD, null));
        assertSame(originalOut, System.out);
    }

    @Test public void malformedApkThrowsAfterLoadingAValidKey() {
        assertThrows(IOException.class, () -> sign(PASSWORD, ALIAS, PASSWORD, null));
        assertSame(originalOut, System.out);
    }

    private void sign(String storePassword, String alias, String keyPassword,
                      ApkSigner.LogCallback callback) throws Exception {
        new ApkSigner().signWithKeyStore(input.getAbsolutePath(), output.getAbsolutePath(),
                keystore.getAbsolutePath(), storePassword, alias, keyPassword, callback);
    }
}
