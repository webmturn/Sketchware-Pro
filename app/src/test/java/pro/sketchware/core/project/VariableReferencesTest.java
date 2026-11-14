package pro.sketchware.core.project;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import pro.sketchware.activities.editor.logic.CustomVariableUtil;
import pro.sketchware.beans.BlockBean;

public class VariableReferencesTest {
    private static final String PAGE = "MainActivity.java";

    @Test
    public void findsVariableSelectorsAndReadsButIgnoresLiteralsAndArguments() {
        var project = new ProjectDataStore("test");
        BlockBean assignment =
                block("1", "setVarInt", "set %m.varInt to %d", " ", "", "score", "1");
        assignment.disabled = true;
        put(
                project,
                PAGE,
                assignment,
                block("2", "getVar", "score", "d", ""),
                block("3", "getArg", "score", "d", ""),
                block("4", "doToast", "toast %s", " ", "", "score"),
                block("5", "setVarString", "set %m.varStr to %s", " ", "", "other", "score"),
                block("6", "custom", "show \\%m.varInt %s", " ", "", "score"));
        put(project, "OtherActivity.java", block("7", "getVar", "score", "d", ""));

        assertEquals(List.of("1", "2"), references(project, "score", false, null));
    }

    @Test
    public void distinguishesCustomVariablesAndListsFromOtherSelectors() {
        var project = new ProjectDataStore("test");
        put(
                project,
                PAGE,
                block("1", "getVar", "clock", "v", "Clock"),
                block("2", "custom", "format %m.Clock", " ", "", "clock"),
                block("3", "custom", "format %m.intent", " ", "", "clock"),
                block("4", "getVar", "rows", "l", "List"),
                block("5", "lengthList", "size %m.list", "d", "", "rows"),
                block("6", "getArg", "rows", "l", "List String"),
                block("7", "listRefresh", "refresh %m.listview", " ", "", "rows"));

        assertEquals(List.of("1", "2"), references(project, "clock", false, "Clock"));
        assertEquals(List.of("4", "5"), references(project, "rows", true, null));
    }

    @Test
    public void findsQualifiedCustomTypeAfterTheEditorShortensItsTypeName() {
        var project = new ProjectDataStore("test");
        String declaration = "private java.time.Instant clock = java.time.Instant.now()";
        put(
                project,
                PAGE,
                block("1", "getVar", "clock", "v", "java"),
                block("2", "custom", "format %m.java.time.Instant", " ", "", "clock"));

        assertEquals("clock", CustomVariableUtil.getVariableName(declaration));
        assertEquals("java.time.Instant", CustomVariableUtil.getVariableType(declaration));
        assertEquals(List.of("1", "2"), references(project, "clock", false, "java.time.Instant"));
    }

    @Test
    public void findsVariableReadsAndWritesInSourceBlocks() {
        var project = new ProjectDataStore("test");
        put(
                project,
                PAGE,
                source("1", "long now = time();\r\nthis.deadline = now + 1000;"),
                source("2", "if (deadline > time()) {"),
                source("3", "MainActivity.this.deadline++;"),
                source("4", "String label = \"deadline\"; // deadline\n/* deadline */"),
                source("5", "long last_deadline = other.deadline; deadline();"),
                source("6", "if (other.deadline() > 0) { // deadline"),
                source("7", "rows.add(\"deadline\");"),
                source("8", "dead\\u006cine++;"));

        assertEquals(List.of("1", "2", "3", "8"), references(project, "deadline", false, null));
        assertEquals(List.of("7"), references(project, "rows", true, null));
        var results = project.findVariableReferences(PAGE, "deadline", false, null);
        assertEquals("this.deadline = now + 1000;", results.get(0).sourceLine);
        assertEquals("if (deadline > time()) {", results.get(1).sourceLine);
    }

    @Test
    public void sourceReferencesResolveToThePageField() {
        var project = new ProjectDataStore("test");
        project.buildConfig.packageName = "com.example.app";
        put(
                project,
                PAGE,
                source("1", "MainActivity.deadline++;"),
                source("2", "long deadline = 0; consume(deadline);"),
                source("3", "task.onTick(deadline -> consume(deadline));"),
                source(
                        "4",
                        "var holder = new Object() { long deadline; void reset() { this.deadline++; } };"),
                source("5", "long deadline = 0; this.deadline++;"),
                source(
                        "6",
                        "var holder = new Object() { void reset() { MainActivity.this.deadline++; } };"),
                source("7", "if (MainActivity.deadline > 0) {"),
                source("8", "try { test(); } catch (Exception deadline) { consume(deadline); }"),
                source("9", "new Runnable() { public void run() { deadline++; } };"));

        assertEquals(
                List.of("1", "5", "6", "7", "9"), references(project, "deadline", false, null));
    }

    @Test
    public void packagePrefixMustNotResolveToAVariable() {
        var project = new ProjectDataStore("test");
        project.buildConfig.packageName = "com.example.app";
        put(
                project,
                PAGE,
                source("1", "com.example.app.MainActivity.deadline++;"),
                source("2", "com.example.other.MainActivity.deadline++;"),
                source(
                        "3",
                        "ForeignReceiver com = receiver(); com.example.app.MainActivity.deadline++;"),
                source("4", "run(com -> com.example.app.MainActivity.deadline++);"),
                source(
                        "5",
                        "if (receiver instanceof ForeignReceiver com) com.example.app.MainActivity.deadline++;"));

        assertEquals(List.of("1"), references(project, "deadline", false, null));
    }

    @Test
    public void unknownCallbackCanShadowPageFields() {
        var project = new ProjectDataStore("test");
        project.buildConfig.packageName = "com.example.app";
        put(
                project,
                PAGE,
                source("1", "new ForeignCallback() { public void run() { deadline++; } };"),
                source(
                        "2",
                        "new ForeignCallback() { int deadline; public void run() { deadline++; } };"),
                source(
                        "3",
                        "new ForeignCallback() { public void run(int deadline) { deadline++; } };"),
                source(
                        "4",
                        "new ForeignCallback() { public void run() { com.example.app.MainActivity.deadline++; } };"),
                source(
                        "5",
                        "new ForeignCallback() { public void run() { MainActivity.this.deadline++; } };"));

        assertEquals(List.of("5"), references(project, "deadline", false, null));
    }

    @Test
    public void connectedSourceBlocksShareCommentsAndLocalScope() {
        var project = new ProjectDataStore("test");
        BlockBean commentStart = source("1", "/*");
        BlockBean commentedOut = source("2", "deadline++;");
        BlockBean commentEnd = source("3", "*/");
        BlockBean declareLocal = source("4", "long deadline = 0;");
        BlockBean localUse = source("5", "deadline++;");
        BlockBean fieldUse = source("6", "this.deadline++;");
        commentStart.nextBlock = 2;
        commentedOut.nextBlock = 3;
        commentEnd.nextBlock = 4;
        declareLocal.nextBlock = 5;
        localUse.nextBlock = 6;
        commentedOut.disabled = true;
        localUse.disabled = true;
        put(
                project,
                PAGE,
                commentStart,
                fieldUse,
                commentedOut,
                localUse,
                commentEnd,
                declareLocal);

        assertEquals(List.of("6"), references(project, "deadline", false, null));
    }

    @Test
    public void disabledDeclarationDoesNotMaskAnEnabledReference() {
        var project = new ProjectDataStore("test");
        BlockBean declaration = source("1", "int deadline = 0;");
        declaration.disabled = true;
        declaration.nextBlock = 2;
        put(project, PAGE, declaration, source("2", "deadline++;"));

        assertEquals(List.of("2"), references(project, "deadline", false, null));
    }

    @Test
    public void sourceSearchKeepsSubstackScopeAndIncludesDisabledBlocks() {
        var project = new ProjectDataStore("test");
        BlockBean declareLocal = source("1", "long deadline = 0;");
        BlockBean condition = block("2", "if", "if %b", "c", "", "true");
        BlockBean inside = source("3", "deadline++; this.deadline++;");
        BlockBean disabledSource = source("4", "MainActivity.deadline++;");
        BlockBean disabledCondition = block("5", "if", "if %b", "c", "", "true");
        BlockBean disabledSubstack = source("6", "MainActivity.deadline--;");
        declareLocal.nextBlock = 2;
        condition.subStack1 = 3;
        condition.nextBlock = 4;
        disabledSource.disabled = true;
        disabledSource.nextBlock = 5;
        disabledCondition.disabled = true;
        disabledCondition.subStack1 = 6;
        put(
                project,
                PAGE,
                declareLocal,
                condition,
                inside,
                disabledSource,
                disabledCondition,
                disabledSubstack);

        assertEquals(List.of("3", "4", "6"), references(project, "deadline", false, null));
    }

    private static BlockBean source(String id, String code) {
        return block(id, "addSourceDirectly", "add source directly %s.inputOnly", " ", "", code);
    }

    private static void put(ProjectDataStore project, String file, BlockBean... blocks) {
        project.putBlocks(file, "onCreate_initializeLogic", new ArrayList<>(List.of(blocks)));
    }

    private static BlockBean block(
            String id,
            String opcode,
            String spec,
            String type,
            String typeName,
            String... parameters) {
        BlockBean block = new BlockBean();
        block.id = id;
        block.opCode = opcode;
        block.spec = spec;
        block.type = type;
        block.typeName = typeName;
        block.parameters.addAll(Arrays.asList(parameters));
        return block;
    }

    private static List<String> references(
            ProjectDataStore project, String name, boolean isList, String customType) {
        return project.findVariableReferences(PAGE, name, isList, customType).stream()
                .map(reference -> reference.blockId)
                .toList();
    }
}
