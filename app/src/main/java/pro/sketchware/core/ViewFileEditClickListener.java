package pro.sketchware.core;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;

public class ViewFileEditClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.FileListAdapter a;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder b;
  
  public ViewFileEditClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder parama, ViewFilesAdapter.FileListAdapter parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public void onClick(View paramView) {
    char c;
    if (UIHelper.isClickThrottled())
      return; 
    ViewFilesAdapter.FileListAdapter.ViewHolder a1 = this.b;
    a1.adapterRef.selectedPosition = a1.getLayoutPosition();
    Intent intent = new Intent((Context)this.b.adapterRef.outerAdapter.getActivity(), PresetSettingActivity.class);
    if (((ProjectFileBean)this.b.adapterRef.outerAdapter.projectFiles.get(this.b.adapterRef.selectedPosition)).fileType == 1) {
      c = 'ĕ';
    } else {
      c = 'Ė';
    } 
    intent.putExtra("request_code", c);
    intent.putExtra("edit_mode", true);
    this.b.adapterRef.outerAdapter.startActivityForResult(intent, c);
  }
}
