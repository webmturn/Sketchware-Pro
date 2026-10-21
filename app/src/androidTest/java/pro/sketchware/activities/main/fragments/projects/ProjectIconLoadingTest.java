package pro.sketchware.activities.main.fragments.projects;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.FileLoader;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.signature.ObjectKey;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
public class ProjectIconLoadingTest {
    @Test(timeout = 30000)
    public void decodeRunsOffMainWhileTheUiRespondsAndUsesTheViewSize() throws Exception {
        try (Scope scope = new Scope()) {
            Gate gate = scope.gate(0);
            scope.bind(0);
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            AtomicInteger pulse = new AtomicInteger();
            scope.scenario.onActivity(activity -> scope.holder.itemView.post(pulse::incrementAndGet));
            waitUntil(() -> pulse.get() > 0);
            gate.release.countDown();
            scope.awaitColor(Color.RED);
            scope.scenario.onActivity(activity -> {
                Bitmap bitmap = ((BitmapDrawable) scope.holder.binding.imgIcon.getDrawable()).getBitmap();
                assertTrue(bitmap.getWidth() <= scope.holder.binding.imgIcon.getWidth());
                assertTrue(bitmap.getHeight() <= scope.holder.binding.imgIcon.getHeight());
                assertTrue(bitmap.getWidth() < 1024);
            });
        }
    }

    @Test(timeout = 30000)
    public void lateOldLoadsCannotOverwriteTheNextProjectIcon() throws Exception {
        try (Scope scope = new Scope()) {
            Gate gate = scope.gate(0);
            scope.bind(0);
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            scope.bind(1);
            scope.awaitColor(Color.GREEN);
            assertTrue(gate.cancelled.await(5, TimeUnit.SECONDS));
            gate.release.countDown();
            assertTrue(gate.returned.await(5, TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scope.assertColor(Color.GREEN);
        }
    }

    @Test(timeout = 30000)
    public void switchingToTheDefaultIconCancelsTheOldRequest() throws Exception {
        try (Scope scope = new Scope()) {
            Gate gate = scope.gate(0);
            scope.bind(0);
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            scope.bind(2);
            assertTrue(gate.cancelled.await(5, TimeUnit.SECONDS));
            gate.release.countDown();
            assertTrue(gate.returned.await(5, TimeUnit.SECONDS));
            scope.assertDefault();
            scope.scenario.onActivity(activity -> assertNull(scope.request()));
        }
    }

    @Test(timeout = 30000)
    public void missingAndCorruptIconsFallBackToTheDefault() throws Exception {
        try (Scope scope = new Scope()) {
            scope.bind(0);
            scope.awaitColor(Color.RED);
            for (int index : new int[]{3, 4}) {
                scope.bind(index);
                waitUntil(() -> {
                    boolean[] failed = {false};
                    scope.scenario.onActivity(activity -> failed[0] = scope.request() == null || !scope.request().isRunning());
                    return failed[0];
                });
                scope.assertDefault();
            }
        }
    }

    @Test(timeout = 30000)
    public void replacingTheSameFileDoesNotReuseOldPixelsEvenWithTheSameTimestamp() throws Exception {
        try (Scope scope = new Scope()) {
            scope.bind(0);
            scope.awaitColor(Color.RED);
            long timestamp = scope.icon(0).lastModified();
            writeIcon(scope.icon(0), Color.GREEN);
            assertTrue(scope.icon(0).setLastModified(timestamp));
            scope.bind(0);
            scope.awaitColor(Color.GREEN);
        }
    }

    @Test(timeout = 30000)
    public void recyclingCancelsAndClearsTheImageRequest() throws Exception {
        try (Scope scope = new Scope()) {
            Gate gate = scope.gate(0);
            scope.bind(0);
            assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
            scope.scenario.onActivity(activity -> scope.adapter.onViewRecycled(scope.holder));
            assertTrue(gate.cancelled.await(5, TimeUnit.SECONDS));
            gate.release.countDown();
            assertTrue(gate.returned.await(5, TimeUnit.SECONDS));
            scope.assertDefault();
            scope.scenario.onActivity(activity -> assertNull(scope.request()));
        }
    }

    @Test(timeout = 30000)
    public void bindingAfterActivityDestructionDoesNotRestartLoading() throws Exception {
        try (Scope scope = new Scope()) {
            scope.scenario.close();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                scope.adapter.onBindViewHolder(scope.holder, 0);
                assertTrue(scope.isDefault());
                assertNull(scope.request());
            });
            assertEquals(0, scope.loads.get());
        }
    }

    public static class TestProjectsFragment extends ProjectsFragment {
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
            return new FrameLayout(requireContext());
        }
        @Override public void onViewCreated(View view, Bundle state) {
            try {
                Field executor = ProjectsFragment.class.getDeclaredField("executorService");
                executor.setAccessible(true); executor.set(this, Executors.newSingleThreadExecutor());
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        }
    }

    private static class Gate {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1), returned = new CountDownLatch(1);
    }

    private static class Scope implements AutoCloseable {
        final String prefix = "__icon_test_" + UUID.randomUUID() + "_";
        final File iconsRoot = new File(SketchwarePaths.getIconsPath());
        final List<File> owned = new ArrayList<>();
        final Map<String, Gate> gates = new ConcurrentHashMap<>();
        final AtomicInteger loads = new AtomicInteger();
        final ActivityScenario<PermissionResultTestActivity> scenario;
        ProjectsAdapter adapter;
        ProjectsAdapter.ProjectViewHolder holder;
        PermissionResultTestActivity activity;

        Scope() throws Exception {
            List<HashMap<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                File dir = new File(iconsRoot, prefix + i);
                assertFalse("Never overwrite a user's icon", dir.exists());
                assertTrue(dir.mkdirs()); owned.add(dir);
                HashMap<String, Object> row = new HashMap<>();
                row.put("sc_id", prefix + i); row.put("custom_icon", i != 2);
                row.put("sc_ver_code", "1"); row.put("sc_ver_name", "1.0"); row.put("sketchware_ver", 61);
                row.put("my_ws_name", "Icon test"); row.put("my_app_name", "Fixture"); row.put("my_sc_pkg_name", "test.icons");
                rows.add(row);
            }
            writeIcon(icon(0), Color.RED); writeIcon(icon(1), Color.GREEN);
            Files.write(icon(4).toPath(), new byte[]{1, 2, 3});
            Glide.get(context()).getRegistry().prepend(File.class, InputStream.class, new ModelLoaderFactory<File, InputStream>() {
                @Override public ModelLoader<File, InputStream> build(MultiModelLoaderFactory unused) {
                    return new ModelLoader<>() {
                        @Override public boolean handles(File file) {
                            return file.getParentFile() != null && file.getParentFile().getName().startsWith(prefix);
                        }
                        @Override public LoadData<InputStream> buildLoadData(File file, int width, int height, Options options) {
                            return new LoadData<>(new ObjectKey(file), fetcher(file));
                        }
                    };
                }
                @Override public void teardown() {}
            });
            scenario = ActivityScenario.launch(PermissionResultTestActivity.class);
            scenario.onActivity(current -> {
                activity = current;
                current.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                TestProjectsFragment fragment = new TestProjectsFragment();
                current.getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment, "icon_test").commitNow();
                adapter = new ProjectsAdapter(fragment, rows);
                adapter.filterData("");
                FrameLayout parent = (FrameLayout) fragment.requireView();
                holder = adapter.onCreateViewHolder(parent, 0);
                parent.addView(holder.itemView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            });
            waitUntil(() -> {
                boolean[] measured = {false};
                scenario.onActivity(current -> measured[0] = holder.binding.imgIcon.getWidth() > 0);
                return measured[0];
            });
        }

        File icon(int index) { return new File(iconsRoot, prefix + index + "/icon.png"); }
        Gate gate(int index) { Gate gate = new Gate(); gates.put(icon(index).getPath(), gate); return gate; }
        void bind(int index) { scenario.onActivity(current -> adapter.onBindViewHolder(holder, index)); }
        Request request() { return new DrawableImageViewTarget(holder.binding.imgIcon).getRequest(); }

        void awaitColor(int color) throws Exception {
            waitUntil(() -> {
                boolean[] found = {false};
                scenario.onActivity(current -> found[0] = hasColor(color));
                return found[0];
            });
        }
        void assertColor(int color) { scenario.onActivity(current -> assertTrue(hasColor(color))); }
        boolean hasColor(int color) {
            Drawable drawable = holder.binding.imgIcon.getDrawable();
            if (!(drawable instanceof BitmapDrawable image)) return false;
            Bitmap bitmap = image.getBitmap();
            return bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2) == color;
        }
        boolean isDefault() {
            Bitmap expected = draw(AppCompatResources.getDrawable(activity, R.drawable.default_icon));
            Bitmap actual = draw(holder.binding.imgIcon.getDrawable());
            boolean same = expected.sameAs(actual);
            expected.recycle(); actual.recycle(); return same;
        }
        void assertDefault() {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(current -> assertTrue("The row must show the default icon", isDefault()));
        }

        DataFetcher<InputStream> fetcher(File file) {
            return new DataFetcher<>() {
                InputStream stream;
                volatile boolean cancelled;
                final Gate gate = gates.get(file.getPath());
                @Override public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
                    loads.incrementAndGet();
                    assertNotSame("Glide must read icon data off main", Looper.getMainLooper(), Looper.myLooper());
                    try {
                        if (gate != null) {
                            gate.entered.countDown();
                            if (!gate.release.await(20, TimeUnit.SECONDS)) throw new IOException("Icon gate timed out");
                        }
                        stream = new FileInputStream(file);
                        // Deliberately allow a late callback after cancellation to exercise target ownership.
                        callback.onDataReady(stream);
                    } catch (Exception e) { callback.onLoadFailed(e); }
                    finally {
                        if (cancelled) cleanup();
                        if (gate != null) gate.returned.countDown();
                    }
                }
                @Override public void cleanup() { if (stream != null) try { stream.close(); } catch (IOException ignored) {} }
                @Override public void cancel() { cancelled = true; if (gate != null) gate.cancelled.countDown(); }
                @Override public Class<InputStream> getDataClass() { return InputStream.class; }
                @Override public DataSource getDataSource() { return DataSource.LOCAL; }
            };
        }

        @Override public void close() throws Exception {
            for (Gate gate : gates.values()) gate.release.countDown();
            try {
                scenario.close();
                for (Gate gate : gates.values()) if (gate.entered.getCount() == 0) assertTrue(gate.returned.await(10, TimeUnit.SECONDS));
            } finally {
                Glide.get(context()).getRegistry().replace(File.class, InputStream.class, new FileLoader.StreamFactory());
                for (File dir : owned) {
                    assertTrue(dir.getName().startsWith(prefix));
                    assertEquals(iconsRoot.getCanonicalFile(), dir.getCanonicalFile().getParentFile());
                    Path root = dir.toPath();
                    try (Stream<Path> paths = Files.walk(root)) {
                        for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                            assertTrue(path.startsWith(root)); Files.delete(path);
                        }
                    }
                }
            }
        }
    }

    private static Bitmap draw(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        if (drawable != null) {
            Rect bounds = new Rect(drawable.getBounds());
            drawable.setBounds(0, 0, 64, 64); drawable.draw(new Canvas(bitmap)); drawable.setBounds(bounds);
        }
        return bitmap;
    }
    private static void writeIcon(File file, int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        try (var output = Files.newOutputStream(file.toPath())) {
            bitmap.eraseColor(color); assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally { bitmap.recycle(); }
    }
    private interface Condition { boolean ready() throws Exception; }
    private static void waitUntil(Condition condition) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition.ready()) return;
            Thread.sleep(30);
        }
        fail("Project icon did not reach its expected state");
    }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
