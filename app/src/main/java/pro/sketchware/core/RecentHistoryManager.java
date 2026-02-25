package pro.sketchware.core;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RecentHistoryManager {
  public static volatile RecentHistoryManager instance;
  
  public static int maxItems = 10;
  
  public HashMap<String, ArrayList<String>> recentMap;
  
  public SharedPrefsHelper database;
  
  public static RecentHistoryManager getInstance() {
    if (instance == null) {
      synchronized (RecentHistoryManager.class) {
        if (instance == null) {
          instance = new RecentHistoryManager();
        }
      }
    }
    return instance;
  }
  
  public ArrayList<String> getRecentItems(String category) {
    return this.recentMap.get(category);
  }
  
  public void initialize(Context context) {
    if (this.recentMap == null)
      this.recentMap = new HashMap<String, ArrayList<String>>(); 
    if (this.database == null)
      this.database = new SharedPrefsHelper(context, "P26"); 
  }
  
  public void addRecentItem(String key, String value) {
    ArrayList<String> existingList = this.recentMap.get(key);
    ArrayList<String> historyList = existingList;
    if (existingList == null) {
      historyList = new ArrayList<>();
      this.recentMap.put(key, historyList);
    } 
    if (historyList.contains(value))
      historyList.remove(value); 
    historyList.add(0, value);
    if (historyList.size() > maxItems)
      historyList.remove(historyList.size() - 1); 
  }
  
  public void saveToDatabase() {
    for (String key : this.recentMap.keySet()) {
      StringBuilder entryBuilder = new StringBuilder();
      for (String item : this.recentMap.get(key)) {
        entryBuilder.append(item);
        entryBuilder.append(",");
      } 
      this.database.put(key, entryBuilder.toString());
    } 
  }
  
  public void loadFromDatabase(String category) {
    if ((ArrayList)this.recentMap.get(category) == null) {
      String[] parts = this.database.getStringDefault(category).split(",");
      int i = parts.length;
      while (true) {
        int j = i - 1;
        if (j >= 0) {
          i = j;
          if (!parts[j].isEmpty()) {
            addRecentItem(category, parts[j]);
            i = j;
          } 
          continue;
        } 
        break;
      } 
    } 
  }
}
