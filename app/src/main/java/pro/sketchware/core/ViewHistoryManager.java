package pro.sketchware.core;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ViewHistoryManager {
  public static volatile ViewHistoryManager instance;

  /** Maximum number of undo/redo steps retained per layout file. */
  public static int MAX_HISTORY_STEPS = 50;
  
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
    int position = positionMap.getOrDefault(key, 0);
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  public final void addHistoryEntry(String key, HistoryViewBean historyViewBean) {
    if (!historyMap.containsKey(key))
      initHistory(key); 
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
    if (entries == null) return;
    entries.add(historyViewBean);
    if (entries.size() > MAX_HISTORY_STEPS) {
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
    int position = positionMap.getOrDefault(key, 0);
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
    int position = positionMap.getOrDefault(key, 0);
    positionMap.put(key, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String key) {
    historyMap.put(key, new ArrayList<>());
    positionMap.put(key, Integer.valueOf(0));
  }
  
  public boolean canRedo(String key) {
    if (!positionMap.containsKey(key)) return false;
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
    return entries != null && positionMap.getOrDefault(key, 0) < entries.size();
  }
  
  public boolean canUndo(String key) {
    return positionMap.getOrDefault(key, 0) > 0;
  }
  
  public HistoryViewBean redo(String key) {
    if (!canRedo(key))
      return null; 
    int position = positionMap.getOrDefault(key, 0);
    incrementPosition(key);
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
    if (entries == null || position >= entries.size()) return null;
    return entries.get(position).clone();
  }
  
  public HistoryViewBean undo(String key) {
    if (!canUndo(key))
      return null; 
    int position = positionMap.getOrDefault(key, 0);
    decrementPosition(key);
    ArrayList<HistoryViewBean> entries = historyMap.get(key);
    if (entries == null || position - 1 < 0) return null;
    return entries.get(position - 1).clone();
  }
}
