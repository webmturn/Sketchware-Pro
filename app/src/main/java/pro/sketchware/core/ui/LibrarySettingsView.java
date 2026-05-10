package pro.sketchware.core.ui;

import pro.sketchware.beans.ProjectLibraryBean;

public interface LibrarySettingsView {
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
  
  void setData(ProjectLibraryBean libraryBean);
}
