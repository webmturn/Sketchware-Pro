package a.a.a;

import android.content.Context;

public class UserExperienceLevel {
  public SharedPrefsHelper database;
  
  public int level;
  
  public int score;
  
  public UserExperienceLevel(Context paramContext) {
    this.database = new SharedPrefsHelper(paramContext, "U1");
    this.level = this.database.d("U1I0");
    int i = this.level;
    int j = i;
    if (i > 3)
      j = 3; 
    this.score = j * 20;
  }
}
