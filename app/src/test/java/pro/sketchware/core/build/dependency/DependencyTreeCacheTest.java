package pro.sketchware.core.build.dependency;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.cosmic.ide.dependency.resolver.api.Artifact;

public class DependencyTreeCacheTest {
    private Artifact artifact(String name) { return new Artifact("example", name, "1", null, "jar"); }

    @Test public void sharedNodeIsSavedOnceWithBothParentsAndBuiltInStatus() {
        Artifact root = artifact("root"), a = artifact("a"), b = artifact("b"), shared = artifact("shared");
        root.setDependencies(List.of(a,b));
        a.setDependencies(List.of(shared));
        b.setDependencies(List.of(shared));
        List<Map<String,Object>> entries = DependencyTreeCache.describe(root, List.of(a,b,shared), Set.of("example:shared:1"));
        assertEquals(3, entries.size());
        Map<String,Object> cached = entries.get(2);
        assertEquals(List.of("example:a:1", "example:b:1"), cached.get("parents"));
        assertEquals("example:a:1", cached.get("parent"));
        assertEquals(Boolean.TRUE, cached.get("builtIn"));
        assertEquals(Boolean.TRUE, cached.get("hierarchyKnown"));
        assertEquals(2, cached.get("depth"));
    }

    @Test public void circularRootReferenceDoesNotSaveOrExpandTheRootAgain() {
        Artifact root = artifact("root"), child = artifact("child");
        root.setDependencies(List.of(child));
        child.setDependencies(List.of(root));
        List<Map<String,Object>> entries = DependencyTreeCache.describe(root,List.of(child),Set.of());
        assertEquals(1, entries.size());
        assertEquals("child", entries.get(0).get("artifactId"));
    }

    @Test public void unresolvedFlatCacheDoesNotClaimKnownRelationships() {
        Artifact root = artifact("root"), child = artifact("child");
        List<Map<String,Object>> entries = DependencyTreeCache.describe(root,List.of(child),Set.of());
        assertEquals(Boolean.FALSE, entries.get(0).get("hierarchyKnown"));
        assertEquals("example:root:1", entries.get(0).get("parent"));
    }
}
