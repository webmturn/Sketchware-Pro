package pro.sketchware.core.build;

/** Shared stage IDs for build producers, progress displays, and notifications. */
public final class BuildStage {
    public static final int DELETE_TEMPORARY_FILES = 1;
    public static final int GENERATE_SOURCES = 2;
    public static final int EXTRACT_LIBRARIES = 3;
    public static final int EXTRACT_LIBRARY_DEX = 4;
    public static final int EXTRACT_LIBRARY_RESOURCES = 5;
    public static final int EXTRACT_SIGNING_KEYS = 6;
    public static final int EXTRACT_ANDROID_JAR = 7;
    public static final int PREPARE_RESOURCE_COMPILATION = 8;
    public static final int COMPILE_RESOURCES = 9;
    public static final int LINK_RESOURCES = 10;
    public static final int GENERATE_VIEW_BINDING = 11;
    public static final int COMPILE_KOTLIN = 12;
    public static final int COMPILE_JAVA = 13;
    public static final int OBFUSCATE_STRINGS = 14;
    public static final int SHRINK_WITH_R8 = 15;
    public static final int SHRINK_WITH_PROGUARD = 16;
    public static final int COMPILE_DEX = 17;
    public static final int MERGE_DEX = 18;
    public static final int PACKAGE_APK = 19;
    public static final int SIGN_APK = 20;
    public static final int INDETERMINATE = -1;
    public static final int TOTAL_STEPS = SIGN_APK;

    private BuildStage() {
    }
}
