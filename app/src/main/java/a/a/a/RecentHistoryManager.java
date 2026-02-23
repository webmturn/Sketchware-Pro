package a.a.a;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RecentHistoryManager {
  public static RecentHistoryManager a;
  
  public static int b = 10;
  
  public HashMap<String, ArrayList<String>> c;
  
  public DB d;
  
  public static RecentHistoryManager a() {
    if (a == null) {
      synchronized (xB.class) {
        if (a == null) {
          a = new RecentHistoryManager();
        }
      }
    }
    return a;
  }
  
  public ArrayList<String> a(String paramString) {
    return this.c.get(paramString);
  }
  
  public void a(Context paramContext) {
    if (this.c == null)
      this.c = new HashMap<String, ArrayList<String>>(); 
    this.c.clear();
    if (this.d == null)
      this.d = new DB(paramContext, "P26"); 
  }
  
  public void a(String paramString1, String paramString2) {
    ArrayList<String> arrayList1 = this.c.get(paramString1);
    ArrayList<String> arrayList2 = arrayList1;
    if (arrayList1 == null) {
      arrayList2 = new ArrayList<>();
      this.c.put(paramString1, arrayList2);
    } 
    if (arrayList2.contains(paramString2))
      arrayList2.remove(paramString2); 
    arrayList2.add(0, paramString2);
    if (arrayList2.size() > b)
      arrayList2.remove(arrayList2.size() - 1); 
  }
  
  public void b() {
    Iterator<String> iterator = this.c.keySet().iterator();
    String str = "";
    while (iterator.hasNext()) {
      String str1 = iterator.next();
      for (String str2 : this.c.get(str1)) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(str2);
        stringBuilder.append(",");
        str = stringBuilder.toString();
      } 
      this.d.a(str1, str);
    } 
  }
  
  public void b(String paramString) {
    if ((ArrayList)this.c.get(paramString) == null) {
      String[] arrayOfString = this.d.f(paramString).split(",");
      int i = arrayOfString.length;
      while (true) {
        int j = i - 1;
        if (j >= 0) {
          i = j;
          if (!arrayOfString[j].isEmpty()) {
            a(paramString, arrayOfString[j]);
            i = j;
          } 
          continue;
        } 
        break;
      } 
    } 
  }
}
