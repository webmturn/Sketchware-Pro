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
  
  public BlockHistoryManager(String str) {
    this.scId = str;
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
  
  public static BlockHistoryManager getInstance(String str) {
    if (instance == null) {
      synchronized (BlockHistoryManager.class) {
        if (instance == null || !instance.scId.equals(str)) {
          instance = new BlockHistoryManager(str);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String str) {
    if (!this.positionMap.containsKey(str))
      return; 
    ArrayList arrayList = this.historyMap.get(str);
    int i = ((Integer)this.positionMap.get(str)).intValue();
    if (arrayList == null)
      return; 
    for (int j = arrayList.size(); j > i; j--)
      arrayList.remove(j - 1); 
  }
  
  public void recordAdd(String str, BlockBean blockBean1, int start, int end, BlockBean blockBean2, BlockBean blockBean3) {
    ArrayList<BlockBean> arrayList = new ArrayList<>();
    arrayList.add(blockBean1);
    recordAddMultiple(str, arrayList, start, end, blockBean2, blockBean3);
  }
  
  public void recordUpdate(String str, BlockBean blockBean1, BlockBean blockBean2) {
    if (blockBean1.isEqual(blockBean2))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(blockBean1, blockBean2);
    if (!this.historyMap.containsKey(str))
      initHistory(str); 
    trimFutureHistory(str);
    addHistoryEntry(str, historyBlockBean);
  }
  
  public final void addHistoryEntry(String str, HistoryBlockBean historyBlockBean) {
    if (!this.historyMap.containsKey(str))
      initHistory(str); 
    ArrayList<HistoryBlockBean> arrayList = this.historyMap.get(str);
    arrayList.add(historyBlockBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      incrementPosition(str);
    } 
  }
  
  public void recordAddMultiple(String str, ArrayList<BlockBean> list, int start, int end, BlockBean blockBean1, BlockBean blockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(list, start, end, blockBean1, blockBean2);
    if (!this.historyMap.containsKey(str))
      initHistory(str); 
    trimFutureHistory(str);
    addHistoryEntry(str, historyBlockBean);
  }
  
  public void recordMove(String str, ArrayList<BlockBean> list1, ArrayList<BlockBean> list2, int start, int end, int width, int height, BlockBean blockBean1, BlockBean blockBean2, BlockBean blockBean3, BlockBean blockBean4) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(list1, list2, start, end, width, height, blockBean1, blockBean2, blockBean3, blockBean4);
    if (!this.historyMap.containsKey(str))
      initHistory(str); 
    trimFutureHistory(str);
    addHistoryEntry(str, historyBlockBean);
  }
  
  public void removeHistory(String str) {
    if (this.historyMap.containsKey(str)) {
      this.historyMap.remove(str);
      this.positionMap.remove(str);
    } 
  }
  
  public void recordRemove(String str, ArrayList<BlockBean> list, int start, int end, BlockBean blockBean1, BlockBean blockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(list, start, end, blockBean1, blockBean2);
    if (!this.historyMap.containsKey(str))
      initHistory(str); 
    trimFutureHistory(str);
    addHistoryEntry(str, historyBlockBean);
  }
  
  public final void decrementPosition(String str) {
    if (!this.positionMap.containsKey(str))
      initHistory(str); 
    int i = ((Integer)this.positionMap.get(str)).intValue();
    if (i == 0)
      return; 
    this.positionMap.put(str, Integer.valueOf(i - 1));
  }
  
  public final void incrementPosition(String str) {
    if (!this.positionMap.containsKey(str))
      initHistory(str); 
    int i = ((Integer)this.positionMap.get(str)).intValue();
    this.positionMap.put(str, Integer.valueOf(i + 1));
  }
  
  public void initHistory(String str) {
    this.historyMap.put(str, new ArrayList<HistoryBlockBean>());
    this.positionMap.put(str, Integer.valueOf(0));
  }
  
  public boolean canRedo(String str) {
    return !this.positionMap.containsKey(str) ? false : ((((Integer)this.positionMap.get(str)).intValue() < ((ArrayList)this.historyMap.get(str)).size()));
  }
  
  public boolean canUndo(String str) {
    return !this.positionMap.containsKey(str) ? false : ((((Integer)this.positionMap.get(str)).intValue() > 0));
  }
  
  public HistoryBlockBean redo(String str) {
    if (!canRedo(str))
      return null; 
    int i = ((Integer)this.positionMap.get(str)).intValue();
    incrementPosition(str);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(str)).get(i - 1 + 1)).clone();
  }
  
  public HistoryBlockBean undo(String str) {
    if (!canUndo(str))
      return null; 
    int i = ((Integer)this.positionMap.get(str)).intValue();
    decrementPosition(str);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(str)).get(i - 1)).clone();
  }
}
