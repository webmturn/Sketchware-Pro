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
  public String T;
  
  public String U;
  
  public ArrayList<View> V;
  
  public int W = 30;
  
  public int aa = 50;
  
  public int ba = 90;
  
  public int ca = 90;
  
  public int da = 4;
  
  public boolean ea = false;
  
  public boolean fa = false;
  
  public boolean ga = false;
  
  public int ha = -1;
  
  public int ia = -1;
  
  public int ja = -1;
  
  public ArrayList<View> ka = new ArrayList<View>();
  
  public ArrayList<String> la = new ArrayList<String>();
  
  public TextView ma = null;
  
  public int na = 0;
  
  public int oa = 0;
  
  public BlockPane pa = null;
  
  public String qa;
  
  public String ra;
  
  private String spec2 = "";
  
  public BlockView(Context paramContext, int paramInt, String paramString1, String paramString2, String paramString3) {
    super(paramContext, paramString2, false);
    setTag(Integer.valueOf(paramInt));
    this.T = paramString1;
    this.U = paramString3;
    l();
  }
  
  public BlockView(Context paramContext, int paramInt, String paramString1, String paramString2, String paramString3, String paramString4) {
    super(paramContext, paramString2, paramString3, false);
    setTag(Integer.valueOf(paramInt));
    this.T = paramString1;
    this.U = paramString4;
    l();
  }
  
  public final int a(TextView paramTextView) {
    Rect rect = new Rect();
    paramTextView.getPaint().getTextBounds(paramTextView.getText().toString(), 0, paramTextView.getText().length(), rect);
    return rect.width();
  }
  
  public final TextView a(String paramString) {
    TextView textView = new TextView(this.context);
    String text = paramString;
    if (this.U.equals("getVar") || this.U.equals("getArg")) {
      String prefix = this.componentType;
      if (prefix != null && prefix.length() > 0) {
        text = this.componentType + " : " + paramString;
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
  
  public final void a(BlockView paramRs) {
    if (paramRs == this) return;
    if (b() && -1 == this.ia) {
      e(paramRs);
    } else {
      BlockView rs = h();
      if (rs != paramRs) {
        rs.ha = ((Integer)paramRs.getTag()).intValue();
        paramRs.parentBlock = rs;
      }
    } 
  }
  
  public void a(BaseBlockView paramTs, BlockView paramRs) {
    int index = this.ka.indexOf(paramTs);
    if (index < 0) return;
    boolean isRs = paramTs instanceof BlockView;
    if (isRs) {
      BlockView oldRs = (BlockView) paramTs;
      paramRs.qa = oldRs.qa;
      paramRs.ra = oldRs.ra;
    } else if (paramTs instanceof FieldBlockView) {
      paramRs.qa = paramTs.blockType;
      paramRs.ra = paramTs.componentType;
    }
    if (!isRs) {
      removeView(paramTs);
    }
    this.ka.set(index, paramRs);
    paramRs.parentBlock = this;
    i();
    o();
    if (paramTs != paramRs && isRs) {
      ((BlockView) paramTs).parentBlock = null;
      paramTs.setX(getX() + getWidthSum() + 10.0f);
      paramTs.setY(getY() + 5.0f);
      ((BlockView) paramTs).k();
    }
  }
  
  public final void a(String paramString, int paramInt) {
    ArrayList<String> arrayList = FormatUtil.c(paramString);
    this.ka = new ArrayList<View>();
    this.la = new ArrayList<String>();
    for (int b = 0; b < arrayList.size(); b++) {
      View view = b(arrayList.get(b), paramInt);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      this.ka.add(view);
      if (view instanceof FieldBlockView) {
        paramString = arrayList.get(b);
      } else {
        paramString = "icon";
      } 
      if (view instanceof TextView)
        paramString = "label"; 
      this.la.add(paramString);
    } 
  }
  
  public final View b(String paramString, int paramInt) {
    if (paramString.length() >= 2 && paramString.charAt(0) == '%') {
      int type = paramString.charAt(1);
      if (type == 98) {
        return new FieldBlockView(this.context, "b", "");
      } else if (type == 100) {
        return new FieldBlockView(this.context, "d", "");
      } else if (type == 109) {
        return new FieldBlockView(this.context, "m", paramString.substring(3));
      } else if (type == 115) {
        String str = "";
        if (paramString.length() > 2)
          str = paramString.substring(3);
        return new FieldBlockView(this.context, "s", str);
      }
      return a(FormatUtil.d(paramString));
    }
    return a(FormatUtil.d(paramString));
  }
  
  public void b(BlockView paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ha));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    paramRs.parentBlock = this;
    this.ha = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((BlockView)view); 
  }
  
  public void c(BlockView paramRs) {
    paramRs.setX(getX());
    paramRs.setY(getY() - paramRs.getHeightSum() + this.borderWidth);
    paramRs.h().b(this);
  }
  
  public void d(BlockView paramRs) {
    if (paramRs == this) return;
    paramRs.setX(getX() - this.cornerRadius);
    paramRs.setY(getY() - f());
    this.parentBlock = paramRs;
    paramRs.ia = ((Integer)getTag()).intValue();
  }
  
  public void e(BlockView paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ia));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    paramRs.parentBlock = this;
    this.ia = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((BlockView)view); 
  }
  
  public void f(BlockView paramRs) {
    if (paramRs == this) return;
    View view = this.pa.findViewWithTag(Integer.valueOf(this.ja));
    if (view != null)
      ((BlockView)view).parentBlock = null; 
    paramRs.parentBlock = this;
    this.ja = ((Integer)paramRs.getTag()).intValue();
    if (view != null && view != paramRs)
      paramRs.a((BlockView)view); 
  }
  
  public void g(BlockView paramRs) {
    if (this.ha == ((Integer)paramRs.getTag()).intValue())
      this.ha = -1; 
    if (this.ia == ((Integer)paramRs.getTag()).intValue())
      this.ia = -1; 
    if (this.ja == ((Integer)paramRs.getTag()).intValue())
      this.ja = -1; 
    if (paramRs.fa) {
      int i = this.ka.indexOf(paramRs);
      if (i < 0)
        return; 
      paramRs.qa = "";
      paramRs.ra = "";
      View view = b(this.la.get(i), this.blockColor);
      if (view instanceof BaseBlockView)
        ((BaseBlockView)view).parentBlock = this; 
      this.ka.set(i, view);
      addView(view);
      i();
      o();
    } 
    p().k();
  }
  
  public ArrayList<BlockView> getAllChildren() {
    ArrayList<BlockView> arrayList = new ArrayList<>();
    BlockView rs = this;
    int limit = 200;
    while (true) {
      arrayList.add(rs);
      for (View view : rs.ka) {
        if (view instanceof BlockView)
          arrayList.addAll(((BlockView)view).getAllChildren()); 
      } 
      if (rs.b()) {
        int j = rs.ia;
        if (j != -1) {
          BlockView sub = (BlockView)this.pa.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      if (rs.c()) {
        int j = rs.ja;
        if (j != -1) {
          BlockView sub = (BlockView)this.pa.findViewWithTag(Integer.valueOf(j));
          if (sub != null && sub != this)
            arrayList.addAll(sub.getAllChildren()); 
        }
      } 
      int i = rs.ha;
      if (i != -1 && --limit > 0) {
        BlockView next = (BlockView)this.pa.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return arrayList;
        rs = next;
        continue;
      } 
      return arrayList;
    } 
  }
  
  public BlockBean getBean() {
    BlockBean blockBean = new BlockBean(getTag().toString(), this.T, this.blockType, this.componentType, this.U);
    blockBean.color = this.blockColor;
    for (View view : this.V) {
      if (view instanceof FieldBlockView) {
        blockBean.parameters.add(((FieldBlockView) view).getArgValue().toString());
      } else if (view instanceof BlockView) {
        blockBean.parameters.add("@" + view.getTag().toString());
      }
    } 
    blockBean.subStack1 = this.ia;
    blockBean.subStack2 = this.ja;
    blockBean.nextBlock = this.ha;
    return blockBean;
  }
  
  public int getBlockType() {
    return this.oa;
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
      j = rs.ha;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)this.pa.findViewWithTag(Integer.valueOf(j));
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
      if (rs.b()) {
        int k = rs.ia;
        j = i;
        if (k != -1) {
          BlockView sub1 = (BlockView)this.pa.findViewWithTag(Integer.valueOf(k));
          if (sub1 != null) {
            j = this.cornerRadius;
            j = Math.max(i, sub1.getWidthSum() + j);
          }
        } 
      } 
      i = j;
      if (rs.c()) {
        int k = rs.ja;
        i = j;
        if (k != -1) {
          BlockView sub2 = (BlockView)this.pa.findViewWithTag(Integer.valueOf(k));
          if (sub2 != null) {
            i = this.cornerRadius;
            i = Math.max(j, sub2.getWidthSum() + i);
          }
        } 
      } 
      j = rs.ha;
      if (j != -1 && --limit > 0) {
        BlockView next = (BlockView)this.pa.findViewWithTag(Integer.valueOf(j));
        if (next == null || next == this) break;
        rs = next;
        continue;
      } 
      return i;
    }
    return i;
  }
  
  public BlockView h() {
    BlockView rs = this;
    int limit = 200;
    while (true) {
      int i = rs.ha;
      if (i != -1 && --limit > 0) {
        BlockView next = (BlockView)this.pa.findViewWithTag(Integer.valueOf(i));
        if (next == null || next == this) return rs;
        rs = next;
        continue;
      } 
      return rs;
    } 
  }
  
  public final void i() {
    this.V = new ArrayList<View>();
    for (int b = 0; b < this.ka.size(); b++) {
      View view = this.ka.get(b);
      if (view instanceof BlockView || view instanceof FieldBlockView)
        this.V.add(view); 
    } 
  }
  
  public final void j() {
    TextView textView = this.ma;
    if (textView != null) {
      textView.bringToFront();
      this.ma.setX(this.leftIndent);
      this.ma.setY((g() - this.bottomPadding));
    } 
  }
  
  public void k() {
    BlockView current = this;
    int limit = 1000;
    while (current != null && --limit > 0) {
      current.kSingle();
      int next = current.ha;
      if (next > -1) {
        BlockView nextRs = (BlockView) current.pa.findViewWithTag(Integer.valueOf(next));
        if (nextRs == null || nextRs == this) break;
        nextRs.setX(current.getX());
        nextRs.setY(current.getY() + (float) current.d());
        nextRs.bringToFront();
        current = nextRs;
      } else {
        current = null;
      }
    }
  }

  private void kSingle() {
    bringToFront();
    int xOffset = this.leftIndent;
    for (int idx = 0; idx < this.ka.size(); idx++) {
      View child = (View) this.ka.get(idx);
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
      if (((String) this.la.get(idx)).equals("label")) {
        childWidth = a((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof FieldBlockView) {
        childWidth = ((FieldBlockView) child).getW();
      }
      if (isRs) {
        childWidth = ((BlockView) child).getWidthSum();
      }
      xOffset += childWidth + this.da;
      if (isRs) {
        BlockView childRs = (BlockView) child;
        child.setY(getY() + (float) this.topSpacing + (float) ((this.na - childRs.na - 1) * this.shadowOffset));
        childRs.k();
      } else {
        child.setY((float) (this.topSpacing + this.na * this.shadowOffset));
      }
    }
    int w = xOffset;
    if (this.blockType.equals("b") || this.blockType.equals("d") || this.blockType.equals("s") || this.blockType.equals("a")) {
      w = Math.max(xOffset, this.W);
    }
    int w2 = w;
    if (this.blockType.equals(" ") || this.blockType.equals("") || this.blockType.equals("f")) {
      w2 = Math.max(w, this.aa);
    }
    int w3 = w2;
    if (this.blockType.equals("c") || this.blockType.equals("e")) {
      w3 = Math.max(w2, this.ca);
    }
    int w4 = w3;
    if (this.blockType.equals("h")) {
      w4 = Math.max(w3, this.ba);
    }
    a((float) (this.rightIndent + w4), (float) (this.topSpacing + this.textHeight + this.na * this.shadowOffset * 2 + this.bottomSpacing), true);
    if (b()) {
      int ss1Height = this.minHeight;
      int sub1 = this.ia;
      if (sub1 > -1) {
        BlockView sub1Rs = (BlockView) this.pa.findViewWithTag(Integer.valueOf(sub1));
        if (sub1Rs != null) {
          sub1Rs.setX(getX() + (float) this.cornerRadius);
          sub1Rs.setY(getY() + (float) f());
          sub1Rs.bringToFront();
          sub1Rs.k();
          ss1Height = sub1Rs.getHeightSum();
        }
      }
      setSubstack1Height(ss1Height);
      int ss2Height = this.minHeight;
      int sub2 = this.ja;
      if (sub2 > -1) {
        BlockView sub2Rs = (BlockView) this.pa.findViewWithTag(Integer.valueOf(sub2));
        if (sub2Rs != null) {
          sub2Rs.setX(getX() + (float) this.cornerRadius);
          sub2Rs.setY(getY() + (float) g());
          sub2Rs.bringToFront();
          sub2Rs.k();
          ss2Height = sub2Rs.getHeightSum();
          if (sub2Rs.h().ga) {
            ss2Height += this.borderWidth;
          }
        }
      }
      setSubstack2Height(ss2Height);
      j();
    }
  }
  
  public void l() {
    setDrawingCacheEnabled(false);
    float scale = this.density;
    this.W = (int) (this.W * scale);
    this.aa = (int) (this.aa * scale);
    this.ba = (int) (this.ba * scale);
    this.ca = (int) (this.ca * scale);
    this.da = (int) (this.da * scale);
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
        this.fa = true;
        break;
      case 10:
        this.ga = true;
        break;
      case 11:
        this.ea = true;
        break;
      default:
        break;
    }
    int color = BlockColorMapper.a(this.U, this.blockType);
    if (!this.ea && !this.U.equals("definedFunc") && !this.U.equals("getVar")
        && !this.U.equals("getResStr") && !this.U.equals("getArg") && color != -7711273) {
      this.T = StringResource.b().a(getContext(), this.U);
    }
    if (color == -7711273) {
      mod.hey.studios.editor.manage.block.ExtraBlockInfo info = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockInfo(this.U);
      mod.hey.studios.editor.manage.block.ExtraBlockInfo blockInfo = info;
      if (mod.hey.studios.project.ProjectTracker.SC_ID != null && info.isMissing
          && !mod.hey.studios.project.ProjectTracker.SC_ID.equals("")) {
        blockInfo = mod.hey.studios.editor.manage.block.v2.BlockLoader.getBlockFromProject(
            mod.hey.studios.project.ProjectTracker.SC_ID, this.U);
        android.util.Log.d("BlockView", "BlockView:returned block: " + new com.google.gson.Gson().toJson(blockInfo));
      }
      this.spec2 = blockInfo.getSpec2();
      if (this.T.equals("")) {
        this.T = blockInfo.getSpec();
      }
      if (this.T.equals("")) {
        this.T = this.U;
      }
      setSpec(this.T);
      int blockColor = blockInfo.getColor();
      int paletteColor = blockInfo.getPaletteColor();
      if (!this.U.equals("definedFunc") && !this.U.equals("getVar")
          && !this.U.equals("getResStr") && !this.U.equals("getArg")) {
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
      if (this.T.equals("")) {
        this.T = this.U;
      }
      setSpec(this.T);
    }
    this.blockColor = color;
  }
  
  public void m() {
    BlockView rs2;
    BlockView rs1 = this;
    int count = 0;
    do {
      rs1.n();
      rs2 = rs1.parentBlock;
      count++;
      if (count > 500) {
        android.util.Log.e("BlockView", "m() infinite loop detected! count=" + count + " tag=" + rs1.getTag() + " E.tag=" + (rs2 != null ? rs2.getTag() : "null"));
        break;
      }
      rs1 = rs2;
    } while (rs2 != null);
  }
  
  public void n() {
    int xOffset = this.leftIndent;
    for (int idx = 0; idx < this.ka.size(); idx++) {
      View child = (View) this.ka.get(idx);
      int childWidth;
      if (((String) this.la.get(idx)).equals("label")) {
        childWidth = a((TextView) child);
      } else {
        childWidth = 0;
      }
      if (child instanceof FieldBlockView) {
        childWidth = ((FieldBlockView) child).getW();
      }
      if (child instanceof BlockView) {
        childWidth = ((BlockView) child).getWidthSum();
      }
      xOffset += childWidth + this.da;
    }
    int w = xOffset;
    if (this.blockType.equals("b") || this.blockType.equals("d") || this.blockType.equals("s") || this.blockType.equals("a")) {
      w = Math.max(xOffset, this.W);
    }
    int w2 = w;
    if (this.blockType.equals(" ") || this.blockType.equals("") || this.blockType.equals("o")) {
      w2 = Math.max(w, this.aa);
    }
    int w3 = w2;
    if (this.blockType.equals("c") || this.blockType.equals("e")) {
      w3 = Math.max(w2, this.ca);
    }
    int w4 = w3;
    if (this.blockType.equals("h")) {
      w4 = Math.max(w3, this.ba);
    }
    int finalW = w4;
    TextView label = this.ma;
    if (label != null) {
      finalW = Math.max(w4, label.getWidth() + this.leftIndent + 2);
    }
    a((float) (this.rightIndent + finalW), (float) (this.topSpacing + this.textHeight + this.na * this.shadowOffset * 2 + this.bottomSpacing), false);
  }
  
  public void o() {
    BlockView rs = this;
    int limit = 200;
    while (rs != null && --limit > 0) {
      Iterator<View> iterator = rs.V.iterator();
      int i = 0;
      while (iterator.hasNext()) {
        View view = iterator.next();
        if (view instanceof BlockView)
          i = Math.max(i, ((BlockView)view).na + 1); 
      } 
      rs.na = i;
      rs.n();
      if (rs.fa) {
        rs = rs.parentBlock;
        continue;
      } 
      break;
    } 
  }
  
  public BlockView p() {
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
  
  public void setBlockType(int paramInt) {
    this.oa = paramInt;
  }
  
  public void setSpec(String paramString) {
    this.T = paramString;
    removeAllViews();
    a(this.T, this.blockColor);
    Iterator<View> iterator = this.ka.iterator();
    while (true) {
      if (!iterator.hasNext()) {
        i();
        if (this.blockType.equals("e") && this.U.equals("ifElse")) {
          this.ma = a(StringResource.b().a(getContext(), "else"));
          addView((View)this.ma);
        } 
        if (this.blockType.equals("e") && !this.spec2.equals("")) {
          this.ma = a(this.spec2);
          addView((View)this.ma);
        } 
        k();
        return;
      } 
      addView(iterator.next());
    } 
  }
}
