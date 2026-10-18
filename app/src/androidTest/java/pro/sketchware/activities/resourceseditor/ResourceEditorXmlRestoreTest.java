package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import pro.sketchware.core.resources.StringResourceResolver;

/**
 * Task 1B fix regression: after XML B replaced draft A, A must stop being an incremental base.
 * A later edit in another chunk must persist a complete B-based page, not B + A's chunks.
 */
@RunWith(AndroidJUnit4.class)
public class ResourceEditorXmlRestoreTest {
    private static final int ROWS = 1024;

    private File root;
    private String session;
    private ResourceEditorDraftController.Gate gate;
    private File[] xmlPaths;
    private ResourceEditorDraftStore store;
    private String failure;
    private ArrayList<HashMap<String, Object>> rows;
    private HashMap<Integer, String> notes;
    private ResourceEditorStringChanges changes;
    private long revision;

    @Before public void setUp() throws Exception {
        root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "xml-restore-test-" + UUID.randomUUID());
        File values = new File(root, "project-xml");
        assertTrue(values.mkdirs());
        String[] names = {"strings.xml", "colors.xml", "styles.xml", "themes.xml", "arrays.xml", "dimens.xml"};
        xmlPaths = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            xmlPaths[i] = new File(values, names[i]);
            Files.write(xmlPaths[i].toPath(), "<resources/>".getBytes(StandardCharsets.UTF_8));
        }
        session = UUID.randomUUID().toString();
        gate = new ResourceEditorDraftController.Gate();
        gate.owner = 1;
        rows = new ArrayList<>();
        notes = new HashMap<>();
        changes = new ResourceEditorStringChanges();
        revision = 0;
        store = create();
        store.open(false);
    }

    @After public void tearDown() throws Exception {
        ResourceEditorDraftStore.xmlReadFault = page -> false;
        ResourceEditorDraftStore.testFault = point -> { };
        remove(root);
    }

    private static void remove(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) remove(child);
        if (file.exists() && !file.delete()) throw new IOException("Cannot remove owned fixture");
    }

    private ResourceEditorDraftStore create() throws IOException {
        String[] paths = new String[xmlPaths.length];
        for (int i = 0; i < xmlPaths.length; i++) paths[i] = xmlPaths[i].getAbsolutePath();
        return new ResourceEditorDraftStore(root, session, "project", "-fr", gate, gate.owner, point -> {
            if (point.equals(failure)) throw new IOException("Injected " + point);
        }, paths);
    }

    private void seedA() throws IOException {
        rows.clear();
        notes.clear();
        for (int i = 0; i < ROWS; i++) {
            HashMap<String, Object> row = new HashMap<>();
            row.put("key", "row_" + i);
            row.put("text", "A-" + i);
            rows.add(row);
        }
        notes.put(0, "header A");
        notes.put(700, "old A note");
        notes.put(ROWS, "footer A");
        revision = 1;
        protect();
    }

    /** Mirrors ResourceEditorDraftController.prewrite for the strings page. */
    private void protect() throws IOException {
        boolean same = store.protectedPage(ResourceEditorTabs.STRINGS, revision, true);
        ResourceEditorDraftState.Snapshot change = same
                ? null : changes.snapshot(rows, notes, store.incrementalRevision(ResourceEditorTabs.STRINGS));
        store.commit(new long[]{revision, 0, 0, 0, 0, 0},
                new boolean[]{true, false, false, false, false, false},
                change == null ? Map.of() : Map.of(ResourceEditorTabs.STRINGS, change));
    }

    private void edit(int index, String text) {
        rows.get(index).put("text", text);
        changes.changed(index, false, rows.size(), ++revision);
    }

    private void delete(int index) {
        rows.remove(index);
        HashMap<Integer, String> shifted = new HashMap<>();
        for (Map.Entry<Integer, String> note : notes.entrySet()) {
            if (note.getKey() == index) continue;
            shifted.put(note.getKey() > index ? note.getKey() - 1 : note.getKey(), note.getValue());
        }
        notes.clear();
        notes.putAll(shifted);
        changes.changed(index, true, rows.size(), ++revision);
    }

    private void append(String key, String text) {
        HashMap<String, Object> row = new HashMap<>();
        row.put("key", key);
        row.put("text", text);
        rows.add(row);
        changes.changed(rows.size() - 1, false, rows.size(), ++revision);
    }

    private String render() {
        return new StringResourceResolver().convertListMapToXmlStrings(rows, notes);
    }

    /** Same order as Activity.performXmlOnlySave: intent first, then the exact same bytes. */
    private void saveXmlOnly(String xml) throws IOException {
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        assertTrue(store.writeXmlCommitIntent(Map.of(ResourceEditorTabs.STRINGS, digest(bytes))));
        Files.write(xmlPaths[ResourceEditorTabs.STRINGS].toPath(), bytes);
    }

    /** Activity's XML-load path: new fragment model, revision from the manifest, no restored(). */
    private void loadXml(String xml) {
        StringResourceResolver resolver = new StringResourceResolver();
        rows.clear();
        notes.clear();
        resolver.convertXmlStringsToListMap(xml, rows);
        assertFalse("fixture XML must parse", resolver.isDataLoadingFailed);
        notes.putAll(resolver.notesMap);
        changes = new ResourceEditorStringChanges();
        revision = store.revision(ResourceEditorTabs.STRINGS);
    }

    private ResourceEditorDraftState.Snapshot restore() throws IOException {
        store = create();
        ResourceEditorDraftState.Snapshot page = store.open(true)[ResourceEditorTabs.STRINGS];
        if (page != null) {
            ResourceEditorDraftState.apply(page, rows, notes);
            changes = new ResourceEditorStringChanges();
            revision = store.revision(ResourceEditorTabs.STRINGS);
            changes.restored(revision, rows.size(), notes);
        }
        return page;
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

    private String text(int index) { return String.valueOf(rows.get(index).get("text")); }

    @Test public void recoveredXmlSupersedesDraftBaseAcrossChunks() throws Exception {
        seedA();
        long generation = store.generation();
        edit(700, "saved B at 700");
        notes.remove(700);
        String xmlB = render();
        saveXmlOnly(xmlB);

        // The CLEAN commit keeps failing, so draft A stays on disk with generation + intent.
        failure = "before_manifest";
        try { protect(); fail("CLEAN commit must fail"); } catch (IOException expected) { }
        failure = null;
        assertEquals(generation, store.generation());

        assertNull("XML B must win over draft A", restore());
        assertTrue(store.wasXmlCommitted(ResourceEditorTabs.STRINGS));
        assertEquals("A must not remain an incremental base",
                -1, store.incrementalRevision(ResourceEditorTabs.STRINGS));
        assertFalse("A must not stay protected",
                store.protectedPage(ResourceEditorTabs.STRINGS, store.revision(ResourceEditorTabs.STRINGS), true));
        loadXml(xmlB);
        assertEquals(ROWS, rows.size());
        assertEquals("saved B at 700", text(700));
        assertNull("XML B removed the old comment", notes.get(700));

        // Edit another chunk while CLEAN still fails, then recover.
        edit(10, "point C at 10");
        failure = "before_manifest";
        try { protect(); fail("CLEAN commit must still fail"); } catch (IOException expected) { }
        failure = null;
        assertEquals("saved B at 700", text(700));
        protect();
        assertFalse("intent must be consumed once a complete snapshot exists",
                new File(store.directory, "xml-commit.json").exists());

        assertNotNull("a complete page must be restorable", restore());
        assertEquals(ROWS, rows.size());
        assertEquals("saved B at 700", text(700));
        assertEquals("point C at 10", text(10));
        assertEquals("A-11", text(11));
        assertEquals("header A", notes.get(0));
        assertEquals("footer A", notes.get(ROWS));
        assertNull("the superseded comment must not return", notes.get(700));
    }

    @Test public void chunkedDraftAIsReplacedWithoutLosingSavedXmlRows() throws Exception {
        seedA();
        // A already has one overlay chunk before the XML-only save.
        edit(700, "protected A chunk");
        protect();
        assertEquals("fixture requires a chunked draft", 2, store.incrementalRevision(ResourceEditorTabs.STRINGS));

        edit(700, "saved B at 700");
        notes.remove(700);
        String xmlB = render();
        saveXmlOnly(xmlB);

        assertNull(restore());
        loadXml(xmlB);
        edit(300, "point C at 300");
        failure = "before_manifest";
        try { protect(); fail("CLEAN commit must fail"); } catch (IOException expected) { }
        failure = null;
        protect();

        assertNotNull(restore());
        assertEquals("saved B at 700", text(700));
        assertEquals("point C at 300", text(300));
        assertEquals("A-10", text(10));
        assertNull(notes.get(700));

        // The recovered full snapshot is a valid new incremental base.
        edit(500, "after recovery D");
        protect();
        assertNotNull(restore());
        assertEquals("saved B at 700", text(700));
        assertEquals("point C at 300", text(300));
        assertEquals("after recovery D", text(500));
        assertEquals("A-10", text(10));
    }

    @Test public void appendAndDeleteAfterXmlRecoveryKeepSavedXmlRow() throws Exception {
        seedA();
        edit(700, "saved B at 700");
        notes.remove(700);
        String xmlB = render();
        saveXmlOnly(xmlB);
        assertNull(restore());
        loadXml(xmlB);

        delete(100);
        assertEquals(1023, rows.size());
        assertEquals("saved B at 700", text(699));
        edit(10, "point C at 10");
        protect();

        assertNotNull(restore());
        assertEquals(1023, rows.size());
        assertEquals("saved B at 700", text(699));
        assertEquals("point C at 10", text(10));
        assertEquals("header A", notes.get(0));
        assertEquals("footer A", notes.get(1023));

        append("appended_after_recovery", "tail value");
        protect();
        assertNotNull(restore());
        assertEquals(1024, rows.size());
        assertEquals("appended_after_recovery", rows.get(1023).get("key"));
        assertEquals("tail value", text(1023));
        assertEquals("saved B at 700", text(699));
        assertEquals("point C at 10", text(10));
    }

    @Test public void partialXmlSaveKeepsSavedXmlWhenEditingAnotherChunk() throws Exception {
        seedA();
        // Strings XML B is written while the session stays open (another page failed).
        edit(700, "saved B at 700");
        notes.remove(700);
        saveXmlOnly(render());
        // The user keeps editing a different chunk in the same session, without a restore.
        edit(10, "point C at 10");
        protect();

        assertNotNull(restore());
        assertEquals("saved B at 700", text(700));
        assertEquals("point C at 10", text(10));
        assertEquals("A-11", text(11));
        assertNull("the superseded comment must not return", notes.get(700));
    }

    @Test public void unreadableXmlFailsRestoreInsteadOfApplyingDraft() throws Exception {
        seedA();
        edit(700, "saved B at 700");
        notes.remove(700);
        String xmlB = render();
        saveXmlOnly(xmlB);
        File draftManifest = new File(store.directory, "manifest.json");
        File intent = new File(store.directory, "xml-commit.json");
        assertTrue(draftManifest.isFile());
        assertTrue(intent.isFile());

        ResourceEditorDraftStore.xmlReadFault = page -> page == ResourceEditorTabs.STRINGS;
        store = create();
        try {
            store.open(true);
            fail("an unreadable XML must fail the restore, not apply draft A");
        } catch (IOException expected) { }

        // Recovery material must be retained for a later, readable attempt.
        assertTrue(draftManifest.isFile());
        assertTrue(intent.isFile());
        File[] stored = new File(store.directory, "pages").listFiles();
        assertNotNull(stored);
        assertTrue("the draft payload must stay on disk", stored.length > 0);

        ResourceEditorDraftStore.xmlReadFault = page -> false;
        assertNull("once readable, XML B still wins", restore());
        assertTrue(store.wasXmlCommitted(ResourceEditorTabs.STRINGS));
    }

    @Test public void missingOrDifferentXmlStillAppliesDraft() throws Exception {
        seedA();
        store.writeXmlCommitIntent(Map.of(ResourceEditorTabs.STRINGS,
                digest("not the current xml".getBytes(StandardCharsets.UTF_8))));

        assertNotNull("a readable mismatch still allows the draft", restore());
        assertFalse(store.wasXmlCommitted(ResourceEditorTabs.STRINGS));
        assertEquals(ROWS, rows.size());
        assertEquals("A-700", text(700));
        assertEquals("old A note", notes.get(700));

        assertTrue(xmlPaths[ResourceEditorTabs.STRINGS].delete());
        assertNotNull("a missing file still allows the draft", restore());
        assertFalse(store.wasXmlCommitted(ResourceEditorTabs.STRINGS));
        assertEquals("A-10", text(10));
    }
}
