package pro.sketchware.activities.design;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.activities.main.fragments.projects.BackupFactory;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.SketchwarePaths;

@RunWith(AndroidJUnit4.class)
public class BackupFailureTest {
    private static final Context CONTEXT =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void projectWithoutADataDirectoryStillRoundTrips() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            for (String name : List.of("logic", "view", "file", "resource", "library")) {
                Files.delete(new File(source.fixture.data, name).toPath());
            }
            assertTrue(source.fixture.data.delete());
            try {
                File archive = backup(source.id());
                String[] entries = backupEntries(scope.backups);
                try (ZipFile zip = new ZipFile(archive)) {
                    assertNotNull(zip.getEntry("data/.nomedia"));
                }
                BackupFactory restore = new BackupFactory(destination.id());
                restore.restore(archive);

                assertTrue(restore.getError(), restore.isRestoreSuccess());
                assertTrue(
                        new File(SketchwarePaths.getProjectListPath(destination.id()), "project")
                                .isFile());
                assertTrue(destination.fixture.data.isDirectory());
                assertEquals(0, destination.fixture.data.list().length);
                assertFalse(source.fixture.data.exists());
                assertArrayEquals(entries, backupEntries(scope.backups));
            } finally {
                Files.createDirectories(source.fixture.data.toPath());
            }
        }
    }

    @Test
    public void failedBackupClearsPreviousSuccessAndKeepsExistingArchive() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            BackupFactory backup = new BackupFactory(source.id());
            backup.backup(CONTEXT, "copy-failure");
            File original = backup.getOutFile();
            assertNotNull(backup.getError(), original);
            byte[] originalBytes = Files.readAllBytes(original.toPath());
            Files.delete(
                    new File(SketchwarePaths.getProjectListPath(source.id()), "project").toPath());
            var sourceBeforeFailure = source.fixture.snapshot();

            backup.backup(CONTEXT, "copy-failure");

            assertNull(backup.getOutFile());
            assertFalse(backup.getError().isEmpty());
            assertArrayEquals(originalBytes, Files.readAllBytes(original.toPath()));
            source.fixture.assertUnchanged(sourceBeforeFailure);
            try (Stream<java.nio.file.Path> paths = Files.walk(scope.backups.toPath())) {
                assertEquals(1, paths.filter(path -> path.toString().endsWith(".swb")).count());
            }
            try (Stream<java.nio.file.Path> paths = Files.walk(scope.backups.toPath())) {
                assertFalse(
                        paths.anyMatch(
                                path -> path.getFileName().toString().startsWith(".backup-")));
            }
        }
    }

    @Test
    public void failedResourceCopyDoesNotPublishProjectAndCanBeRetried() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            File sourceFolder =
                    new File(SketchwarePaths.getFontsResourcePath(), source.id() + "/blocked");
            assertTrue(sourceFolder.mkdir());
            Files.write(new File(sourceFolder, "font.ttf").toPath(), new byte[] {1, 2, 3});
            File blocked =
                    new File(SketchwarePaths.getFontsResourcePath(), destination.id() + "/blocked");
            Files.write(blocked.toPath(), new byte[] {9});
            File archive = backup(source.id());
            String[] entries = backupEntries(scope.backups);
            byte[] archiveBytes = Files.readAllBytes(archive.toPath());
            BackupFactory restore = new BackupFactory(destination.id());

            restore.restore(archive);

            assertFalse(restore.isRestoreSuccess());
            assertFalse(restore.getError().isEmpty());
            assertFalse(
                    new File(SketchwarePaths.getProjectListPath(destination.id()), "project")
                            .exists());
            assertTrue(new File(destination.fixture.data, "logic").isFile());
            assertArrayEquals(new byte[] {9}, Files.readAllBytes(blocked.toPath()));
            assertArrayEquals(archiveBytes, Files.readAllBytes(archive.toPath()));
            assertArrayEquals(entries, backupEntries(scope.backups));

            Files.delete(blocked.toPath());
            restore.restore(archive);

            assertTrue(restore.getError(), restore.isRestoreSuccess());
            assertTrue(restore.getError().isEmpty());
            assertArrayEquals(
                    new byte[] {1, 2, 3},
                    Files.readAllBytes(new File(blocked, "font.ttf").toPath()));
            ProjectDataManager.reloadProjectFromData(destination.id());
            assertTrue(ProjectDataManager.canSaveProject(destination.id()));
            assertArrayEquals(archiveBytes, Files.readAllBytes(archive.toPath()));
            assertArrayEquals(entries, backupEntries(scope.backups));
        }
    }

    @Test
    public void restoreRefusesToOverwriteAnExistingProject() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            destination.fixture.seed(false, false);
            Files.write(
                    new File(destination.fixture.data, "logic").toPath(),
                    "existing project".getBytes(StandardCharsets.UTF_8));
            var original = destination.fixture.snapshot();
            File archive = backup(source.id());

            BackupFactory restore = new BackupFactory(destination.id());
            restore.restore(archive);

            assertFalse(restore.isRestoreSuccess());
            assertFalse(restore.getError().isEmpty());
            destination.fixture.assertUnchanged(original);
        }
    }

    @Test
    public void malformedArchiveLeavesNoRestoreDirectory() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            File archive = new File(scope.backups, "broken.swb");
            Files.write(archive.toPath(), new byte[] {1, 2, 3});
            String[] entries = backupEntries(scope.backups);
            var original = destination.fixture.snapshot();

            BackupFactory restore = new BackupFactory(destination.id());
            restore.restore(archive);

            assertFalse(restore.isRestoreSuccess());
            assertFalse(restore.getError().isEmpty());
            assertArrayEquals(entries, backupEntries(scope.backups));
            assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(archive.toPath()));
            destination.fixture.assertUnchanged(original);
        }
    }

    @Test
    public void runtimeBackupFailureClearsPreviousSuccessAndReportsTheError() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            BackupFactory backup = new BackupFactory(source.id());
            backup.backup(CONTEXT, "runtime-failure");
            File archive = backup.getOutFile();
            assertNotNull(backup.getError(), archive);
            byte[] bytes = Files.readAllBytes(archive.toPath());
            String[] entries = backupEntries(scope.backups);

            backup.backup(CONTEXT, null);

            assertNull(backup.getOutFile());
            assertFalse(backup.getError().isEmpty());
            assertArrayEquals(bytes, Files.readAllBytes(archive.toPath()));
            assertArrayEquals(entries, backupEntries(scope.backups));
        }
    }

    @Test
    public void runtimeRestoreFailureCleansItsDirectoryAndReportsTheError() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            File archive = backup(source.id());
            byte[] bytes = Files.readAllBytes(archive.toPath());
            String[] entries = backupEntries(scope.backups);
            var original = destination.fixture.snapshot();
            File denied =
                    new File(archive.getAbsolutePath()) {
                        @Override
                        public String getPath() {
                            throw new SecurityException("Archive read denied");
                        }
                    };

            BackupFactory restore = new BackupFactory(destination.id());
            restore.restore(denied);

            assertFalse(restore.isRestoreSuccess());
            assertTrue(restore.getError().contains("Archive read denied"));
            assertArrayEquals(entries, backupEntries(scope.backups));
            assertArrayEquals(bytes, Files.readAllBytes(archive.toPath()));
            destination.fixture.assertUnchanged(original);
        }
    }

    private static String[] backupEntries(File directory) {
        String[] entries = directory.list();
        assertNotNull(entries);
        Arrays.sort(entries);
        return entries;
    }

    private static File backup(String id) {
        BackupFactory backup = new BackupFactory(id);
        backup.backup(CONTEXT, "restore-failure");
        assertNotNull(backup.getError(), backup.getOutFile());
        return backup.getOutFile();
    }
}
