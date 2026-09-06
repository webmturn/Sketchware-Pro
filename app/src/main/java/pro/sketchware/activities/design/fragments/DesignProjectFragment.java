package pro.sketchware.activities.design.fragments;

import android.os.Bundle;

import pro.sketchware.activities.base.BaseFragment;
import pro.sketchware.core.project.LibraryManager;
import pro.sketchware.core.project.ProjectDataManager;
import pro.sketchware.core.project.ProjectDataStore;
import pro.sketchware.core.project.ResourceManager;

/**
 * Base fragment for design tabs that require an explicit project identity.
 *
 * <p>Fragment arguments survive configuration changes and process recreation, so design tabs no
 * longer need to reach into their host Activity's Intent or duplicate project IDs in saved state.</p>
 */
public abstract class DesignProjectFragment extends BaseFragment {

    private static final String ARG_PROJECT_ID = "design_project_id";

    protected static Bundle projectArguments(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("projectId must not be empty");
        }
        Bundle arguments = new Bundle();
        arguments.putString(ARG_PROJECT_ID, projectId);
        return arguments;
    }

    protected final String requireProjectId() {
        String projectId = requireArguments().getString(ARG_PROJECT_ID);
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalStateException("Missing design project ID");
        }
        return projectId;
    }

    protected final ProjectDataStore projectData() {
        return ProjectDataManager.getProjectDataManager(requireProjectId());
    }

    protected final ResourceManager projectResources() {
        return ProjectDataManager.getResourceManager(requireProjectId());
    }

    protected final LibraryManager projectLibraries() {
        return ProjectDataManager.getLibraryManager(requireProjectId());
    }
}
