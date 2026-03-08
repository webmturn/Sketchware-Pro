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

package org.cosmic.ide.dependency.resolver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.cosmic.ide.dependency.resolver.api.Artifact
import org.cosmic.ide.dependency.resolver.api.EventReciever
import org.cosmic.ide.dependency.resolver.api.Exclusion
import org.cosmic.ide.dependency.resolver.api.ProjectObjectModel
import org.cosmic.ide.dependency.resolver.api.Repository
import org.cosmic.ide.dependency.resolver.repository.GoogleMaven
import org.cosmic.ide.dependency.resolver.repository.Jitpack
import org.cosmic.ide.dependency.resolver.repository.MavenCentral
import org.cosmic.ide.dependency.resolver.repository.SonatypeSnapshots
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue


val repositories = ConcurrentLinkedQueue<Repository>().apply {
    addAll(listOf(MavenCentral(), GoogleMaven(), Jitpack(), SonatypeSnapshots()))
}
var eventReciever = EventReciever()
val okHttpClient = okhttp3.OkHttpClient.Builder()
    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .build()

val xmlDeserializer: ObjectMapper = XmlMapper(JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}).registerKotlinModule().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)

fun getArtifact(groupId: String, artifactId: String, version: String): Artifact? {
    val artifact = initHost(Artifact(groupId, artifactId, version)) ?: return null

    val pom = artifact.getPOM() ?: return null
    artifact.extension = if (pom.packaging != null && pom.packaging != "bundle") pom.packaging else "jar"

    return artifact
}

/*
 * Finds the host repository of the artifact and initialises it.
 * Returns null if no repository hosts this artifact
 */
// In-memory cache: groupId → repo URL (same groupId is typically hosted on the same repo)
private val repoCache = ConcurrentHashMap<String, String>()
val pomCache = ConcurrentHashMap<String, ProjectObjectModel>()
private val expandedBoms = ConcurrentHashMap<String, Boolean>()

/**
 * Clears per-session caches that must not persist across separate dependency resolution calls.
 * [expandedBoms] tracks which BOMs have been expanded into [managedDependencies], but since
 * [managedDependencies] is created fresh for each [resolveDependencyTree] call, stale entries
 * in [expandedBoms] would cause BOMs to be skipped for new resolution sessions.
 *
 * [pomCache] and [repoCache] are safe to keep — they cache immutable data (POM content, repo URLs).
 */
fun clearSessionCaches() {
    expandedBoms.clear()
}

fun initHost(artifact: Artifact): Artifact? {
    if (artifact.repository != null) {
        return artifact // Already initialized or repository was set externally
    }
    // Try cached repo first — trust the cache, skip HTTP verification.
    // Same groupId is always hosted on the same repo in Maven conventions.
    val cachedRepoUrl = repoCache[artifact.groupId]
    if (cachedRepoUrl != null) {
        val cachedRepo = repositories.find { it.getURL() == cachedRepoUrl }
        if (cachedRepo != null) {
            artifact.repository = cachedRepo
            return artifact
        }
    }
    // Attempt to find a repository only if not cached
    for (repository in repositories) {
        if (repository.checkExists(artifact)) {
            artifact.repository = repository
            repoCache[artifact.groupId] = repository.getURL()
            return artifact
        }
    }
    eventReciever.onArtifactNotFound(artifact)
    return null
}

/**
 * Expands all managed dependency entries from the given POM into [managedDependencies],
 * including BOM imports (scope=import) and parent POM <dependencyManagement> inheritance.
 * Entries already present in [managedDependencies] are not overwritten (current POM takes precedence).
 */
private fun expandManagedDependencies(
    pom: ProjectObjectModel,
    managedDependencies: ConcurrentLinkedDeque<Artifact>,
    skipFilter: ((Artifact) -> Boolean)? = null
) {
    val pomKey = "${pom.groupId ?: pom.parent?.groupId}:${pom.artifactId}:${pom.version ?: pom.parent?.version}"
    if (expandedBoms.putIfAbsent(pomKey, true) != null) return // already expanded or cycle guard

    // Step 1: Add current POM's non-BOM <dependencyManagement> entries (highest priority — skip if already present)
    pom.dependencyManagement?.dependencies.orEmpty()
        .filterNot { it.scope == "import" }
        .forEach { dep ->
            val gId = dep.groupId ?: pom.groupId ?: pom.parent?.groupId ?: return@forEach
            if (managedDependencies.none { it.groupId == gId && it.artifactId == dep.artifactId }) {
                managedDependencies.add(Artifact(gId, dep.artifactId, dep.version ?: ""))
            }
        }

    // Step 2: Expand BOM imports (scope=import) — lower priority than explicit entries above
    pom.dependencyManagement?.dependencies.orEmpty()
        .filter { it.scope == "import" }
        .forEach { bomDep ->
            val bomGroupId = bomDep.groupId ?: pom.groupId ?: pom.parent?.groupId ?: return@forEach
            // Skip downloading BOM POMs for built-in groups
            if (skipFilter?.invoke(Artifact(bomGroupId, bomDep.artifactId, bomDep.version ?: "")) == true) return@forEach
            val bomVersion = bomDep.version ?: ""
            if (bomVersion.isEmpty()) return@forEach
            val bomArtifact = Artifact(bomGroupId, bomDep.artifactId, bomVersion)
            initHost(bomArtifact)
            val bomPom = bomArtifact.getPOM() ?: return@forEach
            // Recursively expand the BOM's managed deps (skips entries already added above)
            expandManagedDependencies(bomPom, managedDependencies, skipFilter)
        }

    // Step 3: Inherit parent POM's <dependencyManagement> (lowest priority — recursively)
    val parentGav = pom.parent
    if (parentGav != null) {
        // Skip downloading parent POMs for built-in groups
        if (skipFilter?.invoke(Artifact(parentGav.groupId, parentGav.artifactId, parentGav.version)) != true) {
            val parentArtifact = Artifact(parentGav.groupId, parentGav.artifactId, parentGav.version)
            initHost(parentArtifact)
            val parentPom = parentArtifact.getPOM()
            if (parentPom != null) {
                expandManagedDependencies(parentPom, managedDependencies, skipFilter)
            }
        }
    }
}

suspend fun ProjectObjectModel.resolveDependencies(
    resolved: ConcurrentHashMap<Pair<String, String>, Pair<Artifact, ConcurrentLinkedDeque<Artifact>>>,
    managedDependencies: ConcurrentLinkedDeque<Artifact>,
    parentExclusions: List<Exclusion> = emptyList(),
    skipFilter: ((Artifact) -> Boolean)? = null
): ConcurrentLinkedDeque<Artifact> {
    // Expand BOM imports + current POM's dependencyManagement + parent POM's dependencyManagement
    expandManagedDependencies(this, managedDependencies, skipFilter = skipFilter)

    val deps = ConcurrentLinkedDeque<Artifact>()
    dependencies.orEmpty().filterNot { dep ->
        // Filter deps excluded by ancestor declarations
        val excludedByParent = parentExclusions.any { ex ->
            val groupMatch = ex.groupId == null || ex.groupId == dep.groupId
            val artifactMatch = ex.artifactId == "*" || ex.artifactId == dep.artifactId
            groupMatch && artifactMatch
        }
        if (excludedByParent) {
            val depGroupId = dep.groupId ?: groupId ?: parent?.groupId ?: "unknown.groupId"
            eventReciever.logger.info("Excluding ${depGroupId}:${dep.artifactId} due to ancestor exclusion")
        }
        if (!excludedByParent) {
            val invalidScope = dep.scope == "test" || dep.scope == "provided" || dep.optional
            if (invalidScope) {
                val depGroupId = dep.groupId ?: groupId ?: parent?.groupId ?: "unknown.groupId"
                eventReciever.onInvalidScope(Artifact(depGroupId, dep.artifactId, dep.version ?: ""), dep.scope ?: "Optional")
            }
            invalidScope
        } else true
    }.parallelForEach { dependency ->
        val depGroupId = dependency.groupId ?: groupId ?: parent?.groupId
            ?: throw IllegalStateException("GroupId missing for dependency ${dependency.artifactId} in POM")
        val depArtifactId = dependency.artifactId

        // Create the artifact instance for this dependency
        val artifact = Artifact(depGroupId, depArtifactId, dependency.version ?: "")
        // Propagate exclusions transitively: ancestor exclusions + this dep's own exclusions
        artifact.activeExclusions = parentExclusions + dependency.exclusions.orEmpty()

        // Skip network requests entirely for built-in dependencies
        if (skipFilter?.invoke(artifact) == true) {
            artifact.dependencies = emptyList()
            deps.add(artifact)
            eventReciever.onSkippingResolution(artifact)
            return@parallelForEach
        }

        val originalGroupIdForEvent = this.groupId ?: parent?.groupId ?: "unknown.parent.groupId"
        val originalArtifactIdForEvent = this.artifactId
        val originalVersionForEvent = this.version ?: parent?.version ?: "unknown.parent.version"

        eventReciever.onResolving(Artifact(originalGroupIdForEvent, originalArtifactIdForEvent, originalVersionForEvent), artifact)

        // Initialize host repository for the artifact
        initHost(artifact)

        if (artifact.repository == null && !needsVersionFix(artifact.version)) {
            eventReciever.onArtifactNotFound(artifact)
            return@parallelForEach
        }

        // Apply version fixing (e.g., for '+', ranges, properties)
        if (needsVersionFix(artifact.version)) {
            fixVersion(artifact, this, resolved)
            initHost(artifact) // Re-initialize host if version changed
        }

        // If, after version fixing, the repository is still null, then artifact is not found
        if (artifact.repository == null) {
            eventReciever.onArtifactNotFound(artifact)
            return@parallelForEach
        }

        eventReciever.artifactFound(artifact)

        // Apply managed dependencies version override
        managedDependencies.find { it.groupId == artifact.groupId && it.artifactId == artifact.artifactId }?.let { managedDep ->
            if (artifact.version != managedDep.version) {
                eventReciever.logger.warning("Using managed dependency ${managedDep.groupId}:${managedDep.artifactId}:${managedDep.version} for ${artifact.groupId}:${artifact.artifactId} (was ${artifact.version})")
                artifact.version = managedDep.version
                initHost(artifact)
                if (needsVersionFix(artifact.version)) {
                    fixVersion(artifact, this, resolved)
                    initHost(artifact)
                }
            }
        }

        // Get POM and set extension (packaging)
        val pom = artifact.getPOM()
        artifact.extension = if (pom?.packaging != null && pom.packaging != "bundle") pom.packaging else "jar"

        deps.add(artifact)
        eventReciever.onResolutionComplete(artifact)
    }
    return deps
}

private fun needsVersionFix(version: String): Boolean {
    return version.isEmpty() || version == "+" || version.startsWith("[") || version.startsWith("\${")
}

private fun fixVersion(
    artifact: Artifact,
    pom: ProjectObjectModel,
    resolved: ConcurrentHashMap<Pair<String, String>, Pair<Artifact, ConcurrentLinkedDeque<Artifact>>> = ConcurrentHashMap()
) {
    // Ensure metadata is loaded if needed for version fixing (e.g., for '+', ranges)
    if (artifact.repository == null) {
        initHost(artifact)
    }

    // If still no repo or metadata (for '+' or range), and version needs it, cannot fix version.
    if ((artifact.version.isEmpty() || artifact.version == "+" || artifact.version.startsWith("[")) && artifact.mavenMetadata == null && artifact.repository != null) {
        if (artifact.repository?.checkExists(artifact) == false) {
            eventReciever.logger.warning("Could not load metadata for ${artifact.groupId}:${artifact.artifactId} to fix version '${artifact.version}'.")
            if (artifact.version.isEmpty() || artifact.version == "+") return
        }
    }

    if (artifact.version.isEmpty() || artifact.version == "+") {
        eventReciever.onFetchingLatestVersion(artifact)
        val latestFromMeta = artifact.mavenMetadata?.versioning?.let { it.release ?: it.latest ?: it.versions.lastOrNull() }
        artifact.version = latestFromMeta ?: artifact.version.substringBefore("+")
        if (latestFromMeta == null) {
            eventReciever.logger.warning("Could not determine latest version for ${artifact.groupId}:${artifact.artifactId} from metadata. Using derived or existing: '${artifact.version}'.")
        }
        eventReciever.onFetchedLatestVersion(artifact, artifact.version)
    } else if (artifact.version.startsWith("[")) {
        if (artifact.mavenMetadata == null && artifact.repository != null) {
            artifact.repository?.checkExists(artifact)
        }
        artifact.version = getLatestRangeVersion(artifact, artifact.version, resolved)
    } else if (artifact.version.startsWith("\${")) {
        val propertyName = artifact.version.substring(2, artifact.version.length - 1)
        var resolvedVersion: String? = null

        if (propertyName == "project.version") {
            resolvedVersion = pom.version ?: pom.parent?.version
        } else {
            // Search in current POM properties, then parent POM properties recursively.
            var currentPomForProps: ProjectObjectModel? = pom
            while (currentPomForProps != null) {
                resolvedVersion = currentPomForProps.properties?.get(propertyName)
                if (resolvedVersion != null) break

                val parentGAV = currentPomForProps.parent
                currentPomForProps = if (parentGAV != null) {
                    val parentArtifact = Artifact(parentGAV.groupId, parentGAV.artifactId, parentGAV.version)
                    parentArtifact.getPOM()
                } else {
                    null
                }
            }
        }
        artifact.version = resolvedVersion ?: ""

        // If version resolved to another property or needs further fixing
        if (needsVersionFix(artifact.version) || artifact.version.startsWith("\${")) {
            eventReciever.logger.warning("Version for ${artifact.groupId}:${artifact.artifactId} resolved from property '\${${propertyName}}' to '${artifact.version}'. Re-evaluating.")
            if (artifact.version.startsWith("\${") && artifact.version.substring(2, artifact.version.length - 1) == propertyName) {
                eventReciever.logger.severe("Circular or unresolvable property ${artifact.version} for ${artifact.groupId}:${artifact.artifactId}")
                artifact.version = ""
            } else {
                fixVersion(artifact, pom, resolved)
            }
        }
    }
}

/*
 * Gets the latest version of the artifact from the given version range.
 *
 * @param artifact The artifact to get the latest version of.
 * @param version The version range to get the latest version from.
 * @return The latest version of the artifact.
 */
fun getLatestRangeVersion(
    artifact: Artifact,
    versionRange: String,
    resolved: ConcurrentHashMap<Pair<String, String>, Pair<Artifact, ConcurrentLinkedDeque<Artifact>>> = ConcurrentHashMap()
): String {
    // Ensure metadata is available
    if (artifact.mavenMetadata == null) {
        initHost(artifact)
        if (artifact.mavenMetadata == null) {
            eventReciever.logger.warning("Cannot determine latest range version for ${artifact.groupId}:${artifact.artifactId} ('${versionRange}') as maven metadata is missing.")
            return versionRange.substringBefore(",").trimStart('[').trimEnd(']').ifEmpty { versionRange }
        }
    }

    val actualRange = versionRange.trim()
    if (!actualRange.startsWith("[") && !actualRange.startsWith("(")) {
        return actualRange
    }
    if (!actualRange.contains(",")) {
        return actualRange.substring(1, actualRange.length - 1)
    }

    val parts = actualRange.substring(1, actualRange.length - 1).split(",")
    val startVersionString = parts.getOrNull(0)?.trim() ?: ""
    val endVersionString = parts.getOrNull(1)?.trim() ?: ""

    val startInclusive = actualRange.startsWith("[")
    val endInclusive = actualRange.endsWith("]")

    eventReciever.onFetchingLatestVersion(artifact)

    var bestVersion: String? = null
    artifact.mavenMetadata!!.versioning.versions.forEach { v ->
        val vComparable = v

        val afterStart = when {
            startVersionString.isEmpty() -> true
            startInclusive -> getNewerVersion(vComparable, startVersionString) == vComparable || vComparable == startVersionString
            else -> getNewerVersion(vComparable, startVersionString) == vComparable && vComparable != startVersionString
        }

        val beforeEnd = when {
            endVersionString.isEmpty() -> true
            endInclusive -> getNewerVersion(vComparable, endVersionString) == endVersionString || vComparable == endVersionString
            else -> getNewerVersion(vComparable, endVersionString) == endVersionString && vComparable != endVersionString
        }

        if (afterStart && beforeEnd) {
            if (bestVersion == null || getNewerVersion(bestVersion!!, vComparable) == vComparable) {
                bestVersion = vComparable
            }
        }
    }

    if (bestVersion != null) {
        eventReciever.onFetchedLatestVersion(artifact, bestVersion!!)
        return bestVersion!!
    }

    eventReciever.logger.warning("No version found in metadata for range '${actualRange}' for ${artifact.groupId}:${artifact.artifactId}. Fallback might be used.")
    val resolvedVersionFromCache = resolved[Pair(artifact.groupId, artifact.artifactId)]?.first?.version
    if (resolvedVersionFromCache != null) {
        val afterStart = when {
            startVersionString.isEmpty() -> true
            startInclusive -> getNewerVersion(resolvedVersionFromCache, startVersionString) == resolvedVersionFromCache || resolvedVersionFromCache == startVersionString
            else -> getNewerVersion(resolvedVersionFromCache, startVersionString) == resolvedVersionFromCache && resolvedVersionFromCache != startVersionString
        }
        val beforeEnd = when {
            endVersionString.isEmpty() -> true
            endInclusive -> getNewerVersion(resolvedVersionFromCache, endVersionString) == endVersionString || resolvedVersionFromCache == endVersionString
            else -> getNewerVersion(resolvedVersionFromCache, endVersionString) == endVersionString && resolvedVersionFromCache != endVersionString
        }
        if (afterStart && beforeEnd) return resolvedVersionFromCache
    }

    return artifact.mavenMetadata!!.versioning.release
        ?: artifact.mavenMetadata!!.versioning.latest
        ?: startVersionString.takeIf { it.isNotEmpty() }
        ?: versionRange
}

fun getNewerVersion(existing: String, new: String): String {
    if (new.startsWith(existing) && new.length > existing.length) return new
    if (existing.startsWith(new) && existing.length > new.length) return existing

    val newSegments = new.split('.', '-').mapNotNull { it.toIntOrNull() }
    val existingSegments = existing.split('.', '-').mapNotNull { it.toIntOrNull() }

    for (i in 0 until minOf(newSegments.size, existingSegments.size)) {
        if (newSegments[i] > existingSegments[i]) return new
        if (newSegments[i] < existingSegments[i]) return existing
    }
    if (newSegments.size > existingSegments.size) return new
    if (existingSegments.size > newSegments.size) return existing

    return if (new >= existing) new else existing
}

/*
 * Runs the given action on each element of the iterable in parallel.
 * Returns a list of the results of the actions.
 *
 * @param action The action to run on each element.
 */
private val resolutionSemaphore = Semaphore(8)

suspend fun <T> Iterable<T>.parallelForEach(action: suspend (T) -> Unit) = coroutineScope {
    map { element ->
        async(kotlinx.coroutines.Dispatchers.IO) { resolutionSemaphore.withPermit { action(element) } }
    }.awaitAll()
}
