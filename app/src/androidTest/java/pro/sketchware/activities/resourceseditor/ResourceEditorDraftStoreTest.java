package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import pro.sketchware.activities.resourceseditor.components.fragments.ArraysEditor;
import pro.sketchware.activities.resourceseditor.components.models.ArrayModel;
import pro.sketchware.activities.resourceseditor.components.models.StyleModel;
import pro.sketchware.core.resources.ColorModel;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorDraftStoreTest {
    private File root;
    private String session;
    private ResourceEditorDraftController.Gate gate;
    private String failingPoint;
    private ResourceEditorDraftStore store;

    @Before public void setUp() throws Exception {
        root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "draft-store-test-" + UUID.randomUUID());
        session = UUID.randomUUID().toString();
        gate = new ResourceEditorDraftController.Gate(); gate.owner = 1;
        store = create("project", "-fr");
        store.open(false);
    }

    @After public void tearDown() throws Exception { removeOwned(root); }

    private static void removeOwned(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) removeOwned(child);
        if (file.exists() && !file.delete()) throw new IOException("Cannot clean test directory");
    }

    private ResourceEditorDraftStore create(String project, String variant) throws IOException {
        return new ResourceEditorDraftStore(root, session, project, variant, gate, gate.owner,
                point -> { if (point.equals(failingPoint)) throw new IOException("Injected " + point); });
    }

    private ResourceEditorDraftState.Snapshot strings(String value) {
        return ResourceEditorDraftState.snapshot(List.of(new HashMap<>(Map.of("key", "probe", "text", value))), Map.of(0, "note <&>\n第二行"));
    }

    private void commitString(long revision, String value) throws IOException {
        long[] revisions = new long[6]; revisions[0] = revision;
        boolean[] dirty = new boolean[6]; dirty[0] = true;
        store.commit(revisions, dirty, Map.of(0, strings(value)));
    }

    private ResourceEditorDraftState.Snapshot[] restore() throws IOException { return create("project", "-fr").open(true); }
    private static String value(ResourceEditorDraftState.Snapshot snapshot) { return (String) ((Map<?, ?>) snapshot.entries.get(0)).get("text"); }

    @Test public void allModelsNotesAndEmptyDirtyPageRoundTrip() throws Exception {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>(Map.of("android:text", "<&> \"中\""));
        Map<Integer, ResourceEditorDraftState.Snapshot> pages = new HashMap<>();
        pages.put(0, strings("<&> \"中\"\n"));
        pages.put(1, ResourceEditorDraftState.snapshot(List.of(new ColorModel("primary", "#123456")), Map.of()));
        pages.put(2, ResourceEditorDraftState.snapshot(List.of(new StyleModel("Style", "Parent", attributes)), Map.of()));
        pages.put(3, ResourceEditorDraftState.snapshot(List.of(new StyleModel("Theme", "Theme.Parent", attributes)), Map.of()));
        pages.put(4, ResourceEditorDraftState.snapshot(List.of(new ArrayModel("items", ArraysEditor.ARRAYS_TYPES.STRING, attributes)), Map.of()));
        pages.put(5, ResourceEditorDraftState.snapshot(List.of(), Map.of(0, "empty note")));
        attributes.put("android:text", "later mutation");
        store.commit(new long[]{1,2,3,4,5,6}, new boolean[]{true,true,true,true,true,true}, pages);
        ResourceEditorDraftState.Snapshot[] restored = restore();
        assertEquals("<&> \"中\"\n", value(restored[0]));
        assertEquals("note <&>\n第二行", restored[0].notes.get(0));
        assertEquals("#123456", ((ColorModel) restored[1].entries.get(0)).getColorValue());
        assertEquals("<&> \"中\"", ((StyleModel) restored[2].entries.get(0)).getAttribute("android:text"));
        assertEquals("Theme.Parent", ((StyleModel) restored[3].entries.get(0)).getParent());
        assertEquals("<&> \"中\"", ((ArrayModel) restored[4].entries.get(0)).getAttribute("android:text"));
        assertTrue(restored[5].entries.isEmpty());
        assertEquals("empty note", restored[5].notes.get(0));
    }

    @Test public void unchangedPagesReuseFilesAndObsoleteVersionsAreCollected() throws Exception {
        commitString(1, "first");
        File pages = new File(store.directory, "pages");
        String first = pages.list()[0];
        store.commit(new long[]{1,0,0,0,0,0}, new boolean[]{true,false,false,false,false,false}, Map.of());
        assertArrayEquals(new String[]{first}, pages.list());
        commitString(2, "second");
        assertEquals(1, pages.list().length);
        assertFalse(new File(pages, first).exists());
        assertEquals("second", value(restore()[0]));
        store.commit(new long[]{3,0,0,0,0,0}, new boolean[6], Map.of());
        assertEquals(0, pages.list().length);
        assertNull(restore()[0]);
    }

    @Test public void failedPageOrManifestWriteRetainsPreviousGroup() throws Exception {
        commitString(1, "protected");
        for (String point : new String[]{"before_encode", "after_encode", "after_page", "before_manifest"}) {
            failingPoint = point;
            try { commitString(2, "uncommitted"); fail("Expected injected failure"); }
            catch (IOException expected) { }
            assertEquals("protected", value(restore()[0]));
        }
        failingPoint = null;
        commitString(2, "retry");
        assertEquals("retry", value(restore()[0]));
        assertEquals(1, new File(store.directory, "pages").list().length);
    }

    @Test public void cleanupFailureDoesNotRollBackSuccessfulCommit() throws Exception {
        commitString(1, "old");
        failingPoint = "after_manifest";
        commitString(2, "committed");
        assertEquals("committed", value(restore()[0]));
        failingPoint = null;
        store.commit(new long[]{2,0,0,0,0,0}, new boolean[]{true,false,false,false,false,false}, Map.of());
        assertEquals(1, new File(store.directory, "pages").list().length);
    }

    @Test public void missingOrCorruptPageRejectsWholeRestore() throws Exception {
        commitString(1, "protected");
        File page = new File(store.directory, "pages").listFiles()[0];
        byte[] original = Files.readAllBytes(page.toPath());
        Files.write(page.toPath(), "corrupt".getBytes(StandardCharsets.UTF_8));
        try { restore(); fail("Corrupt page must fail"); } catch (IOException expected) { }
        Files.write(page.toPath(), original);
        assertEquals("protected", value(restore()[0]));
        assertTrue(page.delete());
        try { restore(); fail("Missing page must fail"); } catch (IOException expected) { }
    }

    @Test public void identitySchemaAndSessionPathAreChecked() throws Exception {
        commitString(1, "protected");
        for (String[] identity : new String[][]{{"other", "-fr"}, {"project", "-ja"}}) {
            try { create(identity[0], identity[1]).open(true); fail("Identity mismatch must fail"); }
            catch (IOException expected) { }
        }
        try { new ResourceEditorDraftStore(root, "../bad", "project", "", gate, 1, point -> { }); fail(); }
        catch (IOException expected) { }
        File manifest = new File(store.directory, "manifest.json");
        JsonObject json = JsonParser.parseString(new String(Files.readAllBytes(manifest.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
        json.addProperty("schema", 100);
        Files.write(manifest.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        try { restore(); fail("Unknown schema must fail"); } catch (IOException expected) { }
    }

    @Test public void oldOwnerAndClosedSessionCannotCommitOrRestore() throws Exception {
        commitString(1, "protected");
        gate.owner++;
        try { commitString(2, "obsolete"); fail(); } catch (IOException expected) { }
        ResourceEditorDraftStore replacement = create("project", "-fr");
        replacement.open(true);
        replacement.close();
        assertEquals(0, new File(store.directory, "pages").list().length);
        try { replacement.commit(new long[6], new boolean[6], Map.of()); fail(); } catch (IOException expected) { }
        try { restore(); fail(); } catch (IOException expected) { }
    }

    @Test public void atomicXmlFailureKeepsOriginalAndSuccessReplacesIt() throws Exception {
        File xml = new File(root, "resources/strings.xml");
        ResourceXmlWriter.write(xml.getPath(), "original");
        try {
            ResourceXmlWriter.write(xml, writer -> { writer.write("partial"); throw new IOException("Injected"); });
            fail();
        } catch (IOException expected) { }
        assertEquals("original", new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8));
        ResourceXmlWriter.write(xml.getPath(), "replacement");
        assertEquals("replacement", new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8));
        assertEquals(1, xml.getParentFile().list().length);
    }

    @Test public void ageCleanupOnlyRemovesClosedSessions() throws Exception {
        commitString(1, "open unsaved draft");
        File openManifest = new File(store.directory, "manifest.json");
        assertTrue(openManifest.setLastModified(1));
        ResourceEditorDraftStore.pruneClosedSessions(root);
        assertTrue(openManifest.exists());
        assertEquals("open unsaved draft", value(restore()[0]));
        store.close();
        assertTrue(openManifest.setLastModified(1));
        ResourceEditorDraftStore.pruneClosedSessions(root);
        assertFalse(store.directory.exists());
    }

    @Test public void differentSessionsAreIsolatedAndGroupFailureKeepsBothOldPages() throws Exception {
        long[] revisions = {1,1,0,0,0,0};
        boolean[] dirty = {true,true,false,false,false,false};
        Map<Integer,ResourceEditorDraftState.Snapshot> original = Map.of(0,strings("old"),
                1,ResourceEditorDraftState.snapshot(List.of(new ColorModel("probe","#112233")),Map.of()));
        store.commit(revisions,dirty,original);
        ResourceEditorDraftStore other = new ResourceEditorDraftStore(root, UUID.randomUUID().toString(),"project","-fr",
                new ResourceEditorDraftController.Gate(),0,point -> { });
        other.open(false);
        other.commit(new long[]{1,0,0,0,0,0},new boolean[]{true,false,false,false,false,false},Map.of(0,strings("other session")));
        failingPoint = "before_manifest";
        try {
            store.commit(new long[]{2,2,0,0,0,0},dirty,Map.of(0,strings("new"),
                    1,ResourceEditorDraftState.snapshot(List.of(new ColorModel("probe","#445566")),Map.of())));
            fail();
        } catch (IOException expected) { }
        ResourceEditorDraftState.Snapshot[] restored = restore();
        assertEquals("old", value(restored[0]));
        assertEquals("#112233",((ColorModel)restored[1].entries.get(0)).getColorValue());
        assertEquals(2,new File(store.directory,"pages").list().length);
        assertTrue(new File(other.directory,"manifest.json").exists());
    }

    @Test public void measureLargeDraftCaptureCommitAndRestore() throws Exception {
        List<Map<String,Object>> entries = new java.util.ArrayList<>();
        String value = new String(new char[100]).replace('\0','x');
        for (int i=0;i<10000;i++) entries.add(new HashMap<>(Map.of("key","review_"+i,"text",value)));
        StringBuilder report = new StringBuilder("rows=10000 valueCharacters=100\n");
        for (int run=0;run<5;run++) {
            ResourceEditorDraftState.Snapshot[] snapshot = {null};
            double[] capture = {0};
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                long start = android.os.SystemClock.elapsedRealtimeNanos();
                snapshot[0] = ResourceEditorDraftState.snapshot(entries,Map.of());
                capture[0] = (android.os.SystemClock.elapsedRealtimeNanos()-start)/1000000.0;
            });
            long start = android.os.SystemClock.elapsedRealtimeNanos();
            store.commit(new long[]{run+1,0,0,0,0,0},new boolean[]{true,false,false,false,false,false},Map.of(0,snapshot[0]));
            long committed = android.os.SystemClock.elapsedRealtimeNanos();
            ResourceEditorDraftState.Snapshot[] restored = restore();
            long end = android.os.SystemClock.elapsedRealtimeNanos();
            assertEquals(10000,restored[0].entries.size());
            report.append("run=").append(run).append(" mainCopyMs=").append(capture[0])
                    .append(" workerCommitMs=").append((committed-start)/1000000.0)
                    .append(" readDecodeMs=").append((end-committed)/1000000.0).append('\n');
        }
        File output = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),
                "codex-device-review-20261015/performance-phases.txt");
        assertTrue(output.getParentFile().mkdirs() || output.getParentFile().isDirectory());
        Files.write(output.toPath(),report.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test public void stringEncodingMatchesLegacyJsonIncludingNullAndOptionalAttributes() throws Exception {
        Map<String,Object> row = new HashMap<>();
        row.put("key", "special");
        row.put("text", "<&> \"quotes\" \\ slash\n\r\t中文\u2028\u2029\uD83D\uDE00");
        row.put("translatable", "false");
        row.put("formatted", "false");
        row.put("optional", null);
        ResourceEditorDraftState.Snapshot snapshot = ResourceEditorDraftState.snapshot(List.of(row), Map.of(0, "note <&>\n"));
        store.commit(new long[]{1,0,0,0,0,0},new boolean[]{true,false,false,false,false,false},Map.of(0,snapshot));
        ResourceEditorDraftState.Snapshot[] restored = restore();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        assertEquals(gson.toJsonTree(snapshot.entries),gson.toJsonTree(restored[0].entries));
        assertEquals(gson.toJsonTree(snapshot.notes),gson.toJsonTree(restored[0].notes));
        Map<?,?> restoredRow = (Map<?,?>)restored[0].entries.get(0);
        assertEquals(row.get("text"),restoredRow.get("text"));
        assertEquals("false",restoredRow.get("translatable"));
        assertFalse(restoredRow.containsKey("optional"));
    }

    @Test public void checksumCoversExactlyTheCommittedUtf8Bytes() throws Exception {
        File file = new File(root,"checksum/strings.xml");
        java.security.MessageDigest checksum = java.security.MessageDigest.getInstance("SHA-256");
        ResourceXmlWriter.write(file,writer -> {
            writer.write("中文 <&>\n"); writer.write("\uD83D"); writer.write("\uDE00");
        },checksum);
        assertArrayEquals(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())),checksum.digest());
    }

    @Test public void stringEncoderRoundTripsAllControlsAndMixedUnicode() throws Exception {
        java.util.Random random = new java.util.Random(7391);
        List<Map<String,Object>> entries = new java.util.ArrayList<>();
        StringBuilder controls = new StringBuilder();
        for (int i = 0; i < 32; i++) controls.append((char)i);
        for (int i = 0; i < 500; i++) {
            StringBuilder value = new StringBuilder(controls);
            for (int j = 0; j < 128; j++) {
                switch (random.nextInt(6)) {
                    case 0 -> value.append((char)random.nextInt(32));
                    case 1 -> value.append('"').append('\\');
                    case 2 -> value.append((char)(0x4e00 + random.nextInt(512)));
                    case 3 -> value.appendCodePoint(0x1f600 + random.nextInt(64));
                    case 4 -> value.append('\u2028').append('\u2029');
                    default -> value.append((char)(32 + random.nextInt(95)));
                }
            }
            Map<String,Object> row = new HashMap<>();
            row.put("key", "random_" + i); row.put("text", value.toString());
            row.put("formatted", "false");
            if (i % 4 == 0) row.put("optional", null);
            entries.add(row);
        }
        ResourceEditorDraftState.Snapshot snapshot = ResourceEditorDraftState.snapshot(entries, Map.of(0, controls.toString()));
        store.commit(new long[]{1,0,0,0,0,0},new boolean[]{true,false,false,false,false,false},Map.of(0,snapshot));
        ResourceEditorDraftState.Snapshot restored = restore()[0];
        com.google.gson.Gson gson = new com.google.gson.Gson();
        assertEquals(gson.toJsonTree(snapshot.entries),gson.toJsonTree(restored.entries));
        assertEquals(controls.toString(),restored.notes.get(0));
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(entries.get(i).get("text"),((Map<?,?>)restored.entries.get(i)).get("text"));
        }
    }

    @Test public void oldJsonPageRemainsReadableAndConvertsOnlyWhenEdited() throws Exception {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String object = UUID.randomUUID().toString();
        JsonObject page = new JsonObject();
        page.addProperty("schema",1); page.addProperty("session",session); page.addProperty("project","project");
        page.addProperty("variant","-fr"); page.addProperty("page",0); page.addProperty("revision",4);
        page.add("entries",gson.toJsonTree(strings("legacy JSON").entries));
        page.add("notes",gson.toJsonTree(strings("legacy JSON").notes));
        File old = new File(new File(store.directory,"pages"),"0-"+object+".json");
        assertTrue(old.getParentFile().mkdirs() || old.getParentFile().isDirectory());
        byte[] bytes = page.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(old.toPath(),bytes);
        StringBuilder checksum = new StringBuilder();
        for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(bytes)) checksum.append(String.format("%02x",b&255));
        JsonObject manifest = new JsonObject();
        manifest.addProperty("schema",1); manifest.addProperty("session",session); manifest.addProperty("project","project");
        manifest.addProperty("variant","-fr"); manifest.addProperty("generation",1); manifest.addProperty("closed",false);
        com.google.gson.JsonArray pages = new com.google.gson.JsonArray();
        for (int i = 0; i < 6; i++) {
            JsonObject ref = new JsonObject(); ref.addProperty("revision",i==0?4:0); ref.addProperty("dirty",i==0);
            if (i==0) { ref.addProperty("object",object); ref.addProperty("checksum",checksum.toString()); }
            pages.add(ref);
        }
        manifest.add("pages",pages);
        Files.write(new File(store.directory,"manifest.json").toPath(),manifest.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals("legacy JSON",value(store.open(true)[0]));
        store.commit(new long[]{4,0,0,0,0,0},new boolean[]{true,false,false,false,false,false},Map.of());
        assertTrue("An unchanged legacy page must be reused",old.exists());
        commitString(5,"new encoding");
        assertFalse(old.exists());
        assertTrue(new File(store.directory,"pages").list()[0].endsWith(".bin"));
        assertEquals("new encoding",value(restore()[0]));
    }

    @Test public void binaryStringsSupportLargeValuesAndRejectMalformedPayloads() throws Exception {
        String value = new String(new char[100000]).replace('\0','中') + "\uD83D\uDE00";
        commitString(1,value);
        assertEquals(value,value(restore()[0]));
        byte[] bytes = Files.readAllBytes(new File(store.directory,"pages").listFiles()[0].toPath());
        assertTrue(bytes.length>65535);
        java.util.List<byte[]> malformed = new java.util.ArrayList<>();
        byte[] magic = bytes.clone(); magic[0]^=1; malformed.add(magic);
        byte[] version = bytes.clone(); version[7]=2; malformed.add(version);
        byte[] length = bytes.clone(); java.util.Arrays.fill(length,8,12,(byte)0xff); malformed.add(length);
        malformed.add(java.util.Arrays.copyOf(bytes,bytes.length-1));
        malformed.add(java.util.Arrays.copyOf(bytes,bytes.length+1));
        for (byte[] input : malformed) {
            try { ResourceEditorStringsSnapshot.read(input,session,"project","-fr",1); fail("Malformed binary page must fail"); }
            catch (IOException expected) { }
        }
        try { ResourceEditorStringsSnapshot.read(bytes,session,"project","-ja",1); fail(); } catch (IOException expected) { }
        try { ResourceEditorStringsSnapshot.read(bytes,session,"project","-fr",2); fail(); } catch (IOException expected) { }
    }
}
