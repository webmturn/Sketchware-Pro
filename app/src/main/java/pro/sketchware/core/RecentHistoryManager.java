package pro.sketchware.core;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;

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
    return recentMap.get(category);
  }
  
  public void initialize(Context context) {
    if (recentMap == null)
      recentMap = new HashMap<>(); 
    if (database == null)
      database = new SharedPrefsHelper(context, "P26"); 
  }
  
  public void addRecentItem(String key, String value) {
    ArrayList<String> existingList = recentMap.get(key);
    ArrayList<String> historyList = existingList;
    if (existingList == null) {
      historyList = new ArrayList<>();
      recentMap.put(key, historyList);
    } 
    if (historyList.contains(value))
      historyList.remove(value); 
    historyList.add(0, value);
    if (historyList.size() > maxItems)
      historyList.remove(historyList.size() - 1); 
  }
  
  public void saveToDatabase() {
    for (String key : recentMap.keySet()) {
      StringBuilder entryBuilder = new StringBuilder();
      for (String item : recentMap.get(key)) {
        entryBuilder.append(item);
        entryBuilder.append(",");
      } 
      database.put(key, entryBuilder.toString());
    } 
  }
  
  public void loadFromDatabase(String category) {
    if ((ArrayList)recentMap.get(category) == null) {
      String[] parts = database.getStringDefault(category).split(",");
      int size = parts.length;
      while (true) {
        int idx = size - 1;
        if (idx >= 0) {
          size = idx;
          if (!parts[idx].isEmpty()) {
            addRecentItem(category, parts[idx]);
            size = idx;
          } 
          continue;
        } 
        break;
      } 
    } 
  }
}
