package pro.sketchware.core.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import pro.sketchware.beans.RootLayoutBean;

public class RootLayoutStorageCompatibilityTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private static void write(File file, String text) throws IOException {
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static String readText(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void readsLegacyRootLayoutJson() throws Exception {
        File file = folder.newFile("view_root");
        write(file, "{\"main\":{\"class_name\":\"RelativeLayout\","
                + "\"attributes\":{\"android:layout_width\":\"match_parent\",\"app:custom\":\"value\"}}}");

        RootLayoutBean root = new InjectRootLayoutManager(file).getLayoutByFileName("main");

        assertEquals("RelativeLayout", root.getClassName());
        assertEquals("value", root.getAttributes().get("app:custom"));
    }

    @Test public void preservesExistingEntriesAndSerializedKeysOnSave() throws Exception {
        File file = folder.newFile("view_root");
        write(file, "{\"main\":{\"class_name\":\"RelativeLayout\",\"attributes\":{}}}");
        InjectRootLayoutManager manager = new InjectRootLayoutManager(file);
        manager.set("detail", new RootLayoutBean("FrameLayout", Map.of("android:layout_height", "wrap_content")));

        JsonObject data = JsonParser.parseString(readText(file)).getAsJsonObject();
        assertEquals("RelativeLayout", data.getAsJsonObject("main").get("class_name").getAsString());
        JsonObject detail = data.getAsJsonObject("detail");
        assertEquals("FrameLayout", detail.get("class_name").getAsString());
        assertEquals("wrap_content", detail.getAsJsonObject("attributes").get("android:layout_height").getAsString());
        assertFalse(detail.has("className"));
        assertFalse(detail.has("attrs"));
    }

    @Test public void missingLayoutsReceiveIndependentDefaultMetadata() throws Exception {
        InjectRootLayoutManager manager = new InjectRootLayoutManager(new File(folder.getRoot(), "view_root"));
        RootLayoutBean first = manager.getLayoutByFileName("missing");
        first.getAttributes().put("android:orientation", "horizontal");

        RootLayoutBean second = manager.getLayoutByFileName("missing");
        assertEquals("LinearLayout", second.getClassName());
        assertEquals("vertical", second.getAttributes().get("android:orientation"));
    }
}
