package pro.sketchware.activities.debug;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.core.debug.AdbDiscovery;
import pro.sketchware.core.debug.DiscoveredAdbService;
import pro.sketchware.core.debug.RemoteDebugManager;
import pro.sketchware.core.debug.RemoteEndpoint;

/** UI contract with a fixed discovery result; real mDNS changes are covered by AdbDiscoveryTest. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class RemoteDebugDiscoveryUiTest {
    @Test(timeout = 60000) public void discoveringAPhoneNeedsOnlyItsPairingCode() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteDebugManager manager = RemoteDebugManager.get(app);
        CountDownLatch disconnected = new CountDownLatch(1);
        Runnable idle = () -> { if (!manager.isBusy()) disconnected.countDown(); };
        manager.addListener(idle);
        try {
            manager.disconnect();
            idle.run();
            assertTrue(disconnected.await(10, TimeUnit.SECONDS));
        } finally { manager.removeListener(idle); }

        DiscoveredAdbService service = new DiscoveredAdbService("adb-ui-fixture", DiscoveredAdbService.PAIRING,
                new RemoteEndpoint("192.0.2.10", 32201));
        Field discoveryField = RemoteDebugManager.class.getDeclaredField("discovery");
        discoveryField.setAccessible(true);
        AdbDiscovery discovery = (AdbDiscovery) discoveryField.get(manager);
        Field servicesField = AdbDiscovery.class.getDeclaredField("services");
        servicesField.setAccessible(true);
        Map<String, DiscoveredAdbService> services = (Map<String, DiscoveredAdbService>) servicesField.get(discovery);
        try (ActivityScenario<RemoteDebugActivity> scenario = ActivityScenario.launch(new Intent(app, RemoteDebugActivity.class)
                .putExtra("sc_id", "discovery-ui-test").putExtra("package_name", "com.example.discoverytest"))) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                synchronized (discovery) { services.put(service.key(), service); }
                refresh(activity);
                assertNotNull(findDevice(activity.findViewById(R.id.devices), service.name));
            });
            capture(scenario, app, "wireless-discovery.png", false);
            scenario.onActivity(activity -> assertTrue(findDevice(activity.findViewById(R.id.devices), service.name).performClick()));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture(scenario, app, "wireless-pair.png", true);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                View root = findDialogRoot();
                assertNotNull(root);
                assertEquals(View.GONE, root.findViewById(R.id.pair_port_layout).getVisibility());
                assertEquals(View.VISIBLE, root.findViewById(R.id.pair_code).getVisibility());
                root.findViewById(android.R.id.button2).performClick();
            });
            scenario.onActivity(activity -> {
                assertEquals(View.GONE, activity.findViewById(R.id.advanced_group).getVisibility());
                activity.findViewById(R.id.advanced_toggle).performClick();
                assertEquals(View.VISIBLE, activity.findViewById(R.id.advanced_group).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.host).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.port).getVisibility());
                EditText host = activity.findViewById(R.id.host), port = activity.findViewById(R.id.port);
                host.setText("192.0.2.25");
                port.setText("41234");
                View body = activity.findViewById(R.id.remote_body);
                body.setFocusableInTouchMode(true);
                body.requestFocus();
                refresh(activity);
                assertEquals("192.0.2.25", host.getText().toString());
                assertEquals("41234", port.getText().toString());
            });
            capture(scenario, app, "wireless-advanced.png", false);
        } finally {
            synchronized (discovery) { services.remove(service.key()); }
        }
    }

    private static void refresh(RemoteDebugActivity activity) {
        try {
            Field field = RemoteDebugActivity.class.getDeclaredField("observer");
            field.setAccessible(true);
            ((Runnable) field.get(activity)).run();
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void capture(ActivityScenario<RemoteDebugActivity> scenario, Context app, String name,
                                boolean dialog) throws Exception {
        AtomicReference<Bitmap> rendered = new AtomicReference<>();
        scenario.onActivity(activity -> {
            // Render this window only, so other apps and system transitions cannot enter the QA image.
            View root = dialog ? findDialogRoot() : activity.getWindow().getDecorView();
            assertNotNull(root);
            int width = root.getWidth(), height = root.getHeight();
            assertTrue(width > 0 && height > 0);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(com.google.android.material.color.MaterialColors.getColor(root,
                    com.google.android.material.R.attr.colorSurface));
            root.draw(canvas);
            rendered.set(bitmap);
        });
        Bitmap screen = rendered.get();
        try (FileOutputStream output = new FileOutputStream(new File(app.getExternalFilesDir("debug-ui"), name))) {
            assertTrue(screen.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { screen.recycle(); }
    }

    private static View findDevice(View view, String name) {
        if (name.equals(view.getTag())) return view;
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                View found = findDevice(group.getChildAt(index), name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static View findDialogRoot() {
        for (View root : android.view.inspector.WindowInspector.getGlobalWindowViews()) {
            if (root.findViewById(R.id.pair_code) != null) return root;
        }
        return null;
    }
}
