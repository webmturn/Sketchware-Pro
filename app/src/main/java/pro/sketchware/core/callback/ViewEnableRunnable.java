package pro.sketchware.core.callback;

import android.view.View;

public class ViewEnableRunnable implements Runnable {
  public final View view;
  
  public ViewEnableRunnable(View view) {
    this.view = view;
  }
  
  public void run() {
    view.setEnabled(true);
  }
}
