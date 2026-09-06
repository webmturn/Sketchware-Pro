package pro.sketchware.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.util.ArrayList;

import pro.sketchware.beans.BlockBean;

/** Characterizes block JSON written before disabled/collapsed state fields were introduced. */
public class LegacyBlockBeanCompatibilityTest {

    private static final Gson PROJECT_GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Test
    public void readsLegacyBlockWithoutNewUiStateFields() {
        ArrayList<BlockBean> blocks = ProjectDataParser.parseBlockBeans(PROJECT_GSON,
                "{\"color\":-123,\"id\":\"42\",\"nextBlock\":-1,"
                        + "\"opCode\":\"mathPow\",\"parameters\":[\"2\",\"8\"],"
                        + "\"spec\":\"%d ^ %d\",\"subStack1\":-1,\"subStack2\":-1,"
                        + "\"type\":\"d\",\"typeName\":\"\"}");

        assertEquals(1, blocks.size());
        BlockBean block = blocks.get(0);
        assertEquals("42", block.id);
        assertEquals("mathPow", block.opCode);
        assertEquals(2, block.parameters.size());
        assertEquals("2", block.parameters.get(0));
        assertEquals("8", block.parameters.get(1));
        assertFalse(block.disabled);
        assertFalse(block.collapsed);
        assertFalse(block.collapsed2);
    }

    @Test
    public void readsLegacyLineDelimitedBlockStreamInOrder() {
        String first = "{\"id\":\"1\",\"opCode\":\"currentTime\",\"parameters\":[],"
                + "\"spec\":\"current time\",\"type\":\"d\",\"typeName\":\"\"}";
        String second = "{\"id\":\"2\",\"opCode\":\"mapSize\",\"parameters\":[\"values\"],"
                + "\"spec\":\"map size\",\"type\":\"d\",\"typeName\":\"\"}";

        ArrayList<BlockBean> blocks = ProjectDataParser.parseBlockBeans(
                PROJECT_GSON, first + System.lineSeparator() + second);

        assertEquals(2, blocks.size());
        assertEquals("1", blocks.get(0).id);
        assertEquals("2", blocks.get(1).id);
    }
}
