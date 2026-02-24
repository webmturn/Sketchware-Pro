package pro.sketchware.core;

public interface BuildCallback {
  void onSelectionChanged();
  
  void onViewSelected(String input);
  
  void onViewSelectedWithProperty(boolean flag, String input);
}
