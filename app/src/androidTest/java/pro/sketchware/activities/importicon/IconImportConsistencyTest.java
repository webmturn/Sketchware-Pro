package pro.sketchware.activities.importicon;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Pair;
import android.util.Xml;
import android.view.View;
import android.view.WindowManager;
import android.view.inspector.WindowInspector;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.PathParser;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import pro.sketchware.R;
import pro.sketchware.activities.importicon.adapters.IconAdapter;
import pro.sketchware.core.async.BackgroundTasks;
import pro.sketchware.core.async.TaskHost;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.graphics.SvgUtils;
import pro.sketchware.util.UIHelper;

/** Uses private SVG fixtures and returns intents; never imports files into a user's project. */
@RunWith(AndroidJUnit4.class)
public class IconImportConsistencyTest {
    @Test public void failedCatalogReadKeepsSelectionAndLoadedPages() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(121);
            fixture.on(activity -> {
                invoke(activity, "loadMoreItems");
                invoke(activity, "loadMoreItems");
            });
            fixture.awaitCount(120);
            List<Pair<String, String>> originalList = new ArrayList<>(fixture.catalog.subList(0, 120));
            String missingFamily = "__missing_icon_pack_" + UUID.randomUUID();
            assertFalse(Files.exists(Path.of(SketchwarePaths.getExtractedIconPackStoreLocation(), missingFamily)));
            fixture.on(activity -> {
                Method method = ImportIconActivity.class.getDeclaredMethod("applyIconFilter", String.class, String.class);
                method.setAccessible(true);
                method.invoke(activity, missingFamily, "outline");
            });
            CountDownLatch done = new CountDownLatch(1);
            BackgroundTasks.runSerial(TaskHost.of(fixture.activity), "IconImportReadFailureTest",
                    () -> {}, done::countDown, null);
            assertTrue(done.await(10, TimeUnit.SECONDS));
            fixture.on(activity -> {
                assertEquals("svg", get(activity, "selected_icon_family"));
                assertEquals("round", get(activity, "selected_icon_type"));
                assertEquals(fixture.catalog, get(activity, "allIconPaths"));
                assertEquals(originalList, fixture.adapter().getCurrentList());
                assertEquals(3, get(activity, "currentPage"));
                assertFalse((Boolean) get(activity, "isLastPage"));
            });
        }
    }

    @Test public void switchingSymbolStyleKeepsLoadedPagesAndScrollPosition() throws Exception {
        try (Fixture fixture = new Fixture()) {
            changeFilter(fixture, "symbols", "round");
            fixture.on(activity -> {
                invoke(activity, "loadMoreItems");
                invoke(activity, "loadMoreItems");
            });
            fixture.awaitCount(120);
            fixture.on(activity -> ((GridLayoutManager)
                    ((RecyclerView) activity.findViewById(R.id.image_list)).getLayoutManager()).scrollToPositionWithOffset(80, -17));
            await(() -> {
                AtomicInteger first = new AtomicInteger();
                fixture.on(activity -> first.set(((GridLayoutManager)
                        ((RecyclerView) activity.findViewById(R.id.image_list)).getLayoutManager()).findFirstVisibleItemPosition()));
                return first.get() > 0;
            });
            AtomicInteger position = new AtomicInteger(), offset = new AtomicInteger();
            AtomicReference<List<Pair<String, String>>> originalList = new AtomicReference<>();
            fixture.on(activity -> {
                var manager = (GridLayoutManager)
                        ((RecyclerView) activity.findViewById(R.id.image_list)).getLayoutManager();
                position.set(manager.findFirstVisibleItemPosition());
                offset.set(manager.getDecoratedTop(manager.findViewByPosition(position.get())));
                originalList.set(new ArrayList<>(fixture.adapter().getCurrentList()));
            });
            assertTrue(position.get() > 0);
            changeFilter(fixture, "symbols", "sharp");
            fixture.awaitCount(120);
            fixture.on(activity -> {
                assertEquals(originalList.get(), fixture.adapter().getCurrentList());
                assertEquals(3, get(activity, "currentPage"));
                assertFalse((Boolean) get(activity, "isLastPage"));
                var manager = (GridLayoutManager)
                        ((RecyclerView) activity.findViewById(R.id.image_list)).getLayoutManager();
                assertEquals(position.get(), manager.findFirstVisibleItemPosition());
                assertEquals(offset.get(), manager.getDecoratedTop(manager.findViewByPosition(position.get())));
            });
        }
    }

    @Test public void failedSelectedSymbolExportCanBeRetried() throws Exception {
        Path root = Files.createTempDirectory(context().getCacheDir().toPath(), "symbol_export_");
        try {
            MaterialSymbolsPack pack = MaterialSymbolsPack.load(new ContextWrapper(context()) {
                @Override public File getCacheDir() { return root.toFile(); }
            }, "sharp");
            assertFalse(Files.exists(root.resolve("icon_import")));
            Path source = pack.exportIcon("database").toPath();
            assertArrayEquals(pack.getSvg("database"), Files.readAllBytes(source));
            Files.delete(source);
            Files.createDirectory(source);
            try {
                pack.exportIcon("database");
                fail("A failed selected SVG write must reach the caller");
            } catch (IOException expected) {
                assertTrue(Files.isDirectory(source));
            }
            Files.delete(source);
            assertEquals(source, pack.exportIcon("database").toPath());
            assertArrayEquals(pack.getSvg("database"), Files.readAllBytes(source));
            Files.delete(source);
            assertTrue(pack.exportIcon("database").isFile());
        } finally {
            removeTestFiles(root);
        }
    }

    @Test public void symbolsSelectionSurvivesActivityRecreation() throws Exception {
        try (Fixture fixture = new Fixture()) {
            changeFilter(fixture, "symbols", "sharp");
            fixture.scenario.recreate();
            fixture.on(activity -> fixture.activity = activity);
            awaitFilter(fixture, "symbols", "sharp");
            fixture.awaitCount(40);
            fixture.on(activity -> assertNotNull(get(activity, "symbolsPack")));
        }
    }

    @Test public void symbolPreviewRendersFromTheBundledCatalog() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, "database_search"));
            fixture.awaitCount(0);
            changeFilter(fixture, "symbols", "outline");
            fixture.awaitCount(1);
            await(() -> {
                AtomicReference<Boolean> ready = new AtomicReference<>(false);
                fixture.on(activity -> {
                    RecyclerView list = activity.findViewById(R.id.image_list);
                    var holder = list.findViewHolderForAdapterPosition(0);
                    ready.set(holder != null && ((ImageView) holder.itemView.findViewById(R.id.img)).getDrawable() != null);
                });
                return ready.get();
            });
            fixture.on(activity -> {
                var holder = ((RecyclerView) activity.findViewById(R.id.image_list)).findViewHolderForAdapterPosition(0);
                var drawable = ((ImageView) holder.itemView.findViewById(R.id.img)).getDrawable();
                Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                drawable.setBounds(0, 0, 48, 48);
                drawable.draw(new Canvas(bitmap));
                int[] pixels = new int[48 * 48];
                bitmap.getPixels(pixels, 0, 48, 0, 0, 48, 48);
                int visible = 0;
                for (int pixel : pixels) if (Color.alpha(pixel) > 0) visible++;
                assertTrue(visible > 20 && visible < pixels.length);
                bitmap.recycle();
            });
        }
    }

    @Test public void legacyExtractedNamesDoNotEnterTheBundledCatalog() throws Exception {
        Path legacy = Path.of(SketchwarePaths.getExtractedIconPackStoreLocation(), "symbols");
        Path removed = Files.createDirectories(legacy.resolve("__removed_symbol__"));
        Files.write(removed.resolve("sharp.svg"), new byte[]{1});
        MaterialSymbolsPack pack = MaterialSymbolsPack.load(context(), "sharp");
        assertFalse(pack.getIconPaths().isEmpty());
        assertFalse(pack.getIconPaths().stream().anyMatch(icon -> icon.first.equals("__removed_symbol__")));
        MaterialSymbolsPack.removeLegacyFiles();
        assertFalse(Files.exists(legacy));
    }

    @Test public void symbolsFilterShowsSupportedStylesAndCancelKeepsCurrentSelection() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.on(activity -> invoke(activity, "updateIcons", String.class, "twotone"));
            openFilter(fixture);
            fixture.on(activity -> {
                View root = filterDialog();
                ((ChipGroup) root.findViewById(R.id.chipGroupFamily)).check(R.id.chip_material_symbols);
                assertEquals(View.GONE, root.findViewById(R.id.chip_baseline).getVisibility());
                assertEquals(View.GONE, root.findViewById(R.id.chip_twotone).getVisibility());
                assertEquals(R.id.chip_outline, ((ChipGroup) root.findViewById(R.id.chipGroupStyle)).getCheckedChipId());
                root.findViewById(android.R.id.button2).performClick();
                assertEquals("svg", get(activity, "selected_icon_family"));
                assertEquals("twotone", get(activity, "selected_icon_type"));
            });
        }
    }

    @Test public void switchingIconSetsPreservesSearchAndImportsTheSelectedSymbol() throws Exception {
        List<Pair<String, String>> matchingSymbols = MaterialSymbolsPack.load(context(), "round").getIconPaths();
        matchingSymbols.removeIf(icon -> !icon.first.contains("database"));
        try (Fixture fixture = new Fixture()) {
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, "database"));
            openFilter(fixture);
            fixture.on(activity -> {
                View root = filterDialog();
                ((ChipGroup) root.findViewById(R.id.chipGroupFamily)).check(R.id.chip_material_symbols);
                root.findViewById(android.R.id.button1).performClick();
            });
            fixture.awaitList(matchingSymbols);
            fixture.on(activity -> {
                assertEquals("database", get(activity, "currentQuery"));
                assertTrue(fixture.adapter().getCurrentList().stream().allMatch(icon -> icon.second.contains("/symbols/")));
            });
            openFilter(fixture);
            fixture.on(activity -> {
                View root = filterDialog();
                ((ChipGroup) root.findViewById(R.id.chipGroupStyle)).check(R.id.chip_sharp);
                root.findViewById(android.R.id.button1).performClick();
            });
            awaitFilter(fixture, "symbols", "sharp");
            fixture.awaitList(matchingSymbols);
            fixture.on(activity -> {
                int selected = -1;
                for (int i = 0; i < fixture.adapter().getItemCount(); i++) {
                    Pair<String, String> icon = fixture.adapter().getCurrentList().get(i);
                    assertNotNull(((MaterialSymbolsPack) get(activity, "symbolsPack")).getSvg(icon.first));
                    if (icon.first.equals("database_search")) selected = i;
                }
                assertTrue(selected >= 0);
                UIHelper.lastClickTime = 0;
                activity.onIconSelected(selected);
                assertNotNull(dialog());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            fixture.on(activity -> {
                View root = dialog();
                assertEquals("icon_database_search_sharp", ((EditText) root.findViewById(R.id.inputText)).getText().toString());
                assertTrue(root.findViewById(android.R.id.button1).performClick());
                assertTrue("Saving a readable new icon must return a result; name error="
                        + ((TextInputLayout) root.findViewById(R.id.textInputLayout)).getError()
                        + ", source error visibility=" + root.findViewById(R.id.source_error).getVisibility(), activity.isFinishing());
            });
            var result = fixture.scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            assertTrue(result.getResultData().getStringExtra("iconPath").endsWith("/symbols/database_search/sharp.svg"));
            assertTrue(Files.isRegularFile(Path.of(result.getResultData().getStringExtra("iconPath"))));
            assertEquals("icon_database_search_sharp", result.getResultData().getStringExtra("iconName"));
        }
    }

    @Test public void newSymbolsConvertToVisibleVectorsInAllThreeStyles() throws Exception {
        Path root = Files.createTempDirectory(context().getCacheDir().toPath(), "symbol_vectors_");
        try {
            SvgUtils svg = new SvgUtils(context());
            for (String style : new String[]{"outline", "round", "sharp"}) {
                MaterialSymbolsPack pack = MaterialSymbolsPack.load(context(), style);
                for (String name : new String[]{"database", "database_search", "robot_2", "network_node", "markdown", "code_blocks", "terminal_2", "automation"}) {
                    Path copy = root.resolve(name + "_" + style + ".svg");
                    Files.write(copy, pack.getSvg(name));
                    Path output = root.resolve("vectors");
                    svg.convert(copy.toString(), output.toString(), "#123456");
                    var parser = Xml.newPullParser();
                    parser.setInput(new StringReader(new String(Files.readAllBytes(output.resolve(name + "_" + style + ".xml")), StandardCharsets.UTF_8)));
                    Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    String android = "http://schemas.android.com/apk/res/android";
                    parser.nextTag();
                    assertEquals("vector", parser.getName());
                    assertEquals(24f, Float.parseFloat(parser.getAttributeValue(android, "viewportWidth")), 0f);
                    assertEquals(24f, Float.parseFloat(parser.getAttributeValue(android, "viewportHeight")), 0f);
                    canvas.scale(2, 2);
                    while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (parser.getEventType() == org.xmlpull.v1.XmlPullParser.START_TAG && "path".equals(parser.getName())) {
                            String color = parser.getAttributeValue(android, "fillColor");
                            assertEquals("#123456", color);
                            paint.setColor(Color.parseColor(color));
                            canvas.drawPath(PathParser.createPathFromPathData(parser.getAttributeValue(android, "pathData")), paint);
                        }
                    }
                    int[] pixels = new int[48 * 48];
                    bitmap.getPixels(pixels, 0, 48, 0, 0, 48, 48);
                    int visible = 0;
                    for (int pixel : pixels) if (Color.alpha(pixel) > 0) visible++;
                    assertTrue(name + "/" + style + " must render inside the viewport", visible > 20 && visible < pixels.length);
                    bitmap.recycle();
                }
            }
        } finally {
            removeTestFiles(root);
        }
    }

    private static void openFilter(Fixture fixture) throws Exception {
        fixture.on(activity -> invoke(activity, "showFilterDialog"));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        fixture.on(activity -> assertNotNull(filterDialog()));
    }

    private static void changeFilter(Fixture fixture, String family, String type) throws Exception {
        fixture.on(activity -> {
            Method method = ImportIconActivity.class.getDeclaredMethod("applyIconFilter", String.class, String.class);
            method.setAccessible(true);
            method.invoke(activity, family, type);
        });
        awaitFilter(fixture, family, type);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void awaitFilter(Fixture fixture, String family, String type) throws Exception {
        await(() -> {
            AtomicReference<Boolean> ready = new AtomicReference<>();
            fixture.on(activity -> ready.set(family.equals(get(activity, "selected_icon_family"))
                    && type.equals(get(activity, "selected_icon_type"))));
            return ready.get();
        });
    }

    private static View filterDialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) if (root.findViewById(R.id.chipGroupFamily) != null) return root;
        return null;
    }

    @Test public void missingSourceKeepsDialogAndNameThenAllowsRetry() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            Path source = Path.of(fixture.catalog.get(0).second, "round.svg");
            byte[] original = Files.readAllBytes(source);
            Files.delete(source);
            openSave(fixture, "my_saved_icon");
            assertSourceRejected(fixture, "my_saved_icon");
            Files.write(source, original);
            fixture.on(activity -> dialog().findViewById(android.R.id.button1).performClick());
            var result = fixture.scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            assertEquals("my_saved_icon", result.getResultData().getStringExtra("iconName"));
            assertEquals(source.toString(), result.getResultData().getStringExtra("iconPath"));
        }
    }

    @Test public void sourceRemovedAfterOpeningCannotBeImported() throws Exception {
        assertInvalidSource("removed");
    }

    @Test public void emptySourceCannotBeImported() throws Exception {
        assertInvalidSource("empty");
    }

    @Test public void directoryCannotBeImportedAsSvg() throws Exception {
        assertInvalidSource("directory");
    }

    @Test public void unreadableSourceCannotBeImported() throws Exception {
        assertInvalidSource("unreadable");
    }

    @Test public void sourceErrorIsLocalizedAndFailedDialogCanBeCanceled() throws Exception {
        Context base = context();
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(Locale.SIMPLIFIED_CHINESE);
        String chinese = base.createConfigurationContext(configuration).getString(R.string.import_icon_source_unavailable);
        assertTrue(chinese, chinese.contains("图标文件"));
        configuration.setLocale(Locale.ENGLISH);
        assertTrue(base.createConfigurationContext(configuration).getString(R.string.import_icon_source_unavailable).contains("icon file"));
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            openSave(fixture, "my_saved_icon");
            Files.delete(Path.of(fixture.catalog.get(0).second, "round.svg"));
            assertSourceRejected(fixture, "my_saved_icon");
            fixture.on(activity -> {
                dialog().findViewById(android.R.id.button2).performClick();
                assertFalse(activity.isFinishing());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            fixture.on(activity -> assertNull(dialog()));
        }
    }

    private static void assertInvalidSource(String state) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            Path source = Path.of(fixture.catalog.get(0).second, "round.svg");
            openSave(fixture, "my_saved_icon");
            try {
                switch (state) {
                    case "removed": Files.delete(source); break;
                    case "empty": Files.write(source, new byte[0]); break;
                    case "directory": Files.delete(source); Files.createDirectory(source); break;
                    case "unreadable":
                        assertTrue(source.toFile().setReadable(false, false));
                        assertFalse("Fixture must actually be unreadable by this app", source.toFile().canRead());
                        break;
                    default: throw new AssertionError(state);
                }
                assertSourceRejected(fixture, "my_saved_icon");
            } finally {
                if (Files.isRegularFile(source)) assertTrue(source.toFile().setReadable(true, true));
            }
        }
    }

    private static void openSave(Fixture fixture, String name) throws Exception {
        fixture.on(activity -> {
            UIHelper.lastClickTime = 0;
            activity.onIconSelected(0);
            assertNotNull(dialog());
            ((EditText) dialog().findViewById(R.id.inputText)).setText(name);
        });
        // Dialog installs the real Save listener in an asynchronously dispatched OnShow callback.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void assertSourceRejected(Fixture fixture, String name) throws Exception {
        AtomicReference<Boolean> finishing = new AtomicReference<>(false);
        fixture.on(activity -> {
            assertTrue(dialog().findViewById(android.R.id.button1).performClick());
            finishing.set(activity.isFinishing());
        });
        assertFalse("Invalid SVG must leave the dialog open instead of returning RESULT_OK", finishing.get());
        fixture.on(activity -> {
            View root = dialog(); assertNotNull(root);
            TextView message = root.findViewById(R.id.source_error);
            assertNotNull(message); assertEquals(View.VISIBLE, message.getVisibility());
            assertEquals(activity.getString(R.string.import_icon_source_unavailable), message.getText().toString());
            assertEquals(name, ((EditText) root.findViewById(R.id.inputText)).getText().toString());
            assertTrue(root.findViewById(android.R.id.button1).isEnabled());
        });
    }

    @Test public void filteredResultsNeverAppendUnfilteredPages() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, "home"));
            fixture.awaitCount(3);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(3);
            fixture.on(activity -> assertTrue(fixture.adapter().getCurrentList().stream().allMatch(icon -> icon.first.startsWith("home"))));
        }
    }

    @Test public void clearingSearchRestartsPaginationAfterTheLastPage() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            fixture.on(activity -> {
                invoke(activity, "loadMoreItems"); invoke(activity, "loadMoreItems"); invoke(activity, "loadMoreItems");
                assertTrue((Boolean) get(activity, "isLastPage"));
                invoke(activity, "filterIcons", String.class, "home");
                invoke(activity, "filterIcons", String.class, "");
                assertFalse("Clearing the query must reopen pagination", (Boolean) get(activity, "isLastPage"));
            });
            fixture.awaitCount(40);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(80);
            fixture.on(activity -> invoke(activity, "loadMoreItems"));
            fixture.awaitCount(95);
        }
    }

    @Test public void searchIgnoresWhitespaceAndProcessLocale() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(95);
            Locale original = Locale.getDefault();
            Locale format = Locale.getDefault(Locale.Category.FORMAT), display = Locale.getDefault(Locale.Category.DISPLAY);
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, "  OTHER  "));
                fixture.awaitCount(92);
                fixture.on(activity -> {
                    List<Pair<String, String>> renamed = new ArrayList<>(fixture.catalog);
                    renamed.set(0, Pair.create("image", renamed.get(0).second));
                    set(activity, "allIconPaths", renamed);
                    invoke(activity, "filterIcons", String.class, " IMAGE ");
                });
                fixture.awaitCount(1);
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, "   "));
                fixture.awaitCount(40);
                fixture.on(activity -> invoke(activity, "filterIcons", String.class, null));
                fixture.awaitCount(40);
            } finally {
                Locale.setDefault(original);
                Locale.setDefault(Locale.Category.FORMAT, format); Locale.setDefault(Locale.Category.DISPLAY, display);
            }
        }
    }

    @Test public void saveKeepsSelectedIconWhenTheListIsReordered() throws Exception {
        saveAfterListChange(false);
    }

    @Test public void catalogCompletionKeepsTheActiveSearch() throws Exception {
        try (Fixture fixture = new Fixture()) {
            String query = "__no_match_" + UUID.randomUUID();
            fixture.on(activity -> invoke(activity, "filterIcons", String.class, query));
            fixture.awaitCount(0);
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            fixture.on(activity -> invoke(activity, "reloadIcons"));
            BackgroundTasks.runSerial(TaskHost.of(fixture.activity), "IconImportReloadTest",
                    () -> {}, done::countDown, error -> { failure.set(error); done.countDown(); });
            assertTrue(done.await(10, TimeUnit.SECONDS));
            assertNull(failure.get());
            fixture.awaitCount(0);
        }
    }

    @Test public void emptyCatalogClearsPreviousResults() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(0);
            fixture.on(activity -> assertTrue((Boolean) get(activity, "isLastPage")));
        }
    }

    @Test public void saveKeepsSelectedIconWhenTheListShrinks() throws Exception {
        saveAfterListChange(true);
    }

    @Test public void invalidClickPositionsAreIgnored() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            fixture.on(activity -> {
                UIHelper.lastClickTime = 0; activity.onIconSelected(-1);
                UIHelper.lastClickTime = 0; activity.onIconSelected(2);
                assertNull(dialog());
                assertFalse(activity.isFinishing());
            });
        }
    }

    @Test public void changedPathsRebindAndStyleUpdatesKeepAsyncDiffs() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            AtomicInteger fullRefreshes = new AtomicInteger(), changes = new AtomicInteger(), colorPayloads = new AtomicInteger();
            RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
                @Override public void onChanged() { fullRefreshes.incrementAndGet(); }
                @Override public void onItemRangeChanged(int start, int count, Object payload) {
                    changes.addAndGet(count); if (payload != null) colorPayloads.incrementAndGet();
                }
            };
            List<Pair<String, String>> replacement = List.of(Pair.create("home_0", fixture.root.resolve("changed").toString()), fixture.catalog.get(1));
            fixture.on(activity -> {
                fixture.adapter().registerAdapterDataObserver(observer);
                fixture.adapter().submitList(replacement);
            });
            fixture.awaitList(replacement);
            fixture.on(activity -> {
                assertTrue("A changed SVG path must rebind its row", changes.get() > 0);
                fixture.adapter().setSelectedColor(0xff123456);
                fixture.adapter().setSelectedIconType("outline");
                assertTrue("Color changes should use a content payload", colorPayloads.get() > 0);
                assertEquals(0, fullRefreshes.get());
                fixture.adapter().unregisterAdapterDataObserver(observer);
            });
        }
    }

    private static void saveAfterListChange(boolean shrink) throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.catalog(2);
            Pair<String, String> selected = fixture.catalog.get(1);
            fixture.on(activity -> { UIHelper.lastClickTime = 0; activity.onIconSelected(1); assertNotNull(dialog()); });
            List<Pair<String, String>> next = shrink ? List.of(fixture.catalog.get(0)) : List.of(selected, fixture.catalog.get(0));
            fixture.on(activity -> fixture.adapter().submitList(next));
            fixture.awaitList(next);
            fixture.on(activity -> {
                View root = dialog(); assertNotNull(root);
                assertTrue(root.findViewById(android.R.id.button1).performClick());
            });
            var result = fixture.scenario.getResult();
            assertEquals(Activity.RESULT_OK, result.getResultCode());
            assertEquals(selected.second + "/round.svg", result.getResultData().getStringExtra("iconPath"));
            assertEquals("icon_" + selected.first + "_round", result.getResultData().getStringExtra("iconName"));
            assertEquals(0xff9e9e9e, result.getResultData().getIntExtra("iconColor", 0));
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory(context().getCacheDir().toPath(), "icon_import_");
        final ActivityScenario<ImportIconActivity> scenario;
        final long originalClickTime = UIHelper.lastClickTime;
        final String originalFamily = context().getSharedPreferences("icon_pack", Context.MODE_PRIVATE).getString("selected_family", null);
        final String originalStyle = context().getSharedPreferences("icon_pack", Context.MODE_PRIVATE).getString("selected_style", null);
        ImportIconActivity activity;
        List<Pair<String, String>> catalog;

        Fixture() throws Exception {
            try {
                context().getSharedPreferences("icon_pack", Context.MODE_PRIVATE).edit()
                        .remove("selected_family").remove("selected_style").commit();
                scenario = ActivityScenario.launchActivityForResult(new Intent(context(), ImportIconActivity.class)
                        .putExtra("sc_id", "__icon_import_" + UUID.randomUUID()).putStringArrayListExtra("imageNames", new ArrayList<>()));
                on(value -> { activity = value; value.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); });
                await(() -> {
                    AtomicInteger size = new AtomicInteger(); on(value -> size.set(adapter().getItemCount())); return size.get() > 0;
                });
                CountDownLatch done = new CountDownLatch(1);
                BackgroundTasks.runSerial(TaskHost.of(activity), "IconImportTestDrain", done::countDown, null, null);
                assertTrue(done.await(10, TimeUnit.SECONDS));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            } catch (Exception | AssertionError failure) {
                try {
                    close();
                } catch (Exception | AssertionError cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
                throw failure;
            }
        }

        void catalog(int size) throws Exception {
            catalog = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                String name = index < 3 ? "home_" + index : "other_" + index;
                Path folder = Files.createDirectory(root.resolve(name));
                String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\"><path d=\"M0 0h24v24H0z\"/></svg>";
                for (String type : new String[]{"round", "outline"}) Files.write(folder.resolve(type + ".svg"), svg.getBytes(StandardCharsets.UTF_8));
                catalog.add(Pair.create(name, folder.toString()));
            }
            on(value -> {
                set(value, "allIconPaths", new ArrayList<>(catalog)); set(value, "icons", new ArrayList<>());
                set(value, "currentPage", 0); set(value, "isLastPage", false); set(value, "isLoading", false);
                invoke(value, "filterIcons", String.class, "");
            });
            awaitCount(Math.min(40, size));
        }

        IconAdapter adapter() throws Exception { return (IconAdapter) get(activity, "adapter"); }

        void awaitCount(int expected) throws Exception {
            await(() -> { AtomicInteger size = new AtomicInteger(); on(value -> size.set(adapter().getItemCount())); return size.get() == expected; });
        }

        void awaitList(List<Pair<String, String>> expected) throws Exception {
            await(() -> { AtomicReference<List<Pair<String, String>>> list = new AtomicReference<>(); on(value -> list.set(adapter().getCurrentList())); return expected.equals(list.get()); });
        }

        void on(Action action) throws Exception {
            AtomicReference<Throwable> error = new AtomicReference<>();
            scenario.onActivity(value -> { try { action.run(value); } catch (Throwable failure) { error.set(failure); } });
            if (error.get() != null) throw new AssertionError(error.get());
        }

        @Override public void close() throws Exception {
            try {
                if (scenario != null) scenario.close();
            } finally {
                UIHelper.lastClickTime = originalClickTime;
                context().getSharedPreferences("icon_pack", Context.MODE_PRIVATE).edit()
                        .putString("selected_family", originalFamily).putString("selected_style", originalStyle).commit();
                removeTestFiles(root);
            }
        }
    }

    private static void removeTestFiles(Path root) throws Exception {
        assertEquals(context().getCacheDir().getCanonicalFile().toPath(), root.toFile().getCanonicalFile().toPath().getParent());
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }

    private static View dialog() {
        for (View root : WindowInspector.getGlobalWindowViews()) if (root.findViewById(R.id.inputText) != null) return root;
        return null;
    }

    private static void await(Check check) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        do { if (check.get()) return; SystemClock.sleep(25); } while (SystemClock.uptimeMillis() < end);
        fail("Timed out waiting for icon list");
    }
    private interface Check { boolean get() throws Exception; }
    private interface Action { void run(ImportIconActivity activity) throws Exception; }
    private static Object get(Object target, String name) throws Exception {
        Field field = ImportIconActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = ImportIconActivity.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void invoke(Object target, String name) throws Exception { invoke(target, name, null, null); }
    private static void invoke(Object target, String name, Class<?> parameter, Object argument) throws Exception {
        Method method = parameter == null ? ImportIconActivity.class.getDeclaredMethod(name) : ImportIconActivity.class.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        try { if (parameter == null) method.invoke(target); else method.invoke(target, argument); }
        catch (InvocationTargetException error) { throw new Exception(error.getCause()); }
    }
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
}
