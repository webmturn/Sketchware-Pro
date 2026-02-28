package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.CollectionBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
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

    public MoreBlockCollectionBean getMoreBlockByName(String name) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                return new MoreBlockCollectionBean(bean.name, bean.reserved1, ProjectDataParser.parseBlockBeans(moreBlockGson, bean.data));
            }
        }
        return null;
    }

    public void addMoreBlock(String name, String spec, ArrayList<BlockBean> blocks, boolean save) throws CompileException {
        if (collections == null) initialize();
        if (moreBlockGson == null) initializeGson();
        for (CollectionBean bean : collections) {
            if (bean.name.equals(name)) {
                throw new CompileException("duplicate_name");
            }
        }
        StringBuilder contentBuilder = new StringBuilder();
        for (BlockBean block : blocks) {
            contentBuilder.append(moreBlockGson.toJson(block)).append("\n");
        }
        collections.add(new CollectionBean(name, contentBuilder.toString(), spec));
        if (save) saveCollections();
    }

    public void renameMoreBlock(String oldName, String newName, boolean save) {
        for (CollectionBean bean : collections) {
            if (bean.name.equals(oldName)) {
                bean.name = newName;
                break;
            }
        }
        if (save) saveCollections();
    }

    public void removeMoreBlock(String name, boolean save) {
        collections.removeIf(bean -> bean.name.equals(name));
        if (save) saveCollections();
    }

    public void initializePaths() {
        collectionFilePath = SketchwarePaths.getCollectionPath() + File.separator + "more_block" + File.separator + "list";
    }

    public ArrayList<MoreBlockCollectionBean> getMoreBlocks() {
        if (collections == null) initialize();
        if (moreBlockGson == null) initializeGson();
        ArrayList<MoreBlockCollectionBean> result = new ArrayList<>();
        for (CollectionBean bean : collections) {
            result.add(new MoreBlockCollectionBean(bean.name, bean.reserved1, ProjectDataParser.parseBlockBeans(moreBlockGson, bean.data)));
        }
        return result;
    }

    public ArrayList<String> getMoreBlockNames() {
        if (collections == null) initialize();
        ArrayList<String> names = new ArrayList<>();
        for (CollectionBean bean : collections) {
            names.add(bean.name);
        }
        return names;
    }

    public final void initializeGson() {
        moreBlockGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }
}
