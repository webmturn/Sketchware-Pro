package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.CollectionBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.block.view.DefinitionBlockView;
import pro.sketchware.core.project.BlockCollectionManager;
import pro.sketchware.core.project.ProjectDataManager;

/** Uses an isolated collection manager; the user's saved favorites are never changed. */
@RunWith(AndroidJUnit4.class)
public class LogicBlockCollectionLoadingTest {
    private static final Gson BLOCK_GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Test(timeout = 30000) public void parsingRunsOffMainAndViewsAreAttachedOnMain() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            AtomicInteger pulses = new AtomicInteger();
            AtomicInteger attachments = new AtomicInteger();
            editor.scenario.onActivity(activity -> {
                favorites(activity).setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override public void onChildViewAdded(View parent, View child) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());
                        attachments.incrementAndGet();
                    }
                    @Override public void onChildViewRemoved(View parent, View child) {}
                });
                activity.loadBlockCollections();
                activity.getWindow().getDecorView().post(pulses::incrementAndGet);
            });
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            waitUntil(() -> pulses.get() > 0);
            gate.release.countDown();
            editor.drain();
            editor.assertNames("first");
            assertEquals(2, attachments.get()); // Definition view and its spacer.
        }
    }

    @Test(timeout = 30000) public void latestRequestWinsWhenOlderParsingFinishesLast() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            editor.reload();
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            editor.scenario.onActivity(activity -> {
                editor.manager.renameBlock("first", "latest", false);
                editor.add("second");
                activity.loadBlockCollections();
            });
            editor.awaitNames("latest", "second");
            gate.release.countDown();
            editor.drain();
            editor.assertNames("latest", "second");
        }
    }

    @Test(timeout = 30000) public void editsDuringParsingInvalidateTheSnapshotWithoutAnotherRequest() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            editor.scenario.onActivity(activity -> editor.add("other"));
            editor.reload();
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            editor.scenario.onActivity(activity -> {
                editor.manager.removeBlock("first", false);
                editor.manager.renameBlock("other", "renamed", false);
                editor.add("new");
            });
            gate.release.countDown();
            editor.awaitNames("renamed", "new");
            editor.drain();
            editor.assertNames("renamed", "new");
        }
    }

    @Test(timeout = 30000) public void clearingTheManagerCannotRestoreTheOldSnapshot() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            editor.reload();
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            editor.scenario.onActivity(activity -> editor.manager.clearCollections());
            gate.release.countDown();
            editor.awaitNames();
            editor.drain();
            editor.assertNames();
        }
    }

    @Test(timeout = 30000) public void emptyAndMalformedCollectionsDoNotHideValidFavorites() throws Exception {
        try (Editor editor = new Editor()) {
            editor.scenario.onActivity(activity -> {
                ArrayList<CollectionBean> sources = new ArrayList<>();
                sources.add(new CollectionBean("empty", ""));
                sources.add(new CollectionBean("invalid", "not-json"));
                sources.add(new CollectionBean("null-block", "null\n"));
                sources.add(new CollectionBean("null-data", null));
                sources.add(new CollectionBean("valid", BLOCK_GSON.toJson(block())));
                sources.add(new CollectionBean("readable-prefix", BLOCK_GSON.toJson(block()) + "\n{"));
                set(BlockCollectionManager.class.getSuperclass(), editor.manager, "collections", sources);
                activity.loadBlockCollections();
            });
            editor.awaitNames("valid", "readable-prefix");
            editor.drain();
            editor.assertNames("valid", "readable-prefix");
            editor.scenario.onActivity(activity -> {
                for (int i = 0; i < favorites(activity).getChildCount(); i++) {
                    if (favorites(activity).getChildAt(i) instanceof DefinitionBlockView view) {
                        assertEquals(1, view.getData().size());
                        assertEquals("1", view.getData().get(0).id);
                    }
                }
            });
        }
    }

    @Test(timeout = 30000) public void recreationDoesNotApplyTheOldActivityResult() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            AtomicReference<ViewGroup> oldFavorites = new AtomicReference<>();
            AtomicInteger oldAttachments = new AtomicInteger();
            editor.reload();
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            editor.scenario.onActivity(activity -> {
                oldFavorites.set(favorites(activity));
                watchAttachments(oldFavorites.get(), oldAttachments);
            });
            editor.scenario.recreate();
            editor.scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            editor.awaitNames("first");
            gate.release.countDown();
            editor.drain();
            assertEquals(0, oldAttachments.get());
            editor.assertNames("first");
        }
    }

    @Test(timeout = 30000) public void finishingEditorNeverAttachesPendingViews() throws Exception {
        try (Editor editor = new Editor(); Gate gate = editor.gateParsing()) {
            AtomicInteger attachments = new AtomicInteger();
            editor.reload();
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            editor.scenario.onActivity(activity -> watchAttachments(favorites(activity), attachments));
            editor.scenario.close();
            gate.release.countDown();
            editor.drain();
            assertEquals(0, attachments.get());
        }
    }

    private static void watchAttachments(ViewGroup parent, AtomicInteger count) {
        parent.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override public void onChildViewAdded(View parent, View child) { count.incrementAndGet(); }
            @Override public void onChildViewRemoved(View parent, View child) {}
        });
    }

    private static class Gate implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean first = new AtomicBoolean(true);
        @Override public void close() { release.countDown(); }
    }

    private static class Editor implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture project = new ProjectEditorPersistenceTest.Fixture();
        final Path source = Files.createTempDirectory(context().getCacheDir().toPath(), "block-collections-");
        final Field instance = field(BlockCollectionManager.class, "instance");
        final Object originalManager = instance.get(null);
        final BlockCollectionManager manager;
        final ActivityScenario<LogicEditorActivity> scenario;

        Editor() throws Exception {
            Path isolated = source;
            manager = new BlockCollectionManager() {
                @Override public void initializePaths() {
                    collectionFilePath = isolated.resolve("list").toString();
                    dataDirPath = isolated.resolve("data").toString();
                }
            };
            add("first");
            instance.set(null, manager);
            project.seed(false, false);
            ProjectDataManager.reloadProjectFromData(project.id);
            Intent intent = new Intent(context(), LogicEditorActivity.class)
                    .putExtra("sc_id", project.id).putExtra("id", "onCreate")
                    .putExtra("event", "initializeLogic").putExtra("event_text", "Collection loading test")
                    .putExtra("project_file", new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
            scenario = ActivityScenario.launch(intent);
            scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            awaitNames("first");
            drain();
        }

        void add(String name) {
            try { manager.addBlock(name, new ArrayList<>(List.of(block())), false); }
            catch (Exception e) { throw new AssertionError(e); }
        }

        Gate gateParsing() {
            Gate gate = new Gate();
            Gson parser = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(BlockBean.class, (JsonDeserializer<BlockBean>) (json, type, context) -> {
                        assertNotSame("Collection parsing must leave the UI thread", Looper.getMainLooper(), Looper.myLooper());
                        if (gate.first.compareAndSet(true, false)) {
                            gate.entered.countDown();
                            try { assertTrue("Parsing gate timed out", gate.release.await(15, TimeUnit.SECONDS)); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                        }
                        return BLOCK_GSON.fromJson(json, BlockBean.class);
                    }).create();
            scenario.onActivity(activity -> set(BlockCollectionManager.class, manager, "blockGson", parser));
            return gate;
        }

        void reload() { scenario.onActivity(LogicEditorActivity::loadBlockCollections); }

        List<String> names() {
            ArrayList<String> names = new ArrayList<>();
            scenario.onActivity(activity -> {
                ViewGroup container = favorites(activity);
                for (int i = 0; i < container.getChildCount(); i++) {
                    if (container.getChildAt(i) instanceof DefinitionBlockView view) names.add(view.getCollectionName());
                }
            });
            return names;
        }

        void awaitNames(String... expected) throws Exception { waitUntil(() -> names().equals(List.of(expected))); }
        void assertNames(String... expected) { assertEquals(List.of(expected), names()); }

        void drain() throws Exception {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) field(BackgroundTasks.class, "IO_EXECUTOR").get(null);
            for (int i = 0; i < 2; i++) {
                waitUntil(() -> executor.getActiveCount() == 0 && executor.getQueue().isEmpty());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            }
        }

        @Override public void close() throws Exception {
            try {
                drain();
                scenario.close();
            } finally {
                instance.set(null, originalManager);
                project.close();
                assertEquals(context().getCacheDir().getCanonicalFile(), source.toFile().getCanonicalFile().getParentFile());
                try (var entries = Files.walk(source)) {
                    for (Path entry : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(entry);
                }
            }
        }
    }

    private static BlockBean block() { return new BlockBean("1", "saved block", " ", "doToast"); }
    private static ViewGroup favorites(LogicEditorActivity activity) { return activity.findViewById(R.id.layout_favorite); }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Class<?> type, Object owner, String name, Object value) {
        try { field(type, name).set(owner, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private interface Check { boolean get() throws Exception; }
    private static void waitUntil(Check check) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        while (!check.get()) {
            if (SystemClock.uptimeMillis() > end) fail("Timed out waiting for collection load");
            SystemClock.sleep(30);
        }
    }
}
