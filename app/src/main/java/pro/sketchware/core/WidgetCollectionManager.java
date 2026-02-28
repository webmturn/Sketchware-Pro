package pro.sketchware.core;

import android.util.Log;

import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.beans.WidgetCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class WidgetCollectionManager extends BaseCollectionManager {
    private static volatile WidgetCollectionManager instance;
    private Gson widgetGson;

    public WidgetCollectionManager() {
        initializeGson();
    }

    public static WidgetCollectionManager getInstance() {
        if (instance == null) {
            synchronized (WidgetCollectionManager.class) {
                if (instance == null) {
                    instance = new WidgetCollectionManager();
                }
            }
        }
        return instance;
    }

    public WidgetCollectionBean getWidgetByName(String name) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                return new WidgetCollectionBean(bean.name, ProjectDataParser.parseViewBeans(widgetGson, bean.data));
            }
        }
        return null;
    }

    public void renameWidget(String oldName, String newName, boolean save) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(oldName)) {
                bean.name = newName;
                break;
            }
        }
        if (save) saveCollections();
    }

    public void addWidget(String name, ArrayList<ViewBean> widgets, boolean save) throws CompileException {
        if (collections == null) initialize();
        if (widgetGson == null) initializeGson();
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                throw new CompileException("duplicate_name");
            }
        }
        StringBuilder contentBuilder = new StringBuilder();
        for (ViewBean widget : widgets) {
            contentBuilder.append(widgetGson.toJson(widget)).append("\n");
        }
        collections.add(new CollectionBean(name, contentBuilder.toString()));
        if (save) saveCollections();
    }

    public void removeWidget(String name, boolean save) {
        collections.removeIf(bean -> bean.name.equals(name));
        if (save) saveCollections();
    }

    public void initializePaths() {
        String basePath = SketchwarePaths.getCollectionPath() + File.separator + "widget" + File.separator;
        collectionFilePath = basePath + "list";
        dataDirPath = basePath + "data";
    }

    @Override
    public void loadCollections() {
        collections = new ArrayList<>();
        try {
            if (fileUtil.exists(collectionFilePath)) {
                String content = fileUtil.readFile(collectionFilePath);
                try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;
                        CollectionBean bean = gson.fromJson(line, CollectionBean.class);
                        collections.add(bean);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("WidgetCollectionManager", "Failed to load collections", e);
        }
    }

    public ArrayList<WidgetCollectionBean> getWidgets() {
        if (collections == null) initialize();
        if (widgetGson == null) initializeGson();
        ArrayList<WidgetCollectionBean> result = new ArrayList<>();
        for (CollectionBean bean : collections) {
            result.add(new WidgetCollectionBean(bean.name, ProjectDataParser.parseViewBeans(widgetGson, bean.data)));
        }
        return result;
    }

    public ArrayList<String> getWidgetNames() {
        if (collections == null) initialize();
        ArrayList<String> names = new ArrayList<>();
        for (CollectionBean bean : collections) {
            names.add(bean.name);
        }
        return names;
    }

    public final void initializeGson() {
        widgetGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
