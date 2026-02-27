package com.besome.sketch.editor.logic;

import pro.sketchware.core.ClassInfo;
import pro.sketchware.core.BlockView;
import pro.sketchware.core.BaseBlockView;
import pro.sketchware.core.ComponentTypeMapper;
import pro.sketchware.core.ViewUtil;
import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.widget.RelativeLayout;
import com.besome.sketch.beans.BlockBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlockPane extends RelativeLayout {
  public Context context;
  
  public int[] locationBuffer = new int[2];
  
  public BaseBlockView activeBlock;
  
  public BlockView dragBlock;
  
  public ArrayList<Object[]> blockSnapPoints = new ArrayList();
  
  public Object[] currentSnapPoint = null;
  
  public int nextBlockId = 10;
  
  public float densityScale = ViewUtil.dpToPx(getContext(), 1.0F);
  
  public BlockPane(Context context) {
    super(context);
    init(context);
  }
  
  public BlockView findBlockById(int position) {
    return (BlockView)findViewWithTag(Integer.valueOf(position));
  }
  
  public BlockView addBlock(BlockView blockView, int start, int end) {
    getLocationOnScreen(this.locationBuffer);
    BlockView rs = blockView;
    if (blockView.getBlockType() == 1) {
      Context context = getContext();
      int blockId = this.nextBlockId;
      this.nextBlockId = blockId + 1;
      rs = new BlockView(context, blockId, blockView.spec, ((BaseBlockView)blockView).blockType, ((BaseBlockView)blockView).componentType, blockView.opCode);
    } 
    rs.blockPane = this;
    addView((View)rs);
    rs.setX((start - this.locationBuffer[0] - getPaddingLeft()));
    rs.setY((end - this.locationBuffer[1] - getPaddingTop()));
    return rs;
  }
  
  public BlockView dropBlock(BlockView blockView, int start, int end, boolean enabled) {
    BlockView rs;
    if (!enabled) {
      rs = addBlock(blockView, start, end);
    } else {
      blockView.setX((start - this.locationBuffer[0] - getPaddingLeft()));
      blockView.setY((end - this.locationBuffer[1] - getPaddingTop()));
      rs = blockView;
    } 
    Object[] objects = this.currentSnapPoint;
    if (objects == null) {
      rs.getRootBlock().layoutChain();
      updatePaneSize();
      return rs;
    } 
    if (blockView.isParameter) {
      ((BaseBlockView)objects[1]).parentBlock.replaceParameter((BaseBlockView)objects[1], rs);
    } else {
      blockView = (BlockView)objects[1];
      start = ((Integer)objects[2]).intValue();
      if (start != 0) {
        if (start != 1) {
          if (start != 2) {
            if (start != 3) {
              if (start == 4)
                blockView.positionInSubstack1(rs); 
            } else {
              blockView.setSubstack2Block(rs);
            } 
          } else {
            blockView.setSubstack1Block(rs);
          } 
        } else {
          blockView.positionAbove(rs);
        } 
      } else {
        blockView.setNextBlock(rs);
      } 
    } 
    rs.getRootBlock().layoutChain();
    updatePaneSize();
    return rs;
  }
  
  public BlockView findBlockByString(String blockId) {
    return (BlockView)findViewWithTag(Integer.valueOf(blockId));
  }
  
  public final void initActiveBlock() {
    if (this.activeBlock == null)
      this.activeBlock = new BaseBlockView(this.context, " ", true); 
    this.activeBlock.setBlockSize(10.0F, 10.0F, false);
    addView((View)this.activeBlock);
    hideActiveBlock();
  }
  
  public void computeSnapPoints(BlockView blockView) {
    boolean hasEmptySubstack;
    boolean hasEndCap = (blockView.getLastInChain()).hasEndCap;
    if (blockView.hasSubstack() && -1 == blockView.subStack1) {
      hasEmptySubstack = true;
    } else {
      hasEmptySubstack = false;
    } 
    boolean isParameter = blockView.isParameter;
    buildSnapPoints(blockView.getTag().toString(), hasEndCap, hasEmptySubstack, isParameter, blockView.getHeight(), blockView.getBlockHeight());
    this.currentSnapPoint = null;
  }
  
  public void setBlockTreeVisibility(BlockView blockView, int position) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (blockView != null) {
      Integer tag = (Integer) blockView.getTag();
      if (!visited.add(tag)) break;
      blockView.setVisibility(position);
      for (View view : blockView.childViews) {
        if (view instanceof BlockView)
          setBlockTreeVisibility((BlockView)view, position); 
      } 
      if (blockView.hasSubstack()) {
        int subStackId = blockView.subStack1;
        if (subStackId != -1 && !visited.contains(subStackId)) {
          BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(subStackId));
          if (sub != null) setBlockTreeVisibility(sub, position);
        }
      } 
      if (blockView.hasDoubleSubstack()) {
        int subStackId = blockView.subStack2;
        if (subStackId != -1 && !visited.contains(subStackId)) {
          BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(subStackId));
          if (sub != null) setBlockTreeVisibility(sub, position);
        }
      } 
      int nextBlockId = blockView.nextBlock;
      if (nextBlockId != -1) {
        blockView = (BlockView)findViewWithTag(Integer.valueOf(nextBlockId));
      } else {
        blockView = null;
      }
    } 
  }
  
  public final void collectParameterSnapPoints(BlockView blockView, String excludeBlockId) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (blockView != null) {
      Integer tag = (Integer) blockView.getTag();
      if (!visited.add(tag)) break;
      if (!blockView.isDefinitionBlock)
        for (int b = 0; b < blockView.childViews.size(); b++) {
          View view = blockView.childViews.get(b);
          boolean isBlockView = view instanceof BlockView;
          if ((isBlockView || view instanceof pro.sketchware.core.FieldBlockView) && (!isBlockView || !view.getTag().toString().equals(excludeBlockId))) {
            int[] intValues = new int[2];
            view.getLocationOnScreen(intValues);
            addSnapPoint(intValues, view, 0);
            if (isBlockView)
              collectParameterSnapPoints((BlockView)view, excludeBlockId); 
          } 
        }  
      int traverseId = blockView.subStack1;
      if (traverseId != -1 && !visited.contains(traverseId)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
        if (sub != null) collectParameterSnapPoints(sub, excludeBlockId);
      }
      traverseId = blockView.subStack2;
      if (traverseId != -1 && !visited.contains(traverseId)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
        if (sub != null) collectParameterSnapPoints(sub, excludeBlockId);
      }
      traverseId = blockView.nextBlock;
      if (traverseId != -1) {
        blockView = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
      } else {
        blockView = null;
      }
    } 
  }
  
  public void computeSnapPointsForBlocks(BlockView blockView, ArrayList<BlockBean> list) {
    if (list.size() <= 0) {
      computeSnapPoints(blockView);
      return;
    }
    BlockBean first = list.get(0);
    BlockBean last = list.get(list.size() - 1);
    String firstType = first.type;
    boolean isArgType = "b".equals(firstType) || "s".equals(firstType) || "d".equals(firstType)
        || "v".equals(firstType) || "p".equals(firstType) || "l".equals(firstType) || "a".equals(firstType);
    boolean endsWithF = "f".equals(last.type);
    boolean hasEmptySubStack = ("c".equals(first.type) || "e".equals(first.type)) && first.subStack1 <= 0;
    buildSnapPoints(first.id, endsWithF, hasEmptySubStack, isArgType, blockView.getHeight(), blockView.getBlockHeight());
    this.currentSnapPoint = null;
  }
  
  public final void collectSnapPoints(BlockView blockView, boolean enabled) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (blockView != null && blockView.getVisibility() != 8) {
      Integer tag = (Integer) blockView.getTag();
      if (!visited.add(tag)) break;
      if (!blockView.hasEndCap && (!enabled || -1 == blockView.nextBlock)) {
        int[] intValues = new int[2];
        blockView.getLocationOnScreen(intValues);
        intValues[1] = intValues[1] + blockView.getContentBottom();
        addSnapPoint(intValues, (View)blockView, 0);
      } 
      if (blockView.hasSubstack() && (!enabled || blockView.subStack1 == -1)) {
        int[] intValues = new int[2];
        blockView.getLocationOnScreen(intValues);
        intValues[0] = intValues[0] + ((BaseBlockView)blockView).cornerRadius;
        intValues[1] = intValues[1] + blockView.getBlockHeight();
        addSnapPoint(intValues, (View)blockView, 2);
      } 
      if (blockView.hasDoubleSubstack() && (!enabled || blockView.subStack2 == -1)) {
        int[] intValues = new int[2];
        blockView.getLocationOnScreen(intValues);
        intValues[0] = intValues[0] + ((BaseBlockView)blockView).cornerRadius;
        intValues[1] = intValues[1] + blockView.getSubstackBottom();
        addSnapPoint(intValues, (View)blockView, 3);
      } 
      int traverseId = blockView.subStack1;
      if (traverseId != -1 && !visited.contains(traverseId)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
        if (sub != null) collectSnapPoints(sub, enabled);
      }
      traverseId = blockView.subStack2;
      if (traverseId != -1 && !visited.contains(traverseId)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
        if (sub != null) collectSnapPoints(sub, enabled);
      }
      traverseId = blockView.nextBlock;
      if (traverseId != -1) {
        blockView = (BlockView)findViewWithTag(Integer.valueOf(traverseId));
        continue;
      } 
      break;
    } 
  }
  
  public final void init(Context context) {
    this.context = context;
    initActiveBlock();
  }
  
  public void removeBlock(BlockBean blockBean, boolean enabled) {
    String blockId = blockBean.id;
    if (blockId != null && !blockId.equals("") && !blockBean.id.equals("0")) {
      BlockView targetView = (BlockView)findViewWithTag(Integer.valueOf(blockBean.id));
      if (targetView == null)
        return; 
      BlockView parentBlockView = ((BaseBlockView)targetView).parentBlock;
      if (targetView != parentBlockView) {
        detachFromParent(targetView);
        removeView((View)targetView);
      } else {
        removeView((View)targetView);
      } 
      if (enabled && parentBlockView != null)
        parentBlockView.getRootBlock().layoutChain(); 
    } 
  }
  
  public void createHeaderBlock(String key, String value) {
    this.dragBlock = new BlockView(getContext(), 0, key, "h", value);
    BlockView rs = this.dragBlock;
    rs.blockPane = this;
    addView((View)rs);
    float padding = ViewUtil.dpToPx(getContext(), 1.0F);
    rs = this.dragBlock;
    padding *= 8.0F;
    rs.setX(padding);
    this.dragBlock.setY(padding);
  }
  
  public void buildSnapPoints(String excludeBlockId, boolean endsWithF, boolean hasEmptySubStack, boolean isArgType, int start, int end) {
    this.blockSnapPoints = new ArrayList();
    int snapMargin = (int)(this.densityScale * 3.0F);
    for (int b = 0; b < getChildCount(); b++) {
      View view = getChildAt(b);
      if (view instanceof BlockView) {
        BlockView rs = (BlockView)view;
        if (rs.getVisibility() != 8 && ((BaseBlockView)rs).parentBlock == null)
          if (isArgType) {
            collectParameterSnapPoints(rs, excludeBlockId);
          } else if (!rs.isParameter) {
            boolean enableChildren = true;
            if (!endsWithF && !rs.isDefinitionBlock) {
              int[] intValues = new int[2];
              rs.getLocationOnScreen(intValues);
              intValues[1] = intValues[1] - start - snapMargin;
              addSnapPoint(intValues, (View)rs, 1);
            } 
            if (hasEmptySubStack && !rs.isDefinitionBlock) {
              int[] intValues = new int[2];
              rs.getLocationOnScreen(intValues);
              intValues[0] = intValues[0] - ((BaseBlockView)rs).cornerRadius;
              intValues[1] = intValues[1] - end - snapMargin;
              addSnapPoint(intValues, (View)rs, 4);
            } 
            if (!endsWithF || hasEmptySubStack)
              enableChildren = false; 
            collectSnapPoints(rs, enableChildren);
          }  
      } 
    } 
  }
  
  public final void addSnapPoint(int[] values, View view, int position) {
    this.blockSnapPoints.add(new Object[] { values, view, Integer.valueOf(position) });
  }
  
  public final boolean isCompatibleBlock(BlockView blockView, View view) {
    if (!blockView.isParameter)
      return true; 
    if (view instanceof BaseBlockView) {
      if (((BaseBlockView)view).componentType.equals("!"))
        return true; 
      ClassInfo gx1 = blockView.getClassInfo();
      if (gx1 == null)
        return false; 
      ClassInfo gx2 = ((BaseBlockView)view).getClassInfo();
      if (gx2 == null)
        return false; 
      if (gx1.isAssignableFrom(gx2))
        return true; 
      if (view instanceof BlockView && gx1.matchesType(ComponentTypeMapper.getInternalTypeName(((BlockView)view).componentTypeStr)))
        return true; 
    } 
    return false;
  }
  
  public void updatePaneSize() {
    int childCount = getChildCount();
    int maxWidth = (getLayoutParams()).width;
    int maxHeight = (getLayoutParams()).width;
    int childIdx = 0;
    while (childIdx < childCount) {
      View view = getChildAt(childIdx);
      int newWidth = maxWidth;
      int newHeight = maxHeight;
      if (view instanceof BlockView) {
        BlockView rs = (BlockView)view;
        if (rs.parentBlock == null) {
          float f = view.getX();
          newWidth = Math.max((int)(f + rs.getWidthSum()) + 150, maxWidth);
          newHeight = Math.max((int)(view.getY() + rs.getHeightSum()) + 150, maxHeight);
        }
      } 
      childIdx++;
      maxWidth = newWidth;
      maxHeight = newHeight;
    } 
    (getLayoutParams()).width = maxWidth;
    (getLayoutParams()).height = maxHeight;
  }
  
  public void removeBlockTree(BlockView blockView) {
    detachFromParent(blockView);
    Iterator<BlockView> iterator = blockView.getAllChildren().iterator();
    while (iterator.hasNext())
      removeView((View)iterator.next()); 
  }
  
  private static final Map<String, Integer> LIST_REF_PARAM_INDEX = new HashMap<>();
  static {
    LIST_REF_PARAM_INDEX.put("getVar", -1);
    for (String op : new String[]{"lengthList", "containListInt", "containListStr",
        "containListMap", "clearList", "listMapToStr"})
      LIST_REF_PARAM_INDEX.put(op, 0);
    for (String op : new String[]{"addListInt", "addListStr", "getAtListInt", "getAtListStr",
        "indexListInt", "indexListStr", "deleteList", "spnSetData", "listSetData",
        "listSetCustomViewData", "strToListMap"})
      LIST_REF_PARAM_INDEX.put(op, 1);
    for (String op : new String[]{"insertListInt", "insertListStr", "addListMap", "getAtListMap"})
      LIST_REF_PARAM_INDEX.put(op, 2);
    for (String op : new String[]{"insertListMap", "setListMap"})
      LIST_REF_PARAM_INDEX.put(op, 3);
  }

  public boolean hasListReference(String listName) {
    int childCount = getChildCount();
    for (int childIdx = 0; childIdx < childCount; childIdx++) {
      View view = getChildAt(childIdx);
      if (view instanceof BlockView) {
        BlockBean blockBean = ((BlockView)view).getBean();
        Integer paramIndex = LIST_REF_PARAM_INDEX.get(blockBean.opCode);
        if (paramIndex == null) continue;
        if (paramIndex == -1) {
          if (blockBean.spec.equals(listName)) return true;
        } else {
          if (((String)blockBean.parameters.get(paramIndex)).equals(listName)) return true;
        }
      } 
    } 
    return false;
  }
  
  public Object[] findNearestSnapPoint(BlockView blockView, int start, int end) {
    byte snapThreshold;
    Object[] objects = null;
    if (blockView.isParameter) {
      snapThreshold = 40;
    } else {
      snapThreshold = 60;
    } 
    int minDistance = 100000;
    Point touchPoint = new Point(start, end);
    start = 0;
    end = minDistance;
    while (start < this.blockSnapPoints.size()) {
      Object[] snapEntry = this.blockSnapPoints.get(start);
      int[] intValues = (int[])snapEntry[0];
      int dx = touchPoint.x - intValues[0];
      int dy = touchPoint.y - intValues[1];
      int distance = Math.abs(dx / 2) + Math.abs(dy);
      if (distance < end && distance < snapThreshold) {
        if (isCompatibleBlock(blockView, (View)snapEntry[1])) {
          objects = snapEntry;
          end = distance;
        } 
      } 
      start++;
    } 
    return objects;
  }
  
  public void clearSnapState() {
    hideActiveBlock();
    this.blockSnapPoints = new ArrayList();
    this.currentSnapPoint = null;
  }
  
  public void detachFromParent(BlockView blockView) {
    BlockView rs = ((BaseBlockView)blockView).parentBlock;
    if (rs == null)
      return; 
    if (rs != null) {
      rs.detachBlock(blockView);
      ((BaseBlockView)blockView).parentBlock = null;
    } 
  }
  
  public void updateDragPreview(BlockView blockView, int start, int end) {
    getLocationOnScreen(this.locationBuffer);
    this.currentSnapPoint = findNearestSnapPoint(blockView, start, end);
    boolean hasSubstack = blockView.hasSubstack();
    boolean showNotch = true;
    if (hasSubstack && -1 == blockView.subStack1) {
      Object[] snapEntry = this.currentSnapPoint;
      if (snapEntry != null) {
        BlockView rs = (BlockView)snapEntry[1];
        start = ((Integer)snapEntry[2]).intValue();
        if (start != 0) {
          if (start != 2) {
            if (start == 3)
              rs = (BlockView)findViewWithTag(Integer.valueOf(rs.subStack2)); 
          } else {
            rs = (BlockView)findViewWithTag(Integer.valueOf(rs.subStack1));
          } 
        } else {
          rs = (BlockView)findViewWithTag(Integer.valueOf(rs.nextBlock));
        } 
      } 
    } 
    Object[] objects = this.currentSnapPoint;
    if (objects != null) {
      int[] intValues = (int[])objects[0];
      View view = (View)objects[1];
      this.activeBlock.setX((intValues[0] - this.locationBuffer[0]));
      this.activeBlock.setY((intValues[1] - this.locationBuffer[1]));
      this.activeBlock.bringToFront();
      this.activeBlock.setVisibility(0);
      if (blockView.isParameter) {
        if (view instanceof BlockView)
          this.activeBlock.copyBlockDimensions((BaseBlockView)view, true, false, 0); 
        if (view instanceof pro.sketchware.core.FieldBlockView)
          this.activeBlock.copyBlockDimensions((BaseBlockView)view, true, false, 0); 
      } else {
        end = ((Integer)this.currentSnapPoint[2]).intValue();
        if (end == 4) {
          start = ((BlockView)view).getHeightSum();
        } else {
          start = 0;
        } 
        if (end == 1 || end == 4)
          showNotch = false; 
        this.activeBlock.copyBlockDimensions((BaseBlockView)blockView, false, showNotch, start);
      } 
    } else {
      hideActiveBlock();
    } 
  }
  
  private static final Map<String, Integer> MAP_REF_PARAM_INDEX = new HashMap<>();
  static {
    MAP_REF_PARAM_INDEX.put("getVar", -1);
    for (String op : new String[]{"setVarBoolean", "setVarInt", "setVarString",
        "increaseInt", "decreaseInt", "mapCreateNew", "mapPut", "mapGet", "mapContainKey",
        "mapRemoveKey", "mapSize", "mapClear", "mapIsEmpty", "mapGetAllKeys",
        "addListMap", "insertListMap", "mapToStr"})
      MAP_REF_PARAM_INDEX.put(op, 0);
    MAP_REF_PARAM_INDEX.put("strToMap", 1);
    MAP_REF_PARAM_INDEX.put("getAtListMap", 2);
  }

  public boolean hasMapReference(String mapName) {
    int childCount = getChildCount();
    for (int childIdx = 0; childIdx < childCount; childIdx++) {
      View view = getChildAt(childIdx);
      if (view instanceof BlockView) {
        BlockBean blockBean = ((BlockView)view).getBean();
        Integer paramIndex = MAP_REF_PARAM_INDEX.get(blockBean.opCode);
        if (paramIndex == null) continue;
        if (paramIndex == -1) {
          if (blockBean.spec.equals(mapName)) return true;
        } else {
          if (((String)blockBean.parameters.get(paramIndex)).equals(mapName)) return true;
        }
      } 
    } 
    return false;
  }
  
  public void hideActiveBlock() {
    this.activeBlock.setVisibility(8);
  }
  
  public int getAddTargetId() {
    Object[] target = getNearestTarget();
    int result = -1;
    if (target == null || target[2] == null) return result;
    int type = ((Integer) target[2]).intValue();
    if (type != 0 && type != 2 && type != 3 && type != 5) return result;
    if (target[1] == null) return result;
    View view = (View) target[1];
    if (view instanceof BlockView) {
      BlockView rs = (BlockView) view;
      if (rs.isParameter) {
        result = ((Integer) rs.parentBlock.getTag()).intValue();
      } else {
        result = ((Integer) ((BlockView) target[1]).getTag()).intValue();
      }
    }
    if (view instanceof pro.sketchware.core.FieldBlockView) {
      result = ((Integer) ((pro.sketchware.core.FieldBlockView) view).parentBlock.getTag()).intValue();
    }
    return result;
  }
  
  public ArrayList<BlockBean> getBlocks() {
    ArrayList<BlockBean> blocks = new ArrayList();
    BlockView rs = (BlockView)findViewWithTag(Integer.valueOf(this.dragBlock.nextBlock));
    if (rs != null) {
      Iterator<BlockView> iterator = rs.getAllChildren().iterator();
      while (iterator.hasNext())
        blocks.add(((BlockView)iterator.next()).getBean()); 
    } 
    return blocks;
  }
  
  public Object[] getNearestTarget() {
    return this.currentSnapPoint;
  }
  
  public BlockView getRoot() {
    return this.dragBlock;
  }
}
