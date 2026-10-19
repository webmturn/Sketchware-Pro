package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import pro.sketchware.activities.editor.view.ItemView;
import pro.sketchware.activities.editor.view.ViewPane;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.io.EncryptedFileUtil;

/** Preview failures must not become destructive edits to the project's resources or model. */
@RunWith(AndroidJUnit4.class)
public class VectorPreviewPreservationTest {
    private static final String RESOURCE_NAME = "preview_vector";
    private static final String IMAGE_ID = "vector_image";
    private static final String VALID_VECTOR = """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp" android:height="24dp"
                android:viewportWidth="24" android:viewportHeight="24">
                <path android:fillColor="#FFFF0000" android:pathData="M0,0 L24,0 L24,24 L0,24 Z"/>
            </vector>
            """;

    @Test(timeout = 30000)
    public void malformedVectorSurvivesPreviewSaveAndRecreation() throws Exception {
        verifyFailedPreview("<vector broken XML");
    }

    @Test(timeout = 30000)
    public void emptyVectorSurvivesPreviewSaveAndRecreation() throws Exception {
        verifyFailedPreview("");
    }

    @Test(timeout = 30000)
    public void missingVectorKeepsReferenceWithoutCreatingAFile() throws Exception {
        verifyFailedPreview(null);
    }

    @Test(timeout = 30000)
    public void validVectorStillRendersItsContent() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            seed(fixture, VALID_VECTOR);
            File vector = vectorFile(fixture);
            byte[] before = Files.readAllBytes(vector.toPath());
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                scenario.onActivity(activity -> assertRedImage(activity));
                assertReference(fixture);
                assertArrayEquals(before, Files.readAllBytes(vector.toPath()));
            }
        }
    }

    @Test(timeout = 30000)
    public void repairedVectorCanReloadUsingTheOriginalReference() throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            seed(fixture, "<vector broken XML");
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                assertReference(fixture);
                writeVector(fixture, VALID_VECTOR);
                scenario.onActivity(activity -> {
                    ViewPane pane = findPane(activity.getWindow().getDecorView());
                    assertNotNull(pane);
                    pane.updateView(imageBean(fixture));
                    assertRedImage(activity);
                });
                assertReference(fixture);
                assertEquals(VALID_VECTOR, new String(Files.readAllBytes(vectorFile(fixture).toPath()),
                        StandardCharsets.UTF_8));
            }
        }
    }

    private static void verifyFailedPreview(String content) throws Exception {
        try (ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture()) {
            seed(fixture, content);
            try (ActivityScenario<DesignActivity> scenario = fixture.launch()) {
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                assertSource(fixture, content);
                assertReference(fixture);
                scenario.onActivity(activity -> {
                    ImageView image = findImage(activity.getWindow().getDecorView());
                    assertNotNull("The editor must still show the image widget", image);
                    assertNotNull("A failed preview must have a placeholder", image.getDrawable());
                    ProjectDataManager.getProjectDataManager(fixture.id)
                            .addVariable(ProjectFileBean.DEFAULT_JAVA_NAME, 2, "vector_preview_saved");
                });
                ProjectEditorPersistenceTest.clickSave();
                ProjectEditorPersistenceTest.waitUntil(() -> fixture.read(fixture.data, "logic")
                        .contains("vector_preview_saved"));
                String savedViews = fixture.read(fixture.data, "view");
                assertTrue(savedViews, savedViews.contains("\"resName\":\"" + RESOURCE_NAME + "\""));
                assertSource(fixture, content);
                scenario.recreate();
                ProjectEditorPersistenceTest.waitForEditor(scenario);
                assertReference(fixture);
                assertSource(fixture, content);
            }
        }
    }

    private static void seed(ProjectEditorPersistenceTest.Fixture fixture, String vector) throws Exception {
        fixture.seed(false, false);
        ViewBean image = new ViewBean(IMAGE_ID, ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
        image.parent = "root";
        image.parentType = ViewBean.VIEW_TYPE_LAYOUT_LINEAR;
        image.image.resName = RESOURCE_NAME;
        String views = "@main.xml\n" + ProjectDataStore.gson.toJson(image) + "\n";
        Files.write(new File(fixture.data, "view").toPath(),
                new EncryptedFileUtil().encryptString(views, true));
        if (vector != null) writeVector(fixture, vector);
    }

    private static File vectorFile(ProjectEditorPersistenceTest.Fixture fixture) {
        return new File(SketchwarePaths.getProjectResourceDrawableXmlFilePath(fixture.id, RESOURCE_NAME));
    }

    private static void writeVector(ProjectEditorPersistenceTest.Fixture fixture, String content) throws Exception {
        File vector = vectorFile(fixture);
        Files.createDirectories(vector.toPath().getParent());
        Files.write(vector.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertSource(ProjectEditorPersistenceTest.Fixture fixture, String content) throws Exception {
        File vector = vectorFile(fixture);
        if (content == null) {
            assertFalse("Preview must not create a missing source file", vector.exists());
        } else {
            assertTrue("Preview must preserve the original XML file", vector.isFile());
            assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(vector.toPath()));
        }
    }

    private static ViewBean imageBean(ProjectEditorPersistenceTest.Fixture fixture) {
        return ProjectDataManager.getProjectDataManager(fixture.id).getViews(ProjectFileBean.DEFAULT_XML_NAME)
                .stream().filter(bean -> IMAGE_ID.equals(bean.id)).findFirst().orElseThrow();
    }

    private static void assertReference(ProjectEditorPersistenceTest.Fixture fixture) {
        assertEquals("A display fallback must not replace the saved image reference",
                RESOURCE_NAME, imageBean(fixture).image.resName);
    }

    private static void assertRedImage(DesignActivity activity) {
        ImageView image = findImage(activity.getWindow().getDecorView());
        assertNotNull(image);
        Drawable drawable = image.getDrawable();
        assertNotNull(drawable);
        Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        try {
            drawable.setBounds(0, 0, 24, 24);
            drawable.draw(new Canvas(bitmap));
            assertEquals("The vector must render rather than retain its failure placeholder",
                    Color.RED, bitmap.getPixel(12, 12));
        } finally {
            bitmap.recycle();
        }
    }

    private static ImageView findImage(View view) {
        if (view instanceof ImageView && view instanceof ItemView item
                && item.getBean() != null && IMAGE_ID.equals(item.getBean().id)) return (ImageView) view;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                ImageView result = findImage(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
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
}
