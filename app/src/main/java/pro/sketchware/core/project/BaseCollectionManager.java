package pro.sketchware.core.project;

import pro.sketchware.core.EncryptedFileUtil;

import android.util.Log;

import com.besome.sketch.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public abstract class BaseCollectionManager {
    protected String collectionFilePath;
    protected String dataDirPath;
    protected EncryptedFileUtil fileUtil;
    protected Gson gson;
    protected ArrayList<CollectionBean> collections;

    public BaseCollectionManager() {
        initialize();
    }

    public void initialize() {
        initializePaths();
        fileUtil = new EncryptedFileUtil();
        gson = new GsonBuilder().create();
        loadCollections();
    }

    public abstract void initializePaths();

    public void loadCollections() {
        collections = new ArrayList<>();
        try {
            String content = fileUtil.readFile(collectionFilePath);
            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    CollectionBean collection = gson.fromJson(line, CollectionBean.class);
                    String collectionDataPath = dataDirPath + File.separator + collection.data;
                    if (fileUtil.exists(collectionDataPath)) {
                        collections.add(collection);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("BaseCollectionManager", "Failed to load collections", e);
        }
    }

    public void clearCollections() {
        if (collections != null) {
            collections.clear();
            collections = null;
        }
    }

    public void saveCollections() {
        if (collections == null) return;
        StringBuilder contentBuilder = new StringBuilder(1024);
        for (CollectionBean collection : collections) {
            contentBuilder.append(gson.toJson(collection)).append("\n");
        }
        try {
            fileUtil.deleteFileByPath(collectionFilePath);
            fileUtil.writeText(collectionFilePath, contentBuilder.toString());
        } catch (Exception e) {
            Log.e("BaseCollectionManager", "Failed to save collections", e);
        }
    }
}
