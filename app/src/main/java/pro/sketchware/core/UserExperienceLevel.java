package pro.sketchware.core;

import android.content.Context;

public class UserExperienceLevel {
  public SharedPrefsHelper database;
  
  public int level;
  
  public int score;
  
  public UserExperienceLevel(Context context) {
    this.database = new SharedPrefsHelper(context, "U1");
    this.level = this.database.getIntDefault("U1I0");
    int levelValue = this.level;
    int clampedLevel = levelValue;
    if (levelValue > 3)
      clampedLevel = 3; 
    this.score = clampedLevel * 20;
  }
}
