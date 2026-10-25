package pro.sketchware.core.debug;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RemoteAdbCommandsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void streamsBinaryApkWithoutChangingBytesAndReportsCompletion() throws Exception {
        byte[] bytes = new byte[100003];
        new Random(42).nextBytes(bytes);
        File apk = temporary.newFile("app with spaces.apk");
        Files.write(apk.toPath(), bytes);
        FakeTransport transport = new FakeTransport("Success\n");
        List<Integer> progress = new ArrayList<>();
        new RemoteAdbCommands(transport).install(apk, 10, progress::add);
        assertArrayEquals(bytes, transport.channels.get(0).written.toByteArray());
        assertEquals("exec:cmd package install -r -t --user 10 -S 100003", transport.services.get(0));
        assertEquals(Integer.valueOf(100), progress.get(progress.size() - 1));
        assertTrue(transport.channels.get(0).closed);
    }

    @Test public void signatureConflictIsReturnedWithoutUninstallingOrDowngrading() throws Exception {
        File apk = temporary.newFile();
        Files.write(apk.toPath(), new byte[]{0, 1, -1});
        FakeTransport transport = new FakeTransport("Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures differ]\n");
        IOException error = assertThrows(IOException.class,
                () -> new RemoteAdbCommands(transport).install(apk, 0, ignored -> { }));
        assertTrue(error.getMessage().contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE"));
        assertEquals(1, transport.services.size());
        assertTrue(transport.channels.get(0).closed);
    }

    @Test public void emptyApkDoesNotOpenAnInstallSession() throws Exception {
        FakeTransport transport = new FakeTransport();
        assertThrows(IOException.class, () -> new RemoteAdbCommands(transport)
                .install(temporary.newFile(), 0, ignored -> { }));
        assertTrue(transport.services.isEmpty());
    }

    @Test public void commandWithoutExitMarkerFailsInsteadOfReportingSuccess() {
        FakeTransport transport = new FakeTransport("some partial output\n");
        assertThrows(IOException.class, () -> new RemoteAdbCommands(transport).currentUser());
        assertTrue(transport.channels.get(0).closed);
    }

    @Test public void nonzeroExitIncludesDeviceError() {
        FakeTransport transport = new FakeTransport(exit("Permission denied", 1));
        IOException error = assertThrows(IOException.class, () -> new RemoteAdbCommands(transport).currentUser());
        assertTrue(error.getMessage().contains("Permission denied"));
    }

    @Test public void selectsExactPackageUidRatherThanSimilarPackageName() throws Exception {
        FakeTransport transport = new FakeTransport(exit("package:com.example.app.extra uid:10123\npackage:com.example.app uid:1010124", 0));
        assertEquals(1010124, new RemoteAdbCommands(transport).uid("com.example.app", 10));
        assertTrue(transport.services.get(0).contains("--user 10"));
    }

    @Test public void missingLauncherDoesNotSendLaunchCommand() {
        FakeTransport transport = new FakeTransport(exit("No activity found", 0));
        assertThrows(IOException.class, () -> new RemoteAdbCommands(transport).launch("com.example.app", 0));
        assertEquals(1, transport.services.size());
    }

    @Test public void launchesResolvedComponentForTheSelectedUser() throws Exception {
        FakeTransport transport = new FakeTransport(exit("priority=0\ncom.example.app/.MainActivity", 0), exit("Starting: Intent", 0));
        new RemoteAdbCommands(transport).launch("com.example.app", 10);
        assertTrue(transport.services.get(1).startsWith("shell:am start --user 10 -S -n 'com.example.app/.MainActivity'"));
    }

    @Test public void rejectsComponentFromAnotherPackage() {
        FakeTransport transport = new FakeTransport(exit("com.other/.MainActivity", 0));
        assertThrows(IOException.class, () -> new RemoteAdbCommands(transport).launch("com.example.app", 0));
        assertEquals(1, transport.services.size());
    }

    @Test public void rejectsPackageShellInjectionBeforeOpeningAChannel() {
        FakeTransport transport = new FakeTransport();
        assertThrows(IllegalArgumentException.class,
                () -> new RemoteAdbCommands(transport).launch("com.example; echo injected", 0));
        assertTrue(transport.services.isEmpty());
    }

    @Test public void logStreamUsesUidAndDeviceTimestampIncludingStartup() {
        String command = new RemoteAdbCommands(new FakeTransport()).logService(10123, "1792890000.000");
        assertTrue(command.contains("--uid=10123"));
        assertTrue(command.endsWith("-T '1792890000.000'"));
        assertTrue(command.contains("-b crash"));
    }

    private static String exit(String output, int code) {
        return output + "\n__SKETCHWARE_REMOTE_EXIT__:" + code + "\n";
    }

    private static final class FakeTransport implements RemoteAdbCommands.Transport {
        final ArrayDeque<String> responses = new ArrayDeque<>();
        final List<String> services = new ArrayList<>();
        final List<FakeChannel> channels = new ArrayList<>();
        FakeTransport(String... responses) { this.responses.addAll(List.of(responses)); }
        @Override public RemoteAdbCommands.Channel open(String service) {
            services.add(service);
            FakeChannel channel = new FakeChannel(responses.removeFirst());
            channels.add(channel);
            return channel;
        }
    }

    private static final class FakeChannel implements RemoteAdbCommands.Channel {
        final ByteArrayOutputStream written = new ByteArrayOutputStream();
        final ByteArrayInputStream input;
        boolean closed;
        FakeChannel(String output) { input = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)); }
        @Override public InputStream input() { return input; }
        @Override public OutputStream output() { return written; }
        @Override public void close() { closed = true; }
    }
}
