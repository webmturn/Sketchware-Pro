package pro.sketchware.activities.editor.manage.font;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import pro.sketchware.R;
import pro.sketchware.beans.ProjectResourceBean;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
public class FontNameInputTest {
    @Test(timeout = 30000)
    public void renamingWithoutAFontListIsCanceled() {
        Intent intent = new Intent(targetContext(), AddFontCollectionActivity.class);
        intent.putExtra("edit_target", new ProjectResourceBean(
                ProjectResourceBean.PROJECT_RES_TYPE_FILE, "original_font", "unused.ttf"));
        assertRenameIsCanceled(intent);
    }

    @Test(timeout = 30000)
    public void renamingWithoutATargetIsCanceled() {
        Intent intent = new Intent(targetContext(), AddFontCollectionActivity.class);
        intent.putParcelableArrayListExtra("fonts", new ArrayList<ProjectResourceBean>());
        assertRenameIsCanceled(intent);
    }

    @Test(timeout = 30000)
    public void launchingRenameWithoutExtrasIsCanceled() {
        assertRenameIsCanceled(new Intent(targetContext(), AddFontCollectionActivity.class));
    }

    @Test(timeout = 30000)
    public void addingAFontRemainsResponsiveAfterInvalidFirstCharacters() {
        Intent intent = new Intent(targetContext(), AddFontActivity.class);
        intent.putStringArrayListExtra("font_names", new ArrayList<>());
        try (ActivityScenario<AddFontActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertInvalidThenCorrected(activity, "font_a");
                activity.findViewById(R.id.common_dialog_cancel_button).performClick();
            });
        }
    }

    @Test(timeout = 30000)
    public void renamingACollectionFontRemainsResponsiveAndAllowsItsOriginalName() throws Exception {
        verifyCollectionRename(false);
    }

    @Test(timeout = 30000)
    public void recreatingACollectionFontRenameKeepsItsInputWorking() throws Exception {
        verifyCollectionRename(true);
    }

    private void verifyCollectionRename(boolean recreate) throws Exception {
        // Only the preview needs a font file. No collection entry is created or saved.
        File dataDirectory = new File(SketchwarePaths.getCollectionPath(), "font/data");
        ArrayList<File> createdDirectories = new ArrayList<>();
        File fixture = null;
        try {
            createMissingDirectories(dataDirectory, createdDirectories);
            fixture = Files.createTempFile(dataDirectory.toPath(), "font_validation_", ".ttf").toFile();
            Files.copy(systemFont().toPath(), fixture.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ProjectResourceBean font = new ProjectResourceBean(
                    ProjectResourceBean.PROJECT_RES_TYPE_FILE, "original_font", fixture.getName());
            ArrayList<ProjectResourceBean> fonts = new ArrayList<>();
            fonts.add(font);
            Intent intent = new Intent(targetContext(), AddFontCollectionActivity.class);
            intent.putParcelableArrayListExtra("fonts", fonts);
            intent.putExtra("edit_target", font);
            try (ActivityScenario<AddFontCollectionActivity> scenario = ActivityScenario.launch(intent)) {
                if (recreate) {
                    scenario.recreate();
                }
                scenario.onActivity(activity -> {
                    assertInvalidThenCorrected(activity, "original_font");
                    EditText input = activity.findViewById(R.id.ed_input);
                    TextInputLayout layout = activity.findViewById(R.id.ti_input);
                    input.setText("renamed_font");
                    assertNull(layout.getError());
                    activity.findViewById(R.id.common_dialog_cancel_button).performClick();
                });
            }
        } finally {
            if (fixture != null) {
                Files.deleteIfExists(fixture.toPath());
                assertFalse("Temporary font was not removed", fixture.exists());
            }
            for (int i = createdDirectories.size() - 1; i >= 0; i--) {
                // File.delete only removes an empty directory, preserving concurrent user files.
                createdDirectories.get(i).delete();
            }
        }
    }

    private static void assertInvalidThenCorrected(Activity activity, String validName) {
        EditText input = activity.findViewById(R.id.ed_input);
        TextInputLayout layout = activity.findViewById(R.id.ti_input);
        assertNotNull(input);
        assertNotNull(layout);
        for (String name : new String[]{"1font", "_font"}) {
            input.setText(name);
            assertEquals(activity.getString(R.string.logic_editor_message_variable_name_must_start_letter),
                    layout.getError().toString());
            input.setText(validName);
            assertNull(layout.getError());
        }
        assertFalse(activity.isFinishing());
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static void assertRenameIsCanceled(Intent intent) {
        try (ActivityScenario<AddFontCollectionActivity> scenario = ActivityScenario.launch(intent)) {
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    private static File systemFont() {
        File[] fonts = new File("/system/fonts").listFiles((directory, name) -> name.endsWith(".ttf"));
        assertNotNull("Cannot read system fonts", fonts);
        assertTrue("No system TrueType font available", fonts.length > 0);
        return fonts[0];
    }

    private static void createMissingDirectories(File directory, ArrayList<File> created) {
        if (directory.isDirectory()) {
            return;
        }
        File parent = directory.getParentFile();
        if (parent != null) {
            createMissingDirectories(parent, created);
        }
        assertTrue("Cannot create font fixture directory: " + directory, directory.mkdir());
        created.add(directory);
    }
}
