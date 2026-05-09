package pro.sketchware.core.project;

import pro.sketchware.core.CompileException;
import pro.sketchware.core.SketchwarePaths;

import android.util.Log;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class MoreBlockCollectionManager extends BaseCollectionManager {
    private static volatile MoreBlockCollectionManager instance;
    private Gson moreBlockGson;

    public MoreBlockCollectionManager() {
        initializeGson();
    }

    public static MoreBlockCollectionManager getInstance() {
        if (instance == null) {
            synchronized (MoreBlockCollectionManager.class) {
                if (instance == null) {
                    instance = new MoreBlockCollectionManager();
                }
            }
        }
        return instance;
    }

    public MoreBlockCollectionBean getMoreBlockByName(String moreBlockName) {
        for (CollectionBean collection : collections) {
            if (collection.name.equals(moreBlockName)) {
                return new MoreBlockCollectionBean(collection.name, collection.reserved1, ProjectDataParser.parseBlockBeans(moreBlockGson, collection.data));
            }
        }
        return null;
    }

    public void addMoreBlock(String moreBlockName, String moreBlockSpec, ArrayList<BlockBean> blocks, boolean save) throws CompileException {
        if (collections == null) initialize();
        if (moreBlockGson == null) initializeGson();
        for (CollectionBean collection : collections) {
            if (collection.name.equals(moreBlockName)) {
                throw new CompileException("duplicate_name");
            }
        }
        StringBuilder contentBuilder = new StringBuilder();
        for (BlockBean block : blocks) {
            contentBuilder.append(moreBlockGson.toJson(block)).append("\n");
        }
        collections.add(new CollectionBean(moreBlockName, contentBuilder.toString(), moreBlockSpec));
        if (save) saveCollections();
    }

    public void renameMoreBlock(String oldName, String newName, boolean save) {
        for (CollectionBean collection : collections) {
            if (collection.name.equals(oldName)) {
                collection.name = newName;
                break;
            }
        }
        if (save) saveCollections();
    }

    public void removeMoreBlock(String moreBlockName, boolean save) {
        collections.removeIf(collection -> collection.name.equals(moreBlockName));
        if (save) saveCollections();
    }

    public void initializePaths() {
        collectionFilePath = SketchwarePaths.getCollectionPath() + File.separator + "more_block" + File.separator + "list";
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
                        CollectionBean collection = gson.fromJson(line, CollectionBean.class);
                        collections.add(collection);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("MoreBlockCollectionManager", "Failed to load collections", e);
        }
    }

    public ArrayList<MoreBlockCollectionBean> getMoreBlocks() {
        if (collections == null) initialize();
        if (moreBlockGson == null) initializeGson();
        ArrayList<MoreBlockCollectionBean> result = new ArrayList<>();
        for (CollectionBean collection : collections) {
            result.add(new MoreBlockCollectionBean(collection.name, collection.reserved1, ProjectDataParser.parseBlockBeans(moreBlockGson, collection.data)));
        }
        return result;
    }

    public ArrayList<String> getMoreBlockNames() {
        if (collections == null) initialize();
        ArrayList<String> names = new ArrayList<>();
        for (CollectionBean collection : collections) {
            names.add(collection.name);
        }
        return names;
    }

    public final void initializeGson() {
        moreBlockGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
