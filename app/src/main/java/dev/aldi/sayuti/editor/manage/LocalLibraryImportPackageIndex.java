package dev.aldi.sayuti.editor.manage;

import static pro.sketchware.utility.FileUtil.isExistFile;
import static pro.sketchware.utility.FileUtil.readFile;
import static pro.sketchware.utility.FileUtil.writeFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import mod.jbk.util.LogUtil;

public final class LocalLibraryImportPackageIndex {
    private static final String TAG = "LocalLibImportIndex";
    private static final String PACKAGE_INDEX_FILE_NAME = "packages.txt";

    private LocalLibraryImportPackageIndex() {
    }

    public static ArrayList<String> rebuildPackages(File libraryDir) {
        if (libraryDir == null || !libraryDir.isDirectory()) {
            return new ArrayList<>();
        }

        try {
            ArrayList<String> packages = scanPackages(libraryDir);
            writePackageIndex(libraryDir, packages);
            return packages;
        } catch (Throwable e) {
            LogUtil.w(TAG, "Failed to rebuild import package index for " + libraryDir.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    public static ArrayList<String> readPackages(File libraryDir) {
        if (libraryDir == null) {
            return new ArrayList<>();
        }

        File indexFile = getPackageIndexFile(libraryDir);
        if (!isExistFile(indexFile.getAbsolutePath())) {
            return new ArrayList<>();
        }

        return parsePackageIndex(readFile(indexFile.getAbsolutePath()));
    }

    private static ArrayList<String> scanPackages(File libraryDir) throws IOException {
        LinkedHashSet<String> packages = new LinkedHashSet<>();

        // Imports must follow the Java compile classpath, which only contains classes.jar.
        scanJarPackages(new File(libraryDir, "classes.jar"), packages);

        return new ArrayList<>(packages);
    }

    private static void scanJarPackages(File jarFile, LinkedHashSet<String> packages) throws IOException {
        if (!jarFile.isFile()) {
            return;
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(jarFile)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                addJarEntryPackage(entry.getName(), packages);
                zipInputStream.closeEntry();
            }
        }
    }

    private static void addJarEntryPackage(String entryName, LinkedHashSet<String> packages) {
        if (entryName == null
                || entryName.startsWith("META-INF/")
                || !entryName.endsWith(".class")
                || entryName.endsWith("module-info.class")) {
            return;
        }

        int lastSeparator = entryName.lastIndexOf('/');
        if (lastSeparator <= 0) {
            return;
        }

        String packageName = entryName.substring(0, lastSeparator).replace('/', '.').trim();
        if (!packageName.isEmpty()) {
            packages.add(packageName);
        }
    }

    private static void writePackageIndex(File libraryDir, List<String> packages) {
        StringBuilder content = new StringBuilder();
        for (String packageName : packages) {
            if (packageName == null || packageName.trim().isEmpty()) {
                continue;
            }
            if (content.length() > 0) {
                content.append('\n');
            }
            content.append(packageName.trim());
        }

        writeFile(getPackageIndexFile(libraryDir).getAbsolutePath(), content.toString());
    }

    private static ArrayList<String> parsePackageIndex(String rawContent) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String[] lines = rawContent.split("\\R");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                packages.add(trimmed);
            }
        }

        return new ArrayList<>(packages);
    }

    private static File getPackageIndexFile(File libraryDir) {
        return new File(libraryDir, PACKAGE_INDEX_FILE_NAME);
    }
}
