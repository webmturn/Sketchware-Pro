package a.a.a;

import android.content.Context;

public class ro {
  public DB a;
  
  public int b;
  
  public int c;
  
  public ro(Context paramContext) {
    this.a = new DB(paramContext, "U1");
    this.b = this.a.d("U1I0");
    int i = this.b;
    int j = i;
    if (i > 3)
      j = 3; 
    this.c = j * 20;
  }
}
