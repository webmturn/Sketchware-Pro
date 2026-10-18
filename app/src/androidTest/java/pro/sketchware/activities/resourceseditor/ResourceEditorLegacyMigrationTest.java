package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import pro.sketchware.core.resources.ColorModel;

/**
 * Task 2 storage-level regression: legacy Bundle import is all-or-nothing, never overrides a newer
 * draft or a proven XML commit, and keeps unreadable pages separate from the readable ones.
 */
@RunWith(AndroidJUnit4.class)
public class ResourceEditorLegacyMigrationTest {
    private File root;
    private String session;
    private ResourceEditorDraftController.Gate gate;
    private String failingPoint;
    private ResourceEditorDraftStore store;
    private File stringsXml;

    @Before public void setUp() throws Exception {
        root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "legacy-migration-test-" + UUID.randomUUID());
        File values = new File(root, "project-xml");
        assertTrue(values.mkdirs());
        stringsXml = new File(values, "strings.xml");
        Files.write(stringsXml.toPath(), "<resources><string name=\"probe\">base</string></resources>"
                .getBytes(StandardCharsets.UTF_8));
        session = UUID.randomUUID().toString();
        gate = new ResourceEditorDraftController.Gate();
        gate.owner = 1;
        store = create();
        store.open(false);
    }

    @After public void tearDown() throws Exception {
        ResourceEditorDraftStore.testFault = point -> { };
        remove(root);
    }

    private static void remove(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) remove(child);
        if (file.exists() && !file.delete()) throw new IOException("Cannot remove owned fixture");
    }

    private ResourceEditorDraftStore create() throws IOException {
        return new ResourceEditorDraftStore(root, session, "project", "-fr", gate, gate.owner,
                point -> { if (point.equals(failingPoint)) throw new IOException("Injected " + point); },
                new String[]{stringsXml.getAbsolutePath(), null, null, null, null, null});
    }

    private static ResourceEditorDraftState.Snapshot strings(String text) {
        return ResourceEditorDraftState.snapshot(
                List.of(new HashMap<>(Map.of("key", "probe", "text", text))), Map.of(0, "note"));
    }

    private static Bundle page(List<?> entries, Map<Integer, String> notes) {
        return ResourceEditorDraftState.capture(entries, notes);
    }

    private void commitString(long revision, String value) throws IOException {
        long[] revisions = new long[ResourceEditorTabs.COUNT];
        revisions[ResourceEditorTabs.STRINGS] = revision;
        boolean[] dirty = new boolean[ResourceEditorTabs.COUNT];
        dirty[ResourceEditorTabs.STRINGS] = true;
        store.commit(revisions, dirty, Map.of(ResourceEditorTabs.STRINGS, strings(value)));
    }

    private ResourceEditorDraftState.Snapshot[] reopen() throws IOException {
        ResourceEditorDraftStore reopened = create();
        return reopened.open(true);
    }

    private static String text(ResourceEditorDraftState.Snapshot snapshot) {
        return (String) ((Map<?, ?>) snapshot.entries.get(0)).get("text");
    }

    @Test public void migratesModelsNotesAndEmptyDirtyPage() throws Exception {
        byte[] originalXml = Files.readAllBytes(stringsXml.toPath());
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "legacy", "text", "saved"))),
                Map.of(0, "legacy note")));
        legacy.putBundle("1", page(List.of(new ColorModel("legacy_color", "#123456")), Map.of()));
        legacy.putBundle("5", page(List.of(), Map.of()));

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED, result.state);
        assertTrue(result.corrupt.isEmpty());
        assertEquals(3, result.pages.size());
        assertEquals(1, store.generation());

        ResourceEditorDraftState.Snapshot[] pages = reopen();
        assertEquals(ResourceEditorTabs.COUNT, pages.length);
        assertEquals("saved", text(pages[0]));
        assertEquals("legacy note", pages[0].notes.get(0));
        assertEquals("#123456", ((ColorModel) pages[1].entries.get(0)).getColorValue());
        assertNotNull(pages[5]);
        assertTrue(pages[5].entries.isEmpty());
        assertTrue(pages[5].notes.isEmpty());
        assertArrayEquals("legacy import must not touch project XML", originalXml,
                Files.readAllBytes(stringsXml.toPath()));
    }

    @Test public void newerDraftWinsPerPageAndOtherLegacyPageMigrates() throws Exception {
        commitString(2, "newer draft");
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "probe", "text", "older"))), Map.of()));
        legacy.putBundle("1", page(List.of(new ColorModel("legacy_color", "#445566")), Map.of()));

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED, result.state);
        assertFalse("the page with a newer draft must not be rewritten", result.pages.containsKey(0));

        ResourceEditorDraftState.Snapshot[] pages = reopen();
        assertEquals("newer draft", text(pages[0]));
        assertEquals("#445566", ((ColorModel) pages[1].entries.get(0)).getColorValue());
    }

    @Test public void xmlCommitDefersLegacyImportWithoutChangingGenerationOrIntent() throws Exception {
        commitString(1, "draft A");
        byte[] xml = "<resources><string name=\"probe\">saved B</string></resources>"
                .getBytes(StandardCharsets.UTF_8);
        String hash = digest(xml);
        assertTrue(store.writeXmlCommitIntent(Map.of(ResourceEditorTabs.STRINGS, hash)));
        Files.write(stringsXml.toPath(), xml);

        store = create();
        ResourceEditorDraftState.Snapshot[] pages = store.open(true);
        assertNull("XML B must win over draft A", pages[0]);
        long generation = store.generation();
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "probe", "text", "legacy A"))), Map.of()));
        legacy.putBundle("1", page(List.of(new ColorModel("legacy_color", "#778899")), Map.of()));

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_DEFERRED, result.state);
        assertEquals("a deferred import must not commit", generation, store.generation());
        assertTrue(store.wasXmlCommitted(ResourceEditorTabs.STRINGS));
        assertNull(reopen()[0]);
    }

    @Test public void unreadablePageIsKeptWhileReadablePagesMigrate() throws Exception {
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "legacy", "text", "ok"))),
                Map.of(0, "note")));
        Bundle corrupt = new Bundle();
        corrupt.putString("entries", "not json");
        corrupt.putString("notes", "{}");
        legacy.putBundle("1", corrupt);

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_PARTIAL, result.state);
        assertTrue(result.corrupt.containsKey("1"));
        assertEquals("not json", result.corrupt.getBundle("1").getString("entries"));

        ResourceEditorDraftState.Snapshot[] pages = reopen();
        assertEquals("ok", text(pages[0]));
        assertNull("the unreadable page must not be applied", pages[1]);
    }

    @Test public void failedCommitKeepsSourceAndRetrySucceeds() throws Exception {
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "legacy", "text", "retry me"))), Map.of()));
        failingPoint = "before_manifest";

        ResourceEditorDraftStore.LegacyImport failed = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_FAILED, failed.state);
        assertEquals(0, store.generation());
        assertFalse("a failed commit must not publish a manifest",
                new File(store.directory, "manifest.json").exists());

        failingPoint = null;
        ResourceEditorDraftStore.LegacyImport retried = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED, retried.state);
        ResourceEditorDraftState.Snapshot[] pages = reopen();
        assertEquals("retry me", text(pages[0]));
    }

    @Test public void allPagesSupersededDoesNotCommit() throws Exception {
        commitString(1, "draft");
        long generation = store.generation();
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "probe", "text", "old"))), Map.of()));

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_SUPERSEDED, result.state);
        assertEquals(generation, store.generation());
        assertEquals("draft", text(reopen()[0]));
    }

    @Test public void controllerRetriesLegacyBeforeAnyEditorLoads() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "retry", "text", "value"))),
                Map.of(0, "note")));
        ResourceEditorDraftController controller = null;
        ResourceEditorDraftController reopened = null;
        File directory = null;
        try {
            ResourceEditorDraftStore.testFault = point -> {
                if ("before_manifest".equals(point)) throw new RuntimeException("injected migration failure");
            };
            controller = new ResourceEditorDraftController(context, "project", "-fr", null, null, legacy);
            String session = controller.session;
            directory = new File(context.getNoBackupFilesDir(), "resource-editor-drafts/" + session);
            ResourceEditorDraftController.Loaded loaded = controller.opening.join();
            assertEquals(ResourceEditorDraftStore.LEGACY_FAILED, loaded.legacy.state);
            assertNull(loaded.pages[0]);

            ResourceEditorDraftStore.testFault = point -> { };
            ResourceEditorDraftStore.LegacyImport retried = controller.retryLegacy(legacy);
            assertNotNull("retry must return a result", retried);
            assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED, retried.state);
            assertNotNull("a successful retry fills the page before the UI load", loaded.pages[0]);
            assertEquals("value", text(loaded.pages[0]));
            assertEquals("note", loaded.pages[0].notes.get(0));

            controller.detach(false);
            controller = null;
            reopened = new ResourceEditorDraftController(context, "project", "-fr", session, null, null);
            ResourceEditorDraftController.Loaded restored = reopened.opening.join();
            assertNotNull(restored.pages[0]);
            assertEquals("value", text(restored.pages[0]));
            assertEquals("note", restored.pages[0].notes.get(0));
        } finally {
            ResourceEditorDraftStore.testFault = point -> { };
            if (controller != null) controller.detach(false);
            if (reopened != null) reopened.detach(false);
            if (directory != null) remove(directory);
        }
    }

    @Test public void replacedOwnerCannotCommitALateMigration() throws Exception {
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "legacy", "text", "late"))), Map.of()));
        gate.owner = 2; // the store still owns generation 1

        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_FAILED, result.state);
        assertEquals(0, store.generation());
        assertFalse(new File(store.directory, "manifest.json").exists());
    }

    @Test public void emptyBundleIsNoOp() throws Exception {
        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(new Bundle());
        assertEquals(ResourceEditorDraftStore.LEGACY_NONE, result.state);
        assertEquals(0, store.generation());
        assertTrue(result.corrupt.isEmpty());
    }

    @Test public void wrongStringAttributeTypeIsIsolatedBeforeEncoding() throws Exception {
        Bundle legacy = new Bundle();
        Bundle bad = new Bundle();
        bad.putString("entries", "[{\"key\":\"bad\",\"text\":{\"nested\":\"value\"}}]");
        bad.putString("notes", "{}");
        legacy.putBundle("0", bad);
        legacy.putBundle("1", page(List.of(new ColorModel("valid_color", "#123456")), Map.of()));
        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_PARTIAL, result.state);
        assertTrue(result.corrupt.containsKey("0"));
        assertEquals("#123456", ((ColorModel) reopen()[1].entries.get(0)).getColorValue());
        assertNull(reopen()[0]);
    }

    @Test public void missingModelFieldsAreIsolatedPerPage() throws Exception {
        Bundle legacy = new Bundle();
        legacy.putBundle("0", page(List.of(new HashMap<>(Map.of("key", "valid", "text", "ok"))), Map.of()));
        for (String position : List.of("1", "2", "3", "4", "5")) {
            Bundle bad = new Bundle();
            bad.putString("entries", "[{}]");
            bad.putString("notes", "{}");
            legacy.putBundle(position, bad);
        }
        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_PARTIAL, result.state);
        assertEquals(5, result.corrupt.size());
        assertEquals("ok", text(reopen()[0]));
    }

    @Test public void supersededMalformedPageDoesNotBecomeCorruptMaterial() throws Exception {
        commitString(2, "newer");
        Bundle legacy = new Bundle();
        Bundle bad = new Bundle();
        bad.putString("entries", "not json");
        legacy.putBundle("0", bad);
        long generation = store.generation();
        ResourceEditorDraftStore.LegacyImport result = store.importLegacy(legacy);
        assertEquals(ResourceEditorDraftStore.LEGACY_SUPERSEDED, result.state);
        assertTrue(result.corrupt.isEmpty());
        assertEquals(generation, store.generation());
        assertEquals("newer", text(reopen()[0]));
    }

    @Test public void diagnosticArchiveIsDeduplicatedAndIndependentOfManifest() throws Exception {
        Bundle corrupt = new Bundle();
        Bundle bad = new Bundle();
        bad.putString("entries", "bad material");
        corrupt.putBundle("0", bad);
        long generation = store.generation();
        String identity = store.archiveLegacyCorrupt(corrupt);
        assertTrue(identity.matches("[0-9a-f]{64}"));
        assertEquals(identity, store.archiveLegacyCorrupt(corrupt));
        File[] files = new File(new File(root, "legacy-material"), identity).listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        android.os.Parcel parcel = android.os.Parcel.obtain();
        try {
            byte[] bytes = Files.readAllBytes(files[0].toPath());
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Bundle material = parcel.readBundle(getClass().getClassLoader());
            assertEquals("project", material.getString("project"));
            assertEquals("-fr", material.getString("variant"));
            assertEquals("bad material", material.getBundle("pages").getBundle("0").getString("entries"));
        } finally { parcel.recycle(); }
        assertEquals(generation, store.generation());
        failingPoint = "before_legacy_material";
        try { store.archiveLegacyCorrupt(corrupt); fail("fault must be surfaced"); }
        catch (IOException expected) { }
        assertEquals("bad material", corrupt.getBundle("0").getString("entries"));
        assertEquals(generation, store.generation());
    }

    private static String digest(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
