package pro.sketchware.core.debug;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;

public class AdbServiceSelectorTest {
    private static DiscoveredAdbService connect(String name, String host, int port) {
        return new DiscoveredAdbService(name, DiscoveredAdbService.CONNECT, new RemoteEndpoint(host, port));
    }

    private static RemoteDeviceTarget saved(String serial, String guid) {
        return new RemoteDeviceTarget("192.168.1.2", 30001, "adb-old-instance", serial, guid, "Test phone", true);
    }

    @Test public void findsNewAddressAndPortForTheSavedDevice() {
        var advertised = connect("adb-SERIAL1-new", "192.168.1.25", 42017);
        assertSame(advertised, AdbServiceSelector.connectionFor(List.of(advertised), saved("SERIAL1", "guid1")));
    }

    @Test public void deviceIdentityTakesPriorityOverAnAddressReusedByAnotherPhone() {
        var wrong = connect("adb-SERIAL2-random", "192.168.1.2", 31001);
        var right = connect("adb-SERIAL1-random", "192.168.1.9", 41001);
        assertSame(right, AdbServiceSelector.connectionFor(List.of(wrong, right), saved("SERIAL1", "guid1")));
    }

    @Test public void resolvesStableGuidWhenTheServiceInstanceChanges() {
        var advertised = connect("adb-guid1-new", "192.168.1.5", 32001);
        assertSame(advertised, AdbServiceSelector.connectionFor(List.of(advertised), saved("", "guid1")));
    }

    @Test public void doesNotConfuseSerialNumberPrefixes() {
        var wrong = connect("adb-SERIAL12-random", "192.168.1.9", 31001);
        assertNull(AdbServiceSelector.connectionFor(List.of(wrong), saved("SERIAL1", "guid1")));
    }

    @Test public void neverChoosesTheOnlyNearbyDeviceWithoutASelection() {
        var unselected = new RemoteDeviceTarget("", 0, "", "", "", "", true);
        assertFalse(unselected.isConfigured());
        assertNull(AdbServiceSelector.connectionFor(List.of(connect("adb-other", "192.168.1.9", 1234)), unselected));
    }

    @Test public void pairingPortsCannotBeUsedAsConnectionPorts() {
        var pairing = new DiscoveredAdbService("adb-pairing", DiscoveredAdbService.PAIRING,
                new RemoteEndpoint("192.168.1.2", 31000));
        assertNull(AdbServiceSelector.connectionFor(List.of(pairing), saved("", "")));
    }

    @Test public void ambiguousPortsOnTheSameHostRequireADeviceSelection() {
        assertNull(AdbServiceSelector.connectionAt(List.of(connect("one", "192.168.1.2", 31001),
                connect("two", "192.168.1.2", 31002)), "192.168.1.2"));
    }

    @Test public void migratedManualAddressCanFindItsCurrentPort() {
        var migrated = new RemoteDeviceTarget("192.168.1.2", 30001, "", "", "", "", true);
        var advertised = connect("adb-SERIAL1-random", "192.168.1.2", 41001);
        assertSame(advertised, AdbServiceSelector.connectionFor(List.of(advertised), migrated));
    }

    @Test public void rejectsAnotherPairedPhoneBeforeDeployment() {
        assertFalse(saved("SERIAL1", "guid1").acceptsIdentity("guid2", "SERIAL2"));
        assertFalse(saved("SERIAL1", "guid1").acceptsIdentity("guid2", "SERIAL1"));
    }

    @Test public void knownDevicesRequireAReadableIdentity() {
        assertFalse(saved("SERIAL1", "guid1").acceptsIdentity("", ""));
        assertTrue(saved("SERIAL1", "guid1").acceptsIdentity("", "SERIAL1"));
        assertTrue(saved("SERIAL1", "guid1").acceptsIdentity("guid1", ""));
    }

    @Test public void normalizesAndroidServiceTypeFormatting() {
        var service = new DiscoveredAdbService("adb-device", "._adb-tls-pairing._tcp",
                new RemoteEndpoint("192.168.1.2", 31000));
        assertTrue(service.isPairing());
        assertEquals("_adb-tls-pairing._tcp./adb-device", service.key());
    }

    @Test public void aNewManualSelectionDoesNotInheritAnotherDevicesIdentity() {
        var manual = RemoteDeviceTarget.manual(new RemoteEndpoint("192.168.1.3", 45000));
        assertTrue(manual.acceptsIdentity("new-guid", "new-serial"));
        assertFalse(saved("SERIAL1", "guid1").sameDevice(new RemoteDeviceTarget("192.168.1.2", 40001,
                "adb-other", "SERIAL2", "guid2", "Other phone", true)));
    }
}
