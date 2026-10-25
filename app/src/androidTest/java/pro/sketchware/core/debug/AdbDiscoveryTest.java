package pro.sketchware.core.debug;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AdbDiscoveryTest {
    @Test(timeout = 45000) public void discoversPairingAndConnectionPortsAndRefreshesAChangedPort() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String prefix = "adb-codex-" + Long.toHexString(System.nanoTime());
        AdbDiscovery discovery = new AdbDiscovery(context, () -> { });
        try (AdvertisedService pairing = new AdvertisedService(context, prefix + "-pair", DiscoveredAdbService.PAIRING, 32101);
             AdbDiscovery.Scan scan = discovery.acquire()) {
            assertNotNull(discovery.await(s -> s.isPairing() && s.name.equals(pairing.name()), 15000, () -> false));
            try (AdvertisedService connection = new AdvertisedService(context, prefix + "-connect", DiscoveredAdbService.CONNECT, 32102)) {
                DiscoveredAdbService result = discovery.await(s -> !s.isPairing() && s.name.equals(connection.name()), 15000, () -> false);
                assertNotNull("The advertised connection service must be resolved", result);
                assertEquals(32102, result.endpoint.port);
                assertFalse(result.endpoint.host.isEmpty());
            }
            try (AdvertisedService moved = new AdvertisedService(context, prefix + "-connect", DiscoveredAdbService.CONNECT, 32103)) {
                assertNotNull("Rediscovery must replace a stale connection port", discovery.await(
                        s -> s.name.equals(moved.name()) && s.endpoint.port == 32103, 15000, () -> false));
            }
        }
    }

    @Test(timeout = 40000) public void releasingTheLastScanClearsResultsAndAllowsANewScan() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "adb-codex-" + Long.toHexString(System.nanoTime());
        AdbDiscovery discovery = new AdbDiscovery(context, () -> { });
        try (AdvertisedService service = new AdvertisedService(context, name, DiscoveredAdbService.PAIRING, 32111)) {
            try (AdbDiscovery.Scan scan = discovery.acquire()) {
                assertNotNull(discovery.await(s -> s.name.equals(service.name()), 15000, () -> false));
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue(discovery.snapshot().isEmpty());
            try (AdbDiscovery.Scan scan = discovery.acquire()) {
                assertNotNull(discovery.await(s -> s.name.equals(service.name()), 15000, () -> false));
            }
        }
    }

    /** Publish real mDNS records without opening pairing or installation sessions. */
    public static final class AdvertisedService implements AutoCloseable {
        private final NsdManager nsd;
        private final AtomicReference<NsdServiceInfo> registered = new AtomicReference<>();
        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch removed = new CountDownLatch(1);
        private final NsdManager.RegistrationListener listener = new NsdManager.RegistrationListener() {
            @Override public void onServiceRegistered(NsdServiceInfo info) { registered.set(info); ready.countDown(); }
            @Override public void onRegistrationFailed(NsdServiceInfo info, int code) { ready.countDown(); }
            @Override public void onServiceUnregistered(NsdServiceInfo info) { removed.countDown(); }
            @Override public void onUnregistrationFailed(NsdServiceInfo info, int code) { removed.countDown(); }
        };

        public AdvertisedService(Context context, String name, String type, int port) throws Exception {
            nsd = context.getSystemService(NsdManager.class);
            NsdServiceInfo info = new NsdServiceInfo();
            info.setServiceName(name);
            info.setServiceType(type);
            info.setPort(port);
            nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener);
            if (!ready.await(10, TimeUnit.SECONDS) || registered.get() == null) {
                try { nsd.unregisterService(listener); } catch (RuntimeException ignored) { }
                throw new AssertionError("Cannot publish test mDNS service");
            }
        }

        public String name() { return registered.get().getServiceName(); }

        @Override public void close() throws Exception {
            nsd.unregisterService(listener);
            removed.await(5, TimeUnit.SECONDS);
        }
    }
}
