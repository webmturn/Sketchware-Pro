package pro.sketchware.activities.iconcreator;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.databinding.ActivityIconCreatorBinding;

@RunWith(AndroidJUnit4.class)
public class IconExportTest {
    private static final String[] DENSITIES = {
            "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"
    };

    @Test(timeout = 30000) public void exportWaitsForWorkerWhileMainThreadRemainsResponsive() throws Exception {
        try (Project project = new Project(); Gate gate = project.blockWorker()) {
            AtomicInteger pulses = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                binding(activity).save.performClick();
                assertFalse("Export must wait for the worker", activity.isFinishing());
                assertFalse(binding(activity).save.isEnabled());
                binding(activity).getRoot().post(pulses::incrementAndGet);
            });
            waitUntil(() -> pulses.get() > 0);
            assertFalse(Files.exists(project.data.resolve("data.json")));
            assertFalse(Files.exists(project.launcher()));
            gate.release.countDown();
            project.awaitSuccess(false, 5);
        }
    }

    @Test(timeout = 30000) public void adaptiveExportKeepsAllLayersAndRestoresPreviewVisibility() throws Exception {
        try (Project project = new Project(); Gate gate = project.blockWorker()) {
            project.scenario.onActivity(activity -> {
                var views = binding(activity);
                views.adaptiveCheck.setChecked(true);
                views.scoreCheck.setChecked(true);
                views.textureCheck.setChecked(true);
                views.save.performClick();
                assertEquals(View.VISIBLE, views.appIcoBg.getVisibility());
                assertEquals(View.VISIBLE, views.appIcoScore.getVisibility());
                assertEquals(View.VISIBLE, views.appIcoTexture.getVisibility());
            });
            gate.release.countDown();
            project.awaitSuccess(true, 20);
            for (String name : new String[]{"ic_launcher.png", "ic_launcher_foreground.png",
                    "ic_launcher_monochrome.png", "ic_launcher_background.png"}) {
                byte[] reference = Files.readAllBytes(project.icons.resolve("mipmap-mdpi/" + name));
                for (String density : DENSITIES) {
                    Path path = project.icons.resolve(density).resolve(name);
                    assertArrayEquals(reference, Files.readAllBytes(path));
                    Bitmap bitmap = BitmapFactory.decodeFile(path.toString());
                    assertNotNull(bitmap);
                    if (name.contains("foreground") || name.contains("monochrome")) {
                        assertEquals(project.launcherWidth + 150, bitmap.getWidth());
                        assertEquals(0, bitmap.getPixel(0, 0));
                    }
                    bitmap.recycle();
                }
            }
        }
    }

    @Test(timeout = 30000) public void workerUsesCapturedSettingsPixelsAndCroppedBitmapCopy() throws Exception {
        try (Project project = new Project(); Gate gate = project.blockWorker()) {
            Bitmap cropped = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            cropped.eraseColor(Color.RED);
            AtomicInteger draws = new AtomicInteger();
            project.scenario.onActivity(activity -> {
                var views = binding(activity);
                views.textValueInput.setText("captured");
                views.appIcoBg.setBackgroundColor(Color.GREEN);
                views.appIcoImg.setBackgroundColor(Color.TRANSPARENT);
                View probe = new View(activity) {
                    @Override public void draw(Canvas canvas) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());
                        draws.incrementAndGet();
                        super.draw(canvas);
                    }
                };
                views.appIcoItems.addView(probe, 20, 20);
                probe.layout(0, 0, 20, 20);
                set(activity, "appIconBitmap", cropped);
                views.save.performClick();
                views.textValueInput.setText("later");
                views.appIcoBg.setBackgroundColor(Color.BLUE);
                cropped.eraseColor(Color.BLUE);
            });
            assertTrue(draws.get() > 0);
            gate.release.countDown();
            project.awaitSuccess(false, 5);
            assertEquals("captured", project.settings().get("text"));
            Bitmap stored = BitmapFactory.decodeFile(project.data.resolve("app_icon.png").toString());
            assertEquals(Color.RED, stored.getPixel(8, 8));
            stored.recycle();
            assertFalse("UI-owned source must not be recycled", cropped.isRecycled());
            cropped.recycle();
            Bitmap launcher = BitmapFactory.decodeFile(project.launcher().toString());
            assertEquals(Color.GREEN, launcher.getPixel(launcher.getWidth() / 2, 10));
            launcher.recycle();
        }
    }

    @Test(timeout = 30000) public void missingSourcePreservesExistingOutputsAndAllowsRetry() throws Exception {
        try (Project project = new Project()) {
            Path source = project.data.resolve("selected.png");
            Files.createDirectories(project.data);
            Files.createDirectories(project.launcher().getParent());
            byte[] previous = {42, 7};
            Files.write(project.launcher(), previous);
            project.scenario.onActivity(activity -> {
                set(activity, "iconFilePath", source.toString());
                binding(activity).save.performClick();
            });
            project.awaitFailure();
            assertArrayEquals(previous, Files.readAllBytes(project.launcher()));
            assertFalse(Files.exists(project.data.resolve("data.json")));
            Files.write(source, new byte[]{1, 2, 3});
            project.scenario.onActivity(activity -> binding(activity).save.performClick());
            project.awaitSuccess(false, 5);
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(project.data.resolve("app_icon.png")));
        }
    }

    @Test(timeout = 30000) public void invalidDestinationDoesNotOverwriteOtherDensities() throws Exception {
        try (Project project = new Project()) {
            Files.createDirectories(project.launcher().getParent());
            Files.write(project.launcher(), new byte[]{42});
            Files.createDirectories(project.icons.resolve("mipmap-xxxhdpi/ic_launcher.png"));
            project.scenario.onActivity(activity -> binding(activity).save.performClick());
            project.awaitFailure();
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(project.launcher()));
            assertFalse(Files.exists(project.data.resolve("data.json")));
        }
    }

    @Test(timeout = 30000) public void recreationKeepsTheSameExportAndReturnsItsOriginalResult() throws Exception {
        try (Project project = new Project(); Gate gate = project.blockWorker()) {
            AtomicReference<Object> original = new AtomicReference<>();
            project.scenario.onActivity(activity -> {
                binding(activity).textValueInput.setText("before rotation");
                binding(activity).save.performClick();
                original.set(get(activity, "exportModel"));
            });
            project.scenario.recreate();
            project.scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertSame(original.get(), get(activity, "exportModel"));
                assertFalse(binding(activity).save.isEnabled());
            });
            gate.release.countDown();
            project.awaitSuccess(false, 5);
            assertEquals("before rotation", project.settings().get("text"));
        }
    }

    @Test(timeout = 30000) public void destroyedActivityDoesNotPreventWorkerCompletion() throws Exception {
        try (Project project = new Project(); Gate gate = project.blockWorker()) {
            project.scenario.onActivity(activity -> binding(activity).save.performClick());
            project.scenario.close();
            gate.release.countDown();
            project.drain();
            assertEquals(5, project.pngCount());
            assertTrue(Files.exists(project.data.resolve("data.json")));
        }
    }

    @Test(timeout = 30000) public void captureFailureLeavesEditorAvailableForRetry() throws Exception {
        try (Project project = new Project()) {
            project.scenario.onActivity(activity -> {
                var views = binding(activity);
                views.appIcoCard.layout(0, 0, 0, 0);
                views.save.performClick();
                assertFalse(activity.isFinishing());
                assertTrue(views.save.isEnabled());
            });
            assertFalse(Files.exists(project.launcher()));
        }
    }

    private static class Gate implements AutoCloseable {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        @Override public void close() { release.countDown(); }
    }

    private static class Project implements AutoCloseable {
        final String id = "__icon_export_" + UUID.randomUUID();
        final Path root = new File(SketchwarePaths.getDataPath(id)).toPath();
        final Path iconRoot = new File(SketchwarePaths.getIconsPath(), id).toPath();
        final Path data = root.resolve("files/app-icon");
        final Path icons = iconRoot.resolve("temp_icons");
        final ActivityScenario<IconCreatorActivity> scenario;
        TaskHost host;
        IconCreatorActivity launchedActivity;
        int launcherWidth;

        Project() throws Exception {
            assertFalse(Files.exists(root));
            assertFalse(Files.exists(iconRoot));
            scenario = ActivityScenario.launchActivityForResult(new Intent(context(), IconCreatorActivity.class)
                    .putExtra("sc_id", id));
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                host = TaskHost.of(activity);
                launchedActivity = activity;
            });
            waitUntil(() -> {
                AtomicInteger width = new AtomicInteger();
                scenario.onActivity(activity -> width.set(binding(activity).appIcoCard.getWidth()));
                launcherWidth = width.get();
                return launcherWidth > 0;
            });
        }

        Path launcher() { return icons.resolve("mipmap-mdpi/ic_launcher.png"); }

        Gate blockWorker() throws Exception {
            Gate gate = new Gate();
            BackgroundTasks.runSerial(host, "IconExportGate", () -> {
                gate.entered.countDown();
                if (!gate.release.await(15, TimeUnit.SECONDS)) throw new AssertionError("Gate timed out");
            }, null, null);
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            return gate;
        }

        void drain() throws Exception {
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(host, "IconExportDrain", done::countDown, null, null);
            assertTrue(done.await(10, TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }

        void awaitFailure() throws Exception {
            drain();
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
            scenario.onActivity(activity -> {
                assertFalse("Failed export must not finish", activity.isFinishing());
                assertTrue("Failed export must allow retry", binding(activity).save.isEnabled());
            });
        }

        void awaitSuccess(boolean adaptive, int count) throws Exception {
            waitUntil(() -> scenario.getState() == Lifecycle.State.DESTROYED);
            var result = scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            Intent intent = result.getResultData();
            assertEquals(adaptive, intent.getBooleanExtra("isIconAdaptive", false));
            Bitmap resultBitmap = intent.getParcelableExtra("appIco");
            assertNotNull(resultBitmap);
            Bitmap output = BitmapFactory.decodeFile(launcher().toString());
            assertNotNull(output);
            assertTrue("Returned preview must be the exported image", resultBitmap.sameAs(output));
            output.recycle();
            assertEquals(count, pngCount());
            assertEquals(adaptive, settings().get("adaptive_icon"));
        }

        long pngCount() throws Exception {
            try (var entries = Files.walk(icons)) {
                return entries.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".png")).count();
            }
        }

        Map<?, ?> settings() throws Exception {
            return new Gson().fromJson(new String(Files.readAllBytes(data.resolve("data.json")),
                    StandardCharsets.UTF_8), Map.class);
        }

        @Override public void close() throws Exception {
            drain();
            // Avoid racing AndroidX's EmptyActivity cleanup with an already requested finish.
            if (launchedActivity.isFinishing() && !launchedActivity.isDestroyed()) {
                waitUntil(launchedActivity::isDestroyed);
            }
            scenario.close();
            for (Path directory : new Path[]{root, iconRoot}) {
                assertEquals(id, directory.getFileName().toString());
                if (!Files.exists(directory)) continue;
                try (var entries = Files.walk(directory)) {
                    for (Path path : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
        }
    }

    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static ActivityIconCreatorBinding binding(IconCreatorActivity activity) {
        return (ActivityIconCreatorBinding) get(activity, "binding");
    }
    private static Object get(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Object owner, String name, Object value) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(owner, value);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private interface Check { boolean get() throws Exception; }
    private static void waitUntil(Check check) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        while (!check.get()) {
            if (SystemClock.uptimeMillis() > end) fail("Timed out waiting for export");
            SystemClock.sleep(30);
        }
    }
}
