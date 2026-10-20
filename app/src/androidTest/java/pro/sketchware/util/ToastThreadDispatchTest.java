package pro.sketchware.util;

import static org.junit.Assert.*;

import android.app.UiAutomation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Looper;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.activities.base.PermissionResultTestActivity;

/** Observe actual toast accessibility events, since the old utility swallowed worker exceptions. */
@RunWith(AndroidJUnit4.class)
public class ToastThreadDispatchTest {
    private ActivityScenario<PermissionResultTestActivity> scenario;

    @Before public void openForegroundHost() {
        scenario = ActivityScenario.launch(PermissionResultTestActivity.class);
        scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    }

    @After public void closeHost() {
        if (scenario != null) scenario.close();
    }

    @Test(timeout = 20000)
    public void regularToastFromAWorkerIsActuallyDisplayed() throws Exception {
        String message = message();
        awaitToast(message, () -> onWorker(() -> SketchwareUtil.toast(message, Toast.LENGTH_SHORT)));
    }

    @Test(timeout = 20000)
    public void warningToastFromAWorkerIsActuallyDisplayed() throws Exception {
        String message = message();
        awaitToast(message, () -> onWorker(() -> SketchwareUtil.toastError(message, Toast.LENGTH_SHORT)));
    }

    @Test(timeout = 20000)
    public void plainMessageFromAWorkerUsesTheMainThreadAndIsDisplayed() throws Exception {
        String message = message();
        MainThreadContext context = new MainThreadContext(targetContext());
        awaitToast(message, () -> onWorker(() -> SketchwareUtil.showMessage(context, message)));
        assertTrue("Toast resources must be read on the main thread", context.resourcesRead);
    }

    @Test(timeout = 30000)
    public void mainThreadEntrypointsStillDisplayAndPlainMessagesRemainImmediate() throws Exception {
        for (int type = 0; type < 3; type++) {
            String message = message();
            int entry = type;
            MainThreadContext context = new MainThreadContext(targetContext());
            awaitToast(message, () -> scenario.onActivity(activity -> {
                switch (entry) {
                    case 0 -> SketchwareUtil.toast(message);
                    case 1 -> SketchwareUtil.toastError(message);
                    case 2 -> {
                        SketchwareUtil.showMessage(context, message);
                        assertTrue("Main-thread calls must not be unnecessarily deferred", context.resourcesRead);
                    }
                    default -> throw new AssertionError(entry);
                }
            }));
        }
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static String message() {
        return "Toast thread test " + UUID.randomUUID();
    }

    private static void awaitToast(String message, Runnable action) throws Exception {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        AccessibilityEvent event = automation.executeAndWaitForEvent(action,
                candidate -> candidate.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                        && candidate.getText().toString().contains(message), 8000);
        try {
            assertEquals("pro.sketchware", event.getPackageName().toString());
        } finally {
            event.recycle();
        }
    }

    private static void onWorker(Runnable action) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                assertNull("Exercise a thread without a prepared Looper", Looper.myLooper());
                action.run();
            } catch (Throwable failure) { error.set(failure); }
        }, "Toast-test-worker");
        worker.start();
        try { worker.join(5000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        assertFalse("Posting a toast must not block the worker", worker.isAlive());
        if (error.get() != null) throw new AssertionError("Worker toast failed", error.get());
    }

    private static final class MainThreadContext extends ContextWrapper {
        volatile boolean resourcesRead;

        MainThreadContext(Context base) { super(base); }

        @Override public Resources getResources() {
            assertSame("Toast creation must run on the main thread", Looper.getMainLooper(), Looper.myLooper());
            resourcesRead = true;
            return super.getResources();
        }
    }
}
