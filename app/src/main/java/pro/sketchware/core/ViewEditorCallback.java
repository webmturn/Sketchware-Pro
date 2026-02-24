package pro.sketchware.core;

import com.besome.sketch.beans.ViewBean;

public interface ViewEditorCallback {
  void onFavoritesChanged();
  
  void onPropertyRequested(String input, ViewBean viewBean);
}
