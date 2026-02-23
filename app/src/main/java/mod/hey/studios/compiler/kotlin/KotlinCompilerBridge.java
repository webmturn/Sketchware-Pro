package mod.hey.studios.compiler.kotlin;

import java.io.File;

import pro.sketchware.core.ProjectBuilder;
import pro.sketchware.core.ProjectFilePaths;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import pro.sketchware.util.library.BuiltInLibraryManager;
import pro.sketchware.utility.FileUtil;

public class KotlinCompilerBridge {
    public static void compileKotlinCodeIfPossible(BuildProgressReceiver receiver, ProjectBuilder builder) throws Throwable {
        if (KotlinCompilerUtil.areAnyKtFilesPresent(builder)) {
            receiver.onProgress("Kotlin is compiling...", 12);
            new KotlinCompiler(builder).compile();
        }
    }

    public static void maybeAddKotlinBuiltInLibraryDependenciesIfPossible(ProjectBuilder builder, BuiltInLibraryManager builtInLibraryManager) {
        if (KotlinCompilerUtil.areAnyKtFilesPresent(builder)) {
            builtInLibraryManager.addLibrary(BuiltInLibraries.JETBRAINS_KOTLIN_STDLIB);
        }
    }

    public static void maybeAddKotlinFilesToClasspath(StringBuilder classpath, ProjectFilePaths workspace) {
        if (FileUtil.isExistFile(workspace.compiledClassesPath)) {
            classpath.append(workspace.compiledClassesPath);
            classpath.append(":");
        }
    }

    public static String getKotlinHome(ProjectFilePaths workspace) {
        return workspace.binDirectoryPath + File.separator + "kotlin_home";
    }
}
