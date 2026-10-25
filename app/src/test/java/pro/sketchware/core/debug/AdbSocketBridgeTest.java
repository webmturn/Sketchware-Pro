package pro.sketchware.core.debug;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AdbSocketBridgeTest {
    @Test(timeout = 10000) public void forwardsBytesInBothDirectionsWithoutChangingThem() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (ServerSocket device = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             AdbSocketBridge bridge = new AdbSocketBridge()) {
            var remote = executor.submit(() -> {
                try (Socket socket = device.accept()) {
                    byte[] request = socket.getInputStream().readNBytes(4);
                    socket.getOutputStream().write(new byte[]{request[3], request[2], request[1], request[0]});
                    // Keep the connection alive until the client has consumed all bytes.
                    socket.getInputStream().read();
                    return request;
                }
            });
            bridge.connect(new RemoteEndpoint("127.0.0.1", device.getLocalPort()));
            try (Socket client = new Socket("127.0.0.1", bridge.getPort())) {
                client.setSoTimeout(3000);
                client.getOutputStream().write(new byte[]{0, 127, -1, -128});
                assertArrayEquals(new byte[]{-128, -1, 127, 0}, client.getInputStream().readNBytes(4));
            }
            assertArrayEquals(new byte[]{0, 127, -1, -128}, remote.get(3, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); }
    }

    @Test(timeout = 10000) public void cancellationUnblocksAStalledPeer() throws Exception {
        try (ServerSocket device = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             AdbSocketBridge bridge = new AdbSocketBridge()) {
            bridge.connect(new RemoteEndpoint("127.0.0.1", device.getLocalPort()));
            try (Socket remote = device.accept(); Socket client = new Socket("127.0.0.1", bridge.getPort())) {
                client.setSoTimeout(3000);
                remote.setSoTimeout(3000);
                client.getOutputStream().write(7);
                assertEquals(7, remote.getInputStream().read());
                bridge.close();
                assertEquals(-1, client.getInputStream().read());
                assertEquals(-1, remote.getInputStream().read());
            }
        }
    }

    @Test public void endpointKeepsIpv6PortUnambiguous() {
        assertEquals("[::1]:37001", new RemoteEndpoint("[::1]", 37001).toString());
        assertEquals(new RemoteEndpoint("::1", 37001), new RemoteEndpoint("[::1]", 37001));
        assertThrows(IllegalArgumentException.class, () -> new RemoteEndpoint("http://192.168.1.2", 1234));
        assertThrows(IllegalArgumentException.class, () -> new RemoteEndpoint("192.168.1.2", 0));
        assertThrows(IllegalArgumentException.class, () -> new RemoteEndpoint("192.168.1.2", 65536));
    }
}
