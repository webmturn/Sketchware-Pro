package pro.sketchware.core.callback;

import com.besome.sketch.beans.ViewBean;

public interface ViewEditorCallback {
  void onFavoritesChanged();
  
  void onPropertyRequested(String xmlName, ViewBean viewBean);
}
