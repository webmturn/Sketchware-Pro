package pro.sketchware.activities.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.logic.ExtraMenuBean;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.project.ProjectDataManager;

@RunWith(AndroidJUnit4.class)
public class VariableReferenceNavigationTest {
    @Test
    public void referencesOpenInitializationAndMoreBlocksIncludingDirectSource() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        try (var fixture = new ProjectEditorPersistenceTest.Fixture()) {
            fixture.seed(false, false);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            var data = ProjectDataManager.getProjectDataManager(fixture.id);
            var page = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
            String file = page.getJavaName();
            String declaration = "ArrayList<String> rows = new ArrayList<>()";
            String timer = "static long deadline";
            data.addListVariable(file, 4, declaration);
            data.addVariable(file, ExtraMenuBean.VARIABLE_TYPE_CUSTOM_DECLARATION, timer);
            data.addMoreBlock(file, "refresh_data", "refresh_data");
            data.addMoreBlock(file, "start_countdown", "start_countdown");
            data.putBlocks(
                    file,
                    "onCreate_initializeLogic",
                    stack("11", "clearList", "clear %m.list", "rows"));
            data.putBlocks(
                    file,
                    "refresh_data_moreBlock",
                    stack("21", "deleteList", "delete %d from %m.list", "1", "rows"));
            data.putBlocks(
                    file,
                    "start_countdown_moreBlock",
                    stack(
                            "31",
                            "addSourceDirectly",
                            "add source directly %s.inputOnly",
                            "long now = android.os.SystemClock.elapsedRealtime();\nMainActivity.deadline = now + 1000;"));
            Intent intent =
                    new Intent(instrumentation.getTargetContext(), LogicEditorActivity.class)
                            .putExtra("sc_id", fixture.id)
                            .putExtra("id", "onResume")
                            .putExtra("event", "onResume")
                            .putExtra("project_file", page);
            try (var editor = ActivityScenario.<LogicEditorActivity>launch(intent)) {
                editor.onActivity(
                        activity ->
                                activity.getWindow()
                                        .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
                ProjectEditorPersistenceTest.waitUntil(
                        () -> {
                            boolean[] ready = {false};
                            editor.onActivity(
                                    activity ->
                                            ready[0] =
                                                    activity.blockPane.getVisibility()
                                                            == View.VISIBLE);
                            return ready[0];
                        });
                followReference(
                        editor,
                        true,
                        declaration,
                        "clear rows",
                        "onCreate",
                        "initializeLogic",
                        "11");
                followReference(
                        editor,
                        true,
                        declaration,
                        "delete 1 from rows",
                        "refresh_data",
                        "moreBlock",
                        "21");
                String sourceLabel =
                        instrumentation
                                .getTargetContext()
                                .getString(R.string.block_add_source_directly_head);
                followReference(
                        editor,
                        false,
                        timer,
                        sourceLabel + " · MainActivity.deadline = now + 1000;",
                        "start_countdown",
                        "moreBlock",
                        "31");
            }
        }
    }

    private static void followReference(
            ActivityScenario<LogicEditorActivity> editor,
            boolean isList,
            String declaration,
            String label,
            String target,
            String event,
            String blockId)
            throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        editor.onActivity(
                activity -> {
                    activity.onBlockSizeChanged(
                            isList ? 1 : 0,
                            activity.getColor(
                                    isList ? R.color.palette_list : R.color.palette_variable));
                    View search =
                            activity.paletteBlock.findViewWithTag(
                                    isList ? "listFindRefs" : "variableFindRefs");
                    assertNotNull(search);
                    search.performClick();
                });
        clickRow(declaration);

        var monitor = instrumentation.addMonitor(LogicEditorActivity.class.getName(), null, false);
        LogicEditorActivity destination = null;
        try {
            clickRow(label);
            destination =
                    (LogicEditorActivity) instrumentation.waitForMonitorWithTimeout(monitor, 8000);
            assertNotNull(destination);
            LogicEditorActivity opened = destination;
            assertEquals(target, opened.getIntent().getStringExtra("id"));
            assertEquals(event, opened.getIntent().getStringExtra("event"));
            assertEquals(blockId, opened.getIntent().getStringExtra("scroll_to_block_id"));
            ProjectEditorPersistenceTest.waitUntil(
                    () -> {
                        boolean[] located = {false};
                        instrumentation.runOnMainSync(
                                () -> {
                                    var block = opened.blockPane.findBlockByString(blockId);
                                    var other = opened.blockPane.findBlockByString("99");
                                    located[0] =
                                            block != null
                                                    && other != null
                                                    && block.getAlpha() == 1f
                                                    && other.getAlpha() == 0.25f;
                                });
                        return located[0];
                    });
        } finally {
            if (destination != null) instrumentation.runOnMainSync(destination::finish);
            instrumentation.removeMonitor(monitor);
            instrumentation.waitForIdleSync();
        }
    }

    private static void clickRow(String text) throws Exception {
        ProjectEditorPersistenceTest.waitUntil(
                () -> {
                    var automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
                    var root = automation.getRootInActiveWindow();
                    if (root == null) return false;
                    for (var node : root.findAccessibilityNodeInfosByText(text)) {
                        if (node.getText() != null
                                && text.contentEquals(node.getText())
                                && node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                            return true;
                    }
                    return false;
                });
    }

    private static ArrayList<BlockBean> stack(
            String id, String opcode, String spec, String... parameters) {
        var block = new BlockBean(id, spec, " ", opcode);
        block.parameters.addAll(List.of(parameters));
        block.nextBlock = 99;
        var next = new BlockBean("99", "toast %s", " ", "doToast");
        next.parameters.add("other");
        return new ArrayList<>(List.of(block, next));
    }
}
