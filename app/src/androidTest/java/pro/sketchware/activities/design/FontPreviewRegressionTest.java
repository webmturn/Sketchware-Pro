package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.base.PermissionResultTestActivity;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.manage.font.AddFontCollectionActivity;
import pro.sketchware.activities.editor.manage.font.FontManagerFragment;
import pro.sketchware.activities.editor.manage.font.ImportFontFragment;
import pro.sketchware.activities.editor.manage.font.ManageFontImportActivity;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.util.Helper;

/** Uses owned cache files and isolated projects; never imports or saves a user font. */
@RunWith(AndroidJUnit4.class)
public class FontPreviewRegressionTest {
    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test(timeout = 30000)
    public void importPreviewHandlesAFileDeletedAfterOpening() throws Exception {
        try (Fonts fonts = new Fonts();
             ActivityScenario<ManageFontImportActivity> scenario = importFonts(fonts.valid)) {
            scenario.onActivity(activity -> assertValid(activity.findViewById(R.id.text_font),
                    R.string.design_manager_font_description_example_sentence));
            Files.delete(fonts.valid.toPath());
            clickFont(scenario, 0);
            scenario.onActivity(activity -> assertFailed(activity.findViewById(R.id.text_font)));
        }
    }

    @Test(timeout = 30000)
    public void importPreviewCanSwitchBetweenMissingAndValidFiles() throws Exception {
        try (Fonts fonts = new Fonts();
             ActivityScenario<ManageFontImportActivity> scenario = importFonts(fonts.missing, fonts.valid)) {
            scenario.onActivity(activity -> assertFailed(activity.findViewById(R.id.text_font)));
            clickFont(scenario, 1);
            scenario.onActivity(activity -> assertValid(activity.findViewById(R.id.text_font),
                    R.string.design_manager_font_description_example_sentence));
            clickFont(scenario, 0);
            scenario.onActivity(activity -> assertFailed(activity.findViewById(R.id.text_font)));
            fonts.assertUnchanged();
        }
    }

    @Test(timeout = 30000)
    public void collectionRenameRemainsUsableWhenItsFileIsMissing() {
        String filename = "font_preview_missing_" + UUID.randomUUID() + ".ttf";
        ProjectResourceBean bean = bean("preview_font", filename);
        Intent intent = new Intent(context(), AddFontCollectionActivity.class)
                .putExtra("edit_target", bean)
                .putParcelableArrayListExtra("fonts", new ArrayList<>(Arrays.asList(bean)));
        try (ActivityScenario<AddFontCollectionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                assertFalse(new File(activity.getFontFilePath(bean)).exists());
                assertFailed(activity.findViewById(R.id.font_preview_txt));
                ((android.widget.EditText) activity.findViewById(R.id.ed_input)).setText("another_font");
                assertFalse(activity.isFinishing());
            });
        }
        assertEquals("preview_font", bean.resName);
        assertEquals(filename, bean.resFullName);
    }

    @Test(timeout = 30000)
    public void recycledImportRowClearsOldTypefaceAfterFileDisappears() throws Exception {
        try (Fonts fonts = new Fonts();
             ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                ImportFontFragment fragment = new ImportFontFragment();
                ProjectResourceBean resource = bean("preview_font", fonts.valid.getPath());
                resource.isNew = true;
                fragment.projectResourceBeans = new ArrayList<>(Arrays.asList(resource));
                ImportFontFragment.FontAdapter adapter = fragment.new FontAdapter();
                var holder = adapter.onCreateViewHolder(new LinearLayout(activity), 0);
                adapter.onBindViewHolder(holder, 0);
                assertValid(holder.binding.tvFontPreview, R.string.common_word_preview);
                resource.resFullName = fonts.missing.getPath();
                adapter.onBindViewHolder(holder, 0);
                assertFailed(holder.binding.tvFontPreview);
            });
            fonts.assertUnchanged();
        }
    }

    @Test(timeout = 30000)
    public void recycledImportRowRestoresItsSampleAfterAnInvalidFont() throws Exception {
        try (Fonts fonts = new Fonts();
             ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                ImportFontFragment fragment = new ImportFontFragment();
                ProjectResourceBean resource = bean("preview_font", fonts.corrupt.getPath());
                resource.isNew = true;
                fragment.projectResourceBeans = new ArrayList<>(Arrays.asList(resource));
                ImportFontFragment.FontAdapter adapter = fragment.new FontAdapter();
                var holder = adapter.onCreateViewHolder(new LinearLayout(activity), 0);
                for (File invalid : new File[]{fonts.empty, fonts.corrupt, fonts.directory}) {
                    resource.resFullName = invalid.getPath();
                    adapter.onBindViewHolder(holder, 0);
                    assertFailed(holder.binding.tvFontPreview);
                }
                resource.resFullName = fonts.valid.getPath();
                adapter.onBindViewHolder(holder, 0);
                assertValid(holder.binding.tvFontPreview, R.string.common_word_preview);
            });
            fonts.assertUnchanged();
        }
    }

    @Test(timeout = 30000)
    public void collectionRowExplainsAMissingFile() {
        try (ActivityScenario<PermissionResultTestActivity> scenario = ActivityScenario.launch(PermissionResultTestActivity.class)) {
            scenario.onActivity(activity -> {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                FontManagerFragment fragment = new FontManagerFragment();
                fragment.projectResourceBeans = new ArrayList<>(Arrays.asList(bean("preview_font",
                        "font_preview_missing_" + UUID.randomUUID() + ".ttf")));
                FontManagerFragment.fontAdapter adapter = fragment.new fontAdapter();
                var holder = adapter.onCreateViewHolder(new LinearLayout(activity), 0);
                adapter.onBindViewHolder(holder, 0);
                assertFailed(holder.binding.tvFontPreview);
            });
        }
    }

    @Test(timeout = 30000)
    public void logicFontPreviewPreservesDefaultAndValidFontsAndExplainsFailures() throws Exception {
        try (Fonts fonts = new Fonts(); ProjectEditorPersistenceTest.Fixture project = new ProjectEditorPersistenceTest.Fixture()) {
            project.seed(false, false);
            ProjectDataManager.reloadProjectFromData(project.id);
            var before = project.snapshot();
            Intent intent = new Intent(context(), LogicEditorActivity.class).putExtra("sc_id", project.id)
                    .putExtra("id", "onCreate").putExtra("event", "initializeLogic")
                    .putExtra("event_text", "Font preview test")
                    .putExtra("project_file", new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
            Method preview = LogicEditorActivity.class.getDeclaredMethod("getFontPreview", String.class);
            preview.setAccessible(true);
            try (ActivityScenario<LogicEditorActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    var resources = ProjectDataManager.getResourceManager(project.id);
                    resources.fontDirPath = fonts.root.toString();
                    for (File file : new File[]{fonts.valid, fonts.empty, fonts.corrupt, fonts.missing, fonts.directory}) {
                        resources.fonts.add(bean(file.getName(), file.getName()));
                    }
                    try {
                        TextView defaultPreview = preview(preview, activity, "default_font");
                        assertEquals(Typeface.DEFAULT, defaultPreview.getTypeface());
                        assertEquals(Helper.getResString(R.string.font_preview), defaultPreview.getText().toString());
                        assertValid(preview(preview, activity, fonts.valid.getName()), R.string.font_preview);
                        for (String name : new String[]{fonts.empty.getName(), fonts.corrupt.getName(),
                                fonts.missing.getName(), fonts.directory.getName(), "unknown_font"}) {
                            assertFailed(preview(preview, activity, name));
                        }
                    } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                });
                project.assertUnchanged(before);
                fonts.assertUnchanged();
            }
        }
    }

    private static TextView preview(Method method, LogicEditorActivity activity, String name) throws ReflectiveOperationException {
        LinearLayout row = (LinearLayout) method.invoke(activity, name);
        return (TextView) row.getChildAt(1);
    }

    private static ActivityScenario<ManageFontImportActivity> importFonts(File... files) {
        ArrayList<ProjectResourceBean> selected = new ArrayList<>();
        for (int i = 0; i < files.length; i++) selected.add(bean("preview_font_" + i, files[i].getPath()));
        Intent intent = new Intent(context(), ManageFontImportActivity.class)
                .putParcelableArrayListExtra("project_fonts", new ArrayList<ProjectResourceBean>())
                .putParcelableArrayListExtra("selected_collections", selected);
        ActivityScenario<ManageFontImportActivity> scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        return scenario;
    }

    private static void clickFont(ActivityScenario<ManageFontImportActivity> scenario, int position) throws Exception {
        Thread.sleep(150);
        ProjectEditorPersistenceTest.waitUntil(() -> {
            boolean[] clicked = {false};
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.recycler_list);
                list.scrollToPosition(position);
                RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(position);
                if (holder != null) clicked[0] = holder.itemView.findViewById(R.id.img).performClick();
            });
            return clicked[0];
        });
    }

    private static ProjectResourceBean bean(String name, String path) {
        return new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE, name, path);
    }

    private static void assertFailed(TextView preview) {
        assertEquals("Unavailable files must clear any previously loaded font", Typeface.DEFAULT, preview.getTypeface());
        assertEquals(Helper.getResString(R.string.font_load_failed), preview.getText().toString());
    }

    private static void assertValid(TextView preview, int sample) {
        assertNotEquals(Typeface.DEFAULT, preview.getTypeface());
        assertEquals(Helper.getResString(sample), preview.getText().toString());
    }

    private static final class Fonts implements AutoCloseable {
        final Path root = Files.createTempDirectory(context().getCacheDir().toPath(), "font_preview_");
        final File valid = root.resolve("valid.ttf").toFile();
        final File empty = root.resolve("empty.ttf").toFile();
        final File corrupt = root.resolve("corrupt.ttf").toFile();
        final File missing = root.resolve("missing.ttf").toFile();
        final File directory = root.resolve("directory.ttf").toFile();
        final byte[] validBytes;

        Fonts() throws Exception {
            try (var source = context().getAssets().open("fonts/droid_sans_mono.ttf")) {
                Files.copy(source, valid.toPath());
            }
            validBytes = Files.readAllBytes(valid.toPath());
            Files.createFile(empty.toPath());
            Files.write(corrupt.toPath(), new byte[]{0, 1, 2, 3});
            assertTrue(directory.mkdir());
        }

        void assertUnchanged() throws Exception {
            assertArrayEquals(validBytes, Files.readAllBytes(valid.toPath()));
            assertEquals(0, empty.length());
            assertArrayEquals(new byte[]{0, 1, 2, 3}, Files.readAllBytes(corrupt.toPath()));
            assertFalse(missing.exists());
            assertTrue(directory.isDirectory());
        }

        @Override public void close() throws Exception {
            assertEquals(context().getCacheDir().getCanonicalFile(), root.toFile().getCanonicalFile().getParentFile());
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) {
                    assertTrue(path.startsWith(root));
                    Files.delete(path);
                }
            }
        }
    }
}
