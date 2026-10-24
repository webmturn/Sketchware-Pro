package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.color.MaterialColors;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.SketchApplication;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.codegen.ExtraBlockFile;
import pro.sketchware.activities.editor.block.BlocksManager;

/** Uses the existing owned-file/settings scope; original user files and caches are restored. */
@RunWith(AndroidJUnit4.class)
public class BlockLoaderPaletteTest {
    private static final Gson GSON = new Gson();

    @Test public void unchangedContentReusesPublishedObjects() throws Exception {
        try (Dataset data = new Dataset()) {
            var block = data.block("unchanged", 9); block.put("color", "#1976D2");
            data.load(List.of(palette("First", "#1976D2")), List.of(block));
            ExtraBlockInfo original = data.info("unchanged");
            for (int i = 0; i < 3; i++) {
                refreshIfChanged();
                assertSame("Unchanged sources should not rebuild every block", original, data.info("unchanged"));
            }
            assertTrue(data.fixture.blocks.setLastModified(data.fixture.blocks.lastModified() + 10000));
            refreshIfChanged();
            assertSame("Timestamp changes alone should not rebuild identical content", original, data.info("unchanged"));
        }
    }

    @Test(timeout = 60000) public void stoppingManagerReusesUnchangedCacheAndObservesExternalChanges() throws Exception {
        try (Dataset data = new Dataset()) {
            var block = data.block("stop", 9); block.put("code", "FIRST");
            data.load(List.of(palette("First", "#1976D2")), List.of(block));
            ExtraBlockInfo first = data.info("stop");
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), BlocksManager.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try (ActivityScenario<BlocksManager> manager = ActivityScenario.launch(intent)) { manager.onActivity(a -> {}); }
            assertSame(first, data.info("stop"));
            long stamp = data.fixture.blocks.lastModified();
            block.put("code", "LATER");
            Files.write(data.fixture.blocks.toPath(), GSON.toJson(List.of(block)).getBytes(StandardCharsets.UTF_8));
            assertTrue(data.fixture.blocks.setLastModified(stamp));
            try (ActivityScenario<BlocksManager> manager = ActivityScenario.launch(intent)) { manager.onActivity(a -> {}); }
            assertEquals("LATER", data.info("stop").getCode());
        }
    }

    @Test public void sameStampExternalChangesRefreshBlocksAndPaletteColors() throws Exception {
        try (Dataset data = new Dataset()) {
            var block = data.block("external", 9); block.put("code", "FIRST");
            data.load(List.of(palette("First", "#1976D2")), List.of(block));
            ExtraBlockInfo original = data.info("external");
            long size = data.fixture.blocks.length(), stamp = data.fixture.blocks.lastModified();
            block.put("code", "LATER");
            Files.write(data.fixture.blocks.toPath(), GSON.toJson(List.of(block)).getBytes(StandardCharsets.UTF_8));
            assertEquals(size, data.fixture.blocks.length()); assertTrue(data.fixture.blocks.setLastModified(stamp));
            refreshIfChanged();
            assertNotSame(original, data.info("external")); assertEquals("LATER", data.info("external").getCode());
            stamp = data.fixture.palettes.lastModified(); size = data.fixture.palettes.length();
            Files.write(data.fixture.palettes.toPath(), GSON.toJson(List.of(palette("First", "#E91E63"))).getBytes(StandardCharsets.UTF_8));
            assertEquals(size, data.fixture.palettes.length()); assertTrue(data.fixture.palettes.setLastModified(stamp));
            refreshIfChanged();
            assertEquals(0xffe91e63, data.info("external").getPaletteColor());
        }
    }

    @Test public void changedConfigurationAndCacheInvalidationCannotReuseOldObjects() throws Exception {
        try (Dataset data = new Dataset()) {
            data.load(List.of(palette("First", "#1976D2")), List.of(data.block("config", 9)));
            ExtraBlockInfo first = data.info("config");
            File otherBlocks = new File(data.fixture.blocks.getParentFile(), "other-blocks.json");
            File otherPalettes = new File(data.fixture.palettes.getParentFile(), "other-palettes.json");
            Files.copy(data.fixture.blocks.toPath(), otherBlocks.toPath());
            Files.copy(data.fixture.palettes.toPath(), otherPalettes.toPath());
            data.fixture.configure(otherBlocks, otherPalettes);
            refreshIfChanged(); assertNotSame(first, data.info("config"));
            ExtraBlockInfo second = data.info("config");
            ExtraBlockFile.invalidateCache(); refreshIfChanged();
            assertNotSame(second, data.info("config"));
        }
    }

    @Test public void deletingExternalSourceRemovesItsDefinitions() throws Exception {
        try (Dataset data = new Dataset()) {
            data.load(List.of(palette("First", "#1976D2")), List.of(data.block("deleted", 9)));
            assertFalse(data.info("deleted").isMissing);
            assertTrue(data.fixture.blocks.delete()); refreshIfChanged();
            assertTrue(BlockLoader.getBlockInfo(data.name("deleted")).isMissing);
        }
    }

    @Test public void unchangedGlobalRefreshStillInvalidatesProjectDefinitions() throws Exception {
        try (Dataset data = new Dataset()) {
            data.load(List.of(palette("First", "#1976D2")), List.of(data.block("global", 9)));
            String projectId = data.fixture.project.id();
            ExtraBlockInfo first = BlockLoader.getBlockFromProject(projectId, data.fixture.name);
            assertFalse(first.isMissing);
            first = new ExtraBlockInfo(); first.setName(data.fixture.name); first.setCode("PROJECT_UPDATED");
            Files.write(new File(pro.sketchware.core.project.SketchwarePaths.getProjectCustomBlocksPath(projectId)).toPath(),
                    GSON.toJson(List.of(first)).getBytes(StandardCharsets.UTF_8));
            refreshIfChanged();
            assertEquals("PROJECT_UPDATED", BlockLoader.getBlockFromProject(projectId, data.fixture.name).getCode());
        }
    }

    @Test public void distinctColorsAndTemplatesBeyondMemoCapacityRemainCorrect() throws Exception {
        try (Dataset data = new Dataset()) {
            ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
            for (int i = 0; i < 513; i++) {
                var block = data.block("unique" + i, 9);
                block.put("color", String.format("#%08X", 0xff000000 | i * 19997));
                block.put("spec", "Unique " + i + " %s.argument");
                block.put("code", "consume" + i + "(%s);"); definitions.add(block);
            }
            data.load(List.of(palette("First", "#1976D2")), definitions);
            for (int i = 0; i < definitions.size(); i++) {
                var block = definitions.get(i); ExtraBlockInfo actual = data.info("unique" + i);
                assertEquals(MaterialColors.harmonizeWithPrimary(themedContext(), Color.parseColor((String) block.get("color"))), actual.getColor());
                assertEquals(block.get("code"), actual.getCode()); assertEquals(block.get("spec"), actual.getSpec());
            }
        }
    }

    // Reflection keeps these regression cases runnable against the preserved pre-fix APK.
    private static void refreshIfChanged() throws Exception {
        try { BlockLoader.class.getMethod("refreshIfChanged").invoke(null); }
        catch (NoSuchMethodException beforeFix) { BlockLoader.refresh(); }
    }

    @Test public void paletteIdsKeepNullSlotsFallbacksAndTransparentColors() throws Exception {
        try (Dataset data = new Dataset()) {
            ArrayList<HashMap<String, Object>> palettes = new ArrayList<>();
            palettes.add(null);
            palettes.add(palette("Blue", "#1976D2"));
            palettes.add(palette("Transparent", "#00000000"));
            palettes.add(new HashMap<>()); // Hidden record still occupies its original slot.
            palettes.add(palette("Fallback", "invalid-color"));
            ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
            for (int id : new int[]{9, 10, 11, 12, 13, -1, 9999}) definitions.add(data.block("p" + id, id));
            data.load(palettes, definitions);
            int[] expected = {0, 0xff1976d2, 0, 0, 0xff8a55d7, 0, 0};
            for (int i = 0; i < definitions.size(); i++) {
                ExtraBlockInfo info = data.info((String) definitions.get(i).get("name"));
                assertEquals(0, info.getColor());
                assertEquals(expected[i], info.getPaletteColor());
                assertEquals("probe", info.getSpec());
                assertEquals("else probe", info.getSpec2());
                assertEquals("java.util.Locale", info.getImports());
            }
        }
    }

    @Test public void explicitColorsMatchThePreviousMaterialColorOperationExactly() throws Exception {
        try (Dataset data = new Dataset()) {
            String[] colors = {"#1976D2", "#FF1976D2", "#00000000", "#80112233", "red", "black", "white", "#00FFFFFF"};
            ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
            for (int i = 0; i < colors.length; i++) {
                HashMap<String, Object> block = data.block("own" + i, 9);
                block.put("color", colors[i]);
                definitions.add(block);
            }
            data.load(List.of(palette("Palette", "#E91E63")), definitions);
            Context context = themedContext();
            for (int i = 0; i < colors.length; i++) {
                ExtraBlockInfo info = data.info("own" + i);
                assertEquals(colors[i], MaterialColors.harmonizeWithPrimary(context, Color.parseColor(colors[i])), info.getColor());
                assertEquals("Explicit color keeps precedence over palette inheritance", 0, info.getPaletteColor());
            }
        }
    }

    @Test public void invalidDefinitionsAndMissingPaletteIdsKeepTheirExistingBehavior() throws Exception {
        try (Dataset data = new Dataset()) {
            var invalidColor = data.block("badColor", 9); invalidColor.put("color", "not-a-color");
            var invalidNumber = data.block("badNumber", 9); invalidNumber.put("palette", "9.5");
            var invalidType = data.block("badType", 9); invalidType.put("palette", 9);
            var missing = data.block("missing", 9); missing.remove("palette");
            var malformedColorType = data.block("legacyColorType", 9); malformedColorType.put("color", 17);
            var unknown = data.block("unknown", 123456);
            var invalidName = data.block("invalidName", 9); invalidName.put("name", 17);
            var noName = data.block("noName", 9); noName.remove("name");
            data.load(List.of(palette("Palette", "#1976D2")), List.of(
                    invalidColor, invalidNumber, invalidType, missing, malformedColorType, unknown, invalidName, noName));
            for (String suffix : List.of("badColor", "badNumber", "badType", "missing", "invalidName", "noName")) {
                assertTrue(suffix, BlockLoader.getBlockInfo(data.name(suffix)).isMissing);
            }
            assertEquals(0, data.info("legacyColorType").getColor());
            assertEquals(0, data.info("legacyColorType").getPaletteColor());
            assertEquals(0, data.info("unknown").getPaletteColor());
        }
    }

    @Test public void refreshRebuildsPaletteIndexAndPreservesCodeAndImports() throws Exception {
        try (Dataset data = new Dataset()) {
            HashMap<String, Object> block = data.block("refresh", 9);
            block.put("code", "System.out.println(\"FIRST\");");
            data.load(List.of(palette("First", "#1976D2")), List.of(block));
            assertEquals(0xff1976d2, data.info("refresh").getPaletteColor());
            block.put("code", "System.out.println(\"SECOND\");");
            data.load(List.of(palette("Second", "#E91E63")), List.of(block));
            ExtraBlockInfo updated = data.info("refresh");
            assertEquals(0xffe91e63, updated.getPaletteColor());
            assertEquals(block.get("code"), updated.getCode());
            assertEquals("java.util.Locale", updated.getImports());
            assertEquals("probe", updated.getSpec());
        }
    }

    @Test public void duplicateNamesKeepListOrderAndLastDefinitionLookup() throws Exception {
        try (Dataset data = new Dataset()) {
            HashMap<String, Object> first = data.block("duplicate", 9); first.put("code", "FIRST");
            HashMap<String, Object> second = data.block("duplicate", 10); second.put("code", "SECOND");
            data.load(List.of(palette("First", "#1976D2"), palette("Second", "#E91E63")), List.of(first, second));
            assertEquals("SECOND", data.info("duplicate").getCode());
            assertEquals(0xffe91e63, data.info("duplicate").getPaletteColor());
            List<String> codes = new ArrayList<>();
            for (ExtraBlockInfo info : loaded()) if (data.name("duplicate").equals(info.getName())) codes.add(info.getCode());
            assertEquals(List.of("FIRST", "SECOND"), codes);
        }
    }

    @Test(timeout = 60000) public void thousandsOfDefinitionsAreLoadedWithoutAnyQuantityCutoff() throws Exception {
        try (Dataset data = new Dataset()) {
            ArrayList<HashMap<String, Object>> palettes = palettes(512);
            palettes.set(7, null);
            List<HashMap<String, Object>> definitions = data.definitions(10000, 512, false);
            data.load(palettes, definitions);
            assertEquals(definitions.size() + ExtraBlockFile.buildInBlocks.size(), loaded().size());
            for (int i = 0; i < definitions.size(); i++) {
                int position = i % 512;
                assertEquals(position == 7 ? 0 : paletteColor(position), data.info("b" + i).getPaletteColor());
            }
            assertEquals("java.util.Locale", data.info("b9999").getImports());
        }
    }

    /** Timings are observations, not a flaky speed assertion; both workloads use the real refresh path. */
    @Test(timeout = 120000) public void warmCacheLoadBenchmark() throws Exception {
        try (Dataset data = new Dataset()) {
            for (boolean explicit : new boolean[]{false, true}) {
                int count = explicit ? 2000 : 8000;
                int paletteCount = 800;
                data.load(palettes(paletteCount), data.definitions(count, paletteCount, explicit));
                long[] samples = new long[9];
                for (int round = -2; round < samples.length; round++) {
                    long start = System.nanoTime();
                    BlockLoader.refresh();
                    long elapsed = System.nanoTime() - start;
                    if (round >= 0) samples[round] = elapsed;
                }
                assertEquals(count + ExtraBlockFile.buildInBlocks.size(), loaded().size());
                for (int i = 0; i < count; i++) assertFalse(data.info("b" + i).isMissing);
                String workload = explicit ? "explicit_color" : "palette_lookup";
                long[] sorted = samples.clone(); Arrays.sort(sorted);
                Bundle result = new Bundle();
                result.putString(workload + "_workload", count + " custom blocks + built-ins, " + paletteCount + " palettes, warm file caches, 2 warmups + 9 samples");
                result.putString(workload + "_samples_ns", Arrays.toString(samples));
                result.putString(workload + "_median_ms", Double.toString(sorted[4] / 1_000_000.0));
                InstrumentationRegistry.getInstrumentation().sendStatus(0, result);
            }
        }
    }

    private static final class Dataset implements AutoCloseable {
        final CustomBlockPaletteConfigurationTest.Fixture fixture = new CustomBlockPaletteConfigurationTest.Fixture();
        Dataset() throws Exception {}
        String name(String suffix) { return fixture.name + "_" + suffix; }
        HashMap<String, Object> block(String suffix, int palette) {
            HashMap<String, Object> block = new HashMap<>();
            block.put("name", name(suffix)); block.put("palette", Integer.toString(palette));
            block.put("spec", "probe"); block.put("spec2", "else probe"); block.put("code", "");
            block.put("imports", "java.util.Locale"); block.put("type", " ");
            return block;
        }
        List<HashMap<String, Object>> definitions(int count, int paletteCount, boolean explicit) {
            ArrayList<HashMap<String, Object>> definitions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                HashMap<String, Object> block = block("b" + i, 9 + i % paletteCount);
                if (explicit) block.put("color", (i & 1) == 0 ? "#1976D2" : "#E91E63");
                definitions.add(block);
            }
            return definitions;
        }
        void load(List<HashMap<String, Object>> palettes, List<HashMap<String, Object>> definitions) throws Exception {
            Files.write(fixture.palettes.toPath(), GSON.toJson(palettes).getBytes(StandardCharsets.UTF_8));
            Files.write(fixture.blocks.toPath(), GSON.toJson(definitions).getBytes(StandardCharsets.UTF_8));
            ExtraBlockFile.invalidateCache();
            BlockLoader.refresh();
        }
        ExtraBlockInfo info(String suffixOrName) {
            ExtraBlockInfo info = BlockLoader.getBlockInfo(suffixOrName.startsWith(fixture.name) ? suffixOrName : name(suffixOrName));
            assertFalse("Definition must remain loaded: " + suffixOrName, info.isMissing);
            return info;
        }
        @Override public void close() throws Exception { fixture.close(); }
    }

    private static ArrayList<HashMap<String, Object>> palettes(int count) {
        ArrayList<HashMap<String, Object>> palettes = new ArrayList<>();
        for (int i = 0; i < count; i++) palettes.add(palette("Palette " + i, String.format("#%08X", paletteColor(i))));
        return palettes;
    }
    private static int paletteColor(int index) { return Color.rgb(index & 255, index * 3 & 255, index * 7 & 255); }
    private static HashMap<String, Object> palette(String name, String color) {
        HashMap<String, Object> palette = new HashMap<>(); palette.put("name", name); palette.put("color", color); return palette;
    }
    @SuppressWarnings("unchecked") private static List<ExtraBlockInfo> loaded() throws Exception {
        Field field = BlockLoader.class.getDeclaredField("blocks"); field.setAccessible(true);
        return (List<ExtraBlockInfo>) field.get(null);
    }
    private static Context themedContext() {
        return new ContextThemeWrapper(SketchApplication.getContext(), R.style.Theme_SketchwarePro);
    }
}
