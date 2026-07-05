package pro.sketchware.activities.editor.manage.library.firebase;

import pro.sketchware.beans.ProjectLibraryBean;

public interface LibraryConfigView {
  void onSave();
  
  void saveToBean(ProjectLibraryBean libraryBean);
  
  String getDocUrl();
  
  boolean isValid();
}
