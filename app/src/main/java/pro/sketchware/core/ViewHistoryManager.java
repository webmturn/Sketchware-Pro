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
  
  public ViewHistoryManager(String paramString) {
    this.scId = paramString;
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
  
  public static ViewHistoryManager getInstance(String paramString) {
    if (instance == null) {
      synchronized (ViewHistoryManager.class) {
        if (instance == null || !instance.scId.equals(paramString)) {
          instance = new ViewHistoryManager(paramString);
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
  
  public final void addHistoryEntry(String paramString, HistoryViewBean paramHistoryViewBean) {
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    ArrayList<HistoryViewBean> arrayList = this.historyMap.get(paramString);
    arrayList.add(paramHistoryViewBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      incrementPosition(paramString);
    } 
  }
  
  public void recordAdd(String paramString, ViewBean paramViewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList<>();
    arrayList.add(paramViewBean);
    recordAddMultiple(paramString, arrayList);
  }
  
  public void recordUpdate(String paramString, ViewBean paramViewBean1, ViewBean paramViewBean2) {
    if (paramViewBean1.isEqual(paramViewBean2))
      return; 
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionUpdate(paramViewBean1, paramViewBean2);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyViewBean);
  }
  
  public void recordAddMultiple(String paramString, ArrayList<ViewBean> paramArrayList) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionAdd(paramArrayList);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyViewBean);
  }
  
  public final void decrementPosition(String paramString) {
    if (!this.positionMap.containsKey(paramString))
      initHistory(paramString); 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    if (i == 0)
      return; 
    this.positionMap.put(paramString, Integer.valueOf(i - 1));
  }
  
  public void recordMove(String paramString, ViewBean paramViewBean) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionMove(paramViewBean);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyViewBean);
  }
  
  public void recordRemove(String paramString, ArrayList<ViewBean> paramArrayList) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionRemove(paramArrayList);
    if (!this.historyMap.containsKey(paramString))
      initHistory(paramString); 
    trimFutureHistory(paramString);
    addHistoryEntry(paramString, historyViewBean);
  }
  
  public final void incrementPosition(String paramString) {
    if (!this.positionMap.containsKey(paramString))
      initHistory(paramString); 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    this.positionMap.put(paramString, Integer.valueOf(i + 1));
  }
  
  public void initHistory(String paramString) {
    this.historyMap.put(paramString, new ArrayList<HistoryViewBean>());
    this.positionMap.put(paramString, Integer.valueOf(0));
  }
  
  public boolean canRedo(String paramString) {
    return !this.positionMap.containsKey(paramString) ? false : ((((Integer)this.positionMap.get(paramString)).intValue() < ((ArrayList)this.historyMap.get(paramString)).size()));
  }
  
  public boolean canUndo(String paramString) {
    return !this.positionMap.containsKey(paramString) ? false : ((((Integer)this.positionMap.get(paramString)).intValue() > 0));
  }
  
  public HistoryViewBean redo(String paramString) {
    if (!canRedo(paramString))
      return null; 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    incrementPosition(paramString);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.historyMap.get(paramString)).get(i - 1 + 1)).clone();
  }
  
  public HistoryViewBean undo(String paramString) {
    if (!canUndo(paramString))
      return null; 
    int i = ((Integer)this.positionMap.get(paramString)).intValue();
    decrementPosition(paramString);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.historyMap.get(paramString)).get(i - 1)).clone();
  }
}
