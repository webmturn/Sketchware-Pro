package pro.sketchware.activities.debug;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.appbar.MaterialToolbar;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import pro.sketchware.R;
import pro.sketchware.activities.editor.LogReaderActivity;
import pro.sketchware.core.debug.RemoteDebugManager;
import pro.sketchware.core.debug.RemoteLogBuffer;

@RunWith(AndroidJUnit4.class)
public class RemoteLogcatUiTest {
    private static final String PACKAGE = "com.example.remote.logtest";

    @Test public void remoteLogsReuseSearchAndClearWithoutDuplicatesAfterReopening() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        RemoteDebugManager manager = RemoteDebugManager.get(app);
        Field field = RemoteDebugManager.class.getDeclaredField("logs");
        field.setAccessible(true);
        RemoteLogBuffer buffer = (RemoteLogBuffer) field.get(manager);
        buffer.clear();
        buffer.append(PACKAGE, "10-25 10:00:01.001  1000  1001 D Example: alpha");
        buffer.append(PACKAGE, "10-25 10:00:02.002  1000  1001 F Example: needle crash");
        buffer.append("com.example.other", "This belongs to a different project");
        try (ActivityScenario<LogReaderActivity> scenario = ActivityScenario.launch(new Intent(app, LogReaderActivity.class)
                .putExtra(LogReaderActivity.EXTRA_REMOTE, true).putExtra("package_name", PACKAGE))) {
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.logsRecyclerView);
                assertEquals(2, list.getAdapter().getItemCount());
                assertEquals(View.GONE, activity.findViewById(R.id.noContentLayout).getVisibility());
                MaterialToolbar toolbar = activity.findViewById(R.id.topAppBar);
                assertTrue(toolbar.getMenu().findItem(R.id.action_remote_capture).isVisible());
                // Bind through the existing Logcat adapter, including the fatal level from remote logcat.
                RecyclerView.Adapter adapter = list.getAdapter();
                RecyclerView.ViewHolder row = adapter.createViewHolder(list, 0);
                adapter.bindViewHolder(row, 1);
                assertEquals("F", ((TextView) row.itemView.findViewById(R.id.type)).getText().toString());
                assertEquals("needle crash", ((TextView) row.itemView.findViewById(R.id.log)).getText().toString());
                EditText search = activity.findViewById(R.id.searchInput);
                search.setText("needle");
                assertEquals(1, list.getAdapter().getItemCount());
                search.setText("no matching lines");
                assertEquals(0, list.getAdapter().getItemCount());
                assertEquals("Keep search accessible when no lines match", View.GONE,
                        activity.findViewById(R.id.noContentLayout).getVisibility());
                search.setText("");
                assertEquals(2, list.getAdapter().getItemCount());
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            buffer.append(PACKAGE, "10-25 10:00:03.003  1000  1001 I Example: next");
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.logsRecyclerView);
                assertEquals(3, list.getAdapter().getItemCount());
                MaterialToolbar toolbar = activity.findViewById(R.id.topAppBar);
                assertTrue(toolbar.getMenu().performIdentifierAction(R.id.action_clear, 0));
                assertEquals(0, list.getAdapter().getItemCount());
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(0,
                    ((RecyclerView) activity.findViewById(R.id.logsRecyclerView)).getAdapter().getItemCount()));
        } finally { manager.clearLogs(); }
    }

    @Test public void localBroadcastLogsStillWorkAfterClearingSearch() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ActivityScenario<LogReaderActivity> scenario = ActivityScenario.launch(new Intent(app, LogReaderActivity.class))) {
            broadcast(app, "first");
            scenario.onActivity(activity -> {
                assertFalse(((MaterialToolbar) activity.findViewById(R.id.topAppBar)).getMenu()
                        .findItem(R.id.action_remote_capture).isVisible());
                ((EditText) activity.findViewById(R.id.searchInput)).setText("first");
                ((EditText) activity.findViewById(R.id.searchInput)).setText("");
            });
            broadcast(app, "second");
            scenario.onActivity(activity -> assertEquals(2,
                    ((RecyclerView) activity.findViewById(R.id.logsRecyclerView)).getAdapter().getItemCount()));
        }
    }

    private static void broadcast(Context app, String message) {
        app.sendBroadcast(new Intent("pro.sketchware.ACTION_NEW_DEBUG_LOG").setPackage(app.getPackageName())
                .putExtra("packageName", PACKAGE).putExtra("log", message));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
