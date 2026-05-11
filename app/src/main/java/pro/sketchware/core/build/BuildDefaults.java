package pro.sketchware.core.build;

/**
 * Default values for build-related settings.
 *
 * <p>These constants represent the SDK versions used when a project does not specify
 * its own values (e.g. newly created projects, or projects imported without complete
 * build configuration). They are referenced by {@link ProjectFilePaths},
 * {@link ManifestGenerator}, the resource compiler, and the project settings dialog.
 */
public final class BuildDefaults {

    public static final int VAR_DEFAULT_MIN_SDK_VERSION = 21;
    public static final int VAR_DEFAULT_TARGET_SDK_VERSION = 34;

    private BuildDefaults() {
        // no instances
    }
}
