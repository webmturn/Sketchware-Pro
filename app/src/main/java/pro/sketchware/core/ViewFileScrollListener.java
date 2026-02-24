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
  
  public void onScrolled(RecyclerView recyclerView, int x, int y) {
    super.onScrolled(recyclerView, x, y);
    if (y > 2) {
      if (((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).fab.isEnabled())
        ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).fab.hide(); 
    } else if (y < -2 && ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).fab.isEnabled()) {
      ((ManageViewActivity)this.innerAdapter.outerAdapter.getActivity()).fab.show();
    } 
  }
}
