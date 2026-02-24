package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileLongClickListener implements View.OnLongClickListener {
  public final ViewFilesAdapter.a a;
  
  public final ViewFilesAdapter.a.ViewHolder b;
  
  public ViewFileLongClickListener(ViewFilesAdapter.a.ViewHolder parama, ViewFilesAdapter.a parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public boolean onLongClick(View paramView) {
    ((ManageViewActivity)this.b.adapterRef.outerAdapter.getActivity()).a(true);
    ViewFilesAdapter.a.ViewHolder a1 = this.b;
    a1.adapterRef.selectedPosition = a1.getLayoutPosition();
    CheckBox checkBox = this.b.checkbox;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)this.b.adapterRef.outerAdapter.projectFiles.get(this.b.adapterRef.selectedPosition)).isSelected = this.b.checkbox.isChecked();
    return true;
  }
}
