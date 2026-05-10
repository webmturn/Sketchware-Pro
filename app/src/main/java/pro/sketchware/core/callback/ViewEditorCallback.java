package pro.sketchware.core.callback;

import pro.sketchware.beans.ViewBean;

public interface ViewEditorCallback {
  void onFavoritesChanged();
  
  void onPropertyRequested(String xmlName, ViewBean viewBean);
}
