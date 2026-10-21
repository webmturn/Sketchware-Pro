package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import pro.sketchware.R;
import pro.sketchware.activities.editor.LogicEditorActivity;
import pro.sketchware.activities.editor.logic.ExtraPaletteBlock;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.core.block.view.BaseBlockView;
import pro.sketchware.core.block.view.BlockView;
import pro.sketchware.core.block.view.FieldBlockView;
import pro.sketchware.core.codegen.BlockInterpreter;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.ProjectDataManager;

/** Checks the real palette, parameter matching, persisted project data and search renderer. */
@RunWith(AndroidJUnit4.class)
public class PaletteReporterTypeTest {
    @Test public void getChildAtPaletteKeepsViewTypeAndRejectsOtherSlots() throws Exception {
        try (Editor editor = new Editor()) {
            editor.onMain(activity -> {
                palette(activity).setBlock(6, 0xff4a6cd4);
                BlockView reporter = require(activity.paletteBlock, "viewGetChildAt");
                assertType(reporter, "View");
                for (String name : new String[]{"view", "View"}) {
                    assertTrue(activity.blockPane.isCompatibleBlock(reporter, new FieldBlockView(activity, "m", name)));
                }
                for (String name : new String[]{"MenuItem", "menuitem", "textview"}) {
                    assertFalse(name, activity.blockPane.isCompatibleBlock(reporter, new FieldBlockView(activity, "m", name)));
                }
            });
        }
    }

    @Test public void menuFindItemKeepsMenuItemTypeIncludingLowercaseSelectors() throws Exception {
        try (Editor editor = new Editor()) {
            editor.onMain(activity -> {
                palette(activity).setBlock(0, 0xffee7d16);
                BlockView reporter = require(activity.paletteBlock, "menuFindItem");
                assertType(reporter, "MenuItem");
                for (String name : new String[]{"MenuItem", "menuitem"}) {
                    assertTrue(name, activity.blockPane.isCompatibleBlock(reporter, new FieldBlockView(activity, "m", name)));
                }
                assertFalse(activity.blockPane.isCompatibleBlock(reporter, new FieldBlockView(activity, "m", "view")));
                assertFalse(activity.blockPane.isCompatibleBlock(reporter, new FieldBlockView(activity, "s", "")));
            });
        }
    }

    @Test public void nestedReportersKeepTypesAcrossDiskReloadAndActivityRecreation() throws Exception {
        try (Editor editor = new Editor()) {
            String[] ids = new String[2];
            AtomicReference<String> originalCode = new AtomicReference<>();
            editor.onMain(activity -> {
                palette(activity).setBlock(6, 0xff4a6cd4);
                BlockView viewStatement = activity.dropBlockOnPane(require(activity.paletteBlock, "setEnable"), 250, 250, false);
                BlockView childAt = activity.dropBlockOnPane(require(activity.paletteBlock, "viewGetChildAt"), 350, 350, false);
                fields(childAt).get(0).setArgValue("linear1");
                fields(childAt).get(1).setArgValue("0");
                fields(viewStatement).get(1).setArgValue("true");
                viewStatement.replaceParameter(fields(viewStatement).get(0), childAt);
                activity.blockPane.getRoot().setNextBlock(viewStatement);

                palette(activity).setBlock(0, 0xffee7d16);
                BlockView menuStatement = activity.dropBlockOnPane(require(activity.paletteBlock, "menuItemSetVisible"), 250, 450, false);
                BlockView findItem = activity.dropBlockOnPane(require(activity.paletteBlock, "menuFindItem"), 350, 550, false);
                fields(findItem).get(0).setArgValue("1");
                fields(menuStatement).get(1).setArgValue("true");
                menuStatement.replaceParameter(fields(menuStatement).get(0), findItem);
                viewStatement.setNextBlock(menuStatement);
                ids[0] = childAt.getTag().toString(); ids[1] = findItem.getTag().toString();
                assertType(childAt, "View"); assertType(findItem, "MenuItem");
                assertEquals(4, activity.blockPane.getBlocks().size());
                activity.saveBlocks();
                assertTrue(ProjectDataManager.getProjectDataManager(activity.scId).saveAllData());
                originalCode.set(generate(activity.scId, activity.blockPane.getBlocks()));
            });

            ProjectDataManager.reloadProjectFromData(editor.fixture.id);
            ArrayList<BlockBean> disk = ProjectDataManager.getProjectDataManager(editor.fixture.id)
                    .getBlocks(ProjectFileBean.DEFAULT_JAVA_NAME, "onCreateOptionsMenu_onCreateOptionsMenu");
            assertEquals(4, disk.size());
            assertEquals("View", bean(disk, "viewGetChildAt").typeName);
            assertEquals("MenuItem", bean(disk, "menuFindItem").typeName);
            assertEquals(originalCode.get(), generate(editor.fixture.id, disk));
            assertTrue(originalCode.get(), originalCode.get().contains("linear1.getChildAt("));
            assertTrue(originalCode.get(), originalCode.get().contains("menu.findItem(1)"));

            editor.scenario.recreate();
            editor.await(() -> {
                boolean[] ready = {false};
                editor.onMain(activity -> ready[0] = activity.blockPane.findBlockByString(ids[0]) != null
                        && activity.blockPane.findBlockByString(ids[1]) != null);
                return ready[0];
            });
            editor.onMain(activity -> {
                assertType(activity.blockPane.findBlockByString(ids[0]), "View");
                assertType(activity.blockPane.findBlockByString(ids[1]), "MenuItem");
                assertEquals(originalCode.get(), generate(activity.scId, activity.blockPane.getBlocks()));
            });
        }
    }

    @Test public void searchResultsRetainBothReporterTypes() throws Exception {
        try (Editor editor = new Editor()) {
            for (String[] item : new String[][]{{"viewGetChildAt", "View"}, {"menuFindItem", "MenuItem"}}) {
                editor.onMain(activity -> {
                    palette(activity).setBlock(item[1].equals("View") ? 6 : 0, 0xff4a6cd4);
                    BlockView source = require(activity.paletteBlock, item[0]);
                    // Palette search indexes displayed labels, which may be translated, rather than opcodes.
                    String query = null;
                    for (View child : source.specViews) if (child instanceof TextView label
                            && label.getText().toString().trim().length() >= 2) {
                        query = label.getText().toString().trim(); break;
                    }
                    assertNotNull("A visible label is needed for the search", query);
                    palette(activity).searchAllBlocks(query);
                });
                editor.await(() -> {
                    boolean[] ready = {false};
                    editor.onMain(activity -> ready[0] = find(activity.paletteBlock, item[0]) != null);
                    return ready[0];
                });
                editor.onMain(activity -> assertType(require(activity.paletteBlock, item[0]), item[1]));
            }
        }
    }

    private static final class Editor implements AutoCloseable {
        final ProjectEditorPersistenceTest.Fixture fixture = new ProjectEditorPersistenceTest.Fixture();
        final ActivityScenario<LogicEditorActivity> scenario;

        Editor() throws Exception {
            fixture.seed(false, false);
            ProjectDataManager.reloadProjectFromData(fixture.id);
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), LogicEditorActivity.class)
                    .putExtra("sc_id", fixture.id).putExtra("id", "onCreateOptionsMenu")
                    .putExtra("event", "onCreateOptionsMenu").putExtra("event_text", "Palette reporter types")
                    .putExtra("project_file", new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main"));
            scenario = ActivityScenario.launch(intent);
            onMain(activity -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(() -> {
                boolean[] ready = {false};
                onMain(activity -> ready[0] = activity.blockPane.getWidth() > 0 && palette(activity) != null
                        && activity.blockPane.getVisibility() == View.VISIBLE);
                return ready[0];
            });
        }

        void onMain(Consumer<LogicEditorActivity> action) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            scenario.onActivity(activity -> { try { action.accept(activity); } catch (Throwable error) { failure.set(error); } });
            if (failure.get() != null) throw new AssertionError(failure.get());
        }
        void await(Condition condition) throws Exception {
            long deadline = SystemClock.uptimeMillis() + 15000;
            while (SystemClock.uptimeMillis() < deadline) {
                if (condition.get()) return;
                SystemClock.sleep(40);
            }
            fail("Palette did not finish loading");
        }
        @Override public void close() throws Exception { try { scenario.close(); } finally { fixture.close(); } }
    }

    private interface Condition { boolean get() throws Exception; }
    private static ExtraPaletteBlock palette(LogicEditorActivity activity) {
        try {
            Field field = LogicEditorActivity.class.getDeclaredField("extraPaletteBlock"); field.setAccessible(true);
            return (ExtraPaletteBlock) field.get(activity);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static BlockView require(View view, String opcode) {
        BlockView block = find(view, opcode); assertNotNull(opcode, block); return block;
    }
    private static BlockView find(View view, String opcode) {
        if (view instanceof BlockView block && opcode.equals(block.opCode)) return block;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            BlockView block = find(group.getChildAt(i), opcode); if (block != null) return block;
        }
        return null;
    }
    private static void assertType(BlockView view, String type) {
        assertEquals(type, view.componentType);
        assertEquals(type, view.getClassInfo().getClassName());
        assertEquals(type, view.getBean().typeName);
        assertEquals("v", view.getBean().type);
    }
    private static ArrayList<FieldBlockView> fields(BlockView view) {
        ArrayList<FieldBlockView> fields = new ArrayList<>();
        for (View child : view.specViews) if (child instanceof FieldBlockView field) fields.add(field);
        return fields;
    }
    private static BlockBean bean(ArrayList<BlockBean> blocks, String opcode) {
        for (BlockBean block : blocks) if (opcode.equals(block.opCode)) return block;
        throw new AssertionError(opcode);
    }
    private static String generate(String id, ArrayList<BlockBean> blocks) {
        BuildConfig config = new BuildConfig(); config.sc_id = id;
        return new BlockInterpreter("MainActivity", config, blocks, false).interpretBlocks();
    }
}
