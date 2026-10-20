package pro.sketchware.core.project;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads entry names from the ZIP directory without decompressing file contents. */
public final class BackupArchiveInspector {
    private BackupArchiveInspector() {}

    public static boolean containsEntry(File archive, String rootEntry) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.equals(rootEntry) || name.startsWith(rootEntry + "/")) return true;
            }
            return false;
        }
    }
}
