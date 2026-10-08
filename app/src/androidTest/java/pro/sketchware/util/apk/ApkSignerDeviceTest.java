package pro.sketchware.util.apk;

import static org.junit.Assert.*;

import android.content.Context;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.apksig.ApkVerifier;
import com.android.apksigner.ApkSignerTool;

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
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import pro.sketchware.core.library.BuiltInLibraries;
import pro.sketchware.R;
import pro.sketchware.activities.settings.AppSettings;

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

    @Before public void createFixture() throws Exception {
        originalOut = System.out;
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        directory = Files.createTempDirectory(target.getCacheDir().toPath(), "F07Signing.").toFile();
        status("signing_owned_fixture", directory.getAbsolutePath());
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
        if (directory != null) {
            try (var paths = Files.walk(directory.toPath())) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            assertFalse(directory.exists());
            status("signing_cleaned_fixture", directory.getAbsolutePath());
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
            assertEquals(2, lines.size());
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

    @Test public void missingInputWithoutTestkeyCallbackThrowsFailure() throws Exception {
        assertMissingInput(true, false);
    }

    @Test public void missingInputWithoutKeystoreCallbackThrowsFailure() throws Exception {
        assertMissingInput(false, false);
    }

    private void assertMissingInput(boolean useTestkey, boolean callback) throws Exception {
        List<String> lines = new ArrayList<>();
        File missing = new File(directory, "不存在.apk");
        assertThrows(IOException.class, () -> sign(useTestkey, missing, callback ? lines::add : null));
        assertSame(originalOut, System.out);
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

    @Test public void wrongStorePasswordReturnsFailureThenRetrySucceeds() throws Exception {
        List<String> lines = new ArrayList<>();
        sign(false, input, null);
        byte[] signed = Files.readAllBytes(output.toPath());
        File sidecar = new File(output + ".idsig");
        byte[] v4 = Files.readAllBytes(sidecar.toPath());
        assertThrows(IOException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                output.getAbsolutePath(), keystore.getAbsolutePath(), "wrong-store-password", ALIAS, PASSWORD, lines::add));
        assertSame(originalOut, System.out);
        assertArrayEquals(signed, Files.readAllBytes(output.toPath()));
        assertArrayEquals(v4, Files.readAllBytes(sidecar.toPath()));
        assertTrue(String.join("", lines).contains("Failed to sign APK"));
        assertFalse(String.join("", lines).contains("wrong-store-password"));
        assertSigning(false, true);
    }

    @Test public void wrongKeyPasswordWithoutCallbackReturnsFailureThenRetrySucceeds() throws Exception {
        assertThrows(IOException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                output.getAbsolutePath(), keystore.getAbsolutePath(), PASSWORD, ALIAS, "wrong-key-password", null));
        assertSame(originalOut, System.out);
        assertSigning(false, false);
    }

    @Test public void missingAliasReturnsFailureThenRetrySucceeds() throws Exception {
        assertThrows(GeneralSecurityException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                output.getAbsolutePath(), keystore.getAbsolutePath(), PASSWORD, "missing-alias", PASSWORD, null));
        assertSame(originalOut, System.out);
        assertSigning(false, true);
    }

    @Test public void absentAndCorruptKeystoreReturnFailureWithoutExiting() throws Exception {
        for (File file : List.of(new File(directory, "missing.keystore"), new File(directory, "corrupt.keystore"))) {
            if (file.getName().startsWith("corrupt")) Files.write(file.toPath(), new byte[]{1, 2, 3});
            assertThrows(IOException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                    output.getAbsolutePath(), file.getAbsolutePath(), PASSWORD, ALIAS, PASSWORD, null));
            assertSame(originalOut, System.out);
        }
        assertSigning(false, true);
    }

    @Test public void bridgePropagatesMissingInputAndSignsOnRetry() throws Exception {
        assertThrows(IOException.class, () -> TestkeySignBridge.signWithTestkey(
                new File(directory, "missing.apk").getAbsolutePath(), output.getAbsolutePath()));
        TestkeySignBridge.signWithTestkey(input.getAbsolutePath(), output.getAbsolutePath());
        assertVerified(output);
    }

    @Test public void inPlaceSigningPreservesPayloadAndProducesValidSignatures() throws Exception {
        byte[] manifest;
        byte[] payload;
        try (ZipFile before = new ZipFile(input)) {
            manifest = readBytes(before.getInputStream(before.getEntry("AndroidManifest.xml")));
            payload = readBytes(before.getInputStream(before.getEntry("assets/f07.txt")));
        }
        new ApkSigner().signWithKeyStore(input.getAbsolutePath(), input.getAbsolutePath(),
                keystore.getAbsolutePath(), PASSWORD, ALIAS, PASSWORD, null);
        assertVerified(input);
        assertTrue(new File(input + ".idsig").isFile());
        try (ZipFile after = new ZipFile(input)) {
            assertArrayEquals(manifest, readBytes(after.getInputStream(after.getEntry("AndroidManifest.xml"))));
            assertArrayEquals(payload, readBytes(after.getInputStream(after.getEntry("assets/f07.txt"))));
        }
    }

    @Test public void failedInPlaceSigningPreservesInputAndRemovesTemporaryApk() throws Exception {
        byte[] malformed = "This is not an APK".getBytes(StandardCharsets.UTF_8);
        Files.write(input.toPath(), malformed);
        var cache = context().getCacheDir().toPath();
        List<String> before = temporaryApks(cache);
        assertThrows(IOException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                input.getAbsolutePath(), keystore.getAbsolutePath(), PASSWORD, ALIAS, PASSWORD, null));
        assertArrayEquals(malformed, Files.readAllBytes(input.toPath()));
        assertEquals(before, temporaryApks(cache));
        assertSame(originalOut, System.out);
    }

    @Test public void concurrentFailureCannotChangeAnotherSigningResult() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var failing = workers.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                assertThrows(IOException.class, () -> new ApkSigner().signWithKeyStore(input.getAbsolutePath(),
                        new File(directory, "failure.apk").getAbsolutePath(), keystore.getAbsolutePath(),
                        "wrong-password", ALIAS, PASSWORD, line -> {}));
                return null;
            });
            var succeeding = workers.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                sign(true, input, null);
                return null;
            });
            start.countDown();
            failing.get(10, TimeUnit.SECONDS);
            succeeding.get(10, TimeUnit.SECONDS);
            assertVerified(output);
            assertFalse(new File(directory, "failure.apk").exists());
            assertSame(originalOut, System.out);
        } finally {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test public void apiMatchesCliApkAndV4OutputForBothSigningModes() throws Exception {
        for (boolean useTestkey : List.of(true, false)) {
            File cli = new File(directory, "cli-" + useTestkey + ".apk");
            if (useTestkey) {
                File keys = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "testkey");
                ApkSignerTool.main(new String[]{"sign", "--in", input.getAbsolutePath(), "--out", cli.getAbsolutePath(),
                        "--key", new File(keys, "testkey.pk8").getAbsolutePath(),
                        "--cert", new File(keys, "testkey.x509.pem").getAbsolutePath()});
            } else {
                ApkSignerTool.main(new String[]{"sign", "--in", input.getAbsolutePath(), "--out", cli.getAbsolutePath(),
                        "--ks", keystore.getAbsolutePath(), "--ks-pass", "pass:" + PASSWORD,
                        "--ks-key-alias", ALIAS, "--key-pass", "pass:" + PASSWORD});
            }
            sign(useTestkey, input, null);
            assertVerified(cli);
            assertVerified(output);
            assertArrayEquals(Files.readAllBytes(cli.toPath()), Files.readAllBytes(output.toPath()));
            assertArrayEquals(Files.readAllBytes(new File(cli + ".idsig").toPath()),
                    Files.readAllBytes(new File(output + ".idsig").toPath()));
        }
    }

    @Test public void settingsMissingInputThenSuccessShowsSuccessForThisAttempt() throws Exception {
        assertSettingsRetry(true, false);
    }

    @Test public void settingsWrongPasswordThenSuccessKeepsActivityAlive() throws Exception {
        assertSettingsRetry(false, true);
    }

    private void assertSettingsRetry(boolean useTestkey, boolean wrongPassword) throws Exception {
        // Drain the preceding test's Toast before observing this dialog's result.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(4000);
        output = new File(directory, "settings-retry-" + useTestkey + ".apk");
        ConcurrentLinkedQueue<String> toasts = new ConcurrentLinkedQueue<>();
        var automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        AccessibilityServiceInfo serviceInfo = automation.getServiceInfo();
        int originalFlags = serviceInfo.flags;
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        automation.setServiceInfo(serviceInfo);
        automation.setOnAccessibilityEventListener(event -> {
            if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
                for (CharSequence text : event.getText()) toasts.add(text.toString());
            }
        });
        try (ActivityScenario<AppSettings> scenario = ActivityScenario.launch(AppSettings.class)) {
            scenario.onActivity(activity -> invokeDialog(activity,
                    wrongPassword ? input : new File(directory, "missing.apk"), useTestkey,
                    wrongPassword ? "wrong-ui-password" : PASSWORD));
            String running = context().getString(R.string.signing_apk_progress);
            waitUntil(() -> progressText() != null && !progressText().equals(running));
            status("signing_ui_failure_progress", progressText());
            assertTrue(toasts.toString(), toasts.isEmpty());
            assertFalse(output.exists());
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            waitUntil(() -> progressText() == null);
            scenario.onActivity(activity -> invokeDialog(activity, input, useTestkey, PASSWORD));
            waitUntil(() -> apkIsVerified(output));
            assertVerified(output);
            status("signing_ui_retry_apk_verified", output.getName());
            String expected = String.format(context().getString(R.string.apk_sign_success), output.getName());
            waitUntil(() -> toasts.contains(expected));
            status("signing_ui_retry_success_toast", expected);
            waitUntil(() -> progressText() == null);
            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                assertFalse(activity.isDestroyed());
            });
        } finally {
            automation.setOnAccessibilityEventListener(null);
            serviceInfo.flags = originalFlags;
            automation.setServiceInfo(serviceInfo);
        }
    }

    private void invokeDialog(AppSettings activity, File apk, boolean useTestkey, String password) {
        try {
            Method method = AppSettings.class.getDeclaredMethod("signApkFileWithDialog", String.class, String.class,
                    boolean.class, String.class, String.class, String.class, String.class);
            method.setAccessible(true);
            method.invoke(activity, apk.getAbsolutePath(), output.getAbsolutePath(), useTestkey,
                    keystore.getAbsolutePath(), password, ALIAS, PASSWORD);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static String progressText() {
        var root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        if (root == null) return null;
        var nodes = root.findAccessibilityNodeInfosByViewId("pro.sketchware:id/tv_progress");
        if (nodes.isEmpty() || nodes.get(0).getText() == null) return null;
        return nodes.get(0).getText().toString();
    }

    private void assertVerified(File apk) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(apk).build().verify();
        assertTrue(result.getErrors().toString(), result.isVerified());
        assertEquals(List.of(testCertificate), result.getSignerCertificates());
        // The default verifier skips v1 for minSdk >= 24; explicitly verify each platform range.
        for (int version : new int[]{21, 24, 28}) {
            ApkVerifier.Result scheme = new ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(version)
                    .setMaxCheckedPlatformVersion(version == 21 ? 23 : version == 24 ? 27 : 32)
                    .build().verify();
            assertTrue(scheme.getErrors().toString(), scheme.isVerified());
            assertTrue("Signature scheme at SDK " + version, version == 21 ? scheme.isVerifiedUsingV1Scheme()
                    : version == 24 ? scheme.isVerifiedUsingV2Scheme() : scheme.isVerifiedUsingV3Scheme());
        }
    }

    private static boolean apkIsVerified(File apk) {
        try {
            return apk.isFile() && new ApkVerifier.Builder(apk).build().verify().isVerified();
        } catch (Exception incomplete) {
            return false;
        }
    }

    private static List<String> temporaryApks(java.nio.file.Path cache) throws Exception {
        try (var files = Files.list(cache)) {
            return files.map(path -> path.getFileName().toString()).filter(name -> name.startsWith("apksigner"))
                    .sorted().toList();
        }
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static void waitUntil(BooleanSupplier condition) throws Exception {
        long deadline = android.os.SystemClock.elapsedRealtime() + 10000;
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(100);
        }
        assertTrue("Timed out waiting for signing UI", condition.getAsBoolean());
    }

    private static void status(String name, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(name, value);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, bundle);
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
