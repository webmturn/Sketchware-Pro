package pro.sketchware.activities.resourceseditor;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.UUID;

public class ResourceEditorDraftOrphansTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private File session(File root) throws Exception {
        File directory = new File(root, UUID.randomUUID().toString());
        assertTrue(new File(directory,"pages").mkdirs());
        Files.write(new File(directory,"pages/2-"+UUID.randomUUID()+".seg").toPath(),new byte[]{1});
        return directory;
    }
    private void age(File file) throws Exception {
        File[] children=file.listFiles();if(children!=null)for(File child:children)age(child);
        assertTrue(file.setLastModified(1));
    }
    @Test public void oldKnownFirstWriteFilesAreCollectedAndRetryIsSafe() throws Exception {
        File root=temporary.newFolder();File directory=session(root);
        for(String suffix:new String[]{"json","bin"})Files.write(new File(directory,"pages/0-"+UUID.randomUUID()+"."+suffix).toPath(),new byte[]{1});
        Files.write(new File(directory,".resource-manifest.tmp").toPath(),new byte[]{1});
        age(directory);ResourceEditorDraftOrphans.prune(root,Set.of());
        assertFalse(directory.exists());ResourceEditorDraftOrphans.prune(root,Set.of());
    }
    @Test public void activeOwnerKeepsEvenAnOldUncommittedPage() throws Exception {
        File root=temporary.newFolder();File directory=session(root);age(directory);
        ResourceEditorDraftOrphans.prune(root,Set.of(directory.getName()));assertTrue(directory.exists());
        assertEquals(1,new File(directory,"pages").list().length);
    }
    @Test public void recentPayloadOrDirectoryProtectsTheWholeCandidate() throws Exception {
        File root=temporary.newFolder();File directory=session(root);age(directory);
        assertTrue(new File(directory,"pages").listFiles()[0].setLastModified(System.currentTimeMillis()));
        ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(directory.exists());
        age(directory);assertTrue(directory.setLastModified(System.currentTimeMillis()));
        ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(directory.exists());
    }
    @Test public void evenDamagedManifestOrIntentIsNeverAnOrphan() throws Exception {
        File root=temporary.newFolder();
        for(String name:new String[]{"manifest.json","xml-commit.json"}) {
            File directory=session(root);Files.write(new File(directory,name).toPath(),"broken".getBytes());age(directory);
            ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(directory.exists());
        }
    }
    @Test public void unknownFilesAndNestedDirectoriesKeepAllPayloads() throws Exception {
        File root=temporary.newFolder();File directory=session(root);
        Files.write(new File(directory,"pages/user-material.txt").toPath(),new byte[]{2});age(directory);
        ResourceEditorDraftOrphans.prune(root,Set.of());assertEquals(2,new File(directory,"pages").list().length);
        assertTrue(new File(directory,"pages/unknown").mkdir());age(directory);
        ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(directory.exists());
    }
    @Test public void diagnosticAndUnrecognizedDirectoriesAreOutsideTheScan() throws Exception {
        File root=temporary.newFolder();
        for(String name:new String[]{"legacy-material","legacy-xml-barriers","not-a-session"}) {
            File directory=new File(root,name);assertTrue(directory.mkdir());age(directory);
            ResourceEditorDraftOrphans.prune(root,Set.of());assertTrue(directory.exists());
        }
    }

    @Test public void queuedCleanupChecksOwnersAndNewIntentAtExecutionTime() throws Exception {
        File root = temporary.newFolder();
        File owned = session(root), protectedByIntent = session(root);
        age(owned); age(protectedByIntent);
        Set<String> active = new HashSet<>();
        ArrayDeque<Runnable> queue = new ArrayDeque<>();
        ResourceEditorDraftMaintenance maintenance = new ResourceEditorDraftMaintenance(queue::addLast,
                (directory, cutoff) -> ResourceEditorDraftOrphans.pruneDirectory(directory, active, cutoff),
                failure -> fail(failure.toString()));
        maintenance.schedule(root);
        queue.removeFirst().run(); // Enumerate before a new owner and XML intent become visible.
        active.add(owned.getName());
        File intent = new File(protectedByIntent, "xml-commit.json");
        Files.write(intent.toPath(), "protected intent".getBytes());
        age(protectedByIntent); // The intent, rather than its age, must preserve this directory.
        while (!queue.isEmpty()) queue.removeFirst().run();
        assertTrue(owned.exists());
        assertTrue(intent.exists());
        assertEquals(1, new File(owned, "pages").list().length);
        assertEquals(1, new File(protectedByIntent, "pages").list().length);

        active.clear();
        assertTrue(intent.delete());
        age(protectedByIntent);
        maintenance.schedule(root);
        while (!queue.isEmpty()) queue.removeFirst().run();
        assertFalse(owned.exists());
        assertFalse(protectedByIntent.exists());
    }
}
