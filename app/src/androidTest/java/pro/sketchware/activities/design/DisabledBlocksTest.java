package pro.sketchware.activities.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.build.ProjectFilePaths;
import pro.sketchware.core.codegen.ActivityCodeGenerator;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;

@RunWith(AndroidJUnit4.class)
public class DisabledBlocksTest {
    @Test
    public void disabledBranchesDoNotAddPermissionsLibrariesOrImports() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            var data = ProjectDataManager.getProjectDataManager(fixture.id);
            var libraries = ProjectDataManager.getLibraryManager(fixture.id);
            var files = ProjectDataManager.getFileManager(fixture.id);
            var activity = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
            ViewBean thumbnail = new ViewBean("thumbnail", ViewBean.VIEW_TYPE_WIDGET_IMAGEVIEW);
            thumbnail.parent = "root";
            data.addView(activity.getXmlName(), thumbnail);
            String imageLoad =
                    "Glide.with(getApplicationContext()).load(Uri.parse(\"https://example.invalid/image\")).into(thumbnail);";

            BlockBean branch = new BlockBean("1", "if %b", "e", "ifElse");
            branch.parameters.add("true");
            branch.subStack1 = 2;
            branch.subStack2 = 3;
            branch.nextBlock = 4;
            branch.disabled = true;
            BlockBean image =
                    new BlockBean("2", "set %m.imageview image from %s", " ", "setImageUrl");
            image.parameters.addAll(List.of("thumbnail", "https://example.invalid/image"));
            BlockBean clipboard = new BlockBean("3", "copy %s", " ", "copyToClipboard");
            clipboard.parameters.add("branch clipboard");
            BlockBean next = new BlockBean("4", "add source %s", " ", "addSourceDirectly");
            next.parameters.add("setTitle(\"still enabled\");");
            data.putBlocks(
                    activity.getJavaName(),
                    "onCreate_initializeLogic",
                    new ArrayList<>(List.of(branch, image, clipboard, next)));

            var paths = new ProjectFilePaths(context, fixture.id);
            paths.initializeMetadata(libraries, files, data);
            assertFalse(paths.buildConfig.isGlideUsed);
            assertFalse(paths.buildConfig.hasPermission(BuildConfig.PERMISSION_INTERNET));
            String code =
                    new ActivityCodeGenerator(paths.buildConfig, activity, data)
                            .generateCode(false, fixture.id, false);
            assertFalse(code.contains("import com.bumptech.glide.Glide;"));
            assertFalse(code.contains("import android.content.ClipData;"));
            assertFalse(code.contains("https://example.invalid/image"));
            assertTrue(code.contains("setTitle(\"still enabled\");"));

            branch.disabled = false;
            paths.initializeMetadata(libraries, files, data);
            assertTrue(paths.buildConfig.isGlideUsed);
            assertTrue(paths.buildConfig.hasPermission(BuildConfig.PERMISSION_INTERNET));
            code =
                    new ActivityCodeGenerator(paths.buildConfig, activity, data)
                            .generateCode(false, fixture.id, false);
            assertTrue(code.contains("import com.bumptech.glide.Glide;"));
            assertTrue(code.contains("import android.content.ClipData;"));
            assertTrue(code, code.contains(imageLoad));
        }
    }

    @Test
    public void disabledStripesStayOutOfTheSubstackOpening() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(
                () -> {
                    BlockView block =
                            new BlockView(
                                    instrumentation.getTargetContext(), 1, "repeat", "c", "repeat");
                    block.blockColor = Color.BLUE;
                    block.setBlockSize(160, 36, false);
                    block.setSubstack1Height(80);
                    int width = block.getTotalWidth();
                    int height = block.getTotalHeight();
                    block.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                    block.layout(0, 0, width, height);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    try {
                        var canvas = new Canvas(bitmap);
                        int center = width / 2;
                        int header = block.getBlockHeight() / 2;
                        int opening = block.getBlockHeight() + block.contentHeight / 2;
                        block.draw(canvas);
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(center, opening));
                        int enabledColor = bitmap.getPixel(center, header);
                        assertNotEquals(Color.TRANSPARENT, enabledColor);

                        block.disabled = true;
                        bitmap.eraseColor(Color.TRANSPARENT);
                        block.draw(canvas);
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(center, opening));
                        assertNotEquals(enabledColor, bitmap.getPixel(center, header));
                    } finally {
                        bitmap.recycle();
                    }
                });
    }
}
