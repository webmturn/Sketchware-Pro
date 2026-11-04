package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.design.fragments.ComponentListFragment;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.component.AddComponentBottomSheet;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectLibraryBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.dialogs.InnerAddComponentBottomSheet;

@RunWith(AndroidJUnit4.class)
public class ComponentCreationLifecycleTest {
    private static final String NAME = "lifecycle_intent";

    @Test(timeout = 30000)
    public void designerAddRefreshesTheComponentList() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            host.openInput();
            host.save(NAME);
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void landscapeSelectorOpensExpandedAndCanAddAfterRecreation() throws Exception {
        try (Host host = new Host(false)) {
            host.rotate(Configuration.ORIENTATION_LANDSCAPE);
            host.openSelector();
            assertSelectorExpanded(host);
            assertSelectorContentVisible(host);
            host.recreate();
            assertSelectorExpanded(host);
            assertSelectorContentVisible(host);
            host.rotate(Configuration.ORIENTATION_PORTRAIT);
            assertSelectorCollapsed(host);
            assertSelectorContentVisible(host);
            host.rotate(Configuration.ORIENTATION_LANDSCAPE);
            assertSelectorExpanded(host);
            assertSelectorContentVisible(host);
            host.openInput();
            host.save(NAME);
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void logicSelectorFollowsRotationAndKeepsListGestures() throws Exception {
        try (Host host = new Host(true)) {
            host.openSelector();
            assertSelectorCollapsed(host);
            host.rotate(Configuration.ORIENTATION_LANDSCAPE);
            assertSelectorExpanded(host);
            assertSelectorContentVisible(host);
            Rect[] listBounds = {null};
            host.onActivity(activity -> {
                listBounds[0] = screenBounds(find(activity, AddComponentBottomSheet.class)
                        .requireView().findViewById(R.id.component_list));
                Rect available = new Rect();
                activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(available);
                assertTrue(listBounds[0].intersect(available));
            });
            Rect area = listBounds[0];
            float upper = area.top + area.height() * 0.25f;
            float lower = area.top + area.height() * 0.75f;
            swipe(area.centerX(), upper, lower);
            assertSelectorExpanded(host);
            swipe(area.centerX(), lower, upper);
            assertSelectorExpanded(host);
            host.rotate(Configuration.ORIENTATION_PORTRAIT);
            assertSelectorCollapsed(host);
            assertSelectorContentVisible(host);
        }
    }

    @Test(timeout = 30000)
    public void additionalFirebaseComponentsRequireTheLibraryAndCanBeRetried() throws Exception {
        for (int type : new int[]{ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_PHONE,
                ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE, ComponentBean.COMPONENT_TYPE_FIREBASE_AUTH_GOOGLE_LOGIN}) {
            try (Host host = new Host(false)) {
                var firebase = ProjectDataManager.getLibraryManager(host.fixture.id).getFirebaseDB();
                firebase.useYn = ProjectLibraryBean.LIB_USE_N;
                host.onActivity(activity -> InnerAddComponentBottomSheet.newInstance(host.fixture.id,
                        new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"), new ComponentBean(type))
                        .showNow(activity.getSupportFragmentManager(), "firebase-requirement"));
                idle();
                host.save(NAME);
                long rejectedSaveAt = SystemClock.elapsedRealtime();
                assertTrue("A disabled library must prevent creation of type " + type,
                        host.store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).isEmpty());
                host.onActivity(activity -> assertNotNull(input(activity)));

                firebase.useYn = ProjectLibraryBean.LIB_USE_Y;
                ProjectEditorPersistenceTest.waitUntil(() -> SystemClock.elapsedRealtime() - rejectedSaveAt > 100);
                host.save(NAME);
                host.assertCreatedAndClosed();
                assertEquals(type, host.store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0).type);
            }
        }
    }

    @Test(timeout = 30000)
    public void listSwipesStayInTheSelectorAndHandleStillDismisses() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(500, 5000);
            assertSelectorCollapsed(host);
            Rect[] dragHandle = {null};
            int[] expandTo = {0};
            host.onActivity(activity -> {
                dragHandle[0] = screenBounds(((android.view.ViewGroup)
                        find(activity, AddComponentBottomSheet.class).requireView()).getChildAt(0));
                Rect available = new Rect();
                activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(available);
                expandTo[0] = available.top + dragHandle[0].height();
            });
            swipe(dragHandle[0].centerX(), dragHandle[0].centerY(), expandTo[0]);
            assertSelectorExpanded(host);
            Rect[] listBounds = {null};
            boolean[] scrollable = {false};
            host.onActivity(activity -> {
                RecyclerView list = find(activity, AddComponentBottomSheet.class)
                        .requireView().findViewById(R.id.component_list);
                listBounds[0] = screenBounds(list);
                scrollable[0] = list.canScrollVertically(1);
                Rect available = new Rect();
                activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(available);
                assertTrue(listBounds[0].intersect(available));
            });
            Rect area = listBounds[0];
            float upper = area.top + area.height() * 0.25f;
            float lower = area.top + area.height() * 0.75f;

            // Starting at the top must not turn a list swipe into a sheet dismissal.
            swipe(area.centerX(), upper, lower);
            assertSelectorExpanded(host);
            for (int i = 0; i < 3; i++) {
                swipe(area.centerX(), lower, upper);
                assertSelectorExpanded(host);
                int[] offsetBeforeDown = {0};
                host.onActivity(activity -> {
                    RecyclerView list = find(activity, AddComponentBottomSheet.class).requireView()
                            .findViewById(R.id.component_list);
                    var first = list.findViewHolderForAdapterPosition(0);
                    if (scrollable[0]) {
                        assertTrue("Swipe up must move the first row above the viewport",
                                first == null || first.itemView.getTop() < list.getPaddingTop());
                    }
                    offsetBeforeDown[0] = list.computeVerticalScrollOffset();
                });
                swipe(area.centerX(), upper, lower);
                assertSelectorExpanded(host);
                host.onActivity(activity -> {
                    RecyclerView list = find(activity, AddComponentBottomSheet.class).requireView()
                            .findViewById(R.id.component_list);
                    var first = list.findViewHolderForAdapterPosition(0);
                    assertNotNull("Swipe down must return to the first row", first);
                    if (scrollable[0]) {
                        assertTrue("Swipe down must move the list towards the top",
                                list.computeVerticalScrollOffset() < offsetBeforeDown[0]);
                    } else {
                        assertEquals("A list that fits must remain at the top", 0, list.computeVerticalScrollOffset());
                    }
                });
            }

            Rect[] handle = {null};
            host.onActivity(activity -> {
                AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
                assertTrue("Ending a list swipe must restore handle dragging",
                        ((BottomSheetDialog) sheet.requireDialog()).getBehavior().isDraggable());
                handle[0] = screenBounds(((android.view.ViewGroup) sheet.requireView()).getChildAt(0));
            });
            swipe(handle[0].centerX(), handle[0].centerY(), area.bottom - 40);
            ProjectEditorPersistenceTest.waitUntil(() -> {
                boolean[] dismissed = {false};
                host.onActivity(activity -> dismissed[0] = find(activity, AddComponentBottomSheet.class) == null);
                return dismissed[0];
            });
        }
    }

    private static void awaitSelectorSettled(Host host) throws Exception {
        ProjectEditorPersistenceTest.waitUntil(() -> {
            boolean[] settled = {false};
            host.onActivity(activity -> {
                AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
                if (sheet == null) {
                    settled[0] = true;
                    return;
                }
                int state = ((BottomSheetDialog) sheet.requireDialog()).getBehavior().getState();
                RecyclerView list = sheet.requireView().findViewById(R.id.component_list);
                settled[0] = state != BottomSheetBehavior.STATE_SETTLING && state != BottomSheetBehavior.STATE_DRAGGING
                        && list.getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            });
            return settled[0];
        });
    }

    private static void assertSelectorExpanded(Host host) throws Exception {
        awaitSelectorSettled(host);
        host.onActivity(activity -> {
            AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
            assertNotNull("Scrolling the list must keep the selector open", sheet);
            BottomSheetDialog dialog = (BottomSheetDialog) sheet.requireDialog();
            assertEquals(BottomSheetBehavior.STATE_EXPANDED, dialog.getBehavior().getState());
        });
    }

    private static void assertSelectorCollapsed(Host host) throws Exception {
        awaitSelectorSettled(host);
        host.onActivity(activity -> {
            AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
            assertNotNull(sheet);
            BottomSheetDialog dialog = (BottomSheetDialog) sheet.requireDialog();
            assertEquals(BottomSheetBehavior.STATE_COLLAPSED, dialog.getBehavior().getState());
            Rect available = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(available);
            Rect bounds = screenBounds(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet));
            assertTrue("The selector must leave space above it", bounds.top > available.top);
        });
    }

    private static void assertSelectorContentVisible(Host host) {
        host.onActivity(activity -> {
            AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
            View title = sheet.requireView().findViewById(R.id.title);
            Rect visible = new Rect();
            assertTrue("The selector title must be visible", title.getGlobalVisibleRect(visible));
            assertEquals("The selector title must not be clipped", title.getHeight(), visible.height());
            RecyclerView list = sheet.requireView().findViewById(R.id.component_list);
            assertTrue("The component list must be visible", list.getGlobalVisibleRect(visible));
            assertTrue(list.getChildCount() > 0);
            assertTrue("The list must show at least one full row", visible.height() >= list.getChildAt(0).getHeight());
        });
    }

    private static Rect screenBounds(View view) {
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        return new Rect(position[0], position[1], position[0] + view.getWidth(), position[1] + view.getHeight());
    }

    private static void swipe(float x, float fromY, float toY) throws IOException {
        // Avoid the extra window-transaction waits around sendPointerSync on some devices.
        String command = "input touchscreen swipe " + Math.round(x) + " " + Math.round(fromY)
                + " " + Math.round(x) + " " + Math.round(toY) + " 400";
        try (var output = new ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command))) {
            assertEquals("The input command must finish without errors", -1, output.read());
        }
        SystemClock.sleep(400);
        idle();
    }

    @Test(timeout = 30000)
    public void cancelKeepsTheSelectorAndDoesNotChangeTheProject() throws Exception {
        try (Host host = new Host(false)) {
            var before = host.fixture.snapshot();
            host.openSelector();
            host.openInput();
            host.onActivity(activity -> input(activity).requireView().findViewById(R.id.btn_cancel).performClick());
            idle();
            host.onActivity(activity -> {
                assertNull(find(activity, InnerAddComponentBottomSheet.class));
                assertNotNull(find(activity, AddComponentBottomSheet.class));
                assertEquals(0, componentList(activity).getAdapter().getItemCount());
            });
            assertEquals(0, host.store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).size());
            host.fixture.assertUnchanged(before);
        }
    }

    @Test(timeout = 30000)
    public void selectorViewCanBeDestroyedAndRecreatedBeforeAdding() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            assertSelectorCollapsed(host);
            host.onActivity(activity -> {
                AddComponentBottomSheet selector = find(activity, AddComponentBottomSheet.class);
                FragmentManager manager = activity.getSupportFragmentManager();
                manager.beginTransaction().detach(selector).commitNow();
                manager.beginTransaction().attach(selector).commitNow();
            });
            idle();
            assertSelectorCollapsed(host);
            host.openInput();
            host.save(NAME);
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void designerInputCanSaveAfterActivityRecreation() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            host.openInput();
            host.onActivity(activity -> ((EditText) input(activity).requireView()
                    .findViewById(R.id.ed_input)).setText(NAME));
            host.recreate();
            host.onActivity(activity -> assertEquals(NAME, ((EditText) input(activity).requireView()
                    .findViewById(R.id.ed_input)).getText().toString()));
            host.save(NAME);
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void restoredInputWithoutTransientCallbacksCanSaveAndRefresh() throws Exception {
        try (Host host = new Host(false)) {
            host.onActivity(activity -> {
                InnerAddComponentBottomSheet sheet = new InnerAddComponentBottomSheet();
                Bundle args = arguments(host.fixture.id);
                args.putParcelable("component_bean", new ComponentBean(ComponentBean.COMPONENT_TYPE_INTENT));
                sheet.setArguments(args);
                sheet.showNow(activity.getSupportFragmentManager(), "restored-input");
            });
            host.save(NAME);
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void queuedSaveFromADestroyedViewDoesNotCreateAComponent() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            host.openInput();
            host.onActivity(activity -> {
                InnerAddComponentBottomSheet sheet = input(activity);
                ((EditText) sheet.requireView().findViewById(R.id.ed_input)).setText(NAME);
                View save = sheet.requireView().findViewById(R.id.btn_save);
                sheet.dismissNow();
                save.performClick();
            });
            idle();
            assertEquals(0, host.store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).size());
        }
    }

    @Test(timeout = 30000)
    public void repeatedSaveDeliveryCreatesOnlyOneComponent() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            host.openInput();
            host.onActivity(activity -> {
                View root = input(activity).requireView();
                ((EditText) root.findViewById(R.id.ed_input)).setText(NAME);
                View save = root.findViewById(R.id.btn_save);
                save.performClick();
                // Keep dismissal queued while delivering a second click beyond the UI throttle.
                android.os.SystemClock.sleep(150);
                save.performClick();
            });
            idle();
            host.assertCreatedAndClosed();
        }
    }

    @Test(timeout = 30000)
    public void recreatedLogicEditorRefreshesItsComponentPalette() throws Exception {
        try (Host host = new Host(true)) {
            host.openSelector();
            host.openInput();
            host.recreate();
            host.save(NAME);
            host.assertCreatedAndClosed();
            host.onActivity(activity -> {
                try {
                    Field field = LogicEditorActivity.class.getDeclaredField("lastSelectedPaletteId");
                    field.setAccessible(true);
                    assertEquals("The current logic editor must receive the creation result", 7, field.getInt(activity));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static Bundle arguments(String projectId) {
        Bundle args = new Bundle();
        args.putString("sc_id", projectId);
        args.putParcelable("project_file_bean",
                new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
        return args;
    }

    private static InnerAddComponentBottomSheet input(FragmentActivity activity) {
        InnerAddComponentBottomSheet result = find(activity, InnerAddComponentBottomSheet.class);
        assertNotNull("The component input sheet must be present", result);
        return result;
    }

    private static <T extends Fragment> T find(FragmentActivity activity, Class<T> type) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
            if (type.isInstance(fragment) && fragment.isAdded()) return type.cast(fragment);
        }
        return null;
    }

    private static RecyclerView componentList(FragmentActivity activity) {
        ComponentListFragment page = find(activity, ComponentListFragment.class);
        assertNotNull(page);
        return page.requireView().findViewById(R.id.component_list);
    }

    private static void idle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static final class Host implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        ActivityScenario<DesignActivity> designer;
        ActivityScenario<LogicEditorActivity> logic;

        Host(boolean logicEditor) throws Exception {
            try {
                fixture.seed(false, false);
                if (logicEditor) {
                    ProjectDataManager.reloadProjectFromData(fixture.id);
                    Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                            LogicEditorActivity.class).putExtra("sc_id", fixture.id)
                            .putExtra("id", "onCreate").putExtra("event", "initializeLogic")
                            .putExtra("event_text", "Component lifecycle test")
                            .putExtra("project_file", new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
                    logic = ActivityScenario.launch(intent);
                    keepScreenOn();
                    android.os.SystemClock.sleep(350);
                } else {
                    designer = fixture.launch();
                    keepScreenOn();
                    ProjectEditorPersistenceTest.waitForEditor(designer);
                    designer.onActivity(activity -> ((ViewPager) activity.findViewById(R.id.viewpager)).setCurrentItem(2, false));
                }
                idle();
                rotate(Configuration.ORIENTATION_PORTRAIT);
            } catch (Exception | AssertionError e) {
                close();
                throw e;
            }
        }

        void onActivity(Consumer<FragmentActivity> action) {
            if (designer != null) designer.onActivity(action::accept); else logic.onActivity(action::accept);
        }

        void rotate(int orientation) throws Exception {
            onActivity(activity -> activity.setRequestedOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE
                    ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            ProjectEditorPersistenceTest.waitUntil(() -> {
                boolean[] ready = {false};
                onActivity(activity -> {
                    View decor = activity.getWindow().getDecorView();
                    ready[0] = activity.getResources().getConfiguration().orientation == orientation
                            && (orientation == Configuration.ORIENTATION_LANDSCAPE
                            ? decor.getWidth() > decor.getHeight() : decor.getHeight() > decor.getWidth());
                });
                return ready[0];
            });
            idle();
        }

        void keepScreenOn() {
            onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }

        ProjectDataStore store() {
            return ProjectDataManager.getProjectDataManager(fixture.id);
        }

        void openSelector() throws Exception {
            Thread.sleep(150);
            onActivity(activity -> {
                if (designer != null) {
                    find(activity, ComponentListFragment.class).requireView().findViewById(R.id.fab).performClick();
                } else {
                    AddComponentBottomSheet selector = new AddComponentBottomSheet();
                    selector.setArguments(arguments(fixture.id));
                    selector.showNow(activity.getSupportFragmentManager(), "logic-selector");
                }
            });
            idle();
            ProjectEditorPersistenceTest.waitUntil(() -> {
                boolean[] ready = {false};
                onActivity(activity -> {
                    AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
                    if (sheet != null && sheet.getView() != null) {
                        RecyclerView list = sheet.requireView().findViewById(R.id.component_list);
                        ready[0] = list.findViewHolderForAdapterPosition(0) != null;
                    }
                });
                return ready[0];
            });
        }

        void openInput() {
            onActivity(activity -> {
                RecyclerView list = find(activity, AddComponentBottomSheet.class).requireView().findViewById(R.id.component_list);
                list.findViewHolderForAdapterPosition(0).itemView.performClick();
            });
            idle();
        }

        void save(String name) {
            onActivity(activity -> {
                View root = input(activity).requireView();
                ((EditText) root.findViewById(R.id.ed_input)).setText(name);
                root.findViewById(R.id.btn_save).performClick();
            });
            idle();
        }

        void recreate() throws Exception {
            if (designer != null) {
                designer.recreate();
                keepScreenOn();
                // The restored component tab displays the Java filename, unlike the initial view tab.
                ProjectEditorPersistenceTest.waitUntil(() -> {
                    boolean[] ready = {false};
                    designer.onActivity(activity -> ready[0] = activity.getProjectFilePaths() != null
                            && ProjectFileBean.DEFAULT_JAVA_NAME.contentEquals(
                            ((TextView) activity.findViewById(R.id.file_name)).getText())
                            && ((ViewPager) activity.findViewById(R.id.viewpager)).getCurrentItem() == 2);
                    return ready[0];
                });
            } else {
                logic.recreate();
                keepScreenOn();
                android.os.SystemClock.sleep(350);
            }
            idle();
        }

        void assertCreatedAndClosed() throws Exception {
            assertEquals(1, store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).size());
            assertEquals(NAME, store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).get(0).componentId);
            onActivity(activity -> {
                assertNull(find(activity, InnerAddComponentBottomSheet.class));
                assertNull(find(activity, AddComponentBottomSheet.class));
                if (designer != null) assertEquals(1, componentList(activity).getAdapter().getItemCount());
            });
            awaitBackup();
        }

        void awaitBackup() throws Exception {
            ProjectDataStore data = store();
            ProjectEditorPersistenceTest.waitUntil(() -> {
                synchronized (data) {
                    try { return fixture.read(fixture.backup, "logic").contains(NAME); }
                    catch (Exception ignored) { return false; }
                }
            });
        }

        @Override public void close() throws Exception {
            try {
                if (store().getComponents(ProjectFileBean.DEFAULT_JAVA_NAME).stream()
                        .anyMatch(component -> NAME.equals(component.componentId))) awaitBackup();
            } finally {
                if (designer != null) designer.close();
                if (logic != null) logic.close();
                fixture.close();
            }
        }
    }
}
