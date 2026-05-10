package pro.sketchware.core.ui;

import pro.sketchware.beans.ProjectLibraryBean;

public interface LibraryConfigView {
  void onSave();
  
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
}
