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

import org.cosmic.ide.dependency.resolver.okHttpClient
import org.cosmic.ide.dependency.resolver.xmlDeserializer
import kotlin.jvm.java

interface Repository {
    fun checkExists(artifact: Artifact): Boolean {
        val repository = getURL()
        var dependencyUrl =
            "$repository/${artifact.groupId.replace(".", "/")}/${artifact.artifactId}/maven-metadata.xml"
        val request = okhttp3.Request.Builder()
            .url(dependencyUrl)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    artifact.mavenMetadata = xmlDeserializer.readValue(response.body!!.byteStream(),
                            MavenMetadata::class.java)

                    return true
                } else {
                    dependencyUrl =
                        "$repository/${artifact.groupId.replace(".", "/")}/${artifact.artifactId}/${artifact.version}/${artifact.artifactId}-${artifact.version}.pom"
                    val request = okhttp3.Request.Builder()
                        .url(dependencyUrl)
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            artifact.pom = xmlDeserializer.readValue(response.body!!.byteStream(),
                                    ProjectObjectModel::class.java)
                            return true
                        }
                    }
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getName(): String

    fun getURL(): String
}
