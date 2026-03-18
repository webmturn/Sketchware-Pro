package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.SelectableBean;

public class ViewFileClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.FileListAdapter adapter;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder holder, ViewFilesAdapter.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public void onClick(View view) {
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = viewHolder;
    int pos = holder.getLayoutPosition();
    if (pos == RecyclerView.NO_POSITION) return;
    holder.adapterRef.selectedPosition = pos;
    if (viewHolder.adapterRef.outerAdapter.isSelectionMode.booleanValue()) {
      CheckBox checkBox = viewHolder.checkbox;
      checkBox.setChecked(checkBox.isChecked() ^ true);
      ((SelectableBean)viewHolder.adapterRef.outerAdapter.projectFiles.get(viewHolder.adapterRef.selectedPosition)).isSelected = viewHolder.checkbox.isChecked();
      ViewFilesAdapter.FileListAdapter listAdapter = viewHolder.adapterRef;
      listAdapter.notifyItemChanged(listAdapter.selectedPosition);
    } 
  }
}
