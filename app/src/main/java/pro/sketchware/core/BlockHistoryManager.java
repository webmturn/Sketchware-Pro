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
    this.historyMap = new HashMap<String, ArrayList<HistoryBlockBean>>();
    this.positionMap = new HashMap<String, Integer>();
  }
  
  public static String buildKey(String key, String value, String extra) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(key);
    stringBuilder.append("_");
    stringBuilder.append(value);
    stringBuilder.append("_");
    stringBuilder.append(extra);
    return stringBuilder.toString();
  }
  
  public static void clearInstance() {
    BlockHistoryManager bC1 = instance;
    if (bC1 != null) {
      bC1.scId = "";
      bC1.historyMap = null;
      bC1.positionMap = null;
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
    if (!this.positionMap.containsKey(historyKey))
      return; 
    ArrayList historyEntries = this.historyMap.get(historyKey);
    int i = ((Integer)this.positionMap.get(historyKey)).intValue();
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > i; j--)
      historyEntries.remove(j - 1); 
  }
  
  public void recordAdd(String historyKey, BlockBean blockBean1, int start, int end, BlockBean blockBean2, BlockBean blockBean3) {
    ArrayList<BlockBean> arrayList = new ArrayList<>();
    arrayList.add(blockBean1);
    recordAddMultiple(historyKey, arrayList, start, end, blockBean2, blockBean3);
  }
  
  public void recordUpdate(String historyKey, BlockBean blockBean1, BlockBean blockBean2) {
    if (blockBean1.isEqual(blockBean2))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(blockBean1, blockBean2);
    if (!this.historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void addHistoryEntry(String historyKey, HistoryBlockBean historyBlockBean) {
    if (!this.historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    ArrayList<HistoryBlockBean> arrayList = this.historyMap.get(historyKey);
    arrayList.add(historyBlockBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      incrementPosition(historyKey);
    } 
  }
  
  public void recordAddMultiple(String historyKey, ArrayList<BlockBean> list, int start, int end, BlockBean blockBean1, BlockBean blockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(list, start, end, blockBean1, blockBean2);
    if (!this.historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public void recordMove(String historyKey, ArrayList<BlockBean> list1, ArrayList<BlockBean> list2, int start, int end, int width, int height, BlockBean blockBean1, BlockBean blockBean2, BlockBean blockBean3, BlockBean blockBean4) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(list1, list2, start, end, width, height, blockBean1, blockBean2, blockBean3, blockBean4);
    if (!this.historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public void removeHistory(String historyKey) {
    if (this.historyMap.containsKey(historyKey)) {
      this.historyMap.remove(historyKey);
      this.positionMap.remove(historyKey);
    } 
  }
  
  public void recordRemove(String historyKey, ArrayList<BlockBean> list, int start, int end, BlockBean blockBean1, BlockBean blockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(list, start, end, blockBean1, blockBean2);
    if (!this.historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void decrementPosition(String historyKey) {
    if (!this.positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int i = ((Integer)this.positionMap.get(historyKey)).intValue();
    if (i == 0)
      return; 
    this.positionMap.put(historyKey, Integer.valueOf(i - 1));
  }
  
  public final void incrementPosition(String historyKey) {
    if (!this.positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int i = ((Integer)this.positionMap.get(historyKey)).intValue();
    this.positionMap.put(historyKey, Integer.valueOf(i + 1));
  }
  
  public void initHistory(String historyKey) {
    this.historyMap.put(historyKey, new ArrayList<HistoryBlockBean>());
    this.positionMap.put(historyKey, Integer.valueOf(0));
  }
  
  public boolean canRedo(String historyKey) {
    return !this.positionMap.containsKey(historyKey) ? false : ((((Integer)this.positionMap.get(historyKey)).intValue() < ((ArrayList)this.historyMap.get(historyKey)).size()));
  }
  
  public boolean canUndo(String historyKey) {
    return !this.positionMap.containsKey(historyKey) ? false : ((((Integer)this.positionMap.get(historyKey)).intValue() > 0));
  }
  
  public HistoryBlockBean redo(String historyKey) {
    if (!canRedo(historyKey))
      return null; 
    int i = ((Integer)this.positionMap.get(historyKey)).intValue();
    incrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(historyKey)).get(i - 1 + 1)).clone();
  }
  
  public HistoryBlockBean undo(String historyKey) {
    if (!canUndo(historyKey))
      return null; 
    int i = ((Integer)this.positionMap.get(historyKey)).intValue();
    decrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(historyKey)).get(i - 1)).clone();
  }
}
