package pro.sketchware.core.project;

import static com.github.javaparser.GeneratedJavaParserConstants.EOF;
import static com.github.javaparser.GeneratedJavaParserConstants.LBRACE;
import static com.github.javaparser.GeneratedJavaParserConstants.RBRACE;

import com.github.javaparser.GeneratedJavaParserTokenManager;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Providers;
import com.github.javaparser.SimpleCharStream;
import com.github.javaparser.Token;
import com.github.javaparser.TokenMgrException;
import com.github.javaparser.UnicodeEscapeProcessingProvider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pro.sketchware.beans.BlockBean;

final class SourceBlockReferences {
    private static final JavaSymbolSolver SYMBOLS =
            new JavaSymbolSolver(new ReflectionTypeSolver(true));

    private final JavaParser parser =
            new JavaParser(
                    new ParserConfiguration()
                            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                            .setPreprocessUnicodeEscapes(true)
                            .setAttributeComments(false));
    private final String className;
    private final String name;

    SourceBlockReferences(String className, String name) {
        this.className = className;
        this.name = name;
    }

    Map<String, String> find(List<BlockBean> blocks) {
        if (blocks.stream().noneMatch(this::mayReferenceField)) return Map.of();

        var byId = new HashMap<String, BlockBean>();
        var children = new HashSet<String>();
        for (BlockBean block : blocks) {
            byId.put(block.id, block);
            children.add(Integer.toString(block.nextBlock));
            children.add(Integer.toString(block.subStack1));
            children.add(Integer.toString(block.subStack2));
        }

        var matches = new HashMap<String, String>();
        var visited = new HashSet<String>();
        var deferred = new ArrayList<BlockBean>();
        for (BlockBean block : blocks) {
            if (children.contains(block.id)) continue;
            var lines = new ArrayList<SourceLine>();
            appendStack(block, byId, visited, lines, deferred);
            findReferences(lines, matches);
        }
        // Disabled code can still contain references that reappear when enabled.
        for (int i = 0; i < deferred.size(); i++) {
            BlockBean block = deferred.get(i);
            if (block.disabled && "addSourceDirectly".equals(block.opCode)) {
                findReferences(sourceLines(block), matches);
                continue;
            }
            var lines = new ArrayList<SourceLine>();
            appendStack(block, byId, visited, lines, deferred);
            findReferences(lines, matches);
        }
        return matches;
    }

    private boolean mayReferenceField(BlockBean block) {
        if (!"addSourceDirectly".equals(block.opCode) || block.parameters.isEmpty()) return false;
        String source = block.parameters.get(0);
        return source != null && (source.contains(name) || source.contains("\\u"));
    }

    private void appendStack(
            BlockBean block,
            Map<String, BlockBean> byId,
            Set<String> visited,
            List<SourceLine> lines,
            List<BlockBean> deferred) {
        while (block != null && visited.add(block.id)) {
            if ("addSourceDirectly".equals(block.opCode)) {
                if (block.disabled) deferred.add(block);
                else lines.addAll(sourceLines(block));
            } else {
                // Preserve statement and substack boundaries for the source snippets.
                if (!block.disabled) lines.add(new SourceLine(null, ";"));
                for (int id : new int[] {block.subStack1, block.subStack2}) {
                    BlockBean child = byId.get(Integer.toString(id));
                    if (child == null) continue;
                    if (block.disabled) deferred.add(child);
                    else {
                        lines.add(new SourceLine(null, "{"));
                        appendStack(child, byId, visited, lines, deferred);
                        lines.add(new SourceLine(null, "}"));
                    }
                }
            }
            block = byId.get(Integer.toString(block.nextBlock));
        }
    }

    private static List<SourceLine> sourceLines(BlockBean block) {
        var lines = new ArrayList<SourceLine>();
        if (!block.parameters.isEmpty() && block.parameters.get(0) != null) {
            for (String line : block.parameters.get(0).split("\\r\\n|\\r|\\n", -1)) {
                lines.add(new SourceLine(block.id, line));
            }
        }
        return lines;
    }

    private void findReferences(List<SourceLine> lines, Map<String, String> matches) {
        var source = new StringBuilder("{\n");
        for (SourceLine line : lines) {
            source.append(line.text()).append('\n');
        }
        String code = source.toString();
        if (!code.contains(name) && !code.contains("\\u")) return;

        BlockStmt body = parseBody(code);
        if (body == null) return;
        var unit = new CompilationUnit();
        var owner = unit.addClass(className);
        // Bind names to the page field while resolving the snippet's own locals and parameters.
        var field = owner.addField("Object", name).getVariable(0);
        owner.addMethod("_event").setBody(body);
        SYMBOLS.inject(unit);
        for (SimpleName identifier :
                body.findAll(SimpleName.class, node -> node.asString().equals(name))) {
            if (!refersToField(identifier, field)) continue;
            int index = identifier.getBegin().orElseThrow().line - 2;
            if (index < 0 || index >= lines.size()) continue;
            SourceLine line = lines.get(index);
            if (line.blockId() != null) matches.putIfAbsent(line.blockId(), line.text().trim());
        }
    }

    private BlockStmt parseBody(String code) {
        var parsed = parser.parseBlock(code + "}");
        if (parsed.isSuccessful()) return parsed.getResult().orElseThrow();

        // A source stack can end with an unfinished compound statement.
        int missing = unclosedBraces(code);
        if (missing > 1) parsed = parser.parseBlock(code + "}".repeat(missing));
        return parsed.getResult().orElse(null);
    }

    private static int unclosedBraces(String code) {
        var lexer =
                new GeneratedJavaParserTokenManager(
                        new SimpleCharStream(
                                new UnicodeEscapeProcessingProvider(Providers.provider(code))));
        lexer.setStoreTokens(false);
        int depth = 0;
        try {
            for (Token token = lexer.getNextToken();
                    token.kind != EOF;
                    token = lexer.getNextToken()) {
                if (token.kind == LBRACE) depth++;
                if (token.kind == RBRACE) depth--;
            }
        } catch (TokenMgrException unfinishedLiteral) {
            return 0;
        }
        return depth;
    }

    private static boolean refersToField(SimpleName identifier, VariableDeclarator field) {
        var access = identifier.getParentNode().orElseThrow();
        try {
            if (access instanceof NameExpr variable) return isField(variable.resolve(), field);
            if (access instanceof FieldAccessExpr member) return isField(member.resolve(), field);
            return false;
        } catch (RuntimeException unresolved) {
            // Other project classes and libraries are unavailable in a snippet.
            return false;
        }
    }

    private static boolean isField(ResolvedValueDeclaration declaration, VariableDeclarator field) {
        return declaration instanceof JavaParserFieldDeclaration resolved
                && resolved.getVariableDeclarator() == field;
    }

    private record SourceLine(String blockId, String text) {}
}
