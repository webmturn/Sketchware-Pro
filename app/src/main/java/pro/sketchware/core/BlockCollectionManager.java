package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.BlockCollectionBean;
import com.besome.sketch.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.ArrayList;

public class BlockCollectionManager extends BaseCollectionManager {
    private static volatile BlockCollectionManager instance;
    private Gson blockGson;

    public BlockCollectionManager() {
        initializeGson();
    }

    public static BlockCollectionManager getInstance() {
        if (instance == null) {
            synchronized (BlockCollectionManager.class) {
                if (instance == null) {
                    instance = new BlockCollectionManager();
                }
            }
        }
        return instance;
    }

    public BlockCollectionBean getBlockByName(String name) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                return new BlockCollectionBean(bean.name, ProjectDataParser.parseBlockBeans(blockGson, bean.data));
            }
        }
        return null;
    }

    public void renameBlock(String oldName, String newName, boolean save) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(oldName)) {
                bean.name = newName;
                break;
            }
        }
        if (save) saveCollections();
    }

    public void addBlock(String name, ArrayList<BlockBean> blocks, boolean save) throws CompileException {
        if (collections == null) initialize();
        if (blockGson == null) initializeGson();
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                throw new CompileException("duplicate_name");
            }
        }
        StringBuilder contentBuilder = new StringBuilder();
        for (BlockBean block : blocks) {
            contentBuilder.append(blockGson.toJson(block)).append("\n");
        }
        collections.add(new CollectionBean(name, contentBuilder.toString()));
        if (save) saveCollections();
    }

    public void removeBlock(String name, boolean save) {
        collections.removeIf(bean -> bean.name.equals(name));
        if (save) saveCollections();
    }

    public void initializePaths() {
        String basePath = SketchwarePaths.getCollectionPath() + File.separator + "block" + File.separator;
        collectionFilePath = basePath + "list";
        dataDirPath = basePath + "data";
    }

    public ArrayList<BlockCollectionBean> getBlocks() {
        if (collections == null) initialize();
        if (blockGson == null) initializeGson();
        ArrayList<BlockCollectionBean> result = new ArrayList<>();
        for (CollectionBean bean : collections) {
            result.add(new BlockCollectionBean(bean.name, ProjectDataParser.parseBlockBeans(blockGson, bean.data)));
        }
        return result;
    }

    public ArrayList<String> getBlockNames() {
        if (collections == null) initialize();
        ArrayList<String> names = new ArrayList<>();
        for (CollectionBean bean : collections) {
            names.add(bean.name);
        }
        return names;
    }

    public final void initializeGson() {
        blockGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
