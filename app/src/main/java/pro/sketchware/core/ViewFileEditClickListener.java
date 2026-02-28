package pro.sketchware.core;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;

public class ViewFileEditClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.FileListAdapter adapter;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder viewHolder;
  
  public ViewFileEditClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder holder, ViewFilesAdapter.FileListAdapter adapter) {
    this.adapter = adapter;
    viewHolder = holder;
  }
  
  public void onClick(View view) {
    char c;
    if (UIHelper.isClickThrottled())
      return; 
    ViewFilesAdapter.FileListAdapter.ViewHolder holder = viewHolder;
    holder.adapterRef.selectedPosition = holder.getLayoutPosition();
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
