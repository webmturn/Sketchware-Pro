package pro.sketchware.core.ui;

import com.besome.sketch.beans.ProjectLibraryBean;

public interface LibraryConfigView {
  void onSave();
  
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
}
