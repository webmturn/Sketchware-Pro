package pro.sketchware.core;

import com.besome.sketch.beans.ProjectLibraryBean;

public interface LibrarySettingsView {
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
  
  void setData(ProjectLibraryBean libraryBean);
}
