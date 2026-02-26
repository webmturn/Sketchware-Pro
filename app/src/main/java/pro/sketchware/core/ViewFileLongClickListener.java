package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileLongClickListener implements View.OnLongClickListener {
  public final ViewFilesAdapter.FileListAdapter adapter;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileLongClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder parama, ViewFilesAdapter.FileListAdapter parama1) {
    this.adapter = parama1;
    this.viewHolder = parama;
  }
  
  public boolean onLongClick(View view) {
    ((ManageViewActivity)this.viewHolder.adapterRef.outerAdapter.getActivity()).setSelectionMode(true);
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = this.viewHolder;
    holder.adapterRef.selectedPosition = holder.getLayoutPosition();
    CheckBox checkBox = this.viewHolder.checkbox;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)this.viewHolder.adapterRef.outerAdapter.projectFiles.get(this.viewHolder.adapterRef.selectedPosition)).isSelected = this.viewHolder.checkbox.isChecked();
    return true;
  }
}
