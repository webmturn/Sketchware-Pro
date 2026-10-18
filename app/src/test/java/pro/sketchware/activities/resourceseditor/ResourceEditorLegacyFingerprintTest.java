package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResourceEditorLegacyFingerprintTest {
    @Test public void objectOrderingAndWhitespaceDoNotCreateANewSource() {
        assertEquals(ResourceEditorLegacyXmlBarrier.fingerprint("[{\"text\":\"A\",\"key\":\"k\"}]", "{\"2\":\"tail\",\"0\":\"note\"}"),
                ResourceEditorLegacyXmlBarrier.fingerprint("[ { \"key\":\"k\", \"text\":\"A\" } ]", "{\"0\":\"note\",\"2\":\"tail\"}"));
    }
    @Test public void valuesNotesAndRowOrderBelongToTheSource() {
        String original = ResourceEditorLegacyXmlBarrier.fingerprint("[1,2]", "{}");
        assertNotEquals(original, ResourceEditorLegacyXmlBarrier.fingerprint("[2,1]", "{}"));
        assertNotEquals(original, ResourceEditorLegacyXmlBarrier.fingerprint("[1,3]", "{}"));
        assertNotEquals(original, ResourceEditorLegacyXmlBarrier.fingerprint("[1,2]", "{\"0\":\"note\"}"));
    }
    @Test public void invalidInputsStillHaveStableDistinctDiagnosticIdentities() {
        assertEquals(ResourceEditorLegacyXmlBarrier.fingerprint("broken", null), ResourceEditorLegacyXmlBarrier.fingerprint("broken", null));
        assertNotEquals(ResourceEditorLegacyXmlBarrier.fingerprint("broken", null), ResourceEditorLegacyXmlBarrier.fingerprint("other", null));
        assertNotEquals(ResourceEditorLegacyXmlBarrier.fingerprint(null, "{}"), ResourceEditorLegacyXmlBarrier.fingerprint("null", "{}"));
    }
}
