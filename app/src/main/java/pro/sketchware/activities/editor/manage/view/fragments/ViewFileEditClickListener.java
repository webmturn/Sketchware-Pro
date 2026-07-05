package pro.sketchware.activities.editor.manage.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.beans.ProjectFileBean;
import pro.sketchware.activities.editor.manage.view.PresetSettingActivity;
import pro.sketchware.util.UIHelper;

public class ViewFileEditClickListener implements View.OnClickListener {
  public final CustomViewFilesFragment.FileListAdapter adapter;
  
  public final CustomViewFilesFragment.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileEditClickListener(CustomViewFilesFragment.FileListAdapter.ViewHolder holder, CustomViewFilesFragment.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public void onClick(View view) {
    char c;
    if (UIHelper.isClickThrottled())
      return; 
    CustomViewFilesFragment.FileListAdapter.ViewHolder holder = viewHolder;
    int pos = holder.getLayoutPosition();
    if (pos == RecyclerView.NO_POSITION) return;
    holder.adapterRef.selectedPosition = pos;
    Intent intent = new Intent(viewHolder.adapterRef.outerAdapter.getActivity(), PresetSettingActivity.class);
    if (((ProjectFileBean)viewHolder.adapterRef.outerAdapter.projectFiles.get(viewHolder.adapterRef.selectedPosition)).fileType == 1) {
      c = 'ĕ';
    } else {
      c = 'Ė';
    } 
    intent.putExtra("request_code", c);
    intent.putExtra("edit_mode", true);
    viewHolder.adapterRef.outerAdapter.startActivityForResult(intent, c);
  }
}
