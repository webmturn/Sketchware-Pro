package pro.sketchware.widgets;

import static org.junit.Assert.*;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;

@RunWith(AndroidJUnit4.class)
public class MessageBottomSheetDialogTest {
    @Test(timeout = 15000) public void shortViewportKeepsActionsVisibleWhileLongContentScrolls() {
        checkSheet(true);
    }

    @Test(timeout = 15000) public void ordinaryMessageStartsExpandedWithAnAccessibleAction() {
        checkSheet(false);
    }

    private static void checkSheet(boolean constrained) {
        AtomicReference<MessageBottomSheetDialog> shown = new AtomicReference<>();
        AtomicBoolean clicked = new AtomicBoolean();
        var scenario = ActivityScenario.launch(PermissionResultTestActivity.class);
        try {
            scenario.onActivity(activity -> {
                MessageBottomSheetDialog dialog = new MessageBottomSheetDialog(activity);
                shown.set(dialog);
                dialog.setTitle("Startup update");
                dialog.setDescription(constrained ? "A long update description\n".repeat(80) : "This release includes changes.");
                dialog.setPositiveButton("View changes", (d, which) -> clicked.set(true));
                dialog.setCancelable(false);
                if (constrained) dialog.getPositiveButton().setTextSize(28);
                dialog.show();
                if (constrained) {
                    int height = Math.round(280 * activity.getResources().getDisplayMetrics().density);
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
                }
            });
            SystemClock.sleep(500);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                MessageBottomSheetDialog dialog = shown.get();
                assertEquals(BottomSheetBehavior.STATE_EXPANDED, dialog.getBehavior().getState());
                assertFalse(dialog.getBehavior().isDraggable());
                View button = dialog.getPositiveButton();
                assertFullyVisible(button);
                Rect original = bounds(button);
                if (constrained) {
                    NestedScrollView content = dialog.findViewById(R.id.messageContent);
                    assertTrue("Long content must be scrollable", content.canScrollVertically(1));
                    content.scrollTo(0, content.getChildAt(0).getHeight());
                    assertEquals("Scrolling content must not move the action", original, bounds(button));
                    assertFullyVisible(button);
                }
                assertTrue(button.performClick());
            });
            assertTrue(clicked.get());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                if (shown.get() != null && shown.get().isShowing()) shown.get().dismiss();
            });
            scenario.close();
        }
    }

    private static void assertFullyVisible(View view) {
        Rect visible = new Rect();
        assertTrue("Action must have a visible hit area", view.getGlobalVisibleRect(visible));
        assertTrue("Action must not be clipped: " + visible + " / " + bounds(view), visible.contains(bounds(view)));
        assertTrue(view.getWidth() > 0 && view.getHeight() > 0);
    }

    private static Rect bounds(View view) {
        int[] location = new int[2];
        // getGlobalVisibleRect is relative to the window, which can be centered in this test.
        view.getLocationInWindow(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }
}
