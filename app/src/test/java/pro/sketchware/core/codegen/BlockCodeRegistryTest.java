package pro.sketchware.core.codegen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Characterization tests for generated Java snippets.
 *
 * <p>These strings are part of the persisted block model's compatibility contract. Refactors may
 * move the handlers, but must not silently change their output without an explicit migration.</p>
 */
public class BlockCodeRegistryTest {

    @Test
    public void generatesStableMathExpression() {
        assertGenerated("mathPow", "Math.pow(base, exponent)", "base", "exponent");
    }

    @Test
    public void generatesStableMapMutation() {
        assertGenerated("mapPut", "values.put(\"key\", value);", "values", "\"key\"", "value");
    }

    @Test
    public void generatesStableListMutation() {
        assertGenerated("addListStr", "items.add(\"entry\");", "\"entry\"", "items");
    }

    @Test
    public void generatesStableCurrentTimeExpression() {
        assertGenerated("currentTime", "System.currentTimeMillis()");
    }

    @Test
    public void unknownOpcodeHasNoRegisteredGenerator() {
        assertNull(BlockCodeRegistry.get("__unknown_opcode__"));
    }

    private static void assertGenerated(String opcode, String expected, String... params) {
        BlockCodeHandler handler = BlockCodeRegistry.get(opcode);
        assertNotNull("Missing handler for " + opcode, handler);
        assertEquals(expected, handler.generate(null, new ArrayList<>(Arrays.asList(params)), null));
    }
}
