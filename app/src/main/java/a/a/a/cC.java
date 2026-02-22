package a.a.a;

import com.besome.sketch.beans.HistoryViewBean;
import com.besome.sketch.beans.ViewBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class cC {
  public static cC a;
  
  public Map<String, Integer> b;
  
  public Map<String, ArrayList<HistoryViewBean>> c;
  
  public String d;
  
  public cC(String paramString) {
    this.d = paramString;
    this.c = new HashMap<String, ArrayList<HistoryViewBean>>();
    this.b = new HashMap<String, Integer>();
  }
  
  public static void a() {
    cC cC1 = a;
    if (cC1 != null) {
      cC1.d = "";
      cC1.c = null;
      cC1.b = null;
    } 
    a = null;
  }
  
  public static cC c(String paramString) {
    if (a == null) {
      synchronized (cC.class) {
        if (a == null || !a.d.equals(paramString)) {
          a = new cC(paramString);
        }
      }
    }
    return a;
  }
  
  public final void a(String paramString) {
    if (!this.b.containsKey(paramString))
      return; 
    ArrayList arrayList = this.c.get(paramString);
    int i = ((Integer)this.b.get(paramString)).intValue();
    if (arrayList == null)
      return; 
    for (int j = arrayList.size(); j > i; j--)
      arrayList.remove(j - 1); 
  }
  
  public final void a(String paramString, HistoryViewBean paramHistoryViewBean) {
    if (!this.c.containsKey(paramString))
      e(paramString); 
    ArrayList<HistoryViewBean> arrayList = this.c.get(paramString);
    arrayList.add(paramHistoryViewBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      d(paramString);
    } 
  }
  
  public void a(String paramString, ViewBean paramViewBean) {
    ArrayList<ViewBean> arrayList = new ArrayList();
    arrayList.add(paramViewBean);
    a(paramString, arrayList);
  }
  
  public void a(String paramString, ViewBean paramViewBean1, ViewBean paramViewBean2) {
    if (paramViewBean1.isEqual(paramViewBean2))
      return; 
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionUpdate(paramViewBean1, paramViewBean2);
    if (!this.c.containsKey(paramString))
      e(paramString); 
    a(paramString);
    a(paramString, historyViewBean);
  }
  
  public void a(String paramString, ArrayList<ViewBean> paramArrayList) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionAdd(paramArrayList);
    if (!this.c.containsKey(paramString))
      e(paramString); 
    a(paramString);
    a(paramString, historyViewBean);
  }
  
  public final void b(String paramString) {
    if (!this.b.containsKey(paramString))
      e(paramString); 
    int i = ((Integer)this.b.get(paramString)).intValue();
    if (i == 0)
      return; 
    this.b.put(paramString, Integer.valueOf(i - 1));
  }
  
  public void b(String paramString, ViewBean paramViewBean) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionMove(paramViewBean);
    if (!this.c.containsKey(paramString))
      e(paramString); 
    a(paramString);
    a(paramString, historyViewBean);
  }
  
  public void b(String paramString, ArrayList<ViewBean> paramArrayList) {
    HistoryViewBean historyViewBean = new HistoryViewBean();
    historyViewBean.actionRemove(paramArrayList);
    if (!this.c.containsKey(paramString))
      e(paramString); 
    a(paramString);
    a(paramString, historyViewBean);
  }
  
  public final void d(String paramString) {
    if (!this.b.containsKey(paramString))
      e(paramString); 
    int i = ((Integer)this.b.get(paramString)).intValue();
    this.b.put(paramString, Integer.valueOf(i + 1));
  }
  
  public void e(String paramString) {
    this.c.put(paramString, new ArrayList<HistoryViewBean>());
    this.b.put(paramString, Integer.valueOf(0));
  }
  
  public boolean f(String paramString) {
    return !this.b.containsKey(paramString) ? false : ((((Integer)this.b.get(paramString)).intValue() < ((ArrayList)this.c.get(paramString)).size()));
  }
  
  public boolean g(String paramString) {
    return !this.b.containsKey(paramString) ? false : ((((Integer)this.b.get(paramString)).intValue() > 0));
  }
  
  public HistoryViewBean h(String paramString) {
    if (!f(paramString))
      return null; 
    int i = ((Integer)this.b.get(paramString)).intValue();
    d(paramString);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.c.get(paramString)).get(i - 1 + 1)).clone();
  }
  
  public HistoryViewBean i(String paramString) {
    if (!g(paramString))
      return null; 
    int i = ((Integer)this.b.get(paramString)).intValue();
    b(paramString);
    return ((HistoryViewBean)((ArrayList<HistoryViewBean>)this.c.get(paramString)).get(i - 1)).clone();
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\cC.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */