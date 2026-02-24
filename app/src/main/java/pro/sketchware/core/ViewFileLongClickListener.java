package pro.sketchware.core;

import android.view.View;
import android.widget.CheckBox;
import com.besome.sketch.beans.SelectableBean;
import com.besome.sketch.editor.manage.view.ManageViewActivity;

public class ViewFileLongClickListener implements View.OnLongClickListener {
  public final ViewFilesAdapter.FileListAdapter a;
  
  public final ViewFilesAdapter.FileListAdapter.ViewHolder b;
  
  public ViewFileLongClickListener(ViewFilesAdapter.FileListAdapter.ViewHolder parama, ViewFilesAdapter.FileListAdapter parama1) {
    this.a = parama1;
    this.b = parama;
  }
  
  public boolean onLongClick(View paramView) {
    ((ManageViewActivity)this.b.adapterRef.outerAdapter.getActivity()).a(true);
    ViewFilesAdapter.FileListAdapter.ViewHolder a1 = this.b;
    a1.adapterRef.selectedPosition = a1.getLayoutPosition();
    CheckBox checkBox = this.b.checkbox;
    checkBox.setChecked(checkBox.isChecked() ^ true);
    ((SelectableBean)this.b.adapterRef.outerAdapter.projectFiles.get(this.b.adapterRef.selectedPosition)).isSelected = this.b.checkbox.isChecked();
    return true;
  }
}
