package a.a.a;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;

public class uw implements View.OnClickListener {
  public final xw.a a;
  
  public final xw.a.ViewHolder b;
  
  public uw(xw.a.ViewHolder parama, xw.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public void onClick(View paramView) {
    xw.a.ViewHolder a1 = this.b;
    a1.z.c = a1.getLayoutPosition();
    if (this.b.z.d.j.booleanValue()) {
      CheckBox checkBox = this.b.t;
      checkBox.setChecked(checkBox.isChecked() ^ true);
      ((SelectableBean)this.b.z.d.h.get(this.b.z.c)).isSelected = this.b.t.isChecked();
      xw.a a2 = this.b.z;
      a2.notifyItemChanged(a2.c);
    } 
  }
}
