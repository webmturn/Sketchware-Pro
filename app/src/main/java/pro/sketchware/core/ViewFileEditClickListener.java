package pro.sketchware.core;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;

public class ViewFileEditClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.a a;
  
  public final ViewFilesAdapter.a.ViewHolder b;
  
  public ViewFileEditClickListener(ViewFilesAdapter.a.ViewHolder parama, ViewFilesAdapter.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public void onClick(View paramView) {
    char c;
    if (UIHelper.a())
      return; 
    ViewFilesAdapter.a.ViewHolder a1 = this.b;
    a1.z.c = a1.getLayoutPosition();
    Intent intent = new Intent((Context)this.b.z.d.getActivity(), PresetSettingActivity.class);
    if (((ProjectFileBean)this.b.z.d.h.get(this.b.z.c)).fileType == 1) {
      c = 'ĕ';
    } else {
      c = 'Ė';
    } 
    intent.putExtra("request_code", c);
    intent.putExtra("edit_mode", true);
    this.b.z.d.startActivityForResult(intent, c);
  }
}
