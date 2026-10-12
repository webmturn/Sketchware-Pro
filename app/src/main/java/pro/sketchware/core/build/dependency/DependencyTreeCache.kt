package pro.sketchware.core.build.dependency

import org.cosmic.ide.dependency.resolver.api.Artifact

/** Describes resolved edges without downloading or touching the filesystem. */
object DependencyTreeCache {
    @JvmStatic
    fun describe(mainDep: Artifact, deps: Collection<Artifact>, builtInKeys: Set<String>): List<Map<String, Any?>> {
        // Walk the actual dependency tree (BFS) to record depth + parent for tree visualization.
        // Falls back to flat list if tree structure is unavailable (e.g. dependencies not resolved).
        val entries = mutableListOf<HashMap<String, Any?>>()
        val entriesByCoordinate = mutableMapOf<String, HashMap<String, Any?>>()
        val visited = mutableSetOf<String>()
        val mainCoord = "${mainDep.groupId}:${mainDep.artifactId}:${mainDep.version}"
        visited.add(mainCoord)

        data class QueueEntry(val artifact: Artifact, val parentCoord: String, val depth: Int)
        val queue = ArrayDeque<QueueEntry>()

        // Seed with direct deps of the root artifact
        val directDeps = mainDep.dependencies
        if (directDeps != null && directDeps.isNotEmpty()) {
            directDeps.forEach { dep -> queue.add(QueueEntry(dep, mainCoord, 1)) }

            while (queue.isNotEmpty()) {
                val (dep, parentCoord, depth) = queue.removeFirst()
                val key = "${dep.groupId}:${dep.artifactId}:${dep.version}"
                // A shared dependency still needs every incoming edge after de-duplication.
                entriesByCoordinate[key]?.let { existing ->
                    @Suppress("UNCHECKED_CAST")
                    val parents = existing["parents"] as MutableList<String>
                    if (parentCoord !in parents) parents.add(parentCoord)
                }
                if (!visited.add(key)) continue

                val entry = hashMapOf<String, Any?>(
                    "groupId" to dep.groupId,
                    "artifactId" to dep.artifactId,
                    "version" to dep.version,
                    "extension" to dep.extension,
                    "repoUrl" to dep.repository?.getURL(),
                    "repoName" to dep.repository?.getName(),
                    "builtIn" to builtInKeys.contains(key),
                    "depth" to depth,
                    "parent" to parentCoord,
                    "parents" to mutableListOf(parentCoord),
                    "hierarchyKnown" to true
                )
                entries.add(entry)
                entriesByCoordinate[key] = entry

                dep.dependencies?.forEach { childDep ->
                    // Queue even an already visited node once per edge; traversal stays bounded
                    // because children are expanded only on the first visit to a coordinate.
                    queue.add(QueueEntry(childDep, key, depth + 1))
                }
            }
        } else {
            // Fallback: tree structure unavailable, save as flat list (depth 1, no parent)
            deps.forEach { dep ->
                val key = "${dep.groupId}:${dep.artifactId}:${dep.version}"
                entries.add(hashMapOf(
                    "groupId" to dep.groupId,
                    "artifactId" to dep.artifactId,
                    "version" to dep.version,
                    "extension" to dep.extension,
                    "repoUrl" to dep.repository?.getURL(),
                    "repoName" to dep.repository?.getName(),
                    "builtIn" to builtInKeys.contains(key),
                    "depth" to 1,
                    "parent" to mainCoord,
                    "hierarchyKnown" to false
                ))
            }
        }
        return entries
    }
}
