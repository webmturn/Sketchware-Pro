package pro.sketchware.core.fragments;

import android.view.View;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

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
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = viewHolder;
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
