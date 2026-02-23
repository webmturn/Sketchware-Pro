package a.a.a;

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
  
  public BlockHistoryManager(String paramString) {
    this.scId = paramString;
    this.historyMap = new HashMap<String, ArrayList<HistoryBlockBean>>();
    this.positionMap = new HashMap<String, Integer>();
  }
  
  public static String buildKey(String paramString1, String paramString2, String paramString3) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(paramString1);
    stringBuilder.append("_");
    stringBuilder.append(paramString2);
    stringBuilder.append("_");
    stringBuilder.append(paramString3);
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
  
  public static BlockHistoryManager getInstance(String paramString) {
    if (instance == null) {
      synchronized (BlockHistoryManager.class) {
        if (instance == null || !instance.scId.equals(paramString)) {
          instance = new BlockHistoryManager(paramString);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String paramString) {
    if (!this.positionMap.containsKey(paramString))
      return; 
    ArrayList arrayList = this.historyMap.get(paramString);
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    if (arrayList == null)
      return; 
    for (int j = arrayList.size(); j > i; j--)
      arrayList.remove(j - 1); 
  }
  
  public void recordAdd(String paramString, BlockBean paramBlockBean1, int paramInt1, int paramInt2, BlockBean paramBlockBean2, BlockBean paramBlockBean3) {
    ArrayList<BlockBean> arrayList = new ArrayList<>();
    arrayList.add(paramBlockBean1);
    recordAddMultiple(paramString, arrayList, paramInt1, paramInt2, paramBlockBean2, paramBlockBean3);
  }
  
  public void recordUpdate(String paramString, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    if (paramBlockBean1.isEqual(paramBlockBean2))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(paramBlockBean1, paramBlockBean2);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyBlockBean);
  }
  
  public final void addHistoryEntry(String paramString, HistoryBlockBean paramHistoryBlockBean) {
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    ArrayList<HistoryBlockBean> arrayList = this.historyMap.get(paramString);
    arrayList.add(paramHistoryBlockBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      incrementPosition(paramString);
    } 
  }
  
  public void recordAddMultiple(String paramString, ArrayList<BlockBean> paramArrayList, int paramInt1, int paramInt2, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(paramArrayList, paramInt1, paramInt2, paramBlockBean1, paramBlockBean2);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyBlockBean);
  }
  
  public void recordMove(String paramString, ArrayList<BlockBean> paramArrayList1, ArrayList<BlockBean> paramArrayList2, int paramInt1, int paramInt2, int paramInt3, int paramInt4, BlockBean paramBlockBean1, BlockBean paramBlockBean2, BlockBean paramBlockBean3, BlockBean paramBlockBean4) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(paramArrayList1, paramArrayList2, paramInt1, paramInt2, paramInt3, paramInt4, paramBlockBean1, paramBlockBean2, paramBlockBean3, paramBlockBean4);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyBlockBean);
  }
  
  public void removeHistory(String paramString) {
    if (this.historyMap.containsKey(paramString)) {
      this.historyMap.remove(paramString);
      this.positionMap.remove(paramString);
    } 
  }
  
  public void recordRemove(String paramString, ArrayList<BlockBean> paramArrayList, int paramInt1, int paramInt2, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(paramArrayList, paramInt1, paramInt2, paramBlockBean1, paramBlockBean2);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyBlockBean);
  }
  
  public final void decrementPosition(String paramString) {
    if (!this.positionMap.containsKey(paramString))
      initHistory(paramString); 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    if (i == 0)
      return; 
    this.positionMap.put(paramString, Integer.valueOf(i - 1));
  }
  
  public final void incrementPosition(String paramString) {
    if (!this.positionMap.containsKey(paramString))
      initHistory(paramString); 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    this.positionMap.put(paramString, Integer.valueOf(i + 1));
  }
  
  public void initHistory(String paramString) {
    this.historyMap.put(paramString, new ArrayList<HistoryBlockBean>());
    this.positionMap.put(paramString, Integer.valueOf(0));
  }
  
  public boolean canRedo(String paramString) {
    return !this.positionMap.containsKey(paramString) ? false : ((((Integer)this.positionMap.get(paramString)).intValue() < ((ArrayList)this.historyMap.get(paramString)).size()));
  }
  
  public boolean canUndo(String paramString) {
    return !this.positionMap.containsKey(paramString) ? false : ((((Integer)this.positionMap.get(paramString)).intValue() > 0));
  }
  
  public HistoryBlockBean redo(String paramString) {
    if (!canRedo(paramString))
      return null; 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    incrementPosition(paramString);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(paramString)).get(i - 1 + 1)).clone();
  }
  
  public HistoryBlockBean undo(String paramString) {
    if (!canUndo(paramString))
      return null; 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    decrementPosition(paramString);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.historyMap.get(paramString)).get(i - 1)).clone();
  }
}
