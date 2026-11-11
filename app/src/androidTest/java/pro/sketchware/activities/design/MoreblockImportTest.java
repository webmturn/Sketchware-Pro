package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inspector.WindowInspector;
import android.widget.Button;
import android.widget.EditText;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.makeblock.MoreblockImporter;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.CollectionBean;
import pro.sketchware.beans.MoreBlockCollectionBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.project.BaseCollectionManager;
import pro.sketchware.core.project.FontCollectionManager;
import pro.sketchware.core.project.ImageCollectionManager;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.project.SoundCollectionManager;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class MoreblockImportTest {
    private static final Instrumentation INSTRUMENTATION =
            InstrumentationRegistry.getInstrumentation();
    private static final Context CONTEXT = INSTRUMENTATION.getTargetContext();
    private static final ProjectFileBean FILE =
            new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");

    @BeforeClass
    public static void grantStoragePermissions() {
        for (String permission :
                List.of(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (CONTEXT.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                INSTRUMENTATION
                        .getUiAutomation()
                        .grantRuntimePermission(CONTEXT.getPackageName(), permission);
            }
        }
    }

    @Test
    public void missingCollectionResourceDoesNotChangeTheProject() throws Exception {
        try (Scope scope = new Scope()) {
            MoreBlockCollectionBean block =
                    favorite(
                            variable(),
                            resource("font", "missing_font"),
                            resource("font", "missing_font"));
            String before = scope.state();
            var disk = scope.project.snapshot();

            scope.start(block);
            scope.clickIfPresent(android.R.id.button1);

            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            scope.project.assertUnchanged(disk);
            String error = scope.awaitError("missing_font");
            assertEquals(error.indexOf("missing_font"), error.lastIndexOf("missing_font"));
        }
    }

    @Test
    public void partialResourceFailureRollsBackAndTheSameDialogCanRetry() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            String sound = scope.source("sound", ".mp3", true);
            String font = scope.source("font", ".ttf", false);
            MoreBlockCollectionBean block =
                    favorite(
                            variable(),
                            list(),
                            resource("resource", image),
                            resource("sound", sound),
                            resource("font", font));
            String before = scope.state();
            var disk = scope.project.snapshot();

            scope.start(block);
            scope.click(android.R.id.button1);
            String error = scope.awaitError(font);
            assertTrue(
                    error,
                    error.contains(
                            CONTEXT.getString(
                                    R.string.moreblock_import_file_missing, font + ".ttf")));
            assertFalse(error, error.contains(SketchwarePaths.getCollectionPath()));

            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            scope.project.assertUnchanged(disk);
            assertFalse(scope.target("image", image, ".png").exists());
            assertFalse(scope.target("sound", sound, ".mp3").exists());
            assertFalse(scope.target("font", font, ".ttf").exists());
            assertTrue(scope.hasButton(android.R.id.button1));

            Files.write(scope.sources.get(font), new byte[] {1, 2, 3});
            scope.click(android.R.id.button1);
            scope.awaitCompletion();

            assertEquals(1, scope.completed.get());
            assertArrayEquals(
                    new byte[] {1, 2, 3},
                    Files.readAllBytes(scope.target("image", image, ".png").toPath()));
            assertArrayEquals(
                    new byte[] {1, 2, 3},
                    Files.readAllBytes(scope.target("sound", sound, ".mp3").toPath()));
            assertArrayEquals(
                    new byte[] {1, 2, 3},
                    Files.readAllBytes(scope.target("font", font, ".ttf").toPath()));
            scope.onMain(
                    () -> {
                        assertEquals(1, scope.resources().images.size());
                        assertEquals(1, scope.resources().sounds.size());
                        assertEquals(1, scope.resources().fonts.size());
                        assertEquals(1, scope.data().getMoreBlocks(FILE.getJavaName()).size());
                        assertTrue(
                                scope.data().getVariables(FILE.getJavaName()).stream()
                                        .anyMatch(pair -> pair.second.equals("added_variable")));
                        assertTrue(
                                scope.data().getListVariables(FILE.getJavaName()).stream()
                                        .anyMatch(pair -> pair.second.equals("added_list")));
                    });
            assertEquals("test_moreblock", block.spec);
        }
    }

    @Test
    public void projectWriteFailureRestoresDependenciesAndThePreviousBlockBody() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            String sound = scope.source("sound", ".mp3", true);
            String font = scope.source("font", ".ttf", true);
            String activity = FILE.getJavaName();
            String blockKey = "test_moreblock_moreBlock";
            var previousBody = new ArrayList<>(List.of(variable()));
            var failNextWrite = new AtomicBoolean(false);
            scope.onMain(
                    () -> {
                        var blocks =
                                new HashMap<String, ArrayList<BlockBean>>(
                                        scope.data().getBlockMap(activity)) {
                                    @Override
                                    public ArrayList<BlockBean> put(
                                            String key, ArrayList<BlockBean> body) {
                                        var previous = super.put(key, body);
                                        if (key.equals(blockKey)
                                                && failNextWrite.getAndSet(false)) {
                                            throw new IllegalStateException(
                                                    "Failed after storing the MoreBlock body");
                                        }
                                        return previous;
                                    }
                                };
                        blocks.put(blockKey, previousBody);
                        scope.data().blockMap.put(activity, blocks);
                    });
            String before = scope.state();
            var disk = scope.project.snapshot();
            scope.start(
                    favorite(
                            variable(),
                            list(),
                            resource("resource", image),
                            resource("sound", sound),
                            resource("font", font)));

            failNextWrite.set(true);
            scope.click(android.R.id.button1);
            awaitImports();

            assertFalse(failNextWrite.get());
            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            scope.onMain(
                    () ->
                            assertSame(
                                    previousBody,
                                    scope.data().getBlockMap(activity).get(blockKey)));
            scope.project.assertUnchanged(disk);
            assertFalse(scope.target("image", image, ".png").exists());
            assertFalse(scope.target("sound", sound, ".mp3").exists());
            assertFalse(scope.target("font", font, ".ttf").exists());

            scope.click(android.R.id.button1);
            scope.awaitCompletion();
            scope.onMain(() -> assertEquals(1, scope.data().getMoreBlocks(activity).size()));
        }
    }

    @Test
    public void anExistingUnregisteredFileIsNeverOverwritten() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            File target = scope.target("image", image, ".png");
            Files.write(target.toPath(), new byte[] {9});
            String before = scope.state();
            var disk = scope.project.snapshot();

            scope.start(favorite(variable(), resource("resource", image)));
            scope.click(android.R.id.button1);
            String error = scope.awaitError(image);
            assertTrue(
                    error,
                    error.contains(
                            CONTEXT.getString(
                                    R.string.moreblock_import_file_exists, image + ".png")));
            assertFalse(error, error.contains(target.getParent()));

            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(target.toPath()));
            scope.project.assertUnchanged(disk);
        }
    }

    @Test
    public void builtinAndExistingResourcesKeepTheirOriginalMeaning() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.prefix + "existing";
            File target = scope.target("image", image, ".png");
            Files.write(target.toPath(), new byte[] {9});
            scope.onMain(
                    () ->
                            scope.resources()
                                    .images
                                    .add(
                                            new ProjectResourceBean(
                                                    ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                                                    image,
                                                    image + ".png")));

            scope.start(
                    favorite(
                            resource("resource", image),
                            resource("font", "default_font"),
                            resource("resource", "app_icon"),
                            resource("resource_bg", "NONE")));

            assertEquals(1, scope.completed.get());
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(target.toPath()));
            scope.onMain(
                    () -> {
                        assertEquals(1, scope.resources().images.size());
                        assertTrue(scope.resources().fonts.isEmpty());
                        assertEquals(1, scope.data().getMoreBlocks(FILE.getJavaName()).size());
                    });
        }
    }

    @Test
    public void cancellingAfterRenameDoesNotRenameTheFavorite() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            scope.onMain(
                    () ->
                            scope.data()
                                    .addMoreBlock(
                                            FILE.getJavaName(),
                                            "test_moreblock",
                                            "test_moreblock"));
            MoreBlockCollectionBean block = favorite(resource("resource", image));
            String before = scope.state();

            scope.start(block);
            scope.onMain(
                    () -> {
                        EditText input = find(R.id.ed_input);
                        assertNotNull(input);
                        input.setText("renamed_moreblock");
                    });
            scope.click(android.R.id.button1);
            scope.click(android.R.id.button2);

            assertEquals("test_moreblock", block.spec);
            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            assertFalse(scope.target("image", image, ".png").exists());
        }
    }

    @Test
    public void importingWithoutDependenciesDoesNotShareTheFavoriteBlocks() throws Exception {
        try (Scope scope = new Scope()) {
            BlockBean original = new BlockBean("1", "initial", "s", "getVar");
            MoreBlockCollectionBean block = favorite(original);

            scope.start(block);

            assertEquals(1, scope.completed.get());
            scope.onMain(
                    () -> {
                        ArrayList<BlockBean> imported =
                                scope.data()
                                        .getBlocks(FILE.getJavaName(), "test_moreblock_moreBlock");
                        assertEquals(1, imported.size());
                        imported.get(0).spec = "edited_in_project";
                    });
            assertEquals("initial", original.spec);
        }
    }

    @Test
    public void resourceFileIoRunsOffTheMainThread() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            List<Throwable> violations = new CopyOnWriteArrayList<>();
            AtomicReference<StrictMode.ThreadPolicy> previous = new AtomicReference<>();
            scope.onMain(
                    () -> {
                        previous.set(StrictMode.getThreadPolicy());
                        StrictMode.setThreadPolicy(
                                new StrictMode.ThreadPolicy.Builder()
                                        .detectDiskReads()
                                        .detectDiskWrites()
                                        .penaltyListener(
                                                Runnable::run,
                                                violation -> {
                                                    for (StackTraceElement frame :
                                                            violation.getStackTrace()) {
                                                        if (frame.getClassName()
                                                                .endsWith(
                                                                        ".MoreblockResourceFiles")) {
                                                            violations.add(violation);
                                                            break;
                                                        }
                                                    }
                                                })
                                        .build());
                    });
            try {
                scope.start(favorite(resource("resource", image)));
                scope.click(android.R.id.button1);
                scope.awaitCompletion();
                assertTrue(violations.toString(), violations.isEmpty());
                assertArrayEquals(
                        new byte[] {1, 2, 3},
                        Files.readAllBytes(scope.target("image", image, ".png").toPath()));
            } finally {
                scope.onMain(() -> StrictMode.setThreadPolicy(previous.get()));
            }
        }
    }

    @Test
    public void destroyingTheActivityBeforeAQueuedImportDoesNotChangeTheProject() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            String before = scope.state();
            AtomicReference<ProjectDataStore> data = new AtomicReference<>();
            AtomicReference<ResourceManager> resources = new AtomicReference<>();
            scope.onMain(
                    () -> {
                        data.set(scope.data());
                        resources.set(scope.resources());
                    });

            try (SerialGate ignored = new SerialGate()) {
                scope.start(favorite(variable(), resource("resource", image)));
                scope.click(android.R.id.button1);
                scope.scenario.close();
            }
            awaitImports();

            assertEquals(0, scope.completed.get());
            assertFalse(scope.target("image", image, ".png").exists());
            StringBuilder after = new StringBuilder();
            data.get().serializeLogicData(after);
            resources.get().serializeResources(after);
            assertEquals(before, after.toString());
        }
    }

    @Test
    public void cancellingAfterCopyRollsBackBeforeChangingTheProject() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            File target = scope.target("image", image, ".png");
            String before = scope.state();
            scope.start(favorite(variable(), resource("resource", image)));

            scope.onMain(
                    () -> {
                        Button positive = find(android.R.id.button1);
                        assertNotNull(positive);
                        positive.performClick();
                        // Copy and a following serial task must finish while the UI commit is held.
                        awaitSerialWork();
                        assertEquals(3, target.length());
                        Button cancel = find(android.R.id.button2);
                        assertNotNull(cancel);
                        cancel.performClick();
                    });
            awaitImports();

            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            assertFalse(target.exists());
        }
    }

    @Test
    public void finishingTheActivityAfterCopyStillRollsBackTheFiles() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            File target = scope.target("image", image, ".png");
            String before = scope.state();
            AtomicReference<ProjectDataStore> data = new AtomicReference<>();
            AtomicReference<ResourceManager> resources = new AtomicReference<>();
            scope.start(favorite(variable(), resource("resource", image)));

            scope.scenario.onActivity(
                    activity -> {
                        data.set(scope.data());
                        resources.set(scope.resources());
                        Button positive = find(android.R.id.button1);
                        assertNotNull(positive);
                        positive.performClick();
                        awaitSerialWork();
                        assertEquals(3, target.length());
                        activity.finish();
                    });
            scope.scenario.close();
            awaitImports();

            assertEquals(0, scope.completed.get());
            assertFalse(target.exists());
            StringBuilder after = new StringBuilder();
            data.get().serializeLogicData(after);
            resources.get().serializeResources(after);
            assertEquals(before, after.toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void invalidResourceFilenameIsReportedWithoutExposingAPath() throws Exception {
        try (Scope scope = new Scope()) {
            String image = scope.source("image", ".png", true);
            ArrayList<CollectionBean> collection =
                    (ArrayList<CollectionBean>)
                            field(BaseCollectionManager.class, "collections")
                                    .get(ImageCollectionManager.getInstance());
            collection.get(0).data = "../invalid.png";
            String before = scope.state();
            var disk = scope.project.snapshot();

            scope.start(favorite(variable(), resource("resource", image)));
            scope.click(android.R.id.button1);

            String error = scope.awaitError(image);
            assertTrue(
                    error,
                    error.contains(
                            CONTEXT.getString(R.string.moreblock_import_invalid_filename, image)));
            assertFalse(error, error.contains("../invalid.png"));
            assertEquals(0, scope.completed.get());
            assertEquals(before, scope.state());
            scope.project.assertUnchanged(disk);
        }
    }

    @Test
    public void failingUiCleanupStillRestoresManagersAndRemovesOwnedFiles() throws Exception {
        Scope scope =
                new Scope() {
                    @Override
                    void closeActivity() {
                        super.closeActivity();
                        throw new IllegalStateException("UI cleanup failure");
                    }
                };
        try {
            String image = scope.source("image", ".png", true);
            scope.start(favorite(resource("resource", image)));

            Exception failure = assertThrows(Exception.class, scope::close);
            assertTrue(failure.getSuppressed()[0] instanceof IllegalStateException);
            for (var manager : scope.originalManagers.entrySet()) {
                assertSame(manager.getValue(), manager.getKey().get(null));
            }
            assertFalse(Files.exists(scope.cache));
            for (Path source : scope.sources.values()) assertFalse(Files.exists(source));
            for (File root : scope.project.roots) assertFalse(root.exists());
        } finally {
            scope.close();
        }
    }

    private static MoreBlockCollectionBean favorite(BlockBean... blocks) {
        return new MoreBlockCollectionBean(
                "favorite", "test_moreblock", new ArrayList<>(List.of(blocks)));
    }

    private static BlockBean variable() {
        return new BlockBean("variable", "added_variable", "b", "getVar");
    }

    private static BlockBean list() {
        return new BlockBean("list", "added_list", "l", "List String", "getVar");
    }

    private static BlockBean resource(String type, String name) {
        BlockBean block = new BlockBean(name, "use %m." + type, " ", "test_resource");
        block.parameters.add(name);
        return block;
    }

    private static <T extends View> T find(int id) {
        List<View> windows = WindowInspector.getGlobalWindowViews();
        for (int i = windows.size() - 1; i >= 0; i--) {
            T view = windows.get(i).findViewById(id);
            if (view != null && view.isShown()) return view;
        }
        return null;
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ExecutorService serialExecutor() throws Exception {
        return (ExecutorService) field(BackgroundTasks.class, "SERIAL_EXECUTOR").get(null);
    }

    private static void awaitSerialWork() {
        try {
            serialExecutor().submit(() -> {}).get(5, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new AssertionError("Serial work did not finish", failure);
        }
    }

    private static void awaitImports() {
        awaitSerialWork();
        INSTRUMENTATION.waitForIdleSync();
        // A rejected UI commit schedules its rollback as a second background phase.
        awaitSerialWork();
        INSTRUMENTATION.waitForIdleSync();
    }

    private static class SerialGate implements AutoCloseable {
        final CountDownLatch release = new CountDownLatch(1);
        final Future<?> pending;

        SerialGate() throws Exception {
            CountDownLatch entered = new CountDownLatch(1);
            pending =
                    serialExecutor()
                            .submit(
                                    () -> {
                                        entered.countDown();
                                        assertTrue(release.await(10, TimeUnit.SECONDS));
                                        return null;
                                    });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
        }

        @Override
        public void close() throws Exception {
            release.countDown();
            pending.get(10, TimeUnit.SECONDS);
        }
    }

    private static class Scope implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture project =
                new ProjectEditorPersistenceTest.Fixture();
        final String prefix = "mbtest_" + UUID.randomUUID().toString().replace("-", "") + "_";
        final Path cache =
                Files.createTempDirectory(CONTEXT.getCacheDir().toPath(), "moreblock-import-");
        final Map<Field, Object> originalManagers = new LinkedHashMap<>();
        final Map<String, Path> sources = new LinkedHashMap<>();
        final List<Path> sourceDirectories = new ArrayList<>();
        final List<String> toasts = new CopyOnWriteArrayList<>();
        final AtomicInteger completed = new AtomicInteger();
        ActivityScenario<LogicEditorActivity> scenario;
        boolean closed;

        Scope() throws Exception {
            try {
                install(
                        ImageCollectionManager.class,
                        new ImageCollectionManager() {
                            @Override
                            public void initializePaths() {
                                collectionFilePath = cache.resolve("images").toString();
                                dataDirPath = sourceDirectory("image").toString();
                            }
                        });
                install(
                        SoundCollectionManager.class,
                        new SoundCollectionManager() {
                            @Override
                            public void initializePaths() {
                                collectionFilePath = cache.resolve("sounds").toString();
                                dataDirPath = sourceDirectory("sound").toString();
                            }
                        });
                install(
                        FontCollectionManager.class,
                        new FontCollectionManager() {
                            @Override
                            public void initializePaths() {
                                collectionFilePath = cache.resolve("fonts").toString();
                                dataDirPath = sourceDirectory("font").toString();
                            }
                        });
                project.seed(false, false);
                ProjectDataManager.reloadProjectFromData(project.id);
                Intent intent =
                        new Intent(CONTEXT, LogicEditorActivity.class)
                                .putExtra("sc_id", project.id)
                                .putExtra("id", "onCreate")
                                .putExtra("event", "initializeLogic")
                                .putExtra("event_text", "MoreBlock import test")
                                .putExtra("project_file", FILE);
                scenario = ActivityScenario.launch(intent);
                INSTRUMENTATION.waitForIdleSync();
                INSTRUMENTATION
                        .getUiAutomation()
                        .setOnAccessibilityEventListener(
                                event -> {
                                    if (event.getEventType()
                                            == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
                                        toasts.add(event.getText().toString());
                                });
            } catch (Exception | Error failure) {
                try {
                    close();
                } catch (Exception | AssertionError cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
        }

        void install(Class<?> type, BaseCollectionManager manager) throws Exception {
            Field instance = field(type, "instance");
            originalManagers.put(instance, instance.get(null));
            instance.set(null, manager);
        }

        Path sourceDirectory(String kind) {
            return new File(SketchwarePaths.getCollectionPath(), kind + "/data").toPath();
        }

        @SuppressWarnings("unchecked")
        String source(String kind, String extension, boolean present) throws Exception {
            String name = prefix + kind;
            Path path = sourceDirectory(kind).resolve(name + extension);
            assertFalse(Files.exists(path));
            createParents(path.getParent());
            sources.put(name, path);
            if (present) Files.write(path, new byte[] {1, 2, 3});
            BaseCollectionManager manager =
                    switch (kind) {
                        case "image" -> ImageCollectionManager.getInstance();
                        case "sound" -> SoundCollectionManager.getInstance();
                        case "font" -> FontCollectionManager.getInstance();
                        default -> throw new AssertionError(kind);
                    };
            ((ArrayList<CollectionBean>)
                            field(BaseCollectionManager.class, "collections").get(manager))
                    .add(new CollectionBean(name, name + extension));
            return name;
        }

        void createParents(Path directory) throws Exception {
            if (Files.isDirectory(directory)) return;
            createParents(directory.getParent());
            Files.createDirectory(directory);
            sourceDirectories.add(directory);
        }

        File target(String kind, String name, String extension) {
            String root =
                    switch (kind) {
                        case "image" -> SketchwarePaths.getImagesPath();
                        case "sound" -> SketchwarePaths.getSoundsPath();
                        case "font" -> SketchwarePaths.getFontsResourcePath();
                        default -> throw new AssertionError(kind);
                    };
            return new File(root, project.id + "/" + name + extension);
        }

        ProjectDataStore data() {
            return ProjectDataManager.getProjectDataManager(project.id);
        }

        ResourceManager resources() {
            return ProjectDataManager.getResourceManager(project.id);
        }

        void onMain(Runnable action) {
            scenario.onActivity(ignored -> action.run());
        }

        String state() {
            AtomicReference<String> state = new AtomicReference<>();
            onMain(
                    () -> {
                        StringBuilder content = new StringBuilder();
                        data().serializeLogicData(content);
                        resources().serializeResources(content);
                        state.set(content.toString());
                    });
            return state.get();
        }

        void start(MoreBlockCollectionBean block) {
            scenario.onActivity(
                    activity -> {
                        MoreblockImporter importer =
                                new MoreblockImporter(activity, project.id, FILE);
                        importer.importMoreblock(block, completed::incrementAndGet);
                    });
            INSTRUMENTATION.waitForIdleSync();
        }

        boolean hasButton(int id) {
            AtomicReference<Boolean> found = new AtomicReference<>(false);
            onMain(() -> found.set(find(id) != null));
            return found.get();
        }

        void clickIfPresent(int id) {
            onMain(
                    () -> {
                        Button button = find(id);
                        if (button != null) button.performClick();
                    });
            INSTRUMENTATION.waitForIdleSync();
        }

        void click(int id) {
            assertTrue("Expected import dialog button", hasButton(id));
            clickIfPresent(id);
        }

        void awaitCompletion() throws Exception {
            awaitImports();
            assertEquals(1, completed.get());
        }

        String awaitError(String resource) {
            String prefix = CONTEXT.getString(R.string.error_import_moreblocks, "");
            long deadline = SystemClock.uptimeMillis() + 5000;
            while (SystemClock.uptimeMillis() < deadline) {
                for (String text : toasts) {
                    if (text.contains(prefix) && text.contains(resource)) return text;
                }
                SystemClock.sleep(50);
            }
            fail("Import error was not shown: " + toasts);
            return null;
        }

        void closeActivity() {
            if (scenario == null) return;
            try {
                if (scenario.getState() != Lifecycle.State.DESTROYED) {
                    clickIfPresent(android.R.id.button2);
                }
            } finally {
                scenario.close();
                INSTRUMENTATION.waitForIdleSync();
            }
        }

        @Override
        public void close() throws Exception {
            if (closed) return;
            closed = true;
            List<AutoCloseable> cleanup = new ArrayList<>();
            cleanup.add(
                    () -> INSTRUMENTATION.getUiAutomation().setOnAccessibilityEventListener(null));
            cleanup.add(this::closeActivity);
            cleanup.add(MoreblockImportTest::awaitImports);
            cleanup.add(ProjectDataManager::clearAll);
            for (var manager : originalManagers.entrySet()) {
                cleanup.add(() -> manager.getKey().set(null, manager.getValue()));
            }
            for (Path path : sources.values()) {
                cleanup.add(
                        () -> {
                            assertTrue(path.getFileName().toString().startsWith(prefix));
                            assertEquals(
                                    SketchwarePaths.getCollectionPath(),
                                    path.getParent().getParent().getParent().toString());
                            Files.deleteIfExists(path);
                        });
            }
            for (int i = sourceDirectories.size() - 1; i >= 0; i--) {
                Path directory = sourceDirectories.get(i);
                cleanup.add(() -> Files.delete(directory));
            }
            cleanup.add(
                    () -> {
                        try (Stream<Path> paths = Files.walk(cache)) {
                            for (Path path :
                                    (Iterable<Path>)
                                            paths.sorted(Comparator.reverseOrder())::iterator) {
                                Files.delete(path);
                            }
                        }
                    });
            cleanup.add(project);
            Exception failure = null;
            for (AutoCloseable action : cleanup) {
                try {
                    action.close();
                } catch (Exception | AssertionError error) {
                    if (failure == null) failure = new Exception("Import test cleanup failed");
                    failure.addSuppressed(error);
                }
            }
            if (failure != null) throw failure;
        }
    }
}
