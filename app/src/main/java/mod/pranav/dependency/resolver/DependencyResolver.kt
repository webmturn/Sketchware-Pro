package mod.pranav.dependency.resolver

import android.os.Environment
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mod.hey.studios.build.BuildSettings
import mod.hey.studios.util.Helper
import mod.jbk.build.BuiltInLibraries
import org.cosmic.ide.dependency.resolver.api.Artifact
import org.cosmic.ide.dependency.resolver.api.EventReciever
import org.cosmic.ide.dependency.resolver.api.Repository
import org.cosmic.ide.dependency.resolver.eventReciever
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmic.ide.dependency.resolver.repositories
import pro.sketchware.utility.FilePathUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.TimeoutCancellationException
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DependencyResolver(
    private val groupId: String,
    private val artifactId: String,
    private val version: String,
    private val skipDependencies: Boolean,
    private val buildSettings: BuildSettings?
) {
    companion object {
        private val DEFAULT_REPOS = """
          |[
          |    {"url": "https://repo.hortonworks.com/content/repositories/releases", "name": "HortanWorks"},
          |    {"url": "https://maven.atlassian.com/content/repositories/atlassian-public", "name": "Atlassian"},
          |    {"url": "https://jcenter.bintray.com", "name": "JCenter"},
          |    {"url": "https://oss.sonatype.org/content/repositories/releases", "name": "Sonatype"},
          |    {"url": "https://repo.spring.io/plugins-release", "name": "Spring Plugins"},
          |    {"url": "https://repo.spring.io/libs-milestone", "name": "Spring Milestone"},
          |    {"url": "https://repo.maven.apache.org/maven2", "name": "Apache Maven"}
          |]
        """.trimMargin()

        /** Specific androidx groupIds that are bundled in BuiltInLibraries.
         *  Non-built-in groups (media3, camera, compose, navigation, paging, etc.) are NOT listed. */
        private val BUILT_IN_ANDROIDX_GROUPS = setOf(
            "androidx.activity",
            "androidx.annotation",
            "androidx.appcompat",
            "androidx.arch.core",
            "androidx.asynclayoutinflater",
            "androidx.browser",
            "androidx.cardview",
            "androidx.collection",
            "androidx.concurrent",
            "androidx.constraintlayout",
            "androidx.coordinatorlayout",
            "androidx.core",
            "androidx.cursoradapter",
            "androidx.customview",
            "androidx.documentfile",
            "androidx.drawerlayout",
            "androidx.dynamicanimation",
            "androidx.emoji2",
            "androidx.exifinterface",
            "androidx.fragment",
            "androidx.graphics.shapes",
            "androidx.interpolator",
            "androidx.legacy",
            "androidx.lifecycle",
            "androidx.loader",
            "androidx.localbroadcastmanager",
            "androidx.media",
            "androidx.multidex",
            "androidx.recyclerview",
            "androidx.room",
            "androidx.savedstate",
            "androidx.slidingpanelayout",
            "androidx.sqlite",
            "androidx.startup",
            "androidx.swiperefreshlayout",
            "androidx.tracing",
            "androidx.transition",
            "androidx.vectordrawable",
            "androidx.versionedparcelable",
            "androidx.viewpager",
            "androidx.viewpager2",
            "androidx.work",
        )
    }

    private var downloadPath: String = FilePathUtil.getLocalLibsDir().absolutePath

    private fun isStoragePermissionError(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            val msg = current.message
            if (msg != null && (msg.contains("Operation not permitted") || msg.contains("EPERM"))) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun switchToFallbackPath() {
        downloadPath = FilePathUtil.getLocalLibsFallbackDir().absolutePath
    }

    /**
     * Checks if a library file exists in either primary or fallback path.
     * Returns the existing file, or null if not found in either location.
     */
    private fun resolveExistingLibFile(artifactId: String, version: String, filename: String): File? {
        val primary = File(FilePathUtil.getLocalLibsDir(), "$artifactId-v$version/$filename")
        if (primary.exists() && primary.length() > 0) return primary
        val fallback = File(FilePathUtil.getLocalLibsFallbackDir(), "$artifactId-v$version/$filename")
        if (fallback.exists() && fallback.length() > 0) return fallback
        return null
    }

    private val repositoriesJson = Paths.get(
        Environment.getExternalStorageDirectory().absolutePath,
        ".sketchware",
        "libs",
        "repositories.json"
    )

    init {
        if (Files.notExists(repositoriesJson)) {
            Files.createDirectories(repositoriesJson.parent)
            repositoriesJson.writeText(DEFAULT_REPOS)
        }
        // Remove previously added custom repos to prevent infinite growth on repeated instantiation
        repositories.removeAll { repo ->
            repo !is org.cosmic.ide.dependency.resolver.repository.MavenCentral &&
            repo !is org.cosmic.ide.dependency.resolver.repository.GoogleMaven &&
            repo !is org.cosmic.ide.dependency.resolver.repository.Jitpack &&
            repo !is org.cosmic.ide.dependency.resolver.repository.SonatypeSnapshots
        }
        Gson().fromJson(repositoriesJson.readText(), Helper.TYPE_MAP_LIST).forEach {
            val url: String? = it["url"] as String?
            if (url != null) {
                repositories.add(object : Repository {
                    override fun getName(): String {
                        return it["name"] as String
                    }

                    override fun getURL(): String {
                        return if (url.endsWith("/")) {
                            url.substringBeforeLast("/")
                        } else {
                            url
                        }
                    }
                })
            }
        }
    }

    open class DependencyResolverCallback : EventReciever() {
        override fun artifactFound(artifact: Artifact) {}
        override fun onArtifactNotFound(artifact: Artifact) {}
        override fun onFetchingLatestVersion(artifact: Artifact) {}
        override fun onFetchedLatestVersion(artifact: Artifact, version: String) {}
        override fun onResolving(artifact: Artifact, dependency: Artifact) {}
        override fun onResolutionComplete(artifact: Artifact) {}
        override fun onSkippingResolution(artifact: Artifact) {}
        override fun onVersionNotFound(artifact: Artifact) {}
        override fun onDependenciesNotFound(artifact: Artifact) {}
        override fun onInvalidScope(artifact: Artifact, scope: String) {}
        override fun onInvalidPOM(artifact: Artifact) {}
        override fun onDownloadStart(artifact: Artifact) {}
        override fun onDownloadEnd(artifact: Artifact) {}
        override fun onDownloadError(artifact: Artifact, error: Throwable) {}
        open fun unzipping(artifact: Artifact) {}
        open fun dexing(artifact: Artifact) {}
        open fun onTaskCompleted(artifacts: List<String>) {}
        open fun dexingFailed(artifact: Artifact, e: Exception) {}
        open fun invalidPackaging(artifact: Artifact) {}
    }

    fun resolveDependency(callback: DependencyResolverCallback) = runBlocking(kotlinx.coroutines.Dispatchers.IO) {
        eventReciever = callback
        val dependency = getArtifact(groupId, artifactId, version) ?: return@runBlocking

        if (dependency.extension != "jar" && dependency.extension != "aar") {
            callback.invalidPackaging(dependency)
            return@runBlocking
        }

        val defaultAndroidJar = BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.resolve("android.jar").absolutePath
        val libraryJars = listOf(
            BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.toPath()
                .resolve("core-lambda-stubs.jar"), Paths.get(
                buildSettings?.getValue(
                    BuildSettings.SETTING_ANDROID_JAR_PATH,
                    defaultAndroidJar
                ) ?: defaultAndroidJar
            )
        )
        val dependencyClasspath = mutableListOf<Path>()

        val classpath = buildSettings?.getValue(BuildSettings.SETTING_CLASSPATH, "") ?: ""

        classpath.split(":").forEach {
            if (it.isEmpty()) return@forEach
            dependencyClasspath.add(Paths.get(it))
        }

        // For AAR, check classes.jar (the unzip product) since classes.aar gets deleted after extraction
        val mainCacheCheckFile = if (dependency.extension == "aar") "classes.jar" else "classes.${dependency.extension}"
        val existingMainFile = resolveExistingLibFile(dependency.artifactId, dependency.version, mainCacheCheckFile)
        val mainCached = existingMainFile != null
        if (!mainCached) {
            try {
                dependency.downloadTo(
                    File(downloadPath + "/${dependency.artifactId}-v${dependency.version}/classes.${dependency.extension}")
                        .apply { parentFile?.mkdirs() }
                )
            } catch (e: Exception) {
                if (isStoragePermissionError(e) && downloadPath == FilePathUtil.getLocalLibsDir().absolutePath) {
                    // Primary path blocked by FUSE, retry with app-specific fallback
                    switchToFallbackPath()
                    try {
                        dependency.downloadTo(
                            File(downloadPath + "/${dependency.artifactId}-v${dependency.version}/classes.${dependency.extension}")
                                .apply {
                                    parentFile?.mkdirs()
                                }
                        )
                    } catch (e2: Exception) {
                        callback.onDownloadError(dependency, e2)
                        return@runBlocking
                    }
                } else {
                    callback.onDownloadError(dependency, e)
                    return@runBlocking
                }
            }
        }

        if (dependency.extension == "aar" && !mainCached) {
            callback.unzipping(dependency)
            try {
                unzip(
                    Paths.get(
                        downloadPath,
                        "${dependency.artifactId}-v${dependency.version}",
                        "classes.aar"
                    )
                )
                Files.delete(
                    Paths.get(
                        downloadPath,
                        "${dependency.artifactId}-v${dependency.version}",
                        "classes.aar"
                    )
                )
                val packageName = findPackageName(
                    Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}")
                        .toAbsolutePath().toString(),
                    dependency.groupId
                )
                Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}", "config")
                    .writeText(packageName)
            } catch (e: Exception) {
                callback.onDownloadError(dependency, e)
                return@runBlocking
            }
        }

        val existingMainJar = resolveExistingLibFile(dependency.artifactId, dependency.version, "classes.jar")
        val jar = existingMainJar?.toPath() ?: Paths.get(
            downloadPath,
            "${dependency.artifactId}-v${dependency.version}",
            "classes.jar"
        )

        val existingMainDex = resolveExistingLibFile(dependency.artifactId, dependency.version, "classes.dex")
        if (existingMainDex != null) {
            callback.onResolutionComplete(dependency)
        } else {
            callback.dexing(dependency)
            try {
                compileJarWithFallback(jar, dependencyClasspath, libraryJars)
                callback.onResolutionComplete(dependency)
            } catch (t: Throwable) {
                if (t is Exception || t is OutOfMemoryError) {
                    System.gc()
                    val reportException = if (t is OutOfMemoryError)
                        RuntimeException("Out of memory during dexing. The library may be too large for this device.", t)
                    else t as Exception
                    callback.dexingFailed(dependency, reportException)
                    return@runBlocking
                } else throw t
            }
        }

        if (skipDependencies) {
            callback.onSkippingResolution(dependency)
            callback.onTaskCompleted(listOf("${dependency.artifactId}-v${dependency.version}"))
            return@runBlocking
        }
        val cachedDeps = loadDependencyTreeCache(dependency)
        val allDeps: Collection<Artifact>
        if (cachedDeps != null) {
            allDeps = cachedDeps
        } else {
            try {
                allDeps = withTimeout(300_000L) {
                    dependency.resolveDependencyTree(skipFilter = { dep ->
                        isBuiltInDependency(dep.groupId, dep.artifactId, dep.version)
                    })
                    dependency.getAllDependencies()
                }
            } catch (e: TimeoutCancellationException) {
                // Timed out resolving transitive deps; complete with just the main library
                callback.onTaskCompleted(listOf("${dependency.artifactId}-v${dependency.version}"))
                return@runBlocking
            } catch (t: Throwable) {
                callback.onDependenciesNotFound(dependency)
                return@runBlocking
            }
            // Note: dependency tree cache is saved after processedDeps is populated below,
            // so only actually-downloaded (non-built-in) deps are recorded.
        }

        val processedDeps = mutableListOf<Artifact>()
        val builtInKeys = mutableSetOf<String>()

        allDeps.forEach { dep ->
            if (isBuiltInDependency(dep.groupId, dep.artifactId, dep.version)) {
                builtInKeys.add("${dep.groupId}:${dep.artifactId}:${dep.version}")
                callback.onSkippingResolution(dep)
                return@forEach
            }

            if (dep.extension != "jar" && dep.extension != "aar") {
                callback.invalidPackaging(dep)
                return@forEach
            }

            if (dep.version.isEmpty()) {
                callback.onVersionNotFound(dep)
                return@forEach
            }

            var path = Paths.get(
                downloadPath,
                "${dep.artifactId}-v${dep.version}",
                "classes.${dep.extension}"
            )

            // For AAR, check classes.jar (the unzip product) since classes.aar gets deleted after extraction
            val depCacheCheckFile = if (dep.extension == "aar") "classes.jar" else "classes.${dep.extension}"
            val existingDepFile = resolveExistingLibFile(dep.artifactId, dep.version, depCacheCheckFile)
            val depCached = existingDepFile != null
            if (!depCached) {
                try {
                    Files.createDirectories(path.parent)
                    dep.downloadTo(File(path.toString()))
                } catch (e: Exception) { 
                    if (isStoragePermissionError(e) && downloadPath == FilePathUtil.getLocalLibsDir().absolutePath) {
                        switchToFallbackPath()
                        path = Paths.get(
                            downloadPath,
                            "${dep.artifactId}-v${dep.version}",
                            "classes.${dep.extension}"
                        )
                        try {
                            Files.createDirectories(path.parent)
                            dep.downloadTo(File(path.toString()))
                        } catch (e2: Exception) {
                            callback.onDownloadError(dep, e2)
                            return@forEach
                        }
                    } else {
                        callback.onDownloadError(dep, e)
                        return@forEach
                    }
                }
            }

            if (dep.extension == "aar" && !depCached) {
                callback.unzipping(dep)
                try {
                    unzip(path)
                    Files.delete(path)
                    val packageName =
                        findPackageName(path.parent.toAbsolutePath().toString(), dep.groupId)
                    path.parent.resolve("config").writeText(packageName)
                } catch (e: Exception) {
                    callback.onDownloadError(dep, e)
                    return@forEach
                }
            }

            val depJar = if (existingDepFile != null) {
                // Use the actual location (may be in fallback path)
                existingDepFile.parentFile!!.resolve("classes.jar").toPath()
            } else if (dep.extension == "jar") {
                path
            } else {
                Paths.get(downloadPath, "${dep.artifactId}-v${dep.version}", "classes.jar")
            }
            if (Files.notExists(depJar)) {
                callback.onDependenciesNotFound(dep)
                return@forEach
            }

            dependencyClasspath.add(depJar)
            processedDeps.add(dep)
        }

        // Save ALL resolved deps (including built-in) so the UI can display the full dependency tree.
        // Built-in deps are flagged so smart deletion knows not to delete their (non-existent) folders.
        if (cachedDeps == null) {
            saveDependencyTreeCache(dependency, allDeps, builtInKeys)
        }

        processedDeps.forEach { dep ->
            val existingDepDex = resolveExistingLibFile(dep.artifactId, dep.version, "classes.dex")
            if (existingDepDex != null) {
                callback.onResolutionComplete(dep)
                return@forEach
            }

            val dexJar = resolveExistingLibFile(dep.artifactId, dep.version, "classes.jar")?.toPath()
                ?: Paths.get(downloadPath, "${dep.artifactId}-v${dep.version}", "classes.jar")

            callback.dexing(dep)
            try {
                compileJarWithFallback(
                    dexJar, dependencyClasspath.toMutableList().apply { remove(dexJar) }, libraryJars
                )
                callback.onResolutionComplete(dep)
            } catch (t: Throwable) {
                if (t is Exception || t is OutOfMemoryError) {
                    System.gc()
                    val reportException = if (t is OutOfMemoryError)
                        RuntimeException("Out of memory during dexing: ${dep.artifactId}", t)
                    else t as Exception
                    callback.dexingFailed(dep, reportException)
                } else throw t
                return@forEach
            }
        }

        val mainDepName = "${dependency.artifactId}-v${dependency.version}"
        val completedNames = buildList {
            add(mainDepName)
            processedDeps.forEach { dep ->
                val name = "${dep.artifactId}-v${dep.version}"
                if (name != mainDepName) add(name)
            }
        }
        callback.onTaskCompleted(completedNames)
    }

    private fun findPackageName(path: String, defaultValue: String): String {
        val manifest =
            File(path).walk().filter { it.isFile && it.name == "AndroidManifest.xml" }.firstOrNull()
        val content = manifest?.readText() ?: return defaultValue
        val p = Pattern.compile("<manifest.*package=\"(.*?)\"", Pattern.DOTALL)
        val m = p.matcher(content)
        if (m.find()) {
            return m.group(1)!!
        }

        return defaultValue
    }

    private fun unzip(path: Path) {
        val zipFile = ZipFile(path.toFile())
        zipFile.use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryDestination = path.parent.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(entryDestination)
                } else {
                    Files.createDirectories(entryDestination.parent)
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(entryDestination).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun saveDependencyTreeCache(
        mainDep: Artifact,
        deps: Collection<Artifact>,
        builtInKeys: Set<String> = emptySet()
    ) {
        try {
            val cacheFile = Paths.get(downloadPath, "${mainDep.artifactId}-v${mainDep.version}", "dependency-tree.json")
            Files.createDirectories(cacheFile.parent)
            val cachedDeps = deps.map { dep ->
                val key = "${dep.groupId}:${dep.artifactId}:${dep.version}"
                hashMapOf<String, Any?>(
                    "groupId" to dep.groupId,
                    "artifactId" to dep.artifactId,
                    "version" to dep.version,
                    "extension" to dep.extension,
                    "repoUrl" to dep.repository?.getURL(),
                    "repoName" to dep.repository?.getName(),
                    "builtIn" to builtInKeys.contains(key)
                )
            }
            cacheFile.writeText(Gson().toJson(cachedDeps))
        } catch (_: Exception) {
            // Cache write failure is non-fatal
        }
    }

    private fun loadDependencyTreeCache(mainDep: Artifact): List<Artifact>? {
        val cacheFile = resolveExistingLibFile(mainDep.artifactId, mainDep.version, "dependency-tree.json")?.toPath()
            ?: return null
        return try {
            val cached = Gson().fromJson(cacheFile.readText(), Helper.TYPE_MAP_LIST)
                ?: return null
            cached.map { entry ->
                val repoUrl = entry["repoUrl"] as? String
                val repoName = entry["repoName"] as? String
                Artifact(
                    groupId = entry["groupId"] as? String ?: return null,
                    artifactId = entry["artifactId"] as? String ?: return null,
                    version = entry["version"] as? String ?: return null
                ).apply {
                    extension = entry["extension"] as? String ?: "jar"
                    if (repoUrl != null && repoName != null) {
                        repository = object : Repository {
                            override fun getName() = repoName
                            override fun getURL() = repoUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isBuiltInDependency(groupId: String, artifactId: String, version: String): Boolean {
        // Skip transitive deps that are already bundled as BuiltInLibraries.
        // This reduces unnecessary downloads, dex compilations, and DEX merge memory during build.
        // Even without skipping, DexMerger's KEEP_FIRST means the built-in wins unpredictably;
        // skipping makes that behavior deterministic.
        //
        // For libraries with breaking API changes between major versions (e.g. OkHttp 4→5),
        // we only skip if the built-in major version >= the required major version.
        // For all other groups the built-in versions are backward-compatible across minor versions.

        // Only skip specific androidx groups that are actually bundled in BuiltInLibraries.
        // Non-built-in androidx libs (media3, camera, compose, navigation, etc.) must NOT be skipped.
        if (groupId.startsWith("androidx.") && groupId in BUILT_IN_ANDROIDX_GROUPS) return true
        if (groupId == "com.google.firebase") return true
        if (groupId.startsWith("com.google.android.gms")) return true
        if (groupId.startsWith("com.google.android.datatransport")) return true
        if (groupId == "com.google.android.material") return true
        if (groupId == "com.google.android.play") return true
        if (groupId == "com.google.android.recaptcha") return true
        if (groupId == "com.google.android.ump") return true
        if (groupId.startsWith("org.jetbrains.kotlin")) return true  // Kotlin guarantees binary compat
        if (groupId == "org.jetbrains") return true
        if (groupId == "org.jspecify") return true
        if (groupId == "com.google.code.gson") return true
        if (groupId == "com.google.errorprone") return true
        if (groupId == "com.google.auto.value") return true
        if (groupId == "com.github.bumptech.glide") return true
        if (groupId == "com.airbnb.android" && artifactId == "lottie") return true
        if (groupId == "com.pierfrancescosoffritti.androidyoutubeplayer") return true
        if (groupId == "de.hdodenhof" && artifactId == "circleimageview") return true
        if (groupId == "com.andrognito" && artifactId == "patternlockview") return true
        if (groupId == "br.tiagohm.codeview") return true
        if (groupId == "affan.ahmad.otp") return true
        if (groupId == "com.sayuti") return true

        // Version-aware check for libraries with known breaking changes between major versions.
        // Skip only if the built-in major version >= required major version.
        if (groupId == "com.squareup.okhttp3") {
            // Built-in: okhttp-android-5.1.0 (major 5). OkHttp 4→5 removed some APIs.
            return parseMajorVersion(version) <= 5
        }
        if (groupId == "com.squareup.okio") {
            // Built-in: okio-jvm-3.15.0 (major 3). Okio 2→3 removed some APIs.
            return parseMajorVersion(version) <= 3
        }

        return false
    }

    /** Extracts the leading integer major version from a version string, e.g. "4.12.0" → 4. */
    private fun parseMajorVersion(version: String): Int =
        version.trimStart().split(".", "-").firstOrNull()?.toIntOrNull() ?: 0

    /**
     * Compiles a JAR to DEX using D8, trying without classpath first to minimize memory usage.
     * If the minimal-classpath attempt fails (e.g. desugaring needs type info from dependencies),
     * retries with full classpath. This reduces D8 memory from O(N) to O(1) for most libraries.
     */
    private fun compileJarWithFallback(jarFile: Path, jars: List<Path>, libraryJars: List<Path>) {
        Files.createDirectories(jarFile.parent)
        val minApi = buildSettings?.minSdkVersion ?: 26
        try {
            // Fast path: no classpath, minimal memory
            D8.run(
                D8Command.builder().setIntermediate(true).setMode(CompilationMode.RELEASE)
                    .setMinApiLevel(minApi)
                    .addProgramFiles(jarFile).addLibraryFiles(libraryJars)
                    .setOutput(jarFile.parent, OutputMode.DexIndexed).build()
            )
        } catch (_: Throwable) {
            // Fallback: full classpath for desugaring
            System.gc()
            D8.run(
                D8Command.builder().setIntermediate(true).setMode(CompilationMode.RELEASE)
                    .setMinApiLevel(minApi)
                    .addProgramFiles(jarFile).addLibraryFiles(libraryJars).addClasspathFiles(jars)
                    .setOutput(jarFile.parent, OutputMode.DexIndexed).build()
            )
        } finally {
            System.gc()
        }
    }
}
