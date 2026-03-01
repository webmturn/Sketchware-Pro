/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package org.cosmic.ide.dependency.resolver.api

import okhttp3.Request
import org.cosmic.ide.dependency.resolver.eventReciever
import org.cosmic.ide.dependency.resolver.okHttpClient
import org.cosmic.ide.dependency.resolver.resolveDependencies
import org.cosmic.ide.dependency.resolver.xmlDeserializer
import org.cosmic.ide.dependency.resolver.parallelForEach
import org.cosmic.ide.dependency.resolver.getNewerVersion
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.collections.ArrayDeque

data class Artifact(
    val groupId: String,
    val artifactId: String,
    var version: String = "",
    var repository: Repository? = null,
    var extension: String = "jar"
) {
    var mavenMetadata: MavenMetadata? = null
    var dependencies: List<Artifact>? = null
    var pom: ProjectObjectModel? = null
    // Exclusions accumulated from ancestor declarations — applied when resolving this artifact's transitive deps
    var activeExclusions: List<Exclusion> = emptyList()

    suspend fun downloadArtifact(output: File) {
        output.mkdirs()
        val allArtifactsToDownload = (getAllDependencies() + this).toSet()

        allArtifactsToDownload.forEach { artifact ->
            val artifactFile = File(output, "${artifact.artifactId}-${artifact.version}.${artifact.extension}")
            if (!artifactFile.exists()) {
                artifact.downloadTo(artifactFile)
            }
        }
    }

    suspend fun getAllDependencies(): Set<Artifact> {
        if (this.dependencies == null) {
            resolveDependencyTree()
        }

        val allResolvedArtifactsInGraph = mutableSetOf<Artifact>()
        val queue = ArrayDeque<Artifact>()

        this.dependencies?.forEach { queue.add(it) }

        val visitedForTraversal = mutableSetOf<Artifact>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (visitedForTraversal.add(current)) {
                allResolvedArtifactsInGraph.add(current)
                current.dependencies?.forEach { dep ->
                    if (!visitedForTraversal.contains(dep)) {
                        queue.add(dep)
                    }
                }
            }
        }

        val newestArtifactsMap = mutableMapOf<Pair<String, String>, Artifact>()
        for (artifact in allResolvedArtifactsInGraph) {
            val key = Pair(artifact.groupId, artifact.artifactId)
            val existingNewest = newestArtifactsMap[key]
            if (existingNewest == null) {
                newestArtifactsMap[key] = artifact
            } else {
                val newerVersionString = getNewerVersion(existingNewest.version, artifact.version)
                if (newerVersionString == artifact.version && existingNewest.version != artifact.version) {
                    newestArtifactsMap[key] = artifact
                }
            }
        }
        return newestArtifactsMap.values.toSet()
    }

    fun showDependencyTree(depth: Int = 0) {
        println("    ".repeat(depth) + this)
        dependencies?.forEach { dep ->
            dep.showDependencyTree(depth + 1)
        }
    }

    suspend fun resolve(
        resolved: ConcurrentHashMap<Pair<String, String>, Pair<Artifact, ConcurrentLinkedDeque<Artifact>>>,
        managedDependencies: ConcurrentLinkedDeque<Artifact>
    ) {
        if (this.dependencies != null) {
            eventReciever.onSkippingResolution(this)
            return
        }

        val key = Pair(groupId, artifactId)
        val cachedEntry = resolved[key]

        if (cachedEntry != null) {
            val cachedArtifactInstance = cachedEntry.first
            val cachedDependencies = cachedEntry.second

            val comparisonResult: Int = when {
                getNewerVersion(this.version, cachedArtifactInstance.version) == this.version && this.version != cachedArtifactInstance.version -> 1
                getNewerVersion(this.version, cachedArtifactInstance.version) == cachedArtifactInstance.version && this.version != cachedArtifactInstance.version -> -1
                else -> 0
            }

            if (comparisonResult < 0) {
                this.dependencies = emptyList()
                eventReciever.onSkippingResolution(this)
                eventReciever.logger.info("Skipping $this - older than cached version ${cachedArtifactInstance.version}")
                return
            } else if (comparisonResult == 0) {
                this.dependencies = cachedDependencies.toList()
                eventReciever.onSkippingResolution(this)
                eventReciever.logger.info("Skipping $this - same as cached version, reusing dependencies")
                return
            }
            eventReciever.logger.info("Proceeding with $this - newer than cached version ${cachedArtifactInstance.version}")
        }

        if (repository == null) {
            org.cosmic.ide.dependency.resolver.initHost(this)
            if (repository == null) {
                this.dependencies = emptyList()
                resolved[key] = Pair(this, ConcurrentLinkedDeque())
                throw IllegalStateException("Repository is not declared for $groupId:$artifactId:$version and could not be initialized.")
            }
        }

        val pomFile = getPOM()
        if (pomFile == null) {
            this.dependencies = emptyList()
            resolved[key] = Pair(this, ConcurrentLinkedDeque())
            eventReciever.onInvalidPOM(this)
            return
        }

        val directDependencies = pomFile.resolveDependencies(resolved, managedDependencies, this.activeExclusions)
        this.dependencies = directDependencies.toList()
        if (this.dependencies?.isEmpty() == true) {
            eventReciever.onDependenciesNotFound(this)
        }
        resolved[key] = Pair(this, directDependencies)
        eventReciever.onResolutionComplete(this)
    }

    suspend fun resolveDependencyTree(
        resolved: ConcurrentHashMap<Pair<String, String>, Pair<Artifact, ConcurrentLinkedDeque<Artifact>>> = ConcurrentHashMap(),
        managedDependencies: ConcurrentLinkedDeque<Artifact> = ConcurrentLinkedDeque()
    ) {
        val queue = ArrayDeque<Artifact>()
        queue.add(this)

        val visitedInThisCall = mutableSetOf<Artifact>()
        visitedInThisCall.add(this)

        while (queue.isNotEmpty()) {
            val currentLevelArtifacts = mutableListOf<Artifact>()
            while (queue.isNotEmpty()) {
                currentLevelArtifacts.add(queue.removeFirst())
            }

            currentLevelArtifacts.filter { it.dependencies == null }.parallelForEach { artifact ->
                artifact.resolve(resolved, managedDependencies)
            }

            for (artifact in currentLevelArtifacts) {
                artifact.dependencies?.forEach { dependency ->
                    if (visitedInThisCall.add(dependency)) {
                        queue.add(dependency)
                    }
                }
            }
        }
    }

    fun downloadTo(output: File) {
        if (repository == null) {
            throw IllegalStateException("Repository is not declared for $groupId:$artifactId:$version during downloadTo.")
        }
        output.parentFile?.mkdirs()
        output.createNewFile()
        val dependencyUrl = "${repository!!.getURL()}/${
            groupId.replace(
                ".", "/"
            )
        }/$artifactId/$version/$artifactId-$version.$extension"
        eventReciever.onDownloadStart(this)
        val request = Request.Builder().url(dependencyUrl).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code ${response.code} for $dependencyUrl")
                response.body!!.byteStream().use { input ->
                    output.outputStream().use { input.copyTo(it) }
                }
            }
            eventReciever.onDownloadEnd(this)
        } catch (e: Exception) {
            eventReciever.onDownloadError(this, e)
        }
    }

    fun getPOM(): ProjectObjectModel? {
        if (pom != null) {
            return pom
        }
        if (repository == null) {
            org.cosmic.ide.dependency.resolver.initHost(this)
            if (repository == null) {
                eventReciever.onInvalidPOM(this)
                return null
            }
        }
        if (version.isEmpty()) {
            eventReciever.onInvalidPOM(this)
            return null
        }
        val pomUrl = "${repository?.getURL()}/${
            groupId.replace(
                ".", "/"
            )
        }/$artifactId/$version/$artifactId-$version.pom"

        val request = Request.Builder().url(pomUrl).build()
        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                eventReciever.onVersionNotFound(this)
                return null
            }
            this.pom = xmlDeserializer.readValue(
                response.body!!.byteStream(),
                ProjectObjectModel::class.java
            )
            return this.pom
        } catch (_: SocketException) {
            eventReciever.onVersionNotFound(this)
            return null
        } catch (_: IOException) {
            eventReciever.onInvalidPOM(this)
            return null
        } catch (_: Exception) {
            eventReciever.onInvalidPOM(this)
            return null
        }
    }

    override fun toString(): String {
        return "$groupId:$artifactId:$version"
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Artifact
        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (version != other.version) return false
        return true
    }
}
