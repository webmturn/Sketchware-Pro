package pro.sketchware.core.project;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JavaSourceImporterTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void javaAndKotlinPackagesUseTheDestinationNamespace() throws Exception {
        File destination = folder.newFolder();
        File java = source("A.java", "package old;\nclass A {}\n");
        File kotlin = source("B.kt", "package old\nclass B\n");
        JavaSourceImporter.importFile(java, destination, "test.target");
        JavaSourceImporter.importFile(kotlin, destination, "test.target");
        assertEquals("package test.target;\nclass A {}\n", read(new File(destination, "A.java")));
        assertEquals("package test.target\nclass B\n", read(new File(destination, "B.kt")));
        assertEquals("package old;\nclass A {}\n", read(java));
    }

    @Test
    public void sourcesWithoutPackagesKeepTheirUtf8Text() throws Exception {
        File destination = folder.newFolder();
        File source = source("A.java", "// 中文 package example;\nclass A {}\n");
        JavaSourceImporter.importFile(source, destination, "test.target");
        assertEquals(read(source), read(new File(destination, "A.java")));
    }

    @Test
    public void preservesTheClassWhenItSharesThePackageLine() throws Exception {
        File source = source("A.java", "package old.ns; public class A {}\n");
        JavaSourceImporter.importFile(source, source.getParentFile(), "test.target");
        assertEquals("package test.target; public class A {}\n", read(source));
    }

    @Test
    public void ignoresCommentsAndPreservesCrLf() throws Exception {
        String header = "// package line.comment;\r\n/*\r\npackage block.comment;\r\n*/\r\n";
        File source = source("A.java", header + "package old.ns;\r\npublic class A {}\r\n");
        JavaSourceImporter.importFile(source, source.getParentFile(), "test.target");
        assertEquals(header + "package test.target;\r\npublic class A {}\r\n", read(source));
        assertEquals("test.target", JavaSourceImporter.getPackageName(read(source), false));
    }

    @Test
    public void rewritesAnEscapedJavaPackageNameWithoutChangingTheRestOfTheSource()
            throws Exception {
        String prefix =
                "// \\\\u000a package ignored;\r\n"
                        + "\\u002f\\u002f package also.ignored;\\u000a"
                        + "p\\uu0061ckage ";
        String suffix = "\\u003b\r\nclass A { String literal = \"\\\\u0061\"; }\r\n";
        File source = source("A.java", prefix + "\\u006Fld\\u002ens" + suffix);
        File destination = folder.newFolder();

        assertEquals("old.ns", JavaSourceImporter.getPackageName(read(source), false));
        JavaSourceImporter.importFile(source, destination, "test.target");

        assertEquals(prefix + "test.target" + suffix, read(new File(destination, "A.java")));
    }

    @Test
    public void rewritesPackageAtEndOfFileWithoutAddingANewline() throws Exception {
        File source = source("package-info.java", "@Deprecated\npackage old.ns;");
        JavaSourceImporter.importFile(source, source.getParentFile(), "test.target");
        assertEquals("@Deprecated\npackage test.target;", read(source));
    }

    @Test
    public void handlesKotlinFileAnnotationsAndNestedComments() throws Exception {
        String header =
                "/* Outer /* package comment */ still a comment */\n"
                        + "@file:Suppress(\"package annotation )\")\r\n";
        File source = source("A.kt", header + "package old.`when`\r\nclass A\r\n");
        assertEquals("old.when", JavaSourceImporter.getPackageName(read(source), true));
        JavaSourceImporter.importFile(source, source.getParentFile(), "test.target");
        assertEquals(header + "package test.target\r\nclass A\r\n", read(source));
    }

    @Test
    public void incompletePackageDeclarationsDoNotOverwriteOrCreateFiles() throws Exception {
        File destination = folder.newFolder();
        File original = new File(destination, "A.java");
        Files.write(original.toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        File java = source("A.java", "package old.ns\nclass A {}\n");
        File kotlin = source("B.kt", "package old.\r\n");

        assertThrows(
                IOException.class,
                () -> JavaSourceImporter.importFile(java, destination, "test.target"));
        assertThrows(
                IOException.class,
                () -> JavaSourceImporter.importFile(kotlin, destination, "test.target"));
        assertEquals("keep", read(original));
        assertEquals(1, destination.listFiles().length);
        assertEquals("", JavaSourceImporter.getPackageName(read(java), false));
    }

    @Test
    public void missingSourceDoesNotTruncateAnExistingDestination() throws Exception {
        File destination = folder.newFolder();
        File original = new File(destination, "A.java");
        Files.write(original.toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        assertThrows(
                IOException.class,
                () ->
                        JavaSourceImporter.importFile(
                                new File(folder.getRoot(), "A.java"), destination, "test.target"));
        assertEquals("keep", read(original));
        assertEquals(1, destination.listFiles().length);
    }

    @Test
    public void malformedUtf8DoesNotReplaceAnExistingDestination() throws Exception {
        File destination = folder.newFolder();
        File source = folder.newFile("A.java");
        Files.write(source.toPath(), new byte[] {(byte) 0xc3});
        File original = new File(destination, "A.java");
        Files.write(original.toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        assertThrows(
                IOException.class,
                () -> JavaSourceImporter.importFile(source, destination, "test.target"));
        assertEquals("keep", read(original));
        assertEquals(1, destination.listFiles().length);
    }

    @Test
    public void destinationDirectoriesAreNotReplacedByFiles() throws Exception {
        File destination = folder.newFolder();
        File existing = new File(destination, "A.java");
        assertTrue(existing.mkdir());
        File child = new File(existing, "keep");
        Files.write(child.toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        File source = source("A.java", "class A {}\n");
        assertThrows(
                IOException.class,
                () -> JavaSourceImporter.importFile(source, destination, "test.target"));
        assertEquals("keep", read(child));
        assertEquals(1, destination.listFiles().length);
    }

    @Test
    public void importingAFileInPlaceIsSafe() throws Exception {
        File source = source("A.java", "package old;\nclass A {}\n");
        JavaSourceImporter.importFile(source, source.getParentFile(), "test.target");
        assertEquals("package test.target;\nclass A {}\n", read(source));
        assertEquals(1, source.getParentFile().listFiles().length);
    }

    @Test
    public void removedDestinationFoldersAreNotRecreated() throws Exception {
        File source = source("A.java", "class A {}\n");
        File missing = new File(folder.getRoot(), "missing");
        assertThrows(
                IOException.class,
                () -> JavaSourceImporter.importFile(source, missing, "test.target"));
        assertFalse(missing.exists());
    }

    @Test
    public void dollarSignsInPackageNamesAreLiteral() throws Exception {
        File destination = folder.newFolder();
        JavaSourceImporter.importFile(
                source("A.java", "package old;\nclass A {}\n"), destination, "test.$generated");
        assertEquals(
                "package test.$generated;\nclass A {}\n", read(new File(destination, "A.java")));
    }

    private File source(String name, String text) throws Exception {
        File source = folder.newFile(name);
        Files.write(source.toPath(), text.getBytes(StandardCharsets.UTF_8));
        return source;
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
