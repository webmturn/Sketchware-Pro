package pro.sketchware.core;

import com.besome.sketch.beans.ProjectLibraryBean;

public interface LibraryConfigView {
  void onSave();
  
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
}
