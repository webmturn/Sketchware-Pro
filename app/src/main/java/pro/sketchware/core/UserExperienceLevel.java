package pro.sketchware.core;

import android.content.Context;

public class UserExperienceLevel {
  public SharedPrefsHelper database;
  
  public int level;
  
  public int score;
  
  public UserExperienceLevel(Context context) {
    database = new SharedPrefsHelper(context, "U1");
    level = database.getIntDefault("U1I0");
    int levelValue = level;
    int clampedLevel = levelValue;
    if (levelValue > 3)
      clampedLevel = 3; 
    score = clampedLevel * 20;
  }
}
