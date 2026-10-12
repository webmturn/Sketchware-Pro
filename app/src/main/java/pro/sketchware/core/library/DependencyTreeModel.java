package pro.sketchware.core.library;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared relationship model for the tree and level browsers. No Android or file I/O. */
public final class DependencyTreeModel {
    public static final String ROOT = "";

    public static final class Node {
        public final String coordinate;
        public final String folderName;
        public final String name;
        public final String group;
        public final String version;
        public final boolean builtIn;
        public final boolean available;
        public final String size;
        public final List<String> parents;
        public final boolean hierarchyKnown;

        public Node(String coordinate, String folderName, boolean builtIn, boolean available,
                    String size, List<String> parents, boolean hierarchyKnown) {
            this.coordinate = coordinate;
            this.folderName = folderName;
            String[] parts = coordinate.split(":", -1);
            name = parts.length == 3 ? parts[1] : coordinate;
            group = parts.length == 3 ? parts[0] : "";
            version = parts.length == 3 ? parts[2] : "";
            this.builtIn = builtIn;
            this.available = available;
            this.size = size;
            this.parents = List.copyOf(parents);
            this.hierarchyKnown = hierarchyKnown;
        }
    }

    public static final class Row {
        public final Node node;
        public final String parent;
        public final int depth;
        public final boolean last;
        public final List<Boolean> ancestorLines;
        public final List<String> path;
        public final boolean reference;
        public final int builtInCount;

        private Row(Node node, String parent, int depth, boolean last, List<Boolean> ancestorLines,
                    List<String> path, boolean reference, int builtInCount) {
            this.node = node;
            this.parent = parent;
            this.depth = depth;
            this.last = last;
            this.ancestorLines = List.copyOf(ancestorLines);
            this.path = List.copyOf(path);
            this.reference = reference;
            this.builtInCount = builtInCount;
        }

        public boolean isGroup() { return node == null; }
        public String id() { return isGroup() ? "group:" + parent : node.coordinate; }
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, List<Node>> children = new LinkedHashMap<>();
    private final Set<String> attached = new HashSet<>();
    private final Set<String> rootEdges = new HashSet<>();
    private boolean hierarchy;
    private boolean incomplete;

    public DependencyTreeModel(String rootCoordinate, List<Node> entries) {
        for (Node node : entries) nodes.putIfAbsent(node.coordinate, node);
        for (Node node : nodes.values()) {
            if (!node.hierarchyKnown) continue;
            for (String parent : new HashSet<>(node.parents)) {
                if (parent.equals(node.coordinate)) continue;
                if (parent.equals(rootCoordinate) && !parent.isEmpty()) {
                    addChild(ROOT, node);
                    rootEdges.add(node.coordinate);
                } else if (nodes.containsKey(parent)) {
                    addChild(parent, node);
                } else {
                    incomplete = true;
                    continue;
                }
                attached.add(node.coordinate);
                hierarchy = true;
            }
        }
        // Keep flat legacy entries and broken/orphaned relationships visible.
        for (Node node : nodes.values()) {
            if (!attached.contains(node.coordinate)) {
                addChild(ROOT, node);
                if (hierarchy) incomplete = true;
            }
        }
        Set<String> reachable = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>(children(ROOT));
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (reachable.add(node.coordinate)) queue.addAll(children(node.coordinate));
        }
        for (Node node : nodes.values()) {
            if (!reachable.contains(node.coordinate)) {
                addChild(ROOT, node);
                incomplete = true;
            }
        }
    }

    private void addChild(String parent, Node node) {
        List<Node> list = children.computeIfAbsent(parent, k -> new ArrayList<>());
        if (!list.contains(node)) list.add(node);
    }

    public Node node(String coordinate) { return nodes.get(coordinate); }
    public List<Node> allNodes() { return List.copyOf(nodes.values()); }
    public List<Node> children(String coordinate) {
        return children.getOrDefault(coordinate, Collections.emptyList());
    }
    public boolean hasHierarchy() { return hierarchy; }
    public boolean isIncomplete() { return incomplete; }
    public int directCount() { return rootEdges.size(); }

    public boolean hasKnownPath(List<String> path) {
        if (path.isEmpty() || !rootEdges.contains(path.get(0))) return false;
        for (int i = 1; i < path.size(); i++) {
            Node child = node(path.get(i));
            if (child == null || !children(path.get(i - 1)).contains(child)) return false;
        }
        return true;
    }

    public List<Row> rows(String parent, boolean tree, Set<String> expanded, Set<String> groups) {
        return rows(pathTo(parent), tree, expanded, groups);
    }

    public List<Row> rows(List<String> parentPath, boolean tree, Set<String> expanded, Set<String> groups) {
        String parent = parentPath.isEmpty() ? ROOT : parentPath.get(parentPath.size() - 1);
        List<Row> result = new ArrayList<>();
        Set<String> seen = new HashSet<>(parentPath);
        appendChildren(parent, tree, expanded, groups, 0, List.of(), parentPath, seen, result);
        return result;
    }

    private void appendChildren(String parent, boolean tree, Set<String> expanded, Set<String> groups,
                                int depth, List<Boolean> lines, List<String> path,
                                Set<String> seen, List<Row> result) {
        List<Node> normal = new ArrayList<>(), builtInLeaves = new ArrayList<>();
        for (Node node : children(parent)) {
            if (node.builtIn && children(node.coordinate).isEmpty()) builtInLeaves.add(node);
            else normal.add(node);
        }
        boolean hasGroup = !builtInLeaves.isEmpty();
        for (int i = 0; i < normal.size(); i++) {
            Node node = normal.get(i);
            boolean last = !hasGroup && i == normal.size() - 1;
            appendNode(node, parent, tree, expanded, groups, depth, last, lines, path, seen, result);
        }
        if (hasGroup) {
            result.add(new Row(null, parent, depth, true, lines, path, false, builtInLeaves.size()));
            if (groups.contains(parent)) {
                List<Boolean> childLines = new ArrayList<>(lines);
                childLines.add(false);
                for (int i = 0; i < builtInLeaves.size(); i++) {
                    // The group is a presentation container, not a Maven parent.
                    appendNode(builtInLeaves.get(i), parent, false, expanded, groups, depth + 1,
                            i == builtInLeaves.size() - 1, childLines, path, seen, result);
                }
            }
        }
    }

    private void appendNode(Node node, String parent, boolean tree, Set<String> expanded,
                            Set<String> groups, int depth, boolean last, List<Boolean> lines,
                            List<String> path, Set<String> seen, List<Row> result) {
        boolean reference = !seen.add(node.coordinate);
        List<String> nodePath = new ArrayList<>(path);
        nodePath.add(node.coordinate);
        result.add(new Row(node, parent, depth, last, lines, nodePath, reference, 0));
        // Bound visible indentation. Deeper branches are opened as a subtree by the UI.
        if (tree && !reference && depth < 3 && expanded.contains(node.coordinate)) {
            List<Boolean> childLines = new ArrayList<>(lines);
            childLines.add(!last);
            appendChildren(node.coordinate, true, expanded, groups, depth + 1,
                    childLines, nodePath, seen, result);
        }
    }

    /** Shortest known introduction path, guarded against cycles. ROOT is implicit. */
    public List<String> pathTo(String coordinate) {
        if (coordinate.isEmpty()) return List.of();
        Map<String, String> previous = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Node node : children(ROOT)) {
            previous.put(node.coordinate, ROOT);
            queue.add(node.coordinate);
        }
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (current.equals(coordinate)) break;
            for (Node node : children(current)) {
                if (!previous.containsKey(node.coordinate)) {
                    previous.put(node.coordinate, current);
                    queue.add(node.coordinate);
                }
            }
        }
        if (!previous.containsKey(coordinate)) return List.of();
        List<String> path = new ArrayList<>();
        String current = coordinate;
        while (!current.isEmpty()) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
