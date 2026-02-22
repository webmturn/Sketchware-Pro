package a.a.a;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.manage.view.PresetSettingActivity;

public class ww implements View.OnClickListener {
  public final xw.a a;
  
  public final xw.a.ViewHolder b;
  
  public ww(xw.a.ViewHolder parama, xw.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public void onClick(View paramView) {
    char c;
    if (mB.a())
      return; 
    xw.a.ViewHolder a1 = this.b;
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


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\ww.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */