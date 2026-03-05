package pro.sketchware.core;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.HistoryBlockBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages undo/redo history for block editing operations in the logic editor.
 * <p>
 * Each event handler has its own history stack, keyed by a composite string
 * built via {@link #buildKey(String, String, String)}. History entries are
 * {@link HistoryBlockBean} objects that record add, remove, move, and update actions.
 * <p>
 * This class is a singleton scoped by project ID ({@code scId}). Use
 * {@link #getInstance(String)} to obtain the instance and {@link #clearInstance()}
 * to release it when the project is closed.
 *
 * @see HistoryBlockBean
 */
public class BlockHistoryManager {
  public static volatile BlockHistoryManager instance;

  /** Maximum number of undo/redo steps retained per event. */
  public static int MAX_HISTORY_STEPS = 50;
  
  public Map<String, Integer> positionMap;
  
  public Map<String, ArrayList<HistoryBlockBean>> historyMap;
  
  public String scId;
  
  public BlockHistoryManager(String scId) {
    this.scId = scId;
    historyMap = new HashMap<>();
    positionMap = new HashMap<>();
  }
  
  /**
   * Builds a composite history key from file name, event name, and extra data.
   *
   * @param key   the Java filename (e.g. {@code "MainActivity.java"})
   * @param value the event/MoreBlock name (e.g. {@code "btn1_onClick"})
   * @param extra additional qualifier
   * @return the composite key in format {@code "key_value_extra"}
   */
  public static String buildKey(String key, String value, String extra) {
    return key + "_" + value + "_" + extra;
  }
  
  /**
   * Releases the singleton instance, clearing all history data.
   * Should be called when closing a project.
   */
  public static void clearInstance() {
    BlockHistoryManager currentInstance = instance;
    if (currentInstance != null) {
      currentInstance.scId = "";
      currentInstance.historyMap = null;
      currentInstance.positionMap = null;
    } 
    instance = null;
  }
  
  /**
   * Returns the singleton instance for the given project, creating one if needed.
   * Thread-safe via double-checked locking.
   *
   * @param scId the project identifier
   * @return the history manager instance
   */
  public static BlockHistoryManager getInstance(String scId) {
    if (instance == null) {
      synchronized (BlockHistoryManager.class) {
        if (instance == null || !instance.scId.equals(scId)) {
          instance = new BlockHistoryManager(scId);
        }
      }
    }
    return instance;
  }
  
  public final void trimFutureHistory(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      return; 
    ArrayList historyEntries = historyMap.get(historyKey);
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    if (historyEntries == null)
      return; 
    for (int j = historyEntries.size(); j > position; j--)
      historyEntries.remove(j - 1); 
  }
  
  /**
   * Records a single block addition to the history.
   *
   * @param historyKey        the composite history key
   * @param addedBlock        the block that was added
   * @param currentX          the X position of the block
   * @param currentY          the Y position of the block
   * @param prevParentData    the previous parent block state (before addition)
   * @param currentParentData the current parent block state (after addition)
   */
  public void recordAdd(String historyKey, BlockBean addedBlock, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    ArrayList<BlockBean> blockList = new ArrayList<>();
    blockList.add(addedBlock);
    recordAddMultiple(historyKey, blockList, currentX, currentY, prevParentData, currentParentData);
  }
  
  /**
   * Records a block parameter update to the history.
   * Skips recording if the previous and current data are equal.
   *
   * @param historyKey        the composite history key
   * @param prevUpdateData    the block state before the update
   * @param currentUpdateData the block state after the update
   */
  public void recordUpdate(String historyKey, BlockBean prevUpdateData, BlockBean currentUpdateData) {
    if (prevUpdateData.isEqual(currentUpdateData))
      return; 
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionUpdate(prevUpdateData, currentUpdateData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void addHistoryEntry(String historyKey, HistoryBlockBean historyBlockBean) {
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    ArrayList<HistoryBlockBean> historyEntries = historyMap.get(historyKey);
    historyEntries.add(historyBlockBean);
    if (historyEntries.size() > MAX_HISTORY_STEPS) {
      historyEntries.remove(0);
    } else {
      incrementPosition(historyKey);
    } 
  }
  
  public void recordAddMultiple(String historyKey, ArrayList<BlockBean> addedData, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionAdd(addedData, currentX, currentY, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  /**
   * Records a block move operation to the history, capturing position and parent changes.
   *
   * @param historyKey             the composite history key
   * @param beforeMove             the block chain state before the move
   * @param afterMove              the block chain state after the move
   * @param prevX                  the previous X position
   * @param prevY                  the previous Y position
   * @param currentX               the new X position
   * @param currentY               the new Y position
   * @param prevOriginalParent     the original parent before the move
   * @param currentOriginalParent  the original parent after the move
   * @param prevParentData         the previous parent block state
   * @param currentParentData      the current parent block state
   */
  public void recordMove(String historyKey, ArrayList<BlockBean> beforeMove, ArrayList<BlockBean> afterMove, int prevX, int prevY, int currentX, int currentY, BlockBean prevOriginalParent, BlockBean currentOriginalParent, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionMove(beforeMove, afterMove, prevX, prevY, currentX, currentY, prevOriginalParent, currentOriginalParent, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public void removeHistory(String historyKey) {
    if (historyMap.containsKey(historyKey)) {
      historyMap.remove(historyKey);
      positionMap.remove(historyKey);
    } 
  }
  
  /**
   * Records a block removal to the history.
   *
   * @param historyKey        the composite history key
   * @param removedData       the blocks that were removed
   * @param currentX          the X position at removal time
   * @param currentY          the Y position at removal time
   * @param prevParentData    the parent block state before removal
   * @param currentParentData the parent block state after removal
   */
  public void recordRemove(String historyKey, ArrayList<BlockBean> removedData, int currentX, int currentY, BlockBean prevParentData, BlockBean currentParentData) {
    HistoryBlockBean historyBlockBean = new HistoryBlockBean();
    historyBlockBean.actionRemove(removedData, currentX, currentY, prevParentData, currentParentData);
    if (!historyMap.containsKey(historyKey))
      initHistory(historyKey); 
    trimFutureHistory(historyKey);
    addHistoryEntry(historyKey, historyBlockBean);
  }
  
  public final void decrementPosition(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    if (position == 0)
      return; 
    positionMap.put(historyKey, Integer.valueOf(position - 1));
  }
  
  public final void incrementPosition(String historyKey) {
    if (!positionMap.containsKey(historyKey))
      initHistory(historyKey); 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    positionMap.put(historyKey, Integer.valueOf(position + 1));
  }
  
  public void initHistory(String historyKey) {
    historyMap.put(historyKey, new ArrayList<>());
    positionMap.put(historyKey, Integer.valueOf(0));
  }
  
  /**
   * Checks if redo is available for the given history key.
   *
   * @param historyKey the composite history key
   * @return {@code true} if there are future history entries to redo
   */
  public boolean canRedo(String historyKey) {
    return !positionMap.containsKey(historyKey) ? false : ((((Integer)positionMap.get(historyKey)).intValue() < ((ArrayList)historyMap.get(historyKey)).size()));
  }
  
  /**
   * Checks if undo is available for the given history key.
   *
   * @param historyKey the composite history key
   * @return {@code true} if there are past history entries to undo
   */
  public boolean canUndo(String historyKey) {
    return !positionMap.containsKey(historyKey) ? false : ((((Integer)positionMap.get(historyKey)).intValue() > 0));
  }
  
  /**
   * Performs a redo operation, advancing the history position forward.
   *
   * @param historyKey the composite history key
   * @return a clone of the history entry to replay, or {@code null} if redo is not available
   */
  public HistoryBlockBean redo(String historyKey) {
    if (!canRedo(historyKey))
      return null; 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    incrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)historyMap.get(historyKey)).get(position - 1 + 1)).clone();
  }
  
  /**
   * Performs an undo operation, moving the history position backward.
   *
   * @param historyKey the composite history key
   * @return a clone of the history entry to reverse, or {@code null} if undo is not available
   */
  public HistoryBlockBean undo(String historyKey) {
    if (!canUndo(historyKey))
      return null; 
    int position = ((Integer)positionMap.get(historyKey)).intValue();
    decrementPosition(historyKey);
    return ((HistoryBlockBean)((ArrayList<HistoryBlockBean>)historyMap.get(historyKey)).get(position - 1)).clone();
  }
}
