package pro.sketchware.activities.editor;

import static org.junit.Assert.*;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pro.sketchware.activities.base.BaseAppCompatActivity;
import pro.sketchware.activities.editor.manage.image.ManageImageActivity;
import pro.sketchware.activities.editor.manage.image.fragments.ImageListFragment;
import pro.sketchware.activities.importicon.MaterialSymbolsPack;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ResourceManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.graphics.SvgUtils;

/** Owns UUID-named resources; restores manager references without saving a user's project. */
@RunWith(AndroidJUnit4.class)
public class ImageSaveConsistencyTest {
    @Test public void missingSourceStopsBeforeWritesAndKeepsTheResourceList() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.add("valid", fixture.svg("valid"));
            ProjectResourceBean missing = fixture.add("missing", fixture.root.resolve("missing.svg"));
            ArrayList<ProjectResourceBean> original = fixture.resources.images;
            expectFailure(fixture.fragment);
            assertSame(original, fixture.resources.images);
            assertTrue(original.isEmpty());
            assertTrue(missing.resFullName.endsWith("missing.svg"));
            assertFalse(Files.exists(fixture.output("valid.xml")));
            assertFalse(Files.exists(Path.of(SketchwarePaths.getBackupPath(fixture.id), "resource")));
        }
    }

    @Test public void conversionFailureDoesNotCommitAnyResourceNamesAndCanBeRetried() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.add("first", fixture.svg("first"));
            fixture.add("second", fixture.svg("second"));
            Files.createDirectory(fixture.output("second.xml"));
            expectFailure(fixture.fragment);
            assertTrue(fixture.resources.images.isEmpty());
            assertTrue(fixture.fragment.getImages().stream().allMatch(image -> image.resFullName.endsWith(".svg")));
            Files.delete(fixture.output("second.xml"));
            fixture.save();
            assertEquals(List.of("first.xml", "second.xml"), fixture.resources.images.stream().map(image -> image.resFullName).collect(Collectors.toList()));
            assertTrue(Files.isRegularFile(fixture.output("first.xml")));
            assertTrue(Files.isRegularFile(fixture.output("second.xml")));
        }
    }

    @Test public void invalidSvgDoesNotCommitTheResourceList() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path source = fixture.root.resolve("invalid.svg");
            Files.write(source, "<svg><path".getBytes(StandardCharsets.UTF_8));
            fixture.add("invalid", source);
            expectFailure(fixture.fragment);
            assertTrue(fixture.resources.images.isEmpty());
            assertFalse(Files.exists(fixture.output("invalid.xml")));
        }
    }

    @Test public void resourceListWriteFailureRestoresThePreviousList() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.add("saved", fixture.svg("saved"));
            ArrayList<ProjectResourceBean> previous = fixture.resources.images;
            Path blocked = Files.createDirectories(Path.of(SketchwarePaths.getBackupPath(fixture.id), "resource"));
            Files.write(blocked.resolve("keep"), new byte[]{1});
            expectFailure(fixture.fragment);
            assertSame(previous, fixture.resources.images);
            assertTrue(previous.isEmpty());
            assertTrue(fixture.fragment.getImages().get(0).resFullName.endsWith(".svg"));
            Files.delete(blocked.resolve("keep"));
            Files.delete(blocked);
            fixture.save();
            assertEquals("saved.xml", fixture.resources.images.get(0).resFullName);
        }
    }

    @Test public void bitmapWriteFailureDoesNotCommitTheResourceList() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path source = fixture.root.resolve("bitmap.png");
            Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            try (var output = Files.newOutputStream(source)) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
            } finally {
                bitmap.recycle();
            }
            fixture.add("bitmap", source);
            Files.createDirectory(fixture.output("bitmap.png"));
            expectFailure(fixture.fragment);
            assertTrue(fixture.resources.images.isEmpty());
            Files.delete(fixture.output("bitmap.png"));
            fixture.save();
            assertEquals("bitmap.png", fixture.resources.images.get(0).resFullName);
            assertTrue(Files.size(fixture.output("bitmap.png")) > 0);
        }
    }

    @Test public void unchangedVectorIsPreservedWhenOtherImagesAreSaved() throws Exception {
        try (Fixture fixture = new Fixture()) {
            byte[] vector = "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"24dp\" android:height=\"24dp\" android:viewportWidth=\"24\" android:viewportHeight=\"24\"/>".getBytes(StandardCharsets.UTF_8);
            Files.write(fixture.output("keep.xml"), vector);
            ProjectResourceBean existing = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, "keep", "keep.xml");
            existing.savedPos = 0;
            fixture.resources.images.add(existing);
            fixture.fragment.getImages().add(existing);
            fixture.add("added", fixture.svg("added"));
            fixture.save();
            assertArrayEquals(vector, Files.readAllBytes(fixture.output("keep.xml")));
            assertFalse(Files.exists(fixture.output("keep.png")));
            assertEquals("keep.xml", fixture.resources.images.get(0).resFullName);
            assertEquals("added.xml", fixture.resources.images.get(1).resFullName);
        }
    }

    @Test public void stagedSymbolSurvivesCacheRemovalAndSavesLater() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path cache = Files.createDirectory(fixture.root.resolve("cache"));
            Files.write(cache.resolve("temporary"), new byte[]{1});
            Context isolated = new ContextWrapper(context()) {
                @Override public File getFilesDir() { return fixture.root.resolve("files").toFile(); }
                @Override public File getCacheDir() { return cache.toFile(); }
            };
            Path exported = MaterialSymbolsPack.load(isolated, "outline").exportIcon("database_search").toPath();
            removeTree(cache);
            assertTrue(Files.isRegularFile(exported));
            fixture.add("staged_symbol", exported);
            fixture.save();
            assertEquals("staged_symbol.xml", fixture.resources.images.get(0).resFullName);
            assertTrue(Files.isRegularFile(fixture.output("staged_symbol.xml")));
        }
    }

    @Test public void failedSaveKeepsTheImageManagerOpenForRetry() throws Exception {
        try (Fixture fixture = new Fixture();
             ActivityScenario<ManageImageActivity> scenario = ActivityScenario.launch(new Intent(context(), ManageImageActivity.class).putExtra("sc_id", fixture.id))) {
            CountDownLatch completed = new CountDownLatch(1);
            scenario.onActivity(activity -> {
                ImageListFragment fragment = activity.getProjectImagesFragment();
                ProjectResourceBean missing = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, "missing", fixture.root.resolve("missing.svg").toString());
                missing.isNew = true;
                missing.savedPos = 2;
                fragment.getImages().add(missing);
                activity.showLoadingDialog();
                try {
                    Class<?> loader = Class.forName(ManageImageActivity.class.getName() + "$SaveImagesAsyncTask");
                    var constructor = loader.getDeclaredConstructor(ManageImageActivity.class);
                    constructor.setAccessible(true);
                    var execute = loader.getDeclaredMethod("execute");
                    execute.setAccessible(true);
                    execute.invoke(constructor.newInstance(activity));
                    BackgroundTasks.runSerial(TaskHost.of(activity), "ImageSaveTestDrain", () -> {}, completed::countDown, null);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            assertTrue(completed.await(10, TimeUnit.SECONDS));
            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                assertTrue(fixture.resources.images.isEmpty());
                try {
                    assertFalse(((Dialog) field(BaseAppCompatActivity.class, "lottieDialog").get(activity)).isShowing());
                } catch (Exception e) { throw new AssertionError(e); }
            });
        }
    }

    @Test public void pendingSymbolColorSurvivesImageManagerRecreation() throws Exception {
        try (Fixture fixture = new Fixture();
             ActivityScenario<ManageImageActivity> scenario = ActivityScenario.launch(new Intent(context(), ManageImageActivity.class).putExtra("sc_id", fixture.id))) {
            Path source = fixture.svg("colored");
            scenario.onActivity(activity -> {
                try {
                    ImageListFragment fragment = activity.getProjectImagesFragment();
                    ProjectResourceBean image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, "colored", source.toString());
                    image.isNew = true;
                    image.savedPos = 2;
                    fragment.getImages().add(image);
                    var setColor = ImageListFragment.class.getDeclaredMethod("addNewColorFilterInfo", String.class, int.class, int.class);
                    setColor.setAccessible(true);
                    setColor.invoke(fragment, "#123456", 0xff123456, 0);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                try { activity.getProjectImagesFragment().saveImages(); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            assertTrue(fixture.backupFinished.await(10, TimeUnit.SECONDS));
            assertTrue(new String(Files.readAllBytes(fixture.output("colored.xml")), StandardCharsets.UTF_8).contains("#123456"));
        }
    }

    private static void expectFailure(ImageListFragment fragment) throws Exception {
        try { fragment.saveImages(); fail("Image save must report the failure"); }
        catch (Exception expected) { assertTrue(expected instanceof IOException || expected instanceof XmlPullParserException); }
    }

    private static final class Fixture implements AutoCloseable {
        final String id = "__image_save_" + UUID.randomUUID();
        final Path root = Files.createTempDirectory(context().getFilesDir().toPath(), "image_save_");
        final ResourceManager resources = new ResourceManager(id);
        final ImageListFragment fragment = new ImageListFragment();
        final Field resourceField = field(ProjectDataManager.class, "resourceManager");
        final Field dataField = field(ProjectDataManager.class, "projectDataStore");
        final Object previousResources = resourceField.get(null), previousData = dataField.get(null);
        final CountDownLatch backupFinished = new CountDownLatch(1);
        final List<Path> owned = new ArrayList<>(List.of(root));

        Fixture() throws Exception {
            try {
                List<Path> projectPaths = List.of(Path.of(SketchwarePaths.getDataPath(id)), Path.of(SketchwarePaths.getBackupPath(id)), Path.of(SketchwarePaths.getTempImagesPath(id)), Path.of(resources.imageDirPath));
                for (Path path : projectPaths) assertFalse("Never overwrite a user project", Files.exists(path));
                owned.addAll(projectPaths);
                Files.createDirectories(Path.of(resources.imageDirPath));
                resources.parseResourceData(new BufferedReader(new StringReader("")));
                resourceField.set(null, resources);
                dataField.set(null, new ProjectDataStore(id) {
                    @Override public boolean canSave() { return true; }
                    @Override public boolean saveAllBackup() { backupFinished.countDown(); return true; }
                });
                field(ImageListFragment.class, "sc_id").set(fragment, id);
                field(ImageListFragment.class, "projectImagesDirectory").set(fragment, resources.imageDirPath);
                field(ImageListFragment.class, "images").set(fragment, new ArrayList<ProjectResourceBean>());
                fragment.svgUtils = new SvgUtils(context());
            } catch (Exception | AssertionError failure) {
                try { close(); } catch (Exception cleanupFailure) { failure.addSuppressed(cleanupFailure); }
                throw failure;
            }
        }

        Path svg(String name) throws Exception {
            Path source = root.resolve(name + ".svg");
            Files.write(source, MaterialSymbolsPack.load(context(), "outline").getSvg("database_search"));
            return source;
        }

        ProjectResourceBean add(String name, Path source) {
            ProjectResourceBean image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, source.toString());
            image.isNew = true;
            image.savedPos = 2;
            fragment.getImages().add(image);
            return image;
        }

        Path output(String name) { return Path.of(resources.imageDirPath, name); }
        void save() throws Exception { fragment.saveImages(); assertTrue(backupFinished.await(10, TimeUnit.SECONDS)); }

        @Override public void close() throws Exception {
            resourceField.set(null, previousResources);
            dataField.set(null, previousData);
            for (Path path : owned) {
                assertTrue(path.equals(root) || path.getFileName().toString().equals(id));
                removeTree(path);
            }
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field result = owner.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static void removeTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
