package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;

public class ViewFileClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.FileListAdapter adapter;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder parama, ViewFilesAdapter.FileListAdapter parama1) {
    this.adapter = parama1;
    this.viewHolder = parama;
  }
  
  public void onClick(View view) {
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = this.viewHolder;
    holder.adapterRef.selectedPosition = holder.getLayoutPosition();
    if (this.viewHolder.adapterRef.outerAdapter.isSelectionMode.booleanValue()) {
      CheckBox checkBox = this.viewHolder.checkbox;
      checkBox.setChecked(checkBox.isChecked() ^ true);
      ((SelectableBean)this.viewHolder.adapterRef.outerAdapter.projectFiles.get(this.viewHolder.adapterRef.selectedPosition)).isSelected = this.viewHolder.checkbox.isChecked();
      ViewFilesAdapter.FileListAdapter listAdapter = this.viewHolder.adapterRef;
      listAdapter.notifyItemChanged(listAdapter.selectedPosition);
    } 
  }
}
