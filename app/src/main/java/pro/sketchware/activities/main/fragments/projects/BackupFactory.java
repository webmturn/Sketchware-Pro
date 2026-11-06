package pro.sketchware.activities.main.fragments.projects;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pro.sketchware.R;
import pro.sketchware.activities.design.CustomBlocksManager;
import pro.sketchware.beans.BlockBean;
import pro.sketchware.core.block.ExtraBlockInfo;
import pro.sketchware.core.codegen.BlockLoader;
import pro.sketchware.core.library.LocalLibrariesUtil;
import pro.sketchware.core.project.BackupArchiveInspector;
import pro.sketchware.core.project.ProjectListManager;
import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.core.settings.ApplicationSettings;
import pro.sketchware.util.BackupFilenameFormatter;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.Helper;
import pro.sketchware.util.MapValueHelper;
import pro.sketchware.util.SketchwareUtil;

public class BackupFactory {
    public static final String EXTENSION = "swb";
    public static final String DEF_PATH = SketchwarePaths.BACKUPS_PATH + File.separator;

    // WARNING: These crypto constants are part of the original Sketchware project file format.
    // Do NOT change them — doing so will break reading/writing of all existing project files
    // and backups, as well as compatibility with other Sketchware variants.
    // The key == IV pattern is a known weakness but is required for format compatibility.
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] LEGACY_KEY =
            "sketchwaresecure".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private static final String[] resSubfolders = {"fonts", "icons", "images", "sounds"};

    final String sc_id;
    File outPath;
    boolean backupLocalLibs;
    boolean backupCustomBlocks;
    String error = "";
    boolean restoreSuccess;

    /**
     * @param sc_id For backing up, the target project's ID, for restoring, the new project ID
     */
    public BackupFactory(String sc_id) {
        this.sc_id = sc_id;
    }

    public static String getBackupDir() {
        return SketchwarePaths.resolveExternalStorageRelativePath(
                ApplicationSettings.getBackupPath());
    }

    private static File getAllLocalLibsDir() {
        return SketchwarePaths.getLocalLibsDir();
    }

    private static HashMap<String, Object> getProject(File file) {
        try {
            byte[] data;
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                data = new byte[(int) raf.length()];
                raf.readFully(data);
            }
            try {
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                cipher.init(
                        Cipher.DECRYPT_MODE,
                        new SecretKeySpec(LEGACY_KEY, "AES"),
                        new IvParameterSpec(LEGACY_KEY));
                byte[] decrypted = cipher.doFinal(data);
                return new Gson()
                        .fromJson(
                                new String(decrypted, java.nio.charset.StandardCharsets.UTF_8)
                                        .trim(),
                                Helper.TYPE_MAP);
            } catch (Exception e) {
                // Decryption failed — project file is stored as plaintext
                return new Gson()
                        .fromJson(
                                new String(data, java.nio.charset.StandardCharsets.UTF_8).trim(),
                                Helper.TYPE_MAP);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean writeEncrypted(File file, String string) {
        String path = file.getAbsolutePath();

        try {
            byte[] data;
            if (ApplicationSettings.isSettingEnabled(
                    ApplicationSettings.SETTING_PROJECT_DATA_ENCRYPTION)) {
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                cipher.init(
                        Cipher.ENCRYPT_MODE,
                        new SecretKeySpec(LEGACY_KEY, "AES"),
                        new IvParameterSpec(LEGACY_KEY));
                data =
                        cipher.doFinal(
                                (string.trim()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } else {
                data = string.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
                raf.setLength(0);
                raf.write(data);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getNewScId() {
        File myscList = new File(SketchwarePaths.getProjectListBasePath());

        ArrayList<String> list = new ArrayList<>();
        FileUtil.listDir(myscList.getAbsolutePath(), list);
        //noinspection Java8ListSort
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);

        int id = 600;
        for (String path : list) {
            try {
                id = Math.max(id, Integer.parseInt(new File(path).getName()));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.valueOf(id + 1);
    }

    /************************ UTILITIES ************************/

    public static boolean unzip(File zipFile, File destinationDir) {
        int DEFAULT_BUFFER = 2048;
        try (ZipFile zip = new ZipFile(zipFile)) {
            destinationDir.mkdirs();
            String canonicalDestDir = destinationDir.getCanonicalPath() + File.separator;
            Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = zipFileEntries.nextElement();
                String entryName = entry.getName();
                File destFile = new File(destinationDir, entryName);
                if (!destFile.getCanonicalPath().startsWith(canonicalDestDir)) {
                    throw new IOException("Zip entry outside target dir: " + entryName);
                }
                File destinationParent = destFile.getParentFile();
                if (destinationParent != null && !destinationParent.exists()) {
                    destinationParent.mkdirs();
                }
                if (!entry.isDirectory()) {
                    try (BufferedInputStream is =
                            new BufferedInputStream(zip.getInputStream(entry))) {
                        int currentByte;
                        byte[] data = new byte[DEFAULT_BUFFER];
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            try (BufferedOutputStream dest =
                                    new BufferedOutputStream(fos, DEFAULT_BUFFER)) {
                                while ((currentByte = is.read(data, 0, DEFAULT_BUFFER))
                                        != -1 /*EOF*/) {
                                    dest.write(data, 0, currentByte);
                                }
                                dest.flush();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void zipFolder(File srcFolder, File destZipFile) throws IOException {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile)) {
            try (ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
                addFolderToZip(srcFolder, srcFolder, zip);
                zip.flush();
            }
        }
    }

    private static void addFileToZip(File rootPath, File srcFile, ZipOutputStream zip)
            throws IOException {

        if (srcFile.isDirectory()) {
            addFolderToZip(rootPath, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try (FileInputStream in = new FileInputStream(srcFile)) {
                String name = srcFile.getPath();
                name = name.replace(rootPath.getPath() + "/", "");
                zip.putNextEntry(new ZipEntry(name));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }

    private static void addFolderToZip(File rootPath, File srcFolder, ZipOutputStream zip)
            throws IOException {
        File[] srcFolderFiles = srcFolder.listFiles();
        if (srcFolderFiles == null) {
            throw new IOException("Cannot list backup directory: " + srcFolder);
        }
        for (File fileName : srcFolderFiles) {
            addFileToZip(rootPath, fileName, zip);
        }
    }

    public static void createNomediaFileIn(File dir) throws IOException {
        Files.write(new File(dir, ".nomedia").toPath(), new byte[0]);
    }

    public static void copySafe(File source, File destination) throws IOException {
        if (!source.exists()) {
            Files.createDirectories(destination.toPath());
            createNomediaFileIn(destination);
        } else {
            copy(source, destination);
        }
    }

    public static void copy(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            Files.createDirectories(destination.toPath());
            String[] files = source.list();
            if (files == null) {
                throw new IOException("Cannot list backup directory: " + source);
            }
            for (String file : files) {
                copy(new File(source, file), new File(destination, file));
            }
        } else {
            // skip .nomedia files
            if (source.getName().equals(".nomedia")) return;

            Path target = destination.toPath().toAbsolutePath();
            Path pending = Files.createTempFile(target.getParent(), ".backup-copy-", ".tmp");
            try {
                Files.copy(source.toPath(), pending, StandardCopyOption.REPLACE_EXISTING);
                replaceFile(pending, target, Files::move);
            } finally {
                Files.deleteIfExists(pending);
            }
        }
    }

    @FunctionalInterface
    interface FileMove {
        void move(Path source, Path target, CopyOption... options) throws IOException;
    }

    static void replaceFile(Path pending, Path target, FileMove move) throws IOException {
        try {
            move.move(
                    pending,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (AtomicMoveNotSupportedException unsupported) {
            // A non-atomic move may damage the target before it throws.
        }

        Path previous = null;
        try {
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target)) {
                    throw new IOException("Copy target is not a regular file: " + target);
                }
                previous = Files.createTempFile(target.getParent(), ".backup-previous-", ".tmp");
                Files.copy(target, previous, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                move.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | RuntimeException failure) {
                try {
                    if (previous != null) {
                        Files.copy(previous, target, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(target);
                    }
                } catch (IOException | RuntimeException recoveryFailure) {
                    if (previous != null) {
                        failure.addSuppressed(
                                new IOException(
                                        "Previous file preserved at " + previous, recoveryFailure));
                        previous = null;
                    } else {
                        failure.addSuppressed(recoveryFailure);
                    }
                }
                throw failure;
            }
        } finally {
            if (previous != null) {
                Files.deleteIfExists(previous);
            }
        }
    }

    public static boolean zipContainsFile(String zipPath, String fileName) {
        try {
            return BackupArchiveInspector.containsEntry(new File(zipPath), fileName);
        } catch (Exception e) {
            Log.e("BackupFactory", "Failed to check zip contents", e);
        }
        return false;
    }

    /************************ BACKUP ************************/

    public void backup(Context context, String project_name) {
        outPath = null;
        error = "";
        try {
            backupProject(context, project_name);
        } catch (IOException | RuntimeException e) {
            outPath = null;
            error = Log.getStackTraceString(e);
            Log.e("BackupFactory", "Failed to back up project " + sc_id, e);
        }
    }

    private void backupProject(Context context, String project_name) throws IOException {
        String customFileName = ApplicationSettings.getBackupFileName();

        String versionName =
                MapValueHelper.getString(ProjectListManager.getProjectById(sc_id), "sc_ver_name");
        String versionCode =
                MapValueHelper.getString(ProjectListManager.getProjectById(sc_id), "sc_ver_code");
        String pkgName =
                MapValueHelper.getString(
                        ProjectListManager.getProjectById(sc_id), "my_sc_pkg_name");
        String projectNameOnly = project_name.replace("_d", "").replace(File.separator, "");
        String finalFileName;

        try {
            finalFileName =
                    BackupFilenameFormatter.format(
                            customFileName,
                            projectNameOnly,
                            versionName,
                            versionCode,
                            pkgName,
                            System.currentTimeMillis());
        } catch (Exception ignored) {
            SketchwareUtil.toastError(Helper.getResString(R.string.error_parse_backup_filename));
            // Example name: InternalDemo v1.0 (com.jbk.internal.demo, 1) 2021-12-31T125827
            finalFileName =
                    projectNameOnly
                            + " v"
                            + versionName
                            + " ("
                            + pkgName
                            + ", "
                            + versionCode
                            + ") "
                            + getFormattedDateFrom("yyyy-M-dd'T'HHmmss");
        }
        createBackupsFolder();

        // Init output zip file
        File outZip =
                new File(
                        getBackupDir() + File.separator + projectNameOnly,
                        finalFileName
                                +
                                // Adds all the _d if exists. Otherwise its possible that there'll
                                // be an infinite loop
                                (project_name.contains("_d")
                                        ? project_name.replace(projectNameOnly, "")
                                        : "")
                                + "."
                                + EXTENSION);

        // Create a duplicate if already exists (impossible now :3)
        if (outZip.exists()) {
            backupProject(context, project_name + "_d");
            return;
        }
        File outFolder =
                Files.createTempDirectory(new File(getBackupDir()).toPath(), ".backup-").toFile();
        Path pendingArchive = null;
        try {
            Files.createDirectories(outZip.toPath().getParent());

            // Copy data
            File dataF = new File(outFolder, "data");
            copySafe(getDataDir(), dataF);

            // Copy res
            File resF = new File(outFolder, "resources");
            Files.createDirectories(resF.toPath());

            for (String subfolder : resSubfolders) {
                File resSubf = new File(resF, subfolder);
                Files.createDirectories(resSubf.toPath());

                // 6.3.0 fix1
                copySafe(getResDir(subfolder), resSubf);

                // Write an empty file inside each folder (except icons)
                if (!subfolder.equals("icons")) {
                    // 6.3.0 fix1
                    createNomediaFileIn(resSubf);
                    // FileUtil.writeFile(new File(resSubf, ".nomedia").getAbsolutePath(), "");
                }
            }

            // Copy project
            File projectF = new File(outFolder, "project");
            copy(getProjectPath(), projectF);

            // Find local libs used and include them in the backup
            if (backupLocalLibs) {
                File localLibs = getLocalLibsPath();

                if (localLibs.exists()) {
                    try {
                        JSONArray ja =
                                new JSONArray(FileUtil.readFile(localLibs.getAbsolutePath()));

                        File libsF = new File(outFolder, "local_libs");
                        Files.createDirectories(libsF.toPath());

                        for (int i = 0; i < ja.length(); i++) {
                            JSONObject jo = ja.getJSONObject(i);
                            String libName = jo.optString("name", "");
                            if (libName.isEmpty()) continue;

                            File libDir = new File(getAllLocalLibsDir(), libName);
                            if (!libDir.exists()) {
                                libDir =
                                        new File(
                                                SketchwarePaths.getLocalLibsFallbackDir(), libName);
                            }
                            if (libDir.isDirectory()) {
                                copy(libDir, new File(libsF, libName));
                            }
                        }

                    } catch (JSONException e) {
                        throw new IOException(
                                "Invalid local library metadata for project " + sc_id, e);
                    }
                }
            }

            // Find custom blocks used and include them in the backup
            if (backupCustomBlocks) {
                CustomBlocksManager cbm = new CustomBlocksManager(context, sc_id);

                Set<ExtraBlockInfo> blocks = new HashSet<>();
                Set<String> block_names = new HashSet<>();
                for (BlockBean bean : cbm.getUsedBlocks()) {
                    if (!block_names.contains(bean.opCode)) {
                        block_names.add(bean.opCode);
                        // Match BlockInterpreter: global definition first, project definition as
                        // fallback.
                        ExtraBlockInfo block = BlockLoader.getBlockInfo(bean.opCode);
                        if (block.isMissing) {
                            block = BlockLoader.getBlockFromProject(sc_id, bean.opCode);
                        }
                        // A missing placeholder loses its transient flag when serialized.
                        if (!block.isMissing) {
                            blocks.add(block);
                        }
                    }
                }

                String json = new Gson().toJson(blocks);

                File customBlocksF = new File(dataF, "custom_blocks");
                Files.write(customBlocksF.toPath(), json.getBytes(StandardCharsets.UTF_8));
            }

            pendingArchive = Files.createTempFile(outZip.toPath().getParent(), ".backup-", ".tmp");
            zipFolder(outFolder, pendingArchive.toFile());
            Files.move(pendingArchive, outZip.toPath());
            outPath = outZip;
        } finally {
            FileUtil.deleteFile(outFolder.getAbsolutePath());
            if (pendingArchive != null) {
                FileUtil.deleteFile(pendingArchive.toString());
            }
        }
    }

    private String getFormattedDateFrom(String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH)
                .format(Calendar.getInstance().getTime());
    }

    public File getOutFile() {
        return outPath;
    }

    public void setBackupLocalLibs(boolean backupLocalLibs) {
        this.backupLocalLibs = backupLocalLibs;
    }

    public void setBackupCustomBlocks(boolean backupCustomBlocks) {
        this.backupCustomBlocks = backupCustomBlocks;
    }

    /************************ RESTORE ************************/

    public void restore(File swbPath) {
        restoreSuccess = false;
        error = "";
        try {
            if (getProjectPath().exists()) {
                throw new IOException("Project already exists: " + sc_id);
            }
            restoreSuccess = restoreProject(swbPath);
        } catch (IOException | RuntimeException e) {
            error = Log.getStackTraceString(e);
            Log.e("BackupFactory", "Failed to restore project " + sc_id, e);
        }
    }

    private boolean restoreProject(File swbPath) throws IOException {
        createBackupsFolder();
        File outFolder =
                Files.createTempDirectory(new File(getBackupDir()).toPath(), ".restore-").toFile();
        try {
            return restoreContents(swbPath, outFolder);
        } finally {
            FileUtil.deleteFile(outFolder.getAbsolutePath());
        }
    }

    private boolean restoreContents(File swbPath, File outFolder) throws IOException {
        // Unzip
        if (!unzip(swbPath, outFolder)) {
            error = Helper.getResString(R.string.backup_unzip_failed);
            return false;
        }

        // Init files
        File project = new File(outFolder, "project");
        File data = new File(outFolder, "data");
        File res = new File(outFolder, "resources");

        HashMap<String, Object> map = getProject(project);

        if (map == null) {
            error = Helper.getResString(R.string.backup_read_project_failed);
            return false;
        }

        // Put new sc_id
        map.put("sc_id", sc_id);

        // Write new file
        if (!writeEncrypted(project, new Gson().toJson(map))) {
            error = Helper.getResString(R.string.backup_write_project_failed);
            return false;
        }

        // Copy data
        copy(data, getDataDir());

        // Copy res
        for (String subfolder : resSubfolders) {
            File subf = new File(res, subfolder);

            copySafe(subf, getResDir(subfolder));
        }

        // Copy local libs if they do not exist
        if (backupLocalLibs) {
            File local_libs = new File(outFolder, "local_libs");

            if (local_libs.exists()) {
                File[] local_libs_content = local_libs.listFiles();
                if (local_libs_content != null) {

                    for (File local_lib : local_libs_content) {

                        File primaryPath = new File(getAllLocalLibsDir(), local_lib.getName());
                        File fallbackPath =
                                new File(
                                        SketchwarePaths.getLocalLibsFallbackDir(),
                                        local_lib.getName());

                        if (!primaryPath.exists() && !fallbackPath.exists()) {
                            try {
                                Files.createDirectories(primaryPath.toPath());
                                copy(local_lib, primaryPath);
                            } catch (IOException primaryFailure) {
                                // Primary path blocked by FUSE, clean up empty dir
                                // and use app-specific fallback
                                FileUtil.deleteFile(primaryPath.getAbsolutePath());
                                try {
                                    Files.createDirectories(fallbackPath.toPath());
                                    copy(local_lib, fallbackPath);
                                } catch (IOException fallbackFailure) {
                                    FileUtil.deleteFile(fallbackPath.getAbsolutePath());
                                    fallbackFailure.addSuppressed(primaryFailure);
                                    throw fallbackFailure;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rebuild local_library paths to use current device's storage layout
        rebuildLocalLibraryPaths();

        // Publish the project only after its data, resources, and libraries are ready.
        Files.createDirectories(getProjectPath().toPath().getParent());
        copy(project, getProjectPath());

        return true;
    }

    private void rebuildLocalLibraryPaths() throws IOException {
        ArrayList<HashMap<String, Object>> libs = LocalLibrariesUtil.getLocalLibraries(sc_id);
        if (libs.isEmpty()) return;

        ArrayList<HashMap<String, Object>> rebuilt = new ArrayList<>();
        for (HashMap<String, Object> lib : libs) {
            String name = MapValueHelper.getString(lib, "name");
            if (name == null || name.isEmpty()) continue;
            String dependency = MapValueHelper.getString(lib, "dependency");
            rebuilt.add(
                    LocalLibrariesUtil.createLibraryMap(
                            name, dependency.isEmpty() ? null : dependency));
        }
        Files.write(
                LocalLibrariesUtil.getLocalLibFile(sc_id).toPath(),
                new Gson().toJson(rebuilt).getBytes(StandardCharsets.UTF_8));
    }

    public String getError() {
        return error;
    }

    public boolean isRestoreSuccess() {
        return restoreSuccess;
    }

    /************************ SW METHODS ************************/

    private void createBackupsFolder() throws IOException {
        // Create the backups folder if it doesn't exist
        String backupsPath = getBackupDir();

        Files.createDirectories(new File(backupsPath).toPath());
    }

    private File getDataDir() {
        return new File(SketchwarePaths.getDataPath(sc_id));
    }

    private File getResDir(String subfolder) {
        String resourcePath =
                switch (subfolder) {
                    case "fonts" -> SketchwarePaths.getFontsResourcePath();
                    case "icons" -> SketchwarePaths.getIconsPath();
                    case "images" -> SketchwarePaths.getImagesPath();
                    case "sounds" -> SketchwarePaths.getSoundsPath();
                    default ->
                            SketchwarePaths.getAbsolutePathOf(
                                    SketchwarePaths.RESOURCES_PATH + File.separator + subfolder);
                };
        return new File(resourcePath, sc_id);
    }

    private File getProjectPath() {
        return new File(SketchwarePaths.getProjectListPath(sc_id), "project");
    }

    private File getLocalLibsPath() {
        return new File(SketchwarePaths.getProjectLocalLibraryPath(sc_id));
    }
}
