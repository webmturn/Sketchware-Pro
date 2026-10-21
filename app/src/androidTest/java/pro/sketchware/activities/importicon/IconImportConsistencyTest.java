package pro.sketchware.activities.importicon;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.importicon.adapters.IconAdapter;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.util.UIHelper;

/** Uses private SVG fixtures and returns intents; never imports files into a user's project. */
@RunWith(AndroidJUnit4.class)
public class IconImportConsistencyTest {
    @Test public void filteredResultsNeverAppendUnfilteredPages() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, "home"));
            fixture.awaitCount(3);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(3);
            fixture.on(activity -> assertTrue(fixture.adapter().getCurrentList().stream().allMatch(icon -> icon.first.startsWith("home"))));
        }
    }

    @Test public void clearingSearchRestartsPaginationAfterTheLastPage() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            fixture.on(activity -> {
                invoke(activity, "loadMoreItems"); invoke(activity, "loadMoreItems"); invoke(activity, "loadMoreItems");
                assertTrue((Boolean) get(activity, "isLastPage"));
                invoke(activity, "filterIcons", String.class, "home");
                invoke(activity, "filterIcons", String.class, "");
                assertFalse("Clearing the query must reopen pagination", (Boolean) get(activity, "isLastPage"));
            });
            fixture.awaitCount(40);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(80);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(95);
        }
    }

    @Test public void searchIgnoresWhitespaceAndProcessLocale() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            Locale original = Locale.getDefault();
            Locale format = Locale.getDefault(Locale.Category.FORMAT), display = Locale.getDefault(Locale.Category.DISPLAY);
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, "  OTHER  "));
                fixture.awaitCount(92);
                fixture.on(activity -> {
                    List<Pair<String, String>> renamed = new ArrayList<>(fixture.catalog);
                    renamed.set(0, Pair.create("image", renamed.get(0).second));
                    set(activity, "allIconPaths", renamed);
                    invoke(activity, "filterIcons", String.class, " IMAGE ");
                });
                fixture.awaitCount(1);
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, "   "));
                fixture.awaitCount(40);
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, null));
                fixture.awaitCount(40);
            } finally {
                Locale.setDefault(original);
                Locale.setDefault(Locale.Category.FORMAT, format); Locale.setDefault(Locale.Category.DISPLAY, display);
            }
        }
    }

    @Test public void saveKeepsSelectedIconWhenTheListIsReordered() throws Exception {
        saveAfterListChange(false);
    }

    @Test public void catalogCompletionKeepsTheActiveSearch() throws Exception {
        try (Fixture fixture = new Fixture()) {
            String query = "__no_match_" + UUID.randomUUID();
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, query));
            fixture.awaitCount(0);
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            BackgroundTasks.runSerial(TaskHost.of(fixture.activity), "IconImportReloadTest",
                    () -> invoke(fixture.activity, "listIcons"), done::countDown, error -> { failure.set(error); done.countDown(); });
            assertTrue(done.await(10, TimeUnit.SECONDS));
            assertNull(failure.get());
            fixture.awaitCount(0);
        }
    }

    @Test public void emptyCatalogClearsPreviousResults() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(0);
            fixture.on(activity -> assertTrue((Boolean) get(activity, "isLastPage")));
        }
    }

    @Test public void saveKeepsSelectedIconWhenTheListShrinks() throws Exception {
        saveAfterListChange(true);
    }

    @Test public void invalidClickPositionsAreIgnored() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            fixture.on(activity -> {
                UIHelper.lastClickTime = 0; activity.onIconSelected(-1);
                UIHelper.lastClickTime = 0; activity.onIconSelected(2);
                assertNull(dialog());
                assertFalse(activity.isFinishing());
            });
        }
    }

    @Test public void changedPathsRebindAndStyleUpdatesKeepAsyncDiffs() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            AtomicInteger fullRefreshes = new AtomicInteger(), changes = new AtomicInteger(), colorPayloads = new AtomicInteger();
            RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
                @Override public void onChanged() { fullRefreshes.incrementAndGet(); }
                @Override public void onItemRangeChanged(int start, int count, Object payload) {
                    changes.addAndGet(count); if (payload != null) colorPayloads.incrementAndGet();
                }
            };
            List<Pair<String, String>> replacement = List.of(Pair.create("home_0", fixture.root.resolve("changed").toString()), fixture.catalog.get(1));
            fixture.on(activity -> {
                fixture.adapter().registerAdapterDataObserver(observer);
                fixture.adapter().submitList(replacement);
            });
            fixture.awaitList(replacement);
            fixture.on(activity -> {
                assertTrue("A changed SVG path must rebind its row", changes.get() > 0);
                fixture.adapter().setSelectedColor(0xff123456);
                fixture.adapter().setSelectedIconType("outline");
                assertTrue("Color changes should use a content payload", colorPayloads.get() > 0);
                assertEquals(0, fullRefreshes.get());
                fixture.adapter().unregisterAdapterDataObserver(observer);
            });
        }
    }

    private static void saveAfterListChange(boolean shrink) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            Pair<String, String> selected = fixture.catalog.get(1);
            fixture.on(activity -> { UIHelper.lastClickTime = 0; activity.onIconSelected(1); assertNotNull(dialog()); });
            List<Pair<String, String>> next = shrink ? List.of(fixture.catalog.get(0)) : List.of(selected, fixture.catalog.get(0));
            fixture.on(activity -> fixture.adapter().submitList(next));
            fixture.awaitList(next);
            fixture.on(activity -> {
                View root = dialog(); assertNotNull(root);
                assertTrue(root.findViewById(android.R.id.button1).performClick());
            });
            var result = fixture.scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            assertEquals(selected.second + "/round.svg", result.getResultData().getStringExtra("iconPath"));
            assertEquals("icon_" + selected.first + "_round", result.getResultData().getStringExtra("iconName"));
            assertEquals(0xff9e9e9e, result.getResultData().getIntExtra("iconColor", 0));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory(context().getCacheDir().toPath(), "icon_import_");
        final ActivityScenario<ImportIconActivity> scenario;
        final long originalClickTime = UIHelper.lastClickTime;
        ImportIconActivity activity;
        List<Pair<String, String>> catalog;

        Fixture() throws Exception {
            scenario = ActivityScenario.launchActivityForResult(new Intent(context(), ImportIconActivity.class)
                    .putExtra("sc_id", "__icon_import_" + UUID.randomUUID()).putStringArrayListExtra("imageNames", new ArrayList<>()));
            on(value -> { activity = value; value.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); });
            await(() -> {
                AtomicInteger size = new AtomicInteger(); on(value -> size.set(adapter().getItemCount())); return size.get() > 0;
            });
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(activity), "IconImportTestDrain", done::countDown, null, null);
            assertTrue(done.await(10, TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void catalog(int size) throws Exception {
            catalog = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                String name = index < 3 ? "home_" + index : "other_" + index;
                Path folder = Files.createDirectory(root.resolve(name));
                String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\"><path d=\"M0 0h24v24H0z\"/></svg>";
                for (String type : new String[]{"round", "outline"}) Files.write(folder.resolve(type + ".svg"), svg.getBytes(StandardCharsets.UTF_8));
                catalog.add(Pair.create(name, folder.toString()));
            }
            on(value -> {
                set(value, "allIconPaths", new ArrayList<>(catalog)); set(value, "icons", new ArrayList<>());
                set(value, "currentPage", 0); set(value, "isLastPage", false); set(value, "isLoading", false);
                invoke(value, "filterIcons", String.class, "");
            });
            awaitCount(Math.min(40, size));
        }

        IconAdapter adapter() throws Exception { return (IconAdapter) get(activity, "adapter"); }

        void awaitCount(int expected) throws Exception {
            await(() -> { AtomicInteger size = new AtomicInteger(); on(value -> size.set(adapter().getItemCount())); return size.get() == expected; });
        }

        void awaitList(List<Pair<String, String>> expected) throws Exception {
            await(() -> { AtomicReference<List<Pair<String, String>>> list = new AtomicReference<>(); on(value -> list.set(adapter().getCurrentList())); return expected.equals(list.get()); });
        }

        void on(Action action) throws Exception {
            AtomicReference<Throwable> error = new AtomicReference<>();
            scenario.onActivity(value -> { try { action.run(value); } catch (Throwable failure) { error.set(failure); } });
            if (error.get() != null) throw new AssertionError(error.get());
        }

        @Override public void close() throws Exception {
            scenario.close(); UIHelper.lastClickTime = originalClickTime;
            assertEquals(context().getCacheDir().getCanonicalFile().toPath(), root.toFile().getCanonicalFile().toPath().getParent());
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
            }
        }
    }

    private static View dialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) if (root.findViewById(R.id.inputText) != null) return root;
        return null;
    }

    private static void await(Check check) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        do { if (check.get()) return; SystemClock.sleep(25); } while (SystemClock.uptimeMillis() < end);
        fail("Timed out waiting for icon list");
    }
    private interface Check { boolean get() throws Exception; }
    private interface Action { void run(ImportIconActivity activity) throws Exception; }
    private static Object get(Object target, String name) throws Exception {
        Field field = ImportIconActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = ImportIconActivity.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void invoke(Object target, String name) throws Exception { invoke(target, name, null, null); }
    private static void invoke(Object target, String name, Class<?> parameter, Object argument) throws Exception {
        Method method = parameter == null ? ImportIconActivity.class.getDeclaredMethod(name) : ImportIconActivity.class.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        try { if (parameter == null) method.invoke(target); else method.invoke(target, argument); }
        catch (InvocationTargetException error) { throw new Exception(error.getCause()); }
    }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
