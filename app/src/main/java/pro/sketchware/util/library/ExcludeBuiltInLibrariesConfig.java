package pro.sketchware.util.library;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import pro.sketchware.core.project.SketchwarePaths;
import pro.sketchware.util.library.BuiltInLibraries;
import pro.sketchware.util.FileUtil;
import pro.sketchware.util.LogUtil;

public class ExcludeBuiltInLibrariesConfig {
    private static final String TAG = "ExcludeBuiltInLibraries";

    private ExcludeBuiltInLibrariesConfig() {
    }

    public static File getConfigPath(String scId) {
        return new File(SketchwarePaths.getProjectExcludedBuiltInLibrariesPath(scId));
    }

    public static void saveConfig(String scId, boolean isExcludingEnabled, List<BuiltInLibraries.BuiltInLibrary> excludedLibraries) {
        List<String> excludedLibraryNames = excludedLibraries.stream()
                .map(BuiltInLibraries.BuiltInLibrary::getName)
                .collect(Collectors.toList());
        Pair<Boolean, List<String>> config = new Pair<>(isExcludingEnabled, excludedLibraryNames);
        FileUtil.writeFile(getConfigPath(scId).getAbsolutePath(), new Gson().toJson(config));
    }

    @Nullable
    public static Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> readConfig(String scId) {
        File configPath = getConfigPath(scId);
        if (configPath.isFile()) {
            String content = FileUtil.readFile(configPath.getAbsolutePath());

            String errorMessage;
            try {
                Pair<Boolean, List<String>> config = new Gson().fromJson(content, new TypeToken<>() {
                });
                if (config != null) {
                    List<BuiltInLibraries.BuiltInLibrary> libraries = config.second.stream()
                            .map(s -> {
                                Optional<BuiltInLibraries.BuiltInLibrary> library = BuiltInLibraries.BuiltInLibrary.ofName(s);
                                return library.orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    return new Pair<>(config.first, libraries);
                }
                errorMessage = "read config was null";
            } catch (JsonSyntaxException e) {
                errorMessage = Log.getStackTraceString(e);
            }

            LogUtil.e(TAG, "Couldn't parse config: " + errorMessage);
        }
        return null;
    }

    public static boolean isExcludingEnabled(String scId) {
        Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config = readConfig(scId);
        if (config != null) {
            return config.first;
        } else {
            return false;
        }
    }

    @NonNull
    public static List<BuiltInLibraries.BuiltInLibrary> getExcludedLibraries(String scId) {
        Pair<Boolean, List<BuiltInLibraries.BuiltInLibrary>> config = readConfig(scId);
        if (config != null) {
            return config.second;
        } else {
            return Collections.emptyList();
        }
    }
}
