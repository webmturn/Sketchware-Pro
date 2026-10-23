package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.tools.ToolProvider;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.x509.X509V3CertificateGenerator;

/** Exercises the generated client against local TLS servers, without public network access. */
public class RequestNetworkTlsTest {
    @ClassRule public static TemporaryFolder temporary = new TemporaryFolder();

    private static URLClassLoader generatedClasses;
    private static Class<?> controller;
    private static File trustStoreFile;
    private static KeyPair serverKey;
    private static X509Certificate[] validChain;
    private static X509Certificate[] wrongHostnameChain;
    private static X509Certificate[] expiredChain;
    private static X509Certificate[] untrustedChain;

    @BeforeClass public static void compileTemplatesAndCreateCertificates() throws Exception {
        File output = temporary.newFolder("generated");
        File controllerSource = new File(output, "RequestNetworkController.java");
        File networkSource = new File(output, "RequestNetwork.java");
        Files.write(controllerSource.toPath(), ComponentTemplates.getRequestNetworkControllerCode("tls.generated").getBytes(StandardCharsets.UTF_8));
        Files.write(networkSource.toPath(), ComponentTemplates.getRequestNetworkCode("tls.generated").getBytes(StandardCharsets.UTF_8));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("Run these JVM tests with a JDK", compiler);
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            assertTrue("Generated network sources must compile", compiler.getTask(null, files, null,
                    List.of("-proc:none", "-classpath", compilationClasspath(), "-d", output.getAbsolutePath()),
                    null, files.getJavaFileObjects(controllerSource, networkSource)).call());
        }
        generatedClasses = new URLClassLoader(new URL[]{output.toURI().toURL()}, RequestNetworkTlsTest.class.getClassLoader());
        controller = generatedClasses.loadClass("tls.generated.RequestNetworkController");

        KeyPairGenerator keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        KeyPair rootKey = keys.generateKeyPair();
        KeyPair untrustedRootKey = keys.generateKeyPair();
        serverKey = keys.generateKeyPair();
        long now = System.currentTimeMillis();
        long day = TimeUnit.DAYS.toMillis(1);
        X509Certificate root = certificate(rootKey, "Test Root", "Test Root", rootKey,
                null, now - 7 * day, now + 7 * day);
        X509Certificate untrustedRoot = certificate(untrustedRootKey, "Untrusted Root", "Untrusted Root", untrustedRootKey,
                null, now - 7 * day, now + 7 * day);
        validChain = new X509Certificate[]{certificate(serverKey, "localhost", "Test Root", rootKey,
                "localhost", now - day, now + day), root};
        wrongHostnameChain = new X509Certificate[]{certificate(serverKey, "wrong.invalid", "Test Root", rootKey,
                "wrong.invalid", now - day, now + day), root};
        expiredChain = new X509Certificate[]{certificate(serverKey, "localhost", "Test Root", rootKey,
                "localhost", now - 3 * day, now - day), root};
        untrustedChain = new X509Certificate[]{certificate(serverKey, "localhost", "Untrusted Root", untrustedRootKey,
                "localhost", now - day, now + day), untrustedRoot};

        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setCertificateEntry("root", root);
        trustStoreFile = temporary.newFile("truststore.p12");
        try (var stream = Files.newOutputStream(trustStoreFile.toPath())) {
            store.store(stream, "test-password".toCharArray());
        }
    }

    @AfterClass public static void closeGeneratedClasses() throws Exception {
        if (generatedClasses != null) generatedClasses.close();
    }

    @Test public void acceptsTrustedCertificateForCorrectHostname() throws Exception {
        OkHttpClient client = newClient();
        try (TlsServer server = new TlsServer(validChain);
             var response = client.newCall(request(server)).execute()) {
            assertEquals(200, response.code());
            assertEquals("tls-ok", response.body().string());
        } finally { client.connectionPool().evictAll(); }
    }

    @Test public void rejectsUntrustedCertificate() throws Exception {
        OkHttpClient client = newClient();
        try (TlsServer server = new TlsServer(untrustedChain)) {
            assertThrows(SSLHandshakeException.class, () -> client.newCall(request(server)).execute().close());
        } finally { client.connectionPool().evictAll(); }
    }

    @Test public void rejectsWrongHostnameEvenWhenIssuerIsTrusted() throws Exception {
        OkHttpClient client = newClient();
        try (TlsServer server = new TlsServer(wrongHostnameChain)) {
            assertThrows(SSLPeerUnverifiedException.class, () -> client.newCall(request(server)).execute().close());
        } finally { client.connectionPool().evictAll(); }
    }

    @Test public void rejectsExpiredCertificateEvenWhenIssuerAndHostnameMatch() throws Exception {
        OkHttpClient client = newClient();
        try (TlsServer server = new TlsServer(expiredChain)) {
            assertThrows(SSLHandshakeException.class, () -> client.newCall(request(server)).execute().close());
        } finally { client.connectionPool().evictAll(); }
    }

    @Test public void preservesTimeoutsAndReusesClient() throws Exception {
        Object instance = controller.getDeclaredConstructor().newInstance();
        OkHttpClient client = client(instance);
        assertSame(client, client(instance));
        assertEquals(15_000, client.connectTimeoutMillis());
        assertEquals(25_000, client.readTimeoutMillis());
        assertEquals(25_000, client.writeTimeoutMillis());
    }

    private static Request request(TlsServer server) {
        return new Request.Builder().url("https://localhost:" + server.socket.getLocalPort() + "/").build();
    }

    private static OkHttpClient newClient() throws Exception {
        return client(controller.getDeclaredConstructor().newInstance());
    }

    private static OkHttpClient client(Object instance) throws Exception {
        // Give the generated client a deterministic JVM trust store; restore it immediately
        // after construction. Certificate and hostname validation remain production defaults.
        List<String> names = List.of("javax.net.ssl.trustStore", "javax.net.ssl.trustStoreType", "javax.net.ssl.trustStorePassword");
        String[] previous = names.stream().map(System::getProperty).toArray(String[]::new);
        try {
            System.setProperty(names.get(0), trustStoreFile.getAbsolutePath());
            System.setProperty(names.get(1), "PKCS12");
            System.setProperty(names.get(2), "test-password");
            Method getClient = controller.getDeclaredMethod("getClient");
            getClient.setAccessible(true);
            return (OkHttpClient) getClient.invoke(instance);
        } finally {
            for (int i = 0; i < names.size(); i++) {
                if (previous[i] == null) System.clearProperty(names.get(i));
                else System.setProperty(names.get(i), previous[i]);
            }
        }
    }

    private static String compilationClasspath() throws Exception {
        Set<String> paths = new LinkedHashSet<>(Arrays.asList(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator))));
        for (ClassLoader loader = RequestNetworkTlsTest.class.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (URL url : urls.getURLs()) {
                    if (url.getProtocol().equals("file")) paths.add(new File(url.toURI()).getAbsolutePath());
                }
            }
        }
        return String.join(File.pathSeparator, paths);
    }

    @SuppressWarnings("deprecation")
    private static X509Certificate certificate(KeyPair subjectKey, String subject, String issuer, KeyPair issuerKey,
                                               String hostname, long start, long end) throws Exception {
        X509V3CertificateGenerator generator = new X509V3CertificateGenerator();
        generator.setSerialNumber(new BigInteger(128, new java.security.SecureRandom()));
        generator.setSubjectDN(new X509Principal("CN=" + subject));
        generator.setIssuerDN(new X509Principal("CN=" + issuer));
        generator.setNotBefore(new Date(start));
        generator.setNotAfter(new Date(end));
        generator.setPublicKey(subjectKey.getPublic());
        generator.setSignatureAlgorithm("SHA256withRSA");
        generator.addExtension("2.5.29.19", true, new BasicConstraints(hostname == null));
        if (hostname != null) {
            generator.addExtension("2.5.29.17", false, new GeneralNames(new GeneralName(GeneralName.dNSName, hostname)));
        }
        byte[] encoded = generator.generate(issuerKey.getPrivate(), "SunRsaSign").getEncoded();
        return (X509Certificate) java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(encoded));
    }

    private static final class TlsServer implements AutoCloseable {
        private final SSLServerSocket socket;
        private final ExecutorService worker = Executors.newSingleThreadExecutor();

        TlsServer(X509Certificate[] chain) throws Exception {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(null, null);
            char[] password = "test-password".toCharArray();
            store.setKeyEntry("server", serverKey.getPrivate(), password, chain);
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(store, password);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), null, null);
            socket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            worker.submit(() -> {
                try (SSLSocket connection = (SSLSocket) socket.accept()) {
                    connection.setSoTimeout(5_000);
                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.US_ASCII));
                    for (String line; (line = input.readLine()) != null && !line.isEmpty();) { }
                    connection.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 6\r\nConnection: close\r\n\r\ntls-ok".getBytes(StandardCharsets.US_ASCII));
                    connection.getOutputStream().flush();
                } catch (java.io.IOException expectedForRejectedHandshake) {
                    // The negative cases close the connection during TLS validation.
                }
            });
        }

        @Override public void close() throws Exception {
            socket.close();
            worker.shutdownNow();
            assertTrue("TLS server worker must stop", worker.awaitTermination(6, TimeUnit.SECONDS));
        }
    }
}
