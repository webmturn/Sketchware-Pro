package a.a.a;

import android.view.View;

public final class jB implements Runnable {
  public final View a;
  
  public jB(View paramView) {
    this.a = paramView;
  }
  
  public void run() {
    this.a.setEnabled(true);
  }
}
