package pro.sketchware.activities.design;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.color.MaterialColors;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
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

/** Uses the existing owned-file/settings scope; original user files and caches are restored. */
@RunWith(AndroidJUnit4.class)
public class BlockLoaderPaletteTest {
    private static final Gson GSON = new Gson();

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
