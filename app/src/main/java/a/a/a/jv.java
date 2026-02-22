package a.a.a;

import android.view.View;

// Legacy click listener for Firebase checkbox toggle.
// Functionality now handled directly in FirebasePreviewView.initialize().
public class jv implements View.OnClickListener {
  public final FirebasePreviewView a;
  
  public jv(FirebasePreviewView param) {
    this.a = param;
  }
  
  public void onClick(View paramView) {
    // No-op: FirebasePreviewView handles its own click listener
  }
}
