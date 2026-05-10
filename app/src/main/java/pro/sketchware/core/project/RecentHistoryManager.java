package pro.sketchware.core.project;

import pro.sketchware.core.util.SharedPrefsHelper;

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
  
  public void addRecentItem(String category, String item) {
    ArrayList<String> existingList = recentMap.get(category);
    ArrayList<String> historyList = existingList;
    if (existingList == null) {
      historyList = new ArrayList<>();
      recentMap.put(category, historyList);
    } 
    if (historyList.contains(item))
      historyList.remove(item);
    historyList.add(0, item);
    if (historyList.size() > maxItems)
      historyList.remove(historyList.size() - 1); 
  }
  
  public void saveToDatabase() {
    for (String category : recentMap.keySet()) {
      StringBuilder entryBuilder = new StringBuilder();
      for (String item : recentMap.get(category)) {
        entryBuilder.append(item);
        entryBuilder.append(",");
      } 
      database.put(category, entryBuilder.toString());
    } 
  }
  
  public void loadFromDatabase(String category) {
    if ((ArrayList)recentMap.get(category) == null) {
      String[] parts = database.getStringDefault(category).split(",");
      for (int idx = parts.length - 1; idx >= 0; idx--) {
        if (!parts[idx].isEmpty()) {
          addRecentItem(category, parts[idx]);
        }
      }
    } 
  }
}
