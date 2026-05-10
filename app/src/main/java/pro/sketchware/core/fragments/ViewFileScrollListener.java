package pro.sketchware.core.fragments;

import androidx.recyclerview.widget.RecyclerView;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileScrollListener extends RecyclerView.OnScrollListener {
  public final ViewFilesAdapter filesAdapter;
  
  public final ViewFilesAdapter.FileListAdapter innerAdapter;
  
  public ViewFileScrollListener(ViewFilesAdapter.FileListAdapter fileListAdapter, ViewFilesAdapter viewFilesAdapter) {
    filesAdapter = viewFilesAdapter;
    innerAdapter = fileListAdapter;
  }
  
  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);
    if (dy > 2) {
      if (((ManageViewActivity)innerAdapter.outerAdapter.getActivity()).fab.isEnabled())
        ((ManageViewActivity)innerAdapter.outerAdapter.getActivity()).fab.hide(); 
    } else if (dy < -2 && ((ManageViewActivity)innerAdapter.outerAdapter.getActivity()).fab.isEnabled()) {
      ((ManageViewActivity)innerAdapter.outerAdapter.getActivity()).fab.show();
    } 
  }
}
