package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.editor.logic.BlockPane;
import com.google.gson.Gson;
import java.util.ArrayList;
import mod.hey.studios.editor.manage.block.ExtraBlockInfo;
import mod.hey.studios.editor.manage.block.v2.BlockLoader;
import mod.hey.studios.project.ProjectTracker;

public class BlockView extends BaseBlockView {
  public String spec;
  
  public String opCode;
  
  public ArrayList<View> childViews;
  
  public int minBlockWidth = 30;
  
  public int minSimpleWidth = 50;
  
  public int minHatWidth = 90;
  
  public int minCWidth = 90;
  
  public int spacing = 4;
  
  public boolean isDefinitionBlock = false;
  
  public boolean isParameter = false;
  
  public boolean hasEndCap = false;
  
  public int nextBlock = -1;
  
  public int subStack1 = -1;
  
  public int subStack2 = -1;
  
  public ArrayList<View> specViews = new ArrayList<>();
  
  public ArrayList<String> specTypes = new ArrayList<>();
  
  public TextView elseLabel = null;
  
  public int depth = 0;
  
  public int blockTypeInt = 0;
  
  public BlockPane blockPane = null;
  
  public String blockTypeStr;
  
  public String componentTypeStr;
  
  private String spec2 = "";
  
  public BlockView(Context context, int index, String key, String value, String extra) {
    super(context, value, false);
    setTag(Integer.valueOf(index));
    spec = key;
    opCode = extra;
    initBlock();
  }
  
  public BlockView(Context context, int index, String key, String value, String extra, String tag) {
    super(context, value, extra, false);
    setTag(Integer.valueOf(index));
    spec = key;
    opCode = tag;
    initBlock();
  }
  
  public final int getTextWidth(TextView textView) {
    Rect rect = new Rect();
    textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().length(), rect);
    return rect.width();
  }
  
  public final TextView createLabel(String label) {
    TextView textView = new TextView(context);
    String text = label;
    if (opCode.equals("getVar") || opCode.equals("getArg")) {
      String prefix = componentType;
      if (prefix != null && prefix.length() > 0) {
        text = componentType + " : " + label;
      }
    }
    textView.setText(text);
    textView.setTextSize(10.0f);
    textView.setPadding(0, 0, 0, 0);
    textView.setGravity(16);
    textView.setTextColor(-1);
    textView.setTypeface(null, 1);
    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(-2, textHeight);
    lp.setMargins(0, 0, 0, 0);
    textView.setLayoutParams(lp);
    return textView;
  }
  
  public final void appendToChain(BlockView blockView) {
    if (blockView == this) return;
    if (hasSubstack() && -1 == subStack1) {
      setSubstack1Block(blockView);
    } else {
      BlockView rs = getLastInChain();
      if (rs != blockView) {
        rs.nextBlock = ((Integer)blockView.getTag()).intValue();
        blockView.parentBlock = rs;
      }
    } 
  }
  
  public void replaceParameter(BaseBlockView targetBlock, BlockView blockView) {
    int index = specViews.indexOf(targetBlock);
    if (index < 0) return;
    boolean isRs = targetBlock instanceof BlockView;
    if (isRs) {
      BlockView oldRs = (BlockView) targetBlock;
      blockView.blockTypeStr = oldRs.blockTypeStr;
      blockView.componentTypeStr = oldRs.componentTypeStr;
    } else if (targetBlock instanceof FieldBlockView) {
      blockView.blockTypeStr = targetBlock.blockType;
      blockView.componentTypeStr = targetBlock.componentType;
    }
    if (!isRs) {
      removeView(targetBlock);
    }
    specViews.set(index, blockView);
    blockView.parentBlock = this;
    rebuildChildViews();
    recalculateDepthChain();
    if (targetBlock != blockView && isRs) {
      ((BlockView) targetBlock).parentBlock = null;
      targetBlock.setX(getX() + getWidthSum() + 10.0f);
      targetBlock.setY(getY() + 5.0f);
      ((BlockView) targetBlock).layoutChain();
    }
  }
  
  public final void parseSpec(String spec, int index) {
    ArrayList<String> specTokens = FormatUtil.parseBlockSpec(spec);
    specViews = new ArrayList<>();
    specTypes = new ArrayList<>();
    for (int b = 0; b < specTokens.size(); b++) {
      View view = createSpecView(specTokens.get(b), index);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      specViews.add(view);
      if (view instanceof FieldBlockView) {
        spec = specTokens.get(b);
      } else {
        spec = "icon";
      } 
      if (view instanceof TextView)
        spec = "label"; 
      specTypes.add(spec);
    } 
  }
  
  public final View createSpecView(String spec, int index) {
    if (spec.length() >= 2 && spec.charAt(0) == '%') {
      int type = spec.charAt(1);
      if (type == 98) {
        return new FieldBlockView(context, "b", "");
      } else if (type == 100) {
        return new FieldBlockView(context, "d", "");
      } else if (type == 109) {
        return new FieldBlockView(context, "m", spec.substring(3));
      } else if (type == 115) {
        String defaultValue = "";
        if (spec.length() > 2)
          defaultValue = spec.substring(3);
        return new FieldBlockView(context, "s", defaultValue);
      }
      return createLabel(FormatUtil.unescapeString(spec));
    }
    return createLabel(FormatUtil.unescapeString(spec));
  }
  
  public void setNextBlock(BlockView blockView) {
    if (blockView == this) return;
    View view = blockPane.findViewWithTag(Integer.valueOf(nextBlock));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    nextBlock = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void positionAbove(BlockView blockView) {
    blockView.setX(getX());
    blockView.setY(getY() - blockView.getHeightSum() + borderWidth);
    blockView.getLastInChain().setNextBlock(this);
  }
  
  public void positionInSubstack1(BlockView blockView) {
    if (blockView == this) return;
    blockView.setX(getX() - cornerRadius);
    blockView.setY(getY() - getBlockHeight());
    parentBlock = blockView;
    blockView.subStack1 = ((Integer)getTag()).intValue();
  }
  
  public void setSubstack1Block(BlockView blockView) {
    if (blockView == this) return;
    View view = blockPane.findViewWithTag(Integer.valueOf(subStack1));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    subStack1 = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void setSubstack2Block(BlockView blockView) {
    if (blockView == this) return;
    View view = blockPane.findViewWithTag(Integer.valueOf(subStack2));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    subStack2 = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void detachBlock(BlockView blockView) {
    if (nextBlock == ((Integer)blockView.getTag()).intValue())
      nextBlock = -1; 
    if (subStack1 == ((Integer)blockView.getTag()).intValue())
      subStack1 = -1; 
    if (subStack2 == ((Integer)blockView.getTag()).intValue())
      subStack2 = -1; 
    if (blockView.isParameter) {
      int i = specViews.indexOf(blockView);
      if (i < 0)
        return; 
      blockView.blockTypeStr = "";
      blockView.componentTypeStr = "";
      View view = createSpecView(specTypes.get(i), blockColor);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      specViews.set(i, view);
      addView(view);
      rebuildChildViews();
      recalculateDepthChain();
    } 
    getRootBlock().layoutChain();
  }
  
  public ArrayList<BlockView> getAllChildren() {
    ArrayList<BlockView> children = new ArrayList<>();
    BlockView rs = this;
    int limit = 200;
    while (true) {
      children.add(rs);
      for (View view : rs.specViews) {
        if (view instanceof BlockView)
          children.addAll(((BlockView)view).getAllChildren()); 
      } 
      if (rs.hasSubstack()) {
        int j = rs.subStack1;
        if (j != -1) {
          BlockView sub = (BlockView)blockPane.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            children.addAll(sub.getAllChildren()); 
        }
      } 
      if (rs.hasDoubleSubstack()) {
        int j = rs.subStack2;
        if (j != -1) {
          BlockView sub = (BlockView)blockPane.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            children.addAll(sub.getAllChildren()); 
        }
      } 
      int i = rs.nextBlock;
      if (i != -1 && --limit > 0) {
        BlockView next = (BlockView)blockPane.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return children;
        rs = next;
        continue;
      } 
      return children;
    } 
  }
  
  public BlockBean getBean() {
    BlockBean blockBean = new BlockBean(getTag().toString(), spec, blockType, componentType, opCode);
    blockBean.color = blockColor;
    for (View view : childViews) {
      if (view instanceof FieldBlockView) {
        blockBean.parameters.add(((FieldBlockView) view).getArgValue().toString());
      } else if (view instanceof BlockView) {
        blockBean.parameters.add("@" + view.getTag().toString());
      }
    } 
    blockBean.subStack1 = subStack1;
    blockBean.subStack2 = subStack2;
    blockBean.nextBlock = nextBlock;
    return blockBean;
  }
  
  public int getBlockType() {
    return blockTypeInt;
  }
  
  public int getDepth() {
    int b = 0;
    BlockView rs = this;
    int limit = 500;
    while (true) {
      rs = rs.parentBlock;
      if (rs != null && --limit > 0) {
        b++;
        continue;
      } 
      return b;
    } 
  }
  public int getHeightSum() {
    int i = 0;
    BlockView rs = this;
    int limit = 200;
    while (true) {
      int j = i;
      if (i != 0)
        j = i - borderWidth; 
      i = j + rs.getTotalHeight();
      j = rs.nextBlock;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)blockPane.findViewWithTag(Integer.valueOf(j));
        if (next == null || next == this) break;
        rs = next;
        continue;
      } 
      return i;
    }
    return i;
  }
  
  public int getWidthSum() {
    int i = 0;
    BlockView rs = this;
    int limit = 200;
    while (true) {
      i = Math.max(i, rs.getW());
      int j = i;
      if (rs.hasSubstack()) {
        int k = rs.subStack1;
        j = i;
        if (k != -1) {
          BlockView sub1 = (BlockView)blockPane.findViewWithTag(Integer.valueOf(k));
          if (sub1 != null) {
            j = cornerRadius;
            j = Math.max(i, sub1.getWidthSum() + j);
          }
        } 
      } 
      i = j;
      if (rs.hasDoubleSubstack()) {
        int k = rs.subStack2;
        i = j;
        if (k != -1) {
          BlockView sub2 = (BlockView)blockPane.findViewWithTag(Integer.valueOf(k));
          if (sub2 != null) {
            i = cornerRadius;
            i = Math.max(j, sub2.getWidthSum() + i);
          }
        } 
      } 
      j = rs.nextBlock;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)blockPane.findViewWithTag(Integer.valueOf(j));
        if (next == null || next == this) break;
        rs = next;
        continue;
      } 
      return i;
    }
    return i;
  }
  
  public BlockView getLastInChain() {
    BlockView rs = this;
    int limit = 200;
    while (true) {
      int i = rs.nextBlock;
      if (i != -1 && --limit > 0) {
        BlockView next = (BlockView)blockPane.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return rs;
        rs = next;
        continue;
      } 
      return rs;
    } 
  }
  
  public final void rebuildChildViews() {
    childViews = new ArrayList<>();
    for (View view : specViews) {
      if (view instanceof BlockView || view instanceof FieldBlockView)
        childViews.add(view); 
    } 
  }
  
  public final void positionElseLabel() {
    TextView textView = elseLabel;
    if (textView != null) {
      textView.bringToFront();
      elseLabel.setX(leftIndent);
      elseLabel.setY(getSubstackBottom() - bottomPadding);
    } 
  }
  
  public void layoutChain() {
    BlockView current = this;
    int limit = 1000;
    while (current != null && --limit > 0) {
      current.layoutSingle();
      int next = current.nextBlock;
      if (next > -1) {
        BlockView nextRs = (BlockView) current.blockPane.findViewWithTag(Integer.valueOf(next));
        if (nextRs == null || nextRs == this) break;
        nextRs.setX(current.getX());
        nextRs.setY(current.getY() + (float) current.getContentBottom());
        nextRs.bringToFront();
        current = nextRs;
      } else {
        current = null;
      }
    }
  }

  private void layoutSingle() {
    bringToFront();
    int xOffset = leftIndent;
    for (int idx = 0; idx < specViews.size(); idx++) {
      View child = specViews.get(idx);
      child.bringToFront();
      boolean isRs = child instanceof BlockView;
      float childX;
      if (isRs) {
        childX = getX() + (float) xOffset;
      } else {
        childX = (float) xOffset;
      }
      child.setX(childX);
      int childWidth;
      if (specTypes.get(idx).equals("label")) {
        childWidth = getTextWidth((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof FieldBlockView) {
        childWidth = ((FieldBlockView) child).getW();
      }
      if (isRs) {
        childWidth = ((BlockView) child).getWidthSum();
      }
      xOffset += childWidth + spacing;
      if (isRs) {
        BlockView childRs = (BlockView) child;
        child.setY(getY() + (float) topSpacing + (float) ((depth - childRs.depth - 1) * shadowOffset));
        childRs.layoutChain();
      } else {
        child.setY((float) (topSpacing + depth * shadowOffset));
      }
    }
    int w = xOffset;
    if (blockType.equals("b") || blockType.equals("d") || blockType.equals("s") || blockType.equals("a")) {
      w = Math.max(xOffset, minBlockWidth);
    }
    int widthWithSimple = w;
    if (blockType.equals(" ") || blockType.equals("") || blockType.equals("f")) {
      widthWithSimple = Math.max(w, minSimpleWidth);
    }
    int widthWithC = widthWithSimple;
    if (blockType.equals("c") || blockType.equals("e")) {
      widthWithC = Math.max(widthWithSimple, minCWidth);
    }
    int widthWithHat = widthWithC;
    if (blockType.equals("h")) {
      widthWithHat = Math.max(widthWithC, minHatWidth);
    }
    setBlockSize((float) (rightIndent + widthWithHat), (float) (topSpacing + textHeight + depth * shadowOffset * 2 + bottomSpacing), true);
    if (hasSubstack()) {
      int ss1Height = minHeight;
      int sub1 = subStack1;
      if (sub1 > -1) {
        BlockView sub1Rs = (BlockView) blockPane.findViewWithTag(Integer.valueOf(sub1));
        if (sub1Rs != null) {
          sub1Rs.setX(getX() + (float) cornerRadius);
          sub1Rs.setY(getY() + (float) getBlockHeight());
          sub1Rs.bringToFront();
          sub1Rs.layoutChain();
          ss1Height = sub1Rs.getHeightSum();
        }
      }
      setSubstack1Height(ss1Height);
      int ss2Height = minHeight;
      int sub2 = subStack2;
      if (sub2 > -1) {
        BlockView sub2Rs = (BlockView) blockPane.findViewWithTag(Integer.valueOf(sub2));
        if (sub2Rs != null) {
          sub2Rs.setX(getX() + (float) cornerRadius);
          sub2Rs.setY(getY() + (float) getSubstackBottom());
          sub2Rs.bringToFront();
          sub2Rs.layoutChain();
          ss2Height = sub2Rs.getHeightSum();
          if (sub2Rs.getLastInChain().hasEndCap) {
            ss2Height += borderWidth;
          }
        }
      }
      setSubstack2Height(ss2Height);
      positionElseLabel();
    }
  }
  
  public void initBlock() {
    setDrawingCacheEnabled(false);
    float scale = density;
    minBlockWidth = (int) (minBlockWidth * scale);
    minSimpleWidth = (int) (minSimpleWidth * scale);
    minHatWidth = (int) (minHatWidth * scale);
    minCWidth = (int) (minCWidth * scale);
    spacing = (int) (spacing * scale);
    String type = blockType;
    byte typeNum = 0;
    if (type.equals("b")) { typeNum = 1; }
    else if (type.equals("s")) { typeNum = 2; }
    else if (type.equals("d")) { typeNum = 3; }
    else if (type.equals("v")) { typeNum = 4; }
    else if (type.equals("p")) { typeNum = 5; }
    else if (type.equals("l")) { typeNum = 6; }
    else if (type.equals("a")) { typeNum = 7; }
    else if (type.equals("c")) { typeNum = 8; }
    else if (type.equals("e")) { typeNum = 9; }
    else if (type.equals("f")) { typeNum = 10; }
    else if (type.equals("h")) { typeNum = 11; }
    else if (type.equals(" ")) { typeNum = 0; }
    else { typeNum = -1; }
    switch (typeNum) {
      case 1: case 2: case 3: case 4: case 5: case 6: case 7:
        isParameter = true;
        break;
      case 10:
        hasEndCap = true;
        break;
      case 11:
        isDefinitionBlock = true;
        break;
      default:
        break;
    }
    int color = BlockColorMapper.getBlockColor(opCode, blockType);
    if (!isDefinitionBlock && !opCode.equals("definedFunc") && !opCode.equals("getVar")
        && !opCode.equals("getResStr") && !opCode.equals("getArg") && color != -7711273) {
      spec = StringResource.getInstance().getEventTranslation(getContext(), opCode);
    }
    if (color == -7711273) {
      ExtraBlockInfo info = BlockLoader.getBlockInfo(opCode);
      ExtraBlockInfo blockInfo = info;
      if (ProjectTracker.SC_ID != null && info.isMissing
          && !ProjectTracker.SC_ID.equals("")) {
        blockInfo = BlockLoader.getBlockFromProject(
            ProjectTracker.SC_ID, opCode);
        Log.d("BlockView", "BlockView:returned block: " + new Gson().toJson(blockInfo));
      }
      spec2 = blockInfo.getSpec2();
      if (spec.equals("")) {
        spec = blockInfo.getSpec();
      }
      if (spec.equals("")) {
        spec = opCode;
      }
      setSpec(spec);
      int blockColor = blockInfo.getColor();
      int paletteColor = blockInfo.getPaletteColor();
      if (!opCode.equals("definedFunc") && !opCode.equals("getVar")
          && !opCode.equals("getResStr") && !opCode.equals("getArg")) {
        if (blockColor != 0) {
          this.blockColor = blockColor;
          return;
        }
        if (paletteColor != 0) {
          this.blockColor = paletteColor;
          return;
        }
      }
    } else {
      if (spec.equals("")) {
        spec = opCode;
      }
      setSpec(spec);
    }
    this.blockColor = color;
  }
  
  public void recalculateToRoot() {
    BlockView parent;
    BlockView current = this;
    int count = 0;
    do {
      current.recalculateSize();
      parent = current.parentBlock;
      count++;
      if (count > 500) {
        Log.e("BlockView", "m() infinite loop detected! count=" + count + " tag=" + current.getTag() + " E.tag=" + (parent != null ? parent.getTag() : "null"));
        break;
      }
      current = parent;
    } while (parent != null);
  }
  
  public void recalculateSize() {
    int xOffset = leftIndent;
    for (int idx = 0; idx < specViews.size(); idx++) {
      View child = specViews.get(idx);
      int childWidth;
      if (specTypes.get(idx).equals("label")) {
        childWidth = getTextWidth((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof FieldBlockView) {
        childWidth = ((FieldBlockView) child).getW();
      }
      if (child instanceof BlockView) {
        childWidth = ((BlockView) child).getWidthSum();
      }
      xOffset += childWidth + spacing;
    }
    int w = xOffset;
    if (blockType.equals("b") || blockType.equals("d") || blockType.equals("s") || blockType.equals("a")) {
      w = Math.max(xOffset, minBlockWidth);
    }
    int widthWithSimple = w;
    if (blockType.equals(" ") || blockType.equals("") || blockType.equals("o")) {
      widthWithSimple = Math.max(w, minSimpleWidth);
    }
    int widthWithC = widthWithSimple;
    if (blockType.equals("c") || blockType.equals("e")) {
      widthWithC = Math.max(widthWithSimple, minCWidth);
    }
    int widthWithHat = widthWithC;
    if (blockType.equals("h")) {
      widthWithHat = Math.max(widthWithC, minHatWidth);
    }
    int finalW = widthWithHat;
    TextView label = elseLabel;
    if (label != null) {
      finalW = Math.max(widthWithHat, label.getWidth() + leftIndent + 2);
    }
    setBlockSize((float) (rightIndent + finalW), (float) (topSpacing + textHeight + depth * shadowOffset * 2 + bottomSpacing), false);
  }
  
  public void recalculateDepthChain() {
    BlockView rs = this;
    int limit = 200;
    while (rs != null && --limit > 0) {
      int i = 0;
      for (View view : rs.childViews) {
        if (view instanceof BlockView)
          i = Math.max(i, ((BlockView)view).depth + 1); 
      } 
      rs.depth = i;
      rs.recalculateSize();
      if (rs.isParameter) {
        rs = rs.parentBlock;
        continue;
      } 
      break;
    } 
  }
  
  public BlockView getRootBlock() {
    BlockView rs = this;
    int limit = 200;
    while (true) {
      BlockView parent = rs.parentBlock;
      if (parent != null && --limit > 0) {
        rs = parent;
        continue;
      } 
      return rs;
    } 
  }
  
  public void setBlockType(int index) {
    blockTypeInt = index;
  }
  
  public void setSpec(String spec) {
    this.spec = spec;
    removeAllViews();
    parseSpec(this.spec, blockColor);
    for (View view : specViews)
      addView(view);
    rebuildChildViews();
    if (blockType.equals("e") && opCode.equals("ifElse")) {
      elseLabel = createLabel(StringResource.getInstance().getEventTranslation(getContext(), "else"));
      addView(elseLabel);
    } 
    if (blockType.equals("e") && !spec2.equals("")) {
      elseLabel = createLabel(spec2);
      addView(elseLabel);
    } 
    layoutChain();
  }
}
