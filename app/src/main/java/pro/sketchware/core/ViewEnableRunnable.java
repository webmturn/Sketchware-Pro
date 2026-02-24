package pro.sketchware.core;

import android.view.View;

public class ViewEnableRunnable implements Runnable {
  public final View view;
  
  public ViewEnableRunnable(View view) {
    this.view = view;
  }
  
  public void run() {
    this.view.setEnabled(true);
  }
}
