package pro.sketchware.core;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ViewHistoryManager {
  public static ViewHistoryManager instance;
  
  public Map<String, Integer> positionMap;
  
  public Map<String, ArrayList<HistoryViewBean>> historyMap;
  
  public String scId;
  
  public ViewHistoryManager(String key) {
    this.scId = key;
    this.historyMap = new HashMap<String, ArrayList<HistoryViewBean>>();
    this.positionMap = new HashMap<String, Integer>();
  }
  
  public static void clearInstance() {
    ViewHistoryManager cC1 = instance;
    if (cC1 != null) {
      cC1.scId = "";
      cC1.historyMap = null;
      cC1.positionMap = null;
    } 
    instance = null;
  }
  
  public static ViewHistoryManager getInstance(String key) {
    if (instance == null) {
      synchronized (ViewHistoryManager.class) {
        if (instance == null || !instance.scId.equals(key)) {
          instance = new ViewHistoryManager(key);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String key) {
    if (!this.positionMap.containsKey(key))
      return; 
    ArrayList historyEntries = this.historyMap.get(key);
    int position = ((Integer)this.positionMap.get(key)).intValue();
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  public final void addHistoryEntry(String key, HistoryViewBean historyViewBean) {
    if (!this.historyMap.containsKey(key))
      initHistory(key); 
    ArrayList<HistoryViewBean> entries = this.historyMap.get(key);
    entries.add(historyViewBean);
    if (entries.size() > 50) {
      entries.remove(0);
    } else {
      incrementPosition(key);
    } 
  }
  
  public void recordAdd(String key, ViewBean viewBean) {
    ArrayList<ViewBean> views = new ArrayList<>();
    views.add(viewBean);
    recordAddMultiple(key, views);
  }
  
  public void recordUpdate(String key, ViewBean prevViewData, ViewBean currentViewData) {
    if (prevViewData.isEqual(currentViewData))
      return; 
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionUpdate(prevViewData, currentViewData);
    if (!this.historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public void recordAddMultiple(String key, ArrayList<ViewBean> list) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionAdd(list);
    if (!this.historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public final void decrementPosition(String key) {
    if (!this.positionMap.containsKey(key))
      initHistory(key); 
    int position = ((Integer)this.positionMap.get(key)).intValue();
    if (position == 0)
      return; 
    this.positionMap.put(key, Integer.valueOf(position - 1));
  }
  
  public void recordMove(String key, ViewBean viewBean) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionMove(viewBean);
    if (!this.historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public void recordRemove(String key, ArrayList<ViewBean> list) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionRemove(list);
    if (!this.historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public final void incrementPosition(String key) {
    if (!this.positionMap.containsKey(key))
      initHistory(key); 
    int position = ((Integer)this.positionMap.get(key)).intValue();
    this.positionMap.put(key, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String key) {
    this.historyMap.put(key, new ArrayList<HistoryViewBean>());
    this.positionMap.put(key, Integer.valueOf(0));
  }
  
  public boolean canRedo(String key) {
    return !this.positionMap.containsKey(key) ? false : ((((Integer)this.positionMap.get(key)).intValue() < ((ArrayList)this.historyMap.get(key)).size()));
  }
  
  public boolean canUndo(String key) {
    return !this.positionMap.containsKey(key) ? false : ((((Integer)this.positionMap.get(key)).intValue() > 0));
  }
  
  public HistoryViewBean redo(String key) {
    if (!canRedo(key))
      return null; 
    int position = ((Integer)this.positionMap.get(key)).intValue();
    incrementPosition(key);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.historyMap.get(key)).get(position - 1 + 1)).clone();
  }
  
  public HistoryViewBean undo(String key) {
    if (!canUndo(key))
      return null; 
    int position = ((Integer)this.positionMap.get(key)).intValue();
    decrementPosition(key);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.historyMap.get(key)).get(position - 1)).clone();
  }
}
