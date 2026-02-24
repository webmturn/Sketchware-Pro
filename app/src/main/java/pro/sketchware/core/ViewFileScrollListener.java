package pro.sketchware.core;

import androidx.recyclerview.widget.RecyclerView;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileScrollListener extends RecyclerView.OnScrollListener {
  public final ViewFilesAdapter a;
  
  public final ViewFilesAdapter.a b;
  
  public ViewFileScrollListener(ViewFilesAdapter.a parama, ViewFilesAdapter paramxw) {
    this.a = paramxw;
    this.b = parama;
  }
  
  public void onScrolled(RecyclerView paramRecyclerView, int paramInt1, int paramInt2) {
    super.onScrolled(paramRecyclerView, paramInt1, paramInt2);
    if (paramInt2 > 2) {
      if (((ManageViewActivity)this.b.outerAdapter.getActivity()).s.isEnabled())
        ((ManageViewActivity)this.b.outerAdapter.getActivity()).s.hide(); 
    } else if (paramInt2 < -2 && ((ManageViewActivity)this.b.outerAdapter.getActivity()).s.isEnabled()) {
      ((ManageViewActivity)this.b.outerAdapter.getActivity()).s.show();
    } 
  }
}
