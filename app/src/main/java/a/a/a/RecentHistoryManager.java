package a.a.a;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RecentHistoryManager {
  public static RecentHistoryManager instance;
  
  public static int maxItems = 10;
  
  public HashMap<String, ArrayList<String>> recentMap;
  
  public SharedPrefsHelper database;
  
  public static RecentHistoryManager getInstance() {
    if (instance == null) {
      synchronized (StringResource.class) {
        if (instance == null) {
          instance = new RecentHistoryManager();
        }
      }
    }
    return instance;
  }
  
  public ArrayList<String> getRecentItems(String paramString) {
    return this.recentMap.get(paramString);
  }
  
  public void initialize(Context paramContext) {
    if (this.recentMap == null)
      this.recentMap = new HashMap<String, ArrayList<String>>(); 
    this.recentMap.clear();
    if (this.database == null)
      this.database = new SharedPrefsHelper(paramContext, "P26"); 
  }
  
  public void addRecentItem(String paramString1, String paramString2) {
    ArrayList<String> arrayList1 = this.recentMap.get(paramString1);
    ArrayList<String> arrayList2 = arrayList1;
    if (arrayList1 == null) {
      arrayList2 = new ArrayList<>();
      this.recentMap.put(paramString1, arrayList2);
    } 
    if (arrayList2.contains(paramString2))
      arrayList2.remove(paramString2); 
    arrayList2.add(0, paramString2);
    if (arrayList2.size() > maxItems)
      arrayList2.remove(arrayList2.size() - 1); 
  }
  
  public void saveToDatabase() {
    Iterator<String> iterator = this.recentMap.keySet().iterator();
    String str = "";
    while (iterator.hasNext()) {
      String str1 = iterator.next();
      for (String str2 : this.recentMap.get(str1)) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(str2);
        stringBuilder.append(",");
        str = stringBuilder.toString();
      } 
      this.database.a(str1, str);
    } 
  }
  
  public void loadFromDatabase(String paramString) {
    if ((ArrayList)this.recentMap.get(paramString) == null) {
      String[] arrayOfString = this.database.f(paramString).split(",");
      int i = arrayOfString.length;
      while (true) {
        int j = i - 1;
        if (j >= 0) {
          i = j;
          if (!arrayOfString[j].isEmpty()) {
            addRecentItem(paramString, arrayOfString[j]);
            i = j;
          } 
          continue;
        } 
        break;
      } 
    } 
  }
}
