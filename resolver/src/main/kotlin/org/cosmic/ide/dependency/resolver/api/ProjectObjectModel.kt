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

@JacksonXmlRootElement(localName = "project")
data class ProjectObjectModel(
    @JacksonXmlProperty(localName = "modelVersion") val modelVersion: String?,
    @JacksonXmlProperty(localName = "parent") val parent: Parent?,
    @JacksonXmlProperty(localName = "groupId") val groupId: String?,
    @JacksonXmlProperty(localName = "artifactId") val artifactId: String,
    @JacksonXmlProperty(localName = "version") val version: String?,
    @JacksonXmlProperty(localName = "packaging") val packaging: String?,
    @JacksonXmlProperty(localName = "name") val name: String?,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "license") val licenses: List<License>?,

    @JacksonXmlProperty(localName = "dependencyManagement") val dependencyManagement: DependencyManagement?,
    @JacksonXmlProperty(localName = "dependencies")
    @JacksonXmlElementWrapper(localName = "dependencies", useWrapping = true)
    val dependencies: List<Dependency>?,

    @JacksonXmlProperty(localName = "properties")
    @JacksonXmlElementWrapper(localName = "properties", useWrapping = true)
    val properties: Map<String, String>?,

    @JacksonXmlProperty(localName = "build") val build: Build?,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "profile") val profiles: List<Profile>?
)

data class DependencyManagement(
    @JacksonXmlProperty(localName = "dependencies")
    @JacksonXmlElementWrapper(localName = "dependencies", useWrapping = true)
    val dependencies: List<Dependency>,
    )

data class Parent(
    @JacksonXmlProperty(localName = "groupId") val groupId: String,
    @JacksonXmlProperty(localName = "artifactId") val artifactId: String,
    @JacksonXmlProperty(localName = "version") val version: String
)

data class License(
    @JacksonXmlProperty(localName = "name") val name: String,
    @JacksonXmlProperty(localName = "url") val url: String
)

data class Exclusion(
    @JacksonXmlProperty(localName = "groupId") val groupId: String?,
    @JacksonXmlProperty(localName = "artifactId") val artifactId: String,
)

data class Dependency(
    @JacksonXmlProperty(localName = "groupId")
    val groupId: String?,
    @JacksonXmlProperty(localName = "artifactId")
    val artifactId: String,
    @JacksonXmlProperty(localName = "version")
    val version: String?,
    @JacksonXmlProperty(localName = "scope")
    val scope: String?,
    @JacksonXmlProperty(localName = "type")
    val type: String? = null,
    @JacksonXmlProperty(localName = "optional")
    val optional: Boolean = false,
    @JacksonXmlProperty(localName = "exclusions")
    @JacksonXmlElementWrapper(localName = "exclusions", useWrapping = true)
    val exclusions: List<Exclusion>? = null,
) {
    override fun toString(): String {
        return "$groupId:$artifactId:$version"
    }
}

data class Build(
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "plugin") val plugins: List<Plugin>?
)

data class Plugin(
    @JacksonXmlProperty(localName = "groupId") val groupId: String?,
    @JacksonXmlProperty(localName = "artifactId") val artifactId: String?,
    @JacksonXmlProperty(localName = "version") val version: String?,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "execution") val executions: List<Execution>?
)

data class Execution(
    @JacksonXmlProperty(localName = "id") val id: String?,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "goal") val goals: List<String>?
)

data class Profile(
    @JacksonXmlProperty(localName = "id") val id: String?,
    @JacksonXmlProperty(localName = "activation") val activation: Activation?,
    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "property") val properties: Map<String, String>?
)

data class Activation(
    @JacksonXmlProperty(localName = "jdk") val jdk: String?
)
