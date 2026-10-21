package pro.sketchware.core.project;

import pro.sketchware.core.exception.CompileException;
import pro.sketchware.core.project.SketchwarePaths;

import android.util.Log;

import pro.sketchware.beans.BlockBean;
import pro.sketchware.beans.BlockCollectionBean;
import pro.sketchware.beans.CollectionBean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class BlockCollectionManager extends BaseCollectionManager {
    private static volatile BlockCollectionManager instance;
    private Gson blockGson;
    private long revision;

    public record BlocksSnapshot(long revision, List<BlockCollectionBean> blocks) {
        public BlocksSnapshot { blocks = List.copyOf(blocks); }
    }

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

    public synchronized BlockCollectionBean getBlockByName(String blockName) {
        for (CollectionBean collection : collections) {
            if (collection.name.equals(blockName)) {
                return new BlockCollectionBean(collection.name, ProjectDataParser.parseBlockBeans(blockGson, collection.data));
            }
        }
        return null;
    }

    public synchronized void renameBlock(String oldName, String newName, boolean save) {
        for (CollectionBean collection : collections) {
            if (collection.name.equals(oldName)) {
                collection.name = newName;
                revision++;
                break;
            }
        }
        if (save) saveCollections();
    }

    public synchronized void addBlock(String blockName, ArrayList<BlockBean> blocks, boolean save) throws CompileException {
        if (collections == null) initialize();
        if (blockGson == null) initializeGson();
        for (CollectionBean collection : collections) {
            if (collection.name.equals(blockName)) {
                throw new CompileException("duplicate_name");
            }
        }
        StringBuilder contentBuilder = new StringBuilder();
        for (BlockBean block : blocks) {
            contentBuilder.append(blockGson.toJson(block)).append("\n");
        }
        collections.add(new CollectionBean(blockName, contentBuilder.toString()));
        revision++;
        if (save) saveCollections();
    }

    public synchronized void removeBlock(String blockName, boolean save) {
        if (collections.removeIf(collection -> collection.name.equals(blockName))) revision++;
        if (save) saveCollections();
    }

    public void initializePaths() {
        String basePath = SketchwarePaths.getCollectionPath() + File.separator + "block" + File.separator;
        collectionFilePath = basePath + "list";
        dataDirPath = basePath + "data";
    }

    @Override
    public synchronized void loadCollections() {
        revision++;
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
            Log.e("BlockCollectionManager", "Failed to load collections", e);
        }
    }

    public ArrayList<BlockCollectionBean> getBlocks() {
        return new ArrayList<>(getBlocksSnapshot().blocks());
    }

    /** Copy immutable names/data under the lock; expensive block parsing must run outside it. */
    public BlocksSnapshot getBlocksSnapshot() {
        ArrayList<CollectionBean> sources = new ArrayList<>();
        Gson parser;
        long capturedRevision;
        synchronized (this) {
            if (collections == null) initialize();
            if (blockGson == null) initializeGson();
            for (CollectionBean collection : collections) {
                if (collection != null) sources.add(new CollectionBean(collection.name, collection.data));
            }
            parser = blockGson;
            capturedRevision = revision;
        }
        ArrayList<BlockCollectionBean> result = new ArrayList<>();
        for (CollectionBean collection : sources) {
            ArrayList<BlockBean> blocks = ProjectDataParser.parseBlockBeans(parser,
                    collection.data != null ? collection.data : "");
            blocks.removeIf(block -> block == null);
            result.add(new BlockCollectionBean(collection.name, blocks));
        }
        return new BlocksSnapshot(capturedRevision, result);
    }

    public synchronized boolean isCurrent(BlocksSnapshot snapshot) {
        return snapshot.revision() == revision;
    }

    @Override public synchronized void initialize() { super.initialize(); }

    @Override public synchronized void clearCollections() {
        super.clearCollections();
        revision++;
    }

    @Override public synchronized void saveCollections() { super.saveCollections(); }

    public synchronized ArrayList<String> getBlockNames() {
        if (collections == null) initialize();
        ArrayList<String> names = new ArrayList<>();
        for (CollectionBean collection : collections) {
            names.add(collection.name);
        }
        return names;
    }

    public final synchronized void initializeGson() {
        blockGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        revision++;
    }
}
