package mod.hey.studios.project.custom_blocks;

import android.content.Context;
import android.os.Environment;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import a.a.a.ProjectDataStore;
import a.a.a.ProjectFileManager;
import a.a.a.ProjectDataManager;
import a.a.a.BlockColorMapper;
import dev.aldi.sayuti.block.ExtraBlockFile;
import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class CustomBlocksManager {
    private final Context context;
    final String sc_id;
    ArrayList<BlockBean> blocks;
    ArrayList<ExtraBlockInfo> custom_blocks;

    public CustomBlocksManager(Context context, String sc_id) {
        this.context = context;
        this.sc_id = sc_id;

        load();
    }

    public ArrayList<BlockBean> getUsedBlocks() {
        ArrayList<BlockBean> filteredCustomBlocks = new ArrayList<>();
        for (BlockBean bean : blocks) {
            if (!isBuildInBlock(bean.opCode)) {
                filteredCustomBlocks.add(bean);
            }
        }
        return filteredCustomBlocks;
    }

    private boolean isBuildInBlock(String blockName) {
        if (ExtraBlockFile.buildInBlocks.isEmpty()) {
            BlockLoader.refresh();
        }
        for (HashMap<String, Object> block : ExtraBlockFile.buildInBlocks) {
            if (Objects.requireNonNull(block.get("name")).toString().equals(blockName)) {
                return true;
            }
        }
        return false;
    }

    public ExtraBlockInfo getExtraBlockInfo(String name) {
        return getExtraBlockInfoByName(name).orElse(null);
    }

    public boolean contains(String name) {
        return getExtraBlockInfoByName(name).isPresent();
    }

    private Optional<ExtraBlockInfo> getExtraBlockInfoByName(String name) {
        if (custom_blocks != null && !custom_blocks.isEmpty()) {
            for (ExtraBlockInfo info : custom_blocks) {
                if (info.getName().equals(name)) {
                    return Optional.of(info);
                }
            }
        }
        return Optional.empty();
    }

    private void load() {
        blocks = new ArrayList<>();

        ArrayList<String> usedBlocks = new ArrayList<>();

        ProjectFileManager hc = ProjectDataManager.getFileManager(sc_id);
        ProjectDataStore ec = ProjectDataManager.getProjectDataManager(sc_id);

        for (ProjectFileBean bean : hc.b()) {
            for (Map.Entry<String, ArrayList<BlockBean>> entry : ec.b(bean.getJavaName()).entrySet()) {
                for (BlockBean block : entry.getValue()) {
                    if (!(block.opCode.equals("definedFunc")
                            || block.opCode.equals("getVar")
                            || block.opCode.equals("getArg"))) {
                        if (BlockColorMapper.a(context, block.opCode, block.type) == 0xff8a55d7) {
                            if (!usedBlocks.contains(block.opCode)) {
                                usedBlocks.add(block.opCode);

                                blocks.add(block);
                            }
                        }
                    }
                }
            }
        }

        File customBlocksConfig = new File(Environment.getExternalStorageDirectory(),
                ".sketchware/data/" + sc_id + "/custom_blocks");
        if (customBlocksConfig.exists()) {
            try {
                custom_blocks = new Gson().fromJson(
                        FileUtil.readFile(customBlocksConfig.getAbsolutePath()),
                        new TypeToken<ArrayList<ExtraBlockInfo>>() {
                        }.getType());
            } catch (JsonSyntaxException e) {
                SketchwareUtil.toastError(String.format(Helper.getResString(R.string.error_get_custom_blocks), e.getMessage()));
            }
        }

    }

    public String getCustomBlockCode(String opCode) {
        try {
            for (ExtraBlockInfo info : custom_blocks) {
                if (info.getName().equals(opCode)) {
                    return info.getCode();
                }
            }
        } catch (NullPointerException e) {
            android.util.Log.e("CustomBlocksManager", "Failed to get code for custom block: " + opCode, e);
        }
        return "";
    }

    public String getCustomBlockSpec2(String opCode) {
        try {
            for (ExtraBlockInfo info : custom_blocks) {
                if (info.getName().equals(opCode)) {
                    return info.getSpec2();
                }
            }
        } catch (NullPointerException e) {
            android.util.Log.e("CustomBlocksManager", "Failed to get spec2 for custom block: " + opCode, e);
        }
        return "";
    }


    /*public String getCustomBlocksJsonPath() {
        return new File(
            Environment.getExternalStorageDirectory(),
            ".sketchware/data/" + sc_id + "/custom_blocks")
            .getAbsolutePath();
    }

    public void writeCustomBlocksJson() {
        ArrayList<ExtraBlockInfo> blockss = new ArrayList<>();

        for (BlockBean bean : getUsedBlocks()) {
            blockss.add(BlockLoader.getBlockInfo(bean.opCode));
        }

        if (blockss.size() != 0) {
            FileUtil.writeFile(getCustomBlocksJsonPath(), new Gson().toJson(blockss));
        }
    }*/
}


/*package mod.hey.studios.project.custom_blocks;

import java.util.ArrayList;
import com.besome.sketch.beans.ProjectFileBean;
import java.util.Map;
import com.besome.sketch.beans.BlockBean;
import a.a.a.BlockColorMapper;
import a.a.a.ProjectFileManager;
import a.a.a.ProjectDataStore;
import a.a.a.ProjectDataManager;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import pro.sketchware.utility.FileUtil;
import com.google.gson.Gson;
import java.io.File;
import android.os.Environment;

public class CustomBlocksManager {
    String sc_id;
    ArrayList<BlockBean> blocks;

    public CustomBlocksManager(String sc_id) {
        this.sc_id = sc_id;

        load();
    }

    public ArrayList<BlockBean> getUsedBlocks() {
        return blocks;
    }

    private void load() {

        blocks = new ArrayList<>();

        ArrayList<String> usedBlocks = new ArrayList<>();

        ProjectFileManager hc = ProjectDataManager.getFileManager(sc_id);
        ProjectDataStore ec = ProjectDataManager.getProjectDataManager(sc_id);

        for (ProjectFileBean bean : hc.b())
        {

            for (Map.Entry<String, ArrayList<BlockBean>> entry : ec.b(bean.getJavaName()).entrySet())
            {

                for (BlockBean block : entry.getValue())
                {

                    if (!(block.opCode.equals("definedFunc") || block.opCode.equals("getVar") || block.opCode.equals("getArg")))
                    {
                        if (BlockColorMapper.a(block.opCode, block.type) == -7711273)
                        {
                            if (!usedBlocks.contains(block.opCode))
                            {
                                usedBlocks.add(block.opCode);

                                blocks.add(block);
                            }
                        }
                    }

                }

            }

        }
    }
    
    
    public String getCustomBlocksJsonPath() {
        return new File(Environment.getExternalStorageDirectory(), ".sketchware/data/"+sc_id+"/custom_blocks").getAbsolutePath();
    }
    
    
    public void writeCustomBlocksJson() {
        ArrayList<ExtraBlockInfo> blockss = new ArrayList<>();

        for(BlockBean bean : getUsedBlocks()) {
            blockss.add(BlockLoader.getBlockInfo(bean.opCode));
        }
        
        if(blockss.size() != 0) {
            FileUtil.writeFile(getCustomBlocksJsonPath(), new Gson().toJson(blockss));
        }
    }
    
    
}
*/
