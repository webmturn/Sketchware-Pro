package pro.sketchware.core;

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
  
  public ArrayList<String> getRecentItems(String str) {
    return this.recentMap.get(str);
  }
  
  public void initialize(Context context) {
    if (this.recentMap == null)
      this.recentMap = new HashMap<String, ArrayList<String>>(); 
    this.recentMap.clear();
    if (this.database == null)
      this.database = new SharedPrefsHelper(context, "P26"); 
  }
  
  public void addRecentItem(String key, String value) {
    ArrayList<String> arrayList1 = this.recentMap.get(key);
    ArrayList<String> arrayList2 = arrayList1;
    if (arrayList1 == null) {
      arrayList2 = new ArrayList<>();
      this.recentMap.put(key, arrayList2);
    } 
    if (arrayList2.contains(value))
      arrayList2.remove(value); 
    arrayList2.add(0, value);
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
      this.database.put(str1, str);
    } 
  }
  
  public void loadFromDatabase(String str) {
    if ((ArrayList)this.recentMap.get(str) == null) {
      String[] parts = this.database.getStringDefault(str).split(",");
      int i = parts.length;
      while (true) {
        int j = i - 1;
        if (j >= 0) {
          i = j;
          if (!parts[j].isEmpty()) {
            addRecentItem(str, parts[j]);
            i = j;
          } 
          continue;
        } 
        break;
      } 
    } 
  }
}
