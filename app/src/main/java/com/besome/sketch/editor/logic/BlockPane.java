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
import java.util.Iterator;

public class BlockPane extends RelativeLayout {
  public Context context;
  
  public int[] locationBuffer = new int[2];
  
  public BaseBlockView activeBlock;
  
  public BlockView dragBlock;
  
  public ArrayList<Object[]> blockSnapPoints = new ArrayList();
  
  public Object[] currentSnapPoint = null;
  
  public int nextBlockId = 10;
  
  public float densityScale = ViewUtil.dpToPx(getContext(), 1.0F);
  
  public BlockPane(Context paramContext) {
    super(paramContext);
    init(paramContext);
  }
  
  public BlockView findBlockById(int paramInt) {
    return (BlockView)findViewWithTag(Integer.valueOf(paramInt));
  }
  
  public BlockView addBlock(BlockView paramRs, int paramInt1, int paramInt2) {
    getLocationOnScreen(this.locationBuffer);
    BlockView rs = paramRs;
    if (paramRs.getBlockType() == 1) {
      Context context = getContext();
      int i = this.nextBlockId;
      this.nextBlockId = i + 1;
      rs = new BlockView(context, i, paramRs.spec, ((BaseBlockView)paramRs).blockType, ((BaseBlockView)paramRs).componentType, paramRs.opCode);
    } 
    rs.blockPane = this;
    addView((View)rs);
    rs.setX((paramInt1 - this.locationBuffer[0] - getPaddingLeft()));
    rs.setY((paramInt2 - this.locationBuffer[1] - getPaddingTop()));
    return rs;
  }
  
  public BlockView dropBlock(BlockView paramRs, int paramInt1, int paramInt2, boolean paramBoolean) {
    BlockView rs;
    if (!paramBoolean) {
      rs = addBlock(paramRs, paramInt1, paramInt2);
    } else {
      paramRs.setX((paramInt1 - this.locationBuffer[0] - getPaddingLeft()));
      paramRs.setY((paramInt2 - this.locationBuffer[1] - getPaddingTop()));
      rs = paramRs;
    } 
    Object[] arrayOfObject = this.currentSnapPoint;
    if (arrayOfObject == null) {
      rs.getRootBlock().layoutChain();
      updatePaneSize();
      return rs;
    } 
    if (paramRs.isParameter) {
      ((BaseBlockView)arrayOfObject[1]).parentBlock.replaceParameter((BaseBlockView)arrayOfObject[1], rs);
    } else {
      paramRs = (BlockView)arrayOfObject[1];
      paramInt1 = ((Integer)arrayOfObject[2]).intValue();
      if (paramInt1 != 0) {
        if (paramInt1 != 1) {
          if (paramInt1 != 2) {
            if (paramInt1 != 3) {
              if (paramInt1 == 4)
                paramRs.positionInSubstack1(rs); 
            } else {
              paramRs.setSubstack2Block(rs);
            } 
          } else {
            paramRs.setSubstack1Block(rs);
          } 
        } else {
          paramRs.positionAbove(rs);
        } 
      } else {
        paramRs.setNextBlock(rs);
      } 
    } 
    rs.getRootBlock().layoutChain();
    updatePaneSize();
    return rs;
  }
  
  public BlockView findBlockByString(String paramString) {
    return (BlockView)findViewWithTag(Integer.valueOf(paramString));
  }
  
  public final void initActiveBlock() {
    if (this.activeBlock == null)
      this.activeBlock = new BaseBlockView(this.context, " ", true); 
    this.activeBlock.setBlockSize(10.0F, 10.0F, false);
    addView((View)this.activeBlock);
    hideActiveBlock();
  }
  
  public void computeSnapPoints(BlockView paramRs) {
    boolean bool;
    boolean bool1 = (paramRs.getLastInChain()).hasEndCap;
    if (paramRs.hasSubstack() && -1 == paramRs.subStack1) {
      bool = true;
    } else {
      bool = false;
    } 
    boolean bool2 = paramRs.isParameter;
    buildSnapPoints(paramRs.getTag().toString(), bool1, bool, bool2, paramRs.getHeight(), paramRs.getBlockHeight());
    this.currentSnapPoint = null;
  }
  
  public void setBlockTreeVisibility(BlockView paramRs, int paramInt) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (paramRs != null) {
      Integer tag = (Integer) paramRs.getTag();
      if (!visited.add(tag)) break;
      paramRs.setVisibility(paramInt);
      for (View view : paramRs.childViews) {
        if (view instanceof BlockView)
          setBlockTreeVisibility((BlockView)view, paramInt); 
      } 
      if (paramRs.hasSubstack()) {
        int j = paramRs.subStack1;
        if (j != -1 && !visited.contains(j)) {
          BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(j));
          if (sub != null) setBlockTreeVisibility(sub, paramInt);
        }
      } 
      if (paramRs.hasDoubleSubstack()) {
        int j = paramRs.subStack2;
        if (j != -1 && !visited.contains(j)) {
          BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(j));
          if (sub != null) setBlockTreeVisibility(sub, paramInt);
        }
      } 
      int i = paramRs.nextBlock;
      if (i != -1) {
        paramRs = (BlockView)findViewWithTag(Integer.valueOf(i));
      } else {
        paramRs = null;
      }
    } 
  }
  
  public final void collectParameterSnapPoints(BlockView paramRs, String paramString) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (paramRs != null) {
      Integer tag = (Integer) paramRs.getTag();
      if (!visited.add(tag)) break;
      if (!paramRs.isDefinitionBlock)
        for (int b = 0; b < paramRs.childViews.size(); b++) {
          View view = paramRs.childViews.get(b);
          boolean bool = view instanceof BlockView;
          if ((bool || view instanceof pro.sketchware.core.FieldBlockView) && (!bool || !view.getTag().toString().equals(paramString))) {
            int[] arrayOfInt = new int[2];
            view.getLocationOnScreen(arrayOfInt);
            addSnapPoint(arrayOfInt, view, 0);
            if (bool)
              collectParameterSnapPoints((BlockView)view, paramString); 
          } 
        }  
      int i = paramRs.subStack1;
      if (i != -1 && !visited.contains(i)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(i));
        if (sub != null) collectParameterSnapPoints(sub, paramString);
      }
      i = paramRs.subStack2;
      if (i != -1 && !visited.contains(i)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(i));
        if (sub != null) collectParameterSnapPoints(sub, paramString);
      }
      i = paramRs.nextBlock;
      if (i != -1) {
        paramRs = (BlockView)findViewWithTag(Integer.valueOf(i));
      } else {
        paramRs = null;
      }
    } 
  }
  
  public void computeSnapPointsForBlocks(BlockView paramRs, ArrayList<BlockBean> paramArrayList) {
    if (paramArrayList.size() <= 0) {
      computeSnapPoints(paramRs);
      return;
    }
    BlockBean first = paramArrayList.get(0);
    BlockBean last = paramArrayList.get(paramArrayList.size() - 1);
    String firstType = first.type;
    boolean isArgType = "b".equals(firstType) || "s".equals(firstType) || "d".equals(firstType)
        || "v".equals(firstType) || "p".equals(firstType) || "l".equals(firstType) || "a".equals(firstType);
    boolean endsWithF = "f".equals(last.type);
    boolean hasEmptySubStack = ("c".equals(first.type) || "e".equals(first.type)) && first.subStack1 <= 0;
    buildSnapPoints(first.id, endsWithF, hasEmptySubStack, isArgType, paramRs.getHeight(), paramRs.getBlockHeight());
    this.currentSnapPoint = null;
  }
  
  public final void collectSnapPoints(BlockView paramRs, boolean paramBoolean) {
    java.util.HashSet<Integer> visited = new java.util.HashSet<>();
    while (paramRs != null && paramRs.getVisibility() != 8) {
      Integer tag = (Integer) paramRs.getTag();
      if (!visited.add(tag)) break;
      if (!paramRs.hasEndCap && (!paramBoolean || -1 == paramRs.nextBlock)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[1] = arrayOfInt[1] + paramRs.getContentBottom();
        addSnapPoint(arrayOfInt, (View)paramRs, 0);
      } 
      if (paramRs.hasSubstack() && (!paramBoolean || paramRs.subStack1 == -1)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[0] = arrayOfInt[0] + ((BaseBlockView)paramRs).cornerRadius;
        arrayOfInt[1] = arrayOfInt[1] + paramRs.getBlockHeight();
        addSnapPoint(arrayOfInt, (View)paramRs, 2);
      } 
      if (paramRs.hasDoubleSubstack() && (!paramBoolean || paramRs.subStack2 == -1)) {
        int[] arrayOfInt = new int[2];
        paramRs.getLocationOnScreen(arrayOfInt);
        arrayOfInt[0] = arrayOfInt[0] + ((BaseBlockView)paramRs).cornerRadius;
        arrayOfInt[1] = arrayOfInt[1] + paramRs.getSubstackBottom();
        addSnapPoint(arrayOfInt, (View)paramRs, 3);
      } 
      int i = paramRs.subStack1;
      if (i != -1 && !visited.contains(i)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(i));
        if (sub != null) collectSnapPoints(sub, paramBoolean);
      }
      i = paramRs.subStack2;
      if (i != -1 && !visited.contains(i)) {
        BlockView sub = (BlockView)findViewWithTag(Integer.valueOf(i));
        if (sub != null) collectSnapPoints(sub, paramBoolean);
      }
      i = paramRs.nextBlock;
      if (i != -1) {
        paramRs = (BlockView)findViewWithTag(Integer.valueOf(i));
        continue;
      } 
      break;
    } 
  }
  
  public final void init(Context paramContext) {
    this.context = paramContext;
    initActiveBlock();
  }
  
  public void removeBlock(BlockBean paramBlockBean, boolean paramBoolean) {
    String str = paramBlockBean.id;
    if (str != null && !str.equals("") && !paramBlockBean.id.equals("0")) {
      BlockView rs1 = (BlockView)findViewWithTag(Integer.valueOf(paramBlockBean.id));
      if (rs1 == null)
        return; 
      BlockView rs2 = ((BaseBlockView)rs1).parentBlock;
      if (rs1 != rs2) {
        detachFromParent(rs1);
        removeView((View)rs1);
      } else {
        removeView((View)rs1);
      } 
      if (paramBoolean && rs2 != null)
        rs2.getRootBlock().layoutChain(); 
    } 
  }
  
  public void createHeaderBlock(String paramString1, String paramString2) {
    this.dragBlock = new BlockView(getContext(), 0, paramString1, "h", paramString2);
    BlockView rs = this.dragBlock;
    rs.blockPane = this;
    addView((View)rs);
    float f = ViewUtil.dpToPx(getContext(), 1.0F);
    rs = this.dragBlock;
    f *= 8.0F;
    rs.setX(f);
    this.dragBlock.setY(f);
  }
  
  public void buildSnapPoints(String paramString, boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, int paramInt1, int paramInt2) {
    this.blockSnapPoints = new ArrayList();
    int i = (int)(this.densityScale * 3.0F);
    for (int b = 0; b < getChildCount(); b++) {
      View view = getChildAt(b);
      if (view instanceof BlockView) {
        BlockView rs = (BlockView)view;
        if (rs.getVisibility() != 8 && ((BaseBlockView)rs).parentBlock == null)
          if (paramBoolean3) {
            collectParameterSnapPoints(rs, paramString);
          } else if (!rs.isParameter) {
            boolean bool = true;
            if (!paramBoolean1 && !rs.isDefinitionBlock) {
              int[] arrayOfInt = new int[2];
              rs.getLocationOnScreen(arrayOfInt);
              arrayOfInt[1] = arrayOfInt[1] - paramInt1 - i;
              addSnapPoint(arrayOfInt, (View)rs, 1);
            } 
            if (paramBoolean2 && !rs.isDefinitionBlock) {
              int[] arrayOfInt = new int[2];
              rs.getLocationOnScreen(arrayOfInt);
              arrayOfInt[0] = arrayOfInt[0] - ((BaseBlockView)rs).cornerRadius;
              arrayOfInt[1] = arrayOfInt[1] - paramInt2 - i;
              addSnapPoint(arrayOfInt, (View)rs, 4);
            } 
            if (!paramBoolean1 || paramBoolean2)
              bool = false; 
            collectSnapPoints(rs, bool);
          }  
      } 
    } 
  }
  
  public final void addSnapPoint(int[] paramArrayOfint, View paramView, int paramInt) {
    this.blockSnapPoints.add(new Object[] { paramArrayOfint, paramView, Integer.valueOf(paramInt) });
  }
  
  public final boolean isCompatibleBlock(BlockView paramRs, View paramView) {
    if (!paramRs.isParameter)
      return true; 
    if (paramView instanceof BaseBlockView) {
      if (((BaseBlockView)paramView).componentType.equals("!"))
        return true; 
      ClassInfo gx1 = paramRs.getClassInfo();
      if (gx1 == null)
        return false; 
      ClassInfo gx2 = ((BaseBlockView)paramView).getClassInfo();
      if (gx2 == null)
        return false; 
      if (gx1.isAssignableFrom(gx2))
        return true; 
      if (paramView instanceof BlockView && gx1.matchesType(ComponentTypeMapper.getInternalTypeName(((BlockView)paramView).componentTypeStr)))
        return true; 
    } 
    return false;
  }
  
  public void updatePaneSize() {
    int i = getChildCount();
    int j = (getLayoutParams()).width;
    int k = (getLayoutParams()).width;
    int b = 0;
    while (b < i) {
      View view = getChildAt(b);
      int m = j;
      int n = k;
      if (view instanceof BlockView) {
        BlockView rs = (BlockView)view;
        if (rs.parentBlock == null) {
          float f = view.getX();
          m = Math.max((int)(f + rs.getWidthSum()) + 150, j);
          n = Math.max((int)(view.getY() + rs.getHeightSum()) + 150, k);
        }
      } 
      b++;
      j = m;
      k = n;
    } 
    (getLayoutParams()).width = j;
    (getLayoutParams()).height = k;
  }
  
  public void removeBlockTree(BlockView paramRs) {
    detachFromParent(paramRs);
    Iterator<BlockView> iterator = paramRs.getAllChildren().iterator();
    while (iterator.hasNext())
      removeView((View)iterator.next()); 
  }
  
  public boolean hasListReference(String paramString) {
    int i = getChildCount();
    for (int b = 0; b < i; b++) {
      View view = getChildAt(b);
      if (view instanceof BlockView) {
        byte b1 = -1;
        BlockBean blockBean = ((BlockView)view).getBean();
        String str = blockBean.opCode;
        switch (str.hashCode()) {
          default:
            b1 = -1;
            break;
          case 2090189010:
            if (str.equals("addListStr")) {
              b1 = 8;
              break;
            } 
          case 2090182653:
            if (str.equals("addListMap")) {
              b1 = 20;
              break;
            } 
          case 2090179216:
            if (str.equals("addListInt")) {
              b1 = 7;
              break;
            } 
          case 1764351209:
            if (str.equals("deleteList")) {
              b1 = 13;
              break;
            } 
          case 1252547704:
            if (str.equals("listMapToStr")) {
              b1 = 6;
              break;
            } 
          case 1160674468:
            if (str.equals("lengthList")) {
              b1 = 1;
              break;
            } 
          case 762292097:
            if (str.equals("indexListStr")) {
              b1 = 12;
              break;
            } 
          case 762282303:
            if (str.equals("indexListInt")) {
              b1 = 11;
              break;
            } 
          case 389111867:
            if (str.equals("spnSetData")) {
              b1 = 14;
              break;
            } 
          case 134874756:
            if (str.equals("listSetCustomViewData")) {
              b1 = 16;
              break;
            } 
          case -96303809:
            if (str.equals("containListStr")) {
              b1 = 3;
              break;
            } 
          case -96310166:
            if (str.equals("containListMap")) {
              b1 = 4;
              break;
            } 
          case -96313603:
            if (str.equals("containListInt")) {
              b1 = 2;
              break;
            } 
          case -329552966:
            if (str.equals("insertListStr")) {
              b1 = 19;
              break;
            } 
          case -329559323:
            if (str.equals("insertListMap")) {
              b1 = 22;
              break;
            } 
          case -329562760:
            if (str.equals("insertListInt")) {
              b1 = 18;
              break;
            } 
          case -733318734:
            if (str.equals("strToListMap")) {
              b1 = 17;
              break;
            } 
          case -1139353316:
            if (str.equals("setListMap")) {
              b1 = 23;
              break;
            } 
          case -1249347599:
            if (str.equals("getVar")) {
              b1 = 0;
              break;
            } 
          case -1271141237:
            if (str.equals("clearList")) {
              b1 = 5;
              break;
            } 
          case -1384851894:
            if (str.equals("getAtListStr")) {
              b1 = 10;
              break;
            } 
          case -1384858251:
            if (str.equals("getAtListMap")) {
              b1 = 21;
              break;
            } 
          case -1384861688:
            if (str.equals("getAtListInt")) {
              b1 = 9;
              break;
            } 
          case -1998407506:
            if (str.equals("listSetData")) {
              b1 = 15;
              break;
            } 
        } 
        switch (b1) {
          case 22:
          case 23:
            if (((String)blockBean.parameters.get(3)).equals(paramString))
              return true; 
            break;
          case 18:
          case 19:
          case 20:
          case 21:
            if (((String)blockBean.parameters.get(2)).equals(paramString))
              return true; 
            break;
          case 7:
          case 8:
          case 9:
          case 10:
          case 11:
          case 12:
          case 13:
          case 14:
          case 15:
          case 16:
          case 17:
            if (((String)blockBean.parameters.get(1)).equals(paramString))
              return true; 
            break;
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
          case 6:
            if (((String)blockBean.parameters.get(0)).equals(paramString))
              return true; 
            break;
          case 0:
            if (blockBean.spec.equals(paramString))
              return true; 
            break;
        } 
      } 
    } 
    return false;
  }
  
  public Object[] findNearestSnapPoint(BlockView paramRs, int paramInt1, int paramInt2) {
    byte b;
    Object[] arrayOfObject = null;
    if (paramRs.isParameter) {
      b = 40;
    } else {
      b = 60;
    } 
    int i = 100000;
    Point point2 = new Point(paramInt1, paramInt2);
    paramInt1 = 0;
    paramInt2 = i;
    while (paramInt1 < this.blockSnapPoints.size()) {
      Object[] arrayOfObject1 = this.blockSnapPoints.get(paramInt1);
      int[] arrayOfInt = (int[])arrayOfObject1[0];
      int dx = point2.x - arrayOfInt[0];
      int dy = point2.y - arrayOfInt[1];
      int j = Math.abs(dx / 2) + Math.abs(dy);
      if (j < paramInt2 && j < b) {
        if (isCompatibleBlock(paramRs, (View)arrayOfObject1[1])) {
          arrayOfObject = arrayOfObject1;
          paramInt2 = j;
        } 
      } 
      paramInt1++;
    } 
    return arrayOfObject;
  }
  
  public void clearSnapState() {
    hideActiveBlock();
    this.blockSnapPoints = new ArrayList();
    this.currentSnapPoint = null;
  }
  
  public void detachFromParent(BlockView paramRs) {
    BlockView rs = ((BaseBlockView)paramRs).parentBlock;
    if (rs == null)
      return; 
    if (rs != null) {
      rs.detachBlock(paramRs);
      ((BaseBlockView)paramRs).parentBlock = null;
    } 
  }
  
  public void updateDragPreview(BlockView paramRs, int paramInt1, int paramInt2) {
    getLocationOnScreen(this.locationBuffer);
    this.currentSnapPoint = findNearestSnapPoint(paramRs, paramInt1, paramInt2);
    boolean bool = paramRs.hasSubstack();
    boolean bool1 = true;
    if (bool && -1 == paramRs.subStack1) {
      Object[] arrayOfObject1 = this.currentSnapPoint;
      if (arrayOfObject1 != null) {
        BlockView rs = (BlockView)arrayOfObject1[1];
        paramInt1 = ((Integer)arrayOfObject1[2]).intValue();
        if (paramInt1 != 0) {
          if (paramInt1 != 2) {
            if (paramInt1 == 3)
              rs = (BlockView)findViewWithTag(Integer.valueOf(rs.subStack2)); 
          } else {
            rs = (BlockView)findViewWithTag(Integer.valueOf(rs.subStack1));
          } 
        } else {
          rs = (BlockView)findViewWithTag(Integer.valueOf(rs.nextBlock));
        } 
      } 
    } 
    Object[] arrayOfObject = this.currentSnapPoint;
    if (arrayOfObject != null) {
      int[] arrayOfInt = (int[])arrayOfObject[0];
      View view = (View)arrayOfObject[1];
      this.activeBlock.setX((arrayOfInt[0] - this.locationBuffer[0]));
      this.activeBlock.setY((arrayOfInt[1] - this.locationBuffer[1]));
      this.activeBlock.bringToFront();
      this.activeBlock.setVisibility(0);
      if (paramRs.isParameter) {
        if (view instanceof BlockView)
          this.activeBlock.copyBlockDimensions((BaseBlockView)view, true, false, 0); 
        if (view instanceof pro.sketchware.core.FieldBlockView)
          this.activeBlock.copyBlockDimensions((BaseBlockView)view, true, false, 0); 
      } else {
        paramInt2 = ((Integer)this.currentSnapPoint[2]).intValue();
        if (paramInt2 == 4) {
          paramInt1 = ((BlockView)view).getHeightSum();
        } else {
          paramInt1 = 0;
        } 
        if (paramInt2 == 1 || paramInt2 == 4)
          bool1 = false; 
        this.activeBlock.copyBlockDimensions((BaseBlockView)paramRs, false, bool1, paramInt1);
      } 
    } else {
      hideActiveBlock();
    } 
  }
  
  public boolean hasMapReference(String paramString) {
    int i = getChildCount();
    for (int b = 0; b < i; b++) {
      View view = getChildAt(b);
      if (view instanceof BlockView) {
        byte b1 = -1;
        BlockBean blockBean = ((BlockView)view).getBean();
        String str = blockBean.opCode;
        switch (str.hashCode()) {
          default:
            b1 = -1;
            break;
          case 2090182653:
            if (str.equals("addListMap")) {
              b1 = 15;
              break;
            } 
          case 1775620400:
            if (str.equals("strToMap")) {
              b1 = 18;
              break;
            } 
          case 1431171391:
            if (str.equals("mapRemoveKey")) {
              b1 = 10;
              break;
            } 
          case 845089750:
            if (str.equals("setVarString")) {
              b1 = 3;
              break;
            } 
          case 836692861:
            if (str.equals("mapSize")) {
              b1 = 11;
              break;
            } 
          case 754442829:
            if (str.equals("increaseInt")) {
              b1 = 4;
              break;
            } 
          case 747168008:
            if (str.equals("mapCreateNew")) {
              b1 = 6;
              break;
            } 
          case 657721930:
            if (str.equals("setVarInt")) {
              b1 = 2;
              break;
            } 
          case 463560551:
            if (str.equals("mapContainKey")) {
              b1 = 9;
              break;
            } 
          case 442768763:
            if (str.equals("mapGetAllKeys")) {
              b1 = 14;
              break;
            } 
          case 168740282:
            if (str.equals("mapToStr")) {
              b1 = 17;
              break;
            } 
          case 152967761:
            if (str.equals("mapClear")) {
              b1 = 12;
              break;
            } 
          case -329559323:
            if (str.equals("insertListMap")) {
              b1 = 16;
              break;
            } 
          case -1081391085:
            if (str.equals("mapPut")) {
              b1 = 7;
              break;
            } 
          case -1081400230:
            if (str.equals("mapGet")) {
              b1 = 8;
              break;
            } 
          case -1249347599:
            if (str.equals("getVar")) {
              b1 = 0;
              break;
            } 
          case -1377080719:
            if (str.equals("decreaseInt")) {
              b1 = 5;
              break;
            } 
          case -1384858251:
            if (str.equals("getAtListMap")) {
              b1 = 19;
              break;
            } 
          case -1920517885:
            if (str.equals("setVarBoolean")) {
              b1 = 1;
              break;
            } 
          case -2120571577:
            if (str.equals("mapIsEmpty")) {
              b1 = 13;
              break;
            } 
        } 
        switch (b1) {
          case 19:
            if (((String)blockBean.parameters.get(2)).equals(paramString))
              return true; 
            break;
          case 18:
            if (((String)blockBean.parameters.get(1)).equals(paramString))
              return true; 
            break;
          case 1:
          case 2:
          case 3:
          case 4:
          case 5:
          case 6:
          case 7:
          case 8:
          case 9:
          case 10:
          case 11:
          case 12:
          case 13:
          case 14:
          case 15:
          case 16:
          case 17:
            if (((String)blockBean.parameters.get(0)).equals(paramString))
              return true; 
            break;
          case 0:
            if (blockBean.spec.equals(paramString))
              return true; 
            break;
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
    ArrayList<BlockBean> arrayList = new ArrayList();
    BlockView rs = (BlockView)findViewWithTag(Integer.valueOf(this.dragBlock.nextBlock));
    if (rs != null) {
      Iterator<BlockView> iterator = rs.getAllChildren().iterator();
      while (iterator.hasNext())
        arrayList.add(((BlockView)iterator.next()).getBean()); 
    } 
    return arrayList;
  }
  
  public Object[] getNearestTarget() {
    return this.currentSnapPoint;
  }
  
  public BlockView getRoot() {
    return this.dragBlock;
  }
}
