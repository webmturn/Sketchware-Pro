package pro.sketchware.core.block;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

/** The optional imports field must not change legacy definition fields or template text. */
public class ExtraBlockInfoCompatibilityTest {
    private static final Gson GSON = new Gson();
    private static final String LEGACY = "{\"name\":\"legacy\",\"spec\":\"show %s.value\","
            + "\"spec2\":\"otherwise\",\"code\":\"call(%1$s); // %%\",\"color\":123,\"paletteColor\":456}";

    @Test public void oldDefinitionWithoutImportsRoundTripsUnchanged() {
        ExtraBlockInfo info = GSON.fromJson(LEGACY, ExtraBlockInfo.class);
        assertEquals("", info.getImports());
        assertEquals(GSON.fromJson(LEGACY, JsonObject.class), GSON.toJsonTree(info));
    }

    @Test public void explicitNullImportsRemainOptionalAndSafe() {
        JsonObject json = GSON.fromJson(LEGACY, JsonObject.class);
        json.add("imports", null);
        ExtraBlockInfo info = GSON.fromJson(json, ExtraBlockInfo.class);
        assertEquals("", info.getImports());
        assertEquals(GSON.fromJson(LEGACY, JsonObject.class), GSON.toJsonTree(info));
    }

    @Test public void multilineImportsAndAllLegacyFieldsArePreserved() {
        String imports = "java.security.MessageDigest\r\n\nstatic java.util.Collections.*\n";
        JsonObject json = GSON.fromJson(LEGACY, JsonObject.class);
        json.addProperty("imports", imports);
        ExtraBlockInfo info = GSON.fromJson(json, ExtraBlockInfo.class);
        ExtraBlockInfo reread = GSON.fromJson(GSON.toJson(info), ExtraBlockInfo.class);
        assertEquals(imports, reread.getImports());
        assertEquals(json, GSON.toJsonTree(reread));
    }

    @Test public void explicitlyEmptyImportsRoundTripWithoutChangingCode() {
        ExtraBlockInfo info = GSON.fromJson(LEGACY, ExtraBlockInfo.class);
        info.setImports("");
        JsonObject expected = GSON.fromJson(LEGACY, JsonObject.class);
        expected.addProperty("imports", "");
        assertEquals(expected, GSON.toJsonTree(GSON.fromJson(GSON.toJson(info), ExtraBlockInfo.class)));
    }
}
