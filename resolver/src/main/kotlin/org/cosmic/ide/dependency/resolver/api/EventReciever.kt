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

import java.util.logging.Logger

open class EventReciever {

    val logger: Logger = Logger.getLogger("DependencyResolver")

    open fun artifactFound(artifact: Artifact) {
        logger.info("Found ${artifact.groupId}:${artifact.artifactId}:${artifact.version} in ${artifact.repository?.getName()}")
    }

    open fun onArtifactNotFound(artifact: Artifact) {
        logger.info("No repository contains ${artifact.groupId}:${artifact.artifactId}:${artifact.version}")
    }

    open fun onFetchingLatestVersion(artifact: Artifact) {
        logger.info("Fetching latest version of ${artifact.artifactId}")
    }

    open fun onFetchedLatestVersion(artifact: Artifact, version: String) {
        logger.info("Fetched latest version of ${artifact.artifactId}: $version")
    }

    open fun onResolving(artifact: Artifact, dependency: Artifact) {
        logger.info("Resolving $dependency from $artifact")
    }

    open fun onResolutionComplete(artifact: Artifact) {
        logger.info("Resolution complete for ${artifact.artifactId}")
    }

    open fun onSkippingResolution(artifact: Artifact) {
        logger.info("Skipping resolution of $artifact as it is already resolved")
    }

    open fun onVersionNotFound(artifact: Artifact) {
        logger.info("Version not found for ${artifact.artifactId}")
    }

    open fun onDependenciesNotFound(artifact: Artifact) {
        logger.info("No dependencies found for ${artifact.artifactId}")
    }

    open fun onInvalidScope(artifact: Artifact, scope: String) {
        logger.info("Invalid scope $scope for ${artifact.artifactId}")
    }

    open fun onInvalidPOM(artifact: Artifact) {
        logger.info("Invalid POM for ${artifact.artifactId}")
    }

    open fun onDownloadStart(artifact: Artifact) {
        logger.info("Starting download of ${artifact.artifactId}:${artifact.version}")
    }

    open fun onDownloadEnd(artifact: Artifact) {
        logger.info("Download complete for ${artifact.artifactId}:${artifact.version}")
    }

    open fun onDownloadError(artifact: Artifact, error: Throwable) {
        logger.severe("Error downloading ${artifact.artifactId}:${artifact.version}: ${error.message}")
    }
}
