package pro.sketchware.core;

import com.besome.sketch.beans.ProjectLibraryBean;

public interface LibrarySettingsView {
  void a(ProjectLibraryBean paramProjectLibraryBean);
  
  String getDocUrl();
  
  boolean isValid();
  
  void setData(ProjectLibraryBean paramProjectLibraryBean);
}
