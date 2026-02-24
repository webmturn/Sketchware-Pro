package pro.sketchware.core;

import androidx.recyclerview.widget.RecyclerView;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileScrollListener extends RecyclerView.OnScrollListener {
  public final ViewFilesAdapter filesAdapter;
  
  public final ViewFilesAdapter.FileListAdapter innerAdapter;
  
  public ViewFileScrollListener(ViewFilesAdapter.FileListAdapter parama, ViewFilesAdapter paramxw) {
    this.filesAdapter = paramxw;
    this.innerAdapter = parama;
  }
  
  public void onScrolled(RecyclerView paramRecyclerView, int paramInt1, int paramInt2) {
    super.onScrolled(paramRecyclerView, paramInt1, paramInt2);
    if (paramInt2 > 2) {
      if (((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).s.isEnabled())
        ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).s.hide(); 
    } else if (paramInt2 < -2 && ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).s.isEnabled()) {
      ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).s.show();
    } 
  }
}
