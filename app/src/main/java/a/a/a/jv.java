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

/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\jv.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */