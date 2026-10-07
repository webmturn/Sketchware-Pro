package pro.sketchware.core.project;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import pro.sketchware.beans.RootLayoutBean;
import pro.sketchware.util.FileUtil;

/** Persists root layout metadata in the project's existing view_root file. */
public class InjectRootLayoutManager {
    private final String path;

    public InjectRootLayoutManager(String scId) {
        this(new File(SketchwarePaths.getDataPath(scId), "view_root"));
    }

    InjectRootLayoutManager(File storageFile) {
        path = storageFile.getAbsolutePath();
    }

    public static RootLayoutBean getDefaultRootLayout() {
        return RootLayoutBean.createDefault();
    }

    public void set(String name, RootLayoutBean layout) {
        Map<String, RootLayoutBean> data = get();
        if (data == null) {
            data = new LinkedHashMap<>();
        }
        data.put(name, layout);
        save(data);
    }

    private void save(Map<String, RootLayoutBean> data) {
        FileUtil.writeFile(path, new Gson().toJson(data));
    }

    public RootLayoutBean getLayoutByFileName(String name) {
        return get().getOrDefault(name, getDefaultRootLayout());
    }

    public Map<String, RootLayoutBean> get() {
        if (FileUtil.isExistFile(path)) {
            return new Gson().fromJson(FileUtil.readFile(path),
                    new TypeToken<LinkedHashMap<String, RootLayoutBean>>() { }.getType());
        }
        return new LinkedHashMap<>();
    }
}
