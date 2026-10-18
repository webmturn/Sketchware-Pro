package pro.sketchware.core.resources;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

public class StringResourceResolverTest {
    private static final String LATE_APP = "<resources><!-- first --><string name=\"first\">1</string>"
            + "<!-- app --><string name=\"app_name\">App</string>"
            + "<!-- second --><string name=\"second\">2</string><!-- tail --></resources>";

    @Test public void movingAppNameMovesItsOwnCommentAndEarlierRows() {
        StringResourceResolver resolver = new StringResourceResolver();
        ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
        resolver.convertXmlStringsToListMap(LATE_APP, rows);
        assertFalse(resolver.isDataLoadingFailed);
        assertEquals("app_name", rows.get(0).get("key"));
        assertEquals("app", resolver.notesMap.get(0));
        assertEquals("first", resolver.notesMap.get(1));
        assertEquals("second", resolver.notesMap.get(2));
        assertEquals("tail", resolver.notesMap.get(3));
    }

    @Test public void reorderedCommentsSurviveXmlRoundTrip() {
        StringResourceResolver resolver = new StringResourceResolver();
        ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
        resolver.convertXmlStringsToListMap(LATE_APP, rows);
        String xml = resolver.convertListMapToXmlStrings(rows, resolver.notesMap);
        StringResourceResolver again = new StringResourceResolver();
        ArrayList<HashMap<String, Object>> restored = new ArrayList<>();
        again.convertXmlStringsToListMap(xml, restored);
        assertEquals(rows, restored);
        assertEquals(resolver.notesMap, again.notesMap);
        assertEquals("app", again.notesMap.get(0));
    }

    @Test public void emptyPageKeepsMultipleTrailingComments() {
        StringResourceResolver resolver = new StringResourceResolver();
        ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
        resolver.convertXmlStringsToListMap("<resources><!-- end one --><!-- end two --></resources>", rows);
        assertTrue(rows.isEmpty());
        assertEquals("end one\nend two", resolver.notesMap.get(0));
        assertTrue(resolver.convertListMapToXmlStrings(rows, resolver.notesMap).contains("<!-- end two -->"));
    }

    @Test public void appNameWithoutACommentDoesNotStealTheFirstRowsComment() {
        StringResourceResolver resolver = new StringResourceResolver();
        ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
        resolver.convertXmlStringsToListMap("<resources><!-- first --><string name=\"first\">1</string>"
                + "<string name=\"app_name\">App</string><!-- tail --></resources>", rows);
        assertFalse(resolver.notesMap.containsKey(0));
        assertEquals("first", resolver.notesMap.get(1));
        assertEquals("tail", resolver.notesMap.get(2));
    }
}
