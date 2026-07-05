package pro.sketchware.activities.editor.manage.view.fragments;

import androidx.recyclerview.widget.RecyclerView;
import pro.sketchware.activities.editor.manage.view.ManageViewActivity;

public class ViewFileScrollListener extends RecyclerView.OnScrollListener {
  public final CustomViewFilesFragment filesAdapter;
  
  public final CustomViewFilesFragment.FileListAdapter innerAdapter;
  
  public ViewFileScrollListener(CustomViewFilesFragment.FileListAdapter fileListAdapter, CustomViewFilesFragment viewFilesAdapter) {
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
