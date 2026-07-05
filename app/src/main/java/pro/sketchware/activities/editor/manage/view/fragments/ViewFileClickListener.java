package pro.sketchware.activities.editor.manage.view.fragments;

import android.view.View;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.beans.SelectableBean;

public class ViewFileClickListener implements View.OnClickListener {
  public final CustomViewFilesFragment.FileListAdapter adapter;
  
  public final CustomViewFilesFragment.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileClickListener(CustomViewFilesFragment.FileListAdapter.ViewHolder holder, CustomViewFilesFragment.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public void onClick(View view) {
    CustomViewFilesFragment.FileListAdapter.ViewHolder holder = viewHolder;
    int pos = holder.getLayoutPosition();
    if (pos == RecyclerView.NO_POSITION) return;
    holder.adapterRef.selectedPosition = pos;
    if (viewHolder.adapterRef.outerAdapter.isSelectionMode.booleanValue()) {
      CheckBox checkBox = viewHolder.checkbox;
      checkBox.setChecked(checkBox.isChecked() ^ true);
      ((SelectableBean)viewHolder.adapterRef.outerAdapter.projectFiles.get(viewHolder.adapterRef.selectedPosition)).isSelected = viewHolder.checkbox.isChecked();
      CustomViewFilesFragment.FileListAdapter listAdapter = viewHolder.adapterRef;
      listAdapter.notifyItemChanged(listAdapter.selectedPosition);
    } 
  }
}
