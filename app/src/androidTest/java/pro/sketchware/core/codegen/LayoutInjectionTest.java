package pro.sketchware.core.codegen;

import static org.junit.Assert.*;

import android.os.Bundle;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.beans.ViewBean;
import pro.sketchware.core.project.BuildConfig;

/** Exercises the actual generator without writing or registering a project. */
@RunWith(AndroidJUnit4.class)
public class LayoutInjectionTest {
    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    @Test public void widgetWithoutInjection() throws Exception { check(false, "empty", "", Map.of()); }
    @Test public void fabWithoutInjection() throws Exception { check(true, "empty", "", Map.of()); }

    @Test public void widgetCompactAttribute() throws Exception { compact(false); }
    @Test public void fabCompactAttribute() throws Exception { compact(true); }

    @Test public void widgetValueWithSpaces() throws Exception { spaced(false); }
    @Test public void fabValueWithSpaces() throws Exception { spaced(true); }

    @Test public void widgetSameLineAttributes() throws Exception { sameLine(false); }
    @Test public void fabSameLineAttributes() throws Exception { sameLine(true); }

    @Test public void widgetMultilineAttributes() throws Exception { multiline(false); }
    @Test public void fabMultilineAttributes() throws Exception { multiline(true); }

    @Test public void widgetSingleQuotesAndEntity() throws Exception { quoted(false); }
    @Test public void fabSingleQuotesAndEntity() throws Exception { quoted(true); }

    @Test public void widgetDimensionOverrides() throws Exception { overrides(false); }
    @Test public void fabDimensionOverrides() throws Exception { overrides(true); }

    private static void compact(boolean fab) throws Exception {
        check(fab, "compact", " android:contentDescription=\"Compact\"",
                Map.of("contentDescription", "Compact"));
    }

    private static void spaced(boolean fab) throws Exception {
        check(fab, "spaced", " android:contentDescription=\"Add new item\"",
                Map.of("contentDescription", "Add new item"));
    }

    private static void sameLine(boolean fab) throws Exception {
        check(fab, "same_line", " android:contentDescription=\"Add new item\" android:tag=\"two word tag\"",
                Map.of("contentDescription", "Add new item", "tag", "two word tag"));
    }

    private static void multiline(boolean fab) throws Exception {
        check(fab, "multiline", "\n android:contentDescription=\"Add new item\"\n android:tag=\"two word tag\"",
                Map.of("contentDescription", "Add new item", "tag", "two word tag"));
    }

    private static void quoted(boolean fab) throws Exception {
        check(fab, "quoted", " android:contentDescription='Left &amp; Right'\tandroid:tag='two  spaces'",
                Map.of("contentDescription", "Left & Right", "tag", "two  spaces"));
    }

    private static void overrides(boolean fab) throws Exception {
        check(fab, "overrides", " android:layout_width = \"96dp\" tools:replace=\"android:layout_height\""
                        + " android:layout_height=\"48dp\" android:contentDescription=\"Wide action\"",
                Map.of("layout_width", "96dp", "layout_height", "48dp", "contentDescription", "Wide action"));
    }

    private static void check(boolean fab, String name, String injection, Map<String, String> expected) throws Exception {
        BuildConfig config = new BuildConfig();
        config.sc_id = "F15Injection." + UUID.randomUUID();
        config.isAppCompatEnabled = fab;
        ProjectFileBean file = new ProjectFileBean(fab ? ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY
                : ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW, "f15_test");
        file.options = fab ? ProjectFileBean.OPTION_ACTIVITY_FAB : 0;
        ViewBean subject = new ViewBean(fab ? "_fab" : "subject", fab ? ViewBean.VIEW_TYPE_WIDGET_FAB
                : ViewBean.VIEW_TYPE_LAYOUT_LINEAR);
        subject.parent = "root";
        subject.inject = injection;
        LayoutGenerator generator = new LayoutGenerator(config, file);
        ArrayList<ViewBean> views = new ArrayList<>();
        if (fab) {
            generator.setViews(views, subject);
        } else {
            views.add(subject);
            generator.setViews(views);
        }
        String xml = generator.toXmlString();
        Bundle result = new Bundle();
        result.putString("f15_case", (fab ? "fab_" : "widget_") + name);
        result.putString("f15_xml_base64", Base64.encodeToString(xml.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        InstrumentationRegistry.getInstrumentation().sendStatus(0, result);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        Map<String, String> attributes = new HashMap<>();
        int matches = 0;
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.START_TAG
                    && ("@+id/" + subject.id).equals(parser.getAttributeValue(ANDROID, "id"))) {
                matches++;
                assertEquals(fab ? "com.google.android.material.floatingactionbutton.FloatingActionButton"
                        : "LinearLayout", parser.getName());
                for (int i = 0; i < parser.getAttributeCount(); i++) {
                    if (ANDROID.equals(parser.getAttributeNamespace(i))) {
                        String key = parser.getAttributeName(i);
                        assertNull("Duplicate android:" + key, attributes.put(key, parser.getAttributeValue(i)));
                    }
                }
                if (name.equals("overrides")) {
                    assertEquals("android:layout_height",
                            parser.getAttributeValue("http://schemas.android.com/tools", "replace"));
                }
            }
            parser.next();
        }
        assertEquals("Generated subject missing or duplicated", 1, matches);
        assertTrue(attributes.containsKey("layout_width"));
        assertTrue(attributes.containsKey("layout_height"));
        for (Map.Entry<String, String> attribute : expected.entrySet()) {
            assertEquals(attribute.getKey(), attribute.getValue(), attributes.get(attribute.getKey()));
        }
        if (expected.isEmpty()) {
            assertFalse(attributes.containsKey("contentDescription"));
            assertFalse(attributes.containsKey("tag"));
        }
    }
}
