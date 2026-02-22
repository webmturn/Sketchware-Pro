package a.a.a;

import androidx.recyclerview.widget.RecyclerView;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class tw extends RecyclerView.OnScrollListener {
  public final xw a;
  
  public final xw.a b;
  
  public tw(xw.a parama, xw paramxw) {
    this.a = paramxw;
    this.b = parama;
  }
  
  public void onScrolled(RecyclerView paramRecyclerView, int paramInt1, int paramInt2) {
    super.onScrolled(paramRecyclerView, paramInt1, paramInt2);
    if (paramInt2 > 2) {
      if (((ManageViewActivity)this.b.d.getActivity()).s.isEnabled())
        ((ManageViewActivity)this.b.d.getActivity()).s.hide(); 
    } else if (paramInt2 < -2 && ((ManageViewActivity)this.b.d.getActivity()).s.isEnabled()) {
      ((ManageViewActivity)this.b.d.getActivity()).s.show();
    } 
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\tw.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */