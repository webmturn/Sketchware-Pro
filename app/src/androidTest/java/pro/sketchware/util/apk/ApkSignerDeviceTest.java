package pro.sketchware.util.apk;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.apksig.ApkVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import pro.sketchware.core.library.BuiltInLibraries;

/** Signs disposable APKs with the repository's public testkey, never user keystores. */
@RunWith(AndroidJUnit4.class)
public class ApkSignerDeviceTest {
    private static final String PASSWORD = "f07-test-password";
    private static final String ALIAS = "f07-testkey";
    private File directory;
    private File input;
    private File output;
    private File keystore;
    private Certificate testCertificate;
    private Map<String, byte[]> testKeys;
    private PrintStream originalOut;
    private int originalErrors;

    @Before public void createFixture() throws Exception {
        originalOut = System.out;
        originalErrors = ApkSigner.LogCallback.errorCount.get();
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        directory = Files.createTempDirectory(target.getCacheDir().toPath(), "F07Signing.").toFile();
        input = new File(directory, "输入.apk");
        output = new File(directory, "signed.apk");
        keystore = new File(directory, "test.keystore");

        // Reuse a real compiled manifest; this tiny APK is only signed and verified, never installed.
        Context tests = InstrumentationRegistry.getInstrumentation().getContext();
        try (ZipFile apk = new ZipFile(tests.getApplicationInfo().sourceDir);
             ZipOutputStream fixture = new ZipOutputStream(new FileOutputStream(input))) {
            fixture.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            try (var manifest = apk.getInputStream(apk.getEntry("AndroidManifest.xml"))) {
                copy(manifest, fixture);
            }
            fixture.closeEntry();
            fixture.putNextEntry(new ZipEntry("assets/f07.txt"));
            fixture.write("F07 isolated signing fixture 中文🙂".getBytes(StandardCharsets.UTF_8));
            fixture.closeEntry();
        }

        testKeys = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(target.getAssets().open("libs/testkey.zip"))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) testKeys.put(new File(entry.getName()).getName(), readBytes(zip));
            }
        }
        testCertificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(testKeys.get("testkey.x509.pem")));
        var key = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(testKeys.get("testkey.pk8")));
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, PASSWORD.toCharArray());
        store.setKeyEntry(ALIAS, key, PASSWORD.toCharArray(), new Certificate[]{testCertificate});
        try (FileOutputStream stream = new FileOutputStream(keystore)) {
            store.store(stream, PASSWORD.toCharArray());
        }
    }

    @After public void cleanFixture() throws Exception {
        System.setOut(originalOut);
        ApkSigner.LogCallback.errorCount.set(originalErrors);
        if (directory != null) {
            try (var paths = Files.walk(directory.toPath())) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            assertFalse(directory.exists());
        }
    }

    @Test public void testkeySignsAndReportsCompleteLog() throws Exception {
        assertSigning(true, true);
    }

    @Test public void keystoreSignsAndReportsCompleteLogWithoutPasswords() throws Exception {
        assertSigning(false, true);
    }

    @Test public void testkeySignsWithoutCallback() throws Exception {
        assertSigning(true, false);
    }

    @Test public void keystoreSignsWithoutCallback() throws Exception {
        assertSigning(false, false);
    }

    private void assertSigning(boolean useTestkey, boolean callback) throws Exception {
        byte[] originalInput = Files.readAllBytes(input.toPath());
        List<String> lines = new ArrayList<>();
        sign(useTestkey, input, callback ? lines::add : null);
        assertSame(originalOut, System.out);
        assertEquals(originalErrors, ApkSigner.LogCallback.errorCount.get());
        assertTrue(output.isFile());
        ApkVerifier.Result result = new ApkVerifier.Builder(output).build().verify();
        assertTrue(result.getErrors().toString(), result.isVerified());
        assertEquals(List.of(testCertificate), result.getSignerCertificates());
        assertArrayEquals(originalInput, Files.readAllBytes(input.toPath()));
        try (ZipFile before = new ZipFile(input); ZipFile after = new ZipFile(output)) {
            for (String name : List.of("AndroidManifest.xml", "assets/f07.txt")) {
                try (var first = before.getInputStream(before.getEntry(name));
                     var second = after.getInputStream(after.getEntry(name))) {
                    assertArrayEquals(readBytes(first), readBytes(second));
                }
            }
        }
        if (callback) {
            assertEquals(2, lines.size()); // The non-verbose signing tool itself is silent on success.
            assertTrue(lines.get(0).startsWith("Signing an APK"));
            assertTrue(lines.get(1).matches("Signing an APK( file)? took \\d+ ms\n"));
            assertFalse(String.join("", lines).contains(PASSWORD));
        } else {
            assertTrue(lines.isEmpty());
        }
    }

    @Test public void missingInputWithTestkeyReportsFailureAndRestoresOutput() throws Exception {
        assertMissingInput(true, true);
    }

    @Test public void missingInputWithKeystoreReportsFailureAndRestoresOutput() throws Exception {
        assertMissingInput(false, true);
    }

    @Test public void missingInputWithoutTestkeyCallbackStillRecordsFailure() throws Exception {
        assertMissingInput(true, false);
    }

    @Test public void missingInputWithoutKeystoreCallbackStillRecordsFailure() throws Exception {
        assertMissingInput(false, false);
    }

    private void assertMissingInput(boolean useTestkey, boolean callback) throws Exception {
        List<String> lines = new ArrayList<>();
        File missing = new File(directory, "不存在.apk");
        sign(useTestkey, missing, callback ? lines::add : null);
        assertSame(originalOut, System.out);
        assertEquals(originalErrors + 1, ApkSigner.LogCallback.errorCount.get());
        assertFalse(output.exists());
        if (callback) {
            String log = String.join("", lines);
            assertTrue(log, log.contains("FileNotFoundException"));
            assertTrue(log, log.contains(missing.getAbsolutePath()));
            assertTrue(log, lines.get(lines.size() - 1).matches("Signing an APK( file)? took \\d+ ms\n"));
            assertFalse(log.contains(PASSWORD));
        } else {
            assertTrue(lines.isEmpty());
        }
    }

    private void sign(boolean useTestkey, File apk, ApkSigner.LogCallback callback) throws Exception {
        ApkSigner signer = new ApkSigner();
        if (useTestkey) {
            File extracted = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "testkey");
            // Only read the existing built-in keys, and ensure they are the known public testkey.
            for (String name : List.of("testkey.pk8", "testkey.x509.pem")) {
                assertArrayEquals(testKeys.get(name), Files.readAllBytes(new File(extracted, name).toPath()));
            }
            signer.signWithTestKey(apk.getAbsolutePath(), output.getAbsolutePath(), callback);
        } else {
            signer.signWithKeyStore(apk.getAbsolutePath(), output.getAbsolutePath(),
                    keystore.getAbsolutePath(), PASSWORD, ALIAS, PASSWORD, callback);
        }
    }

    private static byte[] readBytes(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        copy(input, bytes);
        return bytes.toByteArray();
    }

    private static void copy(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
    }
}
