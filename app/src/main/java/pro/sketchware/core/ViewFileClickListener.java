package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;

public class ViewFileClickListener implements View.OnClickListener {
  public final ViewFilesAdapter.a a;
  
  public final ViewFilesAdapter.a.ViewHolder b;
  
  public ViewFileClickListener(ViewFilesAdapter.a.ViewHolder parama, ViewFilesAdapter.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public void onClick(View paramView) {
    ViewFilesAdapter.a.ViewHolder a1 = this.b;
    a1.adapterRef.selectedPosition = a1.getLayoutPosition();
    if (this.b.adapterRef.outerAdapter.isSelectionMode.booleanValue()) {
      CheckBox checkBox = this.b.checkbox;
      checkBox.setChecked(checkBox.isChecked() ^ true);
      ((SelectableBean)this.b.adapterRef.outerAdapter.projectFiles.get(this.b.adapterRef.selectedPosition)).isSelected = this.b.checkbox.isChecked();
      ViewFilesAdapter.a a2 = this.b.adapterRef;
      a2.notifyItemChanged(a2.selectedPosition);
    } 
  }
}
