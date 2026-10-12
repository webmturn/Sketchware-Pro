package pro.sketchware.core.library;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pro.sketchware.core.library.DependencyTreeModel.Node;
import pro.sketchware.core.library.DependencyTreeModel.Row;

public class DependencyTreeModelTest {
    private static final String ROOT = "example:root:1";
    private Node node(String name, boolean builtIn, String... parents) {
        return new Node("example:" + name + ":1", name + "-v1", builtIn, !builtIn,
                "1 KB", List.of(parents), parents.length > 0);
    }
    private List<String> ids(List<Row> rows) {
        return rows.stream().filter(row -> !row.isGroup()).map(row -> row.node.coordinate).collect(Collectors.toList());
    }

    @Test public void collapsedTreeShowsDirectNodesAndGroupsOnlyBuiltInLeaves() {
        Node a = node("a", false, ROOT), child = node("child", false, a.coordinate);
        Node internal = node("internal", true, ROOT);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(a, child, internal));
        List<Row> collapsed = model.rows("", true, Set.of(), Set.of());
        assertEquals(List.of(a.coordinate), ids(collapsed));
        assertEquals(1, collapsed.get(1).builtInCount);
        List<Row> expanded = model.rows("", true, Set.of(a.coordinate), Set.of(""));
        assertEquals(List.of(a.coordinate, child.coordinate, internal.coordinate), ids(expanded));
        assertEquals(List.of(a.coordinate, child.coordinate), expanded.get(1).path);
    }

    @Test public void sharedDependencyKeepsBothEdgesWithoutRepeatingItsSubtree() {
        Node a = node("a", false, ROOT), b = node("b", false, ROOT);
        Node shared = node("shared", false, a.coordinate, b.coordinate);
        Node leaf = node("leaf", false, shared.coordinate);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(a, b, shared, leaf));
        List<Row> rows = model.rows("", true, Set.of(a.coordinate, b.coordinate, shared.coordinate), Set.of());
        assertEquals(2, rows.stream().filter(row -> row.node == shared).count());
        assertEquals(1, rows.stream().filter(row -> row.node == leaf).count());
        assertTrue(rows.get(rows.size() - 1).reference);
        assertEquals(List.of(b.coordinate, shared.coordinate), rows.get(rows.size() - 1).path);
    }

    @Test public void levelViewRetainsTheChosenSharedIntroductionPath() {
        Node a = node("a", false, ROOT), b = node("b", false, ROOT);
        Node shared = node("shared", false, a.coordinate, b.coordinate);
        Node child = node("child", false, shared.coordinate);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(a, b, shared, child));
        List<Row> rows = model.rows(List.of(b.coordinate, shared.coordinate), false, Set.of(), Set.of());
        assertEquals(List.of(b.coordinate, shared.coordinate, child.coordinate), rows.get(0).path);
        assertTrue(model.hasKnownPath(rows.get(0).path));
    }

    @Test public void builtInParentsRemainVisibleAndExpandable() {
        Node internal = node("internal", true, ROOT), local = node("local", false, internal.coordinate);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(internal, local));
        List<Row> rows = model.rows("", true, Set.of(internal.coordinate), Set.of());
        assertEquals(List.of(internal.coordinate, local.coordinate), ids(rows));
        assertFalse(rows.get(0).isGroup());
    }

    @Test public void flatFallbackDoesNotInventDirectRelationships() {
        Node legacy = new Node("example:legacy:1", "legacy-v1", false, false,
                "", List.of(ROOT), false);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(legacy));
        assertFalse(model.hasHierarchy());
        assertEquals(0, model.directCount());
        assertFalse(model.hasKnownPath(List.of(legacy.coordinate)));
        assertEquals(List.of(legacy.coordinate), ids(model.rows("", true, Set.of(), Set.of())));
    }

    @Test public void orphanAndCyclicNodesStayVisibleWithoutRecursiveExpansion() {
        Node a = node("a", false, "example:b:1"), b = node("b", false, a.coordinate);
        Node orphan = node("orphan", false, "example:missing:1");
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(a, b, orphan));
        List<Row> rows = model.rows("", true, Set.of(a.coordinate, b.coordinate), Set.of());
        assertTrue(model.isIncomplete());
        assertEquals(0, model.directCount());
        assertFalse(model.hasKnownPath(List.of(orphan.coordinate)));
        assertTrue(ids(rows).containsAll(List.of(a.coordinate, b.coordinate, orphan.coordinate)));
        assertTrue(rows.size() < 10);
        List<Row> level = model.rows(List.of(a.coordinate, b.coordinate), false, Set.of(), Set.of());
        assertTrue(level.get(0).reference);
    }

    @Test public void deepBranchesCanBeOpenedAsAnUnindentedSubtree() {
        List<Node> nodes = new ArrayList<>();
        String parent = ROOT;
        for (int i = 0; i < 12; i++) { Node node = node("n" + i, false, parent); nodes.add(node); parent = node.coordinate; }
        DependencyTreeModel model = new DependencyTreeModel(ROOT, nodes);
        Set<String> expanded = nodes.stream().map(node -> node.coordinate).collect(Collectors.toSet());
        List<Row> rows = model.rows("", true, expanded, Set.of());
        assertEquals(4, rows.size());
        assertEquals(3, rows.get(3).depth);
        List<Row> subtree = model.rows(rows.get(3).path, true, expanded, Set.of());
        assertEquals(0, subtree.get(0).depth);
        assertEquals(nodes.get(4).coordinate, subtree.get(0).node.coordinate);
        assertEquals(5, subtree.get(0).path.size());
    }

    @Test public void differentVersionsRemainSeparateNodes() {
        Node a = node("library", false, ROOT);
        Node b = new Node("example:library:2", "library-v2", false, true, "2 KB", List.of(ROOT), true);
        DependencyTreeModel model = new DependencyTreeModel(ROOT, List.of(a, b));
        assertEquals(2, model.allNodes().size());
        assertEquals(List.of(b.coordinate), model.pathTo(b.coordinate));
    }
}
