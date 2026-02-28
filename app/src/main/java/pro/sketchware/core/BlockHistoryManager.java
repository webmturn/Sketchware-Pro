package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.HistoryBlockBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockHistoryManager {
  public static BlockHistoryManager instance;
  
  public Map<String, Integer> positionMap;
  
  public Map<String, ArrayList<HistoryBlockBean>> historyMap;
  
  public String scId;
  
  public BlockHistoryManager(String scId) {
    this.scId = scId;
    historyMap = new HashMap<>();
    positionMap = new HashMap<>();
  }
  
  public static String buildKey(String key, String value, String extra) {
    return key + "_" + value + "_" + extra;
  }
  
  public static void clearInstance() {
    BlockHistoryManager currentInstance = instance;
    if (currentInstance != null) {
      currentInstance.scId = "";
      currentInstance.historyMap = null;
      currentInstance.positionMap = null;
    } 
    instance = null;
  }
  
  public static BlockHistoryManager getInstance(String scId) {
    if (instance == null) {
      synchronized (BlockHistoryManager.class) {
        if (instance == null || !instance.scId.equals(scId)) {
          instance = new BlockHistoryManager(scId);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      return; 
    ArrayList historyEntries = historyMap.get(historyKey);
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  public void recordAdd(String historyKey, BlockBean addedBlock, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    ArrayList<BlockBean> blockList = new ArrayList<>();
    blockList.add(addedBlock);
    recordAddMultiple(historyKey, blockList, currentX, currentY, prevParentData, currentParentData);
  }
  
  public void recordUpdate(String historyKey, BlockBean prevUpdateData, BlockBean currentUpdateData) {
    if (prevUpdateData.isEqual(currentUpdateData))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(prevUpdateData, currentUpdateData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void addHistoryEntry(String historyKey, HistoryBlockBean historyBlockBean) {
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    ArrayList<HistoryBlockBean> historyEntries = historyMap.get(historyKey);
    historyEntries.add(historyBlockBean);
    if (historyEntries.size() > 50) {
      historyEntries.remove(0);
    } else {
      incrementPosition(historyKey);
    } 
  }
  
  public void recordAddMultiple(String historyKey, ArrayList<BlockBean> addedData, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(addedData, currentX, currentY, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public void recordMove(String historyKey, ArrayList<BlockBean> beforeMove, ArrayList<BlockBean> afterMove, int prevX, int prevY, int currentX, int currentY, BlockBean prevOriginalParent, BlockBean currentOriginalParent, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(beforeMove, afterMove, prevX, prevY, currentX, currentY, prevOriginalParent, currentOriginalParent, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public void removeHistory(String historyKey) {
    if (historyMap.containsKey(historyKey)) {
      historyMap.remove(historyKey);
      positionMap.remove(historyKey);
    } 
  }
  
  public void recordRemove(String historyKey, ArrayList<BlockBean> removedData, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(removedData, currentX, currentY, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void decrementPosition(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    if (position == 0)
      return; 
    positionMap.put(historyKey, Integer.valueOf(position - 1));
  }
  
  public final void incrementPosition(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    positionMap.put(historyKey, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String historyKey) {
    historyMap.put(historyKey, new ArrayList<>());
    positionMap.put(historyKey, Integer.valueOf(0));
  }
  
  public boolean canRedo(String historyKey) {
    return !positionMap.containsKey(historyKey) ? false : ((((Integer)positionMap.get(historyKey)).intValue() < ((ArrayList)historyMap.get(historyKey)).size()));
  }
  
  public boolean canUndo(String historyKey) {
    return !positionMap.containsKey(historyKey) ? false : ((((Integer)positionMap.get(historyKey)).intValue() > 0));
  }
  
  public HistoryBlockBean redo(String historyKey) {
    if (!canRedo(historyKey))
      return null; 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    incrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)historyMap.get(historyKey)).get(position - 1 + 1)).clone();
  }
  
  public HistoryBlockBean undo(String historyKey) {
    if (!canUndo(historyKey))
      return null; 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    decrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)historyMap.get(historyKey)).get(position - 1)).clone();
  }
}
