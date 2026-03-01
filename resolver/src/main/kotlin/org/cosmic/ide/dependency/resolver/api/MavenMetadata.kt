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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "metadata")
data class MavenMetadata(
    @JacksonXmlProperty(localName = "modelVersion", isAttribute = true)
    val modelVersion: String?,
    @JacksonXmlProperty(localName = "groupId")
    val groupId: String,
    @JacksonXmlProperty(localName = "artifactId")
    val artifactId: String,
    @JacksonXmlProperty(localName = "versioning")
    val versioning: Versioning
)

data class Versioning(
    @JacksonXmlProperty(localName = "latest")
    val latest: String?,
    @JacksonXmlProperty(localName = "release")
    val release: String?,
    @JacksonXmlProperty(localName = "versions")
    @get:JacksonXmlElementWrapper(useWrapping = false)
    val versionsList: Versions,
    @JacksonXmlProperty(localName = "lastUpdated")
    val lastUpdated: String?
) {
    val versions: List<String>
        get() = versionsList.versionList
}

data class Versions(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "version")
    val versionList: List<String>
)
