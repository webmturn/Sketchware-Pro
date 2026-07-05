package pro.sketchware.core.block.view;

public interface BlockViewContainer {
  BlockView getBlockByTag(int id);

  void updatePaneSize();
}
