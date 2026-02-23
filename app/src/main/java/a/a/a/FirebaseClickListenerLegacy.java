package a.a.a;

import android.view.View;

// Legacy click listener for Firebase checkbox toggle.
// Functionality now handled directly in FirebasePreviewView.initialize().
public class FirebaseClickListenerLegacy implements View.OnClickListener {
  public final FirebasePreviewView noOpClickListener;
  
  public FirebaseClickListenerLegacy(FirebasePreviewView param) {
    this.noOpClickListener = param;
  }
  
  public void onClick(View paramView) {
    // No-op: FirebasePreviewView handles its own click listener
  }
}
