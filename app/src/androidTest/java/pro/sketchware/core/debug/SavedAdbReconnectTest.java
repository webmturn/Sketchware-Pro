package pro.sketchware.core.debug;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Optional hardware check: only connects to the previously selected device, never installs an APK. */
@RunWith(AndroidJUnit4.class)
public class SavedAdbReconnectTest {
    @Test(timeout = 65000) public void discoversAndReconnectsThePreviouslySelectedPhone() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteDebugSettings settings = new RemoteDebugSettings(context);
        RemoteDeviceTarget target;
        try { target = settings.getTarget(); }
        catch (IllegalArgumentException missing) { assumeNoException(missing); return; }
        AdbDiscovery discovery = new AdbDiscovery(context, () -> { });
        try (AdbDiscovery.Scan scan = discovery.acquire()) {
            DiscoveredAdbService found = discovery.await(service -> {
                DiscoveredAdbService selected = AdbServiceSelector.connectionFor(discovery.snapshot(), target);
                return selected != null && service.key().equals(selected.key());
            }, 15000, () -> false);
            Bundle evidence = new Bundle();
            evidence.putString("saved_adb_discovery", found == null ? "saved device not currently advertised"
                    : "found selected device at port " + found.endpoint.port);
            InstrumentationRegistry.getInstrumentation().sendStatus(0, evidence);
            assumeNotNull(found);
            RemoteDebugManager manager = RemoteDebugManager.get(context);
            try {
                assertTrue(manager.reconnect(null));
                long deadline = SystemClock.uptimeMillis() + 45000;
                while (manager.isBusy() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
                assertFalse("Connection must complete within its deadline", manager.isBusy());
                assertTrue(manager.getStatus(), manager.isConnected());
                assertEquals(found.endpoint.port, settings.getPort());
                assertFalse("Remember the device identity for later rediscovery", settings.getTarget().guid.isEmpty()
                        && settings.getTarget().serial.isEmpty());
                evidence.putString("saved_adb_connection", manager.getConnectedName());
                InstrumentationRegistry.getInstrumentation().sendStatus(0, evidence);
            } finally { manager.disconnect(); }
        }
    }
}
