package a.a.a;

import android.view.View;

public class ViewEnableRunnable implements Runnable {
  public final View a;
  
  public ViewEnableRunnable(View paramView) {
    this.a = paramView;
  }
  
  public void run() {
    this.a.setEnabled(true);
  }
}
