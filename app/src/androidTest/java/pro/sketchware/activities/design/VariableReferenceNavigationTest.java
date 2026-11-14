package pro.sketchware.activities.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static pro.sketchware.activities.design.ProjectEditorPersistenceTest.waitUntil;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inspector.WindowInspector;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.text.NumberFormat;
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
            for (int i = 0; i < 40; i++) {
                data.addVariable(file, ExtraMenuBean.VARIABLE_TYPE_NUMBER, "unused_" + i);
            }
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
            var countdown =
                    stack(
                            "31",
                            "addSourceDirectly",
                            "add source directly %s.inputOnly",
                            "test.f01.editor.MainActivity.deadline = 1000;");
            var branch = new BlockBean("30", "if %b", "c", "if");
            branch.parameters.add("true");
            branch.subStack1 = 31;
            branch.collapsed = true;
            countdown.add(0, branch);
            data.putBlocks(file, "start_countdown_moreBlock", countdown);

            var current =
                    stack("43", "addSourceDirectly", "add source %s.inputOnly", "deadline++;");
            var inner = new BlockBean("41", "if %b", "e", "ifElse");
            inner.parameters.add("true");
            inner.subStack1 = 42;
            inner.subStack2 = 43;
            inner.collapsed = true;
            inner.collapsed2 = true;
            var sibling = new BlockBean("42", "toast %s", " ", "doToast");
            sibling.parameters.add("unrelated branch");
            var outer = new BlockBean("40", "if %b", "c", "if");
            outer.parameters.add("true");
            outer.subStack1 = 41;
            outer.collapsed = true;
            current.addAll(0, List.of(outer, inner, sibling));
            data.putBlocks(file, "onResume_onResume", current);
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
                waitUntil(
                        () -> {
                            boolean[] ready = {false};
                            editor.onActivity(
                                    activity ->
                                            ready[0] =
                                                    activity.blockPane.getVisibility()
                                                            == View.VISIBLE);
                            return ready[0];
                        });
                openSearch(editor, true);
                clickRow("rows");
                followReference(
                        editorString(editor, R.string.block_clear_list_head) + " rows",
                        "onCreate",
                        "initializeLogic",
                        "11");
                followReference(
                        editorString(editor, R.string.block_delete_list_head)
                                + " 1 "
                                + editorString(editor, R.string.block_delete_list_body_1)
                                + " rows",
                        "refresh_data",
                        "moreBlock",
                        "21");
                clickView(R.id.reference_back);
                waitForView(R.id.reference_search);
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                waitUntil(() -> activeNodes(R.id.reference_title).isEmpty());

                openSearch(editor, false);
                waitForView(R.id.reference_search);
                waitUntil(() -> activeNodes(R.id.reference_query).isEmpty());
                performAction(
                        R.id.reference_list, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null);
                clickView(R.id.reference_search);
                setFilter("nothing_matches");
                waitForText(editorString(editor, R.string.search_canvas_no_results));
                clickView(R.id.reference_clear_query);
                waitForView(R.id.reference_row_title);
                waitUntil(() -> activeNodes(R.id.reference_clear_query).isEmpty());
                setFilter("unused_39");
                clickRow("unused_39");
                waitForText(editorString(editor, R.string.logic_editor_message_no_refs_found));
                if (Build.VERSION.SDK_INT >= 30) {
                    waitForKeyboardToHide();
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                } else {
                    clickView(R.id.reference_back);
                }
                waitForView(R.id.reference_query);
                waitForText("unused_39");
                setFilter("dead");
                clickRow("deadline");
                followReference(
                        "…ainActivity.deadline = 1000;", "start_countdown", "moreBlock", "31");
                editor.onActivity(
                        activity ->
                                assertEquals(
                                        View.GONE,
                                        activity.blockPane
                                                .findBlockByString("43")
                                                .getVisibility()));
                clickRow("deadline++;");
                waitUntil(() -> activeNodes(R.id.reference_title).isEmpty());
                editor.onActivity(
                        activity -> {
                            var pane = activity.blockPane;
                            assertEquals(
                                    View.VISIBLE, pane.findBlockByString("43").getVisibility());
                            assertTrue(pane.findBlockByString("41").collapsed);
                            assertEquals(View.GONE, pane.findBlockByString("42").getVisibility());
                        });
            }
        }
    }

    private static String editorString(ActivityScenario<LogicEditorActivity> editor, int resId) {
        String[] text = new String[1];
        editor.onActivity(activity -> text[0] = activity.getString(resId));
        return text[0];
    }

    private static void followReference(String label, String target, String event, String blockId)
            throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
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
            waitUntil(
                    () -> {
                        boolean[] located = {false};
                        instrumentation.runOnMainSync(
                                () -> {
                                    var block = opened.blockPane.findBlockByString(blockId);
                                    var other = opened.blockPane.findBlockByString("99");
                                    located[0] =
                                            block != null
                                                    && other != null
                                                    && block.getVisibility() == View.VISIBLE
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
        waitUntil(
                () ->
                        activeNodes(R.id.reference_result_row).stream()
                                .anyMatch(
                                        row ->
                                                row.isSelected()
                                                        && !row.findAccessibilityNodeInfosByText(
                                                                        label)
                                                                .isEmpty()));
        var locale =
                instrumentation
                        .getTargetContext()
                        .getResources()
                        .getConfiguration()
                        .getLocales()
                        .get(0);
        String oneResult = NumberFormat.getIntegerInstance(locale).format(1);
        waitUntil(
                () ->
                        activeNodes(R.id.reference_group_count).stream()
                                .anyMatch(
                                        node ->
                                                node.isVisibleToUser()
                                                        && node.getText() != null
                                                        && oneResult.contentEquals(
                                                                node.getText())));
    }

    private static void openSearch(ActivityScenario<LogicEditorActivity> editor, boolean isList) {
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
    }

    private static void clickRow(String text) throws Exception {
        waitUntil(
                () -> {
                    var automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
                    var root = automation.getRootInActiveWindow();
                    if (root == null) return false;
                    for (var node : root.findAccessibilityNodeInfosByText(text)) {
                        if (node.getText() == null || !text.contentEquals(node.getText())) continue;
                        String id = node.getViewIdResourceName();
                        if (id == null
                                || (!id.endsWith(":id/reference_row_title")
                                        && !id.endsWith(":id/reference_row_detail"))) {
                            continue;
                        }
                        for (var target = node; target != null; target = target.getParent()) {
                            if (target.isClickable()
                                    && target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                                return true;
                        }
                    }
                    return false;
                });
    }

    private static void waitForText(String text) throws Exception {
        waitUntil(
                () -> {
                    var root =
                            InstrumentationRegistry.getInstrumentation()
                                    .getUiAutomation()
                                    .getRootInActiveWindow();
                    return root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty();
                });
    }

    private static void clickView(int id) throws Exception {
        performAction(id, AccessibilityNodeInfo.ACTION_CLICK, null);
    }

    private static void setFilter(String text) throws Exception {
        var args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        performAction(R.id.reference_query, AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private static void performAction(int id, int action, Bundle args) throws Exception {
        waitUntil(
                () -> {
                    for (var node : activeNodes(id)) {
                        if (node.performAction(action, args)) return true;
                    }
                    return false;
                });
    }

    private static void waitForView(int id) throws Exception {
        waitUntil(() -> !activeNodes(id).isEmpty());
    }

    private static void waitForKeyboardToHide() throws Exception {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        waitUntil(
                () -> {
                    boolean[] hidden = {false};
                    instrumentation.runOnMainSync(
                            () -> {
                                for (View window : WindowInspector.getGlobalWindowViews()) {
                                    View title = window.findViewById(R.id.reference_title);
                                    if (title == null || !title.isShown()) continue;
                                    var insets = ViewCompat.getRootWindowInsets(title);
                                    hidden[0] =
                                            insets != null
                                                    && !insets.isVisible(
                                                            WindowInsetsCompat.Type.ime());
                                    break;
                                }
                            });
                    return hidden[0];
                });
    }

    private static List<AccessibilityNodeInfo> activeNodes(int id) {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        String name = instrumentation.getTargetContext().getResources().getResourceName(id);
        var root = instrumentation.getUiAutomation().getRootInActiveWindow();
        return root == null ? List.of() : root.findAccessibilityNodeInfosByViewId(name);
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
