package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import android.os.Bundle;
import pro.sketchware.core.resources.ColorModel;

@RunWith(AndroidJUnit4.class)
public class ResourceEditorLegacyXmlBarrierTest {
    private File root, xml, color;
    private String[] paths;
    private String failure;
    private ResourceEditorLegacyXmlBarrier barrier;
    private String source = ResourceEditorLegacyXmlBarrier.fingerprint("[{\"key\":\"k\",\"text\":\"A\"}]","{}");
    @Before public void setup() throws Exception {
        File parent=InstrumentationRegistry.getInstrumentation().getTargetContext().getNoBackupFilesDir();
        root=new File(parent,"legacy-barrier-test-"+UUID.randomUUID());
        assertTrue(new File(root,"values").mkdirs());
        assertTrue(root.getCanonicalPath().startsWith(parent.getCanonicalPath()+File.separator));
        xml=new File(root,"values/strings.xml");color=new File(root,"values/colors.xml");
        write(xml,"original XML");write(color,"original color");
        paths=new String[]{xml.getAbsolutePath(),color.getAbsolutePath(),null,null,null,null};
        barrier=create("project","-fr");
    }
    @After public void cleanup() throws Exception { ResourceEditorDraftStore.xmlReadFault=p->false;remove(root); }
    private ResourceEditorLegacyXmlBarrier create(String project,String variant) {
        return new ResourceEditorLegacyXmlBarrier(root,project,variant,paths,
                point->{if(point.equals(failure))throw new IOException("Injected "+point);});
    }
    private ResourceEditorLegacyXmlBarrier reopen() {return create("project","-fr");}
    private void register() throws Exception {barrier.remember(0,source);barrier.flushSources();}
    private File registry() {return new File(root,"legacy-xml-barriers").listFiles()[0];}
    private static void write(File file,String text) throws Exception {Files.write(file.toPath(),text.getBytes(StandardCharsets.UTF_8));}
    private interface Io {void run() throws Exception;}
    private void rejected(Io action) throws Exception {try{action.run();fail("Unverified old source must not be authorized");}catch(IOException expected){}}
    private static void remove(File file) throws Exception {
        if(!Files.isSymbolicLink(file.toPath())){File[] children=file.listFiles();if(children!=null)for(File child:children)remove(child);}
        if(file.exists()||Files.isSymbolicLink(file.toPath()))assertTrue(file.delete());
    }
    @Test public void confirmedSourceStaysSupersededAcrossNewOwnersAndLaterXmlC() throws Exception {
        register();String token=barrier.prepare(0,"B");write(xml,"B");barrier.confirm(0,token);
        assertTrue(reopen().superseded(0,source));write(xml,"later C");
        assertTrue(reopen().superseded(0,source));
        assertFalse(reopen().superseded(0,ResourceEditorLegacyXmlBarrier.fingerprint("[]","{}")));
    }
    @Test public void failedWritesKeepLegacyAndRetriesMergeTargets() throws Exception {
        register();barrier.prepare(0,"B");assertFalse(reopen().superseded(0,source));
        barrier.prepare(0,"C");write(xml,"B");assertTrue(reopen().superseded(0,source));
    }
    @Test public void failedConfirmationRetainsTargetAndNextWriteCanConfirmBeforeC() throws Exception {
        register();String token=barrier.prepare(0,"B");write(xml,"B");failure="before_legacy_xml_confirm";
        rejected(()->barrier.confirm(0,token));failure=null;assertTrue(reopen().superseded(0,source));
        assertNull(reopen().prepare(0,"C"));write(xml,"C");assertTrue(reopen().superseded(0,source));
    }
    @Test public void partialSuccessSuppressesOnlyTheWrittenPage() throws Exception {
        register();barrier.remember(1,source);barrier.flushSources();
        String token=barrier.prepare(0,"B");barrier.prepare(1,"color B");
        write(xml,"B");barrier.confirm(0,token);
        assertTrue(reopen().superseded(0,source));assertFalse(reopen().superseded(1,source));
    }
    @Test public void failedIntentDoesNotChangeXmlOrSupersedeTheLegacySource() throws Exception {
        register();failure="before_legacy_xml_intent";rejected(()->barrier.prepare(0,"B"));failure=null;
        assertEquals("original XML",new String(Files.readAllBytes(xml.toPath()),StandardCharsets.UTF_8));
        assertFalse(reopen().superseded(0,source));
    }
    @Test public void failedRegistrationCanRetryButAnUninformedOwnerCannotWrite() throws Exception {
        barrier.remember(0,source);failure="before_legacy_registry";rejected(()->barrier.flushSources());
        rejected(()->reopen().prepare(0,"B"));failure=null;
        assertFalse(barrier.superseded(0,source));barrier.flushSources();assertFalse(reopen().superseded(0,source));
    }
    @Test public void corruptAndMissingRegistryBothBlockOldSourceAndNewXmlWrites() throws Exception {
        register();File file=registry();byte[] original=Files.readAllBytes(file.toPath());write(file,"broken");
        rejected(()->reopen().superseded(0,source));rejected(()->reopen().prepare(0,"B"));
        assertTrue(file.exists());Files.write(file.toPath(),original);assertTrue(file.delete());
        rejected(()->reopen().superseded(0,source));rejected(()->reopen().prepare(0,"B"));
    }
    @Test public void unreadableOrUnexpectedXmlNeverTurnsIntoAnOldDraft() throws Exception {
        register();barrier.prepare(0,"B");ResourceEditorDraftStore.xmlReadFault=p->p==0;
        rejected(()->reopen().superseded(0,source));ResourceEditorDraftStore.xmlReadFault=p->false;
        write(xml,"unrelated C");rejected(()->reopen().superseded(0,source));assertTrue(registry().exists());
    }
    @Test public void ownersReloadAndMergeRatherThanOverwriteEachOthersSources() throws Exception {
        register();String second=ResourceEditorLegacyXmlBarrier.fingerprint("[2]","{}");
        var other=reopen();other.remember(0,second);other.flushSources();
        String token=barrier.prepare(0,"B");write(xml,"B");barrier.confirm(0,token);
        assertTrue(reopen().superseded(0,source));assertTrue(reopen().superseded(0,second));
    }
    @Test public void projectVariantAndRecreatedProjectScopeAreIsolated() throws Exception {
        register();String token=barrier.prepare(0,"B");write(xml,"B");barrier.confirm(0,token);
        assertFalse(create("another-project","-fr").superseded(0,source));
        assertFalse(create("project","-ja").superseded(0,source));
        assertTrue(new File(xml.getParentFile(),".resource-editor-legacy-scope").delete());write(xml,"new project");
        var replacement=reopen();assertFalse(replacement.superseded(0,source));
        replacement.remember(0,source);replacement.flushSources();assertFalse(reopen().superseded(0,source));
        rejected(()->barrier.prepare(0,"old owner must not write"));
    }
    @Test public void suppressedPageCannotFallBackWhenAnotherLegacyPageStillFailsMigration() throws Exception {
        Bundle legacy=new Bundle();legacy.putBundle("0",ResourceEditorDraftState.capture(
                List.of(new HashMap<>(Map.of("key","k","text","A"))),Map.of()));
        legacy.putBundle("1",ResourceEditorDraftState.capture(List.of(new ColorModel("c","#112233")),Map.of()));
        var gate=new ResourceEditorDraftController.Gate();gate.owner=1;
        var store=new ResourceEditorDraftStore(root,UUID.randomUUID().toString(),"project","-fr",gate,1,
                point->{if(point.equals(failure))throw new IOException("Injected");},paths);
        store.open(false);failure="before_manifest";assertEquals(ResourceEditorDraftStore.LEGACY_FAILED,store.importLegacy(legacy).state);
        failure=null;String token=store.prepareLegacyXml(0,"B");write(xml,"B");store.confirmLegacyXml(0,token);
        var next=new ResourceEditorDraftStore(root,UUID.randomUUID().toString(),"project","-fr",gate,1,
                point->{if(point.equals(failure))throw new IOException("Injected");},paths);
        next.open(false);failure="before_manifest";
        assertEquals(ResourceEditorDraftStore.LEGACY_FAILED,next.importLegacy(legacy).state);
        assertTrue(next.wasLegacySuperseded(0));assertFalse(next.wasLegacySuperseded(1));
    }
    @Test public void orphanCollectorDoesNotFollowSymlinkedPages() throws Exception {
        File outside=new File(root,"outside-test-material");assertTrue(outside.mkdir());
        File material=new File(outside,"2-"+UUID.randomUUID()+".seg");write(material,"keep");
        File candidate=new File(root,UUID.randomUUID().toString());assertTrue(candidate.mkdir());
        Files.createSymbolicLink(new File(candidate,"pages").toPath(),outside.toPath());assertTrue(candidate.setLastModified(1));
        ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(candidate.exists());assertTrue(material.exists());
    }
    @Test public void failedOpeningStillReleasesItsActiveOwnerWhenDetachedForClose() throws Exception {
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String missing=UUID.randomUUID().toString();
        var controller=new ResourceEditorDraftController(context,"owner-audit-"+missing,"",missing);
        try {
            controller.opening.handle((loaded,error)->null).join();assertTrue(controller.opening.isCompletedExceptionally());
            var ownersField=ResourceEditorDraftController.class.getDeclaredField("OWNERS");ownersField.setAccessible(true);
            Map<?,?> owners=(Map<?,?>)ownersField.get(null);assertTrue(owners.containsKey(missing));
            controller.detach(true);
            var ioField=ResourceEditorDraftController.class.getDeclaredField("IO");ioField.setAccessible(true);
            ((java.util.concurrent.ExecutorService)ioField.get(null)).submit(()->{}).get();
            assertFalse(owners.containsKey(missing));
        } finally {controller.detach(false);}
    }
    @Test public void actualFirstWriteOrphanIsCollectedOnlyAfterItsOwnerIsInactive() throws Exception {
        var gate=new ResourceEditorDraftController.Gate();gate.owner=1;
        String session=UUID.randomUUID().toString();
        var store=new ResourceEditorDraftStore(root,session,"orphan-project","",gate,1,
                point->{if(point.equals("before_manifest"))throw new IOException("Interrupted first manifest");});
        store.open(false);
        rejected(()->store.commit(new long[]{0,0,1,0,0,0},new boolean[]{false,false,true,false,false,false},
                Map.of(2,ResourceEditorDraftState.snapshot(List.of(new pro.sketchware.activities.resourceseditor.components.models.StyleModel("S","P")),Map.of()))));
        assertFalse(new File(store.directory,"manifest.json").exists());
        assertEquals(1,new File(store.directory,"pages").list().length);age(store.directory);
        ResourceEditorDraftOrphans.prune(root,Set.of(session));assertTrue(store.directory.exists());
        ResourceEditorDraftOrphans.prune(root,Set.of());assertFalse(store.directory.exists());
    }
    @Test public void androidOrphanScanPreservesDamagedManifestAndUnknownFiles() throws Exception {
        for(String extra:new String[]{"manifest.json","xml-commit.json","pages/user-material.txt"}) {
            File candidate=new File(root,UUID.randomUUID().toString());assertTrue(new File(candidate,"pages").mkdirs());
            write(new File(candidate,"pages/2-"+UUID.randomUUID()+".seg"),"material");
            write(new File(candidate,extra),"unrecognized or damaged");age(candidate);
            ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(candidate.exists());
            assertTrue(new File(candidate,extra).exists());
        }
    }
    private static void age(File file) throws Exception {
        File[] children=file.listFiles();if(children!=null)for(File child:children)age(child);
        assertTrue(file.setLastModified(1));
    }
    private Bundle legacy(String value) {
        Bundle legacy=new Bundle();legacy.putBundle("0",ResourceEditorDraftState.capture(
                List.of(new HashMap<>(Map.of("key","k","text",value))),Map.of()));return legacy;
    }
    private ResourceEditorDraftStore store(String session) throws Exception {
        var gate=new ResourceEditorDraftController.Gate();gate.owner=1;
        return new ResourceEditorDraftStore(root,session,"project","-fr",gate,1,
                point->{if(point.equals(failure))throw new IOException("Injected");},paths);
    }
    @Test public void closingUuidOnlyRestoreAlsoRetiresTheOriginalLegacyInput() throws Exception {
        String session=UUID.randomUUID().toString();var first=store(session);first.open(false);
        assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED,first.importLegacy(legacy("A")).state);
        var restored=store(session);assertNotNull(restored.open(true)[0]);restored.close();
        var fresh=store(UUID.randomUUID().toString());fresh.open(false);
        assertEquals(ResourceEditorDraftStore.LEGACY_SUPERSEDED,fresh.importLegacy(legacy("A")).state);
        assertTrue(fresh.wasLegacySuperseded(0));
    }
    @Test public void closingOneSessionKeepsAnotherSessionsDistinctLegacySource() throws Exception {
        var first=store(UUID.randomUUID().toString());first.open(false);first.importLegacy(legacy("A"));
        var second=store(UUID.randomUUID().toString());second.open(false);second.importLegacy(legacy("D"));first.close();
        var fresh=store(UUID.randomUUID().toString());fresh.open(false);
        assertEquals(ResourceEditorDraftStore.LEGACY_SUPERSEDED,fresh.importLegacy(legacy("A")).state);
        assertEquals(ResourceEditorDraftStore.LEGACY_MIGRATED,fresh.importLegacy(legacy("D")).state);
    }
    @Test public void failedLegacyDiscardKeepsTheSessionOpenAndCanRetry() throws Exception {
        String session=UUID.randomUUID().toString();var first=store(session);first.open(false);first.importLegacy(legacy("A"));
        byte[] before=Files.readAllBytes(new File(first.directory,"manifest.json").toPath());
        failure="before_legacy_discard";rejected(first::close);failure=null;
        assertArrayEquals(before,Files.readAllBytes(new File(first.directory,"manifest.json").toPath()));
        assertNotNull(store(session).open(true)[0]);first.close();
        var fresh=store(UUID.randomUUID().toString());fresh.open(false);
        assertEquals(ResourceEditorDraftStore.LEGACY_SUPERSEDED,fresh.importLegacy(legacy("A")).state);
    }
}
