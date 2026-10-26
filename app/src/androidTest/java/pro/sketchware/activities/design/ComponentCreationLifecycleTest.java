package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
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

import java.lang.reflect.Field;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.design.fragments.ComponentListFragment;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.component.AddComponentBottomSheet;
import pro.sketchware.beans.ComponentBean;
import pro.sketchware.beans.ProjectFileBean;
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
    public void listSwipesStayInTheSelectorAndHandleStillDismisses() throws Exception {
        try (Host host = new Host(false)) {
            host.openSelector();
            Rect[] listBounds = {null};
            host.onActivity(activity -> listBounds[0] = screenBounds(find(activity, AddComponentBottomSheet.class)
                    .requireView().findViewById(R.id.component_list)));
            Rect area = listBounds[0];
            float upper = area.top + area.height() * 0.25f;
            float lower = area.top + area.height() * 0.75f;

            // Starting at the top must not turn a list swipe into a sheet dismissal.
            swipe(area.centerX(), upper, lower);
            assertSelectorExpanded(host);
            for (int i = 0; i < 3; i++) {
                swipe(area.centerX(), lower, upper);
                assertSelectorExpanded(host);
                host.onActivity(activity -> {
                    RecyclerView list = find(activity, AddComponentBottomSheet.class).requireView()
                            .findViewById(R.id.component_list);
                    var first = list.findViewHolderForAdapterPosition(0);
                    assertTrue("Swipe up must move the first row above the viewport",
                            first == null || first.itemView.getTop() < list.getPaddingTop());
                });
                swipe(area.centerX(), upper, lower);
                assertSelectorExpanded(host);
                host.onActivity(activity -> {
                    RecyclerView list = find(activity, AddComponentBottomSheet.class).requireView()
                            .findViewById(R.id.component_list);
                    var first = list.findViewHolderForAdapterPosition(0);
                    assertNotNull("Swipe down must return to the first row", first);
                    assertEquals(list.getPaddingTop(), first.itemView.getTop());
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

    private static void assertSelectorExpanded(Host host) {
        host.onActivity(activity -> {
            AddComponentBottomSheet sheet = find(activity, AddComponentBottomSheet.class);
            assertNotNull("Scrolling the list must keep the selector open", sheet);
            BottomSheetDialog dialog = (BottomSheetDialog) sheet.requireDialog();
            assertEquals(BottomSheetBehavior.STATE_EXPANDED, dialog.getBehavior().getState());
        });
    }

    private static Rect screenBounds(View view) {
        int[] position = new int[2];
        view.getLocationOnScreen(position);
        return new Rect(position[0], position[1], position[0] + view.getWidth(), position[1] + view.getHeight());
    }

    private static void swipe(float x, float fromY, float toY) {
        long downTime = SystemClock.uptimeMillis();
        for (int step = 0; step <= 24; step++) {
            int action = step == 0 ? MotionEvent.ACTION_DOWN : step == 24 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_MOVE;
            MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                    x, fromY + (toY - fromY) * step / 24, 0);
            try {
                InstrumentationRegistry.getInstrumentation().sendPointerSync(event);
            } finally {
                event.recycle();
            }
            SystemClock.sleep(16);
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
            host.onActivity(activity -> {
                AddComponentBottomSheet selector = find(activity, AddComponentBottomSheet.class);
                FragmentManager manager = activity.getSupportFragmentManager();
                manager.beginTransaction().detach(selector).commitNow();
                manager.beginTransaction().attach(selector).commitNow();
            });
            idle();
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
            } catch (Exception | AssertionError e) {
                close();
                throw e;
            }
        }

        void onActivity(Consumer<FragmentActivity> action) {
            if (designer != null) designer.onActivity(action::accept); else logic.onActivity(action::accept);
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
