package pro.sketchware.core;

public interface BuildCallback {
  void onSelectionChanged();
  
  void onViewSelected(String paramString);
  
  void onViewSelectedWithProperty(boolean paramBoolean, String paramString);
}
