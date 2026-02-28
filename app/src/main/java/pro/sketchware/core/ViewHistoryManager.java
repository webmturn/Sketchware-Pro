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
    scId = key;
    historyMap = new HashMap<>();
    positionMap = new HashMap<>();
  }
  
  public static void clearInstance() {
    ViewHistoryManager currentInstance = instance;
    if (currentInstance != null) {
      currentInstance.scId = "";
      currentInstance.historyMap = null;
      currentInstance.positionMap = null;
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
    if (!positionMap.containsKey(key))
      return; 
    ArrayList historyEntries = historyMap.get(key);
    int position = ((Integer)positionMap.get(key)).intValue();
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  public final void addHistoryEntry(String key, HistoryViewBean historyViewBean) {
    if (!historyMap.containsKey(key))
      initHistory(key); 
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
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
    if (!historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public void recordAddMultiple(String key, ArrayList<ViewBean> list) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionAdd(list);
    if (!historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public final void decrementPosition(String key) {
    if (!positionMap.containsKey(key))
      initHistory(key); 
    int position = ((Integer)positionMap.get(key)).intValue();
    if (position == 0)
      return; 
    positionMap.put(key, Integer.valueOf(position - 1));
  }
  
  public void recordMove(String key, ViewBean viewBean) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionMove(viewBean);
    if (!historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public void recordRemove(String key, ArrayList<ViewBean> list) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionRemove(list);
    if (!historyMap.containsKey(key))
      initHistory(key); 
    trimFutureHistory(key);
    addHistoryEntry(key, historyViewBean);
  }
  
  public final void incrementPosition(String key) {
    if (!positionMap.containsKey(key))
      initHistory(key); 
    int position = ((Integer)positionMap.get(key)).intValue();
    positionMap.put(key, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String key) {
    historyMap.put(key, new ArrayList<>());
    positionMap.put(key, Integer.valueOf(0));
  }
  
  public boolean canRedo(String key) {
    return !positionMap.containsKey(key) ? false : ((((Integer)positionMap.get(key)).intValue() < ((ArrayList)historyMap.get(key)).size()));
  }
  
  public boolean canUndo(String key) {
    return !positionMap.containsKey(key) ? false : ((((Integer)positionMap.get(key)).intValue() > 0));
  }
  
  public HistoryViewBean redo(String key) {
    if (!canRedo(key))
      return null; 
    int position = ((Integer)positionMap.get(key)).intValue();
    incrementPosition(key);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)historyMap.get(key)).get(position - 1 + 1)).clone();
  }
  
  public HistoryViewBean undo(String key) {
    if (!canUndo(key))
      return null; 
    int position = ((Integer)positionMap.get(key)).intValue();
    decrementPosition(key);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)historyMap.get(key)).get(position - 1)).clone();
  }
}
