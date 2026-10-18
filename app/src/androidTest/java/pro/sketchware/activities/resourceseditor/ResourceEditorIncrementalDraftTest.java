package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorIncrementalDraftTest {
    private final Gson gson = new Gson();
    private File root;
    private String session;
    private ResourceEditorDraftStore store;
    private ResourceEditorDraftController.Gate gate;
    private String failure;
    private int failAfterPages;
    private final List<Map<String,Object>> rows = new ArrayList<>();
    private final Map<Integer,String> notes = new HashMap<>();
    private final ResourceEditorStringChanges changes = new ResourceEditorStringChanges();
    private long revision;

    @Before public void setUp() throws Exception {
        root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir(),
                "incremental-draft-test-" + UUID.randomUUID());
        session = UUID.randomUUID().toString();
        gate = new ResourceEditorDraftController.Gate(); gate.owner = 1;
        store = create(); store.open(false);
    }

    @After public void tearDown() throws Exception { remove(root); }
    private static void remove(File file) throws IOException {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) remove(child);
        if (file.exists() && !file.delete()) throw new IOException("Cannot remove owned fixture");
    }
    private ResourceEditorDraftStore create() throws IOException {
        return new ResourceEditorDraftStore(root, session, "project", "-fr", gate, gate.owner, point -> {
            if (point.equals(failure) || point.equals("after_page") && failAfterPages > 0 && --failAfterPages == 0)
                throw new IOException("Injected " + point);
        });
    }
    private void seed(int count) throws Exception {
        for (int i = 0; i < count; i++) rows.add(new HashMap<>(Map.of("key", "row_"+i,"text","初始 😀 " + i)));
        revision = 1;
        commit(ResourceEditorDraftState.snapshot(rows, notes));
        changes.restored(revision, rows.size(), notes);
    }
    private void commit(ResourceEditorDraftState.Snapshot snapshot) throws IOException {
        store.commit(new long[]{revision,0,0,0,0,0}, new boolean[]{true,false,false,false,false,false}, Map.of(0,snapshot));
    }
    private ResourceEditorDraftState.Snapshot capture() { return changes.snapshot(rows, notes, store.incrementalRevision(0)); }
    private void edit(int index, String text) {
        rows.get(index).put("text", text); changes.changed(index,false,rows.size(),++revision);
    }
    private void delete(int index) {
        rows.remove(index);
        Map<Integer,String> reindexed = new HashMap<>();
        for (Map.Entry<Integer,String> note : notes.entrySet()) if (note.getKey() != index)
            reindexed.put(note.getKey() > index ? note.getKey()-1 : note.getKey(),note.getValue());
        notes.clear(); notes.putAll(reindexed);
        changes.changed(index,true,rows.size(),++revision);
    }
    private ResourceEditorDraftState.Snapshot restored() throws Exception { return create().open(true)[0]; }
    private void verify() throws Exception {
        ResourceEditorDraftState.Snapshot result = restored();
        assertEquals(gson.toJsonTree(rows),gson.toJsonTree(result.entries));
        assertEquals(notes,result.notes);
        assertTrue(new File(store.directory,"pages").list().length <= ResourceEditorStringChanges.chunkCount(rows.size())+1);
    }
    private JsonObject manifest() throws IOException {
        return JsonParser.parseString(new String(Files.readAllBytes(new File(store.directory,"manifest.json").toPath()),StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test public void pointEditCopiesOneChunkAndReusesBaseAndOtherChunks() throws Exception {
        seed(10000);
        File base = new File(store.directory,"pages").listFiles()[0];
        byte[] original = Files.readAllBytes(base.toPath());
        edit(5000,"changed"); notes.put(5000,"comment <&> 😀");
        ResourceEditorDraftState.Snapshot snapshot = capture();
        assertNotNull(snapshot.chunks);
        assertEquals(1,snapshot.chunks.changed.size());
        assertEquals(256,snapshot.chunks.changed.get(19).entries.size());
        commit(snapshot); verify();
        assertArrayEquals(original,Files.readAllBytes(base.toPath()));
        String first = manifest().getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonObject("chunks")
                .getAsJsonObject("19").get("object").getAsString();
        edit(20,"other chunk"); commit(capture()); verify();
        edit(5001,"same chunk again"); commit(capture()); verify();
        assertFalse(new File(new File(store.directory,"pages"),"0-"+first+".bin").exists());
        assertEquals(3,new File(store.directory,"pages").list().length);
    }

    @Test public void appendAndTailDeletePreserveBoundaryAndTrailingComments() throws Exception {
        notes.put(0,"header"); notes.put(255,"before boundary"); notes.put(256,"boundary"); notes.put(1024,"footer");
        seed(1024);
        edit(255,"edit before boundary"); commit(capture());
        rows.add(new HashMap<>(Map.of("key","appended","text","new")));
        changes.changed(1024,false,rows.size(),++revision);
        notes.put(1025,"new footer");
        commit(capture()); verify();
        assertEquals("footer",restored().notes.get(1024));
        delete(1024); commit(capture()); verify();
        assertEquals("new footer",restored().notes.get(1024));
        delete(1023); commit(capture()); verify();
    }

    @Test public void deletionShiftsLaterRowsAndNotesAcrossChunks() throws Exception {
        notes.put(770,"shift me"); notes.put(1537,"footer"); seed(1537);
        edit(1280,"keep overlay"); commit(capture());
        delete(1200);
        assertNotNull(capture().chunks);
        commit(capture()); verify();
        delete(256); assertNull(capture().chunks); commit(capture()); verify();
    }

    @Test public void resetBulkReorderEmptyAndOrphanLegacyNotesUseFullSnapshot() throws Exception {
        seed(1537); edit(10,"first"); commit(capture());
        java.util.Collections.reverse(rows); changes.reset(++revision);
        assertNull(capture().chunks); commit(capture()); verify();
        notes.put(rows.size()+10,"legacy orphan"); changes.reset(++revision); commit(capture()); verify();
        edit(15,"after orphan"); assertNull(capture().chunks); commit(capture()); verify();
        rows.clear(); notes.clear(); notes.put(0,"empty footer"); changes.reset(++revision);
        commit(capture()); verify(); assertEquals(1,new File(store.directory,"pages").list().length);
    }

    @Test public void failedMultiChunkCommitKeepsWholePreviousGenerationAndCanRetry() throws Exception {
        seed(2048); edit(1,"protected"); commit(capture());
        edit(270,"pending one"); edit(520,"pending two");
        ResourceEditorDraftState.Snapshot pending = capture();
        assertEquals(2,pending.chunks.changed.size());
        failAfterPages = 1;
        try { commit(pending); fail(); } catch (IOException expected) { }
        assertEquals("protected",((Map<?,?>)restored().entries.get(1)).get("text"));
        assertFalse("pending one".equals(((Map<?,?>)restored().entries.get(270)).get("text")));
        // Restore cleanup may discard uncommitted objects, while UI stamps must retain both edits.
        commit(capture()); verify();
        for (String point : new String[]{"before_encode","after_encode","after_page","before_manifest"}) {
            edit(530,point); failure=point;
            try { commit(capture()); fail(); } catch (IOException expected) { }
            assertFalse(point.equals(((Map<?,?>)restored().entries.get(530)).get("text")));
            failure=null; commit(capture()); verify();
        }
    }

    @Test public void queuedCapturesIncludeChangesSinceDurableBase() throws Exception {
        seed(2048); long base=store.incrementalRevision(0);
        edit(10,"queued first"); long firstRevision=revision;
        ResourceEditorDraftState.Snapshot first=capture();
        edit(300,"queued second"); ResourceEditorDraftState.Snapshot second=capture(); long secondRevision=revision;
        assertEquals(base,second.chunks.baseRevision);
        revision=firstRevision; commit(first); revision=secondRevision; commit(second); verify();
        // An unrelated later UI mutation cannot change either captured map.
        ResourceEditorDraftState.Snapshot immutable=capture();
        edit(300,"not captured");
        assertFalse(gson.toJson(immutable).contains("not captured"));
    }

    @Test public void queuedTruncateAndAppendRetryCannotResurrectAnOldTailChunk() throws Exception {
        notes.put(1536,"original footer"); seed(1536);
        rows.add(new HashMap<>(Map.of("key","queued_tail","text","old queued append")));
        notes.put(1537,"queued footer");
        changes.changed(1536,false,rows.size(),++revision);
        long queuedRevision=revision;
        ResourceEditorDraftState.Snapshot queued=capture();
        assertNotNull(queued.chunks);
        delete(1536);
        rows.add(new HashMap<>(Map.of("key","replacement_tail","text","latest append")));
        notes.put(1536,"replacement note"); notes.put(1537,"latest footer");
        changes.changed(1536,false,rows.size(),++revision);
        long latestRevision=revision;
        ResourceEditorDraftState.Snapshot latest=capture();
        assertNotNull(latest.chunks);
        revision=queuedRevision; commit(queued); revision=latestRevision;
        failure="before_manifest";
        try { commit(latest); fail(); } catch(IOException expected) { }
        assertEquals("old queued append",((Map<?,?>)restored().entries.get(1536)).get("text"));
        failure=null; commit(capture()); verify();
        delete(1536); commit(capture()); verify();
        assertEquals("latest footer",restored().notes.get(1536));
    }

    @Test public void fullResetAndLaterEditSupersedeAnEarlierQueuedDelta() throws Exception {
        seed(2048); edit(100,"earlier queued delta");
        long firstRevision=revision;
        ResourceEditorDraftState.Snapshot earlier=capture();
        java.util.Collections.reverse(rows);
        notes.clear(); notes.put(2048,"new footer"); changes.reset(++revision);
        long resetRevision=revision;
        ResourceEditorDraftState.Snapshot reset=capture(); assertNull(reset.chunks);
        edit(900,"latest edit after reset"); long latestRevision=revision;
        ResourceEditorDraftState.Snapshot latest=capture(); assertNull(latest.chunks);
        revision=firstRevision; commit(earlier);
        revision=resetRevision; commit(reset);
        revision=latestRevision; commit(latest); verify();
        assertEquals(1,new File(store.directory,"pages").list().length);
    }

    @Test public void chunkAndOtherPageFailureCannotMixGenerations() throws Exception {
        seed(2048);
        boolean[] dirty={true,true,false,false,false,false};
        store.commit(new long[]{revision,1,0,0,0,0},dirty,Map.of(1,ResourceEditorDraftState.snapshot(
                List.of(new pro.sketchware.core.resources.ColorModel("probe","#112233")),Map.of())));
        edit(300,"new string");
        Map<Integer,ResourceEditorDraftState.Snapshot> pending=Map.of(0,capture(),1,ResourceEditorDraftState.snapshot(
                List.of(new pro.sketchware.core.resources.ColorModel("probe","#445566")),Map.of()));
        failure="before_manifest";
        try { store.commit(new long[]{revision,2,0,0,0,0},dirty,pending); fail(); } catch(IOException expected) { }
        ResourceEditorDraftState.Snapshot[] old=create().open(true);
        assertEquals("初始 😀 300",((Map<?,?>)old[0].entries.get(300)).get("text"));
        assertEquals("#112233",((pro.sketchware.core.resources.ColorModel)old[1].entries.get(0)).getColorValue());
        failure=null; store.commit(new long[]{revision,2,0,0,0,0},dirty,pending);
        ResourceEditorDraftState.Snapshot[] current=create().open(true);
        assertEquals("new string",((Map<?,?>)current[0].entries.get(300)).get("text"));
        assertEquals("#445566",((pro.sketchware.core.resources.ColorModel)current[1].entries.get(0)).getColorValue());
    }

    @Test public void malformedMissingAndWrongIndexChunksRejectEntireRestore() throws Exception {
        seed(2048); edit(300,"changed"); commit(capture());
        JsonObject json=manifest(); JsonObject page=json.getAsJsonArray("pages").get(0).getAsJsonObject();
        JsonObject chunks=page.getAsJsonObject("chunks"); JsonObject chunk=chunks.getAsJsonObject("1");
        File file=new File(new File(store.directory,"pages"),"0-"+chunk.get("object").getAsString()+".bin");
        byte[] bytes=Files.readAllBytes(file.toPath()); Files.write(file.toPath(),new byte[]{0});
        try { restored(); fail(); } catch(IOException expected) { }
        Files.write(file.toPath(),bytes); verify();
        chunks.remove("1"); chunks.add("2",chunk);
        Files.write(new File(store.directory,"manifest.json").toPath(),json.toString().getBytes(StandardCharsets.UTF_8));
        try { restored(); fail("Header must bind payload to chunk index"); } catch(IOException expected) { }
        chunks.remove("2"); chunks.add("1",chunk);
        page.addProperty("rows",Integer.MAX_VALUE);
        Files.write(new File(store.directory,"manifest.json").toPath(),json.toString().getBytes(StandardCharsets.UTF_8));
        try { restored(); fail("Oversized row count must fail before allocation"); } catch(IOException expected) { }
        page.addProperty("rows",2048);
        Files.write(new File(store.directory,"manifest.json").toPath(),json.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(file.delete());
        try { restored(); fail(); } catch(IOException expected) { }
    }

    @Test public void restoreSeedsTrackingAndCleanCheckpointDropsAllPayloads() throws Exception {
        seed(2048); edit(400,"before recreate"); commit(capture());
        ResourceEditorDraftState.Snapshot saved=restored();
        changes.restored(revision,saved.entries.size(),saved.notes);
        edit(900,"after recreate"); commit(capture()); verify();
        store.commit(new long[]{++revision,0,0,0,0,0},new boolean[6],Map.of());
        assertEquals(-1,store.incrementalRevision(0));
        assertEquals(0,new File(store.directory,"pages").list().length);
        assertNull(create().open(true)[0]);
        edit(901,"new dirty generation"); assertNull(capture().chunks); commit(capture()); verify();
    }

    @Test public void deterministicMixedEditsRoundTripWithoutGrowingAnUnboundedLog() throws Exception {
        notes.put(1537,"footer"); seed(1537); Random random=new Random(7391);
        for (int step=0;step<60;step++) {
            if (step%9==0) {
                rows.add(new HashMap<>(Map.of("key","added_"+step,"text","追加 "+step)));
                changes.changed(rows.size()-1,false,rows.size(),++revision);
            } else if (step%7==0) delete(rows.size()-1-random.nextInt(400));
            else {
                int index=random.nextInt(rows.size()); edit(index,"change "+step+" <&> 😀");
                if (step%2==0) notes.put(index,"note "+step); else notes.remove(index);
            }
            commit(capture()); verify();
        }
    }
}
