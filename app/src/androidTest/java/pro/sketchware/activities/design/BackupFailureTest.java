package pro.sketchware.activities.design;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import pro.sketchware.activities.main.fragments.projects.BackupFactory;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectListManager;
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
    public void failedRestoreDoesNotLeaveFilesForTheNextArchive() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject nextSource =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            nextSource.fixture.seed(false, false);
            Files.write(
                    new File(source.fixture.data, "custom_blocks").toPath(),
                    "[]".getBytes(StandardCharsets.UTF_8));
            Files.write(
                    new File(SketchwarePaths.getFontsResourcePath(), source.id() + "/first.ttf")
                            .toPath(),
                    new byte[] {1});
            File sourceFolder = new File(SketchwarePaths.getSoundsPath(), source.id() + "/samples");
            assertTrue(sourceFolder.mkdir());
            Files.write(new File(sourceFolder, "sound.wav").toPath(), new byte[] {1, 2, 3});
            File existingEntry =
                    new File(SketchwarePaths.getSoundsPath(), destination.id() + "/samples");
            File destinationFonts =
                    new File(SketchwarePaths.getFontsResourcePath(), destination.id());
            Files.write(existingEntry.toPath(), new byte[] {9});
            File archive = backup(source.id());
            File nextArchive = backup(nextSource.id());
            String[] entries = backupEntries(scope.backups);
            byte[] archiveBytes = Files.readAllBytes(archive.toPath());
            BackupFactory restore = new BackupFactory(destination.id());
            try {
                Files.delete(destination.fixture.data.toPath());
                restore.restore(archive);

                assertFalse(restore.isRestoreSuccess());
                assertFalse(
                        new File(SketchwarePaths.getProjectListPath(destination.id()), "project")
                                .exists());
                assertFalse(destination.fixture.data.exists());
                assertTrue(destinationFonts.isDirectory());
                assertFalse(new File(destinationFonts, "first.ttf").exists());
                assertArrayEquals(new byte[] {9}, Files.readAllBytes(existingEntry.toPath()));
                assertArrayEquals(archiveBytes, Files.readAllBytes(archive.toPath()));
                assertArrayEquals(entries, backupEntries(scope.backups));

                Files.delete(existingEntry.toPath());
                restore.restore(nextArchive);

                assertTrue(restore.getError(), restore.isRestoreSuccess());
                assertFalse(new File(destination.fixture.data, "custom_blocks").exists());
                assertFalse(new File(destinationFonts, "first.ttf").exists());
                assertFalse(existingEntry.exists());
                ProjectDataManager.reloadProjectFromData(destination.id());
                assertTrue(ProjectDataManager.canSaveProject(destination.id()));
                assertArrayEquals(entries, backupEntries(scope.backups));
            } finally {
                Files.createDirectories(destination.fixture.data.toPath());
            }
        }
    }

    @Test
    public void failedDataCopyRemovesFilesAlreadyCopied() throws Exception {
        try (CustomBlockBackupTest.Scope scope = new CustomBlockBackupTest.Scope();
                CustomBlockBackupTest.OwnedProject source =
                        new CustomBlockBackupTest.OwnedProject();
                CustomBlockBackupTest.OwnedProject destination =
                        new CustomBlockBackupTest.OwnedProject()) {
            source.fixture.seed(false, false);
            File archive = backup(source.id());
            String[] entries = backupEntries(scope.backups);
            BackupFactory restore =
                    new BackupFactory(destination.id()) {
                        @Override
                        protected void copyRestoreData(File source, File destination)
                                throws IOException {
                            File withMissingEntry =
                                    new File(source.getPath()) {
                                        @Override
                                        public String[] list() {
                                            return new String[] {"logic", "missing-during-copy"};
                                        }
                                    };
                            var failure =
                                    assertThrows(
                                            NoSuchFileException.class,
                                            () ->
                                                    super.copyRestoreData(
                                                            withMissingEntry, destination));
                            assertTrue(new File(destination, "logic").isFile());
                            throw failure;
                        }
                    };

            restore.restore(archive);

            assertFalse(restore.isRestoreSuccess());
            assertFalse(
                    new File(SketchwarePaths.getProjectListPath(destination.id()), "project")
                            .exists());
            assertArrayEquals(new String[0], backupEntries(destination.fixture.data));
            assertArrayEquals(entries, backupEntries(scope.backups));
        }
    }

    @Test
    public void nextProjectIdSkipsAnUnpublishedDataDirectory() throws Exception {
        String id = ProjectListManager.getNextProjectId();
        var data = new File(SketchwarePaths.getDataPath(id)).toPath();
        Files.createDirectories(data.getParent());
        Files.createDirectory(data);
        try {
            assertFalse(id.equals(ProjectListManager.getNextProjectId()));
        } finally {
            Files.delete(data);
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
