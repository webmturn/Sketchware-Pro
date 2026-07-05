package pro.sketchware.activities.editor.manage.view.fragments;

import android.view.View;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.beans.SelectableBean;
import pro.sketchware.activities.editor.manage.view.ManageViewActivity;

public class ViewFileLongClickListener implements View.OnLongClickListener {
  public final CustomViewFilesFragment.FileListAdapter adapter;
  
  public final CustomViewFilesFragment.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileLongClickListener(CustomViewFilesFragment.FileListAdapter.ViewHolder holder, CustomViewFilesFragment.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public boolean onLongClick(View view) {
    CustomViewFilesFragment.FileListAdapter.ViewHolder holder = viewHolder;
    int pos = holder.getLayoutPosition();
    if (pos == RecyclerView.NO_POSITION) return true;
    ((ManageViewActivity)viewHolder.adapterRef.outerAdapter.getActivity()).setSelectionMode(true);
    holder.adapterRef.selectedPosition = pos;
    CheckBox checkBox = viewHolder.checkbox;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)viewHolder.adapterRef.outerAdapter.projectFiles.get(viewHolder.adapterRef.selectedPosition)).isSelected = viewHolder.checkbox.isChecked();
    return true;
  }
}
