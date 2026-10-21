package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import pro.sketchware.R;
import pro.sketchware.activities.editor.view.ViewPane;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.io.EncryptedFileUtil;

/** Real bitmap decoding and editor persistence, using only an owned disposable project. */
@RunWith(AndroidJUnit4.class)
public class BitmapPreviewPreservationTest {
    private static final String RESOURCE_NAME = "preview_bitmap";
    private static final String IMAGE_ID = "bitmap_image";
    private static final byte[] CORRUPT = "not a PNG image".getBytes(StandardCharsets.UTF_8);

    @Test(timeout = 30000)
    public void corruptBitmapHasAPlaceholderOnFirstLoad() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed()) {
            Files.write(imageFile(fixture).toPath(), CORRUPT);
            try (ActivityScenario<DesignActivity> scenario = launch(fixture)) {
                check(scenario, activity -> assertPlaceholder(image(activity)));
                assertArrayEquals(CORRUPT, Files.readAllBytes(imageFile(fixture).toPath()));
                assertReference(fixture);
            }
        }
    }

    @Test(timeout = 30000)
    public void failedDecodeReplacesTheOldImageAndRepairReloadsTheSamePath() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed();
             ActivityScenario<DesignActivity> scenario = launch(fixture)) {
            check(scenario, activity -> assertColor(image(activity), Color.RED));
            Files.write(imageFile(fixture).toPath(), CORRUPT);
            check(scenario, activity -> {
                refresh(activity, fixture);
                assertPlaceholder(image(activity));
            });
            assertArrayEquals(CORRUPT, Files.readAllBytes(imageFile(fixture).toPath()));
            assertReference(fixture);
            writePng(imageFile(fixture), Color.BLUE);
            check(scenario, activity -> {
                refresh(activity, fixture);
                assertColor(image(activity), Color.BLUE);
            });
            assertReference(fixture);
        }
    }

    @Test(timeout = 30000)
    public void directoryInPlaceOfAnImageDoesNotRetainTheOldPreview() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed();
             ActivityScenario<DesignActivity> scenario = launch(fixture)) {
            check(scenario, activity -> assertColor(image(activity), Color.RED));
            File file = imageFile(fixture);
            Files.delete(file.toPath());
            assertTrue(file.mkdir());
            check(scenario, activity -> {
                refresh(activity, fixture);
                assertPlaceholder(image(activity));
            });
            assertTrue("Preview must not delete the source entry", file.isDirectory());
            assertReference(fixture);
        }
    }

    @Test(timeout = 30000)
    public void replacingAValidBitmapAtTheSamePathRefreshesItsPixels() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed();
             ActivityScenario<DesignActivity> scenario = launch(fixture)) {
            check(scenario, activity -> assertColor(image(activity), Color.RED));
            writePng(imageFile(fixture), Color.BLUE);
            byte[] before = Files.readAllBytes(imageFile(fixture).toPath());
            check(scenario, activity -> {
                refresh(activity, fixture);
                assertColor(image(activity), Color.BLUE);
            });
            assertArrayEquals(before, Files.readAllBytes(imageFile(fixture).toPath()));
            assertReference(fixture);
        }
    }

    @Test(timeout = 30000)
    public void emptyAndMissingImagesKeepTheirReferenceAndShowAPlaceholder() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed();
             ActivityScenario<DesignActivity> scenario = launch(fixture)) {
            for (boolean missing : new boolean[]{false, true}) {
                writePng(imageFile(fixture), Color.RED);
                check(scenario, activity -> {
                    refresh(activity, fixture);
                    assertColor(image(activity), Color.RED);
                });
                if (missing) Files.delete(imageFile(fixture).toPath());
                else Files.write(imageFile(fixture).toPath(), new byte[0]);
                check(scenario, activity -> {
                    refresh(activity, fixture);
                    assertPlaceholder(image(activity));
                });
                if (missing) assertFalse(imageFile(fixture).exists());
                else assertEquals(0, imageFile(fixture).length());
                assertReference(fixture);
            }
        }
    }

    @Test(timeout = 30000)
    public void failedBitmapPreviewPreservesSourceAndReferenceAcrossSaveAndRecreation() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = seed();
             ActivityScenario<DesignActivity> scenario = launch(fixture)) {
            Files.write(imageFile(fixture).toPath(), CORRUPT);
            check(scenario, activity -> {
                refresh(activity, fixture);
                assertPlaceholder(image(activity));
                ProjectDataManager.getProjectDataManager(fixture.id)
                        .addVariable(ProjectFileBean.DEFAULT_JAVA_NAME, 2, "bitmap_preview_saved");
            });
            ProjectEditorPersistenceTest.clickSave();
            ProjectEditorPersistenceTest.waitUntil(() -> fixture.read(fixture.data, "logic")
                    .contains("bitmap_preview_saved"));
            assertTrue(fixture.read(fixture.data, "view").contains("\"resName\":\"" + RESOURCE_NAME + "\""));
            assertTrue(fixture.read(fixture.data, "resource").contains("\"resName\":\"" + RESOURCE_NAME + "\""));
            assertArrayEquals(CORRUPT, Files.readAllBytes(imageFile(fixture).toPath()));
            scenario.recreate();
            ready(scenario);
            check(scenario, activity -> assertPlaceholder(image(activity)));
            assertReference(fixture);
            assertArrayEquals(CORRUPT, Files.readAllBytes(imageFile(fixture).toPath()));
        }
    }

    private static ProjectEditorPersistenceTest.Fixture seed() throws Exception {
        ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        try {
            fixture.seed(false, false);
            ViewBean image = new ViewBean(IMAGE_ID, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
            image.parent = "root";
            image.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
            image.image.resName = RESOURCE_NAME;
            ProjectResourceBean resource = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                    RESOURCE_NAME, RESOURCE_NAME + ".png");
            EncryptedFileUtil io = new EncryptedFileUtil();
            Files.write(new File(fixture.data, "view").toPath(), io.encryptString(
                    "@main.xml\n" + ProjectDataStore.gson.toJson(image) + "\n", true));
            Files.write(new File(fixture.data, "resource").toPath(), io.encryptString(
                    "@images\n" + ProjectDataStore.gson.toJson(resource) + "\n@sounds\n@fonts\n", true));
            writePng(imageFile(fixture), Color.RED);
            return fixture;
        } catch (Exception | AssertionError failure) {
            fixture.close();
            throw failure;
        }
    }

    private static File imageFile(ProjectEditorPersistenceTest.Fixture fixture) {
        return new File(new File(SketchwarePaths.getImagesPath(), fixture.id), RESOURCE_NAME + ".png");
    }

    private static void writePng(File file, int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.eraseColor(color);
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
    }

    private static ActivityScenario<DesignActivity> launch(ProjectEditorPersistenceTest.Fixture fixture) throws Exception {
        ActivityScenario<DesignActivity> scenario = fixture.launch();
        try {
            ready(scenario);
            return scenario;
        } catch (Exception | AssertionError failure) {
            scenario.close();
            throw failure;
        }
    }

    private static void ready(ActivityScenario<DesignActivity> scenario) throws Exception {
        check(scenario, activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        ProjectEditorPersistenceTest.waitForEditor(scenario);
    }

    private static ViewBean imageBean(ProjectEditorPersistenceTest.Fixture fixture) {
        return ProjectDataManager.getProjectDataManager(fixture.id).getViews(ProjectFileBean.DEFAULT_XML_NAME)
                .stream().filter(bean -> IMAGE_ID.equals(bean.id)).findFirst().orElseThrow();
    }

    private static void assertReference(ProjectEditorPersistenceTest.Fixture fixture) {
        assertEquals(RESOURCE_NAME, imageBean(fixture).image.resName);
    }

    private static void refresh(DesignActivity activity, ProjectEditorPersistenceTest.Fixture fixture) {
        ViewPane pane = findPane(activity.getWindow().getDecorView());
        assertNotNull(pane);
        pane.updateView(imageBean(fixture));
    }

    private static ImageView image(DesignActivity activity) {
        ViewPane pane = findPane(activity.getWindow().getDecorView());
        assertNotNull(pane);
        ImageView image = (ImageView) pane.findViewWithTag(IMAGE_ID);
        assertNotNull(image);
        return image;
    }

    private static ViewPane findPane(View view) {
        if (view instanceof ViewPane) return (ViewPane) view;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                ViewPane result = findPane(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void assertColor(ImageView image, int color) {
        Bitmap rendered = render(image.getDrawable());
        try {
            assertEquals(color, rendered.getPixel(16, 16));
        } finally {
            rendered.recycle();
        }
    }

    private static void assertPlaceholder(ImageView image) {
        Bitmap actual = render(image.getDrawable());
        Bitmap expected = render(AppCompatResources.getDrawable(image.getContext(), R.drawable.default_image));
        try {
            assertTrue("A failed bitmap decode must display the placeholder rather than blank or stale pixels",
                    expected.sameAs(actual));
        } finally {
            actual.recycle();
            expected.recycle();
        }
    }

    private static Bitmap render(Drawable drawable) {
        assertNotNull("A failed bitmap decode must not leave an empty image widget", drawable);
        Bitmap result = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        android.graphics.Rect bounds = drawable.copyBounds();
        try {
            drawable.setBounds(0, 0, 32, 32);
            drawable.draw(new Canvas(result));
            return result;
        } finally {
            drawable.setBounds(bounds);
        }
    }

    private interface Check { void run(DesignActivity activity) throws Exception; }

    private static void check(ActivityScenario<DesignActivity> scenario, Check check) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        scenario.onActivity(activity -> {
            try { check.run(activity); }
            catch (Throwable error) { failure.set(error); }
        });
        Throwable error = failure.get();
        if (error instanceof Exception exception) throw exception;
        if (error != null) throw new AssertionError(error);
    }
}
