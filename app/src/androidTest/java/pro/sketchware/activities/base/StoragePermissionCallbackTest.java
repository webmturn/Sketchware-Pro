package pro.sketchware.activities.base;

import static org.junit.Assert.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.app.ActivityCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import pro.sketchware.R;
import pro.sketchware.util.ThrottleTimer;

@RunWith(AndroidJUnit4.class)
public class StoragePermissionCallbackTest {
    private static final String WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String READ = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String CAMERA = Manifest.permission.CAMERA;
    private static final int GRANTED = PackageManager.PERMISSION_GRANTED;
    private static final int DENIED = PackageManager.PERMISSION_DENIED;
    private static final int REQUEST = 16385;

    @Test(timeout = 30000)
    public void activityHandlesIncompleteAndReorderedResults() {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> verifyCases(activity, false));
        }
    }

    @Test(timeout = 30000)
    public void fragmentHandlesIncompleteAndReorderedResults() {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> verifyCases(activity, true));
        }
    }

    private static void verifyCases(PermissionResultTestActivity activity, boolean fragment) {
        verify(activity, fragment, new String[]{WRITE, READ}, new int[]{GRANTED, GRANTED}, true, 1, 0);
        verify(activity, fragment, new String[]{READ, WRITE}, new int[]{GRANTED, GRANTED}, true, 1, 0);
        verify(activity, fragment, new String[]{WRITE}, new int[]{GRANTED}, true, 1, 0);
        verify(activity, fragment, new String[]{READ}, new int[]{GRANTED}, true, 1, 0);
        verify(activity, fragment, new String[]{WRITE}, new int[]{GRANTED}, false, 0, 1);
        verify(activity, fragment, new String[]{READ}, new int[]{GRANTED}, false, 0, 1);
        verify(activity, fragment, new String[]{WRITE, READ}, new int[]{GRANTED}, true, 0, 1);
        verify(activity, fragment, new String[]{WRITE, READ}, new int[]{}, true, 0, 1);
        verify(activity, fragment, new String[]{WRITE}, new int[]{}, true, 0, 1);
        verify(activity, fragment, new String[]{WRITE, READ}, new int[]{GRANTED, DENIED}, true, 0, 1);
        verify(activity, fragment, new String[]{READ, WRITE}, new int[]{DENIED, GRANTED}, true, 0, 1);
        verify(activity, fragment, new String[]{WRITE}, new int[]{DENIED}, true, 0, 1);
        verify(activity, fragment, new String[]{READ}, new int[]{DENIED}, true, 0, 1);
        verify(activity, fragment, new String[]{WRITE, READ}, new int[]{GRANTED, GRANTED}, false, 0, 1);
        verify(activity, fragment, new String[]{CAMERA, WRITE, READ}, new int[]{DENIED, GRANTED, GRANTED}, true, 1, 0);
        verify(activity, fragment, new String[]{WRITE, CAMERA, READ}, new int[]{GRANTED, GRANTED, DENIED}, true, 0, 1);
        verify(activity, fragment, new String[]{CAMERA, READ, WRITE}, new int[]{GRANTED, GRANTED}, true, 0, 1);
        verify(activity, fragment, new String[]{}, new int[]{}, true, 0, 0);
        verify(activity, fragment, new String[]{CAMERA}, new int[]{GRANTED}, false, 0, 0);
        verify(activity, fragment, new String[]{CAMERA}, new int[]{DENIED}, true, 0, 0);
        verify(activity, fragment, new String[]{WRITE, WRITE}, new int[]{GRANTED, GRANTED}, true, 1, 0);
    }

    private static void verify(PermissionResultTestActivity activity, boolean fragment, String[] permissions,
                               int[] results, boolean currentAccess, int grants, int denials) {
        String context = Arrays.toString(permissions) + Arrays.toString(results) + " access=" + currentAccess;
        if (fragment) {
            var target = activity.fragment;
            target.resetCallbacks();
            target.storagePermissionOverride = currentAccess;
            target.onRequestPermissionsResult(REQUEST, permissions, results);
            assertNull(context, target.callbackFailure);
            assertEquals(context, grants, target.grantedRequests.size());
            assertEquals(context, denials, target.deniedRequests.size());
            if (grants > 0) assertEquals(REQUEST, target.grantedRequests.get(0).intValue());
            if (denials > 0) assertEquals(REQUEST, target.deniedRequests.get(0).intValue());
        } else {
            activity.resetCallbacks();
            activity.storagePermissionOverride = currentAccess;
            activity.onRequestPermissionsResult(REQUEST, permissions, results);
            assertNull(context, activity.callbackFailure);
            assertEquals(context, grants, activity.grantedRequests.size());
            assertEquals(context, denials, activity.deniedRequests.size());
            if (grants > 0) assertEquals(REQUEST, activity.grantedRequests.get(0).intValue());
            if (denials > 0) assertEquals(REQUEST, activity.deniedRequests.get(0).intValue());
        }
    }

    @Test(timeout = 30000)
    public void systemDeliversSingleStorageResultToActivity() throws Exception {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue("Test requires existing storage grants; never revoke user permissions", activity.isStoragePermissionGranted());
                ActivityCompat.requestPermissions(activity, new String[]{WRITE}, REQUEST);
            });
            waitUntil(() -> {
                AtomicBoolean received = new AtomicBoolean();
                scenario.onActivity(activity -> received.set(activity.callbackResults != null));
                return received.get();
            });
            scenario.onActivity(activity -> {
                assertArrayEquals(new String[]{WRITE}, activity.callbackPermissions);
                assertArrayEquals(new int[]{GRANTED}, activity.callbackResults);
                assertNull(activity.callbackFailure);
                assertEquals(Arrays.asList(REQUEST), activity.grantedRequests);
                assertTrue(activity.deniedRequests.isEmpty());
            });
        }
    }

    @Test(timeout = 30000)
    public void fragmentPermissionRequestDeliversSingleGrantedResult() throws Exception {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.fragment.hasStoragePermission());
                activity.fragment.requestPermissions(new String[]{WRITE}, REQUEST);
            });
            waitUntil(() -> {
                AtomicBoolean received = new AtomicBoolean();
                scenario.onActivity(activity -> received.set(activity.fragment.callbackResults != null));
                return received.get();
            });
            scenario.onActivity(activity -> {
                assertArrayEquals(new String[]{WRITE}, activity.fragment.callbackPermissions);
                assertArrayEquals(new int[]{GRANTED}, activity.fragment.callbackResults);
                assertNull(activity.fragment.callbackFailure);
                assertNull(activity.callbackFailure);
                assertEquals(Arrays.asList(REQUEST), activity.fragment.grantedRequests);
                assertTrue(activity.fragment.deniedRequests.isEmpty());
            });
        }
    }

    @Test(timeout = 30000)
    public void systemDeliversBothStorageResults() throws Exception {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.isStoragePermissionGranted());
                ActivityCompat.requestPermissions(activity, new String[]{WRITE, READ}, REQUEST);
            });
            waitUntil(() -> {
                AtomicBoolean received = new AtomicBoolean();
                scenario.onActivity(activity -> received.set(activity.callbackResults != null));
                return received.get();
            });
            scenario.onActivity(activity -> {
                assertArrayEquals(new String[]{WRITE, READ}, activity.callbackPermissions);
                assertArrayEquals(new int[]{GRANTED, GRANTED}, activity.callbackResults);
                assertNull(activity.callbackFailure);
                assertEquals(Arrays.asList(REQUEST), activity.grantedRequests);
            });
        }
    }

    @Test(timeout = 30000)
    public void activityDeniedDialogCanBeCanceled() throws Exception { verifyDialog(false, false); }

    @Test(timeout = 30000)
    public void fragmentDeniedDialogCanBeCanceled() throws Exception { verifyDialog(true, false); }

    @Test(timeout = 30000)
    public void activityDeniedDialogKeepsSettingsAction() throws Exception { verifyDialog(false, true); }

    @Test(timeout = 30000)
    public void fragmentDeniedDialogKeepsSettingsAction() throws Exception { verifyDialog(true, true); }

    private static void verifyDialog(boolean fragment, boolean settings) throws Exception {
        boolean throttle = ThrottleTimer.isThrottled;
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                ThrottleTimer.isThrottled = false;
                if (fragment) {
                    activity.fragment.showRealDialogs = true;
                    activity.fragment.onRequestPermissionsResult(REQUEST, new String[]{WRITE, READ}, new int[]{DENIED, DENIED});
                } else {
                    activity.showRealDialogs = true;
                    activity.onRequestPermissionsResult(REQUEST, new String[]{WRITE, READ}, new int[]{DENIED, DENIED});
                    assertNull(activity.callbackFailure);
                }
            });
            Thread.sleep(150);
            String button = InstrumentationRegistry.getInstrumentation().getTargetContext()
                    .getString(settings ? R.string.common_word_settings : R.string.common_word_cancel);
            waitUntil(() -> clickExactText(button));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                assertEquals(1, fragment ? (settings ? activity.fragment.settingsCount : activity.fragment.deniedCancelCount)
                        : (settings ? activity.settingsCount : activity.deniedCancelCount));
                assertFalse(ThrottleTimer.isThrottled);
            });
        } finally {
            ThrottleTimer.isThrottled = throttle;
        }
    }

    private static boolean clickExactText(String text) {
        AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        if (root == null) return false;
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
            if (text.contentEquals(node.getText()) && node.isClickable()
                    && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }
        return false;
    }

    interface Condition { boolean satisfied() throws Exception; }
    private static void waitUntil(Condition condition) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.satisfied()) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Permission callback did not complete");
    }
}
