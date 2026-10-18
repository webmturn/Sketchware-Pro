package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorLegacyStateTest {
    private static Bundle body(int characters) {
        Bundle pages = new Bundle();
        Bundle page = new Bundle();
        page.putString("entries", "x".repeat(characters));
        page.putString("notes", "{}");
        pages.putBundle("0", page);
        return pages;
    }

    @Test public void smallOriginalInputFitsAlongsideFrameworkState() {
        Bundle state = new Bundle();
        state.putByteArray("framework", new byte[17 * 1024]);
        Bundle original = body(1024);
        assertEquals(0, ResourceEditorLegacyState.writeFallback(state, original, null));
        assertEquals(original.getBundle("0").getString("entries"),
                state.getBundle("resource_editor_drafts").getBundle("0").getString("entries"));
        assertTrue(ResourceEditorLegacyState.parcelSize(state) < 48 * 1024);
    }

    @Test public void combinedBudgetPrioritizesReadableDrafts() {
        Bundle state = new Bundle();
        state.putByteArray("framework", new byte[17 * 1024]);
        int result = ResourceEditorLegacyState.writeFallback(state, body(10 * 1024), body(10 * 1024));
        assertEquals(ResourceEditorLegacyState.CORRUPT_OMITTED, result);
        assertNotNull(state.getBundle("resource_editor_drafts"));
        assertFalse(state.containsKey("resource_legacy_corrupt"));
        assertTrue(ResourceEditorLegacyState.parcelSize(state) < 48 * 1024);
    }

    @Test public void existingFrameworkSizeCountsAgainstFallbackBudget() {
        Bundle state = new Bundle();
        state.putByteArray("framework", new byte[47 * 1024]);
        assertEquals(ResourceEditorLegacyState.DRAFT_OMITTED,
                ResourceEditorLegacyState.writeFallback(state, body(1024), null));
        assertFalse(state.containsKey("resource_editor_drafts"));
    }

    @Test public void oversizedInputsAreNeverTruncatedOrMutated() {
        Bundle original = body(300 * 1024);
        Bundle corrupt = body(300 * 1024);
        Bundle state = new Bundle();
        assertEquals(ResourceEditorLegacyState.DRAFT_OMITTED | ResourceEditorLegacyState.CORRUPT_OMITTED,
                ResourceEditorLegacyState.writeFallback(state, original, corrupt));
        assertTrue(state.isEmpty());
        assertEquals(300 * 1024, original.getBundle("0").getString("entries").length());
        assertEquals(300 * 1024, corrupt.getBundle("0").getString("entries").length());
    }

    @Test public void multipleCorruptPagesShareOneTotalBudget() {
        Bundle corrupt = body(16 * 1024);
        corrupt.putBundle("1", body(16 * 1024).getBundle("0"));
        Bundle state = new Bundle();
        assertEquals(ResourceEditorLegacyState.CORRUPT_OMITTED,
                ResourceEditorLegacyState.writeFallback(state, null, corrupt));
        assertFalse(state.containsKey("resource_legacy_corrupt"));
        assertEquals(2, corrupt.size());
    }
}
