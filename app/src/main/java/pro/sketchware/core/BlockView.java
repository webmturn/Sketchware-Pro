package pro.sketchware.core;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.editor.logic.BlockPane;
import java.util.ArrayList;
import java.util.Iterator;

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
  
  public ArrayList<View> specViews = new ArrayList<View>();
  
  public ArrayList<String> specTypes = new ArrayList<String>();
  
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
    this.spec = key;
    this.opCode = extra;
    initBlock();
  }
  
  public BlockView(Context context, int index, String key, String value, String extra, String tag) {
    super(context, value, extra, false);
    setTag(Integer.valueOf(index));
    this.spec = key;
    this.opCode = tag;
    initBlock();
  }
  
  public final int getTextWidth(TextView textView) {
    Rect rect = new Rect();
    textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().length(), rect);
    return rect.width();
  }
  
  public final TextView createLabel(String label) {
    TextView textView = new TextView(this.context);
    String text = label;
    if (this.opCode.equals("getVar") || this.opCode.equals("getArg")) {
      String prefix = this.componentType;
      if (prefix != null && prefix.length() > 0) {
        text = this.componentType + " : " + label;
      }
    }
    textView.setText(text);
    textView.setTextSize(10.0f);
    textView.setPadding(0, 0, 0, 0);
    textView.setGravity(16);
    textView.setTextColor(-1);
    textView.setTypeface(null, 1);
    android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(-2, this.textHeight);
    lp.setMargins(0, 0, 0, 0);
    textView.setLayoutParams(lp);
    return textView;
  }
  
  public final void appendToChain(BlockView blockView) {
    if (blockView == this) return;
    if (hasSubstack() && -1 == this.subStack1) {
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
    int index = this.specViews.indexOf(targetBlock);
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
    this.specViews.set(index, blockView);
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
    ArrayList<String> arrayList = FormatUtil.parseBlockSpec(spec);
    this.specViews = new ArrayList<View>();
    this.specTypes = new ArrayList<String>();
    for (int b = 0; b < arrayList.size(); b++) {
      View view = createSpecView(arrayList.get(b), index);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      this.specViews.add(view);
      if (view instanceof FieldBlockView) {
        spec = arrayList.get(b);
      } else {
        spec = "icon";
      } 
      if (view instanceof TextView)
        spec = "label"; 
      this.specTypes.add(spec);
    } 
  }
  
  public final View createSpecView(String spec, int index) {
    if (spec.length() >= 2 && spec.charAt(0) == '%') {
      int type = spec.charAt(1);
      if (type == 98) {
        return new FieldBlockView(this.context, "b", "");
      } else if (type == 100) {
        return new FieldBlockView(this.context, "d", "");
      } else if (type == 109) {
        return new FieldBlockView(this.context, "m", spec.substring(3));
      } else if (type == 115) {
        String defaultValue = "";
        if (spec.length() > 2)
          defaultValue = spec.substring(3);
        return new FieldBlockView(this.context, "s", defaultValue);
      }
      return createLabel(FormatUtil.unescapeString(spec));
    }
    return createLabel(FormatUtil.unescapeString(spec));
  }
  
  public void setNextBlock(BlockView blockView) {
    if (blockView == this) return;
    View view = this.blockPane.findViewWithTag(Integer.valueOf(this.nextBlock));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    this.nextBlock = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void positionAbove(BlockView blockView) {
    blockView.setX(getX());
    blockView.setY(getY() - blockView.getHeightSum() + this.borderWidth);
    blockView.getLastInChain().setNextBlock(this);
  }
  
  public void positionInSubstack1(BlockView blockView) {
    if (blockView == this) return;
    blockView.setX(getX() - this.cornerRadius);
    blockView.setY(getY() - getBlockHeight());
    this.parentBlock = blockView;
    blockView.subStack1 = ((Integer)getTag()).intValue();
  }
  
  public void setSubstack1Block(BlockView blockView) {
    if (blockView == this) return;
    View view = this.blockPane.findViewWithTag(Integer.valueOf(this.subStack1));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    this.subStack1 = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void setSubstack2Block(BlockView blockView) {
    if (blockView == this) return;
    View view = this.blockPane.findViewWithTag(Integer.valueOf(this.subStack2));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    blockView.parentBlock = this;
    this.subStack2 = ((Integer)blockView.getTag()).intValue();
    if (view != null && view != blockView)
      blockView.appendToChain((BlockView)view); 
  }
  
  public void detachBlock(BlockView blockView) {
    if (this.nextBlock == ((Integer)blockView.getTag()).intValue())
      this.nextBlock = -1; 
    if (this.subStack1 == ((Integer)blockView.getTag()).intValue())
      this.subStack1 = -1; 
    if (this.subStack2 == ((Integer)blockView.getTag()).intValue())
      this.subStack2 = -1; 
    if (blockView.isParameter) {
      int i = this.specViews.indexOf(blockView);
      if (i < 0)
        return; 
      blockView.blockTypeStr = "";
      blockView.componentTypeStr = "";
      View view = createSpecView(this.specTypes.get(i), this.blockColor);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      this.specViews.set(i, view);
      addView(view);
      rebuildChildViews();
      recalculateDepthChain();
    } 
    getRootBlock().layoutChain();
  }
  
  public ArrayList<BlockView> getAllChildren() {
    ArrayList<BlockView> arrayList = new ArrayList<>();
    BlockView rs = this;
    int limit = 200;
    while (true) {
      arrayList.add(rs);
      for (View view : rs.specViews) {
        if (view instanceof BlockView)
          arrayList.addAll(((BlockView)view).getAllChildren()); 
      } 
      if (rs.hasSubstack()) {
        int j = rs.subStack1;
        if (j != -1) {
          BlockView sub = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      if (rs.hasDoubleSubstack()) {
        int j = rs.subStack2;
        if (j != -1) {
          BlockView sub = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      int i = rs.nextBlock;
      if (i != -1 && --limit > 0) {
        BlockView next = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return arrayList;
        rs = next;
        continue;
      } 
      return arrayList;
    } 
  }
  
  public BlockBean getBean() {
    BlockBean blockBean = new BlockBean(getTag().toString(), this.spec, this.blockType, this.componentType, this.opCode);
    blockBean.color = this.blockColor;
    for (View view : this.childViews) {
      if (view instanceof FieldBlockView) {
        blockBean.parameters.add(((FieldBlockView) view).getArgValue().toString());
      } else if (view instanceof BlockView) {
        blockBean.parameters.add("@" + view.getTag().toString());
      }
    } 
    blockBean.subStack1 = this.subStack1;
    blockBean.subStack2 = this.subStack2;
    blockBean.nextBlock = this.nextBlock;
    return blockBean;
  }
  
  public int getBlockType() {
    return this.blockTypeInt;
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
        j = i - this.borderWidth; 
      i = j + rs.getTotalHeight();
      j = rs.nextBlock;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(j));
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
          BlockView sub1 = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(k));
          if (sub1 != null) {
            j = this.cornerRadius;
            j = Math.max(i, sub1.getWidthSum() + j);
          }
        } 
      } 
      i = j;
      if (rs.hasDoubleSubstack()) {
        int k = rs.subStack2;
        i = j;
        if (k != -1) {
          BlockView sub2 = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(k));
          if (sub2 != null) {
            i = this.cornerRadius;
            i = Math.max(j, sub2.getWidthSum() + i);
          }
        } 
      } 
      j = rs.nextBlock;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(j));
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
        BlockView next = (BlockView)this.blockPane.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return rs;
        rs = next;
        continue;
      } 
      return rs;
    } 
  }
  
  public final void rebuildChildViews() {
    this.childViews = new ArrayList<View>();
    for (int b = 0; b < this.specViews.size(); b++) {
      View view = this.specViews.get(b);
      if (view instanceof BlockView || view instanceof FieldBlockView)
        this.childViews.add(view); 
    } 
  }
  
  public final void positionElseLabel() {
    TextView textView = this.elseLabel;
    if (textView != null) {
      textView.bringToFront();
      this.elseLabel.setX(this.leftIndent);
      this.elseLabel.setY((getSubstackBottom() - this.bottomPadding));
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
    int xOffset = this.leftIndent;
    for (int idx = 0; idx < this.specViews.size(); idx++) {
      View child = (View) this.specViews.get(idx);
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
      if (((String) this.specTypes.get(idx)).equals("label")) {
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
      xOffset += childWidth + this.spacing;
      if (isRs) {
        BlockView childRs = (BlockView) child;
        child.setY(getY() + (float) this.topSpacing + (float) ((this.depth - childRs.depth - 1) * this.shadowOffset));
        childRs.layoutChain();
      } else {
        child.setY((float) (this.topSpacing + this.depth * this.shadowOffset));
      }
    }
    int w = xOffset;
    if (this.blockType.equals("b") || this.blockType.equals("d") || this.blockType.equals("s") || this.blockType.equals("a")) {
      w = Math.max(xOffset, this.minBlockWidth);
    }
    int w2 = w;
    if (this.blockType.equals(" ") || this.blockType.equals("") || this.blockType.equals("f")) {
      w2 = Math.max(w, this.minSimpleWidth);
    }
    int w3 = w2;
    if (this.blockType.equals("c") || this.blockType.equals("e")) {
      w3 = Math.max(w2, this.minCWidth);
    }
    int w4 = w3;
    if (this.blockType.equals("h")) {
      w4 = Math.max(w3, this.minHatWidth);
    }
    setBlockSize((float) (this.rightIndent + w4), (float) (this.topSpacing + this.textHeight + this.depth * this.shadowOffset * 2 + this.bottomSpacing), true);
    if (hasSubstack()) {
      int ss1Height = this.minHeight;
      int sub1 = this.subStack1;
      if (sub1 > -1) {
        BlockView sub1Rs = (BlockView) this.blockPane.findViewWithTag(Integer.valueOf(sub1));
        if (sub1Rs != null) {
          sub1Rs.setX(getX() + (float) this.cornerRadius);
          sub1Rs.setY(getY() + (float) getBlockHeight());
          sub1Rs.bringToFront();
          sub1Rs.layoutChain();
          ss1Height = sub1Rs.getHeightSum();
        }
      }
      setSubstack1Height(ss1Height);
      int ss2Height = this.minHeight;
      int sub2 = this.subStack2;
      if (sub2 > -1) {
        BlockView sub2Rs = (BlockView) this.blockPane.findViewWithTag(Integer.valueOf(sub2));
        if (sub2Rs != null) {
          sub2Rs.setX(getX() + (float) this.cornerRadius);
          sub2Rs.setY(getY() + (float) getSubstackBottom());
          sub2Rs.bringToFront();
          sub2Rs.layoutChain();
          ss2Height = sub2Rs.getHeightSum();
          if (sub2Rs.getLastInChain().hasEndCap) {
            ss2Height += this.borderWidth;
          }
        }
      }
      setSubstack2Height(ss2Height);
      positionElseLabel();
    }
  }
  
  public void initBlock() {
    setDrawingCacheEnabled(false);
    float scale = this.density;
    this.minBlockWidth = (int) (this.minBlockWidth * scale);
    this.minSimpleWidth = (int) (this.minSimpleWidth * scale);
    this.minHatWidth = (int) (this.minHatWidth * scale);
    this.minCWidth = (int) (this.minCWidth * scale);
    this.spacing = (int) (this.spacing * scale);
    String type = this.blockType;
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
        this.isParameter = true;
        break;
      case 10:
        this.hasEndCap = true;
        break;
      case 11:
        this.isDefinitionBlock = true;
        break;
      default:
        break;
    }
    int color = BlockColorMapper.getBlockColor(this.opCode, this.blockType);
    if (!this.isDefinitionBlock && !this.opCode.equals("definedFunc") && !this.opCode.equals("getVar")
        && !this.opCode.equals("getResStr") && !this.opCode.equals("getArg") && color != -7711273) {
      this.spec = StringResource.getInstance().getEventTranslation(getContext(), this.opCode);
    }
    if (color == -7711273) {
      mod.hey.studios.editor.manage.block.ExtraBlockInfo info = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockInfo(this.opCode);
      mod.hey.studios.editor.manage.block.ExtraBlockInfo blockInfo = info;
      if (mod.hey.studios.project.ProjectTracker.SC_ID != null && info.isMissing
          && !mod.hey.studios.project.ProjectTracker.SC_ID.equals("")) {
        blockInfo = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockFromProject(
            mod.hey.studios.project.ProjectTracker.SC_ID, this.opCode);
        android.util.Log.d("BlockView", "BlockView:returned block: " + new com.google.gson.Gson().toJson(blockInfo));
      }
      this.spec2 = blockInfo.getSpec2();
      if (this.spec.equals("")) {
        this.spec = blockInfo.getSpec();
      }
      if (this.spec.equals("")) {
        this.spec = this.opCode;
      }
      setSpec(this.spec);
      int blockColor = blockInfo.getColor();
      int paletteColor = blockInfo.getPaletteColor();
      if (!this.opCode.equals("definedFunc") && !this.opCode.equals("getVar")
          && !this.opCode.equals("getResStr") && !this.opCode.equals("getArg")) {
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
      if (this.spec.equals("")) {
        this.spec = this.opCode;
      }
      setSpec(this.spec);
    }
    this.blockColor = color;
  }
  
  public void recalculateToRoot() {
    BlockView rs2;
    BlockView rs1 = this;
    int count = 0;
    do {
      rs1.recalculateSize();
      rs2 = rs1.parentBlock;
      count++;
      if (count > 500) {
        android.util.Log.e("BlockView", "m() infinite loop detected! count=" + count + " tag=" + rs1.getTag() + " E.tag=" + (rs2 != null ? rs2.getTag() : "null"));
        break;
      }
      rs1 = rs2;
    } while (rs2 != null);
  }
  
  public void recalculateSize() {
    int xOffset = this.leftIndent;
    for (int idx = 0; idx < this.specViews.size(); idx++) {
      View child = (View) this.specViews.get(idx);
      int childWidth;
      if (((String) this.specTypes.get(idx)).equals("label")) {
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
      xOffset += childWidth + this.spacing;
    }
    int w = xOffset;
    if (this.blockType.equals("b") || this.blockType.equals("d") || this.blockType.equals("s") || this.blockType.equals("a")) {
      w = Math.max(xOffset, this.minBlockWidth);
    }
    int w2 = w;
    if (this.blockType.equals(" ") || this.blockType.equals("") || this.blockType.equals("o")) {
      w2 = Math.max(w, this.minSimpleWidth);
    }
    int w3 = w2;
    if (this.blockType.equals("c") || this.blockType.equals("e")) {
      w3 = Math.max(w2, this.minCWidth);
    }
    int w4 = w3;
    if (this.blockType.equals("h")) {
      w4 = Math.max(w3, this.minHatWidth);
    }
    int finalW = w4;
    TextView label = this.elseLabel;
    if (label != null) {
      finalW = Math.max(w4, label.getWidth() + this.leftIndent + 2);
    }
    setBlockSize((float) (this.rightIndent + finalW), (float) (this.topSpacing + this.textHeight + this.depth * this.shadowOffset * 2 + this.bottomSpacing), false);
  }
  
  public void recalculateDepthChain() {
    BlockView rs = this;
    int limit = 200;
    while (rs != null && --limit > 0) {
      Iterator<View> iterator = rs.childViews.iterator();
      int i = 0;
      while (iterator.hasNext()) {
        View view = iterator.next();
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
      BlockView rs1 = rs.parentBlock;
      if (rs1 != null && --limit > 0) {
        rs = rs1;
        continue;
      } 
      return rs;
    } 
  }
  
  public void setBlockType(int index) {
    this.blockTypeInt = index;
  }
  
  public void setSpec(String spec) {
    this.spec = spec;
    removeAllViews();
    parseSpec(this.spec, this.blockColor);
    Iterator<View> iterator = this.specViews.iterator();
    while (true) {
      if (!iterator.hasNext()) {
        rebuildChildViews();
        if (this.blockType.equals("e") && this.opCode.equals("ifElse")) {
          this.elseLabel = createLabel(StringResource.getInstance().getEventTranslation(getContext(), "else"));
          addView((View)this.elseLabel);
        } 
        if (this.blockType.equals("e") && !this.spec2.equals("")) {
          this.elseLabel = createLabel(this.spec2);
          addView((View)this.elseLabel);
        } 
        layoutChain();
        return;
      } 
      addView(iterator.next());
    } 
  }
}
