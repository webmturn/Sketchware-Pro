package pro.sketchware.core.codegen;
import pro.sketchware.core.project.BuildConfig;
import pro.sketchware.core.project.BuiltInLibrary;
import pro.sketchware.core.project.SketchwarePaths;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.util.Helper;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.editor.manage.library.ExcludeBuiltInLibrariesActivity;
import mod.agus.jcoderz.handle.component.ConstVarComponent;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.core.codegen.CodeFormatter;

/**
 * Generates Gradle build files (settings.gradle, build.gradle) for compiled projects.
 * Extracted from {@link ComponentCodeGenerator} to reduce its size.
 */
public class GradleFileGenerator {

    private static final String FIREBASE_BOM_VERSION = "33.7.0";

    /**
     * @return Content of a <code>settings.gradle</code> file, with indentation
     */
    public static String getSettingsGradle() {
        return "include ':app'\r\n";
    }

    /**
     * @return Content of a <code>build.gradle</code> file for the module ':app', with indentation
     */
    public static String getBuildGradleString(int compileSdkVersion, int minSdkVersion, String targetSdkVersion, BuildConfig metadata, boolean isViewBindingEnabled) {
        StringBuilder content = new StringBuilder("plugins {\r\n" +
                "id 'com.android.application'\r\n" +
                "}\r\n" +
                "\r\n" +
                "android {\r\n" +
                "compileSdk " + compileSdkVersion + "\r\n" +
                "\r\n");
        if (new BuildSettings(metadata.sc_id)
                .getValue(BuildSettings.SETTING_NO_HTTP_LEGACY, BuildSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(BuildSettings.SETTING_GENERIC_VALUE_FALSE)) {
            content.append("""
                    useLibrary 'org.apache.http.legacy'\r
                    \r
                    """);
        }
        content.append("defaultConfig {\r\n" + "applicationId \"")
                .append(metadata.packageName)
                .append("\"\r\n")
                .append("namespace \"")
                .append(metadata.packageName)
                .append("\"\r\n")
                .append("minSdkVersion ")
                .append(minSdkVersion)
                .append("\r\n")
                .append("targetSdkVersion ")
                .append(targetSdkVersion)
                .append("\r\n")
                .append("versionCode ")
                .append(metadata.versionCode)
                .append("\r\n")
                .append("versionName \"")
                .append(metadata.versionName)
                .append("\"\r\n")
                .append("}\r\n")
                .append("\r\n")
                .append("buildTypes {\r\n")
                .append("release {\r\n")
                .append("minifyEnabled false\r\n")
                .append("proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'\r\n")
                .append("}\r\n")
                .append("}\r\n");

        if (isViewBindingEnabled) {
            content.append("buildFeatures {\r\n viewBinding true\r\n}\r\n");
        }

        content.append("}\r\n")
                .append("\r\n")
                .append("dependencies {\r\n")
                .append("implementation fileTree(dir: 'libs', include: ['*.jar'])\r\n");

        List<BuiltInLibraries.BuiltInLibrary> excludedLibraries = ExcludeBuiltInLibrariesActivity.getExcludedLibraries(metadata.sc_id);
        if (isLibraryNotExcluded(BuiltInLibraries.ANDROIDX_APPCOMPAT, excludedLibraries) && metadata.isAppCompatEnabled) {
            content.append(dependency("androidx.appcompat", "appcompat", BuiltInLibraries.ANDROIDX_APPCOMPAT));
            content.append(dependency("com.google.android.material", "material", BuiltInLibraries.MATERIAL));
        }

        if (metadata.isFirebaseEnabled) {
            content.append("implementation platform('com.google.firebase:firebase-bom:").append(FIREBASE_BOM_VERSION).append("')\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.FIREBASE_AUTH, excludedLibraries) && metadata.isFirebaseAuthUsed) {
            content.append("implementation 'com.google.firebase:firebase-auth'\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.FIREBASE_DATABASE, excludedLibraries) && metadata.isFirebaseDatabaseUsed) {
            content.append("implementation 'com.google.firebase:firebase-database'\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.FIREBASE_STORAGE, excludedLibraries) && metadata.isFirebaseStorageUsed) {
            content.append("implementation 'com.google.firebase:firebase-storage'\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.PLAY_SERVICES_ADS, excludedLibraries) && metadata.isAdMobEnabled) {
            content.append(dependency("com.google.android.gms", "play-services-ads", BuiltInLibraries.PLAY_SERVICES_ADS));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.PLAY_SERVICES_MAPS, excludedLibraries) && metadata.isMapUsed) {
            content.append(dependency("com.google.android.gms", "play-services-maps", BuiltInLibraries.PLAY_SERVICES_MAPS));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.GLIDE, excludedLibraries) && metadata.isGlideUsed) {
            content.append(dependency("com.github.bumptech.glide", "glide", BuiltInLibraries.GLIDE));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.GSON, excludedLibraries) && metadata.isGsonUsed) {
            content.append(dependency("com.google.code.gson", "gson", BuiltInLibraries.GSON));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.OKHTTP_ANDROID, excludedLibraries) && metadata.isHttp3Used) {
            content.append(dependency("com.squareup.okhttp3", "okhttp", BuiltInLibraries.OKHTTP_ANDROID, "okhttp-android"));
        }

        ConstVarComponent extraMetadata = metadata.constVarComponent;
        if (isLibraryNotExcluded(BuiltInLibraries.CIRCLEIMAGEVIEW, excludedLibraries) && extraMetadata.isCircleImageViewUsed) {
            content.append(dependency("de.hdodenhof", "circleimageview", BuiltInLibraries.CIRCLEIMAGEVIEW));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.ANDROID_YOUTUBE_PLAYER, excludedLibraries) && extraMetadata.isYoutubePlayerUsed) {
            content.append(dependency("com.pierfrancescosoffritti", "androidyoutubeplayer", BuiltInLibraries.ANDROID_YOUTUBE_PLAYER, "android-youtube-player"));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.CODEVIEW, excludedLibraries) && extraMetadata.isCodeViewUsed) {
            content.append(dependency("br.tiagohm", "codeview", BuiltInLibraries.CODEVIEW, "CodeView"));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.LOTTIE, excludedLibraries) && extraMetadata.isLottieUsed) {
            content.append(dependency("com.airbnb.android", "lottie", BuiltInLibraries.LOTTIE));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.OTPVIEW, excludedLibraries) && extraMetadata.isOTPViewUsed) {
            content.append("implementation 'affan.ahmad:otp:0.1.0'\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.PATTERN_LOCK_VIEW, excludedLibraries) && extraMetadata.isPatternLockViewUsed) {
            content.append("implementation 'com.andrognito:patternlockview:1.0.0'\r\n");
        }

        if (isLibraryNotExcluded(BuiltInLibraries.PLAY_SERVICES_AUTH, excludedLibraries) && extraMetadata.isFBGoogleUsed) {
            content.append(dependency("com.google.android.gms", "play-services-auth", BuiltInLibraries.PLAY_SERVICES_AUTH));
        }

        if (isLibraryNotExcluded(BuiltInLibraries.FIREBASE_MESSAGING, excludedLibraries) && extraMetadata.isFCMUsed) {
            content.append("implementation 'com.google.firebase:firebase-messaging'\r\n");
        }

        String sc_id = metadata.sc_id;
        String local_lib_file = SketchwarePaths.getProjectLocalLibraryPath(sc_id);
        String fileContent = FileUtil.readFile(local_lib_file);

        if (!fileContent.isEmpty()) {
            Gson gson = new Gson();
            ArrayList<HashMap<String, Object>> localLibraries = gson.fromJson(fileContent, Helper.TYPE_MAP_LIST);
            if (localLibraries != null) {
                for (HashMap<String, Object> library : localLibraries) {
                    String dependency = resolveDependencyNotation(library);
                    if (dependency != null) {
                        content.append("implementation '").append(dependency).append("'\r\n");
                    }
                }
            }
        }

        return CodeFormatter.formatCode(content + "}\r\n", false);
    }

    private static boolean isLibraryNotExcluded(String libraryName, List<BuiltInLibraries.BuiltInLibrary> excludedLibraries) {
        var library = BuiltInLibraries.BuiltInLibrary.ofName(libraryName);
        return library.isPresent() && !excludedLibraries.contains(library.get());
    }

    private static String dependency(String groupId, String artifactId, String builtInLibraryName) {
        return dependency(groupId, artifactId, builtInLibraryName, artifactId);
    }

    private static String dependency(String groupId, String artifactId, String builtInLibraryName, String builtInArtifactName) {
        return "implementation '" + groupId + ":" + artifactId + ":" + getBuiltInLibraryVersion(builtInLibraryName, builtInArtifactName) + "'\r\n";
    }

    private static String getBuiltInLibraryVersion(String builtInLibraryName, String artifactName) {
        String versionPrefix = artifactName + "-";
        if (!builtInLibraryName.startsWith(versionPrefix)) {
            throw new IllegalArgumentException("Built-in library name does not start with " + versionPrefix + ": " + builtInLibraryName);
        }
        return builtInLibraryName.substring(versionPrefix.length());
    }

    private static String resolveDependencyNotation(HashMap<String, Object> library) {
        Object dependencyObject = library.get("dependency");
        if (dependencyObject instanceof String dependency && isValidMavenCoordinate(dependency)) {
            return dependency;
        }

        Object nameObject = library.get("name");
        if (nameObject instanceof String name) {
            File coordinateFile = resolveCoordinateFile(name);
            if (coordinateFile.exists()) {
                String storedDependency = FileUtil.readFile(coordinateFile.getAbsolutePath()).trim();
                if (isValidMavenCoordinate(storedDependency)) {
                    return storedDependency;
                }
            }
        }

        return null;
    }

    private static File resolveCoordinateFile(String libraryName) {
        File primaryCoordinateFile = new File(FilePathUtil.getLocalLibsDir(), libraryName + "/maven-coordinate");
        if (primaryCoordinateFile.exists()) {
            return primaryCoordinateFile;
        }

        File fallbackCoordinateFile = new File(FilePathUtil.getLocalLibsFallbackDir(), libraryName + "/maven-coordinate");
        if (fallbackCoordinateFile.exists()) {
            return fallbackCoordinateFile;
        }

        return primaryCoordinateFile;
    }

    private static boolean isValidMavenCoordinate(String dependency) {
        if (dependency == null || dependency.isEmpty()) {
            return false;
        }

        String[] parts = dependency.split(":");
        if (parts.length != 3) {
            return false;
        }

        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return A generated top-level <code>build.gradle</code> file, with indentation
     */
    public static String getTopLevelBuildGradle(String androidGradlePluginVersion, String
            googleMobileServicesVersion) {
        return "// Top-level build file where you can add configuration options common to all sub-projects/modules.\r\n" +
                "buildscript {\r\n" +
                "    repositories {\r\n" +
                "        google()\r\n" +
                "        mavenCentral()\r\n" +
                "    }\r\n" +
                "    dependencies {\r\n" +
                "        classpath 'com.android.tools.build:gradle:" + androidGradlePluginVersion + "'\r\n" +
                "        classpath 'com.google.gms:google-services:" + googleMobileServicesVersion + "'\r\n" +
                "        // NOTE: Do not place your application dependencies here; they belong\r\n" +
                "        // in the individual module build.gradle files\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "\r\n" +
                "allprojects {\r\n" +
                "    repositories {\r\n" +
                "        google()\r\n" +
                "        mavenCentral()\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "\r\n" +
                "tasks.register(\"clean\", Delete) {\r\n" +
                "    delete rootProject.buildDir\r\n" +
                "}\r\n";
    }
}
