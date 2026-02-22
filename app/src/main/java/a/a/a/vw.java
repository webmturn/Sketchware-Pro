package a.a.a;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class vw implements View.OnLongClickListener {
  public final xw.a a;
  
  public final xw.a.ViewHolder b;
  
  public vw(xw.a.ViewHolder parama, xw.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public boolean onLongClick(View paramView) {
    ((ManageViewActivity)this.b.z.d.getActivity()).a(true);
    xw.a.ViewHolder a1 = this.b;
    a1.z.c = a1.getLayoutPosition();
    CheckBox checkBox = this.b.t;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)this.b.z.d.h.get(this.b.z.c)).isSelected = this.b.t.isChecked();
    return true;
  }
}


/* Location:              C:\Users\Administrator\IdeaProjects\Sketchware-Pro\app\libs\a.a.a-notimportant-classes.jar!\a\a\a\vw.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       1.1.3
 */