package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileLongClickListener implements View.OnLongClickListener {
  public final ViewFilesAdapter.FileListAdapter adapter;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileLongClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder holder, ViewFilesAdapter.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public boolean onLongClick(View view) {
    ((ManageViewActivity)viewHolder.adapterRef.outerAdapter.getActivity()).setSelectionMode(true);
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = viewHolder;
    holder.adapterRef.selectedPosition = holder.getLayoutPosition();
    CheckBox checkBox = viewHolder.checkbox;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)viewHolder.adapterRef.outerAdapter.projectFiles.get(viewHolder.adapterRef.selectedPosition)).isSelected = viewHolder.checkbox.isChecked();
    return true;
  }
}
