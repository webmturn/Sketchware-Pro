package a.a.a;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.HistoryBlockBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class bC {
  public static bC a;
  
  public Map<String, Integer> b;
  
  public Map<String, ArrayList<HistoryBlockBean>> c;
  
  public String d;
  
  public bC(String paramString) {
    this.d = paramString;
    this.c = new HashMap<String, ArrayList<HistoryBlockBean>>();
    this.b = new HashMap<String, Integer>();
  }
  
  public static String a(String paramString1, String paramString2, String paramString3) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(paramString1);
    stringBuilder.append("_");
    stringBuilder.append(paramString2);
    stringBuilder.append("_");
    stringBuilder.append(paramString3);
    return stringBuilder.toString();
  }
  
  public static void a() {
    bC bC1 = a;
    if (bC1 != null) {
      bC1.d = "";
      bC1.c = null;
      bC1.b = null;
    } 
    a = null;
  }
  
  public static bC d(String paramString) {
    if (a == null) {
      synchronized (bC.class) {
        if (a == null || !a.d.equals(paramString)) {
          a = new bC(paramString);
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
  
  public void a(String paramString, BlockBean paramBlockBean1, int paramInt1, int paramInt2, BlockBean paramBlockBean2, BlockBean paramBlockBean3) {
    ArrayList<BlockBean> arrayList = new ArrayList<>();
    arrayList.add(paramBlockBean1);
    a(paramString, arrayList, paramInt1, paramInt2, paramBlockBean2, paramBlockBean3);
  }
  
  public void a(String paramString, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    if (paramBlockBean1.isEqual(paramBlockBean2))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(paramBlockBean1, paramBlockBean2);
    if (!this.c.containsKey(paramString))
      f(paramString); 
    a(paramString);
    a(paramString, historyBlockBean);
  }
  
  public final void a(String paramString, HistoryBlockBean paramHistoryBlockBean) {
    if (!this.c.containsKey(paramString))
      f(paramString); 
    ArrayList<HistoryBlockBean> arrayList = this.c.get(paramString);
    arrayList.add(paramHistoryBlockBean);
    if (arrayList.size() > 50) {
      arrayList.remove(0);
    } else {
      e(paramString);
    } 
  }
  
  public void a(String paramString, ArrayList<BlockBean> paramArrayList, int paramInt1, int paramInt2, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(paramArrayList, paramInt1, paramInt2, paramBlockBean1, paramBlockBean2);
    if (!this.c.containsKey(paramString))
      f(paramString); 
    a(paramString);
    a(paramString, historyBlockBean);
  }
  
  public void a(String paramString, ArrayList<BlockBean> paramArrayList1, ArrayList<BlockBean> paramArrayList2, int paramInt1, int paramInt2, int paramInt3, int paramInt4, BlockBean paramBlockBean1, BlockBean paramBlockBean2, BlockBean paramBlockBean3, BlockBean paramBlockBean4) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(paramArrayList1, paramArrayList2, paramInt1, paramInt2, paramInt3, paramInt4, paramBlockBean1, paramBlockBean2, paramBlockBean3, paramBlockBean4);
    if (!this.c.containsKey(paramString))
      f(paramString); 
    a(paramString);
    a(paramString, historyBlockBean);
  }
  
  public void b(String paramString) {
    if (this.c.containsKey(paramString)) {
      this.c.remove(paramString);
      this.b.remove(paramString);
    } 
  }
  
  public void b(String paramString, ArrayList<BlockBean> paramArrayList, int paramInt1, int paramInt2, BlockBean paramBlockBean1, BlockBean paramBlockBean2) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(paramArrayList, paramInt1, paramInt2, paramBlockBean1, paramBlockBean2);
    if (!this.c.containsKey(paramString))
      f(paramString); 
    a(paramString);
    a(paramString, historyBlockBean);
  }
  
  public final void c(String paramString) {
    if (!this.b.containsKey(paramString))
      f(paramString); 
    int i = ((Integer)this.b.get(paramString)).intValue();
    if (i == 0)
      return; 
    this.b.put(paramString, Integer.valueOf(i - 1));
  }
  
  public final void e(String paramString) {
    if (!this.b.containsKey(paramString))
      f(paramString); 
    int i = ((Integer)this.b.get(paramString)).intValue();
    this.b.put(paramString, Integer.valueOf(i + 1));
  }
  
  public void f(String paramString) {
    this.c.put(paramString, new ArrayList<HistoryBlockBean>());
    this.b.put(paramString, Integer.valueOf(0));
  }
  
  public boolean g(String paramString) {
    return !this.b.containsKey(paramString) ? false : ((((Integer)this.b.get(paramString)).intValue() < ((ArrayList)this.c.get(paramString)).size()));
  }
  
  public boolean h(String paramString) {
    return !this.b.containsKey(paramString) ? false : ((((Integer)this.b.get(paramString)).intValue() > 0));
  }
  
  public HistoryBlockBean i(String paramString) {
    if (!g(paramString))
      return null; 
    int i = ((Integer)this.b.get(paramString)).intValue();
    e(paramString);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.c.get(paramString)).get(i - 1 + 1)).clone();
  }
  
  public HistoryBlockBean j(String paramString) {
    if (!h(paramString))
      return null; 
    int i = ((Integer)this.b.get(paramString)).intValue();
    c(paramString);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)this.c.get(paramString)).get(i - 1)).clone();
  }
}
