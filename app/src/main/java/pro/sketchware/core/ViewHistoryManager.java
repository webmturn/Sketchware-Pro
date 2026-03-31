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
  
  public ViewHistoryManager(String scId) {
    this.scId = scId;
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
  
  public static ViewHistoryManager getInstance(String scId) {
    if (instance == null) {
      synchronized (ViewHistoryManager.class) {
        if (instance == null || !instance.scId.equals(scId)) {
          instance = new ViewHistoryManager(scId);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String layoutFileName) {
    if (!positionMap.containsKey(layoutFileName))
      return; 
    ArrayList historyEntries = historyMap.get(layoutFileName);
    int position = positionMap.getOrDefault(layoutFileName, 0);
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  public final void addHistoryEntry(String layoutFileName, HistoryViewBean historyViewBean) {
    if (!historyMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    ArrayList<HistoryViewBean> entries = historyMap.get(layoutFileName);
    if (entries == null) return;
    entries.add(historyViewBean);
    if (entries.size() > MAX_HISTORY_STEPS) {
      entries.remove(0);
    } else {
      incrementPosition(layoutFileName);
    } 
  }
  
  public void recordAdd(String layoutFileName, ViewBean viewBean) {
    ArrayList<ViewBean> views = new ArrayList<>();
    views.add(viewBean);
    recordAddMultiple(layoutFileName, views);
  }
  
  public void recordUpdate(String layoutFileName, ViewBean prevViewData, ViewBean currentViewData) {
    if (prevViewData.isEqual(currentViewData))
      return; 
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionUpdate(prevViewData, currentViewData);
    if (!historyMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    trimFutureHistory(layoutFileName);
    addHistoryEntry(layoutFileName, historyViewBean);
  }
  
  public void recordAddMultiple(String layoutFileName, ArrayList<ViewBean> viewBeans) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionAdd(viewBeans);
    if (!historyMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    trimFutureHistory(layoutFileName);
    addHistoryEntry(layoutFileName, historyViewBean);
  }
  
  public final void decrementPosition(String layoutFileName) {
    if (!positionMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    int position = positionMap.getOrDefault(layoutFileName, 0);
    if (position == 0)
      return; 
    positionMap.put(layoutFileName, Integer.valueOf(position - 1));
  }
  
  public void recordMove(String layoutFileName, ViewBean viewBean) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionMove(viewBean);
    if (!historyMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    trimFutureHistory(layoutFileName);
    addHistoryEntry(layoutFileName, historyViewBean);
  }
  
  public void recordRemove(String layoutFileName, ArrayList<ViewBean> viewBeans) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionRemove(viewBeans);
    if (!historyMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    trimFutureHistory(layoutFileName);
    addHistoryEntry(layoutFileName, historyViewBean);
  }
  
  public final void incrementPosition(String layoutFileName) {
    if (!positionMap.containsKey(layoutFileName))
      initHistory(layoutFileName);
    int position = positionMap.getOrDefault(layoutFileName, 0);
    positionMap.put(layoutFileName, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String layoutFileName) {
    historyMap.put(layoutFileName, new ArrayList<>());
    positionMap.put(layoutFileName, Integer.valueOf(0));
  }
  
  public boolean canRedo(String layoutFileName) {
    if (!positionMap.containsKey(layoutFileName)) return false;
    ArrayList<HistoryViewBean> entries = historyMap.get(layoutFileName);
    return entries != null && positionMap.getOrDefault(layoutFileName, 0) < entries.size();
  }
  
  public boolean canUndo(String layoutFileName) {
    return positionMap.getOrDefault(layoutFileName, 0) > 0;
  }
  
  public HistoryViewBean redo(String layoutFileName) {
    if (!canRedo(layoutFileName))
      return null; 
    int position = positionMap.getOrDefault(layoutFileName, 0);
    incrementPosition(layoutFileName);
    ArrayList<HistoryViewBean> entries = historyMap.get(layoutFileName);
    if (entries == null || position >= entries.size()) return null;
    return entries.get(position).clone();
  }
  
  public HistoryViewBean undo(String layoutFileName) {
    if (!canUndo(layoutFileName))
      return null; 
    int position = positionMap.getOrDefault(layoutFileName, 0);
    decrementPosition(layoutFileName);
    ArrayList<HistoryViewBean> entries = historyMap.get(layoutFileName);
    if (entries == null || position - 1 < 0) return null;
    return entries.get(position - 1).clone();
  }
}
